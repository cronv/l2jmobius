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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code ProtectionBlessing} skill effect.<br>
 * It manages the mechanics applied to a creature when this blessing is active.
 * @author Kerberos
 */
public class ProtectionBlessing extends AbstractEffect
{
	/**
	 * Creates a new {@code ProtectionBlessing} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the values for this blessing.
	 */
	public ProtectionBlessing(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It verifies that both creatures are not {@code null}.<br>
	 * The target must be a player character.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effector != null) && (effected != null) && effected.isPlayer();
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * This value identifies the specific {@code EffectFlag} associated with this class.
	 * @return The {@code long} mask representing the protection blessing flag.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.PROTECTION_BLESSING.getMask();
	}
}
