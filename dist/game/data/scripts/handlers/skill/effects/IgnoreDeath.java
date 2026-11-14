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
 * This class handles the effect that prevents a character from dying.<br>
 * It ensures that the target remains alive even when their health reaches {@code 0}.<br>
 * It extends {@code AbstractEffect} to manage this specific mechanic.
 * @author Sdw
 */
public class IgnoreDeath extends AbstractEffect
{
	/**
	 * Creates a new instance of {@code IgnoreDeath}.<br>
	 * This effect prevents the target from dying.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public IgnoreDeath(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.IGNORE_DEATH}.
	 * @return The {@code long} mask representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.IGNORE_DEATH.getMask();
	}
}
