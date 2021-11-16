package com.distrimind.madkit.database;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java language 

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
import com.distrimind.madkit.kernel.network.RealTimeTransferStat;
import com.distrimind.ood.database.DatabaseRecord;
import com.distrimind.ood.database.SynchronizedTransaction;
import com.distrimind.ood.database.Table;
import com.distrimind.ood.database.TransactionIsolation;
import com.distrimind.ood.database.annotations.Field;
import com.distrimind.ood.database.annotations.NotNull;
import com.distrimind.ood.database.annotations.PrimaryKey;
import com.distrimind.ood.database.annotations.Unique;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.AbstractDecentralizedIDGenerator;
import com.distrimind.util.RenforcedDecentralizedIDGenerator;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.SecureExternalizable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.distrimind.util.ReflectionTools.getMethod;
import static com.distrimind.util.ReflectionTools.invoke;
import static com.distrimind.util.ReflectionTools.loadClass;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.3.0
 */
public final class AsynchronousBigDataTable extends Table<AsynchronousBigDataTable.Record> {
	private final Map<AbstractDecentralizedIDGenerator, BigDataTransferID> transferIdsPerInternalAsynchronousId = Collections.synchronizedMap(new HashMap<>());
	@SuppressWarnings("ProtectedMemberInFinalClass")
	protected AsynchronousBigDataTable() throws DatabaseException {
	}

	public static class Record extends DatabaseRecord{
		@PrimaryKey
		private AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier;

		@Field(index = true, limit = AsynchronousMessageTable.MAX_PATH_LENGTH)
		@NotNull
		private String groupPath;

		@Field(index = true, limit=Group.MAX_PATH_LENGTH)
		@NotNull
		private String roleSender;

		@Field(index = true, limit = Group.MAX_PATH_LENGTH)
		@NotNull
		private String roleReceiver;



		@NotNull
		@Field(limit = AsynchronousBigDataIdentifier.MAX_ASYNCHRONOUS_BIG_DATA_IDENTIFIER_SIZE_IN_BYTES)
		@Unique
		private AsynchronousBigDataIdentifier asynchronousBigDataIdentifier;

		@Field(limit = AsynchronousMessageTable.MAX_ASYNCHRONOUS_MESSAGE_LENGTH)
		private SecureExternalizable attachedData;

		@Field
		private long currentStreamPosition;

		@Field
		private MessageDigestType messageDigestType;

		@Field
		private boolean excludedFromEncryption;

		@Field
		private AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper;

		@Field
		private AsynchronousBigDataToReceiveWrapper asynchronousBigDataToReceiveWrapper;

		@Field
		private long timeOutInMs;

		@Field
		private long lastTimeUpdateUTCInMs;

		@Field
		private boolean transferStarted;

		@SuppressWarnings("unused")
		private Record() {
		}

		private Record(AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
					   String groupPath, String roleSender, String roleReceiver,
					  AsynchronousBigDataIdentifier asynchronousBigDataIdentifier,
					  SecureExternalizable attachedData,
					  MessageDigestType messageDigestType, boolean excludedFromEncryption,
					   long timeOutInMs) {
			if (groupPath==null)
				throw new NullPointerException();
			if (roleSender==null)
				throw new NullPointerException();
			if (roleReceiver==null)
				throw new NullPointerException();
			if (asynchronousBigDataIdentifier ==null)
				throw new NullPointerException();
			if (asynchronousBigDataInternalIdentifier ==null)
				throw new NullPointerException();
			this.groupPath=groupPath;
			this.roleSender=roleSender;
			this.roleReceiver=roleReceiver;
			this.asynchronousBigDataInternalIdentifier = asynchronousBigDataInternalIdentifier;
			this.asynchronousBigDataIdentifier = asynchronousBigDataIdentifier;
			this.attachedData = attachedData;
			this.messageDigestType = messageDigestType;
			this.excludedFromEncryption = excludedFromEncryption;
			this.asynchronousBigDataToSendWrapper = null;
			this.asynchronousBigDataToReceiveWrapper = null;
			this.currentStreamPosition=0;
			this.timeOutInMs=timeOutInMs;
			this.lastTimeUpdateUTCInMs=System.currentTimeMillis();

		}
		public Record(AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
					  String groupPath, String roleSender, String roleReceiver,
					  AsynchronousBigDataIdentifier asynchronousBigDataIdentifier,
					  SecureExternalizable attachedData,
					  MessageDigestType messageDigestType, boolean excludedFromEncryption,long timeOutInMs,
					  AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper
					  ) {
			this(asynchronousBigDataInternalIdentifier, groupPath, roleSender, roleReceiver, asynchronousBigDataIdentifier,
					attachedData, messageDigestType, excludedFromEncryption, timeOutInMs);
			if (asynchronousBigDataToSendWrapper ==null)
				throw new NullPointerException();
			this.asynchronousBigDataToSendWrapper = asynchronousBigDataToSendWrapper;
			this.transferStarted=false;
		}
		public Record(AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
					  String groupPath, String roleSender, String roleReceiver,
					  AsynchronousBigDataIdentifier asynchronousBigDataIdentifier,
					  SecureExternalizable attachedData,
					  MessageDigestType messageDigestType, boolean excludedFromEncryption,long timeOutInMs,
					  AsynchronousBigDataToReceiveWrapper asynchronousBigDataToReceiveWrapper
					  ) {
			this(asynchronousBigDataInternalIdentifier, groupPath, roleSender, roleReceiver, asynchronousBigDataIdentifier,
					attachedData, messageDigestType, excludedFromEncryption, timeOutInMs);
			if (asynchronousBigDataToReceiveWrapper ==null)
				throw new NullPointerException();
			this.asynchronousBigDataToReceiveWrapper = asynchronousBigDataToReceiveWrapper;
			this.transferStarted=true;
		}

		public AbstractDecentralizedIDGenerator getAsynchronousBigDataInternalIdentifier() {
			return asynchronousBigDataInternalIdentifier;
		}

		public AsynchronousBigDataIdentifier getAsynchronousBigDataIdentifier() {
			return asynchronousBigDataIdentifier;
		}

		public SecureExternalizable getAttachedData() {
			return attachedData;
		}

		public long getCurrentStreamPosition() {
			return currentStreamPosition;
		}

		public MessageDigestType getMessageDigestType() {
			return messageDigestType;
		}

		public boolean isExcludedFromEncryption() {
			return excludedFromEncryption;
		}

		public AsynchronousBigDataToSendWrapper getAsynchronousBigDataToSendWrapper() {
			return asynchronousBigDataToSendWrapper;
		}

		public AsynchronousBigDataToReceiveWrapper getAsynchronousBigDataToReceiveWrapper() {
			return asynchronousBigDataToReceiveWrapper;
		}

		public String getGroupPath() {
			return groupPath;
		}

		public String getRoleSender() {
			return roleSender;
		}

		public String getRoleReceiver() {
			return roleReceiver;
		}

		public long getTimeOutInMs() {
			return timeOutInMs;
		}

		public long getLastTimeUpdateUTCInMs() {
			return lastTimeUpdateUTCInMs;
		}

		public boolean isTransferStarted() {
			return transferStarted;
		}
	}
	private Record startAsynchronousBigDataTransfer(Group group, String roleSender, String roleReceiver,
													AsynchronousBigDataIdentifier asynchronousBigDataIdentifier,
													SecureExternalizable attachedData,
													MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
													AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper
	)
	{
		return startAsynchronousBigDataTransfer(group, roleSender, roleReceiver, asynchronousBigDataIdentifier, attachedData, messageDigestType, excludedFromEncryption, timeOutInMs,
				asynchronousBigDataToSendWrapper, null, new RenforcedDecentralizedIDGenerator(false, true));
	}
	public Record startAsynchronousBigDataTransfer(Group group, String roleSender, String roleReceiver,
												   AsynchronousBigDataIdentifier asynchronousBigDataIdentifier,
												   MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
												   AsynchronousBigDataToReceiveWrapper asynchronousBigDataToReceiveWrapper,
												   AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier
	)
	{
		return startAsynchronousBigDataTransfer(group, roleSender, roleReceiver, asynchronousBigDataIdentifier, null, messageDigestType, excludedFromEncryption, timeOutInMs,
				null, asynchronousBigDataToReceiveWrapper, asynchronousBigDataInternalIdentifier);
	}
	private Record startAsynchronousBigDataTransfer(Group group, String roleSender, String roleReceiver,
													AsynchronousBigDataIdentifier asynchronousBigDataIdentifier,
													SecureExternalizable attachedData,
													MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
													AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper,
													AsynchronousBigDataToReceiveWrapper asynchronousBigDataToReceiveWrapper,
													AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier
									  ) {
		assert asynchronousBigDataToSendWrapper ==null || asynchronousBigDataToReceiveWrapper ==null;
		assert asynchronousBigDataInternalIdentifier!=null;
		try {

			return getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Record>() {
				@Override
				public Record run() throws Exception {
					Record r = getRecords("asynchronousBigDataIdentifier", asynchronousBigDataIdentifier).stream().findAny().orElse(null);
					if (asynchronousBigDataToSendWrapper !=null) {
						if (r != null)
							return null;
						r = addRecord(new Record(asynchronousBigDataInternalIdentifier,
								group.toString(), roleSender, roleReceiver, asynchronousBigDataIdentifier,
								attachedData, messageDigestType, excludedFromEncryption, timeOutInMs, asynchronousBigDataToSendWrapper));
					}
					else {
						if (r==null) {
							r = addRecord(new Record(asynchronousBigDataInternalIdentifier,
									group.toString(), roleSender, roleReceiver, asynchronousBigDataIdentifier,
									null, messageDigestType, excludedFromEncryption, timeOutInMs, asynchronousBigDataToReceiveWrapper));
						}
						else
						{
							if (r.getAsynchronousBigDataToReceiveWrapper()!=null)
								return null;
							if (!r.getAsynchronousBigDataInternalIdentifier().equals(asynchronousBigDataInternalIdentifier))
								return null;
							updateRecord(r, "asynchronousBigDataToReceiveWrapper", asynchronousBigDataToReceiveWrapper, "attachedData", null);
						}
					}
					return r;
				}

				@Override
				public TransactionIsolation getTransactionIsolation() {
					return TransactionIsolation.TRANSACTION_REPEATABLE_READ;
				}

				@Override
				public boolean doesWriteData() {
					return true;
				}

				@Override
				public void initOrReset() {

				}
			});
		}
		catch (DatabaseException e)
		{
			e.printStackTrace();
			return null;
		}

	}

	public Record startAsynchronousBigDataTransfer(Collection<String> baseGroupPath, final AbstractAgent requester, final Group group, String roleSender, String roleReceiver,
												   AsynchronousBigDataIdentifier asynchronousBigDataIdentifier,
												   SecureExternalizable attachedData,
												   MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
												   AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper
	) {
		final String groupPath=group.getPath();
		if (!AsynchronousMessageTable.isConcerned(baseGroupPath, groupPath))
			return null;

		if (!requester.hasGroup(group))
			return null;
		AgentAddress senderAA=requester.getAgentAddressIn(group, roleSender);
		if (senderAA==null)
			return null;

		Record r= startAsynchronousBigDataTransfer(group, roleSender, roleReceiver, asynchronousBigDataIdentifier, attachedData, messageDigestType, excludedFromEncryption, timeOutInMs,
				asynchronousBigDataToSendWrapper);
		if (r!=null)
		{
			AgentAddress receiverAA=requester.getAgentWithRole(group, roleReceiver);
			if (receiverAA!=null)
			{
				BigDataTransferID tid= sendAsynchronousBigData(requester, senderAA, receiverAA, r);
				if (tid==null)
					return null;
				else
					transferIdsPerInternalAsynchronousId.put(r.getAsynchronousBigDataInternalIdentifier(), tid);
			}
		}
		return r;

	}

	public void cancelTransfer(AbstractAgent requester, Record record)
	{
		Group group;
		try {
			group = Group.parseGroup(record.getGroupPath());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		BigDataTransferID bigDataTransferID= transferIdsPerInternalAsynchronousId.remove(record.getAsynchronousBigDataInternalIdentifier());
		if (bigDataTransferID==null && record.isTransferStarted())
		{
			sendMessageAndDifferItIfNecessary(requester, group, record.getRoleReceiver(), new CancelAsynchronousBigDataTransferMessage(record.getAsynchronousBigDataInternalIdentifier()), record.getRoleSender());
		}
		try {
			removeRecord(record);
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		cancelBigDataTransfer(requester, bigDataTransferID);
	}
	public RealTimeTransferStat getBytePerSecondsStat(AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier)
	{
		BigDataTransferID b= transferIdsPerInternalAsynchronousId.get(asynchronousBigDataInternalIdentifier);
		if (b==null)
			return null;
		else
			return b.getBytePerSecondsStat();
	}

	private static final Method m_send_asynchronous_big_data;
	private static final Method m_get_madkit_kernel;
	private static final Method m_send_message_and_differ_it_if_necessary;
	private static final Method m_cancel_big_data_transfer;
	static
	{
		Class<?> madkitKernelClass= loadClass("com.distrimind.madkit.kernel.MadkitKernel");
		Class<?> abstractAgentClass= loadClass("com.distrimind.madkit.kernel.AbstractAgent");
		if (madkitKernelClass==null || abstractAgentClass==null) {
			m_send_asynchronous_big_data = null;
			m_get_madkit_kernel=null;
			m_send_message_and_differ_it_if_necessary=null;
			m_cancel_big_data_transfer=null;
			System.exit(-1);
		}
		else {
			m_send_asynchronous_big_data = getMethod(madkitKernelClass, "sendAsynchronousBigData", AbstractAgent.class,
					AgentAddress.class, AgentAddress.class, Record.class);
			m_send_message_and_differ_it_if_necessary = getMethod(madkitKernelClass, "sendMessageAndDifferItIfNecessary", AbstractAgent.class,
					Group.class, String.class, Message.class, String.class);
			m_cancel_big_data_transfer = getMethod(madkitKernelClass, "cancelBigDataTransfer", AbstractAgent.class,
					BigDataTransferID.class);
			m_get_madkit_kernel = getMethod(abstractAgentClass, "getMadkitKernel");
		}
	}
	Agent getMadkitKernel(AbstractAgent abstractAgent)
	{
		try {
			return (Agent)invoke(m_get_madkit_kernel, abstractAgent);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}
	BigDataTransferID sendAsynchronousBigData(AbstractAgent requester, AgentAddress senderAA, AgentAddress receiverAA,
											  AsynchronousBigDataTable.Record record)
	{
		try {
			return (BigDataTransferID)invoke(m_send_asynchronous_big_data, getMadkitKernel(requester),requester, senderAA, receiverAA, requester, record);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}

	}
	void sendMessageAndDifferItIfNecessary(final AbstractAgent requester, Group group, final String role,
															   final Message message, final String senderRole)  {
		try {
			invoke(m_send_message_and_differ_it_if_necessary, getMadkitKernel(requester), requester, group, role, message, senderRole);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

	void cancelBigDataTransfer(AbstractAgent requester, BigDataTransferID bigDataTransferID)
	{
		try {
			invoke(m_cancel_big_data_transfer, getMadkitKernel(requester), requester, bigDataTransferID);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}

}
