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
package org.l2jmobius.gameserver.util;

/**
 * Utility methods for arithmetic, division, clamping, range scaling, distance and power operations.
 * @author Mobius
 */
public class MathUtil
{
	/**
	 * Adds two {@code byte} values, casting the result to {@code byte}.
	 * @param a first value to add
	 * @param b second value to add
	 * @return sum of the values as a {@code byte}
	 */
	public static byte add(byte a, byte b)
	{
		return (byte) (a + b);
	}
	
	/**
	 * Adds two {@code short} values, casting the result to {@code short}.
	 * @param a first value to add
	 * @param b second value to add
	 * @return sum of the values as a {@code short}
	 */
	public static short add(short a, short b)
	{
		return (short) (a + b);
	}
	
	/**
	 * Adds two {@code int} values.
	 * @param a first value to add
	 * @param b second value to add
	 * @return sum of the values
	 */
	public static int add(int a, int b)
	{
		return a + b;
	}
	
	/**
	 * Adds two {@code double} values.
	 * @param a first value to add
	 * @param b second value to add
	 * @return sum of the values
	 */
	public static double add(double a, double b)
	{
		return a + b;
	}
	
	/**
	 * Multiplies two {@code byte} values, casting the result to {@code byte}.
	 * @param a first factor
	 * @param b second factor
	 * @return product of the values as a {@code byte}
	 */
	public static byte mul(byte a, byte b)
	{
		return (byte) (a * b);
	}
	
	/**
	 * Multiplies two {@code short} values, casting the result to {@code short}.
	 * @param a first factor
	 * @param b second factor
	 * @return product of the values as a {@code short}
	 */
	public static short mul(short a, short b)
	{
		return (short) (a * b);
	}
	
	/**
	 * Multiplies two {@code int} values.
	 * @param a first factor
	 * @param b second factor
	 * @return product of the values
	 */
	public static int mul(int a, int b)
	{
		return a * b;
	}
	
	/**
	 * Multiplies two {@code double} values.
	 * @param a first factor
	 * @param b second factor
	 * @return product of the values
	 */
	public static double mul(double a, double b)
	{
		return a * b;
	}
	
	/**
	 * Divides one {@code byte} by another using integer division.
	 * @param a dividend
	 * @param b divisor
	 * @return quotient as a {@code byte}
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static byte div(byte a, byte b)
	{
		if (b == 0)
		{
			throw new ArithmeticException("Division by zero is not allowed for byte values.");
		}
		
		return (byte) (a / b);
	}
	
	/**
	 * Divides one {@code short} by another using integer division.
	 * @param a dividend
	 * @param b divisor
	 * @return quotient as a {@code short}
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static short div(short a, short b)
	{
		if (b == 0)
		{
			throw new ArithmeticException("Division by zero is not allowed for short values.");
		}
		
		return (short) (a / b);
	}
	
	/**
	 * Divides one {@code int} by another using integer division.
	 * @param a dividend
	 * @param b divisor
	 * @return quotient as an {@code int}
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static int div(int a, int b)
	{
		if (b == 0)
		{
			throw new ArithmeticException("Division by zero is not allowed for int values.");
		}
		
		return a / b;
	}
	
	/**
	 * Divides one {@code double} by another using floating-point division.
	 * @param a dividend
	 * @param b divisor
	 * @return quotient
	 * @throws ArithmeticException if {@code b} is zero
	 */
	public static double div(double a, double b)
	{
		if (b == 0d)
		{
			throw new ArithmeticException("Division by zero is not allowed for double values.");
		}
		
		return a / b;
	}
	
	/**
	 * Restricts an {@code int} to the inclusive range between two bounds.
	 * @param value value to clamp
	 * @param min minimum allowed value
	 * @param max maximum allowed value
	 * @return clamped value
	 */
	public static int clamp(int value, int min, int max)
	{
		return max(min, min(max, value));
	}
	
	/**
	 * Restricts a {@code long} to the inclusive range between two bounds.
	 * @param value value to clamp
	 * @param min minimum allowed value
	 * @param max maximum allowed value
	 * @return clamped value
	 */
	public static long clamp(long value, long min, long max)
	{
		return max(min, min(max, value));
	}
	
	/**
	 * Restricts a {@code float} to the inclusive range between two bounds.
	 * @param value value to clamp
	 * @param min minimum allowed value
	 * @param max maximum allowed value
	 * @return clamped value
	 */
	public static float clamp(float value, float min, float max)
	{
		return max(min, min(max, value));
	}
	
	/**
	 * Restricts a {@code double} to the inclusive range between two bounds.
	 * @param value value to clamp
	 * @param min minimum allowed value
	 * @param max maximum allowed value
	 * @return clamped value
	 */
	public static double clamp(double value, double min, double max)
	{
		return max(min, min(max, value));
	}
	
	/**
	 * Clamps an {@code int} to the source range and linearly maps it to the target range.
	 * @param value value to scale
	 * @param sourceMin minimum bound of the source range
	 * @param sourceMax maximum bound of the source range
	 * @param targetMin minimum bound of the target range
	 * @param targetMax maximum bound of the target range
	 * @return scaled value as an {@code int}
	 */
	public static int scaleToRange(int value, int sourceMin, int sourceMax, int targetMin, int targetMax)
	{
		final int clampedValue = clamp(value, sourceMin, sourceMax);
		return (((clampedValue - sourceMin) * (targetMax - targetMin)) / (sourceMax - sourceMin)) + targetMin;
	}
	
	/**
	 * Clamps a {@code long} to the source range and linearly maps it to the target range.
	 * @param value value to scale
	 * @param sourceMin minimum bound of the source range
	 * @param sourceMax maximum bound of the source range
	 * @param targetMin minimum bound of the target range
	 * @param targetMax maximum bound of the target range
	 * @return scaled value as a {@code long}
	 */
	public static long scaleToRange(long value, long sourceMin, long sourceMax, long targetMin, long targetMax)
	{
		final long clampedValue = clamp(value, sourceMin, sourceMax);
		return (((clampedValue - sourceMin) * (targetMax - targetMin)) / (sourceMax - sourceMin)) + targetMin;
	}
	
	/**
	 * Clamps a {@code double} to the source range and linearly maps it to the target range.
	 * @param value value to scale
	 * @param sourceMin minimum bound of the source range
	 * @param sourceMax maximum bound of the source range
	 * @param targetMin minimum bound of the target range
	 * @param targetMax maximum bound of the target range
	 * @return scaled value as a {@code double}
	 */
	public static double scaleToRange(double value, double sourceMin, double sourceMax, double targetMin, double targetMax)
	{
		final double clampedValue = clamp(value, sourceMin, sourceMax);
		return (((clampedValue - sourceMin) * (targetMax - targetMin)) / (sourceMax - sourceMin)) + targetMin;
	}
	
	/**
	 * Returns the index of the first occurrence of the smallest value in an array.
	 * @param array values to search
	 * @return index of the smallest value
	 */
	public static int getIndexOfMinValue(int... array)
	{
		int minIndex = 0;
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] < array[minIndex])
			{
				minIndex = i;
			}
		}
		
		return minIndex;
	}
	
	/**
	 * Returns the index of the first occurrence of the largest value in an array.
	 * @param array values to search
	 * @return index of the largest value
	 */
	public static int getIndexOfMaxValue(int... array)
	{
		int maxIndex = 0;
		for (int i = 1; i < array.length; i++)
		{
			if (array[i] > array[maxIndex])
			{
				maxIndex = i;
			}
		}
		
		return maxIndex;
	}
	
	/**
	 * Returns the smallest of the given {@code int} values.
	 * @param values values to compare
	 * @return smallest value
	 */
	public static int min(int... values)
	{
		int minValue = values[0];
		for (int value : values)
		{
			if (value < minValue)
			{
				minValue = value;
			}
		}
		
		return minValue;
	}
	
	/**
	 * Returns the largest of the given {@code int} values.
	 * @param values values to compare
	 * @return largest value
	 */
	public static int max(int... values)
	{
		int maxValue = values[0];
		for (int value : values)
		{
			if (value > maxValue)
			{
				maxValue = value;
			}
		}
		
		return maxValue;
	}
	
	/**
	 * Returns the smallest of the given {@code long} values.
	 * @param values values to compare
	 * @return smallest value
	 */
	public static long min(long... values)
	{
		long minValue = values[0];
		for (long value : values)
		{
			if (value < minValue)
			{
				minValue = value;
			}
		}
		
		return minValue;
	}
	
	/**
	 * Returns the largest of the given {@code long} values.
	 * @param values values to compare
	 * @return largest value
	 */
	public static long max(long... values)
	{
		long maxValue = values[0];
		for (long value : values)
		{
			if (value > maxValue)
			{
				maxValue = value;
			}
		}
		
		return maxValue;
	}
	
	/**
	 * Returns the smallest of the given {@code float} values, treating {@code NaN} as the smallest.
	 * @param values values to compare
	 * @return smallest value
	 */
	public static float min(float... values)
	{
		float minValue = values[0];
		for (float value : values)
		{
			if (Float.isNaN(value) || (value < minValue))
			{
				minValue = value;
			}
		}
		
		return minValue;
	}
	
	/**
	 * Returns the largest of the given {@code float} values, treating {@code NaN} as the largest.
	 * @param values values to compare
	 * @return largest value
	 */
	public static float max(float... values)
	{
		float maxValue = values[0];
		for (float value : values)
		{
			if (Float.isNaN(value) || (value > maxValue))
			{
				maxValue = value;
			}
		}
		
		return maxValue;
	}
	
	/**
	 * Returns the smallest of the given {@code double} values, treating {@code NaN} as the smallest.
	 * @param values values to compare
	 * @return smallest value
	 */
	public static double min(double... values)
	{
		double minValue = values[0];
		for (double value : values)
		{
			if (Double.isNaN(value) || (value < minValue))
			{
				minValue = value;
			}
		}
		
		return minValue;
	}
	
	/**
	 * Returns the largest of the given {@code double} values, treating {@code NaN} as the largest.
	 * @param values values to compare
	 * @return largest value
	 */
	public static double max(double... values)
	{
		double maxValue = values[0];
		for (double value : values)
		{
			if (Double.isNaN(value) || (value > maxValue))
			{
				maxValue = value;
			}
		}
		
		return maxValue;
	}
	
	/**
	 * Raises a base to an integer exponent via exponentiation by squaring, supporting negative and zero exponents.
	 * @param base base value
	 * @param exp exponent
	 * @return {@code base} raised to {@code exp}
	 */
	public static double pow(double base, int exp)
	{
		if (exp == 0)
		{
			return 1.0;
		}
		
		if (exp < 0)
		{
			return 1.0 / pow(base, -exp);
		}
		
		double result = 1.0;
		while (exp != 0)
		{
			if ((exp & 1) != 0)
			{
				result *= base;
			}
			base *= base;
			exp >>= 1;
		}
		return result;
	}
	
	/**
	 * Computes the Euclidean distance for two {@code double} deltas.
	 * @param dx horizontal distance
	 * @param dy vertical distance
	 * @return Euclidean distance
	 */
	public static double hypot(double dx, double dy)
	{
		return Math.sqrt((dx * dx) + (dy * dy));
	}
	
	/**
	 * Computes the Euclidean distance for three {@code double} deltas.
	 * @param dx x-coordinate difference
	 * @param dy y-coordinate difference
	 * @param dz z-coordinate difference
	 * @return Euclidean distance
	 */
	public static double hypot(double dx, double dy, double dz)
	{
		return Math.sqrt((dx * dx) + (dy * dy) + (dz * dz));
	}
	
	/**
	 * Computes the Euclidean distance for two {@code int} deltas.
	 * @param dx horizontal distance
	 * @param dy vertical distance
	 * @return Euclidean distance
	 */
	public static double hypot(int dx, int dy)
	{
		final double dxd = dx;
		final double dyd = dy;
		return Math.sqrt((dxd * dxd) + (dyd * dyd));
	}
	
	/**
	 * Computes the Euclidean distance for three {@code int} deltas.
	 * @param dx x-coordinate difference
	 * @param dy y-coordinate difference
	 * @param dz z-coordinate difference
	 * @return Euclidean distance
	 */
	public static double hypot(int dx, int dy, int dz)
	{
		final double dxd = dx;
		final double dyd = dy;
		final double dzd = dz;
		return Math.sqrt((dxd * dxd) + (dyd * dyd) + (dzd * dzd));
	}
}
