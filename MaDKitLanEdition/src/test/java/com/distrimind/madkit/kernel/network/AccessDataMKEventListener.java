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

import com.distrimind.madkit.kernel.JunitMadkit;
import com.distrimind.madkit.kernel.MadkitEventListener;
import com.distrimind.madkit.kernel.MadkitProperties;
import com.distrimind.madkit.kernel.network.connection.access.*;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.DecentralizedIDGenerator;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.crypto.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.*;


/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class AccessDataMKEventListener implements MadkitEventListener {
	private final ArrayList<AccessData> accessData;

	public AccessDataMKEventListener(AccessData... accessData) {
		this.accessData = new ArrayList<>(accessData.length);
        Collections.addAll(this.accessData, accessData);
	}

	public AccessDataMKEventListener(ArrayList<AccessData> accessData) {
		this.accessData = accessData;
	}

	@Override
	public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
		if (accessData != null) {
			for (AccessData ad : accessData)
				_properties.networkProperties.addAccessData(ad);

		}

	}
	public static final int CLOUD_ID_NUMBER = 28;
	private static final byte[] SALT = new byte[30];

	private static final CloudIdentifier[] cloudIdentifiers;
	static {
		new Random(System.currentTimeMillis()).nextBytes(SALT);
		cloudIdentifiers = new CloudIdentifier[CLOUD_ID_NUMBER+4];
		try {
			AbstractSecureRandom random = SecureRandomType.DEFAULT.getSingleton(null);
			for (int i = 0; i < CLOUD_ID_NUMBER; i++) {
				if (i % 2 == 1)
					cloudIdentifiers[i] = new CustomCloudIdentifierWithPublicKey(ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed448.getKeyPairGenerator(random).generateKeyPair(),SALT, i%4==1);
				else
					cloudIdentifiers[i] = new CustumCloudIdentifier("cloud" + i, SALT, i%4==0);
			}
			cloudIdentifiers[CLOUD_ID_NUMBER] = new CustomCloudIdentifierWithPublicKey(ASymmetricAuthenticatedSignatureType.BCPQC_SPHINCS256_SHA3_512.getKeyPairGenerator(random).generateKeyPair(),SALT, true);
			cloudIdentifiers[CLOUD_ID_NUMBER+1] = new CustomCloudIdentifierWithPublicKey(ASymmetricAuthenticatedSignatureType.BCPQC_SPHINCS256_SHA3_512.getKeyPairGenerator(random).generateKeyPair(),SALT, false);
			cloudIdentifiers[CLOUD_ID_NUMBER+2] = new CustomCloudIdentifierWithPublicKey(new HybridASymmetricAuthenticatedSignatureType(ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed448, ASymmetricAuthenticatedSignatureType.BCPQC_SPHINCS256_SHA3_512).generateKeyPair(random),SALT, true);
			cloudIdentifiers[CLOUD_ID_NUMBER+3] = new CustomCloudIdentifierWithPublicKey(new HybridASymmetricAuthenticatedSignatureType(ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed448, ASymmetricAuthenticatedSignatureType.BCPQC_SPHINCS256_SHA3_512).generateKeyPair(random),SALT, false);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static final HostIdentifier[] autoSignedHostIdentifiers;
	static
	{
		autoSignedHostIdentifiers =new HostIdentifier[4];
		AbstractSecureRandom random;
		try {
			random = SecureRandomType.DEFAULT.getSingleton(null);
			autoSignedHostIdentifiers[0]=new CustomAutoSignedHostIdentifier(random, ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed25519);
			autoSignedHostIdentifiers[1]=new CustomAutoSignedHostIdentifier(random, ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed25519);
			autoSignedHostIdentifiers[2]=new CustomAutoSignedHostIdentifier(random, ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed25519, ASymmetricAuthenticatedSignatureType.BCPQC_SPHINCS256_SHA3_512);
			autoSignedHostIdentifiers[3]=new CustomAutoSignedHostIdentifier(random, ASymmetricAuthenticatedSignatureType.BC_FIPS_Ed25519, ASymmetricAuthenticatedSignatureType.BCPQC_SPHINCS256_SHA3_512);
		} catch (NoSuchAlgorithmException | NoSuchProviderException | IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

	}

	private static final CustomPassword[] paswordIdentifiers;
	static {
		paswordIdentifiers = new CustomPassword[cloudIdentifiers.length];
		AbstractSecureRandom random;
		try {
			random = SecureRandomType.DEFAULT.getSingleton(null);
			for (int i = 0; i < paswordIdentifiers.length; i++) {
				String pw = "pw" + i;
				SymmetricSecretKey sk;

				sk=SymmetricAuthenticatedSignatureType.BC_FIPS_HMAC_SHA2_512.getKeyGenerator(random).generateKey();
				paswordIdentifiers[i] = new CustomPassword(pw, SALT, sk);
			}
		} catch (NoSuchAlgorithmException | NoSuchProviderException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}



	public static HostIdentifier getCustomHostIdentifier(int hostNumber) {
	    if (hostNumber<0)
	        return HostIdentifier.getNullHostIdentifierSingleton();
		return new CustumHostIdentifier("host" + hostNumber);
	}

	public static HostIdentifier getCustomAutoSignedHostIdentifier(int hostNumber) {
		if (hostNumber<0)
			return HostIdentifier.getNullHostIdentifierSingleton();
		return autoSignedHostIdentifiers[hostNumber];
	}



	public static ArrayList<IdentifierPassword> getServerLogins(CustumHostIdentifier host) {
		ArrayList<IdentifierPassword> res = new ArrayList<>(cloudIdentifiers.length);
		for (int i = 0; i < cloudIdentifiers.length; i++) {
			res.add(new IdentifierPassword(new Identifier(cloudIdentifiers[i], host), paswordIdentifiers[i]));
		}
		return res;
	}

	public static ArrayList<IdentifierPassword> getClientOrPeerToPeerLogins(HostIdentifier host, int... indexes) {
		ArrayList<IdentifierPassword> res = new ArrayList<>(indexes.length);
		for (int i : indexes) {
			res.add(new IdentifierPassword(new Identifier(cloudIdentifiers[i], host), paswordIdentifiers[i]));
		}
		return res;
	}

	public static Identifier getIdentifier(HostIdentifier host, int index) {
		return new Identifier(cloudIdentifiers[index], host);
	}

	public static IdentifierPassword getIdentifierPassword(HostIdentifier host, int index) {
		return new IdentifierPassword(new Identifier(cloudIdentifiers[index], host), paswordIdentifiers[index]);
	}

	public static AccessData getDefaultAccessData(final ListGroupsRoles defaultGroupAccess) {
		return new AccessData() {

			@Override
			public ListGroupsRoles getDefaultGroupsAccess() {
				return defaultGroupAccess;
			}

			@Override
			public boolean equals(Object _o) {
				if (_o == null)
					return false;
				return this.getClass() == _o.getClass();
			}
		};
	}

	public static LoginData getDefaultLoginData(final ArrayList<IdentifierPassword> identifersAndPasswords,
			final ListGroupsRoles defaultGroupAccess, final ListGroupsRoles groupAccess,
			final boolean canTakeLoginInitiative, final Runnable invalidPassord, final Runnable invalidCloudIdentifier) {
		return new LoginData() {

			@Override
			public ListGroupsRoles getDefaultGroupsAccess() {
				return defaultGroupAccess;
			}

			@Override
			public boolean equals(Object _o) {
				if (_o == null)
					return false;
				return this.getClass() == _o.getClass();
			}

			@Override
			public void parseIdentifiers(IdentifierParser _notifier) throws AccessException {
				for (IdentifierPassword idpw : identifersAndPasswords) {
					if (!_notifier.newIdentifier(idpw.getIdentifier()))
						return;
				}

			}



			@Override
			public boolean acceptAutoSignedIdentifiers() {
				return true;
			}



			@Override
			protected PasswordKey getCloudPassword(CloudIdentifier identifier) {
				for (IdentifierPassword idpw : identifersAndPasswords) {
					if (idpw.getIdentifier().getCloudIdentifier().equals(identifier))
						return idpw.getPassword();
				}
				return null;
			}

			@Override
			protected List<CloudIdentifier> getCloudIdentifiersToInitiateImpl() {
				ArrayList<CloudIdentifier> list = new ArrayList<>(identifersAndPasswords.size());
				for (IdentifierPassword idpw : identifersAndPasswords) {
					list.add(idpw.getIdentifier().getCloudIdentifier());
				}
				return list;
			}

			@Override
			public ListGroupsRoles getGroupsAccess(PairOfIdentifiers _id) {
				return groupAccess;
			}

			@Override
			public boolean canTakesLoginInitiative() {
				return canTakeLoginInitiative;
			}



			@Override
			public boolean isDistantHostIdentifierValidImpl(Identifier distantIdentifier) {
				return true;
			}

			@Override
			public void invalidCloudPassword(CloudIdentifier identifier) {

				if (invalidPassord != null)
					invalidCloudIdentifier.run();
			}

			@Override
			protected Identifier localiseIdentifierImpl(CloudIdentifier _identifier) {
				for (IdentifierPassword idpw : identifersAndPasswords) {
					if (idpw.getIdentifier().getCloudIdentifier().equals(_identifier))
						return idpw.getIdentifier();
				}
				if (_identifier.getAuthenticationMethod().isAuthenticatedByPublicKey())
					return new Identifier(_identifier, HostIdentifier.getNullHostIdentifierSingleton());
				return null;
			}

			@Override
			protected CloudIdentifier getLocalVersionOfDistantCloudIdentifierImpl(CloudIdentifier distantCloudIdentifier) {
				for (IdentifierPassword idpw : identifersAndPasswords) {
					if (idpw.getIdentifier().getCloudIdentifier().equals(distantCloudIdentifier))
						return idpw.getIdentifier().getCloudIdentifier();
				}
				return distantCloudIdentifier;
			}

			@Override
			public DecentralizedValue getDecentralizedDatabaseID(Identifier identifier, MadkitProperties properties) {
				synchronized (databaseIdentifiers) {
					DecentralizedIDGenerator res=databaseIdentifiers.get(identifier);
					if (res==null)
						databaseIdentifiers.put(identifier, res=new DecentralizedIDGenerator());
					return res;
				}
			}

			@Override
			public DecentralizedValue getCentralDatabaseID(Identifier identifier, MadkitProperties properties) throws DatabaseException {
				synchronized (databaseIdentifiers) {
					return centralIdentifiers.get(identifier);
				}
			}
		};
	}
	private static final HashMap<Identifier, DecentralizedIDGenerator> databaseIdentifiers=new HashMap<>();
	public static final HashMap<Identifier, IASymmetricPublicKey> centralIdentifiers=new HashMap<>();

	public static ArrayList<AccessDataMKEventListener> getAccessDataMKEventListenerForPeerToPeerConnections(
			final boolean canTakeLoginInitiative, final Runnable invalidPassord,final Runnable invalidCloudIdentifier, HostIdentifier hostIdentifier,
			int... loginIndexes) {
		ArrayList<AccessDataMKEventListener> res = new ArrayList<>();
		ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();
		ListGroupsRoles groupAccess=new ListGroupsRoles();
		defaultGroupAccess.addGroupsRoles(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA);
		groupAccess.addGroupsRoles(JunitMadkit.NETWORK_GROUP_FOR_LOGIN_DATA);
		AccessData ad1 = getDefaultAccessData(defaultGroupAccess);
		AccessData ad2 = getDefaultLoginData(getClientOrPeerToPeerLogins(hostIdentifier, loginIndexes),
				defaultGroupAccess, groupAccess,
				canTakeLoginInitiative, invalidPassord, invalidCloudIdentifier);

		res.add(new AccessDataMKEventListener(ad1));
		res.add(new AccessDataMKEventListener(ad1, ad2));
		res.add(new AccessDataMKEventListener(ad2));
		return res;
	}

	public static ArrayList<AccessDataMKEventListener> getAccessDataMKEventListenerForServerConnections(
			final boolean canTakeLoginInitiative, final Runnable invalidPassord,final Runnable invalidCloudIdentifier, HostIdentifier hostIdentifier,
			int... loginIndexes) {
		return getAccessDataMKEventListenerForPeerToPeerConnections(canTakeLoginInitiative, invalidPassord,invalidCloudIdentifier,
				hostIdentifier, loginIndexes);
	}

	public static ArrayList<AccessDataMKEventListener> getAccessDataMKEventListenerForClientConnections(
			final boolean canTakeLoginInitiative, final Runnable invalidPassord,final Runnable invalidCloudIdentifier, HostIdentifier hostIdentifier,
			int... loginIndexes) {
		return getAccessDataMKEventListenerForPeerToPeerConnections(canTakeLoginInitiative, invalidPassord,invalidCloudIdentifier,
				hostIdentifier, loginIndexes);
	}
}
