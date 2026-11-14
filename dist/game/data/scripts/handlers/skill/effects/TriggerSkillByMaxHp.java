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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a skill based on a creature's maximum health.<br>
 * It checks if the {@code maxHp} requirement is met before executing the associated effect.
 * @author Mobius
 */
public class TriggerSkillByMaxHp extends AbstractEffect
{
	private final int _skillId;
	private final int _skillLevel;
	private final long _from;
	private final long _to;
	
	/**
	 * Initializes a new effect that triggers a skill based on maximum health.<br>
	 * It extracts the required parameters from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration values like {@code skillId}, {@code skillLevel}, {@code from}, and {@code to}.
	 */
	public TriggerSkillByMaxHp(StatSet params)
	{
		_skillId = params.getInt("skillId", 0);
		_skillLevel = params.getInt("skillLevel", 1);
		_from = params.getLong("from", 0);
		_to = params.getLong("to", Long.MAX_VALUE);
	}
	
	/**
	 * This method handles the logic when the effect starts.<br>
	 * It checks if the {@code effected} creature's maximum HP falls within a specific range.<br>
	 * If it does, it triggers a skill on the target.<br>
	 * Otherwise, it removes the skill effect from the target.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Delay so that HP bonuses will be calculated first.
		ThreadPool.schedule(() ->
		{
			final long hpMax = effected.getMaxHp();
			if ((hpMax >= _from) && (hpMax <= _to))
			{
				if (!effected.isAffectedBySkill(_skillId))
				{
					SkillCaster.triggerCast(effected, effected, SkillData.getInstance().getSkill(_skillId, _skillLevel));
				}
			}
			else
			{
				effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _skillId);
			}
		}, 100);
	}
}
