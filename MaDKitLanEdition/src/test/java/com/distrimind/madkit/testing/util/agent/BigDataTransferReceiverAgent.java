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

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.message.BooleanMessage;
import com.distrimind.util.io.RandomByteArrayOutputStream;
import com.distrimind.util.io.RandomOutputStream;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;
import org.testng.Assert;
import org.testng.AssertJUnit;

import java.io.IOException;

import static com.distrimind.madkit.kernel.TestNGMadkit.*;

/**
 * @author Jason Mahdjoub
 * @since MadkitLanEdition 1.5
 * @version 1.0
 * 
 */
public class BigDataTransferReceiverAgent extends Agent {
	private int dataToReceiveNumber;
	private final long uploadLimitInBytesPerSecond;
	private long downloadLimitInBytesPerSecond;
	final boolean cancelTransfer, cancelTransferFromReceiver, asynchronousMessage, restartMessage;

	public BigDataTransferReceiverAgent(int dataToReceiveNumber, long uploadLimitInBytesPerSecond,
										boolean cancelTransfer, boolean cancelTransferFromReceiver, boolean asynchronousMessage, boolean restartMessage) {
		this.cancelTransfer = cancelTransfer;
		this.cancelTransferFromReceiver=cancelTransferFromReceiver;
		this.asynchronousMessage=asynchronousMessage;
		this.restartMessage=restartMessage;
		this.dataToReceiveNumber=dataToReceiveNumber;
		this.uploadLimitInBytesPerSecond=uploadLimitInBytesPerSecond;
	}
	@Override
	protected void activate() {

		requestRole(GROUP, ROLE);
		downloadLimitInBytesPerSecond=getMaximumGlobalDownloadSpeedInBytesPerSecond();
	}

	static final class AsynchronousToReceive implements AsynchronousBigDataToReceiveWrapper
	{

		@Override
		public RandomOutputStream getRandomOutputStream(ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier) {
			return receivedData;
		}

		@Override
		public int getInternalSerializedSize() {
			return 0;
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {

		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {

		}
	}
	static RandomByteArrayOutputStream receivedData=new RandomByteArrayOutputStream();

	@Override
	protected void liveCycle() throws InterruptedException {
		Message m = waitNextMessage(10000);
		if (m instanceof BigDataPropositionMessage)
		{
			System.out.println("receiving big data proposition message");
			BigDataPropositionMessage bdpm=((BigDataPropositionMessage) m);
			byte[] arrayToSend=BigDataTransferSpeed.bigDataToSendArray.get();
			try {
				if (asynchronousMessage)
					bdpm.acceptTransfer(new AsynchronousToReceive());
				else
					bdpm.acceptTransfer(receivedData);

			} catch (IllegalAccessException e) {
				e.printStackTrace();
				return;
			}
			if (cancelTransfer && cancelTransferFromReceiver)
			{
				sleep(100);
				cancelBigDataTransfer(bdpm.getBigDataTransferID());
			}
			long delay;
			int size=(int)bdpm.getTransferLength();
			if (downloadLimitInBytesPerSecond!=Integer.MAX_VALUE || uploadLimitInBytesPerSecond!=Integer.MAX_VALUE)
				delay=Math.max(60000, size/Math.min(downloadLimitInBytesPerSecond, uploadLimitInBytesPerSecond)*1000+20000);
			else
				delay=60000;

			m = waitNextMessage(delay);
			if (m instanceof BigDataResultMessage)
			{
				BigDataResultMessage rm=((BigDataResultMessage) m);
				if (rm.getType()==BigDataResultMessage.Type.BIG_DATA_TRANSFERRED)
				{
					AssertJUnit.assertFalse(cancelTransfer);
					System.out.println(rm.getTransferredDataLength() +" bytes transfered in "+rm.getTransferDuration()+" ms"+(bdpm.bigDataExcludedFromEncryption()?" without encryption":" with encryption"));
					System.out.println("Transfer speed (MiO per seconds) : "+(((double)rm.getTransferredDataLength())/(((double)rm.getTransferDuration())/1000.0)/1024.0/1024.0));
					if (getMaximumGlobalDownloadSpeedInBytesPerSecond()!=Long.MAX_VALUE) {

                        double speed=((double) rm.getTransferredDataLength()) / ((double) rm.getTransferDuration()) * 1000.0;
						AssertJUnit.assertTrue(speed< getMaximumGlobalDownloadSpeedInBytesPerSecond() * 2);
						AssertJUnit.assertTrue(speed> getMaximumGlobalDownloadSpeedInBytesPerSecond() / 2.0);
                    }
					AssertJUnit.assertEquals(arrayToSend, receivedData.getBytes());
				}
				else if (rm.getType()== BigDataResultMessage.Type.TRANSFER_CANCELED)
				{
					System.out.println("message canceled");
					AssertJUnit.assertTrue(cancelTransfer);
					AgentAddress aa=getAgentWithRole(GROUP, ROLE2);
					Assert.assertNotNull(aa);
					sendMessage(aa, new BooleanMessage(true));
					m=waitNextMessage();
					Assert.assertNotNull(m);
					Assert.assertTrue(m instanceof BooleanMessage, m.toString());
					Assert.assertEquals(((BooleanMessage) m).getContent(), Boolean.TRUE);
				}
				else {
					System.err.println("Problem during transfer : " + rm.getType());
				}

				receivedData=new RandomByteArrayOutputStream();
				if (--dataToReceiveNumber<=0) {
					this.sleep(1000);
					this.killAgent(this);
				}
			}
			else
			{
				System.err.println("Unexpected message :"+m);
				this.sleep(1000);
				this.killAgent(this);
			}
		}
		else
		{
			System.err.println("Unexpected message :"+m);
			this.sleep(1000);
			this.killAgent(this);
		}
	}

    @Override
    protected void end() {
        while(nextMessage()!=null);
    }
}
