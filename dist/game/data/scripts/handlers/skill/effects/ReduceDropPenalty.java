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
import org.l2jmobius.gameserver.mechanics.skill.enums.ReduceDropType;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the effect of reducing drop penalties for a {@code Creature}.<br>
 * It modifies the loot drop logic based on specific skill mechanics.<br>
 * It interacts with {@code Stat} values to determine the final penalty reduction.
 * @author Sdw
 */
public class ReduceDropPenalty extends AbstractEffect
{
	private final double _exp;
	private final double _deathPenalty;
	private final ReduceDropType _type;
	
	/**
	 * Creates a new {@code ReduceDropPenalty} effect.<br>
	 * This constructor initializes the penalty values from a provided set.<br>
	 * It extracts experience, death penalty, and drop type data.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ReduceDropPenalty(StatSet params)
	{
		_exp = params.getDouble("exp", 0);
		_deathPenalty = params.getDouble("deathPenalty", 0);
		_type = params.getEnum("type", ReduceDropType.class, ReduceDropType.MOB);
	}
	
	/**
	 * Applies the reduction effects to a specific creature.<br>
	 * This method updates stats based on the type of skill used.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being cast.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		switch (_type)
		{
			case MOB:
			{
				final CreatureStat stat = effected.getStat();
				stat.mergeMul(Stat.REDUCE_EXP_LOST_BY_MOB, (_exp / 100) + 1);
				stat.mergeMul(Stat.REDUCE_DEATH_PENALTY_BY_MOB, (_deathPenalty / 100) + 1);
				break;
			}
			case PK:
			{
				final CreatureStat stat = effected.getStat();
				stat.mergeMul(Stat.REDUCE_EXP_LOST_BY_PVP, (_exp / 100) + 1);
				stat.mergeMul(Stat.REDUCE_DEATH_PENALTY_BY_PVP, (_deathPenalty / 100) + 1);
				break;
			}
			case RAID:
			{
				final CreatureStat stat = effected.getStat();
				stat.mergeMul(Stat.REDUCE_EXP_LOST_BY_RAID, (_exp / 100) + 1);
				stat.mergeMul(Stat.REDUCE_DEATH_PENALTY_BY_RAID, (_deathPenalty / 100) + 1);
				break;
			}
			case ANY:
			{
				final CreatureStat stat = effected.getStat();
				stat.mergeMul(Stat.REDUCE_EXP_LOST_BY_MOB, (_exp / 100) + 1);
				stat.mergeMul(Stat.REDUCE_DEATH_PENALTY_BY_MOB, (_deathPenalty / 100) + 1);
				stat.mergeMul(Stat.REDUCE_EXP_LOST_BY_PVP, (_exp / 100) + 1);
				stat.mergeMul(Stat.REDUCE_DEATH_PENALTY_BY_PVP, (_deathPenalty / 100) + 1);
				stat.mergeMul(Stat.REDUCE_EXP_LOST_BY_RAID, (_exp / 100) + 1);
				stat.mergeMul(Stat.REDUCE_DEATH_PENALTY_BY_RAID, (_deathPenalty / 100) + 1);
				break;
			}
		}
	}
}
