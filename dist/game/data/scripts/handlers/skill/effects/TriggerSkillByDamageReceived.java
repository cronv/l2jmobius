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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a {@code Skill} when a creature receives damage.<br>
 * It implements the functionality for effects that activate based on incoming damage events.
 * @author UnAfraid
 */
public class TriggerSkillByDamageReceived extends AbstractEffect
{
	private final int _minAttackerLevel;
	private final int _maxAttackerLevel;
	private final int _minDamage;
	private final int _chance;
	private final int _hpPercent;
	private final SkillHolder _skill;
	private final TargetType _targetType;
	private final InstanceType _attackerType;
	private final int _skillLevelScaleTo;
	private final List<SkillHolder> _triggerSkills;
	
	/**
	 * Initializes a new effect that triggers skills based on damage received.<br>
	 * This constructor extracts all necessary configuration values from the provided {@code StatSet}.<br>
	 * It sets up requirements such as attacker level, minimum damage, and trigger chance.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public TriggerSkillByDamageReceived(StatSet params)
	{
		_minAttackerLevel = params.getInt("minAttackerLevel", 1);
		_maxAttackerLevel = params.getInt("maxAttackerLevel", Integer.MAX_VALUE);
		_minDamage = params.getInt("minDamage", 1);
		_chance = params.getInt("chance", 100);
		_hpPercent = params.getInt("hpPercent", 100);
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 1));
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.SELF);
		_attackerType = params.getEnum("attackerType", InstanceType.class, InstanceType.Creature);
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
		
		// Specific skills by level.
		final String triggerSkills = params.getString("triggerSkills", "");
		if (triggerSkills.isEmpty())
		{
			_triggerSkills = null;
		}
		else
		{
			final String[] split = triggerSkills.split(";");
			_triggerSkills = new ArrayList<>(split.length);
			for (String skill : split)
			{
				final String[] splitSkill = skill.split(",");
				_triggerSkills.add(new SkillHolder(Integer.parseInt(splitSkill[0]), Integer.parseInt(splitSkill[1])));
			}
		}
	}
	
	/**
	 * Handles the logic for triggering a skill when a creature receives damage.<br>
	 * This method checks various conditions such as damage amount, attacker level, and chance.<br>
	 * If all conditions are met, it identifies a target and triggers the appropriate skill.
	 * @param event The {@code OnCreatureDamageReceived} event containing details about the damage taken.
	 */
	private void onDamageReceivedEvent(OnCreatureDamageReceived event)
	{
		if (event.isDamageOverTime() || (_chance == 0) || ((_triggerSkills == null) && ((_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0))))
		{
			return;
		}
		
		final Creature attacker = event.getAttacker();
		final Creature targetObj = event.getTarget();
		if (attacker == targetObj)
		{
			return;
		}
		
		final int attackerLevel = attacker.getLevel();
		if ((attackerLevel < _minAttackerLevel) || (attackerLevel > _maxAttackerLevel))
		{
			return;
		}
		
		if (event.getDamage() < _minDamage)
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		if ((_hpPercent < 100) && (targetObj.getCurrentHpPercent() > _hpPercent))
		{
			return;
		}
		
		if (!attacker.getInstanceType().isType(_attackerType))
		{
			return;
		}
		
		WorldObject target = null;
		try
		{
			target = TargetHandler.getInstance().getHandler(_targetType).getTarget(targetObj, attacker, _triggerSkills == null ? _skill.getSkill() : _triggerSkills.get(0).getSkill(), false, false, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in ITargetTypeHandler.getTarget(): " + e.getMessage(), e);
		}
		
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		// Warg - TODO: Unhardcode.
		final int skillId = _skill.getSkillId();
		if (skillId == 31493 /* Transient Instinct */)
		{
			final Creature eventTarget = event.getTarget();
			if (eventTarget.isSkillDisabled(SkillData.getInstance().getSkill(31494 /* Transient Instinct */, 1)) || //
				eventTarget.isSkillDisabled(SkillData.getInstance().getSkill(31495 /* Transient Instinct */, 1)) || //
				eventTarget.isSkillDisabled(SkillData.getInstance().getSkill(31496 /* Transient Instinct */, 1)) || //
				eventTarget.isSkillDisabled(SkillData.getInstance().getSkill(31497 /* Transient Instinct */, 1)) || //
				eventTarget.isSkillDisabled(SkillData.getInstance().getSkill(31498 /* Transient Instinct */, 1)))
			{
				return;
			}
		}
		
		Skill triggerSkill = null;
		if (_triggerSkills == null)
		{
			final BuffInfo buffInfo = target.asCreature().getEffectList().getBuffInfoBySkillId(skillId);
			if ((_skillLevelScaleTo <= 0) || (buffInfo == null))
			{
				triggerSkill = _skill.getSkill();
			}
			else
			{
				triggerSkill = SkillData.getInstance().getSkill(skillId, Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1));
			}
			
			if ((buffInfo == null) || (buffInfo.getSkill().getLevel() < triggerSkill.getLevel()))
			{
				SkillCaster.triggerCast((triggerSkill.getTargetType() == TargetType.SELF) ? target.asCreature() : attacker, target.asCreature(), triggerSkill);
			}
		}
		else // Multiple trigger skills.
		{
			final Iterator<SkillHolder> iterator = _triggerSkills.iterator();
			while (iterator.hasNext())
			{
				final Skill nextSkill = iterator.next().getSkill();
				final Creature targetCreature = target.asCreature();
				if (targetCreature.isAffectedBySkill(nextSkill.getId()))
				{
					if (iterator.hasNext())
					{
						targetCreature.stopSkillEffects(SkillFinishType.SILENT, nextSkill.getId());
						triggerSkill = iterator.next().getSkill();
						break;
					}
					
					// Already at last skill.
					return;
				}
			}
			
			if (triggerSkill == null)
			{
				triggerSkill = _triggerSkills.get(0).getSkill();
			}
			
			SkillCaster.triggerCast((triggerSkill.getTargetType() == TargetType.SELF) ? target.asCreature() : attacker, target.asCreature(), triggerSkill);
		}
	}
	
	/**
	 * Handles the logic when an effect is removed.<br>
	 * This method cleans up listeners from the {@code effected} creature.<br>
	 * It ensures that the damage received event listener owned by this effect is removed.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The skill being executed.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It registers a listener to handle damage received events for the {@code effected} creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect and the damage listener.
	 * @param skill The skill associated with this effect.
	 * @param item The item involved in the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedEvent(event), this));
	}
}
