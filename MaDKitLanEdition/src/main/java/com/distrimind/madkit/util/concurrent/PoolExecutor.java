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
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since Utils 4.8.0
 */
public class PoolExecutor implements ExecutorService {


	private final int minimumNumberOfThreads;
	private final int maximumNumberOfThreads;
	final Deque<Runnable> workQueue;
	private final long keepAliveTime;
	private final ThreadFactory threadFactory;
	private final HandlerForFailedExecution handler;
	private volatile boolean shutdownAsked=false, shutdownNow=false;
	private int pausedThreads=0;
	private int workingThreads=0;

	final ReentrantLock lock=new ReentrantLock();
	final Condition waitEventsCondition =lock.newCondition();
	final Condition waitEmptyWorkingQueue=lock.newCondition();
	private final ArrayList<Executor> executors=new ArrayList<>();


	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit) {
		this(minimumNumberOfThreads, maximumNumberOfThreads, keepAliveTime, unit,  Executors.defaultThreadFactory(),
				defaultHandler);
	}

	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit,
						ThreadFactory threadFactory) {
		this(minimumNumberOfThreads, maximumNumberOfThreads, keepAliveTime, unit,threadFactory, defaultHandler);
	}


	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit,
						HandlerForFailedExecution handler) {
		this(minimumNumberOfThreads, maximumNumberOfThreads, keepAliveTime, unit, Executors.defaultThreadFactory(), handler);
	}

	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit,
						ThreadFactory threadFactory, HandlerForFailedExecution handler) {
		this(minimumNumberOfThreads, maximumNumberOfThreads, keepAliveTime, unit, new CircularArrayList<Runnable>(256, true), threadFactory, handler);
	}
	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit, Deque<Runnable> workQueue) {
		this(minimumNumberOfThreads, maximumNumberOfThreads, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(),
				defaultHandler);
	}

	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit,
						Deque<Runnable> workQueue, ThreadFactory threadFactory) {
		this(minimumNumberOfThreads, maximumNumberOfThreads, keepAliveTime, unit, workQueue, threadFactory, defaultHandler);
	}


	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit,
						Deque<Runnable> workQueue, HandlerForFailedExecution handler) {
		this(minimumNumberOfThreads, maximumNumberOfThreads, keepAliveTime, unit, workQueue, Executors.defaultThreadFactory(), handler);
	}

	public PoolExecutor(int minimumNumberOfThreads, int maximumNumberOfThreads, long keepAliveTime, TimeUnit unit,
						Deque<Runnable> workQueue, ThreadFactory threadFactory, HandlerForFailedExecution handler) {
		if (minimumNumberOfThreads < 1 || maximumNumberOfThreads < 1 || maximumNumberOfThreads < minimumNumberOfThreads || keepAliveTime < 0)
			throw new IllegalArgumentException();
		if (workQueue == null || threadFactory == null || handler == null)
			throw new NullPointerException();
		this.minimumNumberOfThreads = minimumNumberOfThreads;
		this.maximumNumberOfThreads = maximumNumberOfThreads;
		this.workQueue = workQueue;
		this.keepAliveTime = unit.toNanos(keepAliveTime);
		this.threadFactory = threadFactory;
		this.handler = handler;

	}

	public void start()
	{
		lock.lock();
		try
		{
			for (int i = 0; i<this.minimumNumberOfThreads; i++)
				launchNewThreadUnsafe(true);
		}
		finally {
			lock.unlock();
		}
	}

	public int getMinimumNumberOfThreads() {
		lock.lock();
		try {
			return minimumNumberOfThreads;
		}
		finally {
			lock.unlock();
		}

	}

	public int getMaximumNumberOfThreads() {
		lock.lock();
		try {
			return maximumNumberOfThreads;
		}
		finally {
			lock.unlock();
		}
	}

	protected void removeRepetitiveTasksUnsafe()
	{

	}

	private void shutdown(boolean now)
	{
		if (shutdownAsked)
			return;
		lock.lock();
		try {
			shutdownAsked = true;
			shutdownNow = now;
			removeRepetitiveTasksUnsafe();
			waitEventsCondition.signalAll();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void shutdown() {
		shutdown(false);
	}

	@Override
	public List<Runnable> shutdownNow() {
		shutdown(true);
		return getActualTasks();
	}

	public List<Runnable> getActualTasks()
	{
		lock.lock();
		try {
			return new ArrayList<>(workQueue);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isShutdown() {
		return shutdownAsked;
	}

	@Override
	public boolean isTerminated() {
		return shutdownAsked && (shutdownNow || workQueue.size()==0);
	}

	boolean areWorkingQueuesEmptyUnsafe()
	{
		return workQueue.isEmpty();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {

		lock.lock();
		try {
			long start=System.nanoTime();
			timeout=unit.toNanos(timeout);
			while (!areWorkingQueuesEmptyUnsafe()) {
				if (!waitEmptyWorkingQueue.await(timeout, TimeUnit.NANOSECONDS))
					return false;
				long end=System.nanoTime();
				timeout-=end-start;
				if (timeout<0)
					return false;
				start=end;
			}
			return true;
		}
		finally {
			lock.unlock();
		}
	}

	public void setThreadsPriority(int priority) {
		lock.lock();
		try
		{
			for (Executor e : executors)
				e.thread.setPriority(priority);
		}
		finally {
			lock.unlock();
		}
	}

	class Future<T> implements java.util.concurrent.Future<T>, Runnable
	{
		private final Lock flock;
		private final Callable<T> callable;
		private volatile boolean isCancelled=false;
		private volatile boolean isFinished=false;
		private volatile Thread thread;
		private T res=null;
		private Exception exception=null;
		private final Condition waitForComplete;

		protected Future(Callable<T> callable) {
			this(callable, new ReentrantLock());
		}
		protected Future(Callable<T> callable, Lock flock)
		{
			this(callable, flock, flock.newCondition());
		}
		protected Future(Callable<T> callable, Lock flock, Condition waitForComplete) {
			this.callable = callable;
			this.flock = flock;
			this.waitForComplete=waitForComplete;
		}
		@Override
		public void run()
		{
			this.thread = Thread.currentThread();
			try {
				res=callable.call();
			} catch (Exception e) {
				exception=e;
				e.printStackTrace();
				handler.failedExecution(this, PoolExecutor.this);
			}
			flock.lock();
			try{
				isFinished=true;
				this.thread = null;
				waitForComplete.signalAll();
			}
			finally {
				flock.unlock();
			}

		}
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {

			flock.lock();
			try{
				if (isFinished)
					return false;
				if (isCancelled)
					return true;
				if (mayInterruptIfRunning) {
					Thread t=this.thread;
					if (t!=null)
						t.interrupt();
				}
				isCancelled=true;
				waitForComplete.signalAll();
			}
			finally {
				flock.unlock();
			}
			return false;
		}

		@Override
		public boolean isCancelled() {
			return isCancelled;
		}

		@Override
		public boolean isDone() {
			return isFinished || isCancelled;
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			try {
				return get(0, null, false);
			} catch (TimeoutException e) {
				e.printStackTrace();
				throw new IllegalAccessError();
			}
		}

		public T get(long timeout, TimeUnit unit, boolean timeoutUsed) throws InterruptedException, ExecutionException, TimeoutException {
			ExecutorWrapper executorWrapper=incrementMaxThreadNumberIfCurrentThreadPartOfPoolExecutor();
			flock.lock();
			try {
				if (timeoutUsed) {
					long start = System.nanoTime();
					timeout = unit.toNanos(timeout);
					while (!isCancelled && !isFinished) {
						if (!waitForComplete.await(timeout, TimeUnit.NANOSECONDS))
							throw new TimeoutException();
						long end = System.nanoTime();
						timeout -= end - start;
						if (timeout < 0)
							throw new TimeoutException();
						start = end;

					}
				}
				else
				{
					while (!isCancelled && !isFinished)
					{
						waitForComplete.await();
					}
				}
				if (isCancelled)
					throw new CancellationException();
				if (exception!=null)
					throw new ExecutionException(exception);

				return res;
			}
			finally {
				flock.unlock();
				if (executorWrapper!=null)
					executorWrapper.decrementMaxThreadNumber();
			}
		}
		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			return get(timeout, unit, true);
		}
		boolean repeat()
		{
			return false;
		}

	}
	void repeatUnsafe(ScheduledFuture<?> sf) {
		throw new IllegalAccessError();
	}
	@Override
	public <T> Future<T> submit(Callable<T> task) {

		Future<T> future=new Future<>(task);
		execute(future);
		return future;
	}

	@Override
	public <T> Future<T> submit(final Runnable task, final T result) {
		return submit(new Callable<T>() {
			@Override
			public T call() {
				task.run();
				return result;
			}
		});

	}

	@Override
	public Future<?> submit(final Runnable task) {
		return submit(new Callable<Void>() {
			@Override
			public Void call() {
				task.run();
				return null;
			}
		});
	}

	@Override
	public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return invokeAll(tasks, 0, null, false);
	}


	private <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit, boolean useTimeout) throws InterruptedException {
		if (shutdownAsked)
			throw new RejectedExecutionException();
		lock.lock();
		ArrayList<Future<T>> futures;
		ArrayList<java.util.concurrent.Future<T>> futures2;
		try {
			futures=new ArrayList<>(tasks.size());
			futures2=new ArrayList<>(tasks.size());
			for (Callable<T> c : tasks) {
				Future<T> f=new Future<>(c);
				futures.add(f);
				futures2.add(f);
			}
			if (!workQueue.addAll(futures))
				throw new RejectedExecutionException();
			waitEventsCondition.signalAll();


		}
		finally {
			lock.unlock();
		}
		if (useTimeout) {
			long start = System.nanoTime();
			long timeOut = unit.toNanos(timeout);
			for (Future<T> f : futures) {
				try {
					f.get(timeOut, TimeUnit.NANOSECONDS);
					long end = System.nanoTime();
					timeOut -= (end - start);
					if (timeOut < 0)
						break;
					start = end;
				} catch (ExecutionException e) {
					e.printStackTrace();
				} catch (TimeoutException ignored) {
					break;
				}
			}

		} else {
			for (Future<T> f : futures) {
				try {
					f.get();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		}
		return futures2;
	}

	@Override
	public <T> List<java.util.concurrent.Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
		return invokeAll(tasks, timeout, unit, true);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		try {
			return invokeAny(tasks, 0, null, false);
		} catch (TimeoutException e) {
			e.printStackTrace();
			throw new IllegalAccessError();
		}
	}

	private <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit, boolean useTimeout) throws InterruptedException, ExecutionException, TimeoutException {
		if (shutdownAsked)
			throw new RejectedExecutionException();
		lock.lock();
		ArrayList<Future<T>> futures;

		Lock flock = new ReentrantLock();
		Condition waitForFuture = flock.newCondition();

		try {
			futures=new ArrayList<>(tasks.size());

			for (Callable<T> c : tasks) {
				futures.add(new Future<>(c, flock, waitForFuture));
			}
			if (!workQueue.addAll(futures))
				throw new RejectedExecutionException();
			waitEventsCondition.signalAll();


		}
		finally {
			lock.unlock();
		}

		flock.lock();
		try {
			long start=0;
			long timeOut=0;
			if (useTimeout)
			{
				start = System.nanoTime();
				timeOut = unit.toNanos(timeout);
			}


			for (;;) {
				boolean allDone=true;
				for (Future<T> f : futures) {
					if (f.isFinished) {
						return f.get();
					}
					else if (!f.isDone())
						allDone=false;
				}
				if (allDone)
					throw new IllegalAccessError();
				if (useTimeout) {
					if (!waitForFuture.await(timeOut, TimeUnit.NANOSECONDS))
						throw new TimeoutException();
					long end = System.nanoTime();
					timeOut -= (end - start);
					if (timeOut < 0)
						throw new TimeoutException();
					start = end;
				}
				else
					waitForFuture.await();
			}
		}
		finally {
			flock.unlock();
		}
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return invokeAny(tasks, timeout, unit, true);
	}

	@Override
	public void execute(Runnable command) {
		if (shutdownAsked)
			throw new RejectedExecutionException();
		lock.lock();
		try {

			if (!workQueue.add(command))
				throw new RejectedExecutionException();
			waitEventsCondition.signalAll();
		}
		finally {
			lock.unlock();
		}

	}
	public static final DefaultHandlerForFailedExecution defaultHandler=new DefaultHandlerForFailedExecution();
	public static class DefaultHandlerForFailedExecution implements HandlerForFailedExecution {
		public DefaultHandlerForFailedExecution() {
		}

		@Override
		public void failedExecution(Runnable r, ExecutorService executor) {
			throw new RejectedExecutionException("Task " + r.toString() + " rejected from " + executor.toString());
		}

	}

	private Executor getExecutor(Thread thread)
	{
		for (Executor e : executors)
		{
			if (Objects.equals(e.thread, thread))
				return e;
		}
		return null;
	}

	private class ExecutorWrapper
	{
		final Executor executor;

		public ExecutorWrapper(Executor executor) {
			this.executor = executor;
		}
		private void decrementMaxThreadNumber() {
			lock.lock();
			try
			{
				--pausedThreads;
				if (executor!=null) {
					executor.core = false;
				}
			}
			finally {
				lock.unlock();
			}
		}
	}

	private ExecutorWrapper incrementMaxThreadNumberIfCurrentThreadPartOfPoolExecutor()
	{
		lock.lock();
		try
		{
			Executor executor=getExecutor(Thread.currentThread());
			if (executor!=null)
			{
				if (pausedThreads <Integer.MAX_VALUE) {
					++pausedThreads;
					return executor.launchChildIfNecessaryUnsafe();
				}
				else
					throw new OutOfMemoryError();
			}
			else
				return null;
		}
		finally {
			lock.unlock();
		}

	}


	public boolean wait(LockerCondition locker) throws InterruptedException {
		ExecutorWrapper executorWrapper=incrementMaxThreadNumberIfCurrentThreadPartOfPoolExecutor();
		if (executorWrapper!=null) {


			try {
				synchronized (locker.getLocker()) {
					while (locker.isLocked() && !locker.isCanceled()) {
						locker.beforeCycleLocking();
						locker.getLocker().wait();
						locker.afterCycleLocking();
					}
				}

			} finally {
				executorWrapper.decrementMaxThreadNumber();
			}
			return true;
		} else {
			return false;
		}
	}
	public boolean wait(LockerCondition locker, long delay,TimeUnit unit) throws InterruptedException, TimeoutException {
		ExecutorWrapper executorWrapper=incrementMaxThreadNumberIfCurrentThreadPartOfPoolExecutor();
		if (executorWrapper!=null) {

			long start=System.currentTimeMillis();
			delay=unit.toMillis(delay);
			try {
				synchronized (locker.getLocker()) {
					while (locker.isLocked() && !locker.isCanceled()) {
						locker.beforeCycleLocking();
						locker.getLocker().wait(delay);
						locker.afterCycleLocking();
						long end=System.currentTimeMillis();
						delay-=end-start;
						if (delay<0)
							throw new TimeoutException();
						start=end;
					}
				}

			} finally {
				executorWrapper.decrementMaxThreadNumber();
			}
			return true;
		} else {
			return false;
		}
	}
	public boolean wait(LockerCondition locker, Lock personalLocker, Condition personalCondition) throws InterruptedException {
		ExecutorWrapper executorWrapper=incrementMaxThreadNumberIfCurrentThreadPartOfPoolExecutor();
		if (executorWrapper!=null) {


			try {
				personalLocker.lock();
				try{
					while (locker.isLocked() && !locker.isCanceled()) {
						locker.beforeCycleLocking();
						personalCondition.await();
						locker.afterCycleLocking();
					}
				}
				finally {
					personalLocker.unlock();
				}

			} finally {
				executorWrapper.decrementMaxThreadNumber();

			}
			return true;
		} else {
			return false;
		}
	}

	public boolean wait(LockerCondition locker, Lock personalLocker, Condition personalCondition, long delay, TimeUnit unit) throws InterruptedException, TimeoutException {
		ExecutorWrapper executorWrapper=incrementMaxThreadNumberIfCurrentThreadPartOfPoolExecutor();
		if (executorWrapper!=null) {

			long start=System.nanoTime();
			delay=unit.toNanos(delay);

			try {
				personalLocker.lock();

				try{
					while (locker.isLocked() && !locker.isCanceled()) {
						locker.beforeCycleLocking();
						if (!personalCondition.await(delay, TimeUnit.NANOSECONDS))
							delay=-1;
						locker.afterCycleLocking();
						long end=System.nanoTime();
						delay-=end-start;
						if (delay<0)
							throw new TimeoutException();
						start=end;
					}
				}
				finally {
					personalLocker.unlock();
				}

			} finally {
				executorWrapper.decrementMaxThreadNumber();

			}
			return true;
		} else {
			return false;
		}
	}
	public boolean sleep(long duration, TimeUnit unit) throws InterruptedException {
		ExecutorWrapper executorWrapper=this.incrementMaxThreadNumberIfCurrentThreadPartOfPoolExecutor();
		if (executorWrapper!=null) {

			duration=unit.toMillis(duration);

			try {
				if (duration > 0)
					Thread.sleep(duration);

			} finally {
				executorWrapper.decrementMaxThreadNumber();
			}
			return true;
		} else {
			return false;
		}
	}
	private boolean needNewThreadUnsafe()
	{
		assert workingThreads<=executors.size();
		return executors.size()-pausedThreads< minimumNumberOfThreads || (executors.size()<maximumNumberOfThreads && workingThreads==executors.size() && areWorkingQueuesEmptyUnsafe());
	}
	private Executor launchNewThreadUnsafe(boolean core)
	{
		Executor executor=new Executor(core);
		Thread t=threadFactory.newThread(executor);
		executor.thread=t;
		executors.add(executor);
		t.start();
		return executor;
	}


	Runnable pollTaskUnsafe()
	{
		Runnable t=workQueue.poll();
		if (workQueue.isEmpty())
			waitEmptyWorkingQueue.signalAll();
		return t;
	}

	long timeToWaitBeforeNewTaskScheduledInNanoSeconds()
	{
		return Long.MAX_VALUE;
	}




	private class Executor implements Runnable
	{
		Thread thread;
		private boolean core;
		Executor(boolean core) {
			this.core = core;
		}
		ExecutorWrapper launchChildIfNecessaryUnsafe()
		{
			if (shutdownAsked)
				return new ExecutorWrapper(null);
			waitEventsCondition.signalAll();
			if (!needNewThreadUnsafe())
			{
				for (Executor e : executors)
				{
					if (!e.core) {
						e.core = true;

						return new ExecutorWrapper(e);
					}
				}
			}
			return new ExecutorWrapper(launchNewThreadUnsafe(true));
		}



		@Override
		public void run() {
			boolean working=false;
			try {
				ScheduledFuture<?> toRepeat=null;

				for (; ; ) {
					Runnable task = null;
					long timeout = keepAliveTime;
					long start = 0;

					lock.lock();
					if (working)
						--workingThreads;
					if (toRepeat!=null) {
						repeatUnsafe(toRepeat);
						toRepeat = null;
					}
					if (!core)
						start = System.nanoTime();
					try {

						while (!shutdownAsked && (task = pollTaskUnsafe()) == null) {
							try {
								long timeToWait = timeToWaitBeforeNewTaskScheduledInNanoSeconds();
								if (timeToWait != Long.MAX_VALUE && (core || timeout > timeToWait)) {
									waitEventsCondition.await(timeToWait + 1, TimeUnit.NANOSECONDS);
									if (!core) {
										long end = System.nanoTime();
										timeout -= (System.nanoTime() - start);
										if (timeout < 0)
											return;
										start = end;
									}
								}
								else {
									if (core) {
										waitEventsCondition.await();
									} else {

										if (!waitEventsCondition.await(timeout, TimeUnit.NANOSECONDS))
											return;
										long end = System.nanoTime();
										timeout -= (System.nanoTime() - start);
										if (timeout < 0)
											return;
										start = end;

									}
								}
							} catch (InterruptedException ignored) {
								return;
							}
						}

						++workingThreads;
						working=true;
						while (needNewThreadUnsafe()) {
							launchNewThreadUnsafe(false);
						}
						waitEventsCondition.signalAll();

					} finally {
						lock.unlock();
					}
					if (shutdownAsked) {
						if (task instanceof Future) {
							Future<?> f = (Future<?>) task;
							f.cancel(false);
						}
						return;
					}
					assert task != null;

					task.run();

					if (task instanceof Future) {

						Future<?> f = (Future<?>) task;
						if (f.repeat()) {
							toRepeat=(ScheduledFuture<?>) f;
						}

					}
				}
			}
			finally {
				lock.lock();
				try {
					if (working)
						--workingThreads;
					executors.remove(this);
				}
				finally {
					lock.unlock();
				}
			}
		}


	}

}
