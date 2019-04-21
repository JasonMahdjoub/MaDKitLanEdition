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

import com.distrimind.madkit.util.SecureExternalizable;
import com.distrimind.madkit.util.SecuredObjectInputStream;
import com.distrimind.madkit.util.SecuredObjectOutputStream;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 
 * This class represents the conversation ID to which a message belongs.
 * 
 * When a message is created, it is given an ID that will be used to tag all the
 * messages that will be created for answering this message using
 * {@link AbstractAgent#sendReply(Message, Message)} like methods. Especially,
 * if the answer is again used for replying, the ID will be used again to tag
 * this new answer, and so on.
 * 
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @version 2.2
 * @since MadKitLanEdition 1.0
 */
public class ConversationID implements SecureExternalizable, Cloneable {

	final static private AtomicInteger ID_COUNTER = new AtomicInteger(
			(int) (Math.random() * (double) Integer.MAX_VALUE));// TODO if many many ??

	private int id;
	private KernelAddress origin;

	ConversationID() {
		id=-1;
		//id = ID_COUNTER.getAndIncrement();
		origin = null;
	}
	static ConversationID getConversationIDInstance()
	{
		return new ConversationID(ID_COUNTER.getAndIncrement(), null);
	}

	protected int getID() {
		return id;
	}

	ConversationID(int id, KernelAddress origin) {
		this.id = id;
		this.origin = origin;
	}

	ConversationID(ConversationID conversationID) {
		this.id = conversationID.id;
		this.origin = conversationID.origin;
		if (conversationID.global_interfaced_ids!=null)
		{
			this.global_interfaced_ids=conversationID.global_interfaced_ids;
			if (conversationID.myInterfacedIDs!=null)
			{
				this.myInterfacedIDs=Collections.synchronizedMap(new HashMap<KernelAddress, OriginalID>());
				//synchronized (global_interfaced_ids) {
				try {
					for (Map.Entry<KernelAddress, OriginalID> kpi : conversationID.myInterfacedIDs.entrySet()) {
						myInterfacedIDs.put(kpi.getKey(), kpi.getValue());
						kpi.getValue().incrementPointerToThisOriginalID();
						/*InterfacedIDs i2 = global_interfaced_ids.get(kpi.getKey());
						i2.getOriginalID(kpi.getValue().originalID).incrementPointerToThisOriginalID();*/
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
				//}

			}
			else
				this.myInterfacedIDs=null;
		}
		else
			this.global_interfaced_ids=null;
	}


	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public ConversationID clone() {
		return this;
	}

	@Override
	public String toString() {
		return id + (origin == null ? "" : origin.toString());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if ((obj instanceof ConversationID)) {// obj necessarily comes from the network or is different,
																// so origin should have been set priorly if there is a
																// chance of equality
			final ConversationID ci = (ConversationID) obj;// no check is intentional

			return this.getID() == ci.getID()
					&& ((getOrigin() == ci.getOrigin()) || (getOrigin() != null && getOrigin().equals(ci.getOrigin())));
		}

		return false;
	}

	void setOrigin(KernelAddress origin) {
		if (this.origin == null) {
			this.origin = origin;
		}
	}

	@Override
	public int hashCode() {
		return id;
	}

	public KernelAddress getOrigin() {
		return origin;
	}

	

	private static class OriginalID {
		final int originalID;
		private final AtomicInteger nbPointers;

		OriginalID(int originalID) {
			this(originalID, new AtomicInteger(0));
		}

		OriginalID(int originalID, AtomicInteger nbPointers) {
			this.originalID = originalID;
			this.nbPointers = nbPointers;
		}

		public void incrementPointerToThisOriginalID() {
			nbPointers.incrementAndGet();
		}

		public int getOriginalID() {
			// nbPointers.incrementAndGet();
			return originalID;
		}

		public AtomicInteger getNbPointers() {
			return nbPointers;
		}

		public boolean remove() {
			int val = nbPointers.decrementAndGet();
			if (val < 0)
				new IllegalAccessError().printStackTrace();
			return val <= 0;
		}
	}

	static class InterfacedIDs {
		private int id_counter;
		private final HashMap<Integer, OriginalID> original_ids = new HashMap<>();
		private final HashMap<Integer, OriginalID> distant_ids = new HashMap<>();

		InterfacedIDs() {
			id_counter = (int) (Math.random() * (double) Integer.MAX_VALUE);
		}

		private int getAndIncrementIDCounter() {
			if (++id_counter == -1)
				return ++id_counter;
			else
				return id_counter;
		}

		OriginalID getNewID(Integer original) {
			OriginalID res = distant_ids.get(original);
			if (res == null) {
				res = new OriginalID(getAndIncrementIDCounter());
				original_ids.put(res.originalID, new OriginalID(original, res.getNbPointers()));
				distant_ids.put(original, res);
			}
			res.incrementPointerToThisOriginalID();
			return res;
		}

		void removeID(Integer original) {
			OriginalID di = distant_ids.get(original);
			if (di.remove())
				original_ids.remove(distant_ids.remove(original).originalID);
		}

		OriginalID getNewIDFromDistantID(Integer distantID) {

			OriginalID res = original_ids.get(distantID);
			if (res == null) {
				res = new OriginalID(getAndIncrementIDCounter());
				distant_ids.put(res.originalID, new OriginalID(distantID, res.getNbPointers()));
				original_ids.put(distantID, res);
			}
			res.incrementPointerToThisOriginalID();
			return res;
		}

		/*void addNewIds(OriginalID localID, OriginalID distantID)
		{
			original_ids.put(distantID.originalID, localID);
			distant_ids.put(localID.originalID, distantID);
			localID.incrementPointerToThisOriginalID();
		}*/

		OriginalID getOriginalID(int distant_id) {
			return original_ids.get(distant_id);
		}
		OriginalID getDistantOriginalID(int local_id) {
			return distant_ids.get(local_id);
		}


		/*void removeDistantID(Integer distantid) {
			OriginalID oi = original_ids.get(distantid);
			if (oi.remove())
				distant_ids.remove(original_ids.remove(distantid).originalID);
		}*/

		boolean isEmpty() {
			return original_ids.isEmpty();
		}

		/*int getOriginalIDsNumber() {
			return original_ids.size();
		}*/
	}

	private transient volatile Map<KernelAddress, InterfacedIDs> global_interfaced_ids = null;
	protected transient Map<KernelAddress, OriginalID> myInterfacedIDs = null;

	Map<KernelAddress, InterfacedIDs> getGlobalInterfacedIDs() {
		return this.global_interfaced_ids;
	}

	@SuppressWarnings({"SynchronizeOnNonFinalField", "deprecation"})
	@Override
	protected void finalize() {
		if (global_interfaced_ids != null && myInterfacedIDs != null) {

			synchronized (global_interfaced_ids) {
				try {
					for (Map.Entry<KernelAddress, OriginalID> kpi : myInterfacedIDs.entrySet()) {
						InterfacedIDs i2 = global_interfaced_ids.get(kpi.getKey());
						i2.removeID(id);
						if (i2.isEmpty()) {
							global_interfaced_ids.remove(kpi.getKey());
						}
					}
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
			global_interfaced_ids = null;
			myInterfacedIDs = null;

		}
	}

	ConversationID getInterfacedConversationIDToDistantPeer(Map<KernelAddress, InterfacedIDs> global_interfaced_ids,
			KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {

		if (origin.equals(distantKernelAddress))
			return this;
		else if (origin.equals(currentKernelAddress)) {
			OriginalID distantid = null;
			if (myInterfacedIDs != null) {
				distantid = myInterfacedIDs.get(distantKernelAddress);
			}
			if (distantid == null) {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (global_interfaced_ids) {
					if (myInterfacedIDs == null)
						myInterfacedIDs = Collections.synchronizedMap(new HashMap<KernelAddress, OriginalID>());
					else
						distantid = myInterfacedIDs.get(distantKernelAddress);
					if (distantid==null) {
						this.global_interfaced_ids = global_interfaced_ids;
						InterfacedIDs i = global_interfaced_ids.get(distantKernelAddress);
						if (i == null) {
							i = new InterfacedIDs();
							global_interfaced_ids.put(distantKernelAddress, i);
						}
						distantid = i.getNewID(this.id);
					}
				}
				myInterfacedIDs.put(distantKernelAddress, distantid);
			}
			/*else
			{
				distantid.incrementPointerToThisOriginalID();
			}*/
			/*
			 * else { myInterfacedIDs.put(distantKernelAddress, distantid); }
			 */

			return new ConversationID(distantid.getOriginalID(), origin);
			/*
			 * ConversationID cid=new ConversationID(distantid.getOriginalID(), origin);
			 * cid.myInterfacedIDs=new HashMap<KernelAddress, ConversationID.OriginalID>();
			 * cid.myInterfacedIDs.put(distantKernelAddress, distantid);
			 * distantid.incrementPointerToThisOriginalID(); return cid;
			 */
		} else {
			return new ConversationID(0, null);

		}
	}

	@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
	ConversationID getInterfacedConversationIDFromDistantPeer(Map<KernelAddress, InterfacedIDs> global_interfaced_ids,
															  KernelAddress currentKernelAddress, KernelAddress distantKernelAddress) {
		if (origin == null) {
			return getConversationIDInstance();
		} else if (origin.equals(distantKernelAddress)) {
			return this;
		} else if (origin.equals(currentKernelAddress)) {

			synchronized (global_interfaced_ids) {
				/*this.global_interfaced_ids = global_interfaced_ids;
				assert myInterfacedIDs==null;
				myInterfacedIDs = Collections.synchronizedMap(new HashMap<KernelAddress, OriginalID>());*/
				InterfacedIDs i = global_interfaced_ids.get(distantKernelAddress);
				if (i == null) {

					i = new InterfacedIDs();
					global_interfaced_ids.put(distantKernelAddress, i);

				}
				/*InterfacedIDs i2=global_interfaced_ids.get(currentKernelAddress);
				if (i2==null)
				{
					i2 = new InterfacedIDs();
					global_interfaced_ids.put(currentKernelAddress, i2);
				}*/

				OriginalID o = i.getOriginalID(id);
				if (o == null) {
					o = i.getNewIDFromDistantID(this.id);
				}
				else
					o.incrementPointerToThisOriginalID();
				OriginalID distantOriginalID=i.getDistantOriginalID(o.getOriginalID());
				assert distantOriginalID!=null;
				assert distantOriginalID.originalID==this.id;
				/*i2.addNewIds(distantOriginalID, o);

				myInterfacedIDs.put(currentKernelAddress, o);*/

				// return new ConversationID(o.originalID, origin);
				ConversationID cid = new ConversationID(o.getOriginalID(), origin);

				cid.global_interfaced_ids = global_interfaced_ids;
				cid.myInterfacedIDs = Collections
						.synchronizedMap(new HashMap<KernelAddress, ConversationID.OriginalID>());
				cid.myInterfacedIDs.put(distantKernelAddress, distantOriginalID/*i.getNewID(o.getOriginalID())*/);
				/*
				 * if (myInterfacedIDs==null) myInterfacedIDs=new HashMap<>();
				 * myInterfacedIDs.put(distantKernelAddress, i.getNewID(new
				 * Integer(o.getOriginalID())));
				 */
				return cid;
			}
		} else {
			return getConversationIDInstance();
		}
	}

	@Override
	public int getInternalSerializedSize() {
		
		return 4+this.origin.getInternalSerializedSize();
	}

	@Override
	public void writeExternal(SecuredObjectOutputStream out) throws IOException {
		out.writeInt(this.id);
		out.writeObject(this.origin, true);
	}

	@Override
	public void readExternal(SecuredObjectInputStream in) throws IOException, ClassNotFoundException {
		global_interfaced_ids=null;
		myInterfacedIDs=null;
		this.id=in.readInt();
		this.origin=in.readObject(true, KernelAddress.class);
	}
}
