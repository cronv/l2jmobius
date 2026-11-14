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
 * This class implements an effect that consumes {@code MP} based on the character's level.<br>
 * It calculates the cost dynamically to scale with the player's progression.
 */
public class MpConsumePerLevel extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new instance of the {@code MpConsumePerLevel} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the power and tick duration based on the provided parameters.
	 * @param params The {@code StatSet} containing configuration values for this effect.
	 */
	public MpConsumePerLevel(StatSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * This method handles the periodic MP consumption of a skill effect.<br>
	 * It checks if the target is alive and has enough MP to continue.<br>
	 * If the requirements are met, it reduces the target's current MP.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the skill.
	 * @return {@code true} if the skill continues, or {@code false} if it is deactivated.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		final double base = _power * getTicksMultiplier();
		final double consume = (skill.getAbnormalTime() > 0) ? ((effected.getLevel() - 1) / 7.5) * base * skill.getAbnormalTime() : base;
		if (consume > effected.getCurrentMp())
		{
			effected.sendPacket(SystemMessageId.YOUR_SKILL_WAS_DEACTIVATED_DUE_TO_LACK_OF_MP);
			return false;
		}
		
		effected.reduceCurrentMp(consume);
		return skill.isToggle();
	}
}
