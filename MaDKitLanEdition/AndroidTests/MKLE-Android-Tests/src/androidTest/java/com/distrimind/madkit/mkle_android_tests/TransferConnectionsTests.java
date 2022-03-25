package com.distrimind.madkit.mkle_android_tests;

import com.distrimind.madkit.kernel.network.NetworkEventListener;
import com.distrimind.madkit.kernel.network.TransferConnectionTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class TransferConnectionsTests {
    @Parameterized.Parameters
    public static Object[][] data(){
        return TransferConnectionTest.data();
    }
    private TransferConnectionTest t;
    public TransferConnectionsTests(final NetworkEventListener eventListener1, final NetworkEventListener eventListener2,
                                    final NetworkEventListener eventListener3, final NetworkEventListener eventListener4,
                                    final NetworkEventListener eventListener5)
    {
        t=new TransferConnectionTest(eventListener1, eventListener2, eventListener3, eventListener4, eventListener5);
    }
    @Test
    public void tryDirectConnectionTestWithKernelAddressReference() {
        t.tryDirectConnectionTestWithKernelAddressReference();
    }

    @Test
    public void tryDirectConnectionTestWithInetAddressReference() {
        t.tryDirectConnectionTestWithInetAddressReference();
    }

    @Test
    public void transferConnectionTestWithKernelAddressReference() {
        t.transferConnectionTestWithKernelAddressReference();
    }

    @Test
    public void transferConnectionTestWithInetAddressReference() {
        t.transferConnectionTestWithInetAddressReference();
    }
}
