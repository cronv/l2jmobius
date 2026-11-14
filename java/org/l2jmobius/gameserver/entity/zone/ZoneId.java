/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.entity.zone;

/**
 * This class defines the unique identifiers for different game zones.<br>
 * It is used to map specific areas within the world to their respective IDs.
 * @author Zoey76, Mobius
 */
public enum ZoneId
{
	PVP,
	PEACE,
	SIEGE,
	MOTHER_TREE,
	CLAN_HALL,
	CONQUEST,
	LANDING,
	NO_LANDING,
	WATER,
	JAIL,
	MONSTER_TRACK,
	CASTLE,
	SWAMP,
	NO_SUMMON_FRIEND,
	FORT,
	NO_STORE,
	NO_PVP,
	SCRIPT,
	HQ,
	DANGER_AREA,
	ALTERED,
	NO_BOOKMARK,
	NO_ITEM_DROP,
	NO_RESTART,
	SAYUNE,
	FISHING,
	UNDYING,
	TAX,
	TIMED_HUNTING,
	ONLY_PVE,
	NO_ESCAPE;
	
	/**
	 * Returns the total number of zones available.<br>
	 * This method counts all entries in the {@code ZoneId} enum.
	 * @return The total count of zone types as an {@code int}.
	 */
	public static int getZoneCount()
	{
		return values().length;
	}
}
