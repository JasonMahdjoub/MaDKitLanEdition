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

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import com.distrimind.madkit.kernel.AbstractAgent.ReturnCode;

/**
 * @author Fabien Michel
 * @author Jason Mahdjoub
 * @since MaDKit 5.0.0.9
 * @since MadkitLanEdition 1.0
 * @version 2.0
 *
 */
final class AgentExecutor {

	// private boolean started = false;
	final protected Agent myAgent;
	private volatile Future<ReturnCode> activate=null;
	private volatile Future<?> live=null;
	private volatile Future<?> end=null;

	// public AgentExecutor(Agent a, ThreadFactory threadFactory) {
	// super(1, Integer.MAX_VALUE, 0, TimeUnit.NANOSECONDS, new
	// ArrayBlockingQueue<Runnable>(4, false), threadFactory);
	// myAgent = a;
	//// myAgent.setAgentExecutor(this);
	// setThreadFactory(threadFactory);
	// }

	public AgentExecutor(Agent a) {
		//super(1, 1, 0, TimeUnit.NANOSECONDS, new ArrayBlockingQueue<Runnable>(4, false));
		myAgent = a;


	}
	void cancelActivate(boolean mayInterrupt)
	{
		this.activate.cancel(mayInterrupt);
	}
	void cancelLive(boolean mayInterrupt)
	{
		this.live.cancel(mayInterrupt);
	}

	void cancelEnd(boolean mayInterrupt)
	{
		this.end.cancel(mayInterrupt);

	}

	Future<ReturnCode> start()
	{
		List<Future<?>> list=myAgent.getMadkitKernel().getMaDKitServiceExecutor().launchDedicatedThread(myAgent.isDaemon()?myAgent.getMadkitKernel().getDaemonAgentThreadFactory():myAgent.getMadkitKernel().getNormalAgentThreadFactory(),
				Arrays.asList(
						new Callable<ReturnCode>()
						{

							@Override
							public ReturnCode call() {
								myAgent.myThread = Thread.currentThread();
								final ReturnCode r = myAgent.activation();

								if (r != ReturnCode.SUCCESS) {// alive is false && not a suicide
									live.cancel(false);
									if (end.isCancelled())// TO was 0 in the MK
										synchronized (myAgent.state) {
											myAgent.state.notify();
										}
								}
								return r;
							}
						},
						new Callable<Void>()
						{

							@Override
							public Void call() {
								myAgent.myThread = Thread.currentThread();
								if (myAgent.getAlive().get()) {
									myAgent.living();
								}
								if (end.isCancelled()) {// it is a kill with to == 0
									synchronized (myAgent.state) {
										myAgent.state.notify();
									}
								}
								return null;
							}
						},
						new Callable<Void>()
						{

							@Override
							public Void call() {
								myAgent.myThread = Thread.currentThread();
								myAgent.ending();

								synchronized (myAgent.state) {
									myAgent.state.notify();
								}

								return null;
							}
						},
						new Callable<Void>()
						{

							@Override
							public Void call() {
								if (!(myAgent.getKernel() instanceof FakeKernel)) {
									try {

										MadkitKernel k = myAgent.getMadkitKernel();
										myAgent.terminate();
										k.removeThreadedAgent(myAgent);
									} catch (KernelException e) {
										System.err.println(myAgent.getKernel());
										e.printStackTrace();
									}
								}
								return null;
							}
						}
				));
		//noinspection unchecked
		activate=(Future<ReturnCode>)list.get(0);
		live=list.get(1);
		end=list.get(2);
		return activate;
	}

}

// @Override
// protected void afterExecute(Runnable r, Throwable t) {
//// if(t != null){
//// myAgent.getAlive().set(false);
//// if(t instanceof KilledException){
//// if(myAgent.logger != null){
//// myAgent.logger.finer( "-*-GET KILLED in "+methodName()+"-*- :
// "+t.getMessage());
//// }
//// }
//// else{
//// myAgent.kernel.logSevereException(t);
//// myAgent.kernel.getMadkitKernel().kernelLog("Problem for "+this+" in
// "+methodName(), Level.FINER, t);
//// }
//// }
// if(! isTerminating() && myAgent.logger != null){
// myAgent.logger.finer("** exiting "+methodName()+" **");
// }
// }
//
// String methodName(){
// switch (myAgent.getState()) {
// case ACTIVATED:
// return "ACTIVATE";
// case LIVING:
// return "LIVE";
// default:
// return "END";
// }
// }