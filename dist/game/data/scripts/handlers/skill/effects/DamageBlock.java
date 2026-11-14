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
 * This effect blocks incoming damage and healing to {@code HP} and {@code MP}.<br>
 * It does not block regeneration, {@code DOT}, Vampiric Rage, or Balance Life.
 * @author Nik
 */
public class DamageBlock extends AbstractEffect
{
	private final boolean _blockHp;
	private final boolean _blockMp;
	
	/**
	 * Creates a new {@code DamageBlock} effect.<br>
	 * This constructor initializes the block settings based on the provided parameters.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public DamageBlock(StatSet params)
	{
		final String type = params.getString("type", null);
		_blockHp = type.equalsIgnoreCase("BLOCK_HP");
		_blockMp = type.equalsIgnoreCase("BLOCK_MP");
	}
	
	/**
	 * Retrieves the bitmask for the active effect flags.<br>
	 * This method determines which types of damage or healing are blocked.<br>
	 * It checks the {@code _blockHp} and {@code _blockMp} fields to return the correct mask.
	 * @return The {@code long} value representing the combined {@code EffectFlag} masks.
	 */
	@Override
	public long getEffectFlags()
	{
		return _blockHp ? EffectFlag.HP_BLOCK.getMask() : (_blockMp ? EffectFlag.MP_BLOCK.getMask() : EffectFlag.NONE.getMask());
	}
}
