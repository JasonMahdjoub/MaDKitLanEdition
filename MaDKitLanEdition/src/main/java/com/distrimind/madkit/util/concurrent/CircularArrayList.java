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

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since Utils 4.8.0
 */
public class CircularArrayList<E> extends AbstractList<E> implements List<E>, Deque<E>, Cloneable, java.io.Serializable, RandomAccess{
	final static int DEFAULT_BASE_SIZE=32;
	final static boolean DEFAULT_EXTENSIBLE=true;

	private Object[] array;
	private int baseSize;
	private int size;
	private boolean extensibleSize;
	private int position;
	public CircularArrayList() {
		this(DEFAULT_BASE_SIZE, DEFAULT_EXTENSIBLE);
	}
	public CircularArrayList(int baseSize) {
		this(baseSize, DEFAULT_EXTENSIBLE);
	}
	private CircularArrayList(CircularArrayList<E> l) {
		this.array=l.array.clone();
		this.baseSize=l.baseSize;
		this.size=l.size;
		this.extensibleSize=l.extensibleSize;
		this.position=l.position;
	}
	public CircularArrayList(int baseSize, boolean extensibleSize) {
		if (baseSize <1)
			throw new IllegalArgumentException();

		this.baseSize = baseSize;
		array=new Object[baseSize];
		this.size = 0;
		this.extensibleSize = extensibleSize;
		this.position=0;
	}
	public CircularArrayList(Collection<E> collection) {
		this(DEFAULT_BASE_SIZE, DEFAULT_EXTENSIBLE, collection);
	}
	public CircularArrayList(int baseSize, boolean extensibleSize, Collection<E> collection) {
		if (baseSize <1)
			throw new IllegalArgumentException();
		if (collection==null)
			throw new NullPointerException();
		if (!extensibleSize && collection.size()> baseSize)
			throw new IllegalArgumentException();
		this.baseSize = baseSize;
		this.position=0;
		array=new Object[Math.max(collection.size(), baseSize)];
		int i=0;
		for (E e : collection)
			array[i++]=e;
		this.size = i;
		this.extensibleSize = extensibleSize;
	}

	private void ensureCapacity(int nb)
	{
		int requestedCapacity=size+nb;
		if (array.length<requestedCapacity)
		{
			if (extensibleSize)
			{
				Object[] o=new Object[requestedCapacity*2];
				int s=array.length-position;
				if (size<=s)
					System.arraycopy(array, position, o, 0, size);
				else
				{
					System.arraycopy(array, position, o, 0, s);
					System.arraycopy(array, 0, o, s, size-s);
				}
				array=o;
				position=0;
			}
			else
			{
				throw new OutOfMemoryError();
			}
		}
	}

	private void ensureSizeReduction()
	{
		if (extensibleSize)
		{
			if (array.length>baseSize && array.length/4>size)
			{
				Object[] o=new Object[Math.max(size*2, baseSize)];
				int s=array.length-position;
				if (size<=s)
					System.arraycopy(array, position, o, 0, size);
				else
				{
					System.arraycopy(array, position, o, 0, s);
					System.arraycopy(array, 0, o, s, size-s);
				}
				array=o;
				position=0;
			}
		}
	}

	public boolean isExtensibleSize() {
		return extensibleSize;
	}

	@Override
	public void addFirst(E e) {
		ensureCapacity(1);
		if (--position<0)
			position+=array.length;

		array[position]=e;
		++size;
	}

	@Override
	public void addLast(E e) {
		ensureCapacity(1);
		array[(position+(size++))%array.length]=e;
	}

	@Override
	public boolean offerFirst(E e) {
		try
		{
			addFirst(e);
			return true;
		}
		catch (OutOfMemoryError ignored)
		{
			return false;
		}

	}

	@Override
	public boolean offerLast(E e) {
		try
		{
			addLast(e);
			return true;
		}
		catch (OutOfMemoryError ignored)
		{
			return false;
		}
	}

	@Override
	public E removeFirst() {
		if (size==0)
			throw new NoSuchElementException();
		--size;
		//noinspection unchecked
		E r=(E)array[position];
		array[position++]=null;
		position%=array.length;
		ensureSizeReduction();
		return r;
	}

	@Override
	public E removeLast() {
		if (size==0)
			throw new NoSuchElementException();

		int i=(position+(--size))%array.length;
		//noinspection unchecked
		E r= (E)array[i];
		array[i]=null;
		ensureSizeReduction();
		return r;
	}

	@Override
	public E pollFirst() {
		if (size==0)
			return null;
		return removeFirst();

	}

	@Override
	public E pollLast() {
		if (size==0)
			return null;
		return removeLast();
	}

	@Override
	public E getFirst() {
		if (size==0)
			throw new NoSuchElementException();
		//noinspection unchecked
		return (E)array[position];

	}

	@Override
	public E getLast() {
		if (size==0)
			throw new NoSuchElementException();
		//noinspection unchecked
		return (E)array[(position+size-1)%array.length];
	}

	@Override
	public E peekFirst() {
		if (size==0)
			return null;
		//noinspection unchecked
		return (E)array[position];
	}

	@Override
	public E peekLast() {
		if (size==0)
			return null;
		//noinspection unchecked
		return (E)array[(position+size-1)%array.length];
	}

	@Override
	public boolean removeFirstOccurrence(Object o) {
		int i=indexOf(o);
		if (i>=0) {
			remove(i);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean removeLastOccurrence(Object o) {
		int i=lastIndexOf(o);
		if (i>=0) {
			remove(i);
			return true;
		}
		else
			return false;
	}

	@Override
	public boolean offer(E e) {
		return offerLast(e);
	}

	@Override
	public E remove() {
		return removeFirst();
	}

	@Override
	public E poll() {
		return pollFirst();
	}

	@Override
	public E element() {
		return getFirst();
	}

	@Override
	public E peek() {
		return peekFirst();
	}

	@Override
	public void push(E e) {
		addLast(e);
	}

	@Override
	public E pop() {
		return removeFirst();
	}

	@Override
	public Iterator<E> descendingIterator() {
		return new Iterator<E>() {
			int index=size;


			@Override
			public boolean hasNext() {
				return index>=0;
			}

			@Override
			public E next() {
				if (hasNext()) {
					//noinspection unchecked
					return (E) array[(--index+position)%array.length];
				}
				else
				{
					throw new NoSuchElementException();
				}
			}

			@Override
			public void remove() {
				if( size==0)
					throw new IllegalStateException();
				CircularArrayList.this.remove(index);
			}
		};
	}

	@Override
	public int size() {
		return size;
	}

	@Override
	public boolean isEmpty() {
		return size==0;
	}

	@Override
	public boolean contains(Object o) {
		for (Object e : this)
		{
			if (Objects.equals(e, o))
				return true;
		}
		return false;
	}

	@Override
	public Iterator<E> iterator() {
		return new ListIt();
	}

	@Override
	public Object[] toArray() {
		Object[] res=new Object[size];
		int i=0;
		for (E e : this)
			res[i++]=e;
		return res;
	}

	@Override
	public <T> T[] toArray(T[] a) {
		if (a.length < size)
			//noinspection unchecked
			a = (T[])java.lang.reflect.Array.newInstance(
					a.getClass().getComponentType(), size);
		int i=0;
		for (E e : this)
			//noinspection unchecked
			a[i++]=(T)e;
		for (int j=i;j<a.length;j++)
			a[j]=null;
		return a;
	}

	@Override
	public boolean add(E e) {
		return offerLast(e);
	}

	@Override
	public boolean remove(Object o) {
		return this.removeFirstOccurrence(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		LinkedList<?> l=new LinkedList<>(c);
		for (Object e : this)
		{
			for (Iterator<?> it=l.iterator();it.hasNext();) {
				if (Objects.equals(e, it.next())) {
					it.remove();
					break;
				}
			}
			if (l.size()==0)
				return true;
		}
		return false;
	}

	@Override
	public boolean addAll(Collection<? extends E> c) {
		try {
			ensureCapacity(c.size());
			int i=size+position;
			for (Object o : c)
				array[(i++)%array.length] = o;
			size+=c.size();
			return true;
		}
		catch (OutOfMemoryError ignored)
		{
			return false;
		}
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> c) {
		if (index<0 || index>size)
			throw new IllegalArgumentException();
		if (c==null)
			throw new NullPointerException();
		try {
			ensureCapacity(c.size());
			int end=c.size()+index;
			size+=c.size();
			for (int i=size;i>end;--i)
			{
				array[(i+position)%array.length]=array[(i-1+position)%array.length];
			}
			int i=index;
			for (Object o : c )
				array[((i++)+position)%array.length]=o;
			return true;
		}
		catch (OutOfMemoryError ignored)
		{
			return false;
		}
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		boolean changed=false;
		for (Object o : c)
			changed|=remove(o);
		return changed;

	}

	@Override
	public boolean retainAll(Collection<?> c) {
		boolean changed=false;
		for (Iterator<E> it=iterator();it.hasNext();)
		{
			if (!c.contains(it.next())) {
				it.remove();
				changed=true;
			}
		}
		return changed;
	}

	@Override
	public void clear() {
		for (int i=0;i<size;i++)
			array[(i+position)%array.length]=null;
		size=0;
		position=0;
	}

	@Override
	public E get(int index) {
		if (index<0 || index>=size)
			throw new IndexOutOfBoundsException();
		//noinspection unchecked
		return (E)array[(index+position)%array.length];
	}

	@Override
	public E set(int index, E element) {
		if (index<0 || index>=size)
			throw new IndexOutOfBoundsException();

		int p=(index+position)%array.length;
		//noinspection unchecked
		E res=(E)array[p];
		array[p]=element;
		return res;
	}

	@Override
	public void add(int index, E element) {
		if (index<0 || index>size)
			throw new IllegalArgumentException();
		ensureCapacity(1);
		int end=1+index;
		size+=1;
		for (int i=size;i>end;--i)
		{
			array[(i+position)%array.length]=array[(i-1+position)%array.length];
		}

		array[(index+position)%array.length]=element;
	}

	@SuppressWarnings("unchecked")
	@Override
	public E remove(int index) {
		if (index<0 || index>=size)
			throw new IndexOutOfBoundsException();
		E res;
		if (index==0)
		{
			res=(E)array[position];
			array[position++]=null;
			position%=array.length;
		}
		else if (index==size-1)
		{
			int p=(index+position)%array.length;
			res=(E)array[p];
			array[p]=null;
		}
		else {
			int p = (index + position) % array.length;
			res = (E) array[p];
			int s = array.length - position;
			if (size <= s) {
				System.arraycopy(array, p + 1, array, p, size - 1);
			} else {
				System.arraycopy(array, p + 1, array, p, s - 1);
				array[array.length - 1] = array[0];
				s = size - s - 1;
				if (s > 0)
					System.arraycopy(array, 1, array, 0, s);
				array[s] = null;
			}

		}
		--size;
		ensureSizeReduction();
		return res;
	}

	@Override
	public int indexOf(Object o) {
		for (int i=0;i<size;i++)
		{
			Object e=array[(i+position)%array.length];
			if (Objects.equals(e, o))
			{
				return i;
			}
		}
		return -1;
	}

	@Override
	public int lastIndexOf(Object o) {
		for (int i=0;i<size;i++)
		{
			Object e=array[(i+position)%array.length];
			if (Objects.equals(e, o))
			{
				return i;
			}
		}
		return -1;
	}

	@Override
	public ListIterator<E> listIterator() {
		return new ListIt();
	}

	@Override
	public ListIterator<E> listIterator(int index) {
		if (index<0 || index>size)
			throw new IndexOutOfBoundsException();
		ListIt lit=new ListIt();
		lit.index=index;
		return lit;
	}

	private class ListIt implements ListIterator<E>
	{

		int index=0;
		boolean getCalled=false;
		int cur=0;

		@Override
		public boolean hasNext() {
			getCalled=false;
			return index<size;
		}

		@Override
		public E next() {
			//noinspection unchecked
			return (E) array[(nextIndex()+position)%array.length];
		}

		@Override
		public boolean hasPrevious() {
			return index>0;
		}


		@Override
		public E previous() {
			//noinspection unchecked
			return (E) array[(previousIndex()+position)%array.length];

		}

		@Override
		public int nextIndex() {
			if (!hasNext())
				throw new NoSuchElementException();
			cur=index;
			if (!getCalled) {
				getCalled=true;
				++index;
			}
			return cur;
		}

		@Override
		public int previousIndex() {
			if (!hasPrevious())
				throw new NoSuchElementException();

			if (getCalled) {
				cur=index;
			}
			else {
				cur = --index;
				getCalled = true;
			}
			return cur;
		}

		@Override
		public void remove() {
			if( size==0)
				throw new IllegalStateException();
			CircularArrayList.this.remove(cur);
		}

		@Override
		public void set(E e) {
			array[(cur+position)%array.length]=e;
		}

		@Override
		public void add(E e) {
			CircularArrayList.this.add(cur, e);
			++index;
		}

	}

	@Override
	public CircularArrayList<E> subList(int fromIndex, int toIndex) {
		CircularArrayList<E> list=new CircularArrayList<>(baseSize, extensibleSize);
		for (int i=fromIndex;i<toIndex;i++)
			//noinspection unchecked
			list.add((E)array[(i+position)%array.length]);
		return list;
	}

	@SuppressWarnings("MethodDoesntCallSuperMethod")
	@Override
	public CircularArrayList<E> clone() {
		return new CircularArrayList<>(this);
	}
}
