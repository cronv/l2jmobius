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
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

/**
 * Represents a zone type where players are permitted to use the {@code Build Headquarters} action.<br>
 * This class defines specific rules for areas designated for headquarters construction.
 * @author Gnacik, Mobius
 */
public class HqZone extends ZoneType
{
	/**
	 * Creates a new instance of {@code HqZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public HqZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * The residence ids carried by the zone data are accepted and ignored, the siege is resolved from the player position instead.
	 * @param name The name of the parameter to set.
	 * @param value The value to assign to the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (!"castleId".equals(name) && !"fortId".equals(name) && !"clanHallId".equals(name) && !"territoryId".equals(name))
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters this zone.<br>
	 * It checks if the creature is a player.<br>
	 * If it is a player, it sets their inside zone status to {@code ZoneId.HQ}.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.HQ, true);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It checks if the creature is a player.<br>
	 * If it is a player, it updates their status to indicate they are no longer inside the {@code ZoneId.HQ}.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.HQ, false);
		}
	}
}
