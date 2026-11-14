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
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.DamageReturn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the reduction of incoming damage for a {@code Creature}.<br>
 * It listens for {@code OnCreatureDamageReceived} events to modify the final damage amount.<br>
 * The effect applies a modifier based on the character's stats or specific skill properties.
 * @author Sdw, Mobius
 */
public class ReduceDamage extends AbstractEffect
{
	private final double _amount;
	private final StatModifierType _mode;
	
	/**
	 * Creates a new {@code ReduceDamage} effect.<br>
	 * This constructor initializes the damage reduction amount from the provided stats.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ReduceDamage(StatSet params)
	{
		_amount = params.getDouble("amount");
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
	}
	
	/**
	 * Handles the logic for reducing damage received by a creature.<br>
	 * This method ignores damage over time effects.<br>
	 * It calculates the final damage based on the defined percentage.
	 * @param event The {@code OnCreatureDamageReceived} event containing damage details.
	 * @return A new {@code DamageReturn} object with the modified damage value.
	 */
	private DamageReturn onDamageReceivedEvent(OnCreatureDamageReceived event)
	{
		// DOT effects are not taken into account.
		if (event.isDamageOverTime())
		{
			return null;
		}
		
		final double newDamage;
		if (_mode == StatModifierType.PER)
		{
			final double damage = event.getDamage();
			newDamage = damage - (damage * (_amount / 100));
		}
		else // DIFF
		{
			newDamage = event.getDamage() - Math.max((_amount - event.getAttacker().getStat().getAddValue(Stat.IGNORE_REDUCE_DAMAGE)), 0.0);
		}
		
		return new DamageReturn(false, true, false, newDamage);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the damage reduction listener from the {@code effected} creature.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It attaches a damage listener to the {@code effected} creature.<br>
	 * The listener handles the {@code ON_CREATURE_DAMAGE_RECEIVED} event.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item used to trigger this effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedEvent(event), this));
	}
}
