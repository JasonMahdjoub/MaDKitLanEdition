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
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.annotations.BeforeMethod;
import org.testng.AssertJUnit;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.9
 * @since MadkitLanEdition 1.0
 * @version 1.0
 * 
 */
public class AgentAddressTest {

	InternalRole r;
	AbstractAgent a;
	MadkitKernel mk;
	Madkit m;
	AgentAddress aa;

	@BeforeMethod
	public void before() {
		mk = (m=new Madkit("--desktop", "false", "--forceDesktop", "true")).getKernel();
		a = new AbstractAgent();
		a.setKernel(mk);
		mk.createGroup(new Group("c", "g"), null);
		try {
			mk.getGroup(new Group("c", "g")).put("r", mk.getGroup(new Group("c", "g")).createRole("r"));
			r = mk.getRole(new Group("c", "g"), "r");

		} catch (CGRNotAvailable e) {
			e.printStackTrace();
		}
		AssertJUnit.assertTrue(r.addMember(a, false));
		aa = r.getAgentAddressOf(a);
		AssertJUnit.assertNotNull(aa);
	}

	@AfterMethod
	public void after()
	{
		MadkitTest.closeMadkit(m);
	}


	@Test
	public void testAgentAddress() {
		AssertJUnit.assertTrue(a.checkAgentAddress(aa));
		AssertJUnit.assertTrue(aa.isFrom(a.getKernelAddress()));
	}

	/**
	 * Test method for {@link com.distrimind.madkit.kernel.AgentAddress#getAgent()}.
	 */
	@Test
	public void testGetAgent() {
		AssertJUnit.assertEquals(a, aa.getAgent());
	}

	/**
	 * Test method for
	 * {@link com.distrimind.madkit.kernel.AgentAddress#setRoleObject(com.distrimind.madkit.kernel.InternalRole)}.
	 */
	@Test
	public void testCGRNames() {
		AssertJUnit.assertEquals("c", aa.getCommunity());
		AssertJUnit.assertEquals("g", aa.getGroup().getName());
		AssertJUnit.assertEquals("r", aa.getRole());
		AssertJUnit.assertEquals(AbstractAgent.ReturnCode.SUCCESS, r.removeMember(a, false));
		AssertJUnit.assertFalse(a.checkAgentAddress(aa));
		AssertJUnit.assertTrue(aa.isFrom(a.getKernelAddress()));
		AssertJUnit.assertEquals("c", aa.getCommunity());
		AssertJUnit.assertEquals("g", aa.getGroup().getName());
		AssertJUnit.assertEquals("r", aa.getRole());
	}

	/**
	 * Test method for
	 * {@link com.distrimind.madkit.kernel.AgentAddress#getRoleObject()}.
	 */
	@Test
	public void testGetRoleObject() {
		AssertJUnit.assertEquals(r, aa.getRoleObject());
	}

	/**
	 * Test method for
	 * {@link com.distrimind.madkit.kernel.AgentAddress#getKernelAddress()}.
	 */
	@Test
	public void testGetKernelAddress() {
		AssertJUnit.assertEquals(aa.getKernelAddress(), mk.getKernelAddress());
	}

	/**
	 * Test method for {@link com.distrimind.madkit.kernel.AgentAddress#toString()}.
	 */
	@Test
	public void testToString() {
		System.err.println(aa.getKernelAddress());
		System.err.println(aa);
		AssertJUnit.assertTrue(aa.toString(), aa.toString().contains("@(Group(c:/g/:0:0:0),r)"));
	}


	@Test
	public void testHashCode() {
		AssertJUnit.assertEquals(a.hashCode(), aa.hashCode());
		AssertJUnit.assertEquals(a.getAgentID(), aa.getAgentID());
	}

	/**
	 * Test method for
	 * {@link com.distrimind.madkit.kernel.AgentAddress#equals(java.lang.Object)}.
	 */
	@Test
	public void testEqualsObject() {
		AgentAddress other = new AgentAddress(new AbstractAgent(), r, r.getKernelAddress(), true);
        JUnitFunctions.assertNotEquals(other, aa);
		other = new AgentAddress(a, r, r.getKernelAddress(), true);
		System.err.println(aa.toString() + aa.getKernelAddress());
		System.err.println(other.toString() + other.getKernelAddress());
        AssertJUnit.assertEquals(other, aa);
	}


	@Test
	public void testExists() {
		AssertJUnit.assertTrue(a.checkAgentAddress(aa));
		AssertJUnit.assertEquals(AbstractAgent.ReturnCode.SUCCESS, r.removeMember(a, false));
		AssertJUnit.assertFalse(a.checkAgentAddress(aa));
	}


	@Test
	public void testIsLocal() {
		AssertJUnit.assertTrue(aa.isFrom(a.getKernelAddress()));
		AssertJUnit.assertEquals(AbstractAgent.ReturnCode.SUCCESS, r.removeMember(a, false));
		AssertJUnit.assertTrue(aa.isFrom(a.getKernelAddress()));
	}

}
