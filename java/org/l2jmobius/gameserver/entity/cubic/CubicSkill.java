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
package org.l2jmobius.gameserver.entity.cubic;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.cubic.conditions.ICubicCondition;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Represents a skill within the {@code cubic} system.<br>
 * This class handles the logic and conditions required for executing specific skills.
 * @author UnAfraid
 */
public class CubicSkill extends SkillHolder implements ICubicConditionHolder
{
	private final int _triggerRate;
	private final int _successRate;
	private final boolean _canUseOnStaticObjects;
	private final CubicTargetType _targetType;
	private final List<ICubicCondition> _conditions = new ArrayList<>();
	private final boolean _targetDebuff;
	
	/**
	 * Creates a new {@code CubicSkill} instance using the provided data.<br>
	 * This constructor initializes all skill properties from a {@code StatSet}.<br>
	 * It sets default values if specific keys are missing in the set.
	 * @param set The {@code StatSet} containing the skill configuration data.
	 */
	public CubicSkill(StatSet set)
	{
		super(set.getInt("id"), set.getInt("level"));
		_triggerRate = set.getInt("triggerRate", 100);
		_successRate = set.getInt("successRate", 100);
		_canUseOnStaticObjects = set.getBoolean("canUseOnStaticObjects", false);
		_targetType = set.getEnum("target", CubicTargetType.class, CubicTargetType.TARGET);
		_targetDebuff = set.getBoolean("targetDebuff", false);
	}
	
	/**
	 * Retrieves the trigger rate for this {@code CubicSkill}.<br>
	 * This value determines how often the skill is activated.
	 * @return The current trigger rate as an {@code int}.
	 */
	public int getTriggerRate()
	{
		return _triggerRate;
	}
	
	/**
	 * Retrieves the success rate of this {@code CubicSkill}.<br>
	 * This value determines how often the skill succeeds.
	 * @return The success rate as an {@code int}.
	 */
	public int getSuccessRate()
	{
		return _successRate;
	}
	
	/**
	 * Checks if this skill can be used on static objects.<br>
	 * Returns {@code true} if it is allowed.<br>
	 * Returns {@code false} otherwise.
	 * @return the status of whether the skill works on static objects.
	 */
	public boolean canUseOnStaticObjects()
	{
		return _canUseOnStaticObjects;
	}
	
	/**
	 * Retrieves the target type for this {@code CubicSkill}.<br>
	 * This determines what kind of entity can be targeted.
	 * @return the {@code CubicTargetType} associated with this skill.
	 */
	public CubicTargetType getTargetType()
	{
		return _targetType;
	}
	
	/**
	 * Checks if this skill is designed to target a debuff.<br>
	 * Returns {@code true} if the skill targets a debuff.<br>
	 * Returns {@code false} otherwise.
	 * @return The status of whether the skill targets a debuff.
	 */
	public boolean isTargetingDebuff()
	{
		return _targetDebuff;
	}
	
	/**
	 * Checks if all requirements for using a {@code Cubic} skill are met.<br>
	 * It verifies the target's status and evaluates every registered condition.
	 * @param cubic The {@code Cubic} object associated with the skill.
	 * @param owner The {@code Creature} who is attempting to use the skill.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @return {@code true} if all conditions pass, otherwise {@code false}.
	 */
	@Override
	public boolean validateConditions(Cubic cubic, Creature owner, WorldObject target)
	{
		if (_targetDebuff && target.isCreature() && (target.asCreature().getEffectList().getDebuffCount() == 0))
		{
			return false;
		}
		
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
	 * Adds a new condition to the skill.<br>
	 * This method stores the provided {@code ICubicCondition} in the internal list.
	 * @param condition The {@code ICubicCondition} to be added.
	 */
	@Override
	public void addCondition(ICubicCondition condition)
	{
		_conditions.add(condition);
	}
	
	/**
	 * Returns a string representation of the {@code CubicSkill}.<br>
	 * This includes details like ID, level, and rates.
	 * @return A formatted string containing all skill properties.
	 */
	@Override
	public String toString()
	{
		return "Cubic skill id: " + getSkillId() + " level: " + getSkillLevel() + " triggerRate: " + _triggerRate + " successRate: " + _successRate + " canUseOnStaticObjects: " + _canUseOnStaticObjects + " targetType: " + _targetType + " isTargetingDebuff: " + _targetDebuff + System.lineSeparator();
	}
}
