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

import java.util.logging.Level;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code TriggerSkillBySkill} effect.<br>
 * It allows a skill to trigger another specific skill upon completion.<br>
 * It manages the logic for executing these chained skills between entities.
 * @author Zealar, fruit
 */
public class TriggerSkillBySkill extends AbstractEffect
{
	private final int _castSkillId;
	private final int _castSkillLevel;
	private final int _chance;
	private final SkillHolder _skill;
	private final int _skillLevelScaleTo;
	private final TargetType _targetType;
	private final boolean _replace;
	
	/**
	 * Creates a new instance of {@code TriggerSkillBySkill}.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets up properties like skill IDs, chances, and target types.
	 * @param params The {@code StatSet} containing all configuration data for this effect.
	 */
	public TriggerSkillBySkill(StatSet params)
	{
		_castSkillId = params.getInt("castSkillId");
		_castSkillLevel = params.getInt("castSkillLevel", 0);
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 1), params.getInt("skillSubLevel", 0));
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.TARGET);
		_replace = params.getBoolean("replace", true);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It registers a listener to detect when the target finishes casting a skill.<br>
	 * The logic only proceeds if all required skill IDs and chances are valid.
	 * @param effector The creature that applies the effect.
	 * @param effected The creature that receives the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((_chance == 0) || (_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0) || (_castSkillId == 0))
		{
			return;
		}
		
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onSkillUseEvent(event), this));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the skill finish cast listener from the {@code effected} creature.<br>
	 * This ensures that the listener owned by this effect is cleaned up properly.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature that was affected by the effect.
	 * @param skill The skill associated with the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Handles the logic for triggering a skill when another skill finishes casting.<br>
	 * This method validates the cast skill, chance, and target before triggering the new effect.<br>
	 * It also handles level scaling and replaces existing effects if configured.
	 * @param event The {@code OnCreatureSkillFinishCast} event containing the caster and target details.
	 */
	private void onSkillUseEvent(OnCreatureSkillFinishCast event)
	{
		if (_castSkillId != event.getSkill().getId())
		{
			return;
		}
		
		if ((_castSkillLevel > 0) && (_castSkillLevel != event.getSkill().getLevel()))
		{
			return;
		}
		
		WorldObject target = event.getTarget();
		if (target == null)
		{
			return;
		}
		
		if (!target.isCreature())
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		target = null;
		try
		{
			target = TargetHandler.getInstance().getHandler(_targetType).getTarget(event.getCaster(), event.getTarget(), _skill.getSkill(), false, false, false);
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
			final BuffInfo buffInfo = target.asCreature().getEffectList().getBuffInfoBySkillId(_skill.getSkillId());
			if (buffInfo != null)
			{
				triggerSkill = SkillData.getInstance().getSkill(_skill.getSkillId(), Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1), _skill.getSkillSubLevel());
				
				if (event.getCaster().isSkillDisabled(buffInfo.getSkill()))
				{
					if ((_replace) && (buffInfo.getSkill().getLevel() == _skillLevelScaleTo))
					{
						target.asCreature().stopSkillEffects(SkillFinishType.SILENT, triggerSkill.getId());
					}
					return;
				}
			}
			else
			{
				triggerSkill = _skill.getSkill();
			}
		}
		
		// Remove existing effect, otherwise time will not be renewed at max level.
		if (_replace)
		{
			target.asCreature().stopSkillEffects(SkillFinishType.SILENT, triggerSkill.getId());
		}
		
		SkillCaster.triggerCast(event.getCaster(), target.asCreature(), triggerSkill);
	}
}
