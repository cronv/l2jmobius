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
package org.l2jmobius.gameserver.mechanics.captcha;

/**
 * This class handles the generation and validation of visual CAPTCHA images.<br>
 * It is used to verify that a {@code Player} is human rather than an automated bot.
 * @author JoeAlisson
 */
public class Captcha
{
	private final int _code;
	private final byte[] _data;
	private final int _id;
	
	/**
	 * Creates a new instance of the {@code Captcha} class.<br>
	 * This constructor initializes the captcha with an ID, a secret code, and raw data.
	 * @param id The unique identifier for this captcha.
	 * @param code The secret verification code.
	 * @param data The byte array containing the captcha image or content.
	 */
	public Captcha(int id, int code, byte[] data)
	{
		_id = id;
		_code = code;
		_data = data;
	}
	
	/**
	 * Retrieves the unique identification code for this {@code Captcha}.<br>
	 * This value is used to verify user input.
	 * @return The integer value of the captcha code.
	 */
	public int getCode()
	{
		return _code;
	}
	
	/**
	 * Retrieves the raw data associated with this {@code Captcha}.<br>
	 * This method returns the internal byte array.
	 * @return a {@code byte[]} containing the captcha data.
	 */
	public byte[] getData()
	{
		return _data;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Captcha}.<br>
	 * This value is assigned when the object is created.
	 * @return The integer ID of the captcha.
	 */
	public int getId()
	{
		return _id;
	}
}
