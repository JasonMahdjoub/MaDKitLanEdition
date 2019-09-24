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
package com.distrimind.madkit.message.hook;

import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.network.connection.access.CloudIdentifier;
import com.distrimind.madkit.kernel.network.connection.access.Identifier;
import com.distrimind.madkit.kernel.network.connection.access.PairOfIdentifiers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Notification message about accessible logins into the network, concerning one
 * specific kernel address.
 * 
 * 
 * @author Jason Mahdjoub
 * 
 * @version 1.0
 * @since MadKitLanEdition 1.0
 * 
 * @see AgentActionEvent#LOGGED_IDENTIFIERS_UPDATE
 */
public class NetworkLoginAccessEvent extends HookMessage {


	private final KernelAddress concernedKernelAddress;
	private final List<PairOfIdentifiers> currentIdentifiers;
	private final List<PairOfIdentifiers> newAcceptedIdentifiers;
	private final List<CloudIdentifier> newDeniedCloudIdentifiersToOther;
	private final List<Identifier> newDeniedIdentifiersFromOther;
	private final List<Identifier> newDeniedIdentifiersToOther;
	private final List<PairOfIdentifiers> newUnloggedIdentifiers;

	public NetworkLoginAccessEvent(KernelAddress _concerned_kernel_address_interfaced,
								   List<PairOfIdentifiers> allCurrentIdentifiers, List<PairOfIdentifiers> newAcceptedIdentifiers,
								   List<CloudIdentifier> newDeniedCloudIdentifiersToOther,
								   List<Identifier> newDeniedIdentifiersFromOther,
								   List<Identifier> newDeniedIdentifiersToOther, List<PairOfIdentifiers> newUnloggedIdentifiers) {
		super(AgentActionEvent.LOGGED_IDENTIFIERS_UPDATE);
		if (_concerned_kernel_address_interfaced == null)
			throw new NullPointerException("_concerned_kernel_address_interfaced");
		this.concernedKernelAddress = _concerned_kernel_address_interfaced;
		if (allCurrentIdentifiers == null)
			throw new NullPointerException("allCurrentIdentifiers");
		this.currentIdentifiers = Collections.unmodifiableList(allCurrentIdentifiers);
		if (newAcceptedIdentifiers == null)
			this.newAcceptedIdentifiers = Collections.unmodifiableList(new ArrayList<PairOfIdentifiers>());
		else
			this.newAcceptedIdentifiers = Collections.unmodifiableList(newAcceptedIdentifiers);
		if (newDeniedCloudIdentifiersToOther == null)
			this.newDeniedCloudIdentifiersToOther = Collections.unmodifiableList(new ArrayList<CloudIdentifier>());
		else
			this.newDeniedCloudIdentifiersToOther = Collections.unmodifiableList(newDeniedCloudIdentifiersToOther);
		if (newDeniedIdentifiersFromOther == null)
			this.newDeniedIdentifiersFromOther = Collections.unmodifiableList(new ArrayList<Identifier>());
		else
			this.newDeniedIdentifiersFromOther = Collections.unmodifiableList(newDeniedIdentifiersFromOther);
		if (newDeniedIdentifiersToOther == null)
			this.newDeniedIdentifiersToOther = Collections.unmodifiableList(new ArrayList<Identifier>());
		else
			this.newDeniedIdentifiersToOther = Collections.unmodifiableList(newDeniedIdentifiersToOther);
		if (newUnloggedIdentifiers == null)
			this.newUnloggedIdentifiers = Collections.unmodifiableList(new ArrayList<PairOfIdentifiers>());
		else
			this.newUnloggedIdentifiers = Collections.unmodifiableList(newUnloggedIdentifiers);
	}

	@Override
	public String toString() {
		return "NetworkLoginAccessEvent[" +
				"concernedKernelAddress=" + concernedKernelAddress +
				", currentIdentifiers=" + currentIdentifiers +
				", newAcceptedIdentifiers=" + newAcceptedIdentifiers +
				", lastDeniedCloudIdentifiersToOther=" + newDeniedCloudIdentifiersToOther +
				", lastDeniedIdentifiersFromOther=" + newDeniedIdentifiersFromOther +
				", lastDeniedIdentifiersToOther=" + newDeniedIdentifiersToOther +
				", newUnloggedIdentifiers=" + newUnloggedIdentifiers +
				']';
	}

	public List<CloudIdentifier> getNewDeniedCloudIdentifiersToOther() {
		return newDeniedCloudIdentifiersToOther;
	}

	public List<Identifier> getNewDeniedIdentifiersFromOther() {
		return newDeniedIdentifiersFromOther;
	}

	public List<Identifier> getNewDeniedIdentifiersToOther() {
		return newDeniedIdentifiersToOther;
	}

	public List<PairOfIdentifiers> getCurrentIdentifiers() {
		return currentIdentifiers;
	}

	public List<PairOfIdentifiers> getNewAcceptedIdentifiers() {
		return newAcceptedIdentifiers;
	}



	public List<PairOfIdentifiers> getNewUnloggedIdentifiers() {
		return newUnloggedIdentifiers;
	}

	public KernelAddress getConcernedKernelAddress() {
		return concernedKernelAddress;
	}
}
