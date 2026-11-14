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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.DamageReturn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect prevents a creature from receiving ranged damage from the front.<br>
 * It listens for {@code OnCreatureDamageReceived} events to filter incoming attacks.
 * @author Liamxroy
 */
public class BlockFrontalRangedDamage extends AbstractEffect
{
	private static final Map<Integer, Double> DAMAGE_HOLDER = new ConcurrentHashMap<>();
	
	private final double _damage;
	
	/**
	 * Creates a new instance of {@code BlockFrontalRangedDamage}.<br>
	 * This effect handles damage reduction for frontal ranged attacks.<br>
	 * It initializes the damage value from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public BlockFrontalRangedDamage(StatSet params)
	{
		_damage = params.getDouble("damage", 0);
	}
	
	/**
	 * Handles the logic for blocking frontal ranged damage.<br>
	 * This method checks if an attacker is in front of and at a distance from the target.<br>
	 * It calculates the remaining damage to be blocked based on the current effect state.
	 * @param event The {@code OnCreatureDamageReceived} event containing damage details.
	 * @param effected The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} that triggered the damage.
	 * @return A {@code DamageReturn} object containing the modified damage value.
	 */
	private DamageReturn onDamageReceivedEvent(OnCreatureDamageReceived event, Creature effected, Skill skill)
	{
		if (event.isDamageOverTime())
		{
			return null;
		}
		
		double newDamage = event.getDamage();
		final int objectId = event.getTarget().getObjectId();
		final double damageLeft = DAMAGE_HOLDER.getOrDefault(objectId, 0d);
		if ((event.getAttacker() != null) && (event.getTarget() != null))
		{
			if (event.getAttacker().isInFrontOf(event.getTarget()) && (event.getAttacker().calculateDistance2D(event.getTarget()) > 240))
			{
				newDamage = newDamage - damageLeft;
				newDamage = Math.max(0, newDamage);
			}
			else
			{
				return new DamageReturn(false, true, false, newDamage);
			}
		}
		
		final double newDamageLeft = newDamage == 0 ? Math.max(damageLeft - event.getDamage(), 0) : 0;
		if (newDamageLeft > 0)
		{
			DAMAGE_HOLDER.put(objectId, newDamageLeft);
		}
		else
		{
			effected.stopSkillEffects(SkillFinishType.REMOVED, skill.getId());
		}
		
		DAMAGE_HOLDER.put(objectId, newDamageLeft);
		
		return new DamageReturn(false, true, false, newDamage);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the damage listener from the {@code effected} creature.<br>
	 * It also removes the damage data for this creature from the internal map.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
		DAMAGE_HOLDER.remove(effected.getObjectId());
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It stores the damage value for the {@code effected} creature.<br>
	 * It also adds a listener to handle incoming damage events.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item used to cast the skill.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		DAMAGE_HOLDER.put(effected.getObjectId(), _damage);
		effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedEvent(event, effected, skill), this));
	}
}
