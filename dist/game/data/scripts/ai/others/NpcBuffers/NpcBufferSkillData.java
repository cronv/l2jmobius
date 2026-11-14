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
package ai.others.NpcBuffers;

import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class stores data related to skills used by NPCs for buffering purposes.<br>
 * It provides the necessary information for {@code SkillHolder} objects associated with NPCs.
 * @author UnAfraid
 */
class NpcBufferSkillData
{
	private final SkillHolder _skill;
	private final int _scaleToLevel;
	private final int _initialDelay;
	
	/**
	 * Creates a new {@code NpcBufferSkillData} instance.<br>
	 * This constructor initializes the skill data using values from a {@code StatSet}.<br>
	 * It extracts the skill ID, level, scaling factor, and initial delay.
	 * @param set The {@code StatSet} containing the required configuration values.
	 */
	NpcBufferSkillData(StatSet set)
	{
		_skill = new SkillHolder(set.getInt("id"), set.getInt("level"));
		_scaleToLevel = set.getInt("scaleToLevel", -1);
		_initialDelay = set.getInt("initialDelay", 0) * 1000;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this data.<br>
	 * This method fetches the skill from the internal {@code SkillHolder}.
	 * @return The {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill.getSkill();
	}
	
	/**
	 * Retrieves the level scaling value for this skill.<br>
	 * This value determines how the skill scales as the character levels up.
	 * @return The {@code int} scale value.
	 */
	public int getScaleToLevel()
	{
		return _scaleToLevel;
	}
	
	/**
	 * Retrieves the initial delay for the skill.<br>
	 * This value determines how long to wait before the first activation.
	 * @return The {@code int} value of the initial delay.
	 */
	public int getInitialDelay()
	{
		return _initialDelay;
	}
}
