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
package com.distrimind.madkit.kernel.network;

import java.io.IOException;
import java.util.Arrays;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.network.connection.secured.PacketCounterForEncryptionAndSignature;
import com.distrimind.util.crypto.EncryptionSignatureHashDecoder;
import com.distrimind.util.crypto.EncryptionSignatureHashEncoder;
import com.distrimind.util.crypto.SymmetricSecretKey;
import com.distrimind.util.io.*;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public abstract class SubBlockParser {
	
	
	public abstract SubBlockInfo getSubBlock(SubBlock _block) throws BlockParserException;

	public abstract SubBlock getParentBlock(SubBlock _block, boolean excludedFromEncryption) throws BlockParserException;

	protected final SubBlock getParentBlockWithNoTreatments(SubBlock _block) throws BlockParserException {
		int outputSize=getBodyOutputSizeForEncryption(_block.getSize());
		SubBlock res= new SubBlock(_block.getBytes(), _block.getOffset() - getHeadSize(),
				outputSize + getHeadSize());
		int off=_block.getSize()+_block.getOffset();
		byte[] tab=res.getBytes();
		Arrays.fill(tab, off, outputSize+_block.getOffset(), (byte)0);
		Arrays.fill(tab, res.getOffset(), _block.getOffset(), (byte)0);
		return res;
	}

	public abstract int getHeadSize() throws BlockParserException;

	public abstract int getBodyOutputSizeForEncryption(int size) throws BlockParserException;
	public int getBodyOutputSizeForSignature(int size) throws BlockParserException
	{
		return getBodyOutputSizeForEncryption(size);
	}


	public abstract int getBodyOutputSizeForDecryption(int size) throws BlockParserException;
	
	public abstract SubBlockInfo checkIncomingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException;
	
	public abstract SubBlock signIfPossibleOutgoingPointToPointTransferredBlock(SubBlock _block) throws BlockParserException;

	private static final byte[] emptyTab=new byte[0];
	private final RandomByteArrayInputStream rbis;
	private final LimitedRandomInputStream lrim;
	private final RandomByteArrayOutputStream rout;
	private final LimitedRandomOutputStream lrout;
	private final EncryptionSignatureHashDecoder decoderWithEncryption;
	private final EncryptionSignatureHashDecoder decoderWithoutEncryption;
	private final EncryptionSignatureHashEncoder encoderWithEncryption;
	private final EncryptionSignatureHashEncoder encoderWithoutEncryption;
	private PacketCounterForEncryptionAndSignature packetCounter;

	public void setPacketCounter(PacketCounterForEncryptionAndSignature packetCounter) {
		this.packetCounter = packetCounter;
	}

	public SubBlockParser(EncryptionSignatureHashDecoder decoderWithEncryption, EncryptionSignatureHashDecoder decoderWithoutEncryption, EncryptionSignatureHashEncoder encoderWithEncryption, EncryptionSignatureHashEncoder encoderWithoutEncryption, PacketCounterForEncryptionAndSignature packetCounter) throws ConnectionException {
		this.rbis = new RandomByteArrayInputStream(emptyTab);
		this.rout = new RandomByteArrayOutputStream();
		try {
			this.lrim=new LimitedRandomInputStream(rbis, 0 );
			this.lrout=new LimitedRandomOutputStream(rout, 0);
		} catch (IOException e) {
			throw new ConnectionException(e);
		}
		this.decoderWithEncryption = decoderWithEncryption;
		this.decoderWithoutEncryption = decoderWithoutEncryption;
		this.encoderWithEncryption = encoderWithEncryption;
		this.encoderWithoutEncryption = encoderWithoutEncryption;
		this.packetCounter = packetCounter;
	}

	protected SubBlockInfo getEncryptedSubBlock(SubBlock _block, boolean enabledEncryption) throws BlockParserException {
		try {
			try {
				rbis.init(_block.getBytes());
				lrim.init(rbis, _block.getOffset(), _block.getSize());

				if (enabledEncryption)
					enabledEncryption=EncryptionSignatureHashDecoder.isEncrypted(lrim);

				EncryptionSignatureHashDecoder decoder;
				if (enabledEncryption) {
					decoder = decoderWithEncryption;
					if (packetCounter.isLocalActivated()) {
						byte[] mec=packetCounter.getMyEncryptionCounter();
						if (mec!=null)
							decoder.withExternalCounter(mec);
					}
				} else
					decoder = decoderWithoutEncryption;
				decoder.withRandomInputStream(lrim);

				if (packetCounter.isLocalActivated() && packetCounter.getMySignatureCounter()!=null) {
					decoder.withAssociatedData(packetCounter.getMySignatureCounter());
				}

				if (decoder.isEncrypted()) {
					byte[] tab = new byte[_block.getBytes().length];
					try {
						rout.init(tab);
						lrout.init(rout, _block.getOffset()+EncryptionSignatureHashEncoder.headSize, _block.getSize()-EncryptionSignatureHashEncoder.headSize);
						int dl = (int) decoder.decodeAndCheckHashAndSignaturesIfNecessary(lrout);
						return new SubBlockInfo(new SubBlock(tab, _block.getOffset() + EncryptionSignatureHashEncoder.headSize, dl), true, false);
					} finally {
						rout.init(emptyTab);
						lrout.init(rout, 0);
					}
				} else {
					Integrity integrity = decoder.checkHashAndSignatures();
					int dl = (int) decoder.getDataSizeInBytesAfterDecryption();
					return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + EncryptionSignatureHashEncoder.headSize, dl), integrity == Integrity.OK, integrity == Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				}
			}
			finally {
				rbis.init(emptyTab);
				lrim.init(rbis, 0);
			}
		} catch (Exception e) {
			try {
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset(),
						getBodyOutputSizeWithDecryption(_block.getSize()));
				return new SubBlockInfo(res, false, e instanceof MessageExternalizationException && ((MessageExternalizationException) e).getIntegrity()==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			} catch (IOException ioException) {
				throw new BlockParserException(e);
			}
		}
	}
	private int getBodyOutputSizeWithEncryptionImpl(int size) throws IOException {
		SymmetricSecretKey sske=encoderWithEncryption.getSymmetricSecretKeyForEncryption();
		if (sske!=null && !sske.useEncryptionAlgorithm())
			return (int)(encoderWithEncryption.getMaximumOutputLength(size));
		else
			return (int)(encoderWithoutEncryption.getMaximumOutputLength(size));
	}
	protected int getBodyOutputSizeWithEncryption(int size) throws IOException {
		return getBodyOutputSizeWithEncryptionImpl(size)-EncryptionSignatureHashEncoder.headSize;
	}
	protected int getBodyOutputSizeWithSignature(int size) throws IOException {
		return (int)(encoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize);
	}
	protected int getBodyOutputSizeWithDecryption(int size) throws IOException {
		SymmetricSecretKey sske=encoderWithEncryption.getSymmetricSecretKeyForEncryption();
		if (sske!=null && !sske.useEncryptionAlgorithm())
			return (int)decoderWithEncryption.getMaximumOutputLength(size+EncryptionSignatureHashEncoder.headSize);
		else
			return (int)decoderWithoutEncryption.getMaximumOutputLength(size+EncryptionSignatureHashEncoder.headSize);
	}

	protected SubBlock getEncryptedParentBlock(final SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
		try {
			EncryptionSignatureHashEncoder encoder;
			if (excludeFromEncryption) {
				encoder = encoderWithoutEncryption;
			} else {
				encoder = encoderWithEncryption;
				if (packetCounter.isDistantActivated()) {
					byte[] oec=packetCounter.getOtherEncryptionCounter();
					if (oec!=null)
						encoder.withExternalCounter(oec);
				}
			}
			if (packetCounter.isDistantActivated() && packetCounter.getMySignatureCounter()!=null)
				encoder.withAssociatedData(packetCounter.getOtherSignatureCounter());
			if (excludeFromEncryption) {
				byte[] tab=_block.getBytes();
				int l=encoder.encodeWithSameInputAndOutputStreamSource(tab, _block.getOffset(), _block.getSize());
				SubBlock res= new SubBlock(tab, _block.getOffset() - EncryptionSignatureHashEncoder.headSize, getBodyOutputSizeWithEncryptionImpl(_block.getSize()));

				for (int i=res.getOffset()+l, m=res.getOffset()+res.getSize();i<m;i++)
					tab[i]=0;
				return res;
			} else {
				int l=getBodyOutputSizeWithEncryptionImpl(_block.getSize());

				int off=_block.getOffset() - EncryptionSignatureHashEncoder.headSize;
				byte[] tab=new byte[_block.getBytes().length];
				SubBlock res = new SubBlock(tab, off, l);
				l=encoder.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), res.getBytes(), res.getOffset(), res.getSize());
				for (int i=res.getOffset()+l, m=res.getOffset()+res.getSize();i<m;i++)
					tab[i]=0;
				return res;
			}
		}
		catch (Exception e)
		{
			throw new BlockParserException(e);
		}
	}

	protected SubBlock falseSignIfPossibleOutgoingPointToPointTransferredBlockWithoutEncoder(SubBlock _block) throws BlockParserException {
		SubBlock res= new SubBlock(_block.getBytes(), _block.getOffset() - getHeadSize(),
				_block.getSize() + getHeadSize());
		byte[] tab=res.getBytes();
		for (int i=res.getOffset();i<_block.getOffset();i++)
			tab[i]=0;
		return res;

	}
	protected SubBlock signOutgoingPointToPointTransferredBlockWithEncoder(SubBlock _block) throws BlockParserException
	{
		try {
			encoderWithoutEncryption
					.withoutAssociatedData()
					.withoutExternalCounter()
					.encodeWithSameInputAndOutputStreamSource(_block.getBytes(), _block.getOffset(), _block.getSize());
			return new SubBlock(_block.getBytes(), _block.getOffset() - EncryptionSignatureHashEncoder.headSize, (int) encoderWithoutEncryption.getMaximumOutputLength(_block.getSize()));
		}
		catch(Exception e)
		{
			throw new BlockParserException(e);
		}
	}
	protected SubBlockInfo falseCheckEntrantPointToPointTransferredBlockWithoutDecoder(SubBlock _block) throws BlockParserException {
		return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset() + getHeadSize(),
				_block.getSize() - getHeadSize()), true, false);
	}

	protected SubBlockInfo checkEntrantPointToPointTransferredBlockWithDecoder(SubBlock _block) throws BlockParserException {

		try {
			try {
				rbis.init(_block.getBytes());
				lrim.init(rbis, _block.getOffset(), _block.getSize());

				Integrity integrity = decoderWithoutEncryption
						.withoutAssociatedData()
						.withoutExternalCounter()
						.withRandomInputStream(lrim)
						.checkHashAndSignatures();
				SubBlock res = new SubBlock(_block.getBytes(), _block.getOffset() + getHeadSize(),
						(int)decoderWithoutEncryption.getDataSizeInBytesBeforeDecryption());

				return new SubBlockInfo(res, integrity==Integrity.OK, integrity==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
			finally {
				rbis.init(emptyTab);
				lrim.init(rbis, 0);
			}
		} catch (Exception e) {
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset()+ getHeadSize(), _block.getSize()- getHeadSize()),
					false, e instanceof MessageExternalizationException && ((MessageExternalizationException) e).getIntegrity()==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}

	}
}
