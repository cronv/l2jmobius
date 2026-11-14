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

import org.l2jmobius.gameserver.GameServer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;

/**
 * Represents a zone where players are prevented from restarting their characters.<br>
 * This {@code NoRestartZone} type ensures that specific areas remain restricted for character resets.
 * @author GKR, Mobius
 */
public class NoRestartZone extends ZoneType
{
	private int _restartAllowedTime = 0;
	private int _restartTime = 0;
	private boolean _escapeLocked = false;
	
	/**
	 * Creates a new {@code NoRestartZone} instance.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public NoRestartZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method updates internal values like {@code restartAllowedTime} or {@code restartTime}.<br>
	 * If the name is not recognized, it calls the parent {@code setParameter} method.
	 * @param name The name of the parameter to update.
	 * @param value The string value associated with the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equalsIgnoreCase("restartAllowedTime"))
		{
			_restartAllowedTime = Integer.parseInt(value) * 1000;
		}
		else if (name.equalsIgnoreCase("restartTime"))
		{
			_restartTime = Integer.parseInt(value) * 1000;
		}
		else if (name.equalsIgnoreCase("instanceId"))
		{
			// Do nothing.
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If it is a player, it sets their inside zone status to {@code true} for {@code ZoneId.NO_RESTART}.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.NO_RESTART, true);
			if (_escapeLocked)
			{
				creature.setInsideZone(ZoneId.NO_ESCAPE, true);
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * If it is a player, it updates their status regarding the {@code ZoneId.NO_RESTART} zone.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.NO_RESTART, false);
			if (_escapeLocked)
			{
				creature.setInsideZone(ZoneId.NO_ESCAPE, false);
			}
		}
	}
	
	/**
	 * Handles logic when a {@code Player} logs into this zone.<br>
	 * It checks if the player should be teleported to a town based on time limits.<br>
	 * This only runs if the zone is currently enabled.
	 * @param player The {@code Player} who just logged in.
	 */
	@Override
	public void onPlayerLoginInside(Player player)
	{
		if (!isEnabled())
		{
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		if (((currentTime - player.getLastAccess()) > _restartTime) && ((currentTime - GameServer.getStartTime()) > _restartAllowedTime))
		{
			player.teleToLocation(TeleportWhereType.TOWN);
		}
	}
	
	/**
	 * Retrieves the amount of time allowed for a restart.<br>
	 * This value is used to determine if a player can restart within this zone.
	 * @return The allowed restart time as an {@code int}.
	 */
	public int getRestartAllowedTime()
	{
		return _restartAllowedTime;
	}
	
	/**
	 * Sets the amount of time allowed for a restart.<br>
	 * This value is stored in the {@code _restartAllowedTime} field.
	 * @param time The time duration to allow for restarts.
	 */
	public void setRestartAllowedTime(int time)
	{
		_restartAllowedTime = time;
	}
	
	/**
	 * Retrieves the current restart time for this zone.<br>
	 * This value is used to determine when a restart can occur.
	 * @return The current restart time as an {@code int}.
	 */
	public int getRestartTime()
	{
		return _restartTime;
	}
	
	/**
	 * Sets the restart time for this zone.<br>
	 * This value determines when a restart is permitted.
	 * @param time The new restart time to apply.
	 */
	public void setRestartTime(int time)
	{
		_restartTime = time;
	}
	
	/**
	 * Blocks or allows escaping for the players inside, such as during a boss fight.
	 * @param locked {@code true} to block escaping.
	 */
	public void setEscapeLocked(boolean locked)
	{
		if (_escapeLocked == locked)
		{
			return;
		}
		
		_escapeLocked = locked;
		for (Player player : getPlayersInside())
		{
			player.setInsideZone(ZoneId.NO_ESCAPE, locked);
		}
	}
}
