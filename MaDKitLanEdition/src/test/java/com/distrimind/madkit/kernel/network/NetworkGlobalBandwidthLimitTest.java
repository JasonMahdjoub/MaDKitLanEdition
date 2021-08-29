package com.distrimind.madkit.kernel.network;

import org.testng.annotations.Test;
import com.distrimind.madkit.testing.util.agent.BigDataTransferSpeed;
import java.net.UnknownHostException;

public class NetworkGlobalBandwidthLimitTest {

    @Test
    public void testUploadLimit() throws UnknownHostException {
        BigDataTransferSpeed test=new BigDataTransferSpeed(Integer.MAX_VALUE, 40000000);
        test.bigDataTransfer();

    }
    @Test
    public void testDownloadLimit() throws UnknownHostException {
        BigDataTransferSpeed test=new BigDataTransferSpeed(40000000, Integer.MAX_VALUE);
        test.bigDataTransfer();
    }
}
