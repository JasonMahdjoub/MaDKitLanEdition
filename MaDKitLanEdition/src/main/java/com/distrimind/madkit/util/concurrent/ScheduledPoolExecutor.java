/*
Copyright or © or Copr. Jason Mahdjoub (04/02/2016)

jason.mahdjoub@distri-mind.fr

This software (Utils) is a computer program whose purpose is to give several kind of tools for developers
(ciphers, XML readers, decentralized id generators, etc.).

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

package com.distrimind.madkit.util.concurrent;

import java.util.*;
import java.util.concurrent.*;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since Utils 4.8.0
 */
public class ScheduledPoolExecutor extends PoolExecutor implements ScheduledExecutorService {
	private final TreeSet<SF<?>> scheduledFutures=new TreeSet<>();
	private boolean pull=false;
	private long timeOfFirstOccurrenceInNanos =Long.MAX_VALUE;

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit);
	}

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory);
	}

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, HandlerForFailedExecution handler) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit, handler);
	}

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, ThreadFactory threadFactory, HandlerForFailedExecution handler) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit, threadFactory, handler);
	}

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, Deque<Runnable> workQueue) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, Deque<Runnable> workQueue, ThreadFactory threadFactory) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory);
	}

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, Deque<Runnable> workQueue, HandlerForFailedExecution handler) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, handler);
	}

	public ScheduledPoolExecutor(int minimumPoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, Deque<Runnable> workQueue, ThreadFactory threadFactory, HandlerForFailedExecution handler) {
		super(minimumPoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
	}

	private class SF<T> extends PoolExecutor.Future<T> implements ScheduledFuture<T>
	{
		long start;
		public SF(Callable<T> callable, long initialDelay, TimeUnit unit) {
			super(callable);
			start=System.nanoTime()+unit.toNanos(initialDelay);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return unit.convert(start-System.nanoTime(), TimeUnit.NANOSECONDS);
		}

		@Override
		public int compareTo(Delayed o) {
			long r=getDelay(TimeUnit.NANOSECONDS)-o.getDelay(TimeUnit.NANOSECONDS);
			if (r<0)
				return -1;
			else if (r>0)
				return 1;
			else
				return 0;
		}

		public boolean isRepetitive() {
			return false;
		}
	}

	private class DelayedSF<T> extends SF<T>
	{
		private final long delay;
		public DelayedSF(Callable<T> callable, long initialDelay, long delay, TimeUnit unit) {
			super(callable, initialDelay, unit);
			this.delay=unit.toNanos(delay);
		}

		boolean repeat()
		{
			start=System.nanoTime()+delay;
			return true;
		}

		@Override
		public boolean isRepetitive() {
			return true;
		}

	}

	private class RatedSF<T> extends SF<T>
	{
		private final long period;
		public RatedSF(Callable<T> callable, long initialDelay, long period, TimeUnit unit) {
			super(callable, initialDelay, unit);
			this.period=unit.toNanos(period);
		}

		boolean repeat()
		{
			start+=period;
			long c=System.nanoTime();
			if (c>start)
				start=c;
			return true;
		}
		@Override
		public boolean isRepetitive() {
			return true;
		}

	}

	@Override
	public List<Runnable> getActualTasks()
	{
		lock.lock();
		try {
			ArrayList<Runnable> l=new ArrayList<>(workQueue);
			l.addAll(scheduledFutures);
			return l;
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public ScheduledFuture<?> schedule(final Runnable command, long delay, TimeUnit unit) {
		if (command==null)
			throw new NullPointerException();
		return schedule(new SF<>(new Callable<Void>() {
			@Override
			public Void call() {
				command.run();
				return null;
			}
		}, delay, unit));
	}

	@Override
	public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
		return schedule(new SF<>(callable, delay, unit));
	}

	@Override
	public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, long initialDelay, long period, TimeUnit unit) {
		if (command==null)
			throw new NullPointerException();
		return schedule(new RatedSF<>(new Callable<Void>() {
			@Override
			public Void call()  {
				command.run();
				return null;
			}
		}, initialDelay, period, unit));
	}

	@Override
	public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, long initialDelay, long delay, TimeUnit unit) {
		if (command==null)
			throw new NullPointerException();
		return schedule(new DelayedSF<>(new Callable<Void>() {
			@Override
			public Void call() {
				command.run();
				return null;
			}
		}, initialDelay, delay, unit));
	}


	<V> ScheduledFuture<V> schedule(SF<V> sf) {
		if (sf==null)
			throw new NullPointerException();
		lock.lock();
		try {
			repeatUnsafe(sf);
			waitEventsCondition.signalAll();
			return sf;
		}finally {
			lock.unlock();
		}
	}

	@Override
	void repeatUnsafe(ScheduledFuture<?> sf) {
		scheduledFutures.add((SF<?>)sf);
		timeOfFirstOccurrenceInNanos =scheduledFutures.first().start;
	}

	@Override
	boolean areWorkingQueuesEmptyUnsafe() {
		return super.areWorkingQueuesEmptyUnsafe() && scheduledFutures.isEmpty();
	}
	@Override
	protected void removeRepetitiveTasksUnsafe()
	{
		for (Iterator<SF<?>> it = scheduledFutures.iterator(); it.hasNext(); )
		{
			if (it.next().isRepetitive())
			{
				it.remove();
			}
		}
	}
	@Override
	Runnable pollTaskUnsafe() {
		Runnable r=null;
		if (pull) {
			if (timeOfFirstOccurrenceInNanos <=System.nanoTime()) {
				pull=false;
				r= scheduledFutures.pollFirst();
				assert r!=null;
				if (scheduledFutures.size()==0)
					timeOfFirstOccurrenceInNanos =Long.MAX_VALUE;
				else
					timeOfFirstOccurrenceInNanos =scheduledFutures.first().start;
			}
		}
		if (r==null) {
			pull = true;
			r=workQueue.poll();
			if (r==null)
				waitEmptyWorkingQueue.signalAll();
		}
		return r;
	}

	@Override
	long timeToWaitBeforeNewTaskScheduledInNanoSeconds() {
		return timeOfFirstOccurrenceInNanos;
	}
}
