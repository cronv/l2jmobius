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
package org.l2jmobius.gameserver.data.holders;

/**
 * This class acts as a data holder for two {@code String} values.<br>
 * It is used to store and manage pairs of strings within the game server.
 * @author Mobius
 */
public class StringStringHolder
{
	private final String _string1;
	private final String _string2;
	
	/**
	 * Creates a new instance of {@code StringStringHolder}.<br>
	 * This constructor initializes the internal strings.
	 * @param string1 The first string to store.
	 * @param string2 The second string to store.
	 */
	public StringStringHolder(String string1, String string2)
	{
		_string1 = string1;
		_string2 = string2;
	}
	
	/**
	 * Retrieves the first string value.<br>
	 * This method returns the value stored in {@code _string1}.
	 * @return The first string value.
	 */
	public String getString1()
	{
		return _string1;
	}
	
	/**
	 * Retrieves the second string stored in this holder.<br>
	 * This method returns the value of {@code _string2}.
	 * @return The {@code String} value of the second field.
	 */
	public String getString2()
	{
		return _string2;
	}
}
