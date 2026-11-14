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
 * This class handles the effect of increasing a character's maximum physical critical rate.<br>
 * It modifies the {@code Stat} value associated with physical critical chance.
 * @author fruit
 */
public class AddMaxPhysicalCriticalRate extends AbstractStatEffect
{
	/**
	 * Creates a new instance of {@code AddMaxPhysicalCriticalRate}.<br>
	 * This effect adds to the maximum physical critical rate.<br>
	 * It uses the provided parameters to set the initial values.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public AddMaxPhysicalCriticalRate(StatSet params)
	{
		super(params, Stat.ADD_MAX_PHYSICAL_CRITICAL_RATE);
	}
}
