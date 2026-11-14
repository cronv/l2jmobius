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
package org.l2jmobius.gameserver.handler;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * This class handles the logic for the community board system.<br>
 * It processes interactions and messages related to public boards in the game world.
 * @author Zoey76, Mobius
 */
public class CommunityBoardHandler implements IHandler<IParseBoardHandler, String>
{
	private static final Logger LOG = Logger.getLogger(CommunityBoardHandler.class.getName());
	/** The registered handlers. */
	private final Map<String, IParseBoardHandler> _datatable = new HashMap<>();
	/** The bypasses used by the players. */
	private final Map<Integer, String> _bypasses = new ConcurrentHashMap<>();
	
	/**
	 * This is a protected constructor for the {@code CommunityBoardHandler} class.<br>
	 * It prevents other classes from creating new instances of this handler.<br>
	 * Use the {@code getInstance()} method to access the singleton instance.
	 */
	protected CommunityBoardHandler()
	{
		// Prevent external initialization.
	}
	
	/**
	 * Registers a new command handler to the community board.<br>
	 * This method adds all commands from the {@code IParseBoardHandler} to the internal map.<br>
	 * The commands are stored using their lowercase versions as keys.
	 * @param handler The {@code IParseBoardHandler} instance to register.
	 */
	@Override
	public void registerHandler(IParseBoardHandler handler)
	{
		for (String cmd : handler.getCommandList())
		{
			_datatable.put(cmd.toLowerCase(), handler);
		}
	}
	
	/**
	 * Removes a registered handler from the system.<br>
	 * This method deletes all commands associated with the provided {@code IParseBoardHandler}.<br>
	 * The operation is thread-safe because it is synchronized.
	 * @param handler The {@code IParseBoardHandler} to be removed.
	 */
	@Override
	public synchronized void removeHandler(IParseBoardHandler handler)
	{
		for (String cmd : handler.getCommandList())
		{
			_datatable.remove(cmd.toLowerCase());
		}
	}
	
	/**
	 * Retrieves the appropriate handler for a given command.<br>
	 * It searches through all registered {@code IParseBoardHandler} instances.<br>
	 * The search is case-insensitive and checks if the input starts with any known command.
	 * @param cmd The command string to look up.
	 * @return The matching {@code IParseBoardHandler} or {@code null} if no match is found.
	 */
	@Override
	public IParseBoardHandler getHandler(String cmd)
	{
		// The longest matching command wins, so _bbstopics is not taken by _bbstop.
		final String command = cmd.toLowerCase();
		IParseBoardHandler handler = null;
		int length = 0;
		for (Entry<String, IParseBoardHandler> entry : _datatable.entrySet())
		{
			final String key = entry.getKey();
			if ((key.length() > length) && command.startsWith(key))
			{
				handler = entry.getValue();
				length = key.length();
			}
		}
		
		return handler;
	}
	
	/**
	 * Returns the total number of registered handlers.<br>
	 * This count corresponds to the size of the internal {@code _datatable}.
	 * @return The number of registered {@code IParseBoardHandler} instances.
	 */
	@Override
	public int size()
	{
		return _datatable.size();
	}
	
	/**
	 * Checks if a given string is a valid community board command.<br>
	 * It returns {@code true} if the command exists in the registered handlers.<br>
	 * It returns {@code false} if the command is not recognized.
	 * @param cmd The command string to check.
	 * @return {@code true} if it is a valid command, otherwise {@code false}.
	 */
	public boolean isCommunityBoardCommand(String cmd)
	{
		return getHandler(cmd) != null;
	}
	
	/**
	 * Processes a community board command sent by a {@code Player}.<br>
	 * This method checks if the server is online and finds the correct handler.<br>
	 * If no handler exists for the {@code command}, it logs a warning.
	 * @param command The string input received from the player.
	 * @param player The {@code Player} object who sent the command.
	 */
	public void handleParseCommand(String command, Player player)
	{
		if (player == null)
		{
			return;
		}
		
		if (!GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return;
		}
		
		final IParseBoardHandler cb = getHandler(command);
		if (cb == null)
		{
			LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": Couldn't find parse handler for command " + command + "!");
			return;
		}
		
		cb.onCommand(command, player);
	}
	
	/**
	 * Processes a command to write content to the community board.<br>
	 * This method identifies the correct handler based on the provided {@code url}.<br>
	 * It then executes the writing logic using the supplied arguments.
	 * @param player The {@code Player} who is sending the command.
	 * @param url The identifier used to determine which board action to perform.
	 * @param arg1 The first argument for the write command.
	 * @param arg2 The second argument for the write command.
	 * @param arg3 The third argument for the write command.
	 * @param arg4 The fourth argument for the write command.
	 * @param arg5 The fifth argument for the write command.
	 */
	public void handleWriteCommand(Player player, String url, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		if (player == null)
		{
			return;
		}
		
		if (!GeneralConfig.ENABLE_COMMUNITY_BOARD)
		{
			player.sendPacket(SystemMessageId.THE_COMMUNITY_SERVER_IS_CURRENTLY_OFFLINE);
			return;
		}
		
		String cmd = "";
		switch (url)
		{
			case "Topic":
			{
				cmd = "_bbstopics";
				break;
			}
			case "Post":
			{
				cmd = "_bbsposts";
				break;
			}
			case "Region":
			{
				cmd = "_bbsloc";
				break;
			}
			case "Notice":
			{
				cmd = "_bbsclan";
				break;
			}
			default:
			{
				separateAndSend("<html><body><br><br><center>The command: " + url + " is not implemented yet.</center><br><br></body></html>", player);
				return;
			}
		}
		
		final IParseBoardHandler cb = getHandler(cmd);
		if (cb == null)
		{
			LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": Couldn't find write handler for command " + cmd + "!");
			return;
		}
		
		if (!(cb instanceof IWriteBoardHandler))
		{
			LOG.warning(CommunityBoardHandler.class.getSimpleName() + ": " + cb.getClass().getSimpleName() + " doesn't implement write!");
			return;
		}
		
		((IWriteBoardHandler) cb).writeCommunityBoardCommand(player, arg1, arg2, arg3, arg4, arg5);
	}
	
	/**
	 * Adds a new bypass for a specific player.<br>
	 * This method stores the combined {@code title} and {@code bypass} strings.<br>
	 * It uses the unique object ID of the {@code Player} as the key.
	 * @param player The {@code Player} who will receive the bypass.
	 * @param title The title associated with the bypass.
	 * @param bypass The specific bypass string to be applied.
	 */
	public void addBypass(Player player, String title, String bypass)
	{
		_bypasses.put(player.getObjectId(), title + "&" + bypass);
	}
	
	/**
	 * Removes a bypass associated with a specific {@code Player}.<br>
	 * This method deletes the entry from the internal bypass map.
	 * @param player The {@code Player} whose bypass should be removed.
	 * @return The {@code String} value of the removed bypass, or {@code null} if none existed.
	 */
	public String removeBypass(Player player)
	{
		return _bypasses.remove(player.getObjectId());
	}
	
	/**
	 * This method processes an HTML string for a specific player.<br>
	 * It separates the content and sends it to the {@code Player}.<br>
	 * The message is sent using the {@code HtmlUtil} utility class.
	 * @param html The HTML content to be processed and sent.
	 * @param player The {@code Player} who will receive the message.
	 */
	public static void separateAndSend(String html, Player player)
	{
		HtmlUtil.sendCBHtml(player, html);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code CommunityBoardHandler}.<br>
	 * This method provides a global access point to the handler.<br>
	 * Use this instead of creating a new object.
	 * @return The single instance of {@code CommunityBoardHandler}.
	 */
	public static CommunityBoardHandler getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CommunityBoardHandler INSTANCE = new CommunityBoardHandler();
	}
}
