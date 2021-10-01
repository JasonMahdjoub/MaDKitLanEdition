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
package com.distrimind.madkit.kernel.network;

import com.distrimind.madkit.JUnitFunctions;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.distrimind.madkit.exceptions.OverflowException;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.JunitMadkit;
import com.distrimind.madkit.kernel.network.TransferAgent.IDTransfer;
import com.distrimind.util.io.RandomByteArrayInputStream;
import com.distrimind.util.io.RandomByteArrayOutputStream;
import java.io.IOException;

/**
 * @author Jason Mahdjoub
 * 
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class IDTransferTest extends JunitMadkit {
	@Test
	public void testIDTransfer() {
		launchTest(new AbstractAgent() {
			@SuppressWarnings("UnusedAssignment")
            @Override
			protected void activate() throws InterruptedException {
				try {
					int idNumber = 100;
					IDTransfer[] ids = new IDTransfer[idNumber];

					for (int i = 0; i < idNumber; i++) {
						ids[i] = IDTransfer.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(this));
					}
					AssertJUnit.assertEquals(100, MadkitKernelAccess.numberOfValidGeneratedID(this));
					for (int i = 0; i < idNumber; i++) {
						for (int j = 0; j < idNumber; j++) {
							if (i == j) {
                                AssertJUnit.assertEquals(ids[i], ids[j]);
							} else
								JUnitFunctions.assertNotEquals(ids[i], ids[j]);
						}
						try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
							AssertJUnit.assertTrue(ids[i].isGenerated());
							baos.writeObject(ids[i], false);
							try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(baos.getBytes())) {
								IDTransfer id = bais.readObject(false, IDTransfer.class);
								AssertJUnit.assertFalse(id.isGenerated());
								AssertJUnit.assertEquals(id, ids[i]);
							}
						}
					}
					ids=null;
                    System.gc();
					System.gc();
					Thread.sleep(500);
					AssertJUnit.assertEquals(0, MadkitKernelAccess.numberOfValidGeneratedID(this));
					IDTransfer id = IDTransfer.generateIDTransfer(MadkitKernelAccess.getIDTransferGenerator(this));
					//noinspection ResultOfMethodCallIgnored
					id.getID();
					// Assert.assertEquals(0, id.getID());
					AssertJUnit.assertEquals(1, MadkitKernelAccess.numberOfValidGeneratedID(this));

				} catch (OverflowException | IOException | ClassNotFoundException e) {
					e.printStackTrace();
					AssertJUnit.fail();
				}
			}
		});
	}
}
