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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureKilled;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a skill when a creature is killed.<br>
 * It listens for {@code OnCreatureKilled} events to execute specific skill effects.
 * @author Sdw, Norvox
 */
public class TriggerSkillByKill extends AbstractEffect
{
	private final int _chance;
	private final SkillHolder _skill;
	private final InstanceType _victimType;
	private final boolean _isToTarget;
	
	/**
	 * Creates a new effect that triggers a skill when a creature is killed.<br>
	 * It initializes the chance and skill details from the provided parameters.
	 * @param params The {@code StatSet} containing configuration values like chance, skillId, and victimType.
	 */
	public TriggerSkillByKill(StatSet params)
	{
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 0));
		_victimType = params.getEnum("victimType", InstanceType.class, InstanceType.Creature);
		_isToTarget = params.getBoolean("isToTarget", false);
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It sets up a listener to detect when a creature dies.
	 * @param effector The {@code Creature} that triggered the effect.
	 * @param effected The {@code Creature} currently affected by this effect.
	 * @param skill The {@code Skill} used to initiate the effect.
	 * @param item The {@code Item} associated with the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_KILLED, (OnCreatureKilled event) -> onCreatureKilled(event), this));
	}
	
	/**
	 * This method is called when the effect finishes.<br>
	 * It removes the kill event listener from the {@code effected} creature.<br>
	 * This ensures that the skill trigger does not fire again after completion.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The skill being executed.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_KILLED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Handles the logic when a creature is killed.<br>
	 * This method checks if the skill should trigger based on chance and victim type.<br>
	 * If all conditions are met, it triggers the associated {@code Skill}.
	 * @param event The {@code OnCreatureKilled} event containing attacker and target data.
	 */
	private void onCreatureKilled(OnCreatureKilled event)
	{
		if ((_chance == 0) || ((_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0)))
		{
			return;
		}
		
		if (Rnd.get(100) > _chance)
		{
			return;
		}
		
		if (!event.getTarget().getInstanceType().isType(_victimType))
		{
			return;
		}
		
		final Creature target = _isToTarget ? event.getTarget() : event.getAttacker();
		SkillCaster.triggerCast(event.getAttacker(), target, _skill.getSkill());
	}
}
