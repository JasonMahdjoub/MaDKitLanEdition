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
package madkit.kernel;

import static madkit.kernel.AbstractAgent.ReturnCode.AGENT_CRASH;
import static madkit.kernel.AbstractAgent.ReturnCode.ALREADY_GROUP;
import static madkit.kernel.AbstractAgent.ReturnCode.ALREADY_KILLED;
import static madkit.kernel.AbstractAgent.ReturnCode.ALREADY_LAUNCHED;
import static madkit.kernel.AbstractAgent.ReturnCode.INVALID_AA;
import static madkit.kernel.AbstractAgent.ReturnCode.INVALID_ARG;
import static madkit.kernel.AbstractAgent.ReturnCode.LAUNCH_TIME_OUT;
import static madkit.kernel.AbstractAgent.ReturnCode.NETWORK_DOWN;
import static madkit.kernel.AbstractAgent.ReturnCode.NOT_COMMUNITY;
import static madkit.kernel.AbstractAgent.ReturnCode.NOT_GROUP;
import static madkit.kernel.AbstractAgent.ReturnCode.NOT_IN_GROUP;
import static madkit.kernel.AbstractAgent.ReturnCode.NOT_ROLE;
import static madkit.kernel.AbstractAgent.ReturnCode.NOT_YET_LAUNCHED;
import static madkit.kernel.AbstractAgent.ReturnCode.NO_RECIPIENT_FOUND;
import static madkit.kernel.AbstractAgent.ReturnCode.NULL_STRING;
import static madkit.kernel.AbstractAgent.ReturnCode.ROLE_NOT_HANDLED;
import static madkit.kernel.AbstractAgent.ReturnCode.SEVERE;
import static madkit.kernel.AbstractAgent.ReturnCode.SUCCESS;
import static madkit.kernel.AbstractAgent.State.ACTIVATED;
import static madkit.kernel.AbstractAgent.State.INITIALIZING;
import static madkit.kernel.AbstractAgent.State.NOT_LAUNCHED;
import static madkit.kernel.CGRSynchro.Code.CREATE_GROUP;
import static madkit.kernel.CGRSynchro.Code.LEAVE_GROUP;
import static madkit.kernel.CGRSynchro.Code.LEAVE_ROLE;
import static madkit.kernel.CGRSynchro.Code.REQUEST_ROLE;
import static madkit.kernel.Madkit.Roles.GUI_MANAGER_ROLE;
import static madkit.kernel.Madkit.Roles.KERNEL_ROLE;
import static madkit.kernel.Madkit.Roles.LOCAL_COMMUNITY;
import static madkit.kernel.Madkit.Roles.NETWORK_GROUP;
import static madkit.kernel.Madkit.Roles.NETWORK_ROLE;
import static madkit.kernel.Madkit.Roles.SYSTEM_GROUP;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.Future;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import madkit.gui.GUIManagerAgent;
import madkit.gui.actions.MadkitActions;
import madkit.gui.messages.GUIMessage;
import madkit.kernel.AbstractAgent.ReturnCode;
import madkit.kernel.Madkit.Roles;
import madkit.messages.KernelMessage;
import madkit.messages.ObjectMessage;

/**
 * The brand new MadKit kernel and it is now a real Agent :)
 * 
 * @author Fabien Michel
 * @version 1.0
 * @since MadKit 5.0
 * 
 */
class MadkitKernel extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3181382740286439342L;

	final private static ThreadGroup systemGroup = new ThreadGroup("MK_SYSTEM");

	final static private ThreadPoolExecutor serviceExecutor = new ThreadPoolExecutor(
			Runtime.getRuntime().availableProcessors() + 1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
			new SynchronousQueue<Runnable>(), new ThreadFactory() {
				public Thread newThread(Runnable r) {
					final Thread t = new Thread(systemGroup, r);
					t.setPriority(Thread.MAX_PRIORITY);
					t.setName("MK_EXE");
					t.setDaemon(true);
					//					t.setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
					//						@Override
					//						public void uncaughtException(Thread t, Throwable e) {
					//							e.printStackTrace();
					//							e.getCause().printStackTrace();
					//						}
					//					});
					return t;
				}
			});

	public final Executor getMadkitExecutor() {
		return serviceExecutor;
	}

	// ;// = Executors.newCachedThreadPool();
	final private static Map<String, Class<?>> primitiveTypes = new HashMap<String, Class<?>>();
	static {
		primitiveTypes.put("java.lang.Integer", int.class);
		primitiveTypes.put("java.lang.Boolean", boolean.class);
		primitiveTypes.put("java.lang.Byte", byte.class);
		primitiveTypes.put("java.lang.Character", char.class);
		primitiveTypes.put("java.lang.Float", float.class);
		primitiveTypes.put("java.lang.Void", void.class);
		primitiveTypes.put("java.lang.Short", short.class);
		primitiveTypes.put("java.lang.Double", double.class);
		primitiveTypes.put("java.lang.Long", long.class);
	}

	static {
		serviceExecutor.prestartAllCoreThreads();
		Runtime.getRuntime().addShutdownHook(new Thread(){
			@Override
			public void run() {
				AgentLogger.resetLoggers();
			}
		});
	}

	private final ConcurrentHashMap<String, Organization> organizations;
	final private Set<Overlooker<? extends AbstractAgent>> operatingOverlookers;
	final private Madkit platform;
	final private KernelAddress kernelAddress;

	protected LoggedKernel loggedKernel;
	private boolean shuttedDown = false;
	final AgentThreadFactory normalAgentThreadFactory;
	final AgentThreadFactory daemonAgentThreadFactory;

	private AgentAddress netAgent;
	// my private addresses for optimizing the message building
	private AgentAddress netUpdater, netEmmiter, kernelRole;
	private Set<Agent> threadedAgents = new HashSet<Agent>(20); 



	MadkitKernel(Madkit m) {
		super(true);
		platform = m;
		if (m != null) {
			setLogLevel(Level.parse(platform.getConfigOption().getProperty(Madkit.kernelLogLevel)));
			kernelAddress = platform.getPlatformID();
			organizations = new ConcurrentHashMap<String, Organization>();
			operatingOverlookers = new LinkedHashSet<Overlooker<? extends AbstractAgent>>();
			normalAgentThreadFactory = new AgentThreadFactory("MKRA" + kernelAddress, false);
			daemonAgentThreadFactory = new AgentThreadFactory("MKDA" + kernelAddress, true);
			loggedKernel = new LoggedKernel(this);
			// launchingAgent(this, this, false);
		} else {
			logger = null;
			kernelAddress = null;
			organizations = null;
			operatingOverlookers = null;
			normalAgentThreadFactory = null;
			daemonAgentThreadFactory = null;
		}
	}

	MadkitKernel(MadkitKernel k) {
		logger = null;
		organizations = k.organizations;
		kernelAddress = k.kernelAddress;
		operatingOverlookers = k.operatingOverlookers;
		platform = k.platform;
//		normalAgentThreadFactory = k.normalAgentThreadFactory;//no need
//		daemonAgentThreadFactory = k.daemonAgentThreadFactory;//TODO no need
		normalAgentThreadFactory = null;
		daemonAgentThreadFactory = null;
	}

	@Override
	protected void activate() {
		createGroup(Roles.LOCAL_COMMUNITY, Roles.SYSTEM_GROUP, false);
		createGroup(Roles.LOCAL_COMMUNITY, Roles.NETWORK_GROUP, false);
		requestRole(Roles.LOCAL_COMMUNITY, Roles.SYSTEM_GROUP, Roles.KERNEL_ROLE, null);
		requestRole(Roles.LOCAL_COMMUNITY, Roles.NETWORK_GROUP, "emmiter", null);
		requestRole(Roles.LOCAL_COMMUNITY, Roles.NETWORK_GROUP, "updater", null);

		myThread.setPriority(Thread.MAX_PRIORITY - 3);
		// black magic here
		try {
			netUpdater = getRole(LOCAL_COMMUNITY, NETWORK_GROUP, "updater").getAgentAddressOf(this);
			netEmmiter = getRole(LOCAL_COMMUNITY, NETWORK_GROUP, "emmiter").getAgentAddressOf(this);
			kernelRole = getRole(LOCAL_COMMUNITY, SYSTEM_GROUP, KERNEL_ROLE).getAgentAddressOf(this);
		} catch (CGRNotAvailable e) {
			throw new AssertionError("Kernel Agent initialization problem");
		}

		//		platform.logSessionConfig(platform.getConfigOption(), Level.FINER);
		if (platform.isOptionActivated(Madkit.loadLocalDemos)) {
			loadLocalDemos();
		}
		if (platform.isOptionActivated(Madkit.autoConnectMadkitWebsite)) {
			addWebRepository();
		}
		launchGuiManagerAgent();
		startSession();
		//		Message m = nextMessage();// In activate only MadKit can feed my mailbox
		//		while (m != null) {
		//			handleMessage(m);
		//			m = waitNextMessage(100);
		//		}
		// logCurrentOrganization(logger,Level.FINEST);
		// try {
		// platform.getMadkitClassLoader().addJar(new
		// URL("http://www.madkit.net/demonstration/repo/market.jar"));
		// } catch (MalformedURLException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	/**
	 * Starts a session considering the current MadKit configuration
	 */
	private void startSession() {
		launchNetworkAgent();
		launchConfigAgents();
	}

	/**
	 * @see madkit.kernel.Agent#live()
	 */
	@Override
	protected void live() {
		while (! shuttedDown) {
			handleMessage(waitNextMessage());
		}
	}

	@Override
	protected void end() {
		platform.printFareWellString();
	}

	final private void launchGuiManagerAgent() {
		if (platform.isOptionActivated(Madkit.noGUIManager)) {
			if (logger != null)
				logger.fine("** No GUI Manager: " + Madkit.noGUIManager + " option is true**\n");
		} else {
			Agent a = new GUIManagerAgent(! platform.isOptionActivated(Madkit.desktop));
			launchAgent(a);
			threadedAgents.remove(a);
			if (logger != null)
				logger.fine("\n\t****** GUI Manager launched ******\n");
		}
	}

	final private void handleKernelMessage(KernelMessage km) {
		Method operation = null;
		final Object[] arguments = km.getContent();
		switch (km.getCode()) {
		case AGENT_LAUNCH_AGENT:// TODO semantic
			operation = launchAgent(arguments);
			break;
		case MADKIT_KILL_AGENTS:// TODO semantic
			killAgents(true);
			return;
		case LOAD_LOCAL_DEMOS:// TODO semantic
			loadLocalDemos();
			sendReply(km, new Message());
			return;
		case MADKIT_LAUNCH_SESSION:// TODO semantic
			launchSession((String[]) arguments);
			return;
		case CONNECT_WEB_REPO:
			addWebRepository();
			sendReply(km, new Message());
			return;
		case MADKIT_LOAD_JAR_FILE:// TODO semantic
			//			System.err.println((URL) km.getContent()[0]);
			platform.getMadkitClassLoader().addJar((URL) km.getContent()[0]);
			sendReply(km, new Message());
			return;
		case MADKIT_LAUNCH_NETWORK:// TODO semantic
			startNetwork();
			return;
		case MADKIT_STOP_NETWORK:// TODO semantic
			stopNetwork();
			return;
		case MADKIT_CLONE:// TODO semantic
			startSession((Boolean) km.getContent()[0]);
			return;
		case MADKIT_RESTART:
			shuttedDown = true;
			restartSession(500);
		case MADKIT_EXIT_ACTION:
			shutdown();
			return;
		default:
			if (logger != null) logger.warning("I received a kernel message that I do not understand. Discarding " + km);
			return;
		}
		doOperation(operation, arguments);
	}

	/**
	 * 
	 */
	private void addWebRepository() {
		try {
			// URL url = new
			// URL("http://www.madkit.net/MadKit-"+getMadkitProperty("version")+"/repo.properties");
			String repoLocation = getMadkitProperty("madkit.repository.url");
			Properties p = new Properties();
			p.load(new URL(repoLocation+ "repo.properties").openStream());
			//				System.err.println(p);
			for (Entry<Object, Object> object : p.entrySet()) {
				// platform.getMadkitClassLoader().addJar(new
				// URL(repoLocation+object.getKey()+".jar"));
				platform.getMadkitClassLoader().addJar(new URL(repoLocation + object.getValue() + "/" + object.getKey() + ".jar"));
			}
		} catch (final IOException e) {
			serviceExecutor.execute(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(null, e.getMessage(),
							"Cannot connect to madkit.net", JOptionPane.WARNING_MESSAGE);
				}
			});
			if(logger != null)
				logger.info("Cannot connect to madkit.net "+e.getMessage());
		}
	}

	/**
	 * 
	 */
	private void loadLocalDemos() {
		File f = lookForMadkitDemoHome();
		if(f != null && f.isDirectory()){
			if(logger != null)
				logger.fine("** LOADING DEMO DIRECTORY **");
			platform.getMadkitClassLoader().loadJarsFromPath(f.getAbsolutePath());
		}
		else{
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					JOptionPane.showMessageDialog(null, 
							"Cannot find the local demo directory...",
							"Load failed",
							JOptionPane.WARNING_MESSAGE);
				}
			});
		}
	}

	private File lookForMadkitDemoHome() {
		for(URL url : getMadkitClassLoader().getURLs()){
			if(url.getProtocol().equals("file") && url.getPath().contains(platform.getConfigOption().getProperty("madkit.jar.name"))){
				return new File(new File(url.getFile()).getParentFile().getAbsolutePath()+File.separatorChar+"demos");
			}
		}
		return null;
	}

	private void launchSession(String[] arguments) {
		if(logger != null)
			logger.finer("** LAUNCHING SESSION "+arguments);
		Properties mkCfg = platform.getConfigOption();
		Properties currentConfig = new Properties();
		currentConfig.putAll(mkCfg);
		mkCfg.putAll(platform.buildSession(arguments));
		startSession();
		mkCfg.putAll(currentConfig);
	}

	private void launchConfigAgents(){
		logger.fine("** LAUNCHING CONFIG AGENTS **");
		final String agentsTolaunch =platform.getConfigOption().getProperty(Madkit.launchAgents);
		if(! agentsTolaunch.equals("null")){
			final String[] agentsClasses = agentsTolaunch.split(";");
			for(final String classNameAndOption : agentsClasses){
				final String[] classAndOptions = classNameAndOption.split(",");
				final String className = classAndOptions[0].trim();//TODO should test if these classes exist
				final boolean withGUI = (classAndOptions.length > 1 ? Boolean.parseBoolean(classAndOptions[1].trim()) : false);
				int number = 1;
				if(classAndOptions.length > 2) {
					number = Integer.parseInt(classAndOptions[2].trim());
				}
				logger.finer("Launching "+number+ " instance(s) of "+className+" with GUI = "+withGUI);
				for (int i = 0; i < number; i++) {
						launchAgent(className, 0, withGUI);
				}
			}
			Thread.yield();//sufficient for threads to take place, may quit otherwise
		}
	}

	private void startSession(boolean externalVM) {
		if (logger != null)
			logger.info("starting MadKit session");
		if (externalVM) {
			try {
				Runtime.getRuntime().exec(platform.cmdLine);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} else {
			new Madkit(platform.args);
		}
	}

	private void stopNetwork() {
		ReturnCode r = sendNetworkMessageWithRole(new Message(), kernelRole);
		if (r == SUCCESS) {
			if (logger != null) logger.fine("\n\t****** Network agent stopped ******\n");
		}// TODO i18n
		else {
			if (logger != null) logger.fine("\n\t****** Network already down ******\n");
		}
	}

	private void startNetwork() {// TODO never use getLogger
		updateNetworkAgent();
		if (netAgent == null) {
			ReturnCode r = launchAgent(new NetworkAgent());
			if (r == SUCCESS) {
				if (logger != null) logger.fine("\n\t****** Network agent launched ******\n");
			}// TODO i18n
			else {
				if (logger != null) logger.severe("\n\t****** Problem launching network agent ******\n");
			}
		} else {
			if (logger != null) logger.fine("\n\t****** Network agent already up ******\n");
		}
	}

	private void restartSession(final int time) {
		new Thread() {
			public void run() {
				pause(time);
				// for (Object s : new TreeSet(System.getProperties().keySet())) {
				// System.err.println(s+" = "+System.getProperty((String) s));
				// }
				new Madkit(platform.args);
				// try {
				// Process p = Runtime.getRuntime().exec(platform.cmdLine);
				// pause(10000);
				// } catch (IOException e) {
				// e.printStackTrace();
				// }
			}
		}.start();
	}

	private void handleMessage(Message m) {
		if (m instanceof KernelMessage) {
			handleKernelMessage((KernelMessage) m);
		} else {
			if (logger != null) logger.warning("I received a message that I do not understand. Discarding " + m);
		}
	}

	/**
	 * @param operation
	 * @param arguments
	 */
	private void doOperation(Method operation, Object[] arguments) {
		try {// TODO log failures
			operation.invoke(this, arguments);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param content
	 * @return
	 */
	private Method launchAgent(Object[] content) {
		return checkValidity("launchAgent", content);
	}

	private Method checkValidity(String method, Object[] content) {
		Class<?>[] parameters = new Class<?>[content.length];
		for (int i = 0; i < content.length; i++) {
			parameters[i] = content[i].getClass();
			final Class<?> primitive = primitiveTypes.get(parameters[i].getName());
			if (primitive != null)
				parameters[i] = primitive;
		}
		try {// TODO log failures
			return getClass().getMethod(method, parameters);
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}
		return null;
	}

	//	private AbstractAgent launchPlatformAgent(String mkProperty, String userMessage) {
	//		final String agentClassName = getMadkitProperty(mkProperty);
	//		if (logger != null) {
	//			logger.fine("** Launching " + userMessage + ": " + agentClassName + " **");
	//		}
	//		AbstractAgent targetAgent = launchAgent(agentClassName);
	//		if (targetAgent == null) {
	//			if (logger != null) {
	//				logger.warning("Problem building " + userMessage + " " + agentClassName + " -> Using MK default " + userMessage
	//						+ " : " + Madkit.defaultConfig.get(mkProperty));
	//			}
	//			return launchAgent(Madkit.defaultConfig.getProperty(mkProperty));
	//		}
	//		return targetAgent;
	//	}

	private void launchNetworkAgent() {
		if (platform.isOptionActivated(Madkit.network)) {
			startNetwork();
		} else {
			if(logger != null)
				logger.fine("** Networking is off: No Net Agent **\n");
		}
	}

	// /////////////////////////////////////: Kernel Part

	/**
	 * @return the loggedKernel
	 */
	final LoggedKernel getLoggedKernel() {
		return loggedKernel;
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Agent interface
	// /////////////////////////////////////////////////////////////////////////

	// ////////////////////////////////////////////////////////////
	// //////////////////////// Organization interface
	// ////////////////////////////////////////////////////////////

	ReturnCode createGroup(final AbstractAgent creator, final String community, final String group, final String description,
			final GroupIdentifier theIdentifier, final boolean isDistributed) {
		// no need to remove org: never failed
		//will throw null pointer if community is null
		Organization organization = new Organization(community, this);
		if(group == null)
			throw new NullPointerException("group's name is null");
		final Organization tmpOrg = organizations.putIfAbsent(community, organization);
		if (tmpOrg != null) {
			organization = tmpOrg;
		}
		if (!organization.addGroup(creator, group,theIdentifier,isDistributed)) {
			return ALREADY_GROUP;
		}
		if (isDistributed) {
			try {
				sendNetworkMessageWithRole(new CGRSynchro(CREATE_GROUP, getRole(community, group, Roles.GROUP_MANAGER_ROLE)
						.getAgentAddressOf(creator)), netUpdater);
			} catch (CGRNotAvailable e) {
				getLogger().severeLog("Please bug report", e);
			}
		}
		return SUCCESS;
	}

	/**
	 * @param requester
	 * @param roleName
	 * @param groupName
	 * @param community
	 * @param memberCard
	 * @throws RequestRoleException
	 */

	ReturnCode requestRole(AbstractAgent requester, String community, String group, String role, Object memberCard) {
		final Group g;
		try {
			g = getGroup(community, group);
		} catch (CGRNotAvailable e) {
			return e.getCode();
		}
		final ReturnCode result = g.requestRole(requester, role, memberCard);
		if (g.isDistributed() && result == SUCCESS) {
			sendNetworkMessageWithRole(new CGRSynchro(REQUEST_ROLE, g.get(role).getAgentAddressOf(requester)), netUpdater);
		}
		return result;
	}

	/**
	 * @param abstractAgent
	 * @param communityName
	 * @param group
	 * @return
	 */

	ReturnCode leaveGroup(final AbstractAgent requester, final String community, final String group) {
		final Group g;
		try {
			g = getGroup(community, group);
		} catch (CGRNotAvailable e) {
			return e.getCode();
		}
		final ReturnCode result = g.leaveGroup(requester);
		if (g.isDistributed() && result == SUCCESS) {
			sendNetworkMessageWithRole(new CGRSynchro(LEAVE_GROUP, new AgentAddress(requester, new Role(community, group),
					kernelAddress)), netUpdater);
		}
		return result;
	}

	/**
	 * @param abstractAgent
	 * @param community
	 * @param group
	 * @param role
	 * @return
	 */

	ReturnCode leaveRole(AbstractAgent requester, String community, String group, String role) {
		final Role r;
		try {
			r = getRole(community, group, role);
		} catch (CGRNotAvailable e) {
			return e.getCode();
		}
		//this is apart because I need the address before the leave
		if (r.getMyGroup().isDistributed()) {
			AgentAddress leaver = r.getAgentAddressOf(requester);
			if (leaver == null)
				return ReturnCode.ROLE_NOT_HANDLED;
			if (r.removeMember(requester) != SUCCESS)
				throw new AssertionError("cannot remove " + requester + " from " + r.buildAndGetAddresses());
			sendNetworkMessageWithRole(new CGRSynchro(LEAVE_ROLE, leaver), netUpdater);
			return SUCCESS;
		}
		return r.removeMember(requester);
	}

	// Warning never touch this without looking at the logged kernel
	List<AgentAddress> getAgentsWithRole(AbstractAgent requester, String community, String group, String role, boolean callerIncluded) {
		try {
			if (callerIncluded) {
				return getOtherRolePlayers(requester, community, group, role);
			}
			else{
				return getRole(community, group, role).getAgentAddressesCopy();
			}
		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	AgentAddress getAgentWithRole(AbstractAgent requester, String community, String group, String role) {
		try {
			return getAnotherRolePlayer(requester, community, group, role);
		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	// ////////////////////////////////////////////////////////////
	// //////////////////////// Messaging interface
	// ////////////////////////////////////////////////////////////

	ReturnCode sendMessage(final AbstractAgent requester, final String community, final String group, final String role,
			final Message message, final String senderRole) {
		if (message == null) {
			return INVALID_ARG;
		}
		AgentAddress receiver, sender;
		try {
			receiver = getAnotherRolePlayer(requester, community, group, role);
			if (receiver == null) {
				return NO_RECIPIENT_FOUND;
			}
			sender = getSenderAgentAddress(requester, receiver, senderRole);
		} catch (CGRNotAvailable e) {
			return e.getCode();
		}
		return buildAndSendMessage(sender, receiver, message);
	}

	ReturnCode sendMessage(AbstractAgent requester, AgentAddress receiver, final Message message, final String senderRole) {
		if (receiver == null || message == null) {
			return INVALID_ARG;
		}
		// check that the AA is valid : the targeted agent is still playing the
		// corresponding role or it was a candidate request
		if (!receiver.exists()) {// && !
			// receiver.getRole().equals(Roles.GROUP_CANDIDATE_ROLE)){
			return INVALID_AA;
		}
		// get the role for the sender
		AgentAddress sender;
		try {
			sender = getSenderAgentAddress(requester, receiver, senderRole);
		} catch (CGRNotAvailable e) {
			return e.getCode();
		}
		return buildAndSendMessage(sender, receiver, message);
	}

	ReturnCode sendReplyWithRole(final AbstractAgent requester, final Message messageToReplyTo, final Message reply,// TODO
			String senderRole) {
		if (messageToReplyTo == null || reply == null) {
			return INVALID_ARG;
		}
		reply.setID(messageToReplyTo.getConversationID());
		return sendMessage(requester, messageToReplyTo.getSender(), reply, senderRole);
	}

	ReturnCode broadcastMessageWithRole(final AbstractAgent requester, final String community, final String group,
			final String role, final Message messageToSend, String senderRole) {
		if (messageToSend == null)
			return INVALID_ARG;
		try {
			final List<AgentAddress> receivers = getOtherRolePlayers(requester, community, group, role);
			if (receivers == null)
				return NO_RECIPIENT_FOUND; // the requester is the only agent in
			// this group
			messageToSend.setSender(getSenderAgentAddress(requester, receivers.get(0), senderRole));
			broadcasting(receivers, messageToSend);
			return SUCCESS;
		} catch (CGRNotAvailable e) {
			return e.getCode();
		}
	}

	List<Message> broadcastMessageWithRoleAndWaitForReplies(final AbstractAgent abstractAgent, final String community,
			final String group, final String role, Message message, final String senderRole, final Integer timeOutMilliSeconds) {
		try {
			final List<AgentAddress> receivers = getOtherRolePlayers(abstractAgent, community, group, role);
			if (message == null || receivers == null)
				return null; // the requester is the only agent in this group
			message.setSender(getSenderAgentAddress(abstractAgent, receivers.get(0), senderRole));
			broadcasting(receivers, message);
			return abstractAgent.waitAnswers(message, receivers.size(), timeOutMilliSeconds);
		} catch (CGRNotAvailable e) {
			return null;
		}
	}

	void broadcasting(final Collection<AgentAddress> receivers, Message m) {//TODO optimize without cloning
		for (final AgentAddress agentAddress : receivers) {
			if (agentAddress != null) {// TODO this should not be possible
				m = m.clone();
				m.setReceiver(agentAddress);
				sendMessage(m);
			}
		}
	}

	ReturnCode sendMessage(Message m) {
		final AbstractAgent target = m.getReceiver().getAgent();
		if (target == null) {
			return sendNetworkMessageWithRole(new ObjectMessage<Message>(m), netEmmiter);
		} else {
			target.receiveMessage(m);
		}
		return SUCCESS;
	}

	ReturnCode sendNetworkMessageWithRole(Message m, AgentAddress role) {
		updateNetworkAgent();
		if (netAgent != null) {
			m.setSender(role);
			m.setReceiver(netAgent);
			netAgent.getAgent().receiveMessage(m);
			return SUCCESS;
		}
		return NETWORK_DOWN;
	}

	private void updateNetworkAgent() {
		if (netAgent == null || !netAgent.exists()) {// Is it still playing the
			// role ?
			netAgent = getAgentWithRole(LOCAL_COMMUNITY, NETWORK_GROUP, NETWORK_ROLE);
		}
	}

	boolean isOnline() {
		getMadkitKernel().updateNetworkAgent();
		return getMadkitKernel().netAgent != null;
	}

	// ////////////////////////////////////////////////////////////
	// //////////////////////// Launching and Killing
	// ////////////////////////////////////////////////////////////
	@SuppressWarnings("unchecked")
	synchronized List<AbstractAgent> launchAgentBucketWithRoles(final AbstractAgent requester, String agentClassName,
			int bucketSize, Collection<String> CGRLocations) {
		Class<? extends AbstractAgent> agentClass = null;
		try {//TODO put that in the cl
			agentClass = (Class<? extends AbstractAgent>) platform.getMadkitClassLoader().loadClass(agentClassName);
		} catch (ClassCastException e) {
				requester.getLogger().severe("Cannot launch " + agentClassName + " because it is not an agent class");
			return null;
		} catch (ClassNotFoundException e) {
				requester.getLogger().severe("Cannot launch " + agentClassName + " because the class has not been found");
			return null;
		}
		final ArrayList<AbstractAgent> bucket = createBucket(agentClass, bucketSize);
		if (CGRLocations != null) {
			for (final String cgrLocation : CGRLocations) {
				final String[] cgr = cgrLocation.split(";");
				if (cgr.length != 3)
					return null;// TODO logging
				createGroup(requester, cgr[0], cgr[1], null, null, false);
				Group g = null;
				try {
					g = getGroup(cgr[0], cgr[1]);
				} catch (CGRNotAvailable e) {
					e.printStackTrace();
					return Collections.emptyList();
				}
				boolean roleCreated = false;
				Role r = g.get(cgr[2]);
				if (r == null) {
					r = g.createRole(cgr[2]);
					g.put(r.getRoleName(), r);
					roleCreated = true;
				}
				r.addMembers(bucket, roleCreated);
				// test vs assignement ? -> No: cannot touch the organizational
				// structure !!
			}
		}
		for (final AbstractAgent a : bucket) {
			a.activation();
		}
		return bucket;
	}

	private ArrayList<AbstractAgent> createBucket(final Class<? extends AbstractAgent> agentClass, int bucketSize) {
		final int cpuCoreNb = serviceExecutor.getCorePoolSize();
		final ArrayList<AbstractAgent> result = new ArrayList<AbstractAgent>(bucketSize);
		final int nbOfAgentsPerTask = bucketSize / (cpuCoreNb);
		// System.err.println("nb of ag per task "+nbOfAgentsPerTask);
		CompletionService<ArrayList<AbstractAgent>> ecs = new ExecutorCompletionService<ArrayList<AbstractAgent>>(serviceExecutor);
		final ArrayList<Callable<ArrayList<AbstractAgent>>> workers = new ArrayList<Callable<ArrayList<AbstractAgent>>>(cpuCoreNb);
		for (int i = 0; i < cpuCoreNb; i++) {
			workers.add(new Callable<ArrayList<AbstractAgent>>() {

				public ArrayList<AbstractAgent> call() throws Exception {
					final ArrayList<AbstractAgent> list = new ArrayList<AbstractAgent>(nbOfAgentsPerTask);
					for (int i = nbOfAgentsPerTask; i > 0; i--) {
						list.add(initAbstractAgent(agentClass));
					}
					return list;
				}
			});
		}
		for (final Callable<ArrayList<AbstractAgent>> w : workers)
			ecs.submit(w);
		int n = workers.size();
		for (int i = 0; i < n; ++i) {
			try {
				result.addAll(ecs.take().get());
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		}
		for (int i = bucketSize - nbOfAgentsPerTask * cpuCoreNb; i > 0; i--) {
			// System.err.println("adding aone");
			result.add(initAbstractAgent(agentClass));
		}
		// System.err.println(result.size());
		return result;
	}

	private AbstractAgent initAbstractAgent(final Class<? extends AbstractAgent> agentClass) {
		final AbstractAgent a;
		try {
			a = agentClass.newInstance();
		} catch (InstantiationException e) {
			e.printStackTrace();
			return null;
		} catch (IllegalAccessException e) {
			e.printStackTrace();
			return null;
		}
		a.state.set(INITIALIZING); // no need to test : I created these
		// instances
		a.setKernel(this);
		a.getAlive().set(true);
		a.logger = null;
		return a;
	}

//	AbstractAgent launchAgent(AbstractAgent requester, final String agentClass, int timeOutSeconds, boolean defaultGUI) throws ClassNotFoundException {
//		try {
//			final AbstractAgent agent = getAgentClass(requester, agentClass).newInstance();
//			if (launchAgent(requester, agent, timeOutSeconds, defaultGUI) == AGENT_CRASH) {
//				return null; // TODO when time out ?
//			}
//			return agent;
////		} catch (ClassNotFoundException e) {
////				requester.getLogger().severe("Cannot launch " + agentClass + " "+e.getMessage());
//		} catch (InstantiationException e) {
//			final String msg = "Cannot launch " + agentClass + " because it has no default constructor";
//			SwingUtilities.invokeLater(new Runnable() {
//				public void run() {
//					JOptionPane.showMessageDialog(null, msg,
//							"Launch failed", JOptionPane.WARNING_MESSAGE);
//				}
//			});
//			requester.getLogger().severe(msg);
//		} catch (IllegalAccessException e) {
//			requester.getLogger().severe("Cannot launch " + agentClass + " "+e.getMessage());
//		}
//		return null;
//	}
//	
//	@SuppressWarnings("unchecked")
//	Class<? extends AbstractAgent> getAgentClass(AbstractAgent requester, String className) throws ClassNotFoundException{
//		try {
//			return (Class<? extends AbstractAgent>) platform.getMadkitClassLoader().loadClass(className);
//		} catch (ClassCastException e) {
//			requester.getLogger().severe("Cannot launch " + className + " : not an agent class");
//			return null;
//		}
//	}

	ReturnCode launchAgent(final AbstractAgent requester, final AbstractAgent agent, final int timeOutSeconds,
			final boolean defaultGUI) {
		if (agent == null || timeOutSeconds < 0)
			return INVALID_ARG;
		try {
			// if to == 0, this is still quicker than treating the case, this also holds for Integer.MAX_VALUE
			return serviceExecutor.submit(new Callable<ReturnCode>() {
				public ReturnCode call() {
					return launchingAgent(agent, defaultGUI);
				}
			}).get(timeOutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {// requester has been killed or something
			throw new KilledException(e);
		} catch (ExecutionException e) {// target has crashed !
			if(logger != null)
				logger.log(Level.FINEST, "Launch failed on " + agent, e);
			if (e.getCause() instanceof AssertionError)// convenient for Junit
				throw new AssertionError(e);
			return AGENT_CRASH;
		} catch (TimeoutException e) {// launch task time out
			return LAUNCH_TIME_OUT;
		}
	}

	private ReturnCode launchingAgent(final AbstractAgent agent, boolean defaultGUI) {
		// this has to be done by a system thread
		if (!agent.state.compareAndSet(NOT_LAUNCHED, INITIALIZING) || shuttedDown) {
			return ALREADY_LAUNCHED;
		}
		if(defaultGUI)
			agent.activateGUI();
		agent.setKernel(this);
		Level defaultLevel = Level.parse(getMadkitProperty(this, Madkit.agentLogLevel));
		if (defaultLevel == Level.OFF && agent.logger == AbstractAgent.defaultLogger) {
			agent.logger = null;
		} else if (defaultLevel != Level.OFF && agent.logger == AbstractAgent.defaultLogger) {
			agent.setLogLevel(defaultLevel);
			agent.getLogger().setWarningLogLevel(Level.parse(getMadkitProperty(this, Madkit.warningLogLevel)));
		}
		if (!agent.getAlive().compareAndSet(false, true)) {// TODO remove that
			throw new AssertionError("already alive in launch");
		}
		final AgentExecutor ae = agent.getAgentExecutor();
		if (ae == null) {
			return agent.activation() ? SUCCESS : AGENT_CRASH;
		}
		try {
			final Agent a = (Agent) agent;
			ae.setThreadFactory(a.isDaemon() ? daemonAgentThreadFactory : normalAgentThreadFactory);
			if (! shuttedDown && ae.start().get()){
				threadedAgents.add(a);
				return SUCCESS;
			}
			else{
				return AGENT_CRASH;
			}
		} catch (InterruptedException e) {
			if (!shuttedDown) {
				// Kernel cannot be interrupted !!
				bugReport(e); 
				return SEVERE;
			}
		} catch (ExecutionException e) {
			if (!shuttedDown) {
				// Kernel cannot be interrupted !!
				bugReport(e); 
				return SEVERE;
			}
		}  catch (CancellationException e) {
			//This is the case when the agent is killed during activation
			return AGENT_CRASH;
		}
		return LAUNCH_TIME_OUT;
	}

	ReturnCode killAgent(final AbstractAgent requester, final AbstractAgent target, final int timeOutSeconds) {
		if (target == null || timeOutSeconds < 0)
			return INVALID_ARG;
		if (target.getState().compareTo(ACTIVATED) < 0) {
			return NOT_YET_LAUNCHED;
		}
		final Future<ReturnCode> killAttempt = serviceExecutor.submit(new Callable<ReturnCode>() {
			public ReturnCode call() {
				return killingAgent(target);
			}
		});
		try {
			return killAttempt.get(timeOutSeconds, TimeUnit.SECONDS);
		} catch (InterruptedException e) {// requester has been killed or
			// something
			throw new KilledException(e);
		} catch (ExecutionException e) {// target has crashed in end !
			if(logger != null)
				logger.log(Level.FINE, "kill failed on " + target, e);
			if (e.getCause() instanceof AssertionError)
				throw new AssertionError(e);
			return AGENT_CRASH;
		} catch (TimeoutException e) {// kill task time out
			return LAUNCH_TIME_OUT;
		}
	}

	final ReturnCode killingAgent(final AbstractAgent target) {// TODO avec
		// timeout
		// this has to be done by a system thread
		if (!target.getAlive().compareAndSet(true, false)) {
			return ALREADY_KILLED;
		}
		if (target.getAgentExecutor() != null) {
			killThreadedAgent((Agent) target);
			return SUCCESS;
		}
		target.ending();
		target.terminate();
		return SUCCESS;
	}

	private void killThreadedAgent(final Agent target) {
		target.myThread.setPriority(Thread.MIN_PRIORITY);
		AgentExecutor ae = target.getAgentExecutor();
		ae.getLiveProcess().cancel(true);
		ae.getActivate().cancel(true);
		try {
			ae.getEndProcess().get();
			ae.awaitTermination(1, TimeUnit.SECONDS);
		} catch (Throwable e) {
			if (!shuttedDown) {
				bugReport(e);
			}
		}
		// final List<Future<Boolean>> lifeCycle = target.getMyLifeCycle();
		// lifeCycle.get(1).cancel(true);
		// lifeCycle.get(0).cancel(true);
		// try {
		// // JOptionPane.showMessageDialog(null, "coucou");
		// lifeCycle.get(2).get(); // waiting that end ends with to
		// } catch (CancellationException e) {
		// kernelLog("wired", Level.SEVERE, e);
		// } catch (InterruptedException e) {
		// } catch (ExecutionException e) {// agent crashed in end
		// kernelLog("agent crashed in ", Level.SEVERE, e);
		// }
		// try {
		// lifeCycle.get(3).get();
		// } catch (InterruptedException e) {
		// kernelLog("wired bug report", Level.SEVERE, e);
		// } catch (ExecutionException e) {
		// kernelLog("wired bug report", Level.SEVERE, e);
		// }
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Organization access
	// /////////////////////////////////////////////////////////////////////////
	Organization getCommunity(final String community) throws CGRNotAvailable {
		if (community == null)
			throw new CGRNotAvailable(NOT_COMMUNITY);
		Organization org = organizations.get(community);
		if (org == null)
			throw new CGRNotAvailable(NOT_COMMUNITY);
		return org;
	}

	Group getGroup(final String community, final String group) throws CGRNotAvailable {
		Organization o = getCommunity(community);
		if (group == null)
			throw new CGRNotAvailable(NOT_GROUP);
		Group g = o.get(group);
		if (g == null)
			throw new CGRNotAvailable(NOT_GROUP);
		return g;
	}

	Role getRole(final String community, final String group, final String role) throws CGRNotAvailable {
		Group g = getGroup(community, group);// get group before for warning
		// coherency
		if (role == null)
			throw new CGRNotAvailable(NOT_ROLE);
		Role r = g.get(role);
		if (r == null)
			throw new CGRNotAvailable(NOT_ROLE);
		return r;
	}

	/**
	 * @param abstractAgent
	 * @param community
	 * @param group
	 * @param role
	 * @return null if nobody is found
	 * @throws CGRNotAvailable
	 *            if one of community, group or role does not exist
	 */
	List<AgentAddress> getOtherRolePlayers(AbstractAgent abstractAgent, String community, String group, String role)
	throws CGRNotAvailable {
		// never null without throwing Ex
		final List<AgentAddress> result = getRole(community, group, role).getAgentAddressesCopy();
		Role.removeAgentAddressOf(abstractAgent, result);
		if (!result.isEmpty()) {
			return new ArrayList<AgentAddress>(result);
		}
		return null;
	}

	AgentAddress getAnotherRolePlayer(AbstractAgent abstractAgent, String community, String group, String role)
	throws CGRNotAvailable {
		List<AgentAddress> others = getOtherRolePlayers(abstractAgent, community, group, role);
		if (others != null) {
			return others.get((int) (Math.random() * others.size()));
		}
		return null;
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Messaging
	// /////////////////////////////////////////////////////////////////////////

	private ReturnCode buildAndSendMessage(final AgentAddress sender, final AgentAddress receiver, final Message m) {
		m.setSender(sender);
		m.setReceiver(receiver);
		return sendMessage(m);
		// final AbstractAgent target = receiver.getAgent();
		// if(target == null){
		// if(netAgent != null)
		// netAgent.receiveMessage(new MessageConveyor(m));
		// else
		// return NETWORK_DOWN;
		// }
		// else{
		// target.receiveMessage(m);
		// }
		// return SUCCESS;
	}

	final AgentAddress getSenderAgentAddress(final AbstractAgent sender, final AgentAddress receiver, String senderRole)
	throws CGRNotAvailable {
		AgentAddress senderAA = null;
		final Role targetedRole = receiver.getRoleObject();
		if (senderRole == null) {// looking for any role in this group, starting
			// with the receiver role
			senderAA = targetedRole.getAgentAddressInGroup(sender);
			// if still null : this SHOULD be a candidate's request to the
			// manager or it is an error
			if (senderAA == null) {
				if (targetedRole.getRoleName().equals(Roles.GROUP_MANAGER_ROLE))
					return new CandidateAgentAddress(sender, targetedRole, kernelAddress);
				else
					throw new CGRNotAvailable(NOT_IN_GROUP);
			}
			return senderAA;
		}
		// the sender explicitly wants to send the message with a particular
		// role : check that
		else {
			// look into the senderRole role if the agent is in
			final Role senderRoleObject = targetedRole.getMyGroup().get(senderRole);
			if (senderRoleObject != null) {
				senderAA = senderRoleObject.getAgentAddressOf(sender);
			}
			if (senderAA == null) {// if still null : this SHOULD be a
				// candidate's request to the manager or it
				// is an error
				if (senderRole.equals(Roles.GROUP_CANDIDATE_ROLE) && targetedRole.getRoleName().equals(Roles.GROUP_MANAGER_ROLE))
					return new CandidateAgentAddress(sender, targetedRole, kernelAddress);
				if (targetedRole.getAgentAddressInGroup(sender) == null)
					throw new CGRNotAvailable(NOT_IN_GROUP);
				else
					throw new CGRNotAvailable(ROLE_NOT_HANDLED);
			}
			return senderAA;
		}
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Simulation
	// /////////////////////////////////////////////////////////////////////////

	boolean addOverlooker(final AbstractAgent requester, Overlooker<? extends AbstractAgent> o) {
		if (operatingOverlookers.add(o)) {
			try {
				getRole(o.getCommunity(), o.getGroup(), o.getRole()).addOverlooker(o);
			} catch (CGRNotAvailable e) {
			}
			return true;
		}
		return false;
	}

	/**
	 * @param scheduler
	 * @param activator
	 */

	boolean removeOverlooker(final AbstractAgent requester, Overlooker<? extends AbstractAgent> o) {
		final Role r = o.getOverlookedRole();
		if (r != null) {
			r.removeOverlooker(o);
		}
		return operatingOverlookers.remove(o);
	}

	// /////////////////////////////////////////////////////////////////////////
	// //////////////////////// Internal functioning
	// /////////////////////////////////////////////////////////////////////////

	void removeCommunity(String community) {
		organizations.remove(community);
	}

	Class<?> getNewestClassVersion(AbstractAgent requester, String className) throws ClassNotFoundException {
		return platform.getMadkitClassLoader().loadClass(className);
	}

	@Override
	public KernelAddress getKernelAddress() {
		return kernelAddress;
	}

	/**
	 * @return
	 */
	Set<Overlooker<? extends AbstractAgent>> getOperatingOverlookers() {
		return operatingOverlookers;
	}

	/**
	 * @param abstractAgent
	 */

	void removeAgentFromOrganizations(AbstractAgent theAgent) {
		for (final Organization org : organizations.values()) {
			final ArrayList<String> groups = org.removeAgentFromAllGroups(theAgent);
			for (final String groupName : groups) {
				sendNetworkMessageWithRole(new CGRSynchro(LEAVE_GROUP, new AgentAddress(theAgent, new Role(org.getName(), groupName),
						kernelAddress)), netUpdater);
			}
		}
	}

	String getMadkitProperty(AbstractAgent abstractAgent, String key) {
		return platform.getConfigOption().getProperty(key);
	}

	void setMadkitProperty(final AbstractAgent requester, String key, String value) {
		platform.checkAndValidateOption(key, value);// TODO update agent logging
		// on or off
	}

	MadkitKernel getMadkitKernel() {
		return this;
	}

	/**
	 * Asks MasKit to reload the class byte code so that new instances, created
	 * using {@link Class#newInstance()} on a class object obtained with
	 * {@link #getNewestClassVersion(AbstractAgent, String)}, will reflect
	 * compilation changes during run time.
	 * 
	 * @param requester
	 * @param name
	 *           The fully qualified class name of the class
	 * @throws ClassNotFoundException
	 */

	ReturnCode reloadClass(AbstractAgent requester, String name) throws ClassNotFoundException {
		if (name == null)
			throw new ClassNotFoundException(ReturnCode.CLASS_NOT_FOUND + " " + name);
		if (!name.contains("madkit.kernel") && !name.contains("madkit.gui") && !name.contains("madkit.messages")
				&& !name.contains("madkit.simulation") && platform.getMadkitClassLoader().reloadClass(name))
			return SUCCESS;
		return ReturnCode.CLASS_NOT_FOUND;// TODO not the right code here
	}

	boolean isCommunity(AbstractAgent requester, String community) {
		try {
			return getCommunity(community) != null;
		} catch (CGRNotAvailable e) {
			return false;
		}
	}

	boolean isGroup(AbstractAgent requester, String community, String group) {
		try {
			return getGroup(community, group) != null;
		} catch (CGRNotAvailable e) {
			return false;
		}
	}

	boolean isRole(AbstractAgent requester, String community, String group, String role) {
		try {
			return getRole(community, group, role) != null;
		} catch (CGRNotAvailable e) {
			return false;
		}
	}

	synchronized void importDistantOrg(SortedMap<String, SortedMap<String, SortedMap<String, Set<AgentAddress>>>> distantOrg) {
		for (String communityName : distantOrg.keySet()) {
			Organization org = new Organization(communityName, this);
			Organization previous = organizations.putIfAbsent(communityName, org);
			if (previous != null) {
				org = previous;
			}
			org.importDistantOrg(distantOrg.get(communityName));
		}
	}

	@Override
	public Map<String, Map<String, Map<String, Set<AgentAddress>>>> getOrganizationSnapShot(boolean global) {
		Map<String, Map<String, Map<String, Set<AgentAddress>>>> export = new TreeMap<String, Map<String, Map<String, Set<AgentAddress>>>>();
		for (Map.Entry<String, Organization> org : organizations.entrySet()) {
			Map<String, Map<String, Set<AgentAddress>>> currentOrg = org.getValue().getOrgMap(global);
			if (!currentOrg.isEmpty())
				export.put(org.getKey(), org.getValue().getOrgMap(global));
		}
		return export;
	}

	@Override
	public URLClassLoader getMadkitClassLoader() {
		return platform.getMadkitClassLoader();
	}

	// void logCurrentOrganization(Logger requester, Level lvl){
	// if(requester != null){
	// String message = "Current organization is\n";
	// if(organizations.isEmpty()){
	// message+="\n ------------ EMPTY !! ------------\n";
	// }
	// for(final Map.Entry<String, Organization> org :
	// organizations.entrySet()){
	// message+="\n\n--"+org.getKey()+"----------------------";
	// for(final Map.Entry<String, Group> group : org.getValue().entrySet()){
	// // final AgentAddress manager = group.getValue().getManager().get();
	// message+="\n|--"+group.getKey()+"--";// managed by
	// ["+manager.getAgent()+"] "+manager+" --\n";
	// for(final Map.Entry<String, Role> role: group.getValue().entrySet()){
	// message+="\n||--"+role.getKey()+"--";
	// message+="\n|||--players- "+role.getValue().getPlayers();
	// message+="\n|||--addresses- = "+role.getValue().getAgentAddresses();
	// }
	// }
	// message+="\n-----------------------------";
	// }
	// requester.log(lvl, message+"\n");
	// }
	// }

	final void injectMessage(final ObjectMessage<Message> m) {
		final Message toInject = m.getContent();
		final AgentAddress receiver = toInject.getReceiver();
		final AgentAddress sender = toInject.getSender();
		try {
			final Role receiverRole = kernel.getRole(receiver.getCommunity(), receiver.getGroup(), receiver.getRole());
			receiver.setRoleObject(receiverRole);
			if (receiverRole != null) {
				final AbstractAgent target = receiverRole.getAbstractAgentWithAddress(receiver);
				if (target != null) {
					//updating sender address
					sender.setRoleObject(kernel.getRole(sender.getCommunity(), sender.getGroup(), sender.getRole()));
					target.receiveMessage(toInject);
				}
				else if (logger != null)
					logger.finer(m+" received but the agent address is no longer valid !! Current distributed org is "
							+ getOrganizationSnapShot(false));
			}
		} catch (CGRNotAvailable e) {
			kernel.bugReport("Cannot inject "+m+"\n"+getOrganizationSnapShot(false),e);
		}
	}

	final void injectOperation(CGRSynchro m) {
		final Role r = m.getContent().getRoleObject();
		final String communityName = r.getCommunityName();
		final String groupName = r.getGroupName();
		final String roleName = r.getRoleName();
		if (logger != null)
			logger.finer("distant CGR " + m.getCode() + " on " + m.getContent());
		try {
			switch (m.getCode()) {
			case CREATE_GROUP:
				//nerver fails : no need to remove org
				Organization organization = new Organization(communityName, this);// no
				final Organization tmpOrg = organizations.putIfAbsent(communityName, organization);
				if (tmpOrg != null) {
					if (isGroup(communityName, groupName)) {
						if (logger != null)
							logger.finer("distant group creation by " + m.getContent() + " aborted : already exists locally");//TODO what about the manager
						break;
					}
					organization = tmpOrg;
				}
				organization.put(groupName, new Group(communityName, groupName, m.getContent(), null, organization));
				break;
			case REQUEST_ROLE:
				getGroup(communityName, groupName).addDistantMember(m.getContent());
				break;
			case LEAVE_ROLE:
				getRole(communityName, groupName, roleName).removeDistantMember(m.getContent());
				break;
			case LEAVE_GROUP:
				getGroup(communityName, groupName).removeDistantMember(m.getContent());
				break;
				// case CGRSynchro.LEAVE_ORG://TODO to implement
				// break;
			default:
				break;
			}
		} catch (CGRNotAvailable e) {
			if(logger != null)
				logger.log(Level.FINE, "distant CGR " + m.getCode() + " update failed on " + m.getContent(), e);
		}
	}

	synchronized void shutdown() {
		if(System.getProperty("javawebstart.version") != null){
			System.exit(0);
		}
		shuttedDown = true;
		pause(10);//be sure that last executors have started
		if (logger != null)
			logger.finer("***** SHUTINGDOWN MADKIT ********\n");
		//		if (logger != null) {
		//			if (logger.getLevel().intValue() <= Level.FINER.intValue()) {
		//				normalAgentThreadFactory.getThreadGroup().list();
		//			}
		//		}
		broadcastMessageWithRole(MadkitKernel.this, LOCAL_COMMUNITY,
				SYSTEM_GROUP, GUI_MANAGER_ROLE, new GUIMessage(MadkitActions.MADKIT_EXIT_ACTION, MadkitKernel.this), null);// TODO
		killAgents(true);
		pause(100);
		killAgents(false);
		if (logger != null)
			logger.talk(platform.printFareWellString());
	}

	@SuppressWarnings("deprecation")
	private void killAgents(boolean oneShot) {
		threadedAgents.remove(this);
		for(Agent a : new ArrayList<Agent>(threadedAgents)){
			killAgent(a,0);
		}
		if(oneShot || threadedAgents.isEmpty())
			return;
		pause(6000);
		for(Agent a : new ArrayList<Agent>(threadedAgents)){
			int stop = JOptionPane.showConfirmDialog(null, "force exit ?", a.getName() + " is not responding in "+a.getState(),
					JOptionPane.YES_NO_OPTION);
			if (stop == JOptionPane.OK_OPTION) {
				a.getAgentExecutor().shutdownNow();
				try {
					if(! a.getAgentExecutor().awaitTermination(2, TimeUnit.SECONDS)){
						a.myThread.stop();
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}

	boolean createGroupIfAbsent(AbstractAgent abstractAgent, String community, String group, String group2,
			GroupIdentifier theIdentifier, boolean isDistributed) {
		return createGroup(abstractAgent, community, group, group, theIdentifier, isDistributed) == SUCCESS;
	}

	void bugReport(Throwable e) {
		bugReport("", e);
	}

	void bugReport(String m, Throwable e) {
		kernel.getLogger().severeLog("********************** KERNEL PROBLEM, please bug report "+m, e); // Kernel
	}

	final synchronized void removeAgentsFromDistantKernel(KernelAddress kernelAddress2) {
		for (Organization org : organizations.values()) {
			org.removeAgentsFromDistantKernel(kernelAddress2);
		}
	}

	//	void removeThreadedAgent(Agent myAgent) {
	//		synchronized (threadedAgents) {
	//			threadedAgents.remove(myAgent);
	//		}
	//	}

	synchronized void destroyCommunity(AbstractAgent abstractAgent, String community) {
		if(isCommunity(community)){
			organizations.get(community).destroy();
		}
	}

	@Override
	final void terminate() {
	}

	void removeThreadedAgent(Agent myAgent) {
		threadedAgents.remove(myAgent);
	}

}

final class CGRNotAvailable extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -375379801933609564L;
	final ReturnCode code;

	/**
	 * @return the code
	 */
	final ReturnCode getCode() {
		return code;
	}

	/**
	 * @param notCommunity
	 */
	CGRNotAvailable(ReturnCode code) {
		this.code = code;
	}

//	@Override
//	public synchronized Throwable fillInStackTrace() {
//		return null;
//	}

}