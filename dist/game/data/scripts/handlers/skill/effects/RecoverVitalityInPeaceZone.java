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
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for recovering vitality when a player is inside a peace zone.<br>
 * This {@code RecoverVitalityInPeaceZone} effect ensures that players receive health regeneration based on specific zone rules.
 * @author Mobius
 */
public class RecoverVitalityInPeaceZone extends AbstractEffect
{
	private final double _amount;
	private final int _ticks;
	
	/**
	 * This constructor initializes the effect with specific values.<br>
	 * It retrieves the recovery amount and tick count from the provided data.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public RecoverVitalityInPeaceZone(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
		_ticks = params.getInt("ticks", 10);
	}
	
	/**
	 * Retrieves the total number of ticks for this effect.<br>
	 * This value represents how many times the effect will trigger.
	 * @return The number of {@code int} ticks.
	 */
	@Override
	public int getTicks()
	{
		return _ticks;
	}
	
	/**
	 * Handles the periodic logic for this effect.<br>
	 * It checks if the target is a player inside a peace zone.<br>
	 * If valid, it increases the player's vitality points.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill that triggered this effect.
	 * @param item The item associated with the skill.
	 * @return {@code true} if the skill is a toggle, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == null) || effected.isDead() || !effected.isPlayer() || !effected.isInsideZone(ZoneId.PEACE))
		{
			return false;
		}
		
		final Player target = effected.asPlayer();
		long vitality = target.getVitalityPoints();
		vitality += _amount;
		if (vitality >= PlayerStat.MAX_VITALITY_POINTS)
		{
			vitality = PlayerStat.MAX_VITALITY_POINTS;
		}
		
		target.setVitalityPoints((int) vitality, true);
		
		return skill.isToggle();
	}
	
	/**
	 * This method is called when the effect expires.<br>
	 * It checks if the {@code effected} creature is a {@code Player}.<br>
	 * If valid, it increases the player's vitality by a specific amount.<br>
	 * The new vitality value is capped at the maximum allowed limit.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if ((effected != null) && effected.isPlayer())
		{
			final BuffInfo info = effected.getEffectList().getBuffInfoBySkillId(skill.getId());
			if ((info != null) && !info.isRemoved())
			{
				final Player target = effected.asPlayer();
				long vitality = target.getVitalityPoints();
				vitality += _amount * 100;
				if (vitality >= PlayerStat.MAX_VITALITY_POINTS)
				{
					vitality = PlayerStat.MAX_VITALITY_POINTS;
				}
				
				target.setVitalityPoints((int) vitality, true);
			}
		}
	}
}
