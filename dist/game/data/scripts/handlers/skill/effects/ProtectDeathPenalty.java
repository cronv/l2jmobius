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
 * This class handles the effect that prevents a player from losing items upon death.<br>
 * It ensures that the {@code deathPenalty} is not applied to the target.<br>
 * Use this effect when a character should retain their equipment and inventory after dying.
 * @author Sdw
 */
public class ProtectDeathPenalty extends AbstractEffect
{
	/**
	 * Creates a new instance of {@code ProtectDeathPenalty}.<br>
	 * This effect handles the protection logic for death penalties.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ProtectDeathPenalty(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for the effect flags.<br>
	 * This method identifies which specific effects are active.<br>
	 * It returns the mask associated with {@code EffectFlag.PROTECT_DEATH_PENALTY}.
	 * @return The {@code long} value representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.PROTECT_DEATH_PENALTY.getMask();
	}
}
