package com.distrimind.madkit.kernel.network.connection.secured;

import com.distrimind.madkit.kernel.network.connection.AskConnection;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;

/**
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MaDKitLanEdition 1.10
 */
class AskConnectionWithProtocolID extends AskConnection {

	private int protocolID;
	@SuppressWarnings("unused")
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
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		this.protocolID=in.readInt();
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		super.writeExternal(oos);
		oos.writeInt(protocolID);

	}
}
