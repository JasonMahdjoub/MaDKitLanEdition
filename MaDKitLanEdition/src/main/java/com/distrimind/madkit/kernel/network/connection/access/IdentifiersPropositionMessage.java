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

import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.util.crypto.*;
import com.distrimind.util.io.*;

import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
class IdentifiersPropositionMessage extends AccessMessage {

	static class AutoIdentificationCredentials
	{
		private P2PLoginAgreement p2PLoginAgreementForCloudIdentifier;
		private P2PLoginAgreement p2PLoginAgreementForHostIdentifier;

		AutoIdentificationCredentials(P2PLoginAgreement p2PLoginAgreementForCloudIdentifier, P2PLoginAgreement p2PLoginAgreementForHostIdentifier) {
			this.p2PLoginAgreementForCloudIdentifier = p2PLoginAgreementForCloudIdentifier;
			this.p2PLoginAgreementForHostIdentifier = p2PLoginAgreementForHostIdentifier;
		}

		P2PLoginAgreement getP2PLoginAgreementForCloudIdentifier() {
			return p2PLoginAgreementForCloudIdentifier;
		}

		P2PLoginAgreement getP2PLoginAgreementForHostIdentifier() {
			return p2PLoginAgreementForHostIdentifier;
		}
	}

	private Identifier[] identifiers;
	private boolean isEncrypted;
	private final transient short nbAnomalies;

	@SuppressWarnings("unused")
	IdentifiersPropositionMessage()
	{
		nbAnomalies=0;
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		isEncrypted=in.readBoolean();
		SecureExternalizable[] s=in.readObject(false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, SecureExternalizable[].class);
		identifiers=new Identifier[s.length];
		for (int i=0;i<s.length;i++)
		{
			if (!(s[i] instanceof Identifier))
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			identifiers[i]=(Identifier)s[i];
			if (isEncrypted && !(identifiers[i].getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey()
					|| identifiers[i].getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey()) && !(identifiers[i] instanceof EncryptedIdentifier))
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			if (isEncrypted && (identifiers[i].getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey()
					|| identifiers[i].getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey()) && (identifiers[i] instanceof EncryptedIdentifier))
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}
		
	}
	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeBoolean(isEncrypted);
		oos.writeObject(identifiers, false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE);

		
	}
	
	
	/*public IdentifiersPropositionMessage(Collection<Identifier> _id_pws, P2PASymmetricSecretMessageExchanger cipher,
			boolean encryptIdentifiers, short nbAnomalies) throws InvalidKeyException, IOException,
			IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, InvalidKeySpecException,
			NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, IllegalStateException, ShortBufferException {
		identifiers = new Identifier[_id_pws.size()];
		isEncrypted = encryptIdentifiers;
		int index = 0;
		for (Identifier ip : _id_pws) {
			if (encryptIdentifiers && !ip.getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey())
				identifiers[index++] = new EncryptedIdentifier(ip, cipher);
			else {
				identifiers[index++] = ip;
			}
		}
		this.nbAnomalies = nbAnomalies;
	}*/

	public IdentifiersPropositionMessage(Collection<Identifier> _id_pws, AbstractSecureRandom random, AbstractMessageDigest messageDigest,
			boolean encryptIdentifiers, short nbAnomalies, byte[] distantGeneratedSalt, Map<Identifier, AutoIdentificationCredentials> jpakes, ASymmetricLoginAgreementType aSymmetricLoginAgreementType) throws DigestException {
		identifiers = new Identifier[_id_pws.size()];
		isEncrypted = encryptIdentifiers;
		int index = 0;
		for (Identifier ip : _id_pws) {
			if (ip.getCloudIdentifier().getAuthenticationMethod()== Identifier.AuthenticationMethod.NOT_DEFINED
					&& ip.getHostIdentifier().getAuthenticationMethod()== Identifier.AuthenticationMethod.NOT_DEFINED)
				continue;
			if (encryptIdentifiers && !ip.getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey() && !ip.getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey())
			{
				identifiers[index++] = new EncryptedIdentifier(ip, random, messageDigest, distantGeneratedSalt);
			}
			else
			{
				identifiers[index++] = ip;
			}
			P2PLoginAgreement loginAgreementForCloudIdentifier=null;
			if (jpakes!=null && ip.getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey())
			{
				loginAgreementForCloudIdentifier=aSymmetricLoginAgreementType.getAgreementAlgorithmForASymmetricSignatureRequester(random, ip.getCloudIdentifier().getAuthenticationKeyPair());
			}
			P2PLoginAgreement loginAgreementForHostIdentifier=null;
			if (jpakes!=null && ip.getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey())
			{
				loginAgreementForHostIdentifier=aSymmetricLoginAgreementType.getAgreementAlgorithmForASymmetricSignatureRequester(random, ip.getHostIdentifier().getAuthenticationKeyPair());
			}
			if (loginAgreementForCloudIdentifier!=null || loginAgreementForHostIdentifier!=null)
			{
				jpakes.put(ip, new AutoIdentificationCredentials(loginAgreementForCloudIdentifier, loginAgreementForHostIdentifier));
			}
		}
		if (index!=identifiers.length)
			identifiers=Arrays.copyOf(identifiers, index);
		this.nbAnomalies = nbAnomalies;
	}


	
	@Override
	public short getNbAnomalies() {
		return nbAnomalies;
	}

	/*public ArrayList<Identifier> getValidDecodedIdentifiers(LoginData loginData,
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
	}*/
	private boolean checkIdentifiers(Identifier local, Identifier distant, LoginData loginData)
	{
		if (local!=null && distant!=null)
		{
			if (local.equals(distant))
				return false;
			if (local.getCloudIdentifier().getAuthenticationMethod()!=distant.getCloudIdentifier().getAuthenticationMethod())
				return false;
			if (local.getHostIdentifier().getAuthenticationMethod()!=distant.getHostIdentifier().getAuthenticationMethod())
				return false;
			if (local.getCloudIdentifier().getAuthenticationMethod()== Identifier.AuthenticationMethod.NOT_DEFINED)
				return false;
			if ((local.getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey()
					|| local.getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey())
					&& !loginData.acceptAutoSignedLogin())
			{
				return false;
			}
			return (!local.getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey() || local.getCloudIdentifier().getAuthenticationKeyPair() != null)
					&& (!local.getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey() || local.getHostIdentifier().getAuthenticationKeyPair() != null);
		}
		else
			return false;
	}
	public ArrayList<Identifier> getValidDecodedLocalIdentifiers(LoginData loginData,
			AbstractMessageDigest messageDigest, byte[] localGeneratedSalt ) throws AccessException {
		ArrayList<Identifier> res = new ArrayList<>();
		if (isEncrypted) {
			for (Identifier id : identifiers) {
				Identifier i;
				if (id instanceof EncryptedIdentifier) {
					 i = loginData.getLocalIdentifier((EncryptedIdentifier) id, messageDigest, localGeneratedSalt);
				}
				else
				{
					i = loginData.localiseIdentifier(id);
				}
				if (checkIdentifiers(i, id, loginData))
					res.add(i);
			}
		} else {
			for(Identifier id : identifiers) {
				Identifier idLocal=loginData.localiseIdentifier(id);
				if (checkIdentifiers(idLocal, id, loginData))
					res.add(idLocal);

			}
		}

		return res;
	}

	/*public IdentifiersPropositionMessage getIdentifiersPropositionMessageAnswer(LoginData loginData,
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
	}*/
	public IdentifiersPropositionMessage getIdentifiersPropositionMessageAnswer(LoginData loginData,
			AbstractSecureRandom random, AbstractMessageDigest messageDigest, boolean encryptIdentifiers, List<Identifier> identifiers, byte[] distantGeneratedSalt, byte[] localGeneratedSalt, Map<Identifier, AutoIdentificationCredentials> jpakes, ASymmetricLoginAgreementType aSymmetricLoginAgreementType)
			throws AccessException,  DigestException {
		ArrayList<Identifier> validID = getValidDecodedLocalIdentifiers(loginData, messageDigest, localGeneratedSalt);
		identifiers.addAll(validID);
		int nbAno = this.identifiers.length - validID.size();
		return new IdentifiersPropositionMessage(validID, random, messageDigest, encryptIdentifiers,
				loginData.canTakesLoginInitiative()
						? ((validID.size() == 0 && this.identifiers.length > 0) ? (short) 1 : (short) 0)
						: (nbAno > Short.MAX_VALUE) ? Short.MAX_VALUE : (short) nbAno, distantGeneratedSalt, jpakes, aSymmetricLoginAgreementType);
	}

	/*public IdPwMessage getIdPwMessage(LoginData loginData, P2PASymmetricSecretMessageExchanger cipher,
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
}*/
	private int getJakeMessageSub(Identifier distantID, List<PairOfIdentifiers> acceptedIdentifiers, LoginData loginData, Map<Identifier, AutoIdentificationCredentials> agreements, P2PLoginAgreementType agreementType, ASymmetricLoginAgreementType aSymmetricLoginAgreementType, AbstractSecureRandom random,
								   ArrayList<Identifier> newIdentifiersToAdd, MessageDigestType messageDigestType, PasswordHashType passwordHashType, ASymmetricPublicKey myPublicKey) throws InvalidKeySpecException, NoSuchAlgorithmException, NoSuchProviderException, IOException, NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
		if (distantID==null)
			return 1;
		int nbAno=0;
		if (acceptedIdentifiers!=null) {
			for (PairOfIdentifiers poi : acceptedIdentifiers)
				if (poi.getDistantIdentifier().equals(distantID))
					return nbAno;
		}


		Identifier localId = loginData.localiseIdentifier(distantID);

		if (localId!=null) {
			P2PLoginAgreement p2PPasswordLoginAgreementForCloudIdentifier=null, p2PASymmetricLoginAgreementForCloudIdentifier=null;
			P2PLoginAgreement p2PPasswordLoginAgreementForHostIdentifier=null, p2PASymmetricLoginAgreementForHostIdentifier=null;
			if (checkIdentifiers(localId, distantID, loginData)) {
				if (distantID.getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey()) {
					ASymmetricPublicKey pubKey;
					if ((pubKey = distantID.getCloudIdentifier().getAuthenticationPublicKey()).getAuthentifiedSignatureAlgorithmType() != null) {
						p2PASymmetricLoginAgreementForCloudIdentifier = aSymmetricLoginAgreementType.getAgreementAlgorithmForASymmetricSignatureReceiver(random, pubKey);
					} else {
						++nbAno;
						return nbAno;
					}
				}
				if (distantID.getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey()) {
					ASymmetricPublicKey pubKey;
					if ((pubKey = distantID.getHostIdentifier().getAuthenticationPublicKey()).getAuthentifiedSignatureAlgorithmType() != null) {
						p2PASymmetricLoginAgreementForHostIdentifier = aSymmetricLoginAgreementType.getAgreementAlgorithmForASymmetricSignatureReceiver(random, pubKey);
					} else {
						++nbAno;
						return nbAno;
					}
				}
				if (distantID.getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPasswordOrSecretKey())
				{
					PasswordKey pw = loginData.getCloudPassword(localId);
					if (pw != null) {

						p2PPasswordLoginAgreementForCloudIdentifier = agreementType.getAgreementAlgorithm(random, localId.toBytes(), pw.getPasswordBytes(), pw.isKey(), pw.getSecretKeyForSignature(), messageDigestType, passwordHashType, myPublicKey);
					} else{
						++nbAno;
						return nbAno;
					}
				}
				if (distantID.getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPasswordOrSecretKey())
				{
					PasswordKey pw = loginData.getHostPassword(localId);
					if (pw != null) {
						p2PPasswordLoginAgreementForHostIdentifier = agreementType.getAgreementAlgorithm(random, localId.toBytes(), pw.getPasswordBytes(), pw.isKey(), pw.getSecretKeyForSignature(), messageDigestType, passwordHashType, myPublicKey);
					} else{
						++nbAno;
						return nbAno;
					}
				}
				if (p2PASymmetricLoginAgreementForCloudIdentifier!=null || p2PASymmetricLoginAgreementForHostIdentifier!=null)
					agreements.put(distantID, new AutoIdentificationCredentials(p2PASymmetricLoginAgreementForCloudIdentifier, p2PASymmetricLoginAgreementForHostIdentifier));
				if (p2PASymmetricLoginAgreementForCloudIdentifier!=null || p2PASymmetricLoginAgreementForHostIdentifier!=null)
					agreements.put(localId, new AutoIdentificationCredentials(p2PPasswordLoginAgreementForCloudIdentifier, p2PPasswordLoginAgreementForHostIdentifier));
				if (newIdentifiersToAdd != null && !localId.getHostIdentifier().equals(HostIdentifier.getNullHostIdentifierSingleton())) {
					boolean found = false;
					if (acceptedIdentifiers != null) {

						for (PairOfIdentifiers poi : acceptedIdentifiers)
							if (poi.getLocalIdentifier().equals(localId) && !poi.getDistantIdentifier().getHostIdentifier().equals(HostIdentifier.getNullHostIdentifierSingleton())) {
								found = true;
								break;
							}
					}
					if (!found) {
						newIdentifiersToAdd.add(localId);
					}
				}
			}
			else
				++nbAno;
		}
		else
			++nbAno;
		return nbAno;
	}
	public JPakeMessage getJPakeMessage(List<PairOfIdentifiers> acceptedIdentifiers, LoginData loginData, Map<Identifier, AutoIdentificationCredentials> agreements, P2PLoginAgreementType agreementType, ASymmetricLoginAgreementType aSymmetricLoginAgreementType, AbstractSecureRandom random, AbstractMessageDigest messageDigest,
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
