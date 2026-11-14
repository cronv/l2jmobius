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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class provides a base for effects that modify a {@code Stat} by a percentage.<br>
 * It allows for dynamic scaling of statistics based on the target's current values.
 * @author Mobius
 */
public class AbstractStatPercentEffect extends AbstractEffect
{
	private final Stat _stat;
	protected final double _amount;
	
	/**
	 * Creates a new effect that modifies a specific stat by a percentage.<br>
	 * This constructor initializes the required values from the provided parameters.<br>
	 * It ensures that the modifier mode is set to {@code StatModifierType.PER}.
	 * @param params The {@code StatSet} containing the configuration data.
	 * @param stat The specific {@code Stat} type to be modified.
	 */
	public AbstractStatPercentEffect(StatSet params, Stat stat)
	{
		_stat = stat;
		_amount = params.getDouble("amount", 1);
		
		if (params.getEnum("mode", StatModifierType.class, StatModifierType.PER) != StatModifierType.PER)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " can only use PER mode.");
		}
		
		if (params.contains("power"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use amount instead of power.");
		}
	}
	
	/**
	 * Applies a percentage increase to a specific stat of a {@code Creature}.<br>
	 * This method modifies the target's stats based on the provided {@code Skill}.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().mergeMul(_stat, (_amount / 100) + 1);
	}
}
