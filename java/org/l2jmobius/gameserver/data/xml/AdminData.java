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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.AccessLevel;
import org.l2jmobius.gameserver.data.holders.AccessRight;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading of administrator access levels and commands from XML files.<br>
 * It provides the data necessary to manage permissions for administrative users.
 * @author UnAfraid, Mobius
 */
public class AdminData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(AdminData.class.getName());
	
	private final Map<Integer, AccessLevel> _accessLevels = new HashMap<>();
	private final Map<String, AccessRight> _adminCommandAccessRights = new LinkedHashMap<>();
	private final Map<Player, Boolean> _gmList = new ConcurrentHashMap<>();
	private int _highestLevel = 0;
	
	/**
	 * Initializes a new instance of the {@code AdminData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected AdminData()
	{
		load();
	}
	
	/**
	 * Loads the administrator data from configuration files.<br>
	 * This method clears existing data and parses both {@code AccessLevels.xml} and {@code AdminCommands.xml}.<br>
	 * It logs the number of loaded levels and commands to the logger.
	 */
	@Override
	public synchronized void load()
	{
		_accessLevels.clear();
		_adminCommandAccessRights.clear();
		
		parseDatapackFile("config/AccessLevels.xml");
		final String className = getClass().getSimpleName();
		LOGGER.info(className + ": Loaded " + _accessLevels.size() + " access levels.");
		
		parseDatapackFile("config/AdminCommands.xml");
		LOGGER.info(className + ": Loaded " + _adminCommandAccessRights.size() + " access commands.");
	}
	
	/**
	 * Parses the provided {@code Document} to load administrator data.<br>
	 * This method reads access levels and admin commands from the XML file.<br>
	 * It updates the internal maps used by {@code AdminData}.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} object representing the source of the data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, n ->
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				forEach(n, d ->
				{
					switch (d.getNodeName())
					{
						case "access":
						{
							final StatSet set = new StatSet(parseAttributes(d));
							final AccessLevel level = new AccessLevel(set);
							final Integer lvlValue = level.getLevel();
							if (lvlValue > _highestLevel)
							{
								_highestLevel = lvlValue;
							}
							
							_accessLevels.put(lvlValue, level);
							break;
						}
						case "admin":
						{
							final StatSet set = new StatSet(parseAttributes(d));
							final AccessRight command = new AccessRight(set);
							_adminCommandAccessRights.put(command.getCommand(), command);
							break;
						}
					}
				});
			}
		});
	}
	
	/**
	 * Retrieves the {@code AccessLevel} associated with a specific level.<br>
	 * If the provided value is less than {@code 0}, it returns the default level.<br>
	 * It creates a new {@code AccessLevel} if one does not exist for the given key.
	 * @param accessLevel The integer ID of the access level to retrieve.
	 * @return The corresponding {@code AccessLevel} object.
	 */
	public AccessLevel getAccessLevel(int accessLevel)
	{
		if (accessLevel < 0)
		{
			return _accessLevels.get(-1);
		}
		
		return _accessLevels.computeIfAbsent(accessLevel, k -> new AccessLevel());
	}
	
	/**
	 * Retrieves the highest administrator access level.<br>
	 * This method returns the {@code AccessLevel} associated with the maximum rank.
	 * @return the {@code AccessLevel} for the master rank.
	 */
	public AccessLevel getMasterAccessLevel()
	{
		return _accessLevels.get(_highestLevel);
	}
	
	/**
	 * Checks if a specific access level exists in the system.<br>
	 * This method verifies if the provided {@code accessLevel} is registered.
	 * @param accessLevel The integer ID of the access level to check.
	 * @return {@code true} if the level exists, {@code false} otherwise.
	 */
	public boolean hasAccessLevel(int accessLevel)
	{
		return _accessLevels.containsKey(accessLevel);
	}
	
	/**
	 * Checks if a specific {@code AccessLevel} has permission to execute an admin command.<br>
	 * It retrieves the rights for the given {@code adminCommand}.<br>
	 * If no rights are defined, it may automatically assign them based on the highest level.
	 * @param adminCommand The name of the command to check.
	 * @param accessLevel The level being checked for permission.
	 * @return {@code true} if the level has access, {@code false} otherwise.
	 */
	public boolean hasAccess(String adminCommand, AccessLevel accessLevel)
	{
		AccessRight accessRight = _adminCommandAccessRights.get(adminCommand);
		if (accessRight == null)
		{
			// Trying to avoid the spam for next time when the GM would try to use the same command.
			final int accessLvlValue = accessLevel.getLevel();
			if ((accessLvlValue > 0) && (accessLvlValue == _highestLevel))
			{
				accessRight = new AccessRight(adminCommand, "", true, accessLvlValue);
				_adminCommandAccessRights.put(accessRight.getCommand(), accessRight);
				LOGGER.info(getClass().getSimpleName() + ": No rights defined for admin command " + adminCommand + " auto setting accesslevel: " + accessLvlValue + " !");
			}
			else
			{
				LOGGER.info(getClass().getSimpleName() + ": No rights defined for admin command " + adminCommand + " !");
				return false;
			}
		}
		
		return accessRight.hasAccess(accessLevel);
	}
	
	/**
	 * Checks if a specific command requires user confirmation.<br>
	 * It looks up the {@code AccessRight} for the given {@code command}.<br>
	 * If no rights are defined, it returns {@code false}.<br>
	 * Otherwise, it returns the confirmation status from the access right.
	 * @param command The name of the admin command to check.
	 * @return {@code true} if confirmation is required, {@code false} otherwise.
	 */
	public boolean requireConfirm(String command)
	{
		final AccessRight accessRight = _adminCommandAccessRights.get(command);
		if (accessRight == null)
		{
			LOGGER.info(getClass().getSimpleName() + ": No rights defined for admin command " + command + ".");
			return false;
		}
		
		return accessRight.requireConfirm();
	}
	
	/**
	 * Retrieves a list of all Game Masters.<br>
	 * It filters the results based on their hidden status.
	 * @param includeHidden Set to {@code true} to include hidden GMs, or {@code false} to exclude them.
	 * @return A {@code Collection} of {@code Player} objects who are Game Masters.
	 */
	public Collection<Player> getAllGms(boolean includeHidden)
	{
		final List<Player> result = new ArrayList<>();
		for (Entry<Player, Boolean> entry : _gmList.entrySet())
		{
			if (includeHidden || !entry.getValue())
			{
				result.add(entry.getKey());
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves a list of names for all Game Masters.<br>
	 * It filters the names based on their visibility status.
	 * @param includeHidden Set to {@code true} to include hidden GMs with an invisibility tag.
	 * @return A {@code Collection<String>} containing the GM names.
	 */
	public Collection<String> getAllGmNames(boolean includeHidden)
	{
		final List<String> result = new ArrayList<>();
		for (Entry<Player, Boolean> entry : _gmList.entrySet())
		{
			if (!entry.getValue())
			{
				result.add(entry.getKey().getName());
			}
			else if (includeHidden)
			{
				result.add(entry.getKey().getName() + " (invis)");
			}
		}
		
		return result;
	}
	
	/**
	 * Adds a {@code Player} to the list of game masters.<br>
	 * This method stores whether the player is marked as hidden.
	 * @param player The {@code Player} object to add.
	 * @param hidden Set to {@code true} if the player should be hidden, or {@code false} otherwise.
	 */
	public void addGm(Player player, boolean hidden)
	{
		_gmList.put(player, hidden);
	}
	
	/**
	 * Removes a {@code Player} from the list of game masters.<br>
	 * This method updates the internal {@code _gmList} collection.
	 * @param player The {@code Player} object to remove.
	 */
	public void deleteGm(Player player)
	{
		_gmList.remove(player);
	}
	
	/**
	 * Checks if there are any Game Masters currently online.<br>
	 * It iterates through the internal list of GMs to find a match.
	 * @param includeHidden Determines whether to include hidden GMs in the check.
	 * @return {@code true} if at least one GM is online, otherwise {@code false}.
	 */
	public boolean isGmOnline(boolean includeHidden)
	{
		for (Entry<Player, Boolean> entry : _gmList.entrySet())
		{
			if (includeHidden || !entry.getValue())
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Sends a list of online administrators to a specific player.<br>
	 * This method checks if any administrators are visible based on the player's status.<br>
	 * If no administrators are visible, it sends a notification message instead.
	 * @param player The {@code Player} who will receive the administrator list.
	 */
	public void sendListToPlayer(Player player)
	{
		final boolean isGm = player.isGM();
		if (isGmOnline(isGm))
		{
			player.sendPacket(SystemMessageId.GM_LIST);
			for (String name : getAllGmNames(isGm))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.GM_C1);
				sm.addString(name);
				player.sendPacket(sm);
			}
		}
		else
		{
			player.sendPacket(SystemMessageId.THERE_ARE_NO_GMS_CURRENTLY_VISIBLE_IN_THE_PUBLIC_LIST_AS_THEY_MAY_BE_PERFORMING_OTHER_FUNCTIONS_AT_THE_MOMENT);
		}
	}
	
	/**
	 * Sends a packet to all online Game Masters.<br>
	 * This includes hidden Game Masters in the broadcast.
	 * @param packet The {@code ServerPacket} to be sent to each GM.
	 */
	public void broadcastToGMs(ServerPacket packet)
	{
		for (Player player : getAllGms(true))
		{
			player.sendPacket(packet);
		}
	}
	
	/**
	 * Sends a text message to all online Game Masters.<br>
	 * This method includes hidden Game Masters in the broadcast.
	 * @param message The content of the message to send.
	 */
	public void broadcastMessageToGMs(String message)
	{
		for (Player player : getAllGms(true))
		{
			player.sendMessage(message);
		}
	}
	
	/**
	 * Retrieves all administrator command access rights.<br>
	 * This method returns the values stored in the internal map.
	 * @return a {@code Collection} of {@code AccessRight} objects.
	 */
	public Collection<AccessRight> getAdminCommandAccessRights()
	{
		return _adminCommandAccessRights.values();
	}
	
	/**
	 * Retrieves the singleton instance of {@code AdminData}.<br>
	 * This method provides a global access point to the administrator data.
	 * @return The single instance of {@code AdminData}.
	 */
	public static AdminData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AdminData INSTANCE = new AdminData();
	}
}
