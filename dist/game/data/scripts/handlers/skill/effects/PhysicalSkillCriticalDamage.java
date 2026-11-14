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
 * Handles the application of critical damage modifiers to physical skills.<br>
 * This class modifies how {@code Stat} values are calculated during a skill effect.<br>
 * It ensures that critical hits correctly scale based on the character's attributes.
 * @author Sdw
 */
public class PhysicalSkillCriticalDamage extends AbstractStatEffect
{
	/**
	 * Creates a new instance of {@code PhysicalSkillCriticalDamage}.<br>
	 * This effect modifies the physical skill critical damage stats.
	 * @param params The {@code StatSet} containing the values for this effect.
	 */
	public PhysicalSkillCriticalDamage(StatSet params)
	{
		super(params, Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE, Stat.PHYSICAL_SKILL_CRITICAL_DAMAGE_ADD);
	}
}
