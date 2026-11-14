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

import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the calculation of skill mastery rate effects.<br>
 * It allows for the modification of a character's mastery rate based on specific conditions.<br>
 * It extends {@code AbstractStatPercentEffect} to apply percentage-based changes.
 * @author Sdw
 */
public class SkillMasteryRate extends AbstractStatPercentEffect
{
	/**
	 * Creates a new {@code SkillMasteryRate} effect.<br>
	 * This constructor initializes the effect using a set of statistics.<br>
	 * It applies these values to the {@code Stat.SKILL_MASTERY_RATE} stat.
	 * @param params The {@code StatSet} containing the modification values.
	 */
	public SkillMasteryRate(StatSet params)
	{
		super(params, Stat.SKILL_MASTERY_RATE);
	}
}
