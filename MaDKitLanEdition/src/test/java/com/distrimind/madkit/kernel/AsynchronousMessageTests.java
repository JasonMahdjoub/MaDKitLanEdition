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

import com.distrimind.madkit.message.StringMessage;
import com.distrimind.ood.database.EmbeddedH2DatabaseFactory;
import com.distrimind.ood.database.EmbeddedH2DatabaseWrapper;
import com.distrimind.ood.database.exceptions.DatabaseException;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collections;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.11
 */
public class AsynchronousMessageTests extends JunitMadkit{
	private static final File databaseFile=new File("asynchronousDatabaseFile");
	private static final MadkitEventListener madkitEventListener=new MadkitEventListener() {
		@Override
		public void onMaDKitPropertiesLoaded(MadkitProperties properties) {
			try {
				properties.setDatabaseFactory(new EmbeddedH2DatabaseFactory(databaseFile));
			} catch (DatabaseException e) {
				e.printStackTrace();
			}
		}
	};
	@BeforeClass
	public static void beforeClass()
	{
		EmbeddedH2DatabaseWrapper.deleteDatabaseFiles(databaseFile);
	}
	@AfterClass
	public static void afterClass()
	{
		EmbeddedH2DatabaseWrapper.deleteDatabaseFiles(databaseFile);
	}

	@Test
	public void testAsynchroneMessagesWhenNoDatabaseWasLoaded()
	{
		launchTest(new AbstractAgent(){
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE);
				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE2);
					}
				};
				launchAgent(receiver);
				ReturnCode rc=sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new StringMessage("ok"), JunitMadkit.ROLE);
				Assert.assertEquals(ReturnCode.IGNORED, rc);
				rc=sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE, new StringMessage("ok"), JunitMadkit.ROLE);
				Assert.assertEquals(ReturnCode.IGNORED, rc);
				rc=sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new StringMessage("ok"), JunitMadkit.ROLE2);
				Assert.assertEquals(ReturnCode.IGNORED, rc);
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
				requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE);
				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE2);
					}
				};
				launchAgent(receiver);
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new Message(), JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(IllegalArgumentException ignored)
				{
				}

				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(null, JunitMadkit.ROLE2, new StringMessage("ok"), JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, null, new StringMessage("ok"), JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, null, JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new StringMessage("ok"), null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesBySenderRole(JunitMadkit.GROUP, null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesBySenderRole(null, JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesByReceiverRole(JunitMadkit.GROUP, null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesByReceiverRole(null, JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesByGroup(null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesNumberBySenderRole(null, JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesNumberByReceiverRole(null, JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getDifferedMessagesNumberByGroup(null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelDifferedMessagesBySenderRole(JunitMadkit.GROUP, null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelDifferedMessagesBySenderRole(null, JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelDifferedMessagesByReceiverRole(JunitMadkit.GROUP, null);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelDifferedMessagesByReceiverRole(null, JunitMadkit.ROLE);
					Assert.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelDifferedMessagesByGroup(null);
					Assert.fail();
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
				requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE2);
					}
				};
				launchAgent(receiver);
				try {


					ReturnCode rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new StringMessage("ok"), JunitMadkit.ROLE);
					Assert.assertEquals(ReturnCode.SUCCESS, rc);
					Message m = receiver.nextMessage();
					Assert.assertNotNull(m);
					Assert.assertEquals("ok", ((StringMessage) m).getContent());
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new StringMessage("ok2"), JunitMadkit.ROLE2);
					Assert.assertEquals(ReturnCode.ROLE_NOT_HANDLED, rc);
					m = receiver.nextMessage();
					Assert.assertNull(m);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP2, JunitMadkit.ROLE2, new StringMessage("ok2"), JunitMadkit.ROLE);
					Assert.assertEquals(ReturnCode.NOT_IN_GROUP, rc);
					m = receiver.nextMessage();
					Assert.assertNull(m);
					requestRole(JunitMadkit.GROUP2, JunitMadkit.ROLE);
					Assert.assertEquals(0, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP));
					Assert.assertEquals(0, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE));
					Assert.assertEquals(0, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE));

					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP2, JunitMadkit.ROLE, new StringMessage("ok2"), JunitMadkit.ROLE);
					Assert.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					Assert.assertNull(m);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP2, JunitMadkit.ROLE2, new StringMessage("ok2"), JunitMadkit.ROLE);
					Assert.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					m = receiver.nextMessage();
					Assert.assertNull(m);


					Assert.assertEquals(2, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP2));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE2));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));
					Assert.assertEquals(2, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));

					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP2).get(1).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());

					cancelDifferedMessagesByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE2);
					Assert.assertEquals(1, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP2));
					Assert.assertEquals(0, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE2));
					Assert.assertEquals(1, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));

					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());

					cancelDifferedMessagesBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE);
					Assert.assertEquals(0, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP2));
					Assert.assertEquals(0, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE2));
					Assert.assertEquals(0, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterDifferedMessages = Collections.singletonList(JunitMadkit.GROUP2.getPath());
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP2, JunitMadkit.ROLE, new StringMessage("ok2"), JunitMadkit.ROLE);
					m = receiver.nextMessage();
					Assert.assertNull(m);
					Assert.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP2, JunitMadkit.ROLE2, new StringMessage("ok2"), JunitMadkit.ROLE);
					m = receiver.nextMessage();
					Assert.assertNull(m);
					Assert.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					Assert.assertEquals(2, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP2));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE2));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));
					Assert.assertEquals(2, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP2).get(1).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok2", ((StringMessage) getDifferedMessagesBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());
					cancelDifferedMessagesByGroup(JunitMadkit.GROUP2);
					Assert.assertEquals(0, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP2));
					Assert.assertEquals(0, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));
					Assert.assertEquals(0, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP2, JunitMadkit.ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterDifferedMessages = Collections.singletonList(JunitMadkit.GROUP2.getSubGroup("awesome sub group").getPath());
					rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP2, JunitMadkit.ROLE, new StringMessage("ok2"), JunitMadkit.ROLE);
					m = receiver.nextMessage();
					Assert.assertNull(m);
					Assert.assertEquals(ReturnCode.IGNORED, rc);

				} catch (Exception e) {
					e.printStackTrace();
					Assert.fail();
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
				requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				launchAgent(receiver);
				try {


					ReturnCode rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new StringMessage("ok"), JunitMadkit.ROLE);
					Assert.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					Message m = receiver.nextMessage();
					Assert.assertNull(m);
					Assert.assertEquals(1, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2));
					Assert.assertEquals(1, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE));

					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());

					rc=receiver.requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE2 );
					Assert.assertEquals(ReturnCode.SUCCESS, rc);
					m = receiver.nextMessage();
					Assert.assertNotNull(m);
					Assert.assertEquals("ok", ((StringMessage) m).getContent());
					Assert.assertEquals(0, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP));
					Assert.assertEquals(0, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2));
					Assert.assertEquals(0, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					Assert.fail();
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
				requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				launchAgent(receiver);
				try {


					ReturnCode rc = sendMessageWithRoleOrDifferSendingUntilRecipientWasFound(JunitMadkit.GROUP, JunitMadkit.ROLE2, new StringMessage("ok"), JunitMadkit.ROLE);
					Assert.assertEquals(ReturnCode.MESSAGE_DIFFERED, rc);
					Message m = receiver.nextMessage();
					Assert.assertNull(m);
					Assert.assertEquals(1, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2));
					Assert.assertEquals(1, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE));

					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());

					leaveRole(JunitMadkit.GROUP, JunitMadkit.ROLE);
					m = receiver.nextMessage();
					Assert.assertNull(m);
					Assert.assertEquals(1, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2));
					Assert.assertEquals(1, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE));

					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());

					receiver.requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE2 );
					m = receiver.nextMessage();
					Assert.assertNull(m);
					Assert.assertEquals(1, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP));
					Assert.assertEquals(1, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2));
					Assert.assertEquals(1, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE));

					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByGroup(JunitMadkit.GROUP).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2).get(0).getDifferedMessage()).getContent());
					Assert.assertEquals("ok", ((StringMessage) getDifferedMessagesBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE).get(0).getDifferedMessage()).getContent());

					requestRole(JunitMadkit.GROUP, JunitMadkit.ROLE);

					m = receiver.nextMessage();
					Assert.assertNotNull(m);
					Assert.assertEquals("ok", ((StringMessage) m).getContent());
					Assert.assertEquals(0, getDifferedMessagesNumberByGroup(JunitMadkit.GROUP));
					Assert.assertEquals(0, getDifferedMessagesNumberByReceiverRole(JunitMadkit.GROUP, JunitMadkit.ROLE2));
					Assert.assertEquals(0, getDifferedMessagesNumberBySenderRole(JunitMadkit.GROUP, JunitMadkit.ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					Assert.fail();
				}
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}
}
