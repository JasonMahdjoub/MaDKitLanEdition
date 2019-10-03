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

import com.distrimind.util.crypto.*;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;

/**
 * This identifier associates a {@link HostIdentifier} and a
 * {@link CloudIdentifier}. The cloud is associated with a user, or an entity,
 * and with a machine or an instance of the same program.
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadKitLanEdition 1.0
 * @see CloudIdentifier
 * @see HostIdentifier
 */
public class Identifier implements SecureExternalizable {

	private CloudIdentifier cloud_identifier;
	private HostIdentifier host_identifier;

	Identifier()
	{
		
	}
	
	public Identifier(CloudIdentifier _cloud_identifier, HostIdentifier _host_identifier) {
		if (_cloud_identifier == null)
			throw new NullPointerException("_cloud_identifier");
		if (_host_identifier == null)
			throw new NullPointerException("_host_identifier");
		cloud_identifier = _cloud_identifier;
		host_identifier = _host_identifier;
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof Identifier) {
			Identifier id = (Identifier) o;
			return cloud_identifier.equals(id.cloud_identifier) && host_identifier.equals(id.host_identifier);
		}
		return false;
	}

	@Override
	public String toString() {
		return "Identifier["+cloud_identifier.toString()+", "+host_identifier.toString()+"]";
	}

	@Override
	public int hashCode()
	{
		return cloud_identifier.hashCode()^host_identifier.hashCode();
	}
	
	/**
	 * Tells if the given identifier has the same cloud identifier than those of the
	 * current instance.
	 * 
	 * @param _identifier
	 *            the identifier
	 * @return true if the given identifier has the same cloud identifier than those
	 *         of the current instance.
	 * @see CloudIdentifier
	 */
	public boolean equalsCloudIdentifier(Identifier _identifier) {
		return cloud_identifier.equals(_identifier.cloud_identifier);
	}

	/**
	 * Tells if the given identifier has the same host identifier than those of the
	 * current instance.
	 * 
	 * @param _identifier
	 *            the identifier
	 * @return true if the given identifier has the same host identifier than those
	 *         of the current instance.
	 * @see HostIdentifier
	 */
	public boolean equalsHostIdentifier(Identifier _identifier) {
		return host_identifier.equals(_identifier.host_identifier);
	}

	/**
	 * 
	 * @return the cloud identifier
	 * @see CloudIdentifier
	 */
	public CloudIdentifier getCloudIdentifier() {
		return cloud_identifier;
	}

	/**
	 * 
	 * @return the host identifier
	 * @see HostIdentifier
	 */
	public HostIdentifier getHostIdentifier() {
		return host_identifier;
	}

	@Override
	public int getInternalSerializedSize() {
		return cloud_identifier.getInternalSerializedSize()+host_identifier.getInternalSerializedSize();
	}

	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		cloud_identifier=in.readObject(false, CloudIdentifier.class);
		host_identifier=in.readObject(false, HostIdentifier.class);
		if (cloud_identifier.getAuthenticationPublicKey()==null && cloud_identifier.getAuthenticationMethod().isAuthenticatedByPublicKey())
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		if (host_identifier.getAuthenticationPublicKey()==null && host_identifier.isAuthenticatedByPublicKey())
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException
	{
		oos.writeObject(  cloud_identifier, false);
		oos.writeObject(  host_identifier, false);
	}

	public byte[] toBytes() throws IOException {
		try(RandomByteArrayOutputStream soos=new RandomByteArrayOutputStream())
		{
			soos.writeObject(this, false);
			soos.flush();
			return soos.getBytes();
		}
	}

	public enum AuthenticationMethod
	{
		/**
		 * The authentication is not possible. Note that CloudIdentifier or HostIdentifier must define at least one authentication method.
		 */
		NOT_DEFINED,
		/**
		 * The authentication by public key is sufficient and do not need a password/key authentication
		 */
		PUBLIC_KEY,
		/**
		 * The authentication need a password or a secret key
		 */
		PASSWORD_OR_KEY,
		/**
		 * Both authentication by public key and authentication by password or secret key are needed
		 */
		PUBLIC_KEY_WITH_PASSWORD_OR_KEY;

		public boolean isAuthenticatedByPublicKey()
		{
			return this.equals(PUBLIC_KEY) || this.equals(PUBLIC_KEY_WITH_PASSWORD_OR_KEY);
		}

		public boolean isAuthenticatedByPasswordOrSecretKey()
		{
			return this.equals(PASSWORD_OR_KEY) || this.equals(PUBLIC_KEY_WITH_PASSWORD_OR_KEY);
		}
	}

	public interface Authenticated
	{
		/**
		 * Gets the authentication method
		 * If authentication by public key is accepted, the functions {@link #getAuthenticationPublicKey()} and {@link #getAuthenticationKeyPair()} cannot return null.
		 * @return the authentication method
		 * @see #getAuthenticationPublicKey()
		 * @see #getAuthenticationKeyPair()
		 */
		AuthenticationMethod getAuthenticationMethod();

		/**
		 * Returns the authentication's public key used to authenticate the peer.
		 * If it not returns null, than the function {@link #getAuthenticationKeyPair()} must not return null.
		 *
		 *
		 * @return the authentication's public key or null if no authentication through the identifier is necessary
		 * @see #getAuthenticationKeyPair()
		 */
		IASymmetricPublicKey getAuthenticationPublicKey();

		/**
		 * Return the authentication's key pair used to authenticate the peer.
		 * This function cannot returns null if the {@link #getAuthenticationPublicKey()} does not returns null.
		 * @return the cloud key pair if the {@link #getAuthenticationPublicKey()} does not returns null, or null.
		 * @see #getAuthenticationPublicKey()
		 */
		AbstractKeyPair getAuthenticationKeyPair();
	}

	public static byte[] signAuthenticatedIdentifier(AbstractKeyPair keyPair, byte[] ...data) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidKeySpecException, InvalidAlgorithmParameterException, SignatureException, InvalidKeyException, IOException {
		ASymmetricAuthenticatedSignerAlgorithm signer=new ASymmetricAuthenticatedSignerAlgorithm(keyPair.getASymmetricPrivateKey());
		signer.init();
		for (byte[] b : data)
			signer.update(b);
		return signer.getSignature();
	}
	public static boolean checkAuthenticatedSignature(IASymmetricPublicKey publicKey, byte[] signature, byte[] ...data) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException, InvalidParameterSpecException, InvalidKeyException, IOException, SignatureException {
		return checkAuthenticatedSignature(publicKey, signature, 0, signature.length, data);
	}
	public static boolean checkAuthenticatedSignature(IASymmetricPublicKey publicKey, byte[] signature, int off, int len, byte[] ...data) throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeySpecException, InvalidParameterSpecException, InvalidKeyException, IOException, SignatureException {
		ASymmetricAuthenticatedSignatureCheckerAlgorithm checker=new ASymmetricAuthenticatedSignatureCheckerAlgorithm(publicKey);
		checker.init(signature, off, len);
		for (byte[] b : data)
			checker.update(b);
		return checker.verify();

	}
}
