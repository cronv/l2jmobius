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
package org.l2jmobius.gameserver.entity.actor.enums.player;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.CategoryData;

/**
 * Defines the different types of mounts available to players.<br>
 * This enum is used to categorize and identify various mount varieties within the game.
 * @author UnAfraid
 */
public enum MountType
{
	NONE,
	STRIDER,
	WYVERN,
	WOLF;
	
	/**
	 * Finds the {@code MountType} associated with a specific NPC ID.<br>
	 * This method checks which category the {@code npcId} belongs to.<br>
	 * It returns {@code NONE} if no matching mount is found.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return The corresponding {@code MountType} enum value.
	 */
	public static MountType findByNpcId(int npcId)
	{
		if (CategoryData.getInstance().isInCategory(CategoryType.STRIDER, npcId))
		{
			return STRIDER;
		}
		else if (CategoryData.getInstance().isInCategory(CategoryType.WYVERN_GROUP, npcId))
		{
			return WYVERN;
		}
		else if (CategoryData.getInstance().isInCategory(CategoryType.WOLF_GROUP, npcId))
		{
			return WOLF;
		}
		
		return NONE;
	}
}
