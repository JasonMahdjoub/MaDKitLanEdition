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
import com.distrimind.ood.database.messages.*;
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

	static class KernelAddressAndDecentralizedValue
	{
		final KernelAddress kernelAddress;
		final DecentralizedValue decentralizedValue;

		public KernelAddressAndDecentralizedValue(KernelAddress kernelAddress, DecentralizedValue decentralizedValue) {
			if (kernelAddress==null)
				throw new NullPointerException();
			if (decentralizedValue==null)
				throw new NullPointerException();
			this.kernelAddress = kernelAddress;
			this.decentralizedValue = decentralizedValue;
		}

		@Override
		public String toString() {
			return "{" +
					"kernelAddress=" + kernelAddress +
					", decentralizedValue=" + DatabaseWrapper.toString(decentralizedValue) +
					'}';
		}
	}
	private DatabaseWrapper wrapper;
	private DatabaseWrapper.DatabaseSynchronizer synchronizer;
	private DatabaseConfigurationsBuilder databaseConfigurationsBuilder;

	private static final CheckEvents checkEvents=new CheckEvents();
	private DecentralizedValue centralDatabaseID=null;
	private Group centralDatabaseGroup=null;
	private Map<Group, KernelAddressAndDecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
	private Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
	private Map<Group, KernelAddressAndDecentralizedValue> centralGroupIdsPerGroup=new HashMap<>();
	//private Map<DecentralizedValue, Group> centralGroupIdsPerID=new HashMap<>();
	private final HashMap<ConversationID, BigDataMetaData> currentBigDataReceiving=new HashMap<>();
	private final HashMap<ConversationID, BigDataMetaData> currentBigDataSending=new HashMap<>();
	private String agentName=null;

	static final int FILE_BUFFER_LENGTH_BYTES=4096;

	static class BigDataMetaData
	{
		DatabaseEventToSend eventToSend;
		RandomOutputStream randomOutputStream;
		RandomInputStream randomInputStream;

		BigDataMetaData(DatabaseEventToSend eventToSend, RandomOutputStream randomOutputStream) {
			if (eventToSend==null)
				throw new NullPointerException();
			if (randomOutputStream==null)
				throw new NullPointerException();
			this.eventToSend = eventToSend;
			this.randomOutputStream = randomOutputStream;
			this.randomInputStream=null;
		}

		BigDataMetaData(DatabaseEventToSend eventToSend, RandomInputStream randomInputStream) {
			if (eventToSend==null)
				throw new NullPointerException();
			if (randomInputStream==null)
				throw new NullPointerException();
			this.eventToSend = eventToSend;
			this.randomInputStream = randomInputStream;
			this.randomOutputStream = null;
		}


		public void close() throws IOException {
			if (randomInputStream!=null)
			{
				if (!randomInputStream.isClosed())
					randomInputStream.close();
			}
			else if (!randomOutputStream.isClosed())
				randomOutputStream.close();
		}
		public RandomInputStream getRandomInputStream() throws IOException {
			if (randomInputStream==null)
				return randomOutputStream.getRandomInputStream();
			else
				return randomInputStream;
		}
	}

	DatabaseSynchronizerAgent() {
		initAgentName();
	}

	private void initAgentName() {
		String old=agentName;
		try {
			if (getMadkitConfig().getDatabaseWrapper()==null || getMadkitConfig().getDatabaseWrapper().getSynchronizer()==null)
				agentName=super.getName();
			else
				agentName="DbSynchronizerP2PAgent-"+ DatabaseWrapper.toString(getMadkitConfig().getDatabaseWrapper().getSynchronizer().getLocalHostID());

		} catch (DatabaseException e) {
			e.printStackTrace();
			agentName=super.getName();
		}
		if (logger!=null && !agentName.equals(old)) {
			Level l=logger.getLevel();
			setLogLevel(Level.OFF);
			setLogLevel(l);
		}
	}

	@Override
	public String getName() {
		return agentName;
	}

	private static class CheckEvents extends Message
	{

	}

	private Group getDistantGroupID(DecentralizedValue id)
	{
		return distantGroupIdsPerID.get(id);
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
		initAgentName();
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
					initAgentName();
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
			if (logger!=null)
				logger.info("Disconnect database synchronizer agent");
		} catch (DatabaseException e) {
			getLogger().severeLog("Impossible to disconnect database synchronizer", e);
		}
	}

	private DecentralizedValue getDistantPeerID(KernelAddress distantKernelAddress, Group group, String role)
	{
		if (CloudCommunity.Groups.DISTRIBUTED_DATABASE_WITH_SUB_GROUPS.includes(group)
				&& role.equals(CloudCommunity.Roles.SYNCHRONIZER)) {
			KernelAddressAndDecentralizedValue res = distantGroupIdsPerGroup.get(group);
			if (res == null) {
				if (distantKernelAddress != null)
					anomalyDetectedWithOneDistantKernel(true, distantKernelAddress, "Invalided peer ID " + group);

				getLogger().severeLog("Invalided peer ID " + group);
				return null;
			}
			else
				return res.decentralizedValue;
		}
		else
			return null;
	}
	private DecentralizedValue getCentralPeerID(KernelAddress distantKernelAddress, Group group, String role)
	{
		if (CloudCommunity.Groups.CLIENT_SERVER_DATABASE_WITH_SUB_GROUPS.includes(group)
				&& role.equals(CloudCommunity.Roles.CENTRAL_SYNCHRONIZER)) {
			KernelAddressAndDecentralizedValue res = centralGroupIdsPerGroup.get(group);
			if (res == null) {
				new IllegalAccessError().printStackTrace();
				if (distantKernelAddress != null)
					anomalyDetectedWithOneDistantKernel(true, distantKernelAddress, "Invalided server group " + group);

				getLogger().severeLog("Invalided server group " + group);
				return null;
			}
			else
				return res.decentralizedValue;
		}
		else
			return null;
	}
	private DecentralizedValue getDistantPeerIDOrInitIt(AgentAddress aa)
	{
		return getDistantPeerIDOrInitIt(aa.isFrom(getKernelAddress())?null:aa.getKernelAddress(), aa.getGroup(), aa.getRole());
	}
	private DecentralizedValue getDistantPeerIDOrInitIt(KernelAddress distantKernelAddress, Group group, String role)
	{

		if (CloudCommunity.Groups.DISTRIBUTED_DATABASE_WITH_SUB_GROUPS.includes(group)
				&& role.equals(CloudCommunity.Roles.SYNCHRONIZER)) {
			KernelAddressAndDecentralizedValue res = distantGroupIdsPerGroup.get(group);
			if (res == null) {
				res=initDistantID(distantGroupIdsPerGroup, distantGroupIdsPerID, distantKernelAddress, group);
				if (res==null)
					getLogger().severeLog("Invalided peer ID " + group);
				return res==null?null:res.decentralizedValue;
			}
			else
				return res.decentralizedValue;
		}
		else
			return null;


	}
	private KernelAddressAndDecentralizedValue initDistantID(Map<Group, KernelAddressAndDecentralizedValue> distantGroupIdsPerGroup, Map<DecentralizedValue, Group> distantGroupIdsPerID, KernelAddress ka, Group g)
	{
		DecentralizedValue localPeer=databaseConfigurationsBuilder.getConfigurations().getLocalPeer();
		if (localPeer!=null) {
			try {
				DecentralizedValue d = CloudCommunity.Groups.extractDistantHostID(g, localPeer);

				if (d != null && !d.equals(synchronizer.getLocalHostID())) {
					KernelAddressAndDecentralizedValue dv = new KernelAddressAndDecentralizedValue(ka, d);
					if (logger != null && logger.isLoggable(Level.INFO))
						logger.info("Peer available : " + dv);
					distantGroupIdsPerGroup.put(g, dv);
					distantGroupIdsPerID.put(dv.decentralizedValue, g);
					this.requestRole(g, CloudCommunity.Roles.SYNCHRONIZER);
					return dv;
				}
			} catch (IOException | DatabaseException ignored) {

			}
		}
		return null;
	}
	private DecentralizedValue getDistantPeerID(AgentAddress aa)
	{
		return getDistantPeerID(aa.isFrom(getKernelAddress())?null:aa.getKernelAddress(), aa.getGroup(), aa.getRole());

	}
	private DecentralizedValue getCentralPeerIDOrInitIt(AgentAddress aa)
	{
		return getCentralPeerIDOrInitIt(aa.isFrom(getKernelAddress())?null:aa.getKernelAddress(), aa.getGroup(), aa.getRole());
	}
	private DecentralizedValue getCentralPeerIDOrInitIt(KernelAddress distantKernelAddress, Group group, String role)
	{
		if (CloudCommunity.Groups.CLIENT_SERVER_DATABASE_WITH_SUB_GROUPS.includes(group)
				&& role.equals(CloudCommunity.Roles.CENTRAL_SYNCHRONIZER)) {
			KernelAddressAndDecentralizedValue res = centralGroupIdsPerGroup.get(group);
			if (res == null) {
				res=initCentralServer(this.centralGroupIdsPerGroup, distantKernelAddress, group);
				if (res==null)
					getLogger().severeLog("Invalided peer group " + group);

				return res==null?null:res.decentralizedValue;
			}
			else
				return res.decentralizedValue;
		}
		else
			return null;
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
	private KernelAddressAndDecentralizedValue initCentralServer(Map<Group, KernelAddressAndDecentralizedValue> centralGroupIdsPerGroup, KernelAddress ka, Group g)  {
		DecentralizedValue localPeer=databaseConfigurationsBuilder.getConfigurations().getLocalPeer();
		if (localPeer!=null) {
			try {
				DecentralizedValue d = CloudCommunity.Groups.extractDistantHostIDFromCentralDatabaseBackupGroup(g, localPeer);
				if (d != null) {
					KernelAddressAndDecentralizedValue dv=new KernelAddressAndDecentralizedValue(ka, d);

					if (logger != null && logger.isLoggable(Level.INFO))
						logger.info("Central database server available : " + dv);

					centralGroupIdsPerGroup.put(g, dv);
//								centralGroupIdsPerID.put(dv, g);
					this.requestRole(g, CloudCommunity.Roles.SYNCHRONIZER);
					return dv;
				}

			} catch (IOException ignored) {
			}


		}

		return null;
	}
	private void initCentralPeer(Group group, DecentralizedValue id)
	{
		assert centralDatabaseID==null;
		assert id!=null;
		try {
			centralDatabaseID = id;
			centralDatabaseGroup = group;
			synchronizer.centralDatabaseBackupAvailable();
		} catch (DatabaseException ex) {
			getLogger().severeLog("Impossible to connect central database backup" + id, ex);
		}
	}
	private void disconnectCentralDatabaseBackup(boolean initCentralPeerIfAvailable)
	{
		if (centralDatabaseID!=null)
		{
			try {
				if (logger!=null)
					logger.info("Disconnect central database backup");
				synchronizer.centralDatabaseBackupDisconnected();
			} catch (DatabaseException ex) {
				getLogger().severeLog("Impossible to disconnect central database backup " + centralDatabaseID, ex);
			}
			centralDatabaseID = null;
			centralDatabaseGroup = null;
			if (initCentralPeerIfAvailable) {
				if (centralGroupIdsPerGroup.size() > 0) {
					for (Map.Entry<Group, KernelAddressAndDecentralizedValue> e : centralGroupIdsPerGroup.entrySet()) {
						if (hasRole(e.getKey(), CloudCommunity.Roles.SYNCHRONIZER)) {
							initCentralPeer(e.getKey(), e.getValue().decentralizedValue);
							break;
						}
					}

				}
			}
		}
	}
	@Override
	protected void liveByStep(Message _message) throws InterruptedException {

		if (_message instanceof OrganizationEvent)
		{
			if (databaseConfigurationsBuilder.getConfigurations().getLocalPeer()!=null) {
				AgentAddress aa = ((OrganizationEvent) _message).getSourceAgent();
				if (aa.getAgent() == this)
					return;
				DecentralizedValue peerID = getDistantPeerIDOrInitIt(aa);
				if (peerID != null) {

					if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.REQUEST_ROLE)) {
						peerAvailable(aa.getKernelAddress(), peerID);
					} else if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.LEAVE_ROLE)) {

						try {
							synchronizer.peerDisconnected(peerID);
						} catch (DatabaseException e) {
							getLogger().severeLog("Impossible to disconnect " + peerID, e);
						}

					}
				} else {
					DecentralizedValue centralPeerID = getCentralPeerIDOrInitIt(aa);

					if (centralPeerID != null) {
						if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.REQUEST_ROLE)) {
							if (this.centralDatabaseID == null) {
								initCentralPeer(aa.getGroup(), centralPeerID);
							}
						} else if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.LEAVE_ROLE) && centralPeerID.equals(this.centralDatabaseID)) {
							disconnectCentralDatabaseBackup(true);
						}
					}

				}
			}
		}
		else if (_message instanceof NetworkGroupsAccessEvent)
		{
			NetworkGroupsAccessEvent m=(NetworkGroupsAccessEvent)_message;
			HashMap<Group, KernelAddressAndDecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
			Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
			HashMap<Group, KernelAddressAndDecentralizedValue> centralGroupIdsPerGroup=new HashMap<>();
			//Map<DecentralizedValue, Group> centralGroupIdsPerID=new HashMap<>();

			for (Group g : m.getGeneralAcceptedGroups().getGroups().getRepresentedGroups())
			{
				if (CloudCommunity.Groups.DISTRIBUTED_DATABASE_WITH_SUB_GROUPS.includes(g))
				{
					KernelAddressAndDecentralizedValue dv=this.distantGroupIdsPerGroup.get(g);
					if (dv!=null)
					{
						distantGroupIdsPerGroup.put(g, dv );
						distantGroupIdsPerID.put(dv.decentralizedValue, g);
						if (!hasRole(g, CloudCommunity.Roles.SYNCHRONIZER))
							getLogger().warning("CloudCommunity.Roles.SYNCHRONIZER role should be requested with group "+g);
					}
					else
					{
						initDistantID(distantGroupIdsPerGroup, distantGroupIdsPerID, m.getConcernedKernelAddress(), g);
					}
				}
				else if (CloudCommunity.Groups.CLIENT_SERVER_DATABASE_WITH_SUB_GROUPS.includes(g))
				{
					KernelAddressAndDecentralizedValue dv = this.centralGroupIdsPerGroup.get(g);

					if (dv != null) {
						centralGroupIdsPerGroup.put(g, dv );
						//centralGroupIdsPerID.put(dv, g);
						if (!hasRole(g, CloudCommunity.Roles.SYNCHRONIZER))
							getLogger().warning("CloudCommunity.Roles.SYNCHRONIZER role should be requested with group " + g);
					} else {
						initCentralServer(centralGroupIdsPerGroup, m.getConcernedKernelAddress(), g);
					}
				}
			}

			for (Map.Entry<Group, KernelAddressAndDecentralizedValue> e : this.distantGroupIdsPerGroup.entrySet())
			{
				if (!distantGroupIdsPerGroup.containsKey(e.getKey())) {
					if (e.getValue().kernelAddress.equals(m.getConcernedKernelAddress())) {
						if (logger != null && logger.isLoggable(Level.INFO))
							logger.info("Peer disconnected : " + e.getValue());
						leaveGroup(e.getKey());
						try {
							synchronizer.peerDisconnected(e.getValue().decentralizedValue);
						} catch (DatabaseException e2) {
							getLogger().severeLog("Impossible to disconnect " + e.getValue(), e2);
						}
					} else {
						distantGroupIdsPerGroup.put(e.getKey(), e.getValue());
						distantGroupIdsPerID.put(e.getValue().decentralizedValue, e.getKey());
					}
				}
			}

			for (Map.Entry<Group, KernelAddressAndDecentralizedValue> e : this.centralGroupIdsPerGroup.entrySet())
			{
				if (!centralGroupIdsPerGroup.containsKey(e.getKey()))
				{
					if (e.getValue().kernelAddress.equals(m.getConcernedKernelAddress())) {
						if (logger != null && logger.isLoggable(Level.INFO))
							logger.info("Central database server disconnected : " + e.getValue());
						leaveGroup(e.getKey());
					}
					else
						centralGroupIdsPerGroup.put(e.getKey(), e.getValue());
				}
			}
			this.distantGroupIdsPerGroup=distantGroupIdsPerGroup;
			this.distantGroupIdsPerID=distantGroupIdsPerID;
			this.centralGroupIdsPerGroup=centralGroupIdsPerGroup;
		}
		else if (_message==checkEvents)
		{
			DatabaseEvent e;
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
									//currentBigDataHostID = es.getHostDestination();
									final RandomOutputStream bigDataOutputStream = getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, FILE_BUFFER_LENGTH_BYTES, 1);
									BigDataTransferID currentBigDataTransferID=null;
									try {
										be.exportToOutputStream(wrapper, new OutputStreamGetter() {

											@Override
											public RandomOutputStream initOrResetOutputStream() throws IOException {
												bigDataOutputStream.setLength(0);
												return bigDataOutputStream;
											}

											@Override
											public void close() throws Exception {
												bigDataOutputStream.flush();
											}
										});
										RandomInputStream in = bigDataOutputStream.getRandomInputStream();
										currentBigDataTransferID= sendBigDataWithRole(aa, in, be, CloudCommunity.Roles.SYNCHRONIZER);
										if (currentBigDataTransferID == null) {
											getLogger().warning("Impossible to send message to host " + dest);
											synchronizer.peerDisconnected(dest);
										}
										else
										{
											currentBigDataSending.put(currentBigDataTransferID, new BigDataMetaData(be,  bigDataOutputStream));
										}
									}
									finally {
										if (currentBigDataTransferID==null && bigDataOutputStream!=null)
											bigDataOutputStream.close();
									}
								} else {
									if (!ReturnCode.isSuccessOrIsTransferInProgress(sendMessageWithRole(aa, new NetworkObjectMessage<>(es), CloudCommunity.Roles.SYNCHRONIZER))) {
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
						}
					}
					else if (e instanceof MessageDestinedToCentralDatabaseBackup) {

						boolean sent=false;
						if (centralDatabaseGroup!=null) {
							if (e instanceof BigDataEventToSendWithCentralDatabaseBackup) {
								BigDataEventToSendWithCentralDatabaseBackup be = (BigDataEventToSendWithCentralDatabaseBackup) e;
								try {

									RandomInputStream in=null;
									BigDataTransferID currentBigDataTransferID = null;
									try {
										AgentAddress aa=getAgentWithRole(centralDatabaseGroup, CloudCommunity.Roles.CENTRAL_SYNCHRONIZER);
										if (aa!=null) {
											in = be.getPartInputStream();
											currentBigDataTransferID = sendBigDataWithRole(aa, in, be, CloudCommunity.Roles.SYNCHRONIZER);
											if (currentBigDataTransferID == null) {
												getLogger().warning("Impossible to send message to host " + aa);
												disconnectCentralDatabaseBackup(false);
											} else {
												currentBigDataSending.put(currentBigDataTransferID, new BigDataMetaData(be, in));
												sent=true;
											}
										}
									} finally {
										if (currentBigDataTransferID == null && in != null)
											in.close();
									}
								}
								catch (IOException ex) {
									getLogger().severeLog("Unexpected exception", ex);
								}
							}
							else {
								sent = sendMessageWithRole(centralDatabaseGroup, CloudCommunity.Roles.CENTRAL_SYNCHRONIZER, new NetworkObjectMessage<>(e), CloudCommunity.Roles.SYNCHRONIZER).equals(ReturnCode.SUCCESS);
								if (sent && e instanceof DistantBackupCenterConnexionInitialisation)
									sleep(400);
							}
						}
						if (!sent) {
							getLogger().warning("Impossible to send message to central database backup: " + e+", central initialized into client side="+synchronizer.isInitializedWithCentralBackup());
							disconnectCentralDatabaseBackup(false);
						}
					}
				}
				Long utc=wrapper.getNextPossibleEventTimeUTC();
				if (utc!=null) {
					scheduleTask(new Task<Void>(utc) {
						@Override
						public Void call() {
							receiveMessage(checkEvents);
							return null;
						}
					});
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
			if (currentBigDataReceiving.containsKey(m.getConversationID())) {
				getLogger().severeLog("Unexpected big data proposition message " + m);
			}
			else {
				if (m.getAttachedData() instanceof P2PBigDatabaseEventToSend) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving BigDatabaseEventToSend " + m);
					P2PBigDatabaseEventToSend b = (P2PBigDatabaseEventToSend) m.getAttachedData();
					try {
						DecentralizedValue peerID = getDistantPeerID(_message.getSender());
						if (peerID != null) {

							DecentralizedValue source = b.getHostSource();

							if (source != null && source.equals(peerID)) {

								RandomOutputStream out = getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, FILE_BUFFER_LENGTH_BYTES, 1);
								m.acceptTransfer(out);
								currentBigDataReceiving.put(m.getConversationID(), new BigDataMetaData(b, out));
								generateError = false;
							}
						}
					} catch (DatabaseException | IOException | IllegalAccessException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}

				} else if (m.getAttachedData() instanceof BigDataEventToSendWithCentralDatabaseBackup) {
					if (logger != null && logger.isLoggable(Level.FINEST))
						logger.finest("Receiving BigDatabaseEventToSend " + m);
					BigDataEventToSendWithCentralDatabaseBackup b = (BigDataEventToSendWithCentralDatabaseBackup) m.getAttachedData();
					try {
						DecentralizedValue centralPeerID = getCentralPeerID(_message.getSender());
						if (centralPeerID != null) {

							RandomOutputStream out = getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, FILE_BUFFER_LENGTH_BYTES, 1);
							m.acceptTransfer(out);
							currentBigDataReceiving.put(m.getConversationID(), new BigDataMetaData(b, out));
							generateError = false;
						}

					} catch (IOException | IllegalAccessException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}
				}
			}
			if (generateError) {
				m.denyTransfer();
				if (_message.getSender().isFrom(getKernelAddress()))
					getLogger().warning("Invalid message received from " + _message.getSender());
				else
					anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
			}
		}
		else if (_message instanceof BigDataResultMessage)
		{
			BigDataResultMessage res=(BigDataResultMessage)_message;

			BigDataMetaData cur=currentBigDataSending.remove(res.getConversationID());
			if (cur!=null)
			{
				if (res.getType() != BigDataResultMessage.Type.BIG_DATA_TRANSFERRED) {
					try {
						if (cur.eventToSend instanceof BigDataEventToSendWithCentralDatabaseBackup)
						{
							getLogger().warning("Impossible to send message to " + _message.getReceiver());
						}
						else if (cur.eventToSend instanceof P2PBigDatabaseEventToSend)
						{
							synchronizer.peerDisconnected(((P2PBigDatabaseEventToSend) cur.eventToSend).getHostDestination());
						}
						else
						{
							getLogger().warning("Invalid message received from " + _message.getSender());
						}

					} catch (DatabaseException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}
				}
				try {
					cur.close();
				} catch (IOException e) {
					getLogger().severeLog("", e);
				}
			}
			else
			{
				cur=currentBigDataReceiving.remove(res.getConversationID());
				if (cur==null)
					getLogger().warning("Big data receiving should be referenced : "+res);
				else {
					if (res.getType() == BigDataResultMessage.Type.BIG_DATA_TRANSFERRED) {
						if (cur.eventToSend instanceof BigDataEventToSendWithCentralDatabaseBackup)
						{
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Received big data result message " + res);

							try {
								final RandomInputStream in = cur.getRandomInputStream();

								((BigDataEventToSendWithCentralDatabaseBackup) cur.eventToSend).setPartInputStream(in);

								synchronizer.received(cur.eventToSend);
							}
							catch (IOException ex)
							{
								getLogger().severeLog("", ex);
								anomalyDetectedWithOneDistantKernel(ex instanceof MessageExternalizationException && ((MessageExternalizationException) ex).getIntegrity()== Integrity.FAIL_AND_CANDIDATE_TO_BAN, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
							}
							catch (DatabaseException ex) {
								getLogger().severeLog("", ex);
								anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
							}
						}
						else if (cur.eventToSend instanceof P2PBigDatabaseEventToSend)
						{
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Received big data result message " + res);

							try {
								final RandomInputStream in = cur.getRandomInputStream();
								BigDataMetaData cur2=cur;
								synchronizer.received((P2PBigDatabaseEventToSend)cur.eventToSend, new InputStreamGetter() {
									@Override
									public RandomInputStream initOrResetInputStream() throws IOException {
										in.seek(0);
										return in;
									}

									@Override
									public void close() throws Exception {
										cur2.close();
									}
								});
							}
							catch (IOException ex)
							{
								getLogger().severeLog("", ex);
								anomalyDetectedWithOneDistantKernel(ex instanceof MessageExternalizationException && ((MessageExternalizationException) ex).getIntegrity()== Integrity.FAIL_AND_CANDIDATE_TO_BAN, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
							}
							catch (DatabaseException ex) {
								getLogger().severeLog("", ex);
								anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
							}

						} else {
							anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());

						}
					}
					else {
						try {
							if (cur.eventToSend instanceof BigDataEventToSendWithCentralDatabaseBackup)
							{
								getLogger().warning("Impossible to receive message from " + _message.getReceiver());
							}
							else if (cur.eventToSend instanceof P2PBigDatabaseEventToSend)
							{
								synchronizer.peerDisconnected(((P2PBigDatabaseEventToSend) cur.eventToSend).getHostSource());
							}
							else
							{
								anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
							}

						} catch (DatabaseException e) {
							e.printStackTrace();
							getLogger().severe(e.getMessage());
						}
					}
					try {
						cur.close();
					} catch (IOException e) {
						getLogger().severeLog("", e);
					}

				}
			}
			receiveMessage(checkEvents);
		}
		else if (_message instanceof NetworkObjectMessage)
		{
			NetworkObjectMessage<?> m=(NetworkObjectMessage<?>)_message;
			boolean generateError=true;
			if (m.getContent() instanceof P2PDatabaseEventToSend) {
				DecentralizedValue peerID = getDistantPeerID(_message.getSender());
				if (peerID != null) {
					P2PDatabaseEventToSend e = (P2PDatabaseEventToSend) m.getContent();
					try {
						DecentralizedValue source = e.getHostSource();

						if (source != null && source.equals(peerID)) {
							synchronizer.received(e);
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Event " + e.getClass() + " received from peer " + peerID);
							generateError=false;
							receiveMessage(checkEvents);
						}
					} catch (DatabaseException | IOException ex) {
						getLogger().severeLog("Unexpected exception", ex);
						if (_message.getSender().isFrom(getKernelAddress()))
							getLogger().warning("Invalid message received from " + _message.getSender());
						else
							anomalyDetectedWithOneDistantKernel(ex instanceof MessageExternalizationException && ((MessageExternalizationException) ex).getIntegrity() == Integrity.FAIL_AND_CANDIDATE_TO_BAN, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
					}
				}
			}
			else if (m.getContent() instanceof MessageComingFromCentralDatabaseBackup)
			{
				DecentralizedValue centralID = getCentralPeerID(_message.getSender());
				if (centralID != null) {
					MessageComingFromCentralDatabaseBackup e = (MessageComingFromCentralDatabaseBackup) m.getContent();
					try {
						synchronizer.received(e);
						if (logger != null && logger.isLoggable(Level.FINEST))
							logger.finest("Event " + e.getClass() + " received from peer " + centralID);
						generateError=false;
						receiveMessage(checkEvents);
					} catch (DatabaseException | IOException ex) {
						getLogger().severeLog("Unexpected exception", ex);
						if (_message.getSender().isFrom(getKernelAddress()))
							getLogger().warning("Invalid message received from " + _message.getSender());
						else
							anomalyDetectedWithOneDistantKernel(ex instanceof MessageExternalizationException && ((MessageExternalizationException) ex).getIntegrity() == Integrity.FAIL_AND_CANDIDATE_TO_BAN, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
					}
				}

			}
			if (generateError) {
				if (_message.getSender().isFrom(getKernelAddress()))
					getLogger().warning("Invalid message received from " + _message.getSender());
				else
					anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
			}
		}
		else
			anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
	}

	static void updateGroupAccess(AbstractAgent agent) {
		ReturnCode rc;
		if (!ReturnCode.isSuccessOrIsTransferInProgress(rc=agent.broadcastMessageWithRole(LocalCommunity.Groups.NETWORK,
				LocalCommunity.Roles.SOCKET_AGENT_ROLE, new ObjectMessage<>(NetworkAgent.REFRESH_GROUPS_ACCESS), CloudCommunity.Roles.SYNCHRONIZER)))

			if (agent.logger!=null && agent.logger.isLoggable(Level.WARNING))
				agent.logger.warning("Impossible to broadcast group rights update order : "+rc+", kernel="+ agent.getKernelAddress());
	}


}
