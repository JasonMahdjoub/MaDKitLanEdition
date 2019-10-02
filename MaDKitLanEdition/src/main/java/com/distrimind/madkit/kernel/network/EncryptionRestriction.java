package com.distrimind.madkit.kernel.network;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.0.0
 */
public enum EncryptionRestriction {
	/**
	 * The list of accepted encryption algorithms has no restriction
	 */
	NO_RESTRICTION,
	/**
	 * Used algorithms and used protocols must resist to quantum computers.
	 */
	POST_QUANTUM_ALGORITHMS,
	/**
	 * Used algorithms and used protocols must be a combination of both classical algorithms and algorithms that resist to quantum computers.
	 */
	HYBRID_ALGORITHMS
}
