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
 * This class handles the {@code TriggerSkillByDualRange} effect.<br>
 * It manages skill triggers based on a dual range requirement.<br>
 * It is used to process specific skill mechanics within the game engine.
 * @author Mobius
 */
public class TriggerSkillByDualRange extends AbstractEffect
{
	private final SkillHolder _closeSkill;
	private final SkillHolder _rangeSkill;
	private final int _distance;
	private final boolean _adjustLevel;
	
	/**
	 * Initializes a new effect that triggers different skills based on distance.<br>
	 * It uses the provided {@code StatSet} to configure the skill types and range.
	 * @param params The {@code StatSet} containing all required configuration values.
	 */
	public TriggerSkillByDualRange(StatSet params)
	{
		// Just use closeSkill and rangeSkill parameters.
		_closeSkill = new SkillHolder(params.getInt("closeSkill"), params.getInt("closeSkillLevel", 1));
		_rangeSkill = new SkillHolder(params.getInt("rangeSkill"), params.getInt("rangeSkillLevel", 1));
		_distance = params.getInt("distance", 120);
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
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.DUAL_RANGE;
	}
	
	/**
	 * Triggers a skill based on the distance between two creatures.<br>
	 * It selects a skill from either a close or long range holder.<br>
	 * The method also updates PvP status if necessary.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} being targeted.
	 * @param skill The base {@code Skill} used to determine level and sub-level.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || !effector.isPlayer())
		{
			return;
		}
		
		final SkillHolder skillHolder = effector.calculateDistance3D(effected) < _distance ? _closeSkill : _rangeSkill;
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
		
		caster.useMagic(triggerSkill, null, true, triggerSkill.getCastRange() > 600);
	}
}
