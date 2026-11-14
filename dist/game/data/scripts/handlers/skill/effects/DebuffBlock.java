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
 * This effect prevents the target from receiving any incoming debuffs.<br>
 * It acts as a protective layer against negative status effects.
 * @author Nik
 */
public class DebuffBlock extends AbstractEffect
{
	/**
	 * Creates a new {@code DebuffBlock} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public DebuffBlock(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * This method returns the value associated with {@code EffectFlag.DEBUFF_BLOCK}.
	 * @return The {@code long} bitmask representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.DEBUFF_BLOCK.getMask();
	}
}
