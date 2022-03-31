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
package com.distrimind.madkit.action;


import com.distrimind.madkit.i18n.I18nUtilities;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.message.EnumMessage;

import java.util.ResourceBundle;

/**
 * Enum representing agent actions
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.0.14
 * @version 0.9
 * 
 */
public enum AgentAction {

	LAUNCH_AGENT,
	RELOAD,
	CREATE_GROUP,
	REQUEST_ROLE,
	LEAVE_ROLE,
	LEAVE_GROUP,
	SEND_MESSAGE,
	SEND_MESSAGE_AND_DIFFER_IT_IF_NECESSARY,
	CANCEL_DIFFERED_MESSAGES_BY_SENDER_ROLE,
	CANCEL_DIFFERED_MESSAGES_BY_RECEIVER_ROLE,
	CANCEL_DIFFERED_MESSAGES_BY_GROUP,
	BROADCAST_MESSAGE,
	KILL_AGENT;

	/**
	 * Builds an action that will make the agent do the corresponding behavior
	 *
	 * @param agent
	 *            the agent on which this action will operate
	 * @param parameters
	 *            the info to be used
	 * @return the action corresponding to the enum
	 */
	public Action getActionFor(final AbstractAgent agent, final Object... parameters) {
		return new Action(this, () -> {

			if (agent.isAlive()) {
				agent.proceedEnumMessage(new EnumMessage<>(AgentAction.this, parameters));
			}
		});
	}

	final static private ResourceBundle messages = I18nUtilities.getResourceBundle(AgentAction.class.getSimpleName());

	public static ResourceBundle getMessages()
	{
		return messages;
	}
}
