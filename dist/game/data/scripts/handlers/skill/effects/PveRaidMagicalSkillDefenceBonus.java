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
 * This class handles the bonus to magical skill defense during PvE raids.<br>
 * It applies a percentage-based increase to the {@code Stat.MAGICAL_SKILL_DEFENCE} stat.<br>
 * Use this effect to scale defensive capabilities for players in raid scenarios.
 * @author Sdw
 */
public class PveRaidMagicalSkillDefenceBonus extends AbstractStatPercentEffect
{
	/**
	 * Creates a new instance of {@code PveRaidMagicalSkillDefenceBonus}.<br>
	 * This constructor initializes the effect using specific statistics.<br>
	 * It applies the bonus to the {@code Stat.PVE_RAID_MAGICAL_SKILL_DEFENCE} stat.
	 * @param params The set of statistics used to configure this effect.
	 */
	public PveRaidMagicalSkillDefenceBonus(StatSet params)
	{
		super(params, Stat.PVE_RAID_MAGICAL_SKILL_DEFENCE);
	}
}
