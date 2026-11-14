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
package org.l2jmobius.gameserver.mechanics.script.newquestdata;

import java.util.List;

import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;

/**
 * Requirement that must be met to start or progress a quest.
 * @author Magik
 */
public class NewQuestCondition
{
	private final int _minLevel;
	private final int _maxLevel;
	private final List<Integer> _previousQuestIds;
	private final List<PlayerClass> _allowedClassIds;
	private final boolean _oneOfPreQuests;
	private final boolean _specificStart;
	
	/**
	 * Creates a start condition with level bounds, prerequisite quests, class restrictions and start flags.
	 * @param minLevel The minimum level required.
	 * @param maxLevel The maximum level allowed.
	 * @param previousQuestIds The ids of quests that must be completed first.
	 * @param allowedClassIds The {@code PlayerClass} types permitted.
	 * @param oneOfPreQuests {@code true} if completing one prerequisite suffices.
	 * @param specificStart {@code true} if a specific start is required.
	 */
	public NewQuestCondition(int minLevel, int maxLevel, List<Integer> previousQuestIds, List<PlayerClass> allowedClassIds, boolean oneOfPreQuests, boolean specificStart)
	{
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_previousQuestIds = previousQuestIds;
		_allowedClassIds = allowedClassIds;
		_oneOfPreQuests = oneOfPreQuests;
		_specificStart = specificStart;
	}
	
	/**
	 * Retrieves the minimum level required.
	 * @return The minimum level as an {@code int}.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Retrieves the maximum level allowed.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Retrieves the ids of quests that must be completed first.
	 * @return A {@code List} of previous quest ids.
	 */
	public List<Integer> getPreviousQuestIds()
	{
		return _previousQuestIds;
	}
	
	/**
	 * Retrieves the player classes permitted by this condition.
	 * @return A {@code List} of allowed {@code PlayerClass} values.
	 */
	public List<PlayerClass> getAllowedClassIds()
	{
		return _allowedClassIds;
	}
	
	/**
	 * Indicates whether completing a single prerequisite satisfies this condition.
	 * @return {@code true} if one prerequisite suffices, {@code false} if all are required.
	 */
	public boolean getOneOfPreQuests()
	{
		return _oneOfPreQuests;
	}
	
	/**
	 * Indicates whether this condition requires a specific start.
	 * @return {@code true} if a specific start is required, {@code false} otherwise.
	 */
	public boolean getSpecificStart()
	{
		return _specificStart;
	}
}
