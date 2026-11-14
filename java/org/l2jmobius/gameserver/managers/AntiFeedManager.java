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
package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PvpConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.network.GameClient;

/**
 * Manages the anti-feeding system to prevent players from intentionally dying.<br>
 * It monitors player behavior and applies penalties based on configured rules.
 */
public class AntiFeedManager
{
	protected static final Logger LOGGER_ACCOUNTING = Logger.getLogger("accounting");
	
	public static final int GAME_ID = 0;
	public static final int OLYMPIAD_ID = 1;
	public static final int TVT_ID = 2;
	public static final int L2EVENT_ID = 3;
	public static final int OFFLINE_PLAY = 4;
	
	private final Map<Integer, Long> _lastDeathTimes = new ConcurrentHashMap<>();
	private final Map<Integer, Map<Integer, AtomicInteger>> _eventIPs = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code AntiFeedManager}.<br>
	 * This constructor is protected to restrict instantiation.<br>
	 * Use {@code getInstance} to get the singleton instance.
	 */
	protected AntiFeedManager()
	{
	}
	
	/**
	 * Updates the timestamp for when a specific object last died.<br>
	 * This method stores the current system time for the given {@code objectId}.
	 * @param objectId The unique identifier of the creature or player.
	 */
	public void setLastDeathTime(int objectId)
	{
		_lastDeathTimes.put(objectId, System.currentTimeMillis());
	}
	
	/**
	 * Validates if an attack is allowed based on anti-feed rules.<br>
	 * This method checks for death intervals and dual-boxing.
	 * @param attacker The {@code Creature} performing the attack.
	 * @param target The {@code Creature} being attacked.
	 * @return {@code true} if the attack is permitted, {@code false} otherwise.
	 */
	public boolean check(Creature attacker, Creature target)
	{
		if (!PvpConfig.ANTIFEED_ENABLE)
		{
			return true;
		}
		
		if (target == null)
		{
			return false;
		}
		
		final Player targetPlayer = target.asPlayer();
		if (targetPlayer == null)
		{
			return false;
		}
		
		// Players in offline mode should't be valid targets.
		if (targetPlayer.getClient().isDetached())
		{
			return false;
		}
		
		final int objectId = targetPlayer.getObjectId();
		if ((PvpConfig.ANTIFEED_INTERVAL > 0) && _lastDeathTimes.containsKey(objectId) && ((System.currentTimeMillis() - _lastDeathTimes.get(objectId)) < PvpConfig.ANTIFEED_INTERVAL))
		{
			return false;
		}
		
		if (PvpConfig.ANTIFEED_DUALBOX && (attacker != null))
		{
			final Player attackerPlayer = attacker.asPlayer();
			if (attackerPlayer == null)
			{
				return false;
			}
			
			final GameClient targetClient = targetPlayer.getClient();
			final GameClient attackerClient = attackerPlayer.getClient();
			if ((targetClient == null) || (attackerClient == null) || targetClient.isDetached() || attackerClient.isDetached())
			{
				// Unable to check ip address.
				return !PvpConfig.ANTIFEED_DISCONNECTED_AS_DUALBOX;
			}
			
			return !targetClient.getIp().equals(attackerClient.getIp());
		}
		
		return true;
	}
	
	/**
	 * Removes all data from the internal tracking maps.<br>
	 * This resets the death times and event information for all objects.
	 */
	public void clear()
	{
		_lastDeathTimes.clear();
	}
	
	/**
	 * Registers a new event in the system.<br>
	 * This method initializes the internal data structure for the given {@code eventId}.<br>
	 * It ensures that the manager can track players and clients for this specific event.
	 * @param eventId The unique identifier of the event to register.
	 */
	public void registerEvent(int eventId)
	{
		_eventIPs.putIfAbsent(eventId, new ConcurrentHashMap<>());
	}
	
	/**
	 * Attempts to add a {@code Player} to a specific event.<br>
	 * This method delegates the logic to {@code tryAddClient}.<br>
	 * It checks if the player can be added based on the maximum limit.
	 * @param eventId The unique identifier for the event.
	 * @param player The {@code Player} object to add.
	 * @param max The maximum number of allowed participants.
	 * @return {@code true} if the player was successfully added, {@code false} otherwise.
	 */
	public boolean tryAddPlayer(int eventId, Player player, int max)
	{
		return tryAddClient(eventId, player.getClient(), max);
	}
	
	/**
	 * Attempts to add a {@code GameClient} to a specific event.<br>
	 * This method checks if the client's IP address exceeds the allowed limit.<br>
	 * It returns {@code true} if the addition is successful.
	 * @param eventId The unique identifier for the event.
	 * @param client The {@code GameClient} to be added.
	 * @param max The maximum number of connections allowed for this event.
	 * @return {@code true} if the client was successfully added, otherwise {@code false}.
	 */
	public boolean tryAddClient(int eventId, GameClient client, int max)
	{
		if (client == null)
		{
			return false; // Unable to determine IP address.
		}
		
		final Map<Integer, AtomicInteger> event = _eventIPs.get(eventId);
		if (event == null)
		{
			return false; // no such event registered
		}
		
		final Integer addrHash = client.getIp().hashCode();
		final AtomicInteger connectionCount = event.computeIfAbsent(addrHash, _ -> new AtomicInteger());
		if ((connectionCount.get() + 1) <= (max + DualboxCheckConfig.DUALBOX_CHECK_WHITELIST.getOrDefault(addrHash, 0)))
		{
			connectionCount.incrementAndGet();
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes a {@code Player} from a specific event.<br>
	 * This method uses the {@code GameClient} associated with the {@code Player}.
	 * @param eventId The unique identifier for the event.
	 * @param player The {@code Player} to be removed.
	 * @return {@code true} if the removal was successful, {@code false} otherwise.
	 */
	public boolean removePlayer(int eventId, Player player)
	{
		return removeClient(eventId, player.getClient());
	}
	
	/**
	 * Removes a {@code GameClient} from the anti-feed tracking for a specific event.<br>
	 * This method decrements the count of clients associated with the client's IP address.<br>
	 * It returns {@code false} if the client is {@code null} or the event does not exist.
	 * @param eventId The unique identifier for the game event.
	 * @param client The {@code GameClient} to be removed from the tracking map.
	 * @return {@code true} if the client was successfully found and processed, otherwise {@code false}.
	 */
	public boolean removeClient(int eventId, GameClient client)
	{
		if (client == null)
		{
			return false; // Unable to determine IP address.
		}
		
		final Map<Integer, AtomicInteger> event = _eventIPs.get(eventId);
		if (event == null)
		{
			return false; // no such event registered
		}
		
		final Integer addrHash = client.getIp().hashCode();
		return event.computeIfPresent(addrHash, (_, v) ->
		{
			if ((v == null) || (v.decrementAndGet() == 0))
			{
				return null;
			}
			
			return v;
		}) != null;
	}
	
	/**
	 * Handles the cleanup logic when a {@code GameClient} disconnects.<br>
	 * This method removes the client from active event tracking.<br>
	 * It also updates specific counts for olympiad registrations based on the IP address.
	 * @param client The {@code GameClient} that is disconnecting.
	 */
	public void onDisconnect(GameClient client)
	{
		if (client == null)
		{
			return;
		}
		
		final Player player = client.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (player.getOfflineMode().isEnabled())
		{
			return;
		}
		
		final String clientIp = client.getIp();
		if (clientIp == null)
		{
			return;
		}
		
		for (Entry<Integer, Map<Integer, AtomicInteger>> entry : _eventIPs.entrySet())
		{
			final int eventId = entry.getKey();
			if (eventId == OLYMPIAD_ID)
			{
				final AtomicInteger count = entry.getValue().get(clientIp.hashCode());
				if ((count != null) && (OlympiadManager.getInstance().isRegistered(player) || (player.getOlympiadGameId() != -1)))
				{
					count.decrementAndGet();
				}
			}
			else
			{
				removeClient(eventId, client);
			}
		}
	}
	
	/**
	 * Removes all data for a specific event.<br>
	 * This method clears the IP tracking map associated with the provided {@code eventId}.<br>
	 * If no data exists for that ID, nothing happens.
	 * @param eventId The unique identifier of the event to clear.
	 */
	public void clear(int eventId)
	{
		final Map<Integer, AtomicInteger> event = _eventIPs.get(eventId);
		if (event != null)
		{
			event.clear();
		}
	}
	
	/**
	 * Retrieves the current limit for a specific player.<br>
	 * This method checks the limit based on the player's associated {@code GameClient}.
	 * @param player The {@code Player} object to check.
	 * @param max The maximum allowed limit.
	 * @return The calculated limit as an {@code int}.
	 */
	public int getLimit(Player player, int max)
	{
		return getLimit(player.getClient(), max);
	}
	
	/**
	 * Calculates the maximum allowed connection limit for a specific client.<br>
	 * It checks if the client's IP address is in the dualbox whitelist.<br>
	 * If the whitelist entry is less than 1, it returns {@code Integer.MAX_VALUE}.<br>
	 * Otherwise, it adds the whitelist value to the provided maximum.
	 * @param client The {@code GameClient} to check for limits.
	 * @param max The base maximum limit.
	 * @return The calculated connection limit as an {@code int}.
	 */
	public int getLimit(GameClient client, int max)
	{
		if (client == null)
		{
			return max;
		}
		
		final Integer addrHash = client.getIp().hashCode();
		int limit = max;
		final Integer whiteListLimit = DualboxCheckConfig.DUALBOX_CHECK_WHITELIST.get(addrHash);
		if (whiteListLimit != null)
		{
			if (whiteListLimit < 1) // DualboxCheckWhitelist takes 0 or negative value for unlimited number of connections.
			{
				return Integer.MAX_VALUE;
			}
			
			limit += whiteListLimit;
		}
		
		return limit;
	}
	
	/**
	 * Retrieves the singleton instance of {@code AntiFeedManager}.<br>
	 * This provides a global access point for anti-feeding logic.
	 * @return The single instance of {@code AntiFeedManager}.
	 */
	public static AntiFeedManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final AntiFeedManager INSTANCE = new AntiFeedManager();
	}
}
