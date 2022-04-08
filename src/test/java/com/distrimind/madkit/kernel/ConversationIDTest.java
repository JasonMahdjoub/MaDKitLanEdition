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

import com.distrimind.madkit.JUnitFunctions;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.distrimind.madkit.kernel.ConversationID.InterfacedIDs;
import com.distrimind.util.io.RandomByteArrayInputStream;
import com.distrimind.util.io.RandomByteArrayOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 *
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
public class ConversationIDTest extends TestNGMadkit {
	static final Random rand = new Random(System.currentTimeMillis());

	@Test
	public void multipleTestConversationID() {
		for (int i = 0; i < 20; i++) {
			System.out.println("Test " + i);
			testConversationID();
		}
	}

	@SuppressWarnings("UnusedAssignment")
	@Test
	public void testConversationID() {

		/*launchTest(new AbstractAgent() {
			@SuppressWarnings("UnusedAssignment")
			@Override
			protected void activate() {*/
				try {
					final KernelAddress ka1 = new KernelAddress(false);//getKernelAddress();
					final KernelAddress ka2 = new KernelAddress(false);
					Assert.assertNotEquals(ka1, ka2);
					final Map<KernelAddress, InterfacedIDs> globaInterfacedIDs1 = new HashMap<>();//getMadkitKernel().getGlobalInterfacedIDs();
					final Map<KernelAddress, InterfacedIDs> globaInterfacedIDs2 = new HashMap<>();
					int nb = rand.nextInt(100);
					ArrayList<ConversationID> ids = new ArrayList<>();
					for (int i = 0; i < nb; i++) {
						ConversationID id = ConversationID.getConversationIDInstance();

						AssertJUnit.assertEquals(id, id);
						for (ConversationID other : ids)
							JUnitFunctions.assertNotEquals(id, other);
						try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
							baos.writeObject(id,false);
							try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
								ConversationID deserializedID = bais.readObject(false, ConversationID.class);
								AssertJUnit.assertEquals(id, deserializedID);
								for (ConversationID other : ids)
									JUnitFunctions.assertNotEquals(deserializedID, other);
							}
						}
						ids.add(id);
					}
					boolean minka1=false;
					boolean minka2=false;
					for (int i = 0; i < nb; i++) {
						ConversationID id = ConversationID.getConversationIDInstance();
						// id.setOrigin(ka1);
						if (!minka1)
						{
							minka1=true;
							id.setOrigin(ka1);
						}
						else if (!minka2)
						{
							minka2=true;
							id.setOrigin(ka2);
						}
						else
						{

							if (rand.nextInt(2) == 0) {
								id.setOrigin(ka1);
							} else {
								id.setOrigin(ka2);
							}
						}

						AssertJUnit.assertEquals(id, id);
						for (ConversationID other : ids)
							JUnitFunctions.assertNotEquals(id, other);
						try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
							baos.writeObject(id,false);
							try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
								ConversationID deserializedID = bais.readObject(false, ConversationID.class);
								AssertJUnit.assertEquals(id, deserializedID);
								for (ConversationID other : ids)
									JUnitFunctions.assertNotEquals(deserializedID, other);
							}
						}
						ids.add(id);
					}

					ArrayList<ConversationID> notInterfacedIdsToOther = new ArrayList<>();
					ArrayList<ConversationID> interfacedToOtherIds = new ArrayList<>();
					ArrayList<ConversationID> interfacedToOtherIdsSerialized = new ArrayList<>();

					for (ConversationID id : ids) {
						if (id.getOrigin() == null)
							continue;
						if (id.getOrigin().equals(ka2))
							continue;
						ConversationID interfacedToOther = id
								.getInterfacedConversationIDToDistantPeer(globaInterfacedIDs1, ka1, ka2);

						AssertJUnit.assertNotNull(interfacedToOther.getOrigin());
						AssertJUnit.assertEquals(interfacedToOther, interfacedToOther);
						for (ConversationID other : interfacedToOtherIds) {
							JUnitFunctions.assertNotEquals(interfacedToOther, other);
						}
						try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
							baos.writeObject(interfacedToOther,false);
							try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
								ConversationID deserializedID = bais.readObject(false, ConversationID.class);
								AssertJUnit.assertEquals(interfacedToOther, deserializedID);
								for (ConversationID other : interfacedToOtherIds)
									JUnitFunctions.assertNotEquals(deserializedID, other);
								interfacedToOtherIdsSerialized.add(deserializedID);
							}
						}
						interfacedToOtherIds.add(interfacedToOther);
						notInterfacedIdsToOther.add(id);
					}
					ArrayList<ConversationID> interfacedByOtherIds = new ArrayList<>();
					ArrayList<ConversationID> interfacedByOtherIdsSerialized = new ArrayList<>();
					for (ConversationID id : interfacedToOtherIdsSerialized) {
						ConversationID interfacedByOther = id
								.getInterfacedConversationIDFromDistantPeer(globaInterfacedIDs2, ka2, ka1);

						AssertJUnit.assertNotNull("ka1?" + id.getOrigin().equals(ka1), interfacedByOther.getOrigin());
						AssertJUnit.assertEquals(interfacedByOther, interfacedByOther);
						for (ConversationID other : interfacedByOtherIds) {
							JUnitFunctions.assertNotEquals(interfacedByOther, other);
						}
						try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
							baos.writeObject(interfacedByOther,false);
							try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
								ConversationID deserializedID = bais.readObject(false, ConversationID.class);
								AssertJUnit.assertEquals(interfacedByOther, deserializedID);
								for (ConversationID other : interfacedByOtherIds)
									JUnitFunctions.assertNotEquals(deserializedID, other);
								interfacedByOtherIdsSerialized.add(deserializedID);
							}
						}
						interfacedByOtherIds.add(interfacedByOther);
					}
					ArrayList<ConversationID> interfacedFromOtherIds = new ArrayList<>();
					ArrayList<ConversationID> interfacedFromOtherIdsSerialized = new ArrayList<>();
					for (ConversationID id : interfacedByOtherIdsSerialized) {
						ConversationID interfacedFromOther = id
								.getInterfacedConversationIDToDistantPeer(globaInterfacedIDs2, ka2, ka1);
						AssertJUnit.assertNotNull(interfacedFromOther.getOrigin());
						AssertJUnit.assertEquals(interfacedFromOther, interfacedFromOther);
						for (ConversationID other : interfacedFromOtherIds) {
							JUnitFunctions.assertNotEquals(interfacedFromOther, other);
						}
						try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
							baos.writeObject(interfacedFromOther,false);
							try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
								ConversationID deserializedID = bais.readObject(false, ConversationID.class);
								AssertJUnit.assertEquals(interfacedFromOther, deserializedID);
								for (ConversationID other : interfacedFromOtherIds)
									JUnitFunctions.assertNotEquals(deserializedID, other);
								interfacedFromOtherIdsSerialized.add(deserializedID);
							}
						}
						interfacedFromOtherIds.add(interfacedFromOther);
					}
					ArrayList<ConversationID> interfacedByCurrentIds = new ArrayList<>();
					ArrayList<ConversationID> interfacedByCurrentIdsSerialized = new ArrayList<>();
					int i=0;
					for (ConversationID id : interfacedFromOtherIdsSerialized) {
						ConversationID interfacedByCurrent = id
								.getInterfacedConversationIDFromDistantPeer(globaInterfacedIDs1, ka1, ka2);

						Assert.assertEquals(interfacedByCurrent, notInterfacedIdsToOther.get(i++));

						AssertJUnit.assertNotNull(interfacedByCurrent.getOrigin());
						AssertJUnit.assertEquals(interfacedByCurrent, interfacedByCurrent);
						for (ConversationID other : interfacedByCurrentIds) {
							JUnitFunctions.assertNotEquals(interfacedByCurrent, other);
						}
						try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
							baos.writeObject(interfacedByCurrent,false);
							try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
								ConversationID deserializedID = bais.readObject(false, ConversationID.class);
								AssertJUnit.assertEquals(interfacedByCurrent, deserializedID);
								for (ConversationID other : interfacedByCurrentIds)
									JUnitFunctions.assertNotEquals(deserializedID, other);
								interfacedByCurrentIdsSerialized.add(deserializedID);
							}
						}
						interfacedByCurrentIds.add(interfacedByCurrent);
					}

					ArrayList<BigDataTransferID> bigDataTransferID = new ArrayList<>();

					for (ConversationID idOriginal : ids) {
                        if (idOriginal.getOrigin() == null)
                            continue;
                        if (idOriginal.getOrigin().equals(ka2))
                            continue;

                        int nbFound = 0;

                        for (ConversationID id : interfacedByCurrentIdsSerialized) {
                            if (id.equals(idOriginal))
                                nbFound++;
                        }
                        AssertJUnit.assertEquals(1, nbFound);
                        BigDataTransferID bdtid = new BigDataTransferID(idOriginal, null);

                        bigDataTransferID.add(bdtid);
                        AssertJUnit.assertEquals(idOriginal, bdtid);
                        AssertJUnit.assertEquals(bdtid, idOriginal);

                    }
					int sum=0;
					for (ConversationID cid : notInterfacedIdsToOther)
						sum+=cid.hashCode();
					System.out.println("Sum to avoid GC cleaning : "+sum);
					interfacedToOtherIds=null;
					notInterfacedIdsToOther=null;
                    System.gc();
					Thread.sleep(500);
					System.gc();

					AssertJUnit.assertFalse("size=" + globaInterfacedIDs1.size(), globaInterfacedIDs1.isEmpty());
					sum=0;
					for (ConversationID cid : interfacedByCurrentIds)
						sum+=cid.hashCode();
					for (ConversationID cid : bigDataTransferID)
					{
						sum+=cid.hashCode();
					}
					System.out.println("Sum to avoid GC cleaning 2 : "+sum);
					AssertJUnit.assertTrue("size=" + globaInterfacedIDs2.size(), globaInterfacedIDs2.isEmpty());
					i=0;
					for (ConversationID idOriginal : ids) {
						if (idOriginal.getOrigin() == null)
							continue;
						if (idOriginal.getOrigin().equals(ka2))
							continue;

						int nbFound = 0;
						AssertJUnit.assertEquals(interfacedByCurrentIdsSerialized.get(i++), idOriginal);

						for (ConversationID id : interfacedByCurrentIdsSerialized) {
							if (id.equals(idOriginal))
								nbFound++;
						}
						AssertJUnit.assertEquals(1, nbFound);
					}
					AssertJUnit.assertFalse(interfacedByCurrentIds.isEmpty());
					AssertJUnit.assertFalse(bigDataTransferID.isEmpty());

					ids = null;
					interfacedByCurrentIds = null;
					interfacedByOtherIds = null;
					interfacedFromOtherIds = null;
					interfacedToOtherIds = null;
					interfacedByCurrentIdsSerialized = null;
					interfacedByOtherIdsSerialized = null;
					interfacedFromOtherIdsSerialized = null;
					interfacedToOtherIdsSerialized = null;
					notInterfacedIdsToOther=null;



					Thread.sleep(200);
					System.gc();
					System.gc();
					Thread.sleep(200);
					System.gc();
					Thread.sleep(200);
					System.gc();
					AssertJUnit.assertTrue("size=" + globaInterfacedIDs2.size(), globaInterfacedIDs2.isEmpty());

                    bigDataTransferID = null;
					Thread.sleep(200);
					System.gc();
					System.gc();

					int cycles = 0;
					while ((!globaInterfacedIDs1.isEmpty() || !globaInterfacedIDs2.isEmpty()) && cycles++ < 30) {
						Thread.sleep(200);
						System.gc();
					}

					AssertJUnit.assertTrue("size=" + globaInterfacedIDs1.size(), globaInterfacedIDs1.isEmpty());
					AssertJUnit.assertTrue("size=" + globaInterfacedIDs2.size(), globaInterfacedIDs2.isEmpty());

				} catch (Exception e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
			//}

		//});

	}
}
