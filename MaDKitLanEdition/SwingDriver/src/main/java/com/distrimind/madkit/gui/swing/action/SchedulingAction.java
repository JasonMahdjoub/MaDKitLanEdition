/*
 * Copyright or © or Copr. Fabien Michel, Olivier Gutknecht, Jacques Ferber (1997)
 * 
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
package com.distrimind.madkit.gui.swing.action;

import com.distrimind.madkit.kernel.Scheduler;

import static java.awt.event.KeyEvent.*;

/**
 * Enum representing operations which could be done by a {@link Scheduler}
 * agent.
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.14
 * @version 0.9
 * 
 */
public enum SchedulingAction {

	RUN(VK_P, com.distrimind.madkit.action.SchedulingAction.RUN),
	STEP(VK_SPACE, com.distrimind.madkit.action.SchedulingAction.STEP),
	SPEED_UP(VK_RIGHT, com.distrimind.madkit.action.SchedulingAction.SPEED_UP),
	SPEED_DOWN(VK_LEFT, com.distrimind.madkit.action.SchedulingAction.SPEED_DOWN),
	PAUSE(VK_DOLLAR, com.distrimind.madkit.action.SchedulingAction.PAUSE),
	SHUTDOWN(VK_DOLLAR, com.distrimind.madkit.action.SchedulingAction.SHUTDOWN);



	private ActionInfo actionInfo;
	final private int keyEvent;
	private final com.distrimind.madkit.action.SchedulingAction MKSchedulingAction;

	SchedulingAction(int keyEvent, com.distrimind.madkit.action.SchedulingAction MKSchedulingAction) {
		this.keyEvent = keyEvent;
		this.MKSchedulingAction=MKSchedulingAction;
	}

	public com.distrimind.madkit.action.SchedulingAction getMKSchedulingAction() {
		return MKSchedulingAction;
	}

	/**
	 * @return the actionInfo
	 */
	public ActionInfo getActionInfo() {
		if (actionInfo == null)
			actionInfo = new ActionInfo(this, keyEvent, com.distrimind.madkit.action.SchedulingAction.getMessages());
		return actionInfo;
	}



	public static SchedulingAction from(com.distrimind.madkit.action.SchedulingAction action)
	{
		for (SchedulingAction a : SchedulingAction.values())
		{
			if (a.MKSchedulingAction==action)
				return a;
		}
		throw new IllegalAccessError();
	}
}
