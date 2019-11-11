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

import com.distrimind.madkit.kernel.network.EncryptionRestriction;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.crypto.AbstractMessageDigest;
import com.distrimind.util.crypto.IASymmetricPublicKey;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Represents data enabling to identify each peer, and its right access.
 * 
 * @author Jason Mahdjoub
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * @see AccessData
 */
public abstract class LoginData extends AccessData {

	private final AtomicReference<LoginEventsTrigger[]> login_triggers = new AtomicReference<>(null);

	/**
	 * Returns for one identifier the group(s) for which it is enabled to access
	 * 
	 * @param _id
	 *            the identifier of the distant user
	 * @return the authorized group(s)
	 * @throws AccessException if a problem occurs
	 */
	public abstract ListGroupsRoles getGroupsAccess(Identifier _id) throws AccessException;

	/**
	 * Parse all identifiers and call
	 * {@link IdentifierParser#newIdentifier(Identifier)}  function for each
	 * identifier parsed.
	 * 
	 * @param notifier
	 *            the notifier
	 * @throws AccessException
	 *             if a problem occurs
	 * @see IdentifierParser
	 */
	protected abstract void parseIdentifiers(IdentifierParser notifier) throws AccessException;

	/*
	 * Get the cloud identifier corresponding to the given encrypted cloud identifier
	 * 
	 * @param encryptedCloudIdentifier
	 *            the encrypted cloud identifier
	 * @param cipher
	 *            the cipher which will enable to check the parsed identifiers with
	 *            the given encrypted identifier
	 * @return the corresponding clear identifier, or null if no valid identifier was found
	 * @throws AccessException
	 *             if a problem occurs
	 */
	/*public CloudIdentifier getIdentifier(final EncryptedCloudIdentifier encryptedCloudIdentifier,
			final P2PASymmetricSecretMessageExchanger cipher) throws AccessException {
		final AtomicReference<CloudIdentifier> res = new AtomicReference<>(null);

		parseIdentifiers(new IdentifierParser() {

			@Override
			public boolean newIdentifier(Identifier _identifier) throws AccessException {
				try {
					if (encryptedCloudIdentifier
									.verifyWithLocalCloudIdentifier(_identifier.getCloudIdentifier(), cipher)) {
						if (isValidLocalCloudIdentifier(_identifier.getCloudIdentifier()))
							res.set(_identifier.getCloudIdentifier());
						return false;
					} else
						return true;
				} catch (Exception e) {
					throw new AccessException(e);
				}
			}
		});

		return res.get();
	}*/

	private CloudIdentifier getLocalVersionOfDistantCloudIdentifier(final EncryptedCloudIdentifier encryptedCloudIdentifier,
																	final AbstractMessageDigest messageDigest, final byte[] localGeneratedSalt, final EncryptionRestriction encryptionRestriction, final AbstractAccessProtocolProperties accessProtocolProperties) throws AccessException {
		final AtomicReference<CloudIdentifier> res = new AtomicReference<>(null);

		parseIdentifiers(new IdentifierParser() {

			@Override
			public boolean newIdentifier(Identifier _identifier) throws AccessException {
				try {
					CloudIdentifier cloudIdentifier=_identifier.getCloudIdentifier();
					if (encryptedCloudIdentifier
									.verifyWithLocalCloudIdentifier(_identifier.getCloudIdentifier(), messageDigest, localGeneratedSalt)) {


						if (cloudIdentifier!=null && (!cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey() || cloudIdentifier.getAuthenticationKeyPair()!=null)) {
							if (!areCloudIdentifiersCompatible(cloudIdentifier, encryptedCloudIdentifier, encryptionRestriction, accessProtocolProperties)) {
								return false;
							}
						}
						else
						{
							if (cloudIdentifier==null
									|| !cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey()
									|| cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPasswordOrSecretKey()
									|| !isValidDistantCloudIdentifier(cloudIdentifier, encryptionRestriction, accessProtocolProperties))
								return false;
						}
						res.set(cloudIdentifier);

						/*if (areCloudIdentifiersCompatible(_identifier.getCloudIdentifier(), encryptedCloudIdentifier, encryptionRestriction, accessProtocolProperties)) {
							res.set(_identifier.getCloudIdentifier());
						}*/
						return false;
					} else
						return true;
				} catch (Exception e) {
					throw new AccessException(e);
				}
			}
		});

		return res.get();
	}
	/**
	 * Get the local version of cloud identifier corresponding to the given distant wrapped cloud identifier.
	 * If the cloud identifier must be auto-signed, than the signature is checked.
	 * This function works also with anonymous identifiers
	 *
	 * @param wrappedCloudIdentifier
	 *            the wrapped cloud identifier
	 * @param messageDigest
	 *            the cipher which will enable to check the parsed identifiers if a an
	 *            encrypted identifier is transmitted
	 * @param localGeneratedSalt the local salt used to check the signature
	 * @param encryptionRestriction the encryption restriction
	 * @param accessProtocolProperties the access protocol properties
	 * @return the corresponding clear identifier, or null if no valid identifier was found
	 * @throws AccessException if a problem occurs
	 */
	public final CloudIdentifier getLocalVersionOfDistantCloudIdentifier(final WrappedCloudIdentifier wrappedCloudIdentifier,
																		  final AbstractMessageDigest messageDigest,
																		 final byte[] localGeneratedSalt,
																		 EncryptionRestriction encryptionRestriction,
																		 AbstractAccessProtocolProperties accessProtocolProperties) throws AccessException {
		CloudIdentifier res;
		if (wrappedCloudIdentifier.getCloudIdentifier() instanceof EncryptedCloudIdentifier)
			res=getLocalVersionOfDistantCloudIdentifier((EncryptedCloudIdentifier)wrappedCloudIdentifier.getCloudIdentifier(), messageDigest, localGeneratedSalt, encryptionRestriction, accessProtocolProperties);
		else
			res=getLocalVersionOfDistantCloudIdentifier(wrappedCloudIdentifier.getCloudIdentifier(), encryptionRestriction, accessProtocolProperties);

		try {
			if (res==null || !wrappedCloudIdentifier.checkSignature(res, messageDigest, localGeneratedSalt)) {
				return null;
			}
			else
				return res;
		} catch (InvalidKeyException | NoSuchAlgorithmException | IOException | InvalidParameterSpecException | SignatureException | NoSuchProviderException | InvalidAlgorithmParameterException | InvalidKeySpecException e) {
			e.printStackTrace();
			return null;
		}
	}
	/*
	 * Transform the given identifier to a local identifier
	 *
	 * @param encryptedCloudIdentifier
	 *            the encrypted cloud identifier
	 * @param messageDigest
	 *            the message digest type used to hide cloud identifier
	 * @param localGeneratedSalt
	 * 			  the used salt (can be null)
	 * @param encryptionRestriction the encryption restriction
	 * @param accessProtocolProperties the access protocol properties
	 *
	 *
	 * @return an identifier transformed to be understood locally
	 */
	/*public final Identifier getLocalIdentifier(final EncryptedCloudIdentifier encryptedCloudIdentifier,
											   final AbstractMessageDigest messageDigest, final byte[] localGeneratedSalt, final EncryptionRestriction encryptionRestriction, final AbstractAccessProtocolProperties accessProtocolProperties) throws AccessException {
		final AtomicReference<Identifier> res = new AtomicReference<>(null);

		parseIdentifiers(new IdentifierParser() {

			@Override
			public boolean newIdentifier(Identifier _identifier) throws AccessException {
				try {
					if (encryptedCloudIdentifier
							.verifyWithLocalCloudIdentifier(_identifier.getCloudIdentifier(), messageDigest, localGeneratedSalt)) {
						if (isValidLocalIdentifier(_identifier, encryptionRestriction, accessProtocolProperties))
							res.set(_identifier);
						return false;
					} else
						return true;
				} catch (Exception e) {
					throw new AccessException(e);
				}
			}
		});

		return res.get();
	}*/
	/*
	 * Parse the identifiers corresponding to the cloud identifier itself
	 * corresponding to the given encrypted identifier
	 * 
	 * @param parser
	 *            the parser
	 * @param encryptedIdentifier
	 *            the encrypted identifier
	 * @param cipher
	 *            the cipher which will enable to check the parsed identifiers with
	 *            the given encrypted identifier
	 * 
	 * @throws AccessException
	 *             if a problem occurs
	 * @see IdentifierParser
	 */
	/*public void parseHostIdentifiers(final IdentifierParser parser, final EncryptedIdentifier encryptedIdentifier,
			final P2PASymmetricSecretMessageExchanger cipher) throws AccessException {
		parseIdentifiers(new IdentifierParser() {

			@Override
			public boolean newIdentifier(Identifier _identifier) throws AccessException {
				try {
					if (encryptedIdentifier.getEncryptedCloudIdentifier()
							.verifyWithLocalCloudIdentifier(_identifier.getCloudIdentifier(), cipher)) {
						if (isValidLocalIdentifier(_identifier))
							return parser.newIdentifier(_identifier);
					} else
						return true;
				} catch (Exception e) {
					throw new AccessException(e);
				}
			}
		});
	}*/

	/**
	 * Gets a list of possible cloud identifiers candidate to be used for connection
	 * initiative with the distant peer. All identifiers must not be returned. This
	 * function differs from the function
	 * {@link #parseIdentifiers(IdentifierParser)} which parse all identifiers.
	 * 
	 * @return the list of possible identifiers to initiate
	 * @see #canTakesLoginInitiative()
	 * @throws AccessException if an access problem occurs
	 */
	protected abstract List<CloudIdentifier> getCloudIdentifiersToInitiateImpl() throws AccessException;

	/**
	 * Gets a list of possible cloud identifiers candidate to be used for connection
	 * initiative with the distant peer. All identifiers must not be returned. This
	 * function differs from the function
	 * {@link #parseIdentifiers(IdentifierParser)} which parse all identifiers.
	 * @param encryptionRestriction the encryption restriction
	 * @param accessProtocolProperties the access protocol properties
	 *
	 * @return the list of possible identifiers to initiate
	 * @see #canTakesLoginInitiative()
	 * @throws AccessException if an access problem occurs
	 */
	public final Set<CloudIdentifier> getCloudIdentifiersToInitiate(EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties) throws AccessException
	{

		List<CloudIdentifier> preres = getCloudIdentifiersToInitiateImpl();
		if (preres==null)
			return new HashSet<>();
		Set<CloudIdentifier> res=new HashSet<>();
		for (CloudIdentifier ci : preres) {

			if (isValidLocalCloudIdentifier(ci, encryptionRestriction, accessProtocolProperties)) {
				if (ci.getAuthenticationMethod().isAuthenticatedByPasswordOrSecretKey()) {
					PasswordKey pk = getCloudPassword(ci);
					if (!accessProtocolProperties.isAcceptablePassword(encryptionRestriction, pk)) {
						continue;
					}
				}
				res.add(ci);
			}


		}
		return res;

	}

	/**
	 * Ask if the current peer can ask for login, or it must wait to be asked for
	 * login.
	 * 
	 * @return true if the current peer can ask for login itself
	 * @see #getCloudIdentifiersToInitiate(EncryptionRestriction, AbstractAccessProtocolProperties)
	 */
	public abstract boolean canTakesLoginInitiative();

	/**
	 * This class is used to parse identifiers
	 * 
	 * @author Jason Mahdjoub
	 * @see LoginData#parseIdentifiers(IdentifierParser)
	 */
	public static abstract class IdentifierParser {
		/**
		 * 
		 * @param identifier
		 *            the new identifier parsed
		 * @return true if the identifier parsing can continue
		 * @throws AccessException if access problem occurs
		 */
		public abstract boolean newIdentifier(Identifier identifier) throws AccessException;
	}

	/**
	 * According an identifier, returns the cloud password
	 *
	 * @param identifier
	 *            the identifier
	 * @return the cloud password corresponding to the given identifier
	 */
	protected abstract PasswordKey getCloudPassword(CloudIdentifier identifier);


	/**
	 * Returns true if the distant host identifier can be considered as valid
	 *
	 * @param distantIdentifier
	 *            the distant identifier
	 * @return true if the distant host identifier can be considered as valid
	 */
	public final boolean isDistantHostIdentifierValid(Identifier distantIdentifier)
	{
		if (!HostIdentifier.getNullHostIdentifierSingleton().equals(distantIdentifier.getHostIdentifier()))
		{
			Identifier id=localiseIdentifierImpl(distantIdentifier.getCloudIdentifier());
			if (id!=null && id.getHostIdentifier().equals(distantIdentifier.getHostIdentifier()))
				return false;
		}
		return isDistantHostIdentifierValidImpl(distantIdentifier);
	}

	public abstract boolean isDistantHostIdentifierValidImpl(Identifier distantIdentifier);

	/**
	 * Inform that a bad password has been given with the cloud identifier given as
	 * parameter
	 * 
	 * @param identifier
	 *            the cloud identifier
	 */
	public abstract void invalidCloudPassword(CloudIdentifier identifier);

	/**
	 * Inform that a bad password has been given with the host identifier given as
	 * parameter
	 *
	 * @param identifier
	 *            the identifier
	 */
	public abstract void invalidHostPassword(Identifier identifier);

	/**
	 * Gets a local identifier correspond to the given cloud identifier
	 * 
	 * @param _identifier the cloud identifier
	 * @return a local identifier correspond to the given cloud identifier
	 */
	protected abstract Identifier localiseIdentifierImpl(CloudIdentifier _identifier);

	/**
	 * Gets a local identifier correspond to the given cloud identifier
	 *	 * @param encryptionRestriction the encryption restriction
	 * 	 * @param accessProtocolProperties the access protocol properties
	 * @param _identifier the cloud identifier
	 * @param encryptionRestriction the encryption restriction
	 * @param accessProtocolProperties the access protocol properties
	 * @return a valid local identifier correspond to the given cloud identifier, or null no valid identifier was found
	 */
	public final Identifier localiseIdentifier(CloudIdentifier _identifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		Identifier id=localiseIdentifierImpl(_identifier);
		if (areCloudIdentifiersCompatible(id, _identifier, encryptionRestriction, accessProtocolProperties))
			return id;
		else
			return null;
	}
	/**
	 * Gets the local version of the given cloud identifier. Returns true if the distant cloud identifier has no version into the current peer.
	 * @param distantCloudIdentifier the received cloud identifier
	 * @return the local version of the given cloud identifier
	 */
	protected abstract CloudIdentifier getLocalVersionOfDistantCloudIdentifierImpl(CloudIdentifier distantCloudIdentifier);


	/**
	 * Tells if the given cloud identifier is a valid identifier and if it can be used to authenticate the distant peer
	 * @param distantCloudIdentifier the distant cloud identifier
	 * @param encryptionRestriction the encryption restriction
	 * @param accessProtocolProperties the access protocol properties
	 * @return true if the given cloud identifier is a valid identifier and if it can be used to authenticate the distant peer
	 */
	private CloudIdentifier getLocalVersionOfDistantCloudIdentifier(CloudIdentifier distantCloudIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		if (distantCloudIdentifier==null)
			return null;
		CloudIdentifier cloudIdentifier=getLocalVersionOfDistantCloudIdentifierImpl(distantCloudIdentifier);
		if (cloudIdentifier!=null && (!cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey() || cloudIdentifier.getAuthenticationKeyPair()!=null)) {
			if (!areCloudIdentifiersCompatible(cloudIdentifier, distantCloudIdentifier, encryptionRestriction, accessProtocolProperties))
				return null;
		}
		else
		{
			if (cloudIdentifier==null
					|| !cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey()
					|| cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPasswordOrSecretKey()
					|| !isValidDistantCloudIdentifier(cloudIdentifier, encryptionRestriction, accessProtocolProperties))
				return null;
		}
		return cloudIdentifier;
	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean areAuthenticatedIdentifiersCompatible(Identifier.Authenticated localAuthenticatedIdentifier, Identifier.Authenticated distantAuthenticatedIdentifier)
	{
		if (localAuthenticatedIdentifier==null)
			return false;
		if (distantAuthenticatedIdentifier==null)
			return false;
		if ((localAuthenticatedIdentifier instanceof CloudIdentifier)
				&& ((CloudIdentifier)localAuthenticatedIdentifier).mustBeAnonymous()
				&& (!(distantAuthenticatedIdentifier instanceof EncryptedCloudIdentifier) && !((CloudIdentifier)distantAuthenticatedIdentifier).mustBeAnonymous())
			)
			return false;
		return (distantAuthenticatedIdentifier instanceof EncryptedCloudIdentifier) || localAuthenticatedIdentifier.getAuthenticationMethod()==distantAuthenticatedIdentifier.getAuthenticationMethod();
	}

	boolean areCloudIdentifiersCompatible(CloudIdentifier localCloudIdentifier, CloudIdentifier distantCloudIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		if (!areAuthenticatedIdentifiersCompatible(localCloudIdentifier, distantCloudIdentifier))
			return false;
		if (!isValidLocalCloudIdentifier(localCloudIdentifier, encryptionRestriction, accessProtocolProperties) || !isValidDistantCloudIdentifier(distantCloudIdentifier, encryptionRestriction, accessProtocolProperties))
			return false;
		if (distantCloudIdentifier instanceof EncryptedCloudIdentifier)
			return true;
		else
			return localCloudIdentifier.equals(distantCloudIdentifier);
	}
	boolean areCloudIdentifiersCompatible(Identifier localIdentifier, CloudIdentifier distantCloudIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		if (!areAuthenticatedIdentifiersCompatible(localIdentifier.getCloudIdentifier(), distantCloudIdentifier))
			return false;
		if (!isValidLocalIdentifier(localIdentifier, encryptionRestriction, accessProtocolProperties))
			return false;
		if (distantCloudIdentifier instanceof EncryptedCloudIdentifier)
			return true;
		else
			return localIdentifier.getCloudIdentifier().equals(distantCloudIdentifier);
	}

	private boolean isValidAuthenticatedIdentifier(Identifier.Authenticated authenticatedIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties, boolean local)
	{
		if (authenticatedIdentifier==null)
			return false;
		if (authenticatedIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey() && !acceptAutoSignedIdentifiers())
			return false;
		if (!authenticatedIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey()
				|| (authenticatedIdentifier.getAuthenticationPublicKey()!=null && (!local || authenticatedIdentifier.getAuthenticationKeyPair()!=null)))
		{
			return accessProtocolProperties.isAcceptableHostIdentifier(encryptionRestriction, authenticatedIdentifier);
		}
		else
			return false;

	}
	private boolean isValidLocalAuthenticatedIdentifier(Identifier.Authenticated authenticatedIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		return isValidAuthenticatedIdentifier(authenticatedIdentifier, encryptionRestriction, accessProtocolProperties, true);
	}
	private boolean isValidDistantAuthenticatedIdentifier(Identifier.Authenticated authenticatedIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		return isValidAuthenticatedIdentifier(authenticatedIdentifier, encryptionRestriction, accessProtocolProperties, false);
	}
	final boolean isValidLocalCloudIdentifier(CloudIdentifier cloudIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		if (!isValidLocalAuthenticatedIdentifier(cloudIdentifier, encryptionRestriction, accessProtocolProperties))
			return false;
		return cloudIdentifier.getAuthenticationMethod()!= Identifier.AuthenticationMethod.NOT_DEFINED;
	}
	final boolean isValidDistantCloudIdentifier(CloudIdentifier cloudIdentifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		if (!isValidDistantAuthenticatedIdentifier(cloudIdentifier, encryptionRestriction, accessProtocolProperties))
			return false;
		return (cloudIdentifier instanceof EncryptedCloudIdentifier) || cloudIdentifier.getAuthenticationMethod()!= Identifier.AuthenticationMethod.NOT_DEFINED;
	}

	final boolean isValidLocalHostIdentifier(HostIdentifier hostIdentifier)
	{
		if (hostIdentifier==null)
			return false;
		return  !hostIdentifier.isAuthenticatedByPublicKey()
				|| (hostIdentifier.getAuthenticationPublicKey()!=null && hostIdentifier.getAuthenticationKeyPair()!=null);
	}
	final boolean isValidLocalIdentifier(Identifier identifier, EncryptionRestriction encryptionRestriction, AbstractAccessProtocolProperties accessProtocolProperties)
	{
		if (identifier==null)
			return false;
		return isValidLocalCloudIdentifier(identifier.getCloudIdentifier(), encryptionRestriction, accessProtocolProperties) && isValidLocalHostIdentifier(identifier.getHostIdentifier()) && accessProtocolProperties.isAcceptableHostIdentifier(encryptionRestriction, identifier.getHostIdentifier());
	}


	private boolean containsTrigger(LoginEventsTrigger[] logts, LoginEventsTrigger _trigger) {
		if (logts == null)
			return false;
		for (LoginEventsTrigger lt : logts) {
			if (lt == _trigger)
				return true;
		}
		return false;
	}

	void addTrigger(LoginEventsTrigger _trigger) {
		if (_trigger == null)
			return;
		synchronized (this) {
			LoginEventsTrigger[] logts = login_triggers.get();
			if (containsTrigger(logts, _trigger))
				return;
			LoginEventsTrigger[] new_logts;
			if (logts == null)
				new_logts = new LoginEventsTrigger[1];
			else {
				new_logts = new LoginEventsTrigger[logts.length + 1];
				System.arraycopy(logts, 0, new_logts, 0, logts.length);
			}
			new_logts[new_logts.length - 1] = _trigger;
			login_triggers.set(new_logts);
		}
	}

	/*void removeTrigger(LoginEventsTrigger _trigger) {
		if (_trigger == null)
			return;
		synchronized (this) {
			LoginEventsTrigger logts[] = login_triggers.get();
			if (!containsTrigger(logts, _trigger))
				return;
			LoginEventsTrigger new_logts[] = null;
			if (logts == null)
				return;
			else {
				new_logts = new LoginEventsTrigger[logts.length - 1];
				int i = 0;
				for (LoginEventsTrigger lt : logts) {
					if (lt != _trigger)
						new_logts[i++] = lt;
				}
			}
			login_triggers.set(new_logts);
		}
	}*/

	/**
	 * This function must be called when an identifier has been added
	 * 
	 * @param _identifier
	 *            the added identifier
	 */
	public final void newIdentifierAddedEvent(Identifier _identifier) {
		LoginEventsTrigger[] logts = login_triggers.get();
		if (logts != null) {
			for (LoginEventsTrigger lt : logts)
				lt.addingIdentifier(_identifier);
		}
	}

	/**
	 * This function must be called when identifiers have been added
	 * 
	 * @param _identifiers
	 *            the identifiers
	 */
	public final void newIdentifiersAddedEvent(Collection<Identifier> _identifiers) {
		LoginEventsTrigger[] logts = login_triggers.get();
		if (logts != null) {
			for (LoginEventsTrigger lt : logts)
				lt.addingIdentifiers(_identifiers);
		}
	}

	/**
	 * This function must be called when an identifier has been removed
	 * 
	 * @param _identifier
	 *            the identifier
	 */
	public final void newIdentifierRemovedEvent(Identifier _identifier) {
		LoginEventsTrigger[] logts = login_triggers.get();
		if (logts != null) {
			for (LoginEventsTrigger lt : logts)
				lt.removingIdentifier(_identifier);
		}
	}

	/**
	 * This function must be called when identifiers have been removed
	 * 
	 * @param _identifiers the identifiers 
	 */
	public final void newIdentifiersRemovedEvent(Collection<Identifier> _identifiers) {
		LoginEventsTrigger[] logts = login_triggers.get();
		if (logts != null) {
			for (LoginEventsTrigger lt : logts)
				lt.removingIdentifiers(_identifiers);
		}
	}
	/**
	 * Tells if auto signed identifiers are authorized.
	 * If this function returns 'true', the function {@link #parseIdentifiers(IdentifierParser)} must be implemented.
	 * @see AccessProtocolWithP2PAgreementProperties#p2pLoginAgreementType
	 * @return true if auto signed identifiers are authorized
	 */
	public abstract boolean acceptAutoSignedIdentifiers();

	/**
	 * Returns the decentralized database's identifier.
	 * If the function is not override, it returns the decentralized database returned by {@link HostIdentifier#getDecentralizedDatabaseID()}
	 * is equals to {@link HostIdentifier#getAuthenticationPublicKey()}
	 * and if {@link HostIdentifier#isAuthenticatedByPublicKey()} ()} enables an authentication by public key.
	 * Otherwise, you must override this method to determine the database's host identifier.
	 *
	 * If this method returns null, distant peer cannot synchronize its database with local database.
	 * @param identifier the peer identifier
	 * @return Returns the decentralized database's identifier. Returns null if the distant peer cannot synchronize its database with local peer.
	 */
	public DecentralizedValue getDecentralizedDatabaseID(Identifier identifier)
	{
		DecentralizedValue dv=identifier.getHostIdentifier().getDecentralizedDatabaseID();
		if (identifier.getHostIdentifier().isAuthenticatedByPublicKey() && (dv instanceof IASymmetricPublicKey) && dv.equals(identifier.getHostIdentifier().getAuthenticationPublicKey()))
		{
			return dv;
		}
		return null;
	}




}
