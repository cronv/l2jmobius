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
 * This class handles the calculation of physical skill damage bonuses during PvE raids.<br>
 * It applies a percentage-based modifier to the character's physical damage stats.
 * @author Mobius
 */
public class PveRaidPhysicalSkillDamageBonus extends AbstractStatPercentEffect
{
	/**
	 * Creates a new instance of {@code PveRaidPhysicalSkillDamageBonus}.<br>
	 * This constructor initializes the effect using a provided set of statistics.<br>
	 * It targets the {@code Stat.PVE_RAID_PHYSICAL_SKILL_DAMAGE} stat.
	 * @param params The {@code StatSet} containing the values for this bonus.
	 */
	public PveRaidPhysicalSkillDamageBonus(StatSet params)
	{
		super(params, Stat.PVE_RAID_PHYSICAL_SKILL_DAMAGE);
	}
}
