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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect sets the current {@code cp} of a creature to a specific value.<br>
 * It is used to override the existing {@code cp} amount with a new given value.
 * @author QuangNguyen
 */
public class SetCp extends AbstractEffect
{
	private final double _amount;
	private final StatModifierType _mode;
	
	/**
	 * Creates a new {@code SetCp} effect.<br>
	 * This constructor initializes the amount and mode from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public SetCp(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the effect to a target creature immediately.<br>
	 * This method checks if the target is alive and not a door before applying changes.<br>
	 * It calculates the new CP based on the mode and amount defined in the effect.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor())
		{
			return;
		}
		
		final boolean full = (_mode == StatModifierType.PER) && (_amount == 100.0);
		final int maxCp = effected.getMaxCp();
		final double amount = full ? maxCp : (_mode == StatModifierType.PER) ? ((maxCp * _amount) / 100.0) : _amount;
		effected.setCurrentCp(amount);
	}
}
