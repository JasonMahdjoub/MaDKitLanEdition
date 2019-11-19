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

import com.distrimind.madkit.kernel.network.NetworkProperties;
import com.distrimind.util.io.Integrity;
import com.distrimind.util.io.MessageExternalizationException;
import com.distrimind.util.io.SecuredObjectInputStream;
import com.distrimind.util.io.SecuredObjectOutputStream;

import java.io.IOException;
import java.util.*;


/**
 * 
 * @author Jason Mahdjoub
 * @version 1.1
 * @since MadkitLanEdition 1.0
 */
class LoginConfirmationMessage extends AccessMessage {

	public ArrayList<Identifier> accepted_identifiers;
	private transient short nbAnomalies;
	private boolean checkDifferedMessages;


	@SuppressWarnings("unused")
	LoginConfirmationMessage()
	{
		
	}
	
	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		int size=in.readInt();
		int totalSize=4;
		int globalSize=NetworkProperties.GLOBAL_MAX_SHORT_DATA_SIZE;
		if (size<0 || totalSize+size*4>globalSize)
			throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
		accepted_identifiers=new ArrayList<>(size);
		for (int i=0;i<size;i++)
		{
			Identifier id=in.readObject(false, Identifier.class);
			totalSize+=id.getInternalSerializedSize();
			if (totalSize>globalSize)
				throw new MessageExternalizationException(Integrity.FAIL_AND_CANDIDATE_TO_BAN);
			accepted_identifiers.add(id);
		}
		checkDifferedMessages=in.readBoolean();
	}


	@Override
	public void writeExternal(SecuredObjectOutputStream oos) throws IOException {
		oos.writeInt(accepted_identifiers.size()); 
		for (Identifier id : accepted_identifiers)
			oos.writeObject(id, false);

		oos.writeBoolean(checkDifferedMessages);
		
	}
	
	public LoginConfirmationMessage(Collection<Identifier> _distant_accepted_identifiers,
			short nbAnomalies,
			boolean checkDifferedMessages) {
		if (_distant_accepted_identifiers == null)
			throw new NullPointerException("_accepted_identifiers");
		accepted_identifiers = new ArrayList<>();
		for (Identifier did : _distant_accepted_identifiers) {
			if (did==null)
				continue;
			accepted_identifiers.add(did);
		}
		this.nbAnomalies = nbAnomalies;
		this.checkDifferedMessages = checkDifferedMessages;
	}

	@Override
	public short getNbAnomalies() {
		return nbAnomalies;
	}

	

	@Override
	public boolean checkDifferedMessages() {
		return checkDifferedMessages;
	}

	private PairOfIdentifiers getAcceptedPairOfIdentifiers(Collection<PairOfIdentifiers> alreadyValidatedPairOfIdentifiers,
														   Set<PairOfIdentifiers> removedValidatedPairOfIdentifiers,
														   LoginConfirmationMessage localLoginConfirmationMessage,
														   Identifier[] proposedLocalIdentifiers,
														   Identifier identifier,
														   Collection<CloudIdentifier> initializedIdentifiers,
														   Collection<CloudIdentifier> newAcceptedCloudIdentifiers)
	{
		Identifier foundLocalId=null;
		for (Identifier id : proposedLocalIdentifiers)
		{
			if (id.equals(identifier)) {
				foundLocalId = id;
				break;
			}
		}
		if (foundLocalId!=null)
		{
			PairOfIdentifiers proposed=null;


			for (Identifier id : localLoginConfirmationMessage.accepted_identifiers)
			{

				if (id.getCloudIdentifier().equals(foundLocalId.getCloudIdentifier()))
				{
					if (!id.getHostIdentifier().equals(foundLocalId.getHostIdentifier()) || HostIdentifier.getNullHostIdentifierSingleton().equals(id.getHostIdentifier())) {
						proposed = new PairOfIdentifiers(foundLocalId, initializedIdentifiers.contains(id.getCloudIdentifier()), id, newAcceptedCloudIdentifiers.contains(id.getCloudIdentifier()));

					}
				}
			}

			for (PairOfIdentifiers poi : alreadyValidatedPairOfIdentifiers) {
				if (poi.getCloudIdentifier().equals(foundLocalId.getCloudIdentifier())) {
					removedValidatedPairOfIdentifiers.add(poi);
					if (poi.getDistantHostIdentifier().equals(foundLocalId.getHostIdentifier()))
						break;
					else if (proposed == null || proposed.isDistantHostPartOfCloud())
						proposed = new PairOfIdentifiers(foundLocalId, initializedIdentifiers.contains(foundLocalId.getCloudIdentifier()), poi.generateDistantIdentifier(), newAcceptedCloudIdentifiers.contains(poi.getCloudIdentifier()));
				} else if (proposed != null && poi.getCloudIdentifier().equals(proposed.getCloudIdentifier())) {
					removedValidatedPairOfIdentifiers.add(poi);
					break;
				}
			}

			return proposed;
		}
		else {

			return null;
		}
	}

	public ArrayList<PairOfIdentifiers> getAcceptedPairsOfIdentifiers(Collection<PairOfIdentifiers> alreadyValidatedPairOfIdentifiers,
																	  Set<PairOfIdentifiers> removedValidatedPairOfIdentifiers,
																	  LoginConfirmationMessage localLoginConfirmationMessage,
																	  Identifier[] proposedLocalIdentifiers,
																	  Collection<CloudIdentifier> initializedIdentifiers,
																	  Collection<CloudIdentifier> newAcceptedCloudIdentifiers)
	{


		ArrayList<PairOfIdentifiers> res=new ArrayList<>();
		//HashSet<Identifier> usedDistantIdentifiers=new HashSet<>();
		for (Identifier id : accepted_identifiers)
		{
			PairOfIdentifiers poi=getAcceptedPairOfIdentifiers(alreadyValidatedPairOfIdentifiers, removedValidatedPairOfIdentifiers, localLoginConfirmationMessage,proposedLocalIdentifiers, id, initializedIdentifiers, newAcceptedCloudIdentifiers);
			if (poi!=null) {
				res.add(poi);
				//usedDistantIdentifiers.add(poi.getDistantIdentifier());
			}
		}
		/*for (Identifier distantID : localLoginConfirmationMessage.accepted_identifiers)
		{
			if (!usedDistantIdentifiers.contains(distantID)) {
				boolean add=true;
				for (PairOfIdentifiers poi : alreadyValidatedPairOfIdentifiers) {
					if (poi.getDistantIdentifier().getCloudIdentifier().equals(distantID.getCloudIdentifier())) {
						if (!distantID.equals(poi.getDistantIdentifier())) {
							removedValidatedPairOfIdentifiers.add(poi);
						} else
							add = false;
						break;
					}
				}
				if (add)
					res.add(new PairOfIdentifiers(null, distantID));
			}
		}*/
		/*for (Identifier distantID : accepted_identifiers)
		{
			if (distantID.getHostIdentifier().equals(HostIdentifier.getNullHostIdentifierSingleton()))
			{

			}
		}*/
		return res;

	}

}
