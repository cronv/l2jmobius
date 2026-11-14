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
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for limiting the number of times a {@code Skill} can be used.<br>
 * It ensures that specific skills are restricted based on predefined constraints.
 * @author Ofelin
 */
public class LimitSkill extends AbstractEffect
{
	private static final int LIMIT_OF_AEORE = 11833;
	private static final int LIMIT_OF_SIGEL = 19526;
	private static final int LIMIT_OF_ISS = 19527;
	private static final int BATTLE_RAPSODY = 11544;
	private static final int OVERLORDS_DIGNITY = 19439;
	private static final int PROTECTION_OF_FATE = 10019;
	private static final int NINE_AEGIS = 10024;
	private static final int CELESTIAL_PROTECTION = 11758;
	private static final int CELESTIAL_PARTY_PROTECTION = 11759;
	
	/**
	 * Creates a new instance of {@code LimitSkill}.<br>
	 * This constructor initializes the skill using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public LimitSkill(StatSet params)
	{
	}
	
	/**
	 * Handles the logic when a {@code LimitSkill} effect begins.<br>
	 * It checks the {@code Skill} ID and applies specific duration reductions.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		switch (skill.getId())
		{
			case LIMIT_OF_AEORE: // Limit of Aeore
			{
				decreaseAeoreBuffDuration(effector, effected, skill);
				break;
			}
			case LIMIT_OF_SIGEL: // Limit of Sigel
			{
				decreaseSigelBuffDuration(effector, effected, skill);
				break;
			}
			case LIMIT_OF_ISS: // Limit of Iss
			{
				decreaseIssBuffDuration(effector, effected, skill);
				break;
			}
		}
	}
	
	/**
	 * Reduces the duration of specific buffs on a target.<br>
	 * This method handles logic for {@code CELESTIAL_PROTECTION} and {@code CELESTIAL_PARTY_PROTECTION}.<br>
	 * It applies a reduction based on the level of the {@code Skill}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the duration decrease.
	 */
	private void decreaseAeoreBuffDuration(Creature effector, Creature effected, Skill skill)
	{
		switch (skill.getLevel())
		{
			case 1:
			case 2:
			{
				modifyDuration(CELESTIAL_PROTECTION, effected, (int) (10 * 0.50)); // Decrease active effect of Celestial Protection by 50%
				modifyDuration(CELESTIAL_PARTY_PROTECTION, effected, (int) (10 * 0.50)); // Decrease active effect of Celestial Party Protection by 50%
				break;
			}
		}
	}
	
	/**
	 * Reduces the remaining duration of specific buffs on a target.<br>
	 * This method handles logic for skills like Protection of Fate and Nine Aegis.<br>
	 * The reduction amount depends on the level of the {@code skill}.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to calculate reductions.
	 */
	private void decreaseSigelBuffDuration(Creature effector, Creature effected, Skill skill)
	{
		switch (skill.getLevel())
		{
			case 1:
			{
				modifyDuration(PROTECTION_OF_FATE, effected, (int) (30 * 0.80)); // Decrease active effect of Protection of Fate by 20%
				modifyDuration(NINE_AEGIS, effected, (int) (30 * 0.80)); // Decrease active effect of Nine Aegis by 20%
				break;
			}
			case 2:
			{
				modifyDuration(PROTECTION_OF_FATE, effected, (int) (30 * 0.20)); // Decrease active effect of Protection of Fate by 80%
				modifyDuration(NINE_AEGIS, effected, (int) (30 * 0.20)); // Decrease active effect of Nine Aegis by 80%
				break;
			}
		}
	}
	
	/**
	 * Reduces the duration of specific buffs on a target creature.<br>
	 * This method handles logic for skills like Battle Rhapsody and Overlord's Dignity.<br>
	 * The reduction amount depends on the level of the {@code Skill}.
	 * @param effector The creature who performed the action.
	 * @param effected The creature receiving the duration reduction.
	 * @param skill The skill being used to trigger the effect.
	 */
	private void decreaseIssBuffDuration(Creature effector, Creature effected, Skill skill)
	{
		switch (skill.getLevel())
		{
			case 1:
			{
				modifyDuration(BATTLE_RAPSODY, effected, (int) (30 * 0.80)); // Decrease active effect of Battle Rhapsody by 20%
				modifyDuration(OVERLORDS_DIGNITY, effected, (int) (30 * 0.80)); // Decrease active effect of Overlord's Dignity by 20%
				break;
			}
			case 2:
			{
				modifyDuration(BATTLE_RAPSODY, effected, (int) (30 * 0.20)); // Decrease active effect of Battle Rhapsody by 80%
				modifyDuration(OVERLORDS_DIGNITY, effected, (int) (30 * 0.20)); // Decrease active effect of Overlord's Dignity by 80%
				break;
			}
		}
	}
	
	/**
	 * Updates the remaining duration of a specific skill effect on a target.<br>
	 * This method checks all active effects on the {@code effected} creature.<br>
	 * It sets the new time if the {@code skillId} matches and the {@code duration} is greater than 0.
	 * @param skillId The unique identifier of the skill to modify.
	 * @param effected The {@code Creature} receiving the effect modification.
	 * @param duration The new duration value to apply to the effect.
	 */
	private void modifyDuration(int skillId, Creature effected, int duration)
	{
		for (BuffInfo buff : effected.getEffectList().getEffects())
		{
			if ((buff.getSkill().getId() == skillId) && (duration > 0))
			{
				buff.setAbnormalTime(duration);
			}
		}
	}
}
