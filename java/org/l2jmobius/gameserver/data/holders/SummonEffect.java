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

import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * This class holds a cached {@code Skill} instance for a servitor.<br>
 * It also stores the elapsed effect time captured at the moment of caching.
 */
public class SummonEffect
{
	private final Skill _skill;
	private final int _effectCurrentTime;
	
	/**
	 * Creates a new {@code SummonEffect} instance.<br>
	 * This constructor initializes the cached skill and its current elapsed time.
	 * @param skill The {@code Skill} associated with this effect.
	 * @param effectCurrentTime The current time of the effect in milliseconds.
	 */
	public SummonEffect(Skill skill, int effectCurrentTime)
	{
		_skill = skill;
		_effectCurrentTime = effectCurrentTime;
	}
	
	/**
	 * Retrieves the {@code Skill} associated with this summon effect.
	 * @return the {@code Skill} object.
	 */
	public Skill getSkill()
	{
		return _skill;
	}
	
	/**
	 * Retrieves the current elapsed time of the effect.<br>
	 * This value was captured when the {@code SummonEffect} object was created.
	 * @return The current effect time as an {@code int}.
	 */
	public int getEffectCurrentTime()
	{
		return _effectCurrentTime;
	}
}
