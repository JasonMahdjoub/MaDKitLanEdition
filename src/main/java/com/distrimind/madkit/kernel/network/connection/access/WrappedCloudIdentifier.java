package com.distrimind.madkit.kernel.network.connection.access;
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

import com.distrimind.util.crypto.*;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import com.distrimind.util.io.SerializationTools;

import java.io.IOException;
import java.security.*;
import java.util.Arrays;
import java.util.Objects;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
final class WrappedCloudIdentifier extends CloudIdentifier {
	public final static int MAX_SIGNATURE_SIZE;
	static
	{
		int s=0;
		for (ASymmetricAuthenticatedSignatureType sig : ASymmetricAuthenticatedSignatureType.values())
		{
			@SuppressWarnings("deprecation") int s2=sig.getSignatureSizeBits(sig.getDefaultKeySize()*4);
			if (s2>s)
				s=s2;
		}
		MAX_SIGNATURE_SIZE=s;
	}
	private CloudIdentifier cloudIdentifier;
	private byte[] signature;

	@SuppressWarnings("unused")
	WrappedCloudIdentifier getInvalidWrappedCloudIdentifier(AbstractSecureRandom random)
	{
		WrappedCloudIdentifier res=new WrappedCloudIdentifier();
		if (cloudIdentifier instanceof EncryptedCloudIdentifier)
			res.cloudIdentifier=((EncryptedCloudIdentifier)cloudIdentifier).getRandomEncryptedCloudIdentifier(random);
		else
			res.cloudIdentifier=cloudIdentifier;
		res.signature=new byte[signature.length];
		random.nextBytes(res.signature);
		return res;
	}
	WrappedCloudIdentifier()
	{

	}
	WrappedCloudIdentifier(boolean anonymize, CloudIdentifier cloudIdentifier, AbstractSecureRandom random, AbstractMessageDigest messageDigest, byte[] distantGeneratedSalt) throws NoSuchAlgorithmException, IOException, NoSuchProviderException {

		if (cloudIdentifier == null)
			throw new NullPointerException("cloudIdentifier");

		if (cloudIdentifier instanceof EncryptedCloudIdentifier)
		{
			throw new IllegalArgumentException();
		}
		if (anonymize) {
			this.cloudIdentifier = new EncryptedCloudIdentifier(cloudIdentifier, random, messageDigest, distantGeneratedSalt);
			if (cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey())
				this.signature=Identifier.signAuthenticatedIdentifier(cloudIdentifier.getAuthenticationKeyPair(), this.cloudIdentifier.getIdentifierBytes());
			else
				this.signature=null;
		}
		else {
			this.cloudIdentifier = cloudIdentifier;
			if (cloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey()) {
				if (random == null)
					throw new NullPointerException("random");
				int mds=messageDigest.getDigestLengthInBytes();
				byte[] localGeneratedSalt=new byte[mds];
				random.nextBytes(localGeneratedSalt);
				if (distantGeneratedSalt.length!=localGeneratedSalt.length)
					throw new IllegalArgumentException();
				byte[] encodedIdentifier=cloudIdentifier.getBytesTabToEncode();
				byte[] s=Identifier.signAuthenticatedIdentifier(cloudIdentifier.getAuthenticationKeyPair(), encodedIdentifier, localGeneratedSalt, distantGeneratedSalt);
				this.signature=new byte[mds+s.length];
				System.arraycopy(localGeneratedSalt, 0, this.signature, 0, mds);
				System.arraycopy(s, 0, this.signature, mds, s.length);
			}
			else
				this.signature=null;

		}

	}

	@Override
	public boolean equalsTimeConstant(CloudIdentifier o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		WrappedCloudIdentifier that = (WrappedCloudIdentifier) o;
		return cloudIdentifier.equalsTimeConstant(that.cloudIdentifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(cloudIdentifier);
	}

	@Override
	public byte[] getIdentifierBytes() {
		return cloudIdentifier.getIdentifierBytes();
	}

	@Override
	public byte[] getSaltBytes() {
		return cloudIdentifier.getSaltBytes();
	}

	@Override
	public boolean mustBeAnonymous() {
		return cloudIdentifier.mustBeAnonymous();
	}

	@Override
	public Identifier.AuthenticationMethod getAuthenticationMethod() {
		return cloudIdentifier.getAuthenticationMethod();
	}

	@Override
	public IASymmetricPublicKey getAuthenticationPublicKey() {
		return cloudIdentifier.getAuthenticationPublicKey();
	}

	@Override
	public AbstractKeyPair<?,?> getAuthenticationKeyPair() {
		return cloudIdentifier.getAuthenticationKeyPair();
	}

	@Override
	public int getInternalSerializedSize() {
		return SerializationTools.getInternalSize(signature, MAX_SIGNATURE_SIZE)+SerializationTools.getInternalSize(cloudIdentifier);
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream out) throws IOException {
		out.writeObject(cloudIdentifier, false);
		out.writeBytesArray(signature, true, MAX_SIGNATURE_SIZE);
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		cloudIdentifier=in.readObject(false, CloudIdentifier.class);
		signature=in.readBytesArray(true, MAX_SIGNATURE_SIZE);

	}

	CloudIdentifier getCloudIdentifier()
	{
		return cloudIdentifier;
	}


	boolean checkSignature(CloudIdentifier originalCloudIdentifier,
												  AbstractMessageDigest messageDigest, byte[] localGeneratedSalt) throws
			NoSuchAlgorithmException, IOException, NoSuchProviderException {
		if (originalCloudIdentifier.getAuthenticationMethod().isAuthenticatedByPublicKey())
		{
			if (cloudIdentifier instanceof EncryptedCloudIdentifier)
			{
				return Identifier.checkAuthenticatedSignature(originalCloudIdentifier.getAuthenticationPublicKey(), this.signature,  this.cloudIdentifier.getIdentifierBytes());
			}
			else {
				if (signature == null)
					return false;
				int mds = messageDigest.getDigestLengthInBytes();
				if (signature.length <= mds)
					return false;
				byte[] encodedIdentifier = originalCloudIdentifier.getBytesTabToEncode();
				return Identifier.checkAuthenticatedSignature(originalCloudIdentifier.getAuthenticationPublicKey(), signature, mds, signature.length - mds, encodedIdentifier, Arrays.copyOfRange(this.signature, 0, mds), localGeneratedSalt);
			}
		}
		else
			return signature==null;
	}


}
