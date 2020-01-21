package com.distrimind.madkit.util.concurrent;

import org.hamcrest.collection.IsEmptyCollection;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThan;

/**
 * @author Jason Mahdjoub
 * @version 1.0
 * @since Utils 4.8.0
 */
public abstract class TestList {
	protected  abstract <T> List<T> getListInstance(Collection<T> c);

	@Test
	public void testAssertListStrings() {

		List<String> actual = getListInstance( Arrays.asList("a", "b", "c"));
		List<String> expected = getListInstance( Arrays.asList("a", "b", "c"));

		//All passed / true

		//1. Test equal.
		assertThat(actual, is(expected));

		//2. If List has this value?
		assertThat(actual, hasItems("b"));

		//3. Check List Size
		assertThat(actual, hasSize(3));

		assertThat(actual.size(), is(3));

		//4.  List order

		// Ensure Correct order
		assertThat(actual, contains("a", "b", "c"));

		// Can be any order
		assertThat(actual, containsInAnyOrder("c", "b", "a"));

		//5. check empty list
		assertThat(actual, not(IsEmptyCollection.empty()));

		assertThat(new CircularArrayList<>(), IsEmptyCollection.empty());

	}

	@Test
	public void testAssertListInteger() {

		List<Integer> actual = getListInstance( Arrays.asList(1, 2, 3, 4, 5));
		List<Integer> expected = getListInstance(Arrays.asList(1, 2, 3, 4, 5));

		//All passed / true

		//1. Test equal.
		assertThat(actual, is(expected));

		//2. Check List has this value
		assertThat(actual, hasItems(2));

		//3. Check List Size
		assertThat(actual, hasSize(5));

		assertThat(actual.size(), is(5));

		//4.  List order

		// Ensure Correct order
		assertThat(actual, contains(1, 2, 3, 4, 5));

		// Can be any order
		assertThat(actual, containsInAnyOrder(5, 4, 3, 2, 1));

		//5. check empty list
		assertThat(actual, not(IsEmptyCollection.empty()));

		assertThat(new CircularArrayList<>(), IsEmptyCollection.empty());

		//6. Test numeric comparisons
		assertThat(actual, everyItem(greaterThanOrEqualTo(1)));

		assertThat(actual, everyItem(lessThan(10)));

	}

	@Test
	public void testAssertListObjects() {

		List<Fruit> list = getListInstance(Arrays.asList(
				new Fruit("Banana", 99),
				new Fruit("Apple", 20)
		));

		//Test equals
		assertThat(list, hasItems(
				new Fruit("Banana", 99),
				new Fruit("Apple", 20)
		));

		assertThat(list, containsInAnyOrder(
				new Fruit("Apple", 20),
				new Fruit("Banana", 99)
		));

		//Test class property, and its value
		assertThat(list, containsInAnyOrder(
				hasProperty("name", is("Apple")),
				hasProperty("name", is("Banana"))
		));

	}

	public static class Fruit {

		public Fruit(String name, int qty) {
			this.name = name;
			this.qty = qty;
		}

		private String name;
		private int qty;

		public int getQty() {
			return qty;
		}

		public void setQty(int qty) {
			this.qty = qty;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		//Test equal, override equals() and hashCode()
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Fruit fruit = (Fruit) o;
			return qty == fruit.qty &&
					Objects.equals(name, fruit.name);
		}

		@Override
		public int hashCode() {
			return Objects.hash(name, qty);
		}
	}

}
