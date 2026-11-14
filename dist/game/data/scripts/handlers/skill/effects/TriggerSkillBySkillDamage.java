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
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageDealt;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a new skill based on damage dealt by another skill.<br>
 * It listens for {@code OnCreatureDamageDealt} events to execute specific effects.
 * @author Nik
 */
public class TriggerSkillBySkillDamage extends AbstractEffect
{
	private final int _minAttackerLevel;
	private final int _maxAttackerLevel;
	private final int _minDamage;
	private final int _chance;
	private final SkillHolder _attackSkill;
	private final SkillHolder _skill;
	private final int _skillLevelScaleTo;
	private final TargetType _targetType;
	private final InstanceType _attackerType;
	
	/**
	 * Initializes a new instance of the effect using provided parameters.<br>
	 * This constructor extracts all necessary values from the {@code StatSet}.<br>
	 * It sets up requirements for levels, damage, and skill IDs.
	 * @param params The {@code StatSet} containing configuration data for this effect.
	 */
	public TriggerSkillBySkillDamage(StatSet params)
	{
		_minAttackerLevel = params.getInt("minAttackerLevel", 1);
		_maxAttackerLevel = params.getInt("maxAttackerLevel", Integer.MAX_VALUE);
		_minDamage = params.getInt("minDamage", 1);
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1));
		_attackSkill = new SkillHolder(params.getInt("attackSkillId"), params.getInt("attackSkillLevel", 1));
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.TARGET);
		_attackerType = params.getEnum("attackerType", InstanceType.class, InstanceType.Creature);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It registers a listener to handle damage events for the target creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger this effect.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DAMAGE_DEALT, (OnCreatureDamageDealt event) -> onAttackEvent(event), this));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the damage listener from the {@code effected} creature.<br>
	 * This ensures that no further damage events are processed for this specific effect.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The {@code Skill} used to trigger the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_DEALT, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Handles the logic for triggering a skill when an attack deals damage.<br>
	 * This method checks if the attacker meets specific level and type requirements.<br>
	 * It also verifies that the damage exceeds the minimum threshold and passes a random chance check.<br>
	 * If all conditions are met, it triggers the specified skill on a valid target.
	 * @param event The {@code OnCreatureDamageDealt} event containing attack details.
	 */
	private void onAttackEvent(OnCreatureDamageDealt event)
	{
		final int skillId = _skill.getSkillId();
		if (event.isDamageOverTime() || (_chance == 0) || ((skillId == 0) || (_skill.getSkillLevel() == 0)) || (_attackSkill.getSkillId() == 0))
		{
			return;
		}
		
		if (event.getSkill() == null)
		{
			return;
		}
		
		if (event.getSkill().getId() != _attackSkill.getSkillId())
		{
			return;
		}
		
		final ITargetTypeHandler targetHandler = TargetHandler.getInstance().getHandler(_targetType);
		if (targetHandler == null)
		{
			LOGGER.warning("Handler for target type: " + _targetType + " does not exist.");
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
		
		if ((event.getDamage() < _minDamage) || ((_chance < 100) && (Rnd.get(100) > _chance)) || !event.getAttacker().getInstanceType().isType(_attackerType))
		{
			return;
		}
		
		Skill triggerSkill = _skill.getSkill();
		WorldObject target = null;
		try
		{
			target = TargetHandler.getInstance().getHandler(_targetType).getTarget(event.getAttacker(), event.getTarget(), triggerSkill, false, false, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in ITargetTypeHandler.getTarget(): " + e.getMessage(), e);
		}
		
		if ((target != null) && target.isCreature())
		{
			if (_skillLevelScaleTo > 0)
			{
				final BuffInfo buffInfo = target.asCreature().getEffectList().getBuffInfoBySkillId(skillId);
				if (buffInfo != null)
				{
					triggerSkill = SkillData.getInstance().getSkill(skillId, Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1));
				}
			}
			
			SkillCaster.triggerCast(event.getAttacker(), target.asCreature(), triggerSkill);
		}
	}
}
