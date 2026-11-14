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
package org.l2jmobius.loginserver.controller;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.loginserver.config.LoginConfig;

/**
 * This class represents a session key used by the client to authenticate against the gameserver.<br>
 * It acts as a lightweight, immutable container for {@code PlayOk} and {@code LoginOk} 32-bit pairs.
 * @author BazookaRpm
 */
public class SessionKey
{
	// Constants.
	private static final String LABEL_PLAY_OK = "PlayOk:";
	private static final String LABEL_LOGIN_OK = "LoginOk:";
	private static final boolean CHECK_LOGIN_PAIR = LoginConfig.SHOW_LICENCE; // Policy snapshot for equality consistency.
	
	// Session key parts.
	private final int _playOkID1;
	private final int _playOkID2;
	private final int _loginOkID1;
	private final int _loginOkID2;
	
	/**
	 * Creates a new {@code SessionKey} instance.<br>
	 * This constructor initializes the internal authentication IDs.
	 * @param loginOk1 The first part of the login authentication pair.
	 * @param loginOk2 The second part of the login authentication pair.
	 * @param playOk1 The first part of the play authentication pair.
	 * @param playOk2 The second part of the play authentication pair.
	 */
	public SessionKey(int loginOk1, int loginOk2, int playOk1, int playOk2)
	{
		_playOkID1 = playOk1;
		_playOkID2 = playOk2;
		_loginOkID1 = loginOk1;
		_loginOkID2 = loginOk2;
	}
	
	/**
	 * Returns a string representation of the session key.<br>
	 * This includes both the {@code PlayOk} and {@code LoginOk} pairs.
	 * @return A formatted string containing the session key details.
	 */
	@Override
	public String toString()
	{
		return StringUtil.concat(LABEL_PLAY_OK + " " + _playOkID1 + " " + _playOkID2 + " " + LABEL_LOGIN_OK + _loginOkID1 + " " + _loginOkID2);
	}
	
	/**
	 * Verifies if the provided integers match the internal login pair.<br>
	 * This method checks both {@code _loginOkID1} and {@code _loginOkID2}.
	 * @param loginOk1 The first integer to compare against the stored value.
	 * @param loginOk2 The second integer to compare against the stored value.
	 * @return {@code true} if both values match, otherwise {@code false}.
	 */
	public boolean checkLoginPair(int loginOk1, int loginOk2)
	{
		return (_loginOkID1 == loginOk1) && (_loginOkID2 == loginOk2);
	}
	
	/**
	 * Compares this {@code SessionKey} instance with another object.<br>
	 * It returns {@code true} if the objects are identical or have matching values.<br>
	 * The comparison logic depends on the current configuration settings.
	 * @param object The object to compare this instance against.
	 * @return {@code true} if the two objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object object)
	{
		if (this == object)
		{
			return true;
		}
		
		if (!(object instanceof SessionKey))
		{
			return false;
		}
		
		final SessionKey key = (SessionKey) object;
		if ((_playOkID1 != key._playOkID1) || (_playOkID2 != key._playOkID2))
		{
			return false;
		}
		
		if (CHECK_LOGIN_PAIR)
		{
			return (_loginOkID1 == key._loginOkID1) && (_loginOkID2 == key._loginOkID2);
		}
		
		return true;
	}
	
	/**
	 * Returns a hash code value for this {@code SessionKey}.<br>
	 * This value is used to compare objects in collections.<br>
	 * It is calculated based on the internal ID fields.
	 * @return The integer hash code of this object.
	 */
	@Override
	public int hashCode()
	{
		int h = 17;
		h = (31 * h) + _playOkID1;
		h = (31 * h) + _playOkID2;
		
		if (CHECK_LOGIN_PAIR)
		{
			h = (31 * h) + _loginOkID1;
			h = (31 * h) + _loginOkID2;
		}
		
		return h;
	}
	
	/**
	 * Retrieves the first part of the {@code PlayOk} identifier.<br>
	 * This value is used for session authentication.
	 * @return The integer value of {@code _playOkID1}.
	 */
	public int getPlayOkID1()
	{
		return _playOkID1;
	}
	
	/**
	 * Retrieves the second part of the {@code PlayOk} identifier.<br>
	 * This value is used for session authentication.
	 * @return The integer value of {@code _playOkID2}.
	 */
	public int getPlayOkID2()
	{
		return _playOkID2;
	}
	
	/**
	 * Retrieves the first part of the login authentication ID.<br>
	 * This value is used to verify the session key.
	 * @return The {@code int} value of the first login OK identifier.
	 */
	public int getLoginOkID1()
	{
		return _loginOkID1;
	}
	
	/**
	 * Retrieves the second part of the login authentication ID.<br>
	 * This value is used to verify the session key.
	 * @return The {@code int} value of {@code _loginOkID2}.
	 */
	public int getLoginOkID2()
	{
		return _loginOkID2;
	}
}
