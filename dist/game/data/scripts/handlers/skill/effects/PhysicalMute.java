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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code PhysicalMute} effect.<br>
 * It prevents a target from performing physical actions while the effect is active.
 * @author -Nemesiss-, Mobius
 */
public class PhysicalMute extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code PhysicalMute} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public PhysicalMute(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.PSYCHICAL_MUTED}.
	 * @return The long value representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.PSYCHICAL_MUTED.getMask();
	}
	
	/**
	 * Checks if the effect can be applied to the target.<br>
	 * Raid bosses are immune, checked here so that the effect flag is not applied to them either.
	 * @param effector The {@code Creature} casting the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && !effected.isRaid();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * If valid, it notifies the target that their actions are muted.
	 * @param effector The {@code Creature} who triggered the effect.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 * @param item The {@code Item} used to apply the effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getAI().notifyActionMuted();
	}
}
