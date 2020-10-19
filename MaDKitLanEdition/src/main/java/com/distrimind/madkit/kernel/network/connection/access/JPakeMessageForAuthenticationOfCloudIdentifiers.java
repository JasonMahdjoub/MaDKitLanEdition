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
import com.distrimind.util.crypto.*;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Jason Mahdjoub
 * @version 2.1
 * @since MadkitLanEdition 1.2
 */
class JPakeMessageForAuthenticationOfCloudIdentifiers extends AbstractJPakeMessage<WrappedCloudIdentifier>{

	@SuppressWarnings("unused")
	JPakeMessageForAuthenticationOfCloudIdentifiers()
	{
		super();
	}
	private static WrappedCloudIdentifier[] getIdentifiers(Map<WrappedCloudIdentifier, P2PLoginAgreement> agreements)
	{
		WrappedCloudIdentifier[] res=new WrappedCloudIdentifier[agreements.size()];
		int i=0;
		for (WrappedCloudIdentifier id : agreements.keySet()) {
			res[i++] = id;
		}
		return res;
	}


	JPakeMessageForAuthenticationOfCloudIdentifiers(Map<WrappedCloudIdentifier, P2PLoginAgreement> agreements, short nbAnomalies) throws Exception {
		super((short)1, getIdentifiers(agreements), agreements, nbAnomalies);
	}
	JPakeMessageForAuthenticationOfCloudIdentifiers(short step, WrappedCloudIdentifier[] identifiers, Map<WrappedCloudIdentifier, P2PLoginAgreement> agreements, short nbAnomalies) throws Exception {
		super(step, identifiers, agreements, nbAnomalies);
	}

	static void addPairOfIdentifiers(LoginData loginData, WrappedCloudIdentifier distantIdentifier, Collection<CloudIdentifier> deniedIdentifiers, AbstractMessageDigest messageDigest, byte[] localGeneratedSalt, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties) throws AccessException {
		CloudIdentifier localID=loginData.getLocalVersionOfDistantCloudIdentifier(distantIdentifier, messageDigest, localGeneratedSalt,encryptionRestriction, accessProtocolProperties);
		if (localID!=null)
			deniedIdentifiers.add(localID);
	}

	public AccessMessage getJPakeMessageNewStep(JPakeMessageForAuthenticationOfCloudIdentifiers initialJPakeMessage, short newStep, LoginData loginData, AbstractMessageDigest messageDigest,
												Collection<CloudIdentifier> deniedIdentifiers,
												Map<WrappedCloudIdentifier, P2PLoginAgreement> JPakes,
												byte[] localGeneratedSalt,
												EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
			throws Exception {

		int nbAno=0;
		if (step!=newStep-1)
		{
			//nbAno+=JPakes.size();
			JPakes.clear();
			return new AccessErrorMessage(true);
		}

		if (identifiers.length!=initialJPakeMessage.identifiers.length || this.jpakeMessages.length!=initialJPakeMessage.identifiers.length)
		{
			++nbAno;
			JPakes.clear();
			return new AccessErrorMessage(true);
		}

		Map<WrappedCloudIdentifier, P2PLoginAgreement> jpkms=new HashMap<>();
		for (int i = 0; i < identifiers.length; i++) {
			WrappedCloudIdentifier id = identifiers[i];
			if (id==null)
				continue;

			P2PLoginAgreement jpake=JPakes.get(id);
			if (jpake==null) {

				addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
			}
			else
			{
				try
				{
					if (!jpake.hasFinishedReception())
					{
						if (jpakeMessages[i]==null)
						{
							addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
							JPakes.remove(id);
							++nbAno;
						}
						else {
							jpake.receiveData(this.jpakeMessages[i]);
						}
					}
					else if (this.jpakeMessages[i]!=null)
					{
						addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
						JPakes.remove(id);
						++nbAno;
					}
					if (!jpake.hasFinishedSend())
					{
						jpkms.put(id, jpake);
					}
					else {
						jpkms.put(id, null);
					}
				}
				catch(Exception ignored)
				{
					//ignored.printStackTrace();
					addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
					JPakes.remove(id);
					++nbAno;
				}
			}

		}

		return new JPakeMessageForAuthenticationOfCloudIdentifiers(++step, initialJPakeMessage.identifiers, jpkms, nbAno > Short.MAX_VALUE ? Short.MAX_VALUE : (short)nbAno);
	}

	public AccessMessage receiveLastMessage(JPakeMessageForAuthenticationOfCloudIdentifiers initialJPakeMessage,
											LoginData loginData,
											AbstractMessageDigest messageDigest,
											Collection<CloudIdentifier> initializedIdentifiers,
											Collection<CloudIdentifier> newAcceptedDistantCloudIdentifiers,
											Collection<CloudIdentifier> deniedIdentifiers,
											//Collection<CloudIdentifier> acceptedAutoSignedCloudIdentifiers,
											Map<WrappedCloudIdentifier, CloudIdentifier> temporaryAcceptedCloudIdentifiers,
											Map<WrappedCloudIdentifier, P2PLoginAgreement> jpakes,
											byte[] localGeneratedSalt,
											byte[] distantGeneratedSalt,
											AbstractSecureRandom random,
											EncryptionRestriction encryptionRestriction,
											AbstractAccessProtocolProperties accessProtocolProperties)
			throws Exception {


		int nbAno=0;
		
		/*if (step!=maxSteps)
		{
			nbAno+=jpakes.size();
			jpakes.clear();
			return new AccessErrorMessage(true);
		}*/
		if (identifiers.length!=initialJPakeMessage.identifiers.length || this.jpakeMessages.length!=initialJPakeMessage.identifiers.length)
		{
			++nbAno;
			jpakes.clear();
			return new AccessErrorMessage(true);
		}

		for (int i = 0; i < identifiers.length; i++) {
			WrappedCloudIdentifier id = identifiers[i];
			if (id==null)
				continue;

			P2PLoginAgreement jpake=jpakes.get(id);

			if (jpake!=null)
			{

				try
				{
					if (!jpake.hasFinishedReception())
					{
						if (jpakeMessages[i]==null)
						{
							addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
							jpakes.remove(id);
							++nbAno;
						}
						else {

							jpake.receiveData(this.jpakeMessages[i]);
						}
					}
					else if (this.jpakeMessages[i]!=null)
					{
						addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
						jpakes.remove(id);
						++nbAno;
					}
					CloudIdentifier localID=temporaryAcceptedCloudIdentifiers.get(id);

					if (localID!=null && jpake.isAgreementProcessValid())
					{
						newAcceptedDistantCloudIdentifiers.add(localID);
					}
					else {
						addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction,accessProtocolProperties);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
					jpakes.remove(id);
					++nbAno;
				}
			}
			else
			{
				addPairOfIdentifiers(loginData, id, deniedIdentifiers, messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
				++nbAno;
			}
		}

		ArrayList<Identifier> identifiers=new ArrayList<>();
		for (CloudIdentifier cloudIdentifier : newAcceptedDistantCloudIdentifiers) {
			Identifier localID = loginData.localiseIdentifier(cloudIdentifier, encryptionRestriction, accessProtocolProperties);
			if (localID != null) {
				identifiers.add(localID);
			}
			else {
				identifiers.add(new Identifier(cloudIdentifier, HostIdentifier.getNullHostIdentifierSingleton()));
				//it.remove();
			}
		}
		for (CloudIdentifier id : initializedIdentifiers)
		{
			if (id.getAuthenticationMethod()== Identifier.AuthenticationMethod.PUBLIC_KEY && !newAcceptedDistantCloudIdentifiers.contains(id))
			{
				Identifier localID=loginData.localiseIdentifier(id, encryptionRestriction, accessProtocolProperties);
				if (localID!=null)
					identifiers.add(localID);
			}
		}

		temporaryAcceptedCloudIdentifiers.clear();
		++step;
		return new IdentifiersPropositionMessage(identifiers, nbAno > Short.MAX_VALUE ? Short.MAX_VALUE:(short)nbAno, distantGeneratedSalt, random);
	}


	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		int globalSize= NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		SecureExternalizable[] r=in.readObject(false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, SecureExternalizable[].class);
		identifiers=new WrappedCloudIdentifier[r.length];
		int totalSize=0;
		for (int i=0;i<identifiers.length;i++)
		{
			if (r[i]==null)
				identifiers[i]=null;
			else if (r[i] instanceof WrappedCloudIdentifier) {
				identifiers[i] = (WrappedCloudIdentifier) r[i];
				totalSize += identifiers[i].getInternalSerializedSize();
			}
			else
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}

		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		jpakeMessages=in.read2DBytesArray(false, true, identifiers.length, MAX_JPAKE_MESSAGE_LENGTH);
		step=in.readShort();
		totalSize+= SerializationTools.getInternalSize(jpakeMessages, identifiers.length);
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		if (step<1 || step>5)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException
	{
		oos.writeObject(identifiers, false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE);
		oos.write2DBytesArray(jpakeMessages, false, true, identifiers.length, MAX_JPAKE_MESSAGE_LENGTH);
		oos.writeShort(step);
	}


}
