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
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code VampiricAttack} skill effect.<br>
 * It allows a creature to heal based on the damage dealt during an attack.
 * @author Sdw
 */
public class VampiricAttack extends AbstractEffect
{
	private final double _amount;
	private final double _sum;
	
	/**
	 * Creates a new {@code VampiricAttack} effect.<br>
	 * This constructor initializes the attack values from a provided set of statistics.<br>
	 * It calculates the base amount and the total sum based on the chance provided in the parameters.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public VampiricAttack(StatSet params)
	{
		_amount = params.getDouble("amount");
		_sum = _amount * params.getDouble("chance");
	}
	
	/**
	 * Applies the vampiric effect to a specific creature.<br>
	 * This method updates the {@code Stat} of the target.<br>
	 * It modifies both the damage absorption and the vampiric sum.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		final CreatureStat stat = effected.getStat();
		stat.mergeAdd(Stat.ABSORB_DAMAGE_PERCENT, _amount / 100);
		stat.addToVampiricSum(_sum);
	}
}
