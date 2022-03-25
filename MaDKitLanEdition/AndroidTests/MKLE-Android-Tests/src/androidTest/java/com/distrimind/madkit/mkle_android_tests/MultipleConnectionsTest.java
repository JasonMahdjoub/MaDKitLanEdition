package com.distrimind.madkit.mkle_android_tests;


import androidx.test.platform.app.InstrumentationRegistry;

import com.distrimind.madkit.kernel.network.NetworkEventListener;
import com.distrimind.util.harddrive.AndroidHardDriveDetect;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class MultipleConnectionsTest {
    static {
        AndroidHardDriveDetect.context = InstrumentationRegistry.getInstrumentation().getTargetContext();
    }
    @Parameterized.Parameters
    public static Object[][] data(){
        return com.distrimind.madkit.kernel.network.MultipleConnectionsTest.data();
    }
    private com.distrimind.madkit.kernel.network.MultipleConnectionsTest t;
    public MultipleConnectionsTest(Integer localDataAmountAcc, Integer globalDataAmountAcc,
                                   final NetworkEventListener eventListener1, final NetworkEventListener eventListener2,
                                   final NetworkEventListener eventListener3, final NetworkEventListener eventListener4,
                                   final NetworkEventListener eventListener5)
    {
        t=new com.distrimind.madkit.kernel.network.MultipleConnectionsTest(localDataAmountAcc,globalDataAmountAcc,
                eventListener1,
                eventListener2,
                eventListener3,
                eventListener4,
                eventListener5);
    }
    @Test
    public void multipleAsynchronousConnectionTest()
    {
        t.multipleAsynchronousConnectionTest();
        t=null;
    }
}
