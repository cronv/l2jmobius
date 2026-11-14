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

import java.util.Objects;

/**
 * Represents an immutable snapshot of account information used by the login server.<br>
 * This class encapsulates the login name, password hash, access level, and last selected server.<br>
 * It ensures that the login name is normalized to lower case and validates that credentials are not {@code null} or empty.
 * @author BazookaRpm
 */
public class AccountInfo
{
	// Account login name (normalized to lower case).
	private final String _login;
	
	// Encoded password hash associated with this account.
	private final String _passwordHash;
	
	// Access level granted to this account.
	private final int _accessLevel;
	
	// Identifier of the last game server used by this account.
	private final int _lastServer;
	
	/**
	 * Creates a new instance of {@code AccountInfo}.<br>
	 * This constructor initializes the account details.<br>
	 * It ensures that the login name is converted to lower case.
	 * @param login The unique login name for the account.
	 * @param passwordHash The encoded hash of the user's password.
	 * @param accessLevel The permission level assigned to the account.
	 * @param lastServer The ID of the server the user visited last.
	 */
	public AccountInfo(String login, String passwordHash, int accessLevel, int lastServer)
	{
		Objects.requireNonNull(login, "login");
		Objects.requireNonNull(passwordHash, "passwordHash");
		
		_login = requireNonEmpty(login, "login").toLowerCase();
		_passwordHash = requireNonEmpty(passwordHash, "passwordHash");
		_accessLevel = accessLevel;
		_lastServer = lastServer;
	}
	
	/**
	 * Validates that a string is not empty.<br>
	 * Throws an {@code IllegalArgumentException} if the input is empty.
	 * @param value The string to check.
	 * @param argumentName The name of the argument to display in the exception message.
	 * @return The original string if it is not empty.
	 */
	private static String requireNonEmpty(String value, String argumentName)
	{
		if (value.isEmpty())
		{
			throw new IllegalArgumentException(argumentName);
		}
		return value;
	}
	
	/**
	 * Verifies if the provided password hash matches the stored one.<br>
	 * This method compares two {@code String} values for equality.
	 * @param passwordHash The hash to check against the stored value.
	 * @return {@code true} if the hashes match, {@code false} otherwise.
	 */
	public boolean checkPassHash(String passwordHash)
	{
		return _passwordHash.equals(passwordHash);
	}
	
	/**
	 * Retrieves the login name of the account.<br>
	 * The returned string is normalized to lower case.
	 * @return the {@code String} representing the account login.
	 */
	public String getLogin()
	{
		return _login;
	}
	
	/**
	 * Retrieves the access level for this account.<br>
	 * This value determines the permissions granted to the user.
	 * @return the current {@code int} access level
	 */
	public int getAccessLevel()
	{
		return _accessLevel;
	}
	
	/**
	 * Retrieves the identifier of the last game server used by this account.<br>
	 * This value is stored as an {@code int}.
	 * @return the ID of the last selected server.
	 */
	public int getLastServer()
	{
		return _lastServer;
	}
	
	/**
	 * Returns a string representation of the {@code AccountInfo} object.<br>
	 * This includes the login name, access level, and last server ID.
	 * @return A string containing the account details.
	 */
	@Override
	public String toString()
	{
		return "AccountInfo[login=" + _login + ", accessLevel=" + _accessLevel + ", lastServer=" + _lastServer + "]";
	}
}
