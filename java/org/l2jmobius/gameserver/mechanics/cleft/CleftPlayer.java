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
package org.l2jmobius.gameserver.mechanics.cleft;

import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class holds the battle record of a single Aerial Cleft participant.<br>
 * The client shows these counters on the scoreboard, so they are kept for as long as the player stays registered.
 * @author Altur
 */
public class CleftPlayer
{
	private final Player _player;
	private long _joinTime;
	private int _kills;
	private int _deaths;
	private int _compressors;
	private boolean _combatAerialTarget;
	
	/**
	 * Creates a new battle record.
	 * @param player The registered {@code Player}.
	 * @param joinTime The time the player joined the battle, used to check the reward requirement.
	 */
	public CleftPlayer(Player player, long joinTime)
	{
		_player = player;
		_joinTime = joinTime;
	}
	
	/**
	 * Gets the player this record belongs to.
	 * @return The {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Gets the object id of the player this record belongs to.
	 * @return The object id as an {@code int}.
	 */
	public int getObjectId()
	{
		return _player.getObjectId();
	}
	
	/**
	 * Gets the time the player joined the battle.
	 * @return The join time in milliseconds.
	 */
	public long getJoinTime()
	{
		return _joinTime;
	}
	
	/**
	 * Sets the time the player joined the battle.<br>
	 * The clock is restarted when the battle begins, so the reward requirement counts battle time and not waiting time.
	 * @param joinTime The join time in milliseconds.
	 */
	public void setJoinTime(long joinTime)
	{
		_joinTime = joinTime;
	}
	
	/**
	 * Gets the amount of enemies this player has killed.
	 * @return The kill count as an {@code int}.
	 */
	public int getKills()
	{
		return _kills;
	}
	
	/**
	 * Gets the amount of times this player has been killed.
	 * @return The death count as an {@code int}.
	 */
	public int getDeaths()
	{
		return _deaths;
	}
	
	/**
	 * Gets the amount of compressors this player has destroyed.
	 * @return The compressor count as an {@code int}.
	 */
	public int getCompressors()
	{
		return _compressors;
	}
	
	/**
	 * Increases the kill counter by one.
	 */
	public void increaseKills()
	{
		_kills++;
	}
	
	/**
	 * Increases the death counter by one.
	 */
	public void increaseDeaths()
	{
		_deaths++;
	}
	
	/**
	 * Increases the destroyed compressor counter by one.
	 */
	public void increaseCompressors()
	{
		_compressors++;
	}
	
	/**
	 * Verifies if this player is the Combat Aerial Target of the team.
	 * @return {@code true} if the player is the current target, {@code false} otherwise.
	 */
	public boolean isCombatAerialTarget()
	{
		return _combatAerialTarget;
	}
	
	/**
	 * Marks this player as the Combat Aerial Target of the team.
	 * @param value {@code true} to mark the player, {@code false} to clear the mark.
	 */
	public void setCombatAerialTarget(boolean value)
	{
		_combatAerialTarget = value;
	}
}
