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

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.ceremonyofchaos.OnCeremonyOfChaosMatchResult;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to the Ceremony of Chaos.<br>
 * It processes match results and updates player progress within {@code DailyMissionDataHolder}.
 * @author UnAfraid
 */
public class CeremonyOfChaosDailyMissionHandler extends AbstractDailyMissionHandler
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
	 * Initializes the handler for Ceremony of Chaos daily missions.<br>
	 * It sets up the required completion amount from the provided data.
	 * @param holder The {@code DailyMissionDataHolder} containing mission details.
	 */
	public CeremonyOfChaosDailyMissionHandler(DailyMissionDataHolder holder)
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
	 * Initializes the handler for daily missions.<br>
	 * This method registers the necessary event listeners.<br>
	 * It ensures that match results are correctly captured.
	 */
	@Override
	public void init()
	{
		Containers.Players().addListener(new ConsumerEventListener(this, EventType.ON_CEREMONY_OF_CHAOS_MATCH_RESULT, (OnCeremonyOfChaosMatchResult event) -> onCeremonyOfChaosMatchResult(event), this));
	}
	
	/**
	 * Checks if the daily mission is ready for the player.<br>
	 * It verifies if the player has met the required progress amount.<br>
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
	 * Handles the results of a Ceremony of Chaos match.<br>
	 * It updates the progress for all members involved in the event.<br>
	 * If a player reaches the required amount, their mission status changes to {@code AVAILABLE}.
	 * @param event The {@code OnCeremonyOfChaosMatchResult} containing the match details.
	 */
	private void onCeremonyOfChaosMatchResult(OnCeremonyOfChaosMatchResult event)
	{
		List<Player> members;
		if (_winOnly)
		{
			members = event.getWinners();
		}
		else
		{
			members = event.getMembers();
		}
		
		members.forEach(member ->
		{
			if (checkPlayerLevel(member))
			{
				// Check clan only for specific missions
				if ((_missionId == 2109) || (_missionId == 2110))
				{
					if (checkClan(member))
					{
						if (((_requiredMissionCompleteId != 0) && checkRequiredMission(member)) || (_requiredMissionCompleteId == 0))
						{
							processPlayerProgress(member);
						}
					}
				}
				else
				{
					if (((_requiredMissionCompleteId != 0) && checkRequiredMission(member)) || (_requiredMissionCompleteId == 0))
					{
						processPlayerProgress(member);
					}
				}
			}
		});
	}
	
	/**
	 * Updates the progress for a specific player's daily mission.<br>
	 * It checks if the {@code DailyMissionStatus} is {@code NOT_AVAILABLE}.<br>
	 * If the required amount is met, it changes the status to {@code AVAILABLE}.<br>
	 * The updated entry is then saved to the database.
	 * @param player The {@code Player} whose progress needs to be processed.
	 */
	private void processPlayerProgress(Player player)
	{
		final DailyMissionPlayerEntry entry = getPlayerEntry(player.getObjectId(), true);
		if (entry.getStatus() == DailyMissionStatus.NOT_AVAILABLE)
		{
			if (entry.increaseProgress() >= _amount)
			{
				entry.setStatus(DailyMissionStatus.AVAILABLE);
			}
			
			storePlayerEntry(entry);
		}
	}
	
	/**
	 * Verifies if the player meets the level requirements.<br>
	 * It checks if the {@code Player} level is within the allowed range.<br>
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
	 * Verifies if the player belongs to a clan that meets the required level and mastery.<br>
	 * This method checks both the clan level and the specific mastery level.<br>
	 * It returns {@code false} if the player is {@code null}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the clan requirements are met, otherwise {@code false}.
	 */
	private boolean checkClan(Player player)
	{
		if (player == null)
		{
			return false;
		}
		
		final Clan clan = player.getClan();
		final int clanMastery = clan.hasMastery(14) ? 14 : clan.hasMastery(15) ? 15 : clan.hasMastery(16) ? 16 : 0;
		return ((clan.getLevel() >= _minClanLevel) && (clanMastery >= _minClanMasteryLevel));
	}
	
	/**
	 * Checks if the player has completed a specific required mission.<br>
	 * It verifies that the mission entry exists and matches the required ID.<br>
	 * The status must be set to {@code DailyMissionStatus.COMPLETED}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	private boolean checkRequiredMission(Player player)
	{
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
