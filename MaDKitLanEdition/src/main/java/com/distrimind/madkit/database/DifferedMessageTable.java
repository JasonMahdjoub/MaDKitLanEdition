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

import static com.distrimind.madkit.util.ReflectionTools.getMethod;
import static com.distrimind.madkit.util.ReflectionTools.invoke;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.11.0
 */
public final class DifferedMessageTable extends Table<DifferedMessageTable.Record> {

	public static final int MAX_DIFFERED_MESSAGE_LENGTH=128*1024;

	protected DifferedMessageTable() throws DatabaseException {
	}

	public static class Record extends DatabaseRecord
	{

		@AutoPrimaryKey
		private int id;

		@Field(index = true, limit = Group.MAX_PATH_LENGTH)
		@NotNull
		private String groupPath;

		@Field(index = true, limit=Group.MAX_PATH_LENGTH)
		@NotNull
		private String roleSender;

		@Field(index = true, limit = Group.MAX_PATH_LENGTH)
		@NotNull
		private String roleReceiver;

		@Field(limit=MAX_DIFFERED_MESSAGE_LENGTH)
		@NotNull
		private byte[] differedMessage;

		@Field
		private long utcTimeUpdate;

		public Record() {
		}

		public Record(String groupPath, String roleSender, String roleReceiver, Message differedMessage) throws IOException {
			this.id=0;
			if (groupPath==null)
				throw new NullPointerException();
			if (roleSender==null)
				throw new NullPointerException();
			if (roleReceiver==null)
				throw new NullPointerException();
			if (differedMessage==null)
				throw new NullPointerException();
			this.groupPath = groupPath;
			this.roleSender = roleSender;
			this.roleReceiver = roleReceiver;
			try(RandomByteArrayOutputStream baos=new RandomByteArrayOutputStream())
			{
				baos.writeObject(differedMessage, false);
				baos.flush();
				this.differedMessage = baos.getBytes();
				if (this.differedMessage.length>MAX_DIFFERED_MESSAGE_LENGTH)
					throw new IllegalArgumentException("Differed message size ("+HumanReadableBytesCount.convertToString(this.differedMessage.length)+") exceed limit of "+ HumanReadableBytesCount.convertToString(MAX_DIFFERED_MESSAGE_LENGTH));
			}
			this.utcTimeUpdate=System.currentTimeMillis();
		}

		public long getUtcTimeUpdate() {
			return utcTimeUpdate;
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

		public Message getDifferedMessage() throws IOException, ClassNotFoundException {
			try(RandomByteArrayInputStream bais=new RandomByteArrayInputStream(differedMessage))
			{
				return bais.readObject(false, Message.class);
			}
		}
	}


	private final Map<Role, AbstractAgent> availableSenders= Collections.synchronizedMap(new HashMap<>());

	private void checkSender(final AbstractAgent abstractAgent, final Group group, final String r, String groupPath) throws DatabaseException {
		if (abstractAgent==null)
			return ;
		Role role=new Role(group, r);
		AbstractAgent res=availableSenders.get(role);
		if (res!=null)
			return ;
		final AtomicBoolean allRemoved=new AtomicBoolean(true);
		final Set<String> rolesList=new HashSet<>();
		getOrderedRecords(new Filter<Record>(){
			@Override
			public boolean nextRecord(Record _record) {

				AgentAddress aa=abstractAgent.getAgentWithRole(group, _record.getRoleReceiver());
				if (aa!=null) {
					try {

						if (abstractAgent.sendMessageWithRole(aa, _record.getDifferedMessage(), r)== AbstractAgent.ReturnCode.SUCCESS)
							rolesList.add(_record.getRoleReceiver());
					} catch (ClassNotFoundException | IOException e) {
						e.printStackTrace();
					}

				}
				else {
					allRemoved.set(false);
				}
				return false;
			}
		},"groupPath=%groupPath AND roleSender=%roleSender", new Object[]{"groupPath", groupPath, "roleSender", r}, true, "utcTimeUpdate");
		removeRecords(new Filter<Record>(){
			@Override
			public boolean nextRecord(Record _record) {
				return rolesList.contains(_record.getRoleReceiver());

			}
		},"groupPath=%groupPath AND roleSender=%roleSender", "groupPath", groupPath, "roleSender", r);
		if (!allRemoved.get()) {
			availableSenders.put(role, abstractAgent);
		}

	}

	@SuppressWarnings("BooleanMethodIsAlwaysInverted")
	private boolean isConcerned(Collection<String> baseGroupPathList, String groupPath)
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

	public void newAgentConnected(Collection<String> baseGroupPath, final AbstractAgent agent, final AgentAddress agentAddress) throws DatabaseException {
		final String groupPath=agentAddress.getGroup().getPath();
		if (!isConcerned(baseGroupPath, groupPath))
			return ;
		getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Void>() {
			@Override
			public Void run() throws Exception {
				checkSender(agent, agentAddress.getGroup(), agentAddress.getRole(), groupPath);

				final Map<Role, Boolean> allRemoved=new HashMap<>();
				getOrderedRecords(new Filter<Record>(){

					@Override
					public boolean nextRecord(Record _record) {
						Role r=new Role(agentAddress.getGroup(), _record.getRoleSender());
						AbstractAgent abstractAgent=availableSenders.get(r);
						if (abstractAgent==null)
							return false;
						AgentAddress aa=abstractAgent.getAgentWithRole(agentAddress.getGroup(), _record.getRoleReceiver());
						if (aa!=null) {
							try {
								abstractAgent.sendMessageWithRole(aa, _record.getDifferedMessage(), _record.getRoleSender());
								if (!allRemoved.containsKey(r))
									allRemoved.put(r,true);

							} catch (ClassNotFoundException | IOException e) {
								e.printStackTrace();
							}
							return true;
						}
						else {
							allRemoved.put(r,false);
							return false;
						}
					}
				},"groupPath=%groupPath AND roleReceiver=%roleReceiver", new Object[]{"groupPath", groupPath, "roleReceiver", agentAddress.getRole()}, true, "utcTimeUpdate");
				removeRecords(new Filter<Record>(){

					@Override
					public boolean nextRecord(Record _record) {
						Role r=new Role(agentAddress.getGroup(), _record.getRoleSender());
						AbstractAgent abstractAgent=availableSenders.get(r);
						if (abstractAgent==null)
							return false;
						AgentAddress aa=abstractAgent.getAgentWithRole(agentAddress.getGroup(), _record.getRoleReceiver());
						return aa != null;
					}
				},"groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", groupPath, "roleReceiver", agentAddress.getRole());
				for (Map.Entry<Role, Boolean> e : allRemoved.entrySet())
					if (e.getValue())
						availableSenders.remove(e.getKey());
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

	public void newAgentDisconnected(Collection<String> baseGroupPath, AgentAddress agentAddress)
	{
		final String groupPath=agentAddress.getGroup().getPath();
		if (!isConcerned(baseGroupPath, groupPath))
			return ;

		Role role=new Role(agentAddress.getGroup(), agentAddress.getRole());
		availableSenders.remove(role);

	}

	public AbstractAgent.ReturnCode differMessage(Collection<String> baseGroupPath, final AbstractAgent requester, final Group group, final String roleSender, final String roleReceiver, final Message message) throws DatabaseException {

		final String groupPath=group.getPath();
		if (!isConcerned(baseGroupPath, groupPath))
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
			return requester.sendMessageWithRole(receiverAA, message, roleSender);
		}
		else {
			getDatabaseWrapper().runSynchronizedTransaction(new SynchronizedTransaction<Void>() {
				@Override
				public Void run() throws Exception {
					Role role=new Role(group, roleSender);
					availableSenders.putIfAbsent(role, requester);
					addRecord(new Record(group.getPath(), roleSender, roleReceiver, message));

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

	}
	public long cancelDifferedMessagesBySenderRole(Group group, String senderRole) throws DatabaseException {
		return removeRecords("groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.getPath(), "roleSender", senderRole);
	}
	public long cancelDifferedMessagesByReceiverRole(Group group, String receiverRole) throws DatabaseException {
		return removeRecords("groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.getPath(), "roleReceiver", receiverRole);
	}
	public long cancelDifferedMessagesByGroup(Group group) throws DatabaseException {
		return removeRecords("groupPath=%groupPath", "groupPath", group.getPath());
	}

	public List<Record> getDifferedMessagesBySenderRole(Group group, String senderRole) throws DatabaseException {
		return getRecords("groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.getPath(), "roleSender", senderRole);
	}
	public List<Record> getDifferedMessagesByReceiverRole(Group group, String receiverRole) throws DatabaseException {
		return getRecords("groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.getPath(), "roleReceiver", receiverRole);
	}
	public List<Record> getDifferedMessagesByGroup(Group group) throws DatabaseException {
		return getRecords("groupPath=%groupPath", "groupPath", group.getPath());
	}
	public long getDifferedMessagesNumberBySenderRole(Group group, String senderRole) throws DatabaseException {
		return getRecordsNumber("groupPath=%groupPath AND roleSender=%roleSender", "groupPath", group.getPath(), "roleSender", senderRole);
	}
	public long getDifferedMessagesNumberByReceiverRole(Group group, String receiverRole) throws DatabaseException {
		return getRecordsNumber("groupPath=%groupPath AND roleReceiver=%roleReceiver", "groupPath", group.getPath(), "roleReceiver", receiverRole);
	}
	public long getDifferedMessagesNumberByGroup(Group group) throws DatabaseException {
		return getRecordsNumber("groupPath=%groupPath", "groupPath", group.getPath());
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
