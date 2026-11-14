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
package org.l2jmobius.gameserver.mechanics.skill.enums;

/**
 * Defines the basic property types for skills in the game.<br>
 * This enum categorizes skills into {@code PHYSICAL}, {@code MAGIC}, or {@code NONE} categories.<br>
 * It replaces the older system that relied on specific base stats like {@code CON} and {@code MEN}.
 * @author Nik
 */
public enum BasicProperty
{
	NONE,
	PHYSICAL,
	MAGIC;
}
