package com.distrimind.madkit.kernel;

import java.util.Deque;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 4.8.0
 */
public class TestChainedBlockingQueue extends TestDeque {
	@Override
	public Deque<String> getDequeInstance() {
		return new ChainedBlockingDeque<>();
	}
}
