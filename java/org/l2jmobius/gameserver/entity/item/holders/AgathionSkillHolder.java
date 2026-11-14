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
package org.l2jmobius.gameserver.entity.item.holders;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class manages the skills associated with an {@code Agathion} entity.<br>
 * It stores and provides access to a list of {@code Skill} objects that the pet can perform.
 * @author Mobius
 */
public class AgathionSkillHolder
{
	private final Map<Integer, List<Skill>> _mainSkill;
	private final Map<Integer, List<Skill>> _subSkill;
	
	/**
	 * Creates a new {@code AgathionSkillHolder} instance.<br>
	 * This constructor initializes the skills for an Agathion.
	 * @param mainSkill A map containing the primary skills associated with each enchant level.
	 * @param subSkill A map containing the secondary skills associated with each enchant level.
	 */
	public AgathionSkillHolder(Map<Integer, List<Skill>> mainSkill, Map<Integer, List<Skill>> subSkill)
	{
		_mainSkill = mainSkill;
		_subSkill = subSkill;
	}
	
	/**
	 * Retrieves the primary skills associated with this holder.<br>
	 * The map uses an {@code Integer} key to identify different skill categories.
	 * @return a {@code Map} containing lists of {@code Skill} objects.
	 */
	public Map<Integer, List<Skill>> getMainSkills()
	{
		return _mainSkill;
	}
	
	/**
	 * Retrieves the map of sub-skills associated with this holder.<br>
	 * The keys represent the skill IDs.
	 * @return a {@code Map<Integer, List<Skill>>} containing all sub-skills.
	 */
	public Map<Integer, List<Skill>> getSubSkills()
	{
		return _subSkill;
	}
	
	/**
	 * Retrieves the list of main skills for a specific enchantment level.<br>
	 * It returns an empty {@code List} if no skills are found for that level.
	 * @param enchantLevel The level of enchantment to check.
	 * @return A {@code List} of {@code Skill} objects associated with the given level.
	 */
	public List<Skill> getMainSkills(int enchantLevel)
	{
		if (!_mainSkill.containsKey(enchantLevel))
		{
			return Collections.emptyList();
		}
		
		return _mainSkill.get(enchantLevel);
	}
	
	/**
	 * Retrieves the list of sub-skills for a specific enchantment level.<br>
	 * It returns an empty {@code List} if no skills are found for that level.
	 * @param enchantLevel The level of the enchantment to check.
	 * @return A {@code List} of {@code Skill} objects associated with the given level.
	 */
	public List<Skill> getSubSkills(int enchantLevel)
	{
		if (!_subSkill.containsKey(enchantLevel))
		{
			return Collections.emptyList();
		}
		
		return _subSkill.get(enchantLevel);
	}
}
