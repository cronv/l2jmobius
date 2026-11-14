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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.MathUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the reuse logic for skills.<br>
 * This class determines if a skill can be cast again based on specific conditions.
 * @author Sdw, Mobius
 */
public class Reuse extends AbstractEffect
{
	private final int _magicType;
	private final double _amount;
	
	/**
	 * Creates a new {@code Reuse} effect instance.<br>
	 * This constructor initializes the magic type and amount from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Reuse(StatSet params)
	{
		_magicType = params.getInt("magicType", 0);
		_amount = params.getDouble("amount", 0);
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It modifies the reuse value of the {@code effected} creature.<br>
	 * The change is based on the {@code _magicType} and {@code _amount} fields.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the skill execution, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getStat().mergeReuseTypeValue(_magicType, (_amount / 100) + 1, MathUtil::mul);
	}
	
	/**
	 * This method is called when a {@code Reuse} effect ends.<br>
	 * It removes the effect from the target creature.<br>
	 * It handles special cases for 100% resistance values.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		// Handle 100% resistance case explicitly.
		final double amount = (_amount / 100) + 1;
		if (amount == 0d)
		{
			// To reverse 100% resistance, reset to the default.
			effected.getStat().mergeReuseTypeValue(_magicType, 1d, (existing, ignored) -> 1d);
			
			// Recalculate stats in case effected has other instances of this effect.
			ThreadPool.schedule(() ->
			{
				if (effected.isSpawned())
				{
					effected.getStat().recalculateStats(true);
				}
			}, 1000);
		}
		else // For other cases, proceed with normal division to remove the effect.
		{
			effected.getStat().mergeReuseTypeValue(_magicType, amount, MathUtil::div);
		}
	}
}
