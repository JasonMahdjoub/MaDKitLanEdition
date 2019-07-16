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
import com.distrimind.madkit.message.NetworkObjectMessage;
import com.distrimind.madkit.message.hook.HookMessage;
import com.distrimind.madkit.message.hook.OrganizationEvent;
import com.distrimind.ood.database.*;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.util.DecentralizedValue;
import org.apache.commons.codec.binary.Base64;

import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
public class DatabaseSynchronizerAgent extends AgentFakeThread {


	private DecentralizedValue localHostID;
	private String encodedLocalHostID;
	private DatabaseWrapper.DatabaseSynchronizer synchronizer;

	DatabaseSynchronizerAgent()
	{

	}

	private static class CheckEvents extends Message
	{

	}


	@Override
	protected void activate() throws InterruptedException {
		if (logger!=null && logger.isLoggable(Level.INFO))
			logger.info("Launch data synchronizer");
		this.requestRole(LocalCommunity.Groups.DATABASE, LocalCommunity.Roles.DATABASE_SYNCHRONIZER_LISTENER);
		this.requestRole(CloudCommunity.Groups.DISTRIBUTED_DATABASE, encodedLocalHostID=CloudCommunity.Roles.getDistributedDatabaseRole(localHostID));
		this.requestHookEvents(HookMessage.AgentActionEvent.REQUEST_ROLE);
		this.requestHookEvents(HookMessage.AgentActionEvent.LEAVE_ROLE);

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
				encodedLocalHostID=Base64.encodeBase64URLSafeString(localHostID.encodeWithDefaultParameters());
				synchronizer.setNotifier(new DatabaseNotifier() {
					@Override
					public void newDatabaseEventDetected(DatabaseWrapper wrapper) throws DatabaseException {
						receiveMessage(new CheckEvents());
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

			}
		} catch (DatabaseException e) {
			getLogger().severeLog("Unexpected exception ", e);
			this.killAgent(this);
		}
	}

	@Override
	protected void end() {
		synchronizer.disconnectAll();
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
						if (synchronizer.isPairedWith(peerID)) {
							if (logger!=null && logger.isLoggable(Level.FINE))
								logger.info("Connection initialization with peer : "+peerID);

							sendMessage(aa, new DatabaseConnectionInitializationMessage(synchronizer.getLastValidatedSynchronization(localHostID)));
						}
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

			DecentralizedValue peerID=getPeerID(_message.getSender());
			if (peerID!=null) {
				try {

					synchronizer.initHook(peerID, ((DatabaseConnectionInitializationMessage) _message).getContent());
					if (logger!=null && logger.isLoggable(Level.FINE))
						logger.info("Connection initialization with peer : "+peerID+" FINISHED");
				} catch (DatabaseException e) {
					if (!_message.getSender().isFrom(getKernelAddress()))
						anomalyDetectedWithOneDistantKernel(false, _message.getSender().getKernelAddress(), "Impossible to connect database peer "+peerID);
					getLogger().severeLog("Impossible to connect database peer "+peerID, e);
				}
			}
			else
				if (!_message.getSender().isFrom(getKernelAddress()))
					anomalyDetectedWithOneDistantKernel(true, _message.getSender().getKernelAddress(), "Invalided message received from "+_message.getSender());



		} else if (_message instanceof CheckEvents)
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

						if (!sendMessage(CloudCommunity.Groups.DISTRIBUTED_DATABASE, CloudCommunity.Roles.getDistributedDatabaseRole(dest), new NetworkObjectMessage<>(es)).equals(ReturnCode.SUCCESS)) {
							getLogger().warning("Impossible to send message to host " + dest);
							synchronizer.deconnectHook(dest);
						}

					} catch (DatabaseException ex) {
						getLogger().severeLog("Unexpected exception", ex);
					}
				}
			}
		}
		else if (_message instanceof DatabaseEventToSend)
		{
			if (!_message.getReceiver().getRole().equals(encodedLocalHostID))
			{
				getLogger().warning("Invalid destination : "+encodedLocalHostID);
			}
			else {
				boolean generateError = true;
				DecentralizedValue peerID = getPeerID(_message.getSender());
				if (peerID != null) {
					DatabaseEventToSend e = (DatabaseEventToSend) _message;
					try {
						DecentralizedValue source = e.getHostSource();
						if (source != null && source.equals(peerID)) {
							if (synchronizer.isInitialized(source)) {
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
			}
		}
	}
}
