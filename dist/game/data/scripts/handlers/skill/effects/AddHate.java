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
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code AddHate} effect.<br>
 * It is responsible for increasing the hate level of a target creature.
 * @author Adry_85
 */
public class AddHate extends AbstractEffect
{
	private final double _power;
	private final boolean _affectSummoner;
	
	/**
	 * Creates a new {@code AddHate} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the power and whether it affects the summoner.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public AddHate(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_affectSummoner = params.getBoolean("affectSummoner", false);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the hate effect instantly to a target.<br>
	 * This method determines the source of the effect based on whether it affects the summoner.<br>
	 * It checks if the target is attackable before applying damage or reducing hate.
	 * @param creature The {@code Creature} who initiates the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void instant(Creature creature, Creature effected, Skill skill, Item item)
	{
		Creature effector = creature;
		if (_affectSummoner && (effector.getSummoner() != null))
		{
			effector = effector.getSummoner();
		}
		
		if (!effected.isAttackable())
		{
			return;
		}
		
		final double val = _power;
		if (val > 0)
		{
			effected.asAttackable().addDamageHate(effector, 0, (int) val);
			effected.setRunning();
		}
		else if (val < 0)
		{
			effected.asAttackable().reduceHate(effector, (int) -val);
		}
	}
}
