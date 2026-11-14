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
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the {@code Compelling} skill effect.<br>
 * This class manages the logic for applying specific status changes to a target creature.
 * @author Mobius
 */
public class Compelling extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Compelling} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Compelling(StatSet params)
	{
	}
	
	/**
	 * Calculates the success probability of a {@code Compelling} effect.<br>
	 * It uses the {@code Formulas} class to determine if the action succeeds.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the success roll is successful, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(100, effector, effected, skill);
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It verifies that the {@code effected} creature is valid and interactable.<br>
	 * The method returns {@code false} if the target is null, in a raid, or an unattackable NPC.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		if ((effected == null) || effected.isRaid() || (effected.isNpc() && !effected.isAttackable()))
		{
			return false;
		}
		
		return effected.isPlayable() || effected.isAttackable();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It triggers the specific logic for the {@code Compelling} effect.
	 * @param effector The creature that casts the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		compellingAction(effector, effected);
	}
	
	/**
	 * This method is called during the action time of a skill effect.<br>
	 * It triggers the specific logic for this effect.<br>
	 * It always returns {@code false}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being executed.
	 * @param item The item associated with the skill.
	 * @return Always returns {@code false}.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		compellingAction(null, effected);
		return false;
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It notifies the AI of a non-player creature to think about its next action.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} affected by the skill.
	 * @param skill The {@code Skill} used to apply this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (!effected.isPlayer())
		{
			effected.getAI().notifyActionThink();
		}
	}
	
	/**
	 * Forces the {@code effected} creature to move toward the {@code effector}.<br>
	 * This method sets the target's state to running.<br>
	 * It updates the AI intention of the {@code effected} creature.
	 * @param effector The creature that is performing the action.
	 * @param effected The creature being forced to move.
	 */
	private void compellingAction(Creature effector, Creature effected)
	{
		effected.setRunning();
		effected.getAI().setIntentionMoveTo(effector.getLocation());
	}
}
