package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.kernel.network.connection.secured.ServerSecuredProtocolPropertiesWithKnownPublicKey;
import com.distrimind.util.crypto.*;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class ClientServerProfileValidationTest {
    @Test
    public void testProfileValidation() throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        ServerSecuredProtocolPropertiesWithKnownPublicKey server=new ServerSecuredProtocolPropertiesWithKnownPublicKey();
        int id=server.generateAndAddEncryptionProfile(SecureRandomType.DEFAULT.getSingleton(null), ASymmetricEncryptionType.DEFAULT, SymmetricEncryptionType.DEFAULT, ASymmetricKeyWrapperType.DEFAULT, MessageDigestType.DEFAULT);
        Assert.assertTrue(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
        server.invalidateProfile(id);
        Assert.assertFalse(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
        id=server.generateAndAddEncryptionProfile(SecureRandomType.DEFAULT.getSingleton(null), ASymmetricEncryptionType.DEFAULT, System.currentTimeMillis()-1, (short)2048, SymmetricEncryptionType.DEFAULT, (short)128, ASymmetricKeyWrapperType.DEFAULT, SymmetricAuthenticatedSignatureType.DEFAULT, MessageDigestType.DEFAULT);

        Assert.assertFalse(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
        server.invalidateProfile(id);
        Assert.assertFalse(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
    }

}
