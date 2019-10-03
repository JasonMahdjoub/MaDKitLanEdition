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
package com.distrimind.madkit.kernel.network.connection.secured;

import com.distrimind.madkit.exceptions.BlockParserException;
import com.distrimind.madkit.exceptions.ConnectionException;
import com.distrimind.madkit.kernel.network.EncryptionRestriction;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocolProperties;
import com.distrimind.util.crypto.*;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public class ServerSecuredProtocolPropertiesWithKnownPublicKey
		extends ConnectionProtocolProperties<ServerSecuredConnectionProtocolWithKnwonPublicKey> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4979144000199527880L;

	public ServerSecuredProtocolPropertiesWithKnownPublicKey() {
		super(ServerSecuredConnectionProtocolWithKnwonPublicKey.class);
	}

	/**
	 * Generate and add an encryption profile with a new key pair, etc.
	 * 
	 * @param random
	 *            a secured random number generator
	 * @param as_type
	 *            tge asymmetric encryption type
	 * @param s_type
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param keyWrapper the key wrapper type
	 * @return the encryption profile identifier
	 * @throws NoSuchAlgorithmException if the encryption algorithm was not found
	 * @throws InvalidAlgorithmParameterException if the encryption algorithm parameter was not valid
	 * @throws NoSuchProviderException if the encryption algorithm provider was not found 
	 */
	public int generateAndAddEncryptionProfile(AbstractSecureRandom random, ASymmetricEncryptionType as_type,
			SymmetricEncryptionType s_type, ASymmetricKeyWrapperType keyWrapper) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return addEncryptionProfile(as_type.getKeyPairGenerator(random).generateKeyPair(), s_type, s_type.getDefaultKeySizeBits(), keyWrapper, null);
	}

	/**
	 * Generate and add an encryption profile with a new key pair, etc.
	 *
	 * @param random
	 *            a secured random number generator
	 * @param as_type
	 *            tge asymmetric encryption type
	 * @param s_type
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param keyWrapper the key wrapper type
	 * @return the encryption profile identifier
	 * @throws NoSuchAlgorithmException if the encryption algorithm was not found
	 * @throws InvalidAlgorithmParameterException if the encryption algorithm parameter was not valid
	 * @throws NoSuchProviderException if the encryption algorithm provider was not found
	 */
	public int generateAndAddEncryptionProfile(AbstractSecureRandom random, HybridASymmetricEncryptionType as_type,
											   SymmetricEncryptionType s_type, ASymmetricKeyWrapperType keyWrapper) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return addEncryptionProfile(as_type.generateKeyPair(random), s_type, s_type.getDefaultKeySizeBits(), keyWrapper, null);
	}

	/**
	 * Generate and add an encryption profile with a new key pair, etc.
	 * 
	 * @param random
	 *            a secured random number generator
	 * @param as_type
	 *            tge asymmetric encryption type
	 * @param expirationTimeUTC
	 *            the UTC expiration time of the key pair
	 * @param asymmetricKeySizeBits
	 *            the asymmetric key size in bits
	 * 
	 * @param s_type
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param symmetricKeySizeBits
	 *            the signature type (if null, use default signature type)
	 * @param keyWrapper the key wrapper type
	 * @param signatureType the signature type
	 * @return the encryption profile identifier
	 * @throws NoSuchAlgorithmException if the encryption algorithm was not found
	 * @throws InvalidAlgorithmParameterException if the encryption algorithm parameter was not valid
	 * @throws NoSuchProviderException if the encryption algorithm provider was not found
	 */
	public int generateAndAddEncryptionProfile(AbstractSecureRandom random, ASymmetricEncryptionType as_type,
			long expirationTimeUTC, short asymmetricKeySizeBits,
			SymmetricEncryptionType s_type, short symmetricKeySizeBits, ASymmetricKeyWrapperType keyWrapper, SymmetricAuthentifiedSignatureType signatureType) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
		return addEncryptionProfile(
				as_type.getKeyPairGenerator(random, asymmetricKeySizeBits, expirationTimeUTC).generateKeyPair(),
				s_type, symmetricKeySizeBits, keyWrapper, signatureType);
	}

	/**
	 * Add an encryption profile with a new key pair, etc.
	 *
	 * @param keyPairForEncryption
	 *            the key pair for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param keyWrapper the key wrapper type
	 * @return the encryption profile identifier
	 */
	public int addEncryptionProfile(ASymmetricKeyPair keyPairForEncryption, SymmetricEncryptionType symmetricEncryptionType, ASymmetricKeyWrapperType keyWrapper) {
		return this.addEncryptionProfile(generateNewKeyPairIdentifier(), keyPairForEncryption, symmetricEncryptionType, keyWrapper);

	}
	/**
	 * Add an encryption profile with a new key pair, etc.
	 *
	 * @param profileIdentifier the profile identifier
	 * @param keyPairForEncryption
	 *            the key pair for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param keyWrapper the key wrapper type
	 * @return the encryption profile identifier
	 */
	public int addEncryptionProfile(int profileIdentifier, ASymmetricKeyPair keyPairForEncryption, SymmetricEncryptionType symmetricEncryptionType, ASymmetricKeyWrapperType keyWrapper) {
		return this.addEncryptionProfile(profileIdentifier, keyPairForEncryption, symmetricEncryptionType,
				symmetricEncryptionType == null ? (short) -1 : symmetricEncryptionType.getDefaultKeySizeBits(), keyWrapper, null);
	}

	/**
	 * Add an encryption profile with a new key pair, etc.
	 *
	 *
	 * @param keyPairForEncryption
	 *            the key pair for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param symmetricKeySizeBits
	 *            the symmetric key size in bits
	 * @param keyWrapper the key wrapper type
	 * @param signatureType
	 *            the signature type (if null, use default signature type)
	 * @return the encryption profile identifier
	 */
	public int addEncryptionProfile(AbstractKeyPair keyPairForEncryption,
									SymmetricEncryptionType symmetricEncryptionType, short symmetricKeySizeBits, ASymmetricKeyWrapperType keyWrapper, SymmetricAuthentifiedSignatureType signatureType) {
		return addEncryptionProfile(generateNewKeyPairIdentifier(), keyPairForEncryption, symmetricEncryptionType, symmetricKeySizeBits, keyWrapper, signatureType);
	}
	/**
	 * Add an encryption profile with a new key pair, etc.
	 *
	 * @param profileIdentifier the profile identifier
	 * @param keyPairForEncryption
	 *            the key pair for encryption
	 * @param symmetricEncryptionType
	 *            the symmetric encryption type (if null, use default encryption
	 *            type)
	 * @param symmetricKeySizeBits
	 *            the symmetric key size in bits
	 * @param keyWrapper the key wrapper type
	 * @param signatureType
	 *            the signature type (if null, use default signature type)
	 * @return the encryption profile identifier
	 */
	public int addEncryptionProfile(int profileIdentifier, AbstractKeyPair keyPairForEncryption,
			SymmetricEncryptionType symmetricEncryptionType, short symmetricKeySizeBits, ASymmetricKeyWrapperType keyWrapper, SymmetricAuthentifiedSignatureType signatureType) {
		if (keyPairForEncryption == null)
			throw new NullPointerException("keyPairForEncryption");
		if (keyPairsForEncryption.containsKey(profileIdentifier))
			throw new IllegalArgumentException("The profile identifier is already used");
		lastIdentifier=profileIdentifier;
		keyPairsForEncryption.put(profileIdentifier, keyPairForEncryption);
		validProfiles.put(profileIdentifier, true);
		maxAlgos=null;
		maxSizeHead=null;
		if (symmetricEncryptionType == null) {
			symmetricEncryptionType = SymmetricEncryptionType.DEFAULT;
			symmetricKeySizeBits = symmetricEncryptionType.getDefaultKeySizeBits();
		}
		symmetricEncryptionTypes.put(profileIdentifier, symmetricEncryptionType);
		symmetricEncryptionKeySizeBits.put(profileIdentifier, symmetricKeySizeBits);
		if (signatureType == null)
			signatures.put(profileIdentifier, symmetricEncryptionType.getDefaultSignatureAlgorithm());
		else
			signatures.put(profileIdentifier, signatureType);
		
		if (keyWrapper==null)
			keyWrapper=ASymmetricKeyWrapperType.DEFAULT;
		keyWrappers.put(profileIdentifier, keyWrapper);
			
		
		return profileIdentifier;
	}

	/**
	 * Gets the key pair used for encryption and attached to this connection protocol and the given profile
	 * identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the key pair attached to this connection protocol and the given
	 *         profile identifier
	 */
	public AbstractKeyPair getKeyPairForEncryption(int profileIdentifier) {
		return keyPairsForEncryption.get(profileIdentifier);
	}

	/**
	 * Tells if the given profile identifier is valid
	 * @param profileIdentifier the profile identifier
	 * @return true if the given profile identifier is valid
	 */
	public boolean isValidProfile(int profileIdentifier, EncryptionRestriction encryptionRestriction) {
	    Boolean valid=validProfiles.get(profileIdentifier);
	    if (valid==null || !valid)
	        return false;
		AbstractKeyPair kp=keyPairsForEncryption.get(profileIdentifier);
		if (kp!=null && kp.getTimeExpirationUTC()>System.currentTimeMillis())
		{
			if (encryptionRestriction==EncryptionRestriction.NO_RESTRICTION)
				return true;
			if (!this.signatures.get(profileIdentifier).isPostQuantumAlgorithm(this.symmetricEncryptionKeySizeBits.get(profileIdentifier)))
				return false;

			if (enableEncryption && !this.symmetricEncryptionTypes.get(profileIdentifier).isPostQuantumAlgorithm(this.symmetricEncryptionKeySizeBits.get(profileIdentifier)))
				return false;
			if (encryptionRestriction==EncryptionRestriction.HYBRID_ALGORITHMS)
				return kp instanceof HybridASymmetricKeyPair && !keyWrappers.get(profileIdentifier).isPostQuantumKeyAlgorithm();
			else
				return kp.isPostQuantumKey() && keyWrappers.get(profileIdentifier).isPostQuantumKeyAlgorithm();
		}
		else
			return false;
	}

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


	/**
	 * Gets the signature type attached to this connection protocol and the given
	 * profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the signature type attached to this connection protocol and the given
	 *         profile identifier
	 */
	public SymmetricAuthentifiedSignatureType getSignatureType(int profileIdentifier) {
		return signatures.get(profileIdentifier);
	}
	
	/**
	 * Gets the key wrapper attached to this connection protocol and the given profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the key wrapper attached to this connection protocol and the given profile identifier
	 */
	public ASymmetricKeyWrapperType getKeyWrapper(int profileIdentifier) {
		return keyWrappers.get(profileIdentifier);
	}
	

	public int getMaximumSignatureSizeBits() {
		int res = 0;
		for (SymmetricAuthentifiedSignatureType v : signatures.values()) {
			res = Math.max(res, v.getSignatureSizeInBits());
		}
		return res;
	}

	/**
	 * Gets the symmetric encryption type attached to this connection protocol and
	 * the given profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the symmetric encryption type attached to this connection protocol
	 *         and the given profile identifier
	 */
	public SymmetricEncryptionType getSymmetricEncryptionType(int profileIdentifier) {
		return symmetricEncryptionTypes.get(profileIdentifier);
	}

	/**
	 * Gets the symmetric encryption key size in bits attached to this connection
	 * protocol and the given profile identifier
	 * 
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the symmetric encryption key size in bits attached to this connection
	 *         protocol and the given profile identifier
	 */
	public short getSymmetricEncryptionKeySizeBits(int profileIdentifier) {
		return symmetricEncryptionKeySizeBits.get(profileIdentifier);
	}

	/**
	 * Gets the default key pair (for encryption) attached to this connection protocol and its
	 * default profile
	 * 
	 * @return the default key pair attached to this connection protocol and its
	 *         default profile
	 */
	public AbstractKeyPair getDefaultKeyPairForEncryption() {
		return keyPairsForEncryption.get(lastIdentifier);
	}

	/**
	 * Gets the default signature type attached to this connection protocol and its
	 * default profile
	 * 
	 * @return the default signature type attached to this connection protocol and
	 *         its default profile
	 */
	public SymmetricAuthentifiedSignatureType getDefaultSignatureType() {
		return signatures.get(lastIdentifier);
	}
	/**
	 * Gets the default key wrapper attached to this connection protocol and its
	 * default profile
	 * 
	 * @return the default key wrapper attached to this connection protocol and its
	 * default profile
	 */
	public ASymmetricKeyWrapperType getDefaultKeyWrapper() {
		return keyWrappers.get(lastIdentifier);
	}

	/**
	 * Gets the default symmetric encryption type type attached to this connection
	 * protocol and its default profile
	 * 
	 * @return the default symmetric encryption type attached to this connection
	 *         protocol and its default profile
	 */
	public SymmetricEncryptionType getDefaultSymmetricEncryptionType() {
		return symmetricEncryptionTypes.get(lastIdentifier);
	}

	/**
	 * Gets the default symmetric encryption key size in bits attached to this
	 * connection protocol and its default profile
	 * 
	 * @return the default symmetric encryption key size in bits attached to this
	 *         connection protocol and its default profile
	 */
	public short getDefaultSymmetricEncryptionKeySizeBits() {
		return symmetricEncryptionKeySizeBits.get(lastIdentifier);
	}

	/**
	 * Gets the last encryption profile identifier
	 * 
	 * @return the last encryption profile identifier
	 */
	public int getLastEncryptionProfileIdentifier() {
		return lastIdentifier;
	}

	/*
	 * public Map<Integer, ASymmetricPublicKey> getPublicKeys() { Map<Integer,
	 * ASymmetricPublicKey> res=new HashMap<>(); for (Map.Entry<Integer,
	 * ASymmetricKeyPair> e : keyPairs.entrySet()) { res.put(e.getKey(),
	 * e.getValue().getASymmetricPublicKey()); } return res; }
	 * 
	 * public Map<Integer, SignatureType> getSignatures() { Map<Integer,
	 * SignatureType> res=new HashMap<>(); for (Map.Entry<Integer, SignatureType> e
	 * : signatures.entrySet()) { res.put(e.getKey(), e.getValue()); } return res; }
	 */

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public boolean enableEncryption = true;


	/**
	 * The profile validation
	 */
	private Map<Integer, Boolean> validProfiles = new HashMap<>();

	/**
	 * The used key pairs for encryption
	 */
	private Map<Integer, AbstractKeyPair> keyPairsForEncryption = new HashMap<>();

	/**
	 * The used signatures
	 */
	private Map<Integer, SymmetricAuthentifiedSignatureType> signatures = new HashMap<>();

	/**
	 * The used key wrappers
	 */
	private Map<Integer, ASymmetricKeyWrapperType> keyWrappers = new HashMap<>();
	
	private int lastIdentifier = 0;

	private int generateNewKeyPairIdentifier() {
		int id=lastIdentifier+1;
		for (int k : keyPairsForEncryption.keySet())
			if (k>=id)
				id=k+1;
		return id;
	}

	/**
	 * The minimum asymetric cipher RSA Key size
	 */
	public final int minASymetricKeySizeBits = 2048;

	/**
	 * Symmetric encryption algorithm
	 */
	private Map<Integer, SymmetricEncryptionType> symmetricEncryptionTypes = new HashMap<>();

	/**
	 * Symmetric encryption key sizes bits
	 */
	private Map<Integer, Short> symmetricEncryptionKeySizeBits = new HashMap<>();



	private boolean checkKeyPairs(Map<Integer, AbstractKeyPair> keyPairs) throws ConnectionException
	{
		if (keyPairs == null)
			throw new ConnectionException("The key pairs must defined");
		if (keyPairs.isEmpty())
			throw new ConnectionException("The key pairs must defined");
		boolean valid = false;
		for (Map.Entry<Integer, AbstractKeyPair> e : keyPairs.entrySet()) {
			if (e.getValue() == null)
				throw new NullPointerException();
			Boolean vp=validProfiles.get(e.getKey());
			if (e.getValue().getTimeExpirationUTC() > System.currentTimeMillis() && vp!=null && vp) {
				valid = true;
			}
			int s;
			if (e.getValue() instanceof HybridASymmetricKeyPair)
				s=((HybridASymmetricKeyPair)e.getValue()).getNonPQCASymmetricKeyPair().getKeySizeBits();
			else
				s=((ASymmetricKeyPair)e.getValue()).getKeySizeBits();
			int tmp=s;
			while (tmp != 1) {
				if (tmp % 2 == 0)
					tmp = tmp / 2;
				else
					throw new ConnectionException("The RSA key size have a size of " + s
							+ ". This number must correspond to this schema : _rsa_key_size=2^x.");
			}
			if (signatures.get(e.getKey()) == null)
				throw new NullPointerException("No signature found for identifier " + e.getKey());
			if (symmetricEncryptionTypes.get(e.getKey()) == null)
				throw new NullPointerException("No symmetric encryption type found for identifier " + e.getKey());
			if (symmetricEncryptionKeySizeBits.get(e.getKey()) == null)
				throw new NullPointerException(
						"No symmetric encryption key size bits found for identifier " + e.getKey());
		}
		AbstractKeyPair akp=keyPairs.get(this.lastIdentifier);
		int s;
		if (akp instanceof HybridASymmetricKeyPair)
			s=((HybridASymmetricKeyPair)akp).getNonPQCASymmetricKeyPair().getKeySizeBits();
		else
			s=((ASymmetricKeyPair)akp).getKeySizeBits();
		if (s < minASymetricKeySizeBits)
			throw new ConnectionException("_rsa_key_size must be greater or equal than " + minASymetricKeySizeBits
					+ " . Moreover, this number must correspond to this schema : _rsa_key_size=2^x.");
		return valid;
	}

    @Override
    public void checkProperties() throws ConnectionException {
		boolean valid = checkKeyPairs(keyPairsForEncryption);
		
		if (!valid) {
			throw new ConnectionException("All given public keys has expired");
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



    @Override
	public boolean needsServerSocketImpl() {
		return true;
	}

	@Override
	public boolean canTakeConnectionInitiativeImpl() {
		return false;
	}

	@Override
	public boolean supportBidirectionalConnectionInitiativeImpl() {
		return false;
	}

	@Override
	public boolean canBeServer() {
		return true;
	}
	private transient List<SymmetricEncryptionAlgorithm> maxAlgos=null;
	@Override
	public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException
	{
	    if (!isEncrypted())
	        return size;
		try {
			int res=0;
			if (maxAlgos==null) {
				List<SymmetricEncryptionAlgorithm> maxAlgos=new ArrayList<>(this.symmetricEncryptionTypes.size());
				for (Map.Entry<Integer, SymmetricEncryptionType> e : this.symmetricEncryptionTypes.entrySet()) {
					maxAlgos.add(new SymmetricEncryptionAlgorithm(SecureRandomType.DEFAULT.getInstance(null), e.getValue().getKeyGenerator(SecureRandomType.DEFAULT.getInstance(null), this.symmetricEncryptionKeySizeBits.get(e.getKey())).generateKey()));
				}
                this.maxAlgos=maxAlgos;
			}
			for (SymmetricEncryptionAlgorithm maxAlgo : maxAlgos)
			{
				int v=maxAlgo.getOutputSizeForEncryption(size)+4;
				if (v>=res)
				{
					res=v;
				}
			}

			return res;
		} catch (Exception e) {
			throw new BlockParserException(e);
		}
	}

	private transient Integer maxSizeHead=null;

    @Override
    public int getMaximumSizeHead() {
        if (maxSizeHead==null)
            maxSizeHead=getMaximumSignatureSizeBits()/8;
        return maxSizeHead;
    }

	@Override
	public boolean isConcernedBy(EncryptionRestriction encryptionRestriction) {
		if (subProtocolProperties!=null && subProtocolProperties.isConcernedBy(encryptionRestriction))
			return true;

		for (Integer k:  this.keyPairsForEncryption.keySet())
		{
			if (isValidProfile(k, encryptionRestriction))
				return true;
		}
		return false;

	}

}
