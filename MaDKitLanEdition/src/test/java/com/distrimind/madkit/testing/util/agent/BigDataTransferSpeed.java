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
package com.distrimind.madkit.testing.util.agent;

import com.distrimind.madkit.io.RandomByteArrayInputStream;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.access.AbstractAccessProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.access.AccessProtocolWithP2PAgreementProperties;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolWithKeyAgreementProperties;
import com.distrimind.util.crypto.SymmetricAuthentifiedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import org.junit.Assert;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;


/**
 * @author Jason Mahdjoub
 * @since MadkitLanEdition 1.5
 * @version 1.0
 * 
 */
public class BigDataTransferSpeed extends JunitMadkit {
	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
    final int downloadLimitInBytesPerSecond, uploadLimitInBytesPerSecond;

	public BigDataTransferSpeed(final int downloadLimitInBytesPerSecond, final int uploadLimitInBytesPerSecond) throws UnknownHostException {
        this.downloadLimitInBytesPerSecond=downloadLimitInBytesPerSecond;
        this.uploadLimitInBytesPerSecond=uploadLimitInBytesPerSecond;
        P2PSecuredConnectionProtocolWithKeyAgreementProperties p2pprotocol=new P2PSecuredConnectionProtocolWithKeyAgreementProperties();
        p2pprotocol.isServer = true;
        p2pprotocol.symmetricEncryptionType=SymmetricEncryptionType.AES_CTR;
        p2pprotocol.symmetricSignatureType= SymmetricAuthentifiedSignatureType.HMAC_SHA2_256;

        AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();
        this.eventListener1 = new NetworkEventListener(true, false, false, null,
                new ConnectionsProtocolsMKEventListener(p2pprotocol), new AccessProtocolPropertiesMKEventListener(app),
                new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(GROUP)), 5000,
                Collections.singletonList((AbstractIP) new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
                        (Inet6Address) InetAddress.getByName("::1"))),
                InetAddress.getByName("0.0.0.0")) {

            @Override
            public void onMadkitPropertiesLoaded(MadkitProperties _properties) {
                super.onMadkitPropertiesLoaded(_properties);
                _properties.networkProperties.networkLogLevel = Level.INFO;
                _properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;
                _properties.networkProperties.maximumGlobalDownloadSpeedInBytesPerSecond=downloadLimitInBytesPerSecond;
                _properties.networkProperties.maximumGlobalUploadSpeedInBytesPerSecond=uploadLimitInBytesPerSecond;

            }
        };
		/*this.eventListener1 = new MadkitEventListener() {

			@Override
			public void onMadkitPropertiesLoaded(MadkitProperties _properties) {
				AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();

				try {
					P2PSecuredConnectionProtocolWithKeyAgreementProperties p2pprotocol=new P2PSecuredConnectionProtocolWithKeyAgreementProperties();
					p2pprotocol.isServer = true;
					p2pprotocol.symmetricEncryptionType=SymmetricEncryptionType.AES_CTR;
                    p2pprotocol.symmetricSignatureType= SymmetricAuthentifiedSignatureType.HMAC_SHA2_256;
					new NetworkEventListener(true, false, false, null,
							new ConnectionsProtocolsMKEventListener(p2pprotocol),
							new AccessProtocolPropertiesMKEventListener(app),
							new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(GROUP)), 5000,
							null, InetAddress.getByName("0.0.0.0")).onMadkitPropertiesLoaded(_properties);
				} catch (Exception e) {
					e.printStackTrace();
				}
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;
                _properties.networkProperties.maximumGlobalDownloadSpeedInBytesPerSecond=downloadLimitInBytesPerSecond;
				_properties.networkProperties.maximumGlobalUploadSpeedInBytesPerSecond=uploadLimitInBytesPerSecond;
			}
		};*/

		P2PSecuredConnectionProtocolWithKeyAgreementProperties u = new P2PSecuredConnectionProtocolWithKeyAgreementProperties();
		u.isServer = false;
        u.symmetricEncryptionType=SymmetricEncryptionType.AES_CTR;
		u.symmetricSignatureType= SymmetricAuthentifiedSignatureType.HMAC_SHA2_256;

		app = new AccessProtocolWithP2PAgreementProperties();
		
		this.eventListener2 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(u), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(GROUP)), 5000,
				Collections.singletonList((AbstractIP) new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1"))),
				InetAddress.getByName("0.0.0.0")) {

			@Override
			public void onMadkitPropertiesLoaded(MadkitProperties _properties) {
			    super.onMadkitPropertiesLoaded(_properties);
                _properties.networkProperties.networkLogLevel = Level.INFO;
                _properties.networkProperties.maxBufferSize=Short.MAX_VALUE*4;

                _properties.networkProperties.maximumGlobalDownloadSpeedInBytesPerSecond=downloadLimitInBytesPerSecond;
				_properties.networkProperties.maximumGlobalUploadSpeedInBytesPerSecond=uploadLimitInBytesPerSecond;

			}
		};
	}
    private static final long timeOut = 20000;
	public void bigDataTransfer() {

		final AtomicReference<Boolean> transfered1=new AtomicReference<>(null);
		final AtomicReference<Boolean> transfered2=new AtomicReference<>(null);
		// addMadkitArgs("--kernelLogLevel",Level.INFO.toString(),"--networkLogLevel",Level.FINEST.toString());
		launchTest(new AbstractAgent() {
            @Override
            protected void end() {
                while(nextMessage()!=null);
            }

            @Override
            protected void activate() throws InterruptedException {
                AbstractAgent bigDataSenderAgent=new NormalAgent() {
                            @Override
                            protected void activate() throws InterruptedException {
                                requestRole(GROUP, ROLE);
                                sleep(2000);
                                int size=400000000;
                                int delay;
                                if (downloadLimitInBytesPerSecond!=Integer.MAX_VALUE || uploadLimitInBytesPerSecond!=Integer.MAX_VALUE)
                                    delay=Math.max(60000, size/Math.min(downloadLimitInBytesPerSecond, uploadLimitInBytesPerSecond)*1000+20000);
                                else
                                    delay=60000;
                                AgentAddress aa=getAgentsWithRole(GROUP, ROLE).iterator().next();
                                assert aa!=null;

                                try {

                                    Assert.assertNotNull(this.sendBigData(aa, new RandomByteArrayInputStream(new byte[size]), 0, size, null, null, true));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                Message m=this.waitNextMessage(delay);

                                boolean tr1=m instanceof BigDataResultMessage && ((BigDataResultMessage) m).getType() == BigDataResultMessage.Type.BIG_DATA_TRANSFERED;
                                if (tr1) {
                                    BigDataResultMessage br=(BigDataResultMessage)m;
                                    if (this.getMaximumGlobalUploadSpeedInBytesPerSecond() != Integer.MAX_VALUE){
                                        double speed=((double) br.getTransferedDataLength()) / ((double) br.getTransferDuration()) * 1000.0;
                                        Assert.assertTrue(speed< getMaximumGlobalUploadSpeedInBytesPerSecond() * 2);
                                        Assert.assertTrue(speed> getMaximumGlobalUploadSpeedInBytesPerSecond() / 2);
                                    }
                                }
                                transfered1.set(tr1);

                                Assert.assertTrue(""+m, transfered1.get());

                                try {
                                    Assert.assertNotNull(this.sendBigData(aa, new RandomByteArrayInputStream(new byte[size]), 0, size, null, null, false));
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                m=this.waitNextMessage(delay);
                                boolean tr2=m instanceof BigDataResultMessage && ((BigDataResultMessage) m).getType() == BigDataResultMessage.Type.BIG_DATA_TRANSFERED;
                                if (tr2) {
                                    BigDataResultMessage br=(BigDataResultMessage)m;
                                    if (this.getMaximumGlobalUploadSpeedInBytesPerSecond() != Integer.MAX_VALUE) {
                                        double speed=((double) br.getTransferedDataLength()) / ((double) br.getTransferDuration()) * 1000.0;
                                        Assert.assertTrue(speed< getMaximumGlobalUploadSpeedInBytesPerSecond() * 2);
                                        Assert.assertTrue(speed> getMaximumGlobalUploadSpeedInBytesPerSecond() / 2);
                                    }
                                }
                                transfered2.set(tr2);

                                Assert.assertTrue(""+m, transfered2.get());


                            }

                            @Override
                            protected void liveCycle() {
                                this.killAgent(this);
                            }

                            @Override
                            protected void end() {
                                while(nextMessage()!=null);
                            }
                        };
                launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, bigDataSenderAgent, eventListener1);
                sleep(400);
                BigDataTransferReceiverAgent bigDataTransferAgent = new BigDataTransferReceiverAgent(2, uploadLimitInBytesPerSecond);
                launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, bigDataTransferAgent, eventListener2);

                int counter=0;
                while(transfered1.get()==null || transfered2.get()==null)
                {

                    Thread.sleep(1000);
                }
                Assert.assertTrue(transfered1.get());
                Assert.assertTrue(transfered2.get());

                //noinspection UnusedAssignment
                bigDataTransferAgent=null;
                //noinspection UnusedAssignment
                bigDataSenderAgent=null;
                for (Madkit mk : getHelperInstances(2))
                    stopNetwork(mk);

                for (Madkit mk : getHelperInstances(2)) {
                    checkConnectedKernelsNb(this, mk, 0, timeOut);
                    checkConnectedIntancesNb(this, mk, 0, timeOut);
                }
                sleep(400);

                cleanHelperMDKs(this);
                Assert.assertEquals(getHelperInstances(0).size(), 0);


            }
        });
        Assert.assertTrue(transfered1.get());
        Assert.assertTrue(transfered2.get());

		cleanHelperMDKs();
	}
	
	
	

}
