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
package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.database.IPBanStat;
import com.distrimind.madkit.database.IPBanned;
import com.distrimind.madkit.exceptions.*;
import com.distrimind.madkit.i18n.AgentSocketMessage;
import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.AskForTransferMessage.InitiateTransferConnection;
import com.distrimind.madkit.kernel.network.DistantKernelAgent.*;
import com.distrimind.madkit.kernel.network.LocalNetworkAgent.PossibleInetAddressesUsedForDirectConnectionChanged;
import com.distrimind.madkit.kernel.network.TransferAgent.*;
import com.distrimind.madkit.kernel.network.connection.*;
import com.distrimind.madkit.kernel.network.connection.ConnectionProtocol.ConnectionClosedReason;
import com.distrimind.madkit.kernel.network.connection.access.*;
import com.distrimind.madkit.kernel.network.connection.access.AccessException;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.message.hook.HookMessage.AgentActionEvent;
import com.distrimind.madkit.message.hook.*;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.Timer;
import com.distrimind.util.crypto.AbstractSecureRandom;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.2
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings("UnusedReturnValue")
abstract class AbstractAgentSocket extends AgentFakeThread implements AccessGroupsNotifier {
	
	enum State {
		CONNECTION_IN_PROGRESS, CONNECTED_INITIALIZING_ACCESS, CONNECTED, DISCONNECTION_IN_PROGRESS, DISCONNECTION
	}

	protected final AbstractIP distantIP;
	protected final InetSocketAddress distant_inet_address;
	protected final InetSocketAddress local_interface_address;
	// protected final ArrayList<ConnectionProtocol> connection_protocols;
	protected ConnectionProtocol<?> connection_protocol;
	protected State state = State.CONNECTION_IN_PROGRESS;
	protected ConnectionClosedReason connection_closed_reason = null;

	protected int max_buffer_size;
	protected int max_block_size;

	protected final boolean this_ask_connection;
	protected AbstractAccessProtocol access_protocol;
	protected final AgentAddress nio_agent_address;
	protected final SocketChannel socket;
	private final DataSocketSynchronizer dataSynchronizer = new DataSocketSynchronizer();
	final AtomicBoolean distantKernelAddressValidated = new AtomicBoolean(false);
	private boolean distant_kernel_agent_activated = false;
	private AgentAddress distant_socket_agent_address = null;
	protected volatile boolean waitingPongMessage = false;

	private final DataSocketSynchronizer.SocketAgentInterface dataSynchronized = new DataSocketSynchronizer.SocketAgentInterface() {

		@Override
		public void receivedBlock(final Block block)
		{
			receivedBlock(block, true);
		}
		
		
		private void receivedBlock(Block block, boolean firstTime) {
			try {
				try {
					InterfacedIDTransfer idt;
					try
					{
						idt = routesData(block);
					}
					catch(RouterException e)
					{
						if (firstTime)
						{
							final Block f=block;
							scheduleTask(new Task<Void>(System.currentTimeMillis() + 400) {
								@Override
								public Void call() {
									if (isAlive())
									{
										receivedBlock(f, false);
									}
									return null;
								}
							});
						}
						else
						{
							processInvalidBlock(e, null, false);
						}
						return;
					}
					if (idt == null)
						receiveData(block);
					else {
						block.setTransferID(idt.getLocalID().getID());
						if (idt.getTransferToAgentAddress() != null) {

							if (idt.getTransferBlockChecker() == null) {
								if (idt.getLastPointToPointTransferredBlockChecker()!=null)
								{
									//CounterSelector.State counterState=block.getCounterState();
									
									SubBlockInfo sbi = idt.getLastPointToPointTransferredBlockChecker()
											.recursiveCheckSubBlock(new SubBlock(block));
									
									if (sbi.isValid()) {
										block=new Block(sbi.getSubBlock().getBytes());
										//block.setCounterState(counterState);

									} else {
										processInvalidBlock(
												new RouterException("Invalid block with transfer block checker "
														+ idt.getTransferBlockChecker() + " !"),
												block, sbi.isCandidateToBan());
									}
								}
								receiveIndirectData(block, idt);
							} else {
								//CounterSelector.State counterState=block.getCounterState();
								SubBlockInfo sbi = idt.getTransferBlockChecker()
										.recursiveCheckSubBlock(new SubBlock(block));

								if (sbi.isValid()) {
									Block b=new Block(sbi.getSubBlock().getBytes());
									//block.setCounterState(counterState);
									receiveDataToResend(b, idt);
									
								} else {
									processInvalidBlock(
											new RouterException("Invalid block with transfer block checker "
													+ idt.getTransferBlockChecker() + " !"),
											block, sbi.isCandidateToBan());
								}
							}
						} else {
							throw new RouterException("Unexpected exception");
						}
					}
				} catch (RouterException e) {
					processInvalidBlock(e, block, false);
				}
			} catch (Exception e) {
				processInvalidBlock(e, block, false);
			}

		}

		@Override
		public boolean processInvalidBlock(Exception _e, Block _block, boolean _candidate_to_ban) {
			return AbstractAgentSocket.this.processInvalidBlock(_e, _block, _candidate_to_ban);
		}

		@Override
		public boolean isBannedOrDefinitelyRejected() {
			return AbstractAgentSocket.this.isBannedOrDefinitelyRejected();
		}
	};
	AgentAddress agent_for_distant_kernel_aa;
	private boolean need_random;
	private final AbstractSecureRandom random;
	final AtomicReference<HashMap<AgentAddress, SecretMessage>> currentSecretMessages = new AtomicReference<>(null);
	private final AtomicBoolean exceededDataQueueSize = new AtomicBoolean(false);
	protected KernelAddressInterfaced distantInterfacedKernelAddress = null;
	protected final AtomicLong dataToTransferInQueue = new AtomicLong(0);
	private ConnectionInfoSystemMessage distantConnectionInfo = null;
	protected final TransferIDs transfer_ids = new TransferIDs();
	protected final TransferIDs transfer_ids_to_finalize = new TransferIDs();
	private TaskID taskTransferNodeChecker = null;
	private final AtomicReference<StatsBandwidth> stats = new AtomicReference<>(null);
	private boolean isBanned = false;
	protected volatile long lastDistKernADataToUpgradeMessageSentNano = Long.MIN_VALUE;
	protected volatile long lastReceivedDataNano = Long.MIN_VALUE;

	public AbstractAgentSocket(AbstractIP distantIP, AgentAddress agent_for_distant_kernel_aa, SocketChannel _socket,
			AgentAddress _nio_agent_address, InetSocketAddress _distant_inet_address,
			InetSocketAddress _local_interface_address, boolean _this_ask_connection) {
		super();

		if (distantIP == null)
			throw new NullPointerException("distantIP");
		if (_socket == null)
			throw new NullPointerException("_socket");
		if (_nio_agent_address == null)
			throw new NullPointerException("_nio_agent_address");
		if (_distant_inet_address == null)
			throw new NullPointerException("_distant_inet_address");
		if (_local_interface_address == null)
			throw new NullPointerException("_local_interface_address");
		
		distant_inet_address = _distant_inet_address;
		local_interface_address = _local_interface_address;
		this_ask_connection = _this_ask_connection;
		nio_agent_address = _nio_agent_address;
		socket = _socket;
		this.agent_for_distant_kernel_aa = agent_for_distant_kernel_aa;
		this.distantIP = distantIP;
		AbstractSecureRandom r = null;
		try {
			r = getMadkitConfig().getApprovedSecureRandom();
		} catch (Exception e) {
			if (logger != null)
				logger.severeLog("Unexpected exception", e);
		}
		random = r;
		this.connection_protocol = null;
	}

	abstract IDTransfer getTransferType();

	public StatsBandwidth getStatistics() {
		if (stats.get() == null)
			stats.set(getMadkitConfig().networkProperties.addIfNecessaryAndGetStatsBandwidth(
					new ConnectionIdentifier(getTransferType(), distant_inet_address, local_interface_address)));
		return stats.get();
	}

	public int getMaxBlockSize() {
		return max_block_size;
	}

	public InetSocketAddress getDistantInetSocketAddressRoot() {
		return distant_inet_address;
	}

	protected void addTaskTransferCheckerIfNecessary() {
		if (taskTransferNodeChecker == null) {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Adding transfer checker task (distant_inet_address=" + distant_inet_address
						+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");

			taskTransferNodeChecker = this.scheduleTask(new Task<Void>(System.currentTimeMillis() + getMadkitConfig().networkProperties.connectionTimeOutInMs,
					getMadkitConfig().networkProperties.connectionTimeOutInMs) {
				@Override
				public Void call() {
					receiveMessage(new CheckDeadTransferNodes());
					return null;
				}
			});
		}
	}

	protected void removeTaskTransferCheckerIfNecessary() {
		if (taskTransferNodeChecker != null) {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Removing transfer checker task (distant_inet_address=" + distant_inet_address
						+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");

			if (!transfer_ids.hasDataToCheck() && !transfer_ids_to_finalize.hasDataToCheck()) {
				cancelTask(taskTransferNodeChecker, false);
				taskTransferNodeChecker = null;
			}
		}
	}

	protected long getDelayInMsBeforeTransferNodeBecomesObsolete() {
		return getMadkitConfig().networkProperties.connectionTimeOutInMs * 3;
	}

	@Override
	protected void activate() {
		try {
			setLogLevel(getMadkitConfig().networkProperties.networkLogLevel);
			if (logger != null && logger.isLoggable(Level.FINE))
				logger.fine("Starting " + this + " (" + this.distant_inet_address + ")... !");
			if (logger != null)
				logger.info("Starting connection (distant_inet_address=" + distant_inet_address + ", local_interface="
						+ local_interface_address + ")");
			connection_protocol = getMadkitConfig().networkProperties.getConnectionProtocolInstance(
					distant_inet_address, local_interface_address, getMadkitConfig().getDatabaseWrapper(),getMadkitConfig(),
					!this_ask_connection, this instanceof IndirectAgentSocket);



			if (connection_protocol == null)
				throw new IllegalArgumentException(
						"The properties must have at least one connection protocol compatible !");
			if (NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE<getMadkitConfig().networkProperties.maxShortDataSize)
				NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE=getMadkitConfig().networkProperties.maxShortDataSize;
			
			max_buffer_size = getMadkitConfig().networkProperties.maxBufferSize;
			if (max_buffer_size <= 0)
				throw new IllegalArgumentException("The buffer size must be greater than 0");
			int max = SubBlocksStructure.getAbsoluteMaximumBufferSize(connection_protocol);
			if (max <= 0)
				throw new IllegalArgumentException(
						String.format(AgentSocketMessage.TO_MUCH_PACKET_BYTES_IN_CONNECTION_PROTOCOL.toString(),
								SubBlocksStructure.getAbsoluteMaximumBlockSize(connection_protocol, 1, getMadkitConfig().networkProperties.maxRandomPacketValues),
								0xFFFF));
			if (max_buffer_size > max)
				throw new IllegalArgumentException(
						String.format(AgentSocketMessage.BUFFER_SIZE_TO_BIG_CONSIDERING_CONNECTION_PROTOCOL.toString(),
								max, max_buffer_size));
			max_block_size = SubBlocksStructure.getAbsoluteMaximumBlockSize(connection_protocol, max_buffer_size, getMadkitConfig().networkProperties.maxRandomPacketValues);
			if (max_block_size > Block.BLOCK_SIZE_LIMIT)
				throw new NIOException(AgentSocketMessage.UNEXPECTED_EXCEPTION.toString());
			// defining the block size and the packet offset
			boolean nr = false;

			for (Iterator<ConnectionProtocol<?>> it = connection_protocol.reverseIterator(); it.hasNext();) {
				ConnectionProtocol<?> cp = it.next();
				if (cp.isCrypted()) {
					nr = true;
					break;
				}
			}
			need_random = nr;
			LoginEventsTrigger lt = new LoginEventsTrigger() {

				@Override
				public void removingIdentifiers(Collection<Identifier> _identifiers) {
					AbstractAgentSocket.this.receiveMessage(new ObjectMessage<>(
							new NewLocalLoginRemovedMessage(new ArrayList<>(_identifiers))));
				}

				@Override
				public void removingIdentifier(Identifier _identifier) {
					ArrayList<Identifier> identifiers = new ArrayList<>();
					identifiers.add(_identifier);
					AbstractAgentSocket.this.receiveMessage(
							new ObjectMessage<>(new NewLocalLoginRemovedMessage(identifiers)));
				}

				@Override
				public void addingIdentifiers(Collection<Identifier> _identifiers) {
					AbstractAgentSocket.this.receiveMessage(new ObjectMessage<>(
							new NewLocalLoginAddedMessage(new ArrayList<>(_identifiers))));

				}

				@Override
				public void addingIdentifier(Identifier _identifier) {
					ArrayList<Identifier> identifiers = new ArrayList<>();
					identifiers.add(_identifier);
					AbstractAgentSocket.this.receiveMessage(new ObjectMessage<>(
							new NewLocalLoginAddedMessage(new ArrayList<>(identifiers))));
				}
			};

			my_accepted_groups = new Groups();
			my_accepted_logins = new Logins();

			access_protocol = getMadkitConfig().networkProperties.getAccessProtocolProperties(distant_inet_address,local_interface_address).getAccessProtocolInstance(distant_inet_address, local_interface_address, lt, getMadkitConfig());
			
			if (!this.requestRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.SOCKET_AGENT_ROLE)
					.equals(ReturnCode.SUCCESS)) {
				Logger logger = getLogger();
				if (logger != null)
					logger.severe("Cannot request group " + LocalCommunity.Groups.NETWORK + " and role "
							+ LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			}

			if (!this.requestRole(LocalCommunity.Groups.getAgentSocketGroup(getAgentID()),
					LocalCommunity.Roles.MAIN_SOCKET_AGENT_ROLE).equals(ReturnCode.SUCCESS)) {
				Logger logger=getLogger();
				if (logger!=null)
					logger.severe("Cannot request group " + LocalCommunity.Groups.getAgentSocketGroup(getAgentID())
						+ " and role " + LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			}
			if (!requestRole(LocalCommunity.Groups.getDistantKernelAgentGroup(agent_for_distant_kernel_aa),
					LocalCommunity.Roles.SOCKET_AGENT_ROLE).equals(ReturnCode.SUCCESS)) {
				Logger logger=getLogger();
				if (logger!=null)
					logger.severe("Cannot request group "
						+ LocalCommunity.Groups.getDistantKernelAgentGroup(agent_for_distant_kernel_aa) + " and role "
						+ LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			}
			access_protocol.setKernelAddress(getKernelAddress(), this_ask_connection);

			sendMessageWithRole(this.agent_for_distant_kernel_aa,
					new ObjectMessage<>(new AgentSocketData(this)),
					LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			if (this.distantConnectionInfo != null)
				AbstractAgentSocket.this.sendMessageWithRole(agent_for_distant_kernel_aa,
						new ObjectMessage<>(this.distantConnectionInfo), LocalCommunity.Roles.SOCKET_AGENT_ROLE);

			// this.launchAgent(watcher);
			initiateConnectionIfNecessaryOnActivation();
			if (logger != null && logger.isLoggable(Level.FINE))
				logger.fine(" launched ! (distant_inet_address=" + this.distant_inet_address + ", this_ask_connection="
						+ this_ask_connection + ", connectionProtocolsNumber="
						+ (1 + connection_protocol.numberOfSubConnectionProtocols()) + ") ");

		} catch (Exception e) {
			if (logger != null)
				logger.severeLog(
						"Start of " + this.getClass().getName() + " (" + this.distant_inet_address + ") FAILED !", e);
			startDisconnectionProcess(ConnectionClosedReason.CONNECTION_ANOMALY);
		}
	}

	protected void initiateConnectionIfNecessaryOnActivation() throws ConnectionException {
		checkTransferBlockCheckerChanges();
		if (this_ask_connection) {
			ConnectionMessage cm = null;
			for (Iterator<ConnectionProtocol<?>> it = connection_protocol.reverseIterator(); it.hasNext();) {

				ConnectionProtocol<?> cp = it.next();
				if (!cp.hasFinished()) {
					cm = cp.setAndGetNextMessage(new AskConnection(true));

					if (cm == null)
						throw new NullPointerException(
								"The first returned message by the caller connection protocol must be an AskMessage.");
					if (!(cm instanceof AskConnection))
						throw new ConnectionException(
								"The first returned message by the caller connection protocol must be an AskMessage.");
					break;
				}
			}
			if (cm != null) {
				checkTransferBlockCheckerChanges();
				sendData(cm, true, false);
			}
		}
	}

	@Override
	protected void end() {
		Message m;
		while ((m=nextMessage())!=null)
		{
			if (m instanceof DistKernADataToUpgradeMessage)
			{
				try {
					((DistKernADataToUpgradeMessage) m).dataToUpgrade.cancel();
				} catch (IOException | MadkitException ex) {
					if (logger!=null)
						logger.severeLog("Cannot close packet data", ex);
				}
			}
		}
		NetworkBoard nbb=((NetworkBoard) getBoard(LocalCommunity.Groups.NETWORK,
				LocalCommunity.Boards.NETWORK_BOARD));
		if (nbb!=null)
			nbb.unlockSimultaneousConnections(distant_kernel_address);

		cancelTaskTransferNodeChecker();
		getMadkitConfig().networkProperties.removeStatsBandwidth(
				new ConnectionIdentifier(getTransferType(), distant_inet_address, local_interface_address));
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine(this.getClass().getName() + " (" + this.distant_inet_address + ") killed !");
		this.notifyAll();

	}

	protected void startDisconnectionProcess(ConnectionClosedReason reason) {
		this.startDisconnectionProcess(reason, null);
	}

	protected void startDisconnectionProcess(ConnectionClosedReason reason, ConnectionFinished last_message) {
		if (reason == ConnectionClosedReason.CONNECTION_ANOMALY)
			isBanned = true;
		if (state.compareTo(State.DISCONNECTION_IN_PROGRESS) >= 0)
			return;
		if (logger != null)
			logger.info("Starting disconnection process (distant_inet_address=" + distant_inet_address
					+ ", local_interface=" + local_interface_address + ", distantInterfacedKernelAddress="
					+ distantInterfacedKernelAddress + ", reason=" + reason + ")");
		state = State.DISCONNECTION_IN_PROGRESS;
		connection_closed_reason = reason;
		transfer_ids.closeAllTransferID(false);
		transfer_ids_to_finalize.closeAllTransferID(false);
		if (last_message == null) {
			try {

				if (getTransferType().equals(TransferAgent.NullIDTransfer)) {
					if (reason.equals(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED)) {
						sendData(new ConnectionFinished(this.distant_inet_address, reason), true, true);
					}
					else {
						closeConnectionProtocols(reason);
						sendMessageWithRole(
								this.nio_agent_address, new AskForConnectionMessage(reason, distantIP,
										getDistantInetSocketAddressRoot(), local_interface_address, true, false),
								LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					}
				} else {
					if (reason.equals(ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED))
						sendData(new ConnectionFinished(this.distant_inet_address, reason), true, false);
					else {
						closeConnectionProtocols(reason);
						sendMessageWithRole(this.nio_agent_address,
								new AskForConnectionMessage(reason, distantIP, getDistantInetSocketAddressRoot(),
										local_interface_address, true, false, getTransferType(),
										getAgentAddressIn(LocalCommunity.Groups.NETWORK,
												LocalCommunity.Roles.SOCKET_AGENT_ROLE)),
								LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					}
				}

			} catch (ConnectionException e) {
				if (logger != null)
					logger.severeLog("Disconnection problem", e);
			}
		} else
			sendData(last_message, true, true);
	}

	public AgentNetworkID getSocketID() {
		return getNetworkID();
	}

	private void closeConnectionProtocols(ConnectionClosedReason reason) throws ConnectionException {
		if (connection_protocol==null)
			return;
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Closing all protocols (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ", reason=" + reason
					+ ")");
		for (Iterator<ConnectionProtocol<?>> it = connection_protocol.reverseIterator(); it.hasNext();) {
			ConnectionProtocol<?> cp = it.next();
			if (!cp.isConnectionFinishedButClosed())
				cp.setConnectionClosed(reason);
		}

	}

	private <T extends AbstractData> ArrayList<T> getFilteredData(Collection<T> data, boolean checkTransferIds) {
		ArrayList<T> res = new ArrayList<>(data.size());
		for (T ad : data) {
			if ((ad.getIDTransfer()!=null && ad.getIDTransfer().equals(getTransferType()))
					|| (checkTransferIds && this.transfer_ids.getLocal(Objects.requireNonNull(ad.getIDTransfer())) != null))
				res.add(ad);
		}
		return res;
	}

	private void cancelTaskTransferNodeChecker() {
		if (taskTransferNodeChecker != null) {
			taskTransferNodeChecker.cancelTask(false);
			taskTransferNodeChecker = null;
		}

	}

	protected void disconnected(ConnectionClosedReason reason, List<AbstractData> _data_not_sent,
								List<BigPacketData> bigDataNotSent, List<BlockDataToTransfer> dataToTransferNotSent) {

		try {
			cancelTaskTransferNodeChecker();
			connection_closed_reason = reason;
			for (AgentAddress aa : getAgentsWithRole(LocalCommunity.Groups.getAgentSocketGroup(getAgentID()),
					LocalCommunity.Roles.SOCKET_AGENT_ROLE))
				sendMessageWithRole(aa,
						new ConnectionClosed(this.getSocketID(),
								reason == ConnectionClosedReason.CONNECTION_ANOMALY ? reason
										: ConnectionClosedReason.CONNECTION_LOST,
								_data_not_sent, bigDataNotSent, dataToTransferNotSent),
						LocalCommunity.Roles.MAIN_SOCKET_AGENT_ROLE);
			transfer_ids.closeAllTransferID(false);
			transfer_ids_to_finalize.closeAllTransferID(false);

			if (state.compareTo(State.DISCONNECTION_IN_PROGRESS) <= 0
					&& reason == ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED) {
				closeConnectionProtocols(reason);
			}
		} catch (ConnectionException e) {
			if (logger != null)
				logger.severeLog("Disconnection problem", e);

		}

		state = State.DISCONNECTION;
		try {
			_data_not_sent = getFilteredData(_data_not_sent, false);
			bigDataNotSent = getFilteredData(bigDataNotSent, false);
			dataToTransferNotSent = getFilteredData(dataToTransferNotSent, true);
			try {
				for (AbstractData ad : _data_not_sent)
					ad.unlockMessage();
				for (AbstractData ad : bigDataNotSent)
					ad.cancel();
				for (AbstractData ad : dataToTransferNotSent)
					ad.unlockMessage();
			}
			catch (IOException e)
			{
				if (logger != null)
					logger.log(Level.SEVERE, "Unexpected exception", e);
			}


		} catch (MadkitException e) {
			if (logger != null)
				logger.severeLog("", e);
		}
		if (this.agent_for_distant_kernel_aa != null) {
			ReturnCode rc = this.sendMessageWithRole(agent_for_distant_kernel_aa,
					new AgentSocketKilled(_data_not_sent, bigDataNotSent, dataToTransferNotSent),
					LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			if (logger!=null && !ReturnCode.isSuccessOrIsTransferInProgress(rc))
				logger.warning("Impossible to send kill message to distant kernel agent : "+rc);
		}
		else if (logger!=null)
			logger.warning("Impossible to send kill message to distant kernel agent");

		this.sendMessageWithRole(nio_agent_address,
				new AgentSocketKilled(_data_not_sent, bigDataNotSent, dataToTransferNotSent),
				LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		if (logger != null)
			logger.info("Disconnection OK (distant_inet_address=" + distant_inet_address + ", local_interface="
					+ local_interface_address + ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
					+ ", reason=" + reason + ")");
		if (agent_for_distant_kernel_aa != null && distantInterfacedKernelAddress != null)
			MadkitKernelAccess
					.informHooks(this,
							new NetworkEventMessage(connection_closed_reason,
									new Connection(new ConnectionIdentifier(getTransferType(),
											distant_inet_address, local_interface_address),
											distantInterfacedKernelAddress, distantConnectionInfo)));

		this.killAgent(this);
	}

	static class AgentSocketKilled extends Message {


		protected final List<AbstractData> shortDataNotSent;
		protected final List<BigPacketData> bigDataNotSent;
		protected final List<BlockDataToTransfer> dataToTransferNotSent;

		AgentSocketKilled(List<AbstractData> _data_not_sent, List<BigPacketData> bigDataNotSent,
						  List<BlockDataToTransfer> dataToTransferNotSent) {
			this.shortDataNotSent = _data_not_sent;
			this.bigDataNotSent = bigDataNotSent;
			this.dataToTransferNotSent = dataToTransferNotSent;
		}
	}

	@Override
	protected void liveByStep(Message _message) {
		if (_message==null)
			return;
		Class<?> clazz=_message.getClass();
		if (clazz == DistKernADataToUpgradeMessage.class) {

			AbstractPacketData d = ((DistKernADataToUpgradeMessage) _message).dataToUpgrade;
			if (d.isCanceled())
			{
				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("Ignoring canceled data buffer (distant_inet_address=" + distant_inet_address
							+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + d);
			}
			else {
				// d.setStat(getBytesPerSecondsStat());
				if (d.isDataBuildInProgress()) {
					try {
						d.setNewBlock(getTransferType(), getBlock(d.packet, getTransferType().getID(), d.excludedFromEncryption));
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest("Data buffer updated (distant_inet_address=" + distant_inet_address
									+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
									+ ", totalDataLe) : " + d);
					} catch (NIOException e) {
						if (logger != null)
							logger.severeLog("Impossible to send packet " + d.getIDPacket(), e);
						try {
							d.cancel();
						} catch (IOException | MadkitException ex) {
							if (logger!=null)
								logger.severeLog("Cannot close packet data", ex);
						}

					}


				} else {
					d.agentSocket = this;
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Sending data buffer (distant_inet_address=" + distant_inet_address
								+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + d);
				}
				boolean sendMessage = d.getReadDataLengthIncludingHash() == 0;
				if (sendMessage) {
					if (ReturnCode.isSuccessOrIsTransferInProgress(sendMessageWithRole(nio_agent_address, new DataToSendMessage(d, getSocketID()),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE))) {
						lastDistKernADataToUpgradeMessageSentNano = System.nanoTime();
					}
					else
					{
						try {
							d.cancel();
						} catch (IOException | MadkitException ex) {
							if (logger!=null)
								logger.severeLog("Cannot close packet data", ex);
						}
					}
				}
			}
		} else if (clazz == DataReceivedMessage.class) {
			receiveData(((DataReceivedMessage) _message).getReceivedData());
		} else if (clazz == SendPingMessage.class) {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Sending ping message (distant_inet_address=" + distant_inet_address
						+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");
			waitingPongMessage = true;
			sendData(new PingMessage(), true, false);
		} else if (clazz==ConnectionClosed.class) {
			ConnectionClosed cc = (ConnectionClosed) _message;
			disconnected(cc.reason, cc.data_not_sent, cc.bigDataNotSent, cc.dataToTransferNotSent);
		} else if (clazz == FailedCreateIndirectAgentSocket.class) {
			((FailedCreateIndirectAgentSocket) _message).executeTask();
		} else if (clazz == AskForConnectionMessage.class) {
			AskForConnectionMessage cc = (AskForConnectionMessage) _message;
			if (cc.type.equals(ConnectionStatusMessage.Type.DISCONNECT)) {
				if (cc.connection_closed_reason.equals(ConnectionClosedReason.CONNECTION_ANOMALY)) {
					if (!processExternalAnomaly(null, cc.getCandidateToBan()))
						startDisconnectionProcess(cc.connection_closed_reason);
				} else
					startDisconnectionProcess(cc.connection_closed_reason);
			}
		} else if (clazz==ReceivedIndirectData.class) {
			ReceivedIndirectData m = ((ReceivedIndirectData) _message);
			if (m.block.getTransferID() != getTransferType().getID()) {

				InterfacedIDTransfer idt = getValidatedInterfacedIDTransfer(m.getSender(), m.block.getTransferID());
				if (idt == null || idt.getTransferToAgentAddress() == null) {
					processInvalidBlockToTransfer(m.block);
				} else {

					sendMessageWithRole(idt.getTransferToAgentAddress(), m, LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					// receiveIndirectData(m.block, idt);
				}
			} else {
				receiveData(m.block);
			}
		} else if (clazz == ResendData.class) {
			ResendData rd = ((ResendData) _message);
			InterfacedIDTransfer idt = getValidatedInterfacedIDTransfer(_message.getSender(),
					rd.block_data.getIDTransfer());

			if (idt == null) {
				processInvalidBlockToTransfer(rd.block_data.getBlock());
			} else {
				if (idt.getTransferToAgentAddress() != null) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest(
								"Resend indirect data (middle local node, distant_inet_address=" + distant_inet_address
										+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");
					sendMessageWithRole(idt.getTransferToAgentAddress(), rd, LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest(
								"Resend indirect data (end local node, distant_inet_address=" + distant_inet_address
										+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");
					sendMessageWithRole(nio_agent_address, new DataToSendMessage(rd.block_data, getSocketID()),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				}
			}
		} else if (clazz == ReceivedSerializableObject.class) {
			ReceivedSerializableObject m = (ReceivedSerializableObject) _message;
			receiveData(m.getContent(), m);
		} else if (clazz == KernelAddressValidation.class) {
			if ( access_protocol.isNotifyAccessGroupChanges())
				notifyNewAccessChanges();
			if (((KernelAddressValidation) _message).isKernelAddressInterfaceEnabled()) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer(
							"Duplicate kernel address detected, trying to authenticate (middle local node, distant_inet_address="
									+ distant_inet_address + ", distantInterfacedKernelAddress="
									+ distantInterfacedKernelAddress + ", distant_kernel_address="
									+ distant_kernel_address + ")");

				// the received distant kernel address is already used by another connection.
				// Try to know if it these connections are part of the same peer

				// send a secret message to the distant sockets that share the same kernel
				// address.
				HashMap<AgentAddress, SecretMessage> secretMessages = new HashMap<>();
				for (AgentAddress aa : this.getAgentsWithRole(
						LocalCommunity.Groups.getOriginalDistantKernelAgentGroup(distant_kernel_address),
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE)) {
					if (!aa.representsSameAgentThan(agent_for_distant_kernel_aa)) {
						SecretMessage sm = new SecretMessage(random, distant_socket_agent_address,
								agent_for_distant_kernel_aa);
						secretMessages.put(aa, sm);
						sendMessageWithRole(aa, new ObjectMessage<>(sm),
								LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					}
				}
				this.currentSecretMessages.set(secretMessages);

				// add a task that will consider after an elapsed time that the received
				// distant kernel address is not part of a same already connected peer.
				scheduleTask(new Task<Void>(System.currentTimeMillis()
						+ getMadkitConfig().networkProperties.maxDurationInMsOfDistantKernelAddressCheck) {
					@Override
					public Void call() {
						if (isAlive()) {
							if (AbstractAgentSocket.this.getState().compareTo(AbstractAgent.State.ENDING) < 0)
								validateKernelAddress(AbstractAgentSocket.this.agent_for_distant_kernel_aa);
						}
						return null;
					}
				});
				
			} else {
				// The received distant kernel address is unique into this peer.
				// The kernel address is validated.
				// the kernel address is not interfaced
				currentSecretMessages.set(null);
				
				
			}
			
		} else if (clazz == ExceededDataQueueSize.class) {

			boolean paused = ((ExceededDataQueueSize) _message).isPaused();

			if (exceededDataQueueSize.getAndSet(paused) != paused) {
				if (logger != null) {
					if (paused) {
						if (logger.isLoggable(Level.FINEST))
							logger.finest("Exceeding data queue size (" + this.distant_inet_address + ", "
									+ distant_inet_address + ") : " + true);
					} else if (logger.isLoggable(Level.FINEST))
						logger.finest("Exceeding data queue size (" + this.distant_inet_address + ", "
								+ distant_inet_address + ") : " + false);
				}
			}
		} else if (clazz == PossibleInetAddressesUsedForDirectConnectionChanged.class) {
			PossibleInetAddressesUsedForDirectConnectionChanged m = (PossibleInetAddressesUsedForDirectConnectionChanged) _message;
			if (m.isConcernedBy(local_interface_address.getAddress())) {
				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("Possible inet address change (distant_inet_address=" + distant_inet_address
							+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
							+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");

				sendConnectionInfoSystemMessage();
			}
		} else if (clazz == CheckDeadTransferNodes.class) {
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Checking dead transfer nodes (distant_inet_address=" + distant_inet_address
						+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");

			transfer_ids.removeObsoleteData();
			transfer_ids_to_finalize.removeObsoleteData();
			this.removeTaskTransferCheckerIfNecessary();
		} else if (clazz == AnomalyDetectedMessage.class) {
			AnomalyDetectedMessage m = (AnomalyDetectedMessage) _message;
			if ((m.getInetSocketAddress() != null && m.getInetSocketAddress().equals(distant_inet_address))
					|| (m.getKernelAddress() != null && m.getKernelAddress().equals(distantInterfacedKernelAddress))
					|| m.getKernelAddress() == null) {
				processExternalAnomaly(m.getMessage(), m.isCandidateToBan());
			}
		} else if (clazz == ObjectMessage.class) {
			Object o = ((ObjectMessage<?>) _message).getContent();
			if (o==null)
				return;
			clazz=o.getClass();
			if (clazz == NewLocalLoginAddedMessage.class) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer("Local login added (" + this.distant_inet_address + ", " + distant_inet_address + ")");

				receiveData(o, null);
			} else if (clazz == SecretMessage.class) {
				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("Send secret message (distant_inet_address=" + distant_inet_address
							+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");
				SecretMessage sm = (SecretMessage) o;
				sm.removeAgentSocketAddress();
				sendData(sm, true, false);
			} else if (clazz == StatsBandwidth.class) {
				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("Updating statistics for distant kernel address (distant_inet_address="
							+ distant_inet_address + ", distantInterfacedKernelAddress="
							+ distantInterfacedKernelAddress + ")");
				getStatistics().putStateForDistantKernelAddress((StatsBandwidth) o);
			} else if (clazz == KernelAddressInterfaced.class) {

				leaveRole(LocalCommunity.Groups.getDistantKernelAgentGroup(agent_for_distant_kernel_aa),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE);

				if (!requestRole(LocalCommunity.Groups.getDistantKernelAgentGroup(_message.getSender()),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE).equals(ReturnCode.SUCCESS)) {
					Logger logger=getLogger();
					if (logger!=null)
						logger.severe("Cannot request group "
							+ LocalCommunity.Groups.getDistantKernelAgentGroup(_message.getSender()) + " and role "
							+ LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				}

				Collection<AgentAddress> c = getAgentsWithRole(
						LocalCommunity.Groups.getDistantKernelAgentGroup(_message.getSender()),
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);

				AgentAddress aa = null;
				if (c.size() == 1)
					aa = c.iterator().next();
				else {
					Logger logger=getLogger();
					if (logger!=null) {
						if (c.size() == 0)
							logger.severe("Distant kernel agent not found : " + o);
						else
							logger.severe("Distant kernel agent duplicated : " + o);
					}
				}
				if (aa != null) {
					distant_kernel_agent_activated = true;
					agent_for_distant_kernel_aa = aa;
					distantInterfacedKernelAddress = (KernelAddressInterfaced) o;
					sendData(new DistantKernelAddressValidated(), true, false);
					if (this.distantKernelAddressValidated.get()) {
						sendMessageWithRole(this.agent_for_distant_kernel_aa,
								new ObjectMessage<>(new DistantKernelAddressValidated()),
								LocalCommunity.Roles.SOCKET_AGENT_ROLE);
						informHooksForConnectionEstablished();
					}

				} else {
					this.startDisconnectionProcess(ConnectionClosedReason.CONNECTION_ANOMALY);
				}
			} else if (clazz == InitiateTransferConnection.class) {
				InitiateTransferConnection candidate = (InitiateTransferConnection) o;

				initiateTransferConnection(_message.getSender(), candidate);
			} else if (clazz == DataToBroadcast.class) {
				DataToBroadcast m = (DataToBroadcast) o;
				if (logger != null)
					logger.finest("broadcasting indirect data (distant_inet_address=" + distant_inet_address
							+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : "
							+ m.getTransferID());

				receiveBroadcastData(_message.getSender(), m, false);
				// }
			} else if (clazz == TransferConfirmationSystemMessage.class) {
				TransferConfirmationSystemMessage t = (TransferConfirmationSystemMessage) o;
				broadcastPropositionAnswer(t);
			} else if (clazz == TransferImpossibleSystemMessageFromMiddlePeer.class) {
				TransferImpossibleSystemMessageFromMiddlePeer t = (TransferImpossibleSystemMessageFromMiddlePeer) o;
				broadcastPropositionImpossibleAnswer(t);
			} else if (clazz == TransferClosedSystemMessage.class) {
				TransferClosedSystemMessage t = (TransferClosedSystemMessage) o;
				if (logger != null && logger.isLoggable(Level.FINEST))
					logger.finest("closing indirect connection (distant_inet_address=" + distant_inet_address
							+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : "
							+ t.getIdTransfer());
				t = new TransferClosedSystemMessage(getTransferType(), t.getKernelAddressDestination(),
						t.getIdTransfer(), t.isLastPass());

				if (getTransferType().equals(TransferAgent.NullIDTransfer)) {
					InterfacedIDTransfer idt = transfer_ids.getLocal(t.getIdTransfer());
					if (idt != null) {
						idt = transfer_ids.getDistant(idt.getDistantID());
						AgentAddress aa = idt.getTransferToAgentAddress();
						if (aa == null)
							processInvalidTransferConnectionProtocol("Unexpected exception");
						else
							receiveTransferClosedSystemMessage(aa, t, getKernelAddress(), true, true);
					}
				} else {
					InterfacedIDTransfer idt = getValidatedInterfacedIDTransfer(
							((IndirectAgentSocket) this).getParentAgentSocketAddress(), t.getIdTransfer());
					if (idt != null) {
						AgentAddress aa = idt.getTransferToAgentAddress();
						if (aa == null)
							processInvalidTransferConnectionProtocol("Unexpected exception 2");
						else
							receiveTransferClosedSystemMessage(aa, t, getKernelAddress(), true, true);
					}
				}
			} else if (clazz == TryDirectConnection.class) {
				sendData((TryDirectConnection) o, true, false);
			} else if (clazz == DirectConnectionFailed.class) {
				sendData((DirectConnectionFailed) o, true, false);
			} else if (clazz == DirectConnectionSucceeded.class) {
				sendData((DirectConnectionSucceeded) o, true, false);
			} else if (clazz == TooMuchConnectionWithTheSamePeers.class) {
				if (logger != null && logger.isLoggable(Level.FINER))
					logger.finer(
							"Too much connections with the same peers (distant_inet_address=" + distant_inet_address
									+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");
				sendData((TooMuchConnectionWithTheSamePeers) o, true, true);
			} else if (clazz==String.class)
			{
				String m=(String)o;

				if (m.equals(NetworkAgent.REFRESH_GROUPS_ACCESS))
				{
					try {
						access_protocol.updateGroupAccess();
						notifyNewAccessChanges();
					} catch (AccessException e) {
						getLogger().severeLog("", e);
					}
				}
			} else if (o instanceof Runnable) {
				((Runnable) o).run();
			}
		}
	}



	private void sendConnectionInfoSystemMessage() {
		ArrayList<InetAddress> ias = new ArrayList<>();
		MultipleIP mip = null;
		try {
			for (Enumeration<InetAddress> it = NetworkInterface.getByInetAddress(local_interface_address.getAddress())
					.getInetAddresses(); it.hasMoreElements();) {
				InetAddress ia = it.nextElement();
				if (LocalNetworkAgent.isValid(ia) && getMadkitConfig().networkProperties.needsServerSocket(new InetSocketAddress(ia,
						getMadkitConfig().networkProperties.portsToBindForAutomaticLocalConnections), distant_inet_address.getPort())) {
					ias.add(ia);
				}
			}
		} catch (Exception e) {
			logger.severeLog("Unexpected exception", e);
		}
		if (!ias.isEmpty())
			mip = new MultipleIP(getMadkitConfig().networkProperties.portsToBindForAutomaticLocalConnections, ias);
		ConnectionInfoSystemMessage ci = new ConnectionInfoSystemMessage(
				getMadkitConfig().networkProperties.getPossibleAddressesForDirectConnectionToAttemptFromOtherPeersToThisPeer(),
				this.local_interface_address.getAddress(),
				getMadkitConfig().networkProperties.portsToBindForManualDirectConnections,
				getMadkitConfig().networkProperties.portsToBindForAutomaticLocalConnections,
				this.getMadkitConfig().networkProperties.needsServerSocket(local_interface_address, distant_inet_address.getPort()), mip);
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Sending connection information to distant peer (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + ci);

		sendData(ci, true, false);
	}

	private void initiateTransferConnection(AgentAddress transferAgentAddress, InitiateTransferConnection candidate) {
		if (getTransferType().equals(TransferAgent.NullIDTransfer)) {
			transfer_ids_to_finalize.putLocal(new InterfacedIDTransfer(candidate.getIdTransfer(),
					candidate.getAgentAddress(), candidate.getKernelAddress()));

			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Initiate transfer connection (end local node, distant_inet_address="
						+ distant_inet_address + ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
						+ ") : " + candidate);

		} else {
			putInterfacedIDTransferToFinalize(((IndirectAgentSocket) this).getParentAgentSocketAddress(),
					new InterfacedIDTransfer(candidate.getIdTransfer(), candidate.getAgentAddress(),
							candidate.getKernelAddress()));
			putInterfacedIDTransferToFinalize(candidate.getAgentAddress(),
					new InterfacedIDTransfer(candidate.getIdTransfer(),
							((IndirectAgentSocket) this).getParentAgentSocketAddress(), distant_kernel_address));
			if (logger != null && logger.isLoggable(Level.FINEST))
				logger.finest("Initiate transfer connection (intermediate local node, distant_inet_address="
						+ distant_inet_address + ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
						+ ") : " + candidate);
		}

		this.transfer_ids_to_finalize.putTransferAgentAddress(candidate.getIdTransfer(), transferAgentAddress);
		broadcastDataTowardEachIntermediatePeer(new TransferPropositionSystemMessage(getTransferType(),
				candidate.getIdTransfer(), candidate.getKernelAddress(), this.distant_kernel_address,
				candidate.getNumberOfIntermediatePeers(), candidate.getOriginalMessage().getAttachedData(),
				candidate.isYouAskConnection()), true);
	}

	protected abstract int getNumberOfIntermediatePeers();

	protected InterfacedIDTransfer getInterfacedIDTransfer(TransferIDs transfer_ids, AgentAddress sender,
			int idTransfer) {
		if (sender != null) {
			return transfer_ids.getLocal(idTransfer);
		} else
			return transfer_ids.getDistant(idTransfer);
	}

	protected final InterfacedIDTransfer getValidatedInterfacedIDTransfer(AgentAddress sender, IDTransfer idTransfer) {
		return getInterfacedIDTransfer(transfer_ids, sender, idTransfer.getID());
	}

	protected final InterfacedIDTransfer getValidatedInterfacedIDTransfer(AgentAddress sender, int idTransfer) {
		return getInterfacedIDTransfer(transfer_ids, sender, idTransfer);
	}

	protected final InterfacedIDTransfer getInterfacedIDTransferToFinalize(AgentAddress sender, IDTransfer idTransfer) {
		return getInterfacedIDTransfer(transfer_ids_to_finalize, sender, idTransfer.getID());
	}

	protected void putInterfacedIDTransfer(TransferIDs transfer_ids, AgentAddress sender, InterfacedIDTransfer idt) {
		if (sender != null)
			transfer_ids.putLocal(idt);
		else
			transfer_ids.putDistant(idt);
	}

	protected final void putValidatedInterfacedIDTransfer(AgentAddress sender, InterfacedIDTransfer idt) {
		putInterfacedIDTransfer(transfer_ids, sender, idt);
	}

	protected final void putInterfacedIDTransferToFinalize(AgentAddress sender, InterfacedIDTransfer idt) {
		putInterfacedIDTransfer(transfer_ids_to_finalize, sender, idt);
	}

	protected void validateInterfacedIDTransfer(AgentAddress sender, IDTransfer id, boolean forceLocalID) {
		InterfacedIDTransfer idt;
		if (sender != null || forceLocalID) {
			idt = this.transfer_ids_to_finalize.removeLocal(id);

			this.transfer_ids.putLocal(idt);
			if (idt.getDistantID() != null)
				this.transfer_ids.putDistant(this.transfer_ids_to_finalize.removeDistant(idt.getDistantID()));
		} else {
			idt = this.transfer_ids_to_finalize.removeDistant(id);
			this.transfer_ids.putDistant(idt);
			InterfacedIDTransfer idtLocal = this.transfer_ids_to_finalize.removeLocal(idt.getLocalID());
			if (idtLocal != null)
				this.transfer_ids.putLocal(idtLocal);
		}
		TransferPropositionSystemMessage tp = this.transfer_ids_to_finalize
				.removeTransferPropositionSystemMessage(idt.getLocalID());
		if (tp != null)
			this.transfer_ids.putTransferPropositionSystemMessage(idt.getLocalID(), tp);
		AgentAddress aa = this.transfer_ids_to_finalize.removeTransferAgentAddress(idt.getLocalID());
		if (aa != null)
			this.transfer_ids.putTransferAgentAddress(idt.getLocalID(), aa);
	}

	protected final InterfacedIDTransfer removeValidatedInterfacedIDTransfer(AgentAddress sender, IDTransfer id) {
		return removeValidatedInterfacedIDTransfer(sender, id, false);
	}

	protected final InterfacedIDTransfer removeValidatedInterfacedIDTransfer(AgentAddress sender, IDTransfer id,
			boolean forceLocal) {
		return removeInterfacedIDTransfer(transfer_ids, sender, id, forceLocal);
	}

	protected final InterfacedIDTransfer removeInterfacedIDTransferToFinalize(AgentAddress sender, IDTransfer id) {
		return removeInterfacedIDTransferToFinalize(sender, id, false);
	}

	protected final InterfacedIDTransfer removeInterfacedIDTransferToFinalize(AgentAddress sender, IDTransfer id,
			boolean forceLocal) {
		return removeInterfacedIDTransfer(transfer_ids_to_finalize, sender, id, forceLocal);
	}

	protected InterfacedIDTransfer removeInterfacedIDTransfer(TransferIDs transfer_ids, AgentAddress sender,
			IDTransfer id, boolean forceLocal) {
		InterfacedIDTransfer idt;
		if (sender != null || forceLocal) {
			idt = transfer_ids.removeLocal(id);
			if (idt!=null && idt.getDistantID() != null)
				transfer_ids.removeDistant(idt.getDistantID());
		} else {
			idt = transfer_ids.removeDistant(id);
			if (idt!=null)
				transfer_ids.removeLocal(idt.getLocalID());
		}
		if (idt==null)
			return null;
		transfer_ids.removeTransferPropositionSystemMessage(idt.getLocalID());
		transfer_ids.removeTransferAgentAddress(idt.getLocalID());
		getMadkitConfig().networkProperties.removeStatsBandwidth(idt.getLocalID().getID());
		getStatistics().removeTransferAgentStats(idt.getLocalID());

		return idt;
	}

	private void receiveBroadcastData(AgentAddress sender, DataToBroadcast d, boolean justReceivedFromNetwork) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest(
					"Receiving broadcast data (sender=" + sender + ", distant_inet_address=" + distant_inet_address
							+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + d);

		if (d.getMessageToBroadcast().getClass() == TransferPropositionSystemMessage.class) {
			TransferPropositionSystemMessage t = (TransferPropositionSystemMessage) d.getMessageToBroadcast();

			if (t.getIdTransferDestination().equals(this.getTransferType())
					&& getKernelAddress().equals(t.getKernelAddressDestination())) {
				receiveTransferProposition(sender, t);
			} else {
				if (justReceivedFromNetwork) {
					TransferFilter tf = getMadkitConfig().networkProperties.getTransferTriggers();
					if (tf == null || tf.newTransferConnectionPropositionToIntermediatePeers(
							getMadkitConfig().networkProperties, t.getKernelAddressDestination(),
							t.getKernelAddressToConnect(), t.getNumberOfIntermediatePeers(),
							t.getAttachedDataForConnection())) {
						InterfacedIDTransfer idDest = getValidatedInterfacedIDTransfer(sender,
								t.getIdTransferDestination());
						if (idDest == null) {
							processInvalidTransferConnectionProtocol(
									"Received a message to broadcast trough each transfer node, but impossible to found the corresponding TransferID "
											+ t.getIdTransferDestination() + " in the receiving router agent "
											+ this);
						} else {

							try {
								IDTransfer id = IDTransfer
										.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(this));
								InterfacedIDTransfer interfacedIDTransfer = new InterfacedIDTransfer(id,
										idDest.getTransferToAgentAddress(), t.getKernelAddressDestination());
								interfacedIDTransfer.setDistantID(t.getIdTransfer());
								putInterfacedIDTransferToFinalize(sender, interfacedIDTransfer);

								interfacedIDTransfer = new InterfacedIDTransfer(id, null,
										t.getKernelAddressToConnect());
								interfacedIDTransfer.setDistantID(t.getIdTransfer());

								putInterfacedIDTransferToFinalize(idDest.getTransferToAgentAddress(),
										interfacedIDTransfer);

								TransferPropositionSystemMessage t2 = new TransferPropositionSystemMessage(
										idDest.getLocalID(), interfacedIDTransfer.getLocalID(),
										t.getKernelAddressToConnect(), t.getKernelAddressDestination(),
										t.getNumberOfIntermediatePeers(), t.getAttachedDataForConnection(),
										t.isYouAskConnection());
								broadcastDataTowardEachIntermediatePeer(sender, t2, t.getIdTransferDestination(), d);
							} catch (OverflowException e) {
								TransferImpossibleSystemMessage t2 = new TransferImpossibleSystemMessage(
										idDest.getLocalID(), d.getSender(), t.getIdTransfer());
								t2.setMessageLocker(t.getMessageLocker());
								broadcastDataTowardEachIntermediatePeer(t2, true);
								if (logger != null)
									logger.severeLog("Too much transfer connections", e);
							}
						}
					}
				} else {

					InterfacedIDTransfer idLocal = getValidatedInterfacedIDTransfer(sender,
							t.getIdTransferDestination());
					if (idLocal == null) {
						processInvalidTransferConnectionProtocol(
								"Received a message to broadcast through each transfer node, but impossible to found the corresponding TransferID "
										+ t.getIdTransferDestination() + " in the middle router agent "
										+ this);
					} else {
						InterfacedIDTransfer interfacedIDTransfer = new InterfacedIDTransfer(t.getIdTransfer(),
								idLocal.getTransferToAgentAddress(), t.getKernelAddressDestination());// TODO check
																										// transfer to
																										// agent

						putInterfacedIDTransferToFinalize(sender, interfacedIDTransfer);
						broadcastDataTowardEachIntermediatePeer(sender, t, t.getIdTransferDestination(), d);
					}
				}
			}
		} else if (d.getMessageToBroadcast().getClass() == TransferImpossibleSystemMessage.class) {

			TransferImpossibleSystemMessage t = (TransferImpossibleSystemMessage) d.getMessageToBroadcast();

			if ((t.getIdTransferDestination() == getTransferType()
					|| t.getIdTransferDestination().equals(this.getTransferType()))
					&& getKernelAddress().equals(t.getKernelAddressDestination())) {
				receiveTransferImpossibleAnswer(sender, t);
			} else {
				if (justReceivedFromNetwork) {
					InterfacedIDTransfer idDist = getValidatedInterfacedIDTransfer(sender,
							t.getIdTransferDestination());
					if (idDist == null) {
						processInvalidTransferConnectionProtocol(
								"Received a message to broadcast trough each transfer node, but impossible to found the corresponding TransferID "
										+ t.getIdTransferDestination() + " in the receiving router agent "
										+ this);
					} else {
						InterfacedIDTransfer idRemoved = removeInterfacedIDTransferToFinalize(sender,
								t.getYourIDTransfer(), true);

						if (idRemoved == null) {
							processInvalidTransferConnectionProtocol(
									"Received TransferImpossibleSystemMessage, but impossible to found the corresponding TransferID "
											+ t.getYourIDTransfer() + " in the receiving router agent "
											+ this);
						} else {
							TransferImpossibleSystemMessage ti = new TransferImpossibleSystemMessage(
									idDist.getLocalID(), t.getKernelAddressDestination(), t.getYourIDTransfer());
							ti.setMessageLocker(t.getMessageLocker());
							broadcastDataTowardEachIntermediatePeer(sender, ti, t.getIdTransferDestination(), d);
						}
					}
				} else {

					InterfacedIDTransfer idRemoved = removeInterfacedIDTransferToFinalize(sender, t.getYourIDTransfer(),
							true);
					if (idRemoved == null) {
						processInvalidTransferConnectionProtocol(
								"Received TransferImpossibleSystemMessage, but impossible to found the corresponding TransferID "
										+ t.getYourIDTransfer() + " in the sender router agent " + this);
					} else {
						TransferImpossibleSystemMessage ti = new TransferImpossibleSystemMessage(
								t.getIdTransferDestination(), t.getKernelAddressDestination(),
								getTransferType().equals(TransferAgent.NullIDTransfer) ? idRemoved.getLocalID()
										: idRemoved.getDistantID());
						ti.setMessageLocker(t.getMessageLocker());
						broadcastDataTowardEachIntermediatePeer(sender, ti, t.getIdTransferDestination(), d);
					}
				}
			}
		} else if (d.getMessageToBroadcast().getClass() == TransferImpossibleSystemMessageFromMiddlePeer.class) {
			TransferImpossibleSystemMessageFromMiddlePeer t = (TransferImpossibleSystemMessageFromMiddlePeer) d
					.getMessageToBroadcast();

			if ((t.getIdTransferDestination() == getTransferType()
					|| t.getIdTransferDestination().equals(this.getTransferType()))
					&& getKernelAddress().equals(t.getKernelAddressDestination())) {
				receiveTransferImpossibleAnswer(sender, t);
			} else {
				if (justReceivedFromNetwork) {
					InterfacedIDTransfer idDist = getValidatedInterfacedIDTransfer(sender,
							t.getIdTransferDestination());
					if (idDist == null) {
						processInvalidTransferConnectionProtocol(
								"Received a message to broadcast trough each transfer node, but impossible to found the corresponding TransferID "
										+ t.getIdTransferDestination() + " in the receiving router agent "
										+ this);
					} else {
						InterfacedIDTransfer idRemoved = removeValidatedInterfacedIDTransfer(sender,
								t.getYourIDTransfer(), true);
						if (idRemoved == null)
							idRemoved = removeInterfacedIDTransferToFinalize(sender, t.getYourIDTransfer(), true);
						if (idRemoved != null) {
							TransferImpossibleSystemMessageFromMiddlePeer ti = new TransferImpossibleSystemMessageFromMiddlePeer(
									idDist.getLocalID(), t.getKernelAddressDestination(), t.getYourIDTransfer(),
									t.getYourIDTransfer());
							ti.setMessageLocker(t.getMessageLocker());
							broadcastDataTowardEachIntermediatePeer(sender, ti, t.getIdTransferDestination(), d);
						} else
							processInvalidTransferConnectionProtocol(
									"Received TransferImpossibleSystemMessage, but impossible to found the corresponding TransferID "
											+ t.getYourIDTransfer() + " in the sender router agent " + this);
					}
				} else {
					InterfacedIDTransfer idLocal = getValidatedInterfacedIDTransfer(sender,
							t.getIdTransferDestination());

					InterfacedIDTransfer idRemoved = removeValidatedInterfacedIDTransfer(sender, t.getYourIDTransfer(),
							true);
					if (idRemoved == null)
						idRemoved = removeInterfacedIDTransferToFinalize(sender, t.getYourIDTransfer(), true);
					if (idRemoved == null) {
						processInvalidTransferConnectionProtocol(
								"Received TransferImpossibleSystemMessage, but impossible to found the corresponding TransferID "
										+ t.getYourIDTransfer() + " in the sender router agent " + this);
					} else {
						TransferImpossibleSystemMessageFromMiddlePeer ti = new TransferImpossibleSystemMessageFromMiddlePeer(
								idLocal.getLocalID(), t.getKernelAddressDestination(), idRemoved.getDistantID(),
								idRemoved.getLocalID());
						ti.setMessageLocker(t.getMessageLocker());
						broadcastDataTowardEachIntermediatePeer(sender, ti, t.getIdTransferDestination(), d);
					}
				}
			}
		} else if (d.getMessageToBroadcast().getClass() == TransferConfirmationSystemMessage.class) {
			TransferConfirmationSystemMessage t = (TransferConfirmationSystemMessage) d.getMessageToBroadcast();

			if ((t.getIdTransferDestination() == getTransferType()
					|| t.getIdTransferDestination().equals(this.getTransferType()))
					&& getKernelAddress().equals(t.getKernelAddressDestination())) {
				receiveTransferConfirmationAnswer(sender, t);
			} else {
				if (justReceivedFromNetwork) {
					InterfacedIDTransfer idDist = getValidatedInterfacedIDTransfer(sender,
							t.getIdTransferDestination());
					if (idDist == null) {
						processInvalidTransferConnectionProtocol(
								"Received a message to broadcast trough each transfer node, but impossible to found the corresponding TransferID "
										+ t.getIdTransferDestination() + " in the receiving router agent "
										+ this);
					} else {
						if (t.getPointToPointBlockChecker()!=null)
						{
							t.getPointToPointBlockChecker().setConnectionProtocolInput(connection_protocol);
							if (t.isMiddleReached() && t.getKernelAddressDestination().equals(getKernelAddress()))
							{
								idDist.setLastPointToPointTransferredBlockChecker(t.getPointToPointBlockChecker());
							}
							else
								idDist.setTransferBlockChecker(t.getPointToPointBlockChecker());
						}
						
						
						if (t.isMiddleReached()) {
							validateInterfacedIDTransfer(sender, t.getMyIDTransfer(), false);

							InterfacedIDTransfer idt = getValidatedInterfacedIDTransfer(sender, t.getMyIDTransfer());
							if (!t.getKernelAddressDestination().equals(getKernelAddress()))
								idt.setTransferBlockChecker(new ConnectionProtocol.NullBlockChecker(
										t.getNumberOfSubBlocks(), false, (short) 0));

							TransferConfirmationSystemMessage ti = new TransferConfirmationSystemMessage(
									idDist.getLocalID(), t.getKernelAddressDestination(), t.getKernelAddressToConnect(),
									t.getYourIDTransfer(), t.getYourIDTransfer(), t.getNumberOfSubBlocks(), true,
									t.getDistantInetAddress(), t.getPointToPointBlockChecker());
							ti.setMessageLocker(t.getMessageLocker());
							broadcastDataTowardEachIntermediatePeer(sender, ti, t.getIdTransferDestination(), d);
						} else {
							InterfacedIDTransfer idt = new InterfacedIDTransfer(t.getYourIDTransfer(),
									idDist.getTransferToAgentAddress(), t.getKernelAddressDestination());
							idt.setDistantID(t.getMyIDTransfer());

							idt.setTransferBlockChecker(new ConnectionProtocol.NullBlockChecker(
									t.getNumberOfSubBlocks(), false, (short) 0));
							putInterfacedIDTransferToFinalize(sender, idt);
							getInterfacedIDTransferToFinalize(idDist.getTransferToAgentAddress(), idt.getLocalID())
									.setDistantID(t.getMyIDTransfer());

							TransferConfirmationSystemMessage ti = new TransferConfirmationSystemMessage(
									idDist.getLocalID(), t.getKernelAddressDestination(), t.getKernelAddressToConnect(),
									t.getYourIDTransfer(), t.getYourIDTransfer(), t.getNumberOfSubBlocks(), false,
									t.getDistantInetAddress(), t.getPointToPointBlockChecker());
							ti.setMessageLocker(t.getMessageLocker());

							broadcastDataTowardEachIntermediatePeer(sender, ti, t.getIdTransferDestination(), d);
						}
						getStatistics().putTransferAgentStats(t.getYourIDTransfer(), getMadkitConfig().networkProperties
								.addIfNecessaryAndGetStatsBandwidth(idDist.getLocalID().getID()));

					}

				} else {
					InterfacedIDTransfer idLocal = getValidatedInterfacedIDTransfer(sender,
							t.getIdTransferDestination());
					if (idLocal == null) {
						removeInterfacedIDTransferToFinalize(sender, t.getYourIDTransfer());
						processInvalidTransferConnectionProtocol(
								"Received TransferConfirmationSystemMessage to transfer for internal use 1, but impossible to found the corresponding TransferID "
										+ t.getYourIDTransfer() + " in the middle router agent " + this);
					} else {
						if (t.getPointToPointBlockChecker()!=null)
						{
							t.getPointToPointBlockChecker().setConnectionProtocolOutput(connection_protocol);
						}

						if (t.isMiddleReached()) {
							validateInterfacedIDTransfer(sender, t.getYourIDTransfer(), false);

							if (getTransferType().equals(TransferAgent.NullIDTransfer)) {
								InterfacedIDTransfer idt = getValidatedInterfacedIDTransfer(sender,
										t.getYourIDTransfer());
								TransferConfirmationSystemMessage ti = new TransferConfirmationSystemMessage(
										t.getIdTransferDestination(), t.getKernelAddressDestination(),
										t.getKernelAddressToConnect(), idt.getDistantID(), idt.getLocalID(),
										t.getNumberOfSubBlocks(), true, t.getDistantInetAddress(), t.getPointToPointBlockChecker());
								ti.setMessageLocker(t.getMessageLocker());
								t = ti;
							}
						} else {
							if (getTransferType().equals(TransferAgent.NullIDTransfer)) {
								InterfacedIDTransfer idt = getInterfacedIDTransferToFinalize(sender,
										t.getYourIDTransfer());
								TransferConfirmationSystemMessage ti = new TransferConfirmationSystemMessage(
										t.getIdTransferDestination(), t.getKernelAddressDestination(),
										t.getKernelAddressToConnect(), idt.getDistantID(), idt.getLocalID(),
										t.getNumberOfSubBlocks(), false, t.getDistantInetAddress(), t.getPointToPointBlockChecker());
								ti.setMessageLocker(t.getMessageLocker());
								t = ti;
							} else {
								InterfacedIDTransfer idt = new InterfacedIDTransfer(t.getYourIDTransfer(),
										idLocal.getTransferToAgentAddress(), t.getKernelAddressDestination());
								putInterfacedIDTransferToFinalize(sender, idt);
							}
						}
						broadcastDataTowardEachIntermediatePeer(sender, t, t.getIdTransferDestination(), d);
					}
				}
			}
		} else if (d.getMessageToBroadcast().getClass() == TransferBlockCheckerSystemMessage.class) {
			TransferBlockCheckerSystemMessage t = (TransferBlockCheckerSystemMessage) d.getMessageToBroadcast();

			if (!t.getIdTransferDestination().equals(this.getTransferType())
					|| !getKernelAddress().equals(t.getKernelAddressDestination())) {

				if (justReceivedFromNetwork) {
					if (t.getTransferBlockChecker() instanceof PointToPointTransferedBlockChecker)
						((PointToPointTransferedBlockChecker)t.getTransferBlockChecker()).setConnectionProtocolInput(connection_protocol);
					InterfacedIDTransfer idDist = getValidatedInterfacedIDTransfer(sender,
							t.getIdTransferDestination());

					InterfacedIDTransfer idLocal;
					if (idDist == null || (idLocal = this.transfer_ids.getLocal(idDist.getLocalID())) == null) {
						processInvalidTransferConnectionProtocol(
								"Received a message to broadcast trough each transfer node, but impossible to found the corresponding TransferID "
										+ t.getIdTransferDestination() + " in the receiving router agent "
										+ this);
					} else {
						if (!t.getKernelAddressDestination().equals(getKernelAddress())) {
							idDist.setTransferBlockChecker(t.getTransferBlockChecker());
							idLocal.setTransferBlockChecker(t.getTransferBlockChecker());
							TransferBlockCheckerSystemMessage tn = new TransferBlockCheckerSystemMessage(
									idLocal.getLocalID(), t.getKernelAddressDestination(),
									t.getTransferBlockChecker());
							tn.setMessageLocker(t.getMessageLocker());

							broadcastDataTowardEachIntermediatePeer(sender, tn, t.getIdTransferDestination(), d);
						}
						else 
						{
							if (t.getTransferBlockChecker() instanceof PointToPointTransferedBlockChecker)
							{
								idDist.setLastPointToPointTransferredBlockChecker((PointToPointTransferedBlockChecker)t.getTransferBlockChecker());
								idLocal.setLastPointToPointTransferredBlockChecker((PointToPointTransferedBlockChecker)t.getTransferBlockChecker());
							}
							else
							{
								idDist.setLastPointToPointTransferredBlockChecker(null);
								idLocal.setLastPointToPointTransferredBlockChecker(null);
							}
						}
					}
				} else {
					if (this instanceof AgentSocket)
					{
						if (t.getTransferBlockChecker() instanceof PointToPointTransferedBlockChecker)
							((PointToPointTransferedBlockChecker)t.getTransferBlockChecker()).setConnectionProtocolOutput(connection_protocol);
							
					}
					ReturnCode rc = broadcastDataTowardEachIntermediatePeer(sender, t, t.getIdTransferDestination(), d);
					if (!rc.equals(ReturnCode.SUCCESS) && !rc.equals(ReturnCode.TRANSFER_IN_PROGRESS))
						processInvalidTransferConnectionProtocol(
								"Received TransferBlockCheckerSystemMessage, but impossible to found the corresponding TransferID "
										+ t.getIdTransferDestination() + " in the middle router agent "
										+ this);

				}
			}
		} else if (d.getMessageToBroadcast().getClass() == TransferClosedSystemMessage.class) {
			TransferClosedSystemMessage t = (TransferClosedSystemMessage) d.getMessageToBroadcast();

			if (t.getIdTransfer().equals(this.getTransferType())
					&& getKernelAddress().equals(t.getKernelAddressDestination())
					&& !t.getIdTransfer().equals(TransferAgent.NullIDTransfer)) {
				transfer_ids.closeAllTransferID(!t.isLastPass());
				transfer_ids_to_finalize.closeAllTransferID(!t.isLastPass());
				if (!t.isLastPass()) {
					TransferClosedSystemMessage t2 = new TransferClosedSystemMessage(getTransferType(),
							distant_kernel_address, getTransferType(), true);
					t2.setMessageLocker(new MessageLocker(null));
					broadcastDataTowardEachIntermediatePeer(t2, true);
				}

				startDisconnectionProcess(ConnectionClosedReason.CONNECTION_LOST);
			} else {
				receiveTransferClosedSystemMessage(sender, t, d.getSender(), d.isItAPriority(), false);
			}
		}
	}

	protected void receiveTransferClosedSystemMessage(final AgentAddress sender, TransferClosedSystemMessage t,
			KernelAddress kaSender, boolean isItAPriority, boolean fromTransferAgent) {
		IDTransfer id;
		{

			InterfacedIDTransfer idt = getValidatedInterfacedIDTransfer(sender, t.getIdTransferDestination());

			if (idt == null && !t.getIdTransferDestination().equals(TransferAgent.NullIDTransfer)) {
				processInvalidTransferConnectionProtocol(
						"Received a message to broadcast trough each transfer node (1), but impossible to found the corresponding TransferID "
								+ t.getIdTransferDestination() + " in the receiving router agent " + this);
				return;
			}
			id = idt == null ? t.getIdTransferDestination() : idt.getLocalID();
		}

		InterfacedIDTransfer idtToClose = getValidatedInterfacedIDTransfer(sender, t.getIdTransfer());
		AgentAddress transferAA = null;
		if (idtToClose == null) {
			idtToClose = getInterfacedIDTransferToFinalize(sender, t.getIdTransfer());
			if (idtToClose != null) {
				if (t.isLastPass()) {
					transferAA = transfer_ids_to_finalize.removeTransferAgentAddress(idtToClose.getLocalID());
				} else
					transferAA = transfer_ids_to_finalize.getTransferAgentAddress(idtToClose.getLocalID());
			}
		} else {
			if (t.isLastPass())
				transferAA = transfer_ids.removeTransferAgentAddress(idtToClose.getLocalID());
			else
				transferAA = transfer_ids.getTransferAgentAddress(idtToClose.getLocalID());
		}

		boolean destinationReached = t.getIdTransferDestination().equals(getTransferType())
				&& t.getKernelAddressDestination().equals(getKernelAddress());
		if (idtToClose == null && !destinationReached) {
			return;
		}
		if (transferAA == null || fromTransferAgent) {
			if (destinationReached) {
				if (idtToClose != null && idtToClose.getLocalID().getID() != getTransferType().getID()) {
					TransferClosedSystemMessage t2 = new TransferClosedSystemMessage(idtToClose.getLocalID(),
							t.getKernelAddressDestination(), idtToClose.getLocalID(), t.isLastPass());
					sendMessageWithRole(idtToClose.getTransferToAgentAddress(),
							new ObjectMessage<>(
									new DataToBroadcast(t2, kaSender, isItAPriority, idtToClose.getLocalID())),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else
					return;
			} else {
				if (sender == null) {
					TransferClosedSystemMessage t2 = new TransferClosedSystemMessage(id,
							t.getKernelAddressDestination(), idtToClose.getLocalID(), t.isLastPass());
					broadcastDataTowardEachIntermediatePeer(null, t2, t.getIdTransferDestination(), kaSender,
							isItAPriority);
				} else {
					broadcastDataTowardEachIntermediatePeer(sender, t, t.getIdTransferDestination(), kaSender,
							isItAPriority);
				}
			}
		} else {
			TransferClosedSystemMessage t2 = new TransferClosedSystemMessage(id, getKernelAddress(),
					idtToClose.getLocalID(), t.isLastPass());
			sendMessageWithRole(transferAA, new ObjectMessage<>(t2), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		}
		if (t.isLastPass()) {
			final IDTransfer idFinal = t.getIdTransfer();
			scheduleTask(new Task<Void>(getMadkitConfig().networkProperties.connectionTimeOutInMs) {
				@Override
				public Void call() {
					if (isAlive()) {
						receiveMessage(new ObjectMessage<>((Runnable) () -> {
							InterfacedIDTransfer idt = removeValidatedInterfacedIDTransfer(sender, idFinal);
							if (idt == null)
								removeInterfacedIDTransferToFinalize(sender, idFinal);
						}));
					}
					return null;
				}
			});

		}

	}

	private void receiveTransferProposition(AgentAddress sender, TransferPropositionSystemMessage p) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Receiving receiveTransferProposition (sender=" + sender + ", distant_inet_address="
					+ distant_inet_address + ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
					+ ") : " + p);

		TransferFilter tf = getMadkitConfig().networkProperties.getTransferTriggers();
		p.addIntermediateTestResult(tf == null || tf.newTransferConnectionPropositionToFinalPeers(
				getMadkitConfig().networkProperties, p.getKernelAddressToConnect(), p.getNumberOfIntermediatePeers(),
				p.getAttachedDataForConnection()));
		if (p.getFinalTestResult() && getMadkitConfig().networkProperties
				.isConnectionPossible(this.distant_inet_address, this.local_interface_address, true, true, true)) {
			try {
				InterfacedIDTransfer idDist = new InterfacedIDTransfer(
						sender == null ? IDTransfer.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(this))
								: p.getIdTransfer(),
						null, getKernelAddress());
				idDist.setDistantID(p.getIdTransfer());
				putInterfacedIDTransferToFinalize(sender, idDist);

				this.transfer_ids_to_finalize.putTransferPropositionSystemMessage(idDist, p);

				TransferConfirmationSystemMessage transferConfirmationSystemMessage = new TransferConfirmationSystemMessage(getTransferType(),
						this.distant_kernel_address, getKernelAddress(), idDist.getDistantID(), idDist.getLocalID(),
						getMadkitConfig().networkProperties.getConnectionProtocolProperties(distant_inet_address,
								local_interface_address, true, true).getNumberOfSubConnectionProtocols(),
						false, null, getMadkitConfig().networkProperties.canUsePointToPointTransferredBlockChecker ?new PointToPointTransferedBlockChecker():null);
				transferConfirmationSystemMessage.setMessageLocker(p.getMessageLocker());
				broadcastDataTowardEachIntermediatePeer(transferConfirmationSystemMessage, true);
			} catch (OverflowException e) {
				TransferImpossibleSystemMessage ti = new TransferImpossibleSystemMessage(getTransferType(),
						distant_kernel_address, p.getIdTransfer());
				ti.setMessageLocker(p.getMessageLocker());
				broadcastDataTowardEachIntermediatePeer(ti, true);

				if (logger != null)
					logger.severeLog("Too much transfer connections", e);
			}

		} else {
			TransferImpossibleSystemMessage ti = new TransferImpossibleSystemMessage(getTransferType(),
					distant_kernel_address, p.getIdTransfer());
			ti.setMessageLocker(p.getMessageLocker());
			broadcastDataTowardEachIntermediatePeer(ti, true);
		}
	}

	private void receiveTransferImpossibleAnswer(AgentAddress sender, TransferImpossibleSystemMessage ti) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Receiving transfer impossible as answer (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + ti);
		AgentAddress aat = transfer_ids_to_finalize.getTransferAgentAddress(ti.getYourIDTransfer());
		removeInterfacedIDTransferToFinalize(sender, ti.getYourIDTransfer());

		if (aat == null) {
			processInvalidTransferConnectionProtocol(
					"Received TransferImpossibleSystemMessage, but impossible to found the corresponding TransferID "
							+ ti.getYourIDTransfer() + " in the peer agent " + this);
		} else {
			sendMessageWithRole(aat, new ObjectMessage<>(ti), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		}
	}

	private void receiveTransferImpossibleAnswer(AgentAddress sender,
												 TransferImpossibleSystemMessageFromMiddlePeer ti) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Receiving transfer impossible as answer from middle peer (distant_inet_address="
					+ distant_inet_address + ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
					+ ") : " + ti);
		AgentAddress aat = transfer_ids_to_finalize.getTransferAgentAddress(ti.getYourIDTransfer());

		removeInterfacedIDTransferToFinalize(sender, ti.getYourIDTransfer());

		if (aat == null) {
			processInvalidTransferConnectionProtocol(
					"Received TransferImpossibleSystemMessageFromMiddlePeer, but impossible to found the corresponding TransferID "
							+ ti.getYourIDTransfer() + " in the peer agent " + this);
		} else {
			sendMessageWithRole(aat, new ObjectMessage<>(ti), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		}
	}

	class FailedCreateIndirectAgentSocket extends Message {

		private final Exception e;
		private final InterfacedIDTransfer idLocal;
		private final InterfacedIDTransfer idDist;
		private final IndirectAgentSocket indirectAgentSocket;
		private final DistantKernelAgent dka;
		private final TransferConfirmationSystemMessage ti;
		private final TransferPropositionSystemMessage p;

		FailedCreateIndirectAgentSocket(Exception e, InterfacedIDTransfer idLocal, InterfacedIDTransfer idDist,
				IndirectAgentSocket indirectAgentSocket, DistantKernelAgent dka, TransferConfirmationSystemMessage ti,
				TransferPropositionSystemMessage p) {
			this.e = e;
			this.idLocal = idLocal;
			this.idDist = idDist;
			this.indirectAgentSocket = indirectAgentSocket;
			this.dka = dka;
			this.ti = ti;
			this.p = p;
		}

		void executeTask() {
			if (logger != null)
				logger.severeLog("Start of " + this.getClass().getName() + " (" + distant_inet_address + ") FAILED !",
						e);

			transfer_ids.removeLocal(idLocal.getLocalID());
			transfer_ids.removeDistant(idDist.getDistantID());
			getStatistics().removeTransferAgentStats(idLocal.getLocalID());
			getMadkitConfig().networkProperties.removeStatsBandwidth(idLocal.getLocalID().getID());
			killAgent(indirectAgentSocket);
			killAgent(dka);
			TransferClosedSystemMessage tc = new TransferClosedSystemMessage(getTransferType(),
					p.getKernelAddressToConnect(), idLocal.getLocalID(), true);
			tc.setMessageLocker(ti.getMessageLocker());
			broadcastDataTowardEachIntermediatePeer(tc, true);

		}
	}

	private void receiveTransferConfirmationAnswer(AgentAddress sender, final TransferConfirmationSystemMessage ti) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Receiving transfer confirmation as answer (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + ti);
		AgentAddress aat = transfer_ids_to_finalize.getTransferAgentAddress(ti.getYourIDTransfer());
		if (aat == null) {
			// here we are into a final peer

			validateInterfacedIDTransfer(sender, ti.getMyIDTransfer(), false);

			final InterfacedIDTransfer idtDist = getValidatedInterfacedIDTransfer(sender, ti.getMyIDTransfer());
			final TransferPropositionSystemMessage p = transfer_ids
					.removeTransferPropositionSystemMessage(ti.getYourIDTransfer());

			if (idtDist != null) {
				final DistantKernelAgent distantKernelAgent = new DistantKernelAgent();
				this.launchAgent(distantKernelAgent);
				AgentAddress agentAddressIn = distantKernelAgent.getAgentAddressIn(LocalCommunity.Groups.NETWORK,
						LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);

				if (ti.getDistantInetAddress() == null) {
					TransferClosedSystemMessage tc = new TransferClosedSystemMessage(getTransferType(),
							p.getKernelAddressToConnect(), idtDist.getLocalID(), true);
					tc.setMessageLocker(ti.getMessageLocker());
					removeValidatedInterfacedIDTransfer(sender, ti.getMyIDTransfer());
					broadcastDataTowardEachIntermediatePeer(tc, true);
					return;
				}

				final IndirectAgentSocket indirectAgentSocket = new IndirectAgentSocket(this.distantIP, agentAddressIn,
						this.socket, this.nio_agent_address, ti.getDistantInetAddress(), this.local_interface_address,
						p.isYouAskConnection(), idtDist.getLocalID(), p.getNumberOfIntermediatePeers() + 1,
						this.getStatistics(),
						getAgentAddressIn(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.SOCKET_AGENT_ROLE),
						distant_kernel_address, p.getKernelAddressToConnect(), getDistantInetSocketAddressRoot(),
						getSocketID());
				launchAgent(indirectAgentSocket);
				if (indirectAgentSocket.getState().equals(com.distrimind.madkit.kernel.AbstractAgent.State.LIVING)
						&& distantKernelAgent.getState().equals(com.distrimind.madkit.kernel.AbstractAgent.State.LIVING)) {
					AgentAddress agentAddress = indirectAgentSocket.getAgentAddressIn(LocalCommunity.Groups.NETWORK,
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					final InterfacedIDTransfer idtLocal = new InterfacedIDTransfer(idtDist.getLocalID(), sender,
							ti.getKernelAddressToConnect());
					idtLocal.setDistantID(idtDist.getLocalID());
					idtDist.setTransferToAgentAddress(agentAddress);
					putValidatedInterfacedIDTransfer(agentAddress, idtLocal);

					this.scheduleTask(new Task<Void>() {
						@Override
						public Void call() {
							try {
								indirectAgentSocket.initiateConnectionIfNecessary();
							} catch (Exception e) {
								receiveMessage(new FailedCreateIndirectAgentSocket(e, idtLocal, idtDist,
										indirectAgentSocket, distantKernelAgent, ti, p));
							}
							return null;
						}
					});
				} else {
					killAgent(indirectAgentSocket);
					killAgent(distantKernelAgent);
					TransferClosedSystemMessage tc = new TransferClosedSystemMessage(getTransferType(),
							p.getKernelAddressToConnect(), idtDist.getLocalID(), true);
					tc.setMessageLocker(ti.getMessageLocker());
					removeValidatedInterfacedIDTransfer(sender, ti.getMyIDTransfer());
					broadcastDataTowardEachIntermediatePeer(tc, true);
				}
			}

		} else {
			// here we are into the junction between two peers
			if (getTransferType().equals(TransferAgent.NullIDTransfer)) {

				InterfacedIDTransfer idLocal = transfer_ids_to_finalize.getLocal(ti.getYourIDTransfer());
				idLocal.setDistantID(ti.getMyIDTransfer());
				InterfacedIDTransfer idDist = new InterfacedIDTransfer(idLocal.getLocalID(),
						idLocal.getTransferToAgentAddress(), idLocal.getTransferToKernelAddress());
				idLocal.setTransferToAgentAddress(null);
				idDist.setTransferBlockChecker(
						new ConnectionProtocol.NullBlockChecker(ti.getNumberOfSubBlocks(), false, (short) 0));
				idDist.setDistantID(ti.getMyIDTransfer());
				transfer_ids_to_finalize.putDistant(idDist);

				getStatistics().putTransferAgentStats(idDist.getLocalID(), getMadkitConfig().networkProperties
						.addIfNecessaryAndGetStatsBandwidth(idDist.getLocalID().getID()));
			}

			sendMessageWithRole(aat, new ObjectMessage<>(ti), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		}
	}

	private void broadcastPropositionAnswer(TransferConfirmationSystemMessage t) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Broadcasting proposition confirmation answer (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + t);

		t.setIdTransferDestination(getTransferType());
		validateInterfacedIDTransfer(null, t.getMyIDTransfer(), true);

		if (getTransferType().equals(TransferAgent.NullIDTransfer)) {
			InterfacedIDTransfer idDist = getValidatedInterfacedIDTransfer(null, t.getYourIDTransfer());
			if (idDist == null)
				processInvalidTransferConnectionProtocol(
						"Broadcasting TransferConfirmationSystemMessage, but impossible to found the corresponding TransferID "
								+ t.getMyIDTransfer() + " in the peer agent " + this);
			else {
				AgentAddress aa = transfer_ids.getTransferAgentAddress(idDist.getLocalID());
				if (aa == null) {
					processInvalidTransferConnectionProtocol(
							"Broadcasting TransferConfirmationSystemMessage, but impossible to found the corresponding TransferID "
									+ t.getMyIDTransfer() + " and its transfer agent address");
				} else {
					broadcastDataTowardEachIntermediatePeer(t, true);
				}
			}

		} else {

			InterfacedIDTransfer idMiddle = getValidatedInterfacedIDTransfer(
					((IndirectAgentSocket) this).getParentAgentSocketAddress(), t.getMyIDTransfer());
			if (idMiddle == null) {
				processInvalidTransferConnectionProtocol(
						"Broadcasting TransferConfirmationSystemMessage, but impossible to found the corresponding TransferID "
								+ t.getMyIDTransfer() + " in the middle peer agent " + this);
			} else {
				AgentAddress aa = transfer_ids.getTransferAgentAddress(idMiddle);
				if (aa == null) {
					processInvalidTransferConnectionProtocol(
							"Broadcasting TransferConfirmationSystemMessage, but impossible to found the corresponding TransferID "
									+ t.getMyIDTransfer() + " and its transfer agent address");
				} else {
					/*
					 * transfer_ids.putMiddle(((IndirectAgentSocket)this).
					 * getParentAgentSocketAddress(), idMiddle);
					 * transfer_ids.putTransferAgentAddress(idMiddle.getLocalID(), aa);
					 * 
					 * InterfacedIDTransfer idt=new InterfacedIDTransfer(idMiddle.getLocalID(),
					 * ((IndirectAgentSocket)this).getParentAgentSocketAddress(),this.
					 * distantInterfacedKernelAddress);
					 * transfer_ids.putMiddle(idMiddle.getTransferToAgentAddress(), idt);
					 */
					broadcastDataTowardEachIntermediatePeer(t, true);
				}
			}

		}
	}

	private void broadcastPropositionImpossibleAnswer(TransferImpossibleSystemMessageFromMiddlePeer t) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Broadcasting proposition impossible as answer (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ") : " + t);

		t.setIdTransferDestination(getTransferType());
		InterfacedIDTransfer idLocal = transfer_ids_to_finalize.removeLocal(t.getMyIDTransfer());
		if (idLocal != null) {
			transfer_ids_to_finalize.removeTransferAgentAddress(idLocal);
			t.setYourIDTransfer(idLocal.getDistantID());
			broadcastDataTowardEachIntermediatePeer(t, true);
		}
	}

	InetSocketAddress getDistantInetSocketAddress() {
		return distant_inet_address;
	}

	boolean isTransferReadPaused() {
		return exceededDataQueueSize.get() || dataToTransferInQueue
				.get() > getMadkitConfig().networkProperties.numberOfCachedBytesToTransferBeforeBlockingSocket;
	}


	private void receiveData(byte[] _bytes) {
		lastReceivedDataNano = System.nanoTime();
		dataSynchronizer.receiveData(_bytes, dataSynchronized);
	}

	private Timer timer_read = null;
	private final Timer timer_read_static=new Timer(true);
	private int dataRead = 0;

	protected ReturnCode receiveData(Block _block) {
		try {
			if (_block.getTransferID() != getTransferType().getID()) {
				processInvalidBlockToTransfer(_block);
			}
			if (timer_read == null) {
				timer_read=timer_read_static;
				timer_read.reset();
				getStatistics().newDataReceived(_block.getTransferID(), _block.getBlockSize());
			}
			else {
				getStatistics().newDataReceived(_block.getTransferID(), dataRead,
						timer_read.getDeltaMilli(), _block.getBlockSize());
			}
			dataRead = _block.getBlockSize();
			_block.setTransferID(TransferAgent.NullIDTransfer.getID());
			PacketPart p = getPacketPart(_block);
			ReturnCode rc = sendMessageWithRole(this.agent_for_distant_kernel_aa, new ReceivedBlockData(p),
					LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			if (logger != null && !ReturnCode.isSuccessOrIsTransferInProgress(rc))
				logger.severeLog("Block impossible to transfer to " + this.agent_for_distant_kernel_aa);
			return rc;
		} catch (NIOException e) {
			if (logger != null)
				logger.severeLog("", e);
			return ReturnCode.TRANSFER_FAILED;
		}
	}

	protected ReturnCode receiveDataToResend(Block block, InterfacedIDTransfer idt) {
		ReturnCode rc = sendMessageWithRole(idt.getTransferToAgentAddress(),
				new ResendData(new BlockDataToTransfer(block, idt.getLocalID())),
				LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		if (!ReturnCode.isSuccessOrIsTransferInProgress(rc) && logger != null)
			logger.severeLog("Indirect data impossible to resend to " + idt.getTransferToAgentAddress());
		return rc;
	}

	protected ReturnCode receiveIndirectData(Block block, InterfacedIDTransfer idt) {
		ReturnCode rc = sendMessageWithRole(idt.getTransferToAgentAddress(), new ReceivedIndirectData(block),
				LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		if (!ReturnCode.isSuccessOrIsTransferInProgress(rc) && logger != null)
			logger.severeLog("Indirect data impossible to transfer to " + idt.getTransferToAgentAddress());
		return rc;
	}

	static class ReceivedBlockData extends ObjectMessage<PacketPart> {


		public ReceivedBlockData(PacketPart _content) {
			super(_content);
		}

	}

	@SuppressWarnings("ConstantConditions")
    private void receiveData(Object obj, ReceivedSerializableObject originalMessage) {
		try {
			if (obj == null) {
				processInvalidSerializedObject(null, null, "null reference");
				return;
			}
			if (state.compareTo(State.CONNECTED_INITIALIZING_ACCESS) < 0
					&& !((obj instanceof ConnectionMessage) || (obj instanceof ConnectionInfoSystemMessage)
							|| (obj.getClass()==PingMessage.class) || (obj.getClass()==PongMessage.class))) {
				processInvalidSerializedObject(
						new ConnectionException("Attempting to transmit a message of type " + obj.getClass()
								+ " with a connection not initialized !"),
						obj, "message " + obj.getClass() + " not authorized during connection process", true);
				return;
			} else if (state.compareTo(State.CONNECTED) < 0 && !((obj instanceof AccessMessage)
					|| (obj instanceof ConnectionMessage) || (obj instanceof ConnectionInfoSystemMessage)
					|| (obj.getClass()==PingMessage.class) || (obj.getClass()==PongMessage.class))) {
				processInvalidSerializedObject(
						new ConnectionException("Attempting to transmit a message of type " + obj.getClass()
								+ " with a access not initialized !"),
						obj, "message " + obj.getClass() + " not authorized during access/login process", true);
				return;
			}

			if (originalMessage != null && !(obj instanceof LanMessage))
				originalMessage.markDataAsRead();

			if (obj instanceof SystemMessageWithoutInnerSizeControl) {

				if (obj instanceof ConnectionMessage) {
					boolean sendAskConnectionMessage = false;
					// boolean found=false;
					for (Iterator<ConnectionProtocol<?>> it = connection_protocol.reverseIterator(); it.hasNext();) {

						ConnectionProtocol<?> cp = it.next();

						if (!cp.isConnectionEstablished() && !cp.isConnectionFinishedButClosed()) {
							// found=true;
							try {
								ConnectionClosedReason connection_closed_reason = null;
								ConnectionMessage cm;
								if (sendAskConnectionMessage) {
									sendAskConnectionMessage = false;
									if (this_ask_connection) {
										cm = cp.setAndGetNextMessage(new AskConnection(true));
										if (cm == null)
											throw new NullPointerException(
													"The first returned message by the caller connection protocol must be an AskMessage.");
										if (!(cm instanceof AskConnection))
											throw new ConnectionException(
													"The first returned message by the caller connection protocol must be an AskMessage.");
									} else
										break;

								} else
									cm = cp.setAndGetNextMessage((ConnectionMessage) obj);

								boolean send_data = true;
								if (cm != null) {

									if (cm instanceof ConnectionFinished) {
										connection_closed_reason = ((ConnectionFinished) cm)
												.getConnectionClosedReason();

										if (connection_closed_reason != null) {

											ConnectionFinished cf = null;
											if (obj instanceof ConnectionFinished) {
												cf = (ConnectionFinished) obj;
												if (cf.getState().equals(
														ConnectionProtocol.ConnectionState.CONNECTION_ESTABLISHED))
													cm = null;
												else
													send_data = false;
											}

											if (cf != null) {
												for (Iterator<ConnectionProtocol<?>> it2 = connection_protocol
														.reverseIterator(it); it2.hasNext();) {
													ConnectionProtocol<?> cp2 = it.next();
													cp2.setAndGetNextMessage(cf);
												}
											} else {
												for (Iterator<ConnectionProtocol<?>> it2 = connection_protocol
														.reverseIterator(it); it2.hasNext();) {
													ConnectionProtocol<?> cp2 = it.next();
													cp2.setConnectionClosed(connection_closed_reason);
												}
											}
										}

									} else if (cm instanceof ErrorConnection) {
										ErrorConnection ec = (ErrorConnection) cm;
										processInvalidConnectionMessage(null, (ConnectionMessage) obj, ec);
										ec.candidate_to_ban = false;
									}
								}
								if (cm != null) {
									if (connection_closed_reason != null) {
										startDisconnectionProcess(connection_closed_reason,
												send_data ? (ConnectionFinished) cm : null);
									} else if (send_data) {
										checkTransferBlockCheckerChanges();
										sendData(cm, true, false);
									}
								}
								State oldState = state;
								updateState();
								if (oldState == State.CONNECTION_IN_PROGRESS
										&& state != State.CONNECTION_IN_PROGRESS) {
									if (logger != null && logger.isLoggable(Level.FINER))
										logger.finer(
												"Connection protocols successfully initialized (distant_inet_address="
														+ distant_inet_address + ", distantInterfacedKernelAddress="
														+ distantInterfacedKernelAddress + ")");
									//this.connection_protocol.getCounterSelector().setActivated();
									if (state == State.CONNECTED_INITIALIZING_ACCESS) {
										if (logger != null && logger.isLoggable(Level.FINER))
											logger.finer("Initializing access protocol !");
										AccessMessage am = access_protocol
												.setAndGetNextMessage(new AccessAskInitiliazation());
										if (am != null) {
											checkTransferBlockCheckerChanges();
											if (am instanceof AccessMessagesList)
											{
												for (AccessMessage am2 : ((AccessMessagesList) am).getMessages())
													sendData(am2, true, false);	
											}
											else
												sendData(am, true, false);
										}
									}
								}

							} catch (ConnectionException ce) {
								if (logger != null)
									logger.severeLog("", ce);

							}
							if (cp.isConnectionEstablished()) {
								sendAskConnectionMessage = true;
							} else
								break;
						}
					}
					if (sendAskConnectionMessage) {
						checkTransferBlockCheckerChanges();
						sendConnectionInfoSystemMessage();
					}
						
				} else if (obj.getClass() == ConnectionInfoSystemMessage.class) {
					this.distantConnectionInfo = (ConnectionInfoSystemMessage) obj;
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving connection information message (distant_inet_address="
								+ distant_inet_address + ", distantInterfacedKernelAddress="
								+ distantInterfacedKernelAddress + ") : " + this.distantConnectionInfo);
					AbstractAgentSocket.this.sendMessageWithRole(agent_for_distant_kernel_aa,
							new ObjectMessage<>(this.distantConnectionInfo), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					MadkitKernelAccess.setConnectionInfoSystemMessage(this, new ConnectionIdentifier(getTransferType(), distant_inet_address, local_interface_address), this.distantConnectionInfo);
				} else if (obj instanceof AccessMessage) {
					try {
						ConnectionClosedReason con_close_reason = null;
						if (obj instanceof AccessCancelledMessage) {
							con_close_reason = ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED;
						}

						AccessMessage tsm = access_protocol.setAndGetNextMessage((AccessMessage) obj);
						AccessMessage[] messagesToSend = (tsm instanceof AccessMessagesList) ? ((AccessMessagesList) tsm).getMessages() : new AccessMessage[]{tsm};
						for (AccessMessage toSend : messagesToSend)
						{
							if (toSend != null) {
								if (toSend instanceof DoNotSendMessage) {
									toSend = null;
								}
							}
							if (toSend != null) {
								if (toSend instanceof AccessErrorMessage) {
									AccessErrorMessage aem = (AccessErrorMessage) toSend;
									processInvalidAccessMessage(null, (AccessMessage) obj, aem);
									aem.candidate_to_ban = false;
									con_close_reason = ConnectionClosedReason.CONNECTION_ANOMALY;
								}
							}
							if (toSend != null) {
								short nbAnomalies = toSend.getNbAnomalies();
								if (nbAnomalies > 0) {
									if (processInvalidProcess("Too much anomalies during identification protocol",
											nbAnomalies)) {
										toSend = null;
									}
								}
							}
							if (toSend != null && con_close_reason == null && !(toSend instanceof DoNotSendMessage)) {
								AccessMessage am = toSend;
								while (am != null) {
									sendData(am, true, false);
									if (am.checkDifferedMessages()) {
										am = access_protocol.manageDifferedAccessMessage();
									} else
										am = null;
								}
							}
							if (toSend instanceof AccessCancelledMessage) {
								con_close_reason = ConnectionClosedReason.CONNECTION_PROPERLY_CLOSED;
							}
							if (con_close_reason != null)
								startDisconnectionProcess(con_close_reason);
							else if (this_ask_connection && access_protocol.isNotifyAccessGroupChanges()) {
								notifyNewAccessChanges();
							}
							State oldState = state;
							updateState();
						
							if (logger != null && logger.isLoggable(Level.FINER)
									&& oldState == State.CONNECTED_INITIALIZING_ACCESS && state == State.CONNECTED)
								logger.finer("Access protocol successfully finished (distant_inet_address="
										+ distant_inet_address + ", distantInterfacedKernelAddress="
										+ distantInterfacedKernelAddress + ")");
						}
					} catch (Exception e) {
						if (logger != null)
							logger.severeLog("", e);
					}
				} else if (obj.getClass() == AcceptedGroups.class) {
					AcceptedGroups agm = ((AcceptedGroups) obj);
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Receiving distant accepted groups message (distant_inet_address="
								+ distant_inet_address + ", distantInterfacedKernelAddress="
								+ distantInterfacedKernelAddress + ") : " + agm);

					distant_accepted_and_requested_groups = agm.accepted_groups_and_requested;
					distant_general_accepted_groups = agm.accepted_groups;
					if (distant_kernel_address == null) {
						
						if (agm.kernelAddress.equals(getKernelAddress())) {
							processSameDistantKernelAddressWithLocal(agm.kernelAddress, true);
							return;
						} else {
							// receiving distant kernel address
							distant_kernel_address = agm.kernelAddress;
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Receiving distant kernel address (distant_inet_address="
										+ distant_inet_address + ", distantInterfacedKernelAddress="
										+ distantInterfacedKernelAddress + ") : " + distant_kernel_address);

							// generate secret messages
							this.requestRole(
									LocalCommunity.Groups.getOriginalDistantKernelAgentGroup(distant_kernel_address),
									LocalCommunity.Roles.SOCKET_AGENT_ROLE);
							distant_socket_agent_address = agm.distant_agent_socket_address;
							

							
							// send distant kernel address to DistantKernelAddressAgent
							sendMessageWithRole(agent_for_distant_kernel_aa,
									new ObjectMessage<>(distant_kernel_address),
									LocalCommunity.Roles.SOCKET_AGENT_ROLE);
							// send to DistantKernelAddressAgent logins and access authorized
							sendMessageWithRole(agent_for_distant_kernel_aa,
									new NetworkLoginAccessEvent(distant_kernel_address, my_accepted_logins.identifiers,
											my_accepted_logins.identifiers, null, null, null, null),
									LocalCommunity.Roles.SOCKET_AGENT_ROLE);
							
						}
					}

					// distant_accepted_multi_groups=new MultiGroup(distant_accepted_groups);

					sendMessageWithRole(this.agent_for_distant_kernel_aa,
							new NetworkGroupsAccessEvent(AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_BY_DISTANT_PEER,
									distant_general_accepted_groups, distant_accepted_and_requested_groups,
									distant_kernel_address, false),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else if (obj.getClass() == TooMuchConnectionWithTheSamePeers.class) {
					startDisconnectionProcess(ConnectionClosedReason.CONNECTION_LOST);
				} else if (obj.getClass() == SecretMessage.class) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Receiving secret message (distant_inet_address=" + distant_inet_address
								+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");
					SecretMessage sm = (SecretMessage) obj;
					if (sm.getAgentSocketAddress() == null)// if the received secret message must be transferred or
															// tested
					{
						// the received secret message must be tested
						HashMap<AgentAddress, SecretMessage> secretMessages = currentSecretMessages.get();
						if (secretMessages != null) {
							for (Map.Entry<AgentAddress, SecretMessage> entry : secretMessages.entrySet()) {
								if (sm.equals(entry.getValue())) {
									validateKernelAddress(entry.getKey());
									break;
								}
							}
						}

					} else if (sm.getAgentSocketAddress().isFrom(this.getKernelAddress()))
						sendMessageWithRole(sm.getAgentSocketAddress(), new ObjectMessage<>(sm),
								LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					else if (logger != null)
						logger.warning("Unexpected secret message " + sm);

				} else if (obj.getClass() == DistantKernelAddressValidated.class) {
					this.distantKernelAddressValidated.set(true);
					if (distant_kernel_agent_activated) {
						informHooksForConnectionEstablished();
						sendMessageWithRole(this.agent_for_distant_kernel_aa,
								new ObjectMessage<>((DistantKernelAddressValidated) obj),
								LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					}
				} else if (obj.getClass() == PingMessage.class) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving ping message and sending pong message (distant_inet_address="
								+ distant_inet_address + ", distantInterfacedKernelAddress="
								+ distantInterfacedKernelAddress + ")");
					sendData(new PongMessage(), true, false);
				} else if (obj.getClass() == PongMessage.class) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving pong message (distant_inet_address=" + distant_inet_address
								+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ")");
					waitingPongMessage = false;
					sendMessageWithRole(nio_agent_address, new PongMessageReceived(getSocketID()),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				}

				else if (obj.getClass() == TryDirectConnection.class) {
					TryDirectConnection tdc = (TryDirectConnection) obj;
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving request to try direct connection (distant_inet_address="
								+ distant_inet_address + ", distantInterfacedKernelAddress="
								+ distantInterfacedKernelAddress + ") : " + tdc);

					AskForConnectionMessage ask = new AskForConnectionMessage(ConnectionStatusMessage.Type.CONNECT,
							new DoubleIP(tdc.getInetSocketAddress()), false);
					ask.chooseIP(true, true);
					ask.setJoinedPiece(tdc,
							getAgentAddressIn(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.SOCKET_AGENT_ROLE));
					broadcastMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.LOCAL_NETWORK_ROLE,
							ask, LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else if (obj.getClass() == DirectConnectionFailed.class) {
					broadcastMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.TRANSFER_AGENT_ROLE,
							new ObjectMessage<>((DirectConnectionFailed) obj), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else if (obj.getClass() == DirectConnectionSucceeded.class) {
					broadcastMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.TRANSFER_AGENT_ROLE,
							new ObjectMessage<>((DirectConnectionSucceeded) obj),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else if (obj.getClass() == CGRSynchroSystemMessage.class) {
					if (logger != null && logger.isLoggable(Level.FINER))
						logger.finer("Receiving CGRSynchro message (distantInterfacedKernelAddress="
								+ distant_kernel_address + ") : " + obj);
					CGRSynchroSystemMessage cgr = ((CGRSynchroSystemMessage) obj);

					if (!my_accepted_groups.acceptDistant(cgr.getCGRSynchro().getContent()))
						processInvalidSerializedObject(null, cgr, "Invalid CGR Synchro with local interfaced kernel address");
					else
						sendMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NET_AGENT,
							cgr.getCGRSynchro(), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else if (obj.getClass() == CGRSynchrosSystemMessage.class) {
					sendMessageWithRole(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.NET_AGENT,
							((CGRSynchrosSystemMessage) obj).getCGRSynchros(distantInterfacedKernelAddress, my_accepted_groups.groups),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else if (obj.getClass() == ValidateBigDataProposition.class) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving big data proposition validation (distant_inet_address="
								+ distant_inet_address + ", distantInterfacedKernelAddress="
								+ distantInterfacedKernelAddress + ") : " + obj);

					sendMessageWithRole(this.agent_for_distant_kernel_aa,
							new ObjectMessage<>((ValidateBigDataProposition) obj),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				} else if (obj instanceof LanMessage && originalMessage != null) {
					if (obj.getClass() == DirectLanMessage.class) {
						DirectLanMessage dlm = (DirectLanMessage) obj;
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest("Receiving direct lan message (distant_inet_address=" + distant_inet_address
									+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
									+ ", conversationID=" + dlm.message.getConversationID() + ")");

						if (dlm.message.getReceiver().getKernelAddress().equals(getKernelAddress())
								&& my_accepted_groups.acceptLocal(dlm.message.getReceiver())
								&& my_accepted_groups.acceptDistant(dlm.message.getSender())) {
							this.sendMessageWithRole(this.agent_for_distant_kernel_aa,
									new ObjectMessage<>(originalMessage),
									LocalCommunity.Roles.SOCKET_AGENT_ROLE);
						}
					} else if (obj.getClass() == BroadcastLanMessage.class) {
						BroadcastLanMessage blm = (BroadcastLanMessage) obj;
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest(
									"Receiving broadcast lan message (distant_inet_address=" + distant_inet_address
											+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
											+ ", conversationID=" + blm.message.getConversationID() + ")");

						MultiGroup bGroups = my_accepted_groups.getAcceptedGroups(blm.abstract_group, blm.agentAddressesSender);
						if (!bGroups.isEmpty()) {
							blm.setAcceptedGroups(bGroups);
							this.sendMessageWithRole(this.agent_for_distant_kernel_aa,
									new ObjectMessage<>(originalMessage),
									LocalCommunity.Roles.SOCKET_AGENT_ROLE);
						}
					} else {
						processInvalidSerializedObject(
								new NIOException("Unknown type message " + obj.getClass().getCanonicalName()), obj,
								null);
						originalMessage.markDataAsRead();
					}
				} else if (obj.getClass() == DataToBroadcast.class) {
					DataToBroadcast d = (DataToBroadcast) obj;
					receiveBroadcastData(null, d, true);
				} else {
					processInvalidSerializedObject(
							new NIOException("Unknown type message " + obj.getClass().getCanonicalName()), obj, null);
				}

			} else {
				processInvalidSerializedObject(
						new NIOException("Unknown type message " + obj.getClass().getCanonicalName()), obj, null);
			}
		} catch (SelfKillException e) {
			throw e;
		} catch (Exception e) {
			if (logger != null)
				logger.severeLog("Unexpected exception", e);
		}
		
	}

	

	
	
	private void informHooksForConnectionEstablished() {
		if (logger != null)
			logger.info("Connection established (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ", local_interface="
					+ local_interface_address + ")");

		MadkitKernelAccess.informHooks(this,
				new NetworkEventMessage(AgentActionEvent.CONNEXION_ESTABLISHED, new Connection(
						new ConnectionIdentifier(getTransferType(), distant_inet_address, local_interface_address),
						distantInterfacedKernelAddress, distantConnectionInfo)));
	}

	protected abstract void checkTransferBlockCheckerChanges() throws ConnectionException;

	void validateKernelAddress(AgentAddress concernedDistantKernelAgent) {
		
		if (currentSecretMessages.getAndSet(null) != null) {
			if (concernedDistantKernelAgent.equals(this.agent_for_distant_kernel_aa)) {
				// the default DistantKernelAgent can be activated, and the distant kernel
				// address must be interfaced
				AbstractAgentSocket.this.sendMessageWithRole(agent_for_distant_kernel_aa,
						new KernelAddressValidation(true), LocalCommunity.Roles.SOCKET_AGENT_ROLE);
			} else {

				AgentAddress aa = this.agent_for_distant_kernel_aa;
				if (isLocalAgentAddressValid(concernedDistantKernelAgent))
					agent_for_distant_kernel_aa = concernedDistantKernelAgent;
				else // the agent have been deleted during the connection process
					agent_for_distant_kernel_aa = null;

				if (agent_for_distant_kernel_aa == null) {
					// create a new distant kernel agent
					DistantKernelAgent agent = new DistantKernelAgent();
					launchAgent(agent);
					agent_for_distant_kernel_aa = agent.getAgentAddressIn(
							LocalCommunity.Groups.DISTANT_KERNEL_AGENTS_GROUPS,
							LocalCommunity.Roles.DISTANT_KERNEL_AGENT_ROLE);
					sendMessageWithRole(this.agent_for_distant_kernel_aa,
							new ObjectMessage<>(distant_kernel_address),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				}
				if (!requestRole(
						LocalCommunity.Groups
								.getDistantKernelAgentGroup(agent_for_distant_kernel_aa.getAgentNetworkID()),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE).equals(ReturnCode.SUCCESS)) {
					Logger logger=getLogger();
					if (logger!=null)
						logger.severe("Cannot request group "
							+ LocalCommunity.Groups
							.getDistantKernelAgentGroup(agent_for_distant_kernel_aa.getAgentNetworkID())
							+ " and role " + LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				}
				// send data to distant kernel agent
				ReturnCode rc=sendMessageWithRole(this.agent_for_distant_kernel_aa,
						new ObjectMessage<>(new AgentSocketData(this)),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				if (!ReturnCode.isSuccessOrIsTransferInProgress(rc)) {
					Logger logger=getLogger();
					if (logger!=null)
						logger.severe("Unable to send message to distant kernel agent");
				}
				rc=sendMessageWithRole(this.agent_for_distant_kernel_aa,
						new NetworkGroupsAccessEvent(AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_BY_DISTANT_PEER,
								distant_general_accepted_groups, distant_accepted_and_requested_groups,
								distant_kernel_address, false),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				if (!ReturnCode.isSuccessOrIsTransferInProgress(rc)) {
					Logger logger=getLogger();
					if (logger!=null)
						logger.severe("Unable to send message to distant kernel agent");
				}
				rc=sendMessageWithRole(
						this.agent_for_distant_kernel_aa, new NetworkLoginAccessEvent(distant_kernel_address,
								my_accepted_logins.identifiers, my_accepted_logins.identifiers, null, null, null, null),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				if (!ReturnCode.isSuccessOrIsTransferInProgress(rc)) {
					Logger logger=getLogger();
					if (logger!=null)
						logger.severe("Unable to send message to distant kernel agent");
				}
				rc=sendMessageWithRole(agent_for_distant_kernel_aa, new KernelAddressValidation(true),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				if (!ReturnCode.isSuccessOrIsTransferInProgress(rc)) {
					Logger logger=getLogger();
					if (logger!=null)
						logger.severe("Unable to send message to distant kernel agent");
				}
				// validate kernel address
				rc=this.sendMessageWithRole(aa, new KernelAddressValidation(false),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				if (!ReturnCode.isSuccessOrIsTransferInProgress(rc)) {
					Logger logger=getLogger();
					if (logger!=null)
						logger.severe("Unable to send message to distant kernel agent");
				}

				if (my_accepted_groups != null)
					my_accepted_groups.notifyDistantKernelAgent();

			}
		}
		
	}

	private void updateState() {
		if (!state.equals(State.DISCONNECTION_IN_PROGRESS) && !state.equals(State.DISCONNECTION)) {
			if (isConnectionEstablished()) {
				if (access_protocol.isAccessFinalized()) {
					state = State.CONNECTED;
				} else {
					state = State.CONNECTED_INITIALIZING_ACCESS;
				}
			} else
				state = State.CONNECTION_IN_PROGRESS;
		}
	}

	@Override
	public void notifyNewAccessChanges() {
		if (this_ask_connection || distant_kernel_address!=null)
		{
			my_accepted_groups.updateGroups(access_protocol.getGroupsAccess());
			my_accepted_logins.updateData();
		}
	}

	private Groups my_accepted_groups = null;
	private Logins my_accepted_logins = null;
	// private MultiGroup distant_accepted_multi_groups=null;
	private ListGroupsRoles distant_accepted_and_requested_groups = null;
	private ListGroupsRoles distant_general_accepted_groups = null;
	KernelAddress distant_kernel_address = null;
	// KernelAddressInterfaced distant_kernel_address_interfaced=null;

	class Groups
	{
		private volatile ListGroupsRoles groups;
		// private boolean auto_requested=false;
		private volatile ListGroupsRoles represented_groups = null;

		private AgentAddress distant_agent_address = null;
		private boolean kernelAddressSent = false;

		@SuppressWarnings("unused")
		protected Groups() {
			this(null);
		}

		protected Groups(ListGroupsRoles _groups) {
			if (_groups == null)
				groups = new ListGroupsRoles();
			else
				updateGroups(_groups);
		}

		boolean acceptLocal(AgentAddress add) {
			return groups.includesLocal(AbstractAgentSocket.this.getKernelAddress(), add);
		}

		boolean acceptDistant(AgentAddress add) {
			return groups.includesDistant(AbstractAgentSocket.this.distantInterfacedKernelAddress, add);
		}


		MultiGroup getAcceptedGroups(AbstractGroup group, Collection<AgentAddress> agentsAddressesSender) {
			return groups.intersect(getKernelAddress(), distant_kernel_address, group, agentsAddressesSender);
		}

		public ListGroupsRoles getGroups() {
			return groups;
		}

		protected void updateGroups(ListGroupsRoles _groups) {
			if (_groups != null) {
				// MultiGroup old_groups=groups;
				groups = _groups;

				// auto_requested=true;
				boolean changes = areThereDetectedChanges();
				if (changes)
					notifyGroupChanges();
				if (changes || distant_agent_address != agent_for_distant_kernel_aa)
				{
					notifyDistantKernelAgent();
				}

			}
		}

		protected void notifyDistantKernelAgent() {
			distant_agent_address = agent_for_distant_kernel_aa;
			if (agent_for_distant_kernel_aa != null)
				sendMessageWithRole(agent_for_distant_kernel_aa, new ObjectMessage<>(this),
						LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		}

		private boolean areThereDetectedChanges() {
			synchronized (this) {
				if (groups.areDetectedChanges(represented_groups,AbstractAgentSocket.this.getKernelAddress(),  kernelAddressSent))
				{
					represented_groups=groups.getListWithRepresentedGroupsRoles(AbstractAgentSocket.this.getKernelAddress());
					return true;
				}
				else
					return false;
			}
		}

		public AcceptedGroups potentialChangesInGroups() {
			if (getState().compareTo(AbstractAgent.State.ACTIVATED) >= 0 && areThereDetectedChanges())
				return getGroupChanges();
			return null;

		}

		private void notifyGroupChanges() {
			AcceptedGroups ag = getGroupChanges();

			if (ag != null)
				AbstractAgentSocket.this.sendData(ag, true, false);
		}

		private AcceptedGroups getGroupChanges() {
			if (represented_groups != null && access_protocol.isAccessFinalized()) {
				kernelAddressSent = true;
				try {
					return new AcceptedGroups(groups, represented_groups, getKernelAddress(), AbstractAgentSocket.this
							.getAgentAddressIn(LocalCommunity.Groups.NETWORK, LocalCommunity.Roles.SOCKET_AGENT_ROLE));
				} catch (Exception e) {
					if (logger != null)
						logger.severeLog("Invalid accepted groups or no accepted groups by access data : ", e);
					startDisconnectionProcess(ConnectionClosedReason.CONNECTION_ANOMALY);
				}
				// notifyDistantKernelAgent();
			}
			return null;
		}

	}

	private class Logins {
		ArrayList<PairOfIdentifiers> identifiers = new ArrayList<>();

		Logins() {
		}

		void updateData() {
			if (access_protocol != null) {
				ArrayList<PairOfIdentifiers> accepted = access_protocol.getLastAcceptedIdentifiers();
				ArrayList<CloudIdentifier> lastDeniedCloudIdentifiersToOther = access_protocol.getLastDeniedCloudIdentifiersToOther();
				ArrayList<Identifier> lastDeniedIdentifiersFromOther = access_protocol.getLastDeniedIdentifiersFromOther();
				ArrayList<Identifier> lastDeniedIdentifiersToOther = access_protocol.getLastDeniedIdentifiersToOther();
				ArrayList<PairOfIdentifiers> unLogged = access_protocol.getLastUnLoggedIdentifiers();
				identifiers = access_protocol.getAllAcceptedIdentifiers();

				if (distant_kernel_address != null) {
					sendMessageWithRole(AbstractAgentSocket.this.agent_for_distant_kernel_aa,
							new NetworkLoginAccessEvent(distant_kernel_address, identifiers, accepted, lastDeniedCloudIdentifiersToOther,
									lastDeniedIdentifiersFromOther,lastDeniedIdentifiersToOther,
									unLogged),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				}
			}
		}
	}

	private boolean isConnectionEstablished() {
		return connection_protocol.isConnectionEstablishedForAllSubProtocols();
	}


	protected InterfacedIDTransfer routesData(Block _block) throws RouterException, BlockParserException {
		boolean valid = _block.isValid();
		if (valid) {
			if (_block.isDirect() || getTransferType().equals(_block.getTransferID())) {
				return null;
			} else {
				InterfacedIDTransfer idt = transfer_ids.getDistant(_block.getTransferID());

				if (idt == null)
					throw new RouterException("Unknown transfer ID : " + _block.getTransferID());

				return idt;
			}
		} else
			throw new BlockParserException();
	}

	//protected IDGeneratorInt packet_id_generator = new IDGeneratorInt();

	protected ReturnCode broadcastDataTowardEachIntermediatePeer(BroadcastableSystemMessage _data, boolean priority) {
		return broadcastDataTowardEachIntermediatePeer(
				new DataToBroadcast(_data, getKernelAddress(), priority, _data.getIdTransferDestination()),
				priority);
	}

	protected final ReturnCode broadcastDataTowardEachIntermediatePeer(AgentAddress sender,
																	   BroadcastableSystemMessage _data, IDTransfer distantIDDestination, DataToBroadcast d) {
		return broadcastDataTowardEachIntermediatePeer(sender, _data, distantIDDestination, d.getSender(),
				d.isItAPriority());
	}

	protected ReturnCode broadcastDataTowardEachIntermediatePeer(AgentAddress sender, BroadcastableSystemMessage _data,
			IDTransfer distantIDDestination, KernelAddress kaServer, boolean isItAPriority) {
		if (sender == null) {
			InterfacedIDTransfer idt = getValidatedInterfacedIDTransfer(null, distantIDDestination);
			if (idt == null)
				idt = getInterfacedIDTransferToFinalize(null, distantIDDestination);
			if (idt == null) {
				if (logger != null)
					logger.warning("Impossible to route data : " + _data);
				return ReturnCode.NO_RECIPIENT_FOUND;
			}

			return sendMessageWithRole(idt.getTransferToAgentAddress(),
					new ObjectMessage<>(
							new DataToBroadcast(_data, kaServer, isItAPriority, _data.getIdTransferDestination())),
					LocalCommunity.Roles.SOCKET_AGENT_ROLE);
		} else
			return broadcastDataTowardEachIntermediatePeer(_data, isItAPriority);
	}

	protected ReturnCode broadcastDataTowardEachIntermediatePeer(DataToBroadcast _data, boolean isItAPriority) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Broadcasting indirect data toward each intermediate peer (distant_inet_address="
					+ distant_inet_address + ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress
					+ ", isItAPriority=" + isItAPriority + ") : " + _data);
		return sendData(_data, isItAPriority, false);
	}

	protected ReturnCode sendData(SystemMessageWithoutInnerSizeControl _data, boolean priority, boolean last_message) {
		if (logger != null && logger.isLoggable(Level.FINEST))
			logger.finest("Ask for data routing (distant_inet_address=" + distant_inet_address
					+ ", distantInterfacedKernelAddress=" + distantInterfacedKernelAddress + ", priority="
					+ priority + ", last_message=" + last_message + ")");
		return sendMessageWithRole(this.agent_for_distant_kernel_aa,
				new DistantKernelAgent.SendDataFromAgentSocket(_data, last_message, priority),
				LocalCommunity.Roles.SOCKET_AGENT_ROLE);
	}



	private PacketPart getPacketPart(Block _block) throws NIOException {
		try {
			return connection_protocol.getPacketPart(_block, getMadkitConfig().networkProperties);
		} catch (NIOException e) {
			if (e.isInvalid()) {
				processInvalidBlock(null, _block, e.isCandidateForBan());
				return null;
			} else
				throw e;
		}
	}

	private boolean processInvalidSerializedObject(Exception e, Object data, String message) {
		return processInvalidSerializedObject(e, data, message, false);
	}

	@SuppressWarnings("unused")
	private boolean processInvalidSerializedObject(Exception e, Object data, String message, boolean candidate_to_ban) {
		return processInvalidProcess("Invalid serialized object from Kernel Address " + distantInterfacedKernelAddress
				+ " and InetSocketAddress " + distant_inet_address + " : " + message, e, candidate_to_ban);
	}

	@SuppressWarnings("SameParameterValue")
	private boolean processSameDistantKernelAddressWithLocal(KernelAddress ka, boolean candidate_to_ban) {
		return processInvalidProcess(
				"The received distant kernel address is the same than the local kernel address : " + ka,
				candidate_to_ban);
	}

	@SuppressWarnings("unused")
    protected boolean processInvalidBlock(Exception e, Block _block, boolean candidate_to_ban) {
		return processInvalidProcess("Invalid block from Kernel Address " + distantInterfacedKernelAddress
				+ " and InetSocketAddress " + distant_inet_address, e, candidate_to_ban);
	}

	@SuppressWarnings({"SameParameterValue", "unused"})
    private boolean processInvalidAccessMessage(Exception e, AccessMessage received, AccessErrorMessage returned) {
		return processInvalidProcess("Invalid access message from Kernel Address " + distantInterfacedKernelAddress
				+ " and InetSocketAddress " + distant_inet_address+". "+((returned!=null && returned.getLogMessage()!=null)?returned.getLogMessage():""), e, returned != null && returned.candidate_to_ban);
	}

	private boolean processExternalAnomaly(String message, boolean candidate_to_ban) {
		return processInvalidProcess(message, candidate_to_ban);
	}

	@SuppressWarnings({"SameParameterValue", "unused"})
	private boolean processInvalidConnectionMessage(Exception e, ConnectionMessage received, ErrorConnection returned) {
		return processInvalidProcess("Invalid connection message from Kernel Address " + distantInterfacedKernelAddress
				+ " and InetSocketAddress " + distant_inet_address, e, returned.candidate_to_ban);
	}

	private boolean processInvalidTransferConnectionProtocol(String message) {
		return processInvalidProcess(message, false);
	}

	private boolean processInvalidBlockToTransfer(Block _block) {
		return processInvalidProcess("Invalid block to transfer. Unknown TransferID " + _block.getTransferID(), false);
	}

	private boolean processInvalidProcess(String message, boolean candidate_to_ban) {
		return processInvalidProcess(message, null, candidate_to_ban);
	}

	@SuppressWarnings("SameParameterValue")
    boolean processInvalidProcess(String message, short nbAnomalies) {
		try {
			MadkitKernelAccess.informHooks(this, new NetworkAnomalyEvent(distantInterfacedKernelAddress,
					distant_inet_address.getAddress(), false, message));
			if (getMadkitConfig().getDatabaseWrapper() != null) {
				try {
					IPBanned.Record r = getMadkitConfig().getDatabaseWrapper()
							.getTableInstance(IPBanStat.class).processExpulsion(nbAnomalies,
									distant_inet_address.getAddress(), false,
									getMadkitConfig().networkProperties.expulsionDurationInMs,
									getMadkitConfig().networkProperties.nbMaxAnomaliesBeforeTriggeringExpulsion,
									getMadkitConfig().networkProperties.nbMaxExpulsions,
									getMadkitConfig().networkProperties.banishmentDuration,
									getMadkitConfig().networkProperties.nbMaxAnomaliesBeforeTriggeringBanishment,
									getMadkitConfig().networkProperties.nbMaxBans,
									getMadkitConfig().networkProperties.expulsionStatisticDuration,
									getMadkitConfig().networkProperties.banishmentStatisticDuration,
									getMadkitConfig().networkProperties.getAllowInetAddressesList());
					if ((r != null && r.expirationTimeUTC > System.currentTimeMillis())) {
						MadkitKernelAccess.informHooks(this,
								new IPBannedEvent(distant_inet_address.getAddress(), r.expirationTimeUTC));
						startDisconnectionProcess(ConnectionClosedReason.CONNECTION_ANOMALY);
						return true;
					}
				} catch (DatabaseException e2) {
					if (logger != null)
						logger.severeLog("Database exception", e2);
				}
			} else if (nbAnomalies >= getMadkitConfig().networkProperties.nbMaxAnomaliesBeforeTriggeringExpulsion) {
				startDisconnectionProcess(ConnectionClosedReason.CONNECTION_ANOMALY);
				return true;
			}
		} catch (Exception e) {
			if (logger != null)
				logger.severeLog("Unexpected exception", e);
			else
				e.printStackTrace();
		}
		return false;
	}

	boolean proceedEventualBan(boolean candidate_to_ban) 
	{
		try
		{
			if (getMadkitConfig().getDatabaseWrapper() != null) {
				IPBanned.Record r = getMadkitConfig().getDatabaseWrapper()
						.getTableInstance(IPBanStat.class).processExpulsion(distant_inet_address.getAddress(),
								candidate_to_ban, getMadkitConfig().networkProperties.expulsionDurationInMs,
								getMadkitConfig().networkProperties.nbMaxAnomaliesBeforeTriggeringExpulsion,
								getMadkitConfig().networkProperties.nbMaxExpulsions,
								getMadkitConfig().networkProperties.banishmentDuration,
								getMadkitConfig().networkProperties.nbMaxAnomaliesBeforeTriggeringBanishment,
								getMadkitConfig().networkProperties.nbMaxBans,
								getMadkitConfig().networkProperties.expulsionStatisticDuration,
								getMadkitConfig().networkProperties.banishmentStatisticDuration,
								getMadkitConfig().networkProperties.getAllowInetAddressesList());
				if ((r != null && r.expirationTimeUTC > System.currentTimeMillis()) || candidate_to_ban) {
					if (r != null && r.expirationTimeUTC > System.currentTimeMillis()) {
						MadkitKernelAccess.informHooks(this,
								new IPBannedEvent(distant_inet_address.getAddress(), r.expirationTimeUTC));
					}
					
					return true;
				}
			} 
			return false;
		} catch (DatabaseException e2) {
			if (logger != null)
				logger.severeLog("Database exception", e2);
			return false;
		}
	}
	
	boolean processInvalidProcess(String message, Exception e, boolean candidate_to_ban) {
		if (logger != null) {
			if (e == null)
				logger.severeLog(message == null ? "Invalid process" : message);
			else
				logger.severeLog(message == null ? "Invalid process" : message, e);
		}
		MadkitKernelAccess.informHooks(this, new NetworkAnomalyEvent(distantInterfacedKernelAddress,
				distant_inet_address.getAddress(), candidate_to_ban, message));
		
		proceedEventualBan(candidate_to_ban);
		
		if (candidate_to_ban) {
			startDisconnectionProcess(ConnectionClosedReason.CONNECTION_ANOMALY);
			return true;
		}
		

		return false;
	}

	public boolean isBannedOrDefinitelyRejected() {
		return isBanned;

	}

	protected Block getBlock(WritePacket _packet, int _transfer_type, boolean excludedFromEncryption) throws NIOException {
		return connection_protocol.getBlock(_packet, _transfer_type, need_random ? random : null, excludedFromEncryption);
	}

	static protected abstract class BlockData extends AbstractData {
		private final IDTransfer id_transfer;
		private ByteBuffer buffer;
		private final Block block;
		

		BlockData(boolean priority, Block _block, IDTransfer id) {
			super(priority);
			block = _block;
			buffer = ByteBuffer.wrap(_block.getBytes(), 0, _block.getBlockSize());
			id_transfer = id;
		}

		@Override
		boolean isDataBuildInProgress()
		{
			return false;
		}
		
		Block getBlock() {
			return block;
		}

		@Override
		public void unlockMessage(boolean cancel) {
		}

		@Override
		public boolean isUnlocked() {
			return true;
		}

		@Override
		public ByteBuffer getByteBuffer() {
			try
			{
				return buffer;
			}
			finally
			{
				buffer=null;
			}
		}
		@Override
		boolean isCanceledNow()
		{
			return isCanceled() && (buffer==null || buffer.remaining()==0 || buffer.position()==0);
		}

		@Override
		public boolean isFinished() {
			return buffer==null;//buffer.remaining() == 0;
		}

		@Override
		public boolean isCurrentByteBufferFinished() {
			return buffer==null;//buffer.remaining() == 0;
		}
		@Override
		public boolean isCurrentByteBufferFinishedOrNotStarted() {
			return isCurrentByteBufferFinished();
		}

		@Override
		DataTransferType getDataTransferType() {
			return DataTransferType.SHORT_DATA;
		}

		@Override
		IDTransfer getIDTransfer() {
			return id_transfer;
		}

	}

	protected class BlockDataToTransfer extends BlockData {

		private boolean is_locked = true;

		BlockDataToTransfer(Block _block, IDTransfer _id_transfer) {
			super(false, _block, _id_transfer);
			dataToTransferInQueue.addAndGet(getBlock().getBlockSize());
		}

		@Override
		public void unlockMessage(boolean cancel) {
			if (is_locked) {
				is_locked = false;
				dataToTransferInQueue.addAndGet(-getBlock().getBlockSize());

			}
		}

		@Override
		public boolean isUnlocked() {
			return is_locked;
		}

		@Override
		DataTransferType getDataTransferType() {
			return DataTransferType.DATA_TO_TRANSFER;
		}

		@Override
		public void reset() {
			// TODO manage connection closed
		}

		@Override 
		Object getLocker()
		{
			return null;
		}


	}

	protected class TransferIDs {
		private final HashMap<Integer, HashMap<AgentAddress, InterfacedIDTransfer>> middle_transfer_ids = new HashMap<>();
		private final HashMap<Integer, InterfacedIDTransfer> local_transfer_ids = new HashMap<>();
		private final HashMap<Integer, InterfacedIDTransfer> distant_transfer_ids = new HashMap<>();
		private final HashMap<Integer, AgentAddress> transfer_agents = new HashMap<>();
		private final HashMap<Integer, TransferPropositionSystemMessage> propositions_in_progress = new HashMap<>();

		@SuppressWarnings("BooleanMethodIsAlwaysInverted")
        protected boolean hasDataToCheck() {
			return middle_transfer_ids.size() > 0 || local_transfer_ids.size() > 0 || distant_transfer_ids.size() > 0;
		}

		protected void removeObsoleteData() {
			removeObsoleteData(local_transfer_ids, getDelayInMsBeforeTransferNodeBecomesObsolete());
			removeObsoleteData(distant_transfer_ids, getDelayInMsBeforeTransferNodeBecomesObsolete());
			removeObsoleteMiddleData(middle_transfer_ids, getDelayInMsBeforeTransferNodeBecomesObsolete());
		}

		private void removeObsoleteData(HashMap<Integer, InterfacedIDTransfer> data, long timeOutInMs) {
			long timeOutNano = System.nanoTime() - timeOutInMs*1000000L;
			for (Iterator<Map.Entry<Integer, InterfacedIDTransfer>> it = data.entrySet().iterator(); it.hasNext();) {
				Map.Entry<Integer, InterfacedIDTransfer> e = it.next();
				if (e.getValue().getLastAccessNano() < timeOutNano) {
					AgentAddress aa = transfer_agents.remove(e.getValue().getLocalID().getID());

					if (!getTransferType().equals(e.getValue().getLocalID()) && aa != null && checkAgentAddress(aa)) {
						TransferClosedSystemMessage t = new TransferClosedSystemMessage(e.getValue().getLocalID(),
								e.getValue().getTransferToKernelAddress(), e.getValue().getLocalID(), true);
						sendMessageWithRole(aa,
                                new ObjectMessage<>(new DataToBroadcast(t,
                                        e.getValue().getTransferToKernelAddress(), true, e.getValue().getLocalID())),
								LocalCommunity.Roles.SOCKET_AGENT_ROLE);
					}

					propositions_in_progress.remove(e.getValue().getLocalID().getID());
					it.remove();
				}
			}
		}

		private void removeObsoleteMiddleData(HashMap<Integer, HashMap<AgentAddress, InterfacedIDTransfer>> data,
				long timeOutInMs) {
			long timeOutNano = System.nanoTime() - timeOutInMs;
			for (Iterator<Map.Entry<Integer, HashMap<AgentAddress, InterfacedIDTransfer>>> it = data.entrySet()
					.iterator(); it.hasNext();) {
				HashMap<AgentAddress, InterfacedIDTransfer> hm = it.next().getValue();
				boolean remove = false;
				for (InterfacedIDTransfer idt : hm.values()) {
					if (idt.getLastAccessNano() < timeOutNano) {
						remove = true;
						AgentAddress aa = transfer_agents.remove(idt.getLocalID().getID());

						if (!getTransferType().equals(idt.getLocalID()) && aa != null && checkAgentAddress(aa)) {
							TransferClosedSystemMessage t = new TransferClosedSystemMessage(idt.getLocalID(),
									idt.getTransferToKernelAddress(), idt.getLocalID(), true);
							sendMessageWithRole(aa,
                                    new ObjectMessage<>(new DataToBroadcast(t,
                                            idt.getTransferToKernelAddress(), true, idt.getLocalID())),
									LocalCommunity.Roles.SOCKET_AGENT_ROLE);
						}
						propositions_in_progress.remove(idt.getLocalID().getID());
						break;
					}
				}
				if (remove)
					it.remove();
			}
		}


		protected void closeAllTransferID(boolean returnsToCaller) {
			AbstractAgentSocket.this.removeTaskTransferCheckerIfNecessary();

			for (Map.Entry<Integer, InterfacedIDTransfer> e : distant_transfer_ids.entrySet()) {
				getMadkitConfig().networkProperties.removeStatsBandwidth(e.getValue().getLocalID().getID());
				AgentAddress aa = transfer_agents.get(e.getValue().getLocalID().getID());
				if (aa == null) {
					if (e.getValue().getTransferToAgentAddress() != null)
						broadcastDataTowardEachIntermediatePeer(null,
								new TransferClosedSystemMessage(e.getValue().getLocalID(),
										e.getValue().getTransferToKernelAddress(), e.getValue().getLocalID(), true),
								e.getValue().getDistantID(), AbstractAgentSocket.this.getKernelAddress(), true);
				} else {
					sendMessageWithRole(aa,
							new ObjectMessage<>(new TransferClosedSystemMessage(null,
									AbstractAgentSocket.this.getKernelAddress(), e.getValue().getLocalID(), true)),
							LocalCommunity.Roles.SOCKET_AGENT_ROLE);
				}
				if (returnsToCaller) {
					TransferClosedSystemMessage t = new TransferClosedSystemMessage(e.getValue().getLocalID(),
							AbstractAgentSocket.this.distant_kernel_address, e.getValue().getLocalID(), true);
					t.setMessageLocker(new MessageLocker(null));
					AbstractAgentSocket.this.broadcastDataTowardEachIntermediatePeer(t, true);
				}
			}

			for (Map.Entry<Integer, HashMap<AgentAddress, InterfacedIDTransfer>> e : middle_transfer_ids.entrySet()) {

				if (e.getValue().size() > 0) {
					InterfacedIDTransfer idt = getMiddle(e.getKey(),
							((IndirectAgentSocket) AbstractAgentSocket.this).getParentAgentSocketAddress());
					if (idt != null) {
						AgentAddress aa = transfer_agents.get(idt.getLocalID().getID());

						if (aa == null) {
							if (idt.getTransferToAgentAddress() != null) {
								AbstractAgentSocket.this.broadcastDataTowardEachIntermediatePeer(
										((IndirectAgentSocket) AbstractAgentSocket.this).getParentAgentSocketAddress(),
										new TransferClosedSystemMessage(idt.getLocalID(),
												idt.getTransferToKernelAddress(), idt.getLocalID(), true),
										idt.getLocalID(), AbstractAgentSocket.this.getKernelAddress(), true);
							}
						} else {
							AbstractAgentSocket.this.sendMessageWithRole(aa,
									new ObjectMessage<>(new TransferClosedSystemMessage(null,
											AbstractAgentSocket.this.getKernelAddress(), idt.getLocalID(), true)),
									LocalCommunity.Roles.SOCKET_AGENT_ROLE);
						}
						if (returnsToCaller && idt.getTransferToAgentAddress() != null) {
							InterfacedIDTransfer idt2 = getMiddle(idt.getLocalID(), idt.getTransferToAgentAddress());
							KernelAddress ka = AbstractAgentSocket.this.getTransferType()
									.equals(TransferAgent.NullIDTransfer) ? AbstractAgentSocket.this.getKernelAddress()
											: ((IndirectAgentSocket) AbstractAgentSocket.this)
													.getDistantKernelAddressRequester();
							TransferClosedSystemMessage t = new TransferClosedSystemMessage(getTransferType(), ka,
									idt2.getLocalID(), true);
							t.setMessageLocker(new MessageLocker(null));
							broadcastDataTowardEachIntermediatePeer(t, true);
						}
					}
				}
			}

			middle_transfer_ids.clear();
			transfer_agents.clear();
			local_transfer_ids.clear();
			distant_transfer_ids.clear();
			propositions_in_progress.clear();

		}

		protected void putTransferPropositionSystemMessage(InterfacedIDTransfer id,
				TransferPropositionSystemMessage proposition) {
			putTransferPropositionSystemMessage(id.getLocalID(), proposition);
		}

		protected void putTransferPropositionSystemMessage(IDTransfer id,
				TransferPropositionSystemMessage proposition) {
			putTransferPropositionSystemMessage(id.getID(), proposition);
		}

		protected void putTransferPropositionSystemMessage(int id, TransferPropositionSystemMessage proposition) {
			if (proposition == null)
				throw new NullPointerException("null");
			propositions_in_progress.put(id, proposition);
		}

		protected TransferPropositionSystemMessage removeTransferPropositionSystemMessage(IDTransfer id) {
			return removeTransferPropositionSystemMessage(id.getID());
		}

		protected TransferPropositionSystemMessage removeTransferPropositionSystemMessage(int id) {
			return propositions_in_progress.remove(id);
		}


		protected void putTransferAgentAddress(IDTransfer id, AgentAddress transferAgentAddress) {
			putTransferAgentAddress(id.getID(), transferAgentAddress);
		}

		protected void putTransferAgentAddress(int id, AgentAddress transferAgentAddress) {
			if (transferAgentAddress == null)
				throw new NullPointerException("null");
			transfer_agents.put(id, transferAgentAddress);
		}

		protected AgentAddress getTransferAgentAddress(InterfacedIDTransfer id) {
			return getTransferAgentAddress(id.getLocalID());
		}

		protected AgentAddress getTransferAgentAddress(IDTransfer id) {
			return getTransferAgentAddress(id.getID());
		}

		protected AgentAddress getTransferAgentAddress(int id) {
			return transfer_agents.get(id);
		}

		protected AgentAddress removeTransferAgentAddress(InterfacedIDTransfer id) {
			return removeTransferAgentAddress(id.getLocalID());
		}

		protected AgentAddress removeTransferAgentAddress(IDTransfer id) {
			return removeTransferAgentAddress(id.getID());
		}

		protected AgentAddress removeTransferAgentAddress(int id) {
			return transfer_agents.remove(id);
		}

		protected InterfacedIDTransfer getMiddle(IDTransfer id, AgentAddress comingFrom) {
			return getMiddle(id.getID(), comingFrom);
		}

		protected InterfacedIDTransfer getMiddle(int id, AgentAddress comingFrom) {
			HashMap<AgentAddress, InterfacedIDTransfer> hm = middle_transfer_ids.get(id);
			if (hm == null)
				return null;
			else
				return hm.get(comingFrom);
		}

		protected void putMiddle(int id, HashMap<AgentAddress, InterfacedIDTransfer> middle) {
			if (middle == null)
				throw new NullPointerException("middle");
			middle_transfer_ids.put(id, middle);
		}

		protected void putMiddle(AgentAddress comingFrom, InterfacedIDTransfer idTransfer) {
			if (idTransfer == null)
				throw new NullPointerException("idTransfer");
			HashMap<AgentAddress, InterfacedIDTransfer> hm = middle_transfer_ids
					.computeIfAbsent(idTransfer.getLocalID().getID(), k -> new HashMap<>());
			hm.put(comingFrom, idTransfer);
			AbstractAgentSocket.this.addTaskTransferCheckerIfNecessary();
		}

		protected HashMap<AgentAddress, InterfacedIDTransfer> removeMiddle(IDTransfer id) {
			return removeMiddle(id.getID());
		}

		protected HashMap<AgentAddress, InterfacedIDTransfer> removeMiddle(int id) {
			return middle_transfer_ids.remove(id);
		}

		protected InterfacedIDTransfer getLocal(int id) {
			return local_transfer_ids.get(id);
		}

		protected InterfacedIDTransfer getLocal(IDTransfer id) {
			return getLocal(id.getID());
		}

		protected void putLocal(InterfacedIDTransfer idTransfer) {
			if (idTransfer == null)
				throw new NullPointerException("idTransfer");
			local_transfer_ids.put(idTransfer.getLocalID().getID(), idTransfer);
			AbstractAgentSocket.this.addTaskTransferCheckerIfNecessary();
		}

		protected InterfacedIDTransfer removeLocal(int id) {
			return local_transfer_ids.remove(id);
		}

		protected InterfacedIDTransfer removeLocal(IDTransfer id) {
			return removeLocal(id.getID());
		}



		protected InterfacedIDTransfer getDistant(IDTransfer id) {
			return getDistant(id.getID());
		}

		protected InterfacedIDTransfer getDistant(int id) {
			return distant_transfer_ids.get(id);
		}

		protected void putDistant(InterfacedIDTransfer idTransfer) {
			if (idTransfer == null)
				throw new NullPointerException("idTransfer");
			distant_transfer_ids.put(idTransfer.getDistantID().getID(), idTransfer);
			AbstractAgentSocket.this.addTaskTransferCheckerIfNecessary();
		}

		protected InterfacedIDTransfer removeDistant(IDTransfer id) {
			return removeDistant(id.getID());
		}

		protected InterfacedIDTransfer removeDistant(int id) {
			return distant_transfer_ids.remove(id);
		}

	}

	protected static class CheckDeadTransferNodes extends Message {


	}

}
