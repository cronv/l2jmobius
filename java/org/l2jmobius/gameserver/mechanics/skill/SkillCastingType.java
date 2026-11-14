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
package org.l2jmobius.gameserver.mechanics.skill;

/**
 * Defines the different types of casting mechanics for skills.<br>
 * This enum is used to determine how a skill is executed by the game engine.
 * @author Nik
 */
public enum SkillCastingType
{
	SIMULTANEOUS(-1),
	NORMAL(0),
	NORMAL_SECOND(1),
	BLUE(2),
	GREEN(3),
	RED(4);
	
	private final int _clientBarId;
	
	/**
	 * Creates a new {@code SkillCastingType} instance.<br>
	 * This constructor sets the internal ID used by the client.
	 * @param clientBarId The unique identifier for the casting bar.
	 */
	SkillCastingType(int clientBarId)
	{
		_clientBarId = clientBarId;
	}
	
	/**
	 * Retrieves the unique identifier for the client bar.<br>
	 * This value is used to determine which UI element displays the skill progress.
	 * @return The {@code int} value of the client bar ID.
	 */
	public int getClientBarId()
	{
		return _clientBarId;
	}
}
