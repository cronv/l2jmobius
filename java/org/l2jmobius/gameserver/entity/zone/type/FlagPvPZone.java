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
package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

/**
 * Represents a specific type of zone where the {@code PvP} flag is automatically enabled.<br>
 * This ensures that players entering this area are permitted to engage in combat with one another.
 * @author Stayway
 */
public class FlagPvPZone extends ZoneType
{
	/**
	 * Creates a new {@code FlagPvPZone} instance.<br>
	 * This zone automatically enables the PvP flag for players.
	 * @param id The unique identifier for this zone.
	 */
	public FlagPvPZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If it is, the player's PvP flag is updated to {@code 1}.<br>
	 * The player also receives a notification message.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			player.updatePvPFlag(1);
			player.sendMessage("You entered a PvP Flag zone.");
			player.broadcastUserInfo();
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves this zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If the player has an active PvP flag, it resets the flag to {@code 0}.<br>
	 * The player receives a message and their information is broadcasted.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player.getPvpFlag() > 0)
			{
				player.updatePvPFlag(0);
			}
			player.sendMessage("You left the PvP Flag zone.");
			player.broadcastUserInfo();
		}
	}
	
	/**
	 * This method is called when a {@code Creature} dies within this zone.<br>
	 * It treats the death as an exit from the zone.<br>
	 * The {@code onExit} method is triggered for the creature.
	 * @param creature The {@code Creature} that died inside the zone.
	 */
	@Override
	public void onDieInside(Creature creature)
	{
		// When creature dies inside the zone, treat as if it had exited.
		onExit(creature);
	}
	
	/**
	 * This method is called when a {@code Creature} revives within this zone.<br>
	 * It triggers the logic for entering the zone to ensure proper state updates.
	 * @param creature The {@code Creature} that has just revived.
	 */
	@Override
	public void onReviveInside(Creature creature)
	{
		// When creature revives inside the zone, treat as if it had entered.
		onEnter(creature);
	}
}
