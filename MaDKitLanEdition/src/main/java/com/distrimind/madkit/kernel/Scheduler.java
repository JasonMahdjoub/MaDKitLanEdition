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

import com.distrimind.madkit.action.Action;
import com.distrimind.madkit.action.SchedulingAction;
import com.distrimind.madkit.gui.GUIProvider;
import com.distrimind.madkit.message.SchedulingMessage;

import java.util.LinkedHashSet;
import java.util.Observable;
import java.util.Set;
import java.util.logging.Level;

import static com.distrimind.madkit.kernel.Scheduler.SimulationState.PAUSED;


/**
 * This class defines a generic threaded scheduler agent. It holds a collection
 * of activators. The default state of a scheduler is
 * {@link SimulationState#PAUSED}. The default delay between two steps is 0 ms
 * (max speed).
 * 
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 2.0
 * @since MadkitLanEdition 1.0
 * @version 5.3
 * @see Activator
 */
public class Scheduler extends Agent {

	/**
	 * A simulation state. The simulation process managed by a scheduler agent can
	 * be in one of the following states:
	 * <ul>
	 * <li>{@link #RUNNING}<br>
	 * The simulation process is running normally.</li>
	 * <li>{@link #STEP}<br>
	 * The scheduler will process one simulation step and then will be in the
	 * {@link #PAUSED} state.</li>
	 * <li>{@link #PAUSED}<br>
	 * The simulation is paused. This is the default state.</li>
	 * </ul>
	 * 
	 * @author Fabien Michel
	 * @since MaDKit 5.0
	 * @see #getSimulationState
	 */
	public enum SimulationState {

		/**
		 * The simulation process is running normally.
		 */
		RUNNING,

		/**
		 * The scheduler will process one simulation step and then will be in the
		 * {@link #PAUSED} state.
		 * 
		 */
		STEP,

		/**
		 * The simulation is paused.
		 */
		PAUSED,

		/**
		 * The simulation is ending
		 */
		SHUTDOWN
	}

	SimulationState simulationState = SimulationState.PAUSED;

	final private Set<Activator<? extends AbstractAgent>> activators = new LinkedHashSet<>();

	@SuppressWarnings({"FieldCanBeLocal", "unused"})
	private Action run, step, speedUp, speedDown;

	// private JLabel timer;
	private int delay=400;

	/**
	 * specify the delay between 2 steps
	 */
	private final Object speedModel = GUIProvider.getSchedulerBoundedRangeModel(this);

	/**
	 * Returns the delay between two simulation steps
	 * 
	 * @return the delay between two simulation steps.
	 */
	public int getDelay() {
		return delay;
	}

	/**
	 * Sets the delay between two simulation steps. That is the pause time between
	 * to call to {@link #doSimulationStep()}. The value is automatically adjusted
	 * between 0 and 400.
	 * 
	 * @param delay
	 *            the pause between two steps in milliseconds, an integer between 0
	 *            and 400: O is max speed.
	 */
	public void setDelay(final int delay) {
		GUIProvider.setSchedulerDelay(this, delay);
	}

	public void setDelayWithoutInteractionWithGUI(final int delay) {
		this.delay=delay;
	}

	public Object getSpeedModel() {
		return speedModel;
	}

	private double GVT = 0; // simulation global virtual time

	/**
	 * Returns the simulation global virtual time.
	 * 
	 * @return the gVT
	 */
	public double getGVT() {
		return GVT;
	}

	/**
	 * Sets the simulation global virtual time.
	 * 
	 * @param GVT
	 *            the actual simulation time
	 */
	public void setGVT(final double GVT) {
		this.GVT = GVT;
		if (gvtModel != null) {
			gvtModel.notifyObservers((int) GVT);
		}
	}

	private double simulationDuration;

	@SuppressWarnings("unused")
	private GVTModel gvtModel;

	/**
	 * This constructor is equivalent to <code>Scheduler(Double.MAX_VALUE)</code>
	 */
	public Scheduler() {
		this(Double.MAX_VALUE);
	}

	// public Scheduler(boolean multicore) {
	// this(0, Double.MAX_VALUE);
	// }

	/**
	 * Constructor specifying the time at which the simulation ends.
	 * 
	 * @param endTime
	 *            the GVT at which the simulation will automatically stop
	 */
	public Scheduler(final double endTime) {
		buildActions();
		setSimulationDuration(endTime);
	}

	/**
	 * Setup the default Scheduler GUI when launched with the default MaDKit GUI
	 * mechanism.
	 * 
	 *
	 * @since MaDKit 5.0.0.8
	 */
	@Override
	public void setupFrame(Object... parameters) {
		GUIProvider.setupSchedulerFrame(this, parameters);
	}

	/**
	 * Adds an activator to the kernel engine. This has to be done to make an
	 * activator work properly
	 * 
	 * @param activator
	 *            an activator.
	 */
	public void addActivator(final Activator<? extends AbstractAgent> activator) {
		if (kernel.addOverlooker(this, activator)) {
			activators.add(activator);
			if (logger != null && logger.isLoggable(Level.FINE))
				logger.fine("Activator added: " + activator);
		} else if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Impossible to add activator : " + activator);

	}

	/**
	 * Removes an activator from the kernel engine.
	 * 
	 * @param activator
	 *            an activator.
	 */
	public void removeActivator(final Activator<? extends AbstractAgent> activator) {
		kernel.removeOverlooker(this, activator);
		activators.remove(activator);
		if (logger != null && logger.isLoggable(Level.FINE))
			logger.fine("Activator removed: " + activator);
	}

	/**
	 * Executes all the activators in the order they have been added, using
	 * {@link Activator#execute(Object...)}, and then increments the global virtual
	 * time of this scheduler by one unit.
	 * 
	 * This also automatically calls the multicore mode of the activator if it is
	 * set so. This method should be overridden to define customized scheduling
	 * policy. So default implementation is :
	 * 
	 * <pre>
	 * 
	 * 	public void doSimulationStep() {
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;if (logger != null) {
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;logger.finer("Doing simulation step " + GVT);
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;}
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;for (final Activator&lt;? extends AbstractAgent&gt; activator : activators) 
	 * 		{
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;if (logger != null)
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;logger.finer("Activating\n--------&gt; " + activator);
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;activator.execute();
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;
	 * 		}
	 * 		&nbsp;&nbsp; &nbsp; * &nbsp;&nbsp; &nbsp;&nbsp;&nbsp; &nbsp;setGVT(getGVT() + 1);
	 * 		}
	 * </pre>
	 */
	public void doSimulationStep() {
		if (logger != null && logger.isLoggable(Level.FINER)) {
			logger.finer("Doing simulation step " + GVT);
		}
		for (final Activator<? extends AbstractAgent> activator : activators) {
			if (logger != null && logger.isLoggable(Level.FINER))
				logger.finer("Activating --------> " + activator);
			// try {
			activator.execute();
			// } catch (SimulationException e) {//TODO is it better ?
			// setSimulationState(SimulationState.SHUTDOWN);
			// getLogger().log(Level.SEVERE, e.getMessage(), e);
			// }
		}
		setGVT(GVT + 1);
	}

	@Override
	protected void end() {
		simulationState = PAUSED;
		if (logger != null)
			logger.info("Simulation stopped !");
	}

	/**
	 * The state of the simulation.
	 * 
	 * @return the state in which the simulation is.
	 * @see SimulationState
	 */
	public SimulationState getSimulationState() {
		return simulationState;
	}

	/**
	 * Changes the state of the scheduler
	 * 
	 * @param newState
	 *            the new state
	 */
	protected void setSimulationState(final SimulationState newState) {// TODO proceedEnumMessage
		if (simulationState != newState) {
			simulationState = newState;
			if (logger!=null)
				logger.log(Level.FINE, "New simulation state : "+simulationState);
			switch (simulationState) {
			case STEP:case PAUSED:
					run.setEnabled(true);
				break;
			case RUNNING:case SHUTDOWN:
					run.setEnabled(false);
			break;
			default:// impossible
				logLifeException(new Exception("state not handle : " + newState.toString()));
			}
		}
	}


	/**
	 * Scheduler's default behavior.
	 * 
	 * 
	 * 
	 * @throws InterruptedException if the current thread was interrupted
	 * 
	 * @see com.distrimind.madkit.kernel.Agent#liveCycle()
	 */
	@Override
	protected void liveCycle() throws InterruptedException {
		if (GVT > simulationDuration) {
			if (logger != null)
				logger.info("Quitting: Simulation has reached end time " + simulationDuration);
			this.killAgent(this);
		}
		pause(delay);
		checkMail(nextMessage());
		switch (simulationState) {
		case RUNNING:
			doSimulationStep();
			break;
		case PAUSED:
			paused();
			break;
		case STEP:
			simulationState = PAUSED;
			doSimulationStep();
			break;
		case SHUTDOWN:
			this.killAgent(this);
		default:
			getLogger().severe("state not handled " + simulationState);
		}
	}

	/**
	 * Changes my state according to a {@link SchedulingMessage} and sends a reply
	 * to the sender as acknowledgment.
	 * 
	 * @param m
	 *            the received message
	 */
	protected void checkMail(final Message m) {
		if (m != null) {
			try {
				SchedulingAction code = ((SchedulingMessage) m).getCode();
				switch (code) {
				case RUN:
					setSimulationState(SimulationState.RUNNING);
					break;
				case STEP:
					setSimulationState(SimulationState.STEP);
					break;
				case PAUSE:
					setSimulationState(SimulationState.PAUSED);
					break;
				case SHUTDOWN:
					setSimulationState(SimulationState.SHUTDOWN);
					break;
				case SPEED_UP:
					setDelay(delay-50);

					break;
				case SPEED_DOWN:
					setDelay(delay+50);
					break;
				}
				if (m.getSender() != null) {
					sendReply(m, m);
				}
			} catch (ClassCastException e) {
				if (logger != null)
					logger.info("I received a message that I cannot understand" + m);
			}
		}
	}

	/**
	 * Runs {@link #checkMail(Message)} every 1000 ms.
	 * 
	 * @throws InterruptedException if the current thread was interrupted
	 */
	protected void paused() throws InterruptedException {

		checkMail(waitNextMessage(1000));
	}

	/**
	 * @see com.distrimind.madkit.kernel.AbstractAgent#terminate()
	 */
	@Override
	final void terminate() {
		removeAllActivators();
		super.terminate();
	}

	/**
	 * Remove all the activators which have been previously added
	 */
	public void removeAllActivators() {
		for (final Activator<? extends AbstractAgent> a : activators) {
			kernel.removeOverlooker(this, a);
		}
		activators.clear();
	}

	/**
	 * Sets the simulation time for which the scheduler should end the simulation.
	 * 
	 * @param endTime
	 *            the end time to set
	 */
	public void setSimulationDuration(final double endTime) {
		this.simulationDuration = endTime;
	}

	/**
	 * @return the simulationDuration
	 */
	public double getSimulationDuration() {
		return simulationDuration;
	}

	private void buildActions() {
		run = SchedulingAction.RUN.getActionFor(this);
		step = SchedulingAction.STEP.getActionFor(this);
		speedUp = SchedulingAction.SPEED_UP.getActionFor(this);
		speedDown = SchedulingAction.SPEED_DOWN.getActionFor(this);
	}



	public static final class GVTModel extends Observable {
		@Override
		public void notifyObservers(Object arg) {
			setChanged();
			super.notifyObservers(arg);
		}
	}

}


