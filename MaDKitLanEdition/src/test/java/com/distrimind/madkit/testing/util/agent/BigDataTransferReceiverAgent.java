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

import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.agr.Organization;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.AskForConnectionMessage;
import com.distrimind.madkit.kernel.network.ConnectionStatusMessage;
import com.distrimind.madkit.message.BooleanMessage;
import com.distrimind.madkit.message.KernelMessage;
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
	final boolean cancelTransfer, cancelTransferFromReceiver, asynchronousMessage, restartMessage, restartWithLeaveRequestRole, globalDisconnection;
	private final boolean transferPaused, pauseAll;
	public BigDataTransferReceiverAgent(int dataToReceiveNumber, long uploadLimitInBytesPerSecond,
										boolean cancelTransfer, boolean cancelTransferFromReceiver, boolean asynchronousMessage,
										boolean restartMessage, boolean restartWithLeaveRequestRole, boolean globalDisconnection,
										boolean transferPaused, boolean pauseAll) {
		this.cancelTransfer = cancelTransfer;
		this.cancelTransferFromReceiver=cancelTransferFromReceiver;
		this.asynchronousMessage=asynchronousMessage;
		this.restartMessage=restartMessage;
		this.restartWithLeaveRequestRole=restartWithLeaveRequestRole;
		this.dataToReceiveNumber=dataToReceiveNumber;
		this.uploadLimitInBytesPerSecond=uploadLimitInBytesPerSecond;
		this.globalDisconnection=globalDisconnection;
		this.transferPaused=transferPaused;
		this.pauseAll=pauseAll;
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
		public void deleteReceivedData() {
			dataDeleted=true;
			System.out.println("Data deleted");
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
	static boolean dataDeleted=false;
	@Override
	protected void liveCycle() throws InterruptedException {

		Message m = waitNextMessage(10000);
		if (m instanceof BigDataPropositionMessage)
		{
			System.out.println("receiving big data proposition message");
			BigDataPropositionMessage bdpm=((BigDataPropositionMessage) m);
			byte[] arrayToSend=BigDataTransferSpeed.bigDataToSendArray.get();
			dataDeleted=false;
			receivedData=new RandomByteArrayOutputStream();
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
			else if (restartMessage && asynchronousMessage && !cancelTransfer)
			{
				sleep(100);
				if (globalDisconnection) {

					System.out.println("Stop network");
					this.sendMessage(LocalCommunity.Groups.SYSTEM, Organization.GROUP_MANAGER_ROLE, new KernelMessage(KernelAction.STOP_NETWORK));
					sleep(1000);
					int nb=0;
					while (this.getEffectiveConnections().size()>0 && nb++<10)
						sleep(1000);
					Assert.assertEquals(this.getEffectiveConnections().size(), 0);
					System.out.println("Launch network");
					this.sendMessage(LocalCommunity.Groups.SYSTEM, Organization.GROUP_MANAGER_ROLE, new KernelMessage(KernelAction.LAUNCH_NETWORK));
				}
				else if (restartWithLeaveRequestRole && cancelTransferFromReceiver)
				{
					System.out.println("Leave role from receiver");
					this.leaveRole(GROUP, ROLE);
					sleep(1000);

					receivedData=new RandomByteArrayOutputStream(receivedData.getBytes());
					System.out.println("Reconnect from receiver");
					this.requestRole(GROUP, ROLE);
				}
				else if (restartWithLeaveRequestRole)
				{
					sleep(500);
					receivedData=new RandomByteArrayOutputStream(receivedData.getBytes());
				}
				else
				{

					try {
						System.out.println("Disconnect");
						this.manageDirectConnection(new AskForConnectionMessage(ConnectionStatusMessage.Type.DISCONNECT, BigDataTransferSpeed.ipToConnect));

						sleep(1000);
						int nb=0;
						while (this.getEffectiveConnections().size()>0 && nb++<10)
							sleep(1000);

						Assert.assertEquals(this.getEffectiveConnections().size(), 0);
						System.out.println("Reconnect");
						this.manageDirectConnection(new AskForConnectionMessage(ConnectionStatusMessage.Type.CONNECT, BigDataTransferSpeed.ipToConnect));
					} catch (IllegalAccessException e) {
						e.printStackTrace();
					}

				}

			}
			else if (transferPaused && cancelTransferFromReceiver)
			{
				sleep(100);
				if (pauseAll) {
					System.out.println("Pause transfers from receiver (all distant peers concerned)");
					setAllBigDataTransfersPaused(true);
				}
				else {
					System.out.println("Pause one transfer from receiver (one distant peer concerned)");
					setBigDataTransfersOfAGivenKernelPaused(getAvailableDistantKernels().stream().findAny().orElse(null), true);
				}


				sleep(2000);
				Assert.assertEquals(bdpm.getStatistics().getNumberOfIdentifiedBytesDuringTheLastCycle(), 0);
				System.out.println("Restart transfers from receiver");
				if (pauseAll)
					setAllBigDataTransfersPaused(false);
				else
					setBigDataTransfersOfAGivenKernelPaused(getAvailableDistantKernels().stream().findAny().orElse(null), false);

			}
			long delay;
			int size=(int)bdpm.getTransferLength();
			if (downloadLimitInBytesPerSecond!=Integer.MAX_VALUE || uploadLimitInBytesPerSecond!=Integer.MAX_VALUE)
				delay=Math.max(60000, size/Math.min(downloadLimitInBytesPerSecond, uploadLimitInBytesPerSecond)*1000+20000);
			else
				delay=60000;

			m = waitNextMessage(delay);
			sleep(300);
			if (m instanceof BigDataResultMessage)
			{
				BigDataResultMessage rm=((BigDataResultMessage) m);
				if (rm.getType()==BigDataResultMessage.Type.BIG_DATA_TRANSFERRED)
				{
					AssertJUnit.assertFalse(cancelTransfer);
					System.out.println(rm.getTransferredDataLength() +" bytes transfered in "+rm.getTransferDuration()+" ms"+(bdpm.bigDataExcludedFromEncryption()?" without encryption":" with encryption"));
					System.out.println("Transfer speed (MiO per seconds) : "+(((double)rm.getTransferredDataLength())/(((double)rm.getTransferDuration())/1000.0)/1024.0/1024.0));

					if (!transferPaused && !restartMessage && getMaximumGlobalDownloadSpeedInBytesPerSecond()!=Long.MAX_VALUE) {

                        double speed=((double) rm.getTransferredDataLength()) / ((double) rm.getTransferDuration()) * 1000.0;
						AssertJUnit.assertTrue(speed< getMaximumGlobalDownloadSpeedInBytesPerSecond() * 2);
						AssertJUnit.assertTrue(speed> getMaximumGlobalDownloadSpeedInBytesPerSecond() / 2.0);
                    }

					AssertJUnit.assertEquals(arrayToSend, receivedData.getBytes());
					AssertJUnit.assertEquals(arrayToSend.length, rm.getTransferredDataLength());
					if (!restartMessage) {
						AssertJUnit.assertTrue("" + bdpm.getStatistics().getNumberOfIdentifiedBytesFromCreationOfTheseStatistics(), arrayToSend.length <= bdpm.getStatistics().getNumberOfIdentifiedBytesFromCreationOfTheseStatistics());
					}
				}
				else if (rm.getType()== BigDataResultMessage.Type.TRANSFER_CANCELED)
				{
					System.out.println("message canceled");
					AssertJUnit.assertTrue(cancelTransfer);
					if (asynchronousMessage)
						AssertJUnit.assertTrue(dataDeleted);
					else
						AssertJUnit.assertFalse(dataDeleted);
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
				System.err.println("Unexpected message, expected big data result message :"+m);
				this.sleep(1000);
				this.killAgent(this);
			}
		}
		else
		{
			System.err.println("Unexpected message, expected big data proposition message :"+m);
			this.sleep(1000);
			this.killAgent(this);
		}
	}

    @Override
    protected void end() {
        while(nextMessage()!=null);
    }
}
