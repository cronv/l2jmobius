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

import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class represents a skill that is attached to an object or entity.<br>
 * It serves as a container for skills that are not directly owned by a player but are linked to another source.
 * @author Nik
 */
public class AttachSkillHolder extends SkillHolder
{
	private final int _requiredSkillId;
	private final int _requiredSkillLevel;
	
	/**
	 * Creates a new {@code AttachSkillHolder} instance.<br>
	 * This constructor initializes the skill and its requirements.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill being attached.
	 * @param requiredSkillId The unique identifier for the prerequisite skill.
	 * @param requiredSkillLevel The minimum level needed for the prerequisite skill.
	 */
	public AttachSkillHolder(int skillId, int skillLevel, int requiredSkillId, int requiredSkillLevel)
	{
		super(skillId, skillLevel);
		_requiredSkillId = requiredSkillId;
		_requiredSkillLevel = requiredSkillLevel;
	}
	
	/**
	 * Retrieves the ID of the skill required to use this attachment.<br>
	 * This value is stored during the initialization of the {@code AttachSkillHolder}.
	 * @return The unique identifier for the required skill.
	 */
	public int getRequiredSkillId()
	{
		return _requiredSkillId;
	}
	
	/**
	 * Returns the minimum level required for a skill.<br>
	 * This value is used to check if a player can use an attachment.
	 * @return The {@code int} representing the required skill level.
	 */
	public int getRequiredSkillLevel()
	{
		return _requiredSkillLevel;
	}
	
	/**
	 * Creates a new {@code AttachSkillHolder} from a {@code StatSet}.<br>
	 * It extracts the necessary skill IDs and levels from the provided data.
	 * @param set The {@code StatSet} containing the required skill information.
	 * @return A new instance of {@code AttachSkillHolder}.
	 */
	public static AttachSkillHolder fromStatSet(StatSet set)
	{
		return new AttachSkillHolder(set.getInt("skillId"), set.getInt("skillLevel", 1), set.getInt("requiredSkillId"), set.getInt("requiredSkillLevel", 1));
	}
}
