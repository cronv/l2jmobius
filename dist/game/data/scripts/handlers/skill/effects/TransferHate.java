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

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code TransferHate} skill effect.<br>
 * It manages the logic for transferring aggression between entities.
 * @author Adry_85
 */
public class TransferHate extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new {@code TransferHate} effect.<br>
	 * It initializes the effect using the provided configuration.
	 * @param params The {@code StatSet} containing the skill parameters.
	 */
	public TransferHate(StatSet params)
	{
		_chance = params.getInt("chance", 100);
	}
	
	/**
	 * Determines if the effect succeeds based on a probability.<br>
	 * It uses the {@code _chance} value and current entity stats.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the success roll passes, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return Formulas.calcProbability(_chance, effector, effected, skill);
	}
	
	/**
	 * Checks if the effect can be triggered between two creatures.<br>
	 * It verifies if the {@code effector} is within the required range of the {@code effected} target.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @return {@code true} if the creatures are within range, {@code false} otherwise.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return LocationUtil.checkIfInRange(skill.getEffectRange(), effector, effected, true);
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
	 * This method transfers hate from nearby enemies to the target creature.<br>
	 * It identifies all living {@code Attackable} objects within the skill range.<br>
	 * The transferred hate amount is based on the current hate level of those enemies.
	 * @param effector The creature who performs the skill.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} being executed.
	 * @param item The {@code Item} used to trigger the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		World.forEachVisibleObjectInRange(effector, Attackable.class, skill.getAffectRange(), hater ->
		{
			if (hater.isDead())
			{
				return;
			}
			
			final long hate = hater.getHating(effector);
			if (hate <= 0)
			{
				return;
			}
			
			hater.reduceHate(effector, -hate);
			hater.addDamageHate(effected, 0, hate);
		});
	}
}
