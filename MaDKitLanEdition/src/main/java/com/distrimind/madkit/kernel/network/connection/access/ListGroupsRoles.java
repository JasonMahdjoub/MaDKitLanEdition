package com.distrimind.madkit.kernel.network.connection.access;
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

import com.distrimind.madkit.kernel.*;
import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.util.io.*;

import java.io.IOException;
import java.util.*;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.1.0
 */
public class ListGroupsRoles implements Cloneable, SecureExternalizable {
	private final Map<GroupsRoles.RoleID, GroupsRoles> groupsRoles=new HashMap<>();
	private transient volatile MultiGroup multiGroup=null;

	public MultiGroup getGroups() {
		if (multiGroup==null) {
			MultiGroup mg = new MultiGroup();
			for (GroupsRoles gr : groupsRoles.values())
				mg.addGroup(gr.getGroup());
			multiGroup=mg;
		}
		return multiGroup;
	}

	public boolean includes(Group group, String role)
	{
		return groupsRoles.values().stream().anyMatch(gr -> gr.isConcernedByGroupRole(group, role));
	}

	public boolean includes(Group group)
	{
		return groupsRoles.values().stream().anyMatch(gr -> gr.getGroup().includes(group));
	}

	public Group[] getRepresentedGroups(KernelAddress localKernelAddress)
	{
		return getGroups().getRepresentedGroups(localKernelAddress);
	}

	@Override
	public int getInternalSerializedSize() {
		int res=4;
		for (GroupsRoles gr : groupsRoles.values())
		{
			res+=gr.getInternalSerializedSize();
		}
		return res;
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream out) throws IOException {
		int maxDataSize= NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int dataSize=4+groupsRoles.size()*8;
		if (dataSize>maxDataSize)
			throw new IOException();
		out.writeInt(groupsRoles.size());
		for (GroupsRoles gr : groupsRoles.values())
		{
			out.writeObject(gr, false);
			dataSize+= SerializationTools.getInternalSize(gr);
			if (dataSize>maxDataSize)
				throw new IOException();
		}
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		groupsRoles.clear();
		multiGroup=null;
		int maxDataSize= NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		int s=in.readInt();
		if (s<0)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		int dataSize=4+s*8;
		if (dataSize>maxDataSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		for (int i=0;i<s;i++)
		{
			GroupsRoles gr=in.readObject(false, GroupsRoles.class);
			dataSize+=gr.getInternalSerializedSize();
			if (dataSize>maxDataSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			groupsRoles.put(gr.getDistantAcceptedRolesID(), gr);
		}
	}

	@SuppressWarnings({"MethodDoesntCallSuperMethod"})
	@Override
	public ListGroupsRoles clone() {
		ListGroupsRoles res=new ListGroupsRoles();
		for (Map.Entry<GroupsRoles.RoleID, GroupsRoles> e : groupsRoles.entrySet())
		{
			res.groupsRoles.put(e.getKey(), e.getValue().clone());
		}
		return res;
	}

	/**
	 * Add distant shared groups, with distant accepted roles
	 * @param group the distant shared group between the two peers
	 * @param distantAcceptedRoles the accepted roles from distant peer
	 *
	 */
	public void addGroupsRoles(AbstractGroup group, String...distantAcceptedRoles) {
		GroupsRoles.RoleID roleID=GroupsRoles.computeRoleID(distantAcceptedRoles);
		addGroupsRoles(group, roleID, distantAcceptedRoles);
	}
	private void addGroupsRoles(AbstractGroup group, GroupsRoles.RoleID roleID, String...distantAcceptedRoles) {

		GroupsRoles gr=groupsRoles.get(roleID);
		if (gr==null)
		{
			groupsRoles.put(roleID, new GroupsRoles(group, roleID, distantAcceptedRoles));
		}
		else
			gr.getGroup().addGroup(group);
		multiGroup=null;

	}

	public ListGroupsRoles getListWithRepresentedGroupsRoles(KernelAddress localKernelAddress)
	{
		ListGroupsRoles res=new ListGroupsRoles();
		for (Map.Entry<GroupsRoles.RoleID, GroupsRoles> e : groupsRoles.entrySet())
		{
			Group[] groups=e.getValue().getGroup().getRepresentedGroups(localKernelAddress);
			if (groups!=null && groups.length>0) {
				Group[] groups2=new Group[groups.length];
				int distributedRepresentedNumber = 0;
				for (Group g : groups)
				{
					if (g.isDistributed()) {
						groups2[distributedRepresentedNumber++]=g;
					}
				}
				if (distributedRepresentedNumber>0) {
					if (distributedRepresentedNumber!=groups.length)
						groups2= Arrays.copyOfRange(groups2, 0, distributedRepresentedNumber);
					res.groupsRoles.put(e.getKey(), new GroupsRoles(groups2, e.getKey(), e.getValue().getDistantAcceptedRoles()));
				}
			}
		}
		return res;
	}

	public void addListGroupsRoles(ListGroupsRoles l) {
		if (l!=null) {
			for (Map.Entry<GroupsRoles.RoleID, GroupsRoles> e : l.groupsRoles.entrySet()) {
				addGroupsRoles(e.getValue().getGroup(), e.getKey(), e.getValue().getDistantAcceptedRoles());
			}
		}
	}

	public MultiGroup intersect(KernelAddress localKernelAddress, KernelAddress distantKernelAddress, AbstractGroup group, Collection<AgentAddress> agentsAddressesSender)
	{
		Set<Group> groups=new HashSet<>();
		Set<AgentAddress> newAgentsAddressesSender=new HashSet<>();
		for (GroupsRoles gr : groupsRoles.values())
		{
			ArrayList<Group> iGroups=null;

			for (AgentAddress aa : agentsAddressesSender)
			{
				if (!aa.getGroup().isDistributed())
					continue;
				if (!aa.isFrom(distantKernelAddress))
					continue;

				if (gr.isConcernedByDistantAgentAddress(aa)) {
					if (iGroups==null)
						iGroups=gr.getGroup().intersect(localKernelAddress, group);
					Group groupWithSubGroups=aa.getGroup().getThisGroupWithItsSubGroups();
					for (Group g : iGroups)
					{
						if (!g.isDistributed())
							continue;
						if (groups.contains(g))
							continue;
						if (!gr.getGroup().includes(g))
							continue;
						if (g.equals(aa.getGroup()) || groupWithSubGroups.includes(g)) {
							groups.add(g);
							newAgentsAddressesSender.add(aa);
						}
					}
				}
			}
		}
		if (newAgentsAddressesSender.size()>agentsAddressesSender.size())
			throw new InternalError();
		else if (agentsAddressesSender.size()!=newAgentsAddressesSender.size()) {
			agentsAddressesSender.clear();
			agentsAddressesSender.addAll(newAgentsAddressesSender);
		}
		return new MultiGroup(groups);
	}

	public ListGroupsRoles intersect(KernelAddress localKernelAddress, ListGroupsRoles other)
	{
		ListGroupsRoles res=new ListGroupsRoles();
		for (Map.Entry<GroupsRoles.RoleID, GroupsRoles> e : groupsRoles.entrySet())
		{
			GroupsRoles ogr=other.groupsRoles.get(e.getKey());
			if (ogr!=null)
			{
				Group[] groups1=e.getValue().getGroup().getRepresentedGroups(localKernelAddress);
				Group[] groups2=ogr.getGroup().getRepresentedGroups(localKernelAddress);
				ArrayList<Group> groupsRes=null;
				for (Group g1 : groups1) {
					for (Group g2 : groups2) {
						if (g2.equals(g1)) {
							if (groupsRes==null)
								groupsRes=new ArrayList<>();
							groupsRes.add(g1);
							break;
						}
					}
				}
				if (groupsRes!=null) {
					Group[] gs=new Group[groupsRes.size()];
					groupsRes.toArray(gs);
					res.groupsRoles.put(e.getKey(), new GroupsRoles(gs, e.getKey(), e.getValue().getDistantAcceptedRoles()));
				}
			}
		}
		return res;
	}

	public boolean includesDistant(KernelAddress kernelAddress, AgentAddress agentAddress)
	{
		return includes(kernelAddress, agentAddress, true);
	}

	public boolean includesLocal(KernelAddress kernelAddress, AgentAddress agentAddress)
	{
		return includes(kernelAddress, agentAddress, false);
	}

	private boolean includes(KernelAddress kernelAddress, AgentAddress agentAddress, boolean distant)
	{
		if (!agentAddress.isFrom(kernelAddress))
			return false;
		if (!agentAddress.getGroup().isDistributed())
			return false;
		for (GroupsRoles gr : groupsRoles.values())
		{
			if (distant) {
				if (gr.isConcernedByDistantAgentAddress(agentAddress)) {
					return true;
				}
			}
			else
			{
				if (gr.isConcernedByLocalAgentAddress(agentAddress)) {
					return true;
				}
			}
		}
		return false;
	}

	public enum InclusionMode
	{
		NONE,
		PARTIAL,
		TOTAL
	}

	public InclusionMode includesGroup(Group group)
	{
		if (!group.isDistributed())
			return InclusionMode.NONE;
		InclusionMode res=InclusionMode.NONE;
		for (GroupsRoles gr : groupsRoles.values())
		{
			if (gr.getGroup().includes(group))
			{
				if (gr.getDistantAcceptedRoles()==null)
					return InclusionMode.TOTAL;
				else
					res=InclusionMode.PARTIAL;
			}
		}
		return res;
	}

	public boolean areDetectedChanges(ListGroupsRoles roles, KernelAddress localKernelAddress, boolean kernelAddressSent)
	{
		if (!kernelAddressSent)
			return true;
		if (roles==null)
			return true;

		for (Map.Entry<GroupsRoles.RoleID, GroupsRoles> e : groupsRoles.entrySet())
		{
			GroupsRoles gr=roles.groupsRoles.get(e.getKey());
			Group[] rp = e.getValue().getGroup().getRepresentedGroups(localKernelAddress);

			if (gr==null) {
				if (rp==null || rp.length==0)
					continue;
				return true;
			}
			Group[] represented_groups=gr.getGroup().getRepresentedGroups();
			if (represented_groups == null) {
				if (rp==null || rp.length==0)
					continue;
				return true;
			} else {
				if (represented_groups.length != rp.length)
					return true;
				else {
					for (Group g1 : rp) {
						boolean found = false;
						for (Group g2 : represented_groups) {
							if (g2.equals(g1)) {
								found = true;
								break;
							}
						}
						if (!found) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}


	public boolean isEmpty() {
		return groupsRoles.size()==0;
	}

	public boolean isDistantRoleAcceptable(AgentAddress agentAddress) {
		return isDistantRoleAcceptable(agentAddress.getGroup(), agentAddress.getRole());
	}
	public boolean isDistantRoleAcceptable(Group group, String roleName) {
		for (GroupsRoles gr : groupsRoles.values())
		{
			if (gr.getGroup().includes(group))
			{
				String []roles=gr.getDistantAcceptedRoles();
				if (roles==null)
					return true;
				for (String s : roles)
				{
					if (s.equals(roleName))
						return true;
				}
			}
		}

		return false;
	}

	@Override
	public String toString()
	{
		StringBuilder res=new StringBuilder("ListGroupsRoles[");
		for (GroupsRoles gr : groupsRoles.values())
		{
			res.append(gr.toString());
		}
		res.append("]");
		return res.toString();

	}
}
