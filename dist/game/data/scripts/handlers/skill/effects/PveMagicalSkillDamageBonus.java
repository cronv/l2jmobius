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
 * This class handles the calculation of magical skill damage bonuses specifically for PvE content.<br>
 * It applies a percentage-based modifier to the character's magic damage output.
 * @author Sdw
 */
public class PveMagicalSkillDamageBonus extends AbstractStatPercentEffect
{
	/**
	 * Creates a new {@code PveMagicalSkillDamageBonus} effect.<br>
	 * This constructor initializes the bonus using the provided stats.<br>
	 * It applies the values to the {@code Stat.PVE_MAGICAL_SKILL_DAMAGE} stat.
	 * @param params The {@code StatSet} containing the bonus values.
	 */
	public PveMagicalSkillDamageBonus(StatSet params)
	{
		super(params, Stat.PVE_MAGICAL_SKILL_DAMAGE);
	}
}
