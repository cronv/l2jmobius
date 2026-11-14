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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;

/**
 * This class handles the parsing and storage of secondary authentication data from XML files.<br>
 * It allows the server to load configuration settings for multi-factor or additional security layers.<br>
 * The policy is published only after the whole file parses, so a malformed config never empties the forbidden password list.
 * @author NosBit, BazookaRpm
 */
public class SecondaryAuthData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(SecondaryAuthData.class.getName());
	
	private volatile Set<String> _forbiddenPasswords = Collections.emptySet();
	private volatile boolean _enabled = false;
	private volatile int _maxAttempts = 5;
	private volatile int _banTime = 480;
	private volatile String _recoveryLink = "";
	
	/**
	 * Initializes a new instance of the {@code SecondaryAuthData} class.<br>
	 * This constructor automatically calls the {@code load} method to load data.
	 */
	protected SecondaryAuthData()
	{
		load();
	}
	
	/**
	 * Loads the secondary authentication data from a configuration file.<br>
	 * The active policy is replaced only when the file parses successfully, so a failed reload keeps the forbidden passwords already in force.<br>
	 * It reads the data from {@code config/SecondaryAuth.xml}.
	 */
	@Override
	public synchronized void load()
	{
		final Set<String> previous = _forbiddenPasswords;
		parseFile(new File("config/SecondaryAuth.xml"));
		if (_forbiddenPasswords == previous)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Could not load secondary auth data. Keeping the active policy.");
			return;
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _forbiddenPasswords.size() + " forbidden passwords.");
	}
	
	/**
	 * Parses the configuration data from an XML file.<br>
	 * The values are collected first and published together, so a failure part way through leaves the running policy untouched.
	 * @param document The {@code Document} containing the XML content.
	 * @param file The {@code File} source of the configuration data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		if (document == null)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Cannot parse a null XML document.");
			return;
		}
		
		// Missing nodes keep the running value, matching the previous behaviour.
		final Set<String> forbiddenPasswords = new HashSet<>();
		boolean enabled = _enabled;
		int maxAttempts = _maxAttempts;
		int banTime = _banTime;
		String recoveryLink = _recoveryLink;
		try
		{
			for (Node node = document.getFirstChild(); node != null; node = node.getNextSibling())
			{
				if ("list".equalsIgnoreCase(node.getNodeName()))
				{
					for (Node list_node = node.getFirstChild(); list_node != null; list_node = list_node.getNextSibling())
					{
						final String listName = list_node.getNodeName();
						if ("enabled".equalsIgnoreCase(listName))
						{
							enabled = Boolean.parseBoolean(list_node.getTextContent());
						}
						else if ("maxAttempts".equalsIgnoreCase(listName))
						{
							maxAttempts = Integer.parseInt(list_node.getTextContent());
						}
						else if ("banTime".equalsIgnoreCase(listName))
						{
							banTime = Integer.parseInt(list_node.getTextContent());
						}
						else if ("recoveryLink".equalsIgnoreCase(listName))
						{
							recoveryLink = list_node.getTextContent();
						}
						else if ("forbiddenPasswords".equalsIgnoreCase(listName))
						{
							for (Node forbiddenPasswords_node = list_node.getFirstChild(); forbiddenPasswords_node != null; forbiddenPasswords_node = forbiddenPasswords_node.getNextSibling())
							{
								if ("password".equalsIgnoreCase(forbiddenPasswords_node.getNodeName()))
								{
									forbiddenPasswords.add(forbiddenPasswords_node.getTextContent());
								}
							}
						}
					}
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Failed to load secondary auth data from xml.", e);
			return;
		}
		
		_enabled = enabled;
		_maxAttempts = maxAttempts;
		_banTime = banTime;
		_recoveryLink = recoveryLink;
		_forbiddenPasswords = Collections.unmodifiableSet(forbiddenPasswords);
	}
	
	/**
	 * Checks if the secondary authentication feature is currently active.<br>
	 * This method returns the current state of the {@code _enabled} flag.
	 * @return {@code true} if enabled, {@code false} otherwise.
	 */
	public boolean isEnabled()
	{
		return _enabled;
	}
	
	/**
	 * Returns the maximum number of allowed login attempts.<br>
	 * This value determines how many times a user can try to authenticate before being restricted.
	 * @return The maximum number of attempts as an {@code int}.
	 */
	public int getMaxAttempts()
	{
		return _maxAttempts;
	}
	
	/**
	 * Returns the duration of the ban time.<br>
	 * This value represents the number of seconds a user is restricted.
	 * @return The current {@code _banTime} value.
	 */
	public int getBanTime()
	{
		return _banTime;
	}
	
	/**
	 * Retrieves the recovery link for secondary authentication.<br>
	 * This value is loaded from the configuration file.
	 * @return The {@code String} containing the recovery link.
	 */
	public String getRecoveryLink()
	{
		return _recoveryLink;
	}
	
	/**
	 * Retrieves the list of passwords that are not allowed.<br>
	 * These passwords are blocked by the system for security reasons.
	 * @return An unmodifiable {@code Set<String>} containing all forbidden passwords.
	 */
	public Set<String> getForbiddenPasswords()
	{
		return _forbiddenPasswords;
	}
	
	/**
	 * Checks if the provided password is in the forbidden list.<br>
	 * This method returns {@code true} if the password is blocked.<br>
	 * It returns {@code false} otherwise.
	 * @param password The password string to check.
	 * @return {@code true} if the password is forbidden, {@code false} otherwise.
	 */
	public boolean isForbiddenPassword(String password)
	{
		return _forbiddenPasswords.contains(password);
	}
	
	/**
	 * Retrieves the global instance of {@code SecondaryAuthData}.<br>
	 * This method follows the singleton pattern.
	 * @return The single shared instance of {@code SecondaryAuthData}.
	 */
	public static SecondaryAuthData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SecondaryAuthData INSTANCE = new SecondaryAuthData();
	}
}
