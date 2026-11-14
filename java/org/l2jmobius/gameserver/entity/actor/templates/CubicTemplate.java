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
package org.l2jmobius.gameserver.entity.actor.templates;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.cubic.Cubic;
import org.l2jmobius.gameserver.entity.cubic.CubicSkill;
import org.l2jmobius.gameserver.entity.cubic.CubicTargetType;
import org.l2jmobius.gameserver.entity.cubic.ICubicConditionHolder;
import org.l2jmobius.gameserver.entity.cubic.conditions.ICubicCondition;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as the base template for {@code Cubic} entities.<br>
 * It defines the default properties and behaviors for cubic-based actors in the game world.
 * @author UnAfraid
 */
public class CubicTemplate extends CreatureTemplate implements ICubicConditionHolder
{
	private final int _id;
	private final int _level;
	private final int _slot;
	private final int _duration;
	private final int _delay;
	private final int _maxCount;
	private final int _useUp;
	private final double _power;
	private final CubicTargetType _targetType;
	private final List<ICubicCondition> _conditions = new ArrayList<>();
	private final List<CubicSkill> _skills = new ArrayList<>();
	
	/**
	 * Creates a new {@code CubicTemplate} using the provided data.<br>
	 * This constructor initializes all internal fields from a {@code StatSet}.<br>
	 * It sets values for properties like level, duration, and power.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	public CubicTemplate(StatSet set)
	{
		super(set);
		_id = set.getInt("id");
		_level = set.getInt("level");
		_slot = set.getInt("slot");
		_duration = set.getInt("duration");
		_delay = set.getInt("delay");
		_maxCount = set.getInt("maxCount");
		_useUp = set.getInt("useUp");
		_power = set.getDouble("power") / 10;
		_targetType = set.getEnum("targetType", CubicTargetType.class, CubicTargetType.TARGET);
	}
	
	/**
	 * Retrieves the unique identifier for this {@code CubicTemplate}.<br>
	 * This ID is used to distinguish between different templates.
	 * @return The unique integer ID of the template.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the level of this {@code CubicTemplate}.<br>
	 * This value represents the base level assigned to the template.
	 * @return The current level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the slot index of this {@code CubicTemplate}.<br>
	 * This value identifies where the template is located.
	 * @return The integer value of the slot.
	 */
	public int getSlot()
	{
		return _slot;
	}
	
	/**
	 * Retrieves the duration of the {@code CubicTemplate}.<br>
	 * This value represents how long the effect lasts.
	 * @return The duration as an {@code int}.
	 */
	public int getDuration()
	{
		return _duration;
	}
	
	/**
	 * Retrieves the delay value for this {@code CubicTemplate}.<br>
	 * This value represents the time interval between actions.
	 * @return The current delay as an {@code int}.
	 */
	public int getDelay()
	{
		return _delay;
	}
	
	/**
	 * Retrieves the maximum number of allowed instances.<br>
	 * This value is stored in the {@code _maxCount} field.
	 * @return The maximum count as an {@code int}.
	 */
	public int getMaxCount()
	{
		return _maxCount;
	}
	
	/**
	 * Retrieves the maximum number of uses for this {@code CubicTemplate}.<br>
	 * This value determines how many times the cubic can be activated.
	 * @return The total number of allowed uses.
	 */
	public int getUseUp()
	{
		return _useUp;
	}
	
	/**
	 * Retrieves the target type for this cubic template.<br>
	 * This determines what kind of entities can be affected by the cubic.
	 * @return the {@code CubicTargetType} associated with this template.
	 */
	public CubicTargetType getTargetType()
	{
		return _targetType;
	}
	
	/**
	 * Retrieves the list of skills associated with this {@code CubicTemplate}.<br>
	 * This method returns all {@code CubicSkill} objects defined for this template.
	 * @return a {@code List} containing all {@code CubicSkill} objects.
	 */
	public List<CubicSkill> getCubicSkills()
	{
		return _skills;
	}
	
	/**
	 * Retrieves the base physical attack power.<br>
	 * This value is cast from a {@code double} to an {@code int}.
	 * @return The base physical attack power as an {@code int}.
	 */
	@Override
	public int getBasePAtk()
	{
		return (int) _power;
	}
	
	/**
	 * Retrieves the base magic attack power.<br>
	 * This value is cast from a {@code double} to an {@code int}.
	 * @return The base magic attack power as an {@code int}.
	 */
	@Override
	public int getBaseMAtk()
	{
		return (int) _power;
	}
	
	/**
	 * Checks if all required conditions for a {@code Cubic} are met.<br>
	 * This method iterates through the list of conditions defined in the template.<br>
	 * It returns {@code false} if any single condition fails to be satisfied.
	 * @param cubic The {@code Cubic} object being validated.
	 * @param owner The {@code Creature} who owns the {@code Cubic}.
	 * @param target The {@code WorldObject} that is the target of the action.
	 * @return {@code true} if all conditions pass or if there are no conditions, and {@code false} otherwise.
	 */
	@Override
	public boolean validateConditions(Cubic cubic, Creature owner, WorldObject target)
	{
		if (_conditions.isEmpty())
		{
			return true;
		}
		
		for (ICubicCondition condition : _conditions)
		{
			if (!condition.test(cubic, owner, target))
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Adds a new condition to the template.<br>
	 * This method stores the provided {@code ICubicCondition} in the internal list.
	 * @param condition The {@code ICubicCondition} to be added.
	 */
	@Override
	public void addCondition(ICubicCondition condition)
	{
		_conditions.add(condition);
	}
	
	/**
	 * Returns a string representation of the {@code CubicTemplate}.<br>
	 * This includes all primary attributes and lists.
	 * @return A formatted string containing the template data.
	 */
	@Override
	public String toString()
	{
		return "Cubic id: " + _id + " level: " + _level + " slot: " + _slot + " duration: " + _duration + " delay: " + _delay + " maxCount: " + _maxCount + " useUp: " + _useUp + " power: " + _power + System.lineSeparator() + "skills: " + _skills + System.lineSeparator() + "conditions:" + _conditions + System.lineSeparator();
	}
}
