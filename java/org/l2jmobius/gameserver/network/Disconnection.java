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
package org.l2jmobius.gameserver.network;

import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.taskmanagers.AttackStanceTaskManager;

/**
 * This class handles the logic required to disconnect a player or client from the server.<br>
 * It ensures that all necessary cleanup tasks are performed during the disconnection process.
 * @author NB4L1, Mobius
 */
public class Disconnection
{
	private static final Logger LOGGER = Logger.getLogger(Disconnection.class.getName());
	private static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	private final GameClient _client;
	private final Player _player;
	
	/**
	 * Creates a new {@code Disconnection} instance.<br>
	 * This constructor links the {@code GameClient} and {@code Player}.<br>
	 * It stops all active tasks for the player.<br>
	 * It also clears the references between the client and the player.
	 * @param client The {@code GameClient} associated with this disconnection.
	 * @param player The {@code Player} associated with this disconnection.
	 */
	private Disconnection(GameClient client, Player player)
	{
		// Resolve client and player.
		_client = client != null ? client : player != null ? player.getClient() : null;
		_player = player != null ? player : client != null ? client.getPlayer() : null;
		
		// Stop player tasks.
		if (_player != null)
		{
			_player.stopAllTasks();
		}
		
		// Anti Feed.
		AntiFeedManager.getInstance().onDisconnect(_client);
		
		if (_client != null)
		{
			_client.setPlayer(null);
		}
		
		if (_player != null)
		{
			_player.setClient(null);
		}
	}
	
	/**
	 * Creates a new {@code Disconnection} instance for a specific client.<br>
	 * This method is used to start the disconnection process for a {@code GameClient}.
	 * @param client The {@code GameClient} that is being disconnected.
	 * @return A new {@code Disconnection} object.
	 */
	public static Disconnection of(GameClient client)
	{
		return new Disconnection(client, null);
	}
	
	/**
	 * Creates a new {@code Disconnection} instance for a specific player.<br>
	 * This method is used when the disconnection logic needs to be initiated based on a {@code Player} object.
	 * @param player The {@code Player} who is being disconnected.
	 * @return A new {@code Disconnection} instance associated with the provided {@code Player}.
	 */
	public static Disconnection of(Player player)
	{
		return new Disconnection(null, player);
	}
	
	/**
	 * Creates a new {@code Disconnection} instance.<br>
	 * This method links a specific {@code GameClient} and {@code Player}.<br>
	 * It is used to initialize the disconnection process for both entities.
	 * @param client The {@code GameClient} associated with the connection.
	 * @param player The {@code Player} associated with the connection.
	 * @return A new instance of {@code Disconnection}.
	 */
	public static Disconnection of(GameClient client, Player player)
	{
		return new Disconnection(client, player);
	}
	
	/**
	 * Saves the player data and removes them from the game world.<br>
	 * This method handles the final cleanup for a {@code Player} or {@code GameClient}.<br>
	 * It logs the logout event to the accounting logger.
	 */
	public void storeAndDelete()
	{
		try
		{
			if (_player != null)
			{
				_player.storeMe();
				
				if (_player.isOnline())
				{
					_player.deleteMe();
					
					LOGGER_ACCOUNTING.info("Logged out, " + _player);
				}
			}
			else if (_client != null)
			{
				LOGGER_ACCOUNTING.info("Logged out, " + _client);
			}
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Problem with storeAndDelete: " + e.getMessage());
		}
	}
	
	/**
	 * Performs the standard cleanup for a disconnection.<br>
	 * It also closes the {@code GameClient} using the provided {@code ServerPacket}.
	 * @param packet the {@code ServerPacket} to send during the client closure.
	 */
	public void storeAndDeleteWith(ServerPacket packet)
	{
		storeAndDelete();
		
		if (_client != null)
		{
			_client.close(packet);
		}
	}
	
	/**
	 * Handles the logic for a player disconnecting from the server.<br>
	 * It checks if the {@code Player} is allowed to logout immediately.<br>
	 * If not, it schedules the cleanup task after a delay.
	 */
	public void onDisconnection()
	{
		if (_player == null)
		{
			return;
		}
		
		if (_player.canLogout())
		{
			storeAndDelete();
		}
		else
		{
			ThreadPool.schedule(() ->
			{
				if (_player.isOnline())
				{
					storeAndDelete();
				}
			}, AttackStanceTaskManager.COMBAT_TIME);
		}
	}
}
