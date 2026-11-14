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
package org.l2jmobius.gameserver.mechanics.effects;

import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Serves as the base class for all effect implementations in the game.<br>
 * It provides core logic for handling how effects are triggered and managed.<br>
 * Subclasses should follow specific rules regarding {@code onStart} and {@code onExit} overrides.
 * @author Zoey76, Mobius
 */
public abstract class AbstractEffect
{
	protected static final Logger LOGGER = Logger.getLogger(AbstractEffect.class.getName());
	
	private int _ticks;
	
	/**
	 * Retrieves the current number of ticks for this effect.<br>
	 * This value represents how many updates have occurred.
	 * @return The current tick count as an {@code int}.
	 */
	public int getTicks()
	{
		return _ticks;
	}
	
	/**
	 * Sets the number of ticks for this effect.<br>
	 * This value determines how long the effect lasts.
	 * @param ticks The number of ticks to set.
	 */
	protected void setTicks(int ticks)
	{
		_ticks = ticks;
	}
	
	/**
	 * Retrieves the calculated multiplier for effect ticks.<br>
	 * This value is used to determine the duration of an effect in seconds.<br>
	 * It combines the base ticks with the {@code PlayerConfig.EFFECT_TICK_RATIO}.
	 * @return the calculated tick multiplier as a {@code double}.
	 */
	public double getTicksMultiplier()
	{
		return (getTicks() * PlayerConfig.EFFECT_TICK_RATIO) / 1000f;
	}
	
	/**
	 * Determines if the effect can be successfully applied.<br>
	 * This method currently always returns {@code true}.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @return {@code true} if the calculation is successful.
	 */
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} by default.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	/**
	 * Executes an instant effect between two creatures.<br>
	 * This method handles the logic for effects that happen immediately.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
	}
	
	/**
	 * Executes a continuous instant effect.<br>
	 * This method handles effects that occur immediately but may have ongoing properties.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	public void continuousInstant(Creature effector, Creature effected, Skill skill, Item item)
	{
	}
	
	/**
	 * This method is called when an effect begins.<br>
	 * Use this to initialize logic for the effect.<br>
	 * Do not call the super class version of {@code onStart}.
	 * @param effector The creature that triggers the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the effect, if any.
	 */
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
	}
	
	/**
	 * This method is called when an effect finishes.<br>
	 * Use this to perform cleanup actions or final logic.<br>
	 * Do not override this method for instant effects.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} that received the effect.
	 * @param skill The {@code Skill} used to trigger the effect.
	 */
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
	}
	
	/**
	 * This method is called during the action time of an effect.<br>
	 * It determines if the effect should proceed based on current conditions.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 * @return {@code false} as this method currently does not perform any logic.
	 */
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		return false;
	}
	
	/**
	 * Retrieves the bitmask for the effect flags.<br>
	 * This value represents specific properties of the effect.
	 * @return the mask as a {@code long}.
	 */
	public long getEffectFlags()
	{
		return EffectFlag.NONE.getMask();
	}
	
	/**
	 * Checks if a specific condition is met.<br>
	 * This method currently always returns {@code true}.
	 * @param obj The object to check.
	 * @return {@code true} regardless of the input.
	 */
	public boolean checkCondition(Object obj)
	{
		return true;
	}
	
	/**
	 * Checks if the effect is an instant effect.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	public boolean isInstant()
	{
		return false;
	}
	
	/**
	 * Checks if the effect can be pumped.<br>
	 * This method determines if a continuous action is allowed.<br>
	 * It currently always returns {@code true}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used to pump the effect.
	 * @return {@code true} if the effect can be pumped, otherwise {@code false}.
	 */
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return true;
	}
	
	/**
	 * Applies the periodic effect to a target.<br>
	 * This method updates the status of the {@code effected} creature.<br>
	 * It uses the logic defined in the {@code skill} object.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to apply the effect.
	 */
	public void pump(Creature effected, Skill skill)
	{
	}
	
	/**
	 * Checks if the effect should delay its pump action.<br>
	 * This method currently always returns {@code false}.
	 * @return {@code true} if the pump is delayed, otherwise {@code false}.
	 */
	public boolean delayPump()
	{
		return false;
	}
	
	/**
	 * Retrieves the type of the effect.<br>
	 * This method always returns {@code EffectType.NONE}.
	 * @return the current {@code EffectType}
	 */
	public EffectType getEffectType()
	{
		return EffectType.NONE;
	}
	
	/**
	 * Returns a string representation of the effect.<br>
	 * This method uses the simple name of the current class.
	 * @return A string containing the class name.
	 */
	@Override
	public String toString()
	{
		return "Effect " + getClass().getSimpleName();
	}
}
