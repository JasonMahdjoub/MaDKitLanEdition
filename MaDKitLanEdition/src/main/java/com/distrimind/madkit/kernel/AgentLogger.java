/*
 * Copyright 1997-2012 Fabien Michel, Olivier Gutknecht, Jacques Ferber
 * 
 * This file is part of MaDKit.
 * 
 * MaDKit is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * MaDKit is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with MaDKit. If not, see <http://www.gnu.org/licenses/>.
 */
package com.distrimind.madkit.kernel;

import com.distrimind.madkit.gui.menu.AgentLogLevelMenu;
import com.distrimind.madkit.i18n.Words;
import com.distrimind.util.FileTools;

import javax.swing.*;
import java.io.*;
import java.util.*;
import java.util.logging.Formatter;
import java.util.logging.*;

/**
 * This class defines a logger specialized for MaDKit agents.
 * 
 * @author Fabien Michel
 * @version 0.91
 * @since MaDKit 5.0.0.5
 * 
 */
final public class AgentLogger extends Logger {

	/**
	 * Defines the default formatter as : [agent's name] LOG_LEVEL : message
	 */
	final public static Formatter AGENT_FORMATTER = new AgentFormatter();
	/**
	 * Defines the default file formatter as : LOG_LEVEL : message
	 */
	final public static Formatter AGENT_FILE_FORMATTER = new AgentFormatter() {
		@Override
		protected void setHeader(StringBuilder s, LogRecord record) {

		}

	};
	final static AgentLogger defaultAgentLogger = new AgentLogger();

	final static Level talkLevel = Level.parse("1100");
	final static private Map<AbstractAgent, AgentLogger> agentLoggers = new HashMap<>(); // TODO evaluate foot
																									// print

	private FileHandler fh;

	final private AbstractAgent myAgent;

	private Level warningLogLevel = Madkit.getDefaultConfig().warningLogLevel;

	static AgentLogger getLogger(AbstractAgent agent) {
		if (agent instanceof MadkitKernel)
			agent=agent.getMadkitKernel();
		synchronized (agentLoggers) {
			AgentLogger al = agentLoggers.get(agent);
			if (al == null) {

				al = new AgentLogger(agent);
				agentLoggers.put(agent, al);
				al.setWarningLogLevel(agent.getMadkitConfig().warningLogLevel);
			}
			return al;
		}
	}


	static void removeLoggers(MadkitKernel kernel)
	{
		synchronized (agentLoggers) {
			kernel=kernel.getMadkitKernel();
			for (Iterator<Map.Entry<AbstractAgent, AgentLogger>> it = agentLoggers.entrySet().iterator(); it.hasNext(); ) {
				Map.Entry<AbstractAgent, AgentLogger> e = it.next();
				if (e.getKey().getMadkitKernel() == kernel) {
					for (final Handler h : e.getValue().getHandlers()) {

						e.getValue().removeHandler(h);
						h.close();
					}

					it.remove();
				}
			}
		}
	}


	/**
	 * Returns the log level above which MaDKit warnings are displayed for the
	 * corresponding agent.
	 * 
	 * @return the warningLogLevel of the corresponding agent
	 */
	public Level getWarningLogLevel() {
		return warningLogLevel;
	}

	/**
	 * Sets the agent's log level above which MaDKit warnings are displayed
	 * 
	 * @param warningLogLevel
	 *            the log level to set
	 */
	public void setWarningLogLevel(final Level warningLogLevel) {
		if (warningLogLevel == null)
			throw new NullPointerException();
		this.warningLogLevel = warningLogLevel;
		AgentLogLevelMenu.update(myAgent);
	}

	private AgentLogger() {
		super("[UNREGISTERED AGENT]", null);
		myAgent = null;
		setUseParentHandlers(false);
		super.setLevel(Madkit.getDefaultConfig().agentLogLevel);
		if (!Madkit.getDefaultConfig().noAgentConsoleLog) {
			addHandler(new ConsoleHandler());
		}
	}

	private AgentLogger(final AbstractAgent agent) {
		super("[" + agent.getName() + "]", null);

		myAgent = agent;
		setUseParentHandlers(false);
		final MadkitProperties madkitConfig = myAgent.getMadkitConfig();
		final Level l = myAgent.logger == null ? Level.OFF : madkitConfig.agentLogLevel;
		super.setLevel(l);

		if (!madkitConfig.noAgentConsoleLog) {
			ConsoleHandler ch = new ConsoleHandler();
			addHandler(ch);
			ch.setFormatter(AGENT_FORMATTER);
		}

		if (madkitConfig.createLogFiles && agent.getMadkitKernel() != agent) {
			createLogFile();
		}
	}

	/**
	 * Creates a log file for this logger. This file will be located in the
	 * directory specified by the MaDKit property {@link MadkitProperties#logDirectory}, which
	 * is set to "logs" by default.
	 */
	public void createLogFile() {

		if (fh == null) {
			final File logDir = myAgent.getMadkitConfig().logDirectory;

			FileTools.checkFolder(logDir);

			final File logFile = new File(logDir, getName());
			final String lineSeparator = "----------------------------------------------------------------------\n";
			final String logSession = lineSeparator + "-- Log session for "
					+ logFile.toString().substring(logFile.toString().lastIndexOf(File.separator) + 1);
			final String logEnd = " --\n" + lineSeparator + "\n";
			final Date date = new Date();
			try (FileWriter fw = new FileWriter(logFile, true)) {
				fw.write(logSession + " started on " + myAgent.getMadkitKernel().platform.dateFormat.format(date) + logEnd);
				fh = new FileHandler(logFile.toString(), true) {
					public synchronized void close() throws SecurityException {
						super.close();
						try (FileWriter fw2 = new FileWriter(logFile, true)) {
							date.setTime(System.currentTimeMillis());
							fw2.write("\n\n" + logSession + " closed on  " + myAgent.getMadkitKernel().platform.dateFormat.format(date) + logEnd);
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				};
				fh.setFormatter(AGENT_FILE_FORMATTER);
			} catch (SecurityException | IOException e) {
				e.printStackTrace();
			}
			addHandler(fh);
		}
	}

	synchronized void close() {

		for (final Handler h : getHandlers()) {

			removeHandler(h);
			h.close();
		}
		synchronized (agentLoggers) {
			agentLoggers.remove(myAgent);
		}
	}

	@Override
	public void addHandler(final Handler handler) throws SecurityException {
		super.addHandler(handler);
		handler.setLevel(getLevel());
	}

	static void resetLoggers() {
		synchronized (agentLoggers) {
			for (final AgentLogger l : agentLoggers.values()) {
				for (final Handler h : l.getHandlers()) {

					l.removeHandler(h);
					h.close();
				}
			}
			agentLoggers.clear();
		}
	}

	/**
	 * Log a TALK message. This uses a special level which could be used to produce
	 * messages that will be rendered as they are, without any formatting work nor
	 * end-of-line character.
	 * <p>
	 * If the logger's level is not {@link Level#OFF} then the given message is
	 * forwarded to all the registered output Handler objects.
	 * <p>
	 * If the logger's level is {@link Level#OFF} then the message is only printed
	 * to {@link System#out}
	 * 
	 * @param msg
	 *            The string message
	 */
	public void talk(final String msg) {
		if (getLevel() == Level.OFF)
			System.out.print(msg);
		else
			log(talkLevel, msg);
	}

	/**
	 * Set the log level for the corresponding agent.
	 */
	@Override
	public void setLevel(final Level newLevel) throws SecurityException {
		if (newLevel == null)
			throw new NullPointerException();
		super.setLevel(newLevel);
		for (Handler h : getHandlers()) {
			h.setLevel(newLevel);
		}
		if (myAgent.hasGUI()) {
			AgentLogLevelMenu.update(myAgent);
			// updateAgentUi();//TODO level model
		}
	}

	@Override
	public String toString() {
		return getName() + " logger: \n\tlevel " + getLevel() + "\n\twarningLogLevel " + getWarningLogLevel();
	}

	@Override
	public void log(final LogRecord record) {

		Throwable t = record.getThrown();
		if (t != null) {
			try {
				final StringWriter sw = new StringWriter();
				final PrintWriter pw = new PrintWriter(sw);
				t.printStackTrace(pw);

				pw.close();
				sw.close();
				record.setMessage(record.getMessage() + "\n ** " + sw);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		super.log(record);
	}

	/**
	 * This call bypasses any settings and always produces severe log messages which
	 * display the stack trace of the throwable if it is not <code>null</code>
	 * 
	 * @param msg
	 *            the message to display
	 * @param t
	 *            the related exception if any. It can be <code>null</code>
	 */
	public void severeLog(final String msg, final Throwable t) {
		// This will also be logged by the kernel at FINEST
		final Logger l = myAgent.getMadkitKernel().logger;
		if (l != null) {
			l.log(Level.FINEST, "log for " + myAgent + "\n" + msg, t);
		}
		if (t != null) {
			myAgent.setAgentStackTrace(t);
		}
		final Level lvl = getLevel();
		if (lvl == Level.OFF) {
			setLevel(Level.ALL);
		}
		if (t != null) {

			log(Level.SEVERE, msg, t);

		} else {
			log(Level.SEVERE, msg);
		}
		setLevel(lvl);
	}

	/**
	 * This call bypasses any settings and always produces severe log messages.
	 * 
	 * @param msg
	 *            the message to display
	 */
	public void severeLog(final String msg) {
		severeLog(msg, null);
	}

	/**
	 * Set all the agents' loggers to the specified level
	 * 
	 * @param level
	 *            the new level
	 */
	public static void setAllLogLevels(final Level level) {
		synchronized (agentLoggers) {
			for (AbstractAgent loggedAgent : agentLoggers.keySet()) {
				if (loggedAgent != loggedAgent.getMadkitKernel()) {
					loggedAgent.setLogLevel(level);
				}
				loggedAgent.getMadkitConfig().agentLogLevel = level;
			}
		}
	}

	/**
	 * Create a log file for each agent having a non <code>null</code> logger.
	 * 
	 * @see AgentLogger#createLogFile()
	 */
	public static void createLogFiles() {
		synchronized (agentLoggers) {
			try {

				AbstractAgent a = new ArrayList<>(agentLoggers.keySet()).get(0);
				a.getMadkitConfig().createLogFiles = true;
				JOptionPane.showMessageDialog(null,
						Words.DIRECTORY + " " + a.getMadkitConfig().logDirectory.getAbsolutePath() + " " + Words.CREATED,
						"OK", JOptionPane.INFORMATION_MESSAGE);
				for (AgentLogger logger : agentLoggers.values()) {
					logger.createLogFile();
				}
			} catch (IndexOutOfBoundsException e) {
				JOptionPane.showMessageDialog(null, "No active agents yet", Words.FAILED.toString(),
						JOptionPane.WARNING_MESSAGE);
			}
		}
	}
}

class AgentFormatter extends Formatter {
	private static final int HEADER_SIZE=56;
	private final int MAX_LEVEL_SIZE;
	public AgentFormatter()
	{
		int m=Level.ALL.getLocalizedName().length();
		m=Math.max(m, Level.OFF.getLocalizedName().length());
		m=Math.max(m, Level.CONFIG.getLocalizedName().length());
		m=Math.max(m, Level.FINE.getLocalizedName().length());
		m=Math.max(m, Level.FINEST.getLocalizedName().length());
		m=Math.max(m, Level.FINER.getLocalizedName().length());
		m=Math.max(m, Level.INFO.getLocalizedName().length());
		m=Math.max(m, Level.SEVERE.getLocalizedName().length());
		m=Math.max(m, Level.WARNING.getLocalizedName().length());
		MAX_LEVEL_SIZE=m;

	}
	@Override
	public String format(final LogRecord record) {

		final Level lvl = record.getLevel();
		if (lvl.equals(AgentLogger.talkLevel)) {
			return record.getMessage();
		}
		StringBuilder s=new StringBuilder();
		setHeader(s, record);
		String l=lvl.getLocalizedName();
		s.append(l);
		int rl=MAX_LEVEL_SIZE-l.length();
		while (rl-->0)
			s.append(" ");
		s.append(" : ");
		s.append(record.getMessage());
		s.append("\n");
		return s.toString();
	}
	protected void setHeader(StringBuilder s, final LogRecord record) {
		s.append(record.getLoggerName());
		if (s.length()<HEADER_SIZE)
		{
			while (s.length()<HEADER_SIZE)
				s.append(" ");
		}
		else if (s.length()>HEADER_SIZE)
			s.delete(HEADER_SIZE, s.length());
		s.append(" ");
	}

}