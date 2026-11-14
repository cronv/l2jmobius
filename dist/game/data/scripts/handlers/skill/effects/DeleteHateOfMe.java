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

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code DeleteHateOfMe} effect.<br>
 * It removes the hate status from the target entity.
 * @author Adry_85
 */
public class DeleteHateOfMe extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Initializes a new {@code DeleteHateOfMe} effect.<br>
	 * It extracts the success chance from the provided parameters.<br>
	 * The default value for the chance is set to {@code 100}.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public DeleteHateOfMe(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the effect succeeds based on a random chance.<br>
	 * It uses the {@code _chance} value to calculate the probability.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect succeeds, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns the {@code EffectType.HATE} value.
	 * @return The {@code EffectType} of this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HATE;
	}
	
	/**
	 * Checks if this effect happens immediately.<br>
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
	 * This method stops the target from hating the effector.<br>
	 * It also resets the target's walking state and AI intention.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isAttackable())
		{
			return;
		}
		
		final Attackable target = effected.asAttackable();
		target.stopHating(effector);
		target.setWalking();
		target.getAI().setIntentionActive();
	}
}
