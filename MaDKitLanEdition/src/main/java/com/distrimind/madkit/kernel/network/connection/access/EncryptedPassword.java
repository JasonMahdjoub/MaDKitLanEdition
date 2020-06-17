/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.kernel.network.connection.access;

import com.distrimind.util.crypto.P2PASymmetricSecretMessageExchanger;
import com.distrimind.util.crypto.SymmetricSecretKey;
import com.distrimind.util.io.SecureExternalizable;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import com.distrimind.util.io.SerializationTools;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;

/**
 * Represent an encrypted password
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadKitLanEdition 1.0
 * @see PasswordKey
 */
public class EncryptedPassword extends PasswordKey implements SecureExternalizable {

	public static final int MAX_ENCRYPTED_PASSWORD_LENGTH=MAX_PASSWORD_LENGTH+512;
	
	private byte[] bytes;

	@SuppressWarnings("unused")
	EncryptedPassword()
	{
		
	}
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		bytes=in.readBytesArray(false , MAX_ENCRYPTED_PASSWORD_LENGTH);

	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeBytesArray(bytes,false, MAX_ENCRYPTED_PASSWORD_LENGTH);
	}
	
	public EncryptedPassword(PasswordKey password, P2PASymmetricSecretMessageExchanger cipher)
			throws InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IllegalStateException {
		if (password == null)
			throw new NullPointerException("password");
		if (cipher == null)
			throw new NullPointerException("cipher");
		bytes = cipher.encode(password.getPasswordBytes(), password.getSaltBytes(), password.isKey());

	}
	

	@Override
	public byte[] getPasswordBytes() {
		return bytes;
	}

	/**
	 * Tells if the given password corresponds to the current encrypted password,
	 * considering the given cipher.
	 * 
	 * @param originalPassword
	 *            the original password
	 * @param cipher
	 *            the cipher
	 * @return true if the given password corresponds to the current encrypted
	 *         password, considering the given cipher.
	 * @throws IOException
	 *             if a problem occurs
	 */
	public boolean verifyWithLocalPassword(PasswordKey originalPassword, P2PASymmetricSecretMessageExchanger cipher)
			throws IOException {
		if (originalPassword == null)
			throw new NullPointerException("originalPassword");
		if (cipher == null)
			throw new NullPointerException("cipher");
		return cipher.verifyDistantMessage(originalPassword.getPasswordBytes(), originalPassword.getSaltBytes(), bytes,
				originalPassword.isKey());
	}

	@Override
	public byte[] getSaltBytes() {
		return null;
	}

	@Override
	public boolean isKey() {
		return true;
	}


	@Override
	public int getInternalSerializedSize() {
		return SerializationTools.getInternalSize(bytes, MAX_ENCRYPTED_PASSWORD_LENGTH);
	}
	@Override
	public SymmetricSecretKey getSecretKeyForSignature() {
		return null;
	}

}
