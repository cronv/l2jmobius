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

import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.custom.CancelReturnConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.DispelSlotType;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for removing effects based on a specific category.<br>
 * It allows skills to dispel multiple buffs or debuffs at once by filtering them by type.
 * @author DS, Adry_85, Naker
 */
public class DispelByCategory extends AbstractEffect
{
	private final DispelSlotType _slot;
	private final int _rate;
	private final int _max;
	
	/**
	 * Creates a new instance of the {@code DispelByCategory} effect.<br>
	 * This constructor initializes the dispel settings from a configuration set.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public DispelByCategory(StatSet params)
	{
		_slot = params.getEnum("slot", DispelSlotType.class, DispelSlotType.BUFF);
		_rate = params.getInt("rate", 0);
		_max = params.getInt("max", 0);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DISPEL;
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for a skill dispel.<br>
	 * This method handles the logic for removing and potentially returning skills based on configuration.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((skill == null) || (effected == null) || effected.isDead() || effected.isRaid())
		{
			return;
		}
		
		if (!CancelReturnConfig.CANCEL_RETURN_ON)
		{
			normalCancel(effector, effected, skill);
			return;
		}
		
		if ((effector.isPlayer() && !CancelReturnConfig.CANCEL_RETURN_PLAYER) || ((effector.isMonster() || effector.isRaid()) && !CancelReturnConfig.CANCEL_RETURN_MOB))
		{
			normalCancel(effector, effected, skill);
			return;
		}
		
		if (!CancelReturnConfig.CANCEL_RETURN_PLAYER_OLYS && effected.isPlayer() && effected.asPlayer().isInOlympiadMode())
		{
			normalCancel(effector, effected, skill);
			return;
		}
		
		final List<BuffInfo> canceled = Formulas.calcCancelStealEffects(effector, effected, skill, _slot, _rate, _max);
		if (canceled.isEmpty())
		{
			return;
		}
		
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
	 * Handles the removal of skill effects from a target.<br>
	 * This method calculates which buffs should be canceled based on specific rules.<br>
	 * It then stops those effects on the affected creature.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the cancellation.
	 */
	private void normalCancel(Creature effector, Creature effected, Skill skill)
	{
		final List<BuffInfo> canceled = Formulas.calcCancelStealEffects(effector, effected, skill, _slot, _rate, _max);
		for (BuffInfo info : canceled)
		{
			effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, info.getSkill());
		}
	}
}
