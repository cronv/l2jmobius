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
package org.l2jmobius.commons.threads;

/**
 * Defines different levels of thread priorities.<br>
 * This {@code ThreadPriority} enum provides a convenient way to set thread priorities in a readable and maintainable manner.<br>
 * The priorities range from {@code 1} (lowest) to {@code 10} (highest), aligning with Java's standard thread priority levels.
 * @author Mobius
 * @since December 8th, 2023
 */
public enum ThreadPriority
{
	/**
	 * Priority level 1, equivalent to {@link Thread#MIN_PRIORITY}.
	 */
	PRIORITY_1(1),
	
	/**
	 * Priority level 2.
	 */
	PRIORITY_2(2),
	
	/**
	 * Priority level 3.
	 */
	PRIORITY_3(3),
	
	/**
	 * Priority level 4.
	 */
	PRIORITY_4(4),
	
	/**
	 * Priority level 5, equivalent to {@link Thread#NORM_PRIORITY}.
	 */
	PRIORITY_5(5),
	
	/**
	 * Priority level 6.
	 */
	PRIORITY_6(6),
	
	/**
	 * Priority level 7.
	 */
	PRIORITY_7(7),
	
	/**
	 * Priority level 8.
	 */
	PRIORITY_8(8),
	
	/**
	 * Priority level 9.
	 */
	PRIORITY_9(9),
	
	/**
	 * Priority level 10, equivalent to {@link Thread#MAX_PRIORITY}.
	 */
	PRIORITY_10(10);
	
	private final int _id;
	
	/**
	 * Creates a new {@code ThreadPriority} with a specific ID.<br>
	 * The {@code id} represents the numerical priority level.
	 * @param id The priority level from 1 to 10.
	 */
	ThreadPriority(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the unique priority level associated with this {@code ThreadPriority}.
	 * @return the integer value of the priority.
	 */
	public int getId()
	{
		return _id;
	}
}
