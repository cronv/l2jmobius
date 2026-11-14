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
package org.l2jmobius.gameserver.mechanics.skill.holders;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This is a simple data holder used to store the {@code id} and {@code level} of a skill.<br>
 * It provides a convenient way to pass skill information between different mechanics.
 * @author BiggBoss
 */
public class SkillHolder
{
	private final int _skillId;
	private final int _skillLevel;
	private final int _skillSubLevel;
	private Skill _skill;
	
	/**
	 * Creates a new {@code SkillHolder} instance.<br>
	 * This constructor initializes the skill with a default sub-level of {@code 0}.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The current level of the skill.
	 */
	public SkillHolder(int skillId, int skillLevel)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
		_skillSubLevel = 0;
		_skill = null;
	}
	
	/**
	 * Creates a new {@code SkillHolder} instance.<br>
	 * This constructor initializes the basic skill properties.<br>
	 * The {@code _skill} field is set to {@code null}.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The primary level of the skill.
	 * @param skillSubLevel The secondary sub-level of the skill.
	 */
	public SkillHolder(int skillId, int skillLevel, int skillSubLevel)
	{
		_skillId = skillId;
		_skillLevel = skillLevel;
		_skillSubLevel = skillSubLevel;
		_skill = null;
	}
	
	/**
	 * Creates a new {@code SkillHolder} instance using an existing {@code Skill} object.<br>
	 * This constructor copies the ID, level, and sub-level from the provided {@code Skill}.
	 * @param skill The {@code Skill} object to use for initializing this holder.
	 */
	public SkillHolder(Skill skill)
	{
		_skillId = skill.getId();
		_skillLevel = skill.getLevel();
		_skillSubLevel = skill.getSubLevel();
		_skill = skill;
	}
	
	/**
	 * Retrieves the unique identifier for the skill.<br>
	 * This value corresponds to the {@code skillId} defined in the data.
	 * @return The integer ID of the skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Retrieves the current level of the skill.<br>
	 * This value is stored in the {@code _skillLevel} field.
	 * @return The integer level of the skill.
	 */
	public int getSkillLevel()
	{
		return _skillLevel;
	}
	
	/**
	 * Retrieves the sub-level of a skill.<br>
	 * This value represents the specific tier within a skill level.
	 * @return The current {@code int} sub-level of the skill.
	 */
	public int getSkillSubLevel()
	{
		return _skillSubLevel;
	}
	
	/**
	 * Retrieves the {@code Skill} object associated with this holder.<br>
	 * If the skill is not already loaded, it fetches it from {@code SkillData}.
	 * @return The {@code Skill} instance.
	 */
	public Skill getSkill()
	{
		if (_skill == null)
		{
			_skill = SkillData.getInstance().getSkill(_skillId, Math.max(_skillLevel, 1), _skillSubLevel);
		}
		
		return _skill;
	}
	
	/**
	 * Compares this {@code SkillHolder} instance with another object.<br>
	 * It checks if both objects have the same skill ID, level, and sub-level.
	 * @param obj The object to compare this instance against.
	 * @return {@code true} if the objects are equal, {@code false} otherwise.
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		
		if (!(obj instanceof SkillHolder))
		{
			return false;
		}
		
		final SkillHolder holder = (SkillHolder) obj;
		return (holder.getSkillId() == _skillId) && (holder.getSkillLevel() == _skillLevel) && (holder.getSkillSubLevel() == _skillSubLevel);
	}
	
	/**
	 * Returns a hash code value for this {@code SkillHolder} object.<br>
	 * This value is used to compare objects in collections like {@code HashSet}.<br>
	 * It is calculated based on the skill ID, level, and sub-level.
	 * @return The integer hash code of this object.
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = (prime * result) + _skillId;
		result = (prime * result) + _skillLevel;
		result = (prime * result) + _skillSubLevel;
		return result;
	}
	
	/**
	 * Returns a string representation of the {@code SkillHolder}.<br>
	 * This includes the skill ID and level.
	 * @return A formatted string containing the skill details.
	 */
	@Override
	public String toString()
	{
		return "[SkillId: " + _skillId + " Level: " + _skillLevel + "]";
	}
}
