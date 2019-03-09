package com.distrimind.madkit.kernel.network.connection.secured;

import com.distrimind.madkit.kernel.network.connection.AskConnection;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 1.10
 */
public class AskConnectionWithProtocolID extends AskConnection {

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
}
