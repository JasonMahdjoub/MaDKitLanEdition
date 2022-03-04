package com.distrimind.madkit.mkle_android_tests;


import android.util.Log;

import com.distrimind.madkit.kernel.network.NetworkEventListener;

import org.junit.Test;

import java.util.Objects;

public class MultipleConnectionsTest {

    @Test
    public void testAll()
    {
        int i=1;
        for (Object[] params : Objects.requireNonNull(com.distrimind.madkit.kernel.network.MultipleConnectionsTest.data())) {
            com.distrimind.madkit.kernel.network.MultipleConnectionsTest t=new com.distrimind.madkit.kernel.network.MultipleConnectionsTest(
                    (Integer)params[0],
                    (Integer) params[1],
                    (NetworkEventListener) params[2],
                    (NetworkEventListener) params[3],
                    (NetworkEventListener) params[4],
                    (NetworkEventListener) params[5],
                    (NetworkEventListener) params[6]);
            Log.i("mktests", "Launch test N°"+i);
            t.multipleAsynchronousConnectionTest();
            Log.i("mktests", "Test N°" + i + " OK");
        }
    }
}
