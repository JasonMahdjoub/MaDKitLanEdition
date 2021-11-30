package com.distrimind.madkit.kernel;
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

import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeClass;
import org.testng.AssertJUnit;
import com.distrimind.madkit.message.StringMessage;
import com.distrimind.ood.database.EmbeddedH2DatabaseWrapper;
import com.distrimind.ood.database.InMemoryEmbeddedH2DatabaseFactory;
import com.distrimind.ood.database.exceptions.DatabaseException;
import java.io.File;
import java.util.Collections;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.11
 */
public class LocalAsynchronousMessageTests extends TestNGMadkit {
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

	@Test
	public void testAsynchroneMessagesWhenNoDatabaseWasLoaded()
	{
		launchTest(new AbstractAgent(){
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				launchAgent(receiver);
				ReturnCode rc=sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
				AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);
				rc=sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE, new StringMessage("ok"), TestNGMadkit.ROLE);
				AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);
				rc=sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE2);
				AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);
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
				requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				launchAgent(receiver);
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new Message(), TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(IllegalArgumentException ignored)
				{
				}

				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(null, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, null, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesBySenderRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesByReceiverRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesNumberBySenderRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesNumberByReceiverRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousMessagesNumberByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousMessagesBySenderRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousMessagesByReceiverRole(null, TestNGMadkit.ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousMessagesByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}

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
				requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				launchAgent(receiver);
				try {


					ReturnCode rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.ROLE_NOT_HANDLED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.NOT_IN_GROUP, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));

					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);


					AssertJUnit.assertEquals(2, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(1).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					AssertJUnit.assertEquals(1, cancelCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());

					AssertJUnit.assertEquals(1, cancelCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterAsynchronousMessages = Collections.singletonList(TestNGMadkit.GROUPB.getPath());
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					AssertJUnit.assertEquals(2, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(1).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					cancelCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterAsynchronousMessages = Collections.singletonList(TestNGMadkit.GROUPB.getSubGroup("awesome sub group").getPath());
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(ReturnCode.IGNORED, rc);

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
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
				requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2);
					}
				};
				launchAgent(receiver);
				try {


					ReturnCode rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.ROLE_NOT_HANDLED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.NOT_IN_GROUP, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE);
					requestRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));

					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE, new StringMessage("ok2"), TestNGMadkit.ROLE2);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2, new StringMessage("ok2"), TestNGMadkit.ROLE, 1000);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);


					AssertJUnit.assertEquals(2, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(1).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					sleep(2000);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());

					cleanObsoleteMaDKitDataNow();

					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));

					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUPB).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok2", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());

					AssertJUnit.assertEquals(1, cancelCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUPB, TestNGMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
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
				requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				launchAgent(receiver);
				try {


					ReturnCode rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					rc=receiver.requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2 );
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, rc);
					sleep(100);
					m = receiver.nextMessage();
					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
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
				requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				launchAgent(receiver);
				try {


					ReturnCode rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(TestNGMadkit.GROUP, TestNGMadkit.ROLE2, new StringMessage("ok"), TestNGMadkit.ROLE);
					AssertJUnit.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					Message m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					leaveRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					receiver.requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2 );
					m = receiver.nextMessage();
					AssertJUnit.assertNull(m);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByGroup(TestNGMadkit.GROUP).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2).get(0).getAsynchronousMessage()).getContent());
					AssertJUnit.assertEquals("ok", ((StringMessage) getCurrentAsynchronousMessagesBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE).get(0).getAsynchronousMessage()).getContent());

					requestRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE);
					sleep(100);
					m = receiver.nextMessage();
					AssertJUnit.assertNotNull(m);
					AssertJUnit.assertEquals("ok", ((StringMessage) m).getContent());
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByGroup(TestNGMadkit.GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberByReceiverRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousMessagesNumberBySenderRole(TestNGMadkit.GROUP, TestNGMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}
}
