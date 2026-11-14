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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.conditions.Condition;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionPlayerIsInCombat;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingItemType;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingMagicWeapon;
import org.l2jmobius.gameserver.mechanics.conditions.ConditionUsingTwoHandWeapon;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class serves as a base for effects that modify character statistics.<br>
 * It provides the foundation for adding or subtracting values from various {@code Stat} types.
 * @author Sdw, Atronic
 */
public class AbstractStatAddEffect extends AbstractEffect
{
	private final Stat _stat;
	protected final double _amount;
	protected final List<Condition> _conditions = new ArrayList<>();
	
	/**
	 * Creates a new instance of {@code AbstractStatAddEffect}.<br>
	 * This constructor initializes the effect with specific stats and conditions.<br>
	 * It parses parameters from the provided {@code StatSet}.
	 * @param params The set of configuration parameters for this effect.
	 * @param stat The {@code Stat} type that will be modified by this effect.
	 */
	public AbstractStatAddEffect(StatSet params, Stat stat)
	{
		_stat = stat;
		_amount = params.getDouble("amount", 0);
		
		if (params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF) != StatModifierType.DIFF)
		{
			LOGGER.warning(getClass().getSimpleName() + " can only use DIFF mode.");
		}
		
		int weaponTypesMask = 0;
		final List<String> weaponTypes = params.getList("weaponType", String.class);
		if (weaponTypes != null)
		{
			for (String weaponType : weaponTypes)
			{
				try
				{
					weaponTypesMask |= WeaponType.valueOf(weaponType).mask();
				}
				catch (IllegalArgumentException e)
				{
					final IllegalArgumentException exception = new IllegalArgumentException("weaponType should contain WeaponType enum value but found " + weaponType);
					exception.addSuppressed(e);
					throw exception;
				}
			}
		}
		
		int armorTypesMask = 0;
		final List<String> armorTypes = params.getList("armorType", String.class);
		if (armorTypes != null)
		{
			for (String armorType : armorTypes)
			{
				try
				{
					armorTypesMask |= ArmorType.valueOf(armorType).mask();
				}
				catch (IllegalArgumentException e)
				{
					final IllegalArgumentException exception = new IllegalArgumentException("armorTypes should contain ArmorType enum value but found " + armorType);
					exception.addSuppressed(e);
					throw exception;
				}
			}
		}
		
		if (weaponTypesMask != 0)
		{
			_conditions.add(new ConditionUsingItemType(weaponTypesMask));
		}
		
		if (armorTypesMask != 0)
		{
			_conditions.add(new ConditionUsingItemType(armorTypesMask));
		}
		
		if (params.contains("inCombat"))
		{
			_conditions.add(new ConditionPlayerIsInCombat(params.getBoolean("inCombat")));
		}
		
		if (params.contains("magicWeapon"))
		{
			_conditions.add(new ConditionUsingMagicWeapon(params.getBoolean("magicWeapon")));
		}
		
		if (params.contains("twoHandWeapon"))
		{
			_conditions.add(new ConditionUsingTwoHandWeapon(params.getBoolean("twoHandWeapon")));
		}
	}
	
	/**
	 * Applies a stat modification to a target creature.<br>
	 * This method checks all required conditions before applying the effect.<br>
	 * If any condition fails, no changes are made.
	 * @param effected The {@code Creature} receiving the stat change.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (!_conditions.isEmpty())
		{
			for (Condition cond : _conditions)
			{
				if (!cond.test(effected, effected, skill))
				{
					return;
				}
			}
		}
		effected.getStat().mergeAdd(_stat, _amount);
	}
}
