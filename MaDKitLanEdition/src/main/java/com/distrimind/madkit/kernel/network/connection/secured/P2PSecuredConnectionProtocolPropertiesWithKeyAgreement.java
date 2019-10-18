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
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MadkitLanEdition 1.7
 */
public class P2PSecuredConnectionProtocolPropertiesWithKeyAgreement extends ConnectionProtocolProperties<P2PSecuredConnectionProtocolWithKeyAgreement> {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -616754777676015639L;

	public P2PSecuredConnectionProtocolPropertiesWithKeyAgreement() {
		super(P2PSecuredConnectionProtocolWithKeyAgreement.class);
	}

	/**
	 * Tells if the connection must be encrypted or not. If not, only signature
	 * packet will be enabled.
	 */
	public boolean enableEncryption = true;

	/**
	 * Key agreement type
	 */
	public KeyAgreementType keyAgreementType=KeyAgreementType.BC_XDH_X448_WITH_SHA512CKDF;

	/**
	 * Post quantum key agreement type
	 */
	public KeyAgreementType postQuantumKeyAgreement=KeyAgreementType.BCPQC_NEW_HOPE;
	
	/**
	 * Symmetric encryption algorithm
	 */
	public SymmetricEncryptionType symmetricEncryptionType = SymmetricEncryptionType.AES_CBC_PKCS5Padding;

	/**
	 * Symmetric signature algorithm
	 */
	public SymmetricAuthentifiedSignatureType symmetricSignatureType=SymmetricAuthentifiedSignatureType.HMAC_SHA2_256;
	
	/**
	 * symmetric key size in bits
	 */
	public short symmetricKeySizeBits=keyAgreementType.getDefaultKeySizeBits();
	
	/**
	 * Tells if the current peer can receive an ask for connection.
	 */
	public boolean isServer = true;

	private HashMap<Integer, AbstractKeyPair> serverSideKeyPairs =new HashMap<>();

	private IASymmetricPublicKey clientSidePublicKey =null;
	private int clientSideProfileIdentifier =-1;

	private int lastIdentifierServerSide =0;

	/**
	 * The profile validation
	 */
	private Map<Integer, Boolean> serverSideValidProfiles = new HashMap<>();

	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param type the asymmetric signature type
	 * @param random the secure random
	 * @return the generated profile identifier
	 * @throws InvalidAlgorithmParameterException if a the key generator parameter was invalid
	 * @throws NoSuchAlgorithmException if the the key generator was not found
	 * @throws NoSuchProviderException if the key generator provider was not found
	 */
	public int generateServerSideProfile(ASymmetricAuthenticatedSignatureType type, AbstractSecureRandom random) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		return generateServerSideProfile(type, random, Long.MAX_VALUE);
	}

	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param type the asymmetric signature type
	 * @param random the secure random
	 * @param expirationTimeUTC the expiration time of the generated key pair
	 * @return the generated profile identifier
	 * @throws InvalidAlgorithmParameterException if a the key generator parameter was invalid
	 * @throws NoSuchAlgorithmException if the the key generator was not found
	 * @throws NoSuchProviderException if the key generator provider was not found
	 */
	public int generateServerSideProfile(ASymmetricAuthenticatedSignatureType type, AbstractSecureRandom random, long expirationTimeUTC) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		return generateServerSideProfile(type, random, expirationTimeUTC, type.getDefaultKeySize());
	}

	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param type the asymmetric signature type
	 * @param random the secure random
	 * @param expirationTimeUTC the expiration time of the generated key pair
	 * @param asymmetricKeySizeBits the asymmetric key size in bits
	 * @return the generated profile identifier
	 * @throws InvalidAlgorithmParameterException if a the key generator parameter was invalid
	 * @throws NoSuchAlgorithmException if the the key generator was not found
	 * @throws NoSuchProviderException if the key generator provider was not found
	 */
	public int generateServerSideProfile(ASymmetricAuthenticatedSignatureType type, AbstractSecureRandom random, long expirationTimeUTC, int asymmetricKeySizeBits) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		return addServerSideProfile(type.getKeyPairGenerator(random, asymmetricKeySizeBits, expirationTimeUTC).generateKeyPair());
	}

	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param type the hybrid asymmetric signature type
	 * @param random the secure random
	 * @return the generated profile identifier
	 * @throws InvalidAlgorithmParameterException if a the key generator parameter was invalid
	 * @throws NoSuchAlgorithmException if the the key generator was not found
	 * @throws NoSuchProviderException if the key generator provider was not found
	 */
	public int generateServerSideProfile(HybridASymmetricAuthenticatedSignatureType type, AbstractSecureRandom random) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		return generateServerSideProfile(type, random, Long.MAX_VALUE);
	}

	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param type the hybrid asymmetric signature type
	 * @param random the secure random
	 * @param expirationTimeUTC the expiration time of the generated key pair
	 * @return the generated profile identifier
	 * @throws InvalidAlgorithmParameterException if a the key generator parameter was invalid
	 * @throws NoSuchAlgorithmException if the the key generator was not found
	 * @throws NoSuchProviderException if the key generator provider was not found
	 */
	public int generateServerSideProfile(HybridASymmetricAuthenticatedSignatureType type, AbstractSecureRandom random, long expirationTimeUTC) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		return generateServerSideProfile(type, random, expirationTimeUTC, -1);
	}


	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param type the hybrid asymmetric signature type
	 * @param random the secure random
	 * @param expirationTimeUTC the expiration time of the generated key pair
	 * @param asymmetricKeySizeBits the asymmetric key size in bits
	 * @return the generated profile identifier
	 * @throws InvalidAlgorithmParameterException if a the key generator parameter was invalid
	 * @throws NoSuchAlgorithmException if the the key generator was not found
	 * @throws NoSuchProviderException if the key generator provider was not found
	 */
	public int generateServerSideProfile(HybridASymmetricAuthenticatedSignatureType type, AbstractSecureRandom random, long expirationTimeUTC, int asymmetricKeySizeBits) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException {
		return addServerSideProfile(type.generateKeyPair(random, asymmetricKeySizeBits, expirationTimeUTC));
	}



	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param keyPairForSignature the key pair used to authenticate the server
	 * @return the effective profile identifier
	 */
	public int addServerSideProfile(AbstractKeyPair keyPairForSignature) {
		return addServerSideProfile(generateNewKeyPairIdentifier(), keyPairForSignature);
	}

	/**
	 * Add a server profile used to authenticate the server throw an asymmetric signature
	 * @param profileIdentifier the profile identifier
	 * @param keyPairForSignature the key pair used to authenticate the server
	 * @return the effective profile identifier
	 */
	public int addServerSideProfile(int profileIdentifier, AbstractKeyPair keyPairForSignature) {
		if (keyPairForSignature == null)
			throw new NullPointerException("keyPairForEncryption");
		if (keyPairForSignature instanceof HybridASymmetricKeyPair)
		{
			if (((HybridASymmetricKeyPair)keyPairForSignature).getASymmetricPublicKey().getNonPQCPublicKey().getAuthenticatedSignatureAlgorithmType()==null)
				throw new IllegalArgumentException();
		}
		else if (((ASymmetricKeyPair)keyPairForSignature).getAuthenticatedSignatureAlgorithmType()==null)
			throw new IllegalArgumentException();
		if (!isServer)
			throw new IllegalStateException();
		if (serverSideKeyPairs ==null)
			serverSideKeyPairs =new HashMap<>();
		if (serverSideKeyPairs.containsKey(profileIdentifier))
			throw new IllegalArgumentException("The profile identifier is already used");

		lastIdentifierServerSide =profileIdentifier;
		serverSideKeyPairs.put(profileIdentifier, keyPairForSignature);
		serverSideValidProfiles.put(profileIdentifier, true);
		return profileIdentifier;
	}

	/**
	 * Set the profile used to check the server signature
	 * @param profileIdentifier the profile identifier
	 * @param publicKeyForSignature the server public key used to check its signature
	 */
	public void setClientSideProfile(int profileIdentifier, IASymmetricPublicKey publicKeyForSignature)
	{
		if (publicKeyForSignature == null)
			throw new NullPointerException("publicKey");
		if (publicKeyForSignature instanceof HybridASymmetricPublicKey)
		{
			if (((HybridASymmetricPublicKey)publicKeyForSignature).getNonPQCPublicKey().getAuthenticatedSignatureAlgorithmType()==null)
				throw new IllegalArgumentException();
		}
		else if (((ASymmetricPublicKey)publicKeyForSignature).getAuthenticatedSignatureAlgorithmType()==null)
			throw new IllegalArgumentException();

		this.clientSideProfileIdentifier =profileIdentifier;
		this.clientSidePublicKey=publicKeyForSignature;
	}

	private int generateNewKeyPairIdentifier() {
		int id=lastIdentifierServerSide+1;
		for (int k : serverSideKeyPairs.keySet())
			if (k>=id)
				id=k+1;
		return id;
	}

	/**
	 * Gets the key pair used for server signature and attached to this connection protocol and the given profile identifier
	 *
	 * @param profileIdentifier
	 *            the profile identifier
	 * @return the key pair attached to this connection protocol and the given
	 *         profile identifier
	 */
	public AbstractKeyPair getKeyPairForSignature(int profileIdentifier) {
		return serverSideKeyPairs.get(profileIdentifier);
	}

	/**
	 * Gets the server public key used to check the server signature and to authenticate it
	 * @return the server public key
	 */
	public IASymmetricPublicKey getClientSidePublicKey() {
		return clientSidePublicKey;
	}

	/**
	 * Gets the server profile identifier used to choose the public key to authenticate the server
	 * @return the server profile identifier
	 */
	public int getClientSideProfileIdentifier() {
		return clientSideProfileIdentifier;
	}

	/**
	 * Tells if a server profile has been configured into this client
	 * @return true if a server profile has been configured into this client
	 * @see #getClientSideProfileIdentifier()
	 * @see #getClientSidePublicKey()
	 */
	public boolean hasClientSideProfileIdentifier()
	{
		return clientSidePublicKey!=null;
	}

	/**
	 * Tells if the given profile identifier is valid
	 * @param profileIdentifier the profile identifier
	 * @return true if the given profile identifier is valid
	 */
	public boolean isValidProfile(int profileIdentifier, EncryptionRestriction encryptionRestriction) {
		Boolean valid= serverSideValidProfiles.get(profileIdentifier);
		if (valid==null || !valid)
			return false;
		AbstractKeyPair kp=serverSideKeyPairs.get(profileIdentifier);
		if (kp!=null && kp.getTimeExpirationUTC()>System.currentTimeMillis())
		{
			if (encryptionRestriction==EncryptionRestriction.NO_RESTRICTION)
				return true;

			if (encryptionRestriction==EncryptionRestriction.HYBRID_ALGORITHMS)
				return kp instanceof HybridASymmetricKeyPair;
			else
				return kp.isPostQuantumKey();
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
		Boolean valid=serverSideValidProfiles.get(profileIdentifier);
		if (valid!=null)
			serverSideValidProfiles.put(profileIdentifier, false);
	}

	private boolean checkKeyPairs(Map<Integer, AbstractKeyPair> keyPairs) throws ConnectionException
	{
		if (keyPairs == null)
			throw new ConnectionException("The key pairs must defined");
		boolean valid = keyPairs.size()==0;
		for (Map.Entry<Integer, AbstractKeyPair> e : keyPairs.entrySet()) {
			if (e.getValue() == null)
				throw new NullPointerException();
			int s;
			if (e.getValue() instanceof HybridASymmetricKeyPair)
			{
				if (((HybridASymmetricKeyPair)e.getValue()).getASymmetricPublicKey().getNonPQCPublicKey().getAuthenticatedSignatureAlgorithmType()==null)
					throw new ConnectionException();
				s=((HybridASymmetricKeyPair)e.getValue()).getNonPQCASymmetricKeyPair().getKeySizeBits();
			}
			else{
				if (((ASymmetricKeyPair)e.getValue()).getAuthenticatedSignatureAlgorithmType()==null)
					throw new ConnectionException();
				s=((ASymmetricKeyPair)e.getValue()).getKeySizeBits();
			}
			Boolean vp=serverSideValidProfiles.get(e.getKey());
			if (e.getValue().getTimeExpirationUTC() > System.currentTimeMillis() && vp!=null && vp) {
				valid = true;
			}
			int tmp=s;
			while (tmp != 1) {
				if (tmp % 2 == 0)
					tmp = tmp / 2;
				else
					throw new ConnectionException("The RSA key size have a size of " + s
							+ ". This number must correspond to this schema : _rsa_key_size=2^x.");
			}
		}
		return valid;
	}

	@Override
	public void checkProperties() throws ConnectionException {

		if (keyAgreementType==null)
			throw new ConnectionException(new NullPointerException("keyAgreementType"));
		if (symmetricEncryptionType==null && enableEncryption)
			throw new ConnectionException(new NullPointerException("symmetricEncryptionType"));
		if (symmetricSignatureType==null)
			throw new ConnectionException(new NullPointerException("symmetricSignatureType"));
		if (keyAgreementType.isPostQuantumAlgorithm() && enableEncryption && !symmetricEncryptionType.isPostQuantumAlgorithm(symmetricKeySizeBits))
			throw new ConnectionException("The key agreement is a post quantum cryptography. However, the given symmetric encryption algorithm associated with the given symmetric key size are not post quantum compatible algorithms.");
		if (keyAgreementType.isPostQuantumAlgorithm() && !symmetricSignatureType.isPostQuantumAlgorithm(symmetricKeySizeBits))
			throw new ConnectionException("The key agreement is a post quantum cryptography. However, the given symmetric signature algorithm associated with the given symmetric signature size are not post quantum compatible algorithms.");
		if (postQuantumKeyAgreement!=null && !postQuantumKeyAgreement.isPostQuantumAlgorithm())
			throw new ConnectionException("The variable postQuantumKeyAgreement can be null be must a post quantum type when defined.");
		if (postQuantumKeyAgreement!=null && !symmetricSignatureType.isPostQuantumAlgorithm(symmetricKeySizeBits))
			throw new ConnectionException("The key agreement is a post quantum cryptography. However, the given symmetric signature algorithm associated with the given symmetric signature size are not post quantum compatible algorithms.");
		if (postQuantumKeyAgreement!=null && keyAgreementType.isPostQuantumAlgorithm())
			throw new ConnectionException("Hybrid connexion must use a non post quantum algorithm with a post quantum algorithm");

		boolean valid = checkKeyPairs(serverSideKeyPairs);

		if (!valid) {
			throw new ConnectionException("All given public keys has expired");
		}

		if (hasClientSideProfileIdentifier())
		{
			if (clientSidePublicKey==null)
				throw new ConnectionException();
			if (clientSidePublicKey instanceof HybridASymmetricPublicKey)
			{
				if (((HybridASymmetricPublicKey)clientSidePublicKey).getNonPQCPublicKey().getAuthenticatedSignatureAlgorithmType()==null)
					throw new IllegalArgumentException();
			}
			else if (((ASymmetricPublicKey)clientSidePublicKey).getAuthenticatedSignatureAlgorithmType()==null)
				throw new IllegalArgumentException();
			if (clientSidePublicKey.getTimeExpirationUTC() < System.currentTimeMillis()) {
				throw new ConnectionException("The given public key has expired");
			}
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

	private transient SymmetricEncryptionAlgorithm maxAlgo=null;

	@Override
	public int getMaximumBodyOutputSizeForEncryption(int size) throws BlockParserException {
		if (!isEncrypted())
			return size;
		else
		{

			try {
				if (maxAlgo==null)
					maxAlgo=new SymmetricEncryptionAlgorithm(SecureRandomType.DEFAULT.getSingleton(null), symmetricEncryptionType.getKeyGenerator(SecureRandomType.DEFAULT.getSingleton(null), symmetricKeySizeBits).generateKey());
				return maxAlgo.getOutputSizeForEncryption(size)+4;
			} catch (Exception e) {
				throw new BlockParserException(e);
			}

		}
	}



	private transient volatile Integer maxHeadSize=null;
    @Override
    public int getMaximumSizeHead() throws BlockParserException {
        if (maxHeadSize==null)
		{
            try {

				SymmetricAuthenticatedSignerAlgorithm signerTmp = new SymmetricAuthenticatedSignerAlgorithm(symmetricSignatureType.getKeyGenerator(SecureRandomType.DEFAULT.getSingleton(null), symmetricEncryptionType.getDefaultKeySizeBits()).generateKey());
                signerTmp.init();
                maxHeadSize = signerTmp.getMacLengthBytes();

            } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidKeyException | InvalidKeySpecException e) {
                throw new BlockParserException(e);
            }
        }
        return maxHeadSize;
    }

	@Override
	public boolean isConcernedBy(EncryptionRestriction encryptionRestriction) {
    	boolean found=serverSideValidProfiles.size()==0;
    	for (Integer k : serverSideValidProfiles.keySet())
		{
			if (isValidProfile(k, encryptionRestriction))
			{
				found=true;
				break;
			}
		}
    	if (!found)
    		return false;
		if (encryptionRestriction==EncryptionRestriction.HYBRID_ALGORITHMS && hasClientSideProfileIdentifier() && !(clientSidePublicKey instanceof HybridASymmetricPublicKey))
			return false;
		else
			if (encryptionRestriction==EncryptionRestriction.POST_QUANTUM_ALGORITHMS && hasClientSideProfileIdentifier() && !clientSidePublicKey.isPostQuantumKey())
				return false;

		if (subProtocolProperties!=null && subProtocolProperties.isConcernedBy(encryptionRestriction))
			return true;

		if (encryptionRestriction==EncryptionRestriction.NO_RESTRICTION)
    		return true;
    	if (enableEncryption && !symmetricEncryptionType.isPostQuantumAlgorithm(symmetricKeySizeBits))
    		return false;
    	if (symmetricSignatureType.isPostQuantumAlgorithm(symmetricKeySizeBits))
    		return false;
    	if (encryptionRestriction==EncryptionRestriction.HYBRID_ALGORITHMS) {
			if (postQuantumKeyAgreement == null || postQuantumKeyAgreement.isPostQuantumAlgorithm() || keyAgreementType.isPostQuantumAlgorithm())
				return false;
		}
    	else
    	{
			return (postQuantumKeyAgreement==null && keyAgreementType.isPostQuantumAlgorithm())
					|| (postQuantumKeyAgreement!=null && !keyAgreementType.isPostQuantumAlgorithm() && postQuantumKeyAgreement.isPostQuantumAlgorithm());
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

}
