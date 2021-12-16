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

import com.distrimind.madkit.action.KernelAction;
import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.agr.LocalCommunity.Roles;
import com.distrimind.madkit.gui.AgentStatusPanel;
import com.distrimind.madkit.kernel.network.*;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionClosedReason;
import com.distrimind.madkit.message.EnumMessage;
import com.distrimind.madkit.message.KernelMessage;
import com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupReceiver;
import com.distrimind.ood.database.exceptions.DatabaseException;

import java.util.HashMap;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @author Fabien Michel
 * @version 2.0
 * @since MaDKitLanEdition 1.0
 *
 */
@SuppressWarnings("UnusedReturnValue")
public final class NetworkAgent extends AgentFakeThread {

	private AgentAddress NIOAgentAddress = null, LocalNetworkAffectationAgentAddress = null;
	private final HashMap<ConversationID, MessageLocker> messageLockers = new HashMap<>();
	private DatabaseSynchronizerAgent databaseSynchronizerAgent=null;
	private CentralDatabaseBackupReceiverAgent centralDatabaseBackupReceiverAgent=null;
	public static final String REFRESH_GROUPS_ACCESS="REFRESH_GROUPS_ACCESS";

	public NetworkAgent() {

	}

	@Override
	protected void activate() {
		setLogLevel(getMadkitConfig().networkProperties.networkLogLevel);
		setName(super.getName() + getKernelAddress());
		requestRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NET_AGENT);
		requestRole(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS, LocalCommunity.Roles.NET_AGENT);

		requestRole(LocalCommunity.Groups.DATABASE, LocalCommunity.Roles.NET_AGENT);
		// build servers
		weakSetBoard(LocalCommunity.Groups.NETWORK, LocalCommunity.Boards.NETWORK_BOARD,
				MadkitNetworkAccess.getNetworkBoard());
		if (getMadkitConfig().networkProperties.network)
			launchNetwork();

	}

	private void checkCentralDatabaseBackupReceiverAgent() {

		try {
			CentralDatabaseBackupReceiver centralDatabaseBackupReceiver = getMadkitConfig().getCentralDatabaseBackupReceiver();
			if (centralDatabaseBackupReceiverAgent==null) {
				if (centralDatabaseBackupReceiver != null) {
					centralDatabaseBackupReceiverAgent=new CentralDatabaseBackupReceiverAgent();
					if (!launchAgent(centralDatabaseBackupReceiverAgent).equals(ReturnCode.SUCCESS))
					{
						centralDatabaseBackupReceiverAgent=null;
						getLogger().warning("Unable to launch central database backup receiver agent");
					}
				}
			}
			else
			{
				if (centralDatabaseBackupReceiver == null) {
					stopCentralDatabaseBackupReceiverAgent();
				}
			}
		} catch (DatabaseException e) {
			getLogger().severeLog("Problem with database", e);
		}

	}

	private void stopCentralDatabaseBackupReceiverAgent() {
		if (centralDatabaseBackupReceiverAgent!=null) {
			if (centralDatabaseBackupReceiverAgent.isAlive()) {
				if (!killAgent(centralDatabaseBackupReceiverAgent).equals(ReturnCode.SUCCESS))
					getLogger().warning("Unable to kill Database synchronizer agent");
				else
					centralDatabaseBackupReceiverAgent = null;
			}
			else
				centralDatabaseBackupReceiverAgent=null;
		}
	}


	private void launchDatabaseSynchronizerAgent() {
		try {
			if (databaseSynchronizerAgent==null && getMadkitConfig().getDatabaseWrapper()!=null )
			{
				databaseSynchronizerAgent=new DatabaseSynchronizerAgent();
				if (!launchAgent(databaseSynchronizerAgent).equals(ReturnCode.SUCCESS))
				{
					databaseSynchronizerAgent=null;
					getLogger().warning("Unable to launch Database synchronizer agent");
				}
			}
		} catch (DatabaseException e) {
			getLogger().severeLog("Problem with database", e);
		}
	}

	private void stopDatabaseSynchronizerAgent()
	{
		if (databaseSynchronizerAgent!=null)
		{
			if (databaseSynchronizerAgent.isAlive())
			{
				if (!killAgent(databaseSynchronizerAgent).equals(ReturnCode.SUCCESS))
					getLogger().warning("Unable to kill Database synchronizer agent");
				else
					databaseSynchronizerAgent=null;
			}
			else
				databaseSynchronizerAgent=null;
		}
	}

	/**
	 * @return true if servers are launched
	 */
	private boolean launchNetwork() {
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Launching network agent in " + getKernelAddress() + "...");
		launchDatabaseSynchronizerAgent();
		checkCentralDatabaseBackupReceiverAgent();

		if (getMadkitConfig().networkProperties.upnpIGDEnabled
				|| getMadkitConfig().networkProperties.networkInterfaceScan) {
			AbstractAgent aa = MadkitNetworkAccess.getUpnpIDGAgent(this);
			if (aa==null)
				throw new NullPointerException();

			if (aa.getState() == State.NOT_LAUNCHED)
				launchAgent(aa);
		}
		AbstractAgent aa = MadkitNetworkAccess.getNIOAgent(this);
		launchAgent(aa);
		assert aa != null;
		NIOAgentAddress = aa.getAgentAddressIn(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NIO_ROLE);
		aa = MadkitNetworkAccess.getLocalNetworkAffectationAgent(this);
		launchAgent(aa);
		assert aa != null;
		LocalNetworkAffectationAgentAddress = aa.getAgentAddressIn(LocalCommunity.Groups.NETWORK,
				LocalCommunity.Roles.LOCAL_NETWORK_AFFECTATION_ROLE);
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("NetworkAgent in " + getKernelAddress() + " LAUNCHED !");
		return true;
	}

	@Override
	protected void liveByStep(Message _message) {
		handleMessage(_message);
	}

	@Override
	protected void end() {
		Message message;
		while ((message=nextMessage())!=null)
			receiveMessage(message);
		stopNetwork();
		messageLockers.values().forEach(ml ->  {
			if (ml != null) {
				try {

					ml.unlock();
				} catch (Exception e) {
					if (logger != null)
						logger.severeLog("Unexpected exception", e);
				}
			}
		});
		messageLockers.clear();
		removeBoard(LocalCommunity.Groups.NETWORK, LocalCommunity.Boards.NETWORK_BOARD);
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("NetworkAgent in " + getKernelAddress() + " KILLED !");
	}

	public void stopNetwork() {
		if (LocalNetworkAffectationAgentAddress != null && NIOAgentAddress != null) {
			stopDatabaseSynchronizerAgent();
			stopCentralDatabaseBackupReceiverAgent();

			broadcastMessage(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.TRANSFER_AGENT_ROLE,
					new StopNetworkMessage(NetworkCloseReason.NORMAL_DETECTION));
			sendMessage(LocalNetworkAffectationAgentAddress,
					new StopNetworkMessage(NetworkCloseReason.NORMAL_DETECTION));
			sendMessage(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NIO_ROLE, new StopNetworkMessage(NetworkCloseReason.NORMAL_DETECTION));
			this.broadcastMessageWithRole(LocalCommunity.Groups.NETWORK,
					LocalCommunity.Roles.LOCAL_NETWORK_EXPLORER_ROLE, new KernelMessage(KernelAction.STOP_NETWORK),
					LocalCommunity.Roles.NET_AGENT);
			LocalNetworkAffectationAgentAddress = null;
			NIOAgentAddress = null;
			if (logger != null) {
				logger.info("\n\t\t\t\t----- " + getKernelAddress() + " closing network ------\n");
			}
			// leaveGroup(CloudCommunity.Groups.NETWORK_AGENTS);
			AgentStatusPanel.updateAll();
		}
		if (this.getState().compareTo(State.ENDING) < 0)
			this.killAgent(this);
	}

	public static class StopNetworkMessage extends Message {

		private final NetworkCloseReason reason;

		StopNetworkMessage(NetworkCloseReason reason) {
			this.reason = reason;
		}

		public NetworkCloseReason getNetworkCloseReason() {
			return reason;
		}
	}

	public enum NetworkCloseReason {
		NORMAL_DETECTION(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED), ANOMALY_DETECTED(
				ConnectionClosedReason.CONNECTION_ANOMALY);

		private final ConnectionClosedReason reason;

		NetworkCloseReason(ConnectionClosedReason reason) {
			this.reason = reason;
		}

		public ConnectionClosedReason getConnectionClosedReason() {
			return reason;
		}

	}

	private void handleMessage(final Message m) throws ClassCastException {

		if (m.getClass() == Replies.class) {
			MessageLocker ml = messageLockers.remove(((Replies) m).getOriginalMessage().getConversationID());
			if (ml != null) {
				try {

					ml.unlock();
				} catch (Exception e) {
					if (logger != null)
						logger.severeLog("Unexpected exception", e);
				}
			}
			return;
		}
		final AgentAddress sender = m.getSender();
		if (sender == null) {// contacted by my private objects (or by the kernel ? no)
			proceedEnumMessage((EnumMessage<?>) m);
		} else if (sender.isFrom(getKernelAddress())) {// contacted locally
			switch (sender.getRole()) {
				case Roles.CENTRAL_DATABASE_BACKUP_CHECKER:
				{
					stopCentralDatabaseBackupReceiverAgent();
					checkCentralDatabaseBackupReceiverAgent();
				}
				case Roles.UPDATER:// It is a CGR update
				{
					broadcastMessage(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE, m, false/*ml != null*/);

				}
					break;
				case Roles.SECURITY:// It is a security problem
					if (m.getClass() == AnomalyDetectedMessage.class) {
						AnomalyDetectedMessage a = ((AnomalyDetectedMessage) m);
						if (a.getKernelAddress() != null)
							broadcastMessage(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
									LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE, a, false);
						else if (a.getInetSocketAddress() != null)
							broadcastMessage(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
									LocalCommunity.Roles.SOCKET_AGENT_ROLE, a, false);
					}
					break;
				case Roles.EMITTER:// It is a message to send elsewhere
				{
					MessageLocker ml = null;
					if (m instanceof LocalLanMessage) {
						ml = ((LocalLanMessage) m).getMessageLocker();
						ml.lock();

					}

					ReturnCode rc = broadcastMessage(LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE, m, ml != null);
					if (ml != null)
					{
						if (rc.equals(ReturnCode.SUCCESS)) {
							messageLockers.put(m.getConversationID(), ml);
						} else  {
							ml.cancelLock();
						}
					}
					break;
				}
				case Roles.KERNEL:// message from the kernel
					if (m instanceof EnumMessage)
						proceedEnumMessage((EnumMessage<?>) m);
					else if (m.getClass()==MadkitNetworkAccess.PauseBigDataTransferMessageClass)
					{
						broadcastMessage(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NIO_ROLE, m);
						broadcastMessage(LocalCommunity.Groups.NETWORK, Roles.DISTANT_KERNEL_AGENT_ROLE, m);
					}
					/*else if (m instanceof ObjectMessage)
					{
						if (((ObjectMessage<?>) m).getContent() instanceof MadkitKernel.InternalDatabaseSynchronizerEvent)
						{
							MadkitKernel.InternalDatabaseSynchronizerEvent e= (MadkitKernel.InternalDatabaseSynchronizerEvent)((ObjectMessage<?>) m).getContent();
							boolean updateGroupAccess=false;

							try {
								DatabaseWrapper dw = getMadkitConfig().getDatabaseWrapper();
								switch (e.type)
								{

									case SET_LOCAL_IDENTIFIER:

										if (dw != null){
											if (dw.getSynchronizer().getLocalHostID()!=null) {
												updateGroupAccess=true;
											}
											else
												stopDatabaseSynchronizerAgent();
										}
										getMadkitConfig().setLocalDatabaseHostID((DecentralizedValue) e.parameters[0], (Package[])e.parameters[1]);
										launchDatabaseSynchronizerAgent();
										break;
									case RESET_SYNCHRONIZER:

										if (dw!=null && dw.getSynchronizer().getLocalHostID()!=null) {
											updateGroupAccess = true;
										}
										stopDatabaseSynchronizerAgent();
										getMadkitConfig().resetDatabaseSynchronizerAndRemoveAllDatabaseHosts();
										break;
									case ASSOCIATE_DISTANT_DATABASE_HOST:
										if (databaseSynchronizerAgent!=null && databaseSynchronizerAgent.isAlive())
										{

											databaseSynchronizerAgent.receiveMessage(m);
										}
										else {
											getMadkitConfig().differDistantDatabaseHostConfiguration((DecentralizedValue) e.parameters[0], (boolean) e.parameters[1], (Package[]) e.parameters[2]);
										}
										break;
									case DISSOCIATE_DISTANT_DATABASE_HOST:
										if (databaseSynchronizerAgent!=null && databaseSynchronizerAgent.isAlive())
										{


											databaseSynchronizerAgent.receiveMessage(m);
										}
										else
											getMadkitConfig().removeDistantDatabaseHost((DecentralizedValue) e.parameters[0], (Package[])e.parameters[1]);
										break;
								}
								if (updateGroupAccess)
								{
									DatabaseSynchronizerAgent.updateGroupAccess(this);
								}
							} catch (DatabaseException | IOException ex) {
								getLogger().severeLog("Unable to apply database event "+e.type, ex);
							}
						}
						else
							handleNotUnderstoodMessage(m);
					}*/
					else
						handleNotUnderstoodMessage(m);
					break;
				case LocalCommunity.Roles.SOCKET_AGENT_ROLE:
					if (m instanceof CGRSynchro) {
						kernel.getMadkitKernel().injectOperation((CGRSynchro) m);
					} else if (m instanceof CGRSynchros) {
						kernel.getMadkitKernel().importDistantOrg((CGRSynchros) m);
					}
					break;
				case LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE:
					if (m instanceof DirectLocalLanMessage) {
						getMadkitKernel().injectMessage((DirectLocalLanMessage) m);
					} else if (m instanceof BroadcastLocalLanMessage) {
						getMadkitKernel().injectMessage((BroadcastLocalLanMessage) m);
					} else
						handleNotUnderstoodMessage(m);
					break;
				default:
					handleNotUnderstoodMessage(m);
					break;
			}
		}
	}


	private void handleNotUnderstoodMessage(Message m) {
		if (logger != null)
			logger.severeLog("not understood :\n" + m);
	}

	@SuppressWarnings("unused")
	private void exit() {
		this.killAgent(this);
	}

	@Override
	public void manageDirectConnection(final AskForConnectionMessage message) {
		if (message == null)
			throw new NullPointerException("message");
		sendMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.LOCAL_NETWORK_AFFECTATION_ROLE, message,
				LocalCommunity.Roles.NET_AGENT);
	}

	@Override
	public void manageTransferConnection(final AskForTransferMessage message) {
		if (message == null)
			throw new NullPointerException("message");
		sendMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.LOCAL_NETWORK_AFFECTATION_ROLE, message,
				LocalCommunity.Roles.NET_AGENT);
	}

}