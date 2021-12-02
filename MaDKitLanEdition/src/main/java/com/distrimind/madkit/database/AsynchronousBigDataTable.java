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
import com.distrimind.ood.database.*;
import com.distrimind.ood.database.annotations.Field;
import com.distrimind.ood.database.annotations.NotNull;
import com.distrimind.ood.database.annotations.PrimaryKey;
import com.distrimind.ood.database.annotations.Unique;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.AbstractDecentralizedIDGenerator;
import com.distrimind.util.Reference;
import com.distrimind.util.RenforcedDecentralizedIDGenerator;
import com.distrimind.util.concurrent.ScheduledPoolExecutor;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.SecureExternalizable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

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
		@Field(limit = ExternalAsynchronousBigDataIdentifier.MAX_ASYNCHRONOUS_BIG_DATA_IDENTIFIER_SIZE_IN_BYTES)
		@Unique
		private ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier;

		@Field(limit = AsynchronousMessageTable.MAX_ASYNCHRONOUS_MESSAGE_LENGTH)
		private SecureExternalizable attachedData;

		@Field
		private long currentStreamPosition;

		@SuppressWarnings("FieldMayBeFinal")
		@Field
		private boolean currentPositionNeedConfirmationFromReceiver=false;

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
					  ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
					  SecureExternalizable attachedData,
					  MessageDigestType messageDigestType, boolean excludedFromEncryption,
					   long timeOutInMs) {
			if (groupPath==null)
				throw new NullPointerException();
			if (roleSender==null)
				throw new NullPointerException();
			if (roleReceiver==null)
				throw new NullPointerException();
			if (externalAsynchronousBigDataIdentifier ==null)
				throw new NullPointerException();
			if (asynchronousBigDataInternalIdentifier ==null)
				throw new NullPointerException();
			if (roleReceiver.equals(roleSender))
				throw new IllegalArgumentException();
			if (timeOutInMs<1)
				throw new IllegalArgumentException();
			this.groupPath=groupPath;
			this.roleSender=roleSender;
			this.roleReceiver=roleReceiver;
			this.asynchronousBigDataInternalIdentifier = asynchronousBigDataInternalIdentifier;
			this.externalAsynchronousBigDataIdentifier = externalAsynchronousBigDataIdentifier;
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
					  ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
					  SecureExternalizable attachedData,
					  MessageDigestType messageDigestType, boolean excludedFromEncryption,long timeOutInMs,
					  AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper
					  ) {
			this(asynchronousBigDataInternalIdentifier, groupPath, roleSender, roleReceiver, externalAsynchronousBigDataIdentifier,
					attachedData, messageDigestType, excludedFromEncryption, timeOutInMs);
			if (asynchronousBigDataToSendWrapper ==null)
				throw new NullPointerException();
			this.asynchronousBigDataToSendWrapper = asynchronousBigDataToSendWrapper;
			this.transferStarted=false;
		}

		public boolean isObsolete()
		{
			return this.timeOutInMs+this.lastTimeUpdateUTCInMs<System.currentTimeMillis();
		}

		public boolean isCurrentPositionNeedConfirmationFromReceiver() {
			return currentPositionNeedConfirmationFromReceiver;
		}

		public Record(AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
					  String groupPath, String roleSender, String roleReceiver,
					  ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
					  SecureExternalizable attachedData,
					  MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
					  AsynchronousBigDataToReceiveWrapper asynchronousBigDataToReceiveWrapper
					  ) {
			this(asynchronousBigDataInternalIdentifier, groupPath, roleSender, roleReceiver, externalAsynchronousBigDataIdentifier,
					attachedData, messageDigestType, excludedFromEncryption, timeOutInMs);
			if (asynchronousBigDataToReceiveWrapper ==null)
				throw new NullPointerException();
			this.asynchronousBigDataToReceiveWrapper = asynchronousBigDataToReceiveWrapper;
			this.transferStarted=true;
		}

		public AbstractDecentralizedIDGenerator getAsynchronousBigDataInternalIdentifier() {
			return asynchronousBigDataInternalIdentifier;
		}

		public ExternalAsynchronousBigDataIdentifier getExternalAsynchronousBigDataIdentifier() {
			return externalAsynchronousBigDataIdentifier;
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
													ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
													SecureExternalizable attachedData,
													MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
													AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper
	)
	{
		return startAsynchronousBigDataTransfer(group, roleSender, roleReceiver, externalAsynchronousBigDataIdentifier, attachedData, messageDigestType, excludedFromEncryption, timeOutInMs,
				asynchronousBigDataToSendWrapper, null, new RenforcedDecentralizedIDGenerator(false, true));
	}
	public Record startAsynchronousBigDataTransfer(Group group, String roleSender, String roleReceiver,
												   ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
												   MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
												   AsynchronousBigDataToReceiveWrapper asynchronousBigDataToReceiveWrapper,
												   AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
												   BigDataTransferID bigDataTransferID
	)
	{
		Record r= startAsynchronousBigDataTransfer(group, roleSender, roleReceiver, externalAsynchronousBigDataIdentifier, null, messageDigestType, excludedFromEncryption, timeOutInMs,
				null, asynchronousBigDataToReceiveWrapper, asynchronousBigDataInternalIdentifier);
		if (r!=null)
		{
			transferIdsPerInternalAsynchronousId.put(asynchronousBigDataInternalIdentifier, bigDataTransferID);
		}
		return r;
	}
	private Record startAsynchronousBigDataTransfer(Group group, String roleSender, String roleReceiver,
													ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
													SecureExternalizable attachedData,
													MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
													AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper,
													AsynchronousBigDataToReceiveWrapper asynchronousBigDataToReceiveWrapper,
													AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier
									  ) {
		if (externalAsynchronousBigDataIdentifier==null)
			throw new NullPointerException();
		if (asynchronousBigDataInternalIdentifier==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		if (roleSender==null)
			throw new NullPointerException();
		if (roleReceiver==null)
			throw new NullPointerException();

		assert asynchronousBigDataToSendWrapper ==null || asynchronousBigDataToReceiveWrapper ==null;
		try {

			return getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Record>() {
				@Override
				public Record run() throws Exception {
					Record r = getRecordsWithAllFields("externalAsynchronousBigDataIdentifier", externalAsynchronousBigDataIdentifier).stream().findAny().orElse(null);
					if (asynchronousBigDataToSendWrapper !=null) {
						if (r != null)
							return null;
						r = addRecord(new Record(asynchronousBigDataInternalIdentifier,
								group.toString(), roleSender, roleReceiver, externalAsynchronousBigDataIdentifier,
								attachedData, messageDigestType, excludedFromEncryption, timeOutInMs, asynchronousBigDataToSendWrapper));
					}
					else {
						if (r==null) {
							r = addRecord(new Record(asynchronousBigDataInternalIdentifier,
									group.toString(), roleSender, roleReceiver, externalAsynchronousBigDataIdentifier,
									null, messageDigestType, excludedFromEncryption, timeOutInMs, asynchronousBigDataToReceiveWrapper));
						}
						else
						{
							if (r.getAsynchronousBigDataToReceiveWrapper()!=null)
								return null;
							if (!r.getAsynchronousBigDataInternalIdentifier().equals(asynchronousBigDataInternalIdentifier))
								return null;
							updateRecord(r, "asynchronousBigDataToReceiveWrapper", asynchronousBigDataToReceiveWrapper, "attachedData", null/*, "lastTimeUpdateUTCInMs", System.currentTimeMillis()*/);
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
			if (e.getCause() instanceof IllegalArgumentException)
				throw (IllegalArgumentException)e.getCause();
			else if (e.getCause() instanceof NullPointerException)
				throw (NullPointerException)e.getCause();
			e.printStackTrace();
			return null;
		}

	}
	public void setAsynchronousTransferAsStarted(AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier)
	{
		try {
			getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Void>() {
				@Override
				public Void run() throws Exception {
					Record r = getRecord("asynchronousBigDataInternalIdentifier", asynchronousBigDataInternalIdentifier);
					if (r != null && !r.isTransferStarted())
						updateRecord(r, "transferStarted", true);
					return null;
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
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	public boolean receivedPotentialAsynchronousBigDataResultMessage(AbstractAgent requester, BigDataResultMessage.Type resultType,
																	 AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
																	 long transferedDataLength, ScheduledPoolExecutor serviceExecutor) throws DatabaseException {
		AtomicReference<Record> record=new AtomicReference<>(null);
		boolean r=getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Boolean>() {
			@Override
			public Boolean run() throws Exception {
				if (resultType== BigDataResultMessage.Type.CONNECTION_LOST) {
					Record r = getRecord("asynchronousBigDataInternalIdentifier", asynchronousBigDataInternalIdentifier);
					if (r != null) {
						record.set(r);
						long pos=transferedDataLength+r.getCurrentStreamPosition();
						updateRecord(r, "currentStreamPosition", pos, "currentPositionNeedConfirmationFromReceiver", true);
						return true;
					}

				}
				else {
					record.set(getRecord("asynchronousBigDataInternalIdentifier", asynchronousBigDataInternalIdentifier));
					if (record.get()!=null)
						removeRecord("asynchronousBigDataInternalIdentifier", asynchronousBigDataInternalIdentifier);
				}
				return false;
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
		BigDataTransferID btid=transferIdsPerInternalAsynchronousId.remove(asynchronousBigDataInternalIdentifier);
		if (btid!=null && record.get()!=null)
		{
			serviceExecutor.execute(() -> {
				try {
					decrementNumberOfSimultaneousAsynchronousMessagesIfNecessary(requester, record.get());
				} catch (DatabaseException e) {
					e.printStackTrace();
				}
			});

		}
		return r;
	}


	public Record startAsynchronousBigDataTransfer(Collection<String> baseGroupPath, final AbstractAgent requester, final Group group, String roleSender, String roleReceiver,
												   ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
												   SecureExternalizable attachedData,
												   MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
												   AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper
	) {
		if (!AsynchronousMessageTable.isConcerned(baseGroupPath, group))
			return null;

		if (!requester.hasGroup(group))
			return null;
		AgentAddress senderAA=requester.getAgentAddressIn(group, roleSender);
		if (senderAA==null)
			return null;

		Record r= startAsynchronousBigDataTransfer(group, roleSender, roleReceiver, externalAsynchronousBigDataIdentifier, attachedData, messageDigestType, excludedFromEncryption, timeOutInMs,
				asynchronousBigDataToSendWrapper);
		if (r!=null)
		{
			return sendAsynchronousBigData(requester, senderAA, group, roleReceiver,r, null);
		}
		else
			return null;
	}
	private long getMaxNumberOfSimultaneousAsynchronousBigDataMessagesSentToTheSameGroup(AbstractAgent requester)
	{
		return Math.max(1,requester.getMadkitConfig().networkProperties==null?1:requester.getMadkitConfig().networkProperties.maxNumberOfSimultaneousAsynchronousBigDataMessagesSentToTheSameGroup);
	}
	private Record sendAsynchronousBigData2(AbstractAgent requester, AgentAddress senderAA, AgentAddress receiverAA, Record r, Reference<Boolean> maxNumberOfSimultaneousTransfersReached)
	{
		if (incrementNumberOfSimultaneousAsynchronousMessages(receiverAA, getMaxNumberOfSimultaneousAsynchronousBigDataMessagesSentToTheSameGroup(requester))) {
			BigDataTransferID tid = sendAsynchronousBigData(requester, senderAA, receiverAA, r);
			if (tid == null)
				return null;
			else {
				/*try {
					if (r.isTransferStarted())
						updateRecord(r, "lastTimeUpdateUTCInMs", System.currentTimeMillis());
				} catch (DatabaseException e) {
					e.printStackTrace();
				}*/
				transferIdsPerInternalAsynchronousId.put(r.getAsynchronousBigDataInternalIdentifier(), tid);
			}
		}
		else if (maxNumberOfSimultaneousTransfersReached!=null)
			maxNumberOfSimultaneousTransfersReached.set(true);
		return r;
	}
	private Record sendAsynchronousBigData(AbstractAgent requester, AgentAddress senderAA, Group group, String roleReceiver, Record r, Reference<Boolean> maxNumberOfSimultaneousTransfersReached)
	{
		AbstractAgent aa=getAgent(senderAA);
		if (aa==null)
			aa=getMadkitKernel(requester);
		AgentAddress receiverAA=aa.getAgentWithRole(group, roleReceiver);
		if (receiverAA!=null)
		{
			return sendAsynchronousBigData2(requester, senderAA, receiverAA,r, maxNumberOfSimultaneousTransfersReached);
		}
		return r;
	}
	public AbstractAgent.ReturnCode cancelTransfer(AbstractAgent requester, AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier) throws DatabaseException {
		if (requester==null)
			throw new NullPointerException();
		if (asynchronousBigDataInternalIdentifier==null)
			throw new NullPointerException();
		Record r= null;
		try {
			r = getRecord("asynchronousBigDataInternalIdentifier", asynchronousBigDataInternalIdentifier);
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
		if (r==null)
			return AbstractAgent.ReturnCode.IGNORED;
		else
			return cancelTransfer(requester, r);
	}
	public AbstractAgent.ReturnCode cancelTransfer(AbstractAgent requester, Record record) throws DatabaseException {
		AbstractAgent.ReturnCode r=cancelTransferImpl(requester, record, BigDataResultMessage.Type.TRANSFER_CANCELED);
		removeRecord(record);
		return r;
	}

	public long cancelAsynchronousMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole) throws DatabaseException {
		if (requester==null)
			throw new NullPointerException();
		if (senderRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return removeRecords(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) throws DatabaseException {
				cancelTransferImpl(requester, _record, group, BigDataResultMessage.Type.TRANSFER_CANCELED);
				return true;
			}
		},"groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.toString(), "roleSender", senderRole);
	}
	public long cancelAsynchronousMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole) throws DatabaseException {
		if (requester==null)
			throw new NullPointerException();
		if (receiverRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return removeRecords(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) throws DatabaseException {
				cancelTransferImpl(requester, _record, group, BigDataResultMessage.Type.TRANSFER_CANCELED);
				return true;
			}
		}, "groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.toString(), "roleReceiver", receiverRole);
	}
	public long cancelAsynchronousMessagesByGroup(AbstractAgent requester, Group group) throws DatabaseException {
		if (requester==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return removeRecords(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) throws DatabaseException {
				cancelTransferImpl(requester, _record, group, BigDataResultMessage.Type.TRANSFER_CANCELED);
				return true;
			}
		}, "groupPath=%groupPath", "groupPath", group.toString());
	}
	private AbstractAgent.ReturnCode cancelTransferImpl(AbstractAgent requester, Record record, BigDataResultMessage.Type reason) throws DatabaseException {
		return cancelTransferImpl(requester, record,null, reason);
	}
	private AbstractAgent.ReturnCode cancelTransferImpl(AbstractAgent requester, Record record, Group group, BigDataResultMessage.Type reason) throws DatabaseException {
		try {
			if (group==null)
				group = Group.parseGroup(record.getGroupPath());

		} catch (IOException e) {
			e.printStackTrace();
			return AbstractAgent.ReturnCode.INVALID_AGENT_ADDRESS;
		}
		BigDataTransferID bigDataTransferID= transferIdsPerInternalAsynchronousId.remove(record.getAsynchronousBigDataInternalIdentifier());
		if (bigDataTransferID!=null) {
			cancelBigDataTransfer(requester, bigDataTransferID, reason);
			decrementNumberOfSimultaneousAsynchronousMessagesIfNecessary(requester, group, record);
		}
		return AbstractAgent.ReturnCode.SUCCESS;


	}
	public void cleanObsoleteData(AbstractAgent madkitKernel)
	{
		if (madkitKernel==null)
			throw new NullPointerException();
		try {
			removeRecords(new Filter<Record>() {
				@Override
				public boolean nextRecord(Record _record)  {

					if (_record.isObsolete() && !transferIdsPerInternalAsynchronousId.containsKey(_record.getAsynchronousBigDataInternalIdentifier()))
					{
						try {
							cancelTransferImpl(madkitKernel, _record, BigDataResultMessage.Type.TIME_OUT);
						} catch (DatabaseException e) {
							e.printStackTrace();
						}
						return true;
					}
					else
						return false;
				}
			});
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}
	private void decrementNumberOfSimultaneousAsynchronousMessagesIfNecessary(AbstractAgent requester, Record record) throws DatabaseException {
		try {
			Group group = Group.parseGroup(record.getGroupPath());
			decrementNumberOfSimultaneousAsynchronousMessagesIfNecessary(requester, group, record);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	private void decrementNumberOfSimultaneousAsynchronousMessagesIfNecessary(AbstractAgent requester, Group group, Record record) throws DatabaseException {
		if (record.getAsynchronousBigDataToSendWrapper()!=null) {
			AbstractAgent aa=getMadkitKernel(requester);
			AgentAddress receiverAA = aa.getAgentWithRole(group, record.getRoleReceiver());
			if (receiverAA != null) {
				decrementNumberOfSimultaneousAsynchronousMessages(receiverAA);
				getOrderedRecords(new Filter<Record>() {
					final Reference<Boolean> maxNumberOfSimultaneousTransfersReached=new Reference<>(false);
					@Override
					public boolean nextRecord(Record _record) {

						if (_record.isObsolete())
							return false;
						AbstractAgent aa2=getAgent(receiverAA);
						if (aa2==null)
							aa2=aa;
						AgentAddress senderAA=aa2.getAgentWithRole(receiverAA.getGroup(), _record.roleSender);
						if (senderAA!=null) {
							sendAsynchronousBigData(requester, senderAA, receiverAA.getGroup(), _record.roleReceiver, _record, maxNumberOfSimultaneousTransfersReached);
							if (maxNumberOfSimultaneousTransfersReached.get())
								stopTableParsing();
						}
						return false;
					}
				}, "groupPath=%gp and currentPositionNeedConfirmationFromReceiver=%cp and roleReceiver=%rr", new Object[]{"gp", record.getGroupPath(), "cp", false, "rr", record.getRoleReceiver()}, true, "lastTimeUpdateUTCInMs");
			}
			else {
				decrementNumberOfSimultaneousAsynchronousMessagesIfPossible(requester, group, record.getRoleReceiver());
			}








		}
	}

	public RealTimeTransferStat getBytePerSecondsStat(AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier)
	{
		BigDataTransferID b= transferIdsPerInternalAsynchronousId.get(asynchronousBigDataInternalIdentifier);
		if (b==null)
			return null;
		else
			return b.getBytePerSecondsStat();
	}

	public void groupRoleAvailable(AbstractAgent kernel, Collection<String> baseGroupPath, Group distantGroup, String distantRole) throws DatabaseException {
		if (!AsynchronousMessageTable.isConcerned(baseGroupPath, distantGroup))
			return ;
		String path=distantGroup.toString();
		getOrderedRecords(new Filter<Record>() {
			final Reference<Boolean> maxNumberOfSimultaneousTransfersReached=new Reference<>(false);
			@Override
			public boolean nextRecord(Record _record) {
				if (_record.isObsolete())
					return false;
				if (_record.isTransferStarted())
				{
					AgentAddress senderAA=kernel.getAgentWithRole(distantGroup, _record.roleReceiver);

					if (senderAA!=null) {
						AbstractAgent sender=getAgent(senderAA);
						if (sender!=null) {
							AbstractAgent aa=getMadkitKernel(sender);
							if (aa==null)
								aa=kernel;
							AgentAddress receiverAA = aa.getAgentWithRole(distantGroup, _record.roleSender);
							if (receiverAA!=null) {
								sender.sendMessageWithRole(receiverAA, new BigDataToRestartMessage(_record.getAsynchronousBigDataInternalIdentifier(), _record.getCurrentStreamPosition()), _record.roleReceiver);
							}
						}
					}
				}
				else if (!maxNumberOfSimultaneousTransfersReached.get())
				{
					AgentAddress senderAA=kernel.getAgentWithRole(distantGroup, _record.roleSender);
					if (senderAA!=null) {
						sendAsynchronousBigData(kernel, senderAA, distantGroup, _record.roleReceiver, _record, maxNumberOfSimultaneousTransfersReached);
					}
				}
				return false;
			}
		}, "groupPath=%gp and (roleSender=%rs or (transferStarted==%ts and roleReceiver=%rr))", new Object[]{"gp", path, "rs", distantRole, "ts", false, "rr", distantRole}, true, "lastTimeUpdateUTCInMs");

	}

	public void receiveAskingForTransferRestart(AbstractAgent requester, AgentAddress senderAA, AgentAddress receiverAA,
										 AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
										 long position) throws DatabaseException {
		Record r=getRecord("asynchronousBigDataInternalIdentifier", asynchronousBigDataInternalIdentifier);
		if (r!=null)
		{
			if (r.getCurrentStreamPosition()!=position || r.currentPositionNeedConfirmationFromReceiver)
				updateRecord(r, "currentStreamPosition", position, "currentPositionNeedConfirmationFromReceiver", false/*, "lastTimeUpdateUTCInMs", System.currentTimeMillis()*/);
			if (incrementNumberOfSimultaneousAsynchronousMessages(receiverAA, getMaxNumberOfSimultaneousAsynchronousBigDataMessagesSentToTheSameGroup(requester))) {

				BigDataTransferID bigDataTransferID=sendAsynchronousBigData(requester, senderAA, receiverAA, r);
				if (bigDataTransferID!=null)
					transferIdsPerInternalAsynchronousId.put(r.getAsynchronousBigDataInternalIdentifier(), bigDataTransferID);
			}
		}
	}

	private static final Method m_send_asynchronous_big_data;
	private static final Method m_get_madkit_kernel;
	//private static final Method m_send_message_and_differ_it_if_necessary;
	private static final Method m_cancel_big_data_transfer;
	private static final Method m_get_agent;
	private static final Method m_get_role_object, m_decrementNumberOfSimultaneousAsynchronousMessages, m_incrementNumberOfSimultaneousAsynchronousMessages;
	private static final Method m_kernel_get_role;
	static
	{
		Class<?> madkitKernelClass= loadClass("com.distrimind.madkit.kernel.MadkitKernel");
		Class<?> internalRoleClass= loadClass("com.distrimind.madkit.kernel.InternalRole");

		if (madkitKernelClass==null) {
			m_send_asynchronous_big_data = null;
			m_get_madkit_kernel=null;
			//m_send_message_and_differ_it_if_necessary=null;
			m_cancel_big_data_transfer=null;
			m_get_agent=null;
			m_get_role_object=null;
			m_decrementNumberOfSimultaneousAsynchronousMessages=null;
			m_incrementNumberOfSimultaneousAsynchronousMessages=null;
			m_kernel_get_role=null;
			System.exit(-1);
		}
		else {
			m_send_asynchronous_big_data = getMethod(madkitKernelClass, "sendAsynchronousBigData", AbstractAgent.class,
					AgentAddress.class, AgentAddress.class, Record.class);
			/*m_send_message_and_differ_it_if_necessary = getMethod(madkitKernelClass, "sendMessageAndDifferItIfNecessary", AbstractAgent.class,
					Group.class, String.class, Message.class, String.class, long.class);*/
			m_cancel_big_data_transfer = getMethod(madkitKernelClass, "cancelBigDataTransfer", AbstractAgent.class,
					BigDataTransferID.class, BigDataResultMessage.Type.class);
			m_get_madkit_kernel = getMethod(AbstractAgent.class, "getMadkitKernel");
			m_get_agent = getMethod(AgentAddress.class, "getAgent");
			m_get_role_object = getMethod(AgentAddress.class, "getRoleObject");
			m_decrementNumberOfSimultaneousAsynchronousMessages = getMethod(internalRoleClass, "decrementNumberOfSimultaneousAsynchronousMessages");
			m_incrementNumberOfSimultaneousAsynchronousMessages = getMethod(internalRoleClass, "incrementNumberOfSimultaneousAsynchronousMessages", long.class);
			m_kernel_get_role = getMethod(madkitKernelClass, "getRole", Group.class, String.class);
		}
	}
	static AbstractAgent getAgent(AgentAddress aa)
	{
		try {
			return (AbstractAgent)invoke(m_get_agent, aa);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}
	}

	static boolean incrementNumberOfSimultaneousAsynchronousMessages(AgentAddress aa, long maxNumberOfSimultaneousAsynchronousMessages)
	{
		try {
			return (boolean)invoke(m_incrementNumberOfSimultaneousAsynchronousMessages, invoke(m_get_role_object, aa), maxNumberOfSimultaneousAsynchronousMessages);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return false;
		}
	}
	static void decrementNumberOfSimultaneousAsynchronousMessages(AgentAddress aa)
	{
		try {
			invoke(m_decrementNumberOfSimultaneousAsynchronousMessages, invoke(m_get_role_object, aa));
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	static void decrementNumberOfSimultaneousAsynchronousMessagesIfPossible(AbstractAgent agent, Group group, String role)
	{
		try {
			Object internalRole=invoke(m_kernel_get_role, getMadkitKernel(agent), group, role);
			if (internalRole!=null)
				invoke(m_decrementNumberOfSimultaneousAsynchronousMessages, internalRole);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}
	static Agent getMadkitKernel(AbstractAgent abstractAgent)
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
	static BigDataTransferID sendAsynchronousBigData(AbstractAgent requester, AgentAddress senderAA, AgentAddress receiverAA,
											  AsynchronousBigDataTable.Record record)
	{
		try {
			return (BigDataTransferID)invoke(m_send_asynchronous_big_data, getMadkitKernel(requester),requester, senderAA, receiverAA, record);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
			return null;
		}

	}
	/*void sendMessageAndDifferItIfNecessary(final AbstractAgent requester, Group group, final String role,
															   final Message message, final String senderRole, final long timeOutInMs)  {
		try {
			invoke(m_send_message_and_differ_it_if_necessary, getMadkitKernel(requester), requester, group, role, message, senderRole, timeOutInMs);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}*/

	static void cancelBigDataTransfer(AbstractAgent requester, BigDataTransferID bigDataTransferID, BigDataResultMessage.Type reason)
	{
		try {
			invoke(m_cancel_big_data_transfer, getMadkitKernel(requester), requester, bigDataTransferID, reason);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}




	public List<Record> getAsynchronousMessagesBySenderRole(Group group, String senderRole) throws DatabaseException {
		if (senderRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return getRecords(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		}, "groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.toString(), "roleSender", senderRole);
	}
	public List<Record> getAsynchronousMessagesByReceiverRole(Group group, String receiverRole) throws DatabaseException {
		if (receiverRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return getRecords(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		}, "groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.toString(), "roleReceiver", receiverRole);
	}
	public List<Record> getAsynchronousMessagesByGroup(Group group) throws DatabaseException {
		if (group==null)
			throw new NullPointerException();
		return getRecords(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		}, "groupPath=%groupPath", "groupPath", group.toString());
	}
	public long getAsynchronousMessagesNumberBySenderRole(Group group, String senderRole) throws DatabaseException {
		if (senderRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return getRecordsNumber(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		}, "groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.toString(), "roleSender", senderRole);
	}
	public long getAsynchronousMessagesNumberByReceiverRole(Group group, String receiverRole) throws DatabaseException {
		if (receiverRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return getRecordsNumber(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		}, "groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.toString(), "roleReceiver", receiverRole);
	}
	public long getAsynchronousMessagesNumberByGroup(Group group) throws DatabaseException {
		if (group==null)
			throw new NullPointerException();
		return getRecordsNumber(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		},"groupPath=%groupPath", "groupPath", group.toString());
	}


}
