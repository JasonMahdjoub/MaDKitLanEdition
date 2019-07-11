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

import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.MultiGroup;
import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import com.distrimind.util.sizeof.ObjectSizer;

import java.io.IOException;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
final class AcceptedGroups implements WithoutInnerSizeControl {

	public Group[] accepted_groups_and_requested;
	public MultiGroup accepted_groups;
	public KernelAddress kernelAddress;
	public AgentAddress distant_agent_socket_address;

	@SuppressWarnings("unused")
	AcceptedGroups()
	{
		
	}
	
	public AcceptedGroups(MultiGroup accepted_groups, Group[] accepted_groups_and_requested,
			KernelAddress _kernel_address, AgentAddress my_agent_socket_address) {
		if (accepted_groups == null)
			throw new NullPointerException("accepted_groups");
		if (accepted_groups_and_requested == null)
			throw new NullPointerException("accepted_groups_and_requested");
		if (_kernel_address == null)
			throw new NullPointerException("_kernel_address");
		if (my_agent_socket_address == null)
			throw new NullPointerException("my_agent_socket_address");
		if (accepted_groups.isEmpty())
			throw new IllegalArgumentException("accepted_groups cannot be empty : " + accepted_groups);
		this.accepted_groups_and_requested = accepted_groups_and_requested;
		this.accepted_groups = accepted_groups;
		kernelAddress = _kernel_address;
		distant_agent_socket_address = my_agent_socket_address;
	}
	
	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeInt(accepted_groups_and_requested.length);
		for (Group g : accepted_groups_and_requested)
		{
			oos.writeObject(g, false);
		}
		oos.writeObject(accepted_groups, false);
		oos.writeObject(kernelAddress, false);
		oos.writeObject(distant_agent_socket_address, false);
		
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in)
			throws ClassNotFoundException, IOException {
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int totalSize=4;
		int size=in.readInt();
		if (size<0)
			throw new MessageExternalizationException(Integrity.FAIL);
		
		if (totalSize+size*ObjectSizer.OBJREF_SIZE+ObjectSizer.OBJREF_SIZE>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		accepted_groups_and_requested=new Group[size];
		for (int i=0;i<size;i++)
		{
			accepted_groups_and_requested[i]=in.readObject(false, Group.class);
			totalSize+=accepted_groups_and_requested[i].getInternalSerializedSize();
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}
		accepted_groups=in.readObject(false, MultiGroup.class);
		totalSize+=accepted_groups.getInternalSerializedSize();
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		kernelAddress=in.readObject(false, KernelAddress.class);
		totalSize+=kernelAddress.getInternalSerializedSize();
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		distant_agent_socket_address=in.readObject(false, AgentAddress.class);
		totalSize+=distant_agent_socket_address.getInternalSerializedSize();
		if (totalSize>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		
		if (accepted_groups.isEmpty())
			throw new MessageExternalizationException(Integrity.FAIL);
		for (Group g : accepted_groups_and_requested) {
			if (g == null)
				throw new MessageExternalizationException(Integrity.FAIL);
			if (g.isUsedSubGroups())
				throw new MessageExternalizationException(Integrity.FAIL);
		}
		
	}

	

	@Override
	public String toString() {
		return "AcceptedGroups[kernelAddress=" + kernelAddress + ", distant_agent_socket_address="
				+ distant_agent_socket_address + ", accepted_groups=" + accepted_groups + "]";
	}



	@Override
	public boolean excludedFromEncryption() {
		return false;
	}

	

}
