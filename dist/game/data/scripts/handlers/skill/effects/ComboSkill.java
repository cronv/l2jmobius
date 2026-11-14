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
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.MagicSkillUse;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for combo skill effects.<br>
 * It manages how skills interact when used in a sequence.<br>
 * It extends {@code AbstractEffect} to provide specific behavior for these mechanics.
 * @author Prink
 */
public class ComboSkill extends AbstractEffect
{
	private final SkillHolder _skill;
	private final int _checkSkill;
	private final int _checkSkill2;
	private final String _mode;
	private final int _chance;
	
	/**
	 * Creates a new {@code ComboSkill} instance.<br>
	 * This constructor initializes the skill properties from a {@code StatSet}.<br>
	 * It sets values for skills, modes, and chances.
	 * @param params The {@code StatSet} containing all required configuration data.
	 */
	public ComboSkill(StatSet params)
	{
		_skill = new SkillHolder(params.getInt("skillId"), params.getInt("skillLevel", 1), params.getInt("skillSubLevel", 0));
		_checkSkill = params.getInt("targetCheckSkillId", 0);
		_checkSkill2 = params.getInt("targetCheckSkillId2", 0);
		_mode = params.getString("mode", "DUAL");
		_chance = params.getInt("chance", 100);
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
	 * Triggers a combo skill effect based on specific conditions.<br>
	 * This method checks if the target is affected by required skills and handles random chances.<br>
	 * It then executes the resulting skill cast either immediately or after a delay.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} currently being used by the player.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Skill triggerSkill = _skill.getSkill();
		
		switch (_mode)
		{
			case "DUAL":
			{
				if (!effected.isAffectedBySkill(_checkSkill))
				{
					return;
				}
				break;
			}
			case "TRIPLE":
			{
				final boolean affected = effected.isAffectedBySkill(_checkSkill);
				final boolean affected2 = effected.isAffectedBySkill(_checkSkill2);
				if (((!affected && !affected2) || (!affected && affected2) || (affected && !affected2)))
				{
					return;
				}
				break;
			}
		}
		
		if ((_chance < 100) && (Rnd.get(100) >= _chance))
		{
			return;
		}
		
		if (triggerSkill == null)
		{
			return;
		}
		
		final int triggerSkillLevel = triggerSkill.getLevel();
		if ((skill.getId() == triggerSkill.getId()) && (skill.getLevel() == triggerSkillLevel))
		{
			return;
		}
		
		final int hitTime = triggerSkill.getHitTime();
		if (hitTime > 0)
		{
			if (effector.isSkillDisabled(triggerSkill))
			{
				return;
			}
			
			effector.broadcastSkillPacket(new MagicSkillUse(effector, effected, triggerSkill.getDisplayId(), triggerSkillLevel, hitTime, 0), effected);
			ThreadPool.schedule(() -> SkillCaster.triggerCast(effector, effected, triggerSkill), hitTime);
		}
		else
		{
			SkillCaster.triggerCast(effector, effected, triggerSkill);
		}
	}
}
