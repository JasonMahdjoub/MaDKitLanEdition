package com.distrimind.madkit.kernel.network.connection.access;

import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;

import java.io.IOException;

public class AccessMessagesList extends AccessMessage {
	private AccessMessage[] messages;
	
	AccessMessagesList(AccessMessage ...messages)
	{
		this.messages=messages;
	}
	public AccessMessage[] getMessages()
	{
		return messages;
	}
	

	@Override
	public boolean checkDifferedMessages() {
		return false;
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		throw new MessageSerializationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
	}
	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		throw new IOException();
	}
}
