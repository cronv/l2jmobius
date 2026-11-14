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
package handlers.skill.effects;

import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the addition of {@code Stat} power to a character.<br>
 * It is used to modify specific attributes when a skill effect is triggered.
 * @author Mobius
 */
public class SkillPowerAdd extends AbstractStatAddEffect
{
	/**
	 * Creates a new {@code SkillPowerAdd} effect.<br>
	 * This constructor initializes the effect using the provided statistics.<br>
	 * It applies changes to the {@code Stat.SKILL_POWER_ADD} stat.
	 * @param params The set of statistics to be applied.
	 */
	public SkillPowerAdd(StatSet params)
	{
		super(params, Stat.SKILL_POWER_ADD);
	}
}
