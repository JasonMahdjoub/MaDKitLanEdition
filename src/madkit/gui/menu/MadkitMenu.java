/*
 * Copyright 1997-2011 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MadKit.
 * 
 * MadKit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MadKit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with MadKit. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.gui.menu;

import java.awt.event.KeyEvent;

import javax.swing.JMenu;

import madkit.action.GUIManagerAction;
import madkit.action.KernelAction;
import madkit.kernel.AbstractAgent;

/**
 * An out of the box menu for MadKit applications
 * 
 * @author Fabien Michel
 * @since MadKit 5.0.0.9
 * @version 0.9
 * 
 */
public class MadkitMenu extends JMenu {//TODO i18n

	private static final long serialVersionUID = 6177193453649323680L;

	/**
	 * Builds a menu featuring the following actions:
	 * <ul>
	 * <li> {@link KernelAction#EXIT}
	 * <li> {@link KernelAction#RESTART}
	 * <li> {@link KernelAction#LAUNCH_NETWORK}
	 * <li> {@link KernelAction#STOP_NETWORK}
	 * <li> {@link GUIManagerAction#LOAD_JAR_FILE}
	 * <li> {@link GUIManagerAction#ICONIFY_ALL}
	 * <li> {@link GUIManagerAction#DEICONIFY_ALL}
	 * <li> {@link GUIManagerAction#KILL_AGENTS}
	 * </ul>
	 * 
	 * @param agent the agent for which this menu
	 * will be built.
	 */
	public MadkitMenu(final AbstractAgent agent){
		super("MadKit");
		setMnemonic(KeyEvent.VK_M);
		KernelAction.addAllActionsTo(this,agent);
		GUIManagerAction.addAllActionsTo(this, agent);
	}
}
