package com.distrimind.madkit.kernel.network;
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

import com.distrimind.madkit.kernel.network.connection.access.HostIdentifier;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.crypto.*;
import com.distrimind.util.data_buffers.WrappedData;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import com.distrimind.util.io.SerializationTools;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
public class CustomAutoSignedHostIdentifier extends HostIdentifier {

	private AbstractKeyPair<?, ?> keyPair;
	private IASymmetricPublicKey publicKey;

	@SuppressWarnings("unused")
	CustomAutoSignedHostIdentifier()
	{

	}

	@Override
	public int getInternalSerializedSize() {
		return SerializationTools.getInternalSize(publicKey);
	}

	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		publicKey=in.readObject( false, IASymmetricPublicKey.class);
		keyPair=null;
	}
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException
	{
		oos.writeObject(publicKey, false);
	}

	CustomAutoSignedHostIdentifier(AbstractSecureRandom random, ASymmetricAuthenticatedSignatureType type) throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
		keyPair=type.getKeyPairGenerator(random).generateKeyPair();
		publicKey=keyPair.getASymmetricPublicKey();
	}
	CustomAutoSignedHostIdentifier(AbstractSecureRandom random, ASymmetricAuthenticatedSignatureType type, ASymmetricAuthenticatedSignatureType typePQC) throws IOException, NoSuchAlgorithmException, NoSuchProviderException {
		keyPair=new HybridASymmetricAuthenticatedSignatureType(type, typePQC).generateKeyPair(random);
		publicKey=keyPair.getASymmetricPublicKey();
	}

	@Override
	public boolean equals(Object _cloud_identifier) {
		if (_cloud_identifier == null)
			return false;
		if (_cloud_identifier instanceof CustomAutoSignedHostIdentifier) {
			CustomAutoSignedHostIdentifier cci = (CustomAutoSignedHostIdentifier) _cloud_identifier;
			return publicKey.equals(cci.publicKey);
		}
		return false;
	}

	@Override
	public String toString() {
		return publicKey.toString();
	}

	@Override
	public int hashCode() {
		return publicKey.hashCode();
	}

	@Override
	public WrappedData getBytesTabToEncode() {
		return publicKey.encode();
	}

	@Override
	public IASymmetricPublicKey getAuthenticationPublicKey() {
		return publicKey;
	}

	@Override
	public AbstractKeyPair<?, ?> getAuthenticationKeyPair() {
		return keyPair;
	}

	@Override
	public boolean isAuthenticatedByPublicKey() {
		return true;
	}

	@Override
	public DecentralizedValue getDecentralizedDatabaseID() {
		return getAuthenticationPublicKey();
	}

	@Override
	public DecentralizedValue getCentralDecentralizedDatabaseID() {
		return getAuthenticationPublicKey();
	}
}
