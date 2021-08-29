package com.distrimind.madkit.kernel.network;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.distrimind.madkit.kernel.network.connection.secured.ServerSecuredProtocolPropertiesWithKnownPublicKey;
import com.distrimind.util.crypto.*;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class ClientServerProfileValidationTest {
    @Test
    public void testProfileValidation() throws NoSuchProviderException, NoSuchAlgorithmException, IOException {
        ServerSecuredProtocolPropertiesWithKnownPublicKey server=new ServerSecuredProtocolPropertiesWithKnownPublicKey();
        int id=server.generateAndAddEncryptionProfile(SecureRandomType.DEFAULT.getSingleton(null), ASymmetricEncryptionType.DEFAULT, SymmetricEncryptionType.DEFAULT, ASymmetricKeyWrapperType.DEFAULT, MessageDigestType.DEFAULT);
        AssertJUnit.assertTrue(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
        server.invalidateProfile(id);
        AssertJUnit.assertFalse(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
        long t=System.currentTimeMillis();
        id=server.generateAndAddEncryptionProfile(SecureRandomType.DEFAULT.getSingleton(null), ASymmetricEncryptionType.DEFAULT, t-2, t-1, (short)2048, SymmetricEncryptionType.DEFAULT, (short)128, ASymmetricKeyWrapperType.DEFAULT, SymmetricAuthenticatedSignatureType.DEFAULT, MessageDigestType.DEFAULT);
        AssertJUnit.assertFalse(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
        server.invalidateProfile(id);
        AssertJUnit.assertFalse(server.isValidProfile(id, EncryptionRestriction.NO_RESTRICTION));
    }

}
