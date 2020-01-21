package com.distrimind.madkit.util.concurrent;

import org.junit.Test;
import org.hamcrest.collection.IsEmptyCollection;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.hamcrest.collection.IsIterableContainingInOrder.contains;

import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.number.OrderingComparison.lessThan;

import static org.hamcrest.MatcherAssert.assertThat;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

/**
 *
 * @version 1.0
 * @since Utils 4.8.0
 */
public class TestCircularArrayList extends TestList{
	@Override
	protected <T> List<T> getListInstance(Collection<T> c) {
		return new CircularArrayList<>(c);
	}
}
