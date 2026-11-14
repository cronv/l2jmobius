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
package org.l2jmobius.gameserver.entity.actor.transform;

import java.util.HashMap;
import java.util.Map;

import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class stores and manages the level-specific data for a {@code Transform}.<br>
 * It provides a way to handle unique attributes or statistics based on the current level.<br>
 * Use this class to define how transformations scale or change as they progress.
 * @author UnAfraid
 */
public class TransformLevelData
{
	private final int _level;
	private final double _levelMod;
	private Map<Integer, Double> _stats;
	
	/**
	 * Creates a new {@code TransformLevelData} object from a {@code StatSet}.<br>
	 * This constructor extracts level and modifier values.<br>
	 * It also populates the base stats for the transformation.
	 * @param set The {@code StatSet} containing the required data.
	 */
	public TransformLevelData(StatSet set)
	{
		_level = set.getInt("val");
		_levelMod = set.getDouble("levelMod");
		addStats(Stat.MAX_HP, set.getDouble("hp"));
		addStats(Stat.MAX_MP, set.getDouble("mp"));
		addStats(Stat.MAX_CP, set.getDouble("cp"));
		addStats(Stat.REGENERATE_HP_RATE, set.getDouble("hpRegen"));
		addStats(Stat.REGENERATE_MP_RATE, set.getDouble("mpRegen"));
		addStats(Stat.REGENERATE_CP_RATE, set.getDouble("cpRegen"));
	}
	
	/**
	 * Adds a specific value to the internal stats map.<br>
	 * This method ensures the {@code _stats} map is initialized before adding data.<br>
	 * It uses the {@code ordinal()} of the {@code Stat} as the key.
	 * @param stat The {@code Stat} type to update.
	 * @param value The numeric value to associate with the stat.
	 */
	private void addStats(Stat stat, double value)
	{
		if (_stats == null)
		{
			_stats = new HashMap<>();
		}
		
		_stats.put(stat.ordinal(), value);
	}
	
	/**
	 * Retrieves the value of a specific statistic for this transform level.<br>
	 * It returns a default value if the statistic is not found or if the stats map is null.
	 * @param stat The {@code Stat} type to look up.
	 * @param defaultValue The value to return if the requested {@code Stat} does not exist.
	 * @return The double value of the specified {@code Stat}.
	 */
	public double getStats(Stat stat, double defaultValue)
	{
		return _stats == null ? defaultValue : _stats.getOrDefault(stat.ordinal(), defaultValue);
	}
	
	/**
	 * Retrieves the current level of the transform.<br>
	 * This value is stored in the {@code _level} field.
	 * @return The integer value representing the level.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the level modifier for this transform.<br>
	 * This value is used to calculate scaling effects based on the current level.
	 * @return The {@code double} value of the level modifier.
	 */
	public double getLevelMod()
	{
		return _levelMod;
	}
}
