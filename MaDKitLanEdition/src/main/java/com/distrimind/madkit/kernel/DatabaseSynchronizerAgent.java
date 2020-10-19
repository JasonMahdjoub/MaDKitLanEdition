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
import com.distrimind.madkit.database.DifferedDistantDatabaseHostConfigurationTable;
import com.distrimind.madkit.message.NetworkObjectMessage;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.madkit.message.hook.OrganizationEvent;
import com.distrimind.ood.database.*;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.ood.database.messages.P2PBigDatabaseEventToSend;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.io.RandomFileOutputStream;
import com.distrimind.util.io.RandomInputStream;
import com.distrimind.util.io.RandomOutputStream;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
public class DatabaseSynchronizerAgent extends AgentFakeThread {




	private DecentralizedValue localHostID;
	private String localHostIDString;
	private DatabaseWrapper wrapper;
	private DatabaseWrapper.DatabaseSynchronizer synchronizer;

	private static final CheckEvents checkEvents=new CheckEvents();
	private final Map<Group, DecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
	private final Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
	private BigDataTransferID currentBigDataTransferID=null;
	private File currentBigDataFileName=null;
	private DecentralizedValue currentBigDataHostID=null;
	private final HashMap<ConversationID, BigDataMetaData> currentBigDataReceiving=new HashMap<>();

	static final int FILE_BUFFER_LENGTH_BYTES=4096;

	private static class BigDataMetaData
	{
		P2PBigDatabaseEventToSend eventToSend;
		RandomOutputStream randomOutputStream;
		File concernedFile;

		BigDataMetaData(P2PBigDatabaseEventToSend eventToSend, RandomOutputStream randomOutputStream, File concernedFile) {
			this.eventToSend = eventToSend;
			this.randomOutputStream = randomOutputStream;
			this.concernedFile = concernedFile;
		}

		public void close() throws IOException {
			randomOutputStream.close();
			if (concernedFile.exists())
				//noinspection ResultOfMethodCallIgnored
				concernedFile.delete();
		}
	}

	DatabaseSynchronizerAgent()
	{

	}

	private static class CheckEvents extends Message
	{

	}
	private void addDistantGroupID(DecentralizedValue id)
	{
		Group group=CloudCommunity.Groups.getDistributedDatabaseGroup(localHostIDString, id);
		addDistantGroupID(group, id);
	}
	private void addDistantGroupID(Group group, DecentralizedValue id)
	{
		distantGroupIdsPerGroup.put(group, id);
		distantGroupIdsPerID.put(id, group);
		this.requestRole(group, CloudCommunity.Roles.SYNCHRONIZER);
		//return group;
	}
	/*private Group getOrAddDistantGroupID(DecentralizedValue id)
	{
		Group g=distantGroupIdsPerID.get(id);
		if (g==null)
			return addDistantGroupID(id);
		else
			return g;
	}*/

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
			localHostID=synchronizer.getLocalHostID();
			if (localHostID==null)
			{
				getLogger().warning("No local host ID was defined !");
				this.killAgent(this);
			}
			else {
				localHostIDString=CloudCommunity.Groups.encodeDecentralizedValue(localHostID);

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
				});
				if (!synchronizer.isInitialized())
					synchronizer.initLocalHostID(localHostID);
				if (logger!=null && logger.isLoggable(Level.INFO))
					logger.info("Data synchronizer launched");

				for (DecentralizedValue dv : synchronizer.getDistantHostsIDs())
				{
					addDistantGroupID(dv);
				}
				for (DifferedDistantDatabaseHostConfigurationTable.Record r : wrapper.getTableInstance(DifferedDistantDatabaseHostConfigurationTable.class).getRecords()){
					if (!distantGroupIdsPerID.containsKey(r.getHostIdentifier())){
						addDistantGroupID(r.getHostIdentifier());
					}
				}

				this.requestHookEvents(HookMessage.AgentActionEvent.REQUEST_ROLE);
				this.requestHookEvents(HookMessage.AgentActionEvent.LEAVE_ROLE);
			}
		} catch (DatabaseException e) {
			getLogger().severeLog("Unexpected exception ", e);
			this.killAgent(this);
		}
	}

	@Override
	protected void end() {
		try {
			synchronizer.disconnectAll();
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

	private DecentralizedValue getDistantPeerID(AgentAddress aa)
	{
		return getDistantPeerID(aa.isFrom(getKernelAddress())?null:aa.getKernelAddress(), aa.getGroup(), aa.getRole());

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
					try {

						if (synchronizer.isPairedWith(peerID)) {
							if (logger!=null && logger.isLoggable(Level.FINE))
								logger.fine("Connection initialization with peer : "+peerID);

							sendMessageWithRole(aa, new DatabaseConnectionInitializationMessage(synchronizer.getLastValidatedSynchronization(localHostID)), CloudCommunity.Roles.SYNCHRONIZER);
						}
						else
							checkDifferedDistantDatabaseHostConfiguration(peerID);

					} catch (DatabaseException e) {
						if (!_message.getSender().isFrom(getKernelAddress()))
							anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Unexpected exception");

						getLogger().severeLog("Unexpected exception", e);
					}
				} else if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.LEAVE_ROLE)) {

					try {
						if (synchronizer.isInitialized(peerID)) {
							if (logger!=null && logger.isLoggable(Level.FINE))
								logger.fine("Disconnect peer : "+peerID);

							synchronizer.disconnectHook(peerID);
						}
					} catch (DatabaseException e) {
						getLogger().severeLog("Impossible to disconnect " + peerID, e);
					}

				}
			}
		}
		else if (_message instanceof DatabaseConnectionInitializationMessage)
		{

			DecentralizedValue peerID=getDistantPeerID(_message.getSender());
			if (peerID!=null) {
				try {

					synchronizer.initHook(peerID, ((DatabaseConnectionInitializationMessage) _message).getContent());
					if (logger!=null && logger.isLoggable(Level.FINE))
						logger.fine("Connection initialization with peer : "+peerID+" FINISHED");
					checkDifferedDistantDatabaseHostConfiguration(peerID);
				} catch (DatabaseException e) {
					if (!_message.getSender().isFrom(getKernelAddress()))
						anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Impossible to connect database peer "+peerID);
					getLogger().severeLog("Impossible to connect database peer "+peerID, e);
				}
			}
			else
				if (!_message.getSender().isFrom(getKernelAddress()))
					anomalyDetectedWithOneDistantKernel(true, _message.getSender().getKernelAddress(), "Invalided message received from "+_message.getSender());



		} else if (_message==checkEvents)
		{
			DatabaseEvent e;
			if (currentBigDataTransferID!=null)
				return ;
			while ((e=synchronizer.nextEvent())!=null)
			{
				if (e instanceof DBEventToSend)
				{
					DBEventToSend es=(DBEventToSend)e;
					try {
						DecentralizedValue dest=es.getHostDestination();
						if (logger!=null && logger.isLoggable(Level.FINEST))
							logger.finest("Send event "+es.getClass()+" to peer "+dest);
						AgentAddress aa=getAgentWithRole(this.getDistantGroupID(dest), CloudCommunity.Roles.SYNCHRONIZER);
						if (aa!=null) {
							if (es instanceof P2PBigDatabaseEventToSend) {
								P2PBigDatabaseEventToSend be = (P2PBigDatabaseEventToSend) e;
								currentBigDataFileName = getMadkitConfig().getDatabaseSynchronisationFileName();
								currentBigDataHostID=es.getHostDestination();
								final RandomOutputStream out = getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(currentBigDataFileName, RandomFileOutputStream.AccessMode.READ_AND_WRITE, FILE_BUFFER_LENGTH_BYTES, 1);
								be.exportToOutputStream(wrapper, new OutputStreamGetter() {
									@Override
									public RandomOutputStream initOrResetOutputStream() throws IOException {
										out.setLength(0);
										return out;
									}

									@Override
									public void close() throws Exception {
										out.flush();
									}
								});
								RandomInputStream in=out.getRandomInputStream();
								currentBigDataTransferID=sendBigDataWithRole(aa, in,be,CloudCommunity.Roles.SYNCHRONIZER);
								if (currentBigDataTransferID==null) {
									getLogger().warning("Impossible to send message to host " + dest);
									synchronizer.disconnectHook(dest);
								}
								else
									return;
							} else {
								if (!sendMessageWithRole(aa, new NetworkObjectMessage<>(es), CloudCommunity.Roles.SYNCHRONIZER).equals(ReturnCode.SUCCESS)) {
									getLogger().warning("Impossible to send message to host " + dest);
									synchronizer.disconnectHook(dest);
								}
							}
						}
						else {
							getLogger().warning("Impossible to send message to host " + dest);
							synchronizer.disconnectHook(dest);
						}

					} catch (DatabaseException | IOException ex) {
						getLogger().severeLog("Unexpected exception", ex);
					}
					finally {
						if (currentBigDataTransferID==null && currentBigDataFileName!=null)
						{
							if (currentBigDataFileName.exists())
								//noinspection ResultOfMethodCallIgnored
								currentBigDataFileName.delete();
							currentBigDataFileName=null;
							currentBigDataHostID=null;
						}
					}
				}
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

								File fileName=getMadkitConfig().getDatabaseSynchronisationFileName();
								RandomOutputStream out=getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(fileName, RandomFileOutputStream.AccessMode.READ_AND_WRITE, FILE_BUFFER_LENGTH_BYTES, 1);
								m.acceptTransfer(out);
								currentBigDataReceiving.put(m.getConversationID(), new BigDataMetaData(b, out, fileName));
								generateError=false;
							}
						}
					} catch (DatabaseException e) {
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
				if (currentBigDataFileName.exists())
					//noinspection ResultOfMethodCallIgnored
					currentBigDataFileName.delete();
				currentBigDataFileName=null;
				currentBigDataTransferID=null;

				receiveMessage(checkEvents);
				if (res.getType()!=BigDataResultMessage.Type.BIG_DATA_TRANSFERRED)
				{
					try {
						synchronizer.disconnectHook(currentBigDataHostID);
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
					} catch (DatabaseException | IOException ex) {
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
						synchronizer.disconnectHook(d.eventToSend.getHostSource());
					} catch (DatabaseException e) {
						e.printStackTrace();
						getLogger().severe(e.getMessage());
					}
				}
				getLogger().finest("Unable to send big data result message " + res);

			}

		}
		else if (_message instanceof NetworkObjectMessage && ((NetworkObjectMessage) _message).getContent() instanceof DBEventToSend)
		{
			boolean generateError = true;
			DecentralizedValue peerID = getDistantPeerID(_message.getSender());
			if (peerID != null) {
				DBEventToSend e = (DBEventToSend) ((NetworkObjectMessage) _message).getContent();
				try {
					DecentralizedValue source = e.getHostSource();

					if (source != null && source.equals(peerID)) {
						if (e instanceof HookAddRequest)
						{
							generateError = false;
							HookAddRequest rep=synchronizer.receivedHookAddRequest((HookAddRequest)e);
							if (rep!=null)
								sendReply(_message, new NetworkObjectMessage<>(rep));
							else {

								sendMessageWithRole(getDistantGroupID(peerID), CloudCommunity.Roles.SYNCHRONIZER, new DatabaseConnectionInitializationMessage(synchronizer.getLastValidatedSynchronization(localHostID)), CloudCommunity.Roles.SYNCHRONIZER);
							}

						}
						else if (synchronizer.isInitialized(peerID)) {
							generateError = false;
							synchronizer.received(e);

						}
						if (!generateError) {
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Event " + e.getClass() + " received from peer " + peerID);
							receiveMessage(checkEvents);
						}
					}
				} catch (DatabaseException ex) {
					getLogger().severeLog("Unexpected exception", ex);
					anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
				}
			}
			if (generateError) {
				if (_message.getSender().isFrom(getKernelAddress()))
					getLogger().warning("Invalid message received from " + _message.getSender());
				else
					anomalyDetectedWithOneDistantKernel(true, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
			}
		} else if (_message.getSender().isFrom(getKernelAddress()) && _message.getSender().getRole().equals(LocalCommunity.Roles.KERNEL) && _message instanceof ObjectMessage && ((ObjectMessage<?>) _message).getContent() instanceof MadkitKernel.InternalDatabaseSynchronizerEvent)
		{

			MadkitKernel.InternalDatabaseSynchronizerEvent e=(MadkitKernel.InternalDatabaseSynchronizerEvent)((ObjectMessage<?>) _message).getContent();
			if (e.type== MadkitKernel.InternalDatabaseSynchronizerEventType.ASSOCIATE_DISTANT_DATABASE_HOST) {
				try {
					DecentralizedValue hostIdentifier = (DecentralizedValue) e.parameters[0];
					boolean conflictualRecordsReplacedByDistantRecords = (boolean) e.parameters[1];
					Package[] packages = (Package[]) e.parameters[2];


					if (hostIdentifier!=null) {
						Group group = getDistantGroupID(hostIdentifier);
						if (group==null) {
							group = CloudCommunity.Groups.getDistributedDatabaseGroup(localHostIDString, hostIdentifier);
						}
						addDistantGroupID(group, hostIdentifier);
						AgentAddress aa = getAgentWithRole(group, CloudCommunity.Roles.SYNCHRONIZER);
						if (aa == null) {
							getMadkitConfig().differDistantDatabaseHostConfiguration(hostIdentifier, conflictualRecordsReplacedByDistantRecords, packages);
							updateGroupAccess(this);

						} else {
							askForHookAddingAndSynchronizeDatabase(group, hostIdentifier, conflictualRecordsReplacedByDistantRecords, packages);
						}
					}
					else
						getLogger().severeLog("Unable to apply database event " + e.type);
				} catch (Exception ex) {
					getLogger().severeLog("Unable to apply database event " + e.type, ex);
				}
			}
			else if (e.type== MadkitKernel.InternalDatabaseSynchronizerEventType.DISSOCIATE_DISTANT_DATABASE_HOST) {
				try {
					DecentralizedValue hostIdentifier = (DecentralizedValue) e.parameters[0];
					Package[] packages = (Package[]) e.parameters[1];
					if (synchronizer.isInitialized(hostIdentifier)) {
						if (logger!=null && logger.isLoggable(Level.FINE))
							logger.fine("Disconnect peer : "+hostIdentifier);

						synchronizer.disconnectHook(hostIdentifier);

					}
					removeDistantGroupID(hostIdentifier);
					getMadkitConfig().removeDistantDatabaseHost(hostIdentifier, packages);
					updateGroupAccess(this);

				} catch (Exception ex) {
					getLogger().severeLog("Unable to apply database event " + e.type, ex);
				}
			}
			else
				getLogger().warning("incomprehensible message "+_message);
		}
	}

	static void updateGroupAccess(AbstractAgent agent) {
		ReturnCode rc;
		if (!(rc=agent.broadcastMessageWithRole(LocalCommunity.Groups.NETWORK,
				LocalCommunity.Roles.SOCKET_AGENT_ROLE, new ObjectMessage<>(NetworkAgent.REFRESH_GROUPS_ACCESS), CloudCommunity.Roles.SYNCHRONIZER)).equals(ReturnCode.SUCCESS))
			if (agent.logger!=null && agent.logger.isLoggable(Level.WARNING))
				agent.logger.warning("Impossible to broadcast group rights update order : "+rc);
	}

	private void askForHookAddingAndSynchronizeDatabase(Group group, DecentralizedValue hostIdentifier,
																 boolean conflictualRecordsReplacedByDistantRecords, Package... packages) {
		try {
			HookAddRequest request=synchronizer.askForHookAddingAndSynchronizeDatabase(hostIdentifier, conflictualRecordsReplacedByDistantRecords, packages);

			if (!sendMessageWithRole(group, CloudCommunity.Roles.SYNCHRONIZER, new NetworkObjectMessage<>(request), CloudCommunity.Roles.SYNCHRONIZER).equals(ReturnCode.SUCCESS)) {
				getLogger().warning("Impossible to send message to host " + request);
				synchronizer.disconnectHook(hostIdentifier);
			}
		} catch (DatabaseException ex) {
			getLogger().severeLog("Unable to apply database event :CONFIGURE_DISTANT_DATABASE_HOST", ex);
		}
	}


	private void checkDifferedDistantDatabaseHostConfiguration(DecentralizedValue hostIdentifier)
	{
		try {
			DatabaseWrapper dw=getMadkitConfig().getDatabaseWrapper();
			DifferedDistantDatabaseHostConfigurationTable table=dw.getTableInstance(DifferedDistantDatabaseHostConfigurationTable.class);
			DifferedDistantDatabaseHostConfigurationTable.Record r=table.getDifferedDistantDatabaseHostConfiguration(hostIdentifier);
			if (r!=null)
			{
				Group group=getDistantGroupID( hostIdentifier);
				if (group!=null) {
					table.removeRecord(r);
					askForHookAddingAndSynchronizeDatabase(group, r.getHostIdentifier(), r.isConflictualRecordsReplacedByDistantRecords(), r.getPackages());
				}
			}
		} catch (DatabaseException | IOException e) {
			getLogger().severeLog("Unable check database event :CONFIGURE_DISTANT_DATABASE_HOST", e);
		}

	}
}
