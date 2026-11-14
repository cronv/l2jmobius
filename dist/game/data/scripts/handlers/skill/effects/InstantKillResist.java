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
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for providing resistance against instant kill effects.<br>
 * It allows a {@code Creature} to survive skills that would otherwise result in immediate death.
 * @author Sdw
 */
public class InstantKillResist extends AbstractEffect
{
	private final Double _amount;
	
	/**
	 * Creates a new {@code InstantKillResist} effect.<br>
	 * This constructor initializes the resistance amount from a provided set of statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public InstantKillResist(StatSet params)
	{
		_amount = params.getDouble("amount", 0);
	}
	
	/**
	 * Applies the resistance amount to a target creature.<br>
	 * This method updates the {@code Stat.INSTANT_KILL_RESIST} stat.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} that triggered this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().mergeAdd(Stat.INSTANT_KILL_RESIST, _amount);
	}
}
