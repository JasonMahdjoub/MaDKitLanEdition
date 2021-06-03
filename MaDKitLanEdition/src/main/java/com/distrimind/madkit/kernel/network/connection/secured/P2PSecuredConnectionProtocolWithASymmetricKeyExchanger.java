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

import com.distrimind.madkit.database.KeysPairs;
import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.*;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.crypto.*;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Does not support forward secrecy
 * 
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.0
 */
public class P2PSecuredConnectionProtocolWithASymmetricKeyExchanger extends ConnectionProtocol<P2PSecuredConnectionProtocolWithASymmetricKeyExchanger> {
	Step current_step = Step.NOT_CONNECTED;

	private ASymmetricKeyPair myKeyPairForEncryption = null, myKeyPairForSignature;
	private ASymmetricPublicKey distant_public_key_for_encryption = null, distant_public_key_for_signature=null;
	private SymmetricSecretKey secret_key = null;
	
	//protected SymmetricEncryptionAlgorithm symmetricEncryption = null;

	//final int signature_size_bytes;
	private final SubBlockParser parser;
	private final EncryptionSignatureHashEncoder encoderWithEncryption;
	private final EncryptionSignatureHashEncoder encoderWithoutEncryption;
	private final EncryptionSignatureHashDecoder decoderWithEncryption;
	private final EncryptionSignatureHashDecoder decoderWithoutEncryption;

	private final long aSymmetricKeySizeExpiration;
	private final P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties hProperties;
	private final ASymmetricKeyWrapperType keyWrapper;
	private final AbstractSecureRandom approvedRandom, approvedRandomForKeys;
	private boolean blockCheckerChanged = true;
	private boolean currentBlockCheckerIsNull = true;
	/*private ASymmetricAuthenticatedSignerAlgorithm signer=null;
	private ASymmetricAuthenticatedSignatureCheckerAlgorithm signatureChecker=null;*/
	private final PacketCounterForEncryptionAndSignature packetCounter;
	private boolean reInitSymmetricAlgorithm =true;

	private P2PSecuredConnectionProtocolWithASymmetricKeyExchanger(InetSocketAddress _distant_inet_address,
                                                                   InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol,
                                                                   DatabaseWrapper sql_connection, MadkitProperties mkProperties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer,
                                                                   boolean mustSupportBidirectionalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, mkProperties,cpp,
				subProtocolLevel, isServer, mustSupportBidirectionalConnectionInitiative);
		hProperties = (P2PSecuredConnectionProtocolWithASymmetricKeyExchangerProperties) super.connection_protocol_properties;

		hProperties.checkProperties();

		keyWrapper= hProperties.keyWrapper;
		if (hProperties.aSymmetricKeyExpirationMs < 0)
			aSymmetricKeySizeExpiration = hProperties.defaultASymmetricKeyExpirationMs;
		else
			aSymmetricKeySizeExpiration = hProperties.aSymmetricKeyExpirationMs;
		try {
			approvedRandom=mkProperties.getApprovedSecureRandom();
			approvedRandomForKeys=mkProperties.getApprovedSecureRandomForKeys();

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
		this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, hProperties.enableEncryption && hProperties.symmetricEncryptionType.getMaxCounterSizeInBytesUsedWithBlockMode()>0, false);
		
		if (hProperties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();
	}
	
	private void reInitSymmetricAlgorithmIfNecessary() throws ConnectionException
	{
		if (reInitSymmetricAlgorithm)
		{
			reInitSymmetricAlgorithm =false;
			try {
				byte[] ec=packetCounter.getMyEncryptionCounter();
				if (ec==null) {
					encoderWithEncryption.withSymmetricSecretKeyForEncryption(this.approvedRandom, this.secret_key);
					decoderWithEncryption.withSymmetricSecretKeyForEncryption(this.secret_key);
				}
				else {
					encoderWithEncryption.withSymmetricSecretKeyForEncryption(this.approvedRandom, this.secret_key, (byte) ec.length);
					decoderWithEncryption.withSymmetricSecretKeyForEncryption(this.secret_key, (byte) ec.length);
				}

			} catch (IOException e) {
				throw new ConnectionException(e);
			}

		}
	}

	private void setPublicPrivateKeys() throws ConnectionException {
		blockCheckerChanged |= !(myKeyPairForEncryption == null || myKeyPairForSignature==null 
				|| (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
						: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0));
		try {

			if (sql_connection != null)
			{
				myKeyPairForEncryption = sql_connection.getTableInstance(KeysPairs.class).getKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForEncryption,
						hProperties.aSymmetricEncryptionType, hProperties.aSymmetricKeySize, approvedRandomForKeys,
						aSymmetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum);
				myKeyPairForSignature = sql_connection.getTableInstance(KeysPairs.class).getKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForSignature,
						hProperties.signatureType, hProperties.aSymmetricKeySize, approvedRandomForKeys,
						aSymmetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum);
			}
			else
			{
				myKeyPairForEncryption = hProperties.aSymmetricEncryptionType
						.getKeyPairGenerator(approvedRandomForKeys, hProperties.aSymmetricKeySize).generateKeyPair();
				myKeyPairForSignature = hProperties.signatureType
						.getKeyPairGenerator(approvedRandomForKeys, hProperties.aSymmetricKeySize).generateKeyPair();
			}

			encoderWithoutEncryption.withASymmetricPrivateKeyForSignature(myKeyPairForSignature.getASymmetricPrivateKey());
			encoderWithEncryption.withASymmetricPrivateKeyForSignature(myKeyPairForSignature.getASymmetricPrivateKey());

		} catch (NoSuchAlgorithmException | DatabaseException | NoSuchProviderException | IOException e) {
			encoderWithEncryption.withoutSymmetricEncryption();
			decoderWithEncryption.withoutSymmetricEncryption();
			encoderWithEncryption.withoutSymmetricSignature();
			encoderWithoutEncryption.withoutSymmetricSignature();
			decoderWithEncryption.withoutSymmetricSignature();
			decoderWithoutEncryption.withoutSymmetricSignature();
			
			throw new ConnectionException(e);
		}

	}

	private void setNewPublicPrivateKeys() throws ConnectionException {
		blockCheckerChanged |= !(myKeyPairForEncryption == null || myKeyPairForSignature==null
				|| (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
						: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0));
		try {
			
			if (sql_connection != null)
			{
				myKeyPairForEncryption = (sql_connection.getTableInstance(KeysPairs.class).getNewKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForEncryption,
						hProperties.aSymmetricEncryptionType, hProperties.aSymmetricKeySize, approvedRandomForKeys,
						aSymmetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum));
				myKeyPairForSignature = (sql_connection.getTableInstance(KeysPairs.class).getNewKeyPair(
						distant_inet_address.getAddress(), NetworkProperties.connectionProtocolDatabaseUsingCodeForSignature,
						hProperties.signatureType, hProperties.aSymmetricKeySize, approvedRandomForKeys,
						aSymmetricKeySizeExpiration, network_properties.maximumNumberOfCryptoKeysForIpsSpectrum));
				
			}
			else
			{
				myKeyPairForEncryption = hProperties.aSymmetricEncryptionType
						.getKeyPairGenerator(approvedRandomForKeys, hProperties.aSymmetricKeySize).generateKeyPair();
				myKeyPairForSignature = hProperties.signatureType
						.getKeyPairGenerator(approvedRandomForKeys, hProperties.aSymmetricKeySize).generateKeyPair();
			}
			encoderWithoutEncryption.withASymmetricPrivateKeyForSignature(myKeyPairForSignature.getASymmetricPrivateKey());
			encoderWithEncryption.withASymmetricPrivateKeyForSignature(myKeyPairForSignature.getASymmetricPrivateKey());
		} catch (NoSuchAlgorithmException | DatabaseException | NoSuchProviderException | IOException e) {
			encoderWithEncryption.withoutSymmetricEncryption();
			decoderWithEncryption.withoutSymmetricEncryption();
			encoderWithEncryption.withoutSymmetricSignature();
			encoderWithoutEncryption.withoutSymmetricSignature();
			decoderWithEncryption.withoutSymmetricSignature();
			decoderWithoutEncryption.withoutSymmetricSignature();

			
			throw new ConnectionException(e);
		}
	}

	private void setDistantPublicKey(ASymmetricPublicKey _keyForEncryption, ASymmetricPublicKey _keyForSignature) throws ConnectionException {
		distant_public_key_for_encryption = _keyForEncryption;
		distant_public_key_for_signature = _keyForSignature;
		try
		{
			this.decoderWithEncryption.withASymmetricPublicKeyForSignature(distant_public_key_for_signature);
			this.decoderWithoutEncryption.withASymmetricPublicKeyForSignature(distant_public_key_for_signature);
		}
		catch(IOException e)
		{
			encoderWithEncryption.withoutSymmetricEncryption();
			decoderWithEncryption.withoutSymmetricEncryption();
			encoderWithEncryption.withoutSymmetricSignature();
			encoderWithoutEncryption.withoutSymmetricSignature();
			decoderWithEncryption.withoutSymmetricSignature();
			decoderWithoutEncryption.withoutSymmetricSignature();
			throw new ConnectionException(e);
		}
		
	}

	private void resetPublicPrivateKeys() {
		blockCheckerChanged = true;
		myKeyPairForEncryption = null;
		myKeyPairForSignature = null;
		distant_public_key_for_encryption = null;
		distant_public_key_for_signature = null;
		secret_key = null;
		encoderWithEncryption.withoutSymmetricEncryption();
		decoderWithEncryption.withoutSymmetricEncryption();
		encoderWithEncryption.withoutSymmetricSignature();
		encoderWithoutEncryption.withoutSymmetricSignature();
		decoderWithEncryption.withoutSymmetricSignature();
		decoderWithoutEncryption.withoutSymmetricSignature();
	}

	private enum Step {
		NOT_CONNECTED, WAITING_FOR_PUBLIC_KEY, WAITING_FOR_SECRET_KEY, WAITING_FIRST_MESSAGE, WAITING_FOR_CONNECTION_CONFIRMATION, CONNECTED,
	}

	private void generateSecretKey() throws ConnectionException {
		try {
			secret_key = hProperties.symmetricEncryptionType.getKeyGenerator(approvedRandomForKeys, hProperties.symmetricKeySizeBits)
					.generateKey();
			encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, secret_key);
			decoderWithEncryption.withSymmetricSecretKeyForEncryption(secret_key);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			secret_key = null;
			encoderWithEncryption.withoutSymmetricEncryption();
			decoderWithEncryption.withoutSymmetricEncryption();
			throw new ConnectionException(e);
		}
	}

	private WrappedEncryptedSymmetricSecretKey encodeSecretKey() throws ConnectionException {
		try {
			KeyWrapperAlgorithm kwe=new KeyWrapperAlgorithm(keyWrapper, distant_public_key_for_encryption);
			return kwe.wrap(approvedRandom, secret_key);
		} catch (IOException
				| IllegalStateException e) {
			throw new ConnectionException(e);
		}

	}

	private void decodeSecretKey(WrappedEncryptedSymmetricSecretKey _secret_key) throws ConnectionException {
		try {
			KeyWrapperAlgorithm kwe=new KeyWrapperAlgorithm(keyWrapper, myKeyPairForEncryption);
			secret_key = kwe.unwrap(_secret_key);
			encoderWithEncryption.withSymmetricSecretKeyForEncryption(approvedRandom, secret_key);
			decoderWithEncryption.withSymmetricSecretKeyForEncryption(secret_key);
		} catch (IOException | IllegalArgumentException | IllegalStateException e) {
			encoderWithEncryption.withoutSymmetricEncryption();
			decoderWithEncryption.withoutSymmetricEncryption();
			throw new ConnectionException(e);
		}
	}

	@Override
	protected ConnectionMessage getNextStep(ConnectionMessage _m) throws ConnectionException {
		switch (current_step) {
		case NOT_CONNECTED: {
			if (_m instanceof AskConnection) {
				AskConnection ask = (AskConnection) _m;
				setPublicPrivateKeys();
				if (ask.isYouAreAsking()) {
					current_step = Step.WAITING_FOR_PUBLIC_KEY;
					return new AskConnection(false);
				} else {
					current_step = Step.WAITING_FOR_PUBLIC_KEY;
					return new PublicKeyMessage(myKeyPairForEncryption.getASymmetricPublicKey(), distant_public_key_for_encryption, myKeyPairForSignature.getASymmetricPublicKey(), distant_public_key_for_signature, myKeyPairForSignature.getASymmetricPrivateKey());
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
		case WAITING_FOR_PUBLIC_KEY: {
			if (_m instanceof PublicKeyMessage) {
				if (((PublicKeyMessage) _m).getPublicKeyForEncryption().equals(myKeyPairForEncryption.getASymmetricPublicKey())
						|| ((PublicKeyMessage) _m).getPublicKeyForSignature().equals(myKeyPairForSignature.getASymmetricPublicKey())) {
					current_step = Step.WAITING_FOR_PUBLIC_KEY;
					setNewPublicPrivateKeys();
					return new SimilarPublicKeysError();
				}
				
				setDistantPublicKey(((PublicKeyMessage) _m).getPublicKeyForEncryption(), ((PublicKeyMessage) _m).getPublicKeyForSignature());

				if (isCurrentServerAskingConnection()) {
					current_step = Step.WAITING_FOR_SECRET_KEY;
					return new PublicKeyMessage(myKeyPairForEncryption.getASymmetricPublicKey(), distant_public_key_for_encryption, myKeyPairForSignature.getASymmetricPublicKey(), distant_public_key_for_signature, myKeyPairForSignature.getASymmetricPrivateKey());
				} else {

					current_step = Step.WAITING_FIRST_MESSAGE;
					generateSecretKey();
					WrappedEncryptedSymmetricSecretKey encoded_secret_key = encodeSecretKey();
					return new SecretKeyMessage(encoded_secret_key);
				}
			} else if (_m instanceof SimilarPublicKeysError) {
				setNewPublicPrivateKeys();
				return new PublicKeyMessage(myKeyPairForEncryption.getASymmetricPublicKey(), distant_public_key_for_encryption, myKeyPairForSignature.getASymmetricPublicKey(), distant_public_key_for_signature, myKeyPairForSignature.getASymmetricPrivateKey());
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
		case WAITING_FOR_SECRET_KEY: {
			if (_m instanceof SecretKeyMessage) {
				try {
					decodeSecretKey(((SecretKeyMessage) _m).secret_key);
				} catch (ConnectionException e) {
					return new IncomprehensibleSecretKey();
				}
				current_step = Step.WAITING_FIRST_MESSAGE;
				return new FirstMessage();
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
		case WAITING_FIRST_MESSAGE: {
			if (_m instanceof IncomprehensibleSecretKey) {
				generateSecretKey();
				current_step = Step.WAITING_FIRST_MESSAGE;
				WrappedEncryptedSymmetricSecretKey encoded_secret_key = encodeSecretKey();
				return new SecretKeyMessage(encoded_secret_key);
			} else if (_m instanceof FirstMessage) {
				current_step = Step.WAITING_FOR_CONNECTION_CONFIRMATION;
				if (!isCurrentServerAskingConnection()) {
					return new FirstMessage();
				} else {
					return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
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
				current_step = Step.CONNECTED;
				if (!packetCounter.setDistantCounters(((ConnectionFinished) _m).getInitialCounter()))
				{
					current_step=Step.NOT_CONNECTED;
					return new UnexpectedMessage(this.getDistantInetSocketAddress());
				}
				
				if (!isCurrentServerAskingConnection())
					return new ConnectionFinished(getDistantInetSocketAddress(), packetCounter.getMyEncodedCounters());
				else
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
				else
				{
					if (!packetCounter.setDistantCounters(((ConnectionFinished) _m).getInitialCounter()))
					{
						current_step=Step.NOT_CONNECTED;
						return new UnexpectedMessage(this.getDistantInetSocketAddress());
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
		if (_reason.equals(ConnectionClosedReason.CONNECTION_ANOMALY))
			resetPublicPrivateKeys();
		current_step = Step.NOT_CONNECTED;
	}

	private class ParserWithEncryption extends SubBlockParser {

		ParserWithEncryption() throws ConnectionException {
			super(decoderWithEncryption, decoderWithoutEncryption, encoderWithEncryption, encoderWithoutEncryption, packetCounter);
		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY:
				case WAITING_FIRST_MESSAGE: {
					return size;
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
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
				switch (current_step) {
					case NOT_CONNECTED:
					case WAITING_FOR_PUBLIC_KEY:
					case WAITING_FOR_SECRET_KEY:
					case WAITING_FIRST_MESSAGE: {
						return size;
					}
					case WAITING_FOR_CONNECTION_CONFIRMATION:
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
				switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY: {
					return size;
				}
				case WAITING_FIRST_MESSAGE: {
					if (isCurrentServerAskingConnection())
					{
						if (getPacketCounter().isLocalActivated())
						{
							reInitSymmetricAlgorithmIfNecessary();
						}
						return getBodyOutputSizeWithDecryption(size);
					}
					else
						return size;
				}
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
			return size;

		}

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {

			switch (current_step) {
			case NOT_CONNECTED:
			case WAITING_FOR_PUBLIC_KEY:
			case WAITING_FOR_SECRET_KEY: {

				return getSubBlockWithNoEncryption(_block);
			}
			case WAITING_FIRST_MESSAGE:
				if (isCurrentServerAskingConnection()) {
					return getEncryptedSubBlock(_block, true);
				} else {
					return getSubBlockWithNoEncryption(_block);
				}

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



		@Override
		public SubBlock getParentBlock(final SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY:
				case WAITING_FIRST_MESSAGE: {
					return getParentBlockWithNoTreatments(_block);
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED:
					return getEncryptedParentBlock(_block, excludeFromEncryption);
			}
			throw new BlockParserException("Unexpected exception");

		}

		@Override
		public String toString() {
			return "ParserWithEncryption{"+current_step+"}";
		}

		@Override
		public int getHeadSize() {
			return EncryptionSignatureHashEncoder.headSize;
		}


		private SubBlockInfo checkEntrantPointToPointTransferredBlockWithNoEncryption(SubBlock _block) throws BlockParserException
		{
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getHeadSize(),
					_block.getSize() - getHeadSize()), true, false);
			
		}

		
		@Override
		public SubBlockInfo checkIncomingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException {
			switch (current_step) {
				case NOT_CONNECTED:
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY: {

					return checkEntrantPointToPointTransferredBlockWithNoEncryption(_block);
				}
				case WAITING_FIRST_MESSAGE:
					if (isCurrentServerAskingConnection()) {
						return checkEntrantPointToPointTransferredBlockWithDecoder(_block);
					} else {
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
				case WAITING_FOR_PUBLIC_KEY:
				case WAITING_FOR_SECRET_KEY:
				case WAITING_FIRST_MESSAGE: {
					return falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(_block);
				}
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {

					return signOutgoingPointToPointTransferredBlockWithEncoder(_block);
				}
			}

			throw new BlockParserException("Unexpected exception");

		}

	}

	private class ParserWithNoEncryption extends ParserWithEncryption {


		ParserWithNoEncryption() throws ConnectionException {
		}

		@Override
		public int getBodyOutputSizeForEncryption(int size) throws BlockParserException {
			try {
				switch (current_step) {
					case NOT_CONNECTED:
					case WAITING_FOR_PUBLIC_KEY:
					case WAITING_FOR_SECRET_KEY:
					case WAITING_FIRST_MESSAGE: {
						return size;
					}
					case WAITING_FOR_CONNECTION_CONFIRMATION:
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
				switch (current_step) {
					case NOT_CONNECTED:
					case WAITING_FOR_PUBLIC_KEY:
					case WAITING_FOR_SECRET_KEY: {
						return size;
					}
					case WAITING_FIRST_MESSAGE: {
						if (isCurrentServerAskingConnection())
						{
							if (getPacketCounter().isLocalActivated())
							{
								reInitSymmetricAlgorithmIfNecessary();
							}
							return getBodyOutputSizeWithDecryption(size);
						}
						else
							return size;
					}
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

	@Override
	public SubBlockParser getParser() {
		return parser;
	}

	@Override
	protected TransferedBlockChecker getTransferredBlockChecker(TransferedBlockChecker subBlockChecker)
			throws ConnectionException {
		try {
			blockCheckerChanged = false;
			if (myKeyPairForEncryption == null || myKeyPairForSignature == null || (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
					: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0)) {
				currentBlockCheckerIsNull = true;
				return new ConnectionProtocol.NullBlockChecker(subBlockChecker, this.isCrypted(),
						(short) parser.getHeadSize());
			} else {
				currentBlockCheckerIsNull = false;
				return new BlockChecker(subBlockChecker, this.hProperties.signatureType,
						this.myKeyPairForSignature.getASymmetricPublicKey(), this.isCrypted(), this.hProperties.messageDigestType);
			}
		} catch (Exception e) {
			blockCheckerChanged = true;
			throw new ConnectionException(e);
		}
	}

	static class BlockChecker extends TransferedBlockChecker {

		private ASymmetricAuthenticatedSignatureType signatureType;
		private int signatureSize;
		private MessageDigestType messageDigestType;
		private transient EncryptionSignatureHashDecoder decoder;
		//private transient ASymmetricAuthenticatedSignatureCheckerAlgorithm signatureChecker;
		private ASymmetricPublicKey publicKey;
		@SuppressWarnings("unused")
		BlockChecker()
		{
			
		}
		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			super.readExternal(in);
			signatureType=in.readObject(false, ASymmetricAuthenticatedSignatureType.class);
			signatureSize=in.readInt();
			publicKey=in.readObject(false, ASymmetricPublicKey.class);
			messageDigestType=in.readObject(true);
			try
			{
				initSignature();
			}
			catch(Exception e2)
			{
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN, e2);
			}
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
			super.writeExternal(oos);
			oos.writeObject(signatureType, false);
			oos.writeInt(signatureSize);
			oos.writeObject(publicKey, false);
			oos.writeObject(messageDigestType, true);
		}
		
		@Override
		public int getInternalSerializedSize() {
			return SerializationTools.getInternalSize(signatureType)+4+ SerializationTools.getInternalSize(publicKey)+SerializationTools.getInternalSize(messageDigestType);
		}
		
		protected BlockChecker(TransferedBlockChecker _subChecker, ASymmetricAuthenticatedSignatureType signatureType,
				ASymmetricPublicKey publicKey, boolean isEncrypted, MessageDigestType messageDigestType) throws IOException {
			super(_subChecker, !isEncrypted);
			this.signatureType = signatureType;
			this.publicKey = publicKey;
			this.messageDigestType = messageDigestType;
			initSignature();
		}

		

		private void initSignature() throws IOException {
			this.decoder=new EncryptionSignatureHashDecoder()
					.withASymmetricPublicKeyForSignature(publicKey);
			if (messageDigestType!=null)
				this.decoder.withMessageDigestType(messageDigestType);
			//this.signatureChecker = new ASymmetricAuthenticatedSignatureCheckerAlgorithm(publicKey);
		}

	

		@Override
		public SubBlockInfo checkSubBlock(SubBlock _block) throws BlockParserException {
			try {
				Integrity i=decoder.checkHashAndPublicSignature(_block.getBytes(), _block.getOffset(), _block.getSize());
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + EncryptionSignatureHashEncoder.headSize,
						_block.getSize()-EncryptionSignatureHashEncoder.headSize);

				return new SubBlockInfo(res, i==Integrity.OK, i==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
		}

		

		

	}


	@Override
	public boolean isTransferBlockCheckerChangedImpl() {
		if (myKeyPairForEncryption == null || myKeyPairForSignature == null || (isCrypted() ? current_step.compareTo(Step.WAITING_FIRST_MESSAGE) <= 0
				: current_step.compareTo(Step.WAITING_FIRST_MESSAGE) < 0)) {
			return !currentBlockCheckerIsNull || blockCheckerChanged;
		} else
			return currentBlockCheckerIsNull || blockCheckerChanged;

	}

	@Override
	public PacketCounter getPacketCounter() {
		
		return packetCounter;
	}

}
