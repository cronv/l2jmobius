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
package org.l2jmobius.gameserver.mechanics.herobook;

import java.util.Set;

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class acts as a data container for the level of a {@code HeroBook}.<br>
 * It stores information related to the progression and requirements of specific hero books.
 * @author Index
 */
public class HeroBookLevelHolder
{
	private final int _level;
	private final int _exp;
	private final Set<ItemHolder> _items;
	private final Set<SkillHolder> _skills;
	private final long _commission;
	
	/**
	 * Creates a new {@code HeroBookLevelHolder} instance.<br>
	 * This constructor initializes the level, experience, items, skills, and commission.
	 * @param level The current level of the hero book.
	 * @param exp The current experience points of the hero book.
	 * @param items A set of {@code ItemHolder} objects associated with this level.
	 * @param skills A set of {@code SkillHolder} objects associated with this level.
	 * @param commission The commission value for this hero book level.
	 */
	public HeroBookLevelHolder(int level, int exp, Set<ItemHolder> items, Set<SkillHolder> skills, long commission)
	{
		_level = level;
		_exp = exp;
		_items = items;
		_skills = skills;
		_commission = commission;
	}
	
	/**
	 * Retrieves the current level of the hero book.<br>
	 * This value is stored in the {@code _level} field.
	 * @return The integer level of the hero book.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the current experience points. <br>
	 * This value represents the progress made toward the next level.
	 * @return The current amount of {@code exp}.
	 */
	public int getExp()
	{
		return _exp;
	}
	
	/**
	 * Retrieves the set of items associated with this level.
	 * @return a {@code Set} of {@code ItemHolder} objects.
	 */
	public Set<ItemHolder> getItems()
	{
		return _items;
	}
	
	/**
	 * Retrieves the set of skills associated with this hero book level.
	 * @return a {@code Set} containing all {@code SkillHolder} objects.
	 */
	public Set<SkillHolder> getSkills()
	{
		return _skills;
	}
	
	/**
	 * Retrieves the commission value for this hero book level.<br>
	 * This value is stored as a {@code long}.
	 * @return The current commission amount.
	 */
	public long getCommission()
	{
		return _commission;
	}
}
