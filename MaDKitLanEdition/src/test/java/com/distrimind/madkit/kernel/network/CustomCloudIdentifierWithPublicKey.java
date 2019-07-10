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

import com.distrimind.madkit.exceptions.MessageExternalizationException;
import com.distrimind.madkit.kernel.network.connection.access.CloudIdentifier;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;
import com.distrimind.madkit.util.SerializationTools;
import com.distrimind.util.crypto.ASymmetricKeyPair;
import com.distrimind.util.crypto.ASymmetricPublicKey;

import java.io.IOException;

/**
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MaDKitLanEdition 1.9
 */
@SuppressWarnings({"unused"})
public class CustomCloudIdentifierWithPublicKey extends CloudIdentifier {
    private ASymmetricKeyPair keyPair;
    private ASymmetricPublicKey publicKey;
    private byte[] salt;

    private CustomCloudIdentifierWithPublicKey()
    {

    }

    public CustomCloudIdentifierWithPublicKey(ASymmetricKeyPair keyPair, byte[] salt) {
        assert keyPair!=null;
        this.keyPair = keyPair;
        this.publicKey=keyPair.getASymmetricPublicKey();
        this.salt=salt;
    }

    @Override
    public boolean equals(Object _object) {
        if (_object instanceof CustomCloudIdentifierWithPublicKey)
            return publicKey.equals(((CustomCloudIdentifierWithPublicKey) _object).publicKey);
        else
            return false;
    }

    @Override
    public int hashCode() {
        return publicKey.hashCode();
    }

    @Override
    public byte[] getIdentifierBytes() {
        return publicKey.encode();
    }

    @Override
    public byte[] getSaltBytes() {
        return salt;
    }

    @Override
    public boolean isAutoIdentifiedCloudWithPublicKey() {
        return true;
    }

    @Override
    public String toString() {
        return "CloudID["+publicKey.toString()+"]";
    }

    @Override
    public ASymmetricPublicKey getCloudPublicKey() {
        return publicKey;
    }

    @Override
    public ASymmetricKeyPair getCloudKeyPair() {
        return keyPair;
    }

    @Override
    public int getInternalSerializedSize() {
        return SerializationTools.getInternalSize(publicKey, Short.MAX_VALUE);
    }

    @Override
    public void writeExternal(SecuredObjectOutputStream out) throws IOException {
        out.writeBytesArray(salt, false, 64);
        out.writeObject(publicKey, false);

    }

    @Override
    public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
        salt=in.readBytesArray(false, 64);
        publicKey=in.readObject(false, ASymmetricPublicKey.class);
        if (publicKey.getAuthentifiedSignatureAlgorithmType()==null)
            throw new MessageExternalizationException(WithoutInnerSizeControl.Integrity.FAIL_AND_CANDIDATE_TO_BAN);
        keyPair=null;
    }
}
