package com.distrimind.madkit.kernel.network.connection.secured;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java language

This software is governed by the CeCILL-C license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL-C
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL-C license and that you accept its terms.
 */

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.PacketCounter;
import com.distrimind.madkit.kernel.network.SubBlock;
import com.distrimind.madkit.kernel.network.SubBlockInfo;
import com.distrimind.madkit.kernel.network.SubBlockParser;
import com.distrimind.madkit.kernel.network.connection.*;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.EncryptionSignatureHashDecoder;
import com.distrimind.util.crypto.EncryptionSignatureHashEncoder;
import com.distrimind.util.crypto.SymmetricSecretKey;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Does not support forward secrecy
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MaDKitLanEdition 1.10.0
 */
public class P2PSecuredConnectionProtocolWithKnownSymmetricKeys extends ConnectionProtocol<P2PSecuredConnectionProtocolWithKnownSymmetricKeys> {
	private enum Status{
		NOT_CONNECTED,
		WAITING_FOR_CONNECTION_CONFIRMATION,
		CONNECTED
	}
	private final EncryptionSignatureHashEncoder encoderWithEncryption;
	private final EncryptionSignatureHashEncoder encoderWithoutEncryption;
	private final EncryptionSignatureHashDecoder decoderWithEncryption;
	private final EncryptionSignatureHashDecoder decoderWithoutEncryption;

	private Status status=Status.NOT_CONNECTED;
	private final P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties properties;
	private SymmetricSecretKey secretKeyForEncryption, secretKeyForSignature;
	/*private SymmetricAuthenticatedSignatureCheckerAlgorithm signatureChecker;
	private SymmetricAuthenticatedSignerAlgorithm signer;
	private SymmetricEncryptionAlgorithm cipher;*/
	private final AbstractSecureRandom approvedRandom;
	private PacketCounterForEncryptionAndSignature packetCounter=null;
	private boolean reInitSymmetricAlgorithm =false;
	private final SubBlockParser parser;
	private boolean blockCheckerChanged = true;

	private P2PSecuredConnectionProtocolWithKnownSymmetricKeys(InetSocketAddress _distant_inet_address, InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol, DatabaseWrapper sql_connection, MadkitProperties _properties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer, boolean mustSupportBidirectionalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, _properties, cpp, subProtocolLevel, isServer, mustSupportBidirectionalConnectionInitiative);
		this.properties=(P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties)cpp;
		try {
			approvedRandom=_properties.getApprovedSecureRandom();
			encoderWithEncryption=new EncryptionSignatureHashEncoder();
			encoderWithoutEncryption=new EncryptionSignatureHashEncoder();
			decoderWithEncryption=new EncryptionSignatureHashDecoder();
			decoderWithoutEncryption=new EncryptionSignatureHashDecoder();
			if (properties.messageDigestType!=null) {
				encoderWithEncryption.withMessageDigestType(properties.messageDigestType);
				encoderWithoutEncryption.withMessageDigestType(properties.messageDigestType);
				decoderWithEncryption.withMessageDigestType(properties.messageDigestType);
				decoderWithoutEncryption.withMessageDigestType(properties.messageDigestType);
			}
			encoderWithEncryption.connectWithDecoder(decoderWithEncryption);
			encoderWithoutEncryption.connectWithDecoder(decoderWithoutEncryption);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			throw new ConnectionException(e);
		}

		if (properties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();

	}

	private void initSecretKeys(int id) throws ConnectionException {
		if (!properties.isValidProfile(id, network_properties.encryptionRestrictionForConnectionProtocols))
			throw new ConnectionException();
		this.secretKeyForEncryption=properties.getSymmetricSecretKeyForEncryption(id);
		this.secretKeyForSignature=properties.getSymmetricSecretKeyForSignature(id);
		try {
			encoderWithoutEncryption.withSymmetricSecretKeyForSignature(secretKeyForSignature);
			encoderWithEncryption.withSymmetricSecretKeyForSignature(secretKeyForSignature);
			decoderWithoutEncryption.withSymmetricSecretKeyForSignature(secretKeyForSignature);
			decoderWithEncryption.withSymmetricSecretKeyForSignature(secretKeyForSignature);
			if (properties.enableEncryption) {
				encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, secretKeyForEncryption);
				decoderWithEncryption.withSymmetricSecretKeyForEncryption(secretKeyForEncryption);
			} else {
				encoderWithEncryption.withoutSymmetricEncryption();
				decoderWithEncryption.withoutSymmetricEncryption();
			}
			this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, properties.enableEncryption && secretKeyForEncryption.getEncryptionAlgorithmType().getMaxCounterSizeInBytesUsedWithBlockMode()>0, true);

		}
		catch (IOException e)
		{
			throw new ConnectionException(e);
		}

	}

	private void reInitSymmetricAlgorithmIfNecessary() throws BlockParserException
	{
		try {
			if (properties.enableEncryption && reInitSymmetricAlgorithm) {
				reInitSymmetricAlgorithm = false;
				encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, secretKeyForEncryption);
				decoderWithEncryption.withSymmetricSecretKeyForEncryption(secretKeyForEncryption);
			}
		}
		catch (IOException e)
		{
			throw new BlockParserException(e);
		}
	}
	private void reset() {
		secretKeyForEncryption=null;
		secretKeyForSignature = null;
		encoderWithEncryption.withoutSymmetricEncryption();
		decoderWithEncryption.withoutSymmetricEncryption();
		encoderWithEncryption.withoutSymmetricSignature();
		encoderWithoutEncryption.withoutSymmetricSignature();
		decoderWithEncryption.withoutSymmetricSignature();
		decoderWithoutEncryption.withoutSymmetricSignature();


	}
	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) throws ConnectionException {
		switch (status)
		{
			case NOT_CONNECTED:
				if (_m instanceof AskConnection)
				{
					if (this.isCurrentServerAskingConnection())
					{
						status=Status.WAITING_FOR_CONNECTION_CONFIRMATION;
						int id=properties.getDefaultProfileIdentifier(network_properties.encryptionRestrictionForConnectionProtocols);
						initSecretKeys(id);
						return new AskConnectionWithProtocolID(false, id);
					}
					else
					{
						if (!(_m instanceof AskConnectionWithProtocolID))
							throw new ConnectionException();
						int id=((AskConnectionWithProtocolID) _m).getProtocolID();
						try {
							initSecretKeys(id);
						} catch (Exception e) {
							status=Status.NOT_CONNECTED;
							return new UnexpectedMessage(distant_inet_address);

						}
						status=Status.WAITING_FOR_CONNECTION_CONFIRMATION;
						return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
					}
				}
				else if (_m instanceof ConnectionFinished) {

					if (((ConnectionFinished) _m).getState()
							.equals(ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED)) {
						return new UnexpectedMessage(this.getDistantInetSocketAddress());
					} else
						return new ConnectionFinished(this.getDistantInetSocketAddress(),
								ConnectionClosedReason.CONNECTION_ANOMALY);
				} else {

					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				}

			case WAITING_FOR_CONNECTION_CONFIRMATION:
				if (_m instanceof ConnectionFinished)
				{
					ConnectionFinished cf=(ConnectionFinished)_m;
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
							status= Status.NOT_CONNECTED;
							return new UnexpectedMessage(this.getDistantInetSocketAddress());
						}
						else {
							blockCheckerChanged=true;
							status = Status.CONNECTED;
							if (isCurrentServerAskingConnection())
								return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
							else
								return null;
						}
					}

				}
				else {
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				}
			case CONNECTED:
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
		return null;
	}

	@Override
	protected void closeConnection(ConnectionClosedReason _reason) {
		if (_reason.equals(ConnectionClosedReason.CONNECTION_ANOMALY))
			reset();
		status = Status.NOT_CONNECTED;
	}

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	@Override
	protected TransferedBlockChecker getTransferredBlockChecker(TransferedBlockChecker subBlockChecker) throws ConnectionException {
		try {
			blockCheckerChanged = false;
			return new ConnectionProtocol.NullBlockChecker(subBlockChecker, this.isCrypted(), (short) parser.getHeadSize());


		} catch (Exception e) {
			blockCheckerChanged = true;
			throw new ConnectionException(e);
		}
	}

	@Override
	protected boolean isTransferBlockCheckerChangedImpl() {
		if (secretKeyForEncryption == null || secretKeyForSignature==null || status!=Status.CONNECTED) {
			return blockCheckerChanged;
		} else
			return true;
	}

	@Override
	public PacketCounter getPacketCounter() {
		return packetCounter;
	}

	private class ParserWithEncryption extends SubBlockParser
	{

		ParserWithEncryption() throws ConnectionException {
			super(decoderWithEncryption, decoderWithoutEncryption, encoderWithEncryption, encoderWithoutEncryption, packetCounter);
		}
		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {
			switch (status) {
				case NOT_CONNECTED:
					return getSubBlockWithNoEncryption(_block);
				case WAITING_FOR_CONNECTION_CONFIRMATION:

				case CONNECTED:
					return getEncryptedSubBlock(_block, true);

			}
			throw new BlockParserException("Unexpected exception");
		}
		public SubBlockInfo getSubBlockWithNoEncryption(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getHeadSize(),
					getBodyOutputSizeForDecryption(_block.getSize() - getHeadSize())), true, false);
		}


		@Override
		public SubBlock getParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			switch (status) {
				case NOT_CONNECTED:
					return getParentBlockWithNoTreatments(_block);
				case WAITING_FOR_CONNECTION_CONFIRMATION:
					if (isCurrentServerAskingConnection()) {
						return getParentBlockWithNoTreatments(_block);
					}
					else {
						return getEncryptedParentBlock(_block, excludeFromEncryption);
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
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				switch (status) {
					case NOT_CONNECTED:
						return size;
					case WAITING_FOR_CONNECTION_CONFIRMATION:
						if (isCurrentServerAskingConnection())
							return size;
						else
							return getBodyOutputSizeWithEncryption(size);

					case CONNECTED:
						if (packetCounter.isDistantActivated())
						{
							reInitSymmetricAlgorithmIfNecessary();
						}
						return getBodyOutputSizeWithEncryption(size);
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			return size;
		}
		@Override
		public int getBodyOutputSizeForSignature(int size) throws BlockParserException
		{
			try {
				switch (status) {
					case NOT_CONNECTED:
						return size;
					case WAITING_FOR_CONNECTION_CONFIRMATION:
						if (isCurrentServerAskingConnection())
							return size;
						else
							return getBodyOutputSizeWithSignature(size);

					case CONNECTED:
						if (packetCounter.isDistantActivated())
						{
							reInitSymmetricAlgorithmIfNecessary();
						}
						return getBodyOutputSizeWithSignature(size);
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			return size;
		}

		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (status) {
					case NOT_CONNECTED:
						return size;
					case WAITING_FOR_CONNECTION_CONFIRMATION:
						return getBodyOutputSizeWithDecryption(size);
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
			return size;
		}

		@Override
		public SubBlockInfo checkIncomingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			switch (status) {
				case NOT_CONNECTED:

					return falseCheckEntrantPointToPointTransferredBlockWithoutDecoder(_block);
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					return checkEntrantPointToPointTransferredBlockWithDecoder(_block);
				}

			}
			throw new BlockParserException("Unexpected exception");
		}
		@Override
		public SubBlock signIfPossibleOutgoingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			try {
				switch (status) {
					case NOT_CONNECTED:
						return falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(_block);
					case WAITING_FOR_CONNECTION_CONFIRMATION:
					{
						if (isCurrentServerAskingConnection())
							return falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(_block);
						else
							return signOutgoingPointToPointTransferredBlockWithEncoder(_block);
					}

					case CONNECTED: {
						return signOutgoingPointToPointTransferredBlockWithEncoder(_block);
					}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException("Unexpected exception");
		}

	}
	private class ParserWithNoEncryption extends ParserWithEncryption
	{
		ParserWithNoEncryption() throws ConnectionException {
			super();
		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				switch (status) {
					case NOT_CONNECTED:
						return size;
					case WAITING_FOR_CONNECTION_CONFIRMATION:
						if (isCurrentServerAskingConnection())
							return size;
						else
							return getBodyOutputSizeWithEncryption(size);

					case CONNECTED:
						if (packetCounter.isDistantActivated())
						{
							reInitSymmetricAlgorithmIfNecessary();
						}
						return getBodyOutputSizeWithEncryption(size);
				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			return size;
		}

		@Override
		public int getBodyOutputSizeForDecryption(int size) throws BlockParserException {
			try {
				switch (status) {
					case NOT_CONNECTED:
						return size;
					case WAITING_FOR_CONNECTION_CONFIRMATION:
						return getBodyOutputSizeWithDecryption(size);
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
			return size;
		}
		@Override
		protected SubBlockInfo getEncryptedSubBlock(SubBlock _block, boolean enabledEncryption) throws BlockParserException {
			return super.getEncryptedSubBlock(_block, false);
		}

		@Override
		protected SubBlock getEncryptedParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			return super.getEncryptedParentBlock(_block, true);
		}
	}
}
