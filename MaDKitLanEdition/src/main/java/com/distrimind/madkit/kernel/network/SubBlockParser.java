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
import com.distrimind.util.io.*;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public abstract class SubBlockParser {
	
	
	public abstract SubBlockInfo getSubBlock(SubBlockInfo subBlockInfo) throws BlockParserException;

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


	private final EncryptionSignatureHashDecoder decoderWithEncryption;
	private final EncryptionSignatureHashDecoder decoderWithoutEncryption;
	private final EncryptionSignatureHashEncoder encoderWithEncryption;
	private final EncryptionSignatureHashEncoder encoderWithoutEncryption;
	private PacketCounterForEncryptionAndSignature packetCounter;

	public void setPacketCounter(PacketCounterForEncryptionAndSignature packetCounter) {
		this.packetCounter = packetCounter;
	}

	public SubBlockParser(EncryptionSignatureHashDecoder decoderWithEncryption, EncryptionSignatureHashDecoder decoderWithoutEncryption, EncryptionSignatureHashEncoder encoderWithEncryption, EncryptionSignatureHashEncoder encoderWithoutEncryption, PacketCounterForEncryptionAndSignature packetCounter) throws ConnectionException {
		this.decoderWithEncryption = decoderWithEncryption;
		this.decoderWithoutEncryption = decoderWithoutEncryption;
		this.encoderWithEncryption = encoderWithEncryption;
		this.encoderWithoutEncryption = encoderWithoutEncryption;
		this.packetCounter = packetCounter;
	}

	protected SubBlockInfo getEncryptedSubBlock(SubBlockInfo subBlockInfo, boolean enabledEncryption) throws BlockParserException {
		SubBlock subBlock=subBlockInfo.getSubBlock();
		try {

			if (enabledEncryption)
				enabledEncryption=EncryptionSignatureHashDecoder.isEncrypted(subBlock.getBytes(), subBlock.getOffset());
			EncryptionSignatureHashDecoder decoder;
			if (enabledEncryption) {
				decoder = decoderWithEncryption;
			} else
			{
				decoder = decoderWithoutEncryption;
			}





			if (enabledEncryption) {
				byte[] tab = new byte[subBlock.getBytes().length];
				int dl = decoder.decodeAndCheckHashAndSignaturesIfNecessary(subBlock.getBytes(), subBlock.getOffset(), subBlock.getSize(),
						tab, subBlock.getOffset()+EncryptionSignatureHashEncoder.headSize, subBlock.getSize()-EncryptionSignatureHashEncoder.headSize);
				subBlock.setBlock(tab, subBlock.getOffset() + EncryptionSignatureHashEncoder.headSize, dl);
				subBlockInfo.set(true, false);

			} else {
				int dl= decoder.decodeAndCheckHashAndSignaturesIfNecessaryWithSameInputAndOutputStreamSource(subBlock.getBytes(), subBlock.getOffset(), subBlock.getSize());
				subBlock.setOffsetAndSize(subBlock.getOffset() + EncryptionSignatureHashEncoder.headSize, dl);
				subBlockInfo.set(true, false);
			}
			return subBlockInfo;
		} catch (Exception e) {
			try {
				subBlock.setOffsetAndSize(subBlock.getOffset(),getBodyOutputSizeWithDecryption(subBlock.getSize()));
				subBlockInfo.set(false, e instanceof MessageExternalizationException && ((MessageExternalizationException) e).getIntegrity()==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				return subBlockInfo;
			} catch (IOException ioException) {
				throw new BlockParserException(e);
			}
		}
	}
	private int getBodyOutputSizeWithEncryptionImpl(int size) throws IOException {
		if (packetCounter.isDistantActivated()) {
			byte[] oec=packetCounter.getOtherEncryptionCounter();
			if (oec!=null) {
				encoderWithEncryption
						.withExternalCounter(oec);
			}
			oec= packetCounter.getOtherSignatureCounter();
			if (oec!=null)
				encoderWithEncryption.withAssociatedData(oec);
			else
				assert canAvoidSignatureCounter();
		}
		if (encoderWithEncryption.getSymmetricSecretKeyForSignature()==null && encoderWithoutEncryption.getSymmetricSecretKeyForSignature()!=null) {
			return (int) Math.max(encoderWithEncryption.getMaximumOutputLength(size), encoderWithoutEncryption.getMaximumOutputLength(size));
		}
		else
			return (int)(encoderWithEncryption.getMaximumOutputLength(size));
	}
	protected int getBodyOutputSizeWithEncryption(int size) throws IOException {
		return getBodyOutputSizeWithEncryptionImpl(size)-EncryptionSignatureHashEncoder.headSize;
	}
	protected int getBodyOutputSizeWithSignature(int size) throws IOException {
		return (int)(encoderWithoutEncryption.getMaximumOutputLength(size)-EncryptionSignatureHashEncoder.headSize);
	}
	protected int getBodyOutputSizeWithDecryption(int size) throws IOException {

		if (packetCounter.isLocalActivated()) {
			byte[] mec=packetCounter.getMyEncryptionCounter();
			if (mec!=null) {
				decoderWithEncryption.withExternalCounter(mec);
			}
			mec=packetCounter.getMySignatureCounter();
			if (mec!=null)
				decoderWithEncryption.withAssociatedData(mec);
			else
				assert canAvoidSignatureCounter();
		}

		size+=EncryptionSignatureHashEncoder.headSize;
		if (decoderWithEncryption.getSymmetricSecretKeyForSignature()==null && decoderWithoutEncryption.getSymmetricSecretKeyForSignature()!=null) {
			return (int) Math.max(decoderWithEncryption.getMaximumOutputLength(size), decoderWithoutEncryption.getMaximumOutputLength(size));
		}
		else
			return (int)(decoderWithEncryption.getMaximumOutputLength(size));
	}
	public abstract boolean canAvoidSignatureCounter();
	protected SubBlock getEncryptedParentBlock(final SubBlock _block, boolean excludeFromEncryption) throws BlockParserException {
		try {
			EncryptionSignatureHashEncoder encoder;
			if (excludeFromEncryption) {
				encoder = encoderWithoutEncryption;
			} else {
				encoder = encoderWithEncryption;
			}
			if (excludeFromEncryption) {
				byte[] tab=_block.getBytes();
				int l=encoder.encodeWithSameInputAndOutputStreamSource(tab, _block.getOffset(), _block.getSize());
				_block.setOffsetAndSize( _block.getOffset() - EncryptionSignatureHashEncoder.headSize, getBodyOutputSizeWithEncryptionImpl(_block.getSize()));
				//SubBlock res= new SubBlock(tab, _block.getOffset() - EncryptionSignatureHashEncoder.headSize, getBodyOutputSizeWithEncryptionImpl(_block.getSize()));

				Arrays.fill(tab, _block.getOffset()+l, _block.getOffset()+_block.getSize(), (byte)0);
			} else {
				int l=getBodyOutputSizeWithEncryptionImpl(_block.getSize());

				int off=_block.getOffset() - EncryptionSignatureHashEncoder.headSize;
				byte[] tab=new byte[_block.getBytes().length];

				int l2=encoder.encode(_block.getBytes(), _block.getOffset(), _block.getSize(), tab, off, l);
				Arrays.fill(tab, off+l2, off+l, (byte)0);
				_block.setBlock(tab, off, l);

			}
			return _block;
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
		Arrays.fill(tab, res.getOffset(), _block.getOffset(), (byte)0);
		Arrays.fill(tab, _block.getOffset()+ _block.getSize(), res.getOffset()+res.getSize(), (byte)0);
		return res;

	}
	protected SubBlock signOutgoingPointToPointTransferredBlockWithEncoder(SubBlock _block) throws BlockParserException
	{
		try {
			int l=encoderWithoutEncryption
					.withoutAssociatedData()
					.withoutExternalCounter()
					.encodeWithSameInputAndOutputStreamSource(_block.getBytes(), _block.getOffset(), _block.getSize());
			byte[] tab=_block.getBytes();

			_block.setOffsetAndSize( _block.getOffset() - EncryptionSignatureHashEncoder.headSize, (int) encoderWithoutEncryption.getMaximumOutputLength(_block.getSize()));
			Arrays.fill(tab, _block.getOffset()+l, _block.getOffset()+_block.getSize(), (byte)0);
			return _block;
		}
		catch(Exception e)
		{
			throw new BlockParserException(e);
		}
	}
	protected SubBlockInfo falseCheckEntrantPointToPointTransferredBlockWithoutDecoder(SubBlock _block) throws BlockParserException {
		_block.setOffsetAndSize(_block.getOffset() + getHeadSize(),
				_block.getSize() - getHeadSize());
		return new SubBlockInfo(_block, true, false);
	}

	protected SubBlockInfo checkEntrantPointToPointTransferredBlockWithDecoder(SubBlock _block) throws BlockParserException {

		try {
			Integrity integrity = decoderWithoutEncryption
					.withoutAssociatedData()
					.withoutExternalCounter()
					.checkHashAndSignatures(_block.getBytes(), _block.getOffset(), _block.getSize());
			_block.setOffsetAndSize( _block.getOffset() + getHeadSize(),(int)decoderWithoutEncryption.getLastDataLength());
			return new SubBlockInfo(_block, integrity==Integrity.OK, integrity==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		} catch (Exception e) {
			return new SubBlockInfo(new SubBlock(_block.getBytes(), _block.getOffset()+ getHeadSize(), _block.getSize()- getHeadSize()),
					false, e instanceof MessageExternalizationException && ((MessageExternalizationException) e).getIntegrity()==Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}

	}
}
