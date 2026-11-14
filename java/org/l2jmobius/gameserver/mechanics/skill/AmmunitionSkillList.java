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
package org.l2jmobius.gameserver.mechanics.skill;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;

/**
 * This class manages the collection of skills that require ammunition.<br>
 * It provides a way to identify which {@code ItemSkillHolder} instances consume specific resources.
 * @author Mobius
 */
public class AmmunitionSkillList
{
	private static final Set<Integer> SKILLS = ConcurrentHashMap.newKeySet();
	
	/**
	 * Adds a list of skills to the global registry.<br>
	 * This method iterates through each {@code ItemSkillHolder} in the provided list.<br>
	 * It extracts the skill ID and stores it in the internal set.
	 * @param skills The list of {@code ItemSkillHolder} objects to process.
	 */
	public static void add(List<ItemSkillHolder> skills)
	{
		for (ItemSkillHolder skill : skills)
		{
			SKILLS.add(skill.getSkillId());
		}
	}
	
	/**
	 * Retrieves the set of all registered skill IDs.<br>
	 * This method provides access to the internal {@code SKILLS} collection.
	 * @return a {@code Set<Integer>} containing all current skill values.
	 */
	public static Set<Integer> values()
	{
		return SKILLS;
	}
}
