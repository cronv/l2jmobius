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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.SummonAI;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code CallSkill} effect.<br>
 * It manages how the skill behaves when triggered in the game world.
 * @author NosBit, Liamxroy
 */
public class CallSkill extends AbstractEffect
{
	private final SkillHolder _skill;
	private final int _skillLevelScaleTo;
	private final int _chance;
	
	private final Set<Integer> _targetCheckSkillIds = new HashSet<>();
	private final int _targetCheckSkillLevel;
	private final Set<Integer> _isAffectedBySkillId = new HashSet<>();
	private final Set<Integer> _isNotAffectedBySkillId = new HashSet<>();
	private final AbnormalType _checkAbnormalType;
	
	/**
	 * Creates a new {@code CallSkill} effect.<br>
	 * This constructor initializes the skill data from a provided set of parameters.<br>
	 * It sets the skill ID, level, scale, and success chance.
	 * @param params The {@code StatSet} containing all necessary configuration values.
	 */
	public CallSkill(StatSet params)
	{
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1), params.getInt("skillSubLevel", 0));
		_skillLevelScaleTo = params.getInt("skillLevelScaleTo", 0);
		_chance = params.getInt("chance", 100);
		
		for (String skill : params.getString("targetCheckSkillId", "").split(","))
		{
			if (!skill.isEmpty())
			{
				_targetCheckSkillIds.add(Integer.parseInt(skill));
			}
		}
		
		_targetCheckSkillLevel = params.getInt("targetCheckSkillLevel", 1);
		for (String afftectedSkill : params.getString("isAffectedBySkillId", "").split(","))
		{
			if (!afftectedSkill.isEmpty())
			{
				_isAffectedBySkillId.add(Integer.parseInt(afftectedSkill));
			}
		}
		
		for (String notAfftectedSkill : params.getString("isNotAffectedBySkillId", "").split(","))
		{
			if (!notAfftectedSkill.isEmpty())
			{
				_isNotAffectedBySkillId.add(Integer.parseInt(notAfftectedSkill));
			}
		}
		
		final String checkAbnormalType = params.getString("checkAbnormalType", null);
		_checkAbnormalType = checkAbnormalType != null ? AbnormalType.getAbnormalType(checkAbnormalType) : null;
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Triggers an instant skill effect between two creatures.<br>
	 * This method handles the logic for executing a secondary skill based on specific conditions.<br>
	 * It checks for success chances and determines the correct skill level to cast.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The primary skill being used.
	 * @param item The item involved in the interaction.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((_checkAbnormalType != null) && !effected.hasAbnormalType(_checkAbnormalType))
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		if (!_targetCheckSkillIds.isEmpty())
		{
			for (Integer skillId : _targetCheckSkillIds)
			{
				final BuffInfo checkSkill = effected.getEffectList().getBuffInfoBySkillId(skillId);
				if ((checkSkill != null) && (_targetCheckSkillLevel >= checkSkill.getSkill().getLevel()))
				{
					return;
				}
			}
		}
		
		if (!_isAffectedBySkillId.isEmpty())
		{
			for (Integer affectedSkillId : _isAffectedBySkillId)
			{
				final Boolean isAffectedBySkillId = effected.getEffectList().isAffectedBySkill(affectedSkillId);
				if (!isAffectedBySkillId)
				{
					return;
				}
			}
		}
		
		if (!_isNotAffectedBySkillId.isEmpty())
		{
			for (Integer notAfftectedSkill : _isAffectedBySkillId)
			{
				final Boolean notAffectedBySkillId = effected.getEffectList().isAffectedBySkill(notAfftectedSkill);
				if (notAffectedBySkillId)
				{
					return;
				}
			}
		}
		
		final Skill triggerSkill;
		if (_skillLevelScaleTo <= 0)
		{
			// Mobius: Use 0 to trigger max effector learned skill level.
			if (_skill.getSkillLevel() == 0)
			{
				final int skillId = _skill.getSkillId();
				final int knownLevel = effector.getSkillLevel(skillId);
				if (knownLevel > 0)
				{
					triggerSkill = SkillData.getInstance().getSkill(skillId, knownLevel, _skill.getSkillSubLevel());
				}
				else
				{
					return;
				}
			}
			else
			{
				triggerSkill = _skill.getSkill();
			}
		}
		else
		{
			final int skillId = _skill.getSkillId();
			final BuffInfo buffInfo = effected.getEffectList().getBuffInfoBySkillId(skillId);
			if (buffInfo != null)
			{
				triggerSkill = SkillData.getInstance().getSkill(skillId, Math.min(_skillLevelScaleTo, buffInfo.getSkill().getLevel() + 1));
			}
			else
			{
				triggerSkill = _skill.getSkill();
			}
		}
		
		if (triggerSkill != null)
		{
			// Prevent infinite loop.
			final int skillId = skill.getId();
			final int triggerSkillLevel = triggerSkill.getLevel();
			if ((skillId == triggerSkill.getId()) && (skill.getLevel() == triggerSkillLevel))
			{
				return;
			}
			
			final int hitTime = triggerSkill.getHitTime();
			if (hitTime > 0)
			{
				if (effector.isSkillDisabled(triggerSkill))
				{
					return;
				}
				
				effector.broadcastSkillPacket(new MagicSkillUse(effector, effected, triggerSkill.getDisplayId(), triggerSkillLevel, hitTime, 0), effected);
				if (effector.isSummon()) // Age of Magic: Call skill adjustment for summons.
				{
					effector.setTarget(null); // Stop attacking.
					ThreadPool.schedule(() ->
					{
						SkillCaster.triggerCast(effector, effected, triggerSkill);
						((SummonAI) effector.getAI()).defendAttack(effected); // Continue attacking.
					}, hitTime);
				}
				else
				{
					ThreadPool.schedule(() -> SkillCaster.triggerCast(effector, effected, triggerSkill), hitTime);
				}
			}
			else
			{
				SkillCaster.triggerCast(effector, effected, triggerSkill);
			}
		}
	}
}
