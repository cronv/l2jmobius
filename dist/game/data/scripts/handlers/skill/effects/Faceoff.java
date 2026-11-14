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
 * Handles the logic for the {@code Faceoff} skill effect.<br>
 * This class manages how the effect interacts with target creatures and applies specific mechanics.
 * @author Sdw
 */
public class Faceoff extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Faceoff} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the initial values for the effect.
	 */
	public Faceoff(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * This value is used to identify specific behaviors of the {@code Faceoff} effect.
	 * @return The {@code long} mask representing the {@code EffectFlag.FACEOFF} flag.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.FACEOFF.getMask();
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} only if the target is a player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the target is a player, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer();
	}
}
