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
import org.l2jmobius.gameserver.entity.actor.holders.creature.EffectList;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles effects for skills that have a limited number of uses.<br>
 * It tracks the remaining charges of a skill and manages their consumption.
 * @author Ofelin
 */
public class TrackLimitedSkill extends AbstractEffect
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
	private int limitAeoreLevel = 0;
	private int limitSigelLevel = 0;
	private int limitIssLevel = 0;
	
	/**
	 * Creates a new instance of {@code TrackLimitedSkill}.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param param The {@code StatSet} used to configure the skill limits.
	 */
	public TrackLimitedSkill(StatSet param)
	{
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It initializes the tracking for specific skill limits.
	 * @param effector The {@code Creature} who cast the skill.
	 * @param effected The {@code Creature} who received the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 * @param item The {@code Item} used to cast the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		trackAeoreLimit(effector, effected, LIMIT_OF_AEORE); // Tracking Aeore Limit Debuff
		trackSigelLimit(effector, effected, LIMIT_OF_SIGEL); // Tracking Sigel Limit Debuff
		trackIssLimit(effector, effected, LIMIT_OF_ISS); // Tracking Iss Limit Debuff
	}
	
	/**
	 * Handles the logic when a skill effect ends.<br>
	 * This method updates specific limits based on the {@code Skill} ID.<br>
	 * It determines which limit to increase by checking the skill type.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The skill being processed.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final int skillId = skill.getId();
		switch (skillId)
		{
			case BATTLE_RAPSODY:
			case OVERLORDS_DIGNITY:
			{
				increaseLimit(effector, effected, skill, limitIssLevel);
				break;
			}
			case PROTECTION_OF_FATE:
			case NINE_AEGIS:
			{
				increaseLimit(effector, effected, skill, limitSigelLevel);
				break;
			}
			case CELESTIAL_PROTECTION:
			case CELESTIAL_PARTY_PROTECTION:
			{
				increaseLimit(effector, effected, skill, limitAeoreLevel);
				break;
			}
		}
	}
	
	/**
	 * Updates the level of a specific skill and manages related effects.<br>
	 * This method checks for a debuff on the {@code effected} creature.<br>
	 * It removes certain protections if the skill level reaches 3.<br>
	 * Otherwise, it applies the corresponding skill effect based on the current level.
	 * @param effector The creature casting or initiating the action.
	 * @param effected The creature receiving the effect or debuff.
	 * @param limitSkillId The unique identifier of the skill to track.
	 */
	private void trackAeoreLimit(Creature effector, Creature effected, int limitSkillId)
	{
		limitAeoreLevel = 0;
		final EffectList effectList = effected.getEffectList();
		for (BuffInfo debuff : effectList.getDebuffs())
		{
			if (debuff.getSkill().getId() == limitSkillId)
			{
				limitAeoreLevel = debuff.getSkill().getLevel();
				
				if (limitAeoreLevel == 3)
				{
					effected.getEffectList().remove(effectList.getBuffInfoBySkillId(CELESTIAL_PROTECTION), SkillFinishType.NORMAL, false, false); // Remove Celestial Protection
					effected.getEffectList().remove(effectList.getBuffInfoBySkillId(CELESTIAL_PARTY_PROTECTION), SkillFinishType.NORMAL, false, false); // Remove Celestial Party Protection
				}
				else
				{
					new SkillHolder(LIMIT_OF_AEORE, limitAeoreLevel).getSkill().applyEffects(effector, effected);
				}
			}
		}
	}
	
	/**
	 * Tracks the level of a specific Sigel skill on a target.<br>
	 * It removes certain buffs if the skill reaches level 3.<br>
	 * Otherwise, it applies the corresponding Sigel effect based on the current level.
	 * @param effector The creature casting or applying the effect.
	 * @param effected The creature receiving the effect.
	 * @param limitSkillId The unique ID of the skill to track.
	 */
	private void trackSigelLimit(Creature effector, Creature effected, int limitSkillId)
	{
		limitSigelLevel = 0;
		final EffectList effectList = effected.getEffectList();
		for (BuffInfo debuff : effectList.getDebuffs())
		{
			if (debuff.getSkill().getId() == limitSkillId)
			{
				limitSigelLevel = debuff.getSkill().getLevel();
				
				if (limitSigelLevel == 3)
				{
					effected.getEffectList().remove(effectList.getBuffInfoBySkillId(PROTECTION_OF_FATE), SkillFinishType.NORMAL, false, false); // Remove Protection of Fate
					effected.getEffectList().remove(effectList.getBuffInfoBySkillId(NINE_AEGIS), SkillFinishType.NORMAL, false, false); // Remove Nine Aegis
				}
				else
				{
					new SkillHolder(LIMIT_OF_SIGEL, limitSigelLevel).getSkill().applyEffects(effector, effected);
				}
			}
		}
	}
	
	/**
	 * Tracks the level of a specific skill to manage limited effects.<br>
	 * It checks for debuffs on the {@code effected} creature.<br>
	 * If the skill reaches level {@code 3}, it removes specific buffs.<br>
	 * Otherwise, it applies the corresponding effect based on the current level.
	 * @param effector The creature casting or initiating the action.
	 * @param effected The creature receiving the effects.
	 * @param limitSkillId The unique identifier of the skill to track.
	 */
	private void trackIssLimit(Creature effector, Creature effected, int limitSkillId)
	{
		limitIssLevel = 0;
		final EffectList effectList = effected.getEffectList();
		for (BuffInfo debuff : effectList.getDebuffs())
		{
			if (debuff.getSkill().getId() == limitSkillId)
			{
				limitIssLevel = debuff.getSkill().getLevel();
				if (limitIssLevel == 3)
				{
					effected.getEffectList().remove(effectList.getBuffInfoBySkillId(BATTLE_RAPSODY), SkillFinishType.NORMAL, false, false); // Remove Battle Rhapsody
					effected.getEffectList().remove(effectList.getBuffInfoBySkillId(OVERLORDS_DIGNITY), SkillFinishType.NORMAL, false, false); // Remove Overlord's Dignity
				}
				else
				{
					new SkillHolder(LIMIT_OF_ISS, limitIssLevel).getSkill().applyEffects(effector, effected);
				}
			}
		}
	}
	
	/**
	 * Increases the internal limit level for specific skills.<br>
	 * This method applies new effects based on the skill type and current level.<br>
	 * It checks if the {@code limitLevel} is less than {@code 3}.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the increase.
	 * @param limitLevel The current level of the limit to be incremented.
	 */
	private void increaseLimit(Creature effector, Creature effected, Skill skill, int limitLevel)
	{
		if (limitLevel < 3)
		{
			switch (skill.getId())
			{
				case BATTLE_RAPSODY: // Battle Rhapsody
				case OVERLORDS_DIGNITY: // Overlord's Dignity
				{
					// limitIssLevel++;
					new SkillHolder(LIMIT_OF_ISS, ++limitIssLevel).getSkill().applyEffects(effector, effected);
					break;
				}
				case PROTECTION_OF_FATE: // Protection of Fate
				case NINE_AEGIS: // Nine Aegis
				{
					new SkillHolder(LIMIT_OF_SIGEL, ++limitSigelLevel).getSkill().applyEffects(effector, effected);
					break;
				}
				case CELESTIAL_PROTECTION: // Celestial Protection
				case CELESTIAL_PARTY_PROTECTION: // Celestial Party Protection
				{
					new SkillHolder(LIMIT_OF_AEORE, ++limitAeoreLevel).getSkill().applyEffects(effector, effected);
					break;
				}
			}
		}
	}
}
