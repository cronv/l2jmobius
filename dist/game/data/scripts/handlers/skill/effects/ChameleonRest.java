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
 * This class handles the logic for the {@code Chameleon Rest} skill effect.<br>
 * It manages how the effect behaves when applied to a {@code Creature}.
 */
public class ChameleonRest extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new instance of the {@code ChameleonRest} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the internal power and duration based on the provided parameters.
	 * @param params The {@code StatSet} containing configuration values for this effect.
	 */
	public ChameleonRest(StatSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the bitmask of active effect flags.<br>
	 * This method combines {@code EffectFlag.SILENT_MOVE} and {@code EffectFlag.RELAXING}.
	 * @return The combined bitmask as a {@code long}.
	 */
	@Override
	public long getEffectFlags()
	{
		return (EffectFlag.SILENT_MOVE.getMask() | EffectFlag.RELAXING.getMask());
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
	 * Handles the periodic logic for this effect.<br>
	 * It checks if the target is alive and sitting.<br>
	 * It reduces the mana of the target based on the effect power.<br>
	 * Returns {@code false} if requirements are not met or if mana is insufficient.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill that triggered this effect.
	 * @param item The item used to trigger the skill, if any.
	 * @return {@code true} if the effect continues, otherwise {@code false}.
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
		
		final double manaDam = _power * getTicksMultiplier();
		if (manaDam > effected.getCurrentMp())
		{
			effected.sendPacket(SystemMessageId.YOUR_SKILL_WAS_DEACTIVATED_DUE_TO_LACK_OF_MP);
			return false;
		}
		
		effected.reduceCurrentMp(manaDam);
		return skill.isToggle();
	}
	
	/**
	 * This method is called when the effect first begins.<br>
	 * It forces the {@code effected} creature to enter a resting state.<br>
	 * Players will be forced to sit down.<br>
	 * Non-player creatures will have their AI set to rest.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to apply this effect.
	 * @param item The {@code Item} used to apply this effect.
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
}
