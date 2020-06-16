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
package com.distrimind.madkit.kernel.network.connection.secured;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.PacketCounter;
import com.distrimind.madkit.kernel.network.SubBlock;
import com.distrimind.madkit.kernel.network.SubBlockInfo;
import com.distrimind.madkit.kernel.network.SubBlockParser;
import com.distrimind.madkit.kernel.network.connection.*;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.util.Bits;
import com.distrimind.util.crypto.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Support forward secrecy
 * 
 * @author Jason Mahdjoub
 * @version 2.1
 * @since MadkitLanEdition 1.7
 */
public class P2PSecuredConnectionProtocolWithKeyAgreement extends ConnectionProtocol<P2PSecuredConnectionProtocolWithKeyAgreement> {
	
	private static final int MATERIAL_KEY_SIZE_BYTES=64;
	Step current_step = Step.NOT_CONNECTED;

	private SymmetricSecretKey secret_key_for_encryption = null;
	private SymmetricSecretKey secret_key_for_signature = null;

	private final EncryptionSignatureHashEncoder encoderWithEncryption;
	private final EncryptionSignatureHashEncoder encoderWithoutEncryption;
	private final EncryptionSignatureHashDecoder decoderWithEncryption;
	private final EncryptionSignatureHashDecoder decoderWithoutEncryption;

	//protected SymmetricEncryptionAlgorithm symmetricEncryption = null;
	protected KeyAgreement keyAgreementForEncryption=null, keyAgreementForSignature=null;
	//protected SymmetricAuthenticatedSignerAlgorithm signer = null;
	//protected SymmetricAuthenticatedSignatureCheckerAlgorithm signatureChecker = null;
	private byte[] localSalt;

	private final SubBlockParser parser;

	
	private final P2PSecuredConnectionProtocolPropertiesWithKeyAgreement hproperties;
	private final AbstractSecureRandom approvedRandom, approvedRandomForKeys;
	private boolean blockCheckerChanged = true;
	private byte[] materialKeyForSignature=null, materialKeyForEncryption=null;
	private final PacketCounterForEncryptionAndSignature packetCounter;
	private boolean reinitSymmetricAlgorithm=true;
	private boolean myCounterSent=false;
	private boolean doNotTakeIntoAccountNextState=true;
	private P2PSecuredConnectionProtocolWithKeyAgreement(InetSocketAddress _distant_inet_address,
														 InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol,
														 DatabaseWrapper sql_connection, MadkitProperties mkProperties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer,
														 boolean mustSupportBidirectionnalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, mkProperties,cpp,
				subProtocolLevel, isServer, mustSupportBidirectionnalConnectionInitiative);
		hproperties = (P2PSecuredConnectionProtocolPropertiesWithKeyAgreement) super.connection_protocol_properties;
		hproperties.checkProperties();

		
		
		try {
			approvedRandom=mkProperties.getApprovedSecureRandom();
			approvedRandomForKeys=mkProperties.getApprovedSecureRandomForKeys();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new ConnectionException(e);
		}
		
		try {
			encoderWithEncryption=new EncryptionSignatureHashEncoder();
			encoderWithoutEncryption=new EncryptionSignatureHashEncoder();
			decoderWithEncryption=new EncryptionSignatureHashDecoder();
			decoderWithoutEncryption=new EncryptionSignatureHashDecoder();
			if (hproperties.messageDigestType!=null) {
				encoderWithEncryption.withMessageDigestType(hproperties.messageDigestType);
				encoderWithoutEncryption.withMessageDigestType(hproperties.messageDigestType);
				decoderWithEncryption.withMessageDigestType(hproperties.messageDigestType);
				decoderWithoutEncryption.withMessageDigestType(hproperties.messageDigestType);
			}
			encoderWithEncryption.connectWithDecoder(decoderWithEncryption);
			encoderWithoutEncryption.connectWithDecoder(decoderWithoutEncryption);
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
		this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, hproperties.enableEncryption, true);
		
		if (hproperties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();

	}

	private void checkSymmetricSignatureAlgorithm() throws ConnectionException {
		try {
			if (secret_key_for_signature!=null) {
				if (encoderWithoutEncryption.getSymmetricSecretKeyForSignature()==null || decoderWithoutEncryption.getSymmetricSecretKeyForSignature()==null)
				{
					encoderWithoutEncryption.withSymmetricSecretKeyForSignature(secret_key_for_signature);
					encoderWithEncryption.withSymmetricSecretKeyForSignature(secret_key_for_signature);
					decoderWithoutEncryption.withSymmetricSecretKeyForSignature(secret_key_for_signature);
					decoderWithEncryption.withSymmetricSecretKeyForSignature(secret_key_for_signature);
					blockCheckerChanged=true;
				}
			} else {
				encoderWithEncryption.withoutSymmetricSignature();
				encoderWithoutEncryption.withoutSymmetricSignature();
				decoderWithEncryption.withoutSymmetricSignature();
				decoderWithoutEncryption.withoutSymmetricSignature();
				encoderWithEncryption.withoutSymmetricEncryption();
				decoderWithEncryption.withoutSymmetricEncryption();
			}
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
	}
	private void checkSymmetricEncryptionAlgorithm() throws ConnectionException {
		try {
			if (secret_key_for_encryption != null) {
				if (encoderWithEncryption.getSymmetricSecretKeyForEncryption() == null) {
					encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, secret_key_for_encryption);
					decoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, secret_key_for_encryption);
				}
			} else {
				encoderWithEncryption.withoutSymmetricEncryption();
				decoderWithEncryption.withoutSymmetricEncryption();
			}
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
	}

	private void reset() {
		blockCheckerChanged = true;
		encoderWithEncryption.withoutSymmetricEncryption();
		decoderWithEncryption.withoutSymmetricEncryption();
		secret_key_for_encryption = null;
		secret_key_for_signature=null;
		keyAgreementForEncryption=null;
		keyAgreementForSignature=null;
		encoderWithEncryption.withoutSymmetricSignature();
		encoderWithoutEncryption.withoutSymmetricSignature();
		decoderWithEncryption.withoutSymmetricSignature();
		decoderWithoutEncryption.withoutSymmetricSignature();
		this.localSalt=null;

	}

	private enum Step {
		NOT_CONNECTED, WAITING_FOR_SIGNATURE_DATA, WAITING_FOR_ENCRYPTION_DATA, WAITING_FOR_SERVER_PROFILE_MESSAGE, WAITING_FOR_SERVER_SIGNATURE, WAITING_FOR_CONNECTION_CONFIRMATION, CONNECTED,
	}

	private void initKeyAgreementAlgorithm() throws NoSuchAlgorithmException, NoSuchProviderException, IOException {
		if (hproperties.enableEncryption && materialKeyForEncryption==null)
			throw new InternalError();
		if (materialKeyForSignature==null)
			throw new InternalError();
		if (this.isCurrentServerAskingConnection())
		{
			if (hproperties.enableEncryption) {

				if (hproperties.postQuantumKeyAgreement!=null) {
					HybridKeyAgreementType t=new HybridKeyAgreementType(hproperties.keyAgreementType, hproperties.postQuantumKeyAgreement);
					this.keyAgreementForEncryption = t.getKeyAgreementServer(this.approvedRandomForKeys, hproperties.symmetricEncryptionType, hproperties.symmetricKeySizeBits, materialKeyForEncryption);
				}
				else
					this.keyAgreementForEncryption = hproperties.keyAgreementType.getKeyAgreementServer(this.approvedRandomForKeys, hproperties.symmetricEncryptionType, hproperties.symmetricKeySizeBits, materialKeyForEncryption);
			}
			else
				this.keyAgreementForEncryption=null;
			if (hproperties.postQuantumKeyAgreement!=null) {
				HybridKeyAgreementType t=new HybridKeyAgreementType(hproperties.keyAgreementType, hproperties.postQuantumKeyAgreement);
				this.keyAgreementForSignature=t.getKeyAgreementServer(this.approvedRandomForKeys, hproperties.symmetricSignatureType, hproperties.symmetricKeySizeBits, materialKeyForSignature);
			}
			else
				this.keyAgreementForSignature=hproperties.keyAgreementType.getKeyAgreementServer(this.approvedRandomForKeys, hproperties.symmetricSignatureType, hproperties.symmetricKeySizeBits, materialKeyForSignature);
		}
		else
		{
			if (hproperties.enableEncryption) {
				if (hproperties.postQuantumKeyAgreement!=null) {
					HybridKeyAgreementType t=new HybridKeyAgreementType(hproperties.keyAgreementType, hproperties.postQuantumKeyAgreement);
					this.keyAgreementForEncryption = t.getKeyAgreementClient(this.approvedRandomForKeys, hproperties.symmetricEncryptionType, hproperties.symmetricKeySizeBits, materialKeyForEncryption);
				}
				else
					this.keyAgreementForEncryption = hproperties.keyAgreementType.getKeyAgreementClient(this.approvedRandomForKeys, hproperties.symmetricEncryptionType, hproperties.symmetricKeySizeBits, materialKeyForEncryption);
			}
			else
				this.keyAgreementForEncryption=null;
			if (hproperties.postQuantumKeyAgreement!=null) {
				HybridKeyAgreementType t=new HybridKeyAgreementType(hproperties.keyAgreementType, hproperties.postQuantumKeyAgreement);
				this.keyAgreementForSignature=t.getKeyAgreementClient(this.approvedRandomForKeys, hproperties.symmetricSignatureType, hproperties.symmetricKeySizeBits, materialKeyForSignature);
			}
			else
				this.keyAgreementForSignature=hproperties.keyAgreementType.getKeyAgreementClient(this.approvedRandomForKeys, hproperties.symmetricSignatureType, hproperties.symmetricKeySizeBits, materialKeyForSignature);
		}

	}
	
	private void reinitSymmetricAlgorithmIfNecessary() throws IOException {
		if (reinitSymmetricAlgorithm)
		{
			reinitSymmetricAlgorithm=false;
			encoderWithEncryption.withSymmetricSecretKeyForEncryption(this.approvedRandom, this.secret_key_for_encryption, (byte)packetCounter.getMyEncryptionCounter().length);
			decoderWithEncryption.withSymmetricSecretKeyForEncryption(this.approvedRandom, this.secret_key_for_encryption, (byte)packetCounter.getMyEncryptionCounter().length);
		}
	}

	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) throws ConnectionException {
		switch (current_step) {
		case NOT_CONNECTED: {
			if (_m instanceof AskConnection) {
				AskConnection ask = (AskConnection) _m;
				
				current_step = Step.WAITING_FOR_SIGNATURE_DATA;
				if (ask.isYouAreAsking()) {
					return new AskConnection(false);
				} else {
					try {
						
						materialKeyForSignature=new byte[MATERIAL_KEY_SIZE_BYTES];
						approvedRandom.nextBytes(materialKeyForSignature);
						
						byte [] material;
						if (hproperties.enableEncryption)
						{
							materialKeyForEncryption=new byte[MATERIAL_KEY_SIZE_BYTES];
							approvedRandom.nextBytes(materialKeyForEncryption);
							material=Bits.concatenateEncodingWithShortSizedTabs(materialKeyForSignature, materialKeyForEncryption);
						}
						else
							material=materialKeyForSignature;
						initKeyAgreementAlgorithm();
						return new KeyAgreementDataMessage(keyAgreementForSignature.getDataToSend(), material);
						
					} catch (IOException | NoSuchAlgorithmException | NoSuchProviderException e) {
						throw new ConnectionException(e);
					}
				}
			} else if (_m instanceof ConnectionFinished) {
				if (((ConnectionFinished) _m).getState()
						.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				} else
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
			} else {
				reset();
				current_step=Step.NOT_CONNECTED;
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_SIGNATURE_DATA:{
			if (_m instanceof KeyAgreementDataMessage)
			{
				
					
				try
				{
					KeyAgreementDataMessage kadm=(KeyAgreementDataMessage)_m;
					if (isCurrentServerAskingConnection())
					{
						
						
						try
						{
							byte[] material=kadm.getMaterialKey();
							if (hproperties.enableEncryption)
							{
								if (material==null || material.length!=MATERIAL_KEY_SIZE_BYTES*2+2)
									return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
								byte[][] t=Bits.separateEncodingsWithShortSizedTabs(material);
								materialKeyForSignature=t[0];
								materialKeyForEncryption=t[1];
								if (materialKeyForEncryption==null || materialKeyForEncryption.length!=MATERIAL_KEY_SIZE_BYTES
										|| materialKeyForSignature==null || materialKeyForSignature.length!=MATERIAL_KEY_SIZE_BYTES)
									return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
							}
							else
							{
								if (material==null || material.length!=MATERIAL_KEY_SIZE_BYTES)
									return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
								materialKeyForSignature=material;
								
								if (materialKeyForSignature.length != MATERIAL_KEY_SIZE_BYTES)
									return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
							}
							initKeyAgreementAlgorithm();
						}
						catch(Exception e)
						{
							reset();
							current_step=Step.NOT_CONNECTED;
							return new IncomprehensiblePublicKey();
						}
					}
					if (keyAgreementForSignature.hasFinishedReception())
					{
						reset();
						current_step=Step.NOT_CONNECTED;
						return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
					}
					keyAgreementForSignature.receiveData(kadm.getData());
					byte[] data = null;
					if (!keyAgreementForSignature.hasFinishedSend())
						data=keyAgreementForSignature.getDataToSend();
					doNotTakeIntoAccountNextState=false;
					if (keyAgreementForSignature.hasFinishedReception())
					{
						doNotTakeIntoAccountNextState=true;
						if (hproperties.enableEncryption)
							current_step=Step.WAITING_FOR_ENCRYPTION_DATA;
						else
							current_step=Step.WAITING_FOR_SERVER_PROFILE_MESSAGE;
						secret_key_for_signature=keyAgreementForSignature.getDerivedKey();
						checkSymmetricSignatureAlgorithm();
					}
						
					
					if (data!=null)
					{
						KeyAgreementDataMessage ka=new KeyAgreementDataMessage(data, null);
						if (current_step==Step.WAITING_FOR_SERVER_PROFILE_MESSAGE)
							return new DoubleConnectionMessage(ka, getServerProfileMessage());
						else
							return ka;
					}
					else
					{
						doNotTakeIntoAccountNextState=false;

						if (hproperties.enableEncryption)
						{
							current_step=Step.WAITING_FOR_ENCRYPTION_DATA;
							data=keyAgreementForEncryption.getDataToSend();
							return new KeyAgreementDataMessage(data, null);
						}
						else
						{
							myCounterSent=true;
							/*current_step=Step.WAITING_FOR_CONNECTION_CONFIRMATION;
							return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());*/
							current_step=Step.WAITING_FOR_SERVER_PROFILE_MESSAGE;

							return getServerProfileMessage();
						}
					}
				}				
				catch(Exception e)
				{
					reset();
					current_step=Step.NOT_CONNECTED;
					return new IncomprehensiblePublicKey();
				}
			} else {
				reset();
				current_step=Step.NOT_CONNECTED;
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_ENCRYPTION_DATA:{
			if (_m instanceof KeyAgreementDataMessage)
			{
				if (!keyAgreementForSignature.isAgreementProcessValid() || (keyAgreementForEncryption!=null && keyAgreementForEncryption.hasFinishedReception()))
				{
					reset();
					current_step=Step.NOT_CONNECTED;
					return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
				}
				try
				{
					KeyAgreementDataMessage kadm=(KeyAgreementDataMessage)_m;

					assert keyAgreementForEncryption != null;
					keyAgreementForEncryption.receiveData(kadm.getData());
					byte[] data = null;
					if (!keyAgreementForEncryption.hasFinishedSend())
						data=keyAgreementForEncryption.getDataToSend();
					doNotTakeIntoAccountNextState=false;
					if (keyAgreementForEncryption.hasFinishedReception())
					{
						doNotTakeIntoAccountNextState=true;
						current_step=Step.WAITING_FOR_SERVER_PROFILE_MESSAGE;
						secret_key_for_encryption=keyAgreementForEncryption.getDerivedKey();
						checkSymmetricEncryptionAlgorithm();
					}
						
					if (data!=null)
					{
						KeyAgreementDataMessage ka=new KeyAgreementDataMessage(data, null);
						if (current_step==Step.WAITING_FOR_SERVER_PROFILE_MESSAGE)
							return new DoubleConnectionMessage(ka, getServerProfileMessage());
						else
							return ka;
					}
					else
					{
						doNotTakeIntoAccountNextState=false;
						myCounterSent=true;
						return getServerProfileMessage();
						//return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
					}
				}				
				catch(Exception e)
				{
					reset();
					current_step=Step.NOT_CONNECTED;
					return new IncomprehensiblePublicKey();
				}
			} else {
				reset();
				current_step=Step.NOT_CONNECTED;
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_SERVER_PROFILE_MESSAGE:
		{
			doNotTakeIntoAccountNextState=false;
			if (_m instanceof ServerProfileMessage)
			{
				ServerProfileMessage m=(ServerProfileMessage)_m;
				byte[] salt=m.getSalt();
				if (salt!=null)
				{
					AbstractKeyPair<?, ?> keyPair=hproperties.getKeyPairForSignature(m.getProfileIdentifier());
					if (keyPair==null)
					{
						current_step=Step.NOT_CONNECTED;
						return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
					}
					else
					{
						current_step=Step.WAITING_FOR_SERVER_SIGNATURE;
						try {
							return new ServerSignatureMessage(salt, keyPair);
						} catch (NoSuchProviderException | NoSuchAlgorithmException | IOException e) {
							reset();
							current_step=Step.NOT_CONNECTED;
							return new IncomprehensiblePublicKey();
						}
					}
				}
				else
				{
					current_step=Step.WAITING_FOR_SERVER_SIGNATURE;
					return new ServerSignatureMessage();
				}

			}
			else {
				reset();
				current_step=Step.NOT_CONNECTED;
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_SERVER_SIGNATURE:
		{
			doNotTakeIntoAccountNextState=false;
			if (_m instanceof ServerSignatureMessage)
			{
				ServerSignatureMessage m=(ServerSignatureMessage)_m;
				if (m.checkSignature(this.localSalt, hproperties.getClientSidePublicKey()))
				{
					current_step=Step.WAITING_FOR_CONNECTION_CONFIRMATION;
					return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
				}
				else
				{
					reset();
					current_step=Step.NOT_CONNECTED;
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
				}
			}
			else {
				reset();
				current_step=Step.NOT_CONNECTED;
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_CONNECTION_CONFIRMATION:{
			doNotTakeIntoAccountNextState=false;
			if (_m instanceof ConnectionFinished)
			{
				if (!keyAgreementForSignature.isAgreementProcessValid() || (keyAgreementForEncryption!=null && !keyAgreementForEncryption.hasFinishedReception()))
				{
					reset();
					current_step=Step.NOT_CONNECTED;
					return new ConnectionFinished(distant_inet_address, ConnectionClosedReason.CONNECTION_ANOMALY);
				}
				ConnectionFinished cf=((ConnectionFinished) _m);
				if (cf.getState()==ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)
				{
					if (!packetCounter.setDistantCounters(cf.getInitialCounter()))
					{
						current_step=Step.NOT_CONNECTED;
						return new UnexpectedMessage(this.getDistantInetSocketAddress());
					}
					current_step=Step.CONNECTED;
					if (!myCounterSent)
					{
						myCounterSent=true;
						return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
					}
					else
						return null;
				} else {
					reset();
					current_step=Step.NOT_CONNECTED;
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
				}
				
					
			} else {
				reset();
				current_step=Step.NOT_CONNECTED;
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case CONNECTED: {
			if (_m instanceof ConnectionFinished) {
				ConnectionFinished cf = (ConnectionFinished) _m;
				if (!cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					if (cf.getState().equals(ConnectionProtocol.ConnectionState.CONNECTION_CLOSED)) {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED);
					} else {
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_LOST);
					}
				}
				else
				{
					if (!packetCounter.setDistantCounters(cf.getInitialCounter()))
					{
						current_step=Step.NOT_CONNECTED;
						return new UnexpectedMessage(this.getDistantInetSocketAddress());
					}
				}
				return null;
			} else {
				reset();
				current_step=Step.NOT_CONNECTED;
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		}
		return null;
	}

	private ServerProfileMessage getServerProfileMessage()
	{
		if (!hproperties.hasClientSideProfileIdentifier()) {
			this.localSalt=null;
			return new ServerProfileMessage();
		}
		else {

			ServerProfileMessage res=new ServerProfileMessage(hproperties.getClientSideProfileIdentifier(), approvedRandom);
			this.localSalt=res.getSalt();
			return res;
		}
	}


	@Override
	protected void closeConnection(ConnectionClosedReason _reason) {
		if (_reason.equals(ConnectionClosedReason.CONNECTION_ANOMALY))
			reset();
		current_step = Step.NOT_CONNECTED;
	}



	private class ParserWithEncryption extends SubBlockParser {
		public ParserWithEncryption() throws ConnectionException {
			super(decoderWithEncryption, decoderWithoutEncryption, encoderWithEncryption, encoderWithoutEncryption, packetCounter);
		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_SIGNATURE_DATA:
					return size;
				case WAITING_FOR_ENCRYPTION_DATA:
					if (doNotTakeIntoAccountNextState)
						return size;
					else
						return (int)encoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
				case WAITING_FOR_SERVER_PROFILE_MESSAGE:
				case WAITING_FOR_SERVER_SIGNATURE:
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				{
					if (doNotTakeIntoAccountNextState)
						return (int)encoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
					else
					{
						if (packetCounter.isDistantActivated())
						{
							reinitSymmetricAlgorithmIfNecessary();
						}
						return (int)encoderWithEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
					}
				}
				case CONNECTED:
				{
					if (packetCounter.isDistantActivated())
					{
						reinitSymmetricAlgorithmIfNecessary();
					}
					return (int)encoderWithEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
				}
				}
			} catch (IOException e) {
				throw new BlockParserException(e);
			}
			return size;

		}
		

		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_SIGNATURE_DATA:
					return size;
				case WAITING_FOR_ENCRYPTION_DATA:
					return (int)decoderWithoutEncryption.getMaximumOutputLength(size+EncryptionSignatureHashEncoder.headSize);
				case WAITING_FOR_SERVER_PROFILE_MESSAGE:
				case WAITING_FOR_SERVER_SIGNATURE:
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED:
				{
					if (getPacketCounter().isLocalActivated())
					{
						reinitSymmetricAlgorithmIfNecessary();
					}
					return (int)encoderWithEncryption.getMaximumOutputLength(size+EncryptionSignatureHashEncoder.headSize);
				}
				}
			} catch (IOException e) {
				throw new BlockParserException(e);
			}
			return size;

		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {

			switch (current_step) {
			case NOT_CONNECTED:
			case WAITING_FOR_SIGNATURE_DATA:
				return getSubBlockWithNoEncryption(_block);
			case WAITING_FOR_ENCRYPTION_DATA:
				return getEncryptedSubBlock(_block, false);
			case WAITING_FOR_SERVER_PROFILE_MESSAGE:
			case WAITING_FOR_SERVER_SIGNATURE:
			case WAITING_FOR_CONNECTION_CONFIRMATION:
			case CONNECTED: {
				return getEncryptedSubBlock(_block, true);
			}

			}
			throw new BlockParserException("Unexpected exception");
		}

		public SubBlockInfo getSubBlockWithNoEncryption(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getHeadSize(),
					getBodyOutputSizeForDecryption(_block.getSize() - getHeadSize())), true, false);
		}



		/*public SubBlockInfo getSubBlockWithEncryption(SubBlock _block, boolean enabledEncryption) throws BlockParserException {

			try {
				try {
					rbis.init(_block.getBytes());
					lrim.init(rbis, _block.getOffset(), _block.getSize());
					EncryptionSignatureHashDecoder decoder;
					if (enabledEncryption) {
						decoder = decoderWithEncryption;
						if (getPacketCounter().isLocalActivated()) {
							decoder.withExternalCounter(packetCounter.getMyEncryptionCounter());
						}
					} else
						decoder = decoderWithoutEncryption;
					decoder.withRandomInputStream(rbis);
					if (getPacketCounter().isLocalActivated()) {
						decoder.withAssociatedData(packetCounter.getMySignatureCounter());
					}

					if (decoder.isEncrypted()) {
						byte[] tab = new byte[_block.getBytes().length];
						try {
							rout.init(tab);
							lrout.init(rout, _block.getOffset(), _block.getSize());
							int dl = (int) decoder.decodeAndCheckHashAndSignaturesIfNecessary(lrout);
							return new SubBlockInfo(new SubBlock(tab, _block.getOffset() + EncryptionSignatureHashEncoder.headSize, dl), true, false);
						} finally {
							rout.init(emptyTab);
							lrout.init(rout, 0);
						}
					} else {
						Integrity integrity = decoder.checkHashAndSignature();
						int dl = (int) decoder.getDataSizeInBytesAfterDecryption();
						return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + EncryptionSignatureHashEncoder.headSize, dl), integrity == Integrity.OK, integrity == Integrity.FAIL_AND_CANDIDATE_TO_BAN);
					}
				}
				finally {
					rbis.init(emptyTab);
					lrim.init(rbis, 0);
				}
			} catch (IOException e) {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset(),
						getBodyOutputSizeForDecryption(_block.getSize() - getHeadSize()));
				return new SubBlockInfo(res, false, e instanceof MessageExternalizationException && ((MessageExternalizationException) e).getIntegrity()==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}

		}*/

		/*public SubBlock getParentBlockWithEncryption(final SubBlock _block, boolean excludeFromEncryption) throws IOException {
			EncryptionSignatureHashEncoder encoder;
			if (excludeFromEncryption)
			{
				encoder = encoderWithoutEncryption;
			}
			else {
				encoder = encoderWithEncryption;
				if (packetCounter.isDistantActivated())
					encoder.withExternalCounter(packetCounter.getOtherEncryptionCounter());
			}
			if (packetCounter.isDistantActivated())
				encoder.withAssociatedData(packetCounter.getOtherSignatureCounter() );
			if (excludeFromEncryption) {
				encoder.encodeWithSameInputAndOutputStreamSource(_block.getBytes(), _block.getOffset(), _block.getSize());
				return new SubBlock(_block.getBytes(), _block.getOffset() - EncryptionSignatureHashEncoder.headSize, (int) encoder.getMaximumOutputLength(_block.getSize()));
			}
			else
			{
				SubBlock res = new SubBlock(new byte[_block.getSize()], _block.getOffset() - EncryptionSignatureHashEncoder.headSize, (int) encoder.getMaximumOutputLength(_block.getSize()));
				encoder.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), res.getBytes(), res.getOffset(), res.getSize());
				return res;
			}

		}*/
		
		
		@Override
		public SubBlock getParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_SIGNATURE_DATA:
					return getParentBlockWithNoTreatments(_block);
				case WAITING_FOR_ENCRYPTION_DATA:
					if (doNotTakeIntoAccountNextState)
						return getParentBlockWithNoTreatments(_block);
					else
						return getEncryptedParentBlock(_block, true);
				case WAITING_FOR_SERVER_PROFILE_MESSAGE:
				case WAITING_FOR_SERVER_SIGNATURE:
				case WAITING_FOR_CONNECTION_CONFIRMATION: {
					if (doNotTakeIntoAccountNextState)
					{
						if (!hproperties.enableEncryption)
							return getParentBlockWithNoTreatments(_block);
						else
							return getEncryptedParentBlock(_block, true);
					}
					else
					{
						return getEncryptedParentBlock(_block, excludeFromEncryption);
					}
				}
				case CONNECTED: {
					return getEncryptedParentBlock(_block, excludeFromEncryption);
				}
			}

			throw new BlockParserException("Unexpected exception");

		}
		

		@Override
		public int getHeadSize() {
			return EncryptionSignatureHashEncoder.headSize;
		}



		
		@Override
		public SubBlockInfo checkIncomingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_SIGNATURE_DATA:
					return falseCheckEntrantPointToPointTransferredBlockWithoutDecoder(_block);
				case WAITING_FOR_ENCRYPTION_DATA:
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case WAITING_FOR_SERVER_PROFILE_MESSAGE:
				case WAITING_FOR_SERVER_SIGNATURE:
				case CONNECTED: {
					return checkEntrantPointToPointTransferredBlockWithDecoder(_block);
				}
			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock signIfPossibleOutgoingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_SIGNATURE_DATA:
					return falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(_block);
				case WAITING_FOR_ENCRYPTION_DATA:
				{
					if (isCurrentServerAskingConnection())
						return falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(_block);
					else
						return signOutgoingPointToPointTransferredBlockWithEncoder(_block);
				}
	 			case WAITING_FOR_SERVER_PROFILE_MESSAGE:
				case WAITING_FOR_SERVER_SIGNATURE:
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					return signOutgoingPointToPointTransferredBlockWithEncoder(_block);
				}
			}

			throw new BlockParserException("Unexpected exception");
		}

	}

	private class ParserWithNoEncryption extends ParserWithEncryption {
		public ParserWithNoEncryption() throws ConnectionException {
			super();
		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
					case NOT_CONNECTED:
					case WAITING_FOR_SIGNATURE_DATA:
						return size;
					case WAITING_FOR_ENCRYPTION_DATA:
						if (doNotTakeIntoAccountNextState)
							return size;
						else
							return (int)encoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
					case WAITING_FOR_SERVER_PROFILE_MESSAGE:
					case WAITING_FOR_SERVER_SIGNATURE:
					case WAITING_FOR_CONNECTION_CONFIRMATION:
					case CONNECTED:
					{
						return (int)encoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
					}
				}
			} catch (IOException e) {
				throw new BlockParserException(e);
			}
			return size;

		}


		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
					case NOT_CONNECTED:
					case WAITING_FOR_SIGNATURE_DATA:
						return size;
					case WAITING_FOR_ENCRYPTION_DATA:
					case WAITING_FOR_SERVER_PROFILE_MESSAGE:
					case WAITING_FOR_SERVER_SIGNATURE:
					case WAITING_FOR_CONNECTION_CONFIRMATION:
					case CONNECTED:
						return (int)decoderWithoutEncryption.getMaximumOutputLength(size+EncryptionSignatureHashEncoder.headSize);
				}
			} catch (IOException e) {
				throw new BlockParserException(e);
			}
			return size;

		}


		@Override
		public int getHeadSize() {
			return EncryptionSignatureHashEncoder.headSize;
		}

		@Override
		protected SubBlockInfo getEncryptedSubBlock(SubBlock _block, boolean enabledEncryption) throws BlockParserException {
			return super.getEncryptedSubBlock(_block, false);
		}

		@Override
		protected SubBlock getEncryptedParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			return super.getEncryptedParentBlock(_block, true);
		}

		/*@Override
		public SubBlockInfo getSubBlockWithEncryption(SubBlock _block, boolean enabledEncryption) throws BlockParserException {
			try {
				try {
					rbis.init(_block.getBytes());
					lrim.init(rbis, _block.getOffset(), _block.getSize());
					decoderWithoutEncryption.withRandomInputStream(rbis);
					if (getPacketCounter().isLocalActivated()) {
						decoderWithoutEncryption.withAssociatedData(packetCounter.getMySignatureCounter());
					}

					Integrity integrity = decoderWithoutEncryption.checkHashAndSignature();
					int dl = (int) decoderWithoutEncryption.getDataSizeInBytesAfterDecryption();
					return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + EncryptionSignatureHashEncoder.headSize, dl), integrity == Integrity.OK, integrity == Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				}
				finally {
					rbis.init(emptyTab);
					lrim.init(rbis, 0);
				}
			} catch (IOException e) {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset(),
						getBodyOutputSizeForDecryption(_block.getSize() - getHeadSize()));
				return new SubBlockInfo(res, false, e instanceof MessageExternalizationException && ((MessageExternalizationException) e).getIntegrity()==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
		}

		@Override
		public SubBlock getParentBlockWithEncryption(SubBlock _block, boolean excludeFromEncryption) throws IOException {
			if (packetCounter.isDistantActivated())
				encoderWithoutEncryption.withAssociatedData(packetCounter.getOtherSignatureCounter() );
			encoderWithoutEncryption.encodeWithSameInputAndOutputStreamSource(_block.getBytes(), _block.getOffset(), _block.getSize());
			return new SubBlock(_block.getBytes(), _block.getOffset() - EncryptionSignatureHashEncoder.headSize, (int) encoderWithoutEncryption.getMaximumOutputLength(_block.getSize()));
		}*/
		
		
	}

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	@Override
	protected TransferedBlockChecker getTransferredBlockChecker(TransferedBlockChecker subBlockChecker)
			throws ConnectionException {
		
		try {
			blockCheckerChanged = false;
			return new ConnectionProtocol.NullBlockChecker(subBlockChecker, this.isCrypted(), (short) parser.getHeadSize());
			
			/*if (secret_key_for_signature == null || current_step.compareTo(Step.WAITING_FOR_CONNECTION_CONFIRMATION) <= 0) {
				currentBlockCheckerIsNull = true;
				return new ConnectionProtocol.NullBlockChecker(subBlockChecker, this.isEncrypted(), (short) parser.getSizeHead());
			} else {
				currentBlockCheckerIsNull = false;
				return new BlockChecker(subBlockChecker, this.hproperties.signatureType,
						secret_key_for_signature, this.signature_size, this.isEncrypted());
			}*/
		} catch (BlockParserException e) {
			blockCheckerChanged = true;
			throw new ConnectionException(e);
		}
	}

	/*private static class BlockChecker extends TransferedBlockChecker {
		private final SymmetricSignatureType signatureType;
		private final int signatureSize;
		private transient SymmetricSignatureCheckerAlgorithm signatureChecker;
		private transient SymmetricSecretKey secretKey;

		protected BlockChecker(TransferedBlockChecker _subChecker, SymmetricSignatureType signatureType,
				SymmetricSecretKey secretKey, int signatureSize, boolean isEncrypted) throws NoSuchAlgorithmException {
			super(_subChecker, !isEncrypted);
			this.signatureType = signatureType;
			this.secretKey = secretKey;
			this.signatureSize = signatureSize;
			initSignature();
		}

		@Override
		public Integrity checkDataIntegrity() {
			if (signatureType == null)
				return Integrity.FAIL;
			if (signatureChecker == null)
				return Integrity.FAIL;
			if (secretKey == null)
				return Integrity.FAIL;
			return Integrity.OK;
		}

		private void initSignature() throws NoSuchAlgorithmException {
			this.signatureChecker = new SymmetricSignatureCheckerAlgorithm(secretKey);
		}

		private void writeObject(ObjectOutputStream os) throws IOException {
			os.defaultWriteObject();
			byte encodedPK[] = secretKey.encode();
			os.writeInt(encodedPK.length);
			os.write(encodedPK);
		}

		private void readObject(ObjectInputStream is) throws IOException {
			try {
				is.defaultReadObject();
				int len = is.readInt();
				byte encodedPK[] = new byte[len];
				is.read(encodedPK);
				secretKey = SymmetricSecretKey.decode(encodedPK);
				initSignature();
			} catch (IOException e) {
				throw e;
			} catch (IOException e) {
				throw new IOException(e);
			}
		}

		@Override
		public SubBlockInfo checkSubBlock(SubBlock _block) throws BlockParserException {
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + signatureSize,
						_block.getSize() - signatureSize);
				signatureChecker.init();
				signatureChecker.update(res.getBytes(), res.getOffset(), res.getSize());
				boolean check = signatureChecker.verify(_block.getBytes(), _block.getOffset(), signatureSize);
				return new SubBlockInfo(res, check, !check);
			} catch (IOException e) {
				throw new BlockParserException(e);
			}
		}

	}*/


	@Override
	public boolean isTransferBlockCheckerChangedImpl() {
		//boolean currentBlockCheckerIsNull = true;
		if (secret_key_for_encryption == null || secret_key_for_signature==null || current_step.compareTo(Step.WAITING_FOR_CONNECTION_CONFIRMATION) <= 0) {
			return blockCheckerChanged;
		} else
			return true;

	}

	@Override
	public PacketCounter getPacketCounter() {
		return packetCounter;
	}
	
	

}
