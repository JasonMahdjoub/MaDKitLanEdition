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

import com.distrimind.util.io.RandomByteArrayInputStream;
import com.distrimind.util.io.RandomByteArrayOutputStream;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;


/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class MultiGroupTest {
	@Test
	public void testMultiGroup() throws ClassNotFoundException, IOException {
		MultiGroup mg = new MultiGroup(new Group("MGC1", "G1", "G2"), new Group("MGC1", "G1", "G3"),
				new Group("MGC1", "G1", "G3", "G4"));
		Assert.assertTrue(mg.includes(new Group("MGC1", "G1", "G2")));
		Assert.assertFalse(mg.includes(new Group("MGC1", "G1", "G2", "G3")));
		Assert.assertFalse(mg.includes(new Group("MGC2", "G1", "G2")));
		Assert.assertTrue(mg.addGroup(new Group("MGC1", "G1", "G2", "G3")));
		Assert.assertTrue(mg.includes(new Group("MGC1", "G1", "G2", "G3")));
		mg.addGroup(new Group(true, "MGC1", "G1", "G3"));
		Assert.assertTrue(mg.includes(new Group("MGC1", "G1", "G3", "G5")));
		mg.addForbidenGroup(new Group("MGC1", "G1", "G3", "G5"));
		Assert.assertFalse(mg.includes(new Group("MGC1", "G1", "G3", "G5")));
		Assert.assertTrue(mg.includes(new Group("MGC1", "G1", "G3", "G6")));
		Assert.assertTrue(mg.getComplementary().includes(new Group("MGC1", "G1", "G3", "G5")));
		Assert.assertTrue(AbstractGroup.getUniverse().includes(mg.union(mg.getComplementary())));
		Assert.assertTrue(mg.union(mg.getComplementary()).includes(AbstractGroup.getUniverse()));
		Assert.assertEquals(mg.union(mg.getComplementary()), AbstractGroup.getUniverse());
		Assert.assertTrue(mg.union(new Group("MGC1", "G1", "G3", "G5")).includes(new Group("MGC1", "G1", "G3", "G5")));
		Assert.assertTrue(mg.intersect(new Group(true, "MGC1", "G1", "G3")).includes(new Group("MGC1", "G1", "G3", "G10")));
		Assert.assertTrue(mg.intersect(new Group(true, "MGC1", "G1", "G3")).includes(new Group("MGC1", "G1", "G3")));
		Assert.assertTrue(mg.includes(new Group("MGC1", "G1", "G2")));
		Assert.assertFalse(mg.intersect(new Group(true, "MGC1", "G1", "G3")).includes(new Group("MGC1", "G1", "G2")));
		// Assert.assertTrue(mg.intersect(mg.getComplementary()).isEmpty());//TODO
		new MultiGroup();
		for(AbstractGroup[] ag : getGroups())
		{
			testCloneGroup(ag);
		}
		for(AbstractGroup[] ag : getGroups())
		{
			testGroupSerialization(ag);
		}
	}

	public void testCloneGroup(AbstractGroup... groups) {
		MultiGroup g = new MultiGroup(groups);
		MultiGroup g2 = g.clone();
		Assert.assertEquals(g, g2);
	}

	public void testGroupSerialization(AbstractGroup... groups) throws IOException, ClassNotFoundException {
		MultiGroup g = new MultiGroup(groups);

		byte[] array;

		try (RandomByteArrayOutputStream baos = new RandomByteArrayOutputStream()) {
			baos.writeObject(g, false);
			array = baos.getBytes();
		}

		MultiGroup g2;
		try (RandomByteArrayInputStream bais = new RandomByteArrayInputStream(array)) {
			g2 = bais.readObject(false, MultiGroup.class);
		}

		Assert.assertEquals(g, g2);
	}

	AbstractGroup[][] getGroups() {
		return new AbstractGroup[][] {
				{ new Group("MGC1", "G1", "G2"), new Group("MGC1", "G1", "G3"), new Group("MGC1", "G1", "G3", "G4"), },
				{ new Group("MGC1", "G1", "G2"), new Group("MGC1", "G1", "G3"), new Group("MGC2", "G1", "G3", "G4"), },
				{ new Group("MGC1", "G1", "G2"), new MultiGroup(new Group("MGC1", "G1", "G3"), new Group("MGC1", "G5", "G3")),
						new Group("MGC2", "G1", "G3", "G4"), } };
	}

}
