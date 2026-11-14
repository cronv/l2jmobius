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
package org.l2jmobius.gameserver.entity.residences;

import java.time.Duration;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a template for defining various functions and behaviors associated with residences.<br>
 * It allows the system to store and manage specific logic or properties that can be applied to different residence types.
 * @author UnAfraid
 */
public class ResidenceFunctionTemplate
{
	private final int _id;
	private final int _level;
	private final ResidenceFunctionType _type;
	private final ItemHolder _cost;
	private final Duration _duration;
	private final double _value;
	
	/**
	 * Creates a new {@code ResidenceFunctionTemplate} from a {@code StatSet}.<br>
	 * This constructor populates all fields using the data stored in the provided set.
	 * @param set The {@code StatSet} containing the template configuration.
	 */
	public ResidenceFunctionTemplate(StatSet set)
	{
		_id = set.getInt("id");
		_level = set.getInt("level");
		_type = set.getEnum("type", ResidenceFunctionType.class, ResidenceFunctionType.NONE);
		_cost = new ItemHolder(set.getInt("costId"), set.getLong("costCount"));
		_duration = set.getDuration("duration");
		_value = set.getDouble("value", 0);
	}
	
	/**
	 * Retrieves the unique identifier for this residence function.<br>
	 * This value is used to distinguish between different templates.
	 * @return The {@code int} ID of the template.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Returns the current level of this residence function.<br>
	 * This value is used to determine the progression status.
	 * @return The integer level of the template.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the function type of this residence template.<br>
	 * This identifies what specific action or effect the residence provides.
	 * @return The {@code ResidenceFunctionType} associated with this template.
	 */
	public ResidenceFunctionType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the cost associated with this residence function.<br>
	 * The cost is returned as an {@code ItemHolder}.
	 * @return the {@code ItemHolder} representing the cost.
	 */
	public ItemHolder getCost()
	{
		return _cost;
	}
	
	/**
	 * Returns the total time for this residence function.<br>
	 * The value is returned as a {@code Duration} object.
	 * @return the duration of the function.
	 */
	public Duration getDuration()
	{
		return _duration;
	}
	
	/**
	 * Returns the duration of this template in days.<br>
	 * This method converts the internal {@code Duration} into a long value.<br>
	 * The result represents the total number of full days.
	 * @return The duration expressed as a {@code long} count of days.
	 */
	public long getDurationAsDays()
	{
		return _duration.toDays();
	}
	
	/**
	 * Returns the numerical value of this residence function.<br>
	 * This value is used for internal calculations.
	 * @return The {@code double} value associated with this template.
	 */
	public double getValue()
	{
		return _value;
	}
}
