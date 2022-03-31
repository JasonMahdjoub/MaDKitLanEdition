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

import com.distrimind.madkit.kernel.network.LocalNetworkAgent.PossibleAddressForDirectConnection;
import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.3
 * @since MadkitLanEdition 1.0
 *
 */
public class ConnectionInfoSystemMessage implements SystemMessageWithoutInnerSizeControl {

	private ArrayList<AbstractIP> addresses;
	private AbstractIP localAddresses;
	private int manualPortToConnect;
	private int localPortToConnect;
	private boolean canBeDirectServer;

	@SuppressWarnings("unused")
	ConnectionInfoSystemMessage()
	{
		
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int size=in.readInt();
		int totalSize=4;
		if (size<0 || totalSize+size*4>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		addresses=new ArrayList<>(size);
		for (int i=0;i<size;i++)
		{
			AbstractIP aip=in.readObject(false, AbstractIP.class);
			totalSize+=aip.getInternalSerializedSize();
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			addresses.add(aip);
		}
		localAddresses=in.readObject(true, AbstractIP.class);
		if (localAddresses!=null)
		{
			
			totalSize+=localAddresses.getInternalSerializedSize();
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			
		}
		manualPortToConnect=in.readInt();
		localPortToConnect=in.readInt();
		canBeDirectServer=in.readBoolean();
		if (localPortToConnect < 0)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeInt(addresses.size());
		for (AbstractIP aip:addresses)
			oos.writeObject(aip, false);
		oos.writeObject(localAddresses, true);
		oos.writeInt(manualPortToConnect);
		oos.writeInt(localPortToConnect);
		oos.writeBoolean(canBeDirectServer);
	}
	
	
	ConnectionInfoSystemMessage(List<PossibleAddressForDirectConnection> inet_socket_addresses,
								InetAddress local_interface_address, int manualPortToConnect, int localPortToConnect,
								boolean canBeDirectServer, AbstractIP localAddresses) {
		this.addresses = new ArrayList<>(inet_socket_addresses.size());
		for (PossibleAddressForDirectConnection r : inet_socket_addresses) {
			if (r.isConcernedBy(local_interface_address)) {
				if (r.getIP().getPort() < 0)
					throw new IllegalArgumentException();
				if (r.getIP().getInetAddress() == null)
					throw new IllegalArgumentException();
				this.addresses.add(r.getIP());
			}
		}

		this.manualPortToConnect = manualPortToConnect;
		if (localPortToConnect < 0)
			throw new IllegalArgumentException("localPortToConnect must be greater or equal than 0 !");
		this.localPortToConnect = localPortToConnect;
		this.canBeDirectServer = canBeDirectServer;
		this.localAddresses = localAddresses;
	}

	public boolean hasManualPortToConnect() {
		return manualPortToConnect >= 0;
	}

	public int getLocalPortToConnect() {
		return localPortToConnect;
	}

	public boolean isCanBeDirectServer() {
		return canBeDirectServer;
	}

	public int getPortToConnect() {
		if (hasManualPortToConnect())
			return manualPortToConnect;
		else
			return localPortToConnect;
	}

	@Override
	public String toString() {
		return "ConnectionInfo[manualPortToConnect=" + manualPortToConnect + ", localPortToConnect="
				+ localPortToConnect + ", inetAddresses=" + addresses + "]";
	}

	public InetSocketAddress getInetSocketAddress(InetAddress connectFrom, InetAddress perceivedDistantInetAddress) {
		if (perceivedDistantInetAddress == null) {
			return null;
		}

		if (perceivedDistantInetAddress instanceof Inet4Address)
			return getInetSocketAddress(connectFrom instanceof Inet6Address, (Inet4Address) perceivedDistantInetAddress,
					AbstractIP.isLocalAddress(connectFrom) && AbstractIP.isLocalAddress(perceivedDistantInetAddress));
		else if (perceivedDistantInetAddress instanceof Inet6Address)
			return getInetSocketAddress(connectFrom instanceof Inet6Address, (Inet6Address) perceivedDistantInetAddress,
					AbstractIP.isLocalAddress(connectFrom) && AbstractIP.isLocalAddress(perceivedDistantInetAddress));
		else {
			return null;
		}
	}

	private InetSocketAddress getInetSocketAddress(boolean connectionFromIPV6, Inet6Address perceivedDistantInetAddress,
			boolean isLocalToLocal) {
		if (isLocalToLocal && connectionFromIPV6 && this.canBeDirectServer)
			return new InetSocketAddress(perceivedDistantInetAddress, getLocalPortToConnect());

		InetSocketAddress isa = getInetSocketAddress(connectionFromIPV6, isLocalToLocal);
		if (isa == null) {
			if (connectionFromIPV6 && this.canBeDirectServer
					&& (hasManualPortToConnect() && AbstractIP.isInternetAddress(perceivedDistantInetAddress)))
				return new InetSocketAddress(perceivedDistantInetAddress, getPortToConnect());
			else
				return null;
		} else
			return isa;
	}

	private InetSocketAddress getInetSocketAddress(boolean connectionFromIPV6, Inet4Address perceivedDistantInetAddress,
			boolean isLocalToLocal) {
		if (isLocalToLocal && !connectionFromIPV6 && this.canBeDirectServer)
			return new InetSocketAddress(perceivedDistantInetAddress, getLocalPortToConnect());
		InetSocketAddress isa = getInetSocketAddress(connectionFromIPV6, isLocalToLocal);
		if (isa == null) {
			if (!connectionFromIPV6 && this.canBeDirectServer
					&& (hasManualPortToConnect() && AbstractIP.isInternetAddress(perceivedDistantInetAddress)))
				return new InetSocketAddress(perceivedDistantInetAddress, getPortToConnect());
			else
				return null;
		} else
			return isa;
	}





	private InetSocketAddress getInetSocketAddress(boolean connectionFromIPV6, boolean isLocalToLocal) {
		if (connectionFromIPV6) {
			if (isLocalToLocal && localAddresses != null) {
				Inet6Address ia = localAddresses.getInet6Address();

				if (ia != null)
					return new InetSocketAddress(ia, localAddresses.getPort());
			} else {
				for (AbstractIP ip : addresses) {
					InetAddress ia = ip.getInet6Address();
					if (ia != null)
						return new InetSocketAddress(ia, ip.getPort());
				}
			}
		}
		for (AbstractIP ip : addresses) {
			if (isLocalToLocal && localAddresses != null) {
				Inet4Address ia = localAddresses.getInet4Address();
				if (ia != null)
					return new InetSocketAddress(ia, localAddresses.getPort());
			} else {
				InetAddress ia = ip.getInet4Address();
				if (ia != null)
					return new InetSocketAddress(ia, ip.getPort());
			}
		}
		return null;
	}

	

	@Override
	public boolean excludedFromEncryption() {
		return false;
	}

	
}
