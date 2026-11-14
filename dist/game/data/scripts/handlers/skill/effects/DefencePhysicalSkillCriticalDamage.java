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
 * This class handles the critical damage reduction effect for physical skills.<br>
 * It serves as the physical equivalent to {@code DefenceMagicCriticalDamage}.
 * @author Mobius
 */
public class DefencePhysicalSkillCriticalDamage extends AbstractStatEffect
{
	/**
	 * Creates a new instance of {@code DefencePhysicalSkillCriticalDamage}.<br>
	 * This effect handles physical skill critical damage.<br>
	 * It uses the provided statistics to initialize the effect.
	 * @param params The {@code StatSet} containing the required values for this effect.
	 */
	public DefencePhysicalSkillCriticalDamage(StatSet params)
	{
		super(params, Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE, Stat.DEFENCE_PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD);
	}
}
