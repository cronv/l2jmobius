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
package handlers.dailymissions;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerTakeHero;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to the Olympiad Hero system.<br>
 * It processes specific events to update {@code DailyMissionDataHolder} for players.
 * @author CostyKiller
 */
public class OlympiadHeroDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _minLevel;
	private final int _maxLevel;
	private final int _minClanLevel;
	private final int _minClanMasteryLevel;
	private final int _amount;
	private final int _requiredMissionCompleteId;
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * This constructor sets up requirements like level and clan status.<br>
	 * It also defines the amount of completions needed for this mission.
	 * @param holder The data container providing configuration for the daily mission.
	 */
	public OlympiadHeroDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		_minClanLevel = holder.getParams().getInt("minClanLevel", 0);
		_minClanMasteryLevel = holder.getParams().getInt("minClanMasteryLevel", 0);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
		_amount = holder.getRequiredCompletions();
	}
	
	/**
	 * Initializes the handler for the Olympiad Hero daily mission.<br>
	 * This method registers the necessary event listeners to track player actions.<br>
	 * It ensures that {@code ON_PLAYER_TAKE_HERO} events are correctly handled.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_PLAYER_TAKE_HERO, (OnPlayerTakeHero event) -> onPlayerTakeHero(event), this));
	}
	
	/**
	 * Checks if the daily mission is currently available for a specific player.<br>
	 * It verifies the status of the {@code DailyMissionPlayerEntry}.<br>
	 * The method returns {@code true} if the requirements are met and the status is set to {@code AVAILABLE}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the mission is available, otherwise {@code false}.
	 */
	@Override
	public boolean isAvailable(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), false);
		if (entry != null)
		{
			switch (entry.getStatus())
			{
				case NOT_AVAILABLE: // Initial state
				{
					if (entry.getProgress() >= _amount)
					{
						entry.setStatus(DailyMissionStatus.AVAILABLE);
						storePlayerEntry(entry);
					}
					break;
				}
				case AVAILABLE:
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Handles the logic when a player takes a hero.<br>
	 * This method checks if the player meets the level and clan requirements.<br>
	 * It updates the progress of the daily mission for the player.
	 * @param event The {@code OnPlayerTakeHero} event containing the player data.
	 */
	private void onPlayerTakeHero(OnPlayerTakeHero event)
	{
		Player player = event.getPlayer();
		if (player != null)
		{
			final int level = player.getLevel();
			if ((level < _minLevel) || (level > _maxLevel) || !checkClan(player))
			{
				return;
			}
			
			final DailyMissionPlayerEntry heroEntry = getPlayerEntry(player.getObjectId(), true);
			if ((heroEntry.getStatus() == DailyMissionStatus.NOT_AVAILABLE) && (((_requiredMissionCompleteId != 0) && checkRequiredMission(player)) || (_requiredMissionCompleteId == 0)))
			{
				if (heroEntry.increaseProgress() >= _amount)
				{
					heroEntry.setStatus(DailyMissionStatus.AVAILABLE);
				}
				
				storePlayerEntry(heroEntry);
			}
		}
	}
	
	/**
	 * Verifies if the player meets the required clan requirements.<br>
	 * It checks if the player belongs to a {@code Clan}.<br>
	 * It also validates the clan level and mastery level.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	private boolean checkClan(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return false;
		}
		
		final int clanMastery = clan.hasMastery(14) ? 14 : clan.hasMastery(15) ? 15 : clan.hasMastery(16) ? 16 : 0;
		return ((clan.getLevel() >= _minClanLevel) && (clanMastery >= _minClanMasteryLevel));
	}
	
	/**
	 * Verifies if the player has completed a specific required mission.<br>
	 * This check ensures the correct reward ID is associated with a finished status.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	private boolean checkRequiredMission(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
