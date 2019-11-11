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
import com.distrimind.util.io.RandomByteArrayOutputStream;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.1.0
 */
public class GroupsRoles {
	public static int MAX_ROLES_NUMBER=Short.MAX_VALUE;
	private MultiGroup group;
	private String[] distantAcceptedRoles;
	private transient volatile RoleID roleID;

	/*GroupsRoles(AbstractGroup group, String... distantAcceptedRoles) {
		this(group, null, distantAcceptedRoles);
	}*/
	GroupsRoles(AbstractGroup group, RoleID roleID, String... distantAcceptedRoles) {
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
	}

	public MultiGroup getGroup() {
		return group;
	}

	public String[] getDistantAcceptedRoles() {
		return distantAcceptedRoles;
	}

	public RoleID getDistantAcceptedRolesID() throws IOException {
		if (roleID==null) {
			roleID=computeRoleID(this.distantAcceptedRoles);
		}
		return roleID;
	}

	static RoleID computeRoleID(String...roles) throws IOException {
		try (RandomByteArrayOutputStream out = new RandomByteArrayOutputStream()) {
			if (roles != null) {
				out.writeShort((short) roles.length);
				for (String s : roles) {
					out.writeString(s, false, Group.MAX_ROLE_NAME_LENGTH);
				}
			} else
				out.writeShort(0);
			return new RoleID(out.getBytes());
		}
	}

	public boolean isConcernedByDistantSenderAgentAddress(KernelAddress localKernelAddress, AgentAddress aa)
	{
		for (Group g : group.getRepresentedGroups(localKernelAddress))
		{
			if (aa.getGroup().getThisGroupWithItsSubGroups().includes(g)) {
				if (distantAcceptedRoles == null)
					return true;
				for (String r : distantAcceptedRoles) {
					if (r.equals(aa.getRole()))
						return true;
				}
				return false;
			}
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
