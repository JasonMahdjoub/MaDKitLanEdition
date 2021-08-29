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
package com.distrimind.madkit.api.abstractAgent;

import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.JunitMadkit;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 */
public class AutoRequestGroupTest extends JunitMadkit {
	@Test
	public void testAutoRequest() {
		launchTest(new AbstractAgent() {
			@Override
			public void activate() {
				AssertJUnit.assertFalse(hasGroup(GROUP));
				autoRequestRole(GROUP, ROLE, null);
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AbstractAgent aa = new AbstractAgent();
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, launchAgent(aa));

				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.requestRole(GROUP, ROLE));
				AssertJUnit.assertFalse(aa.isConcernedByAutoRequestRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				System.out.println("ici1");
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.leaveRole(GROUP, ROLE));

				AssertJUnit.assertFalse(aa.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));

				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.requestRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.leaveRole(GROUP, ROLE));
				AssertJUnit.assertFalse(aa.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));

				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.requestRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.leaveGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));

				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.requestRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				leaveAutoRequestedGroup(GROUP);
				AssertJUnit.assertFalse(isConcernedByAutoRequestRole(GROUP, ROLE));
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertEquals(ReturnCode.SUCCESS, aa.leaveGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasRole(GROUP, ROLE));

				autoRequestRole(GROUP, ROLE, null);
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));
				aa.requestRole(GROUP, ROLE);
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));

				leaveGroup(GROUP);
				AssertJUnit.assertFalse(isConcernedByAutoRequestRole(GROUP, ROLE));
				aa.leaveGroup(GROUP);
				AssertJUnit.assertFalse(aa.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));
				aa.requestRole(GROUP, ROLE);
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));

				autoRequestRole(GROUP, ROLE, null);
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				AbstractAgent aa2 = new AbstractAgent();
				launchAgent(aa2);
				aa2.requestRole(GROUP, ROLE);
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa2.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa2.hasRole(GROUP, ROLE));
				autoRequestRole(GROUP, ROLE, null);
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa2.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa2.hasRole(GROUP, ROLE));

				aa.leaveGroup(GROUP);
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				AssertJUnit.assertFalse(aa.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa2.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa2.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));
				aa2.leaveGroup(GROUP);
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));
				AssertJUnit.assertFalse(aa.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa.hasRole(GROUP, ROLE));
				AssertJUnit.assertFalse(aa2.hasGroup(GROUP));
				AssertJUnit.assertFalse(aa2.hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(isConcernedByAutoRequestRole(GROUP, ROLE));
				aa.requestRole(GROUP, ROLE);
				AssertJUnit.assertTrue(hasGroup(GROUP));
				AssertJUnit.assertTrue(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
				leaveAllAutoRequestedGroups();
				AssertJUnit.assertFalse(hasGroup(GROUP));
				AssertJUnit.assertFalse(hasRole(GROUP, ROLE));
				AssertJUnit.assertTrue(aa.hasGroup(GROUP));
				AssertJUnit.assertTrue(aa.hasRole(GROUP, ROLE));
			}
		});
	}
}
