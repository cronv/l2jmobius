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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a {@code Skill} based on the type of target hit.<br>
 * It allows specific skills to activate only when they interact with certain types of entities.
 * @author Liamxroy
 */
public class TriggerSkillByTargetType extends AbstractEffect
{
	private final SkillHolder _allySkill;
	private final SkillHolder _enemySkill;
	
	/**
	 * Creates a new effect that triggers skills based on target types.<br>
	 * It initializes the required ally and enemy skills from the provided parameters.
	 * @param params The {@code StatSet} containing skill IDs and levels.
	 */
	public TriggerSkillByTargetType(StatSet params)
	{
		_allySkill = new SkillHolder(params.getInt("allySkill"), params.getInt("allySkillLevel", 1));
		_enemySkill = new SkillHolder(params.getInt("enemySkill"), params.getInt("enemySkillLevel", 1));
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
	 * Triggers a specific skill immediately between two creatures.<br>
	 * This method checks if the {@code effector} is a player and the {@code effected} target is valid.<br>
	 * It determines which skill to cast based on whether the target is an ally or an enemy.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used for this interaction.
	 * @param item The item associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || !effector.isPlayer())
		{
			return;
		}
		
		final Skill triggerSkill = effected.isAutoAttackable(effector) ? _enemySkill.getSkill() : _allySkill.getSkill();
		if (triggerSkill == null)
		{
			return;
		}
		
		SkillCaster.triggerCast(effector, effected, triggerSkill);
	}
}
