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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code RandomizeHate} effect.<br>
 * It applies a randomized hate value to an entity when triggered by a skill.
 */
public class RandomizeHate extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new {@code RandomizeHate} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the internal chance value based on the "chance" key.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public RandomizeHate(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the effect succeeds based on a random chance.<br>
	 * It uses the {@code _chance} value to calculate the probability.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect succeeds, otherwise {@code false}.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a skill.<br>
	 * This method transfers the hate value from one target to another randomly selected creature.<br>
	 * It ensures that the new target is not in the same clan as the original mob.
	 * @param effector The creature performing the action.
	 * @param effected The creature currently being targeted by the effector.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected == effector) || !effected.isAttackable())
		{
			return;
		}
		
		final Attackable effectedMob = effected.asAttackable();
		final List<Creature> targetList = new ArrayList<>();
		World.forEachVisibleObject(effected, Creature.class, cha ->
		{
			if ((cha != effectedMob) && (cha != effector))
			{
				// Aggro cannot be transfered to a mob of the same faction.
				if (cha.isAttackable() && cha.asAttackable().isInMyClan(effectedMob))
				{
					return;
				}
				
				targetList.add(cha);
			}
		});
		// If there is no target, exit function.
		if (targetList.isEmpty())
		{
			return;
		}
		
		// Choosing randomly a new target.
		final Creature target = targetList.get(Rnd.get(targetList.size()));
		final long hate = effectedMob.getHating(effector);
		effectedMob.stopHating(effector);
		effectedMob.addDamageHate(target, 0, hate);
	}
}
