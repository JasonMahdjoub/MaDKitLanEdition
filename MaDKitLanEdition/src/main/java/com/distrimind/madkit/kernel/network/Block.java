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

import com.distrimind.madkit.exceptions.PacketException;
import com.distrimind.util.Bits;

/**
 * Represent a data block, potentially encrypted
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
public final class Block {
	public static final int BLOCK_SIZE_LIMIT = 0x200000;

	private final byte[] block;
	private int transfer_type;
	private int size;
	public Block(SubBlocksStructure _structure) throws PacketException {
		size = _structure.block_size;
		if (size > BLOCK_SIZE_LIMIT)
			throw new PacketException(
					"This block has a size (" + size + ") greater than the size limit : " + BLOCK_SIZE_LIMIT);
		block = new byte[size];
		
		Bits.putUnsignedInt24Bits(block, 0, size);
		Bits.putInt(block, 3, transfer_type);
	}



	
	
	public Block(byte[] tab, int size, SubBlocksStructure _structure, int _transfer_type) throws PacketException {
		this.size=size;
		if (size > BLOCK_SIZE_LIMIT)
			throw new PacketException(
					"This block has a size (" + size + ") greater than the size limit : " + BLOCK_SIZE_LIMIT);
		block = tab;
		transfer_type = _transfer_type;
		Bits.putUnsignedInt24Bits(block, 0, size);
		Bits.putInt(block, 3, transfer_type);
	}

	public static int getTransferID(byte [] _block)
	{
		return Bits.getInt(_block, 3);
	}
	
	
	
	public Block(byte[] _block) throws PacketException {
		block = _block;
		if (block.length < getHeadSize())
			throw new PacketException(
					"the size _block.length (" + block.length + ") must be greater than getHeadSize()");
		if (block.length > getMaximumBlockSize())
			throw new PacketException(
					"the size _block.length (" + block.length + ") must be lower or equal than getMaximumBlockSize()");
		size = getBlockSize(block, 0);
		if (size > BLOCK_SIZE_LIMIT)
			throw new PacketException(
					"This block has a size (" + size + ") greater than the size limit : " + BLOCK_SIZE_LIMIT);
		if (size < 0)
			throw new PacketException(
					"This block has a size (" + size + ") lower than 0");
		if (size != _block.length)
			throw new PacketException(
					"The given block as an invalid size (read: " + size + "; block size: " + _block.length + ")");
		transfer_type = getTransferID(block);
		
	}
	public Block(int block_size, int _transfer_type) throws PacketException {
		this.size=block_size;
		if (block_size > BLOCK_SIZE_LIMIT)
			throw new PacketException(
					"This block has a size (" + block_size + ") greater than the size limit : " + BLOCK_SIZE_LIMIT);
		if (block_size <= getHeadSize() || block_size > getMaximumBlockSize())
			throw new PacketException(
					"block_size must be greater than getHeadSize() and lower or equal than getMaximumBlockSize()");
		block = new byte[block_size];
		Bits.putUnsignedInt24Bits(block, 0, this.size);
		Bits.putInt(block, 3, _transfer_type);
		transfer_type = _transfer_type;
	}


	public void setBlockAttributes(int blockSize, int _transferType) throws PacketException
	{
		this.size=blockSize;
		if (blockSize <= getHeadSize() || blockSize > block.length)
			throw new PacketException(
					"block_size must be greater than getHeadSize() and lower or equal than getMaximumBlockSize()");
		Bits.putUnsignedInt24Bits(block, 0, this.size);
		Bits.putInt(block, 3, _transferType);
		transfer_type = _transferType;
	}
	

	
	
	
	
	public byte[] getBytes() {
		return block;
	}

	public static int getHeadSize() {
		return getBlockSizeLength()+4;
	}
	
	public static int getBlockSizeLength()
	{
		return 3;
	}

	public boolean isDirect() {
		return TransferAgent.NullIDTransfer.equals(transfer_type);
	}

	public boolean isValid() {
		return transfer_type > -2;
	}

	public int getTransferID() {
		return transfer_type;
	}

	public void setTransferID(int _id) {
		transfer_type = _id;
		Bits.putInt(block, 3, _id);
	}

	public int getBlockSize() {
		return size;
	}

	public static int getBlockSize(byte[] _bytes, int offset) {
		return Bits.getUnsignedInt24Bits(_bytes, offset);
	}

	public static int getMaximumBlockSize() {
		return BLOCK_SIZE_LIMIT;
	}

	public static int getMaximumBlockContentSize() {
		return getMaximumBlockSize() - getHeadSize();
	}
}
