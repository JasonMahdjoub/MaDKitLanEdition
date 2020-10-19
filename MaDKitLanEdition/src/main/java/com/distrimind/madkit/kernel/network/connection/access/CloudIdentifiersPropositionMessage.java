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

import com.distrimind.madkit.kernel.network.EncryptionRestriction;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.util.DecentralizedIDGenerator;
import com.distrimind.util.crypto.*;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.security.*;
import java.util.*;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
class CloudIdentifiersPropositionMessage extends AccessMessage {


	private WrappedCloudIdentifier[] identifiers;
	private final transient short nbAnomalies;

	@SuppressWarnings("unused")
	CloudIdentifiersPropositionMessage()
	{
		nbAnomalies=0;
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		SecureExternalizable[] r=in.readObject(false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, SecureExternalizable[].class);
		identifiers=new WrappedCloudIdentifier[r.length];
		for (int i=0;i<identifiers.length;i++)
		{
			if (r[i] instanceof WrappedCloudIdentifier)
				identifiers[i]=(WrappedCloudIdentifier)r[i];
			else
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}

	}
	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {

		oos.writeObject(identifiers, false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE);
	}

	/*WrappedCloudIdentifier[] getIdentifiers() {
		return identifiers;
	}*/

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
	/*static Collection<CloudIdentifier> getCloudIdentifierList(Collection<Identifier> _id_pws)
	{
		ArrayList<CloudIdentifier> res=new ArrayList<>(_id_pws.size());
		for (Identifier id : _id_pws)
			res.add(id.getCloudIdentifier());
		return res;
	}*/

	/*CloudIdentifiersPropositionMessage(Collection<Identifier> _id_pws, AbstractSecureRandom random, AbstractMessageDigest messageDigest,
											  boolean encryptIdentifiers, short nbAnomalies, byte[] distantGeneratedSalt, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties) throws DigestException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException, InvalidKeySpecException {
		this(random, messageDigest, encryptIdentifiers, nbAnomalies, distantGeneratedSalt, getCloudIdentifierList(_id_pws), encryptionRestriction, accessProtocolProperties);
	}*/
	CloudIdentifiersPropositionMessage(AbstractSecureRandom random, AbstractMessageDigest messageDigest,
											  boolean permitAnonymousIdentifiers, short nbAnomalies, byte[] distantGeneratedSalt,
									   Collection<CloudIdentifier> _id_pws) throws NoSuchAlgorithmException, IOException,NoSuchProviderException {
		identifiers = new WrappedCloudIdentifier[_id_pws.size()];
		int index = 0;
		for (CloudIdentifier ip : _id_pws) {
			if (ip.getAuthenticationMethod()== Identifier.AuthenticationMethod.NOT_DEFINED) {
				continue;
			}
			if (ip.getAuthenticationMethod().isAuthenticatedByPublicKey() && ip.getAuthenticationKeyPair()==null) {
				continue;
			}

			identifiers[index++]=new WrappedCloudIdentifier(permitAnonymousIdentifiers && ip.mustBeAnonymous(),ip, random, messageDigest, distantGeneratedSalt);
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
	/*private boolean checkIdentifiers(Identifier local, Identifier distant, LoginData loginData)
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
					&& !loginData.acceptAutoSignedIdentifiers())
			{
				return false;
			}
			return (!local.getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey() || local.getCloudIdentifier().getAuthenticationKeyPair() != null)
					&& (!local.getHostIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey() || local.getHostIdentifier().getAuthenticationKeyPair() != null);
		}
		else
			return false;
	}*/
	public void getValidDecodedCloudIdentifiers(LoginData loginData,
												AbstractMessageDigest messageDigest,
												byte[] localGeneratedSalt ,
												Collection<CloudIdentifier> validCloudIdentifiers,
												//Collection<CloudIdentifier> acceptedAutoSignedCloudIdentifiers,
												EncryptionRestriction encryptionRestriction,
												AbstractAccessProtocolProperties accessProtocolProperties
												) throws AccessException {

		for (WrappedCloudIdentifier id : identifiers) {
			CloudIdentifier i=loginData.getLocalVersionOfDistantCloudIdentifier(id, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
			if (i!=null) {
				/*if (i.getAuthenticationMethod().isAuthenticatedByPublicKey() && i.getAuthenticationKeyPair()==null)
					acceptedAutoSignedCloudIdentifiers.add(i);
				else*/
					validCloudIdentifiers.add(i);
			}
			else {
				validCloudIdentifiers.remove(id.getCloudIdentifier());
			}
		}

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
	public CloudIdentifiersPropositionMessage getIdentifiersPropositionMessageAnswer(LoginData loginData,
																					 AbstractSecureRandom random,
																					 AbstractMessageDigest messageDigest,
																					 boolean encryptIdentifiers,
																					 Set<CloudIdentifier> validCloudIdentifiers,
																					 //Collection<CloudIdentifier> acceptedAutoSignedCloudIdentifiers,
																					 byte[] distantGeneratedSalt,
																					 byte[] localGeneratedSalt,
																					 EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
			throws AccessException, NoSuchAlgorithmException, IOException, NoSuchProviderException {
		HashSet<CloudIdentifier> validID=new HashSet<>();
		getValidDecodedCloudIdentifiers(loginData, messageDigest, localGeneratedSalt, validID, encryptionRestriction, accessProtocolProperties);
		for (CloudIdentifier ci : validID)
		{
			if (!ci.getAuthenticationMethod().isAuthenticatedByPublicKey() || ci.getAuthenticationKeyPair()!=null)
				validCloudIdentifiers.add(ci);
		}

		int nbAno = this.identifiers.length - validID.size();
		return new CloudIdentifiersPropositionMessage(random, messageDigest, encryptIdentifiers,
				loginData.canTakesLoginInitiative()
						? ((validID.size() == 0 && this.identifiers.length > 0) ? (short) 1 : (short) 0)
						: (nbAno > Short.MAX_VALUE) ? Short.MAX_VALUE : (short) nbAno, distantGeneratedSalt, validID);
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
	private int getJakeMessageSub(WrappedCloudIdentifier distantCloudID, Collection<PairOfIdentifiers> acceptedIdentifiers,
								  List<CloudIdentifier> newAcceptedDistantCloudIdentifiers,
								  Map<WrappedCloudIdentifier, CloudIdentifier> temporaryAcceptedCloudIdentifiers, LoginData loginData,
								  Map<WrappedCloudIdentifier, P2PLoginAgreement> agreements, P2PLoginAgreementType agreementType,
								  final AbstractSecureRandom random, MessageDigestType messageDigestType,
								  PasswordHashType passwordHashType, ASymmetricPublicKey myPublicKey, byte[] localGeneratedSalt,
								  EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
			throws NoSuchAlgorithmException, NoSuchProviderException, AccessException, IOException {

		if (distantCloudID==null)
			return 1;
		if (agreements.containsKey(distantCloudID)) {
			return 1;
		}

		int nbAno=0;
		AbstractMessageDigest messageDigest=messageDigestType.getMessageDigestInstance();
		CloudIdentifier localCloudIdentifier = loginData.getLocalVersionOfDistantCloudIdentifier(distantCloudID, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);

		boolean ok=true;

		if (localCloudIdentifier!=null) {

			if (acceptedIdentifiers!=null) {
				for (PairOfIdentifiers poi : acceptedIdentifiers)
					if (poi.getCloudIdentifier().equals(localCloudIdentifier))
						return nbAno;
			}

			if (localCloudIdentifier.getAuthenticationMethod().isAuthenticatedByPasswordOrSecretKey()) {
				PasswordKey pw = loginData.getCloudPassword(localCloudIdentifier);
				if (pw != null) {
					if (accessProtocolProperties.isAcceptablePassword(encryptionRestriction, pw)) {
						P2PLoginAgreement p2PLoginAgreement = agreementType.getAgreementAlgorithm(random, new DecentralizedIDGenerator(true, true).encode(), pw.getPasswordBytes(), pw.isKey(), pw.getSecretKeyForSignature(), messageDigestType, passwordHashType, myPublicKey);
						agreements.put(distantCloudID, p2PLoginAgreement);
						temporaryAcceptedCloudIdentifiers.put(distantCloudID, localCloudIdentifier);
					}
					else {
						++nbAno;
					}
				} else {
					++nbAno;
					ok=false;
				}
			}
			else if (localCloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey())
			{
				newAcceptedDistantCloudIdentifiers.add(localCloudIdentifier);
			}
			else
			{
				++nbAno;
				ok=false;
			}
		}
		else {
			++nbAno;
		}
		if (!ok) {
			PasswordKey pw=PasswordKey.getRandomPasswordKey(random);
			agreements.put(distantCloudID,
					agreementType.getAgreementAlgorithm(random, new DecentralizedIDGenerator(true, true).encode(), pw.getPasswordBytes(), pw.isKey(), pw.getSecretKeyForSignature(), messageDigestType, passwordHashType, myPublicKey));
		}

		return nbAno;
	}
	public JPakeMessageForAuthenticationOfCloudIdentifiers getJPakeMessage(List<PairOfIdentifiers> acceptedIdentifiers,
																		   List<CloudIdentifier> newAcceptedCloudIdentifiers,
																		   Map<WrappedCloudIdentifier, CloudIdentifier> temporaryAcceptedCloudIdentifiers,
																		   LoginData loginData,
																		   Map<WrappedCloudIdentifier,
																				   P2PLoginAgreement> agreements, P2PLoginAgreementType agreementType, AbstractSecureRandom random,
										byte[] localGeneratedSalt, MessageDigestType messageDigestType, PasswordHashType passwordHashType, ASymmetricPublicKey myPublicKey,
																		   EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties) throws Exception {
		int nbAno = 0;
		for (WrappedCloudIdentifier id : identifiers) {
			nbAno+=getJakeMessageSub(id, acceptedIdentifiers, newAcceptedCloudIdentifiers,
					temporaryAcceptedCloudIdentifiers, loginData, agreements, agreementType, random, messageDigestType, passwordHashType, myPublicKey,
					localGeneratedSalt,encryptionRestriction, accessProtocolProperties);
		}
		return new JPakeMessageForAuthenticationOfCloudIdentifiers(agreements, nbAno > Short.MAX_VALUE ? Short.MAX_VALUE:(short)nbAno);
	}

	

	@Override
	public boolean checkDifferedMessages() {
		return false;
	}

}
