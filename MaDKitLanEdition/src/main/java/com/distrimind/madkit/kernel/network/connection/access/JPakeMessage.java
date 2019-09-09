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

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.*;

/**
 * 
 * @author Jason Mahdjoub
 * @version 2.1
 * @since MadkitLanEdition 1.2
 */
class JPakeMessage extends AccessMessage{
	/**
	 * 
	 */
	public static final int MAX_JPAKE_MESSAGE_LENGTH=16392;

	private boolean identifiersIsEncrypted;
	private Identifier[] identifiers;
	private byte[][] jpakeMessages;
	private short step;
	private final transient short nbAnomalies;

	@SuppressWarnings("unused")
	JPakeMessage()
	{
		nbAnomalies=0;
	}
	
	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		identifiersIsEncrypted=in.readBoolean();
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		SecureExternalizable[] tab = in.readObject(false, globalSize, SecureExternalizable[].class);
		int totalSize=1;

		identifiers=new Identifier[tab.length];
		for (int i=0;i<tab.length;i++)
		{
			if (!(tab[i] instanceof Identifier))
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			identifiers[i]=(Identifier)tab[i];
			totalSize+=tab[i].getInternalSerializedSize();
		}
		
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		jpakeMessages=in.read2DBytesArray(false, true, identifiers.length, MAX_JPAKE_MESSAGE_LENGTH);
		step=in.readShort();
		totalSize+= SerializationTools.getInternalSize(jpakeMessages, identifiers.length);
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		/*for (byte[] b : jpakeMessages)
			if (b==null)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);*/
		if (step<1 || step>5)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException
	{
		oos.writeBoolean(identifiersIsEncrypted);
		oos.writeObject(identifiers, false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE);
		oos.write2DBytesArray(jpakeMessages, false, true, identifiers.length, MAX_JPAKE_MESSAGE_LENGTH);
		oos.writeShort(step);
	}
	
	
	JPakeMessage(Map<Identifier, IdentifiersPropositionMessage.AutoIdentificationCredentials> agreements, boolean identifiersIsEncrypted, short nbAnomalies, AbstractSecureRandom random, AbstractMessageDigest messageDigest, byte[] distantGeneratedSalt) throws Exception {
		super();
		this.identifiersIsEncrypted=identifiersIsEncrypted;
		this.identifiers=new Identifier[agreements.size()];
		this.jpakeMessages=new byte[agreements.size()*2][];
		this.step = 1;
		int i=0;
		for (Map.Entry<Identifier, IdentifiersPropositionMessage.AutoIdentificationCredentials> e : agreements.entrySet())
		{
			if (identifiersIsEncrypted && !e.getKey().getCloudIdentifier().getAuthenticationMethod().isAuthenticatedByPublicKey())
				this.identifiers[i] = new EncryptedIdentifier(e.getKey(), random, messageDigest, distantGeneratedSalt);
			else
				this.identifiers[i] = e.getKey();
			jpakeMessages[i*2]=e.getValue().getP2PLoginAgreementForCloudIdentifier()!=null?e.getValue().getP2PLoginAgreementForCloudIdentifier().getDataToSend():null;
			jpakeMessages[i*2+1]=e.getValue().getP2PLoginAgreementForHostIdentifier()!=null?e.getValue().getP2PLoginAgreementForHostIdentifier().getDataToSend():null;
			++i;
			
		}
		this.nbAnomalies=nbAnomalies;
		
	}
	
	public int getMaxSteps()
	{
		return 5;
	}
	private JPakeMessage(boolean identifiersIsEncrypted, short nbAnomalies, AbstractSecureRandom random, AbstractMessageDigest messageDigest, short step, Map<Identifier, byte[]> jakeMessages, byte[] distantGeneratedSalt) throws DigestException {
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
	
	JPakeMessage(LoginData loginData, AbstractSecureRandom random, AbstractMessageDigest messageDigest, Map<Identifier, P2PLoginAgreement> agreements, P2PLoginAgreementType agreementType, ASymmetricLoginAgreementType aSymmetricLoginAgreementType, boolean encryptIdentifiers, List<Identifier> newIdentifiers, byte[] distantGeneratedSalt, MessageDigestType messageDigestType, PasswordHashType passwordHashType, ASymmetricPublicKey myPublicKey) throws Exception
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
		
		
	}
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
	public Identifier[] getIdentifiers()
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



	static void addPairOfIdentifiers(Identifier distantIdentifier, Identifier localID, Collection<PairOfIdentifiers> list)
	{
		Identifier toCompare=new Identifier(localID.getCloudIdentifier(), HostIdentifier.getNullHostIdentifierSingleton());
		if (distantIdentifier.getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey() && distantIdentifier.equalsHostIdentifier(localID))
		{
			for (PairOfIdentifiers pio : list)
				if (pio.getLocalIdentifier().equals(localID) && pio.getDistantIdentifier().equalsCloudIdentifier(localID))
					return;
			list.add(new PairOfIdentifiers(localID, toCompare));
		}
		else {

			for (Iterator<PairOfIdentifiers> it = list.iterator(); it.hasNext();) {
				PairOfIdentifiers poi=it.next();
				if (poi.getLocalIdentifier().equals(localID) && poi.getDistantIdentifier().equals(toCompare))
					it.remove();
			}

			list.add(new PairOfIdentifiers(localID, distantIdentifier));
		}
	}
	public AccessMessage getJPakeMessageNewStep(short newStep, LoginData lp, AbstractSecureRandom random, AbstractMessageDigest messageDigest, Collection<PairOfIdentifiers> deniedIdentifiers,
												Map<Identifier, P2PLoginAgreement> jpakes, byte[] distantGeneratedSalt, byte[] localGeneratedSalt)
			throws Exception {

		int nbAno=0;
		if (step!=newStep-1)
		{
			//nbAno+=jpakes.size();
			jpakes.clear();
			return new AccessErrorMessage(true);
		}

		Map<Identifier, byte[]> jpkms=new HashMap<>();
		
		for (int i = 0; i < identifiers.length; i++) {
			Identifier id = identifiers[i];
			Identifier decodedID;
			if (identifiersIsEncrypted && id instanceof EncryptedIdentifier)
				decodedID = lp.getIdentifier((EncryptedIdentifier) id, messageDigest, localGeneratedSalt);
			else
				decodedID = id;
			if (decodedID != null) {
				Identifier localID = lp.localiseIdentifier(decodedID);
				if (localID != null) {
					P2PLoginAgreement jpake;
					Identifier destID;
					if (decodedID.getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey())
						jpake=jpakes.get(destID=decodedID);
					else
						jpake=jpakes.get(destID=localID);

					if (jpake!=null)
					{

						try
						{
							if (!jpake.hasFinishedReceiption())
							{
								if (jpakeMessages[i]==null)
								{
									addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
									jpakes.remove(localID);
									if (localID!=destID) {
										jpakes.remove(destID);
									}
									++nbAno;
								}
								else {

									jpake.receiveData(this.jpakeMessages[i]);
								}
							}
							else if (this.jpakeMessages[i]!=null)
							{
								addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
								jpakes.remove(localID);
								if (localID!=destID) {
									jpakes.remove(destID);
								}
								++nbAno;
							}
							if (!jpake.hasFinishedSend())
							{
								byte[] jpakeMessage=jpake.getDataToSend();

								jpkms.put(destID, jpakeMessage);
							}
							else
								jpkms.put(destID, null);
						}
						catch(Exception e)
						{
							e.printStackTrace();
							addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
							jpakes.remove(localID);
							if (localID!=destID) {
								jpakes.remove(destID);
							}
							++nbAno;
						}
					}
					else
					{
						addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
						++nbAno;
					}
				}




			}
		}
		
		return new JPakeMessage(identifiersIsEncrypted, nbAno > Short.MAX_VALUE ? Short.MAX_VALUE : (short)nbAno, random, messageDigest, newStep, jpkms, distantGeneratedSalt);
	}
	
	public AccessMessage receiveLastMessage(LoginData lp, AbstractMessageDigest messageDigest, Collection<PairOfIdentifiers> acceptedIdentifiers, Collection<PairOfIdentifiers> deniedIdentifiers,
			Map<Identifier, P2PLoginAgreement> jpakes, byte[] localGeneratedSalt)
			throws Exception {

		
		int nbAno=0;
		
		/*if (step!=maxSteps)
		{
			nbAno+=jpakes.size();
			jpakes.clear();
			return new AccessErrorMessage(true);
		}*/

		for (int i = 0; i < identifiers.length; i++) {
			Identifier id = identifiers[i];
			Identifier decodedID;
			if (identifiersIsEncrypted  && id instanceof EncryptedIdentifier)
				decodedID = lp.getIdentifier((EncryptedIdentifier) id, messageDigest, localGeneratedSalt);
			else
				decodedID = id;
			if (decodedID != null) {
				Identifier localID = lp.localiseIdentifier(decodedID);

				if (localID != null) {

					P2PLoginAgreement jpake;
					Identifier destID;
					if (decodedID.getCloudIdentifier().isAutoIdentifiedCloudWithPublicKey())
						jpake=jpakes.get(destID=decodedID);
					else
						jpake=jpakes.get(destID=localID);

					if (jpake!=null)
					{

						try
						{
							if (!jpake.hasFinishedReceiption())
							{
								if (jpakeMessages[i]==null)
								{
									addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
									jpakes.remove(localID);
									if (localID!=destID) {
										jpakes.remove(destID);
									}
									++nbAno;
								}
								else {

									jpake.receiveData(this.jpakeMessages[i]);
								}
							}
							else if (this.jpakeMessages[i]!=null)
							{
								addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
								jpakes.remove(localID);
								if (localID!=destID) {
									jpakes.remove(destID);
								}
								++nbAno;
							}
							if (jpake.isAgreementProcessValid())
							{
								addPairOfIdentifiers(decodedID, localID, acceptedIdentifiers);
							}
							else {
								addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
							}
						}
						catch(Exception e)
						{
							e.printStackTrace();
							addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
							jpakes.remove(localID);
							if (localID!=destID) {
								jpakes.remove(destID);
							}

							++nbAno;
						}
					}
					else
					{
						addPairOfIdentifiers(decodedID, localID, deniedIdentifiers);
						++nbAno;
					}
				}
			}
		}
		return new LoginConfirmationMessage(acceptedIdentifiers, deniedIdentifiers, nbAno > Short.MAX_VALUE ? Short.MAX_VALUE : (short)nbAno, false);
	}
	
	
}
