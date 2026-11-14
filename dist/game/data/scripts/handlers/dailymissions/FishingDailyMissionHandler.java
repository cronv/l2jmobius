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
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerFishing;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.fishing.FishingEndReason;

/**
 * Handles the logic for daily missions related to fishing activities.<br>
 * It listens for {@code OnPlayerFishing} events to track and update player progress.
 * @author UnAfraid
 */
public class FishingDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * It sets the required amount and level limits for the fishing mission.
	 * @param holder The holder containing the specific mission configuration.
	 */
	public FishingDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
	}
	
	/**
	 * Initializes the handler by registering necessary event listeners.<br>
	 * This method sets up the {@code ON_PLAYER_FISHING} event to track player actions.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_FISHING, (OnPlayerFishing event) -> onPlayerFishing(event), this));
	}
	
	/**
	 * Checks if the fishing daily mission is ready for the player.<br>
	 * It verifies the current status of the mission in the {@code DailyMissionPlayerEntry}.<br>
	 * The method returns {@code true} if the mission is currently active.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if available, otherwise {@code false}.
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
	 * Handles the logic when a player finishes fishing.<br>
	 * It checks if the player meets the required level range.<br>
	 * If the fish is caught successfully, it updates the mission progress.
	 * @param event The {@code OnPlayerFishing} event containing the player and fishing details.
	 */
	private void onPlayerFishing(OnPlayerFishing event)
	{
		final Player player = event.getPlayer();
		if ((player.getLevel() < _minLevel) || (player.getLevel() > _maxLevel))
		{
			return;
		}
		
		if (event.getReason() == FishingEndReason.WIN)
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
	}
}
