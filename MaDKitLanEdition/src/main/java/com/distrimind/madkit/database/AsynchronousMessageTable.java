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
package com.distrimind.madkit.database;


import com.distrimind.madkit.kernel.*;
import com.distrimind.ood.database.*;
import com.distrimind.ood.database.annotations.AutoPrimaryKey;
import com.distrimind.ood.database.annotations.Field;
import com.distrimind.ood.database.annotations.NotNull;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.HumanReadableBytesCount;
import com.distrimind.util.io.RandomByteArrayInputStream;
import com.distrimind.util.io.RandomByteArrayOutputStream;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.distrimind.util.ReflectionTools.getMethod;
import static com.distrimind.util.ReflectionTools.invoke;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.11.0
 */
public final class AsynchronousMessageTable extends Table<AsynchronousMessageTable.Record> {

	public static final int MAX_ASYNCHRONOUS_MESSAGE_LENGTH =512*1024;
	public static final int MAX_PATH_LENGTH=Group.MAX_PATH_LENGTH+Group.MAX_COMMUNITY_LENGTH+20;

	private AsynchronousMessageTable() throws DatabaseException {
	}

	public static class Record extends DatabaseRecord
	{


		@AutoPrimaryKey
		private int id;

		@Field(index = true, limit = MAX_PATH_LENGTH)
		@NotNull
		private String groupPath;

		@Field(index = true, limit=Group.MAX_PATH_LENGTH)
		@NotNull
		private String roleSender;

		@Field(index = true, limit = Group.MAX_PATH_LENGTH)
		@NotNull
		private String roleReceiver;

		@Field(limit= MAX_ASYNCHRONOUS_MESSAGE_LENGTH)
		@NotNull
		private byte[] asynchronousMessage;

		@Field
		private long queryTimeUTC;

		@Field
		private long timeOutInMs;

		public Record() {
		}

		public Record(String groupPath, String roleSender, String roleReceiver, Message asynchronousMessage, long timeOutInMs) throws IOException {
			this.id=0;
			if (groupPath==null)
				throw new NullPointerException();
			if (roleSender==null)
				throw new NullPointerException();
			if (roleReceiver==null)
				throw new NullPointerException();
			if (asynchronousMessage ==null)
				throw new NullPointerException();
			if (roleReceiver.equals(roleSender))
				throw new IllegalArgumentException();
			if (timeOutInMs<1)
				throw new IllegalArgumentException();
			this.groupPath = groupPath;
			this.roleSender = roleSender;
			this.roleReceiver = roleReceiver;
			try(RandomByteArrayOutputStream baos=new RandomByteArrayOutputStream())
			{
				baos.writeObject(asynchronousMessage, false);
				baos.flush();
				this.asynchronousMessage = baos.getBytes();
				if (this.asynchronousMessage.length> MAX_ASYNCHRONOUS_MESSAGE_LENGTH)
					throw new IllegalArgumentException("Asynchronous message size ("+HumanReadableBytesCount.convertToString(this.asynchronousMessage.length)+") exceed limit of "+ HumanReadableBytesCount.convertToString(MAX_ASYNCHRONOUS_MESSAGE_LENGTH));
			}
			this.queryTimeUTC =System.currentTimeMillis();
			this.timeOutInMs=timeOutInMs;
		}

		public long getQueryTimeUTC() {
			return queryTimeUTC;
		}

		public int getId() {
			return id;
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

		public boolean isObsolete()
		{
			return timeOutInMs+this.queryTimeUTC<System.currentTimeMillis();
		}

		public Message getAsynchronousMessage() throws IOException, ClassNotFoundException {
			try(RandomByteArrayInputStream bais=new RandomByteArrayInputStream(asynchronousMessage))
			{
				return bais.readObject(false, Message.class);
			}
		}

		public long getTimeOutInMs() {
			return timeOutInMs;
		}
	}


	public void cleanObsoleteData()
	{
		try {
			removeRecords(new Filter<Record>() {
				@Override
				public boolean nextRecord(Record _record) {
					return _record.isObsolete();
				}
			});
		} catch (DatabaseException e) {
			e.printStackTrace();
		}
	}

	//private final Map<Role, AbstractAgent> availableSenders= Collections.synchronizedMap(new HashMap<>());

	private void checkSender(AbstractAgent sender, final Group group, final String roleSender, String groupPath) throws DatabaseException {
		if (sender==null || !sender.isAlive())
		{
			return ;
		}


		if (sender.getAgentAddressIn(group, roleSender)==null)
			return;

		final AtomicBoolean allRemoved=new AtomicBoolean(true);

		final StringBuilder where=new StringBuilder("groupPath=%groupPath AND roleSender=%roleSender and (");
		final int startQueryLength=where.length();
		final ArrayList<String> wheres=new ArrayList<>();

		getOrderedRecords(new Filter<Record>(){
			@Override
			public boolean nextRecord(Record _record) {

				AgentAddress aa = sender.getAgentWithRole(group, _record.getRoleReceiver());

				if (aa != null) {
					try {

						if (_record.isObsolete() || canForgiveMessage(sender.sendMessageWithRole(aa, _record.getAsynchronousMessage(), roleSender))) {
							if (where.length() > startQueryLength)
								where.append(" OR ");

							where.append("id=")
									.append(_record.id);
							if (where.length() > 500000) {
								where.append(")");
								wheres.add(where.toString());
								where.setLength(startQueryLength);
							}
						} else {
							allRemoved.set(false);
						}
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}

				} else {

					allRemoved.set(false);
				}
				return false;
			}
		},"groupPath=%groupPath AND roleSender=%roleSender", new Object[]{"groupPath", groupPath, "roleSender", roleSender}, true, "queryTimeUTC");
		if (!allRemoved.get() || where.length() > startQueryLength || wheres.size()>0) {
			if (allRemoved.get()) {
				removeAllRecordsWithCascade();
			} else {
				if (where.length() > startQueryLength) {
					where.append(")");
					wheres.add(where.toString());
				}
				for (String w : wheres)
					removeRecords(w, "groupPath", groupPath, "roleSender", roleSender);
			}
		}
	}
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	static boolean isConcerned(Collection<String> baseGroupPathList, Group group)
	{
		if (baseGroupPathList==null)
			return true;
		return isConcerned(baseGroupPathList, group.getPath());
	}
	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	static boolean isConcerned(Collection<String> baseGroupPathList, String groupPath)
	{
		if (baseGroupPathList==null)
			return true;
		for (String baseGroupPath : baseGroupPathList) {
			if (baseGroupPath==null)
				continue;
			if (baseGroupPath.length() > groupPath.length())
				continue;
			int i = 0;
			int j = baseGroupPath.length();
			int limit = baseGroupPath.length() / 2;

			while (i <= limit && j > limit) {
				--j;
				if (baseGroupPath.charAt(i) != groupPath.charAt(i))
					continue;
				if (i != j && baseGroupPath.charAt(j) != groupPath.charAt(j))
					continue;

				++i;
			}
			return true;
		}
		return false;
	}

	public void groupRoleAvailable(final AbstractAgent madkitKernel, Collection<String> baseGroupPath, AbstractAgent sender, final AgentAddress agentAddress) throws DatabaseException {
		if (madkitKernel==null)
			throw new NullPointerException();

		final String groupPath=agentAddress.getGroup().toString();

		if (!isConcerned(baseGroupPath, agentAddress.getGroup()))
			return ;

		getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Void>() {
			@Override
			public Void run() throws Exception
			{
				checkSender(sender, agentAddress.getGroup(), agentAddress.getRole(), groupPath);
				final StringBuilder where=new StringBuilder("groupPath=%groupPath AND roleReceiver=%roleReceiver and (");
				final int startQueryLength=where.length();
				final ArrayList<String> wheres=new ArrayList<>();
				final AtomicBoolean allRemoved=new AtomicBoolean(true);
				final Map<Role, Boolean> allRemovedPerRole=new HashMap<>();
				getOrderedRecords(new Filter<Record>(){

					@Override
					public boolean nextRecord(Record _record) {

						Role r=new Role(agentAddress.getGroup(), _record.getRoleSender());
						AgentAddress aa=madkitKernel.getAgentWithRole(agentAddress.getGroup(), _record.getRoleSender());
						if (aa==null)
							return false;
						AbstractAgent sender=AsynchronousBigDataTable.getAgent(aa);

						if (sender==null)
							return false;

						aa=sender.getAgentWithRole(agentAddress.getGroup(), _record.getRoleReceiver());
						if (aa!=null) {
							try {
								if (_record.isObsolete() || canForgiveMessage(sender.sendMessageWithRole(aa, _record.getAsynchronousMessage(), _record.getRoleSender())))
								{

									if (where.length() > startQueryLength)
										where.append(" OR ");
									where.append("id=")
											.append(_record.getId());
									if (where.length() > 500000) {
										where.append(")");
										wheres.add(where.toString());
										where.setLength(startQueryLength);
									}
									if (!allRemovedPerRole.containsKey(r))
										allRemovedPerRole.put(r, true);
								}
								else {
									allRemovedPerRole.put(r, false);
									allRemoved.set(false);
								}
							} catch (ClassNotFoundException | IOException e) {
								allRemoved.set(false);
								allRemovedPerRole.put(r, false);
								e.printStackTrace();
							}
						}
						else {
							allRemoved.set(false);
							allRemovedPerRole.put(r, false);
						}
						return false;
					}
				},"groupPath=%groupPath AND roleReceiver=%roleReceiver", new Object[]{"groupPath", groupPath, "roleReceiver", agentAddress.getRole()}, true, "queryTimeUTC");
				if (!allRemoved.get() || where.length() > startQueryLength || wheres.size()>0) {


					if (allRemoved.get()) {
						removeAllRecordsWithCascade();
					} else {
						if (where.length() > startQueryLength) {
							where.append(")");
							wheres.add(where.toString());
						}
						for (String w : wheres) {
							removeRecords(w, "groupPath", groupPath, "roleReceiver", agentAddress.getRole());
						}
					}
				}
				return null;
			}

			@Override
			public TransactionIsolation getTransactionIsolation() {
				return TransactionIsolation.TRANSACTION_SERIALIZABLE;
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

	boolean canForgiveMessage(AbstractAgent.ReturnCode r)
	{
		return r== AbstractAgent.ReturnCode.SUCCESS || r== AbstractAgent.ReturnCode.TRANSFER_IN_PROGRESS || r==AbstractAgent.ReturnCode.INVALID_AGENT_ADDRESS;
	}
	public AbstractAgent.ReturnCode differMessage(Collection<String> baseGroupPath, final AbstractAgent requester,
												  final Group group, final String roleSender,
												  final String roleReceiver, final Message message,
												  final long timeOutInMs) throws DatabaseException {

		if (!isConcerned(baseGroupPath, group))
			return AbstractAgent.ReturnCode.IGNORED;

		if (!requester.hasGroup(group))
			return AbstractAgent.ReturnCode.NOT_IN_GROUP;
		AgentAddress senderAA=requester.getAgentAddressIn(group, roleSender);
		if (senderAA==null)
			return AbstractAgent.ReturnCode.ROLE_NOT_HANDLED;

		setSender(message, senderAA);

		AgentAddress receiverAA=requester.getAgentWithRole(group, roleReceiver);

		if (receiverAA!=null)
		{
			AbstractAgent.ReturnCode r=requester.sendMessageWithRole(receiverAA, message, roleSender);
			if (canForgiveMessage(r))
				return r;
		}
		getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Void>() {
			@Override
			public Void run() throws Exception {
				addRecord(new Record(group.toString(), roleSender, roleReceiver, message,timeOutInMs));

				return null;
			}

			@Override
			public TransactionIsolation getTransactionIsolation() {
				return TransactionIsolation.TRANSACTION_READ_UNCOMMITTED;
			}

			@Override
			public boolean doesWriteData() {
				return true;
			}

			@Override
			public void initOrReset() {

			}
		});
		return AbstractAgent.ReturnCode.MESSAGE_DIFFERED;
	}
	public long cancelAsynchronousMessagesBySenderRole(Group group, String senderRole) throws DatabaseException {
		if (senderRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return removeRecords("groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.toString(), "roleSender", senderRole);
	}
	public long cancelAsynchronousMessagesByReceiverRole(Group group, String receiverRole) throws DatabaseException {
		if (receiverRole==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		return removeRecords("groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.toString(), "roleReceiver", receiverRole);
	}
	public long cancelAsynchronousMessagesByGroup(Group group) throws DatabaseException {
		if (group==null)
			throw new NullPointerException();
		return removeRecords("groupPath=%groupPath", "groupPath", group.toString());
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
		},"groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.toString(), "roleSender", senderRole);
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
		},"groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.toString(), "roleReceiver", receiverRole);
	}
	public List<Record> getAsynchronousMessagesByGroup(Group group) throws DatabaseException {
		if (group==null)
			throw new NullPointerException();
		return getRecords(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		},"groupPath=%groupPath", "groupPath", group.toString());
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
		},"groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.toString(), "roleSender", senderRole);
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
		},"groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.toString(), "roleReceiver", receiverRole);
	}
	public long getAsynchronousMessagesNumberByGroup(Group group) throws DatabaseException {
		if (group==null)
			throw new NullPointerException();
		return getRecordsNumber(new Filter<Record>() {
			@Override
			public boolean nextRecord(Record _record) {
				return !_record.isObsolete();
			}
		}, "groupPath=%groupPath", "groupPath", group.toString());
	}

	private static final Method m_set_message_sender;
	static
	{
		m_set_message_sender = getMethod(Message.class, "setSender", AgentAddress.class);
	}

	static void setSender(Message m, AgentAddress aa) {
		try {
			invoke(m_set_message_sender, m, aa);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
	}


}
