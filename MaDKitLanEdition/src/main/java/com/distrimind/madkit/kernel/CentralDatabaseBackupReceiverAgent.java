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
import com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupReceiver;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.ood.database.messages.P2PDatabaseEventToSend;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.data_buffers.WrappedString;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.2.0
 */
public class CentralDatabaseBackupReceiverAgent extends AgentFakeThread{
	private Map<Group, DecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
	private Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
	private CentralDatabaseBackupReceiver centralDatabaseBackupReceiver;
	private WrappedString centralIDString;
	private static class CheckEvents extends Message
	{

	}
	private static final CheckEvents checkEvents=new CheckEvents();
	static void updateGroupAccess(AbstractAgent agent) {
		ReturnCode rc;
		if (!(rc=agent.broadcastMessageWithRole(LocalCommunity.Groups.NETWORK,
				LocalCommunity.Roles.SOCKET_AGENT_ROLE, new ObjectMessage<>(NetworkAgent.REFRESH_GROUPS_ACCESS),
				CloudCommunity.Roles.CENTRAL_SYNCHRONIZER)).equals(ReturnCode.SUCCESS))
			if (agent.logger!=null && agent.logger.isLoggable(Level.WARNING))
				agent.logger.warning("Impossible to broadcast group rights update order : "+rc);
	}
	@Override
	protected void activate() throws InterruptedException {
		try {
			centralDatabaseBackupReceiver= getMadkitConfig().getCentralDatabaseBackupReceiver();
			if (centralDatabaseBackupReceiver==null)
				throw DatabaseException.getDatabaseException(new IllegalAccessException());
			centralIDString=CloudCommunity.Groups.encodeDecentralizedValue(centralDatabaseBackupReceiver.getCentralID());
			requestRole(LocalCommunity.Groups.NETWORK, CloudCommunity.Roles.CENTRAL_SYNCHRONIZER);
			updateGroupAccess(this);
			this.requestHookEvents(HookMessage.AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER);
			this.requestHookEvents(HookMessage.AgentActionEvent.LEAVE_ROLE);
		} catch (DatabaseException e) {
			getLogger().severeLog("Impossible to load CentralDatabaseBackupReceiverAgent",e );
		}

	}
	private Group getDistantGroupID(DecentralizedValue id)
	{
		return distantGroupIdsPerID.get(id);
	}
	private DecentralizedValue getDistantPeerID(KernelAddress distantKernelAddress, Group group, String role)
	{
		String path=group.getPath();
		path=path.substring(0, path.length()-1);
		path=path.substring(0, path.lastIndexOf("/")+1);
		if (CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP.getPath().equals(path)
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

				if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.LEAVE_ROLE)) {

					try {
						centralDatabaseBackupReceiver.peerDisconnected(peerID);
					} catch (DatabaseException e) {
						getLogger().severeLog("Impossible to disconnect " + peerID, e);
					}

				}
			}
		}
		else if (_message instanceof NetworkGroupsAccessEvent)
		{
			NetworkGroupsAccessEvent m=(NetworkGroupsAccessEvent)_message;
			HashMap<Group, DecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
			Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
			boolean changed=false;
			for (Group g : m.getGeneralAcceptedGroups().getGroups().getRepresentedGroups())
			{

				if (g.getPath().startsWith(CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP.getPath()))
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
							dv=CloudCommunity.Groups.extractDistantHostIDFromCentralDatabaseBackupGroup(g, centralIDString);
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
			}
			if (this.distantGroupIdsPerGroup.size()!=distantGroupIdsPerGroup.size() && changed)
			{
				for (Map.Entry<Group, DecentralizedValue> e : this.distantGroupIdsPerGroup.entrySet())
				{
					if (!distantGroupIdsPerGroup.containsKey(e.getKey()))
					{
						leaveGroup(e.getKey());
						try {
							centralDatabaseBackupReceiver.peerDisconnected(e.getValue());
						} catch (DatabaseException e2) {
							getLogger().severeLog("Impossible to disconnect " + e.getValue(), e2);
						}
					}
				}
			}
			this.distantGroupIdsPerGroup=distantGroupIdsPerGroup;
			this.distantGroupIdsPerID=distantGroupIdsPerID;
		}
		else if (_message==checkEvents)
		{

		}
		else if (_message instanceof BigDataPropositionMessage)
		{

		}
		else if (_message instanceof BigDataResultMessage)
		{

		}
		else if (_message instanceof NetworkObjectMessage && ((NetworkObjectMessage<?>) _message).getContent() instanceof P2PDatabaseEventToSend)
		{

		}
	}

	@Override
	protected void end() throws InterruptedException {
		try {
			centralDatabaseBackupReceiver.disconnectAllPeers();
		} catch (DatabaseException e) {
			getLogger().severeLog("", e);
		}
	}
}
