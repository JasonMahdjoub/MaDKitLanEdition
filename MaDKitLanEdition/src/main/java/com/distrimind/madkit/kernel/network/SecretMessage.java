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
import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.security.SecureRandom;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
final class SecretMessage extends KernelAddressNegotiationMessage {

	private static final int secretMessageSize = 100;

	private byte[] secretMessage;
	private AgentAddress agent_socket_address;
	private transient AgentAddress originalDistantKernelAgent = null;
	@SuppressWarnings("unused")
	SecretMessage()
	{
		
	}
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		secretMessage=in.readBytesArray(false, secretMessageSize);

		if (secretMessage.length != secretMessageSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		agent_socket_address=in.readObject(true, AgentAddress.class);
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeBytesArray(secretMessage, false, secretMessageSize);
		oos.writeObject(agent_socket_address, true);
		
	}
	
	SecretMessage(SecureRandom random, AgentAddress agent_socket_address, AgentAddress originalDistantKernelAgent) {
		if (random == null)
			throw new NullPointerException("random");
		if (originalDistantKernelAgent == null)
			throw new NullPointerException("originalDistantKernelAgent");
		if (agent_socket_address == null)
			throw new NullPointerException("agent_socket_address");
		secretMessage = new byte[secretMessageSize];
		random.nextBytes(secretMessage);
		this.agent_socket_address = agent_socket_address;
		this.originalDistantKernelAgent = originalDistantKernelAgent;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + "[agentSocketAddress=" + agent_socket_address
				+ ", originalDistantKernelAgent=" + originalDistantKernelAgent + "]";
	}

	AgentAddress getOriginalDistantKernelAgent() {
		return originalDistantKernelAgent;
	}

	AgentAddress getAgentSocketAddress() {
		return agent_socket_address;
	}

	void removeAgentSocketAddress() {
		agent_socket_address = null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (o == this)
			return true;
		if (o instanceof SecretMessage) {
			byte[] b = ((SecretMessage) o).secretMessage;
			if (b == null)
				return false;
			if (secretMessage.length != b.length)
				return false;
			for (int i = 0; i < secretMessage.length; i++) {
				if (secretMessage[i] != b[i])
					return false;
			}
			return true;
		}
		return false;
	}

	

	@Override
	public boolean excludedFromEncryption() {
		return false;
	}
	


}
