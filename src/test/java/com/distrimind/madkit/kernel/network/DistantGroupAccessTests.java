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

import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.AssertJUnit;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.connection.access.AbstractAccessProtocolProperties;
import com.distrimind.madkit.kernel.network.connection.access.AccessProtocolWithP2PAgreementProperties;
import com.distrimind.madkit.kernel.network.connection.access.ListGroupsRoles;
import com.distrimind.madkit.kernel.network.connection.secured.P2PSecuredConnectionProtocolPropertiesWithKeyAgreement;
import com.distrimind.madkit.message.StringMessage;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.madkit.message.hook.OrganizationEvent;
import com.distrimind.madkit.testing.util.agent.NormalAgent;
import com.distrimind.util.crypto.SymmetricAuthenticatedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.1.0
 */
public class DistantGroupAccessTests extends TestNGMadkit {
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
	static final long timeToWaitBeforeFinalTest=5000;

	public DistantGroupAccessTests() throws UnknownHostException {
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pprotocol.isServer = true;
		p2pprotocol.symmetricEncryptionType=SymmetricEncryptionType.AES_CBC_PKCS5Padding;
		p2pprotocol.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
		ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();
		defaultGroupAccess.addGroupsRoles(groupWithAllRoles);
		defaultGroupAccess.addGroupsRoles(groupWithAllRolesInOnePeer);
		defaultGroupAccess.addGroupsRoles(groupWithAllRolesNotDistributed);
		defaultGroupAccess.addGroupsRoles(groupWithOneRole, sharedRole1);

		AbstractAccessProtocolProperties app = new AccessProtocolWithP2PAgreementProperties();
		this.eventListener1 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(p2pprotocol), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
				Collections.emptyList(),
				InetAddress.getByName("127.0.0.1"), InetAddress.getByName("::1")) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
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
				Collections.singletonList(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"),
						(Inet6Address) InetAddress.getByName("::1")))) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				_properties.networkProperties.maxBufferSize=Short.MAX_VALUE*2;


			}
		};
	}
	private static final long timeOut = 20000;
	@Test
	public void testDistantGroupAccess() throws InterruptedException {

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
				System.out.println("Begin test for distant group access");
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agent1, eventListener1);
				sleep(400);
				launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, agent2, eventListener2);
				sleep(DistantGroupAccessTests.timeToWaitBeforeFinalTest);

				for (Madkit mk : getHelperInstances(this, 2))
					stopNetworkWithMadkit(mk);

				for (Madkit mk : getHelperInstances(this, 2)) {
					checkConnectedKernelsNb(this, mk, 0, timeOut);
					checkConnectedIntancesNb(this, mk, 0, timeOut);
				}
				sleep(400);




			}
		});
		agent1.validate();
		agent2.validate();
		Thread.sleep(400);
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
	private boolean released=false;

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
			AssertJUnit.assertTrue(testOK==null || testOK);
			AssertJUnit.assertTrue(distantAgentRequestGroupWithAllRoles);
			AssertJUnit.assertFalse(distantAgentRequestGroupWithAllRolesInOnePeer);
			AssertJUnit.assertTrue(distantAgentRequestGroupWithOneRole);
			AssertJUnit.assertFalse(distantAgentRequestGroupNotAccepted);
			AssertJUnit.assertFalse(distantAgentRequestGroupWithAllRolesNotDistributed);

			AssertJUnit.assertTrue(messageSentToGroupWithAllRoles);
			AssertJUnit.assertTrue(messageReceivedFromGroupWithAllRoles);

			AssertJUnit.assertTrue(messageSentToGroupWithOneRoles);
			AssertJUnit.assertTrue(messageReceivedFromGroupWithOneRoles);

			AssertJUnit.assertTrue(broadcastMessageSentToGroupWithAllRoles);
			AssertJUnit.assertTrue(broadcastMessageReceivedFromGroupWithAllRoles);

			AssertJUnit.assertTrue(broadcastMessageSentToGroupWithOneRole);
			AssertJUnit.assertTrue(broadcastMessageReceivedFromGroupWithOneRole);

			AssertJUnit.assertFalse(broadcastMessageSentToGroupWithOneRolesInOnePeer);
			AssertJUnit.assertFalse(broadcastMessageReceivedFromGroupWithOneRolesInOnePeer);

			AssertJUnit.assertFalse(broadcastMessageSentToGroupNotAccepted);
			AssertJUnit.assertFalse(broadcastMessageReceivedFromGroupNotAccepted);

			AssertJUnit.assertFalse(broadcastMessageSentToGroupNotDistributed);
			AssertJUnit.assertFalse(broadcastMessageReceivedFromGroupNotDistributed);

			AssertJUnit.assertTrue(distantAgentLeaveGroupWithAllRoles);
			AssertJUnit.assertFalse(distantAgentLeaveGroupWithAllRolesInOnePeer);

			AssertJUnit.assertFalse(distantAgentLeaveGroupNotAccepted);
			AssertJUnit.assertFalse(distantAgentLeaveGroupWithAllRolesNotDistributed);
			AssertJUnit.assertTrue(distantAgentLeaveGroupWithOneRole);

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
		scheduleTask(new Task<Void>(System.currentTimeMillis() + DistantGroupAccessTests.timeToWaitBeforeBroadcast) {
			@Override
			public Void call() {
				ReturnCode rc=broadcastMessageWithRole(DistantGroupAccessTests.groupWithAllRoles, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupWithAllRolesBroadcastMessage), localAcceptedRoleNotRestricted);
				broadcastMessageSentToGroupWithAllRoles=rc==ReturnCode.SUCCESS || rc==ReturnCode.TRANSFER_IN_PROGRESS;
				rc=broadcastMessageWithRole(DistantGroupAccessTests.groupWithOneRole, distantAcceptedRole, new StringMessage(distantAgentRequestGroupWithOneRoleBroadcastMessage), localAcceptedRole);
				broadcastMessageSentToGroupWithOneRole =rc==ReturnCode.SUCCESS || rc==ReturnCode.TRANSFER_IN_PROGRESS;
				rc=broadcastMessageWithRole(DistantGroupAccessTests.groupNotAccepted, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupNotAcceptedBroadcastMessage), localAcceptedRoleNotRestricted);
				broadcastMessageSentToGroupNotAccepted=rc==ReturnCode.SUCCESS || rc==ReturnCode.TRANSFER_IN_PROGRESS;
				rc=broadcastMessageWithRole(DistantGroupAccessTests.groupWithAllRolesInOnePeer, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupWithAllRolesInOnePeerBroadcastMessage), localAcceptedRoleNotRestricted);
				broadcastMessageSentToGroupWithOneRolesInOnePeer= rc==ReturnCode.SUCCESS || rc==ReturnCode.TRANSFER_IN_PROGRESS;
				rc=broadcastMessageWithRole(DistantGroupAccessTests.groupWithAllRolesNotDistributed, distantAcceptedRoleNotRestricted, new StringMessage(distantAgentRequestGroupWithAllRolesNotDistributedBroadcastMessage), localAcceptedRoleNotRestricted);
				broadcastMessageSentToGroupNotDistributed=rc==ReturnCode.SUCCESS || rc==ReturnCode.TRANSFER_IN_PROGRESS;
				return null;
			}
		});
	}

	@Override
	protected void liveCycle() throws InterruptedException {

		Message m=waitNextMessage();
		if (m instanceof OrganizationEvent)
		{
			OrganizationEvent hm=(OrganizationEvent)m;
			if (hm.getSourceAgent().isFrom(getKernelAddress()))
				return;
			if (hm.getContent()== HookMessage.AgentActionEvent.REQUEST_ROLE) {
				if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupNotAccepted)) {
					distantAgentRequestGroupNotAccepted = true;
					Assert.fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentRequestGroupWithAllRolesNotDistributed = true;
					Assert.fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentRequestGroupWithAllRolesInOnePeer = true;
					Assert.fail();
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRoles)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRoleNotRestricted)) {
						distantAgentRequestGroupWithAllRoles = true;
						ReturnCode rc=sendMessageWithRole(hm.getSourceAgent(), new StringMessage(distantAgentRequestGroupWithAllRolesMessage), localAcceptedRoleNotRestricted);
						AssertJUnit.assertTrue(rc.equals(ReturnCode.SUCCESS) || rc.equals(ReturnCode.TRANSFER_IN_PROGRESS) );
						messageSentToGroupWithAllRoles = true;
					}
					else if (!hm.getSourceAgent().getRole().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)){
						System.err.println(hm.getSourceAgent());
						Assert.fail();
					}
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithOneRole)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRole)) {
						distantAgentRequestGroupWithOneRole =true;
						ReturnCode rc=sendMessageWithRole(hm.getSourceAgent(), new StringMessage(distantAgentRequestGroupWithOneRoleMessage), localAcceptedRole);
						AssertJUnit.assertTrue(rc.equals(ReturnCode.SUCCESS) || rc.equals(ReturnCode.TRANSFER_IN_PROGRESS) );
						messageSentToGroupWithOneRoles=true;
					}
					else if (!hm.getSourceAgent().getRole().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)){
						Assert.fail();
					}
				}
			}
			else if (hm.getContent()== HookMessage.AgentActionEvent.LEAVE_ROLE) {
				if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupNotAccepted)) {
					distantAgentLeaveGroupNotAccepted = true;
					Assert.fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentLeaveGroupWithAllRolesNotDistributed = true;
					Assert.fail();
				} else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
					distantAgentLeaveGroupWithAllRolesInOnePeer = true;
					Assert.fail();
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithAllRoles)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRoleNotRestricted)) {
						distantAgentLeaveGroupWithAllRoles=true;
					}
					else if (!hm.getSourceAgent().getRole().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)){
						Assert.fail();
					}
				}
				else if (hm.getSourceAgent().getGroup().equals(DistantGroupAccessTests.groupWithOneRole)) {
					if (hm.getSourceAgent().getRole().equals(distantAcceptedRole)) {
						distantAgentLeaveGroupWithOneRole =true;
					}
					else if (!hm.getSourceAgent().getRole().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE)){
						Assert.fail();
					}
				}
				else
					Assert.fail(hm.toString());
			}
		}
		else if (m instanceof StringMessage) {
			StringMessage sm=(StringMessage)m;
			if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupNotAccepted)) {
				broadcastMessageReceivedFromGroupNotAccepted = true;
				Assert.fail();
			} else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
				broadcastMessageReceivedFromGroupNotDistributed = true;
				Assert.fail();
			} else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithAllRolesNotDistributed)) {
				broadcastMessageReceivedFromGroupWithOneRolesInOnePeer = true;
				Assert.fail();
			}
			else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithAllRoles)) {
				if (sm.getSender().getRole().equals(distantAcceptedRoleNotRestricted) && sm.getReceiver().getRole().equals(localAcceptedRoleNotRestricted)) {
					if (sm.getContent().equals(distantAgentRequestGroupWithAllRolesMessage))
					{
						messageReceivedFromGroupWithAllRoles=true;
					}
					else if (sm.getContent().equals(distantAgentRequestGroupWithAllRolesBroadcastMessage))
					{
						broadcastMessageReceivedFromGroupWithAllRoles=true;
					}
					else if (!sm.getContent().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE))
					{
						Assert.fail();
					}
				}
				else {
					System.err.println("Role : "+sm.getSender().getRole());
					Assert.fail();
				}
			}
			else if (sm.getSender().getGroup().equals(DistantGroupAccessTests.groupWithOneRole)) {
				if (sm.getSender().getRole().equals(distantAcceptedRole) && sm.getReceiver().getRole().equals(localAcceptedRole)) {
					if (sm.getContent().equals(distantAgentRequestGroupWithOneRoleMessage))
					{
						messageReceivedFromGroupWithOneRoles=true;
					}
					else if (sm.getContent().equals(distantAgentRequestGroupWithOneRoleBroadcastMessage))
					{
						broadcastMessageReceivedFromGroupWithOneRole =true;
					}
					else if (!sm.getContent().equals(com.distrimind.madkit.agr.Organization.GROUP_MANAGER_ROLE))
					{
						Assert.fail();
					}
				}
				else {
					Assert.fail();
				}
			}
		}
		if (canReleaseGroups() && !released)
		{
			released=true;
			sleep(1000);
			Assert.assertEquals(leaveRole(DistantGroupAccessTests.groupWithAllRoles, localAcceptedRoleNotRestricted), ReturnCode.SUCCESS);

			Assert.assertEquals(leaveRole(DistantGroupAccessTests.groupWithOneRole, localAcceptedRole), ReturnCode.SUCCESS);
			Assert.assertEquals(leaveRole(DistantGroupAccessTests.groupWithOneRole, DistantGroupAccessTests.notSharedRole), ReturnCode.SUCCESS);
			Assert.assertEquals(leaveRole(DistantGroupAccessTests.groupWithAllRolesInOnePeer, localAcceptedRoleNotRestricted), ReturnCode.SUCCESS);
			Assert.assertEquals(leaveRole(DistantGroupAccessTests.groupWithAllRolesNotDistributed, localAcceptedRoleNotRestricted), ReturnCode.SUCCESS);
			Assert.assertEquals(leaveRole(DistantGroupAccessTests.groupNotAccepted, localAcceptedRoleNotRestricted), ReturnCode.SUCCESS);

		}
	}

	@Override
	protected void end() {
		//noinspection StatementWithEmptyBody
		while(nextMessage()!=null);
	}
}