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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.ITargetTypeHandler;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttackAvoid;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a skill when an attack is avoided.<br>
 * It implements the {@code TriggerSkillByAvoid} effect to execute specific actions upon successful evasion.
 * @author Zealar
 */
public class TriggerSkillByAvoid extends AbstractEffect
{
	private final int _chance;
	private final SkillHolder _skill;
	private final TargetType _targetType;
	private final int _skillLevelScaleTo;
	
	/**
	 * Initializes a new {@code TriggerSkillByAvoid} effect.<br>
	 * This method extracts configuration values from the provided {@code StatSet}.<br>
	 * It sets up the skill chance, skill data, and target type.
	 * @param params The {@code StatSet} containing the required configuration parameters.
	 */
	public TriggerSkillByAvoid(StatSet params)
	{
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 0));
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.TARGET);
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
	}
	
	/**
	 * Handles the logic when a creature successfully avoids an attack.<br>
	 * This method checks if a skill should be triggered based on specific conditions.<br>
	 * It validates the chance, target type, and required skill data.<br>
	 * If all conditions are met, it triggers the specified skill on the target.
	 * @param event The {@code OnCreatureAttackAvoid} event containing attack details.
	 */
	private void onAvoidEvent(OnCreatureAttackAvoid event)
	{
		final int skillId = _skill.getSkillId();
		if (event.isDamageOverTime() || (_chance == 0) || ((skillId == 0) || (_skill.getSkillLevel() == 0)))
		{
			return;
		}
		
		final ITargetTypeHandler targetHandler = TargetHandler.getInstance().getHandler(_targetType);
		if (targetHandler == null)
		{
			LOGGER.warning("Handler for target type: " + _targetType + " does not exist.");
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		WorldObject target = null;
		try
		{
			target = targetHandler.getTarget(event.getTarget(), event.getAttacker(), _skill.getSkill(), false, false, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in ITargetTypeHandler.getTarget(): " + e.getMessage(), e);
		}
		
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		final Skill triggerSkill;
		if (_skillLevelScaleTo <= 0)
		{
			triggerSkill = _skill.getSkill();
		}
		else
		{
			final BuffInfo buffInfo = target.asCreature().getEffectList().getBuffInfoBySkillId(skillId);
			if (buffInfo != null)
			{
				triggerSkill = SkillData.getInstance().getSkill(skillId, Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1));
			}
			else
			{
				triggerSkill = _skill.getSkill();
			}
		}
		
		SkillCaster.triggerCast(event.getAttacker(), target.asCreature(), triggerSkill);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the specific event listener from the {@code effected} creature.<br>
	 * This ensures that the avoid event no longer triggers for this effect.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The {@code Skill} object used to create the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_ATTACK_AVOID, listener -> listener.getOwner() == this);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It registers a listener to handle attack avoidance events.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} associated with this effect.
	 * @param item The {@code Item} used to trigger the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_ATTACK_AVOID, (OnCreatureAttackAvoid event) -> onAvoidEvent(event), this));
	}
}
