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
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code NoblesseBless} skill effect.<br>
 * This class manages how the blessing affects a {@code Creature}.
 * @author earendil
 */
public class NoblesseBless extends AbstractEffect
{
	/**
	 * Creates a new {@code NoblesseBless} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the values for the blessing.
	 */
	public NoblesseBless(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It verifies that both creatures are not {@code null}.<br>
	 * It also checks if the target is playable.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effector != null) && (effected != null) && effected.isPlayable();
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * This value is used to identify specific properties of the {@code NoblesseBless} effect.
	 * @return The {@code long} mask associated with {@code EffectFlag.NOBLESS_BLESSING}.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.NOBLESS_BLESSING.getMask();
	}
	
	/**
	 * Retrieves the specific type of effect for this instance.<br>
	 * This method returns the {@code EffectType} associated with Noblesse Blessing.
	 * @return The {@code EffectType} of this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.NOBLESSE_BLESSING;
	}
}
