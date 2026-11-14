/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
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
import org.l2jmobius.gameserver.mechanics.events.holders.olympiad.OnOlympiadMatchResult;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to {@code Olympiad} events.<br>
 * It listens for match results and updates the player's mission progress accordingly.
 * @author UnAfraid
 */
public class OlympiadDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _missionId;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _minClanLevel;
	private final int _minClanMasteryLevel;
	private final int _amount;
	private final boolean _winOnly;
	private final int _requiredMissionCompleteId;
	
	/**
	 * Initializes a new instance of the {@code OlympiadDailyMissionHandler}.<br>
	 * It sets up the required completion amount and win conditions from the provided data.
	 * @param holder The {@code DailyMissionDataHolder} containing the mission configuration.
	 */
	public OlympiadDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_missionId = holder.getId();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		_minClanLevel = holder.getParams().getInt("minClanLevel", 0);
		_minClanMasteryLevel = holder.getParams().getInt("minClanMasteryLevel", 0);
		_amount = holder.getRequiredCompletions();
		_winOnly = holder.getParams().getBoolean("winOnly", false);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
	}
	
	/**
	 * Initializes the handler for olympiad daily missions.<br>
	 * This method registers the necessary event listeners to track match results.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(this, EventType.ON_OLYMPIAD_MATCH_RESULT, (OnOlympiadMatchResult event) -> onOlympiadMatchResult(event), this));
	}
	
	/**
	 * Checks if the Olympiad daily mission is ready for the player.<br>
	 * It verifies if the required progress has been met.<br>
	 * The method returns {@code true} if the mission status is {@code AVAILABLE}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if available, {@code false} otherwise.
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
	 * Handles the logic for updating daily missions after an Olympiad match.<br>
	 * It checks if players have met the required progress to complete their goals.<br>
	 * This method updates both winners and losers depending on the mission configuration.
	 * @param event The {@code OnOlympiadMatchResult} containing the match outcome.
	 */
	private void onOlympiadMatchResult(OnOlympiadMatchResult event)
	{
		if ((event.getWinner() != null) && checkPlayerLevel(event.getWinner().getPlayer()))
		{
			// Check clan only for specific missions
			final Player winnerPlayer = event.getWinner().getPlayer();
			if ((_missionId == 2107) || (_missionId == 2108))
			{
				if (checkClan(winnerPlayer))
				{
					processPlayerProgress(winnerPlayer);
				}
			}
			else
			{
				processPlayerProgress(winnerPlayer);
			}
		}
		
		if (!_winOnly && (event.getLoser() != null) && checkPlayerLevel(event.getLoser().getPlayer()))
		{
			final DailyMissionPlayerEntry loserEntry = getPlayerEntry(event.getLoser().getObjectId(), true);
			if ((loserEntry.getStatus() == DailyMissionStatus.NOT_AVAILABLE) && (((_requiredMissionCompleteId != 0) && checkRequiredMission(event.getLoser().getPlayer())) || (_requiredMissionCompleteId == 0)))
			{
				if (loserEntry.increaseProgress() >= _amount)
				{
					loserEntry.setStatus(DailyMissionStatus.AVAILABLE);
				}
				
				storePlayerEntry(loserEntry);
			}
		}
	}
	
	/**
	 * Verifies if the {@code Player} meets the level requirements.<br>
	 * It checks if the player's level is within the allowed range.<br>
	 * Returns {@code false} if the {@code Player} object is {@code null}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the level is valid, otherwise {@code false}.
	 */
	private boolean checkPlayerLevel(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		return ((player.getLevel() >= _minLevel)) || (player.getLevel() <= _maxLevel);
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
	 * Checks if the player has completed a specific prerequisite mission.<br>
	 * It verifies that the required mission ID matches the reward ID of the current entry.<br>
	 * The status must also be set to {@code DailyMissionStatus.COMPLETED}.
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
	
	/**
	 * Updates the progress for a specific player's daily mission.<br>
	 * This method checks if the player meets all requirements to participate.<br>
	 * It increases the progress amount and updates the status if the goal is met.
	 * @param player The {@code Player} object to process.
	 */
	private void processPlayerProgress(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);
		if ((entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE) && (((_requiredMissionCompleteId != 0) && checkRequiredMission(player)) || (_requiredMissionCompleteId == 0)))
		{
			if (entry.increaseProgress() >= _amount)
			{
				entry.setStatus(DailyMissionStatus.AVAILABLE);
			}
			
			storePlayerEntry(entry);
		}
	}
}
