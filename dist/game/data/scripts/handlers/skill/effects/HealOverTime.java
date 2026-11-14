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
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.ExRegenMax;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for a heal over time effect.<br>
 * It gradually restores health to a {@code Creature} over a specified duration.
 */
public class HealOverTime extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code HealOverTime} effect.<br>
	 * This constructor initializes the healing power and duration from a {@code StatSet}.<br>
	 * It will throw an exception if the {@code amount} key is present instead of {@code power}.
	 * @param params The {@code StatSet} containing configuration values like {@code power} and {@code ticks}.
	 */
	public HealOverTime(StatSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Handles the periodic application of a heal or damage effect over time.<br>
	 * This method checks if the target is eligible to receive the effect.<br>
	 * It calculates the new health value based on the effect power and item bonuses.
	 * @param effector The {@code Creature} who initiated the effect.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} involved in the action, if any.
	 * @return {@code true} if the skill is a toggle, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor())
		{
			return false;
		}
		
		double hp = effected.getCurrentHp();
		final double maxhp = effected.getMaxRecoverableHp();
		
		// Not needed to set the HP and send update packet if player is already at max HP.
		if (_power > 0)
		{
			if (hp >= maxhp)
			{
				return false;
			}
		}
		else
		{
			if ((hp - _power) <= 0)
			{
				return false;
			}
		}
		
		double power = _power;
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			power += effected.getStat().getValue(Stat.ADDITIONAL_POTION_HP, 0) / getTicks();
		}
		
		hp += power * getTicksMultiplier();
		if (_power > 0)
		{
			hp = Math.min(hp, maxhp);
		}
		else
		{
			hp = Math.max(hp, 1);
		}
		
		effected.setCurrentHp(hp, false);
		effected.broadcastStatusUpdate(effector);
		return skill.isToggle();
	}
	
	/**
	 * Handles the initial logic when a {@code HealOverTime} effect begins.<br>
	 * It calculates the healing power and sends a regeneration packet to the player.<br>
	 * This method checks for specific conditions like skill types and item bonuses.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to create the effect.
	 * @param item The {@code Item} involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer() && (getTicks() > 0) && (skill.getAbnormalType() == AbnormalType.HP_RECOVER))
		{
			final int ticks = getTicks();
			double power = _power;
			if ((item != null) && (item.isPotion() || item.isElixir()))
			{
				final double bonus = effected.getStat().getValue(Stat.ADDITIONAL_POTION_HP, 0);
				if (bonus > 0)
				{
					power += bonus / ticks;
				}
			}
			
			effected.sendPacket(new ExRegenMax(skill.getAbnormalTime(), ticks, power));
		}
	}
}
