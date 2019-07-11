package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.kernel.network.connection.secured.ServerSecuredProtocolPropertiesWithKnownPublicKey;
import com.distrimind.util.crypto.*;
import org.junit.Assert;
import org.junit.Test;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

public class ClientServerProfileValidationTest {
    @Test
    public void testProfileValidation() throws NoSuchProviderException, NoSuchAlgorithmException, InvalidAlgorithmParameterException {
        ServerSecuredProtocolPropertiesWithKnownPublicKey server=new ServerSecuredProtocolPropertiesWithKnownPublicKey();
        int id=server.generateAndAddEncryptionProfile(SecureRandomType.DEFAULT.getSingleton(null), ASymmetricEncryptionType.DEFAULT, SymmetricEncryptionType.DEFAULT, ASymmetricKeyWrapperType.DEFAULT);
        Assert.assertTrue(server.isValidProfile(id));
        server.invalidateProfile(id);
        Assert.assertFalse(server.isValidProfile(id));
        id=server.generateAndAddEncryptionProfile(SecureRandomType.DEFAULT.getSingleton(null), ASymmetricEncryptionType.DEFAULT, System.currentTimeMillis()-1, (short)2048, SymmetricEncryptionType.DEFAULT, (short)128, ASymmetricKeyWrapperType.DEFAULT, SymmetricAuthentifiedSignatureType.DEFAULT);

        Assert.assertFalse(server.isValidProfile(id));
        server.invalidateProfile(id);
        Assert.assertFalse(server.isValidProfile(id));
    }

}
