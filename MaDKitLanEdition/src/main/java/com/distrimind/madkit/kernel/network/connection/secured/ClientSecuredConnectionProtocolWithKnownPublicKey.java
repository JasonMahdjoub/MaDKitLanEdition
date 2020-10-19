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
 * class must be used by the client. There is no certificate, so the public key
 * must be known in advance with this protocol.
 *
 * Doest not support forward secrecy
 * 
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.0
 * @see ServerSecuredConnectionProtocolWithKnownPublicKey
 */
public class ClientSecuredConnectionProtocolWithKnownPublicKey
		extends ConnectionProtocol<ClientSecuredConnectionProtocolWithKnownPublicKey> {
	Step current_step = Step.NOT_CONNECTED;

	private final EncryptionSignatureHashEncoder encoderWithEncryption;
	private final EncryptionSignatureHashEncoder encoderWithoutEncryption;
	private final EncryptionSignatureHashDecoder decoderWithEncryption;
	private final EncryptionSignatureHashDecoder decoderWithoutEncryption;


	private final IASymmetricPublicKey distant_public_key_for_encryption;
	protected final ClientASymmetricEncryptionAlgorithm aSymmetricAlgorithm;
	/*protected SymmetricEncryptionAlgorithm symmetricEncryption = null;
	protected SymmetricAuthenticatedSignerAlgorithm signer = null;
	protected SymmetricAuthenticatedSignatureCheckerAlgorithm signatureChecker=null;*/
	protected SymmetricSecretKey mySecretKeyForEncryption=null,mySecretKeyForSignature=null;
	protected final ASymmetricKeyWrapperType keyWrapper;
	private final SubBlockParser parser;

	protected final ClientSecuredProtocolPropertiesWithKnownPublicKey hProperties;
	
	boolean firstMessageSent = false;
	/*private boolean currentBlockCheckerIsNull = true;*/
	private boolean needToRefreshTransferBlockChecker = true;
	private final AbstractSecureRandom approvedRandom, approvedRandomForKeys;
	private final PacketCounterForEncryptionAndSignature packetCounter;
	private boolean reInitSymmetricAlgorithm =true;
	
	private ClientSecuredConnectionProtocolWithKnownPublicKey(InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol,
			DatabaseWrapper sql_connection, MadkitProperties mkProperties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer,
			boolean mustSupportBidirectionalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, mkProperties,cpp,
				subProtocolLevel, isServer, mustSupportBidirectionalConnectionInitiative);
		hProperties = (ClientSecuredProtocolPropertiesWithKnownPublicKey) super.connection_protocol_properties;

		hProperties.checkProperties();

		
		this.keyWrapper= hProperties.keyWrapper;
		try {
			approvedRandom=mkProperties.getApprovedSecureRandom();
			approvedRandomForKeys=mkProperties.getApprovedSecureRandomForKeys();
			distant_public_key_for_encryption = hProperties.getPublicKeyForEncryption();
			aSymmetricAlgorithm = new ClientASymmetricEncryptionAlgorithm(approvedRandom, distant_public_key_for_encryption);

			encoderWithEncryption=new EncryptionSignatureHashEncoder();
			encoderWithoutEncryption=new EncryptionSignatureHashEncoder();
			decoderWithEncryption=new EncryptionSignatureHashDecoder();
			decoderWithoutEncryption=new EncryptionSignatureHashDecoder();
			if (hProperties.messageDigestType!=null) {
				encoderWithEncryption.withMessageDigestType(hProperties.messageDigestType);
				encoderWithoutEncryption.withMessageDigestType(hProperties.messageDigestType);
				decoderWithEncryption.withMessageDigestType(hProperties.messageDigestType);
				decoderWithoutEncryption.withMessageDigestType(hProperties.messageDigestType);
			}
			encoderWithEncryption.connectWithDecoder(decoderWithEncryption);
			encoderWithoutEncryption.connectWithDecoder(decoderWithoutEncryption);
			
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new ConnectionException(e);
		}
		this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, hProperties.enableEncryption, true);
		generateSecretKey();
		if (hProperties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();
	}
	private void reInitSymmetricAlgorithmIfNecessary() throws IOException {
		if (reInitSymmetricAlgorithm)
		{
			reInitSymmetricAlgorithm =false;
			encoderWithEncryption.withSymmetricSecretKeyForEncryption(this.approvedRandom, this.mySecretKeyForEncryption, (byte)packetCounter.getMyEncryptionCounter().length);
			decoderWithEncryption.withSymmetricSecretKeyForEncryption(this.mySecretKeyForEncryption, (byte)packetCounter.getMyEncryptionCounter().length);
		}
	}
	private void generateSecretKey() throws ConnectionException {
		//needToRefreshTransferBlockChecker |= current_step.compareTo(Step.WAITING_FOR_CONNECTION_CONFIRMATION) >= 0;
		try {
			if (hProperties.enableEncryption)
			{
				mySecretKeyForEncryption= hProperties.getSymmetricEncryptionType().getKeyGenerator(approvedRandomForKeys, hProperties.getSymmetricKeySizeBits()).generateKey();
				encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, mySecretKeyForEncryption);
				decoderWithEncryption.withSymmetricSecretKeyForEncryption(mySecretKeyForEncryption);
			}
			else {
				mySecretKeyForEncryption = null;
				encoderWithEncryption.withoutSymmetricEncryption();
				decoderWithEncryption.withoutSymmetricEncryption();
			}
			mySecretKeyForSignature= hProperties.getSignatureType().getKeyGenerator(approvedRandomForKeys, hProperties.getSymmetricKeySizeBits()).generateKey();

			encoderWithoutEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
			encoderWithEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
			decoderWithoutEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
			decoderWithEncryption.withSymmetricSecretKeyForSignature(mySecretKeyForSignature);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			resetKeys();
			throw new ConnectionException(e);
		}
	}

	private void resetKeys() {
		mySecretKeyForEncryption = null;


		encoderWithEncryption.withoutSymmetricEncryption();
		decoderWithEncryption.withoutSymmetricEncryption();
		encoderWithEncryption.withoutSymmetricSignature();
		encoderWithoutEncryption.withoutSymmetricSignature();
		decoderWithEncryption.withoutSymmetricSignature();
		decoderWithoutEncryption.withoutSymmetricSignature();
	}

	private enum Step {
		NOT_CONNECTED, WAITING_FOR_CONNECTION_CONFIRMATION, CONNECTED,
	}

	/*
	 * private byte[] encodeSecretKeyAndIV() throws ConnectionException { try {
	 * return symmetricAlgorithm.encodeKeyAndIvParameter(aSymmetricAlgorithm); }
	 * catch(InvalidKeyException | InvalidAlgorithmParameterException | IOException
	 * | IllegalBlockSizeException | BadPaddingException e) { throw new
	 * ConnectionException(e); }
	 * 
	 * }
	 */

	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) throws ConnectionException {
		switch (current_step) {
		case NOT_CONNECTED: {
			
			if (_m instanceof AskConnection) {
				AskConnection ask = (AskConnection) _m;

				if (ask.isYouAreAsking()) {
					try {
						current_step = Step.WAITING_FOR_CONNECTION_CONFIRMATION;
						generateSecretKey();
						if (hProperties.enableEncryption)
							return new AskClientServerConnection(approvedRandom, keyWrapper, mySecretKeyForEncryption, mySecretKeyForSignature, distant_public_key_for_encryption);
						else
							return new AskClientServerConnection(approvedRandom, keyWrapper, mySecretKeyForSignature, distant_public_key_for_encryption);
					} catch (Exception e) {
						throw new ConnectionException(e);
					}
				} else {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				}
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
				return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
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

	void setFirstMessageSent() {
		this.firstMessageSent = true;
		this.needToRefreshTransferBlockChecker = true;
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
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				if (current_step==Step.NOT_CONNECTED || current_step==Step.WAITING_FOR_CONNECTION_CONFIRMATION)
					return size;
				else
				{
					if (packetCounter.isDistantActivated())
					{
						reInitSymmetricAlgorithmIfNecessary();
					}
					return (int)encoderWithEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
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
					{
						return size;
					}
					case WAITING_FOR_CONNECTION_CONFIRMATION:
					case CONNECTED: {
						if (getPacketCounter().isLocalActivated())
						{
							reInitSymmetricAlgorithmIfNecessary();
						}
						return (int)decoderWithEncryption.getMaximumOutputLength(size+EncryptionSignatureHashEncoder.headSize);
					}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException();
		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {

			if (current_step==Step.NOT_CONNECTED)
			{
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getHeadSize(),
						getBodyOutputSizeForDecryption(_block.getSize() - getHeadSize()));
				return new SubBlockInfo(res, true, false);
			}
			else
			{
				return getEncryptedSubBlock(_block, true);
			}
		}

		@Override
		public SubBlock getParentBlock(final SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			switch (current_step) {
				case WAITING_FOR_CONNECTION_CONFIRMATION:case NOT_CONNECTED:
				{
					int outputSize=getBodyOutputSizeForEncryption(_block.getSize());
					SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getHeadSize(),
							outputSize + getHeadSize());
					if (!firstMessageSent)
					{
						Bits.putInt(res.getBytes(), res.getOffset(), hProperties.getEncryptionProfileIdentifier());
						setFirstMessageSent();
					}
					int off=_block.getSize()+_block.getOffset();
					byte[] tab=res.getBytes();
					Arrays.fill(tab, off, outputSize+_block.getOffset(), (byte)0);
					Arrays.fill(tab, res.getOffset()+4, _block.getOffset(), (byte)0);
					return res;
				}

				case CONNECTED: {
					return getEncryptedParentBlock(_block, excludeFromEncryption);
				}
			}

			throw new BlockParserException("Unexpected exception");

		}

		@Override
		public int getHeadSize() {
			if (firstMessageSent)
				return EncryptionSignatureHashEncoder.headSize;
			else {
				return ObjectSizer.sizeOf(hProperties.getEncryptionProfileIdentifier());
			}
		}


		@Override
		public SubBlockInfo checkIncomingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			if (current_step==Step.NOT_CONNECTED)
			{
				return falseCheckEntrantPointToPointTransferredBlockWithoutDecoder(_block);
			}
			else
			{
				return checkEntrantPointToPointTransferredBlockWithDecoder(_block);
			}
		}

		@Override
		public SubBlock signIfPossibleOutgoingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
				case WAITING_FOR_CONNECTION_CONFIRMATION:case NOT_CONNECTED:
				{
					return falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(_block);
				}
				
				case CONNECTED: {
					return signOutgoingPointToPointTransferredBlockWithEncoder(_block);
				}

			}
			throw new BlockParserException("Unexpected exception");
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
				if (current_step==Step.NOT_CONNECTED || current_step==Step.WAITING_FOR_CONNECTION_CONFIRMATION)
					return size;
				else
				{
					if (packetCounter.isDistantActivated())
					{
						reInitSymmetricAlgorithmIfNecessary();
					}
					return (int)encoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize;
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
					{
						return size;
					}
					case WAITING_FOR_CONNECTION_CONFIRMATION:
					case CONNECTED: {
						if (getPacketCounter().isLocalActivated())
						{
							reInitSymmetricAlgorithmIfNecessary();
						}
						return (int)decoderWithoutEncryption.getMaximumOutputLength(size+EncryptionSignatureHashEncoder.headSize);
					}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException();
		}


	}

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	@Override
	protected TransferedBlockChecker getTransferredBlockChecker(TransferedBlockChecker subBlockChecker)
			throws ConnectionException {
		try {
			needToRefreshTransferBlockChecker = false;
			/*currentBlockCheckerIsNull=true;*/
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
