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

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
 * Handles the logic for daily missions related to killing monsters.<br>
 * It tracks progress and updates {@code DailyMissionDataHolder} when a player kills an {@code Attackable} entity.
 * @author Mobius, Norvox
 */
public class MonsterDailyMissionHandler extends AbstractDailyMissionHandler
{
	private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
	
	private final int _requiredMissionCompleteId;
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _ids = new HashSet<>();
	private final String _startHour;
	private final String _endHour;
	
	/**
	 * Initializes the handler with specific mission data.<br>
	 * This constructor sets up requirements like amount, level range, and monster IDs.
	 * @param holder The {@code DailyMissionDataHolder} containing the configuration for this mission.
	 */
	public MonsterDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_requiredMissionCompleteId = holder.getRequiredMissionCompleteId();
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
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
		
		_startHour = holder.getParams().getString("startHour", "");
		_endHour = holder.getParams().getString("endHour", "");
	}
	
	/**
	 * Initializes the monster daily mission handler.<br>
	 * This method registers a listener for {@code ON_ATTACKABLE_KILL} events.<br>
	 * It allows the system to track when players kill monsters.
	 */
	@Override
	public void init()
	{
		Containers.Monsters().addListener(new ConsumerEventListener(this, EventType.ON_ATTACKABLE_KILL, (OnAttackableKill event) -> onAttackableKill(event), this));
	}
	
	/**
	 * Checks if the monster daily mission is available for a specific player.<br>
	 * It verifies if the player has met the required progress to unlock the mission.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if the mission is currently available, otherwise {@code false}.
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
	 * Handles the logic when a player kills an attackable monster.<br>
	 * It checks if the monster is part of the allowed IDs and level range.<br>
	 * Progress is awarded to the attacker or nearby party members.
	 * @param event The {@code OnAttackableKill} event containing the attacker and target.
	 */
	private void onAttackableKill(OnAttackableKill event)
	{
		final Attackable monster = event.getTarget();
		if (!_ids.isEmpty() && !_ids.contains(monster.getId()))
		{
			return;
		}
		
		final Player player = event.getAttacker();
		final int monsterLevel = monster.getLevel();
		if ((_minLevel > 0) && ((monsterLevel < _minLevel) || (monsterLevel > _maxLevel) || ((player.getLevel() - monsterLevel) > 5)))
		{
			return;
		}
		
		if ((((_requiredMissionCompleteId != 0) && checkRequiredMission(player)) || (_requiredMissionCompleteId == 0)) //
			&& (checkTimeInterval() || (_startHour.isEmpty() && _endHour.isEmpty())))
		{
			final Party party = player.getParty();
			if (party != null)
			{
				final CommandChannel channel = party.getCommandChannel();
				final List<Player> members = channel != null ? channel.getMembers() : party.getMembers();
				for (Player member : members)
				{
					if ((member.getLevel() >= (monsterLevel - 5)) && (member.calculateDistance3D(monster) <= PlayerConfig.ALT_PARTY_RANGE))
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
	
	/**
	 * Updates the progress for a player's daily mission.<br>
	 * It checks if the player has met the required amount to unlock the mission.<br>
	 * The status is updated to {@code DailyMissionStatus.AVAILABLE} if successful.
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
				storePlayerEntry(entry);
			}
			else
			{
				// A kill only moves a counter, so it can wait for the batch write.
				storePlayerEntryAsync(entry);
			}
		}
	}
	
	/**
	 * Checks if the current time falls within the allowed mission window.<br>
	 * It compares {@code LocalTime.now()} against the configured start and end hours.
	 * @return {@code true} if the current time is between the limits, otherwise {@code false}.
	 */
	private boolean checkTimeInterval()
	{
		if (!_startHour.isEmpty() && !_endHour.isEmpty())
		{
			final LocalTime now = LocalTime.now();
			final LocalTime start = LocalTime.parse(_startHour, TIME_FORMAT);
			final LocalTime end = LocalTime.parse(_endHour, TIME_FORMAT);
			// Check hour parameters.
			if (now.isAfter(start) && now.isBefore(end))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Verifies if the player has completed a specific required mission.<br>
	 * This method checks if the current mission entry matches the required ID.<br>
	 * It also ensures that the mission status is set to {@code COMPLETED}.
	 * @param player The {@code Player} object to check.
	 * @return {@code true} if all requirements are met, otherwise {@code false}.
	 */
	private boolean checkRequiredMission(Player player)
	{
		final DailyMissionPlayerEntry missionEntry = getPlayerEntry(player.getObjectId(), false);
		return (missionEntry != null) && (_requiredMissionCompleteId != 0) && (missionEntry.getRewardId() == _requiredMissionCompleteId) && (getStatus(player) == DailyMissionStatus.COMPLETED.getClientId());
	}
}
