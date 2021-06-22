package com.distrimind.madkit.kernel;
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

import com.distrimind.madkit.agr.CloudCommunity;
import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.message.NetworkObjectMessage;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.madkit.message.hook.NetworkGroupsAccessEvent;
import com.distrimind.madkit.message.hook.OrganizationEvent;
import com.distrimind.ood.database.*;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.ood.database.messages.P2PBigDatabaseEventToSend;
import com.distrimind.ood.database.messages.P2PDatabaseEventToSend;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 2.0
 * @since MaDKitLanEdition 2.0.0
 */
public class DatabaseSynchronizerAgent extends AgentFakeThread {





	private DatabaseWrapper wrapper;
	private DatabaseWrapper.DatabaseSynchronizer synchronizer;
	private DatabaseConfigurationsBuilder databaseConfigurationsBuilder;

	private static final CheckEvents checkEvents=new CheckEvents();
	private DecentralizedValue centralDatabaseID=null;
	private Group centralDatabaseGroup=null;
	private Map<Group, DecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
	private Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
	private Map<Group, DecentralizedValue> centralGroupIdsPerGroup=new HashMap<>();
	private Map<DecentralizedValue, Group> centralGroupIdsPerID=new HashMap<>();
	private BigDataTransferID currentBigDataTransferID=null;
	private RandomOutputStream currentBigDataOutputStream=null;
	private DecentralizedValue currentBigDataHostID=null;
	private final HashMap<ConversationID, BigDataMetaData> currentBigDataReceiving=new HashMap<>();

	static final int FILE_BUFFER_LENGTH_BYTES=4096;

	private static class BigDataMetaData
	{
		P2PBigDatabaseEventToSend eventToSend;
		RandomOutputStream randomOutputStream;

		BigDataMetaData(P2PBigDatabaseEventToSend eventToSend, RandomOutputStream randomOutputStream) {
			this.eventToSend = eventToSend;
			this.randomOutputStream = randomOutputStream;
		}

		public void close() throws IOException {
			randomOutputStream.close();
		}
	}

	DatabaseSynchronizerAgent()
	{

	}

	private static class CheckEvents extends Message
	{

	}

	private Group getDistantGroupID(DecentralizedValue id)
	{
		return distantGroupIdsPerID.get(id);
	}

	private void removeDistantGroupID(DecentralizedValue id)
	{
		Group group=distantGroupIdsPerID.remove(id);
		if (group!=null) {
			this.leaveRole(group, CloudCommunity.Roles.SYNCHRONIZER);
			distantGroupIdsPerGroup.remove(group);
		}
	}

	private void removeUnusedDistantGroups() {
		for (Iterator<Map.Entry<DecentralizedValue, Group>> it = distantGroupIdsPerID.entrySet().iterator(); it.hasNext();) {
			Map.Entry<DecentralizedValue, Group> e=it.next();
			if (!databaseConfigurationsBuilder.getConfigurations().getLocalPeer().equals(e.getKey())
				&& ! databaseConfigurationsBuilder.getConfigurations().getDistantPeers().contains(e.getKey())) {
				this.leaveRole(e.getValue(), CloudCommunity.Roles.SYNCHRONIZER);
				it.remove();
				distantGroupIdsPerGroup.remove(e.getValue());
			}
		}
	}


	@Override
	protected void activate() throws InterruptedException {
		setLogLevel(getMadkitConfig().networkProperties.networkLogLevel);

		if (logger!=null && logger.isLoggable(Level.INFO))
			logger.info("Launch database synchronizer");
		this.requestRole(LocalCommunity.Groups.DATABASE, LocalCommunity.Roles.DATABASE_SYNCHRONIZER_LISTENER);
		requestRole(LocalCommunity.Groups.NETWORK, CloudCommunity.Roles.SYNCHRONIZER);

		try {
			wrapper = getMadkitConfig().getDatabaseWrapper();
			synchronizer= wrapper.getSynchronizer();
			databaseConfigurationsBuilder= wrapper.getDatabaseConfigurationsBuilder();

			synchronizer.setNotifier(new DatabaseNotifier() {

				@Override
				public void newDatabaseEventDetected(DatabaseWrapper wrapper)  {
					receiveMessage(checkEvents);
				}

				@Override
				public void startNewSynchronizationTransaction() {

				}

				@Override
				public void endSynchronizationTransaction() {

				}

				@Override
				public void hostDisconnected(DecentralizedValue hostID) {
					removeUnusedDistantGroups();
				}

				@Override
				public void hostConnected(DecentralizedValue hostID) {

				}

				@Override
				public void localHostInitialized(DecentralizedValue hostID) {
					removeUnusedDistantGroups();
					updateGroupAccess(DatabaseSynchronizerAgent.this);
				}

				@Override
				public void hostsAdded(Set<DecentralizedValue> peersIdentifiers) {
					updateGroupAccess(DatabaseSynchronizerAgent.this);
				}

				@Override
				public void centralDatabaseBackupCertificateRevoked() {

				}

			});
			if (logger!=null && logger.isLoggable(Level.INFO))
				logger.info("Data synchronizer launched");

			this.requestHookEvents(HookMessage.AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER);
			this.requestHookEvents(HookMessage.AgentActionEvent.REQUEST_ROLE);
			this.requestHookEvents(HookMessage.AgentActionEvent.LEAVE_ROLE);
		} catch (DatabaseException e) {
			getLogger().severeLog("Unexpected exception ", e);
			this.killAgent(this);
		}
	}

	@Override
	protected void end() {
		try {
			synchronizer.connectionLost();
		} catch (DatabaseException e) {
			getLogger().severeLog("Impossible to disconnect database synchronizer", e);
		}
	}

	private DecentralizedValue getDistantPeerID(KernelAddress distantKernelAddress, Group group, String role)
	{
		String path=group.getPath();
		path=path.substring(0, path.length()-1);
		path=path.substring(0, path.lastIndexOf("/")+1);
		if (CloudCommunity.Groups.DISTRIBUTED_DATABASE.getPath().equals(path)
				&& role.equals(CloudCommunity.Roles.SYNCHRONIZER)) {
			DecentralizedValue res = distantGroupIdsPerGroup.get(group);
			if (res == null) {
				if (distantKernelAddress != null)
					anomalyDetectedWithOneDistantKernel(true, distantKernelAddress, "Invalided peer ID " + group);

				getLogger().severeLog("Invalided peer ID " + group);
			}
			return res;
		}
		else
			return null;
	}
	private DecentralizedValue getCentralPeerID(KernelAddress distantKernelAddress, Group group, String role)
	{
		String path=group.getPath();
		path=path.substring(0, path.length()-1);
		path=path.substring(0, path.lastIndexOf("/")+1);
		if (CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP.getPath().equals(path)
				&& role.equals(CloudCommunity.Roles.CENTRAL_SYNCHRONIZER)) {
			DecentralizedValue res = centralDatabaseGroup==null?null:(centralDatabaseGroup.equals(group)?centralDatabaseID:null);
			if (res == null) {
				if (distantKernelAddress != null)
					anomalyDetectedWithOneDistantKernel(true, distantKernelAddress, "Invalided peer group " + group);

				getLogger().severeLog("Invalided peer group " + group);
			}
			return res;
		}
		else
			return null;
	}

	private DecentralizedValue getDistantPeerID(AgentAddress aa)
	{
		return getDistantPeerID(aa.isFrom(getKernelAddress())?null:aa.getKernelAddress(), aa.getGroup(), aa.getRole());

	}
	private DecentralizedValue getCentralPeerID(AgentAddress aa)
	{
		return getCentralPeerID(aa.isFrom(getKernelAddress())?null:aa.getKernelAddress(), aa.getGroup(), aa.getRole());
	}
	private void peerAvailable(KernelAddress distantKernelAddress, DecentralizedValue peerID)
	{
		try {

			if (logger!=null && logger.isLoggable(Level.FINE))
				logger.fine("Connection initialization with peer : "+peerID);
			synchronizer.peerConnected(peerID);

		} catch (DatabaseException e) {
			if (!distantKernelAddress.equals(getKernelAddress()))
				anomalyDetectedWithOneDistantKernel(false, distantKernelAddress, "Unexpected exception");

			getLogger().severeLog("Unexpected exception", e);
		}
	}
	private void initCentralPeer(Group group, DecentralizedValue id)
	{
		assert centralDatabaseID==null;
		try {
			synchronizer.centralDatabaseBackupAvailable();
			centralDatabaseID = id;
			centralDatabaseGroup = group;
		} catch (DatabaseException ex) {
			getLogger().severeLog("Impossible to connect central database backup" + id, ex);
		}
	}
	@Override
	protected void liveByStep(Message _message) throws InterruptedException {

		if (_message instanceof OrganizationEvent)
		{
			AgentAddress aa=((OrganizationEvent) _message).getSourceAgent();
			if (aa.getAgent()==this)
				return;
			DecentralizedValue peerID=getDistantPeerID(aa);
			if (peerID!=null) {

				if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.REQUEST_ROLE)) {
					peerAvailable(aa.getKernelAddress(), peerID);
				} else if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.LEAVE_ROLE)) {

					try {
						synchronizer.peerDisconnected(peerID);
					} catch (DatabaseException e) {
						getLogger().severeLog("Impossible to disconnect " + peerID, e);
					}

				}
			}
			else
			{
				DecentralizedValue centralPeerID=getCentralPeerID(aa);
				if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.REQUEST_ROLE)) {
					if (this.centralDatabaseID==null) {
						initCentralPeer(aa.getGroup(), centralPeerID);
					}
				} else if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.LEAVE_ROLE)) {

					if (centralDatabaseID!=null && centralDatabaseID.equals(centralPeerID))
					{
						try {
							synchronizer.centralDatabaseBackupDisconnected();
						} catch (DatabaseException ex) {
							getLogger().severeLog("Impossible to disconnect central database backup " + centralDatabaseID, ex);
						}
						centralDatabaseID = null;
						centralDatabaseGroup = null;
						if (centralGroupIdsPerGroup.size()>0)
						{
							for (Map.Entry<Group, DecentralizedValue> e : centralGroupIdsPerGroup.entrySet()) {
								if (hasRole(e.getKey(), CloudCommunity.Roles.SYNCHRONIZER)) {
									initCentralPeer(e.getKey(), e.getValue());
									break;
								}
							}

						}
					}
				}

			}
		}
		else if (_message instanceof NetworkGroupsAccessEvent)
		{
			NetworkGroupsAccessEvent m=(NetworkGroupsAccessEvent)_message;
			HashMap<Group, DecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
			Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
			HashMap<Group, DecentralizedValue> centralGroupIdsPerGroup=new HashMap<>();
			Map<DecentralizedValue, Group> centralGroupIdsPerID=new HashMap<>();
			boolean changed=false;
			boolean centralChanged=false;
			for (Group g : m.getGeneralAcceptedGroups().getGroups().getRepresentedGroups())
			{

				if (g.getPath().startsWith(CloudCommunity.Groups.DISTRIBUTED_DATABASE.getPath()))
				{
					DecentralizedValue dv=this.distantGroupIdsPerGroup.get(g);
					if (dv!=null)
					{
						distantGroupIdsPerGroup.put(g, dv );
						distantGroupIdsPerID.put(dv, g);
						if (!hasRole(g, CloudCommunity.Roles.SYNCHRONIZER))
							getLogger().warning("CloudCommunity.Roles.SYNCHRONIZER role should be requested with group "+g);
					}
					else
					{

						try {
							dv=CloudCommunity.Groups.extractDistantHostID(g,  databaseConfigurationsBuilder.getConfigurations().getLocalPeer());
							if (dv!=null)
							{
								changed=true;
								distantGroupIdsPerGroup.put(g, dv );
								distantGroupIdsPerID.put(dv, g);
								this.requestRole(g, CloudCommunity.Roles.SYNCHRONIZER);
							}
						} catch (IOException ignored) {

						}
					}
				}
				else if (g.getPath().startsWith(CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP.getPath()))
				{
					try {

						DecentralizedValue dv = this.centralGroupIdsPerGroup.get(g);

						if (dv != null) {
							centralGroupIdsPerGroup.put(g, dv );
							centralGroupIdsPerID.put(dv, g);
							if (!hasRole(g, CloudCommunity.Roles.SYNCHRONIZER))
								getLogger().warning("CloudCommunity.Roles.SYNCHRONIZER role should be requested with group " + g);
						} else {
							dv = CloudCommunity.Groups.extractDistantHostIDFromCentralDatabaseBackupGroup(g, databaseConfigurationsBuilder.getConfigurations().getLocalPeer());
							if (dv!=null) {
								centralChanged=true;
								centralGroupIdsPerGroup.put(g, dv );
								centralGroupIdsPerID.put(dv, g);
								this.requestRole(g, CloudCommunity.Roles.SYNCHRONIZER);
							}

						}
					} catch (IOException ignored) {

					}
				}
			}
			if (this.distantGroupIdsPerGroup.size()!=distantGroupIdsPerGroup.size() || changed)
			{
				for (Map.Entry<Group, DecentralizedValue> e : this.distantGroupIdsPerGroup.entrySet())
				{
					if (!distantGroupIdsPerGroup.containsKey(e.getKey()))
					{
						leaveGroup(e.getKey());
						try {
							synchronizer.peerDisconnected(e.getValue());
						} catch (DatabaseException e2) {
							getLogger().severeLog("Impossible to disconnect " + e.getValue(), e2);
						}
					}
				}
			}
			if (this.centralGroupIdsPerGroup.size()!=centralGroupIdsPerGroup.size() || centralChanged)
			{
				for (Map.Entry<Group, DecentralizedValue> e : this.centralGroupIdsPerGroup.entrySet())
				{
					if (!centralGroupIdsPerGroup.containsKey(e.getKey()))
					{
						leaveGroup(e.getKey());


					}
				}
			}
			this.distantGroupIdsPerGroup=distantGroupIdsPerGroup;
			this.distantGroupIdsPerID=distantGroupIdsPerID;
			this.centralGroupIdsPerGroup=centralGroupIdsPerGroup;
			this.centralGroupIdsPerID=centralGroupIdsPerID;
			if (centralDatabaseID==null && centralGroupIdsPerGroup.size()>0)
			{
				Map.Entry<Group, DecentralizedValue> e=centralGroupIdsPerGroup.entrySet().iterator().next();
				centralDatabaseID = e.getValue();
				centralDatabaseGroup = e.getKey();
			}
		}
		else if (_message==checkEvents)
		{
			DatabaseEvent e;
			if (currentBigDataTransferID!=null)
				return ;
			try {


				while ((e = synchronizer.nextEvent()) != null) {
					if (e instanceof P2PDatabaseEventToSend) {
						P2PDatabaseEventToSend es = (P2PDatabaseEventToSend) e;
						try {
							DecentralizedValue dest = es.getHostDestination();
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Send event " + es.getClass() + " to peer " + dest);
							AgentAddress aa = getAgentWithRole(this.getDistantGroupID(dest), CloudCommunity.Roles.SYNCHRONIZER);
							if (aa != null) {
								if (es instanceof P2PBigDatabaseEventToSend) {
									P2PBigDatabaseEventToSend be = (P2PBigDatabaseEventToSend) e;
									currentBigDataHostID = es.getHostDestination();
									currentBigDataOutputStream = getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, FILE_BUFFER_LENGTH_BYTES, 1);
									be.exportToOutputStream(wrapper, new OutputStreamGetter() {
										@Override
										public RandomOutputStream initOrResetOutputStream() throws IOException {
											currentBigDataOutputStream.setLength(0);
											return currentBigDataOutputStream;
										}

										@Override
										public void close() throws Exception {
											currentBigDataOutputStream.flush();
										}
									});
									RandomInputStream in = currentBigDataOutputStream.getRandomInputStream();
									currentBigDataTransferID = sendBigDataWithRole(aa, in, be, CloudCommunity.Roles.SYNCHRONIZER);
									if (currentBigDataTransferID == null) {
										getLogger().warning("Impossible to send message to host " + dest);
										synchronizer.peerDisconnected(dest);
									} else
										return;
								} else {
									if (!sendMessageWithRole(aa, new NetworkObjectMessage<>(es), CloudCommunity.Roles.SYNCHRONIZER).equals(ReturnCode.SUCCESS)) {
										getLogger().warning("Impossible to send message to host " + dest);
										synchronizer.peerDisconnected(dest);
									}
								}
							} else {
								getLogger().warning("Impossible to send message to host " + dest);
								synchronizer.peerConnected(dest);
							}

						} catch (DatabaseException | IOException ex) {
							getLogger().severeLog("Unexpected exception", ex);
						} finally {
							if (currentBigDataTransferID == null && currentBigDataOutputStream != null) {
								try {
									currentBigDataOutputStream.close();
								} catch (IOException ioException) {
									getLogger().severeLog("Unexpected exception", ioException);
								}
								currentBigDataOutputStream = null;
								currentBigDataHostID = null;
							}
						}
					}
				}
			}
			catch (DatabaseException e2)
			{
				getLogger().severeLog("Unexpected exception", e2);
			}
		}
		else if (_message instanceof BigDataPropositionMessage)
		{

			BigDataPropositionMessage m=(BigDataPropositionMessage)_message;
			boolean generateError=true;
			if (m.getAttachedData() instanceof P2PBigDatabaseEventToSend)
			{
				if (currentBigDataReceiving.containsKey(m.getConversationID()))
					getLogger().warning("Unexpected big data proposition message " + m);
				else
				{


					if (logger!=null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving BigDatabaseEventToSend " + m);
					P2PBigDatabaseEventToSend b=(P2PBigDatabaseEventToSend)m.getAttachedData();
					try {
						DecentralizedValue peerID = getDistantPeerID(_message.getSender());
						if (peerID != null) {

							DecentralizedValue source = b.getHostSource();

							if (source != null && source.equals(peerID)) {

								RandomOutputStream out=getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, FILE_BUFFER_LENGTH_BYTES, 1);
								m.acceptTransfer(out);
								currentBigDataReceiving.put(m.getConversationID(), new BigDataMetaData(b, out));
								generateError=false;
							}
						}
					} catch (DatabaseException | IOException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}
				}

			}

			if (generateError) {
				if (_message.getSender().isFrom(getKernelAddress()))
					getLogger().warning("Invalid message received from " + _message.getSender());
				else
					anomalyDetectedWithOneDistantKernel(true, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
			}
		}
		else if (_message instanceof BigDataResultMessage)
		{
			BigDataResultMessage res=(BigDataResultMessage)_message;
			if (res.getConversationID().equals(currentBigDataTransferID))
			{
				try {
					currentBigDataOutputStream.close();
				} catch (IOException e) {
					getLogger().severeLog("Unexpected exception", e);
				}

				currentBigDataOutputStream=null;
				currentBigDataTransferID=null;

				receiveMessage(checkEvents);
				if (res.getType()!=BigDataResultMessage.Type.BIG_DATA_TRANSFERRED)
				{
					try {
						synchronizer.peerDisconnected(currentBigDataHostID);
					} catch (DatabaseException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}
				}
				currentBigDataHostID=null;
			}
			else if (res.getType()==BigDataResultMessage.Type.BIG_DATA_TRANSFERRED) {


				final BigDataMetaData e = currentBigDataReceiving.remove(res.getConversationID());

				if (e != null) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Received big data result message " + res);

					try {
						final RandomInputStream in = e.randomOutputStream.getRandomInputStream();

						synchronizer.received(e.eventToSend, new InputStreamGetter() {
							@Override
							public RandomInputStream initOrResetInputStream() throws IOException {
								in.seek(0);
								return in;
							}

							@Override
							public void close() throws Exception {
								e.close();
							}
						});
					}
					catch (IOException ex)
					{
						ex.printStackTrace();
						anomalyDetectedWithOneDistantKernel(ex instanceof MessageExternalizationException && ((MessageExternalizationException) ex).getIntegrity()== Integrity.FAIL_AND_CANDIDATE_TO_BAN, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
					}
					catch (DatabaseException ex) {
						ex.printStackTrace();
						getLogger().severe(ex.getMessage());
						try {
							e.close();
						} catch (IOException ex2) {
							ex.printStackTrace();
							getLogger().severe(ex.getMessage());
						}
					}

				} else {
					getLogger().warning("Unexpected big data result message " + res);

				}

			}
			else
			{

				BigDataMetaData d=currentBigDataReceiving.remove(res.getConversationID());

				if (d!=null) {
					try {
						d.close();
					} catch (IOException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}
					try {
						synchronizer.peerDisconnected(d.eventToSend.getHostSource());
					} catch (DatabaseException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}
				}
				getLogger().finest("Unable to send big data result message " + res);

			}

		}
		else if (_message instanceof NetworkObjectMessage && ((NetworkObjectMessage<?>) _message).getContent() instanceof P2PDatabaseEventToSend)
		{
			DecentralizedValue peerID = getDistantPeerID(_message.getSender());
			if (peerID != null) {
				P2PDatabaseEventToSend e = (P2PDatabaseEventToSend) ((NetworkObjectMessage<?>) _message).getContent();
				try {
					DecentralizedValue source = e.getHostSource();

					if (source != null && source.equals(peerID)) {
						synchronizer.received(e);
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest("Event " + e.getClass() + " received from peer " + peerID);
						receiveMessage(checkEvents);
					}
				} catch (DatabaseException | IOException ex) {
					getLogger().severeLog("Unexpected exception", ex);
					if (_message.getSender().isFrom(getKernelAddress()))
						getLogger().warning("Invalid message received from " + _message.getSender());
					else
						anomalyDetectedWithOneDistantKernel(ex instanceof MessageExternalizationException && ((MessageExternalizationException) ex).getIntegrity()== Integrity.FAIL_AND_CANDIDATE_TO_BAN, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
				}
			}

		}
	}

	static void updateGroupAccess(AbstractAgent agent) {
		ReturnCode rc;
		if (!(rc=agent.broadcastMessageWithRole(LocalCommunity.Groups.NETWORK,
				LocalCommunity.Roles.SOCKET_AGENT_ROLE, new ObjectMessage<>(NetworkAgent.REFRESH_GROUPS_ACCESS), CloudCommunity.Roles.SYNCHRONIZER)).equals(ReturnCode.SUCCESS))
			if (agent.logger!=null && agent.logger.isLoggable(Level.WARNING))
				agent.logger.warning("Impossible to broadcast group rights update order : "+rc);
	}


}
