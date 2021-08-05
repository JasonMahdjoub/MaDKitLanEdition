package com.distrimind.madkit.kernel.network.connection.secured;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java language

This software is governed by the CeCILL-C license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL-C
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL-C license and that you accept its terms.
 */

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.util.crypto.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.2.0
 */
class MaximumBodyOutputSizeComputer {
	private final EncryptionSignatureHashEncoder maxEncoder;
	private final EncryptionSignatureHashEncoder maxEncoderWithoutEncryption;

	MaximumBodyOutputSizeComputer(boolean encryptionEnabled, SymmetricEncryptionType symmetricEncryptionType, short symmetricKeySizeBits, SymmetricAuthenticatedSignatureType symmetricSignatureType, MessageDigestType messageDigestType) throws BlockParserException {
		this(encryptionEnabled, symmetricEncryptionType, symmetricKeySizeBits, messageDigestType);
		try {
			SymmetricSecretKey ssk=symmetricSignatureType.getKeyGenerator(SecureRandomType.DEFAULT.getSingleton(null), symmetricKeySizeBits).generateKey();
			if (!encryptionEnabled || !symmetricEncryptionType.isAuthenticatedAlgorithm()) {
				maxEncoder.withSymmetricSecretKeyForSignature(ssk);
			}
			else {
				maxEncoderWithoutEncryption.withSymmetricSecretKeyForSignature(ssk);
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new BlockParserException(e);
		}
	}
	MaximumBodyOutputSizeComputer(boolean encryptionEnabled, SymmetricSecretKey symmetricSecretKeyForEncryption, SymmetricSecretKey symmetricSecretKeyForSignature, MessageDigestType messageDigestType) throws BlockParserException {
		try {
			maxEncoder = new EncryptionSignatureHashEncoder();
			if (encryptionEnabled && symmetricSecretKeyForEncryption.getEncryptionAlgorithmType().isAuthenticatedAlgorithm())
				maxEncoderWithoutEncryption = new EncryptionSignatureHashEncoder();
			else
				maxEncoderWithoutEncryption=null;
			init(encryptionEnabled, encryptionEnabled?symmetricSecretKeyForEncryption:null, messageDigestType);
			if (maxEncoderWithoutEncryption==null) {
				maxEncoder.withSymmetricSecretKeyForSignature(symmetricSecretKeyForSignature);
			}
			else {
				maxEncoderWithoutEncryption.withSymmetricSecretKeyForSignature(symmetricSecretKeyForSignature);
			}

		} catch (IOException e) {
			throw new BlockParserException(e);
		}
	}
	private void init(boolean encryptionEnabled, SymmetricSecretKey symmetricSecretKeyForEncryption, MessageDigestType messageDigestType) throws BlockParserException {
		try {
			if (encryptionEnabled)
				maxEncoder.withSymmetricSecretKeyForEncryption(SecureRandomType.DEFAULT.getSingleton(null), symmetricSecretKeyForEncryption);
			if (messageDigestType != null) {
				maxEncoder.withMessageDigestType(messageDigestType);
				if (maxEncoderWithoutEncryption!=null)
					maxEncoderWithoutEncryption.withMessageDigestType(messageDigestType);
			}

		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new BlockParserException(e);
		}
	}
	private MaximumBodyOutputSizeComputer(boolean encryptionEnabled, SymmetricEncryptionType symmetricEncryptionType, short symmetricKeySizeBits, MessageDigestType messageDigestType) throws BlockParserException{
		try {
			maxEncoder = new EncryptionSignatureHashEncoder();
			if (encryptionEnabled && symmetricEncryptionType.isAuthenticatedAlgorithm())
				maxEncoderWithoutEncryption = new EncryptionSignatureHashEncoder();
			else
				maxEncoderWithoutEncryption=null;
			init(encryptionEnabled, encryptionEnabled?symmetricEncryptionType.getKeyGenerator(SecureRandomType.DEFAULT.getSingleton(null), symmetricKeySizeBits).generateKey():null, messageDigestType);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new BlockParserException(e);
		}
	}
	MaximumBodyOutputSizeComputer(boolean encryptionEnabled, SymmetricEncryptionType symmetricEncryptionType, short symmetricKeySizeBits, ASymmetricAuthenticatedSignatureType asymmetricSignatureType, short asymmetricKeySizeBits, MessageDigestType messageDigestType) throws BlockParserException {
		this(encryptionEnabled, symmetricEncryptionType, symmetricKeySizeBits, messageDigestType);
		try {
			maxEncoder.withASymmetricPrivateKeyForSignature(asymmetricSignatureType.getKeyPairGenerator(SecureRandomType.DEFAULT.getSingleton(null), asymmetricKeySizeBits).generateKeyPair().getASymmetricPrivateKey());
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new BlockParserException(e);
		}


	}

	int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException {
		try {
			int res=(int)maxEncoder.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
			if (maxEncoderWithoutEncryption!=null)
				return Math.max(res, (int)maxEncoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize);
			else
				return res;
		} catch (IOException e) {
			throw new BlockParserException(e);
		}
	}
}
