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

import java.util.ArrayList;
import java.util.List;

/**
 * This class serves as a data container for NPC buffer information.<br>
 * It stores specific attributes used to manage NPC behaviors and states within the game world.
 * @author UnAfraid
 */
class NpcBufferData
{
	private final int _id;
	private final List<NpcBufferSkillData> _skills = new ArrayList<>();
	
	/**
	 * Creates a new instance of {@code NpcBufferData}.<br>
	 * This constructor initializes the object with a specific identifier.
	 * @param id The unique identification number for this buffer data.
	 */
	NpcBufferData(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code NpcBufferData}.<br>
	 * This value is assigned when the object is created.
	 * @return The integer ID of the buffer.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Adds a new skill to the list of skills.<br>
	 * This method stores the provided {@code NpcBufferSkillData} object.
	 * @param skill The {@code NpcBufferSkillData} to be added.
	 */
	void addSkill(NpcBufferSkillData skill)
	{
		_skills.add(skill);
	}
	
	/**
	 * Retrieves the list of skills associated with this NPC buffer.<br>
	 * This method returns all {@code NpcBufferSkillData} objects stored in the internal list.
	 * @return A {@code List} containing all {@code NpcBufferSkillData} entries.
	 */
	public List<NpcBufferSkillData> getSkills()
	{
		return _skills;
	}
}
