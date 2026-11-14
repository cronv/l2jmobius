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
import org.l2jmobius.gameserver.mechanics.stats.BaseStat;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for skill mastery effects in the game.<br>
 * This class manages how specific skills interact with player stats and progression.<br>
 * It extends {@code AbstractEffect} to apply these mechanics to a {@code Creature}.
 * @author Sdw
 */
public class SkillMastery extends AbstractEffect
{
	private final Double _stat;
	
	/**
	 * Creates a new {@code SkillMastery} effect.<br>
	 * This constructor initializes the mastery based on provided statistics.
	 * @param params The {@code StatSet} containing the required configuration data.
	 */
	public SkillMastery(StatSet params)
	{
		_stat = Double.valueOf(params.getEnum("stat", BaseStat.class, BaseStat.STR).ordinal());
	}
	
	/**
	 * Applies a stat boost to a specific creature.<br>
	 * This method increases the {@code Stat.SKILL_MASTERY} value of the target.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().mergeAdd(Stat.SKILL_MASTERY, _stat);
	}
}
