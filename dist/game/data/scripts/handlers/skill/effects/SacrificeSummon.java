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
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for sacrificing a {@code Summon} to trigger a specific effect.<br>
 * This class manages the removal of the summon and the subsequent execution of the skill's mechanics.
 * @author Mobius
 */
public class SacrificeSummon extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code SacrificeSummon} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters for the summon.
	 */
	public SacrificeSummon(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a specific target.<br>
	 * This method returns {@code true} only if the target is a summon.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the target is a summon, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isSummon();
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the immediate effects of a sacrifice skill.<br>
	 * This method kills all active summons owned by the {@code effector}.<br>
	 * It also sends a notification message to the player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the transaction.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (Summon summon : effector.getServitors().values())
		{
			summon.abortAttack();
			summon.abortCast();
			summon.stopAllEffects();
			
			summon.doDie(summon);
		}
		
		effector.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED_YOU_LL_NEED_TO_SUMMON_A_NEW_ONE);
	}
}
