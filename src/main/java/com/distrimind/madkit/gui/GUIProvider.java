package com.distrimind.madkit.gui;
/*
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

import com.distrimind.madkit.action.Action;
import com.distrimind.madkit.action.AgentAction;
import com.distrimind.madkit.kernel.AbstractAgent;
import com.distrimind.madkit.kernel.KernelAddress;
import com.distrimind.madkit.kernel.Scheduler;

import java.util.logging.Logger;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.4.0
 */
public abstract class GUIProvider {
	private static GUIProvider provider;

	protected GUIProvider() {
		if (provider!=null)
			throw new IllegalStateException("Only one MadKit GUI provider can be instantiated. The current loaded provider is "+provider.getClass().getName());
		provider=this;
	}

	public static void removeAgentLogLevelMenu(AbstractAgent agent)
	{
		if (provider!=null)
		{
			provider.removeAgentLogLevelMenuImpl(agent);
		}

	}
	public static void removeAgentStatusPanel(AbstractAgent agent)
	{
		if (provider!=null)
		{
			provider.removeAgentStatusPanelImpl(agent);
		}
	}
	public static void removeAgentLogLevelMenu(KernelAddress kernelAddress)
	{
		if (provider!=null)
		{
			provider.removeAgentLogLevelMenuImpl(kernelAddress);
		}

	}
	public static void removeAgentStatusPanel(KernelAddress kernelAddress)
	{
		if (provider!=null)
		{
			provider.removeAgentStatusPanelImpl(kernelAddress);
		}
	}

	public static void setupAgentFrame(AbstractAgent agent, Object ... parameters)
	{
		if (provider!=null)
		{
			provider.setupAgentFrameImpl(agent, parameters);
		}
	}
	public static void setupSchedulerFrame(Scheduler agent, Object ... parameters)
	{
		if (provider!=null)
		{
			provider.setupSchedulerFrameImpl(agent, parameters);
		}
	}
	public static Object getGUIAction(Action action)
	{
		if (provider==null)
			return null;
		return provider.getGUIActionImpl(action);
	}
	/**
	 * Converts the name of an enum object to a Java standardized method name. For
	 * instance, using this on {@link AgentAction#LAUNCH_AGENT} will return
	 * <code>launchAgent</code>. This is especially used by
	 * {@link AbstractAgent#proceedEnumMessage(com.distrimind.madkit.message.EnumMessage)} to
	 * reflexively call the method of an agent which corresponds to the code of such
	 * messages.
	 *
	 * @param e
	 *            the enum object to convert
	 * @return a string having a Java standardized method name form.
	 * @param <E> the enum type
	 */
	public static <E extends Enum<E>> String enumToMethodName(final E e) {
		final String[] tab = e.name().split("_");
		StringBuilder methodName = new StringBuilder(tab[0].toLowerCase());
		for (int i = 1; i < tab.length; i++) {
			final String s = tab[i];
			methodName.append(s.charAt(0)).append(s.substring(1).toLowerCase());
		}
		return methodName.toString();
	}
	public static Object getSchedulerBoundedRangeModel(Scheduler scheduler)
	{
		if (provider!=null)
		{
			return provider.getSchedulerBoundedRangeModelImpl(scheduler);
		}
		else
			return null;
	}
	public static void setSchedulerDelay(Scheduler scheduler, int delay)
	{
		if (provider!=null)
		{
			provider.setSchedulerDelayImpl(scheduler, delay);
		}
		else
			scheduler.setDelayWithoutInteractionWithGUI(delay);
	}

	public static void setGUIActionEnabled(Object guiAction, boolean enabled)
	{
		provider.setGUIActionEnabledImpl(guiAction, enabled);
	}
	public static void launchAgentGUI(AbstractAgent agent, Logger logger)
	{
		if (provider!=null)
		{
			provider.launchAgentGUIImpl(agent, logger);
		}
	}
	public static void terminateAgentGUI(AbstractAgent agent, Logger logger)
	{
		if (provider!=null)
		{
			provider.terminateAgentGUIImpl(agent, logger);
		}
	}
	public static void launchConsoleAgent(AbstractAgent kernel)
	{
		if (provider!=null)
		{
			provider.launchConsoleAgentImpl(kernel);
		}
	}
	public static void loadLocalDemos()
	{
		if (provider!=null)
		{
			provider.loadLocalDemosImpl();
		}
	}
	public static void updateAgentLogLevelMenu(AbstractAgent agent)
	{
		if (provider!=null)
		{
			provider.updateAgentLogLevelMenuImpl(agent);
		}
	}
	public static void updateAllMenus()
	{
		if (provider!=null)
		{
			provider.updateAllMenusImpl();
		}
	}
	public static void updateAllAgentStatusPanel()
	{
		if (provider!=null)
		{
			provider.updateAllAgentStatusPanelImpl();
		}
	}
	public static Class<?> getDefaultDesktopFrameClass()
	{
		if (provider!=null)
		{
			return provider.getDefaultDesktopFrameClassImpl();
		}
		return null;
	}
	public static Class<?> getDefaultAgentFrameClass()
	{
		if (provider!=null)
		{
			return provider.getDefaultAgentFrameClassImpl();
		}
		return null;
	}
	public static Class<?> getDefaultGUIManagerAgent()
	{
		if (provider!=null)
		{
			return provider.getDefaultGUIManagerAgentImpl();
		}
		return null;
	}
	public static void showMessageDialog(String message, String title, MessageType messageType)
	{
		if (provider!=null)
		{
			provider.showMessageDialogImpl(message, title, messageType);
		}
	}
	protected abstract void removeAgentLogLevelMenuImpl(AbstractAgent agent);
	protected abstract void removeAgentStatusPanelImpl(AbstractAgent agent);
	protected abstract void removeAgentLogLevelMenuImpl(KernelAddress kernelAddress);
	protected abstract void removeAgentStatusPanelImpl(KernelAddress kernelAddress);
	protected abstract void setupAgentFrameImpl(AbstractAgent agent, Object ... parameters);
	protected abstract void setupSchedulerFrameImpl(Scheduler agent, Object ... parameters);
	protected abstract Object getSchedulerBoundedRangeModelImpl(Scheduler scheduler);
	protected abstract void setSchedulerDelayImpl(Scheduler scheduler, int delay);
	protected abstract Object getGUIActionImpl(Action action);
	protected abstract void setGUIActionEnabledImpl(Object guiAction, boolean enabled);
	protected abstract void launchAgentGUIImpl(AbstractAgent agent, Logger logger);
	protected abstract void terminateAgentGUIImpl(AbstractAgent agent, Logger logger);
	protected abstract void launchConsoleAgentImpl(AbstractAgent kernel);
	protected abstract void loadLocalDemosImpl();
	protected abstract void updateAgentLogLevelMenuImpl(AbstractAgent agent);
	protected abstract void updateAllMenusImpl();
	protected abstract void updateAllAgentStatusPanelImpl();
	protected abstract Class<?> getDefaultDesktopFrameClassImpl();
	protected abstract Class<?> getDefaultAgentFrameClassImpl();
	protected abstract void showMessageDialogImpl(String message, String title, MessageType messageType);
	protected abstract Class<?> getDefaultGUIManagerAgentImpl();
	public enum MessageType
	{
		INFORMATION,
		WARNING
	}

}
