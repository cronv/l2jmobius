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

import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;

/**
 * Rewards granted to a player upon completing a quest.
 * @author Magik
 */
public class NewQuestReward
{
	private final long _exp;
	private final long _sp;
	private final int _level;
	private final List<ItemHolder> _items;
	
	/**
	 * Creates a reward bundle of experience, skill points, level gain and items.
	 * @param exp The experience to grant.
	 * @param sp The skill points to grant.
	 * @param level The level increase to apply.
	 * @param items The {@code ItemHolder} items to give.
	 */
	public NewQuestReward(long exp, long sp, int level, List<ItemHolder> items)
	{
		_exp = exp;
		_sp = sp;
		_level = level;
		_items = items;
	}
	
	/**
	 * Retrieves the experience awarded.
	 * @return The experience as a {@code long}.
	 */
	public long getExp()
	{
		return _exp;
	}
	
	/**
	 * Retrieves the skill points awarded.
	 * @return The skill points as a {@code long}.
	 */
	public long getSp()
	{
		return _sp;
	}
	
	/**
	 * Retrieves the level gain granted.
	 * @return The level as an {@code int}.
	 */
	public int getLevel()
	{
		return _level;
	}
	
	/**
	 * Retrieves the items granted.
	 * @return A {@code List} of reward {@code ItemHolder} objects.
	 */
	public List<ItemHolder> getItems()
	{
		return _items;
	}
}
