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

import com.distrimind.madkit.exceptions.SelfKillException;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.madkit.kernel.network.RealTimeTransferStat;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.madkit.message.hook.OrganizationEvent;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.*;
import org.junit.Assert;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public class AgentBigTransfer extends AgentFakeThread {
	public static final String bigTransferRole = "Big Transfer Role";
	public static final SecureExternalizable attachedData = new SecureExternalizable() {
		
		@Override
		public int getInternalSerializedSize() {
			return 0;
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) {
			
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) {
			
		}
		
		@Override
		public boolean equals(Object o)
		{
			if (o==null)
				return false;
			return o.getClass()==this.getClass();
		}
	};
	private final Map<AgentAddress, Boolean> otherConnected = Collections.synchronizedMap(new HashMap<AgentAddress, Boolean>());
	private final Map<ConversationID, RandomByteArrayInputStream> inputStreams = Collections.synchronizedMap(new HashMap<ConversationID, RandomByteArrayInputStream>());
	private final Map<ConversationID, RealTimeTransferStat> myStats = Collections.synchronizedMap(new HashMap<ConversationID, RealTimeTransferStat>()),
			otherStats = new HashMap<>();
	// private final HashMap<String, ConversationID> otherConversationIDs=new
	// HashMap<>();
	private final boolean accept;
	private final boolean useMessageDigest;
	private volatile boolean ok = true;
	private final AgentBigTransfer nextBigTransfer;
	private volatile boolean myTransferFinished = false;
	private volatile boolean otherTransferFinished = false;
	private final boolean isLocal;
	private final boolean launchOtherLocalAgent;
	private final String thisRole;
	private final int thisPeerNumber;
	private final boolean sendData, sendShortData;
	private final int inputStreamLengh;
	private final int otherNumber;
	private final int otherSendDataNumber;
	private final int otherReplieNumber;
	private int otherSentManaged = 0;
	private int otherRepliedManaged = 0;
	private final boolean otherSendData;
	private boolean hasReceivedProposition = false;
	private AgentBigTransfer otherBigTransferAgent=null;

	public AgentBigTransfer(int thisPeerNumber, boolean accept, boolean useMessageDigest, boolean sendShortData,
			boolean sendData, boolean otherSendData, int otherNumber, boolean isLocal,
			AgentBigTransfer nextBigTransfer) {
		this(thisPeerNumber, accept, useMessageDigest, sendShortData, sendData, otherSendData, otherNumber, isLocal,
				true, nextBigTransfer);
	}

	private AgentBigTransfer(int thisPeerNumber, boolean accept, boolean useMessageDigest, boolean sendShortData,
			boolean sendData, boolean otherSendData, int otherNumber, boolean isLocal, boolean launchOtherLocalAgent,
			AgentBigTransfer nextBigTransfer) {
		this.accept = accept;
		this.useMessageDigest = useMessageDigest;

		this.sendData = sendData;
		if (!sendData)
			myTransferFinished = true;
		this.sendShortData = sendShortData;
		this.nextBigTransfer = nextBigTransfer;
		this.isLocal = isLocal;
		this.launchOtherLocalAgent = launchOtherLocalAgent && isLocal;
		this.thisRole = bigTransferRole + thisPeerNumber;
		this.thisPeerNumber = thisPeerNumber;
		this.inputStreamLengh = sendShortData ? 200 : 50000000;
		this.otherSendData = otherSendData || !sendData;
		this.otherNumber = isLocal ? 1 : otherNumber;
		this.otherSendDataNumber = otherSendData ? this.otherNumber : 0;
		this.otherReplieNumber = sendData ? this.otherNumber : 0;

	}

	public Map<ConversationID, RealTimeTransferStat> getMyStats() {
		return myStats;
	}

	public Map<ConversationID, RealTimeTransferStat> getOtherStats() {
		return otherStats;
	}

	@Override
	public void activate() {
		try {

			Assert.assertEquals(ReturnCode.SUCCESS, requestHookEvents(AgentActionEvent.REQUEST_ROLE));

			requestRole(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, thisRole);

			if (isLocal) {
				if (launchOtherLocalAgent) {
					AgentAddress myAgentAddress = getAgentAddressIn(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA,
							thisRole);
					otherBigTransferAgent = new AgentBigTransfer(thisPeerNumber, accept, useMessageDigest, sendShortData,
							otherSendData, sendData, 1, isLocal, false, null);
					otherBigTransferAgent.otherConnected.put(myAgentAddress, Boolean.TRUE);
					// agb.otherNumber++;
					launchAgent(otherBigTransferAgent);
					ok &= otherBigTransferAgent.getKernelAddress().equals(this.getKernelAddress());
					Assert.assertTrue(ok);

					AgentAddress otherAgentAddress = otherBigTransferAgent
							.getAgentAddressIn(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, thisRole);
					ok &= otherAgentAddress != null;
					Assert.assertNotNull(otherAgentAddress);
					otherConnected.put(otherAgentAddress, Boolean.TRUE);
					/*
					 * if (otherSendData) otherNumber++;
					 */

					if (otherSendData)
						otherBigTransferAgent.sendData(myAgentAddress);
					sendData(otherAgentAddress);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			ok = false;
			Assert.fail();
		}
	}

	private void sendData(AgentAddress destination) {
		if (sendData) {
			// sendMessageWithRole(m.getSourceAgent(), new
			// OrganizationEvent(AgentActionEvent.REQUEST_ROLE,
			// getAgentAddressIn(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA,
			// thisRole)), thisRole);
			try {
				BigDataTransferID myTransferID;

				RandomByteArrayInputStream inputStream = new RandomByteArrayInputStream(new byte[inputStreamLengh]);

				myTransferID = sendBigDataWithRole(destination, inputStream, 0, inputStream.length(), attachedData,
						useMessageDigest ? MessageDigestType.BC_FIPS_SHA3_512 : null, thisRole, false);
				Assert.assertTrue(ok);
				ok &= myTransferID != null;
				if (!ok)
					Assert.assertTrue(""+thisPeerNumber, ok);

				inputStreams.put(myTransferID, inputStream);
				myStats.put(myTransferID, myTransferID.getBytePerSecondsStat());
				System.out.println("Sending data : " + myTransferID);

			} catch (Exception e) {
				e.printStackTrace();
				ok = false;
				Assert.fail();
			}
		}
	}

	@Override
	protected void liveByStep(Message _message) throws InterruptedException {
		if (_message instanceof OrganizationEvent) {
			OrganizationEvent m = (OrganizationEvent) _message;
			if (m.getContent() == AgentActionEvent.REQUEST_ROLE) {
				// if (m.getSourceAgent().getRole().equals(thisRole) &&
				// !m.getSourceAgent().representsSameAgentThan(getAgentAddressIn(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA,
				// thisRole) ))
				if (m.getSourceAgent().getRole().equals(thisRole) && (!m.getSourceAgent().representsSameAgentThan(
						getAgentAddressIn(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, thisRole)) && !isLocal)) {
					if (otherConnected.put(m.getSourceAgent(), Boolean.TRUE) == null) {

						sendMessageWithRole(m.getSourceAgent(),
								new OrganizationEvent(AgentActionEvent.REQUEST_ROLE,
										getAgentAddressIn(JunitMadkit.DEFAULT_NETWORK_GROUP_FOR_ACCESS_DATA, thisRole)),
								thisRole);

						/*
						 * if (otherSendData) ++otherSendDataNumber;
						 */
						sendData(m.getSourceAgent());
					}
					/*
					 * else { ok=false; Assert.fail(); }
					 */

				}
			}
		} else if (_message instanceof BigDataPropositionMessage) {
			if (!_message.getSender().getRole().equals(thisRole))
				Assert.fail();
			BigDataPropositionMessage m = (BigDataPropositionMessage) _message;

			if (otherConnected.get(m.getSender()) != null) {
				hasReceivedProposition = true;

				if (accept) {

					ok &= m.getAttachedData().equals(attachedData);
					Assert.assertEquals(attachedData, m.getAttachedData());
					Assert.assertTrue(ok);
					otherStats.put(m.getConversationID(), m.getStatistics());
					// otherConversationIDs.put(m.getSender().getRole(), m.getConversationID());
					m.acceptTransfer(new RandomByteArrayOutputStream());
				} else {
					m.denyTransfer();
					++otherSentManaged;

					if (otherStats.size() == 0 && otherSentManaged == otherSendDataNumber
							&& otherRepliedManaged == otherReplieNumber)
						otherTransferFinished = true;
					if (myTransferFinished && otherTransferFinished) {
						if (ok && nextBigTransfer != null) {
							launchAgent(nextBigTransfer);
						}
						//this.sleep(300);

						this.killAgent(this);
					}
				}
			} else {
				System.err.println(this);
				if (otherConnected.keySet().size()>0)
					System.err.println(otherConnected.keySet().iterator().next() + " ; " + m.getSender());
				m.denyTransfer();
				ok = false;
				Assert.fail();

			}
		} else if (_message instanceof BigDataResultMessage) {
			if (!_message.getSender().getRole().equals(thisRole)) {
				Assert.fail();
			}

			BigDataResultMessage m = (BigDataResultMessage) _message;
			try {
				RealTimeTransferStat myStat = myStats.get(m.getConversationID());
				RealTimeTransferStat otherStat = otherStats.get(m.getConversationID());

				RandomInputStream inputStream = inputStreams.get(m.getConversationID());
				ConversationID myTransferID = inputStream == null ? null : m.getConversationID();
				ConversationID otherConversationID = otherStat == null ? null : m.getConversationID();
				ok &= myTransferID != otherConversationID;
				Assert.assertNotSame(m.toString()+" ; myTransferID="+myTransferID+" ; otherConversationID="+otherConversationID+" ; isLocal="+isLocal+" ; shortData="+sendShortData, myTransferID, otherConversationID);

				if (accept) {

					ok &= (inputStream == null || m.getTransferredDataLength() == inputStream.length())
							&& m.getType().equals(BigDataResultMessage.Type.BIG_DATA_TRANSFERRED);

					if (!ok)
						System.err.println(thisRole + " Error : Transfered length=" + m.getTransferredDataLength()
								+ ", inputStreamLength=" + (inputStream == null ? -1 : inputStream.length()) + ", type="
								+ m.getType());

					Assert.assertTrue(ok);

					System.out.println("---------------------");
					if (m.getConversationID().equals(myTransferID)) {
						++otherRepliedManaged;
						System.out.println("My transfer has finished : " + m.getType());
					} else if (m.getConversationID().equals(otherConversationID)) {
						++otherSentManaged;
						System.out.println("Other transfer has finished : " + m.getType());
					} else {
						System.err.println("unexpected message : " + m.getConversationID());
						ok = false;
						Assert.fail();
					}

					System.out.println("\tTransfer Type=" + m.getType() + ", Transfer duration="
							+ m.getTransferDuration() + ", transferDataLendth=" + m.getTransferredDataLength()
							+ ", bytesPerSeconds="
							+ (((double) m.getTransferredDataLength()) / (((double) m.getTransferDuration())) * 1000.0));
					if (m.getConversationID().equals(myTransferID)) {
						System.out.println(
								"\tTransfer speed through stats : " + (((double) myStat.getNumberOfIdentifiedBytes())
										/ ((double) myStat.getDurationMilli()) * 1000.0));
					} else if (m.getConversationID().equals(otherConversationID))
						System.out.println("\tTransfer speed through stats : "
								+ (((double) otherStat.getNumberOfIdentifiedBytes())
										/ ((double) otherStat.getDurationMilli()) * 1000.0));
					RealTimeTransferStat globalStatUp = getMadkitConfig().networkProperties.getGlobalStatsBandwidth()
							.getBytesUploadedInRealTime(
									NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_ONE_SECOND_SEGMENTS);
					RealTimeTransferStat globalStatDown = getMadkitConfig().networkProperties.getGlobalStatsBandwidth()
							.getBytesDownloadedInRealTime(
									NetworkProperties.DEFAULT_TRANSFER_STAT_IN_REAL_TIME_PER_ONE_SECOND_SEGMENTS);
					System.out.println("\tGlobal transfer speed (upload) :"
							+ (((double) globalStatUp.getNumberOfIdentifiedBytes())
									/ ((double) globalStatUp.getDurationMilli()) * 1000.0));
					System.out.println("\tGlobal transfer speed (download) :"
							+ (((double) globalStatDown.getNumberOfIdentifiedBytes())
									/ ((double) globalStatDown.getDurationMilli()) * 1000.0));

					if (inputStream != null)
						Assert.assertEquals(inputStream.length(), m.getTransferredDataLength());
					Assert.assertEquals(BigDataResultMessage.Type.BIG_DATA_TRANSFERRED, m.getType());

				} else {
					ok &= m.getType() == BigDataResultMessage.Type.BIG_DATA_TRANSFER_DENIED;
					Assert.assertTrue(ok);
					ok &= m.getConversationID().equals(myTransferID);
					Assert.assertTrue("myTransferID=" + myTransferID + ", conversationID=" + m.getConversationID(), ok);
					ok &= m.getTransferredDataLength() == 0;
					Assert.assertTrue(ok);
					ok &= otherConversationID == null;
					Assert.assertTrue(ok);
					++otherRepliedManaged;
					//myTransferFinished=true;
					/*otherTransferFinished=true;*/

				}
				inputStreams.remove(m.getConversationID());
				myStats.remove(m.getConversationID());
				otherStats.remove(m.getConversationID());

				if (inputStreams.size() == 0)
					myTransferFinished = true;
				if (otherStats.size() == 0 && otherSentManaged == otherSendDataNumber
						&& otherRepliedManaged == otherReplieNumber)
					otherTransferFinished = true;
				if (myTransferFinished && otherTransferFinished) {
					if (ok && nextBigTransfer != null) {
						launchAgent(nextBigTransfer);
					}
					sleep(300);
					this.killAgent(this);
				}
			} catch (SelfKillException e) {
				throw e;
			} catch (Exception e) {
				e.printStackTrace();
				ok = false;
				Assert.fail();
			}
		}
	}
	@Override
	protected void end() {
		//noinspection StatementWithEmptyBody
		while (nextMessage()!=null);

	}
	@Override
	public String toString() {
		String otherLocalAgent=otherBigTransferAgent==null?"":" (other local: "+otherBigTransferAgent+") ";
		if (myTransferFinished && otherTransferFinished && ok
			&& (otherBigTransferAgent==null || (otherBigTransferAgent.myTransferFinished && otherBigTransferAgent.otherTransferFinished && otherBigTransferAgent.ok))
				&& nextBigTransfer != null)
			return "Agent " + this.thisPeerNumber + ", Sub " + nextBigTransfer.toString();
		else
			return "Agent " + this.thisPeerNumber + otherLocalAgent+getStatString();
	}

	@SuppressWarnings("unchecked")
	public String getStatString() {
		StringBuilder res = new StringBuilder();
		res.append(", other number=").append(otherNumber).append(" other sent finished=").append(otherSentManaged).append("/").append(otherSendDataNumber).append(" other replied finished=").append(otherRepliedManaged).append("/").append(otherReplieNumber).append(", otherStatSize=").append(otherStats.size()).append(", alive=").append(this.isAlive()).append(", myTransferFinished=").append(myTransferFinished).append(", otherTransferFinished=").append(otherTransferFinished).append(", hasReceivedProposition=").append(hasReceivedProposition).append("\n\t");
		for (ConversationID s : (new HashMap<>(myStats)).keySet()) {
			RealTimeTransferStat myStat = myStats.get(s);

			res.append(", upload(").append(s).append(")=").append(myStat == null ? Double.valueOf(-1)
					: Double.valueOf((((double) myStat.getNumberOfIdentifiedBytes()) / ((double) myStat.getDurationMilli()))));
		}
		res.append("\n\t");
		for (ConversationID s : (new HashMap<>(otherStats)).keySet()) {
			RealTimeTransferStat otherStat = otherStats.get(s);

			res.append(", download(").append(s).append(")=").append(otherStat == null ? Double.valueOf(-1)
					: Double.valueOf(
					(((double) otherStat.getNumberOfIdentifiedBytes()) / ((double) otherStat.getDurationMilli()))));
		}
		return res.toString();
	}

	public boolean isKilled()
	{
		return !isAlive() && getState().equals(State.TERMINATED) && (otherBigTransferAgent==null || otherBigTransferAgent.isKilled())  && (nextBigTransfer==null || nextBigTransfer.isKilled());
	}
	public boolean isFinished() {
		if (nextBigTransfer != null)
			return (myTransferFinished && otherTransferFinished && (otherBigTransferAgent==null || otherBigTransferAgent.isFinished()) && nextBigTransfer.isFinished()) || !ok;
		else
			return (myTransferFinished && otherTransferFinished && (otherBigTransferAgent==null || otherBigTransferAgent.isFinished()) ) || !ok;
	}

	public boolean isOK() {
		if (nextBigTransfer != null)
			return ok && (otherBigTransferAgent==null || otherBigTransferAgent.ok)  && nextBigTransfer.isOK();
		else
			return ok && (otherBigTransferAgent==null || otherBigTransferAgent.ok);
	}

}
