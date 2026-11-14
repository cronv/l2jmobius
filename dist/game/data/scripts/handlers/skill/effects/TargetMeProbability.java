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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the "Target Me" probability effect.<br>
 * It determines how likely an entity is to be targeted by others based on specific mechanics.
 * @author Adry_85
 */
public class TargetMeProbability extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new {@code TargetMeProbability} effect.<br>
	 * This constructor initializes the chance value from the provided stats.<br>
	 * It defaults to {@code 100} if no chance is specified.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public TargetMeProbability(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the effect successfully triggers based on a probability.<br>
	 * It uses the {@code _chance} value to calculate the result.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect succeeds, otherwise {@code false}.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * This method handles the instant execution of the effect.<br>
	 * It sets the target of the {@code effected} creature to the {@code effector}.<br>
	 * This only happens if the {@code effected} creature is playable and not already targeting the {@code effector}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayable() && (effected.getTarget() != effector))
		{
			effected.setTarget(effector);
		}
	}
}
