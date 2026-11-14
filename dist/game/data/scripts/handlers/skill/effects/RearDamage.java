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

import java.util.EnumSet;
import java.util.Set;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.MathUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the calculation of additional damage dealt to targets from a given position.<br>
 * This effect applies a percentage modifier based on the target's position relative to the attacker.
 * @author Mobius
 */
public class RearDamage extends AbstractEffect
{
	private final double _amount;
	private final Set<Position> _positions = EnumSet.noneOf(Position.class);
	
	/**
	 * Creates a new {@code RearDamage} effect.<br>
	 * This constructor initializes the effect using the provided statistics.<br>
	 * It applies the values to the {@code Stat.REAR_DAMAGE_RATE} stat of each given {@code Position}.
	 * @param params The {@code StatSet} containing the damage modifiers.
	 */
	public RearDamage(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		for (String position : params.getString("position", "BACK").split(";"))
		{
			_positions.add(Position.valueOf(position.trim()));
		}
		
		if (params.getEnum("mode", StatModifierType.class, StatModifierType.PER) != StatModifierType.PER)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " can only use PER mode.");
		}
	}
	
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final CreatureStat stat = effected.getStat();
		for (Position position : _positions)
		{
			stat.mergePositionTypeValue(Stat.REAR_DAMAGE_RATE, position, (_amount / 100) + 1, MathUtil::mul);
		}
	}
	
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		// Handle the total negation case explicitly.
		final CreatureStat stat = effected.getStat();
		final double amount = (_amount / 100) + 1;
		if (amount == 0d)
		{
			// To reverse a total negation, reset to the default value.
			for (Position position : _positions)
			{
				stat.mergePositionTypeValue(Stat.REAR_DAMAGE_RATE, position, 1d, (existing, ignored) -> 1d);
			}
			
			// Recalculate stats in case effected has other instances of this effect.
			ThreadPool.schedule(() ->
			{
				if (effected.isSpawned())
				{
					stat.recalculateStats(true);
				}
			}, 1000);
		}
		else // For other cases, proceed with normal division to remove the effect.
		{
			for (Position position : _positions)
			{
				stat.mergePositionTypeValue(Stat.REAR_DAMAGE_RATE, position, amount, MathUtil::div);
			}
		}
	}
}
