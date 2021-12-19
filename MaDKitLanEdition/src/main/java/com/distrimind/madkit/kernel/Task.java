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

import java.util.concurrent.Callable;

import com.distrimind.madkit.agr.LocalCommunity;
import com.distrimind.madkit.message.task.TasksExecutionConfirmationMessage;

/**
 * This class represent a task to execute according a given time. This task can
 * also be repetitive according a given duration between each execution.
 * 
 * To launch a programmed task, use the function
 * {@link AbstractAgent#scheduleTask(Task, boolean)}
 * 
 * 
 * If you don't specify the TaskAgent name, than the default task agent is used.
 * This last is automically launched with the first added task.
 * 
 * @author Jason Mahdjoub
 * @since MadKitLanEdition 1.0
 * @version 1.0
 * 
 * @see AbstractAgent#scheduleTask(Task)
 * @see AbstractAgent#scheduleTask(Task, boolean)
 * @see AbstractAgent#cancelTask(TaskID, boolean)
 * 
 *
 *
 *
 */
public abstract class Task<V> implements Callable<V>, Runnable{

	/**
	 * This role is given to internal MadKit agents that manage task execution
	 */
	public static final String TASK_MANAGER_ROLE = "~~TASK_MANAGER";

	/**
	 * This role is automatically given to agents that launch tasks
	 */
	public static final String TASK_CALLER_ROLE = "~~TASK_ASKER";

	/**
	 * The name of the default task manager agent
	 */
	public static final String DEFAULT_TASK_EXECUTOR_NAME = "~~MKLE_DEFAULT_TASK_AGENT";

	public static final Group TASK_AGENTS = LocalCommunity.Groups.SYSTEM_ROOT.getSubGroup(false, new Gatekeeper() {

		@Override
		public boolean allowAgentToTakeRole(Group _group, String _roleName,
				final Class<? extends AbstractAgent> requesterClass, AgentNetworkID _agentNetworkID,
				Object _memberCard) {
			return _memberCard == memberCard;
		}

		@Override
		public boolean allowAgentToCreateSubGroup(Group _parent_group, Group _sub_group,
				final Class<? extends AbstractAgent> requesterClass, AgentNetworkID _agentNetworkID,
				Object _memberCard) {
			return _memberCard == memberCard;
		}
	}, true, "~~Tasks Agent");

	final static MemberCard memberCard = new MemberCard();

	static class MemberCard {

	}

	//private final Callable<V> callable;
	private V result = null;
	private long nanoTime;

	final long durationBetweenEachRepetitionInNanoSeconds;
	private boolean executeEvenIfLauncherAgentIsKilled =false;
	private final TaskID taskID=new TaskID();
	private transient AbstractAgent agent;
	private transient boolean ask_for_execution_confirmation;
	/**
	 * Construct a task to execute at the current time
	 * 
	 * @throws NullPointerException
	 *             if _runnable is null
	 * @see AbstractAgent#scheduleTask(Task, boolean)
	 * @see AbstractAgent#cancelTask(TaskID, boolean)
	 */
	public Task() {
		this(System.currentTimeMillis());
	}

	/**
	 * Construct a task to execute at the given time
	 * 
	 * @param _timeUTC
	 *            the moment in UTC when the TaskAgent must execute this task
	 * @throws NullPointerException
	 *             if _runnable is null
	 * @see AbstractAgent#scheduleTask(Task, boolean)
	 * @see AbstractAgent#cancelTask(TaskID, boolean)
	 */
	public Task(long _timeUTC) {
		this(_timeUTC, -1);
	}

	/**
	 * Construct a task to execute at the current time
	 *
	 * @param executeEvenIfLauncherAgentIsKilled execute the task even if the agent is killed (only available for non-repetitive tasks)
	 * @throws NullPointerException
	 *             if _runnable is null
	 * @see AbstractAgent#scheduleTask(Task, boolean)
	 * @see AbstractAgent#cancelTask(TaskID, boolean)
	 */
	public Task(boolean executeEvenIfLauncherAgentIsKilled) {
		this();
		this.executeEvenIfLauncherAgentIsKilled = executeEvenIfLauncherAgentIsKilled;
	}
	/**
	 * Construct a task to execute at the given time
	 *
	 * @param _timeUTC
	 *            the moment in UTC when the TaskAgent must execute this task
	 * @param executeEvenIfLauncherAgentIsKilled execute the task even if the agent is killed (only available for non-repetitive tasks)
	 * @throws NullPointerException
	 *             if _runnable is null
	 * @see AbstractAgent#scheduleTask(Task, boolean)
	 * @see AbstractAgent#cancelTask(TaskID, boolean)
	 */
	public Task(long _timeUTC, boolean executeEvenIfLauncherAgentIsKilled) {
		this(_timeUTC);
		this.executeEvenIfLauncherAgentIsKilled = executeEvenIfLauncherAgentIsKilled;
	}

	/**
	 * Construct a repetitive task to start at a given time
	 * 
	 * @param _timeUTC
	 *            the moment in UTC when the TaskAgent must execute this task
	 * @param _duration_between_each_repetition_in_milliseconds
	 *            the duration between each execution in milliseconds
	 * @throws NullPointerException
	 *             if _runnable is null
	 * @see AbstractAgent#scheduleTask(Task, boolean)
	 * @see AbstractAgent#cancelTask(TaskID, boolean)
	 */
	public Task(long _timeUTC, long _duration_between_each_repetition_in_milliseconds) {
		nanoTime = System.nanoTime()+((_timeUTC-System.currentTimeMillis())*1000000L);
		durationBetweenEachRepetitionInNanoSeconds = _duration_between_each_repetition_in_milliseconds==-1?-1:_duration_between_each_repetition_in_milliseconds*1000000L;
	}
	final void initRunnable(AbstractAgent agent, boolean ask_for_execution_confirmation)
	{
		this.agent=agent;
		this.ask_for_execution_confirmation=ask_for_execution_confirmation;
	}
	@Override
	public final void run() {
		boolean killed=agent.state.get().compareTo(AbstractAgent.State.WAIT_FOR_KILL) >= 0 || !agent.isAlive();
		try {

			if (!isExecuteEvenIfLauncherAgentIsKilled() && killed) {
				taskID.cancelTask(false);
				return;
			}

			long date_begin = System.currentTimeMillis();
			result = call();
			if (isRepetitive())
				renewTask();
			if (ask_for_execution_confirmation && !killed) {
				Message m = new TasksExecutionConfirmationMessage(taskID, this, date_begin,
						System.currentTimeMillis());
				m.setIDFrom(taskID);

				agent.receiveMessage(m);
			}
		} catch (Exception e) {
			if (agent.logger != null && !killed)
				agent.logger.severeLog("Exception in task execution : ", e);
			else
				e.printStackTrace();

		}
	}

	public final V getResult() {
		return result;
	}

	public final long getTimeUTCOfExecution() {
		return System.currentTimeMillis()+((nanoTime -System.nanoTime())/1000000L);
	}
	public final long getNanoTimeOfExecution() {
		return nanoTime;
	}

	public final boolean isRepetitive() {
		return durationBetweenEachRepetitionInNanoSeconds >= 0;
	}

	public final long getDurationBetweenEachRepetitionInNanoSeconds() {
		return durationBetweenEachRepetitionInNanoSeconds;
	}
	public final long getDurationBetweenEachRepetitionInMilliSeconds() {
		return durationBetweenEachRepetitionInNanoSeconds/1000000L;
	}

	final void renewTask() {
		if (isRepetitive())
			nanoTime = System.nanoTime() + durationBetweenEachRepetitionInNanoSeconds;
	}

	public final boolean isExecuteEvenIfLauncherAgentIsKilled() {
		return executeEvenIfLauncherAgentIsKilled;
	}

	public final TaskID getTaskID() {
		return taskID;
	}
}
