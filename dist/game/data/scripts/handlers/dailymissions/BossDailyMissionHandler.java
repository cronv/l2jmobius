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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.entity.groups.CommandChannel;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnAttackableKill;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to defeating bosses.<br>
 * It listens for {@code OnAttackableKill} events to track progress for specific boss targets.
 * @author UnAfraid
 */
public class BossDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _requiredMissionCompleteId;
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _ids = new HashSet<>();
	
	/**
	 * Initializes the handler for boss daily missions.<br>
	 * This constructor sets up the required completion amount from the provided data.
	 * @param holder The {@code DailyMissionDataHolder} containing the mission configuration.
	 */
	public BossDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 0);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		final String ids = holder.getParams().getString("ids", "");
		if (!ids.isEmpty())
		{
			for (String s : ids.split(","))
			{
				final int id = Integer.parseInt(s);
				if (!_ids.contains(id))
				{
					_ids.add(id);
				}
			}
		}
	}
	
	/**
	 * Initializes the handler for boss daily missions.<br>
	 * This method registers a listener to track monster kills.<br>
	 * It uses {@code EventType.ON_ATTACKABLE_KILL} to trigger progress updates.
	 */
	@Override
	public void init()
	{
		Containers.Monsters().addListener(new ConsumerEventListener(this, EventType.ON_ATTACKABLE_KILL, (OnAttackableKill event) -> onAttackableKill(event), this));
	}
	
	/**
	 * Checks if the boss daily mission is ready for the player.<br>
	 * It verifies if the player has met the required progress amount.<br>
	 * The method returns {@code true} if the status is set to {@code AVAILABLE}.
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
	 * Handles the logic when a player kills an attackable target.<br>
	 * This method checks if the monster is a raid boss.<br>
	 * It updates progress for the attacker or nearby party members.
	 * @param event The {@code OnAttackableKill} event containing the attacker and the target.
	 */
	private void onAttackableKill(OnAttackableKill event)
	{
		final Attackable monster = event.getTarget();
		if (!_ids.isEmpty() && !_ids.contains(monster.getId()))
		{
			return;
		}
		
		final Player player = event.getAttacker();
		if (((_requiredMissionCompleteId != 0) && checkRequiredMission(player)) || (_requiredMissionCompleteId == 0))
		{
			if (monster.isRaid() && (player != null) && checkPlayerLevel(player))
			{
				final Party party = player.getParty();
				if (party != null)
				{
					final CommandChannel channel = party.getCommandChannel();
					final List<Player> members = channel != null ? channel.getMembers() : party.getMembers();
					for (Player member : members)
					{
						if (member.calculateDistance3D(monster) <= PlayerConfig.ALT_PARTY_RANGE)
						{
							processPlayerProgress(member);
						}
					}
				}
				else
				{
					processPlayerProgress(player);
				}
			}
		}
	}
	
	/**
	 * Updates the progress for a player's daily mission.<br>
	 * It checks if the player has met the required amount to unlock the mission.<br>
	 * The status is updated and saved if the goal is reached.
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
	 * Verifies if the player's level is within the allowed range.<br>
	 * It checks if the {@code Player} object is not {@code null}.<br>
	 * The check ensures the level is between {@code _minLevel} and {@code _maxLevel}.
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
	 * Verifies if the player has completed a specific required mission.<br>
	 * It checks if the mission entry exists and matches the required ID.<br>
	 * It also ensures the current status is {@code DailyMissionStatus.COMPLETED}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	private boolean checkRequiredMission(Player player)
	{
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
