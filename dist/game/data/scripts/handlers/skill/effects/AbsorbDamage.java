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
import org.l2jmobius.gameserver.mechanics.skill.enums.StatModifierType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for an effect that allows a creature to absorb incoming damage.<br>
 * It listens for {@code OnCreatureDamageReceived} events and modifies the resulting damage based on specific stats.
 * @author Sdw, Mobius
 */
public class AbsorbDamage extends AbstractEffect
{
	private static final Map<Integer, Double> DIFF_DAMAGE_HOLDER = new ConcurrentHashMap<>();
	private static final Map<Integer, Double> PER_DAMAGE_HOLDER = new ConcurrentHashMap<>();
	private static final Map<Integer, Integer> HITS_HOLDER = new ConcurrentHashMap<>();
	
	private final double _damage;
	private final StatModifierType _mode;
	private final int _hits;
	private final double _casterHpMod;
	
	/**
	 * Initializes a new {@code AbsorbDamage} effect.<br>
	 * This constructor sets the damage amount and the modifier mode from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public AbsorbDamage(StatSet params)
	{
		_damage = params.getDouble("damage", 0);
		_mode = params.getEnum("mode", StatModifierType.class, StatModifierType.DIFF);
		_hits = params.getInt("hits", -1);
		_casterHpMod = params.getDouble("casterHpMod", 0); // % from caster Max HP to _damage
	}
	
	/**
	 * Handles damage reduction for different types of events.<br>
	 * This method ignores damage over time effects.<br>
	 * It calculates the remaining damage based on a stored value.<br>
	 * If the absorbed amount is exhausted, it stops the skill effect.
	 * @param event The {@code OnCreatureDamageReceived} event containing damage details.
	 * @param effected The {@code Creature} receiving the damage.
	 * @param skill The {@code Skill} that triggered this effect.
	 * @return A {@code DamageReturn} object with the calculated remaining damage.
	 */
	private DamageReturn onDamageReceivedDiffEvent(OnCreatureDamageReceived event, Creature effected, Skill skill)
	{
		// DOT effects are not taken into account.
		if (event.isDamageOverTime())
		{
			return null;
		}
		
		final int objectId = event.getTarget().getObjectId();
		
		final double damageLeft = DIFF_DAMAGE_HOLDER.getOrDefault(objectId, 0d);
		final double newDamageLeft = Math.max(damageLeft - event.getDamage(), 0);
		final double newDamage = Math.max(event.getDamage() - damageLeft, 0);
		
		int hitsUsed = HITS_HOLDER.getOrDefault(objectId, 0);
		if (_hits != -1)
		{
			hitsUsed++;
			if (hitsUsed > _hits)
			{
				effected.stopSkillEffects(skill);
			}
			else
			{
				HITS_HOLDER.put(objectId, hitsUsed);
			}
		}
		
		final int eventDamage = (int) event.getDamage();
		if (newDamageLeft > 0)
		{
			effected.sendMessage("You absorbed " + (int) damageLeft + " damage from " + eventDamage + ".");
			DIFF_DAMAGE_HOLDER.put(objectId, newDamageLeft);
		}
		else
		{
			effected.sendMessage("You absorbed " + (int) damageLeft + " damage from " + eventDamage + " and barrier has been removed.");
			effected.stopSkillEffects(skill);
		}
		
		return new DamageReturn(false, true, false, newDamage);
	}
	
	/**
	 * Processes the damage received by a creature for this effect.<br>
	 * It ignores damage over time effects.<br>
	 * It calculates and returns the modified damage amount based on stored percentages.
	 * @param event The {@code OnCreatureDamageReceived} event containing the current damage details.
	 * @return A {@code DamageReturn} object containing the new calculated damage value.
	 */
	private DamageReturn onDamageReceivedPerEvent(OnCreatureDamageReceived event)
	{
		// DOT effects are not taken into account.
		if (event.isDamageOverTime())
		{
			return null;
		}
		
		final int objectId = event.getTarget().getObjectId();
		
		final double damagePercent = PER_DAMAGE_HOLDER.getOrDefault(objectId, 0d);
		final double currentDamage = event.getDamage();
		final double newDamage = currentDamage - ((currentDamage / 100) * damagePercent);
		
		return new DamageReturn(false, true, false, newDamage);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the damage listener from the {@code effected} creature.<br>
	 * It also clears any stored damage values from the internal maps.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The skill used to apply this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
		final int objectId = effected.getObjectId();
		if (_mode == StatModifierType.DIFF)
		{
			DIFF_DAMAGE_HOLDER.remove(objectId);
		}
		else
		{
			PER_DAMAGE_HOLDER.remove(objectId);
		}
		
		HITS_HOLDER.remove(objectId);
		
		// Stop other effects when shield is removed.
		if (skill != null)
		{
			effected.stopSkillEffects(SkillFinishType.SILENT, skill.getId());
		}
	}
	
	/**
	 * This method initializes the effect when it is first applied.<br>
	 * It registers a listener to handle damage received by the target.<br>
	 * The behavior depends on whether the mode is {@code DIFF} or another type.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply this effect.
	 * @param item The item used to trigger the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_mode == StatModifierType.DIFF)
		{
			DIFF_DAMAGE_HOLDER.put(effected.getObjectId(), _damage + ((effector.getMaxHp() * _casterHpMod) / 100));
			effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedDiffEvent(event, effected, skill), this));
		}
		else
		{
			PER_DAMAGE_HOLDER.put(effected.getObjectId(), _damage + ((effector.getMaxHp() * _casterHpMod) / 100));
			effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedPerEvent(event), this));
		}
		
		HITS_HOLDER.put(effected.getObjectId(), 0);
	}
}
