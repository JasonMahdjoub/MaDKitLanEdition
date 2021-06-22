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
import com.distrimind.madkit.message.NetworkObjectMessage;
import com.distrimind.ood.database.DatabaseWrapper;
import com.distrimind.ood.database.EncryptedDatabaseBackupMetaDataPerFile;
import com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupCertificate;
import com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupReceiver;
import com.distrimind.ood.database.centraldatabaseapi.FileReference;
import com.distrimind.ood.database.exceptions.DatabaseException;
import com.distrimind.ood.database.messages.BigDataEventToSendWithCentralDatabaseBackup;
import com.distrimind.ood.database.messages.MessageComingFromCentralDatabaseBackup;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.crypto.EncryptionProfileProvider;
import com.distrimind.util.io.RandomCacheFileOutputStream;
import com.distrimind.util.io.RandomFileOutputStream;

import java.io.IOException;
import java.util.logging.Level;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.2.0
 */
public class CentralDatabaseBackupReceiverPerPeer extends com.distrimind.ood.database.centraldatabaseapi.CentralDatabaseBackupReceiverPerPeer {

	private final CentralDatabaseBackupReceiverAgent agent;
	private BigDataTransferID currentBigDataTransferID=null;
	private RandomCacheFileOutputStream currentBigDataOutputStream=null;

	protected CentralDatabaseBackupReceiverPerPeer(CentralDatabaseBackupReceiver centralDatabaseBackupReceiver, DatabaseWrapper wrapper, CentralDatabaseBackupReceiverAgent agent) {
		super(centralDatabaseBackupReceiver, wrapper);
		if (agent==null)
			throw new NullPointerException();
		this.agent=agent;
	}
	private void sendMessage(MessageComingFromCentralDatabaseBackup message, AgentAddress aa, DecentralizedValue dest) {
		try {
			if (message instanceof BigDataEventToSendWithCentralDatabaseBackup) {
				BigDataEventToSendWithCentralDatabaseBackup be = (BigDataEventToSendWithCentralDatabaseBackup) message;
				RandomCacheFileOutputStream currentBigDataOutputStream = agent.getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, DatabaseSynchronizerAgent.FILE_BUFFER_LENGTH_BYTES, 1);
				be.getPartInputStream().transferTo(currentBigDataOutputStream);

				currentBigDataTransferID = agent.sendBigDataWithRole(aa, currentBigDataOutputStream.getRandomInputStream(), message, CloudCommunity.Roles.CENTRAL_SYNCHRONIZER);
				if (currentBigDataTransferID == null) {
					agent.getLogger().warning("Impossible to send message to host " + dest);
					disconnect();
				}
			} else {
				if (!agent.sendMessageWithRole(aa, new NetworkObjectMessage<>(message), CloudCommunity.Roles.CENTRAL_SYNCHRONIZER).equals(AbstractAgent.ReturnCode.SUCCESS)) {
					agent.getLogger().warning("Impossible to send message to host " + dest);
					disconnect();
				}
			}
		}
		catch (DatabaseException | IOException ex) {
			agent.getLogger().severeLog("Unexpected exception", ex);
		}
		finally {
			if (currentBigDataTransferID == null && currentBigDataOutputStream != null) {
				try {
					currentBigDataOutputStream.close();
				} catch (IOException ioException) {
					agent.getLogger().severeLog("Unexpected exception", ioException);
				}
				currentBigDataOutputStream = null;
				//currentBigDataHostID = null;
			}
		}
	}

	@Override
	protected void sendMessageFromThisCentralDatabaseBackup(MessageComingFromCentralDatabaseBackup message)  {
		try {
			DecentralizedValue dest = message.getHostDestination();
			if (agent.logger != null && agent.logger.isLoggable(Level.FINEST))
				agent.logger.finest("Send event " + message.getClass() + " to peer " + dest);
			AgentAddress aa = agent.getAgentWithRole(agent.getDistantGroupID(dest), CloudCommunity.Roles.SYNCHRONIZER);
			if (aa != null) {
				sendMessage(message, aa, dest);
			}
			else {
				agent.getLogger().warning("Impossible to send message to host " + dest);
				disconnect();
			}
		}
		catch (DatabaseException ex) {
			agent.getLogger().severeLog("Unexpected exception", ex);
		}
	}

	@Override
	protected void sendMessageFromOtherCentralDatabaseBackup(DecentralizedValue centralDatabaseBackupID, MessageComingFromCentralDatabaseBackup message) {
		DecentralizedValue dest=message.getHostDestination();
		AgentAddress aa = agent.getAgentWithRole(agent.getDistantGroupID(dest), CloudCommunity.Roles.SYNCHRONIZER);
		if (aa!=null)
		{
			sendMessage(message, aa, dest);
		}
		else {
			String roleDest = CloudCommunity.Groups.encodeDecentralizedValue(centralDatabaseBackupID).toString();
			aa = agent.getAgentAddressIn(CloudCommunity.Groups.CENTRAL_DATABASE_BACKUP, roleDest);
			if (aa != null) {
				try {

					if (agent.logger != null && agent.logger.isLoggable(Level.FINEST))
						agent.logger.finest("Send event " + message.getClass() + " to peer " + centralDatabaseBackupID);
					if (message instanceof BigDataEventToSendWithCentralDatabaseBackup) {
						BigDataEventToSendWithCentralDatabaseBackup be = (BigDataEventToSendWithCentralDatabaseBackup) message;
						RandomCacheFileOutputStream currentBigDataOutputStream = agent.getMadkitConfig().getCacheFileCenter().getNewBufferedRandomCacheFileOutputStream(true, RandomFileOutputStream.AccessMode.READ_AND_WRITE, DatabaseSynchronizerAgent.FILE_BUFFER_LENGTH_BYTES, 1);
						be.getPartInputStream().transferTo(currentBigDataOutputStream);

						BigDataTransferID bdid=agent.sendBigData(aa, currentBigDataOutputStream.getRandomInputStream(), message);
						if (bdid==null && agent.logger!=null)
							agent.logger.warning("Message not sent to other central database backup : "+message);

					} else {
						if (!agent.sendMessage(aa, new NetworkObjectMessage<>(message)).equals(AbstractAgent.ReturnCode.SUCCESS) && agent.logger!=null)
							agent.logger.warning("Message not sent to other central database backup : "+message);
					}
				} catch (IOException ex) {
					agent.getLogger().severeLog("Unexpected exception", ex);
				}
			}
			else if (agent.logger!=null)
				agent.logger.warning("Message not sent to other central database backup : "+message);
		}
	}

	@Override
	protected EncryptionProfileProvider getEncryptionProfileProviderToValidateCertificateOrGetNullIfNoValidProviderIsAvailable(CentralDatabaseBackupCertificate certificate) {
		return null;
	}

	@Override
	public FileReference getFileReference(EncryptedDatabaseBackupMetaDataPerFile encryptedDatabaseBackupMetaDataPerFile) {
		return null;
	}
}
