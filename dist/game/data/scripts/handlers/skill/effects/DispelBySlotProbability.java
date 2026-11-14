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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.CancelReturnConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements an effect that dispels buffs based on a specific slot probability.<br>
 * It determines whether to remove an effect from a creature by checking a random chance against the defined probability.
 * @author Adry_85, Zoey76, Naker
 */
public class DispelBySlotProbability extends AbstractEffect
{
	private final Set<AbnormalType> _dispelAbnormals;
	private final int _rate;
	
	/**
	 * Creates a new effect that dispels specific status types based on a probability.<br>
	 * It reads the required abnormal types and the success rate from the provided configuration.
	 * @param params The {@code StatSet} containing the "dispel" list and "rate" value.
	 */
	public DispelBySlotProbability(StatSet params)
	{
		final String[] dispelEffects = params.getString("dispel").split(";");
		_rate = params.getInt("rate", 100);
		_dispelAbnormals = new HashSet<>(dispelEffects.length);
		
		for (String slot : dispelEffects)
		{
			_dispelAbnormals.add(Enum.valueOf(AbnormalType.class, slot));
		}
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
	 * Applies the instant effect of a skill to a target creature.<br>
	 * This method handles logic for canceling and potentially returning buffs based on configuration.<br>
	 * It checks various conditions like raid status, olympiad mode, and cancel settings before processing.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the skill, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || effected.isRaid())
		{
			return;
		}
		
		if (!CancelReturnConfig.CANCEL_RETURN_ON)
		{
			normalCancel(effected);
			return;
		}
		
		if ((effector.isPlayer() && !CancelReturnConfig.CANCEL_RETURN_PLAYER) || ((effector.isMonster() && !CancelReturnConfig.CANCEL_RETURN_MOB) || ((effector.isRaid()) && !CancelReturnConfig.CANCEL_RETURN_MOB)))
		{
			normalCancel(effected);
			return;
		}
		
		if (!CancelReturnConfig.CANCEL_RETURN_PLAYER_OLYS && effected.isPlayer() && effected.asPlayer().isInOlympiadMode())
		{
			normalCancel(effected);
			return;
		}
		
		final EffectList effectList = effected.getEffectList();
		final List<BuffInfo> canceled = new ArrayList<>();
		
		effectList.stopEffects(info ->
		{
			final boolean cancel = !info.getSkill().isIrreplaceableBuff() && (Rnd.get(100) < _rate) && _dispelAbnormals.contains(info.getSkill().getAbnormalType());
			if (cancel)
			{
				canceled.add(info);
			}
			return cancel;
		}, true, true);
		
		if (canceled.isEmpty())
		{
			return;
		}
		
		ThreadPool.schedule(() ->
		{
			if ((!effected.asPlayer().isOnline()) || effected.isDead() || effected.isMonster())
			{
				return;
			}
			
			for (BuffInfo oldInfo : canceled)
			{
				final Skill sk = oldInfo.getSkill();
				final int timeLeft = oldInfo.getTime();
				if ((sk == null) || (timeLeft <= 0))
				{
					continue;
				}
				
				if (effectList.isAffectedBySkill(sk.getId()))
				{
					continue;
				}
				
				sk.applyEffects(effected, effected);
				
				final BuffInfo newInfo = effectList.getBuffInfoBySkillId(sk.getId());
				if (newInfo != null)
				{
					newInfo.setAbnormalTime(timeLeft);
				}
			}
			
		}, CancelReturnConfig.TIME_TO_RETURN);
	}
	
	/**
	 * Cancels specific effects on a target creature.<br>
	 * It checks if the effect is replaceable and matches the required abnormal type.<br>
	 * The cancellation happens based on a random chance.
	 * @param effected The {@code Creature} that will have its effects removed.
	 */
	private void normalCancel(Creature effected)
	{
		effected.getEffectList().stopEffects(info -> !info.getSkill().isIrreplaceableBuff() && (Rnd.get(100) < _rate) && _dispelAbnormals.contains(info.getSkill().getAbnormalType()), true, true);
	}
}
