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
 * This class handles the {@code DeleteHate} effect.<br>
 * It removes the hate value from a target entity when triggered by a skill.
 * @author Adry_85
 */
public class DeleteHate extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new {@code DeleteHate} effect.<br>
	 * This constructor initializes the effect using the provided statistics.<br>
	 * It sets the success chance from the {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public DeleteHate(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the effect succeeds based on a random chance.<br>
	 * It uses the {@code _chance} value to calculate the probability.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect succeeds, or {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns the {@code EffectType} for hate-related mechanics.
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
	 * Applies the instant effect of a skill to a target.<br>
	 * This method clears the aggro list and resets the AI state of the {@code effected} creature.<br>
	 * It only executes if the {@code effected} creature is attackable.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isAttackable())
		{
			return;
		}
		
		final Attackable target = effected.asAttackable();
		target.clearAggroList();
		target.setWalking();
		target.getAI().setIntentionActive();
	}
}
