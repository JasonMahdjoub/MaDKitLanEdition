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
package com.distrimind.madkit.kernel;

import com.distrimind.madkit.message.MessageWithSilentInference;
import com.distrimind.util.AbstractDecentralizedIDGenerator;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import com.distrimind.util.io.SerializationTools;

import java.io.IOException;

/**
 * Gives the result of a big data transfer
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public final class BigDataResultMessage extends Message implements com.distrimind.madkit.util.NetworkMessage, MessageWithSilentInference {


	private long transferredData;
	private Type type;
	private int idPacket;
	private long duration;
	private AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier;
	private ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier;
	private transient boolean updateDatabase=true;
	@Override
	public int getInternalSerializedSize() {
		return super.getInternalSerializedSizeImpl()+22+(type.name().length()*2)
				+ SerializationTools.getInternalSize(externalAsynchronousBigDataIdentifier)+(externalAsynchronousBigDataIdentifier ==null?0:SerializationTools.getInternalSize(asynchronousBigDataInternalIdentifier));
	}
	
	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);
		
			
		transferredData =in.readLong();
		type=in.readObject(false, Type.class);
		idPacket=in.readInt();
		duration=in.readLong();
		externalAsynchronousBigDataIdentifier =in.readObject(true);
		if (externalAsynchronousBigDataIdentifier !=null)
			asynchronousBigDataInternalIdentifier =in.readObject(false);
		else
			asynchronousBigDataInternalIdentifier =null;
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException{
		super.writeExternal(oos);
		oos.writeLong(transferredData);
		oos.writeObject(type, false);
		oos.writeInt(idPacket);
		oos.writeLong(duration);
		oos.writeObject(externalAsynchronousBigDataIdentifier, true);
		if (externalAsynchronousBigDataIdentifier !=null)
			oos.writeObject(asynchronousBigDataInternalIdentifier, false);
	}	
	void setUpdateDatabase(boolean updateDatabase)
	{
		this.updateDatabase=updateDatabase;
	}
	boolean isUpdateDatabase()
	{
		return updateDatabase;
	}
	@SuppressWarnings("unused")
	private BigDataResultMessage()
	{
		
	}
	BigDataResultMessage(Type type, long transferredData, int idPacket, long duration, AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
			ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier) {
		if (type == null)
			throw new NullPointerException("type");
		if (type == Type.BIG_DATA_TRANSFER_DENIED && transferredData != 0)
			throw new IllegalArgumentException("transferredData must be equal to 0");
		this.type = type;
		this.transferredData = transferredData;
		this.idPacket = idPacket;
		this.duration = duration;
		if ((asynchronousBigDataInternalIdentifier ==null)!=(externalAsynchronousBigDataIdentifier ==null))
			throw new NullPointerException();
		this.asynchronousBigDataInternalIdentifier = asynchronousBigDataInternalIdentifier;
		this.externalAsynchronousBigDataIdentifier = externalAsynchronousBigDataIdentifier;
	}

	/**
	 * Gets the transfer duration in milliseconds
	 * 
	 * @return the transfer duration in milliseconds
	 */
	public long getTransferDuration() {
		return duration;
	}

	/**
	 * Gets the result type
	 * 
	 * @return the result type
	 * @see Type
	 */
	public Type getType() {
		return type;
	}

	/**
	 * Gets the total data transferred in bytes
	 * 
	 * @return the total data transferred in bytes
	 */
	public long getTransferredDataLength() {
		return transferredData;
	}
	void setTransferredDataLength(long transferredData) {
		if (transferredData<this.transferredData)
			throw new IllegalArgumentException();
		this.transferredData=transferredData;
	}
	@SuppressWarnings("unused")
	int getIDPacket() {
		return idPacket;
	}

	public enum Type {
		/**
		 * The big data was entirely transferred.
		 */
		BIG_DATA_TRANSFERRED,
		/**
		 * when a problem occurs during the data writing into the distant peer. The
		 * associated transferred data length is precise and exact.
		 */
		BIG_DATA_PARTIALLY_TRANSFERRED,

		/**
		 * when the received data was invalid or if hash tag was invalid.
		 */
		BIG_DATA_CORRUPTED,

		/**
		 * when the connection is lost. The associated transferred data length is
		 * approximate.
		 */
		CONNECTION_LOST,

		/**
		 * The proposed big data transfer is rejected
		 */
		BIG_DATA_TRANSFER_DENIED,

		/**
		 * The was canceled during its transfer
		 */
		TRANSFER_CANCELED,

		/**
		 * Asynchronous has been automatically canceled because it takes too much time
		 */
		TIME_OUT;

		public boolean isCanceled()
		{
			return this==TIME_OUT || this==TRANSFER_CANCELED;
		}
	}
	
	public String toString()
	{
		return "BigDataResultMessage[type="+type+", dataTransferredInBytes="+ getTransferredDataLength()+", durationInMs="+getTransferDuration()/*+", sender="+getSender()+", receiver="+getReceiver()*/+"]";
	}

	@SuppressWarnings("unused")
	AbstractDecentralizedIDGenerator getAsynchronousBigDataInternalIdentifier() {
		return asynchronousBigDataInternalIdentifier;
	}

	public ExternalAsynchronousBigDataIdentifier getExternalAsynchronousBigDataIdentifier() {
		return externalAsynchronousBigDataIdentifier;
	}

	public boolean isAsynchronousMessage()
	{
		return externalAsynchronousBigDataIdentifier !=null;
	}
}
