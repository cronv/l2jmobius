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
package org.l2jmobius.gameserver.mechanics.ensoul;

import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Represents a specific property or effect associated with an {@code Ensoul} mechanic.<br>
 * This class extends {@code SkillHolder} to manage data related to individual soul options.
 * @author UnAfraid
 */
public class EnsoulOption extends SkillHolder
{
	private final int _id;
	private final String _name;
	private final String _desc;
	
	/**
	 * Creates a new {@code EnsoulOption} instance.<br>
	 * This constructor initializes the basic properties of an ensoul option.
	 * @param id The unique identifier for this option.
	 * @param name The display name of the option.
	 * @param desc The description text for the option.
	 * @param skillId The ID of the associated skill.
	 * @param skillLevel The level of the associated skill.
	 */
	public EnsoulOption(int id, String name, String desc, int skillId, int skillLevel)
	{
		super(skillId, skillLevel);
		_id = id;
		_name = name;
		_desc = desc;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code EnsoulOption}.<br>
	 * This value is used to distinguish between different options.
	 * @return The integer {@code id} of the option.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the name of this {@code EnsoulOption}.<br>
	 * This is the display name used for the option.
	 * @return The name as a {@code String}.
	 */
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Retrieves the description of this {@code EnsoulOption}.<br>
	 * This string provides details about the specific option.
	 * @return The description as a {@code String}.
	 */
	public String getDesc()
	{
		return _desc;
	}
	
	/**
	 * Returns a string representation of this {@code EnsoulOption}.<br>
	 * It includes the ID, name, and description.
	 * @return A formatted string containing the option details.
	 */
	@Override
	public String toString()
	{
		return "Ensoul Id: " + _id + " Name: " + _name + " Desc: " + _desc;
	}
}
