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
import org.l2jmobius.gameserver.handler.TargetHandler;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.skill.targets.TargetType;
import org.l2jmobius.gameserver.util.ArrayUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic to trigger a skill based on its magic type.<br>
 * It allows for specific skill execution when certain magic criteria are met.
 * @author Zealar
 */
public class TriggerSkillByMagicType extends AbstractEffect
{
	private final int[] _magicTypes;
	private final int _chance;
	private final SkillHolder _skill;
	private final int _skillLevelScaleTo;
	private final TargetType _targetType;
	private final boolean _replace;
	
	/**
	 * Initializes a new effect that triggers a skill based on magic types.<br>
	 * This constructor reads all necessary configuration from the provided {@code StatSet}.<br>
	 * It sets up parameters like chance, skill IDs, and target types.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public TriggerSkillByMagicType(StatSet params)
	{
		_magicTypes = params.getIntArray("magicTypes", ";");
		_chance = params.getInt("chance", 100);
		_skill = new SkillHolder(params.getInt("skillId", 0), params.getInt("skillLevel", 0));
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
		_targetType = params.getEnum("targetType", TargetType.class, TargetType.TARGET);
		_replace = params.getBoolean("replace", true);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It registers a listener to detect when the target finishes casting a skill.<br>
	 * The logic only proceeds if all required conditions like chance and magic types are met.
	 * @param effector The creature that triggers the effect.
	 * @param effected The creature who receives the effect.
	 * @param skill The skill being used by the target.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((_chance == 0) || (_skill.getSkillId() == 0) || (_skill.getSkillLevel() == 0) || (_magicTypes.length == 0))
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
	 * Handles the logic for triggering a skill after another skill finishes casting.<br>
	 * It checks if the casted skill matches the required magic types and chance.<br>
	 * The method determines the target and calculates the appropriate skill level to trigger.<br>
	 * Finally, it triggers the new skill on the valid target.
	 * @param event The {@code OnCreatureSkillFinishCast} event containing the caster, target, and original skill.
	 */
	private void onSkillUseEvent(OnCreatureSkillFinishCast event)
	{
		WorldObject target = event.getTarget();
		if (target == null)
		{
			return;
		}
		
		if (!target.isCreature())
		{
			return;
		}
		
		final Skill eventSkill = event.getSkill();
		if (!ArrayUtil.contains(_magicTypes, eventSkill.getMagicType()))
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		final Creature caster = event.getCaster();
		target = null;
		try
		{
			target = TargetHandler.getInstance().getHandler(_targetType).getTarget(caster, event.getTarget(), _skill.getSkill(), false, false, false);
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Exception in ITargetTypeHandler.getTarget(): " + e.getMessage(), e);
		}
		
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		// Ignore common skills.
		for (CommonSkill skill : CommonSkill.values())
		{
			if (skill.getId() == eventSkill.getId())
			{
				return;
			}
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
				triggerSkill = SkillData.getInstance().getSkill(_skill.getSkillId(), Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1));
				
				if (caster.isSkillDisabled(buffInfo.getSkill()))
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
		if (_replace && !caster.isSkillDisabled(triggerSkill))
		{
			target.asCreature().stopSkillEffects(SkillFinishType.SILENT, triggerSkill.getId());
		}
		
		SkillCaster.triggerCast(caster, target.asCreature(), triggerSkill);
	}
}
