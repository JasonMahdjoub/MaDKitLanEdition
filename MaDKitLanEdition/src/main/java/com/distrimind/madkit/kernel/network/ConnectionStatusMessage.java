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

import com.distrimind.bcfips.util.Arrays;
import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionClosedReason;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * Gives a connection status for a given IP and a given port.
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class ConnectionStatusMessage extends Message {


	private final AbstractIP address;
	final Type type;
	InetSocketAddress interface_address;
	final ConnectionClosedReason connection_closed_reason;
	protected InetSocketAddress chosenIP;
	protected transient int numberOfAnomalies=0;
	protected final transient Set<InetAddress> rejectedIps=new HashSet<>();
	protected transient long timeUTCOfAnomaliesCycle=-1;

	@Override
	public String toString() {
		if (type.equals(Type.DISCONNECT))
			return getClass().getSimpleName() + "[address=" + address + ", type=" + type + ", interface_address="
					+ interface_address + ", chosen ip=" + chosenIP + ", connectionCloseReason"
					+ connection_closed_reason + "]";
		else
			return getClass().getSimpleName() + "[address=" + address + ", type=" + type + ", interface_address="
					+ interface_address + ", chosen ip=" + chosenIP + "]";
	}

	public InetSocketAddress getChosenIP() {
		return chosenIP;
	}

	public ConnectionStatusMessage(Type _type, AbstractIP _address) {
		this(_type, _address, null, null, null);
	}

	ConnectionStatusMessage(Type _type, AbstractIP _address, InetSocketAddress chosenIP,
			InetSocketAddress _interface_address) {
		this(_type, _address, chosenIP, _interface_address, null);
	}

	ConnectionStatusMessage(Type _type, AbstractIP _address, InetSocketAddress chosenIP,
			InetSocketAddress _interface_address, ConnectionClosedReason _connection_closed_reason) {
		if (_interface_address!=null) {
			boolean ipv6 = _interface_address.getAddress() instanceof Inet6Address;
			boolean ipv6_2 = false;
			boolean ipv4_2 = false;
			for (InetAddress ia : _address.getInetAddresses()) {
				if (ia instanceof Inet6Address) {
					ipv6_2 = true;
				}
				else if (ia instanceof Inet4Address)
					ipv4_2=true;
			}
			if ((ipv6 && !ipv6_2) || (!ipv6 && !ipv4_2))
				throw new IllegalArgumentException("Incompatible address "+_address+" and interface address "+_interface_address);
		}
		address = _address;
		type = _type;
		interface_address = _interface_address;
		connection_closed_reason = _connection_closed_reason;
		this.chosenIP = chosenIP;
	}

	ConnectionStatusMessage(Type _type, AbstractIP _address, InetSocketAddress chosenIP,
							InetSocketAddress _interface_address, ConnectionClosedReason _connection_closed_reason, int numberOfAnomalies, long timeUTCOfAnomaliesCycle){
		this(_type, _address, chosenIP, _interface_address, _connection_closed_reason);
		this.numberOfAnomalies=numberOfAnomalies;
		this.timeUTCOfAnomaliesCycle=timeUTCOfAnomaliesCycle;
	}

	int getNumberOfAnomalies() {
		return numberOfAnomalies;
	}

	long getTimeUTCOfAnomaliesCycle() {
		return timeUTCOfAnomaliesCycle;
	}

	Set<InetAddress> getRejectedIps() {
		return rejectedIps;
	}

	ConnectionStatusMessage(ConnectionStatusMessage m) {
		super(m);
		address = m.address;
		type = m.type;
		interface_address = m.interface_address;
		connection_closed_reason = m.connection_closed_reason;
		this.chosenIP = m.chosenIP;
		this.numberOfAnomalies=m.numberOfAnomalies;
		this.timeUTCOfAnomaliesCycle=m.timeUTCOfAnomaliesCycle;

	}

	public AbstractIP getIP() {
		return address;
	}

	public Type getType() {
		return type;
	}

	public enum Type {
		CONNECT, DISCONNECT
	}

	protected void cleanAnomaliesIfNecessary()
	{
		if (timeUTCOfAnomaliesCycle<System.currentTimeMillis()-86400000L)
		{
			timeUTCOfAnomaliesCycle=System.currentTimeMillis();
			numberOfAnomalies=0;
			rejectedIps.clear();
		}
	}

	boolean incrementNumberOfAnomaliesAndTellsIfReconnectionIsPossible(boolean anomalyPresent, int maxAnomalies)
	{
		cleanAnomaliesIfNecessary();
		if (numberOfAnomalies==-1)
			return false;
		if (anomalyPresent)
		{
			rejectedIps.add(chosenIP.getAddress());
			if (numberOfAnomalies==0)
				timeUTCOfAnomaliesCycle=System.currentTimeMillis();

			return ++numberOfAnomalies <= maxAnomalies;
		}
		return true;
	}


}
