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

import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.util.io.*;

import java.io.IOException;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
class TransferPropositionWithoutInnerSizeControl extends BroadcastableWithoutInnerSizeControl {


	private KernelAddress kernelAddressToConnect;
	private SecureExternalizable attachedData;
	private IDTransfer idTransfer;
	private int numberOfIntermediatePeers;
	private boolean finalTestResult = true;
	private boolean youAskConnection;
	@SuppressWarnings("unused")
	TransferPropositionWithoutInnerSizeControl()
	{
		
	}
	@Override
	public int getInternalSerializedSize() {
		
		return super.getInternalSerializedSize()+kernelAddressToConnect.getInternalSerializedSize()+(attachedData==null?1:attachedData.getInternalSerializedSize()+1)+idTransfer.getInternalSerializedSize()+6;
	}


	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		int totalSize=0;
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;

		kernelAddressToConnect=in.readObject(false, KernelAddress.class);
		totalSize+=kernelAddressToConnect.getInternalSerializedSize()+1;
		attachedData=in.readObject(false, SecureExternalizable.class);
		/*if (attachedData==null)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);*/
		totalSize+=attachedData.getInternalSerializedSize();
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		idTransfer=in.readObject( false, IDTransfer.class);
		totalSize+=idTransfer.getInternalSerializedSize();
		
		if (idTransfer.equals(TransferAgent.NullIDTransfer))
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		numberOfIntermediatePeers=in.readInt();
		finalTestResult=in.readBoolean();
		youAskConnection=in.readBoolean();
		if (numberOfIntermediatePeers < 0)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		totalSize+=6;
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		super.writeExternal(oos);
		oos.writeObject(kernelAddressToConnect, false);
		oos.writeObject(attachedData, true);
		oos.writeObject( idTransfer, false);
		oos.writeInt(numberOfIntermediatePeers);
		oos.writeBoolean(finalTestResult);
		oos.writeBoolean(youAskConnection);
	}
	
	
	TransferPropositionWithoutInnerSizeControl(IDTransfer idTransferDestinationUsedForBroadcast, IDTransfer idTransfer,
											   KernelAddress kernelAddressToConnect, KernelAddress kernelAddressDestination, int numberOfIntermediatePeers,
											   SecureExternalizable attachedData, boolean youAskConnection) {
		super(idTransferDestinationUsedForBroadcast, kernelAddressDestination);
		if (idTransfer == null)
			throw new NullPointerException("idTransfer");
		if (idTransfer.equals(TransferAgent.NullIDTransfer))
			throw new IllegalArgumentException("idTransfer cannot be equals to TransferAgent.NullIDTransfer");
		if (kernelAddressToConnect == null)
			throw new NullPointerException("kernelAddressToConnect");

		this.idTransfer = idTransfer;
		this.kernelAddressToConnect = kernelAddressToConnect;
		this.attachedData = attachedData;
		this.numberOfIntermediatePeers = numberOfIntermediatePeers;
		this.youAskConnection = youAskConnection;
	}

	void addIntermediateTestResult(boolean intermediateTestResult) {
		this.finalTestResult &= intermediateTestResult;
	}

	boolean getFinalTestResult() {
		return finalTestResult;
	}

	int getNumberOfIntermediatePeers() {
		return numberOfIntermediatePeers;
	}

	IDTransfer getIdTransfer() {
		return idTransfer;
	}

	KernelAddress getKernelAddressToConnect() {
		return kernelAddressToConnect;
	}

	SecureExternalizable getAttachedDataForConnection() {
		return attachedData;
	}

	

	boolean isYouAskConnection() {
		return youAskConnection;
	}

	@Override
	public boolean excludedFromEncryption() {
		return false;
	}

	
	
}
