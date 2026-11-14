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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for triggering a {@code Skill} based on specific character stats.<br>
 * It checks if the required conditions are met to activate an effect automatically.
 * @author Mobius
 */
public class TriggerSkillByStat extends AbstractEffect
{
	private final Stat _stat;
	private final int _skillId;
	private final int _skillLevel;
	private final int _skillSubLevel;
	private final int _min;
	private final int _max;
	
	/**
	 * Creates a new effect that triggers a skill based on a specific stat.<br>
	 * This constructor initializes the required stats and skill parameters from a {@code StatSet}.<br>
	 * It validates that the minimum value is a positive number.
	 * @param params The {@code StatSet} containing all configuration values for this effect.
	 */
	public TriggerSkillByStat(StatSet params)
	{
		_stat = params.getEnum("stat", Stat.class);
		_skillId = params.getInt("skillId", 0);
		_skillLevel = params.getInt("skillLevel", 1);
		_skillSubLevel = params.getInt("skillSubLevel", 0);
		_min = params.getInt("min", 1);
		_max = params.getInt("max", 9999999);
		
		if (_min < 1)
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " minimum should be a positive number.");
		}
	}
	
	/**
	 * Checks if the effect should be delayed before pumping.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the pump is not delayed.
	 */
	@Override
	public boolean delayPump()
	{
		return true;
	}
	
	/**
	 * This method triggers a specific skill on a target creature.<br>
	 * It checks if the creature meets the required stat range.<br>
	 * It also ensures the creature is not already at the correct skill level.
	 * @param effected The {@code Creature} that will receive the effect.
	 * @param skill The {@code Skill} to be triggered.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		if (effected == null)
		{
			return;
		}
		
		final int currentValue = (int) effected.getStat().getValue(_stat);
		if ((currentValue < _min) || (currentValue > _max))
		{
			return;
		}
		
		final int level = effected.getAffectedSkillLevel(_skillId);
		if (level == _skillLevel)
		{
			return;
		}
		
		// if (level > 0)
		// {
		// effected.getEffectList().stopSkillEffects(SkillFinishType.SILENT, _skillId);
		// }
		
		ThreadPool.execute(() -> SkillCaster.triggerCast(effected, effected, SkillData.getInstance().getSkill(_skillId, _skillLevel, _skillSubLevel)));
	}
	
	/**
	 * Handles the logic when an effect finishes.<br>
	 * This method stops a specific skill effect on the target creature.
	 * @param effector The {@code Creature} that triggered the effect.
	 * @param effected The {@code Creature} who is currently affected by the effect.
	 * @param skill The {@code Skill} associated with this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, _skillId);
	}
}
