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


import com.distrimind.util.crypto.ASymmetricKeyPair;
import com.distrimind.util.crypto.ASymmetricPublicKey;
import com.distrimind.util.io.SecureExternalizable;

/**
 * This identifier is related to a cloud associated to a user, or an entity.
 * This cloud identifier can represents several machines or instances (see
 * {@link HostIdentifier} of the same program linked through network.
 * 
 * @author Jason Mahdjoub
 * @version 2.2
 * @since MadKitLanEdition 1.0
 * @see Identifier
 * @see HostIdentifier
 */
public abstract class CloudIdentifier implements SecureExternalizable {

	public static int MAX_CLOUD_IDENTIFIER_LENGTH=4096;
	
	@Override
	public abstract boolean equals(Object _cloud_identifier);

	@Override
	public abstract int hashCode();

	/**
	 * 
	 * @return the current identifier as byte tab
	 */
	public abstract byte[] getIdentifierBytes();

	/**
	 * Gets the salt with a byte tab format.
	 * 
	 * @return the salt with a byte tab format. The returned value cannot be null.
	 */
	public abstract byte[] getSaltBytes();

	/**
	 * Tells if the authentication by public key is sufficient and do not need a password/key authentication.
	 * If true is returned, than the functions {@link #getCloudPublicKey()} and {@link #getCloudKeyPair()} cannot return null.
	 * @return true if the authentication by public key is sufficient and do not need a password/key authentication
	 * @see #getCloudPublicKey()
	 * @see #getCloudKeyPair()
	 */
	public abstract boolean isAutoIdentifiedCloudWithPublicKey();

	/**
	 * Returns the cloud public key used to authenticate the peer.
	 * If it not returns null, than the function {@link #getCloudKeyPair()} must no return null.
	 *
	 *
	 * @return the cloud public key or null if no authentication through the identifier is necessary
	 * @see #getCloudKeyPair()
	 */
	public abstract ASymmetricPublicKey getCloudPublicKey();

	/**
	 * Return the cloud key pair used to authenticate the peer.
	 * This function cannot returns null if the {@link #getCloudPublicKey()} doest not returns null.
	 * @return the cloud key pair, or null if the {@link #getCloudPublicKey()} does not returns null.
	 * @see #getCloudPublicKey()
	 */
	public abstract ASymmetricKeyPair getCloudKeyPair();


}
