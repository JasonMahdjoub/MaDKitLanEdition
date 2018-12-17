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

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;

import com.distrimind.util.crypto.*;
import gnu.vm.jgnu.security.DigestException;
import gnu.vm.jgnu.security.InvalidAlgorithmParameterException;
import gnu.vm.jgnu.security.InvalidKeyException;
import gnu.vm.jgnu.security.NoSuchAlgorithmException;
import gnu.vm.jgnu.security.NoSuchProviderException;
import gnu.vm.jgnu.security.spec.InvalidKeySpecException;
import gnu.vm.jgnux.crypto.BadPaddingException;
import gnu.vm.jgnux.crypto.IllegalBlockSizeException;
import gnu.vm.jgnux.crypto.NoSuchPaddingException;
import gnu.vm.jgnux.crypto.ShortBufferException;

import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.madkit.util.SerializationTools;
import com.distrimind.madkit.util.ExternalizableAndSizable;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings("ExternalizableWithoutPublicNoArgConstructor")
class IdentifiersPropositionMessage extends AccessMessage {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1409236452371137326L;

	private Identifier[] identifiers;
	private boolean isEncrypted;
	private final transient short nbAnomalies;

	@SuppressWarnings("unused")
	IdentifiersPropositionMessage()
	{
		nbAnomalies=0;
	}
	
	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		isEncrypted=in.readBoolean();
		ExternalizableAndSizable[] s=SerializationTools.readExternalizableAndSizables(in, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, false);
		assert s != null;
		identifiers=new Identifier[s.length];
		for (int i=0;i<s.length;i++)
		{
			if (!(s[i] instanceof Identifier))
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			identifiers[i]=(Identifier)s[i];
			if (isEncrypted && !identifiers[i].getCloudIdentifier().isAutoIdentifiedHostWithPublicKey() && !(identifiers[i] instanceof EncryptedIdentifier))
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			if (isEncrypted && identifiers[i].getCloudIdentifier().isAutoIdentifiedHostWithPublicKey() && (identifiers[i] instanceof EncryptedIdentifier))
				throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}
		
	}
	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		oos.writeBoolean(isEncrypted);
		SerializationTools.writeExternalizableAndSizables(oos, identifiers, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, false);
		
		
	}
	
	
	public IdentifiersPropositionMessage(Collection<Identifier> _id_pws, P2PASymmetricSecretMessageExchanger cipher,
			boolean encryptIdentifiers, short nbAnomalies) throws InvalidKeyException, IOException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, IllegalStateException, ShortBufferException {
		identifiers = new Identifier[_id_pws.size()];
		isEncrypted = encryptIdentifiers;
		int index = 0;
		for (Identifier ip : _id_pws) {
			if (encryptIdentifiers && !ip.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey())
				identifiers[index++] = new EncryptedIdentifier(ip, cipher);
			else
				identifiers[index++] = ip;
		}
		this.nbAnomalies = nbAnomalies;
	}

	public IdentifiersPropositionMessage(Collection<Identifier> _id_pws, AbstractSecureRandom random, AbstractMessageDigest messageDigest,
			boolean encryptIdentifiers, short nbAnomalies, byte[] distantGeneratedSalt, Map<Identifier, P2PLoginAgreement> jpakes, ASymmetricLoginAgreementType aSymmetricLoginAgreementType) throws  DigestException {
		identifiers = new Identifier[_id_pws.size()];
		isEncrypted = encryptIdentifiers;
		int index = 0;
		for (Identifier ip : _id_pws) {
			if (encryptIdentifiers && !ip.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey())
			{
				identifiers[index++] = new EncryptedIdentifier(ip, random, messageDigest, distantGeneratedSalt);
			}
			else
			{
				identifiers[index++] = ip;
			}
			if (jpakes!=null && ip.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey())
			{
				jpakes.put(ip, aSymmetricLoginAgreementType.getAgreementAlgorithmForASymmetricSignatureRequester(random, ip.getCloudIdentifier().getHostKeyPair()));
			}

		}
		this.nbAnomalies = nbAnomalies;
	}


	
	@Override
	public short getNbAnomalies() {
		return nbAnomalies;
	}

	public ArrayList<Identifier> getValidDecodedIdentifiers(LoginData loginData,
			P2PASymmetricSecretMessageExchanger cipher) throws AccessException {
		ArrayList<Identifier> res = new ArrayList<>();
		if (isEncrypted) {
			for (Identifier id : identifiers) {
				if (id instanceof EncryptedIdentifier) {
					Identifier i = loginData.getIdentifier((EncryptedIdentifier) id, cipher);
					if (i != null)
						res.add(i);
				}
				else
					res.add(id);
			}
		} else {
			res.addAll(Arrays.asList(identifiers));
		}

		return res;
	}
	public ArrayList<Identifier> getValidDecodedLocalIdentifiers(LoginData loginData,
			AbstractMessageDigest messageDigest, byte[] localGeneratedSalt ) throws AccessException {
		ArrayList<Identifier> res = new ArrayList<>();
		if (isEncrypted) {
			for (Identifier id : identifiers) {
				if (id instanceof EncryptedIdentifier) {
					Identifier i = loginData.getLocalIdentifier((EncryptedIdentifier) id, messageDigest, localGeneratedSalt);
					//Identifier idLocal=loginData.localiseIdentifier(i);
					if (i != null && (!i.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey() || i.getCloudIdentifier().getHostKeyPair() != null))
						res.add(i);
				}
				else
				{
					Identifier i = loginData.localiseIdentifier(id);
					if (i != null && (!i.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey() || i.getCloudIdentifier().getHostKeyPair() != null))
						res.add(i);
				}
			}
		} else {
			for(Identifier id : identifiers) {
				Identifier idLocal=loginData.localiseIdentifier(id);

				if (idLocal!=null && (!idLocal.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey() || idLocal.getCloudIdentifier().getHostKeyPair()!=null))
					res.add(idLocal);
			}
		}

		return res;
	}

	public IdentifiersPropositionMessage getIdentifiersPropositionMessageAnswer(LoginData loginData,
			P2PASymmetricSecretMessageExchanger cipher, boolean encryptIdentifiers)
			throws AccessException, InvalidKeyException, IOException, IllegalBlockSizeException, BadPaddingException,
			NoSuchAlgorithmException, InvalidKeySpecException, NoSuchPaddingException,
			InvalidAlgorithmParameterException, NoSuchProviderException, IllegalStateException, ShortBufferException {
		ArrayList<Identifier> validID = getValidDecodedIdentifiers(loginData, cipher);
		int nbAno = identifiers.length - validID.size();
		return new IdentifiersPropositionMessage(validID, cipher, encryptIdentifiers,
				loginData.canTakesLoginInitiative()
						? ((validID.size() == 0 && identifiers.length > 0) ? (short) 1 : (short) 0)
						: (nbAno > Short.MAX_VALUE) ? Short.MAX_VALUE : (short) nbAno);
	}
	public IdentifiersPropositionMessage getIdentifiersPropositionMessageAnswer(LoginData loginData,
			AbstractSecureRandom random, AbstractMessageDigest messageDigest, boolean encryptIdentifiers, List<Identifier> identifiers, byte[] distantGeneratedSalt, byte[] localGeneratedSalt, Map<Identifier, P2PLoginAgreement> jpakes, ASymmetricLoginAgreementType aSymmetricLoginAgreementType)
			throws AccessException,  DigestException {
		ArrayList<Identifier> validID = getValidDecodedLocalIdentifiers(loginData, messageDigest, localGeneratedSalt);
		identifiers.addAll(validID);
		int nbAno = this.identifiers.length - validID.size();
		return new IdentifiersPropositionMessage(validID, random, messageDigest, encryptIdentifiers,
				loginData.canTakesLoginInitiative()
						? ((validID.size() == 0 && this.identifiers.length > 0) ? (short) 1 : (short) 0)
						: (nbAno > Short.MAX_VALUE) ? Short.MAX_VALUE : (short) nbAno, distantGeneratedSalt, jpakes, aSymmetricLoginAgreementType);
	}

	public IdPwMessage getIdPwMessage(LoginData loginData, P2PASymmetricSecretMessageExchanger cipher,
			boolean encryptIdentifiers) throws AccessException, InvalidKeyException, IOException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, IllegalStateException, ShortBufferException {
		ArrayList<IdentifierPassword> res = new ArrayList<>();
		int nbAno = 0;
		if (isEncrypted) {
			for (Identifier id : identifiers) {
				Identifier i;
				if (id instanceof EncryptedIdentifier)
					i = loginData.getIdentifier((EncryptedIdentifier) id, cipher);
				else
					i=id;

				if (i != null) {
					Identifier localId = loginData.localiseIdentifier(i);

					PasswordKey pw = loginData.getPassword(localId);
					if (pw != null)
						res.add(new IdentifierPassword(localId, pw));
					else
						++nbAno;
				} else
					++nbAno;
			}

		} else {
			for (Identifier id : identifiers) {
				Identifier localId = loginData.localiseIdentifier(id);
				if (localId==null) {
					++nbAno;
					continue;
				}
				PasswordKey pw = loginData.getPassword(localId);
				if (pw != null)
					res.add(new IdentifierPassword(localId, pw));
				else
					++nbAno;
			}
		}
		return new IdPwMessage(res, cipher, encryptIdentifiers,
				loginData.canTakesLoginInitiative()
						? ((res.size() == 0 && identifiers.length > 0) ? (short) 1 : (short) 0)
						: (nbAno > Short.MAX_VALUE) ? Short.MAX_VALUE : (short) nbAno);
}
	private int getJakeMessageSub(Identifier distantID, List<PairOfIdentifiers> acceptedIdentifiers, LoginData loginData, Map<Identifier, P2PLoginAgreement> agreements, P2PLoginAgreementType agreementType, ASymmetricLoginAgreementType aSymmetricLoginAgreementType, AbstractSecureRandom random,
								   ArrayList<Identifier> newIdentifiersToAdd, MessageDigestType messageDigestType, PasswordHashType passwordHashType, ASymmetricPublicKey myPublicKey) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
		int nbAno=0;
		if (acceptedIdentifiers!=null) {
			for (PairOfIdentifiers poi : acceptedIdentifiers)
				if (poi.getDistantIdentifier().equals(distantID))
					return nbAno;
		}

		ASymmetricPublicKey pubKey;
		Identifier localId = loginData.localiseIdentifier(distantID);

		if (localId!=null) {

			if (distantID.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey()) {
				if (newIdentifiersToAdd != null && localId.getCloudIdentifier().isAutoIdentifiedHostWithPublicKey() && !localId.getHostIdentifier().equals(HostIdentifier.getNullHostIdentifierSingleton())) {
					boolean found = false;
					if (acceptedIdentifiers != null) {

						for (PairOfIdentifiers poi : acceptedIdentifiers)
							if (poi.getLocalIdentifier().equals(localId) && !poi.getDistantIdentifier().getHostIdentifier().equals(HostIdentifier.getNullHostIdentifierSingleton()))
								found = true;
					}
					if (!found) {
						newIdentifiersToAdd.add(localId);

					}
				}

				if ((pubKey = distantID.getCloudIdentifier().getHostPublicKey()).getAuthentifiedSignatureAlgorithmType() != null) {
					P2PLoginAgreement agreement = aSymmetricLoginAgreementType.getAgreementAlgorithmForASymmetricSignatureReceiver(random, pubKey);
					agreements.put(distantID, agreement);
				} else
					++nbAno;
			} else {

				PasswordKey pw = loginData.getPassword(localId);
				if (pw != null) {

					P2PLoginAgreement agreement = agreementType.getAgreementAlgorithm(random, localId, pw.getPasswordBytes(), pw.isKey(), pw.getSecretKeyForSignature(), messageDigestType, passwordHashType, myPublicKey);
					agreements.put(localId, agreement);
				} else
					++nbAno;
			}
		}
		else
			++nbAno;
		return nbAno;
	}
	public JPakeMessage getJPakeMessage(List<PairOfIdentifiers> acceptedIdentifiers, LoginData loginData, Map<Identifier, P2PLoginAgreement> agreements, P2PLoginAgreementType agreementType, ASymmetricLoginAgreementType aSymmetricLoginAgreementType, AbstractSecureRandom random, AbstractMessageDigest messageDigest,
										boolean encryptIdentifiers, byte[] distantGeneratedSalt, byte[] localGeneratedSalt, ArrayList<Identifier> newIdentifiersToAdd,MessageDigestType messageDigestType, PasswordHashType passwordHashType, ASymmetricPublicKey myPublicKey) throws Exception {
		int nbAno = 0;
		if (encryptIdentifiers) {
			for (Identifier id : identifiers) {
				Identifier i;
				if (id instanceof EncryptedIdentifier)
					i = loginData.getIdentifier((EncryptedIdentifier) id, messageDigest, localGeneratedSalt);
				else
					i = id;
				if (i != null) {
					nbAno+=getJakeMessageSub(i, acceptedIdentifiers, loginData, agreements, agreementType, aSymmetricLoginAgreementType, random, newIdentifiersToAdd, messageDigestType, passwordHashType, myPublicKey);
				} else
					++nbAno;
			}

		} else {
			for (Identifier id : identifiers) {
				nbAno+=getJakeMessageSub(id, acceptedIdentifiers, loginData, agreements, agreementType, aSymmetricLoginAgreementType, random, newIdentifiersToAdd, messageDigestType, passwordHashType, myPublicKey);
			}
		}
		return new JPakeMessage(agreements, encryptIdentifiers,
				loginData.canTakesLoginInitiative()
						? ((agreements.size() == 0 && identifiers.length > 0) ? (short) 1 : (short) 0)
						: (nbAno > Short.MAX_VALUE) ? Short.MAX_VALUE : (short) nbAno, random, messageDigest, distantGeneratedSalt);
	}

	

	@Override
	public boolean checkDifferedMessages() {
		return false;
	}

}
