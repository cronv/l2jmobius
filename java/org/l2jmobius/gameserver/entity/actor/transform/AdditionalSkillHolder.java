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

import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class represents a holder for additional skills assigned to an actor.<br>
 * It extends {@code SkillHolder} to provide extra skill management functionality.
 * @author UnAfraid
 */
public class AdditionalSkillHolder extends SkillHolder
{
	private final int _minLevel;
	
	/**
	 * Creates a new {@code AdditionalSkillHolder} instance.<br>
	 * This constructor initializes the skill and its required level requirements.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The current level of the skill.
	 * @param minLevel The minimum level required to use this skill.
	 */
	public AdditionalSkillHolder(int skillId, int skillLevel, int minLevel)
	{
		super(skillId, skillLevel);
		_minLevel = minLevel;
	}
	
	/**
	 * Retrieves the minimum level required for this skill.<br>
	 * This value is stored in the {@code _minLevel} field.
	 * @return The minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
}
