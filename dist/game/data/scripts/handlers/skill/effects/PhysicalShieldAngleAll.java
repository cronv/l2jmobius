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

import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the effect for a physical shield that covers all angles.<br>
 * This class applies a protective barrier to the target.<br>
 * It ensures the shield is active regardless of the direction of incoming attacks.
 * @author Sdw
 */
public class PhysicalShieldAngleAll extends AbstractEffect
{
	/**
	 * Creates a new {@code PhysicalShieldAngleAll} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public PhysicalShieldAngleAll(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * It returns the value associated with {@code EffectFlag.PHYSICAL_SHIELD_ANGLE_ALL}.
	 * @return The {@code long} mask representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.PHYSICAL_SHIELD_ANGLE_ALL.getMask();
	}
}
