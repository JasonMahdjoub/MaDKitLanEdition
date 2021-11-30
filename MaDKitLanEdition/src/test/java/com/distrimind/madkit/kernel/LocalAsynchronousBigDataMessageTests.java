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

import com.distrimind.ood.database.EmbeddedH2DatabaseWrapper;
import com.distrimind.ood.database.InMemoryEmbeddedH2DatabaseFactory;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.io.*;
import org.testng.AssertJUnit;
import org.testng.annotations.*;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.11
 */
public class LocalAsynchronousBigDataMessageTests extends TestNGMadkit {
	static class BigDataIdentifier implements ExternalAsynchronousBigDataIdentifier
	{
		boolean shortData;
		private int number=counter.getAndIncrement();
		private static final AtomicInteger counter=new AtomicInteger(0);

		@SuppressWarnings("unused")
		public BigDataIdentifier() {
		}

		public BigDataIdentifier(boolean shortData) {
			this.shortData = shortData;
		}

		BigDataIdentifier getNewIdentifier()
		{
			return new BigDataIdentifier(shortData);
		}
		@Override
		public int getInternalSerializedSize() {
			return 5;
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {
			out.writeBoolean(shortData);
			out.writeInt(number);
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			shortData=in.readBoolean();
			number=in.readInt();
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			BigDataIdentifier that = (BigDataIdentifier) o;
			return shortData == that.shortData && number == that.number;
		}

		@Override
		public int hashCode() {
			return Objects.hash(shortData, number);
		}

		@Override
		public String toString() {
			return "BigDataIdentifier{" +
					"shortData=" + shortData +
					", number=" + number +
					'}';
		}
	}
	static class RandomInputStreamWrapper implements AsynchronousBigDataToSendWrapper {
		private BigDataIdentifier identifier;

		public RandomInputStreamWrapper(BigDataIdentifier identifier) {
			if (identifier==null)
				throw new NullPointerException();
			this.identifier = identifier;
		}

		@SuppressWarnings("unused")
		public RandomInputStreamWrapper() {
		}

		@Override
		public RandomInputStream getRandomInputStream(ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier) {
			return new RandomByteArrayInputStream(identifier.shortData?getShortData():getLargeData());
		}

		@Override
		public int getInternalSerializedSize() {
			return SerializationTools.getInternalSize(identifier);
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {
			out.writeObject(identifier, false);
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			identifier=in.readObject(false);
		}
	}
	static class RandomOutputStreamWrapper implements AsynchronousBigDataToReceiveWrapper
	{
		private BigDataIdentifier identifier;

		public RandomOutputStreamWrapper(BigDataIdentifier identifier) {
			if (identifier==null)
				throw new NullPointerException();
			this.identifier = identifier;
		}

		@SuppressWarnings("unused")
		public RandomOutputStreamWrapper() {
		}
		@Override
		public RandomOutputStream getRandomOutputStream(ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier) {
			return identifier.shortData?receivedShortDataStream:receivedLargeDataStream;
		}

		@Override
		public int getInternalSerializedSize() {
			return SerializationTools.getInternalSize(identifier);
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {
			out.writeObject(identifier, false);
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			identifier=in.readObject(false);
		}
	}
	private static byte[] shortData=null, largeData=null;
	private static final RandomByteArrayOutputStream receivedShortDataStream=new RandomByteArrayOutputStream();
	private static final RandomByteArrayOutputStream receivedLargeDataStream =new RandomByteArrayOutputStream();

	@BeforeTest
	public void initReception() throws IOException {
		receivedShortDataStream.setLength(0);
		receivedLargeDataStream.setLength(0);
	}

	public static byte[] getShortData() {
		if (shortData==null) {
			shortData = new byte[4096];
			Random r = new Random(System.currentTimeMillis());
			r.nextBytes(shortData);
		}
		return shortData;
	}

	public static byte[] getLargeData() {
		if (largeData==null) {
			largeData = new byte[100*1024*1024];
			Random r = new Random(System.currentTimeMillis());
			r.nextBytes(largeData);
		}
		return largeData;
	}

	private static final File databaseFile=new File("asynchronousDatabaseFile");
	private static final MadkitEventListener madkitEventListener= properties -> {
		try {
			properties.setDatabaseFactory(new InMemoryEmbeddedH2DatabaseFactory(databaseFile.getName()));
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	};

	static class AttachedData implements SecureExternalizable
	{
		private String message;

		public AttachedData(String message) {
			if (message==null)
				throw new NullPointerException();
			this.message = message;
		}

		@SuppressWarnings("unused")
		public AttachedData() {
		}

		public String getMessage() {
			return message;
		}


		@Override
		public int getInternalSerializedSize() {
			return SerializationTools.getInternalSize(message, 1000);
		}

		@Override
		public void writeExternal(SecuredObjectOutputStream out) throws IOException {
			out.writeString(message, false, 1000);
		}

		@Override
		public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
			message=in.readString(false, 1000);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			AttachedData that = (AttachedData) o;
			return message.equals(that.message);
		}

		@Override
		public int hashCode() {
			return Objects.hash(message);
		}

		@Override
		public String toString() {
			return "AttachedData{" +
					"message='" + message + '\'' +
					'}';
		}
	}



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

	@DataProvider
	Object[][] provideDataIdentifier()
	{
		return new Object[][]{
				{new BigDataIdentifier(false), null},
				{new BigDataIdentifier(true), null},
				{new BigDataIdentifier(false), new AttachedData("ok")},
				{new BigDataIdentifier(true), new AttachedData("ok")}};
	}

	@Test(dataProvider = "provideDataIdentifier")
	public void testAsynchroneMessagesWhenNoDatabaseWasLoaded(BigDataIdentifier dataIdentifier, AttachedData attachedData)
	{
		launchTest(new AbstractAgent(){
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(GROUP, ROLE);
				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(GROUP, ROLE2);
					}
				};
				launchAgent(receiver);
				AsynchronousBigDataTransferID transferID=sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
				AssertJUnit.assertNull(transferID);
				transferID=sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
				AssertJUnit.assertNull(transferID);
				transferID=sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE2);
				AssertJUnit.assertNull(transferID);
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false);
	}
	@Test(dataProvider = "provideDataIdentifier")
	public void testNullPointerExceptionsAndIllegalArgumentsException(BigDataIdentifier dataIdentifier, AttachedData attachedData)
	{

		launchTest(new AbstractAgent(){
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(GROUP, ROLE);
				AbstractAgent receiver=new AbstractAgent(){
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(GROUP, ROLE2);
					}
				};
				launchAgent(receiver);
				try {
					sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					AssertJUnit.fail();
				}
				catch(IllegalArgumentException ignored)
				{
				}

				try {
					sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(null, ROLE2, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, null, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, null, new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, dataIdentifier.getNewIdentifier(), null, attachedData, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesBySenderRole(null, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesByReceiverRole(null, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesNumberBySenderRole(null, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(null, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					getCurrentAsynchronousBigDataMessagesNumberByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousBigDataMessagesBySenderRole(null, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousBigDataMessagesByReceiverRole(null, ROLE);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}
				try {
					cancelCurrentAsynchronousBigDataMessagesByGroup(null);
					AssertJUnit.fail();
				}
				catch(NullPointerException ignored)
				{
				}

			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);

	}
	private void testReceptionOK(BigDataIdentifier dataIdentifier, AttachedData attachedData, AbstractAgent sender, AbstractAgent receiver, AsynchronousBigDataTransferID transferID) throws InterruptedException, IllegalAccessException, IOException {
		AssertJUnit.assertNotNull(transferID);
		receiver.sleep(400);
		Message m = receiver.nextMessage();
		AssertJUnit.assertNotNull(m);
		BigDataPropositionMessage bdpm=(BigDataPropositionMessage)m;
		AssertJUnit.assertEquals(attachedData, bdpm.getAttachedData());
		AssertJUnit.assertEquals(dataIdentifier, bdpm.getExternalAsynchronousBigDataIdentifier());
		RandomOutputStreamWrapper outputStreamWrapper=new RandomOutputStreamWrapper((BigDataIdentifier) bdpm.getExternalAsynchronousBigDataIdentifier());
		bdpm.acceptTransfer(outputStreamWrapper);
		receiver.sleep(1000);
		m = receiver.nextMessage();
		AssertJUnit.assertNotNull(m);
		BigDataResultMessage bdrm=(BigDataResultMessage)m;
		AssertJUnit.assertEquals(BigDataResultMessage.Type.BIG_DATA_TRANSFERRED, bdrm.getType());
		AssertJUnit.assertEquals(bdrm.getTransferredDataLength(), dataIdentifier.shortData?getShortData().length:getLargeData().length);
		AssertJUnit.assertEquals(dataIdentifier, bdrm.getExternalAsynchronousBigDataIdentifier());
		AssertJUnit.assertEquals(dataIdentifier, outputStreamWrapper.identifier);
		AssertJUnit.assertEquals(dataIdentifier.shortData?receivedShortDataStream:receivedLargeDataStream, outputStreamWrapper.getRandomOutputStream(outputStreamWrapper.identifier));
		AssertJUnit.assertEquals(dataIdentifier.shortData?getShortData().length:getLargeData().length, outputStreamWrapper.getRandomOutputStream(outputStreamWrapper.identifier).length());
		RandomInputStream ris=outputStreamWrapper.getRandomOutputStream(outputStreamWrapper.identifier).getRandomInputStream();
		ris.seek(0);
		AssertJUnit.assertEquals(dataIdentifier.shortData?getShortData():getLargeData(), ris.readAllBytes());
		m=sender.nextMessage();
		bdrm=(BigDataResultMessage)m;
		AssertJUnit.assertEquals(BigDataResultMessage.Type.BIG_DATA_TRANSFERRED, bdrm.getType());
		AssertJUnit.assertEquals(dataIdentifier.shortData?getShortData().length:getLargeData().length, bdrm.getTransferredDataLength());
		AssertJUnit.assertEquals(dataIdentifier, bdrm.getExternalAsynchronousBigDataIdentifier());
	}
	private void testReceptionNotOK(AbstractAgent receiver, AsynchronousBigDataTransferID transferID) throws InterruptedException {
		AssertJUnit.assertNull(transferID);
		receiver.sleep(400);
		Message m = receiver.nextMessage();
		AssertJUnit.assertNull(m);
	}
	private void testReceptionDiffered(AbstractAgent receiver, AsynchronousBigDataTransferID transferID) throws InterruptedException {
		AssertJUnit.assertNotNull(transferID);
		receiver.sleep(400);
		Message m = receiver.nextMessage();
		AssertJUnit.assertNull(m);
	}

	@Test(dataProvider = "provideDataIdentifier")
	public void testSynchronousMessage(BigDataIdentifier dataIdentifier, AttachedData attachedData)
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(GROUP, ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(GROUP, ROLE2);
					}
				};
				launchAgent(receiver);
				try {

					BigDataIdentifier id=dataIdentifier.getNewIdentifier();
					AsynchronousBigDataTransferID transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, id, new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionOK(id, attachedData, this,receiver, transferID);

					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE2);
					testReceptionNotOK(receiver, transferID);
					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE2, dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionNotOK(receiver, transferID);
					requestRole(GROUPB, ROLE);
					requestRole(GROUPB, ROLE2);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE, id=dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE2);
					testReceptionDiffered(receiver, transferID);
					BigDataIdentifier id2;
					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE2, id2=dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionDiffered(receiver, transferID);


					AssertJUnit.assertEquals(2, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE2));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(1).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE2).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(1).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());

					cancelCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE2);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE2));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());

					cancelCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterAsynchronousMessages = Collections.singletonList(GROUPB.getPath());
					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE, id=dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE2);
					testReceptionDiffered(receiver, transferID);

					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE2, id2=dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionDiffered(receiver, transferID);
					AssertJUnit.assertEquals(2, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE2));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(1).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE2).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(1).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());

					cancelCurrentAsynchronousBigDataMessagesByGroup(GROUPB);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					getMadkitConfig().rootOfPathGroupUsedToFilterAsynchronousMessages = Collections.singletonList(GROUPB.getSubGroup("awesome sub group").getPath());
					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE, dataIdentifier, new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionNotOK(receiver, transferID);

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}

	@Test(dataProvider = "provideDataIdentifier")
	public void testSynchronousMessageWithTimeOut(BigDataIdentifier dataIdentifier, AttachedData attachedData)
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(GROUP, ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();
						requestRole(GROUP, ROLE2);
					}
				};
				launchAgent(receiver);
				try {


					AsynchronousBigDataTransferID transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, dataIdentifier, new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionOK(dataIdentifier, attachedData, this,receiver, transferID);

					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, dataIdentifier, new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE2);
					testReceptionNotOK(receiver, transferID);
					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE2, dataIdentifier, new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionNotOK(receiver, transferID);
					requestRole(GROUPB, ROLE);
					requestRole(GROUPB, ROLE2);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

					BigDataIdentifier id, id2;
					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE, id=dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE2);
					testReceptionDiffered(receiver, transferID);
					transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUPB, ROLE2, id2=dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE, 1000);
					testReceptionDiffered(receiver, transferID);


					AssertJUnit.assertEquals(2, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE2));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(1).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE2).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(1).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUPB, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id2, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());

					sleep(2000);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE2));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());

					cleanObsoleteMaDKitDataNow();

					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE2));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUPB).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());

					cancelCurrentAsynchronousBigDataMessagesBySenderRole(GROUPB, ROLE2);
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUPB));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUPB, ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUPB, ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}

	@Test(dataProvider = "provideDataIdentifier")
	public void testASynchronousMessageWithSenderAvailable(BigDataIdentifier dataIdentifier, AttachedData attachedData)
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(GROUP, ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				launchAgent(receiver);
				try {

					BigDataIdentifier id;
					AsynchronousBigDataTransferID transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, id=dataIdentifier.getNewIdentifier(), new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionDiffered(receiver, transferID);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUP).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, ROLE2).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, ROLE).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUP).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());

					ReturnCode rc=receiver.requestRole(GROUP, ROLE2);
					AssertJUnit.assertEquals(ReturnCode.SUCCESS, rc);

					testReceptionOK(id, attachedData, this, receiver, transferID);

					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);

	}
	@Test(dataProvider = "provideDataIdentifier")
	public void testASynchronousMessageWithSenderNotAvailable(BigDataIdentifier dataIdentifier, AttachedData attachedData)
	{
		launchTest(new AbstractAgent() {
			@Override
			protected void activate() throws InterruptedException {
				super.activate();
				requestRole(GROUP, ROLE);
				AbstractAgent receiver = new AbstractAgent() {
					@Override
					protected void activate() throws InterruptedException {
						super.activate();

					}
				};
				launchAgent(receiver);
				try {

					BigDataIdentifier id;
					AsynchronousBigDataTransferID transferID = sendBigDataWithRoleOrDifferSendingUntilRecipientWasFound(GROUP, ROLE2, id=dataIdentifier, new RandomInputStreamWrapper(dataIdentifier), attachedData, ROLE);
					testReceptionDiffered(receiver, transferID);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUP).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, ROLE2).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, ROLE).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUP).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());

					leaveRole(GROUP, ROLE);
					testReceptionDiffered(receiver, transferID);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUP).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, ROLE2).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, ROLE).get(0).getAttachedData());

					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByGroup(GROUP).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, ROLE2).get(0).getExternalAsynchronousBigDataIdentifier());
					AssertJUnit.assertEquals(id, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, ROLE).get(0).getExternalAsynchronousBigDataIdentifier());
					receiver.requestRole(GROUP, ROLE2);
					testReceptionDiffered(receiver, transferID);
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE2));
					AssertJUnit.assertEquals(1, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByGroup(GROUP).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesByReceiverRole(GROUP, ROLE2).get(0).getAttachedData());
					AssertJUnit.assertEquals(attachedData, getCurrentAsynchronousBigDataMessagesBySenderRole(GROUP, ROLE).get(0).getAttachedData());

					requestRole(GROUP, ROLE);
					testReceptionOK(id, attachedData, this,receiver, transferID);

					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByGroup(GROUP));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberByReceiverRole(GROUP, ROLE2));
					AssertJUnit.assertEquals(0, getCurrentAsynchronousBigDataMessagesNumberBySenderRole(GROUP, ROLE));

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
			}
		}, AbstractAgent.ReturnCode.SUCCESS, false, madkitEventListener);
	}
}
