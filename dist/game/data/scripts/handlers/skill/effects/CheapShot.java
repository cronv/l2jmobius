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
 * Handles the logic for the {@code CheapShot} skill effect.<br>
 * This class manages how the effect applies to targets during combat.
 * @author Sdw
 */
public class CheapShot extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code CheapShot} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the initial values for this effect.
	 */
	public CheapShot(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the specific flags associated with {@code CheapShot}.
	 * @return The {@code long} value representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.CHEAPSHOT.getMask();
	}
}
