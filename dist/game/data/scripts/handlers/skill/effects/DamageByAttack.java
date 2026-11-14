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
import org.l2jmobius.gameserver.mechanics.skill.enums.DamageByAttackType;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect modifies the amount of damage taken from an attack.<br>
 * It applies additively to multiple instances and does not affect {@code DOT} effects or terrain damage. Note that it reduces reflected damage received but does not decrease reflected damage dealt.
 * @author Nik, Mobius
 */
public class DamageByAttack extends AbstractEffect
{
	private final double _value;
	private final DamageByAttackType _type;
	
	/**
	 * Creates a new {@code DamageByAttack} effect.<br>
	 * This constructor initializes the damage value and type from the provided parameters.<br>
	 * It ensures that the modification mode is set to {@code DIFF}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public DamageByAttack(StatSet params)
	{
		_value = params.getDouble("amount");
		_type = params.getEnum("type", DamageByAttackType.class, DamageByAttackType.NONE);
		
		if (params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF) != StatModifierType.DIFF)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " can only use DIFF mode.");
		}
	}
	
	/**
	 * Updates the target's stats based on the skill type.<br>
	 * This method applies a damage modifier to the {@code Creature}.<br>
	 * It checks if the effect is for PVP or PVE and adds the value accordingly.
	 * @param target The {@code Creature} receiving the stat change.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 */
	@Override
	public void pump(Creature target, Skill skill)
	{
		switch (_type)
		{
			case PK:
			{
				target.getStat().mergeAdd(Stat.PVP_DAMAGE_TAKEN, _value);
				break;
			}
			case ENEMY_ALL:
			{
				target.getStat().mergeAdd(Stat.PVE_DAMAGE_TAKEN, _value);
				break;
			}
			case MOB:
			{
				target.getStat().mergeAdd(Stat.PVE_DAMAGE_TAKEN_MONSTER, _value);
				break;
			}
			case BOSS:
			{
				target.getStat().mergeAdd(Stat.PVE_DAMAGE_TAKEN_RAID, _value);
				break;
			}
		}
	}
}
