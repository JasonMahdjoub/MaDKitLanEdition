package com.distrimind.madkit.kernel.distributed_database;
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
import com.distrimind.madkit.kernel.AgentAddress;
import com.distrimind.madkit.kernel.AgentFakeThread;
import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.Message;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.madkit.message.hook.OrganizationEvent;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.ood.database.HookAddRequest;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.DecentralizedValue;
import org.apache.commons.codec.binary.Base64;

/**
 * @author Jason
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
public class DatabaseSynchronizerAgent extends AgentFakeThread {

	private DecentralizedValue distantPeerID;
	private DecentralizedValue localHostID;
	private String encodedLocalHostID;
	private DatabaseWrapper databaseWrapper;
	private DatabaseWrapper.DatabaseSynchronizer synchronizer;
	private Package []concernedPackages;


	@Override
	protected void activate() throws InterruptedException {
		this.requestRole(LocalCommunity.Groups.DATABASE, LocalCommunity.Roles.DISTANT_DATABASE_PEER_LISTENER);
		this.requestRole(CloudCommunity.Groups.DISTRIBUTED_DATABASE, encodedLocalHostID=CloudCommunity.Roles.getDistributedDatabaseRole(localHostID));
		this.requestHookEvents(HookMessage.AgentActionEvent.REQUEST_ROLE);
		this.requestHookEvents(HookMessage.AgentActionEvent.LEAVE_ROLE);
	}

	private void checkAvailablePeers()
	{

	}

	private DecentralizedValue getPeerID(String role, KernelAddress distantKernelAddress)
	{
		try {
			return DecentralizedValue.decode(Base64.decodeBase64(role));
		}
		catch(Exception e)
		{
			if (distantKernelAddress!=null)
				anomalyDetectedWithOneDistantKernel(true, distantKernelAddress, "Invalided peer ID "+role);
			getLogger().severeLog("Invalided peer ID "+role, e);
			return null;
		}
	}

	private DecentralizedValue getPeerID(AgentAddress aa)
	{
		if (aa.getGroup().getParent()!=null && aa.getGroup().getParent().equals(CloudCommunity.Groups.DISTRIBUTED_DATABASE)) {
			return getPeerID(aa.getRole(), aa.isFrom(getKernelAddress())?null:aa.getKernelAddress());
		}
		else {
			return null;
		}

	}
	@Override
	protected void liveByStep(Message _message) {
		if (_message instanceof OrganizationEvent)
		{
			AgentAddress aa=((OrganizationEvent) _message).getSourceAgent();
			DecentralizedValue peerID=getPeerID(aa);
			if (peerID!=null) {

				if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.REQUEST_ROLE)) {
					try {
						sendMessage(aa, new ConnectionInitialization(synchronizer.getLastValidatedSynchronization(localHostID)));
					} catch (DatabaseException e) {
						if (!_message.getSender().isFrom(getKernelAddress()))
							anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Unexpected exception");

						getLogger().severeLog("Unexpected exception", e);
					}
				} else if (((OrganizationEvent) _message).getContent().equals(HookMessage.AgentActionEvent.LEAVE_ROLE)) {
					try {
						synchronizer.deconnectHook(peerID);
					} catch (DatabaseException e) {
						getLogger().severeLog("Impossible to disconnect " + peerID, e);
					}

				}
			}
		}
		else if (_message instanceof ConnectionInitialization)
		{
			DecentralizedValue peerID=getPeerID(_message.getSender());
			if (peerID!=null) {
				try {
					synchronizer.initHook(peerID, ((ConnectionInitialization) _message).getContent());
				} catch (DatabaseException e) {
					if (!_message.getSender().isFrom(getKernelAddress()))
						anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Impossible to connect database peer "+peerID);
					getLogger().severeLog("Impossible to connect database peer "+peerID, e);
				}
			}
			else
			if (!_message.getSender().isFrom(getKernelAddress()))
				anomalyDetectedWithOneDistantKernel(true, _message.getSender().getKernelAddress(), "Invalided message received from "+_message.getSender());



		}
	}
}
