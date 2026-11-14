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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.player.PlayerClass;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.DamageReturn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect restricts damage taken by a {@code Creature} based on their {@code PlayerClass}.<br>
 * It checks if the target's class is permitted to receive damage from specific sources.<br>
 * If the class is not authorized, the damage is negated.
 * @author Galagard
 */
public class DamageEnabledByClass extends AbstractEffect
{
	private final Set<PlayerClass> _allowedClasses = new HashSet<>();
	
	/**
	 * Creates a new {@code DamageEnabledByClass} effect.<br>
	 * This constructor initializes the allowed classes from the provided parameters.<br>
	 * It parses the {@code allowedClasses} string into a set of {@code PlayerClass} values.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public DamageEnabledByClass(StatSet params)
	{
		for (String classId : params.getString("allowedClasses", "").split(";"))
		{
			if (!classId.isEmpty())
			{
				_allowedClasses.add(PlayerClass.valueOf(classId));
			}
		}
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It attaches a damage listener to the {@code effected} creature.<br>
	 * The listener triggers whenever the target takes damage.
	 * @param effector The creature that cast the skill or caused the effect.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceived(event), this));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the damage listener from the {@code effected} creature.<br>
	 * This ensures that the class-based damage restriction is no longer active.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} that was affected by the skill.
	 * @param skill The {@code Skill} used to apply this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Handles damage received by a creature.<br>
	 * This method checks if the attacker is a player of an allowed class.<br>
	 * If they are, it allows the damage to proceed normally.
	 * @param event The {@code OnCreatureDamageReceived} event containing damage details.
	 * @return A {@code DamageReturn} object indicating whether the damage was modified or blocked.
	 */
	private DamageReturn onDamageReceived(OnCreatureDamageReceived event)
	{
		final Creature attacker = event.getAttacker();
		if (attacker.isPlayer() && _allowedClasses.contains(attacker.asPlayer().getPlayerClass()))
		{
			return null;
		}
		
		return new DamageReturn(false, false, true, 0);
	}
}
