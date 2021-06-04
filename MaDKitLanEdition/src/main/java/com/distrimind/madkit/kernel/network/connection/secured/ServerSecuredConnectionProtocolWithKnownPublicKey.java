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
import com.distrimind.util.sizeof.ObjectSizer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;

/**
 * Represents a connection protocol used between a client and a server. This
 * class must be used by the server. There is no certificate, so the public key
 * must be known in advance with this protocol.
 *
 * Doest not support forward secrecy
 *
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.0
 * @see ClientSecuredConnectionProtocolWithKnownPublicKey
 */
public class ServerSecuredConnectionProtocolWithKnownPublicKey
		extends ConnectionProtocol<ServerSecuredConnectionProtocolWithKnownPublicKey> {
	Step current_step = Step.NOT_CONNECTED;

	AbstractKeyPair<?, ?> myKeyPairForEncryption;

	private final EncryptionSignatureHashEncoder encoderWithEncryption;
	private final EncryptionSignatureHashEncoder encoderWithoutEncryption;
	private final EncryptionSignatureHashDecoder decoderWithEncryption;
	private final EncryptionSignatureHashDecoder decoderWithoutEncryption;

	protected SymmetricEncryptionType symmetricEncryptionType;
	/*protected SymmetricEncryptionAlgorithm symmetricEncryption;
	protected SymmetricAuthenticatedSignerAlgorithm signer = null;
	protected SymmetricAuthenticatedSignatureCheckerAlgorithm signatureChecker=null;*/
	protected SymmetricSecretKey mySecretKeyForEncryption=null,mySecretKeyForSignature=null;
	protected SymmetricAuthenticatedSignatureType signatureType;
	protected ASymmetricKeyWrapperType keyWrapper;
	protected MessageDigestType messageDigestType;
	
	protected short secretKeySizeBits;
	private final SubBlockParser parser;

	protected final ServerSecuredProtocolPropertiesWithKnownPublicKey hProperties;
	private final AbstractSecureRandom approvedRandom;
	final int maximumSignatureSize;
	boolean firstMessageReceived = false;
	private boolean needToRefreshTransferBlockChecker = true;
	private PacketCounterForEncryptionAndSignature packetCounter=null;
	private boolean reInitSymmetricAlgorithm =true;


	private ServerSecuredConnectionProtocolWithKnownPublicKey(InetSocketAddress _distant_inet_address,
															  InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol,
															  DatabaseWrapper sql_connection, MadkitProperties mkProperties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer,
															  boolean mustSupportBidirectionalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, mkProperties,cpp,
				subProtocolLevel, isServer, mustSupportBidirectionalConnectionInitiative);
		hProperties = (ServerSecuredProtocolPropertiesWithKnownPublicKey) super.connection_protocol_properties;

		hProperties.checkProperties();

		myKeyPairForEncryption = null;

		this.keyWrapper=null;
		signatureType = null;
		symmetricEncryptionType=null;
		secretKeySizeBits=-1;
		try {
			approvedRandom=mkProperties.getApprovedSecureRandom();

			maximumSignatureSize = hProperties.getMaximumSignatureSizeBits();
			encoderWithEncryption=new EncryptionSignatureHashEncoder();
			encoderWithoutEncryption=new EncryptionSignatureHashEncoder();
			decoderWithEncryption=new EncryptionSignatureHashDecoder();
			decoderWithoutEncryption=new EncryptionSignatureHashDecoder();
			encoderWithEncryption.connectWithDecoder(decoderWithEncryption);
			encoderWithoutEncryption.connectWithDecoder(decoderWithoutEncryption);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new ConnectionException(e);
		}

		if (hProperties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();

	}

	void initMyKeyPair(int identifier) throws BlockParserException {
		if (myKeyPairForEncryption != null)
			return;
		try {
			if (!hProperties.isValidProfile(identifier, network_properties.encryptionRestrictionForConnectionProtocols))
				throw new BlockParserException(
						"Invalid profile " + identifier);

			myKeyPairForEncryption = hProperties.getKeyPairForEncryption(identifier);
			if (myKeyPairForEncryption == null)
				throw new BlockParserException(
						"Unknown encryption profile. Impossible to find key pair identified by " + identifier);

			
			signatureType = hProperties.getSignatureType(identifier);
			if (signatureType == null)
				throw new BlockParserException(
						"Unknown encryption profile. Impossible to find signature identified by " + identifier);

			keyWrapper= hProperties.getKeyWrapper(identifier);

			if (keyWrapper == null)
				throw new BlockParserException(
						"Unknown encryption profile. Impossible to find keyWrapper identified by " + identifier);
			
			symmetricEncryptionType = hProperties.getSymmetricEncryptionType(identifier);
			if (symmetricEncryptionType == null)
				throw new BlockParserException(
						"Unknown encryption profile. Impossible to find symmetric encryption type identified by "
								+ identifier);
			secretKeySizeBits= hProperties.getSymmetricEncryptionKeySizeBits(identifier);
			messageDigestType= hProperties.getMessageDigestType(identifier);
			if (messageDigestType!=null) {
				encoderWithEncryption.withMessageDigestType(messageDigestType);
				encoderWithoutEncryption.withMessageDigestType(messageDigestType);
				decoderWithEncryption.withMessageDigestType(messageDigestType);
				decoderWithoutEncryption.withMessageDigestType(messageDigestType);
			}
			else
			{
				encoderWithEncryption.withoutMessageDigest();
				encoderWithoutEncryption.withoutMessageDigest();
				decoderWithEncryption.withoutMessageDigest();
				decoderWithoutEncryption.withoutMessageDigest();
			}

		} catch (Exception e) {
			if (e instanceof BlockParserException)
				throw (BlockParserException) e;
			else
				throw new BlockParserException(e);
		}
	}
	private void reInitSymmetricAlgorithmIfNecessary() throws IOException {
		if (reInitSymmetricAlgorithm)
		{
			reInitSymmetricAlgorithm =false;
			if (packetCounter.getMyEncryptionCounter()==null) {
				encoderWithEncryption.withSymmetricSecretKeyForEncryption(this.approvedRandom, this.mySecretKeyForEncryption);
				decoderWithEncryption.withSymmetricSecretKeyForEncryption(this.mySecretKeyForEncryption);
			}
			else {
				encoderWithEncryption.withSymmetricSecretKeyForEncryption(this.approvedRandom, this.mySecretKeyForEncryption, (byte) packetCounter.getMyEncryptionCounter().length);
				decoderWithEncryption.withSymmetricSecretKeyForEncryption(this.mySecretKeyForEncryption, (byte) packetCounter.getMyEncryptionCounter().length);

			}
		}
	}
	boolean isProfileInitialized()
	{
		return firstMessageReceived;
	}

	private void setSecretKeys(AskClientServerConnection askMessage) throws ConnectionException {
		try {
			if (askMessage.getSecretKeyForEncryption()==null && hProperties.enableEncryption)
				throw new ConnectionException("Secret key empty !");

			KeyWrapperAlgorithm kws=new KeyWrapperAlgorithm(keyWrapper, myKeyPairForEncryption);
			mySecretKeyForSignature=kws.unwrap( askMessage.getSecretKeyForSignature());

			if (mySecretKeyForSignature==null /*|| !askMessage.checkSignedMessage(mySecretKeyForSignature, hProperties.enableEncryption)*/)
				throw new ConnectionException("Message signature is not checked !");




			if (hProperties.enableEncryption)
			{
				KeyWrapperAlgorithm kwe=new KeyWrapperAlgorithm(keyWrapper, myKeyPairForEncryption, mySecretKeyForSignature);
				mySecretKeyForEncryption=kwe.unwrap(askMessage.getSecretKeyForEncryption());
				this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, hProperties.enableEncryption && mySecretKeyForEncryption.getEncryptionAlgorithmType().getMaxCounterSizeInBytesUsedWithBlockMode()>0, true);
				if (this.packetCounter.getMyEncryptionCounter()==null) {
					encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, mySecretKeyForEncryption);
					decoderWithEncryption.withSymmetricSecretKeyForEncryption(mySecretKeyForEncryption);
				}
				else
				{
					encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, mySecretKeyForEncryption, (byte)packetCounter.getMyEncryptionCounter().length);
					decoderWithEncryption.withSymmetricSecretKeyForEncryption(mySecretKeyForEncryption, (byte)packetCounter.getMyEncryptionCounter().length);
				}
			}
			else {
				this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, false, true);
				mySecretKeyForEncryption = null;
				encoderWithEncryption.withoutSymmetricEncryption();
				decoderWithEncryption.withoutSymmetricEncryption();
			}
			parser.setPacketCounter(packetCounter);

			encoderWithoutEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
			if (!hProperties.enableEncryption || !mySecretKeyForEncryption.getEncryptionAlgorithmType().isAuthenticatedAlgorithm())
				encoderWithEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
			decoderWithoutEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
			if (!hProperties.enableEncryption || !mySecretKeyForEncryption.getEncryptionAlgorithmType().isAuthenticatedAlgorithm())
				decoderWithEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
			// this.secret_key=symmetricAlgorithm.getSecretKey();
		} catch (Exception e) {
			e.printStackTrace();
			resetKeys();
			throw new ConnectionException(e);
		}
	}

	private void resetKeys() {
		mySecretKeyForEncryption = null;
		mySecretKeyForSignature=null;
		encoderWithEncryption.withoutSymmetricEncryption();
		decoderWithEncryption.withoutSymmetricEncryption();
		encoderWithEncryption.withoutSymmetricSignature();
		encoderWithoutEncryption.withoutSymmetricSignature();
		decoderWithEncryption.withoutSymmetricSignature();
		decoderWithoutEncryption.withoutSymmetricSignature();
		encoderWithEncryption.withoutMessageDigest();
		encoderWithoutEncryption.withoutMessageDigest();
		decoderWithEncryption.withoutMessageDigest();
		decoderWithoutEncryption.withoutMessageDigest();
	}

	private enum Step {
		NOT_CONNECTED, WAITING_FOR_CONNECTION_CONFIRMATION, CONNECTED,
	}

	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) {
		switch (current_step) {
		case NOT_CONNECTED: {
			
			if (_m instanceof AskClientServerConnection) {
				AskClientServerConnection ask = (AskClientServerConnection) _m;

				try {
					setSecretKeys(ask);
				} catch (ConnectionException e) {
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
				}
				current_step = Step.WAITING_FOR_CONNECTION_CONFIRMATION;
				return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
			} else if (_m instanceof ConnectionFinished) {
				if (((ConnectionFinished) _m).getState()
						.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				} else
					return new ConnectionFinished(this.getDistantInetSocketAddress(),
							ConnectionClosedReason.CONNECTION_ANOMALY);
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		case WAITING_FOR_CONNECTION_CONFIRMATION: {
			if (_m instanceof ConnectionFinished && ((ConnectionFinished) _m).getState()
					.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
				if (!packetCounter.setDistantCounters(((ConnectionFinished) _m).getInitialCounter()))
				{
					current_step=Step.NOT_CONNECTED;
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				}
				current_step = Step.CONNECTED;
				return null;
			} else if (_m instanceof ConnectionFinished) {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			} else {
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
				
				return null;
			} else {
				return new UnexpectedMessage(this.getDistantInetSocketAddress());
			}
		}
		}
		return null;
	}


	@Override
	protected void closeConnection(ConnectionClosedReason _reason) {
		if (_reason.equals(ConnectionClosedReason.CONNECTION_ANOMALY)) {
			resetKeys();
		}
		current_step = Step.NOT_CONNECTED;
	}

	private class ParserWithEncryption extends SubBlockParser {


		ParserWithEncryption() throws ConnectionException {
			super(decoderWithEncryption, decoderWithoutEncryption, encoderWithEncryption, encoderWithoutEncryption, packetCounter);
		}

		@Override
		public String toString() {
			return "ServerParserWithEncryption{"+current_step+"}";
		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			
			try {
				if (current_step==Step.NOT_CONNECTED)
					return size;
				else
				{
					if (packetCounter.isDistantActivated())
					{
						reInitSymmetricAlgorithmIfNecessary();
					}
					return getBodyOutputSizeWithEncryption(size);
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}

		@Override
		public int getBodyOutputSizeForSignature(int size) throws BlockParserException
		{
			try {
				if (current_step==Step.NOT_CONNECTED)
					return size;
				else
				{
					if (packetCounter.isDistantActivated())
					{
						reInitSymmetricAlgorithmIfNecessary();
					}
					return getBodyOutputSizeWithSignature(size);
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}
		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
					return size;
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED:
					if (getPacketCounter().isLocalActivated())
					{
						reInitSymmetricAlgorithmIfNecessary();
					}
					return getBodyOutputSizeWithDecryption(size);
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new IllegalAccessError();
		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
				case NOT_CONNECTED: {


					if (!isProfileInitialized())
					{
						int identifier = Bits.getInt(_block.getBytes(), _block.getOffset());
						initMyKeyPair(identifier);
					}
					SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getHeadSize(),
							getBodyOutputSizeForDecryption(_block.getSize() - getHeadSize()));
					setFirstMessageReceived();

					return new SubBlockInfo(res, true, false);
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					return getEncryptedSubBlock(_block, true);
				}
			}
			throw new BlockParserException("Unexpected exception");
		}

		@Override
		public SubBlock getParentBlock(final SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			switch (current_step) {
				case NOT_CONNECTED: {
					final int outputSize = getBodyOutputSizeForEncryption(_block.getSize());
					SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getHeadSize(),
							outputSize + getHeadSize());


					int off = _block.getSize() + _block.getOffset();
					byte[] tab = res.getBytes();
					Arrays.fill(tab, off, outputSize + _block.getOffset(), (byte) 0);
					Arrays.fill(tab, res.getOffset(), _block.getOffset(), (byte) 0);
					return res;
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					return getEncryptedParentBlock(_block, excludeFromEncryption);
				}
			}
			throw new IllegalAccessError();
		}

		@Override
		public int getHeadSize() {
			if (isProfileInitialized())
				return EncryptionSignatureHashEncoder.headSize;
			else
				return ObjectSizer.sizeOf(hProperties.getLastEncryptionProfileIdentifier());
		}

		@Override
		public SubBlockInfo checkIncomingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {

			switch (current_step) {
			
				case NOT_CONNECTED: {
					return falseCheckEntrantPointToPointTransferredBlockWithoutDecoder(_block);
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
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
				{
					return falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(_block);
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					return signOutgoingPointToPointTransferredBlockWithEncoder(_block);
				}
			}
			throw new IllegalAccessError();
		}

	}

	private class ParserWithNoEncryption extends ParserWithEncryption {
		ParserWithNoEncryption() throws ConnectionException {
			super();
		}

		@Override
		protected SubBlockInfo getEncryptedSubBlock(SubBlock _block, boolean enabledEncryption) throws BlockParserException {
			return super.getEncryptedSubBlock(_block, false);
		}

		@Override
		protected SubBlock getEncryptedParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			return super.getEncryptedParentBlock(_block, true);
		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {

			try {
				if (current_step==Step.NOT_CONNECTED)
					return size;
				else
				{
					if (packetCounter.isDistantActivated())
					{
						reInitSymmetricAlgorithmIfNecessary();
					}
					return getBodyOutputSizeWithEncryption(size);
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}


		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
					case NOT_CONNECTED:
						return size;
					case WAITING_FOR_CONNECTION_CONFIRMATION:
					case CONNECTED:
						if (getPacketCounter().isLocalActivated())
						{
							reInitSymmetricAlgorithmIfNecessary();
						}
						return getBodyOutputSizeWithDecryption(size);
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new IllegalAccessError();
		}

	}

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	void setFirstMessageReceived() {
		this.firstMessageReceived = true;
		this.needToRefreshTransferBlockChecker = true;
	}

	@Override
	protected TransferedBlockChecker getTransferredBlockChecker(TransferedBlockChecker subBlockChecker)
			throws ConnectionException {
		try {
			needToRefreshTransferBlockChecker=false;
			return new ConnectionProtocol.NullBlockChecker(subBlockChecker, this.isCrypted(),
					(short) parser.getHeadSize());
		} catch (Exception e) {
			needToRefreshTransferBlockChecker = true;
			throw new ConnectionException(e);
		}
	}

	@Override
	public boolean isTransferBlockCheckerChangedImpl() {
		return needToRefreshTransferBlockChecker;

	}



	@Override
	public PacketCounter getPacketCounter() {
		return packetCounter;
	}

}
