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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.DailyMissionStatus;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionDataHolder;
import org.l2jmobius.gameserver.entity.actor.holders.player.DailyMissionPlayerEntry;
import org.l2jmobius.gameserver.handler.AbstractDailyMissionHandler;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemSoulCrystalAdd;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * Handles the logic for daily missions related to the {@code Ensoul} mechanic.<br>
 * It listens for specific events to update a player's mission progress.
 * @author CostyKiller
 */
public class EnsoulDailyMissionHandler extends AbstractDailyMissionHandler
{
	private final int _amount;
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _crystalIds = new HashSet<>();
	
	/**
	 * Initializes the handler with data from a {@code DailyMissionDataHolder}.<br>
	 * This constructor sets up requirements like amount, level range, and crystal IDs.
	 * @param holder The holder containing the mission configuration data.
	 */
	public EnsoulDailyMissionHandler(DailyMissionDataHolder holder)
	{
		super(holder);
		_amount = holder.getRequiredCompletions();
		_minLevel = holder.getParams().getInt("minLevel", 85);
		_maxLevel = holder.getParams().getInt("maxLevel", Integer.MAX_VALUE);
		final String crystalIds = holder.getParams().getString("crystalIds", "");
		if (!crystalIds.isEmpty())
		{
			for (String s : crystalIds.split(","))
			{
				final int id = Integer.parseInt(s);
				if (!_crystalIds.contains(id))
				{
					_crystalIds.add(id);
				}
			}
		}
	}
	
	/**
	 * Initializes the handler by registering necessary event listeners.<br>
	 * It listens for {@code ON_ITEM_SOUL_CRYSTAL_ADD} events globally.<br>
	 * This allows the system to track when a player receives soul crystals.
	 */
	@Override
	public void init()
	{
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_ITEM_SOUL_CRYSTAL_ADD, (OnItemSoulCrystalAdd event) -> onItemSoulCrystalAdd(event), this));
	}
	
	/**
	 * Checks if the daily mission is currently available for a specific player.<br>
	 * It verifies the status of the {@code DailyMissionPlayerEntry}.<br>
	 * The method returns {@code true} if the mission is ready to be completed.
	 * @param player The {@code Player} object to check.
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
	 * Handles the logic when a player receives an ensoul stone.<br>
	 * It checks if the player level is within the required range.<br>
	 * It verifies if the received item ID is in the allowed list.<br>
	 * If all conditions are met, it updates the player's progress.
	 * @param event The {@code OnItemSoulCrystalAdd} event containing the player and stone data.
	 */
	private void onItemSoulCrystalAdd(OnItemSoulCrystalAdd event)
	{
		final Player player = event.getPlayer();
		final int level = player.getLevel();
		if ((level < _minLevel) || (level > _maxLevel))
		{
			return;
		}
		
		if (_crystalIds.contains(event.getEnsoulStone().getId()))
		{
			processPlayerProgress(player);
		}
	}
	
	/**
	 * Updates the progress for a player's daily mission.<br>
	 * It checks if the player has reached the required amount to unlock the mission.<br>
	 * The status is updated and saved if the goal is met.
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
}
