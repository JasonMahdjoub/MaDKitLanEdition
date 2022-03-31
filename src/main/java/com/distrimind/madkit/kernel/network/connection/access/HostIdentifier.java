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
package com.distrimind.madkit.kernel.network.connection.access;

import com.distrimind.util.*;
import com.distrimind.util.crypto.*;
import com.distrimind.util.data_buffers.WrappedData;
import com.distrimind.util.io.SecureExternalizable;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import com.distrimind.util.io.SerializationTools;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * This identifier is related to a machine, or more precisely to an specific
 * instance of the program.
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadKitLanEdition 1.0
 * @see Identifier
 * @see CloudIdentifier
 */
public abstract class HostIdentifier implements SecureExternalizable {
	protected static final AbstractSecureRandom random;

	static {
		AbstractSecureRandom rand = null;
		try {
			rand = SecureRandomType.BC_FIPS_APPROVED.getSingleton(null);
		} catch (Exception e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		random = rand;
	}

	@Override
	public final boolean equals(Object o)
	{
		if (o==this)
			return true;
		if (o instanceof HostIdentifier)
			return equalsTimeConstant((HostIdentifier) o);
		else
			return false;
	}


	public abstract boolean equalsTimeConstant(HostIdentifier _object);

	@Override
	public abstract int hashCode();

	public abstract WrappedData getBytesTabToEncode();


	/**
	 * Returns the decentralized database's identifier. If the function is not override, it returns null by default. So distant peer cannot synchronize its database with local database.
	 * @return the decentralized database's identifier. Return null if the distant peer cannot synchronize its database with local peer.
	 */
	public DecentralizedValue getDecentralizedDatabaseID()
	{
		return null;
	}

	/**
	 * Returns the central database's identifier. If the function is not override, it returns null by default.
	 * Central database backup permit to synchronize peers, and permit for peer to backup their database with an end-to-end encryption
	 * @return the central database's identifier. If the function is not override, it returns null by default.
	 */
	public DecentralizedValue getCentralDecentralizedDatabaseID()
	{
		return null;
	}

	/**
	 * Generates a unique host identifier
	 * 
	 * @return a unique host identifier
	 * @throws NoSuchAlgorithmException
	 *             if the used encryption algorithm was not found
	 * @throws NoSuchProviderException  if a problem occurs
	 */
	public static HostIdentifier generateDefaultHostIdentifier() throws NoSuchAlgorithmException, NoSuchProviderException {
		return new DefaultHostIdentifier();
	}

	public HostIdentifier getDefaultHostIdentifier(byte[] bytes) throws InvalidEncodedValue {
		return getDefaultHostIdentifier(bytes, 0, bytes.length);
	}

	public static HostIdentifier getDefaultHostIdentifier(byte[] bytes, int off, int len) throws InvalidEncodedValue {
		return new DefaultHostIdentifier(bytes, off, len);
	}

	public static HostIdentifier.NullHostIdentifier getNullHostIdentifierSingleton() {
		return Identifier.getNullHostIdentifierSingleton();
	}
	public static final class NullHostIdentifier extends HostIdentifier
	{
		NullHostIdentifier()
		{

		}
		@Override
		public boolean equalsTimeConstant(HostIdentifier _object) {
			return _object instanceof NullHostIdentifier;
		}

		@Override
		public WrappedData getBytesTabToEncode()
		{
			return new WrappedData(new byte[0]);
		}
		@Override
		public int hashCode() {
			return 0;
		}

		@Override
		public int getInternalSerializedSize() {
			return 0;
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {

		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {

		}

		@Override
		public String toString() {
			return "NullHostIdentifier";
		}

		@Override
		public boolean isAuthenticatedByPublicKey()
		{
			return false;
		}


		@Override
		public IASymmetricPublicKey getAuthenticationPublicKey() {
			return null;
		}

		@Override
		public AbstractKeyPair<?, ?> getAuthenticationKeyPair() {
			return null;
		}
	}


	private static class DefaultHostIdentifier extends HostIdentifier {

		private SecuredDecentralizedID id;

		public DefaultHostIdentifier() throws NoSuchAlgorithmException, NoSuchProviderException {
			synchronized (random) {
				id = new SecuredDecentralizedID(new RenforcedDecentralizedIDGenerator(), random);
			}
		}

		DefaultHostIdentifier(byte[] bytes, int off, int len) throws InvalidEncodedValue {
			id = (SecuredDecentralizedID) AbstractDecentralizedID.decode(bytes, off, len);
		}

		@Override
		public WrappedData getBytesTabToEncode()
		{
			return id.encode();
		}


		@Override
		public boolean equalsTimeConstant(HostIdentifier _host_identifier) {
			if (_host_identifier == null)
				return false;
			if (_host_identifier == this)
				return true;
			if (_host_identifier instanceof DefaultHostIdentifier) {
				DefaultHostIdentifier dhi = (DefaultHostIdentifier) _host_identifier;
				return id.equals(dhi.id);
			}
			return false;
		}

		@Override
		public int hashCode() {
			return id.hashCode();
		}


		@Override
		public int getInternalSerializedSize() {
			return SerializationTools.getInternalSize(id);
		}

		@Override
		public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
		{
			id=in.readObject(false, SecuredDecentralizedID.class);
		}
		@Override
		public void writeExternal(final SecuredObjectOutputStream oos) throws IOException
		{
			oos.writeObject(id, false);
		}

		@Override
		public String toString() {
			return "DefaultHostIdentifier[" +
					"id=" + id +
					']';
		}

		@Override
		public boolean isAuthenticatedByPublicKey()
		{
			return false;
		}

		@Override
		public IASymmetricPublicKey getAuthenticationPublicKey() {
			return null;
		}

		@Override
		public AbstractKeyPair<?,?> getAuthenticationKeyPair() {
			return null;
		}
	}


	public abstract IASymmetricPublicKey getAuthenticationPublicKey();



	public abstract AbstractKeyPair<?, ?> getAuthenticationKeyPair();

	public abstract boolean isAuthenticatedByPublicKey();

}
