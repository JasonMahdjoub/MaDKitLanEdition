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

import com.distrimind.madkit.database.DifferedMessageTable;
import com.distrimind.madkit.i18n.ErrorMessages;
import com.distrimind.madkit.kernel.ConversationID.InterfacedIDs;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.access.PairOfIdentifiers;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.util.AbstractDecentralizedID;
import com.distrimind.util.IDGeneratorInt;
import com.distrimind.util.concurrent.LockerCondition;
import com.distrimind.util.crypto.MessageDigestType;
import com.distrimind.util.io.RandomInputStream;
import com.distrimind.util.io.SecureExternalizable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKitLanEdition 1.0
 * @version 1.4
 * 
 */
class FakeKernel extends MadkitKernel {

	///////////////////////////////////////////////////////////////////////////
	////////////////////////// Agent interface
	///////////////////////////////////////////////////////////////////////////

	//////////////////////////////////////////////////////////////
	////////////////////////// Organization interface
	//////////////////////////////////////////////////////////////

	String buildFailString(final AbstractAgent agent) {
		return (agent != null ? agent.toString() : "Agent" + AbstractAgent.State.NOT_LAUNCHED)
				+ ErrorMessages.MUST_BE_LAUNCHED;
	}

	@Override
	final ReturnCode createGroup(final AbstractAgent agent, Group group, Object passKey, boolean manually_created) {
		throw buildKernelException(agent);
	}

	@Override
	AgentThreadFactory getNormalAgentThreadFactory() {
		throw buildKernelException(null);
	}

	@Override
	AgentThreadFactory getDaemonAgentThreadFactory() {
		throw buildKernelException(null);
	}

	@Override
	AgentAddress getAgentAddressIn(AbstractAgent agent, Group group, String role) {
		throw buildKernelException(agent);
	}

	@Override
	final ReturnCode requestRole(AbstractAgent agent, Group group, String role, SecureExternalizable memberCard,
			boolean manual_request) {
		throw buildKernelException(agent);
	}

	@Override
	final ReturnCode leaveGroup(final AbstractAgent agent, Group group, boolean manually_requested) {
		throw buildKernelException(agent);
	}

	@Override
	final ReturnCode leaveRole(AbstractAgent agent, Group group, String role, boolean manualRequested) {
		throw buildKernelException(agent);
	}

	@Override
	final AgentAddress getAgentWithRole(final AbstractAgent agent, AbstractGroup group, final String role) {
		throw buildKernelException(agent);
	}

	@Override
	final Set<AgentAddress> getAgentsWithRole(AbstractAgent agent, AbstractGroup group, String role,
			boolean callerIncluded) {
		throw buildKernelException(agent);
	}

	@Override
	final boolean isCommunity(AbstractAgent agent, String community) {
		throw buildKernelException(agent);
	}

	@Override
	final boolean isGroup(AbstractAgent agent, Group group) {
		throw buildKernelException(agent);
	}

	@Override
	final boolean isRole(AbstractAgent agent, Group group, String role) {
		throw buildKernelException(agent);
	}

	@Override
	final public boolean isKernelOnline() {
		throw buildKernelException(null);
	}

	@Override
	final boolean isConcernedBy(AbstractAgent requester, AgentAddress agentAddress) {
		throw buildKernelException(requester);
	}

	@Override
	boolean isLocalAgentAddressValid(AbstractAgent requester, AgentAddress agentAddress) {
		throw buildKernelException(requester);
	}

	//////////////////////////////////////////////////////////////
	////////////////////////// Messaging interface
	//////////////////////////////////////////////////////////////

	@Override
	ReturnCode sendMessageAndDifferItIfNecessary(final AbstractAgent requester, Group group, final String role, final Message message,
												 final String senderRole) {
		throw buildKernelException(requester);

	}
	@Override
	long cancelDifferedMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole) {
		throw buildKernelException(requester);
	}
	@Override
	long cancelDifferedMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole) {
		throw buildKernelException(requester);
	}
	@Override
	long cancelDifferedMessagesByGroup(AbstractAgent requester, Group group) {
		throw buildKernelException(requester);

	}

	@Override
	List<DifferedMessageTable.Record> getDifferedMessagesBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		throw buildKernelException(requester);

	}
	@Override
	List<DifferedMessageTable.Record> getDifferedMessagesByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		throw buildKernelException(requester);

	}
	@Override
	List<DifferedMessageTable.Record> getDifferedMessagesByGroup(AbstractAgent requester, Group group)  {
		throw buildKernelException(requester);


	}
	@Override
	long getDifferedMessagesNumberBySenderRole(AbstractAgent requester, Group group, String senderRole)  {
		throw buildKernelException(requester);
	}
	@Override
	long getDifferedMessagesNumberByReceiverRole(AbstractAgent requester, Group group, String receiverRole)  {
		throw buildKernelException(requester);
	}
	@Override
	long getDifferedMessagesNumberByGroup(AbstractAgent requester, Group group)  {
		throw buildKernelException(requester);

	}

	@Override
	final ReturnCode sendMessage(final AbstractAgent agent, AbstractGroup group, final String role,
			final Message messageToSend, final String senderRole) {
		throw buildKernelException(agent);
	}

	@Override
	final ReturnCode sendMessage(AbstractAgent agent, AgentAddress receiver, final Message messageToSend,
			final String senderRole) {
		throw buildKernelException(agent);
	}

	@Override
	final ReturnCode broadcastMessageWithRole(final AbstractAgent agent, AbstractGroup group, final String role,
			final Message messageToSend, String senderRole, boolean sendAllRepliesInOneBlock) {
		throw buildKernelException(agent);
	}

	//////////////////////////////////////////////////////////////
	////////////////////////// Launching and Killing
	//////////////////////////////////////////////////////////////

	@Override
	final ReturnCode launchAgent(final AbstractAgent agent, final AbstractAgent agent2, final int timeOutSeconds,
			final boolean defaultGUI) {
		throw buildKernelException(agent);
	}

	@Override
	void launchAgentBucketWithRoles(AbstractAgent requester, List<AbstractAgent> bucket, int cpuCoreNb,
			Role... CGRLocations) {
		throw buildKernelException(requester);
	}

	@Override
	final ReturnCode killAgent(final AbstractAgent agent, final AbstractAgent target, int timeOutSeconds,
			KillingType killing_type) {
		throw buildKernelException(agent);
	}

	private KernelException buildKernelException(final AbstractAgent agent) {
		final KernelException ke = new KernelException(buildFailString(agent));
		ke.printStackTrace();
		return ke;
	}

	@Override
	final synchronized boolean removeOverlooker(final AbstractAgent agent, Overlooker<? extends AbstractAgent> o) {
		throw buildKernelException(agent);
	}

	@Override
	final synchronized boolean addOverlooker(AbstractAgent agent, Overlooker<? extends AbstractAgent> o) {
		throw buildKernelException(agent);
	}


	@Override
	final public KernelAddress getKernelAddress() {
		throw buildKernelException(null);
	}

	@Override
	public AgentLogger getLogger() {
		throw buildKernelException(null);
	}

	@Override
	final public MadkitProperties getMadkitConfig() {
		return Madkit.getDefaultConfig();
	}

	@Override
	final List<Message> broadcastMessageWithRoleAndWaitForReplies(AbstractAgent agent, AbstractGroup group, String role,
			Message message, String senderRole, Integer timeOutMilliSeconds) {
		throw buildKernelException(agent);
	}

	/*
	 * @Override ScheduledThreadPoolExecutor
	 * killScheduledExecutorService(AbstractAgent requester, String name) { throw
	 * buildKernelException(requester); }
	 * 
	 * @Override ScheduledThreadPoolExecutor
	 * getScheduledExecutorService(AbstractAgent requester, String name) { throw
	 * buildKernelException(requester); }
	 */

	@Override
	TaskID scheduleTask(AbstractAgent requester, Task<?> _task, boolean ask_for_execution_confirmation) {
		throw buildKernelException(requester);

	}

	/*
	 * @Override TaskID scheduleTask(AbstractAgent requester, String
	 * _task_agent_name, Task<?> _task, boolean ask_for_execution_confirmation) {
	 * throw buildKernelException(requester); }
	 */

	/*
	 * @Override ConversationID scheduleTasks(AbstractAgent requester,
	 * Collection<Task<?>> _tasks, boolean ask_for_execution_confirmation) {
	 * ConversationID res=kernel.scheduleTasks(requester, _tasks,
	 * ask_for_execution_confirmation); if (requester.isFinestLogOn())
	 * requester.logger.log(Level.FINEST,"Scheduling a collection of tasks "+res+(
	 * ask_for_execution_confirmation?"with message confirmation":"")
	 * +" and with default task manager agent : "+(res==null?"FAIL":"OK")); return
	 * res; }
	 * 
	 * @Override ConversationID scheduleTasks(AbstractAgent requester, String
	 * _task_agent_name, Collection<Task<?>> _tasks, boolean
	 * ask_for_execution_confirmation) { ConversationID
	 * res=kernel.scheduleTasks(requester, _task_agent_name, _tasks,
	 * ask_for_execution_confirmation); if (requester.isFinestLogOn())
	 * requester.logger.log(Level.FINEST,"Scheduling a collection of tasks "+res+(
	 * ask_for_execution_confirmation?"with message confirmation":"")
	 * +" and with task manager agent named "+_task_agent_name+" : "+(res==null?
	 * "FAIL":"OK")); return res; }
	 */

	@Override
	boolean cancelTask(AbstractAgent requester, TaskID task_id, boolean mayInterruptTask) {
		throw buildKernelException(requester);
	}

	/*
	 * @Override ReturnCode cancelTask(AbstractAgent requester, String
	 * _task_agent_name, ConversationID task_id) { ReturnCode rc=kernel.
	 * cancelTask(requester, _task_agent_name, task_id); if
	 * (requester.isFinestLogOn())
	 * requester.logger.log(Level.FINEST,"Canceling task "
	 * +task_id+" with task manager agent named "+_task_agent_name+" : "+rc); return
	 * rc; }
	 */

	/*
	 * @Override AgentAddress getDefaultTaskAgent(AbstractAgent requester) {
	 * AgentAddress aa=kernel.getDefaultTaskAgent(requester); if
	 * (requester.isFinestLogOn())
	 * requester.logger.log(Level.FINEST,"get default task agent : "+aa); return aa;
	 * }
	 */

	/*
	 * @Override ScheduledThreadPoolExecutor
	 * launchAndOrGetScheduledExecutorService(AbstractAgent requester, String name,
	 * int maximumPoolSize, int priority, long timeOutSeconds) { throw
	 * buildKernelException(requester); }
	 * 
	 * 
	 * @Override ScheduledThreadPoolExecutor
	 * launchAndOrGetScheduledExecutorService(AbstractAgent requester, String
	 * _task_agent_name) { throw buildKernelException(requester); }
	 */

	@Override
	boolean isConcernedByAutoRequestRole(AbstractAgent requester, Group group, String role) {
		throw buildKernelException(requester);
	}

	@Override
	void removeAllAutoRequestedGroups(AbstractAgent requester) {
		throw buildKernelException(requester);
	}

	@Override
	void leaveAutoRequestedRole(AbstractAgent requester, AbstractGroup group, String role) {
		throw buildKernelException(requester);
	}

	@Override
	void leaveAutoRequestedRole(AbstractAgent requester, String role) {
		throw buildKernelException(requester);
	}

	@Override
	void leaveAutoRequestedGroup(AbstractAgent requester, AbstractGroup group) {
		throw buildKernelException(requester);
	}

	@Override
	void autoRequestRole(AbstractAgent requester, AbstractGroup group, String role, SecureExternalizable passKey) {
		throw buildKernelException(requester);
	}

	@Override
	void manageDirectConnection(AbstractAgent requester, AskForConnectionMessage m, boolean addToNetworkProperties, boolean actOnlyIfModifyNetworkProperties) {
		throw buildKernelException(requester);
	}
	@Override
	void manageDirectConnections(AbstractAgent requester, List<AskForConnectionMessage> lm, boolean addToNetworkProperties, boolean actOnlyIfModifyNetworkProperties) {
		throw buildKernelException(requester);
	}

	@Override
	void manageTransferConnection(AbstractAgent requester, AskForTransferMessage m) {
		throw buildKernelException(requester);
	}

	@Override
	BigDataTransferID sendBigData(AbstractAgent requester, AgentAddress agentAddress, RandomInputStream stream,
			long pos, long length, SecureExternalizable attachedData, String senderRole, MessageDigestType messageDigestType, boolean excludeFromEncryption) {
		throw buildKernelException(requester);
	}

	@Override
	void acceptDistantBigDataTransfer(AbstractAgent requester, BigDataPropositionMessage originalMessage) {
		throw buildKernelException(requester);

	}

	@Override
	void connectionLostForBigDataTransfer(AbstractAgent requester, ConversationID conversationID, int idPacket,
			AgentAddress sender, AgentAddress receiver, long readDataLength, long duration, AbstractDecentralizedID differedBigDataInternalIdentifier,
										  DifferedBigDataIdentifier differedBigDataIdentifier) {
		throw buildKernelException(requester);
	}

	@Override
	ReturnCode anomalyDetectedWithOneConnection(AbstractAgent requester, boolean candidateToBan,
			ConnectionIdentifier connection_identifier, String message) {
		throw buildKernelException(requester);
	}

	@Override
	ReturnCode anomalyDetectedWithOneDistantKernel(AbstractAgent requester, boolean candidateToBan,
			KernelAddress kernelAddress, String message) {
		throw buildKernelException(requester);
	}

	@Override
	Set<Connection> getEffectiveConnections(AbstractAgent requester) {
		throw buildKernelException(requester);
	}

	@Override
	Set<KernelAddress> getAvailableDistantKernels(AbstractAgent requester) {
		throw buildKernelException(requester);
	}

	@Override
	Set<Group> getAccessibleGroupsGivenByDistantPeer(AbstractAgent requester, KernelAddress kernelAddress) {
		throw buildKernelException(requester);
	}

	@Override
	Set<KernelAddress> getAccessibleKernelsPerGroupGivenByDistantPeer(AbstractAgent requester, Group group) {
		throw buildKernelException(requester);
	}

	@Override
	Set<KernelAddress> getAccessibleKernelsPerGroupGivenToDistantPeer(AbstractAgent requester, Group group) {
		throw buildKernelException(requester);
	}

	@Override
	Set<Group> getAccessibleGroupsGivenToDistantPeer(AbstractAgent requester, KernelAddress kernelAddress) {
		throw buildKernelException(requester);
	}

	@Override
	Set<PairOfIdentifiers> getEffectiveDistantLogins(AbstractAgent requester, KernelAddress kernelAddress) {
		throw buildKernelException(requester);
	}

	@Override
	ReturnCode requestHookEvents(AbstractAgent requester, AgentActionEvent hookType, boolean autoRemove) {
		throw buildKernelException(requester);
	}

	@Override
	ReturnCode releaseHookEvents(AbstractAgent requester, AgentActionEvent hookType) {
		throw buildKernelException(requester);
	}

	@Override
	void receivingPotentialNetworkMessage(AbstractAgent requester, LocalLanMessage m) {
		throw buildKernelException(requester);
	}

	@Override
	void waitMessageSent(AbstractAgent requester, LockerCondition locker) {
		throw buildKernelException(requester);
	}

	@Override
	Object weakSetBoard(AbstractAgent requester, Group group, String name, Object data) {
		throw buildKernelException(requester);
	}

	@Override
	Object setBoard(AbstractAgent requester, Group group, String name, Object data) {
		throw buildKernelException(requester);
	}

	@Override
	Object getBoard(AbstractAgent requester, Group group, String name) {
		throw buildKernelException(requester);
	}

	@Override
	Object removeBoard(AbstractAgent requester, Group group, String name) {
		throw buildKernelException(requester);
	}

	@Override
	int numberOfValidGeneratedID() {
		throw buildKernelException(null);
	}

	@Override
	IDGeneratorInt getIDTransferGenerator() {
		throw buildKernelException(null);
	}

	@Override
	ReturnCode setCentralDatabaseBackupReceiverFactory(AbstractAgent ignored, CentralDatabaseBackupReceiverFactory<?> centralDatabaseBackupReceiverFactory) {
		throw buildKernelException(null);
	}

	/*@Override
	void setIfNotPresentLocalDatabaseHostIdentifier(AbstractAgent requester, DecentralizedValue localDatabaseHostID, Package ...packages)
	{
		throw buildKernelException(null);
	}

	@Override
	void resetDatabaseSynchronizer(AbstractAgent requester) {
		throw buildKernelException(null);
	}

	@Override
	void addOrConfigureDistantDatabaseHost(AbstractAgent requester, DecentralizedValue hostIdentifier, boolean conflictualRecordsReplacedByDistantRecords, Package... packages) {
		throw buildKernelException(null);
	}
	@Override
	void removeDistantDatabaseHostFromDatabaseSynchronizer(AbstractAgent requester, DecentralizedValue hostIdentifier, Package... packages)  {
		throw buildKernelException(null);
	}*/

	/*@Override
	<V> V take(BlockingDeque<V> toTake) {
		throw buildKernelException(null);
	}*/

	@Override
	List<AbstractAgent> createBucket(final String agentClass, int bucketSize, int cpuCoreNb) {
		throw buildKernelException(null);
	}

	@Override
	Map<KernelAddress, InterfacedIDs> getGlobalInterfacedIDs() {
		throw buildKernelException(null);
	}

	@Override
	boolean isGlobalInterfacedIDsEmpty() {
		throw buildKernelException(null);
	}

	@Override
	ArrayList<AbstractAgent> getConnectedNetworkAgents() {
		throw buildKernelException(null);
	}

}
