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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for reusing a skill based on its unique identifier.<br>
 * It allows specific skills to be triggered again by checking against the {@code SkillData}.<br>
 * Use this effect when you need to bypass standard cooldowns or trigger repeated actions.
 * @author Mobius
 */
public class ReuseSkillById extends AbstractEffect
{
	private final int _skillId;
	private final int _amount;
	
	/**
	 * Creates a new {@code ReuseSkillById} effect.<br>
	 * This constructor initializes the skill ID and amount from the provided data.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ReuseSkillById(StatSet params)
	{
		_skillId = params.getInt("skillId", 0);
		_amount = params.getInt("amount", 0);
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
	 * Triggers an instant skill reuse for a player.<br>
	 * This method reduces the remaining cooldown of a specific {@code Skill}.<br>
	 * It checks if the {@code effector} is a {@code Player} and has the required skill level.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} object being reused.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effector.asPlayer();
		if (player != null)
		{
			final Skill s = player.getKnownSkill(_skillId);
			if (s != null)
			{
				if (_amount > 0)
				{
					final long reuse = player.getSkillRemainingReuseTime(s.getReuseHashCode());
					if (reuse > 0)
					{
						player.removeTimeStamp(s);
						player.addTimeStamp(s, Math.max(0, reuse - _amount));
						player.sendPacket(new SkillCoolTime(player));
					}
				}
				else
				{
					player.removeTimeStamp(s);
					player.enableSkill(s);
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
	}
}
