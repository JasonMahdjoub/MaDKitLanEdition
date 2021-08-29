package com.distrimind.madkit;

import java.nio.ByteBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;

import org.testng.AssertJUnit;

public class JUnitFunctions {
	public static void assertNotEquals(Object expected, Object actual) {
		assertNotEquals(null, expected, actual);
		
	}
	public static void assertNotEquals(final String message, Object expected, Object actual) {
		if (expected == actual) {
			failEquals(message, expected, actual);
		} else if ((expected == null) || !expected.equals(actual)) {
			return;
		}
		failEquals(message, expected, actual);

	}


	private static void failEquals(String message, Object expected, @SuppressWarnings("unused") Object actual) {
		String formatted = "";
		if (message != null) {
			formatted = message + " ";
		}
		AssertJUnit.fail(formatted + "expected not same:<" + expected + ">");
	}

	public static void assertNotEquals(String message, long expected, long actual) {
		assertNotEquals(message, Long.valueOf(expected), Long.valueOf(actual));
	}

	public static void assertNotEquals(long expected, long actual) {
		assertNotEquals(null, expected, actual);
	}

	public static void assertNotEquals(String message, boolean expected, boolean actual) {
		assertNotEquals(message, Boolean.valueOf(expected), Boolean.valueOf(actual));
	}

	public static void assertNotEquals(boolean expected, boolean actual) {
		assertNotEquals(null, expected, actual);
	}

	public static void assertNotEquals(String message, byte expected, byte actual) {
		assertNotEquals(message, Byte.valueOf(expected), Byte.valueOf(actual));
	}

	public static void assertNotEquals(byte expected, byte actual) {
		assertNotEquals(null, expected, actual);
	}

	public static void assertNotEquals(String message, char expected, char actual) {
		assertNotEquals(message, Character.valueOf(expected), Character.valueOf(actual));
	}

	public static void assertNotEquals(char expected, char actual) {
		assertNotEquals(null, expected, actual);
	}

	public static void assertNotEquals(String message, short expected, short actual) {
		assertNotEquals(message, Short.valueOf(expected), Short.valueOf(actual));
	}

	public static void assertNotEquals(short expected, short actual) {
		assertNotEquals(null, expected, actual);
	}

	public static void assertNotEquals(String message, int expected, int actual) {
		assertNotEquals(message, Integer.valueOf(expected), Integer.valueOf(actual));
	}

	public static void assertNotEquals(int expected, int actual) {
		assertNotEquals(null, expected, actual);
	}
	
	public static void assertNotEquals(String message, float expected, float actual) {
		assertNotEquals(message, Float.valueOf(expected), Float.valueOf(actual));
	}

	public static void assertNotEquals(float expected, float actual) {
		assertNotEquals(null, expected, actual);
	}
	
	public static void assertNotEquals(String message, double expected, double actual) {
		assertNotEquals(message, Double.valueOf(expected), Double.valueOf(actual));
	}

	public static void assertNotEquals(double expected, double actual) {
		assertNotEquals(null, expected, actual);
	}
	
	public static void assertNotEquals(String message, byte[] expected, byte[] actual) {
		assertNotEquals(message, ByteBuffer.wrap(expected), ByteBuffer.wrap(actual));
	}

	public static void assertNotEquals(byte[] expected, byte[] actual) {
		assertNotEquals(null, expected, actual);
	}
	public static void assertNotEquals(String message, long[] expected, long[] actual) {
		assertNotEquals(message, LongBuffer.wrap(expected), LongBuffer.wrap(actual));
	}

	public static void assertNotEquals(long[] expected, long[] actual) {
		assertNotEquals(null, expected, actual);
	}
	
	public static void assertNotEquals(String message, int[] expected, int[] actual) {
		assertNotEquals(message, IntBuffer.wrap(expected), IntBuffer.wrap(actual));
	}

	public static void assertNotEquals(int[] expected, int[] actual) {
		assertNotEquals(null, expected, actual);
	}
	
	public static void assertNotEquals(String message, float[] expected, float[] actual) {
		assertNotEquals(message, FloatBuffer.wrap(expected), FloatBuffer.wrap(actual));
	}

	public static void assertNotEquals(float[] expected, float[] actual) {
		assertNotEquals(null, expected, actual);
	}
	
	public static void assertNotEquals(String message, short[] expected, short[] actual) {
		assertNotEquals(message, ShortBuffer.wrap(expected), ShortBuffer.wrap(actual));
	}

	public static void assertNotEquals(short[] expected, short[] actual) {
		assertNotEquals(null, expected, actual);
	}
	
	public static void assertNotEquals(String message, double[] expected, double[] actual) {
		assertNotEquals(message, DoubleBuffer.wrap(expected), DoubleBuffer.wrap(actual));
	}

	public static void assertNotEquals(double[] expected, double[] actual) {
		assertNotEquals(null, expected, actual);
	}
}
