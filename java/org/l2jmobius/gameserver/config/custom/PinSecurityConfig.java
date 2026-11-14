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
package org.l2jmobius.gameserver.config.custom;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * Loads the custom PIN security configuration.<br>
 * This class controls in-game PIN validation and lock policies for accounts or characters.<br>
 * It defines settings such as {@code PIN} length and brute force protection.
 * @author BazookaRpm
 */
public class PinSecurityConfig
{
	// File.
	private static final String PIN_SECURITY_ACCOUNT_CONFIG_FILE = "./config/Custom/PinSecurity.ini";
	
	// Scope.
	public static final String SCOPE_ACCOUNT = "Account";
	public static final String SCOPE_CHARACTER = "Character";
	
	// Internal Security Settings.
	public static final int HASH_ITERATIONS = 50000;
	
	// Configurable Settings.
	public static boolean ENABLED;
	public static String SCOPE;
	public static int MIN_LENGTH;
	public static int MAX_LENGTH;
	public static int MAX_ATTEMPTS;
	public static int LOCK_MINUTES;
	
	/**
	 * Loads the PIN security configuration from the file.<br>
	 * This method populates the static fields of {@code PinSecurityConfig}.<br>
	 * It ensures that values are normalized to valid ranges.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(PIN_SECURITY_ACCOUNT_CONFIG_FILE);
		ENABLED = config.getBoolean("PinSecurityEnabled", false);
		SCOPE = config.getString("PinSecurityScope", SCOPE_ACCOUNT);
		if (!SCOPE_CHARACTER.equalsIgnoreCase(SCOPE))
		{
			SCOPE = SCOPE_ACCOUNT;
		}
		else
		{
			SCOPE = SCOPE_CHARACTER;
		}
		MIN_LENGTH = Math.max(1, config.getInt("PinSecurityMinLength", 4));
		MAX_LENGTH = Math.max(MIN_LENGTH, config.getInt("PinSecurityMaxLength", 8));
		MAX_ATTEMPTS = Math.max(1, config.getInt("PinSecurityMaxAttempts", 5));
		LOCK_MINUTES = Math.max(1, config.getInt("PinSecurityLockMinutes", 15));
	}
}
