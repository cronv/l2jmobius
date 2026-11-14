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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneRespawn;

/**
 * Represents the territory of the Aerial Cleft.<br>
 * The zone carries the locations both teams are teleported to when they enter and leave the battlefield, which the Aerial Cleft manager reads on startup.
 * @author Altur
 */
public class CleftZone extends ZoneRespawn
{
	private final List<Location> _blueStartPoints = new ArrayList<>();
	private final List<Location> _redStartPoints = new ArrayList<>();
	private final List<Location> _blueBanishPoints = new ArrayList<>();
	private final List<Location> _redBanishPoints = new ArrayList<>();
	
	/**
	 * Creates a new instance of a {@code CleftZone}.
	 * @param id The unique identification number for this zone.
	 */
	public CleftZone(int id)
	{
		super(id);
	}
	
	/**
	 * Parses the locations of the battlefield.<br>
	 * The four types of the Aerial Cleft are handled here and everything else is left to {@code ZoneRespawn}.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param type The category of the location.
	 */
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if (type == null)
		{
			super.parseLoc(x, y, z, type);
			return;
		}
		
		switch (type)
		{
			case "blueStartPoint":
			{
				_blueStartPoints.add(new Location(x, y, z));
				break;
			}
			case "redStartPoint":
			{
				_redStartPoints.add(new Location(x, y, z));
				break;
			}
			case "blueBanishPoint":
			{
				_blueBanishPoints.add(new Location(x, y, z));
				break;
			}
			case "redBanishPoint":
			{
				_redBanishPoints.add(new Location(x, y, z));
				break;
			}
			default:
			{
				super.parseLoc(x, y, z, type);
				break;
			}
		}
	}
	
	/**
	 * Gets the locations a team is teleported to when the battle starts.
	 * @param teamId {@code 0} for the blue team, {@code 1} for the red team.
	 * @return A {@code List} of {@code Location} objects.
	 */
	public List<Location> getStartPoints(int teamId)
	{
		return teamId == 0 ? _blueStartPoints : _redStartPoints;
	}
	
	/**
	 * Gets the locations a team is teleported to when it leaves the battlefield.
	 * @param teamId {@code 0} for the blue team, {@code 1} for the red team.
	 * @return A {@code List} of {@code Location} objects.
	 */
	public List<Location> getBanishPoints(int teamId)
	{
		return teamId == 0 ? _blueBanishPoints : _redBanishPoints;
	}
	
	/**
	 * The battlefield itself is covered by the arena and the no restart zones, so entering the territory has no effect of its own.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
	}
	
	/**
	 * The battlefield itself is covered by the arena and the no restart zones, so leaving the territory has no effect of its own.
	 * @param creature The {@code Creature} leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
	}
}
