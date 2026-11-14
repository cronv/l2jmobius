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
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code CpHealOverTime} effect.<br>
 * It manages the logic for healing a target over a period of time.
 */
public class CpHealOverTime extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code CpHealOverTime} effect instance.<br>
	 * This constructor initializes the healing power and tick count from the provided parameters.<br>
	 * It ensures that the {@code power} key is used instead of {@code amount}.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public CpHealOverTime(StatSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Handles the periodic update of the CP heal over time effect.<br>
	 * This method checks if the {@code effected} creature is alive and has room for CP changes.<br>
	 * It calculates the new CP value based on the effect power and any active items.<br>
	 * If the update is successful, it applies the change and returns {@code true}.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill that triggered this effect.
	 * @param item The item used, if any.
	 * @return {@code true} if the CP was updated successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		double cp = effected.getCurrentCp();
		final double maxcp = effected.getMaxRecoverableCp();
		
		// Not needed to set the CP and send update packet if player is already at max CP.
		if (_power > 0)
		{
			if (cp >= maxcp)
			{
				return false;
			}
		}
		else
		{
			if ((cp - _power) <= 0)
			{
				return false;
			}
		}
		
		double power = _power;
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			power += effected.getStat().getValue(Stat.ADDITIONAL_POTION_CP, 0) / getTicks();
		}
		
		cp += power * getTicksMultiplier();
		if (_power > 0)
		{
			cp = Math.min(cp, maxcp);
		}
		else
		{
			cp = Math.max(cp, 1);
		}
		
		effected.setCurrentCp(cp, false);
		effected.broadcastStatusUpdate(effector);
		return true;
	}
}
