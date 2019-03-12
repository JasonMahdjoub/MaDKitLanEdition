package com.distrimind.madkit.kernel.network.connection.secured;

import com.distrimind.madkit.exceptions.MessageSerializationException;
import com.distrimind.madkit.kernel.network.connection.AskConnection;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.10
 */
class AskConnectionWithProtocolID extends AskConnection {

	private int protocolID;
	public AskConnectionWithProtocolID() {
	}

	public AskConnectionWithProtocolID(boolean _you_are_asking, int protocolID) {
		super(_you_are_asking);
		this.protocolID = protocolID;
	}

	public int getProtocolID() {
		return protocolID;
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.protocolID=in.readInt();
	}

	@Override
	public void writeExternal(ObjectOutput oos) throws IOException {
		super.writeExternal(oos);
		oos.writeInt(protocolID);

	}
}
