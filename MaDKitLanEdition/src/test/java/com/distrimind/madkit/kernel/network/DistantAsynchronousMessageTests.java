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
import com.distrimind.ood.database.EmbeddedH2DatabaseWrapper;
import com.distrimind.ood.database.InMemoryEmbeddedH2DatabaseFactory;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.crypto.SymmetricAuthenticatedSignatureType;
import com.distrimind.util.crypto.SymmetricEncryptionType;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.11
 */
public class DistantAsynchronousMessageTests extends TestNGMadkit {

	final NetworkEventListener eventListener1;
	final NetworkEventListener eventListener2;
	final AtomicInteger counter=new AtomicInteger(0);

	public DistantAsynchronousMessageTests() throws UnknownHostException {
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement p2pprotocol=new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		p2pprotocol.isServer = true;
		p2pprotocol.symmetricEncryptionType= SymmetricEncryptionType.AES_CTR;
		p2pprotocol.symmetricSignatureType= SymmetricAuthenticatedSignatureType.HMAC_SHA2_384;
		ListGroupsRoles defaultGroupAccess=new ListGroupsRoles();
		defaultGroupAccess.addGroupsRoles(TestNGMadkit.GROUP,TestNGMadkit.ROLE, TestNGMadkit.ROLE2);
		defaultGroupAccess.addGroupsRoles(TestNGMadkit.GROUP2,TestNGMadkit.ROLE, TestNGMadkit.ROLE2);


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
				try {
					_properties.setDatabaseFactory(new InMemoryEmbeddedH2DatabaseFactory("dbmem"+counter.getAndIncrement()));
				} catch (DatabaseException e) {
					e.printStackTrace();
				}
			}
		};
		P2PSecuredConnectionProtocolPropertiesWithKeyAgreement u = new P2PSecuredConnectionProtocolPropertiesWithKeyAgreement();
		u.isServer = false;
		u.symmetricEncryptionType=p2pprotocol.symmetricEncryptionType;
		u.symmetricSignatureType= p2pprotocol.symmetricSignatureType;

		app = new AccessProtocolWithP2PAgreementProperties();

		this.eventListener2 = new NetworkEventListener(true, false, false, null,
				new ConnectionsProtocolsMKEventListener(u), new AccessProtocolPropertiesMKEventListener(app),
				new AccessDataMKEventListener(AccessDataMKEventListener.getDefaultAccessData(defaultGroupAccess)), 5000,
				Collections.singletonList(new DoubleIP(5000, (Inet4Address) InetAddress.getByName("127.0.0.1"), (Inet6Address) InetAddress.getByName("::1")))
		) {

			@Override
			public void onMaDKitPropertiesLoaded(MadkitProperties _properties) {
				super.onMaDKitPropertiesLoaded(_properties);
				_properties.networkProperties.networkLogLevel = Level.INFO;
				try {
					_properties.setDatabaseFactory(new InMemoryEmbeddedH2DatabaseFactory("dbmem"+counter.getAndIncrement()));
				} catch (DatabaseException e) {
					e.printStackTrace();
				}
			}
		};
	}

	private static final File databaseFile=new File("asynchronousDatabaseFile");
	private static final MadkitEventListener madkitEventListener= properties -> {
		try {
			properties.setDatabaseFactory(new InMemoryEmbeddedH2DatabaseFactory(databaseFile.getName()));
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	};
	@BeforeClass
	public static void beforeClass()
	{
		EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(databaseFile);
	}
	@AfterClass
	public static void afterClass()
	{
		EmbeddedH2DatabaseWrapper.deleteDatabasesFiles(databaseFile);
	}

	private void launchPeers(AbstractAgent launcher, AbstractAgent sender, AbstractAgent receiver) throws InterruptedException {
		launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, receiver, eventListener1);
		launcher.sleep(400);
		launchThreadedMKNetworkInstance(Level.INFO, AbstractAgent.class, sender, eventListener2);
		launcher.sleep(400);
	}

	private void closePeers(AbstractAgent launcher) throws InterruptedException {
		for (Madkit mk : getHelperInstances(launcher, 2))
			stopNetwork(mk);
		for (Madkit mk : getHelperInstances(launcher, 2)) {
			checkConnectedKernelsNb(launcher, mk, 0, 10000);
			checkConnectedIntancesNb(launcher, mk, 0, 10000);
		}
		launcher.sleep(400);
		cleanHelperMDKs(launcher);
		AssertJUnit.assertEquals(getHelperInstances(launcher, 0).size(), 0);
	}

	@Test
	public void testAsynchroneMessagesWhenNoDatabaseWasLoaded()
	{
		launchTest(new AbstractAgent(){
			@Override
			protected void activate() throws InterruptedException {
				super.activate();

				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				AbstractAgent sender=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
					}
				};
				launchPeers(this, sender, receiver);

				ReturnCode rc=sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
				AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);
				rc=sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE, new StringMessage("ok"), TestNGMadkit.ROLE);
				AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);
				rc=sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE2);
				AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);

				closePeers(this);
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false);
	}
	@Test
	public void testNullPointerExceptionsAndIllegalArgumentsException()
	{

		launchTest(new AbstractAgent(){
			@Override
			protected void activate() throws InterruptedException {
				super.activate();

				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				AbstractAgent sender=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
					}
				};
				launchPeers(this, sender, receiver);

				try {
					sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new Message(), TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(IllegalArgumentException ignored)
				{
				}

				try {
					sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(null, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, null, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesBySenderRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesByReceiverRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesNumberBySenderRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesNumberByReceiverRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.getCurrentAsynchronousMessagesNumberByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.cancelCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.cancelCurrentAsynchronousMessagesBySenderRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.cancelCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.cancelCurrentAsynchronousMessagesByReceiverRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sender.cancelCurrentAsynchronousMessagesByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				closePeers(this);
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);

	}
	@Test
	public void testSynchronousMessage()
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();

				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				AbstractAgent sender = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
					}
				};
				launchPeers(this, sender, receiver);

				try {


					ReturnCode rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.ROLE_NOT_HANDLED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP2, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.NOT_IN_GROUP, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));

					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);


					AssertJUnit.assertEquals(2, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(1).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					cancelCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());

					sender.cancelCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterAsynchronousMessages = Collections.singletonList(TestNGMadkit.GROUPB.getPath());
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					AssertJUnit.assertEquals(2, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(1).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					cancelCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB);
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterAsynchronousMessages = Collections.singletonList(TestNGMadkit.GROUPB.getSubGroup("awesome sub group").getPath());
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
				closePeers(this);
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}
	@Test
	public void testSynchronousMessageWithTimeOut()
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();

				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				AbstractAgent sender = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
					}
				};
				launchPeers(this, sender, receiver);
				try {


					ReturnCode rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.ROLE_NOT_HANDLED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP2, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.NOT_IN_GROUP, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));

					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE, 1000);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);


					AssertJUnit.assertEquals(2, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(1).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					sleep(2000);
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());

					cleanObsoleteMaDKitDataNow();

					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());

					sender.cancelCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
				closePeers(this);
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}
	@Test
	public void testASynchronousMessageWithSenderAvailable()
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();

				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				AbstractAgent sender = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);

					}
				};
				launchPeers(this, sender, receiver);
				try {


					ReturnCode rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					rc=receiver.requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2 );
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, rc);
					sleep(400);
					m = receiver.nextMessage();
					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
				closePeers(this);
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);

	}
	@Test
	public void testASynchronousMessageWithSenderNotAvailable()
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();

				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				AbstractAgent sender = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);

					}
				};
				launchPeers(this, sender, receiver);
				try {
					sleep(500);

					ReturnCode rc = sender.sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					sender.leaveRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					receiver.requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2 );
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) sender.getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					sender.requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);

					sleep(400);
					m = receiver.nextMessage();

					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, sender.getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
				closePeers(this);
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}
}
