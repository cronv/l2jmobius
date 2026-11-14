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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class applies a multiplier to a specific {@code Stat} based on the character's level.<br>
 * It allows for scaling statistics dynamically as the player progresses in the game.
 * @author Mobius
 */
public class StatMulForLevel extends AbstractEffect
{
	private final Stat _stat;
	private final Map<Integer, Double> _values;
	
	/**
	 * Creates a new {@code StatMulForLevel} effect.<br>
	 * This constructor initializes the stat and its level-based multipliers.<br>
	 * It requires the modifier mode to be set to {@code PER}.
	 * @param params The {@code StatSet} containing all configuration data.
	 */
	public StatMulForLevel(StatSet params)
	{
		_stat = params.getEnum("stat", Stat.class);
		
		final List<Integer> amount = params.getIntegerList("amount");
		_values = new HashMap<>(amount.size());
		int index = 0;
		for (Integer level : params.getIntegerList("level"))
		{
			_values.put(level, Double.valueOf(amount.get(index++)));
		}
		
		if (params.getEnum("mode", StatModifierType.class, StatModifierType.PER) != StatModifierType.PER)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " can only use PER mode.");
		}
	}
	
	/**
	 * Applies a multiplier to a specific stat based on the level of the target.<br>
	 * This method checks if a value exists for the current level of the {@code effected} creature.<br>
	 * If found, it updates the stat using the percentage provided in the effect values.
	 * @param effected The {@code Creature} receiving the stat modification.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		final Double amount = _values.get(effected.getLevel());
		if (amount != null)
		{
			effected.getStat().mergeMul(_stat, (amount / 100) + 1);
		}
	}
}
