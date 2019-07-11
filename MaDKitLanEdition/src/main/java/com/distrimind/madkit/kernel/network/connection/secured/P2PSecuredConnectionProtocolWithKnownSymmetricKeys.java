package com.distrimind.madkit.kernel.network.connection.secured;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java langage 

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
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.*;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.util.crypto.*;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;

/**
 * Does not support forward secrecy
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.10.0
 */
public class P2PSecuredConnectionProtocolWithKnownSymmetricKeys extends ConnectionProtocol<P2PSecuredConnectionProtocolWithKnownSymmetricKeys> {
	private enum Status{
		NOT_CONNECTED,
		WAITING_FOR_CONNECTION_CONFIRMATION,
		CONNECTED
	}
	private Status status=Status.NOT_CONNECTED;
	private P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties properties;
	private SymmetricSecretKey secretKeyForEncryption, secretKeyForSignature;
	private SymmetricAuthenticatedSignatureCheckerAlgorithm signatureChecker;
	private SymmetricAuthentifiedSignerAlgorithm signer;
	private SymmetricEncryptionAlgorithm cipher;
	private AbstractSecureRandom approvedRandom;
	private final PacketCounterForEncryptionAndSignature packetCounter;
	private boolean reinitSymmetricAlgorithm=false;
	private int signature_size_bytes=0;
	private final SubBlockParser parser;
	private boolean blockCheckerChanged = true;

	private P2PSecuredConnectionProtocolWithKnownSymmetricKeys(InetSocketAddress _distant_inet_address, InetSocketAddress _local_interface_address, ConnectionProtocol<?> _subProtocol, DatabaseWrapper sql_connection, MadkitProperties _properties, ConnectionProtocolProperties<?> cpp, int subProtocolLevel, boolean isServer, boolean mustSupportBidirectionnalConnectionInitiative) throws ConnectionException {
		super(_distant_inet_address, _local_interface_address, _subProtocol, sql_connection, _properties, cpp, subProtocolLevel, isServer, mustSupportBidirectionnalConnectionInitiative);
		this.properties=(P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties)cpp;
		try {
			approvedRandom=_properties.getApprovedSecureRandom();
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			throw new ConnectionException(e);
		}
		this.packetCounter=new PacketCounterForEncryptionAndSignature(approvedRandom, properties.enableEncryption, true);
		if (properties.enableEncryption)
			parser = new ParserWithEncryption();
		else
			parser = new ParserWithNoEncryption();

	}

	private void initSecretKeys(int id) throws ConnectionException, NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidKeyException, InvalidAlgorithmParameterException, NoSuchPaddingException {
		if (!properties.isValidProfile(id))
			throw new ConnectionException();
		this.secretKeyForEncryption=properties.getSymmetricSecretKeyForEncryption(id);
		this.secretKeyForSignature=properties.getSymmetricSecretKeyForSignature(id);
		this.signatureChecker=new SymmetricAuthenticatedSignatureCheckerAlgorithm(secretKeyForSignature);
		this.signer=new SymmetricAuthentifiedSignerAlgorithm(secretKeyForSignature);
		if (properties.enableEncryption)
			this.cipher=new SymmetricEncryptionAlgorithm(approvedRandom, secretKeyForEncryption);
		else
			this.cipher=null;

		if (!isCurrentServerAskingConnection())
			initSizeHead();


	}

	private void initSizeHead()
	{
		if (signer!=null && signature_size_bytes==0) {
			signature_size_bytes = signer.getMacLengthBytes();
		}
	}
	private void reinitSymmetricAlgorithmIfNecessary() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchProviderException, InvalidKeySpecException
	{
		if (properties.enableEncryption && reinitSymmetricAlgorithm)
		{
			reinitSymmetricAlgorithm=false;
			cipher=new SymmetricEncryptionAlgorithm(this.approvedRandom, this.secretKeyForEncryption, (byte)packetCounter.getMyEncryptionCounter().length);
		}
	}
	private void reset() {
		secretKeyForEncryption=null;
		secretKeyForSignature = null;
		signatureChecker=null;
		signer=null;
		cipher=null;


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
						int id=properties.getDefaultProfileIdentifier();
						try {
							initSecretKeys(id);
						} catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
							throw new ConnectionException(e);
						}
						return new AskConnectionWithProtocolID(false, id);
					}
					else
					{
						if (!(_m instanceof AskConnectionWithProtocolID))
							throw new ConnectionException();
						int id=((AskConnectionWithProtocolID) _m).getProtocolID();
						try {
							initSecretKeys(id);
						} catch (NoSuchProviderException | NoSuchAlgorithmException | InvalidKeySpecException | InvalidKeyException | InvalidAlgorithmParameterException | NoSuchPaddingException e) {
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
	protected TransferedBlockChecker getTransferedBlockChecker(TransferedBlockChecker subBlockChercker) throws ConnectionException {
		try {
			blockCheckerChanged = false;
			return new ConnectionProtocol.NullBlockChecker(subBlockChercker, this.isCrypted(), (short) parser.getSizeHead());

			/*if (secret_key_for_signature == null || current_step.compareTo(Step.WAITING_FOR_CONNECTION_CONFIRMATION) <= 0) {
				currentBlockCheckerIsNull = true;
				return new ConnectionProtocol.NullBlockChecker(subBlockChercker, this.isEncrypted(), (short) parser.getSizeHead());
			} else {
				currentBlockCheckerIsNull = false;
				return new BlockChecker(subBlockChercker, this.hproperties.signatureType,
						secret_key_for_signature, this.signature_size, this.isEncrypted());
			}*/
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

		@Override
		public SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException {
			switch (status) {
				case NOT_CONNECTED:
					return getSubBlockWithNoEncryption(_block);
				case WAITING_FOR_CONNECTION_CONFIRMATION:

				case CONNECTED:
					return getSubBlockWithEncryption(_block, true);

			}
			throw new BlockParserException("Unexpected exception");
		}
		public SubBlockInfo getSubBlockWithNoEncryption(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead())), true, false);
		}
		public SubBlockInfo getSubBlockWithEncryption(SubBlock _block, boolean enabledEncryption) throws BlockParserException {
			int off=_block.getOffset() + getSizeHead();
			int offr=_block.getOffset()+_block.getSize();
			boolean excludedFromEncryption=_block.getBytes()[offr-1]==1;
			if (excludedFromEncryption || !enabledEncryption)
			{
				int s= Block.getShortInt(_block.getBytes(), offr-4);
				if (s>Block.BLOCK_SIZE_LIMIT || s>_block.getSize()-getSizeHead()-4 || s<PacketPartHead.getHeadSize(false))
					throw new BlockParserException("s="+s);
				try{



					SubBlock res = new SubBlock(_block.getBytes(), off, s);
					signatureChecker.init(_block.getBytes(), _block.getOffset(),
							signature_size_bytes);
					if (getPacketCounter().isLocalActivated())
					{
						signatureChecker.update(packetCounter.getMySignatureCounter());
					}
					signatureChecker.update(_block.getBytes(),
							off, _block.getSize() - getSizeHead());
					boolean check = signatureChecker.verify();

					return new SubBlockInfo(res, check, !check);
				} catch (Exception e) {

					SubBlock res = new SubBlock(_block.getBytes(), off,
							getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
					return new SubBlockInfo(res, false, true);
				}
			}
			else
			{
				int s=_block.getSize() - getSizeHead()-4;

				try (ByteArrayInputStream bais = new ByteArrayInputStream(_block.getBytes(),
						off, s)) {

					final byte []tab=new byte[_block.getBytes().length];

					ConnectionProtocol.ByteArrayOutputStream os=new ConnectionProtocol.ByteArrayOutputStream(tab, off);


					boolean check=true;
					if (!cipher.getType().isAuthenticatedAlgorithm())
					{
						signatureChecker.init(_block.getBytes(), _block.getOffset(),
								signature_size_bytes);
						if (getPacketCounter().isLocalActivated())
						{
							signatureChecker.update(packetCounter.getMySignatureCounter());
						}
						signatureChecker.update(_block.getBytes(),
								off, _block.getSize() - getSizeHead());
						check = signatureChecker.verify();
					}
					SubBlock res;
					if (check)
					{
						if (getPacketCounter().isLocalActivated())
						{
							reinitSymmetricAlgorithmIfNecessary();
							cipher.decode(bais, os, packetCounter.getMyEncryptionCounter());
						}
						else
							cipher.decode(bais, os);
						res = new SubBlock(tab, off, os.getSize());
					}
					else
					{
						res = new SubBlock(tab, off, cipher.getOutputSizeForDecryption(s));
					}
					return new SubBlockInfo(res, check, !check);
				} catch (Exception e) {
					SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
							getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
					return new SubBlockInfo(res, false, true);
				}
			}
		}


		@Override
		public SubBlock getParentBlock(SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
			try {
				switch (status) {
					case NOT_CONNECTED:
						return getParentBlockWithNoTreatments(_block);
					case WAITING_FOR_CONNECTION_CONFIRMATION:
						if (isCurrentServerAskingConnection()) {
							try {
								return getParentBlockWithNoTreatments(_block);
							}
							finally {
								initSizeHead();
							}
						}
						else {
							return getParentBlockWithEncryption(_block, excludeFromEncryption);
						}
					case CONNECTED: {
						return getParentBlockWithEncryption(_block, excludeFromEncryption);
					}
				}

			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			throw new BlockParserException("Unexpected exception");

		}
		public SubBlock getParentBlockWithEncryption(final SubBlock _block, boolean excludeFromEncryption) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, ShortBufferException, BlockParserException, InvalidAlgorithmParameterException, IllegalStateException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, IOException, NoSuchPaddingException
		{
			final int outputSize = getBodyOutputSizeForEncryption(_block.getSize());

			int s=outputSize + getSizeHead();

			if (excludeFromEncryption)
			{
				final SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),s);
				int off=_block.getSize()+_block.getOffset();
				byte[] tab=res.getBytes();
				Arrays.fill(tab, off, outputSize+_block.getOffset()-4, (byte)0);

				int offr=res.getOffset()+res.getSize();
				tab[offr-1]=1;
				Block.putShortInt(tab, offr-4, _block.getSize());
				signer.init();
				if (packetCounter.isDistantActivated())
				{
					signer.update(packetCounter.getOtherSignatureCounter());
				}
				signer.update(res.getBytes(), _block.getOffset(),
						outputSize);

				signer.getSignature(res.getBytes(), res.getOffset());

				return res;
			}
			else
			{
				final SubBlock res = new SubBlock(new byte[_block.getBytes().length], _block.getOffset() - getSizeHead(),s);

				res.getBytes()[res.getOffset()+res.getSize()-1]=0;

				if (packetCounter.isDistantActivated())
				{
					reinitSymmetricAlgorithmIfNecessary();
					cipher.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), null, 0, 0, new ConnectionProtocol.ByteArrayOutputStream(res.getBytes(), _block.getOffset()), packetCounter.getOtherEncryptionCounter());
				}
				else
					cipher.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), null, 0, 0, new ConnectionProtocol.ByteArrayOutputStream(res.getBytes(), _block.getOffset()));

				if (!cipher.getType().isAuthenticatedAlgorithm())
				{
					signer.init();
					if (packetCounter.isDistantActivated())
					{
						signer.update(packetCounter.getOtherSignatureCounter());
					}
					signer.update(res.getBytes(), _block.getOffset(),
							outputSize);

					signer.getSignature(res.getBytes(), res.getOffset());
				}
				return res;
			}


		}
		@Override
		public int getSizeHead() {
			return P2PSecuredConnectionProtocolWithKnownSymmetricKeys.this.signature_size_bytes;
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
							return cipher.getOutputSizeForEncryption(size)+4;

					case CONNECTED:
						if (packetCounter.isDistantActivated())
						{
							reinitSymmetricAlgorithmIfNecessary();
						}
						return cipher.getOutputSizeForEncryption(size)+4;
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
						return cipher.getOutputSizeForDecryption(size-4);
					case CONNECTED:
						if (getPacketCounter().isLocalActivated())
						{
							reinitSymmetricAlgorithmIfNecessary();
						}
						return cipher.getOutputSizeForDecryption(size-4);

				}
			} catch (Exception e) {
				throw new BlockParserException(e);
			}
			return size;
		}
		public SubBlockInfo checkEntrantPointToPointTransferedBlockWithNoEncryptin(SubBlock _block) throws BlockParserException {
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					_block.getSize() - getSizeHead()), true, false);
		}

		public SubBlockInfo checkEntrantPointToPointTransferedBlockWithEncryption(SubBlock _block) throws BlockParserException {
			SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
					_block.getSize() - getSizeHead());
			try {
				boolean check = signatureChecker
						.verify(_block.getBytes(), res.getOffset(), res.getSize(), _block.getBytes(),
								_block.getOffset(), signature_size_bytes);

				return new SubBlockInfo(res, check, !check);
			} catch (Exception e) {
				return new SubBlockInfo(res, false, true);
			}

		}
		@Override
		public SubBlockInfo checkIncomingPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			switch (status) {
				case NOT_CONNECTED:

					return checkEntrantPointToPointTransferedBlockWithNoEncryptin(_block);
				case WAITING_FOR_CONNECTION_CONFIRMATION:
				case CONNECTED: {
					return checkEntrantPointToPointTransferedBlockWithEncryption(_block);
				}

			}
			throw new BlockParserException("Unexpected exception");
		}
		private SubBlock signIfPossibleSortantPointToPointTransferedBlockWithNoEncryption(SubBlock _block)
		{
			SubBlock res= new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),
					_block.getSize() + getSizeHead());
			byte[] tab=res.getBytes();
			for (int i=res.getOffset();i<_block.getOffset();i++)
				tab[i]=0;
			return res;

		}
		private SubBlock signIfPossibleSortantPointToPointTransferedBlockWithEncryption(SubBlock _block) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, ShortBufferException, InvalidAlgorithmParameterException, IllegalStateException, IOException
		{
			SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),
					_block.getSize() + getSizeHead());

			signer.sign(_block.getBytes(), _block.getOffset(), _block.getSize(),
					res.getBytes(), res.getOffset(), signature_size_bytes);
			return res;
		}
		@Override
		public SubBlock signIfPossibleOutgoingPointToPointTransferedBlock(SubBlock _block) throws BlockParserException {
			try {
				switch (status) {
					case NOT_CONNECTED:
						return signIfPossibleSortantPointToPointTransferedBlockWithNoEncryption(_block);
					case WAITING_FOR_CONNECTION_CONFIRMATION:
					{
						if (isCurrentServerAskingConnection())
							return signIfPossibleSortantPointToPointTransferedBlockWithNoEncryption(_block);
						else
							return signIfPossibleSortantPointToPointTransferedBlockWithEncryption(_block);
					}

					case CONNECTED: {
						return signIfPossibleSortantPointToPointTransferedBlockWithEncryption(_block);
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
		@Override
		public int getBodyOutputSizeForEncryption(int size) {
			return size;
		}


		@Override
		public int getBodyOutputSizeForDecryption(int _size) {
			return _size;
		}

		@Override
		public SubBlockInfo getSubBlockWithEncryption(SubBlock _block, boolean enabledEncryption) throws BlockParserException {
			try {
				SubBlock res=new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				signatureChecker.init(_block.getBytes(),
						_block.getOffset(), signature_size_bytes);
				if (getPacketCounter().isLocalActivated())
				{

					signatureChecker.update(packetCounter.getMySignatureCounter());
				}
				signatureChecker.update(res.getBytes(), res.getOffset(), res.getSize());
				boolean check = signatureChecker.verify();


				return new SubBlockInfo(res, check, !check);
			} catch (Exception e) {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getSizeHead(),
						getBodyOutputSizeForDecryption(_block.getSize() - getSizeHead()));
				return new SubBlockInfo(res, false, true);
			}
		}

		@Override
		public SubBlock getParentBlockWithEncryption(SubBlock _block, boolean excludeFromEncryption) throws InvalidKeyException, SignatureException, NoSuchAlgorithmException, InvalidKeySpecException, ShortBufferException, IllegalStateException
		{
			int outputSize=getBodyOutputSizeForEncryption(_block.getSize());
			SubBlock res= new SubBlock(_block.getBytes(), _block.getOffset() - getSizeHead(),
					outputSize + getSizeHead());
			int off=_block.getSize()+_block.getOffset();
			byte[] tab=res.getBytes();
			Arrays.fill(tab, off, outputSize+_block.getOffset(), (byte)0);


			signer.init();
			if (packetCounter.isDistantActivated())
			{
				signer.update(packetCounter.getOtherSignatureCounter());
			}
			signer.update(_block.getBytes(), _block.getOffset(), _block.getSize());

			signer.getSignature(res.getBytes(), res.getOffset());

			return res;

		}
	}
}
