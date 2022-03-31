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

import com.distrimind.util.io.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
public class MultipleIP extends AbstractIP {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8772565720786798887L;

	private ArrayList<Inet4Address> inet4Addresses;
	private ArrayList<Inet6Address> inet6Addresses;
	private transient Random random;

	protected MultipleIP() {
		super(-1);
		random = new Random(System.currentTimeMillis());
		this.inet4Addresses = new ArrayList<>();
		this.inet6Addresses = new ArrayList<>();
	}
	@Override
	public int getInternalSerializedSize() {
		int res=super.getInternalSerializedSize()+8;
		for (InetAddress ia : inet4Addresses)
			res+= SerializationTools.getInternalSize(ia);
		for (InetAddress ia : inet6Addresses)
			res+=SerializationTools.getInternalSize(ia);
		return res;
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		random = new Random(System.currentTimeMillis());
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int totalSize=4;
		int size=in.readInt();
		if (size<0 || totalSize+size*4>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		this.inet4Addresses = new ArrayList<>(size);
		for (int i=0;i<size;i++)
		{
			Inet4Address ia=in.readObject(false, Inet4Address.class);
			totalSize+=SerializationTools.getInternalSize(ia);
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			inet4Addresses.add(ia);
		}
		size=in.readInt();
		totalSize+=4;
		if (size<0 || totalSize+size*4>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		this.inet6Addresses = new ArrayList<>(size);
		for (int i=0;i<size;i++)
		{
			Inet6Address ia=in.readObject(false, Inet6Address.class);
			totalSize+=SerializationTools.getInternalSize(ia);
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			inet6Addresses.add(ia);
		}
		if (inet4Addresses.isEmpty() && inet6Addresses.isEmpty())
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		
		super.writeExternal(oos);
		if (inet4Addresses.isEmpty() && inet6Addresses.isEmpty())
			throw new IOException();
		oos.writeInt(inet4Addresses.size());
		for (InetAddress ia : inet4Addresses)
			oos.writeObject(ia, false);
		oos.writeInt(inet6Addresses.size());
		for (InetAddress ia : inet6Addresses)
			oos.writeObject(ia, false);

	}
	

	public MultipleIP(int port, Collection<Inet4Address> inet4Addresses, Collection<Inet6Address> inet6Addresses) {
		super(port);
		random = new Random(System.currentTimeMillis());
		this.inet4Addresses = new ArrayList<>();
		this.inet6Addresses = new ArrayList<>();
		for (Inet4Address ia : inet4Addresses) {
			if (ia != null)
				this.inet4Addresses.add(ia);
		}
		for (Inet6Address ia : inet6Addresses) {
			if (ia != null)
				this.inet6Addresses.add(ia);
		}
	}

	public MultipleIP(int port, Collection<?> inetAddresses) {
		super(port);
		random = new Random(System.currentTimeMillis());
		this.inet4Addresses = new ArrayList<>();
		this.inet6Addresses = new ArrayList<>();
		for (Object ia : inetAddresses) {
			if (ia != null) {
				if (ia instanceof Inet4Address)
					this.inet4Addresses.add((Inet4Address) ia);
				else if (ia instanceof Inet6Address)
					this.inet6Addresses.add((Inet6Address) ia);
				else if (ia instanceof DoubleIP) {
					DoubleIP di = (DoubleIP) ia;
					if (di.getInet4Address() != null)
						this.inet4Addresses.add(di.getInet4Address());
					if (di.getInet6Address() != null)
						this.inet6Addresses.add(di.getInet6Address());
				}
			}
		}
	}

	public MultipleIP(int port, InetAddress... inetAddresses) {
		super(port);
		random = new Random(System.currentTimeMillis());
		this.inet4Addresses = new ArrayList<>();
		this.inet6Addresses = new ArrayList<>();
		for (InetAddress ia : inetAddresses) {
			if (ia != null) {
				if (ia instanceof Inet4Address)
					this.inet4Addresses.add((Inet4Address) ia);
				else if (ia instanceof Inet6Address)
					this.inet6Addresses.add((Inet6Address) ia);
			}
		}
	}

	public MultipleIP(int port, DoubleIP... doubleIPS) {
		super(port);
		random = new Random(System.currentTimeMillis());
		this.inet4Addresses = new ArrayList<>();
		this.inet6Addresses = new ArrayList<>();
		for (DoubleIP di : doubleIPS) {
			if (di != null) {
				if (di.getInet4Address() != null)
					this.inet4Addresses.add(di.getInet4Address());
				if (di.getInet6Address() != null)
					this.inet6Addresses.add(di.getInet6Address());
			}
		}
	}

	@SuppressWarnings("SynchronizeOnNonFinalField")
	@Override
	public Inet6Address getInet6Address(Collection<InetAddress> rejectedIps) {
		synchronized (random) {
			if (inet6Addresses.isEmpty())
				return null;
			ArrayList<Inet6Address> res= inet6Addresses;
			if (rejectedIps!=null && !rejectedIps.isEmpty()) {
				ArrayList<Inet6Address> res2 = new ArrayList<>(inet6Addresses);
				for (InetAddress ria : rejectedIps)
					if (ria instanceof Inet6Address)
						res2.remove(ria);

				if (res2.size() > 0)
					res = res2;
			}

			return res.get(random.nextInt(res.size()));
		}
	}

	@SuppressWarnings("SynchronizeOnNonFinalField")
	@Override
	public Inet4Address getInet4Address(Collection<InetAddress> rejectedIps) {
		synchronized (random) {
			if (inet4Addresses.isEmpty())
				return null;
			ArrayList<Inet4Address> res= inet4Addresses;
			if (rejectedIps!=null && !rejectedIps.isEmpty()) {
				ArrayList<Inet4Address> res2 = new ArrayList<>(inet4Addresses);
				for (InetAddress ria : rejectedIps)
					if (ria instanceof Inet4Address)
						res2.remove(ria);

				if (res2.size() > 0)
					res = res2;
			}

			return res.get(random.nextInt(res.size()));
		}
	}

	@Override
	public InetAddress[] getInetAddresses() {
		InetAddress[] res = new InetAddress[inet4Addresses.size() + inet6Addresses.size()];
		int index = 0;
		for (Inet4Address ia : inet4Addresses)
			res[index++] = ia;
		for (Inet6Address ia : inet6Addresses)
			res[index++] = ia;
		return res;
	}

	@Override
	public Inet6Address[] getInet6Addresses() {
		Inet6Address[] res = new Inet6Address[inet6Addresses.size()];
		int index = 0;
		for (Inet6Address ia : inet6Addresses)
			res[index++] = ia;
		return res;
	}

	@Override
	public Inet4Address[] getInet4Addresses() {
		Inet4Address[] res = new Inet4Address[inet4Addresses.size()];
		int index = 0;
		for (Inet4Address ia : inet4Addresses)
			res[index++] = ia;
		return res;
	}


	@Override
	public boolean excludedFromEncryption() {
		return false;
	}


}
