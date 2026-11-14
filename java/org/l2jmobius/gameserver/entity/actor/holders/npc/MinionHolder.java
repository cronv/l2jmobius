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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.time.Duration;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class holds the necessary information required for spawning minions.<br>
 * It serves as a data container for minion-related properties.
 * @author Zealar
 */
public class MinionHolder
{
	private final int _id;
	private final int _count;
	private final int _max;
	private final long _respawnTime;
	private final int _weightPoint;
	
	/**
	 * Creates a new {@code MinionHolder} instance using data from a {@code StatSet}.<br>
	 * This constructor initializes all internal fields like ID, count, and respawn time.
	 * @param set The {@code StatSet} containing the configuration values.
	 */
	public MinionHolder(StatSet set)
	{
		_id = set.getInt("id");
		_count = set.getInt("count", 1);
		_max = set.getInt("max", 0);
		_respawnTime = set.getDuration("respawnTime", Duration.ofSeconds(0)).getSeconds() * 1000;
		_weightPoint = set.getInt("weightPoint", 0);
	}
	
	/**
	 * Creates a new {@code MinionHolder} instance.<br>
	 * This constructor initializes the minion data with specific values.
	 * @param id The unique identifier for the minion.
	 * @param count The current number of minions available.
	 * @param max The maximum allowed number of minions.
	 * @param respawnTime The time in milliseconds before a minion can respawn.
	 * @param weightPoint The weight used for determining spawn priority.
	 */
	public MinionHolder(int id, int count, int max, long respawnTime, int weightPoint)
	{
		_id = id;
		_count = count;
		_max = max;
		_respawnTime = respawnTime;
		_weightPoint = weightPoint;
	}
	
	/**
	 * Returns the unique identifier for this minion.<br>
	 * This value is used to identify specific minion types.
	 * @return The {@code int} ID of the minion.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the current count of minions.<br>
	 * This method returns a random value between {@code _count} and {@code _max} if {@code _max} is greater than {@code _count}.<br>
	 * Otherwise, it returns the exact value of {@code _count}.
	 * @return The number of minions to spawn.
	 */
	public int getCount()
	{
		if (_max > _count)
		{
			return Rnd.get(_count, _max);
		}
		
		return _count;
	}
	
	/**
	 * Returns the time required for a minion to respawn.<br>
	 * This value is stored in milliseconds.
	 * @return The respawn time as a {@code long}.
	 */
	public long getRespawnTime()
	{
		return _respawnTime;
	}
	
	/**
	 * Retrieves the weight point for this minion.<br>
	 * This value determines the spawn priority of the entity.
	 * @return The current {@code int} weight point.
	 */
	public int getWeightPoint()
	{
		return _weightPoint;
	}
}
