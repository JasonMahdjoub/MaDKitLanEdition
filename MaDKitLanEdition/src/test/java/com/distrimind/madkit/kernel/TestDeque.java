package com.distrimind.madkit.kernel;

import com.distrimind.util.CircularArrayList;
import org.junit.Test;

import java.util.Deque;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since Utils 4.8.0
 */
public abstract class TestDeque {

	public abstract Deque<String> getDequeInstance();
	@Test
	public void testFromAddLast(){
		Deque<String> deque=getDequeInstance();
		testAddLast(deque);
		testRemoveLast(deque);
		testIsEmptySize(deque);
		testClear();

	}
	@Test
	public void testFromAddFirst(){

		Deque<String> deque=getDequeInstance();
		testAddFirst(deque);
		testRemoveFirst(deque);
		testAddRemove(deque);

		testResize();
	}

	// Tests the addLast method.
	private void testAddLast(Deque<String> deque) {
		System.out.println("addLast, toString:");
		deque.addLast("seven");
		System.out.println(deque);
		deque.addLast("years");
		System.out.println(deque);
		deque.addLast("ago");
		System.out.println(deque);
	}

	// Tests the removeLast method.
	private void testRemoveLast(Deque<String> deque) {
		System.out.println();
		System.out.println("removeLast:");
		System.out.println(deque.removeLast());
		System.out.println(deque);
		System.out.println(deque.removeLast());
		System.out.println(deque);
		System.out.println(deque.removeLast());
		System.out.println(deque);
	}

	// Tests the isEmpty and size methods.
	private void testIsEmptySize(Deque<String> deque) {
		System.out.println();
		System.out.println("isEmpty / size:");
		System.out.println(deque + ", size " + deque.size() + ", empty? " + deque.isEmpty());
		deque.addLast("seven");
		System.out.println(deque + ", size " + deque.size() + ", empty? " + deque.isEmpty());
		deque.addLast("years");
		System.out.println(deque + ", size " + deque.size() + ", empty? " + deque.isEmpty());
		deque.addLast("ago");
		System.out.println(deque + ", size " + deque.size() + ", empty? " + deque.isEmpty());
		while (!deque.isEmpty()) {
			System.out.println(deque.removeLast());
			System.out.println(deque + ", size " + deque.size() + ", empty? " + deque.isEmpty());
		}
	}

	// Tests the clear method.
	private void testClear() {
		System.out.println();
		System.out.println("clear:");
		Deque<String> deque = new CircularArrayList<>();
		deque.addLast("four");
		deque.addLast("score");
		deque.addLast("and");
		deque.addLast("seven");
		deque.addLast("years");
		deque.addLast("ago");
		System.out.println(deque + ", size " + deque.size() + ", empty? " + deque.isEmpty());
		deque.clear();
		//noinspection ConstantConditions
		System.out.println(deque + ", size " + deque.size() + ", empty? " + deque.isEmpty());
	}

	// Tests the addFirst method.
	private void testAddFirst(Deque<String> deque) {
		System.out.println();
		System.out.println("addFirst:");
		deque.addFirst("and");
		System.out.println(deque);
		deque.addFirst("score");
		System.out.println(deque);
		deque.addFirst("four");
		System.out.println(deque);

		deque.addLast("seven");
		System.out.println(deque);
		deque.addLast("years");
		System.out.println(deque);
		deque.addLast("ago");
		System.out.println(deque);
	}

	// Tests the removeFirst method.
	private void testRemoveFirst(Deque<String> deque) {
		System.out.println();
		System.out.println("removeFirst:");
		while (!deque.isEmpty()) {
			System.out.println(deque.removeFirst());
			System.out.println(deque);
		}
	}

	// Tests the addFirst, addLast, removeFirst, and removeLast methods together.
	private void testAddRemove(Deque<String> deque) {
		System.out.println();
		System.out.println("add/remove-First/Last together:");
		deque.addFirst("and");
		deque.addLast("seven");
		deque.addFirst("score");
		deque.addLast("years");
		deque.addFirst("four");
		deque.addLast("ago");
		System.out.println(deque);
		while (!deque.isEmpty()) {
			System.out.println(deque.removeLast());
			System.out.println(deque);
			System.out.println(deque.removeFirst());
			System.out.println(deque);
		}
	}

	// Tests the iterator method and the for-each loop (Iterable).
	@Test
	public void testIteratorIterable() {
		System.out.println();
		System.out.println("iterator:");
		Deque<String> deque=getDequeInstance();
		deque.addFirst("and");
		deque.addLast("seven");
		deque.addFirst("score");
		deque.addLast("years");
		deque.addFirst("four");
		deque.addLast("ago");
		for (String value : deque) {
			System.out.println("iterator next() = " + value);
		}

		System.out.println();
		System.out.println("Iterable:");
		for (String s : deque) {
			System.out.println("foreach loop value = " + s);
		}

	}

	// A large test that checks whether the internal array can resize properly after many adds.
	@Test
	public void testResize() {
		System.out.println();
		System.out.println("many elements (resize):");
		Deque<String> deque=getDequeInstance();

		// String[] words = GETTYSBURG.split(" ");
		for (int i = 0; i < 50; i++) {
			if (i % 3 == 0) {
				deque.addFirst("f" + i);
				System.out.println("after adding word #f" + i + " at front: " + deque
						+ ", size " + deque.size());
			} else {
				deque.addLast("b" + i);
				System.out.println("after adding word #b" + i + " at back : " + deque
						+ ", size " + deque.size());
			}
		}

		for (int i = 0; !deque.isEmpty(); i++) {
			if (i % 2 == 0) {
				System.out.println(deque.removeLast());
			} else {
				System.out.println(deque.removeFirst());
			}
			System.out.println(deque);
		}
	}
}
