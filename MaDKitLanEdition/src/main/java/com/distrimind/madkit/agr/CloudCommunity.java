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
package com.distrimind.madkit.agr;

import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.AgentNetworkID;
import com.distrimind.madkit.kernel.Gatekeeper;
import com.distrimind.madkit.kernel.Group;
import com.distrimind.util.Bits;
import com.distrimind.util.DecentralizedValue;
import com.distrimind.util.data_buffers.WrappedString;

import java.io.IOException;

/**
 * Defines the default groups and roles used for networking.
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKitGroupEdition 1.0
 * @version 1.0
 * 
 */
public class CloudCommunity implements Organization {// TODO check groups protection

	public static final String NAME = "~~Cloud";

	/**
	 * Default groups in the Cloud community.
	 * 
	 * @since MaDKitGroupEdition 1.0
	 */
	public static final class Groups {




		private static final Gatekeeper databaseGateKeeper = new Gatekeeper() {

			@Override
			public boolean allowAgentToCreateSubGroup(Group _parent_group, Group _sub_group,
													  final Class<? extends AbstractAgent> requesterClass, AgentNetworkID _agentNetworkID,
													  Object _memberCard) {
				return requesterClass.getCanonicalName()
						.equals("com.distrimind.madkit.kernel.DatabaseSynchronizerAgent")
						|| requesterClass.getCanonicalName()
						.equals("com.distrimind.madkit.kernel.CentralDatabaseBackupReceiverAgent");}

			@Override
			public boolean allowAgentToTakeRole(Group _group, String _roleName,
												final Class<? extends AbstractAgent> requesterClass, AgentNetworkID _agentNetworkID,
												Object _memberCard) {
				return requesterClass.getCanonicalName()
						.equals("com.distrimind.madkit.kernel.DatabaseSynchronizerAgent")
						|| requesterClass.getCanonicalName()
						.equals("com.distrimind.madkit.kernel.CentralDatabaseBackupReceiverAgent");

			}
		};


		public static final Group DISTRIBUTED_DATABASE = LocalCommunity.Groups.DATABASE.getSubGroup(true, databaseGateKeeper, true, "~~peers");
		public static final Group DISTRIBUTED_DATABASE_WITH_SUB_GROUPS = DISTRIBUTED_DATABASE.getThisGroupWithItsSubGroups();
		public static final Group CLIENT_SERVER_DATABASE = LocalCommunity.Groups.DATABASE.getSubGroup(true, databaseGateKeeper, true, "~~client_server");
		public static final Group CLIENT_SERVER_DATABASE_WITH_SUB_GROUPS = CLIENT_SERVER_DATABASE.getThisGroupWithItsSubGroups();
		public static final Group CENTRAL_DATABASE_BACKUP = LocalCommunity.Groups.DATABASE.getSubGroup(true, databaseGateKeeper, true, "~~central_database_backup");
		public static final Group CENTRAL_DATABASE_BACKUP_WITH_SUB_GROUPS = CENTRAL_DATABASE_BACKUP.getThisGroupWithItsSubGroups();

		public static WrappedString encodeDecentralizedValue(DecentralizedValue identifier)
		{
			return identifier.encode().toWrappedString();
		}
		public static DecentralizedValue decodeDecentralizedValue(String value) throws IOException {
			return DecentralizedValue.decode(new WrappedString(value).toWrappedData());
		}
		public static Group getDistributedDatabaseGroup(String localIdentifier, WrappedString distantIdentifier)
		{
			return getDistributedDatabaseGroup(localIdentifier, distantIdentifier.toString());
		}
		public static Group getDistributedDatabaseGroup(String localIdentifier, String distantIdentifier)
		{
			if (localIdentifier==null)
				throw new NullPointerException();
			if (distantIdentifier==null)
				throw new NullPointerException();
			if (localIdentifier.length()==0)
				throw new IllegalArgumentException();
			if (distantIdentifier.length()==0)
				throw new IllegalArgumentException();
			String subgroup;
			int cmp=localIdentifier.compareTo(distantIdentifier);
			if (cmp<0)
				subgroup=localIdentifier+"~"+distantIdentifier;
			else if (cmp>0)
				subgroup=distantIdentifier+"~"+localIdentifier;
			else
				throw new IllegalArgumentException(""+cmp);

			return DISTRIBUTED_DATABASE.getSubGroup(true, databaseGateKeeper, false, subgroup);
		}
		public static Group getCentralDatabaseGroup(DecentralizedValue localIdentifier, DecentralizedValue distantIdentifier)
		{
			return getCentralDatabaseGroup(encodeDecentralizedValue(localIdentifier), encodeDecentralizedValue(distantIdentifier));
		}
		public static Group getCentralDatabaseGroup(String localIdentifier, DecentralizedValue distantIdentifier)
		{
			return getCentralDatabaseGroup(localIdentifier, encodeDecentralizedValue(distantIdentifier));
		}
		public static Group getCentralDatabaseGroup(WrappedString centralIdentifier, WrappedString distantPeerIdentifier)
		{
			return getCentralDatabaseGroup(centralIdentifier.toString(), distantPeerIdentifier.toString());
		}
		public static Group getCentralDatabaseGroup(String centralIdentifier, WrappedString distantPeerIdentifier)
		{
			return getCentralDatabaseGroup(centralIdentifier, distantPeerIdentifier.toString());
		}
		public static Group getCentralDatabaseGroup(String centralIdentifier, String distantPeerIdentifier)
		{
			if (centralIdentifier==null)
				throw new NullPointerException();
			if (distantPeerIdentifier==null)
				throw new NullPointerException();
			if (centralIdentifier.length()==0)
				throw new IllegalArgumentException();
			if (distantPeerIdentifier.length()==0)
				throw new IllegalArgumentException();
			String subgroup;
			int cmp=centralIdentifier.compareTo(distantPeerIdentifier);
			if (cmp<0)
				subgroup=centralIdentifier+"~"+distantPeerIdentifier;
			else if (cmp>0)
				subgroup=distantPeerIdentifier+"~"+centralIdentifier;
			else
				throw new IllegalArgumentException(""+cmp);

			return CLIENT_SERVER_DATABASE.getSubGroup(true, databaseGateKeeper, false, subgroup);
		}
		public static DecentralizedValue extractDistantHostID(Group group, DecentralizedValue localHostID) throws IOException {
			return extractDistantHostID(group, encodeDecentralizedValue(localHostID));
		}
		public static String extractDistantHostIDString(Group group, String localHostID)
		{
			if (Groups.DISTRIBUTED_DATABASE_WITH_SUB_GROUPS.includes(group))
			{
				String[] split=group.getName().split("~");
				if (split.length==2)
				{
					if (localHostID.equals(split[0]))
					{
						if (split[1]!=null && !split[1].equals(localHostID))
							return split[1];
					}
					else if (localHostID.equals(split[1]))
					{
						return split[0];
					}
				}
			}
			return null;

		}
		public static DecentralizedValue extractDistantHostIDFromCentralDatabaseBackupGroup(Group group, DecentralizedValue centralID) throws IOException {
			return extractDistantHostIDFromCentralDatabaseBackupGroup(group, encodeDecentralizedValue(centralID));
		}
		public static DecentralizedValue extractDistantHostIDFromCentralDatabaseBackupGroup(Group group, WrappedString centralID) throws IOException {
			return extractDistantHostIDFromCentralDatabaseBackupGroup(group, centralID.toString());
		}
		public static DecentralizedValue extractDistantHostIDFromCentralDatabaseBackupGroup(Group group, String centralID) throws IOException {
			String res=extractDistantHostIDStringFromCentralDatabaseBackupGroup(group, centralID);
			if (res==null)
				return null;
			return DecentralizedValue.decode(Bits.toBytesArrayFromBase64String(res, true));
		}
		public static String extractDistantHostIDStringFromCentralDatabaseBackupGroup(Group group, DecentralizedValue centralID)
		{
			return extractDistantHostIDStringFromCentralDatabaseBackupGroup(group, encodeDecentralizedValue(centralID));
		}
		public static String extractDistantHostIDStringFromCentralDatabaseBackupGroup(Group group, WrappedString centralID)
		{
			return extractDistantHostIDStringFromCentralDatabaseBackupGroup(group, centralID.toString());
		}
		public static String extractDistantHostIDStringFromCentralDatabaseBackupGroup(Group group, String centralID)
		{
			if (Groups.CLIENT_SERVER_DATABASE_WITH_SUB_GROUPS.includes(group))
			{
				String[] split=group.getName().split("~");
				if (split.length==2)
				{
					if (centralID.equals(split[0]))
					{
						if (split[1]!=null && !split[1].equals(centralID))
							return split[1];
					}
					else if (centralID.equals(split[1]))
					{
						return split[0];
					}
				}
			}
			return null;
		}
		public static DecentralizedValue extractDistantHostID(Group group, WrappedString localHostID) throws IOException {
			return extractDistantHostID(group, localHostID.toString());
		}
		public static DecentralizedValue extractDistantHostID(Group group, String localHostID) throws IOException {
			String res=extractDistantHostIDString(group, localHostID);
			if (res==null)
				return null;
			return DecentralizedValue.decode(Bits.toBytesArrayFromBase64String(res, true));
		}
		public static Group getDistributedDatabaseGroup(WrappedString localIdentifier, DecentralizedValue distantIdentifier)
		{
			return getDistributedDatabaseGroup(localIdentifier.toString(), distantIdentifier);
		}
		public static Group getDistributedDatabaseGroup(String localIdentifier, DecentralizedValue distantIdentifier)
		{
			return getDistributedDatabaseGroup(localIdentifier, encodeDecentralizedValue(distantIdentifier));
		}

		public static Group getDistributedDatabaseGroup(DecentralizedValue localIdentifier, DecentralizedValue distantIdentifier)
		{
			return getDistributedDatabaseGroup(encodeDecentralizedValue(localIdentifier), distantIdentifier);
		}

		/*public static Group getDistributedDatabaseGroup(DecentralizedValue identifier)
		{
			return DISTRIBUTED_DATABASE.getSubGroup(true, databaseGateKeeper, false, Base64.encodeBase64URLSafeString(identifier.encodeWithDefaultParameters()));
		}*/

	}

	/**
	 * Default roles in the Cloud community.
	 * 
	 * @since MaDKitGroupEdition 1.0
	 */
	public static final class Roles {

		// public static final String NET_AGENT="~~NET_AGENT";

		/*
		 * Role taken by distant kernel
		 */
		// public static final String DISTANT_KERNEL_ROLE="~~DISTANT_KERNEL";

		/*
		 * Role taken by socket agents
		 */
		// public static final String
		// SOCKET_AGENT_ROLE=LocalCommunity.Roles.SOCKET_AGENT_ROLE;

		/*public static final String DATABASE_EVENT_EMITTER="~~DATABASE_EVENT_EMITTER";

		public static final String DATABASE_EVENT_LISTENER="~~DATABASE_EVENT_LISTENER";*/

		/*public static String getDistributedDatabaseRole(DecentralizedValue identifier)
		{
			return Base64.encodeBase64URLSafeString(identifier.encodeWithDefaultParameters());
		}*/

		public static final String SYNCHRONIZER="~~SYNCHRONIZER";
		public static final String CENTRAL_SYNCHRONIZER="~~CENTRAL_SYNCHRONIZER";
	}
}
