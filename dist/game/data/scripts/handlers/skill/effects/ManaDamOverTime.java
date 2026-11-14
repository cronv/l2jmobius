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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the application of mana damage over time to a target.<br>
 * This {@code ManaDamOverTime} class manages how mana is depleted continuously during an active effect.
 */
public class ManaDamOverTime extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code ManaDamOverTime} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the damage power and the number of ticks.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public ManaDamOverTime(StatSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * This method handles the periodic mana damage logic for this effect.<br>
	 * It checks if the target is alive before applying damage.<br>
	 * If a toggle skill lacks enough mana, it sends a notification and stops.<br>
	 * Otherwise, it reduces the target's current mana.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the action.
	 * @return {@code true} if the skill is a toggle and continues, {@code false} otherwise.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		final double manaDam = _power * getTicksMultiplier();
		if ((manaDam > effected.getCurrentMp()) && skill.isToggle())
		{
			effected.sendPacket(SystemMessageId.YOUR_SKILL_WAS_DEACTIVATED_DUE_TO_LACK_OF_MP);
			return false;
		}
		
		effected.reduceCurrentMp(manaDam);
		return skill.isToggle();
	}
}
