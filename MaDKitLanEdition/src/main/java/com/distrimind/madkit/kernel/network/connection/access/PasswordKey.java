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

import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.crypto.SymmetricAuthenticatedSignatureType;
import com.distrimind.util.crypto.SymmetricSecretKey;

import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * Represent a password.
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 *
 */
public abstract class PasswordKey {
	public static final int MAX_PASSWORD_LENGTH=16392;

	/**
	 * Gets the password with a byte tab format.
	 * 
	 * @return the password with a byte tab format
	 */
	public abstract byte[] getPasswordBytes();

	/**
	 * Gets the salt with a byte tab format.
	 * 
	 * @return the salt with a byte tab format or null is no salt was defined. Salt
	 *         cannot be null if {@link #isKey()} returns false.
	 */
	public abstract byte[] getSaltBytes();

	/**
	 * Tells if this instance represents a complex key, or if it is a standard
	 * password. According that, the system will produce a specific hashing.
	 * 
	 * @return true if this instance represents a complex key, and false if it is a
	 *         standard password.
	 */
	public abstract boolean isKey();
	
	/**
	 * Gets a symmetric secret key used to sign a message and identify the two peers. 
	 * This protocol is used in addition with the password authentication.
	 * However, this function can return a null reference. In this case, only password authentication will be functional
	 * @return the symmetric secret ke for signature or null if no secret key is available
	 */
	public abstract SymmetricSecretKey getSecretKeyForSignature();


	static PasswordKey getRandomPasswordKey(final AbstractSecureRandom random)
	{
		return new PasswordKey() {
			private byte[] passwordBytes=null;
			private byte[] salt=null;
			private SymmetricSecretKey secretKey=null;
			@Override
			public byte[] getPasswordBytes() {
				if (passwordBytes==null)
				{
					if (defaultFakePasswordIsKey)
						passwordBytes=new byte[32];
					else
						passwordBytes=new byte[5+ random.nextInt(25)];
					random.nextBytes(passwordBytes);
				}
				return passwordBytes;
			}

			@Override
			public byte[] getSaltBytes() {
				if (salt==null && defaultFakePasswordSaltSizeBytes>0 && defaultFakePasswordSaltSizeBytes<65)
				{
					salt=new byte[defaultFakePasswordSaltSizeBytes];
					random.nextBytes(salt);
				}
				return salt;
			}

			@Override
			public boolean isKey() {
				return defaultFakePasswordIsKey;
			}

			@Override
			public SymmetricSecretKey getSecretKeyForSignature() {
				if (secretKey==null)
				{
					try {
						secretKey= SymmetricAuthenticatedSignatureType.DEFAULT.getKeyGenerator(random, (short)(defaultFakePasswordSecretKeySizeBytes*8)).generateKey();
					} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
						e.printStackTrace();
					}
				}
				return secretKey;
			}
		};
	}


	public static int defaultFakePasswordSaltSizeBytes=-1;
	public static int defaultFakePasswordSecretKeySizeBytes=32;
	public static boolean defaultFakePasswordIsKey =true;

}
