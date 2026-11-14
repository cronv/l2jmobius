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
 * This class handles the logic for the block escape effect.<br>
 * It defines how characters behave when they are affected by this specific skill mechanic.
 * @author UnAfraid
 */
public class BlockEscape extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code BlockEscape} effect.<br>
	 * It uses the provided statistics to initialize the effect properties.
	 * @param params The {@code StatSet} containing the initial values for this effect.
	 */
	public BlockEscape(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for the effect flags.<br>
	 * This method returns the specific flag associated with this effect.
	 * @return The {@code long} value representing the effect mask.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.CANNOT_ESCAPE.getMask();
	}
}
