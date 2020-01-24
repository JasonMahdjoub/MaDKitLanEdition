package com.distrimind.madkit.kernel;

import com.distrimind.util.Reference;
import com.distrimind.util.concurrent.LockerCondition;

import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since MaDKitLanEdition 2.1.6
 */
public class ChainedBlockingDeque<T> extends AbstractQueue<T> implements BlockingQueue<T>, Deque<T> {

	private final LinkedList<T> list;
	private final Lock lock=new ReentrantLock();
	private final Condition notEmpty=lock.newCondition();
	private MadkitKernel madkitKernel;
	public ChainedBlockingDeque() {
		this((MadkitKernel)null);
	}
	public ChainedBlockingDeque(MadkitKernel madkitKernel) {
		list=new LinkedList<>();
		this.madkitKernel=madkitKernel;
	}
	public ChainedBlockingDeque(Collection<T> c) {
		this(null, c);
	}
	public ChainedBlockingDeque(MadkitKernel madkitKernel, Collection<T> c) {
		list=new LinkedList<>(c);
		this.madkitKernel=madkitKernel;
	}

	@Override
	public boolean add(T t) {
		lock.lock();
		try {
			return list.add(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	public Lock getLocker() {
		return lock;
	}

	@Override
	public boolean offer(T t) {
		lock.lock();
		try {
			return list.offer(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public T remove() {
		lock.lock();
		try {
			return list.remove();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T poll() {
		lock.lock();
		try {
			return list.poll();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T element() {
		lock.lock();
		try {
			return list.element();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T peek() {
		lock.lock();
		try {
			return list.peek();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void put(T t) {
		lock.lock();
		try {
			list.add(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public boolean offer(T t, long timeout, TimeUnit unit) {
		lock.lock();
		try {
			return list.offer(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public T take() throws InterruptedException {


		if (madkitKernel!=null) {
			final Reference<T> res=new Reference<>();
			madkitKernel.wait(madkitKernel, new LockerCondition() {
				boolean ok=false;
				@Override
				public boolean isLocked() {
					if (ok)
						return false;
					if (list.isEmpty())
					{
						return true;
					}
					else {
						res.set(list.remove());
						ok=true;
						return false;
					}
				}
			}, lock, notEmpty);
			return res.get();
		}
		else {
			lock.lock();
			try {
				while (list.isEmpty()) {
					notEmpty.await();
				}
				return list.remove();
			} finally {
				lock.unlock();
			}
		}
	}

	@Override
	public T poll(long time, TimeUnit unit) throws InterruptedException {

		if (madkitKernel!=null) {
			final Reference<T> res=new Reference<>();
			try {
				madkitKernel.wait(madkitKernel, new LockerCondition() {
					boolean ok=false;
					@Override
					public boolean isLocked() {
						if (ok)
							return false;
						if (list.isEmpty())
						{
							return true;
						}
						else {
							res.set(list.remove());
							ok=true;
							return false;
						}
					}
				}, lock, notEmpty, time, unit);
			} catch (TimeoutException ignored) {
				return null;
			}
			return res.get();
		}
		else {
			long start=System.nanoTime();
			time=unit.toNanos(time);

			lock.lock();
			try {
				while (list.isEmpty()) {
					if (!notEmpty.await(time, TimeUnit.NANOSECONDS))
						time=-1;

					long end=System.nanoTime();
					time-=end-start;
					if (time<=0)
						return null;
					start=end;
				}
				return list.remove();
			}
			finally {
				lock.unlock();
			}
		}
	}

	@Override
	public int remainingCapacity() {
		return Integer.MAX_VALUE-size();
	}

	@Override
	public boolean remove(Object o) {
		lock.lock();
		try {
			return list.remove(o);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		lock.lock();
		try {
			return list.containsAll(c);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean addAll(Collection<? extends T> c) {
		lock.lock();
		try {
			return list.addAll(c);
		}
		finally {
			notEmpty.signalAll();
			lock.unlock();
		}
	}

	@Override
	public void push(T t) {
		lock.lock();
		try {
			list.push(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public T pop() {
		lock.lock();
		try {
			return list.pop();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		lock.lock();
		try {
			return list.removeAll(c);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		lock.lock();
		try {
			return list.retainAll(c);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void clear() {
		lock.lock();
		try {
			list.clear();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public int size() {
		lock.lock();
		try {
			return list.size();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean isEmpty() {
		lock.lock();
		try {
			return list.isEmpty();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean contains(Object o) {
		lock.lock();
		try {
			return list.contains(o);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public Iterator<T> iterator() {
		return list.iterator();
	}

	@Override
	public Iterator<T> descendingIterator() {
		return list.descendingIterator();
	}

	@Override
	public Object[] toArray() {
		lock.lock();
		try {
			return list.toArray();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public <T1> T1[] toArray(T1[] a) {
		lock.lock();
		try {
			//noinspection SuspiciousToArrayCall
			return list.toArray(a);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public int drainTo(Collection<? super T> c) {
		lock.lock();
		try {
			if (!c.addAll(list))
				return 0;

			int s=list.size();
			list.clear();
			return s;
		}
		finally {
			lock.unlock();
		}
	}
	@Override
	public T peekFirst() {
		lock.lock();
		try {
			return list.peekFirst();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T peekLast() {
		lock.lock();
		try {
			return list.peekLast();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		lock.lock();
		try {
			return list.removeFirstOccurrence(o);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		lock.lock();
		try {
			return list.removeLastOccurrence(o);
		}
		finally {
			lock.unlock();
		}
	}


	@Override
	public void addFirst(T t) {
		lock.lock();
		try {
			list.addFirst(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public void addLast(T t) {
		lock.lock();
		try {
			list.addLast(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public boolean offerFirst(T t) {
		lock.lock();
		try {
			return list.offerFirst(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public boolean offerLast(T t) {
		lock.lock();
		try {
			return list.offerLast(t);
		}
		finally {
			notEmpty.signal();
			lock.unlock();
		}
	}

	@Override
	public T removeFirst() {
		lock.lock();
		try {
			return list.removeFirst();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T removeLast() {
		lock.lock();
		try {
			return list.removeLast();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T pollFirst() {
		lock.lock();
		try {
			return list.pollFirst();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T pollLast() {
		lock.lock();
		try {
			return list.pollLast();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T getFirst() {
		lock.lock();
		try {
			return list.getFirst();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public T getLast() {
		lock.lock();
		try {
			return list.getLast();
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public int drainTo(Collection<? super T> c, int maxElements) {
		if (maxElements<0)
			throw new IllegalArgumentException();
		lock.lock();
		try {
			if (maxElements<=list.size())
				return drainTo(c);
			else
			{
				int nb=0;
				for (Iterator<T> it=list.iterator();it.hasNext();) {
					c.add(it.next());
					it.remove();
					if (++nb == maxElements)
						return nb;
				}
				return nb;

			}
		}
		finally {
			lock.unlock();
		}
	}

	void setKernel(MadkitKernel kernel) {
		this.madkitKernel=kernel;
	}
}
