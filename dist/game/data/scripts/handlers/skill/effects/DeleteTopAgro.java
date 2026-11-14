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
 * This effect removes the highest aggression target from an {@code Attackable} entity.<br>
 * It is used to clear the primary threat for a specific creature.
 * @author Sdw
 */
public class DeleteTopAgro extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * This method initializes the effect with specific statistics.<br>
	 * It sets the success chance from the provided {@code StatSet}.<br>
	 * If no chance is defined, it defaults to {@code 100}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public DeleteTopAgro(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the effect should trigger based on a random chance.<br>
	 * It uses the internal probability defined for this effect.
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
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HATE;
	}
	
	/**
	 * Checks if this effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of a skill on a target.<br>
	 * This method removes the highest hate target from an {@code Attackable} creature.<br>
	 * It also resets the creature's walking state and AI intention.
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
		target.stopHating(target.getMostHated());
		target.setWalking();
		target.getAI().setIntentionActive();
	}
}
