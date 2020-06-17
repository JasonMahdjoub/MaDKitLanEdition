package com.distrimind.madkit.kernel.network.connection.secured;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java langage 

This software is governed by the CeCILL-C license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL-C
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL-C license and that you accept its terms.
 */

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.network.EncryptionRestriction;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.util.crypto.EncryptionSignatureHashEncoder;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.crypto.SymmetricSecretKey;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.10.0
 */
@SuppressWarnings("FieldMayBeFinal")
public class P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties extends ConnectionProtocolProperties<P2PSecuredConnectionProtocolWithKnownSymmetricKeys> {

	public P2PSecuredConnectionProtocolWithKnownSymmetricKeysProperties() {
		super(P2PSecuredConnectionProtocolWithKnownSymmetricKeys.class);
	}

	private int lastIdentifier=0;
	private Map<Integer, SymmetricSecretKey> secretKeysForEncryption=new HashMap<>();
	private Map<Integer, SymmetricSecretKey> secretKeysForSignature=new HashMap<>();
	private Map<Integer, Boolean> validProfiles=new HashMap<>();
	private int defaultProfileIdentifier=0;

	/**
	 * Invalidate given profile
	 * @param profileIdentifier the profile identifier
	 */
	public void invalidateProfile(int profileIdentifier)
	{
		Boolean valid=validProfiles.get(profileIdentifier);
		if (valid!=null)
			validProfiles.put(profileIdentifier, false);
	}

	private int generateNewKeyPairIdentifier() {
		++lastIdentifier;
		for (int k : secretKeysForEncryption.keySet())
			if (k>lastIdentifier)
				lastIdentifier=k+1;
		return lastIdentifier;
	}

	/**
	 * Add a new profile
	 *
	 * @param symmetricSecretKeyForEncryption the symmetric key for encryption
	 * @param symmetricSecretKeyForSignature the symmetric key for signature
	 * @return the generated profile identifier
	 */
	public int addProfile(SymmetricSecretKey symmetricSecretKeyForEncryption, SymmetricSecretKey symmetricSecretKeyForSignature)
	{
		return addProfile(generateNewKeyPairIdentifier(),symmetricSecretKeyForEncryption,symmetricSecretKeyForSignature);
	}

	public Set<Integer> getProfileIdentifiers()
	{
		return secretKeysForSignature.keySet();
	}

	/**
	 * Add a new profile
	 * @param profileIdentifier the profile identifier
	 * @param symmetricSecretKeyForEncryption the symmetric key for encryption
	 * @param symmetricSecretKeyForSignature the symmetric key for signature
	 * @return the profile identifier
	 */
	public int addProfile(int profileIdentifier, SymmetricSecretKey symmetricSecretKeyForEncryption, SymmetricSecretKey symmetricSecretKeyForSignature)
	{
		if (symmetricSecretKeyForEncryption==null && enableEncryption)
			throw new NullPointerException();
		if (symmetricSecretKeyForSignature==null)
			throw new NullPointerException();
		if (validProfiles.containsKey(profileIdentifier))
			throw new IllegalArgumentException();

		secretKeysForEncryption.put(profileIdentifier, symmetricSecretKeyForEncryption);
		secretKeysForSignature.put(profileIdentifier, symmetricSecretKeyForSignature);
		validProfiles.put(profileIdentifier, true);
		maximumBodyOutputSizeComputer=null;
		return lastIdentifier=profileIdentifier;
	}

	public SymmetricSecretKey getSymmetricSecretKeyForEncryption(int profileIdentifier)
	{
		return secretKeysForEncryption.get(profileIdentifier);
	}

	public SymmetricSecretKey getSymmetricSecretKeyForSignature(int profileIdentifier)
	{
		return secretKeysForSignature.get(profileIdentifier);
	}

	public boolean isValidProfile(int profileIdentifier, EncryptionRestriction encryptionRestriction)
	{
		Boolean valid=validProfiles.get(profileIdentifier);
		if (valid!=null && valid)
		{
			if (encryptionRestriction==EncryptionRestriction.NO_RESTRICTION)
				return true;
			if (enableEncryption && !(this.secretKeysForEncryption.get(profileIdentifier).isPostQuantumKey()))
				return false;
			return this.secretKeysForSignature.get(profileIdentifier).isPostQuantumKey();
		}
		else
			return false;
	}

	/**
	 * Tells if the current peer can receive an ask for connection.
	 */
	public boolean isServer = true;

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public boolean enableEncryption = true;

	/**
	 * Message digest type used to check message validity
	 */
	public MessageDigestType messageDigestType=MessageDigestType.SHA2_256;

	@Override
	public boolean isConcernedBy(EncryptionRestriction encryptionRestriction) {
		if (subProtocolProperties!=null && subProtocolProperties.isConcernedBy(encryptionRestriction))
			return true;

		for (Integer id : validProfiles.keySet())
		{
			if (isValidProfile(id, encryptionRestriction))
				return true;
		}
		return false;
	}

	@Override
	public boolean needsServerSocketImpl() {
		return isServer;
	}
	@Override
	public boolean canTakeConnectionInitiativeImpl() {
		return true;
	}

	@Override
	public boolean supportBidirectionalConnectionInitiativeImpl() {
		return true;
	}

	@Override
	public boolean canBeServer() {
		return true;
	}

	@Override
	public void checkProperties() throws ConnectionException {
		if (secretKeysForSignature==null)
			throw new ConnectionException();
		if (validProfiles==null)
			throw new ConnectionException();
		if (secretKeysForEncryption.size()!=secretKeysForSignature.size())
			throw new ConnectionException();

		if (validProfiles.size()!=secretKeysForSignature.size())
			throw new ConnectionException();
		for (SymmetricSecretKey sk : secretKeysForSignature.values())
			if (sk==null)
				throw new ConnectionException();
		if (enableEncryption) {
			for (SymmetricSecretKey sk : secretKeysForEncryption.values())
				if (sk == null)
					throw new ConnectionException();
		}
	}

	@Override
	public boolean needsMadkitLanEditionDatabase() {
		return false;
	}

	@Override
	public boolean isEncrypted() {
		return enableEncryption;
	}

	private transient MaximumBodyOutputSizeComputer maximumBodyOutputSizeComputer=null;

	@Override
	public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException {
		if (maximumBodyOutputSizeComputer==null) {
			MaximumBodyOutputSizeComputer maximumBodyOutputSizeComputer=null;
			int max=0;
			for (Map.Entry<Integer, Boolean> e : validProfiles.entrySet())
			{
				if (!e.getValue())
					continue;
				int profile=e.getKey();
				MaximumBodyOutputSizeComputer m = new MaximumBodyOutputSizeComputer(isEncrypted(), getSymmetricSecretKeyForEncryption(profile),getSymmetricSecretKeyForSignature(profile) , messageDigestType);
				int v=m.getMaximumBodyOutputSizeForEncryption(size);
				if (v>max)
				{
					max=v;
					maximumBodyOutputSizeComputer=m;
				}
			}
			this.maximumBodyOutputSizeComputer=maximumBodyOutputSizeComputer;
			return max;
		}
		return maximumBodyOutputSizeComputer.getMaximumBodyOutputSizeForEncryption(size);
	}

	@Override
	public int getMaximumHeadSize() {
		return EncryptionSignatureHashEncoder.headSize;
	}

	public int getDefaultProfileIdentifier(EncryptionRestriction encryptionRestriction) {
		if (isValidProfile(defaultProfileIdentifier, encryptionRestriction))
			return defaultProfileIdentifier;
		else
			return lastIdentifier;
	}

	/**
	 * Set the default profile identifier to use with this connection protocol
	 * @param defaultProfileIdentifier the default profile identifier
	 */
	public void setDefaultProfileIdentifier(int defaultProfileIdentifier) {

		this.defaultProfileIdentifier = defaultProfileIdentifier;
	}

}
