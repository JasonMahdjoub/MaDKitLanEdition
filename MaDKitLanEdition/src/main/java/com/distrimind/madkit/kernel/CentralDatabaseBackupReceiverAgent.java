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
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.ood.database.messages.BigDataEventToSendWithCentralDatabaseBackup;
import com.distrimind.ood.database.messages.MessageComingFromCentralDatabaseBackup;
import com.distrimind.ood.database.messages.MessageDestinedToCentralDatabaseBackup;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.data_buffers.WrappedString;
import com.distrimind.util.io.*;

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
	private Map<Group, DatabaseSynchronizerAgent.KernelAddressAndDecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
	private Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();
	private CentralDatabaseBackupReceiver centralDatabaseBackupReceiver;
	private WrappedString centralIDString;
	private final HashMap<ConversationID, DatabaseSynchronizerAgent.BigDataMetaData> currentBigDataReceiving=new HashMap<>();
	private final HashMap<ConversationID, DatabaseSynchronizerAgent.BigDataMetaData> currentBigDataSending=new HashMap<>();
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
			if (logger!=null && logger.isLoggable(Level.INFO))
				logger.info("Launch central database backup receiver");
			centralDatabaseBackupReceiver= getMadkitConfig().getCentralDatabaseBackupReceiver();
			if (centralDatabaseBackupReceiver==null)
				throw DatabaseException.getDatabaseException(new IllegalAccessException());
			centralIDString=CloudCommunity.Groups.encodeDecentralizedValue(centralDatabaseBackupReceiver.getCentralID());
			centralDatabaseBackupReceiver.setAgent(this);
			if (!requestRole(LocalCommunity.Groups.NETWORK, CloudCommunity.Roles.CENTRAL_SYNCHRONIZER).equals(ReturnCode.SUCCESS))
				getLogger().warning("Impossible to enter in the group LocalCommunity.Groups.NETWORK");

			if (!requestRole(CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP, CloudCommunity.Groups.encodeDecentralizedValue(centralDatabaseBackupReceiver.getCentralID()).toString()).equals(ReturnCode.SUCCESS))
				getLogger().warning("Impossible to enter in the group CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP");
			updateGroupAccess(this);
			this.requestHookEvents(HookMessage.AgentActionEvent.ACCESSIBLE_LAN_GROUPS_GIVEN_TO_DISTANT_PEER);
			this.requestHookEvents(HookMessage.AgentActionEvent.LEAVE_ROLE);

		} catch (DatabaseException e) {
			getLogger().severeLog("Impossible to load CentralDatabaseBackupReceiverAgent",e );
		}

	}
	Group getDistantGroupID(DecentralizedValue id)
	{
		return distantGroupIdsPerID.get(id);
	}
	private DecentralizedValue getDistantPeerID(KernelAddress distantKernelAddress, Group group, String role)
	{
		if (CloudCommunity.Groups.CLIENT_SERVER_DATABASE_WITH_SUB_GROUPS.includes(group)
				&& role.equals(CloudCommunity.Roles.SYNCHRONIZER)) {
			DatabaseSynchronizerAgent.KernelAddressAndDecentralizedValue res = distantGroupIdsPerGroup.get(group);
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
			HashMap<Group, DatabaseSynchronizerAgent.KernelAddressAndDecentralizedValue> distantGroupIdsPerGroup=new HashMap<>();
			Map<DecentralizedValue, Group> distantGroupIdsPerID=new HashMap<>();

			for (Group g : m.getGeneralAcceptedGroups().getGroups().getRepresentedGroups())
			{

				if (CloudCommunity.Groups.CLIENT_SERVER_DATABASE_WITH_SUB_GROUPS.includes(g))
				{
					DatabaseSynchronizerAgent.KernelAddressAndDecentralizedValue dv=this.distantGroupIdsPerGroup.get(g);
					if (dv!=null)
					{
						distantGroupIdsPerGroup.put(g, dv );
						distantGroupIdsPerID.put(dv.decentralizedValue, g);
						if (!hasRole(g, CloudCommunity.Roles.CENTRAL_SYNCHRONIZER))
							getLogger().warning("CloudCommunity.Roles.SYNCHRONIZER role should be requested with group "+g);
					}
					else
					{
						try {
							DecentralizedValue d=CloudCommunity.Groups.extractDistantHostIDFromCentralDatabaseBackupGroup(g, centralIDString);
							if (d!=null)
							{
								dv=new DatabaseSynchronizerAgent.KernelAddressAndDecentralizedValue(m.getConcernedKernelAddress(), d);
								if (logger!=null && logger.isLoggable(Level.INFO))
									logger.info("Client available : "+dv);
								distantGroupIdsPerGroup.put(g, dv );
								distantGroupIdsPerID.put(dv.decentralizedValue, g);
								this.requestRole(g, CloudCommunity.Roles.CENTRAL_SYNCHRONIZER);
							}
						} catch (IOException ignored) {

						}
					}
				}
			}
			for (Map.Entry<Group, DatabaseSynchronizerAgent.KernelAddressAndDecentralizedValue> e : this.distantGroupIdsPerGroup.entrySet())
			{
				if (!distantGroupIdsPerGroup.containsKey(e.getKey()))
				{
					if (e.getValue().kernelAddress.equals(m.getConcernedKernelAddress()))
						disconnectPeer(e.getKey(), e.getValue().decentralizedValue);
					else
						distantGroupIdsPerGroup.put(e.getKey(), e.getValue());
				}
			}
			this.distantGroupIdsPerGroup=distantGroupIdsPerGroup;
			this.distantGroupIdsPerID=distantGroupIdsPerID;
		}
		else if (_message instanceof BigDataPropositionMessage)
		{
			BigDataPropositionMessage m=(BigDataPropositionMessage)_message;
			boolean generateError=true;
			if (m.getAttachedData() instanceof MessageComingFromCentralDatabaseBackup)
			{
				BigDataEventToSendWithCentralDatabaseBackup b = (BigDataEventToSendWithCentralDatabaseBackup) m.getAttachedData();
				try {
					DecentralizedValue centralPeerID = getCentralPeerID(_message.getSender());
					if (centralPeerID != null) {
						RandomOutputStream out = getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, DatabaseSynchronizerAgent.FILE_BUFFER_LENGTH_BYTES, 1);
						m.acceptTransfer(out);
						currentBigDataReceiving.put(m.getConversationID(), new DatabaseSynchronizerAgent.BigDataMetaData(b, out));
						generateError = false;
					}
				} catch (IOException e) {
					e.printStackTrace();
					getLogger().severe(e.getMessage());
				}
			}
			else if (m.getAttachedData() instanceof MessageDestinedToCentralDatabaseBackup) {
				MessageDestinedToCentralDatabaseBackup b = (MessageDestinedToCentralDatabaseBackup) m.getAttachedData();
				try {
					DecentralizedValue peerID = getDistantPeerID(_message.getSender());
					if (peerID != null) {
						RandomOutputStream out = getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, DatabaseSynchronizerAgent.FILE_BUFFER_LENGTH_BYTES, 1);
						m.acceptTransfer(out);
						currentBigDataReceiving.put(m.getConversationID(), new DatabaseSynchronizerAgent.BigDataMetaData(b, out));
						generateError = false;
					}
				} catch (IOException e) {
					e.printStackTrace();
					getLogger().severe(e.getMessage());
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
			DatabaseSynchronizerAgent.BigDataMetaData cur=currentBigDataSending.remove(res.getConversationID());
			if (cur!=null)
			{
				if (res.getType() != BigDataResultMessage.Type.BIG_DATA_TRANSFERRED) {
					if (res.getReceiver().getGroup().getPath().startsWith(CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP.getPath()))
					{
						getLogger().warning("Impossible to send message to " + _message.getReceiver());
					}
					if (res.getReceiver().getGroup().getPath().startsWith(CloudCommunity.Groups.CLIENT_SERVER_DATABASE.getPath()))
					{
						disconnectPeer(res.getReceiver() );
					}
					else
					{
						getLogger().warning("Invalid message received from " + _message.getSender());
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
						if (cur.eventToSend instanceof MessageComingFromCentralDatabaseBackup)
						{
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Received big data result message " + res);

							try {
								final RandomInputStream in = cur.randomOutputStream.getRandomInputStream();

								((BigDataEventToSendWithCentralDatabaseBackup) cur.eventToSend).setPartInputStream(in);
								if (!centralDatabaseBackupReceiver.sendMessageFromThisCentralDatabaseBackup((MessageComingFromCentralDatabaseBackup) cur.eventToSend))
									logger.warning("Message not sent : "+cur.eventToSend);
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
						else if (cur.eventToSend instanceof MessageDestinedToCentralDatabaseBackup)
						{
							if (logger != null && logger.isLoggable(Level.FINEST))
								logger.finest("Received big data result message " + res);

							try {
								final RandomInputStream in = cur.randomOutputStream.getRandomInputStream();
								((BigDataEventToSendWithCentralDatabaseBackup) cur.eventToSend).setPartInputStream(in);

								centralDatabaseBackupReceiver.received((MessageDestinedToCentralDatabaseBackup)cur.eventToSend);
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
						if (logger!=null)
							logger.warning("Message not transmitted ("+res.getType()+"): "+_message);
					}
					try {
						cur.close();
					} catch (IOException e) {
						getLogger().severeLog("", e);
					}

				}
			}
		}
		else if (_message instanceof NetworkObjectMessage)
		{
			NetworkObjectMessage<?> m=(NetworkObjectMessage<?>)_message;
			boolean generateError=true;
			if (m.getContent() instanceof MessageComingFromCentralDatabaseBackup)
			{
				MessageComingFromCentralDatabaseBackup b = (MessageComingFromCentralDatabaseBackup) m.getContent();
				try {
					DecentralizedValue centralPeerID = getCentralPeerID(_message.getSender());
					if (centralPeerID != null) {
						if (!centralDatabaseBackupReceiver.sendMessageFromThisCentralDatabaseBackup(b))
							logger.warning("Message not sent : "+b);
						generateError = false;
					}
					else if (logger!=null)
						logger.warning("Invalid message : "+b);
				} catch (DatabaseException e) {
					e.printStackTrace();
					getLogger().severe(e.getMessage());
				}
			}
			else if (m.getContent() instanceof MessageDestinedToCentralDatabaseBackup) {
				MessageDestinedToCentralDatabaseBackup b = (MessageDestinedToCentralDatabaseBackup) m.getContent();
				try {
					DecentralizedValue peerID = getDistantPeerID(_message.getSender());
					if (peerID != null) {
						Integrity i=centralDatabaseBackupReceiver.received(b);
						if (i!=Integrity.OK)
						{
							disconnectPeer(_message.getSender());
							anomalyDetectedWithOneDistantKernel(i==Integrity.FAIL_AND_CANDIDATE_TO_BAN, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
						}
						generateError = false;
					}
					else if (logger!=null)
						logger.warning("Invalid message : "+b);
				} catch (IOException | DatabaseException e) {
					e.printStackTrace();
					getLogger().severe(e.getMessage());
				}
			}
			if (generateError) {
				if (_message.getSender().isFrom(getKernelAddress()))
					getLogger().warning("Invalid message received from " + _message.getSender());
				else
					anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Invalid message received from " + _message.getSender());
			}

		}
	}
	private void disconnectPeer(AgentAddress aa)
	{
		DecentralizedValue peerID=getDistantPeerID(aa.getKernelAddress(), aa.getGroup(), aa.getRole() );
		if (peerID==null) {
			if (logger != null)
				logger.warning("invalid agent address " + aa + ". Impossible to disconnect concerned peer");
		}
		else
		{
			disconnectPeer(aa.getGroup(), peerID);
			this.distantGroupIdsPerGroup.remove(aa.getGroup());
			this.distantGroupIdsPerID.remove(peerID);
		}

	}
	private void disconnectPeer(Group peerGroup, DecentralizedValue peerID)
	{
		leaveGroup(peerGroup);
		try {
			centralDatabaseBackupReceiver.peerDisconnected(peerID);
		} catch (DatabaseException e2) {
			getLogger().severeLog("Impossible to disconnect " + peerID, e2);
		}
	}

	@Override
	protected void end() throws InterruptedException {
		try {
			centralDatabaseBackupReceiver.disconnectAllPeers();
			if (logger!=null)
				logger.info("Disconnect central database synchronizer agent");
		} catch (DatabaseException e) {
			getLogger().severeLog("", e);
		}
	}

	private DecentralizedValue getCentralPeerID(KernelAddress distantKernelAddress, Group group, String role)
	{
		if (CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP.equals(group)
				&& role.equals(CloudCommunity.Roles.CENTRAL_SYNCHRONIZER)) {
			DecentralizedValue res= null;
			try {
				res = CloudCommunity.Groups.decodeDecentralizedValue(role);
			} catch (IOException e) {
				getLogger().severeLog("Invalided peer group " + group, e);
			}

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
	private DecentralizedValue getCentralPeerID(AgentAddress aa)
	{
		return getCentralPeerID(aa.isFrom(getKernelAddress())?null:aa.getKernelAddress(), aa.getGroup(), aa.getRole());
	}
}
