/*
 * MadKitLanEdition (created by Jason MAHDJOUB (jason.mahdjoub@distri-mind.fr)) Copyright (c)
 * 2015 is a fork of MadKit and MadKitGroupExtension. 
 * 
 * Copyright or © or Copr. Jason Mahdjoub, Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
 * jason.mahdjoub@distri-mind.fr
 * fmichel@lirmm.fr
 * olg@no-distance.net
 * ferber@lirmm.fr
 * 
 * This software is a computer program whose purpose is to
 * provide a lightweight Java library for designing and simulating Multi-Agent Systems (MAS).
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package com.distrimind.madkit.kernel;

import com.distrimind.madkit.database.AsynchronousBigDataTable;
import com.distrimind.madkit.database.AsynchronousMessageTable;
import com.distrimind.madkit.i18n.Words;
import com.distrimind.madkit.kernel.ConversationID.InterfacedIDs;
import com.distrimind.madkit.kernel.network.AskForConnectionMessage;
import com.distrimind.madkit.kernel.network.AskForTransferMessage;
import com.distrimind.madkit.kernel.network.Connection;
import com.distrimind.madkit.kernel.network.ConnectionIdentifier;
import com.distrimind.madkit.kernel.network.connection.access.PairOfIdentifiers;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.AbstractDecentralizedIDGenerator;
import com.distrimind.util.IDGeneratorInt;
import com.distrimind.util.concurrent.LockerCondition;
import com.distrimind.util.concurrent.ScheduledPoolExecutor;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.RandomInputStream;
import com.distrimind.util.io.SecureExternalizable;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.logging.Level;

import static com.distrimind.madkit.i18n.I18nUtilities.getCGRString;
import static com.distrimind.madkit.kernel.AbstractAgent.ReturnCode.*;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MaDKitLanEdition 1.0
 *
 */
final class LoggedKernel extends MadkitKernel {

	LoggedKernel(MadkitKernel k) {
		super(k);
		loggedKernel = this;
	}

	@Override
	public AgentLogger getLogger() {
		return kernel.getLogger();
	}

	@Override
	public KernelAddress getKernelAddress() {
		return kernel.getKernelAddress();
	}

	@Override
	ReturnCode createGroup(AbstractAgent requester, Group group, Object passKey, boolean manually_created) {
		final ReturnCode r = kernel.createGroup(requester, group, passKey, manually_created);
		if (r == SUCCESS) {
			if (requester.isFinestLogOn()) {
				requester.logger.log(Level.FINEST,
						Influence.CREATE_GROUP.successString() + getCGRString(group) + "distribution "
								+ (group.isDistributed() ? "ON" : "OFF") + " with "
								+ (group.getGateKeeper() == null ? "no access control "
										: group.getGateKeeper() + " as gatekeeper, with " + passKey
												+ " as pass key"));
			}
			return SUCCESS;
		}
		if (requester.isWarningOn()) {// do not factorize : optimizing strings and exception creation
			requester.handleException(Influence.CREATE_GROUP, new OrganizationWarning(r, group, null));
		}
		return r;
	}

	@Override
	AgentThreadFactory getNormalAgentThreadFactory() {
		return kernel.getNormalAgentThreadFactory();
	}

	@Override
	AgentThreadFactory getDaemonAgentThreadFactory() {
		return kernel.getDaemonAgentThreadFactory();
	}

	@Override
	ReturnCode requestRole(AbstractAgent requester, Group group, String role, SecureExternalizable memberCard,
			boolean manual_request) {
		final ReturnCode r = kernel.requestRole(requester, group, role, memberCard, manual_request);
		if (r == SUCCESS) {
			if (requester.isFinestLogOn()) {
				requester.logger.log(Level.FINEST, Influence.REQUEST_ROLE.successString() + getCGRString(group, role)
						+ "using " + memberCard + " as passKey");
			}
			return SUCCESS;
		}
		if (requester.isWarningOn()) {
			requester.handleException(Influence.REQUEST_ROLE, new OrganizationWarning(r, group, role));
		}
		return r;
	}


	@Override
	ReturnCode leaveGroup(AbstractAgent requester, Group group, boolean manually_requested) {
		final ReturnCode r = kernel.leaveGroup(requester, group, manually_requested);
		if (r == SUCCESS) {
			if (requester.isFinestLogOn()) {
				requester.logger.log(Level.FINEST, Influence.LEAVE_GROUP.successString() + getCGRString(group));
			}
			return SUCCESS;
		}
		if (requester.isWarningOn()) {
			requester.handleException(Influence.LEAVE_GROUP, new OrganizationWarning(r, group, null));
		}
		return r;
	}


	@Override
	ReturnCode leaveRole(AbstractAgent requester, Group group, String role, boolean manualRequested) {
		ReturnCode r = kernel.leaveRole(requester, group, role, manualRequested);
		if (r == SUCCESS) {
			if (requester.isFinestLogOn()) {
				requester.logger.log(Level.FINEST, Influence.LEAVE_ROLE.successString() + getCGRString(group, role));
			}
			return SUCCESS;
		}
		if (requester.isWarningOn()) {
			requester.handleException(Influence.LEAVE_ROLE, new OrganizationWarning(r, group, role));
		}
		return r;
	}


	@Override
	Set<AgentAddress> getAgentsWithRole(AbstractAgent requester, AbstractGroup group, String role,
			boolean callerIncluded) {
		try {
			Set<AgentAddress> res = kernel.getAgentsWithRole(requester, group, role, callerIncluded);
			if (requester.isFinestLogOn())
				requester.logger.log(Level.FINEST,
						Influence.GET_AGENTS_WITH_ROLE + getCGRString(group, role) + ": " + res);
			return res;
		} catch (NullPointerException e) {
			if (requester.isWarningOn()) {
				requester.handleException(Influence.GET_AGENTS_WITH_ROLE, e);
			}
			throw e;
		}
	}


	@Override
	AgentAddress getAgentWithRole(AbstractAgent requester, AbstractGroup group, String role) {
		try {
			AgentAddress aa = kernel.getAgentWithRole(requester, group, role);
			if (requester.isFinestLogOn())
				requester.logger.log(Level.FINEST,
						Influence.GET_AGENT_WITH_ROLE + getCGRString(group, role) + ": " + aa);
			return aa;
		} catch (NullPointerException e) {
			if (requester.isWarningOn()) {
				requester.handleException(Influence.GET_AGENTS_WITH_ROLE, e);
			}
			throw e;
		}
	}

	@Override
	AgentAddress getAgentAddressIn(AbstractAgent agent, Group group, String role) {
		final AgentAddress aa = kernel.getAgentAddressIn(agent, group, role);
		if (aa == null && agent.isWarningOn() && isCreatedRole(group, role)) {
			agent.handleException(Influence.GET_AGENT_ADDRESS_IN,
					new OrganizationWarning(ReturnCode.ROLE_NOT_HANDLED, group, role));
		}
		return aa;
	}

	@Override
	ReturnCode broadcastMessageWithRole(AbstractAgent requester, AbstractGroup group, String role,
			Message messageToSend, String senderRole, boolean sendAllRepliesInOneBlock) {

		ReturnCode r = kernel.broadcastMessageWithRole(requester, group, role, messageToSend, senderRole,
				sendAllRepliesInOneBlock);
		if (r == SUCCESS) {
			if (requester.isFinestLogOn())
				requester.logger.log(Level.FINEST, Influence.BROADCAST_MESSAGE + "-> " + getCGRString(group, role)
						+ (senderRole == null ? "" : " with role " + senderRole) + messageToSend);
			return SUCCESS;
		}
		if (requester.isWarningOn()) {
			if (r == NO_RECIPIENT_FOUND) {
				requester.handleException(Influence.BROADCAST_MESSAGE, new MadkitWarning(r));
			} else if (r == ROLE_NOT_HANDLED) {
				requester.handleException(Influence.BROADCAST_MESSAGE, new OrganizationWarning(r, group, senderRole));
			} else {
				requester.handleException(Influence.BROADCAST_MESSAGE, new OrganizationWarning(r, group, role));
			}
		}
		return r;
	}

	@Override
	ReturnCode sendMessage(AbstractAgent requester, AgentAddress receiver, Message messageToSend, String senderRole) {
		final ReturnCode r = kernel.sendMessage(requester, receiver, messageToSend, senderRole);
		if (r == SUCCESS || r == ReturnCode.TRANSFER_IN_PROGRESS) {
			if (requester.isFinestLogOn())
				requester.logger.log(Level.FINEST, Influence.SEND_MESSAGE.successString() + " " + messageToSend);
			return SUCCESS;
		}
		if (requester.isWarningOn()) {
			if (r == NOT_IN_GROUP || r == ROLE_NOT_HANDLED) {
				requester.handleException(Influence.SEND_MESSAGE,
						new OrganizationWarning(r, receiver.getGroup(), senderRole));
			} else {
				requester.handleException(Influence.SEND_MESSAGE, new MadkitWarning(r));
			}
		}
		return r;
	}

	@Override
	ReturnCode sendMessage(AbstractAgent requester, AbstractGroup group, String role, Message messageToSend,
			String senderRole) {
		ReturnCode r = kernel.sendMessage(requester, group, role, messageToSend, senderRole);
		if (r == SUCCESS || r == ReturnCode.TRANSFER_IN_PROGRESS) {
			if (requester.isFinestLogOn()) {
				requester.logger.log(Level.FINEST,
						(messageToSend.getReceiver().isFrom(requester.getKernelAddress())
								? Influence.SEND_MESSAGE.successString()
								: Influence.SEND_MESSAGE.toString()) + "->" + getCGRString(group, role) + " "
								+ messageToSend);
			}
			return SUCCESS;
		}
		if (requester.isWarningOn()) {
			if (r == NO_RECIPIENT_FOUND) {
				requester.handleException(Influence.SEND_MESSAGE, new MadkitWarning(r));
			} else if (r == ROLE_NOT_HANDLED) {
				requester.handleException(Influence.SEND_MESSAGE, new OrganizationWarning(r, group, senderRole));
			} else {
				requester.handleException(Influence.SEND_MESSAGE, new OrganizationWarning(r, group, role));
			}
		}
		return r;
	}

	@Override
	ReturnCode sendMessageAndDifferItIfNecessary(final AbstractAgent requester, Group group, final String role, final Message message,
												 final String senderRole) {
		ReturnCode r = kernel.sendMessageAndDifferItIfNecessary(requester, group, role, message, senderRole);
		if (r == SUCCESS || r == ReturnCode.TRANSFER_IN_PROGRESS || r == MESSAGE_DIFFERED) {
			if (requester.isFinestLogOn()) {
				requester.logger.log(Level.FINEST,
						((message.getReceiver()!=null && message.getReceiver().isFrom(requester.getKernelAddress()))
								? Influence.SEND_MESSAGE_AND_DIFFER_IT_IF_NECESSARY.successString()
								: Influence.SEND_MESSAGE_AND_DIFFER_IT_IF_NECESSARY.toString()) + "->" + getCGRString(group, role) + " "
								+ message);
			}
			return r;
		}
		if (requester.isWarningOn()) {
			if (r == NO_RECIPIENT_FOUND) {
				requester.handleException(Influence.SEND_MESSAGE_AND_DIFFER_IT_IF_NECESSARY, new MadkitWarning(r));
			} else if (r == ROLE_NOT_HANDLED) {
				requester.handleException(Influence.SEND_MESSAGE_AND_DIFFER_IT_IF_NECESSARY, new OrganizationWarning(r, group, senderRole));
			} else {
				requester.handleException(Influence.SEND_MESSAGE_AND_DIFFER_IT_IF_NECESSARY, new OrganizationWarning(r, group, role));
			}
		}
		return r;

	}
	@Override
	long cancelAsynchronousMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		long res= kernel.cancelAsynchronousMessagesBySenderRole(requester, group, senderRole);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.CANCEL_ASYNCHRONOUS_MESSAGES_BY_SENDER_ROLE + " (group=" + group+" , senderRole="+senderRole + ", removedNumber="+res+")" + requester.getName());
		return res;
	}
	@Override
	long cancelAsynchronousMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		long res= kernel.cancelAsynchronousMessagesByReceiverRole(requester, group, receiverRole);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.CANCEL_ASYNCHRONOUS_MESSAGES_BY_RECEIVER_ROLE + " (group=" + group+" , receiverRole="+receiverRole + ", removedNumber="+res+")" + requester.getName());
		return res;
	}
	@Override
	long cancelAsynchronousMessagesByGroup(AbstractAgent requester, Group group)  {
		long res= kernel.cancelAsynchronousMessagesByGroup(requester, group);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.CANCEL_ASYNCHRONOUS_MESSAGES_BY_GROUP + " (group=" + group+", removedNumber="+res+")" + requester.getName());
		return res;

	}
	@Override
	List<AsynchronousMessageTable.Record> getAsynchronousMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		List<AsynchronousMessageTable.Record> res= kernel.getAsynchronousMessagesBySenderRole(requester, group, senderRole);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.GET_ASYNCHRONOUS_MESSAGES_BY_SENDER_ROLE + " (group=" + group+" , senderRole="+senderRole + ", removedNumber="+res+")" + requester.getName());
		return res;

	}
	@Override
	List<AsynchronousMessageTable.Record> getAsynchronousMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		List<AsynchronousMessageTable.Record> res= kernel.getAsynchronousMessagesByReceiverRole(requester, group, receiverRole);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.GET_ASYNCHRONOUS_MESSAGES_BY_RECEIVER_ROLE + " (group=" + group+" , receiverRole="+receiverRole + ", removedNumber="+res+")" + requester.getName());
		return res;

	}
	@Override
	List<AsynchronousMessageTable.Record> getAsynchronousMessagesByGroup(AbstractAgent requester, Group group)  {
		List<AsynchronousMessageTable.Record> res= kernel.getAsynchronousMessagesByGroup(requester, group);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.GET_ASYNCHRONOUS_MESSAGES_BY_GROUP + " (group=" + group+", removedNumber="+res+")" + requester.getName());
		return res;


	}
	@Override
	long getAsynchronousMessagesNumberBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		long res= kernel.getAsynchronousMessagesNumberBySenderRole(requester, group, senderRole);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.GET_ASYNCHRONOUS_MESSAGES_NUMBER_BY_SENDER_ROLE + " (group=" + group+" , senderRole="+senderRole + ", removedNumber="+res+")" + requester.getName());
		return res;
	}
	@Override
	long getAsynchronousMessagesNumberByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		long res= kernel.getAsynchronousMessagesNumberByReceiverRole(requester, group, receiverRole);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.GET_ASYNCHRONOUS_MESSAGES_NUMBER_BY_RECEIVER_ROLE + " (group=" + group+" , receiverRole="+receiverRole + ", removedNumber="+res+")" + requester.getName());
		return res;
	}
	@Override
	long getAsynchronousMessagesNumberByGroup(AbstractAgent requester, Group group)  {
		long res= kernel.getAsynchronousMessagesNumberByGroup(requester, group);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.GET_ASYNCHRONOUS_MESSAGES_NUMBER_BY_GROUP + " (group=" + group+", removedNumber="+res+")" + requester.getName());
		return res;

	}
	@Override
	// the log is done in the kernel to not deal with the catch or specify
	// requirement in the not logged method
	List<Message> broadcastMessageWithRoleAndWaitForReplies(AbstractAgent requester, AbstractGroup group, String role,
			Message message, String senderRole, Integer timeOutMilliSeconds) throws InterruptedException {

		final List<Message> result = kernel.broadcastMessageWithRoleAndWaitForReplies(requester, group, role, message,
				senderRole, timeOutMilliSeconds);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, Influence.BROADCAST_MESSAGE_AND_WAIT + ": received: " + result);
		return result;
	}


	@Override
	void launchAgentBucketWithRoles(AbstractAgent requester, List<AbstractAgent> bucket, int cpuCoreNb,
			Role... CGRLocations) {
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"launchAgentBucketWithRoles : " + bucket.size() + " "
							+ (bucket.size() > 0 ? bucket.get(0).getClass().getName() : "agents !!!") + " "
							+ (CGRLocations.length > 0 ? Arrays.deepToString(CGRLocations) : ""));
		kernel.launchAgentBucketWithRoles(requester, bucket, cpuCoreNb, CGRLocations);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "launchAgentBucketWithRoles OK !");
	}

	/**
	 * @see com.distrimind.madkit.kernel.MadkitKernel#launchAgent(com.distrimind.madkit.kernel.AbstractAgent,
	 *      com.distrimind.madkit.kernel.AbstractAgent, int, boolean)
	 */
	@Override
	ReturnCode launchAgent(AbstractAgent requester, AbstractAgent agent, int timeOutSeconds, boolean defaultGUI) {
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Influence.LAUNCH_AGENT + " (" + timeOutSeconds + ")" + agent.getName() + "...");
		final ReturnCode r = kernel.launchAgent(requester, agent, timeOutSeconds, defaultGUI);
		if (r == SUCCESS || r == TIMEOUT) {
			if (requester.isFinestLogOn())
				requester.logger.log(Level.FINEST, Influence.LAUNCH_AGENT.toString() + agent + " " + r);
		} else if (requester.isWarningOn()) {
			requester.handleException(Influence.LAUNCH_AGENT, new MadkitWarning(agent.toString(), r));
		}
		return r;
	}

	@Override
	ReturnCode killAgent(final AbstractAgent requester, final AbstractAgent target, int timeOutSeconds,
			KillingType killing_type) {
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, Influence.KILL_AGENT + " (" + timeOutSeconds + ")" + target + "...");
		final ReturnCode r = kernel.killAgent(requester, target, timeOutSeconds, killing_type);
		if (r == SUCCESS || r == TIMEOUT) {
			if (requester.isFinestLogOn())
				requester.logger.log(Level.FINEST, Influence.KILL_AGENT + target.getName() + " " + r);
		} else if (requester.isWarningOn()) {
			requester.handleException(Influence.KILL_AGENT, new MadkitWarning(target.toString(), r));
		}
		return r;
	}

	@Override
	protected void zombieDetected(State s, AbstractAgent target) {
		kernel.zombieDetected(s, target);
	}

	@Override
	void removeThreadedAgent(Agent myAgent) {
		kernel.removeThreadedAgent(myAgent);
	}

	@Override
	boolean isCommunity(AbstractAgent requester, String community) {
		final boolean fact = kernel.isCommunity(requester, community);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, Words.COMMUNITY + " ? " + getCGRString(community) + fact);
		return fact;
	}

	@Override
	boolean isGroup(AbstractAgent requester, Group group) {
		final boolean fact = kernel.isGroup(requester, group);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, Words.GROUP + " ? " + getCGRString(group) + fact);
		return fact;
	}

	@Override
	boolean isRole(AbstractAgent requester, Group group, String role) {
		final boolean fact = kernel.isRole(requester, group, role);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, Words.ROLE + " ? " + getCGRString(group, role) + fact);
		return fact;
	}

	@Override
	boolean isConcernedBy(AbstractAgent requester, AgentAddress agentAddress) {
		final boolean fact = kernel.isConcernedBy(requester, agentAddress);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, Words.AGENT_ADDRESS + " ? " + agentAddress + ":"
					+ (fact ? "" : (Words.NOT + " ")) + Words.CONCERNED);
		return fact;
	}

	@Override
	boolean isLocalAgentAddressValid(AbstractAgent requester, AgentAddress agentAddress) {
		final boolean fact = kernel.isLocalAgentAddressValid(requester, agentAddress);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					Words.AGENT_ADDRESS + " ? " + agentAddress + ":" + (fact ? "" : (Words.NOT + " ")) + Words.VALID);
		return fact;
	}

	@Override
	MadkitKernel getMadkitKernel() {
		return kernel;
	}

	@Override
	AsynchronousBigDataTable getAsynchronousBigDataTable() {
		return kernel.getAsynchronousBigDataTable();
	}

	@Override
	synchronized boolean removeOverlooker(AbstractAgent requester, Overlooker<? extends AbstractAgent> o) {
		final boolean added = kernel.removeOverlooker(requester, o);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, o.getClass().getSimpleName() + (added ? " removed" : " not added") + o);
		return added;
	}

	@Override
	synchronized boolean addOverlooker(AbstractAgent requester, Overlooker<? extends AbstractAgent> o) {
		final boolean added = kernel.addOverlooker(requester, o);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, o.getClass().getSimpleName() + (added ? " OK" : " already added") + o);
		return added;
	}


	@Override
	TaskID scheduleTask(AbstractAgent requester, Task<?> _task, boolean ask_for_execution_confirmation) {
		TaskID t = kernel.scheduleTask(requester, _task, ask_for_execution_confirmation);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"Scheduling task " + _task + (ask_for_execution_confirmation ? "with message confirmation" : "")
							+ " and with default task manager agent : " + (t == null ? "FAIL" : "OK"));
		return t;
	}

	@Override
	boolean cancelTask(AbstractAgent requester, TaskID task_id, boolean mayInterruptTask) {
		boolean rc = kernel.cancelTask(requester, task_id, mayInterruptTask);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"Canceling task " + task_id + " with default task manager agent : " + rc);
		return rc;
	}


	@Override
	boolean isConcernedByAutoRequestRole(AbstractAgent requester, Group group, String role) {
		boolean res = kernel.isConcernedByAutoRequestRole(requester, group, role);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "isConcernedByAutoRequestRole (Agent " + requester + ", Group " + group
					+ ", Role " + role + ") : " + res);
		return res;
	}

	@Override
	void removeAllAutoRequestedGroups(AbstractAgent requester) {
		kernel.removeAllAutoRequestedGroups(requester);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "leaveAllAutoRequestedGroups (Agent " + requester + ")");
	}

	@Override
	void leaveAutoRequestedRole(AbstractAgent requester, String role) {
		kernel.leaveAutoRequestedRole(requester, role);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "leaveAutoRequestedRole (Agent " + requester + ", Role " + role + ") ");
	}

	@Override
	void leaveAutoRequestedRole(AbstractAgent requester, AbstractGroup group, String role) {
		kernel.leaveAutoRequestedRole(requester, group, role);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"leaveAutoRequestedRole (Agent " + requester + ", " + group + ", Role " + role + ") ");
	}

	@Override
	void leaveAutoRequestedGroup(AbstractAgent requester, AbstractGroup group) {
		kernel.leaveAutoRequestedGroup(requester, group);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"leaveAutoRequestedGroup (Agent " + requester + ", Group " + group + ") ");
	}

	@Override
	void autoRequestRole(AbstractAgent requester, AbstractGroup group, String role, SecureExternalizable passKey) {
		kernel.autoRequestRole(requester, group, role, passKey);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"autoRequestRole (Agent " + requester + ", Group " + group + ", Role " + role + ")");
	}

	@Override
	void manageDirectConnection(AbstractAgent requester, AskForConnectionMessage m, boolean addToNetworkProperties, boolean actOnlyIfModifyNetworkProperties) throws IllegalAccessException {
		kernel.manageDirectConnection(requester, m, addToNetworkProperties, actOnlyIfModifyNetworkProperties);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "manageDirectConnection (Agent " + requester + ", Type " + m.getType()
					+ ", IP " + m.getIP() + ", Port " + m.getIP().getPort() + ", addToNetworkProperties="+addToNetworkProperties+", actOnlyIfModifyNetworkProperties="+actOnlyIfModifyNetworkProperties+")");
	}
	@Override
	void manageDirectConnections(AbstractAgent requester, List<AskForConnectionMessage> lm, boolean addToNetworkProperties, boolean actOnlyIfModifyNetworkProperties) throws IllegalAccessException {
		kernel.manageDirectConnections(requester, lm, addToNetworkProperties, actOnlyIfModifyNetworkProperties);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "manageDirectConnection (Agent " + requester + ", listAskForConnectionMessage "+lm +", addToNetworkProperties="+addToNetworkProperties+", actOnlyIfModifyNetworkProperties="+actOnlyIfModifyNetworkProperties+")");
	}

	@Override
	public MadkitProperties getMadkitConfig() {
		return kernel.getMadkitConfig();
	}



		@Override
	void manageTransferConnection(AbstractAgent requester, AskForTransferMessage m) throws IllegalAccessException {
		kernel.manageTransferConnection(requester, m);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"manageTransferConnection (Agent " + requester + ", Type " + m.getType() + ", kernel address 1 "
							+ m.getKernelAddress2() + ", kernel address 2 " + m.getKernelAddress2()
							+ ", InetSocketAddress 1 " + m.getInetSocketAddress1() + ", InetSocketAddress 2 "
							+ m.getInetSocketAddress2() + ")");
	}

	@Override
	BigDataTransferID sendBigData(AbstractAgent requester, AgentAddress agentAddress, RandomInputStream stream,
			long pos, long length, SecureExternalizable attachedData, String senderRole, MessageDigestType messageDigestType, boolean excludeFromEncryption)
			throws IOException {
		BigDataTransferID res = kernel.sendBigData(requester, agentAddress, stream, pos, length, attachedData,
				senderRole, messageDigestType, excludeFromEncryption);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"sendBigData (Agent " + requester + ", agent address " + agentAddress + ", stream type "
							+ stream.getClass() + ", start position " + pos + ", length " + length + ", sender role "
							+ senderRole + ", messageDigestType=" + messageDigestType + ")");
		return res;
	}

	@Override
	void acceptDistantBigDataTransfer(AbstractAgent requester, BigDataPropositionMessage originalMessage) {
		kernel.acceptDistantBigDataTransfer(requester, originalMessage);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "acceptDistantBigDataTransfer (Agent " + requester + ", conversation ID "
					+ originalMessage.getConversationID() + ", ID Packet " + originalMessage.getIDPacket() + ")");

	}

	@Override
	void transferLostForBigDataTransfer(AbstractAgent requester, ConversationID conversationID, int idPacket,
										AgentAddress sender, AgentAddress receiver, long readDataLength, long durationInMs, AbstractDecentralizedIDGenerator asynchronousBigDataInternalIdentifier,
										ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier, BigDataResultMessage.Type cancelingType) {
		kernel.transferLostForBigDataTransfer(requester, conversationID, idPacket, sender, receiver, readDataLength,
				durationInMs, asynchronousBigDataInternalIdentifier, externalAsynchronousBigDataIdentifier, cancelingType);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"transferLostForBigDataTransfer (Agent " + requester + ", conversation ID " + conversationID
							+ ", ID Packet " + idPacket + ", sender " + sender + ", receiver " + receiver +", cancelingType="+cancelingType
							+ ", read data length " + readDataLength + ", duration (ms) " + durationInMs + ")");
	}


	@Override
	ReturnCode anomalyDetectedWithOneConnection(AbstractAgent requester, boolean candidateToBan,
			ConnectionIdentifier connection_identifier, String message) {
		ReturnCode rc = kernel.anomalyDetectedWithOneConnection(requester, candidateToBan, connection_identifier,
				message);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"anomalyDetectedWithOneConnection (Requester=" + requester + ", candidateToBan=" + true
							+ ", connectionIdentifier=" + connection_identifier + ", result=" + rc + ")");

		return rc;
	}

	@Override
	ReturnCode anomalyDetectedWithOneDistantKernel(AbstractAgent requester, boolean candidateToBan,
			KernelAddress kernelAddress, String message) {
		ReturnCode rc = kernel.anomalyDetectedWithOneDistantKernel(requester, candidateToBan, kernelAddress, message);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "anomalyDetectedWithOneDistantKernel (Requester=" + requester
					+ ", candidateToBan=" + true + ", kernelAddress=" + kernelAddress + ", result=" + rc + ")");

		return rc;
	}

	@Override
	Set<Connection> getEffectiveConnections(AbstractAgent requester) {
		Set<Connection> rc = kernel.getEffectiveConnections(requester);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"getEffectiveConnections (Requester=" + requester + ", result=" + rc + ")");

		return rc;
	}

	@Override
	Set<KernelAddress> getAvailableDistantKernels(AbstractAgent requester) {
		Set<KernelAddress> rc = kernel.getAvailableDistantKernels(requester);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"getAvailableDistantKernels (Requester=" + requester + ", result=" + rc + ")");

		return rc;
	}

	@Override
	Set<Group> getAccessibleGroupsGivenByDistantPeer(AbstractAgent requester, KernelAddress kernelAddress) {
		Set<Group> rc = kernel.getAccessibleGroupsGivenByDistantPeer(requester, kernelAddress);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "getAccessibleGroupsGivenByDistantPeer (Requester=" + requester
					+ ", distantKernelAddress=" + kernelAddress + ", result=" + rc + ")");

		return rc;
	}

	@Override
	Set<Group> getAccessibleGroupsGivenToDistantPeer(AbstractAgent requester, KernelAddress kernelAddress) {
		Set<Group> rc = kernel.getAccessibleGroupsGivenToDistantPeer(requester, kernelAddress);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "getAccessibleGroupsGivenToDistantPeer (Requester=" + requester
					+ ", distantKernelAddress=" + kernelAddress + ", result=" + rc + ")");

		return rc;
	}

	@Override
	Set<KernelAddress> getAccessibleKernelsPerGroupGivenByDistantPeer(AbstractAgent requester, Group group) {
		Set<KernelAddress> rc = kernel.getAccessibleKernelsPerGroupGivenByDistantPeer(requester, group);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "getAccessibleKernelsPerGroupGivenByDistantPeer (Requester=" + requester
					+ ", group=" + group + ", result=" + rc + ")");

		return rc;
	}

	@Override
	Set<KernelAddress> getAccessibleKernelsPerGroupGivenToDistantPeer(AbstractAgent requester, Group group) {
		Set<KernelAddress> rc = kernel.getAccessibleKernelsPerGroupGivenToDistantPeer(requester, group);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "getAccessibleKernelsPerGroupGivenToDistantPeer (Requester=" + requester
					+ ", group=" + group + ", result=" + rc + ")");

		return rc;
	}

	@Override
	Set<PairOfIdentifiers> getEffectiveDistantLogins(AbstractAgent requester, KernelAddress kernelAddress) {
		Set<PairOfIdentifiers> rc = kernel.getEffectiveDistantLogins(requester, kernelAddress);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "getEffectiveDistantLogins (Requester=" + requester
					+ ", distantKernelAddress=" + kernelAddress + ", result=" + rc + ")");

		return rc;
	}

	@Override
	ReturnCode requestHookEvents(AbstractAgent requester, AgentActionEvent hookType, boolean autoRemove) {
		ReturnCode rc = kernel.requestHookEvents(requester, hookType, autoRemove);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"requestHookEvents (Requester=" + requester + ", hookType=" + hookType + ")");

		return rc;
	}

	@Override
	ReturnCode releaseHookEvents(AbstractAgent requester, AgentActionEvent hookType) {
		ReturnCode rc = kernel.releaseHookEvents(requester, hookType);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"stopHookEvents (Requester=" + requester + ", hookType=" + hookType + ")");

		return rc;
	}

	@Override
	void wait(AbstractAgent requester, LockerCondition locker) throws InterruptedException {
		kernel.wait(requester, locker);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "wait (Requester=" + requester + ", locker=" + locker + ")");

	}

	@Override
	void wait(AbstractAgent requester, LockerCondition locker, Lock personalLocker, Condition personCondition) throws InterruptedException {
		kernel.wait(requester, locker, personalLocker, personCondition);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "wait (Requester=" + requester + ", locker=" + locker + ")");

	}

	@Override
	void wait(AbstractAgent requester, LockerCondition locker, Lock personalLocker, Condition personCondition, long time, TimeUnit unit) throws InterruptedException, TimeoutException {
		kernel.wait(requester, locker, personalLocker, personCondition, time, unit);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "wait (Requester=" + requester + ", locker=" + locker + ", time="+time+", unit="+unit+")");

	}

	@Override
	void wait(AbstractAgent requester, LockerCondition locker, long delayMillis) throws InterruptedException, TimeoutException {
		kernel.wait(requester, locker, delayMillis);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "wait (Requester=" + requester + ", locker=" + locker + ", delayMillis="+delayMillis+")");

	}

	@Override
	void regularWait(AbstractAgent requester, LockerCondition locker) throws InterruptedException {
		kernel.regularWait(requester, locker);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "regularWait (Requester=" + requester + ", locker=" + locker + ")");
	}

	@Override
	void sleep(AbstractAgent requester, long millis) throws InterruptedException {
		kernel.sleep(requester, millis);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "wait (Requester=" + requester + ", millis=" + millis + ")");
	}


	@Override
	ScheduledPoolExecutor getMaDKitServiceExecutor() {
		return kernel.getMaDKitServiceExecutor();
	}

	@Override
	Object weakSetBoard(AbstractAgent requester, Group group, String name, Object data) {
		Object res = kernel.weakSetBoard(requester, group, name, data);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "weakSetBoard (Requester=" + requester + ", group=" + group
					+ ", name=" + name + ", data=" + data + ", res=" + res + ")");

		return res;

	}

	@Override
	Object setBoard(AbstractAgent requester, Group group, String name, Object data) {
		Object res = kernel.setBoard(requester, group, name, data);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "setBoard (Requester=" + requester + ", group=" + group + ", name="
					+ name + ", data=" + data + ", res=" + res + ")");

		return res;

	}

	@Override
	Object getBoard(AbstractAgent requester, Group group, String name) {
		Object res = kernel.getBoard(requester, group, name);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "getBoard (Requester=" + requester + ", group=" + group + ", name="
					+ name + ", res=" + res + ")");

		return res;
	}

	@Override
	Object removeBoard(AbstractAgent requester, Group group, String name) {
		Object res = kernel.removeBoard(requester, group, name);

		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST, "removeBoard (Requester=" + requester + ", group=" + group
					+ ", name=" + name + ", res=" + res + ")");

		return res;

	}

	@Override
	boolean checkMemoryLeakAfterNetworkStopped() {
		boolean res = kernel.checkMemoryLeakAfterNetworkStopped();

		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "checkMemoryLeakAfterNetworkStopped (res=" + res + ")");

		return res;
	}

	@Override
	ReturnCode setCentralDatabaseBackupReceiverFactory(AbstractAgent requester, CentralDatabaseBackupReceiverFactory<?> centralDatabaseBackupReceiverFactory) throws DatabaseException {
		ReturnCode res = kernel.setCentralDatabaseBackupReceiverFactory(requester, centralDatabaseBackupReceiverFactory);

		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "setCentralDatabaseBackupReceiverFactory (requester="+requester+", centralDatabaseBackupReceiverFactory="+centralDatabaseBackupReceiverFactory+", res=" + res + ")");

		return res;
	}

	@Override
	int numberOfValidGeneratedID() {
		int res = kernel.numberOfValidGeneratedID();

		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "numberOfValidGeneratedID (res=" + res + ")");
		return res;
	}

	@Override
	IDGeneratorInt getIDTransferGenerator() {
		return kernel.getIDTransferGenerator();
	}

	@Override
	List<AbstractAgent> createBucket(final String agentClass, int bucketSize, int cpuCoreNb)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return kernel.createBucket(agentClass, bucketSize, cpuCoreNb);
	}

	@Override
	Map<KernelAddress, InterfacedIDs> getGlobalInterfacedIDs() {
		return kernel.getGlobalInterfacedIDs();
	}

	@Override
	boolean isGlobalInterfacedIDsEmpty() {
		return kernel.isGlobalInterfacedIDsEmpty();
	}

	@Override
	ArrayList<AbstractAgent> getConnectedNetworkAgents() {
		return kernel.getConnectedNetworkAgents();
	}

	@Override
	protected void exit() throws InterruptedException {
		kernel.exit();
	}

	@Override
	AsynchronousBigDataTransferID sendBigDataAndDifferItIfNecessary(AbstractAgent requester, Group group, final String role, String senderRole,
																	ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier,
																	SecureExternalizable attachedData,
																	MessageDigestType messageDigestType, boolean excludedFromEncryption, long timeOutInMs,
																	AsynchronousBigDataToSendWrapper asynchronousBigDataToSendWrapper)
	{
		AsynchronousBigDataTransferID r=kernel.sendBigDataAndDifferItIfNecessary(requester, group, role, senderRole,
				externalAsynchronousBigDataIdentifier, attachedData, messageDigestType, excludedFromEncryption, timeOutInMs,
				asynchronousBigDataToSendWrapper);
		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "sendBigDataAndDifferItIfNecessary (requester="+requester+", group="+group
					+", destinationRole="+role
					+", senderRole="+senderRole
					+", differedBidDataIdentifier="+ externalAsynchronousBigDataIdentifier
					+", messageDigestType="+messageDigestType
					+", excludedFromEncryption="+excludedFromEncryption
					+", timeOutInMs="+timeOutInMs
					+", res=" + r + ")");
		return r;
	}
	@Override
	ReturnCode cancelBigDataTransfer(AbstractAgent requester, BigDataTransferID bigDataTransferID)
	{
		ReturnCode r=kernel.cancelBigDataTransfer(requester, bigDataTransferID);
		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "cancelBigDataTransfer (requester="+requester+", bigDataTransferID="+bigDataTransferID
					+", res=" + r + ")");
		return r;
	}
	@Override
	BigDataTransferID sendAsynchronousBigData(AbstractAgent requester, AgentAddress senderAA, AgentAddress receiverAA,
											  AsynchronousBigDataTable.Record record)
			throws IOException {
		BigDataTransferID r=kernel.sendAsynchronousBigData(requester, senderAA, receiverAA, record);
		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "sendBigDataAndDifferItIfNecessary (requester="+requester+", senderAA="+senderAA
					+", receiverAA="+receiverAA
					+", res=" + r + ")");
		return r;
	}

	@Override
	boolean receivedPotentialAsynchronousBigDataResultMessage(AbstractAgent requester, BigDataResultMessage m)
	{
		boolean r=kernel.receivedPotentialAsynchronousBigDataResultMessage(requester, m);
		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "receivedPotentialAsynchronousBigDataResultMessage (requester="+requester+", message="+m
					+", res=" + r + ")");
		return r;
	}

	@Override
	void receivedBigDataToRestartMessage(AbstractAgent requester, BigDataToRestartMessage message)
	{
		kernel.receivedBigDataToRestartMessage(requester, message);
		if (requester.isFinestLogOn())
			requester.logger.log(Level.FINEST,
					"receivedBigDataToRestartMessage (Requester=" + requester + ", message=" + message+")");
	}

	@Override
	ReturnCode cancelAsynchronousBigData(AbstractAgent requester, AsynchronousBigDataTransferID asynchronousBigDataTransferID)
	{
		ReturnCode r=kernel.cancelAsynchronousBigData(requester, asynchronousBigDataTransferID);
		if (kernel.isFinestLogOn())
			kernel.logger.log(Level.FINEST, "cancelAsynchronousBigData (requester="+requester+", asynchronousBigDataTransferID="+asynchronousBigDataTransferID
					+", res=" + r + ")");
		return r;
	}

	@Override
	public AsynchronousBigDataTransferID getAsynchronousBigDataTransferIDInstance(AbstractAgent requester, ExternalAsynchronousBigDataIdentifier externalAsynchronousBigDataIdentifier)
	{
		return kernel.getAsynchronousBigDataTransferIDInstance(requester, externalAsynchronousBigDataIdentifier);
	}
}
