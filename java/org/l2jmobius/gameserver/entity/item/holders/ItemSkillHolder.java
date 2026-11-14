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

import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class represents a skill that is bound to an item.<br>
 * It extends {@code SkillHolder} to provide specific functionality for items that grant skills.
 * @author UnAfraid, Mobius
 */
public class ItemSkillHolder extends SkillHolder
{
	private final ItemSkillType _type;
	private final int _chance;
	private final int _value;
	
	/**
	 * Creates a new {@code ItemSkillHolder} instance.<br>
	 * This constructor initializes the skill properties and item-specific values.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The primary level of the skill.
	 * @param skillSubLevel The secondary sub-level of the skill.
	 * @param type The {@code ItemSkillType} associated with this item skill.
	 * @param chance The probability chance for the skill to trigger.
	 * @param value The numerical value assigned to the skill effect.
	 */
	public ItemSkillHolder(int skillId, int skillLevel, int skillSubLevel, ItemSkillType type, int chance, int value)
	{
		super(skillId, skillLevel, skillSubLevel);
		_type = type;
		_chance = chance;
		_value = value;
	}
	
	/**
	 * Retrieves the skill type associated with this item.<br>
	 * This method returns the {@code ItemSkillType} assigned to the holder.
	 * @return the {@code ItemSkillType} of the item skill.
	 */
	public ItemSkillType getType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the activation chance for this item skill.<br>
	 * The value represents the probability of the skill triggering.
	 * @return the {@code int} chance value.
	 */
	public int getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the numerical value associated with this item skill.<br>
	 * This value is used to determine the strength of the skill effect.
	 * @return The {@code int} value of the skill.
	 */
	public int getValue()
	{
		return _value;
	}
}
