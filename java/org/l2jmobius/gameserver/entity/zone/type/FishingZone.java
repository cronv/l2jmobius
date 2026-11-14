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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.mechanics.fishing.Fishing;
import org.l2jmobius.gameserver.network.serverpackets.fishing.ExAutoFishAvailable;

/**
 * Represents a specific area designated for fishing activities.<br>
 * This class defines the properties and behaviors associated with {@code FishingZone} types.
 * @author durgus, Mobius
 */
public class FishingZone extends ZoneType
{
	/**
	 * Creates a new instance of a {@code FishingZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this fishing zone.
	 */
	public FishingZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the fishing zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * It handles the logic for enabling auto-fishing packets for valid locations.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (!creature.isPlayer())
		{
			return;
		}
		
		if ((GeneralConfig.ALLOW_FISHING || creature.isGM()) && !creature.isInsideZone(ZoneId.FISHING))
		{
			final Player player = creature.asPlayer();
			
			ThreadPool.execute(new Runnable()
			{
				@Override
				public void run()
				{
					if (player == null)
					{
						return;
					}
					
					final Fishing fishing = player.getFishing();
					if (player.isInsideZone(ZoneId.FISHING))
					{
						if (fishing.canFish() && !fishing.isFishing())
						{
							if (fishing.isAtValidLocation())
							{
								player.sendPacket(ExAutoFishAvailable.YES);
							}
							else
							{
								player.sendPacket(ExAutoFishAvailable.NO);
							}
						}
						
						ThreadPool.schedule(this, 1500);
					}
					else
					{
						player.sendPacket(ExAutoFishAvailable.NO);
					}
				}
			});
		}
		
		creature.setInsideZone(ZoneId.FISHING, true);
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the fishing zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If it is a player, it updates their zone status and sends a packet.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (!creature.isPlayer())
		{
			return;
		}
		
		creature.setInsideZone(ZoneId.FISHING, false);
		creature.sendPacket(ExAutoFishAvailable.NO);
	}
	
	/*
	 * getWaterZ() this added function returns the Z value for the water surface. In effect this simply returns the upper Z value of the zone. This required some modification of ZoneForm, and zone form extensions.
	 */
	/**
	 * Retrieves the vertical coordinate of the water.<br>
	 * This value is taken from the high {@code Z} coordinate of the zone.
	 * @return The water {@code Z} coordinate as an {@code int}.
	 */
	public int getWaterZ()
	{
		return getZone().getHighZ();
	}
}
