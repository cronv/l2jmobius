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
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.config.custom.DualboxCheckConfig;
import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.AntiFeedManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic and state for the {@code Olympiad} system.<br>
 * It handles player participation, scoring, and rewards during competitions.
 * @author DS, Mobius
 */
public class OlympiadManager
{
	private final Set<Integer> _nonClassBasedRegisters = ConcurrentHashMap.newKeySet();
	private final Map<Integer, Set<Integer>> _classBasedRegisters = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code OlympiadManager}.<br>
	 * This constructor is protected to ensure only internal classes can create it.<br>
	 * Use {@code getInstance} instead.
	 */
	protected OlympiadManager()
	{
	}
	
	/**
	 * Retrieves the set of players registered in non-class-based competitions.<br>
	 * This method returns all IDs from the {@code _nonClassBasedRegisters} collection.
	 * @return a {@code Set<Integer>} containing the registered player IDs.
	 */
	public Set<Integer> getRegisteredNonClassBased()
	{
		return _nonClassBasedRegisters;
	}
	
	/**
	 * Retrieves the map of class-based registrations.<br>
	 * The keys represent the class group IDs.<br>
	 * The values are sets of player IDs registered in those groups.
	 * @return a {@code Map<Integer, Set<Integer>>} containing all class-based registrations.
	 */
	public Map<Integer, Set<Integer>> getRegisteredClassBased()
	{
		return _classBasedRegisters;
	}
	
	/**
	 * Checks which class groups have enough registered players.<br>
	 * It compares the size of each group against {@code OlympiadConfig.OLYMPIAD_CLASSED}.
	 * @return A {@code List} of {@code Set} objects containing the valid class groups.
	 */
	protected List<Set<Integer>> hasEnoughRegisteredClassed()
	{
		List<Set<Integer>> result = null;
		for (Entry<Integer, Set<Integer>> classList : _classBasedRegisters.entrySet())
		{
			final Set<Integer> value = classList.getValue();
			if ((value != null) && (value.size() >= OlympiadConfig.OLYMPIAD_CLASSED))
			{
				if (result == null)
				{
					result = new ArrayList<>();
				}
				
				result.add(value);
			}
		}
		
		return result;
	}
	
	/**
	 * Checks if the number of non-class based registrations meets the required amount.<br>
	 * It compares the size of {@code _nonClassBasedRegisters} against the value in {@code OlympiadConfig.OLYMPIAD_NONCLASSED}.
	 * @return {@code true} if there are enough registered players, otherwise {@code false}.
	 */
	protected boolean hasEnoughRegisteredNonClassed()
	{
		return _nonClassBasedRegisters.size() >= OlympiadConfig.OLYMPIAD_NONCLASSED;
	}
	
	/**
	 * Clears all registered players from the olympiad system.<br>
	 * This method empties both class-based and non-class-based register sets.<br>
	 * It also clears the data in {@code AntiFeedManager}.
	 */
	protected void clearRegistered()
	{
		_nonClassBasedRegisters.clear();
		_classBasedRegisters.clear();
		AntiFeedManager.getInstance().clear(AntiFeedManager.OLYMPIAD_ID);
	}
	
	/**
	 * Checks if a specific {@code Player} is currently registered.<br>
	 * This method returns {@code true} if the player is in the registry.<br>
	 * It returns {@code false} otherwise.
	 * @param noble The {@code Player} to check.
	 * @return {@code true} if the player is registered, {@code false} otherwise.
	 */
	public boolean isRegistered(Player noble)
	{
		return isRegistered(noble, noble, false);
	}
	
	/**
	 * Checks if a specific noble is already registered in the olympiad.<br>
	 * It checks both class-based and non-class-based registration lists.<br>
	 * If {@code showMessage} is {@code true}, it sends a notification to the player.
	 * @param noble The {@code Player} object to check for registration status.
	 * @param player The {@code Player} who will receive the message if one is shown.
	 * @param showMessage A boolean flag indicating whether to send a system message.
	 * @return {@code true} if the noble is registered, otherwise {@code false}.
	 */
	private boolean isRegistered(Player noble, Player player, boolean showMessage)
	{
		final Integer objId = noble.getObjectId();
		if (_nonClassBasedRegisters.contains(objId))
		{
			if (showMessage)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_FOR_ALL_CLASS_BATTLES);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			
			return true;
		}
		
		final Set<Integer> classed = _classBasedRegisters.get(getClassGroup(noble));
		if ((classed != null) && classed.contains(objId))
		{
			if (showMessage)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
				sm.addPcName(noble);
				player.sendPacket(sm);
			}
			
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if a {@code Player} is currently involved in any competition.<br>
	 * This method returns {@code true} if the player is registered or actively competing.
	 * @param noble The {@code Player} to check.
	 * @return {@code true} if the player is part of a competition, {@code false} otherwise.
	 */
	public boolean isRegisteredInComp(Player noble)
	{
		return isRegistered(noble, noble, false) || isInCompetition(noble, noble, false);
	}
	
	/**
	 * Checks if a specific noble is currently participating in an olympiad.<br>
	 * It iterates through all active stadiums to find the player.<br>
	 * If found, it may send a system message to the other player based on the {@code showMessage} flag.
	 * @param noble The player whose competition status is being checked.
	 * @param player The player who will receive a notification if {@code showMessage} is {@code true}.
	 * @param showMessage Determines whether to send a packet to the second player.
	 * @return {@code true} if the noble is in a game, otherwise {@code false}.
	 */
	private boolean isInCompetition(Player noble, Player player, boolean showMessage)
	{
		if (!Olympiad._inCompPeriod)
		{
			return false;
		}
		
		AbstractOlympiadGame game;
		for (int i = OlympiadGameManager.getInstance().getNumberOfStadiums(); --i >= 0;)
		{
			game = OlympiadGameManager.getInstance().getOlympiadTask(i).getGame();
			if (game == null)
			{
				continue;
			}
			
			if (game.containsParticipant(noble.getObjectId()))
			{
				if (!showMessage)
				{
					return true;
				}
				
				switch (game.getType())
				{
					case CLASSED:
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_ON_THE_CLASS_MATCH_WAITING_LIST);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
					case NON_CLASSED:
					{
						final SystemMessage sm = new SystemMessage(SystemMessageId.C1_IS_ALREADY_REGISTERED_FOR_ALL_CLASS_BATTLES);
						sm.addPcName(noble);
						player.sendPacket(sm);
						break;
					}
				}
				
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Registers a player for an olympiad competition.<br>
	 * This method checks if the player is eligible to join based on time, match limits, and current status.<br>
	 * It handles both class-based and non-class-based registration types.
	 * @param player The {@code Player} attempting to register.
	 * @param type The {@code CompetitionType} of the olympiad.
	 * @return {@code true} if the registration was successful, {@code false} otherwise.
	 */
	public boolean registerNoble(Player player, CompetitionType type)
	{
		if (!Olympiad._inCompPeriod)
		{
			player.sendPacket(SystemMessageId.THE_OLYMPIAD_IS_NOT_HELD_RIGHT_NOW);
			return false;
		}
		
		if (Olympiad.getInstance().getMillisToCompEnd() < 1200000)
		{
			player.sendPacket(SystemMessageId.GAME_PARTICIPATION_REQUEST_MUST_BE_FILED_NOT_EARLIER_THAN_10_MIN_AFTER_THE_GAME_ENDS);
			return false;
		}
		
		if (player.isFishing())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_PARTICIPATE_IN_THE_OLYMPIAD_WHILE_FISHING);
			return false;
		}
		
		final int charId = player.getObjectId();
		if (Olympiad.getInstance().getRemainingWeeklyMatches(charId) < 1)
		{
			player.sendPacket(SystemMessageId.THE_MAXIMUM_NUMBER_OF_MATCHES_YOU_CAN_PARTICIPATE_IN_1_WEEK_IS_25);
			return false;
		}
		
		if (isRegistered(player, player, true) || isInCompetition(player, player, true))
		{
			return false;
		}
		
		StatSet statDat = Olympiad.getNobleStats(charId);
		if (statDat == null)
		{
			statDat = new StatSet();
			statDat.set(Olympiad.CLASS_ID, player.getBaseClass());
			statDat.set(Olympiad.CHAR_NAME, player.getName());
			statDat.set(Olympiad.POINTS, Olympiad.DEFAULT_POINTS);
			statDat.set(Olympiad.COMP_DONE, 0);
			statDat.set(Olympiad.COMP_WON, 0);
			statDat.set(Olympiad.COMP_LOST, 0);
			statDat.set(Olympiad.COMP_DRAWN, 0);
			statDat.set(Olympiad.COMP_DONE_WEEK, 0);
			statDat.set("to_save", true);
			Olympiad.addNobleStats(charId, statDat);
		}
		
		switch (type)
		{
			case CLASSED:
			{
				if (player.isRegisteredOnEvent())
				{
					player.sendMessage("You can't join olympiad while participating on an event.");
					return false;
				}
				
				if ((DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OLYMPIAD_ID, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP))
				{
					final NpcHtmlMessage message = new NpcHtmlMessage(player.getLastHtmlActionOriginId());
					message.setFile(player, "data/html/mods/OlympiadIPRestriction.htm");
					message.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP)));
					player.sendPacket(message);
					return false;
				}
				
				_classBasedRegisters.computeIfAbsent(getClassGroup(player), _ -> ConcurrentHashMap.newKeySet()).add(charId);
				player.sendPacket(SystemMessageId.YOU_VE_BEEN_REGISTERED_FOR_THE_OLYMPIAD_CLASS_MATCHES);
				break;
			}
			case NON_CLASSED:
			{
				if (player.isRegisteredOnEvent())
				{
					player.sendMessage("You can't join olympiad while participating on an event.");
					return false;
				}
				
				if ((DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0) && !AntiFeedManager.getInstance().tryAddPlayer(AntiFeedManager.OLYMPIAD_ID, player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP))
				{
					final NpcHtmlMessage message = new NpcHtmlMessage(player.getLastHtmlActionOriginId());
					message.setFile(player, "data/html/mods/OlympiadIPRestriction.htm");
					message.replace("%max%", String.valueOf(AntiFeedManager.getInstance().getLimit(player, DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP)));
					player.sendPacket(message);
					return false;
				}
				
				_nonClassBasedRegisters.add(charId);
				player.sendPacket(SystemMessageId.YOU_HAVE_REGISTERED_IN_THE_WORLD_OLYMPIAD);
				break;
			}
		}
		
		return true;
	}
	
	/**
	 * Removes a player from the Olympiad waiting list.<br>
	 * This method checks if the {@code Player} is eligible and currently registered.<br>
	 * It handles both class-based and non-class-based registrations.
	 * @param noble The {@code Player} to be removed from the registration.
	 * @return {@code true} if the player was successfully unregistered, otherwise {@code false}.
	 */
	public boolean unRegisterNoble(Player noble)
	{
		if (!Olympiad._inCompPeriod)
		{
			noble.sendPacket(SystemMessageId.THE_OLYMPIAD_IS_NOT_HELD_RIGHT_NOW);
			return false;
		}
		
		if (noble.getNobleLevel() == 0)
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_DOES_NOT_MEET_THE_PARTICIPATION_REQUIREMENTS_ONLY_NOBLESSE_CHARACTERS_CAN_PARTICIPATE_IN_THE_OLYMPIAD);
			sm.addString(noble.getName());
			noble.sendPacket(sm);
			return false;
		}
		
		if (!isRegistered(noble, noble, false))
		{
			noble.sendPacket(SystemMessageId.YOU_ARE_NOT_CURRENTLY_REGISTERED_FOR_THE_OLYMPIAD);
			return false;
		}
		
		if (isInCompetition(noble, noble, false))
		{
			return false;
		}
		
		final Integer objId = noble.getObjectId();
		if (_nonClassBasedRegisters.remove(objId))
		{
			if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, noble);
			}
			
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REMOVED_FROM_THE_OLYMPIAD_WAITING_LIST);
			return true;
		}
		
		final Set<Integer> classed = _classBasedRegisters.get(getClassGroup(noble));
		if ((classed != null) && classed.remove(objId))
		{
			if (DualboxCheckConfig.DUALBOX_CHECK_MAX_OLYMPIAD_PARTICIPANTS_PER_IP > 0)
			{
				AntiFeedManager.getInstance().removePlayer(AntiFeedManager.OLYMPIAD_ID, noble);
			}
			
			noble.sendPacket(SystemMessageId.YOU_HAVE_BEEN_REMOVED_FROM_THE_OLYMPIAD_WAITING_LIST);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes a {@code Player} from the active competition registers.<br>
	 * This method handles cleanup if the player disconnects during an active game.<br>
	 * It removes the player's unique ID from both class-based and non-class-based sets.
	 * @param player The {@code Player} object to be removed from the records.
	 */
	public void removeDisconnectedCompetitor(Player player)
	{
		final OlympiadGameTask task = OlympiadGameManager.getInstance().getOlympiadTask(player.getOlympiadGameId());
		if ((task != null) && task.isGameStarted())
		{
			task.getGame().handleDisconnect(player);
		}
		
		final Integer objId = player.getObjectId();
		if (_nonClassBasedRegisters.remove(objId))
		{
			return;
		}
		
		_classBasedRegisters.getOrDefault(getClassGroup(player), Collections.emptySet()).remove(objId);
	}
	
	/**
	 * Calculates the total number of registered opponents.<br>
	 * This counts both non-class based and class based registers.
	 * @return The total count of all registered opponents.
	 */
	public int getCountOpponents()
	{
		return _nonClassBasedRegisters.size() + _classBasedRegisters.size();
	}
	
	/**
	 * Determines the group identifier for a specific {@code Player}.<br>
	 * It checks if the player belongs to any special category groups.<br>
	 * If no special group is found, it returns the base class of the player.
	 * @param player The {@code Player} to check.
	 * @return The integer ID of the group or the base class.
	 */
	private int getClassGroup(Player player)
	{
		if (player.isInCategory(CategoryType.SIXTH_TIR_GROUP))
		{
			return 1001;
		}
		else if (player.isInCategory(CategoryType.SIXTH_SIGEL_GROUP))
		{
			return 1002;
		}
		else if (player.isInCategory(CategoryType.SIXTH_OTHEL_GROUP))
		{
			return 1003;
		}
		else if (player.isInCategory(CategoryType.SIXTH_FEOH_GROUP))
		{
			return 1004;
		}
		else if (player.isInCategory(CategoryType.SIXTH_IS_GROUP))
		{
			return 1005;
		}
		else if (player.isInCategory(CategoryType.SIXTH_EOLH_GROUP))
		{
			return 1006;
		}
		else if (player.isInCategory(CategoryType.SIXTH_WYNN_GROUP))
		{
			return 1007;
		}
		else if (player.isInCategory(CategoryType.SIXTH_YR_GROUP))
		{
			return 1008;
		}
		else if (player.isInCategory(CategoryType.ERTHEIA_FOURTH_CLASS_GROUP))
		{
			return 1009;
		}
		else
		{
			return player.getBaseClass();
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code OlympiadManager}.<br>
	 * This method provides a global access point to the manager.<br>
	 * Use this to interact with olympiad mechanics throughout the server.
	 * @return The single instance of {@code OlympiadManager}.
	 */
	public static OlympiadManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final OlympiadManager INSTANCE = new OlympiadManager();
	}
}
