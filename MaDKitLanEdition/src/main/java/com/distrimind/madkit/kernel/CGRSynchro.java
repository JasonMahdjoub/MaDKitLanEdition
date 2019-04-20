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
package com.distrimind.madkit.kernel;

import com.distrimind.madkit.kernel.network.MessageLocker;
import com.distrimind.madkit.message.ObjectMessage;
import com.distrimind.madkit.util.SecureExternalizable;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;

import java.io.IOException;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @version 0.91
 * @since MaDKitLanEdition 1.0
 *
 */
public class CGRSynchro extends ObjectMessage<AgentAddress> implements SecureExternalizable {

	public enum Code {
		CREATE_GROUP, REQUEST_ROLE, LEAVE_ROLE, LEAVE_GROUP
		// LEAVE_ORG
	}

	private Code code;
	private boolean manual;
	private transient MessageLocker messageLocker=null;

	@SuppressWarnings("unused")
	private CGRSynchro()
	{
		super(null, false);
	}
	
	@Override
	public int getInternalSerializedSize() {
		return super.getInternalSerializedSizeImpl(0)+code.name().length()*2+3;
	}	
	
	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		messageLocker=null;
		super.readExternal(in, 0);
		code=in.readObject(false, Code.class);
		manual=in.readBoolean();
		
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException{
		super.writeExternal(oos, 0);
		oos.writeObject(code, false);
		oos.writeBoolean(manual);
	}
	
	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public CGRSynchro clone() {
		return new CGRSynchro(this);
	}
	private CGRSynchro(CGRSynchro cgrSynchro) {
		super(cgrSynchro);
		if (cgrSynchro == null)
			throw new NullPointerException("cgrSynchro");

		this.code = cgrSynchro.getCode();
		manual = cgrSynchro.manual;
		this.messageLocker=cgrSynchro.messageLocker;
	}

	CGRSynchro(final Code code, final AgentAddress aa, boolean manual_operation) {
		super(aa);
		if (code == null)
			throw new NullPointerException("code");
		if (aa == null)
			throw new NullPointerException("aa");

		this.code = code;
		manual = manual_operation;
		this.messageLocker=null;
	}

	@Override
	public String toString() {
		return "CGRSynchro[code=" + code + ", manual=" + manual + ", agentAddress=" + getContent() + "]";
	}

	/**
	 * @return the code
	 */
	public Code getCode() {
		return code;
	}

	public boolean isManualOperation() {
		return manual;
	}
	void initMessageLocker()
	{
		if (messageLocker==null)
			messageLocker=new MessageLocker(this);

	}

	public MessageLocker getMessageLocker()
	{
		return messageLocker;
	}

}

class RequestRoleSecure extends ObjectMessage<SecureExternalizable> implements SecureExternalizable{

	private Class<? extends AbstractAgent> requesterClass;
	private AgentAddress requester;
	private String roleName;

	@Override
	public int getInternalSerializedSize() {
		return super.getInternalSerializedSizeImpl()+requesterClass.getName().length()*2+2+requester.getInternalSerializedSize()+roleName.length()*2+2;
	}	
	
	@Override
	public void readExternal(final SecuredObjectInputStream in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);

		requesterClass=in.readClass(false, AbstractAgent.class);

		requester=in.readObject(false, AgentAddress.class);
		roleName=in.readString(false, Group.MAX_ROLE_NAME_LENGTH);
		
	}
	@Override
	public void writeExternal(final SecuredObjectOutputStream oos) throws IOException{
		super.writeExternal(oos);
		oos.writeClass(requesterClass, false, AbstractAgent.class);
		oos.writeObject(requester, false);
		oos.writeString(roleName, false, Group.MAX_ROLE_NAME_LENGTH);
	}
	
	
	public RequestRoleSecure(Class<? extends AbstractAgent> requesterClass, AgentAddress requester, String roleName,
			SecureExternalizable key) {
		super(key);
		this.requesterClass = requesterClass;
		this.requester = requester;
		this.roleName = roleName;
	}

	/**
	 * @return the requester
	 */
	AgentAddress getRequester() {
		return requester;
	}

	/**
	 * @return the roleName
	 */
	public String getRoleName() {
		return roleName;
	}

	Class<? extends AbstractAgent> getRequesterClass() {
		return requesterClass;
	}
}