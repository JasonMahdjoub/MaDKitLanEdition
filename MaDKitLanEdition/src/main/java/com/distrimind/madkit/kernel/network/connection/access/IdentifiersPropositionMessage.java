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

import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.util.crypto.AbstractSecureRandom;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
class IdentifiersPropositionMessage extends AccessMessage {

	public static final int MAX_SIGNATURE_SIZE=WrappedCloudIdentifier.MAX_SIGNATURE_SIZE;

	private Identifier[] identifiers;
	private byte[][] hostSignatures;
	private final transient short nbAnomalies;

	@SuppressWarnings("unused")
	IdentifiersPropositionMessage()
	{
		nbAnomalies=0;
	}

	public Identifier[] getIdentifiers() {
		return identifiers;
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {

		SecureExternalizable[] s=in.readObject(false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, SecureExternalizable[].class);
		identifiers=new Identifier[s.length];
		for (int i=0;i<s.length;i++)
		{
			Object o=s[i];
			if (o==null || !(o instanceof Identifier))
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			identifiers[i]=(Identifier)o;
		}
		hostSignatures=in.read2DBytesArray(false, true, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, MAX_SIGNATURE_SIZE);
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {

		oos.writeObject(identifiers, false, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE);
		oos.write2DBytesArray(hostSignatures, false, true, NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE, MAX_SIGNATURE_SIZE);
		if (identifiers.length!=hostSignatures.length)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		for (int i=0;i<identifiers.length;i++)
		{
			Identifier id=identifiers[i];
			if (id==null)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			if (id.getHostIdentifier().isAuthenticatedByPublicKey()) {
				if (id.getHostIdentifier().getAuthenticationPublicKey()==null)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				if (hostSignatures[i]==null)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
				if (hostSignatures[i].length<=saltSizeBytes)
					throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			}
			else
			if (hostSignatures[i]!=null)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);

		}

	}

	private static final int saltSizeBytes=32;

	public IdentifiersPropositionMessage(Collection<Identifier> _id_pws, short nbAnomalies,
										 byte[] distantGeneratedSalt, AbstractSecureRandom random) throws InvalidKeyException, NoSuchAlgorithmException, IOException, SignatureException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeySpecException {
		identifiers = new Identifier[_id_pws.size()];
		hostSignatures=new byte[_id_pws.size()][];
		int index=0;
		for (Identifier ip : _id_pws) {
			identifiers[index]=ip;
			if (ip.getHostIdentifier().isAuthenticatedByPublicKey() && ip.getHostIdentifier().getAuthenticationPublicKey()!=null && ip.getHostIdentifier().getAuthenticationKeyPair()!=null) {

				byte[] localGeneratedSalt=new byte[saltSizeBytes];
				random.nextBytes(localGeneratedSalt);
				if (distantGeneratedSalt.length!=localGeneratedSalt.length)
					throw new IllegalArgumentException();
				byte[] encodedIdentifier=ip.getHostIdentifier().getBytesTabToEncode();
				byte[] s=Identifier.signAuthenticatedIdentifier(ip.getHostIdentifier().getAuthenticationKeyPair(), encodedIdentifier, localGeneratedSalt, distantGeneratedSalt);
				hostSignatures[index]=new byte[saltSizeBytes +s.length];
				System.arraycopy(localGeneratedSalt, 0, hostSignatures[index], 0, saltSizeBytes);
				System.arraycopy(s, 0, hostSignatures[index], saltSizeBytes, s.length);
			}
			else
				hostSignatures[index]=null;
			++index;
		}
		this.nbAnomalies = nbAnomalies;
	}

	private Identifier getValidDistantIdentifier(Collection<CloudIdentifier> acceptedCloudIdentifiers, Identifier identifier, byte[] signature, LoginData loginData, byte[] localGeneratedSalt)
			throws InvalidKeyException, NoSuchAlgorithmException, IOException, InvalidParameterSpecException, SignatureException, NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeySpecException {
		CloudIdentifier foundCloudIdentifier=null;

		for (Iterator<CloudIdentifier> it=acceptedCloudIdentifiers.iterator();it.hasNext();)
		{
			CloudIdentifier cid =it.next();
			if (cid.equals(identifier.getCloudIdentifier())) {
				foundCloudIdentifier = cid;
				it.remove();
				break;
			}
		}

		if (foundCloudIdentifier!=null && loginData.isDistantHostIdentifierValid(identifier))
		{
			if (identifier.getHostIdentifier().isAuthenticatedByPublicKey()) {
				byte[] encodedIdentifier = identifier.getHostIdentifier().getBytesTabToEncode();

				if (Identifier.checkAuthenticatedSignature(identifier.getHostIdentifier().getAuthenticationPublicKey(), signature, saltSizeBytes, signature.length - saltSizeBytes, encodedIdentifier, Arrays.copyOfRange(signature, 0, saltSizeBytes), localGeneratedSalt))
					return new Identifier(foundCloudIdentifier, identifier.getHostIdentifier());
				else
					return null;
			}
			else
				return new Identifier(foundCloudIdentifier, identifier.getHostIdentifier());
		}
		else
			return null;

	}

	public LoginConfirmationMessage getLoginConfirmationMessage(Collection<CloudIdentifier> acceptedCloudIdentifiers, LoginData loginData, byte[] localGeneratedSalt)
			throws InvalidAlgorithmParameterException, InvalidKeySpecException, NoSuchAlgorithmException, IOException, SignatureException, NoSuchProviderException, InvalidKeyException, InvalidParameterSpecException {
		ArrayList<Identifier> validDistantIds=new ArrayList<>(identifiers.length);
		int nbAno=nbAnomalies;
		for (int i=0;i<identifiers.length;i++)
		{
			Identifier id = identifiers[i];
			Identifier resid=getValidDistantIdentifier(acceptedCloudIdentifiers, id, hostSignatures[i], loginData, localGeneratedSalt);
			if (resid!=null)
				validDistantIds.add(resid);
			else
				++nbAno;
		}
		return new LoginConfirmationMessage(validDistantIds, nbAno>Short.MAX_VALUE?Short.MAX_VALUE:(short)nbAno, true);
	}
	
	@Override
	public short getNbAnomalies() {
		return nbAnomalies;
	}

	@Override
	public boolean checkDifferedMessages() {
		return false;
	}

}
