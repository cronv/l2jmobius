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
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code Relax} effect logic.<br>
 * It defines how the relaxation mechanic behaves when applied to a creature.
 */
public class Relax extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new instance of the {@code Relax} effect.<br>
	 * It initializes the effect using values from a {@code StatSet}.<br>
	 * This constructor sets the power and duration of the effect.
	 * @param params The {@code StatSet} containing configuration data for this effect.
	 */
	public Relax(StatSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.RELAXING}.
	 * @return The bitmask of the effect flags as a {@code long}.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.RELAXING.getMask();
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.RELAXING;
	}
	
	/**
	 * This method is called when the effect first begins.<br>
	 * It forces a player to stand up or sets an NPC to rest.
	 * @param effector The {@code Creature} that triggered the effect.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to apply this effect.
	 * @param item The {@code Item} associated with the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer())
		{
			effected.asPlayer().sitDown(false);
		}
		else
		{
			effected.getAI().setIntentionRest();
		}
	}
	
	/**
	 * Handles the logic for the effect during its active duration.<br>
	 * It checks if the target is alive and eligible to receive the effect.<br>
	 * It manages mana consumption and toggle deactivation conditions.
	 * @param effector The {@code Creature} who cast the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the skill, if any.
	 * @return {@code true} if the effect continues, or {@code false} if it stops.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		if (effected.isPlayer() && !effected.asPlayer().isSitting())
		{
			return false;
		}
		
		if (((effected.getCurrentHp() + 1) > effected.getMaxRecoverableHp()) && skill.isToggle())
		{
			effected.sendPacket(SystemMessageId.THAT_SKILL_HAS_BEEN_DE_ACTIVATED_AS_HP_WAS_FULLY_RECOVERED);
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
