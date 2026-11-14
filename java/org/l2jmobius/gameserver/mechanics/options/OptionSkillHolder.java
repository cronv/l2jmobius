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
package org.l2jmobius.gameserver.mechanics.options;

import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class acts as a container for {@code Skill} objects associated with specific options.<br>
 * It allows the system to map and manage skills that are triggered or modified by game mechanics.
 * @author UnAfraid, Mobius
 */
public class OptionSkillHolder
{
	private final Skill _skill;
	private final double _chance;
	private final OptionSkillType _type;
	
	/**
	 * Creates a new instance of {@code OptionSkillHolder}.<br>
	 * This constructor initializes the skill, its trigger chance, and its type.
	 * @param skill The {@code Skill} to be associated with this holder.
	 * @param chance The probability value for the skill to trigger.
	 * @param type The {@code OptionSkillType} category for this skill.
	 */
	public OptionSkillHolder(Skill skill, double chance, OptionSkillType type)
	{
		_skill = skill;
		_chance = chance;
		_type = type;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this holder.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Retrieves the probability of this option skill occurring.<br>
	 * The value is returned as a {@code double}.
	 * @return The chance value.
	 */
	public double getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the skill type associated with this holder.<br>
	 * This method returns the {@code OptionSkillType} value stored in the object.
	 * @return The {@code OptionSkillType} of the skill.
	 */
	public OptionSkillType getSkillType()
	{
		return _type;
	}
}
