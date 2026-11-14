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
 * This class loads all the custom chat moderation related configurations.<br>
 * It provides access to settings used for managing and filtering in-game chat.<br>
 * Use {@code ChatModerationConfig} to retrieve these configuration values.
 * @author Mobius
 */
public class ChatModerationConfig
{
	// File
	private static final String CHAT_MODERATION_CONFIG_FILE = "./config/Custom/ChatModeration.ini";
	
	// Constants
	public static boolean CHAT_ADMIN;
	
	/**
	 * Loads the custom chat moderation settings from the configuration file.<br>
	 * This method reads values into the {@code CHAT_ADMIN} variable.<br>
	 * It uses a default value of {@code true} if no setting is found.
	 */
	public static void load()
	{
		final ConfigReader config = new ConfigReader(CHAT_MODERATION_CONFIG_FILE);
		CHAT_ADMIN = config.getBoolean("ChatAdmin", true);
	}
}
