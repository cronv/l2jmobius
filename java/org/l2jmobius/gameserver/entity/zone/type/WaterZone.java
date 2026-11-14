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

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.serverpackets.FakePlayerInfo;
import org.l2jmobius.gameserver.network.serverpackets.NpcInfo;
import org.l2jmobius.gameserver.network.serverpackets.ServerObjectInfo;

/**
 * Represents a zone type specifically for water areas.<br>
 * It handles the unique behaviors and properties associated with aquatic environments.
 */
public class WaterZone extends ZoneType
{
	/**
	 * Creates a new instance of a {@code WaterZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this water zone.
	 */
	public WaterZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the water zone.<br>
	 * It updates the creature's status to indicate it is inside the water.<br>
	 * It handles specific logic for both players and NPCs based on their state.
	 * @param creature The {@code Creature} that entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.WATER, true);
		
		// TODO: update to only send speed status when that packet is known
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			final Transform transform = player.getTransformation();
			if ((transform != null) && !transform.canSwim())
			{
				creature.stopTransformation(true);
			}
			else
			{
				player.broadcastUserInfo();
			}
		}
		else if (creature.isNpc())
		{
			World.forEachVisibleObject(creature, Player.class, player ->
			{
				if (creature.isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo(creature.asNpc()));
				}
				else if (creature.getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo(creature.asNpc(), player));
				}
				else
				{
					player.sendPacket(new NpcInfo(creature.asNpc()));
				}
			});
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the water zone.<br>
	 * It updates the creature's status to indicate it is no longer in water.<br>
	 * It handles specific logic for both players and NPCs.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.WATER, false);
		
		// TODO: update to only send speed status when that packet is known
		if (creature.isPlayer())
		{
			// Mobius: Attempt to stop water task.
			final Player player = creature.asPlayer();
			if (!player.isInsideZone(ZoneId.WATER))
			{
				player.stopWaterTask();
			}
			
			if (!player.isTeleporting())
			{
				player.broadcastUserInfo();
			}
		}
		else if (creature.isNpc())
		{
			World.forEachVisibleObject(creature, Player.class, player ->
			{
				if (creature.isFakePlayer())
				{
					player.sendPacket(new FakePlayerInfo(creature.asNpc()));
				}
				else if (creature.getRunSpeed() == 0)
				{
					player.sendPacket(new ServerObjectInfo(creature.asNpc(), player));
				}
				else
				{
					player.sendPacket(new NpcInfo(creature.asNpc()));
				}
			});
		}
	}
	
	/**
	 * Retrieves the maximum height of the water zone.<br>
	 * This value is obtained from the {@code Zone} object.
	 * @return The high {@code Z} coordinate as an {@code int}.
	 */
	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
}
