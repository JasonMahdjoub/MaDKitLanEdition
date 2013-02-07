/*
 * Copyright 1997-2013 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit.
 * 
 * MaDKit is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 * 
 * MaDKit is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit. If not, see <http://www.gnu.org/licenses/>.
 */
package madkit.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import madkit.action.KernelAction;
import madkit.gui.MASModel;
import madkit.gui.SwingUtil;
import madkit.i18n.Words;
import madkit.kernel.AbstractAgent;
import madkit.kernel.MadkitClassLoader;

/**
 * This class builds a {@link JMenu} containing all the 
 * agent classes containing a main method
 * 
 * @author Fabien Michel
 * @since MaDKit 5.0.1
 * @version 0.9
 * 
 */
public class LaunchMain extends JMenu {

	private static final long serialVersionUID = 6721458300016754609L;
	final static private Set<LaunchMain> menus = new HashSet<>();//TODO Map 
	final private AbstractAgent myAgent;

	/**
	 * Builds a new menu.
	 * @param agent the agent according 
	 * to which this menu should be created, i.e. the
	 * agent that will be responsible of the launch.
	 */
	public LaunchMain(final AbstractAgent agent) {
		super("Main");
		setMnemonic(KeyEvent.VK_S);
		myAgent = agent;
		synchronized (menus) {
			menus.add(this);
		}
		update();
	}

	/**
	 * Called by the kernel when the class path is modified.
	 * This is for instance the case when the 
	 * {@link MadkitClassLoader#addToClasspath(java.net.URL)}
	 * is used.
	 */
	public static void updateAllMenus() {//TODO facto
		synchronized (menus) {
			for (LaunchMain menu : menus) {
				menu.update();
			}
		}
	}

	private void update() {
		if(! myAgent.isAlive())
			return;
		removeAll();
		Set<String> classes = myAgent.getMadkitClassLoader().getAgentsWithMain();
		final String[] params = null; 
		for (final String string : classes) {
			JMenuItem name = new JMenuItem(string+".main");
			name.setIcon(SwingUtil.MADKIT_LOGO_SMALL);
			name.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					try {
						 myAgent.getMadkitClassLoader().loadClass(string).getDeclaredMethod("main", String[].class).invoke(null, (Object) params);
					} catch (ClassNotFoundException | IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e1) {
						e1.printStackTrace();
					}
				}
			});
			add(name);
		}
	}

}
