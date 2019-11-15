package com.distrimind.madkit.kernel.network;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java langage 

This software is governed by the CeCILL-C license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL-C
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL-C license and that you accept its terms.
 */

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.connection.access.AbstractAccessProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.access.AccessProtocolWithP2PAgreementProperties;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolPropertiesWithKeyAgreement;
import com.distrimind.madkit.message.StringMessage;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.madkit.message.hook.OrganizationEvent;
import com.distrimind.madkit.testing.util.agent.NormalAgent;
import com.distrimind.util.crypto.SymmetricAuthentifiedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import org.junit.Assert;
import org.junit.Test;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.1.0
 */
public class DistantGroupAccessTests extends JunitMadkit{
	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
	static final Group groupWithAllRoles=new Group(true, null, false, DistantGroupAccessTests.class.getSimpleName(), "groupWithAllRoles");
	static final Group groupWithAllRolesInOnePeer=new Group(true, null, false, DistantGroupAccessTests.class.getSimpleName(), "groupWithAllRolesInOnePeer");
	static final Group groupWithOneRole=new Group(true, null, false, DistantGroupAccessTests.class.getSimpleName(), "groupWithOneRole");
	static final Group groupNotAccepted=new Group(true, null, false, DistantGroupAccessTests.class.getSimpleName(), "groupNotAccepted");
	static final Group groupWithAllRolesNotDistributed=new Group(false, null, false, DistantGroupAccessTests.class.getSimpleName(), "groupWithAllRolesNotDistributed");
	static final String sharedRole1="SharedRole1";
	static final String sharedRole2="SharedRole2";
	static final String notSharedRole="NotSharedRole";
	static final long timeToWaitBeforeBroadcast=2000;
	static final long timeToWaitBeforeFinalTest=4000;

	public DistantGroupAccessTests() throws UnknownHostException {
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pprotocol.isServer = true;
		p2pprotocol.symmetricEncryptionType=SymmetricEncryptionType.AES_CBC_PKCS5Padding;
		p2pprotocol.symmetricSignatureType= SymmetricAuthentifiedSignatureType.HMAC_SHA2_256;
		ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();
		defaultGroupAccess.addGroupsRoles(groupWithAllRoles);
		defaultGroupAccess.addGroupsRoles(groupWithAllRolesInOnePeer);
		defaultGroupAccess.addGroupsRoles(groupWithAllRolesNotDistributed);
		defaultGroupAccess.addGroupsRoles(groupWithOneRole, sharedRole1);

		AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();
		this.eventListener1 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(p2pprotocol), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
				Collections.singletonList((AbstractIP) new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1"))),
				InetAddress.getByName("0.0.0.0")) {

			@Override
			public void onMadkitPropertiesLoaded(MadkitProperties _properties) {
				super.onMadkitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*2;

			}
		};

		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement u = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		u.isServer = false;
		u.symmetricEncryptionType=p2pprotocol.symmetricEncryptionType;
		u.symmetricSignatureType= p2pprotocol.symmetricSignatureType;

		app = new AccessProtocolWithP2PAgreementProperties();
		defaultGroupAccess=new ListGroupsRoles();
		defaultGroupAccess.addGroupsRoles(groupWithAllRoles);
		defaultGroupAccess.addGroupsRoles(groupWithAllRolesNotDistributed);
		defaultGroupAccess.addGroupsRoles(groupWithOneRole, sharedRole2);

		this.eventListener2 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(u), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
				Collections.singletonList((AbstractIP) new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1"))),
				InetAddress.getByName("0.0.0.0")) {

			@Override
			public void onMadkitPropertiesLoaded(MadkitProperties _properties) {
				super.onMadkitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*2;


			}
		};
	}
	private static final long timeOut = 20000;
	@Test
	public void testGroups() {

		final GroupAccessTesterAgent agent1=new GroupAccessTesterAgent(true);
		final GroupAccessTesterAgent agent2=new GroupAccessTesterAgent(false);
		// addMadkitArgs("--kernelLogLevel",Level.INFO.toString(),"--networkLogLevel",Level.FINEST.toString());
		launchTest(new AbstractAgent() {
			@Override
			protected void end() {
				//noinspection StatementWithEmptyBody
				while(nextMessage()!=null);
			}

			@Override
			protected void activate() throws InterruptedException {
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agent1, eventListener1);
				sleep(400);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agent2, eventListener2);
				sleep(DistantGroupAccessTests.timeToWaitBeforeFinalTest);
				agent1.killAgent(agent1);
				agent2.killAgent(agent2);

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
		agent1.validate();
		agent2.validate();

		cleanHelperMDKs();
	}

}
class GroupAccessTesterAgent extends NormalAgent
{
	private boolean distantAgentRequestGroupWithAllRoles=false;
	private boolean distantAgentRequestGroupWithAllRolesInOnePeer=false;
	private boolean distantAgentRequestGroupWithOneRole =false;
	private final String distantAcceptedRole, distantAcceptedRoleNotRestricted;
	private final String localAcceptedRole, localAcceptedRoleNotRestricted;
	private boolean distantAgentRequestGroupNotAccepted=false;
	private boolean distantAgentRequestGroupWithAllRolesNotDistributed=false;
	public Boolean testOK=null;

	private boolean messageSentToGroupWithAllRoles=false;
	private boolean messageReceivedFromGroupWithAllRoles=false;

	private boolean messageSentToGroupWithOneRoles=false;
	private boolean messageReceivedFromGroupWithOneRoles=false;

	private volatile boolean broadcastMessageSentToGroupWithAllRoles =false;
	private boolean broadcastMessageReceivedFromGroupWithAllRoles=false;

	private volatile boolean broadcastMessageSentToGroupWithOneRole =false;
	private boolean broadcastMessageReceivedFromGroupWithOneRole =false;

	private boolean broadcastMessageSentToGroupWithOneRolesInOnePeer=false;
	private volatile boolean broadcastMessageReceivedFromGroupWithOneRolesInOnePeer=false;

	private volatile boolean broadcastMessageSentToGroupNotAccepted=false;
	private boolean broadcastMessageReceivedFromGroupNotAccepted=false;

	private volatile boolean broadcastMessageSentToGroupNotDistributed=false;
	private boolean broadcastMessageReceivedFromGroupNotDistributed=false;

	private static final String distantAgentRequestGroupWithAllRolesMessage="distantAgentRequestGroupWithAllRolesMessage";
	private static final String distantAgentRequestGroupWithOneRoleMessage ="distantAgentGroupWithOneRoleMessage";

	private static final String distantAgentRequestGroupWithAllRolesBroadcastMessage="distantAgentRequestGroupWithAllRolesBroadcastMessage";
	private static final String distantAgentRequestGroupWithOneRoleBroadcastMessage ="distantAgentGroupWithOneRoleBroadcastMessage";
	private static final String distantAgentRequestGroupWithAllRolesInOnePeerBroadcastMessage="distantAgentRequestGroupWithAllRolesInOnePeerBroadcastMessage";
	private static final String distantAgentRequestGroupNotAcceptedBroadcastMessage="distantAgentRequestGroupNotAcceptedBroadcastMessage";
	private static final String distantAgentRequestGroupWithAllRolesNotDistributedBroadcastMessage="distantAgentRequestGroupWithAllRolesNotDistributedBroadcastMessage";


	private boolean distantAgentLeaveGroupWithAllRoles=false;
	private boolean distantAgentLeaveGroupWithAllRolesInOnePeer=false;
	private boolean distantAgentLeaveGroupWithOneRole=false;
	private boolean distantAgentLeaveGroupNotAccepted=false;
	private boolean distantAgentLeaveGroupWithAllRolesNotDistributed=false;


	GroupAccessTesterAgent(boolean firstAgent)
	{
		if (firstAgent)
		{
			distantAcceptedRole=DistantGroupAccessTests.sharedRole1;
			localAcceptedRole=DistantGroupAccessTests.sharedRole2;
		}
		else
		{
			distantAcceptedRole=DistantGroupAccessTests.sharedRole2;
			localAcceptedRole=DistantGroupAccessTests.sharedRole1;
		}
		localAcceptedRoleNotRestricted=localAcceptedRole+"-NotRestricted";
		distantAcceptedRoleNotRestricted=distantAcceptedRole+"-NotRestricted";
	}

	void validate()
	{
		try {
			Assert.assertTrue(testOK==null || testOK);
			Assert.assertTrue(distantAgentRequestGroupWithAllRoles);
			Assert.assertFalse(distantAgentRequestGroupWithAllRolesInOnePeer);
			Assert.assertTrue(distantAgentRequestGroupWithOneRole);
			Assert.assertFalse(distantAgentRequestGroupNotAccepted);
			Assert.assertFalse(distantAgentRequestGroupWithAllRolesNotDistributed);

			Assert.assertTrue(messageSentToGroupWithAllRoles);
			Assert.assertTrue(messageReceivedFromGroupWithAllRoles);

			Assert.assertTrue(messageSentToGroupWithOneRoles);
			Assert.assertTrue(messageReceivedFromGroupWithOneRoles);

			Assert.assertTrue(broadcastMessageSentToGroupWithAllRoles);
			Assert.assertTrue(broadcastMessageReceivedFromGroupWithAllRoles);

			Assert.assertTrue(broadcastMessageSentToGroupWithOneRole);
			Assert.assertTrue(broadcastMessageReceivedFromGroupWithOneRole);

			Assert.assertFalse(broadcastMessageSentToGroupWithOneRolesInOnePeer);
			Assert.assertFalse(broadcastMessageReceivedFromGroupWithOneRolesInOnePeer);

			Assert.assertFalse(broadcastMessageSentToGroupNotAccepted);
			Assert.assertFalse(broadcastMessageReceivedFromGroupNotAccepted);

			Assert.assertFalse(broadcastMessageSentToGroupNotDistributed);
			Assert.assertFalse(broadcastMessageReceivedFromGroupNotDistributed);

			Assert.assertTrue(distantAgentLeaveGroupWithAllRoles);
			Assert.assertFalse(distantAgentLeaveGroupWithAllRolesInOnePeer);
			Assert.assertTrue(distantAgentLeaveGroupWithOneRole);
			Assert.assertFalse(distantAgentLeaveGroupNotAccepted);
			Assert.assertFalse(distantAgentLeaveGroupWithAllRolesNotDistributed);


			testOK=true;
		}
		catch(AssertionError e)
		{
			testOK=false;
			throw e;
		}
	}
	boolean canReleaseGroups()
	{
		if (testOK!=null && !testOK)
			return false;
		if (!distantAgentRequestGroupWithAllRoles)
			return false;
		if (distantAgentRequestGroupWithAllRolesInOnePeer)
			return false;
		if (!distantAgentRequestGroupWithOneRole)
			return false;
		if (distantAgentRequestGroupNotAccepted)
			return false;
		if (distantAgentRequestGroupWithAllRolesNotDistributed)
			return false;

		if (!messageSentToGroupWithAllRoles)
			return false;
		if (!messageReceivedFromGroupWithAllRoles)
			return false;

		if (!messageSentToGroupWithOneRoles)
			return false;
		if (!messageReceivedFromGroupWithOneRoles)
			return false;

		if (!broadcastMessageSentToGroupWithAllRoles)
			return false;
		if (!broadcastMessageReceivedFromGroupWithAllRoles)
			return false;

		if (!broadcastMessageSentToGroupWithOneRole)
			return false;
		if (!broadcastMessageReceivedFromGroupWithOneRole)
			return false;

		if (broadcastMessageSentToGroupWithOneRolesInOnePeer)
			return false;
		if (broadcastMessageReceivedFromGroupWithOneRolesInOnePeer)
			return false;

		if (broadcastMessageSentToGroupNotAccepted)
			return false;
		if (broadcastMessageReceivedFromGroupNotAccepted)
			return false;

		if (broadcastMessageSentToGroupNotDistributed)
			return false;
		return !broadcastMessageReceivedFromGroupNotDistributed;

	}



	@Override
	protected void activate() throws InterruptedException {
		this.requestHookEvents(HookMessage.AgentActionEvent.REQUEST_ROLE);
		this.requestHookEvents(HookMessage.AgentActionEvent.LEAVE_ROLE);

		requestRole(DistantGroupAccessTests.groupWithAllRoles, localAcceptedRoleNotRestricted);
		requestRole(DistantGroupAccessTests.groupWithOneRole, localAcceptedRole);
		requestRole(DistantGroupAccessTests.groupWithOneRole, DistantGroupAccessTests.notSharedRole);
		requestRole(DistantGroupAccessTests.groupWithAllRolesInOnePeer, localAcceptedRoleNotRestricted);
		requestRole(DistantGroupAccessTests.groupWithAllRolesNotDistributed, localAcceptedRoleNotRestricted);
		requestRole(DistantGroupAccessTests.groupNotAccepted, localAcceptedRoleNotRestricted);
		scheduleTask(new Task<>(new Callable<Object>() {
			@Override
			public Object call()  {
				broadcastMessageSentToGroupWithAllRoles=broadcastMessageWithRole(DistantGroupAccessTests.groupWithAllRoles, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupWithAllRolesBroadcastMessage), localAcceptedRoleNotRestricted).equals(ReturnCode.SUCCESS);
				broadcastMessageSentToGroupWithOneRole =broadcastMessageWithRole(DistantGroupAccessTests.groupWithOneRole, distantAcceptedRole, new StringMessage(distantAgentRequestGroupWithOneRoleBroadcastMessage), localAcceptedRole).equals(ReturnCode.SUCCESS);
				broadcastMessageSentToGroupNotAccepted=broadcastMessageWithRole(DistantGroupAccessTests.groupNotAccepted, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupNotAcceptedBroadcastMessage), localAcceptedRoleNotRestricted).equals(ReturnCode.SUCCESS);
				broadcastMessageSentToGroupWithOneRolesInOnePeer=broadcastMessageWithRole(DistantGroupAccessTests.groupWithAllRolesInOnePeer, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupWithAllRolesInOnePeerBroadcastMessage), localAcceptedRoleNotRestricted).equals(ReturnCode.SUCCESS);
				broadcastMessageSentToGroupNotDistributed=broadcastMessageWithRole(DistantGroupAccessTests.groupWithAllRolesNotDistributed, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupWithAllRolesNotDistributedBroadcastMessage), localAcceptedRoleNotRestricted).equals(ReturnCode.SUCCESS);
				return null;
			}
		}, System.currentTimeMillis()+DistantGroupAccessTests.timeToWaitBeforeBroadcast));
	}

	void fail()
	{
		testOK=false;
		this.killAgent(this);
	}

	@Override
	protected void liveCycle() throws InterruptedException {

		Message m=waitNextMessage();
		if (m instanceof OrganizationEvent)
		{
			if (m.getSender().isFrom(getKernelAddress()))
				return;
			OrganizationEvent hm=(OrganizationEvent)m;
			if (hm.getContent()== HookMessage.AgentActionEvent.REQUEST_ROLE) {
				if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupNotAccepted)) {
					distantAgentRequestGroupNotAccepted = true;
					fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentRequestGroupWithAllRolesNotDistributed = true;
					fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentRequestGroupWithAllRolesInOnePeer = true;
					fail();
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRoles)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRoleNotRestricted)) {
						distantAgentRequestGroupWithAllRoles=true;
						Assert.assertEquals(ReturnCode.SUCCESS, sendMessage(hm.getSourceAgent(), new StringMessage(distantAgentRequestGroupWithAllRolesMessage)));
						messageSentToGroupWithAllRoles=true;
					}
					else {
						fail();
					}
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithOneRole)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRole)) {
						distantAgentRequestGroupWithOneRole =true;
						Assert.assertEquals(ReturnCode.SUCCESS, sendMessage(hm.getSourceAgent(), new StringMessage(distantAgentRequestGroupWithOneRoleMessage)));
						messageSentToGroupWithOneRoles=true;
					}
					else {
						fail();
					}
				}
			}
			else if (hm.getContent()== HookMessage.AgentActionEvent.LEAVE_ROLE) {
				if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupNotAccepted)) {
					distantAgentLeaveGroupNotAccepted = true;
					fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentLeaveGroupWithAllRolesNotDistributed = true;
					fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentLeaveGroupWithAllRolesInOnePeer = true;
					fail();
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRoles)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRoleNotRestricted)) {
						distantAgentLeaveGroupWithAllRoles=true;
					}
					else {
						fail();
					}
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithOneRole)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRole)) {
						distantAgentLeaveGroupWithOneRole =true;
					}
					else {
						fail();
					}
				}
			}
		}
		else if (m instanceof StringMessage) {
			StringMessage sm=(StringMessage)m;
			if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupNotAccepted)) {
				broadcastMessageReceivedFromGroupNotAccepted = true;
				fail();
			} else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
				broadcastMessageReceivedFromGroupNotDistributed = true;
				fail();
			} else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
				broadcastMessageReceivedFromGroupWithOneRolesInOnePeer = true;
				fail();
			}
			else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithAllRoles)) {
				if (sm.getSender().getRole().equals(localAcceptedRoleNotRestricted) && sm.getReceiver().getRole().equals(distantAcceptedRoleNotRestricted)) {
					if (sm.getContent().equals(distantAgentRequestGroupWithAllRolesMessage))
					{
						messageReceivedFromGroupWithAllRoles=true;
					}
					else if (sm.getContent().equals(distantAgentRequestGroupWithAllRolesBroadcastMessage))
					{
						broadcastMessageReceivedFromGroupWithAllRoles=true;
					}
					else
					{
						fail();
					}
				}
				else {
					fail();
				}
			}
			else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithOneRole)) {
				if (sm.getSender().getRole().equals(localAcceptedRole) && sm.getReceiver().getRole().equals(distantAcceptedRole)) {
					if (sm.getContent().equals(distantAgentRequestGroupWithOneRoleMessage))
					{
						messageReceivedFromGroupWithOneRoles=true;
					}
					else if (sm.getContent().equals(distantAgentRequestGroupWithOneRoleBroadcastMessage))
					{
						broadcastMessageReceivedFromGroupWithOneRole =true;
					}
					else
					{
						fail();
					}
				}
				else {
					fail();
				}
			}
			if (canReleaseGroups())
			{
				leaveRole(DistantGroupAccessTests.groupWithAllRoles, localAcceptedRoleNotRestricted);
				leaveRole(DistantGroupAccessTests.groupWithOneRole, localAcceptedRole);
				leaveRole(DistantGroupAccessTests.groupWithOneRole, DistantGroupAccessTests.notSharedRole);
				leaveRole(DistantGroupAccessTests.groupWithAllRolesInOnePeer, localAcceptedRoleNotRestricted);
				leaveRole(DistantGroupAccessTests.groupWithAllRolesNotDistributed, localAcceptedRoleNotRestricted);
				leaveRole(DistantGroupAccessTests.groupNotAccepted, localAcceptedRoleNotRestricted);

			}
		}
	}

	@Override
	protected void end() {
		//noinspection StatementWithEmptyBody
		while(nextMessage()!=null);
	}
}