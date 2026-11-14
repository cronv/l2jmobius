/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import java.util.logging.Level;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for triggering a skill when a creature receives a killing blow.<br>
 * This {@code TriggerSkillByDeathBlow} effect listens for death events to execute specific mechanics.
 * @author Sdw
 */
public class TriggerSkillByDeathBlow extends AbstractEffect
{
	private final int _minAttackerLevel;
	private final int _maxAttackerLevel;
	private final int _chance;
	private final SkillHolder _skill;
	private final TargetType _targetType;
	private final InstanceType _attackerType;
	
	/**
	 * Initializes a new effect that triggers a skill upon a death blow.<br>
	 * This constructor extracts all necessary configuration from the {@code StatSet}.<br>
	 * It sets values for levels, chance, and target types.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public TriggerSkillByDeathBlow(StatSet params)
	{
		_minAttackerLevel = params.getInt("minAttackerLevel", 1);
		_maxAttackerLevel = params.getInt("maxAttackerLevel", Integer.MAX_VALUE);
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1));
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.SELF);
		_attackerType = params.getEnum("attackerType", InstanceType.class, InstanceType.Creature);
	}
	
	/**
	 * Handles the logic when a creature receives damage.<br>
	 * This method checks if the damage dealt is a killing blow and triggers a skill based on specific conditions.<br>
	 * It validates the attacker level, instance type, and a random chance before executing the effect.
	 * @param event The {@code OnCreatureDamageReceived} event containing details about the damage dealt.
	 */
	private void onDamageReceivedEvent(OnCreatureDamageReceived event)
	{
		if (event.getDamage() < event.getTarget().getCurrentHp())
		{
			return;
		}
		
		if ((_chance == 0) || (_skill.getSkillLevel() == 0))
		{
			return;
		}
		
		if (event.getAttacker() == event.getTarget())
		{
			return;
		}
		
		final int attackerLevel = event.getAttacker().getLevel();
		if ((attackerLevel < _minAttackerLevel) || (attackerLevel > _maxAttackerLevel))
		{
			return;
		}
		
		if (((_chance < 100) && (Rnd.get(100) > _chance)) || !event.getAttacker().getInstanceType().isType(_attackerType))
		{
			return;
		}
		
		final Skill triggerSkill = _skill.getSkill();
		WorldObject target = null;
		try
		{
			target = TargetHandler.getInstance().getHandler(_targetType).getTarget(event.getTarget(), event.getAttacker(), triggerSkill, false, false, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in ITargetTypeHandler.getTarget(): " + e.getMessage(), e);
		}
		
		if ((target != null) && target.isCreature())
		{
			SkillCaster.triggerCast(event.getTarget(), target.asCreature(), triggerSkill);
		}
	}
	
	/**
	 * This method is called when the effect finishes.<br>
	 * It removes the damage listener from the {@code effected} creature.<br>
	 * This ensures that the effect no longer tracks damage events for this target.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It attaches a damage listener to the {@code effected} creature.<br>
	 * The listener triggers the logic defined in {@code onDamageReceivedEvent}.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedEvent(event), this));
	}
}
