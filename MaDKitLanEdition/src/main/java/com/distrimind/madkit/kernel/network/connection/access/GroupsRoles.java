package com.distrimind.madkit.kernel.network.connection.access;
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

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.1.0
 */
public class GroupsRoles implements Cloneable, SecureExternalizable {
	public static int MAX_ROLES_NUMBER=Short.MAX_VALUE;
	private transient MultiGroup group;
	private String[] distantAcceptedRoles;
	private transient RoleID roleID;
	private Group[] representedGroups;



	@Override
	public void writeExternal(SecuredObjectOutputStream out) throws IOException {
		int maxDataSize= NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int totalLength=4;
		if (representedGroups==null)
			throw new IOException();

		if (distantAcceptedRoles==null)
			out.writeInt(0);
		else {
			if (distantAcceptedRoles.length>MAX_ROLES_NUMBER)
				throw new IOException();
			totalLength+=8*distantAcceptedRoles.length;
			if (totalLength>maxDataSize)
				throw new IOException();
			out.writeInt(distantAcceptedRoles.length);
			for (String s : distantAcceptedRoles) {
				if (s.length()==0)
					throw new IOException();
				totalLength+=SerializationTools.getInternalSize(s, Group.MAX_ROLE_NAME_LENGTH);
				if (totalLength>maxDataSize)
					throw new IOException();
				out.writeString(s, false, Group.MAX_ROLE_NAME_LENGTH);
			}
		}
		totalLength+=8*this.representedGroups.length;
		if (totalLength>maxDataSize)
			throw new IOException();
		out.writeInt(this.representedGroups.length);

		for (Group g : representedGroups) {
			if (g.isUsedSubGroups())
				throw new IOException();
			if (!g.isDistributed())
				throw new IOException();
			out.writeObject(g, false);
			if (totalLength>maxDataSize)
				throw new IOException();
			totalLength+=SerializationTools.getInternalSize(g);
		}
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		int maxDataSize= NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int totalLength=4;
		this.roleID=null;

		int s=in.readInt();
		if (s<0 || s>MAX_ROLES_NUMBER)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		totalLength+=8*s;
		if (totalLength>maxDataSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		this.distantAcceptedRoles=new String[s];
		for (int i=0;i<s;i++)
		{
			String str=in.readString(false, Group.MAX_ROLE_NAME_LENGTH);
			if (str.length()==0)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			this.distantAcceptedRoles[i]=str;
			totalLength+=SerializationTools.getInternalSize(str, Group.MAX_ROLE_NAME_LENGTH);
			if (totalLength>maxDataSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}
		s=in.readInt();
		if (s<0)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		totalLength+=8*s;
		if (totalLength>maxDataSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		this.representedGroups=new Group[s];
		for (int i=0;i<s;i++)
		{
			Group g=in.readObject(false, Group.class);
			if (g.isUsedSubGroups())
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			if (!g.isDistributed())
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			this.representedGroups[i]=g;
			totalLength+=SerializationTools.getInternalSize(g);
			if (totalLength>maxDataSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		}
		this.group=new MultiGroup(representedGroups);
	}

	@Override
	public int getInternalSerializedSize() {


		int res=8+representedGroups.length*8;
		if (distantAcceptedRoles!=null) {
			res += distantAcceptedRoles.length * 8;

			for (String s : distantAcceptedRoles)
				res += SerializationTools.getInternalSize(s, Group.MAX_ROLE_NAME_LENGTH);
		}
		for (Group g : representedGroups)
		{
			res+=SerializationTools.getInternalSize(g);
		}
		return res;

	}

	@SuppressWarnings({"MethodDoesntCallSuperMethod"})
	@Override
	public GroupsRoles clone() {
		if (representedGroups==null)
			return new GroupsRoles(group.clone(), roleID, distantAcceptedRoles.clone());
		else {
			Group[] gs=new Group[representedGroups.length];
			for (int i=0;i<gs.length;i++)
				gs[i]=representedGroups[i].clone();
			return new GroupsRoles(gs, roleID, distantAcceptedRoles.clone());
		}
	}

	/*GroupsRoles(AbstractGroup group, String... distantAcceptedRoles) {
			this(group, null, distantAcceptedRoles);
		}*/
	GroupsRoles(AbstractGroup group, RoleID roleID, String... distantAcceptedRoles) {
		if (roleID==null)
			throw new NullPointerException();
		if (group==null)
			throw new NullPointerException();
		this.group = new MultiGroup(group);
		if (distantAcceptedRoles!=null) {
			if (distantAcceptedRoles.length>MAX_ROLES_NUMBER)
				throw new IllegalArgumentException();
			if (distantAcceptedRoles.length==0)
				this.distantAcceptedRoles =null;
			else
				this.distantAcceptedRoles = distantAcceptedRoles;
		}
		else
		{
			this.distantAcceptedRoles =null;
		}
		this.roleID=roleID;
		this.representedGroups=null;
	}
	GroupsRoles(Group[] representedGroups, RoleID roleID, String... distantAcceptedRoles) {
		if (roleID==null)
			throw new NullPointerException();
		if (representedGroups==null)
			throw new NullPointerException();
		this.group = new MultiGroup(representedGroups);
		if (distantAcceptedRoles!=null) {
			if (distantAcceptedRoles.length>MAX_ROLES_NUMBER)
				throw new IllegalArgumentException();
			if (distantAcceptedRoles.length==0)
				this.distantAcceptedRoles =null;
			else
				this.distantAcceptedRoles = distantAcceptedRoles;
		}
		else
		{
			this.distantAcceptedRoles =null;
		}
		this.roleID=roleID;
		this.representedGroups=representedGroups;

	}

	public Group[] getRepresentedGroups() {
		return representedGroups;
	}

	public MultiGroup getGroup() {
		return group;
	}

	public String[] getDistantAcceptedRoles() {
		return distantAcceptedRoles;
	}

	public RoleID getDistantAcceptedRolesID() {
		return roleID;
	}

	static RoleID computeRoleID(String...roles)  {

		try (RandomByteArrayOutputStream out = new RandomByteArrayOutputStream()) {
			if (roles != null) {
				Arrays.sort(roles);
				out.writeShort((short) roles.length);
				for (String s : roles) {
					out.writeString(s, false, Group.MAX_ROLE_NAME_LENGTH);
				}
			} else
				out.writeShort(0);
			return new RoleID(out.getBytes());
		}
		catch(IOException e)
		{
			e.printStackTrace();
			throw new IllegalAccessError();
		}
	}

	public boolean isConcernedByDistantAgentAddress(AgentAddress aa)
	{
		if (group.includes(aa.getGroup())) {
			if (distantAcceptedRoles == null)
				return true;
			for (String r : distantAcceptedRoles) {
				if (r.equals(aa.getRole()))
					return true;
			}
		}
		return false;

	}
	public boolean isConcernedByLocalAgentAddress(AgentAddress aa)
	{
		return group.includes(aa.getGroup());

	}

	public boolean isDistantRoleAcceptable(String roleName) {
		if (distantAcceptedRoles==null)
			return true;
		for (String s : distantAcceptedRoles)
		{
			if (s.equals(roleName))
				return true;
		}
		return false;
	}


	public static class RoleID
	{
		private byte[] id;
		private int hashCode;

		private RoleID(byte[] id) {
			if (id==null)
				throw new IllegalArgumentException();
			this.id = id;
			this.hashCode= Arrays.hashCode(this.id);
		}

		public byte[] getId() {
			return id;
		}

		@Override
		public int hashCode() {
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj==null)
				return false;
			if (obj==this)
				return true;
			if (obj.getClass()==this.getClass())
			{
				RoleID o=(RoleID)obj;
				if (o.hashCode!=this.hashCode)
					return false;
				return Arrays.equals(o.id, this.id);
			}
			return false;
		}
	}
}
