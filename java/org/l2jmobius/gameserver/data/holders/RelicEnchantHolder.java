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
package org.l2jmobius.gameserver.data.holders;

/**
 * This class acts as a data holder for relic enchantment information.<br>
 * It stores the specific properties and values associated with an enchanted relic.
 * @author Brado
 */
public class RelicEnchantHolder
{
	private final int _enchantLevel;
	private final int _skillId;
	private final int _skillLevel;
	
	/**
	 * Creates a new {@code RelicEnchantHolder} instance.<br>
	 * This constructor initializes the enchantment and skill data.
	 * @param enchantLevel The level of the relic enchantment.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The current level of the skill.
	 */
	public RelicEnchantHolder(int enchantLevel, int skillId, int skillLevel)
	{
		_enchantLevel = enchantLevel;
		_skillId = skillId;
		_skillLevel = skillLevel;
	}
	
	/**
	 * Retrieves the unique identifier for the skill.<br>
	 * This value is used to identify which skill is associated with this enchantment.
	 * @return The {@code int} ID of the skill.
	 */
	public int getSkillId()
	{
		return _skillId;
	}
	
	/**
	 * Retrieves the current enchantment level.<br>
	 * This value is stored in the {@code RelicEnchantHolder}.
	 * @return The integer value of the enchantment level.
	 */
	public int getEnchantLevel()
	{
		return _enchantLevel;
	}
	
	/**
	 * Retrieves the current level of the skill.<br>
	 * This value is stored within the {@code RelicEnchantHolder}.
	 * @return The integer value representing the skill level.
	 */
	public int getSkillLevel()
	{
		return _skillLevel;
	}
}
