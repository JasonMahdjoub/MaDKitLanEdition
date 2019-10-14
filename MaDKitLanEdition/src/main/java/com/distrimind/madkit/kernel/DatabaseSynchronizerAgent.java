package com.distrimind.madkit.kernel;
/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java langage 

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
import com.distrimind.util.DecentralizedValue;

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
	private DatabaseWrapper.DatabaseSynchronizer synchronizer;
	private static final CheckEvents checkEvents=new CheckEvents();
	private final Map<Group, DecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
	private final Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();

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
			logger.info("Launch data synchronizer");
		this.requestRole(LocalCommunity.Groups.DATABASE, LocalCommunity.Roles.DATABASE_SYNCHRONIZER_LISTENER);


		try {
			DatabaseWrapper databaseWrapper = getMadkitConfig().getDatabaseWrapper();
			synchronizer= databaseWrapper.getSynchronizer();
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
				for (DifferedDistantDatabaseHostConfigurationTable.Record r : databaseWrapper.getTableInstance(DifferedDistantDatabaseHostConfigurationTable.class).getRecords()){
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
	protected void liveByStep(Message _message) {

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
								logger.info("Connection initialization with peer : "+peerID);

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
								logger.info("Disconnect peer : "+peerID);

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
						logger.info("Connection initialization with peer : "+peerID+" FINISHED");
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
			while ((e=synchronizer.nextEvent())!=null)
			{
				if (e instanceof DatabaseEventToSend)
				{
					DatabaseEventToSend es=(DatabaseEventToSend)e;
					try {
						DecentralizedValue dest=es.getHostDestination();
						if (logger!=null && logger.isLoggable(Level.FINEST))
							logger.info("Send event "+es.getClass()+" to peer "+dest);

						if (!sendMessageWithRole(this.getDistantGroupID(dest), CloudCommunity.Roles.SYNCHRONIZER, new NetworkObjectMessage<>(es), CloudCommunity.Roles.SYNCHRONIZER).equals(ReturnCode.SUCCESS)) {
							getLogger().warning("Impossible to send message to host " + dest);
							synchronizer.disconnectHook(dest);
						}

					} catch (DatabaseException ex) {
						getLogger().severeLog("Unexpected exception", ex);
					}
				}
			}
		}
		else if (_message instanceof NetworkObjectMessage && ((NetworkObjectMessage) _message).getContent() instanceof DatabaseEventToSend)
		{
			boolean generateError = true;
			DecentralizedValue peerID = getDistantPeerID(_message.getSender());
			if (peerID != null) {
				DatabaseEventToSend e = (DatabaseEventToSend) ((NetworkObjectMessage) _message).getContent();
				try {
					DecentralizedValue source = e.getHostSource();
					if (source != null && source.equals(peerID)) {
						if ((e instanceof HookAddRequest) || synchronizer.isInitialized(source)) {
							generateError = false;
							synchronizer.received(e);
							if (logger!=null && logger.isLoggable(Level.FINEST))
								logger.info("Event "+e.getClass()+" received from peer "+peerID);

						}
					}
				} catch (DatabaseException ex) {
					getLogger().severeLog("Unexpected exception", ex);
					anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalided message received from " + _message.getSender());
				}
			}
			if (generateError) {
				if (_message.getSender().isFrom(getKernelAddress()))
					getLogger().warning("Invalid message received from " + _message.getSender());
				else
					anomalyDetectedWithOneDistantKernel(true, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
			}
		} else if (_message.getSender().isFrom(getKernelAddress()) && _message.getSender().getRole().equals(LocalCommunity.Roles.KERNEL) && _message instanceof ObjectMessage && ((ObjectMessage) _message).getContent() instanceof MadkitKernel.InternalDatabaseSynchronizerEvent)
		{

			MadkitKernel.InternalDatabaseSynchronizerEvent e=(MadkitKernel.InternalDatabaseSynchronizerEvent)((ObjectMessage) _message).getContent();
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
							logger.info("Disconnect peer : "+hostIdentifier);

						synchronizer.disconnectHook(hostIdentifier);

					}
					removeDistantGroupID(hostIdentifier);
					getMadkitConfig().removeDistantDatabaseHost(hostIdentifier, packages);

				} catch (Exception ex) {
					getLogger().severeLog("Unable to apply database event " + e.type, ex);
				}
			}
			else
				getLogger().warning("incomprehensible message "+_message);
		}
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
