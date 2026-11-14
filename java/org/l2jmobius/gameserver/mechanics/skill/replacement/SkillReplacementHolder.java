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
package org.l2jmobius.gameserver.mechanics.skill.replacement;

/**
 * One directed replacement from a source skill to a target skill.<br>
 * A rule lives only while the effect that registered it is active.
 * @author fruit
 */
public class SkillReplacementHolder
{
	private final int _existingSkillId;
	private final int _replacementSkillId;
	private final int _replacementSkillLevel;
	private final int _priority;
	private final long _activationOrder;
	
	/**
	 * Creates a rule with its precedence and activation order.
	 * @param existingSkillId The source skill ID.
	 * @param replacementSkillId The target skill ID.
	 * @param replacementSkillLevel The fixed target level, or {@code -1} to inherit the resolved level.
	 * @param priority The precedence among rules that share the same source.
	 * @param activationOrder The registration order, breaking ties between equal priorities.
	 */
	public SkillReplacementHolder(int existingSkillId, int replacementSkillId, int replacementSkillLevel, int priority, long activationOrder)
	{
		_existingSkillId = existingSkillId;
		_replacementSkillId = replacementSkillId;
		_replacementSkillLevel = replacementSkillLevel;
		_priority = priority;
		_activationOrder = activationOrder;
	}
	
	/**
	 * @return The source skill ID.
	 */
	public int getExistingSkillId()
	{
		return _existingSkillId;
	}
	
	/**
	 * @return The target skill ID.
	 */
	public int getReplacementSkillId()
	{
		return _replacementSkillId;
	}
	
	/**
	 * @return The fixed target level, or {@code -1} when the resolved level is inherited.
	 */
	public int getReplacementSkillLevel()
	{
		return _replacementSkillLevel;
	}
	
	/**
	 * @return The precedence among rules that share the same source.
	 */
	public int getPriority()
	{
		return _priority;
	}
	
	/**
	 * @return The registration order of this rule.
	 */
	public long getActivationOrder()
	{
		return _activationOrder;
	}
}
