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

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code TriggerSkillByTripleRange} effect.<br>
 * It handles logic for triggering a skill based on three distinct range parameters.
 * @author Mobius, fruit
 */
public class TriggerSkillByTripleRange extends AbstractEffect
{
	private final SkillHolder _closeSkill;
	private final SkillHolder _midSkill;
	private final SkillHolder _rangeSkill;
	private final int _closeDistance;
	private final int _midDistance;
	private final boolean _adjustLevel;
	
	/**
	 * Initializes a new effect that triggers different skills based on distance.<br>
	 * It uses the values provided in the {@code StatSet} to configure the ranges and skill levels.
	 * @param params The {@code StatSet} containing all configuration data for this effect.
	 */
	public TriggerSkillByTripleRange(StatSet params)
	{
		// Just use closeSkill, midSkill and rangeSkill parameters.
		_closeSkill = new SkillHolder(params.getInt("closeSkill"), params.getInt("closeSkillLevel", 1));
		_midSkill = new SkillHolder(params.getInt("midSkill"), params.getInt("midSkillLevel", 1));
		_rangeSkill = new SkillHolder(params.getInt("rangeSkill"), params.getInt("rangeSkillLevel", 1));
		_closeDistance = params.getInt("closeDistance", 120);
		_midDistance = params.getInt("rangeDistance", 360);
		_adjustLevel = params.getBoolean("adjustLevel", true);
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
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DUAL_RANGE;
	}
	
	/**
	 * Triggers a specific skill based on the distance between two creatures.<br>
	 * The method checks if the {@code effector} is a player and the {@code effected} target is not {@code null}.<br>
	 * It selects a skill from three different ranges and executes it using {@code useMagic}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The base skill used to determine levels and sub-levels.
	 * @param item The item associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || !effector.isPlayer())
		{
			return;
		}
		
		final double distance = effector.calculateDistance3D(effected);
		final SkillHolder skillHolder;
		if (distance < _closeDistance)
		{
			skillHolder = _closeSkill;
		}
		else if (distance < _midDistance)
		{
			skillHolder = _midSkill;
		}
		else
		{
			skillHolder = _rangeSkill;
		}
		
		final Skill triggerSkill = _adjustLevel ? SkillData.getInstance().getSkill(skillHolder.getSkillId(), skill.getLevel(), skill.getSubLevel()) : skillHolder.getSkill();
		if (triggerSkill == null)
		{
			return;
		}
		
		final Player caster = effector.asPlayer();
		if ((effected != effector) && effected.isPlayable() && !effected.isAutoAttackable(caster))
		{
			caster.updatePvPStatus();
		}
		
		caster.useMagic(triggerSkill, null, true, false);
	}
}
