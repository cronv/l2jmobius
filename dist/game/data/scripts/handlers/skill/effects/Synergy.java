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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Synergy} skill effect.<br>
 * It manages how specific skills interact to produce combined benefits or behaviors.
 */
public class Synergy extends AbstractEffect
{
	private final Set<AbnormalType> _requiredSlots;
	private final Set<AbnormalType> _optionalSlots;
	private final int _partyBuffSkillId;
	private final int _skillLevelScaleTo;
	private final int _minSlot;
	
	/**
	 * Creates a new instance of the {@code Synergy} effect.<br>
	 * This constructor initializes all required fields from the provided data set.<br>
	 * It parses slot requirements and sets the scaling values for the synergy logic.
	 * @param params The {@code StatSet} containing configuration values for this effect.
	 */
	public Synergy(StatSet params)
	{
		final String requiredSlots = params.getString("requiredSlots", null);
		if ((requiredSlots != null) && !requiredSlots.isEmpty())
		{
			_requiredSlots = new HashSet<>();
			for (String slot : requiredSlots.split(";"))
			{
				_requiredSlots.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_requiredSlots = Collections.<AbnormalType> emptySet();
		}
		
		final String optionalSlots = params.getString("optionalSlots", null);
		if ((optionalSlots != null) && !optionalSlots.isEmpty())
		{
			_optionalSlots = new HashSet<>();
			for (String slot : optionalSlots.split(";"))
			{
				_optionalSlots.add(AbnormalType.getAbnormalType(slot));
			}
		}
		else
		{
			_optionalSlots = Collections.<AbnormalType> emptySet();
		}
		
		_partyBuffSkillId = params.getInt("partyBuffSkillId");
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 1);
		_minSlot = params.getInt("minSlot", 2);
		setTicks(params.getInt("ticks"));
	}
	
	/**
	 * Checks if the synergy effect should be applied during an action.<br>
	 * It verifies if the {@code effector} meets the required abnormal types.<br>
	 * It also checks if the optional abnormal type count meets the minimum threshold.
	 * @param effector The creature casting or triggering the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being executed.
	 * @param item The item associated with the action.
	 * @return {@code true} if the skill is a toggle, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isDead())
		{
			return false;
		}
		
		for (AbnormalType required : _requiredSlots)
		{
			if (!effector.hasAbnormalType(required))
			{
				return skill.isToggle();
			}
		}
		
		int abnormalCount = 0;
		for (AbnormalType abnormalType : _optionalSlots)
		{
			if (effector.hasAbnormalType(abnormalType))
			{
				abnormalCount++;
			}
		}
		
		if (abnormalCount >= _minSlot)
		{
			final SkillHolder partyBuff = new SkillHolder(_partyBuffSkillId, Math.min(abnormalCount - 1, _skillLevelScaleTo));
			final Skill partyBuffSkill = partyBuff.getSkill();
			if (partyBuffSkill != null)
			{
				final WorldObject target = partyBuffSkill.getTarget(effector, effected, false, false, false);
				if ((target != null) && target.isCreature())
				{
					final EffectList effectList = effector.getEffectList();
					final BuffInfo abnormalBuffInfo = effectList.getFirstBuffInfoByAbnormalType(partyBuffSkill.getAbnormalType());
					if ((abnormalBuffInfo != null) && (abnormalBuffInfo.getSkill().getAbnormalLevel() != (abnormalCount - 1)))
					{
						effectList.stopSkillEffects(SkillFinishType.REMOVED, _partyBuffSkillId);
					}
					else
					{
						SkillCaster.triggerCast(effector, target.asCreature(), partyBuffSkill);
					}
				}
			}
			else
			{
				LOGGER.warning("Skill not found effect called from " + skill);
			}
		}
		else
		{
			effector.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _partyBuffSkillId);
		}
		
		return skill.isToggle();
	}
}
