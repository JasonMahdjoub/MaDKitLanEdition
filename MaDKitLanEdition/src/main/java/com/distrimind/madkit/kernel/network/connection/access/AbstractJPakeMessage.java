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

import com.distrimind.util.crypto.P2PLoginAgreement;

import java.util.Map;

/**
 * 
 * @author Jason Mahdjoub
 * @version 2.1
 * @since MadkitLanEdition 1.2
 */
abstract class AbstractJPakeMessage<T> extends AccessMessage{
	/**
	 *
	 */
	public static final int MAX_JPAKE_MESSAGE_LENGTH=16392;

	protected T[] identifiers;
	protected byte[][] jpakeMessages;
	protected short step;
	protected final transient short nbAnomalies;

	@SuppressWarnings("unused")
	AbstractJPakeMessage()
	{
		nbAnomalies=0;
	}



	AbstractJPakeMessage(short step, T[] identifiers, Map<T, P2PLoginAgreement> agreements, short nbAnomalies) throws Exception {
		super();
		if (identifiers.length<agreements.size())
			throw new IllegalArgumentException();
		this.identifiers=identifiers.clone();
		this.jpakeMessages=new byte[identifiers.length][];
		this.step = step;
		for (int i=0;i<identifiers.length;i++)
		{
			if (identifiers[i]==null)
				this.jpakeMessages[i]=null;
			else {
				P2PLoginAgreement agreement=agreements.get(identifiers[i]);
				if (agreement==null)
					this.jpakeMessages[i]=null;
				else
					this.jpakeMessages[i] =agreement.getDataToSend();
			}
		}

		this.nbAnomalies=nbAnomalies;

	}

	public int getMaxSteps()
	{
		return 5;
	}
	/*private AbstractJPakeMessage(boolean identifiersIsEncrypted, short nbAnomalies, AbstractSecureRandom random, AbstractMessageDigest messageDigest, short step, Map<Identifier, byte[]> jakeMessages, byte[] distantGeneratedSalt) throws DigestException {
		super();
		this.identifiersIsEncrypted=identifiersIsEncrypted;
		this.identifiers=new Identifier[jakeMessages.size()];
		this.jpakeMessages=new byte[jakeMessages.size()][];
		this.step = step;
		int i=0;
		for (Map.Entry<Identifier, byte[]> e : jakeMessages.entrySet())
		{
			if (identifiersIsEncrypted && !e.getKey().getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey())
				this.identifiers[i] = new EncryptedIdentifier(e.getKey(), random, messageDigest, distantGeneratedSalt);
			else
				this.identifiers[i] = e.getKey();
			jpakeMessages[i]=e.getValue();
			++i;
		}
		this.nbAnomalies=nbAnomalies;
	}

	AbstractJPakeMessage(LoginData loginData, AbstractSecureRandom random, AbstractMessageDigest messageDigest, Map<Identifier, P2PLoginAgreement> agreements, P2PLoginAgreementType agreementType, ASymmetricLoginAgreementType aSymmetricLoginAgreementType, boolean encryptIdentifiers, List<Identifier> newIdentifiers, byte[] distantGeneratedSalt, MessageDigestType messageDigestType, PasswordHashType passwordHashType, ASymmetricPublicKey myPublicKey) throws Exception
	{
		try
		{
			for (Identifier id : newIdentifiers) {
				if (id==null)
					throw new AccessException(new NullPointerException());
				Identifier localId = loginData.localiseIdentifier(id);
				if (localId==null)
					throw new AccessException(new NullPointerException());

				if (localId.getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey())
				{
					if (!loginData.acceptAutoSignedIdentifiers())
						continue;
					P2PLoginAgreement agreement=aSymmetricLoginAgreementType.getAgreementAlgorithmForASymmetricSignatureRequester(random, localId.getCloudIdentifier().getCloudKeyPair());
					agreements.put(localId, agreement);

				}
				else
				{
					PasswordKey pw = loginData.getPassword(localId);
					if (pw != null) {
						P2PLoginAgreement agreement = agreementType.getAgreementAlgorithm(random, localId.toBytes(), pw.getPasswordBytes(), pw.isKey(), pw.getSecretKeyForSignature(), messageDigestType, passwordHashType, myPublicKey);
						agreements.put(localId, agreement);
					}
					else
						throw new IllegalAccessError();
				}

			}
			this.identifiersIsEncrypted=encryptIdentifiers;
			this.identifiers=new Identifier[agreements.size()];
			this.jpakeMessages=new byte[agreements.size()][];
			this.step = 1;
			int i=0;
			for (Map.Entry<Identifier, P2PLoginAgreement> e : agreements.entrySet())
			{
				if (identifiersIsEncrypted && !e.getKey().getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey())
					this.identifiers[i] = new EncryptedIdentifier(e.getKey(), random, messageDigest, distantGeneratedSalt);
				else
					this.identifiers[i] = e.getKey();
				jpakeMessages[i]=e.getValue().getDataToSend();
				
				++i;
			}
			this.nbAnomalies=0;
		}
		catch ( NoSuchAlgorithmException | InvalidKeySpecException | NoSuchProviderException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchPaddingException | InvalidAlgorithmParameterException | DigestException | IOException e) {
			
			throw new AccessException(e);
		}
		
		
	}*/
	@Override
	public short getNbAnomalies() {
		return nbAnomalies;
	}
	
	/*public byte[][] getJpakeMessages() {
		return jpakeMessages;
	}*/
	public short getStep() {
		return step;
	}
	public T[] getIdentifiers()
	{
		return identifiers;
	}
	/*public boolean areIdentifiersEncrypted()
	{
		return identifiersIsEncrypted;
	}*/
	
	
	@Override
	public boolean checkDifferedMessages() {
		return false;
	}


	
}
