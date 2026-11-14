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
package org.l2jmobius.gameserver.entity.item.appearance;

/**
 * Defines the different types of visual appearances for items.<br>
 * This enum helps determine how an item is rendered in the game world.<br>
 * It categorizes items into specific visual groups like {@code WEAPON} or {@code ARMOR}.
 * @author UnAfraid
 */
public enum AppearanceType
{
	NONE,
	NORMAL,
	BLESSED,
	FIXED,
	RESTORE
}
