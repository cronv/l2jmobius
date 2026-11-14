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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.CancelReturnConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code DispelAll} effect.<br>
 * It removes all active buffs and debuffs from a target creature.
 * @author UnAfraid, Naker
 */
public class DispelAll extends AbstractEffect
{
	/**
	 * Removes all active effects from a target.<br>
	 * This method initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public DispelAll(StatSet params)
	{
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns the {@code DISPEL} constant from the {@code EffectType} enum.
	 * @return The {@code EffectType} of this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DISPEL;
	}
	
	/**
	 * Checks if the effect is applied instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect happens immediately.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a skill on a target.<br>
	 * This method handles the removal and potential return of skills based on configuration.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the skill, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected == null)
		{
			return;
		}
		
		if (!CancelReturnConfig.CANCEL_RETURN_ON)
		{
			normalCancel(effected);
			return;
		}
		
		if ((effector.isPlayer() && !CancelReturnConfig.CANCEL_RETURN_PLAYER) || ((effector.isMonster() || effector.isRaid()) && !CancelReturnConfig.CANCEL_RETURN_MOB))
		{
			normalCancel(effected);
			return;
		}
		
		if (!CancelReturnConfig.CANCEL_RETURN_PLAYER_OLYS && effected.isPlayer() && effected.asPlayer().isInOlympiadMode())
		{
			normalCancel(effected);
			return;
		}
		
		final List<BuffInfo> canceled = new ArrayList<>(effected.getEffectList().getEffects());
		for (BuffInfo info : canceled)
		{
			effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill());
		}
		
		ThreadPool.schedule(() ->
		{
			if ((!effected.asPlayer().isOnline()) || effected.isDead() || effected.isMonster())
			{
				return;
			}
			
			for (BuffInfo info : canceled)
			{
				final Skill sk = info.getSkill();
				final int timeLeft = info.getTime();
				
				if ((sk == null) || (timeLeft <= 0))
				{
					continue;
				}
				
				final int skId = sk.getId();
				if (effected.getEffectList().isAffectedBySkill(skId))
				{
					continue;
				}
				
				sk.applyEffects(effected, effected);
				
				final BuffInfo newInfo = effected.getEffectList().getBuffInfoBySkillId(skId);
				if (newInfo != null)
				{
					newInfo.setAbnormalTime(timeLeft);
				}
			}
			
		}, CancelReturnConfig.TIME_TO_RETURN);
	}
	
	/**
	 * Stops all active effects on a target.<br>
	 * This method excludes buffs that are marked as irreplaceable.
	 * @param effected The {@code Creature} receiving the effect removal.
	 */
	private void normalCancel(Creature effected)
	{
		effected.getEffectList().stopEffects(info -> !info.getSkill().isIrreplaceableBuff(), true, true);
	}
}
