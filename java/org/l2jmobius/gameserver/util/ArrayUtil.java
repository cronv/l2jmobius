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
 * Helper methods for searching arrays.
 * @author Mobius
 */
public class ArrayUtil
{
	/**
	 * Returns whether the array contains the given value.
	 * @param array the array to search
	 * @param value the value to find
	 * @return {@code true} if found, {@code false} otherwise
	 */
	public static boolean contains(int[] array, int value)
	{
		for (int i = 0; i < array.length; i++)
		{
			if (array[i] == value)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Returns whether the array contains the given object, handling {@code null} and equality.
	 * @param array the array to search
	 * @param value the object to find
	 * @return {@code true} if found, {@code false} otherwise
	 */
	public static boolean contains(Object[] array, Object value)
	{
		if (value == null)
		{
			for (int i = 0; i < array.length; i++)
			{
				if (value == array[i])
				{
					return true;
				}
			}
		}
		else
		{
			for (int i = 0; i < array.length; i++)
			{
				if (value.equals(array[i]))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Returns whether the array contains the given string, optionally ignoring case.
	 * @param array the array to search
	 * @param value the string to find
	 * @param ignoreCase {@code true} to ignore case, {@code false} for an exact match
	 * @return {@code true} if found, {@code false} otherwise
	 */
	public static boolean contains(String[] array, String value, boolean ignoreCase)
	{
		if (ignoreCase)
		{
			for (int i = 0; i < array.length; i++)
			{
				if (value.equalsIgnoreCase(array[i]))
				{
					return true;
				}
			}
		}
		else
		{
			for (int i = 0; i < array.length; i++)
			{
				if (value.equals(array[i]))
				{
					return true;
				}
			}
		}
		
		return false;
	}
}
