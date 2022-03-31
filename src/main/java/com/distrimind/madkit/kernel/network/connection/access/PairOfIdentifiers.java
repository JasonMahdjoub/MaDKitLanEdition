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
package com.distrimind.madkit.kernel.network.connection.access;

import java.util.Objects;

/**
 * Represents a pair of identifiers, each equivalent between one peer and one
 * other. Each local and distant identifiers has the same cloud identifier, but
 * not the same host identifier
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
public class PairOfIdentifiers {
	private final CloudIdentifier cloudIdentifier;
	private final HostIdentifier localHostIdentifier, distantHostIdentifier;
	private final int hashCode;
	private volatile Identifier localIdentifier=null;
	private volatile Identifier distantIdentifier=null;
	private final boolean locallyAuthenticatedCloud, distantlyAuthenticatedCloud;

	PairOfIdentifiers(Identifier _localIdentifier, boolean locallyAuthenticatedCloud, Identifier _distant_identifier, boolean distantlyAuthenticatedCloud) {
		if (_localIdentifier == null) {
			throw new NullPointerException();
		}
		if (_distant_identifier == null) {
			throw new NullPointerException();
		}
		if (_localIdentifier.getHostIdentifier().equalsTimeConstant(HostIdentifier.getNullHostIdentifierSingleton()))
		{
			if (_localIdentifier.equalsHostIdentifier(_distant_identifier) && !locallyAuthenticatedCloud && !distantlyAuthenticatedCloud)
				throw new IllegalArgumentException();
		}
		else if (_localIdentifier.equalsHostIdentifier(_distant_identifier))
			throw new IllegalArgumentException(
				"_localIdentifier and _distant_identifier cannot have the same host identifiers : "+_localIdentifier.getHostIdentifier());
		if (!_localIdentifier.equalsCloudIdentifier(_distant_identifier)) {
			throw new IllegalArgumentException(
					"_localIdentifier and _distant_identifier must have the same cloud identifier");
		}
		if (!locallyAuthenticatedCloud && !distantlyAuthenticatedCloud)
			throw new IllegalArgumentException("Cloud authentication can't be not done locally and distantly");
		cloudIdentifier=_localIdentifier.getCloudIdentifier();
		localHostIdentifier=_localIdentifier.getHostIdentifier();
		distantHostIdentifier=_distant_identifier.getHostIdentifier();
		this.locallyAuthenticatedCloud=locallyAuthenticatedCloud;
		this.distantlyAuthenticatedCloud=distantlyAuthenticatedCloud;
		this.hashCode=cloudIdentifier.hashCode()^localHostIdentifier.hashCode()^distantHostIdentifier.hashCode();
	}

	@Override
	public int hashCode()
	{
		return hashCode;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null || getClass() != o.getClass()) return false;
		if (this == o) return true;
		if (this.hashCode==o.hashCode()) {
			PairOfIdentifiers that = (PairOfIdentifiers) o;
			boolean res=Objects.equals(cloudIdentifier, that.cloudIdentifier);
			res=Objects.equals(localHostIdentifier, that.localHostIdentifier) && res;
			res=Objects.equals(distantHostIdentifier, that.distantHostIdentifier) && res;
			return res;
		}
		else
			return false;
	}

	@Override
	public String toString() {
		return "PairOfIdentifiers["+cloudIdentifier+", "+localHostIdentifier+", "+distantHostIdentifier+"]";
	}

	public CloudIdentifier getCloudIdentifier()
	{
		return cloudIdentifier;
	}

	public boolean equalsLocalIdentifier(Identifier identifier)
	{
		return cloudIdentifier.equalsTimeConstant(identifier.getCloudIdentifier()) && localHostIdentifier.equalsTimeConstant(identifier.getHostIdentifier());
	}

	public boolean equalsDistantIdentifier(Identifier identifier)
	{
		return cloudIdentifier.equalsTimeConstant(identifier.getCloudIdentifier()) && distantHostIdentifier.equalsTimeConstant(identifier.getHostIdentifier());
	}

	/**
	 * 
	 * @return the identifier of the local peer
	 */
	public Identifier getLocalIdentifier() {
		if (localIdentifier==null)
			localIdentifier=new Identifier(cloudIdentifier, localHostIdentifier);
		return localIdentifier;
	}

	/**
	 * 
	 * @return the identifier of the distant peer
	 */
	public Identifier getDistantIdentifier() {
		if (distantIdentifier==null)
			distantIdentifier=new Identifier(cloudIdentifier, distantHostIdentifier);
		return distantIdentifier;
	}

	public HostIdentifier getLocalHostIdentifier() {
		return localHostIdentifier;
	}

	public HostIdentifier getDistantHostIdentifier() {
		return distantHostIdentifier;
	}

	public boolean isDistantHostPartOfCloud()
	{
		return HostIdentifier.getNullHostIdentifierSingleton()!=distantHostIdentifier;
	}
	public boolean isLocalHostPartOfCloud()
	{
		return HostIdentifier.getNullHostIdentifierSingleton()!=localHostIdentifier;
	}

	public boolean isLocallyAuthenticatedCloud() {
		return locallyAuthenticatedCloud;
	}

	public boolean isDistantlyAuthenticatedCloud() {
		return distantlyAuthenticatedCloud;
	}
}
