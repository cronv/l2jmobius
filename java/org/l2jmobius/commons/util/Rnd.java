/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.commons.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * A utility class for generating random numbers and values.<br>
 * It provides a simplified wrapper around {@code ThreadLocalRandom} to handle common randomization tasks.
 * @author Mobius
 * @since September 15th 2018
 */
public class Rnd
{
	private static final int MINIMUM_POSITIVE_INT = 1;
	private static final long MINIMUM_POSITIVE_LONG = 1L;
	private static final float MINIMUM_POSITIVE_FLOAT = Float.intBitsToFloat(0x1);
	private static final double MINIMUM_POSITIVE_DOUBLE = Double.longBitsToDouble(0x1L);
	
	/**
	 * Generates a random {@code boolean} value.<br>
	 * This method returns either {@code true} or {@code false}.
	 * @return The next random {@code boolean} value.
	 */
	public static boolean nextBoolean()
	{
		return ThreadLocalRandom.current().nextBoolean();
	}
	
	/**
	 * Fills an array with random bytes.<br>
	 * This method uses {@code ThreadLocalRandom} to populate the data.<br>
	 * The original values in the array will be overwritten.
	 * @param bytes The {@code byte[]} array to be filled with random values.
	 */
	public static void nextBytes(byte[] bytes)
	{
		ThreadLocalRandom.current().nextBytes(bytes);
	}
	
	/**
	 * Generates a random {@code int} value.<br>
	 * The result is between {@code 0} (inclusive) and the provided {@code bound} (exclusive).<br>
	 * If the {@code bound} is less than or equal to {@code 0}, it returns {@code 0}.
	 * @param bound The upper limit for the random number.
	 * @return A random {@code int} between {@code 0} and {@code bound}.
	 */
	public static int get(int bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextInt(bound);
	}
	
	/**
	 * Generates a random {@code int} value within a specific range.<br>
	 * The result is inclusive of both the {@code origin} and the {@code bound}.<br>
	 * The only exception is when {@code bound} equals {@link Integer#MAX_VALUE}, which remains exclusive to prevent overflow.<br>
	 * If {@code origin} is greater than or equal to {@code bound}, it returns {@code origin}.
	 * @param origin The starting value of the range.
	 * @param bound The ending value of the range.
	 * @return A random {@code int} between {@code origin} and {@code bound}.
	 */
	public static int get(int origin, int bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextInt(origin, bound == Integer.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_INT);
	}
	
	/**
	 * Generates a random {@code int} value.<br>
	 * This method uses {@code ThreadLocalRandom} to provide the value.
	 * @return A random {@code int} value.
	 */
	public static int nextInt()
	{
		return ThreadLocalRandom.current().nextInt();
	}
	
	/**
	 * Generates a random {@code long} value.<br>
	 * The result is between {@code 0} (inclusive) and the provided {@code bound} (exclusive).<br>
	 * If the {@code bound} is less than or equal to {@code 0}, it returns {@code 0}.
	 * @param bound The upper limit for the random number.
	 * @return A random {@code long} between {@code 0} and {@code bound}.
	 */
	public static long get(long bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextLong(bound);
	}
	
	/**
	 * Generates a random {@code long} value within a specific range.<br>
	 * The result is inclusive of both the {@code origin} and the {@code bound}.<br>
	 * The only exception is when {@code bound} equals {@link Long#MAX_VALUE}, which remains exclusive to prevent overflow.<br>
	 * If {@code origin} is greater than or equal to {@code bound}, it returns {@code origin}.
	 * @param origin The starting value of the range.
	 * @param bound The ending value of the range.
	 * @return A random {@code long} between {@code origin} and {@code bound}.
	 */
	public static long get(long origin, long bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextLong(origin, bound == Long.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_LONG);
	}
	
	/**
	 * Generates a random {@code long} value.<br>
	 * This method uses the current thread's random number generator.
	 * @return A random {@code long} value.
	 */
	public static long nextLong()
	{
		return ThreadLocalRandom.current().nextLong();
	}
	
	/**
	 * Generates a random {@code float} value.<br>
	 * The result is between {@code 0.0} (inclusive) and the provided {@code bound} (exclusive).<br>
	 * If the {@code bound} is less than or equal to {@code 0}, it returns {@code 0}.
	 * @param bound The upper limit for the random number.
	 * @return A random {@code float} between {@code 0.0} and {@code bound}.
	 */
	public static float get(float bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextFloat(bound);
	}
	
	/**
	 * Generates a random {@code float} value within a specific range.<br>
	 * The result is inclusive of both the {@code origin} and the {@code bound}.<br>
	 * The only exception is when {@code bound} equals {@link Float#MAX_VALUE}, which remains exclusive to prevent overflow.<br>
	 * If {@code origin} is greater than or equal to {@code bound}, it returns {@code origin}.
	 * @param origin The starting value of the range.
	 * @param bound The ending value of the range.
	 * @return A random {@code float} between {@code origin} and {@code bound}.
	 */
	public static float get(float origin, float bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextFloat(origin, bound == Float.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_FLOAT);
	}
	
	/**
	 * Generates a random {@code float} value.<br>
	 * This method uses the current thread's random number generator.
	 * @return A random {@code float} between {@code 0.0} (inclusive) and {@code 1.0} (exclusive).
	 */
	public static float nextFloat()
	{
		return ThreadLocalRandom.current().nextFloat();
	}
	
	/**
	 * Generates a random {@code double} value.<br>
	 * The result is between {@code 0.0} (inclusive) and the provided {@code bound} (exclusive).<br>
	 * If the {@code bound} is less than or equal to {@code 0}, it returns {@code 0}.
	 * @param bound The upper limit for the random number.
	 * @return A random {@code double} between {@code 0.0} and {@code bound}.
	 */
	public static double get(double bound)
	{
		return bound <= 0 ? 0 : ThreadLocalRandom.current().nextDouble(bound);
	}
	
	/**
	 * Generates a random {@code double} value within a specific range.<br>
	 * The result is inclusive of both the {@code origin} and the {@code bound}.<br>
	 * The only exception is when {@code bound} equals {@link Double#MAX_VALUE}, which remains exclusive to prevent overflow.<br>
	 * If {@code origin} is greater than or equal to {@code bound}, it returns {@code origin}.
	 * @param origin The starting value of the range.
	 * @param bound The ending value of the range.
	 * @return A random {@code double} between {@code origin} and {@code bound}.
	 */
	public static double get(double origin, double bound)
	{
		return origin >= bound ? origin : ThreadLocalRandom.current().nextDouble(origin, bound == Double.MAX_VALUE ? bound : bound + MINIMUM_POSITIVE_DOUBLE);
	}
	
	/**
	 * Generates a random {@code double} value.<br>
	 * This method uses the current thread's random number generator.
	 * @return A random {@code double} between {@code 0.0} (inclusive) and {@code 1.0} (exclusive).
	 */
	public static double nextDouble()
	{
		return ThreadLocalRandom.current().nextDouble();
	}
	
	/**
	 * Generates a random number from a Gaussian distribution.<br>
	 * This method uses the {@code ThreadLocalRandom} class to provide the value.<br>
	 * The result follows a normal distribution with a mean of {@code 0.0} and a standard deviation of {@code 1.0}.
	 * @return A random {@code double} value from a Gaussian distribution.
	 */
	public static double nextGaussian()
	{
		return ThreadLocalRandom.current().nextGaussian();
	}
}
