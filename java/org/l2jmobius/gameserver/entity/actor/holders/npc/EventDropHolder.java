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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.Set;

/**
 * This class manages drop data specifically for event-related NPCs.<br>
 * It extends {@code DropHolder} to provide specialized handling for event loot.
 * @author Mobius
 */
public class EventDropHolder extends DropHolder
{
	private final int _minLevel;
	private final int _maxLevel;
	private final Set<Integer> _monsterIds;
	
	/**
	 * Creates a new {@code EventDropHolder} for specific monsters.<br>
	 * This constructor sets the drop requirements and probabilities.<br>
	 * It defines which monsters can trigger this event drop.
	 * @param itemId The unique identifier of the item to be dropped.
	 * @param min The minimum amount of the item that can drop.
	 * @param max The maximum amount of the item that can drop.
	 * @param chance The probability of the item dropping as a double value.
	 * @param minLevel The minimum level required for the monster to drop the item.
	 * @param maxLevel The maximum level allowed for the monster to drop the item.
	 * @param monsterIds A {@code Set} of IDs representing monsters eligible for this drop.
	 */
	public EventDropHolder(int itemId, long min, long max, double chance, int minLevel, int maxLevel, Set<Integer> monsterIds)
	{
		super(null, itemId, min, max, chance);
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_monsterIds = monsterIds;
	}
	
	/**
	 * Returns the minimum level required for this event drop.<br>
	 * This value is retrieved from the {@code _minLevel} field.
	 * @return The minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Returns the maximum level allowed for this event drop.<br>
	 * This value is retrieved from the internal {@code _maxLevel} field.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the set of unique monster identifiers.<br>
	 * These IDs represent the monsters associated with this event drop.
	 * @return a {@code Set<Integer>} containing all monster IDs.
	 */
	public Set<Integer> getMonsterIds()
	{
		return _monsterIds;
	}
}
