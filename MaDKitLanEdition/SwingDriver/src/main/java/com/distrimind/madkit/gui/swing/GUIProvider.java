package com.distrimind.madkit.gui.swing;/*
Copyright or © or Copr. Jason Mahdjoub (01/04/2013)

jason.mahdjoub@distri-mind.fr

This software (Object Oriented Database (OOD)) is a computer program 
whose purpose is to manage a local database with the object paradigm 
and the java language 

This software is governed by the CeCILL-C license under French law and
abiding by the rules of distribution of free software.  You can  use, 
modify and/ or redistribute the software under the terms of the CeCILL-C
license as circulated by CEA, CNRS and INRIA at the following URL
"http://www.cecill.info". 

As a counterpart to the access to the source code and  rights to copy,
modify and redistribute granted by the license, users are provided only
with a limited warranty  and the software's author,  the holder of the
economic rights,  and the successive licensors  have only  limited
liability. 

In this respect, the user's attention is drawn to the risks associated
with loading,  using,  modifying and/or developing or reproducing the
software by the user in light of its specific status of free software,
that may mean  that it is complicated to manipulate,  and  that  also
therefore means  that it is reserved for developers  and  experienced
professionals having in-depth computer knowledge. Users are therefore
encouraged to load and test the software's suitability as regards their
requirements in conditions enabling the security of their systems and/or 
data to be ensured and,  more generally, to use and operate it in the 
same conditions as regards security. 

The fact that you are presently reading this means that you have had
knowledge of the CeCILL-C license and that you accept its terms.
 */

import com.distrimind.madkit.action.AgentAction;
import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.gui.swing.action.ActionInfo;
import com.distrimind.madkit.gui.swing.action.GUIManagerAction;
import com.distrimind.madkit.gui.swing.action.GlobalAction;
import com.distrimind.madkit.gui.swing.action.MDKAbstractAction;
import com.distrimind.madkit.gui.swing.menu.AgentLogLevelMenu;
import com.distrimind.madkit.gui.swing.menu.ClassPathSensitiveMenu;
import com.distrimind.madkit.gui.swing.message.GUIMessage;
import com.distrimind.madkit.kernel.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Observable;
import java.util.Observer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.distrimind.util.ReflectionTools.*;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.4.0
 */
public class GUIProvider extends com.distrimind.madkit.gui.GUIProvider{
	protected GUIProvider() {
	}

	@Override
	protected void removeAgentLogLevelMenuImpl(AbstractAgent agent)
	{
		AgentLogLevelMenu.remove(agent);
	}
	@Override
	protected void removeAgentStatusPanelImpl(AbstractAgent agent)
	{
		AgentStatusPanel.remove(agent);
	}
	@Override
	protected void removeAgentLogLevelMenuImpl(KernelAddress kernelAddress)
	{
		AgentLogLevelMenu.remove(kernelAddress);
	}
	@Override
	protected void removeAgentStatusPanelImpl(KernelAddress kernelAddress)
	{
		AgentStatusPanel.remove(kernelAddress);
	}
	@Override
	protected void launchConsoleAgentImpl(AbstractAgent kernel)
	{
		kernel.launchAgent(new ConsoleAgent());
	}
	@Override
	protected void loadLocalDemosImpl()
	{
		GlobalAction.LOAD_LOCAL_DEMOS.actionPerformed(null);
	}
	@Override
	protected void updateAgentLogLevelMenuImpl(AbstractAgent agent)
	{
		AgentLogLevelMenu.update(agent);
	}
	@Override
	protected void updateAllMenusImpl()
	{
		ClassPathSensitiveMenu.updateAllMenus();
	}
	@Override
	protected void updateAllAgentStatusPanelImpl()
	{
		AgentStatusPanel.updateAll();
	}
	@Override
	protected Class<?> getDefaultDesktopFrameClassImpl()
	{
		return MDKDesktopFrame.class;
	}
	@Override
	protected Class<?> getDefaultAgentFrameClassImpl()
	{
		return AgentFrame.class;
	}
	@Override
	protected void setupAgentFrameImpl(AbstractAgent agent, Object ... parameters)
	{
		if (parameters.length!=1)
			throw new IllegalArgumentException();
		if (parameters[0] instanceof JFrame frame)
		{
			frame.add(new OutputPanel(agent));
		}
		else
			throw new IllegalArgumentException();
	}
	@Override
	protected void setupSchedulerFrameImpl(Scheduler scheduler, Object ... parameters)
	{
		if (parameters.length!=1)
			throw new IllegalArgumentException();
		if (parameters[0] instanceof JFrame frame)
		{
			frame.add(new OutputPanel(scheduler));
			frame.add(getSchedulerToolBar(scheduler), BorderLayout.PAGE_START);
			frame.add(getSchedulerStatusLabel(scheduler), BorderLayout.PAGE_END);
			scheduler.setGVT(scheduler.getGVT());
			frame.getJMenuBar().add(getSchedulerMenu(scheduler), 2);
		}
		else
			throw new IllegalArgumentException();
	}
	/**
	 * Returns a menu which could be used in any GUI.
	 *
	 * @return a menu controlling the scheduler's actions
	 */
	public JMenu getSchedulerMenu(Scheduler scheduler) {
		JMenu myMenu = new JMenu("Scheduling");
		myMenu.setMnemonic(KeyEvent.VK_S);
		myMenu.add(getRun(scheduler));
		myMenu.add(getStep(scheduler));
		myMenu.add(getSpeedUp(scheduler));
		myMenu.add(getSpeedDown(scheduler));
		return myMenu;
	}
	/**
	 * Returns a label giving some information on the simulation process
	 *
	 * @return a label giving some information on the simulation process
	 */
	public JLabel getSchedulerStatusLabel(Scheduler scheduler) {

		try {
			Scheduler.GVTModel gvtModel = (Scheduler.GVTModel)f_gvtModel.get(scheduler);
			if (gvtModel == null) {
				gvtModel = new Scheduler.GVTModel();
				f_gvtModel.set(scheduler, gvtModel);
			}

			GVTJLabel timer = new GVTJLabel() {
				@Override
				public void update(Observable o, Object arg) {
					setText("Simulation " + scheduler.getSimulationState() + ", time is " + arg);
				}
			};
			timer.setText("GVT");
			gvtModel.addObserver(timer);
			timer.setBorder(new EmptyBorder(4, 4, 4, 4));
			timer.setHorizontalAlignment(SwingConstants.LEADING);
			scheduler.setGVT(scheduler.getGVT());
			return timer;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}

	}

	/**
	 * Returns a toolbar which could be used in any GUI.
	 *
	 * @return a toolBar controlling the scheduler's actions
	 */
	protected JToolBar getSchedulerToolBar(Scheduler scheduler) {
		final JToolBar toolBar = new JToolBar("scheduler toolbar");
		toolBar.add(getRun(scheduler));
		toolBar.add(getStep(scheduler));
		final JPanel p = new JPanel();
		p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
		p.setBorder(new TitledBorder("speed"));
		final JSlider sp = new JSlider((BoundedRangeModel) scheduler.getSpeedModel());
		sp.addMouseWheelListener(new MouseWheelListener() {
			@Override
			public void mouseWheelMoved(MouseWheelEvent e) {
				int move = -e.getWheelRotation();
				if (sp.getValue() < 398) {
					move *= 10;
				}
				move = Math.min((move + sp.getValue()), sp.getMaximum());
				sp.setValue(move);
				sp.getChangeListeners()[0].stateChanged(new ChangeEvent(this));
			}
		});
		sp.addChangeListener(e -> updateToolTip(scheduler, p, sp));
		updateToolTip(scheduler, p, sp);
		// p.setPreferredSize(new Dimension(150, 25));
		p.add(sp);
		// toolBar.addSeparator();
		// toolBar.add(Box.createRigidArea(new Dimension(40,5)));
		// toolBar.add(Box.createHorizontalGlue());
		toolBar.add(p);
		// toolBar.add(getGVTLabel());
		SwingUtil.scaleAllAbstractButtonIconsOf(toolBar, 24);
		return toolBar;
	}
	/*
	 * Returns a label giving the simulation time
	 *
	 * @return a label giving the simulation time
	 */
	/*public JLabel getGVTLabel() {
		if (gvtModel == null) {
			gvtModel = new GVTModel();
		}
		final GVTJLabel timer = new GVTJLabel();
		timer.setText("0");
		gvtModel.addObserver(timer);
		timer.setBorder(new EmptyBorder(4, 4, 4, 4));
		timer.setHorizontalAlignment(SwingConstants.LEADING);
		setGVT(getGVT());
		return timer;
	}*/
	void updateToolTip(Scheduler scheduler, final JPanel p, final JSlider sp) {
		final String text = "pause = " + scheduler.getDelay() + " ms";
		sp.setToolTipText(text);
		p.setToolTipText(text);
	}

	@Override
	protected Object getSchedulerBoundedRangeModelImpl(Scheduler scheduler)
	{
		return new DefaultBoundedRangeModel(400, 0, 0, 400) {

			public void setValue(int n) {
				super.setValue(n);
				scheduler.setDelayWithoutInteractionWithGUI(getValue());
			}
		};
	}
	@Override
	protected void setSchedulerDelayImpl(Scheduler scheduler, int delay)
	{
		DefaultBoundedRangeModel m=(DefaultBoundedRangeModel)scheduler.getSpeedModel();
		m.setValue(delay);
	}

	@Override
	protected Object getGUIActionImpl(com.distrimind.madkit.action.Action action)
	{

		return new MDKAbstractAction(getActionInfo(action.getEnumAction())) {
			@Override
			public void actionPerformed(ActionEvent e) {
				action.run();
			}
		};
	}

	private int getTypeMessage(MessageType messageType)
	{
		return switch (messageType) {
			case INFORMATION -> JOptionPane.INFORMATION_MESSAGE;
			case WARNING -> JOptionPane.WARNING_MESSAGE;
		};
	}

	@Override
	protected void showMessageDialogImpl(String message, String title, MessageType messageType)
	{
		JOptionPane.showMessageDialog(null,
				message,
				title, getTypeMessage(messageType));
	}
	@Override
	protected void setGUIActionEnabledImpl(Object guiAction, boolean enabled)
	{
		((MDKAbstractAction)guiAction).setEnabled(enabled);
	}


	public ActionInfo getActionInfo(Enum<?> e)
	{
		if (e instanceof com.distrimind.madkit.action.AgentAction)
		{
			return com.distrimind.madkit.gui.swing.action.AgentAction.from((AgentAction)e).getActionInfo();
		}
		else if (e instanceof com.distrimind.madkit.action.KernelAction)
		{
			return com.distrimind.madkit.gui.swing.action.KernelAction.from((com.distrimind.madkit.action.KernelAction)e).getActionInfo();
		}
		else if (e instanceof com.distrimind.madkit.action.SchedulingAction)
		{
			return com.distrimind.madkit.gui.swing.action.SchedulingAction.from((com.distrimind.madkit.action.SchedulingAction)e).getActionInfo();
		}
		throw new IllegalAccessError();
	}

	@Override
	protected void launchAgentGUIImpl(AbstractAgent agent, Logger logger){
		if (agent.hasGUI()) {
			if (logger != null && logger.isLoggable(Level.FINER)) {
				logger.finer("** setting up  GUI **");
			}
			agent.sendMessage(LocalCommunity.Groups.GUI, LocalCommunity.Roles.GUI,
					new GUIMessage(GUIManagerAction.SETUP_AGENT_GUI, agent));
			try {// wait answer using a big hack
				getMessageBox(agent).take();// works because the agent cannot be joined in anyway
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	private static final Method m_setConnectionInfoSystemMessage;
	private static final Field f_gvtModel;
	private static final Field f_run;
	private static final Field f_step;
	private static final Field f_speedUp;
	private static final Field f_speedDown;
	static
	{
		m_setConnectionInfoSystemMessage=getMethod(AbstractAgent.class, "getMessageBox");
		f_gvtModel=getField(Scheduler.class, "gvtModel");
		f_run=getField(Scheduler.class, "run");
		f_step=getField(Scheduler.class, "step");
		f_speedUp=getField(Scheduler.class, "speedUp");
		f_speedDown=getField(Scheduler.class, "speedDown");
	}
	static ChainedBlockingDeque<Message> getMessageBox(AbstractAgent agent) {
		try {
			//noinspection unchecked
			return (ChainedBlockingDeque<Message>) invoke(m_setConnectionInfoSystemMessage, agent);
		} catch (InvocationTargetException e) {
			System.err.println("Unexpected error :");
			e.printStackTrace();
			System.exit(-1);
		}
		return null;
	}

	@Override
	protected void terminateAgentGUIImpl(AbstractAgent agent, Logger logger)
	{
		if (agent.hasGUI()) {
			AbstractAgent.ReturnCode rc = agent.broadcastMessage(LocalCommunity.Groups.GUI, LocalCommunity.Roles.GUI,
					new GUIMessage(GUIManagerAction.DISPOSE_AGENT_GUI, agent));
			if (rc != AbstractAgent.ReturnCode.SUCCESS)
				agent.getLogger().warning("Agent GUI disposing. Impossible send message to GUI Manager Agent : " + rc);
		}
	}

	public MDKAbstractAction getRun(Scheduler scheduler)
	{
		try {
			return (MDKAbstractAction)((com.distrimind.madkit.action.Action)f_run.get(scheduler)).getGuiAction();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	public MDKAbstractAction getStep(Scheduler scheduler)
	{
		try {
			return (MDKAbstractAction)((com.distrimind.madkit.action.Action)f_step.get(scheduler)).getGuiAction();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	public MDKAbstractAction getSpeedUp(Scheduler scheduler)
	{
		try {
			return (MDKAbstractAction)((com.distrimind.madkit.action.Action)f_speedUp.get(scheduler)).getGuiAction();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}
	public MDKAbstractAction getSpeedDown(Scheduler scheduler)
	{
		try {
			return (MDKAbstractAction)((com.distrimind.madkit.action.Action)f_speedDown.get(scheduler)).getGuiAction();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
	}

}

class GVTJLabel extends JLabel implements Observer {

	@Override
	public void update(Observable o, Object arg) {
		setText(arg.toString());
	}

}