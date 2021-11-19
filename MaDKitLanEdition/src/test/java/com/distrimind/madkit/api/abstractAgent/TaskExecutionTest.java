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

import com.distrimind.madkit.kernel.*;
import org.testng.annotations.Test;
import org.testng.AssertJUnit;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import com.distrimind.madkit.kernel.TestNGMadkit;
import com.distrimind.madkit.message.task.TasksExecutionConfirmationMessage;

/**
 * 
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MadkitLanEdition 1.0
 */
public class TaskExecutionTest extends TestNGMadkit {

	@Test
	public void testDefaultTaskManagerAgentWithOneCore() {
		launchTest(new TaskAgentTester());
	}
}

class TaskAgentTester extends AbstractAgent {
	final AtomicInteger taskExecutionNumber = new AtomicInteger(0);

	TaskAgentTester() {
	}

	@Override
	public void activate() {
		Callable<Void> callable = () -> {
			taskExecutionNumber.incrementAndGet();
			return null;
		};

		taskExecutionNumber.set(0);

		SC(new Task<>(callable), false);

		TestNGMadkit.pause(this, 20);
		AssertJUnit.assertEquals(1, taskExecutionNumber.get());
		TaskID id = SC(new Task<>(callable), true);
		TestNGMadkit.pause(this, 20);
		AssertJUnit.assertEquals(2, taskExecutionNumber.get());
		Message m = nextMessage();
		AssertJUnit.assertNotNull(m);
		AssertJUnit.assertEquals(TasksExecutionConfirmationMessage.class, m.getClass());
		AssertJUnit.assertEquals(id, m.getConversationID());

		SC(new Task<>(callable, System.currentTimeMillis() + 1000), false);
		TestNGMadkit.pause(this, 100);
		AssertJUnit.assertEquals(2, taskExecutionNumber.get());
		TestNGMadkit.pause(this, 1000);
		AssertJUnit.assertEquals(3, taskExecutionNumber.get());
		id = SC(new Task<>(callable, System.currentTimeMillis() + 1000), false);
		TestNGMadkit.pause(this, 100);
		AssertJUnit.assertEquals(3, taskExecutionNumber.get());
		CT(id);
		TestNGMadkit.pause(this, 1000);
		AssertJUnit.assertEquals(3, taskExecutionNumber.get());
		id = SC(new Task<>(callable, System.currentTimeMillis() + 500, 500), false);
		TestNGMadkit.pause(this, 100);
		AssertJUnit.assertEquals(3, taskExecutionNumber.get());
		TestNGMadkit.pause(this, 500);
		AssertJUnit.assertEquals(4, taskExecutionNumber.get());
		TestNGMadkit.pause(this, 600);
		AssertJUnit.assertEquals(5, taskExecutionNumber.get());
		CT(id);
		AssertJUnit.assertTrue(id.isCanceled());
		TestNGMadkit.pause(this, 1200);
		AssertJUnit.assertTrue(id.isCanceled());
		AssertJUnit.assertEquals(5, taskExecutionNumber.get());
		for (int i = 0; i < 100; i++)
			SC(new Task<>(callable), false);
		TestNGMadkit.pause(this, 200);
		AssertJUnit.assertEquals(105, taskExecutionNumber.get());
		for (int i = 0; i < 100; i++)
			SC(new Task<>(callable, System.currentTimeMillis() + ((long) (Math.random() * 200.0))), false);
		TestNGMadkit.pause(this, 1000);
		AssertJUnit.assertEquals(205, taskExecutionNumber.get());

		/*
		 * ArrayList<Task<?>> list=new ArrayList<>(100); for (int i=0;i<100;i++)
		 * list.add(new Task<Void>(callable)); SC(list, false); JunitMadkit.pause(200);
		 * Assert.assertEquals(305, taskExecutionNumber.get()); list=new
		 * ArrayList<>(100); for (int i=0;i<100;i++) list.add(new Task<Void>(callable,
		 * System.currentTimeMillis()+((long)(Math.random()*200.0)))); SC(list, false);
		 * JunitMadkit.pause(1000); Assert.assertEquals(405, taskExecutionNumber.get());
		 */

	}

	private TaskID SC(Task<Void> task, boolean confirmation) {
		return scheduleTask(task, confirmation);
	}

	/*
	 * private TaskID SC(ArrayList<Task<?>> tasks, boolean confirmation) { if
	 * (TaskAgentName==null) return scheduleTasks(tasks, confirmation); else return
	 * scheduleTasks(TaskAgentName, tasks, confirmation);
	 * 
	 * }
	 */
	private void CT(TaskID id) {
		cancelTask(id, false);
	}

}
