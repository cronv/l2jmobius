/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Confuse} status effect.<br>
 * It manages how affected creatures behave when under this debuff.
 * @author littlecrow
 */
public class Confuse extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new {@code Confuse} effect instance.<br>
	 * It initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public Confuse(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the effect successfully applies to a target.<br>
	 * It calculates the probability based on the defined chance.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect succeeds, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * This value identifies the specific type of effect being applied.
	 * @return The {@code long} mask corresponding to {@code EffectFlag#CONFUSED}.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.CONFUSED.getMask();
	}
	
	/**
	 * Checks if the effect is applied instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the instant effect of a confusion skill.<br>
	 * This method notifies the AI and sets a random target for the {@code effected} creature.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item used to trigger the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.getAI().notifyActionConfused();
		
		final List<Creature> targetList = new ArrayList<>();
		// Getting the possible targets.
		
		World.forEachVisibleObject(effected, Creature.class, targetList::add);
		
		// If there is no target, exit function.
		if (!targetList.isEmpty())
		{
			// Choosing randomly a new target.
			final Creature target = targetList.get(Rnd.get(targetList.size()));
			
			// Attacking the target
			effected.setTarget(target);
			effected.getAI().setIntentionAttack(target);
		}
	}
}
