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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.distrimind.madkit.database.KeysPairs;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.crypto.*;

import gnu.vm.jgnu.security.DigestException;
import gnu.vm.jgnu.security.InvalidAlgorithmParameterException;
import gnu.vm.jgnu.security.NoSuchAlgorithmException;
import gnu.vm.jgnu.security.NoSuchProviderException;

/**
 * Represents properties of a specific connection protocol
 * 
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.2
 *
 */
public class AccessProtocolWithP2PAgreement extends AbstractAccessProtocol {

	private final AccessProtocolWithP2PAgreementProperties access_protocol_properties;
	private AccessState access_state = AccessState.ACCESS_NOT_INITIALIZED;
	private Map<Identifier, P2PLoginAgreement> jpakes;
	
	private final AbstractMessageDigest messageDigest;
	private byte[] localGeneratedSalt=null, distantGeneratedSalt=null;
	private short step;
	private int maxSteps;

	private IdentifiersPropositionMessage suspendedTransaction=null;
	private JPakeMessage suspendedJpakeMessage=null;
	private Map<Identifier, P2PLoginAgreement> suspendedJpakes=null;
	private short suspendedStep;
	private int suspendMaxSteps;
	private ASymmetricKeyPair myKeyPair = null;

	private void initKeyPair() throws NoSuchAlgorithmException, DatabaseException, NoSuchProviderException, InvalidAlgorithmParameterException {
		if (myKeyPair == null || myKeyPair.getTimeExpirationUTC()<System.currentTimeMillis()) {
			if (properties.getDatabaseWrapper() == null)
				myKeyPair = access_protocol_properties.aSymetricEncryptionType
						.getKeyPairGenerator(properties.getApprovedSecureRandomForKeys(), access_protocol_properties.aSymetricKeySize).generateKeyPair();
			else
				myKeyPair = (((KeysPairs) properties.getDatabaseWrapper().getTableInstance(KeysPairs.class)).getKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.accessProtocolDatabaseUsingCode,
						access_protocol_properties.aSymetricEncryptionType, access_protocol_properties.aSymetricKeySize,
						properties.getApprovedSecureRandomForKeys(), access_protocol_properties.aSymmetricKeyExpirationMs,
						properties.networkProperties.maximumNumberOfCryptoKeysForIpsSpectrum));
		}
	}

	/*private void initNewKeyPair() throws NoSuchAlgorithmException, DatabaseException, NoSuchProviderException, InvalidAlgorithmParameterException {
		if (properties.getDatabaseWrapper() == null)
			myKeyPair = access_protocol_properties.aSymetricEncryptionType
					.getKeyPairGenerator(properties.getApprovedSecureRandomForKeys(), access_protocol_properties.aSymetricKeySize).generateKeyPair();
		else
			myKeyPair = (((KeysPairs) properties.getDatabaseWrapper().getTableInstance(KeysPairs.class)).getNewKeyPair(
					distant_inet_address.getAddress(), NetworkProperties.accessProtocolDatabaseUsingCode,
					access_protocol_properties.aSymetricEncryptionType, access_protocol_properties.aSymetricKeySize,
					properties.getApprovedSecureRandomForKeys(), access_protocol_properties.aSymmetricKeyExpirationMs,
					properties.networkProperties.maximumNumberOfCryptoKeysForIpsSpectrum));
	}*/


	public AccessProtocolWithP2PAgreement(InetSocketAddress _distant_inet_address,
										  InetSocketAddress _local_interface_address, LoginEventsTrigger loginTrigger, MadkitProperties _properties)
			throws AccessException {
		super(_distant_inet_address, _local_interface_address, loginTrigger, _properties);

		access_protocol_properties = (AccessProtocolWithP2PAgreementProperties)_properties.networkProperties.getAccessProtocolProperties(_distant_inet_address,_local_interface_address);
		if (access_protocol_properties == null)
			throw new NullPointerException("No AccessProtocolProperties was found into the MadkitProperties !");
		
		try
		{
			
			messageDigest=access_protocol_properties.identifierDigestionTypeUsedForAnonymization.getMessageDigestInstance();
		}
		catch(NoSuchProviderException | NoSuchAlgorithmException e)
		{
			throw new AccessException(e);
		}
		
		
	}
	
	private enum AccessState {
		ACCESS_NOT_INITIALIZED, ACCESS_INITIALIZED, WAITING_FOR_IDENTIFIERS, WAITING_FOR_PASSWORD_VALIDATION, WAITING_FOR_LOGIN_CONFIRMATION, ACCESS_FINALIZED, WAITING_FOR_NEW_LOGIN, WAITING_FOR_NEW_LOGIN_CONFIRMATION,
	}

	@Override
	protected void reset() throws AccessException
	{
		super.reset();
		jpakes=new HashMap<>();
		setAcceptedIdentifiers(null);
		setDeniedIdentifiers(null);

		if (access_protocol_properties.p2pLoginAgreementType== P2PLoginAgreementType.ASYMMETRIC_SECRET_MESSAGE_EXCHANGER
				||
				access_protocol_properties.p2pLoginAgreementType== P2PLoginAgreementType.ASYMMETRIC_SECRET_MESSAGE_EXCHANGER_AND_AGREEMENT_WITH_SYMMETRIC_SIGNATURE) {
			try {
				initKeyPair();
			} catch (NoSuchAlgorithmException | DatabaseException | NoSuchProviderException | InvalidAlgorithmParameterException e) {
				e.printStackTrace();
			}
		}
	}
	@Override
	public AccessMessage subSetAndGetNextMessage(AccessMessage _m) throws AccessException {
		try {
			if (_m instanceof AccessErrorMessage) {
				access_state = AccessState.ACCESS_NOT_INITIALIZED;
				return null;
			}

			switch (access_state) {
			case ACCESS_NOT_INITIALIZED: {
				reset();
				if (_m instanceof AccessAskInitiliazation) {
					if (access_data instanceof LoginData) {
						access_state = AccessState.ACCESS_INITIALIZED;
						JPakeAccessInitialized res=new JPakeAccessInitialized(((LoginData)access_data).canTakesLoginInitiative(), properties.getApprovedSecureRandomForKeys());
						localGeneratedSalt=res.getGeneratedSalt();
						return res;
						
					} else {
						access_state = AccessState.ACCESS_INITIALIZED;
						JPakeAccessInitialized res=new JPakeAccessInitialized(false, properties.getApprovedSecureRandomForKeys());
						localGeneratedSalt=res.getGeneratedSalt();
						return res;
					}
				} else
					return new AccessErrorMessage(true);

			}
			
			case ACCESS_INITIALIZED: {
				if (_m instanceof JPakeAccessInitialized) {
					JPakeAccessInitialized m=((JPakeAccessInitialized) _m);
					setOtherCanTakesInitiative( m.can_takes_login_initiative);
					distantGeneratedSalt=m.getGeneratedSalt();
					if (access_data instanceof LoginData) {
						LoginData lp = (LoginData) access_data;

						
						if (lp.canTakesLoginInitiative())
							setIdentifiers(lp.getIdentifiersToInitiate());
						else
							setIdentifiers(null);
						/*if (getIdentifiers() != null && getIdentifiers().size() == 0)
							setIdentifiers(null);*/
						if (getIdentifiers() != null) {
							access_state = AccessState.WAITING_FOR_IDENTIFIERS;
							return new IdentifiersPropositionMessage(getIdentifiers(), properties.getApprovedSecureRandom(), messageDigest,
									this.access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer,
									(short) 0, distantGeneratedSalt, jpakes, access_protocol_properties.asymmetricLoginAgreementType);
						} else {
							if (!isOtherCanTakesInitiative()) {
								access_state = AccessState.ACCESS_NOT_INITIALIZED;
								return new AccessAbordedMessage();
							} else {
								access_state = AccessState.WAITING_FOR_IDENTIFIERS;
								return new NullAccessMessage();
							}
						}
					} else {
						access_state = AccessState.ACCESS_FINALIZED;
						return new AccessFinalizedMessage();
					}
				} else {
					access_state = AccessState.ACCESS_NOT_INITIALIZED;
					return new AccessErrorMessage(false);
				}
			}
			case WAITING_FOR_IDENTIFIERS: {
				if (_m instanceof IdentifiersPropositionMessage) {
					if (access_data instanceof LoginData) {
						step=1;
						LoginData lp = (LoginData) access_data;
						
						if (getIdentifiers() != null) {
							access_state = AccessState.WAITING_FOR_PASSWORD_VALIDATION;
							JPakeMessage res=((IdentifiersPropositionMessage) _m).getJPakeMessage(getAllAcceptedIdentifiers(), lp, jpakes,access_protocol_properties.p2pLoginAgreementType, access_protocol_properties.asymmetricLoginAgreementType, properties.getApprovedSecureRandom(), messageDigest,
									this.access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer, distantGeneratedSalt, localGeneratedSalt, null, access_protocol_properties.identifierDigestionTypeUsedForAnonymization, access_protocol_properties.passwordHashType, myKeyPair==null?null:myKeyPair.getASymmetricPublicKey());
							maxSteps=res.getMaxSteps();
							return res;
						} else {
							access_state = AccessState.WAITING_FOR_PASSWORD_VALIDATION;
							setIdentifiers(new ArrayList<Identifier>());
							IdentifiersPropositionMessage propRep=((IdentifiersPropositionMessage) _m).getIdentifiersPropositionMessageAnswer(lp, properties.getApprovedSecureRandom(), messageDigest,
									this.access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer, getIdentifiers(), distantGeneratedSalt,  localGeneratedSalt, jpakes, access_protocol_properties.asymmetricLoginAgreementType);
							JPakeMessage res=((IdentifiersPropositionMessage) _m).getJPakeMessage(getAllAcceptedIdentifiers(), lp, jpakes,access_protocol_properties.p2pLoginAgreementType, access_protocol_properties.asymmetricLoginAgreementType, properties.getApprovedSecureRandom(), messageDigest,
									this.access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer, distantGeneratedSalt, localGeneratedSalt, null, access_protocol_properties.identifierDigestionTypeUsedForAnonymization, access_protocol_properties.passwordHashType, myKeyPair==null?null:myKeyPair.getASymmetricPublicKey());
							maxSteps=res.getMaxSteps();

							return new AccessMessagesList(propRep,
									res);

						}
					} else
						return new AccessErrorMessage(true);
				} else if (_m instanceof NullAccessMessage) {
					return new DoNotSendMessage();
				} else {
					access_state = AccessState.ACCESS_NOT_INITIALIZED;
					return new AccessErrorMessage(false);
				}
			}
			
			case WAITING_FOR_PASSWORD_VALIDATION:{
				if (_m instanceof JPakeMessage) {
					LoginData lp = (LoginData) access_data;
					JPakeMessage jpakem = (JPakeMessage) _m;
					if (jpakem.getStep()!=step)
					{
						access_state = AccessState.ACCESS_NOT_INITIALIZED;
						return new AccessErrorMessage(false);
					}
					if (step==1)
					{

						setAcceptedIdentifiers(new ArrayList<PairOfIdentifiers>());
						setDeniedIdentifiers(new ArrayList<PairOfIdentifiers>());
					}
					if (step<maxSteps)
					{
						AccessMessage res=jpakem.getJPakeMessageNewStep(++step, lp, properties.getApprovedSecureRandom(), messageDigest, getDeniedIdentifiers(), jpakes, distantGeneratedSalt, localGeneratedSalt);
						if (res instanceof AccessErrorMessage)
						{
							access_state = AccessState.ACCESS_NOT_INITIALIZED;
						}
						return res;
					}
					else if (step==maxSteps)
					{
						AccessMessage res=jpakem.receiveLastMessage(lp, messageDigest, getAcceptedIdentifiers(), getDeniedIdentifiers(), jpakes, localGeneratedSalt);
						if (res instanceof AccessErrorMessage)
						{
							access_state = AccessState.ACCESS_NOT_INITIALIZED;
						}
						else
						{
							access_state = AccessState.WAITING_FOR_LOGIN_CONFIRMATION;
						}
						return res;
					}
					else
					{
						access_state = AccessState.ACCESS_NOT_INITIALIZED;
						return new AccessErrorMessage(false);
					}
					
					//access_state = AccessState.WAITING_FOR_PASSWORD_VALIDATION_2;
				} else {
					access_state = AccessState.ACCESS_NOT_INITIALIZED;
					return new AccessErrorMessage(false);
				}
			}
			case WAITING_FOR_LOGIN_CONFIRMATION: {
				if (_m instanceof LoginConfirmationMessage && access_data instanceof LoginData) {
					ArrayList<PairOfIdentifiers> ai = new ArrayList<>();
					ArrayList<PairOfIdentifiers> denied_identiers = new ArrayList<>();

					for (PairOfIdentifiers id : getAcceptedIdentifiers()) {
						PairOfIdentifiers found_id = null;
						for (Identifier id2 : ((LoginConfirmationMessage) _m).accepted_identifiers) {
							if (id.getLocalIdentifier().equalsCloudIdentifier(id2)) {
								found_id = id;
								break;
							}
						}
						if (found_id != null)
							ai.add(found_id);
						else {
							denied_identiers.add(id);
						}
					}
					setAcceptedIdentifiers(ai);

					//setDistantKernelAddress(((LoginConfirmationMessage) _m).kernel_address);
					addLastAcceptedAndDeniedIdentifiers(getAcceptedIdentifiers(), denied_identiers);

					access_state = AccessState.ACCESS_FINALIZED;
					setAcceptedIdentifiers(null);
					setDeniedIdentifiers(null);
					setIdentifiers(null);
					jpakes.clear();
					return new AccessFinalizedMessage();
				} else {
					access_state = AccessState.ACCESS_NOT_INITIALIZED;
					return new AccessErrorMessage(false);
				}

			}
			case ACCESS_FINALIZED: {
				if (_m instanceof IdentifiersPropositionMessage && access_data instanceof LoginData) {
					LoginData lp = (LoginData) access_data;
					IdentifiersPropositionMessage ipm=((IdentifiersPropositionMessage) _m);
					ArrayList<Identifier> newIdentifiersToAdd=new ArrayList<>();

					/*IdentifiersPropositionMessage propRep=ipm.getIdentifiersPropositionMessageAnswer(lp, properties.getApprovedSecureRandom(), messageDigest,
							this.access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer, getIdentifiers(), distantGeneratedSalt,  localGeneratedSalt, jpakes, access_protocol_properties.asymmetricLoginAgreementType);*/

					JPakeMessage res=ipm.getJPakeMessage(getAllAcceptedIdentifiers(), lp, jpakes, access_protocol_properties.p2pLoginAgreementType,access_protocol_properties.asymmetricLoginAgreementType, properties.getApprovedSecureRandom(), messageDigest, this.access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer, distantGeneratedSalt, localGeneratedSalt, newIdentifiersToAdd, access_protocol_properties.identifierDigestionTypeUsedForAnonymization, access_protocol_properties.passwordHashType, myKeyPair==null?null:myKeyPair.getASymmetricPublicKey());
					maxSteps=res.getMaxSteps();

					step=1;
					access_state = AccessState.WAITING_FOR_NEW_LOGIN;
					if (newIdentifiersToAdd.size()>0) {
						differrAccessMessage(new NewLocalLoginAddedMessage(newIdentifiersToAdd, true));
					}
					return res;

				} else if (_m instanceof AccessFinalizedMessage) {
					updateGroupAccess();
					return null;
				} else if (_m instanceof UnlogMessage) {
					removeAcceptedIdentifiers(((UnlogMessage) _m).identifier_to_unlog);
					updateGroupAccess();
					return null;
				} else {
					return manageDifferableAccessMessage(_m);
				}
			}


			case WAITING_FOR_NEW_LOGIN:{
				if (_m instanceof IdentifiersPropositionMessage && access_data instanceof LoginData)
				{
					suspendedTransaction=(IdentifiersPropositionMessage)_m;

					if (this.isThisAskForConnection())
					{
						return null;
					}
					else
					{
						suspendedJpakes=jpakes;
						jpakes=new HashMap<>();
						suspendedStep=step;
						step=1;
						suspendMaxSteps=maxSteps;
						access_state = AccessState.ACCESS_FINALIZED;
						return this.subSetAndGetNextMessage(_m);
					}
				}
				if (_m instanceof JPakeMessage) {

					JPakeMessage jpakem = (JPakeMessage) _m;
					if (suspendedTransaction!=null && suspendedJpakeMessage==null && this.isThisAskForConnection()) {
						suspendedJpakeMessage = jpakem;
						return null;
					}
					LoginData lp = (LoginData) access_data;
					if (jpakem.getStep()!=step)
					{
						access_state = AccessState.ACCESS_NOT_INITIALIZED;
						return new AccessErrorMessage(false);
					}
					if (step==1)
					{
						setAcceptedIdentifiers(new ArrayList<PairOfIdentifiers>());
						setDeniedIdentifiers(new ArrayList<PairOfIdentifiers>());
					}
					if (step<maxSteps)
					{
						AccessMessage res=jpakem.getJPakeMessageNewStep(++step, lp, properties.getApprovedSecureRandom(), messageDigest, getDeniedIdentifiers(), jpakes, distantGeneratedSalt, localGeneratedSalt);
						if (res instanceof AccessErrorMessage)
						{
							access_state = AccessState.ACCESS_NOT_INITIALIZED;
						}
						return res;
					}
					else if (step==maxSteps)
					{
						AccessMessage res=jpakem.receiveLastMessage(lp, messageDigest, getAcceptedIdentifiers(), getDeniedIdentifiers(), jpakes, localGeneratedSalt);
						if (res instanceof AccessErrorMessage)
						{
							access_state = AccessState.ACCESS_NOT_INITIALIZED;
						}
						else
						{
							access_state = AccessState.WAITING_FOR_NEW_LOGIN_CONFIRMATION;
						}
						return res;
					}
					else
					{
						access_state = AccessState.ACCESS_NOT_INITIALIZED;
						return new AccessErrorMessage(false);
					}
					
					//access_state = AccessState.WAITING_FOR_PASSWORD_VALIDATION_2;
				} else if (!differrAccessMessage(_m)) {
					setIdentifiers(null);
					setAcceptedIdentifiers(null);
					setDeniedIdentifiers(null);
					jpakes.clear();
					access_state = AccessState.ACCESS_FINALIZED;
					return manageDifferedAccessMessage();
				} else 
					return null;
				
				
			}
			
			
			case WAITING_FOR_NEW_LOGIN_CONFIRMATION: {
				if (_m instanceof LoginConfirmationMessage && access_data instanceof LoginData) {
					ArrayList<PairOfIdentifiers> ai = new ArrayList<>();
					ArrayList<PairOfIdentifiers> denied_identiers = new ArrayList<>();

					for (PairOfIdentifiers id : getAcceptedIdentifiers()) {
						PairOfIdentifiers found_id = null;
						for (Identifier id2 : ((LoginConfirmationMessage) _m).accepted_identifiers) {
							if (id.getLocalIdentifier().equalsCloudIdentifier(id2)) {
								found_id = id;
								break;
							}
						}
						if (found_id != null)
							ai.add(found_id);
						else
							denied_identiers.add(id);
					}
					setAcceptedIdentifiers(ai);

					addLastAcceptedAndDeniedIdentifiers(getAcceptedIdentifiers(), denied_identiers);

					access_state = AccessState.ACCESS_FINALIZED;
					setAcceptedIdentifiers(null);
					setDeniedIdentifiers(null);
					setIdentifiers(null);
					jpakes.clear();
					return manageDifferedAccessMessage();
				} else if (!differrAccessMessage(_m)) {
					setIdentifiers(null);
					setAcceptedIdentifiers(null);
					setDeniedIdentifiers(null);
					jpakes.clear();
					access_state = AccessState.ACCESS_FINALIZED;
					return manageDifferedAccessMessage();
				} else 
					return null;

			}
			default:
				throw new IllegalAccessError();

			}
		} catch (Exception e) {
			throw new AccessException(e);
		}
	}
	@Override
	public final boolean isAccessFinalized() {

		return isAccessFinalizedMessage() && access_state.compareTo(AccessState.ACCESS_FINALIZED) >= 0;
	}

	@Override
	protected boolean hasSuspendedAccessMessage()
	{
		return suspendedTransaction!=null;
	}

	@Override
	protected AccessMessage manageSuspendedAccessMessage() throws AccessException
	{
		if (suspendedTransaction!=null)
		{
			try
			{
				if (this.isThisAskForConnection())
				{
					return new AccessMessagesList(setAndGetNextMessage(suspendedTransaction), setAndGetNextMessage(suspendedJpakeMessage));
				}
				else
				{
					jpakes=suspendedJpakes;
					step=suspendedStep;
					maxSteps=suspendMaxSteps;
					access_state=AccessState.WAITING_FOR_NEW_LOGIN;
					return null;
				}
			}
			finally
			{
				suspendedTransaction=null;
				suspendedJpakes=null;
				suspendedJpakeMessage=null;
			}
		}
		else
			throw new InternalError();

	}

	@Override
	protected  AccessMessage manageDifferableAccessMessage(AccessMessage _m) throws AccessException {
		try
		{


			if (_m instanceof NewLocalLoginAddedMessage) {
				NewLocalLoginAddedMessage m=((NewLocalLoginAddedMessage) _m);
				if (access_data instanceof LoginData && (((LoginData) access_data).canTakesLoginInitiative() || m.isForceLoginInitiative())) {
					step=1;
					
					access_state = AccessState.WAITING_FOR_NEW_LOGIN;
					List<Identifier> identifiers = new ArrayList<>(m.identifiers);
					setIdentifiers(identifiers);
					IdentifiersPropositionMessage m1= new IdentifiersPropositionMessage(identifiers, properties.getApprovedSecureRandom(), messageDigest,
							this.access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer, (short) 0, distantGeneratedSalt, jpakes, access_protocol_properties.asymmetricLoginAgreementType);
					
					JPakeMessage m2=new JPakeMessage((LoginData)access_data, properties.getApprovedSecureRandom(), messageDigest, jpakes, access_protocol_properties.p2pLoginAgreementType, access_protocol_properties.asymmetricLoginAgreementType, access_protocol_properties.encryptIdentifiersBeforeSendingToDistantPeer, identifiers, distantGeneratedSalt, access_protocol_properties.identifierDigestionTypeUsedForAnonymization, access_protocol_properties.passwordHashType, myKeyPair==null?null:myKeyPair.getASymmetricPublicKey());
					maxSteps=m2.getMaxSteps();
					return new AccessMessagesList(m1, m2);
				} else {
					return null;
				}
			} else if (_m instanceof NewLocalLoginRemovedMessage) {
				NewLocalLoginRemovedMessage nlrm = (NewLocalLoginRemovedMessage) _m;
				UnlogMessage um = removeAcceptedIdentifiers(nlrm.removed_identifiers);
				if (um.identifier_to_unlog == null || um.identifier_to_unlog.isEmpty())
					return null;
				else {
					updateGroupAccess();
					return um;
				}
			} else {
				access_state = AccessState.ACCESS_NOT_INITIALIZED;
				return new AccessErrorMessage(false);
			}
		}
		catch(Exception e)
		{
			throw new AccessException(e);
		}
	}

	
	static byte[] anonimizeIdentifier(byte[] identifier, AbstractSecureRandom random, AbstractMessageDigest messageDigest, byte[] distantGeneratedSalt) throws DigestException
	{
		if (random==null)
			throw new NullPointerException();
		if (messageDigest==null)
			throw new NullPointerException();
		
		int mds=messageDigest.getDigestLength();
		byte[] ivParameter=new byte[mds];
		random.nextBytes(ivParameter);
		return anonimizeIdentifier(identifier, ivParameter, messageDigest, distantGeneratedSalt);
	}
	
	private static byte[] anonimizeIdentifier(byte[] identifier, byte[] ivParameter, AbstractMessageDigest messageDigest, byte[] generatedSalt) throws DigestException
	{
		if (identifier==null)
			throw new NullPointerException();
		if (identifier.length==0)
			throw new IllegalArgumentException();
		if (messageDigest==null)
			throw new NullPointerException();
		if (generatedSalt==null)
			throw new NullPointerException();
		if (generatedSalt.length==0)
			throw new IllegalArgumentException();
		
		byte[] res=new byte[identifier.length+generatedSalt.length];
		System.arraycopy(generatedSalt, 0, res, 0, generatedSalt.length);
		System.arraycopy(identifier, 0, res, generatedSalt.length, identifier.length);
		identifier=res;
		
		final int mds=messageDigest.getDigestLength();
		if (ivParameter.length<mds)
			throw new IllegalArgumentException("Invalid IvParameter size");
		int index=0;
		res=new byte[(identifier.length/mds+(identifier.length%mds>0?1:0))*mds+mds];
		System.arraycopy(ivParameter, 0, res, 0, mds);
		do
		{
			messageDigest.reset();
			//ivParameter xor identifier
			int s=Math.min(mds+index, identifier.length);
			for (int i=index;i<s;i++)
				messageDigest.update((byte)(identifier[i]^res[i]));
			
			int s2=mds+index;
			for (int i=s;i<s2;i++)
				messageDigest.update(res[i]);
			
			messageDigest.digest(res, index+mds, mds);
			
			index+=mds;
		}while(index<identifier.length);
		return res;
	}

	static boolean compareAnonymizedIdentifier(byte[] identifier, byte[] anonymizedIdentifier, AbstractMessageDigest messageDigest, byte[] localGeneratedSalt) throws DigestException
	{
		if (anonymizedIdentifier==null || anonymizedIdentifier.length<messageDigest.getDigestLength()*2)
			return false;
		byte[] expectedAnonymizedIdentifier=anonimizeIdentifier(identifier, anonymizedIdentifier, messageDigest, localGeneratedSalt);
		return Arrays.equals(expectedAnonymizedIdentifier, anonymizedIdentifier);
	}
	
	
}
