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
import com.distrimind.madkit.kernel.network.KernelAddressInterfaced;
import com.distrimind.util.io.RandomByteArrayInputStream;
import com.distrimind.util.io.RandomByteArrayOutputStream;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;


/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.9
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class KernelAddressTest {

	protected static List<KernelAddress> kas;
	private static List<KernelAddress> simultaneous;

	public static KernelAddress getKernelAddressInstance()  {
		return new KernelAddress(false);
	}

	@BeforeClass
	public static void createNewAddresses()  {
		kas = new ArrayList<>();
		simultaneous = new ArrayList<>();
		for (int i = 0; i < 2000; i++) {
			try {
				Thread.sleep((long) (Math.random() * 2));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			kas.add(new KernelAddress(true));
		}
		for (int i = 0; i < 2000; i++) {
			simultaneous.add(new KernelAddress(true));
		}
	}

	/*
	 * @Test public void testHashCode() { for (KernelAddress ka : kas) { for
	 * (KernelAddress other : kas) { if (ka != other && other.hashCode() ==
	 * ka.hashCode()) { fail("two addresses with identical hashCode"); } } } for
	 * (KernelAddress ka : simultaneous) { for (KernelAddress other : simultaneous)
	 * { if (ka != other && other.hashCode() == ka.hashCode()) {
	 * fail("two addresses with identical hashCode"); } } } }
	 */

	public void createKASimultaneously() throws InterruptedException {
		List<Thread> ts = new ArrayList<>();
		for (int i = 0; i < 5; i++) {
			Thread t = new Thread(() -> {
				for (int j = 0; j < 1000; j++) {
					synchronized (kas) {
						kas.add(new KernelAddress(true));
					}
				}

			});
			ts.add(t);
			t.start();
		}
		for (Thread thread : ts) {
			thread.join();
		}
	}

	@Test
	public void testUniqueness() throws InterruptedException {
		for (int i = 0; i < 1000; i++) {
            JUnitFunctions.assertNotEquals(new KernelAddress(true), new KernelAddress(true));
		}
		for (KernelAddress ka : kas) {
			for (KernelAddress other : simultaneous) {
				if (other.hashCode() == ka.hashCode()) {
					Assert.fail("two addresses with identical hashCode");
				}
			}
		}
		createKASimultaneously();
		ArrayList<KernelAddress> all = new ArrayList<>(kas);
		for (Iterator<KernelAddress> iterator = all.iterator(); iterator.hasNext();) {
			ArrayList<KernelAddress> l = new ArrayList<>(all);
			KernelAddress ka = iterator.next();
			l.remove(ka);
			for (KernelAddress other : l) {
				if (other.equals(ka)) {
					Assert.fail("two addresses with identical hashCode");
				}
			}
			iterator.remove();
		}

	}

	// @Test
	// public void testLocalKernelAddress() {
	// KernelAddress ka = new KernelAddress();
	// System.err.println(ka);
	// KernelAddress lka = new LocalKernelAddress();
	// System.err.println(lka);
	// }

	@Test
	public void testEqualsObject() throws IOException, ClassNotFoundException {
		for (KernelAddress ka : kas) {
			for (KernelAddress other : kas) {
				if (ka != other && other.equals(ka)) {
					Assert.fail("two addresses equals");
				}
			}
		}
		for (KernelAddress ka : kas) {
			KernelAddress kas;
			try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
				baos.writeObject(ka, false);
				try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
					kas=bais.readObject(false, KernelAddress.class);

				}
			}
			AssertJUnit.assertEquals(ka, ka);
			AssertJUnit.assertEquals(ka, kas);
			KernelAddressInterfaced kai = new KernelAddressInterfaced(ka, true);
			KernelAddressInterfaced kais;
			try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
				baos.writeObject(kai, false);
				try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
					kais=bais.readObject(false, KernelAddressInterfaced.class);
				}
			}
			AssertJUnit.assertEquals(kai, kai);
			AssertJUnit.assertEquals(kai, kais);
			AssertJUnit.assertEquals(kai.getOriginalKernelAddress(), kai.getOriginalKernelAddress());
			AssertJUnit.assertEquals(kai.getOriginalKernelAddress(), kais.getOriginalKernelAddress());
			AssertJUnit.assertEquals(kais.getOriginalKernelAddress(), kai.getOriginalKernelAddress());
			AssertJUnit.assertEquals(kais, kai);
			AssertJUnit.assertEquals(kai, ka);
			AssertJUnit.assertEquals(ka, kai);
			AssertJUnit.assertEquals(kai, kas);
			AssertJUnit.assertEquals(kas, kai);
			AssertJUnit.assertEquals(kais, kas);
			AssertJUnit.assertEquals(kas, kais);
			AssertJUnit.assertEquals(kais, ka);
			AssertJUnit.assertEquals(ka, kais);
			kai = new KernelAddressInterfaced(ka, false);
			try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
				baos.writeObject(kai, false);
				try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
					kais=bais.readObject(false, KernelAddressInterfaced.class);

				}
			}
			AssertJUnit.assertEquals(kai, kai);
			AssertJUnit.assertEquals(kai.getOriginalKernelAddress(), kai.getOriginalKernelAddress());
			AssertJUnit.assertEquals(kai.getOriginalKernelAddress(), kais.getOriginalKernelAddress());
			AssertJUnit.assertEquals(kais.getOriginalKernelAddress(), kai.getOriginalKernelAddress());
			AssertJUnit.assertEquals(kai, kais);
			AssertJUnit.assertEquals(kais, kai);
			Assert.assertNotEquals(kai, ka);
			Assert.assertNotEquals(ka, kai);
			Assert.assertNotEquals(kais, ka);
			Assert.assertNotEquals(ka, kais);
			Assert.assertNotEquals(kais, kas);
			Assert.assertNotEquals(kas, kais);
			Assert.assertNotEquals(kai, kas);
			Assert.assertNotEquals(kas, kai);
		}
		for (KernelAddress ka : simultaneous) {
			for (KernelAddress other : simultaneous) {
				if (ka != other && other.equals(ka)) {
					Assert.fail("two addresses equals");
				}
			}
		}
		for (KernelAddress ka : kas) {
			for (KernelAddress other : simultaneous) {
				if (ka != other && other.equals(ka)) {
					Assert.fail("two addresses equals");
				}
			}
		}
	}

	// @Test
	// public void testToString() {
	// for (KernelAddress ka : simultaneous) {
	// System.err.println(ka);
	// }
	// }

}
