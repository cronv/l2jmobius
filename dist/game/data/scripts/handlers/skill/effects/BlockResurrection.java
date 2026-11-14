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
 * This class implements the {@code BlockResurrection} effect.<br>
 * It prevents a target from being resurrected while the effect is active.
 * @author UnAfraid
 */
public class BlockResurrection extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code BlockResurrection} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public BlockResurrection(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * This method returns the specific {@code EffectFlag} associated with this class.
	 * @return The {@code long} value representing the effect mask.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.BLOCK_RESURRECTION.getMask();
	}
}
