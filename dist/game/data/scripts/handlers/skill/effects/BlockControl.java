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
 * This effect restricts the movement and actions of a character.<br>
 * It prevents the target from moving, casting spells, or performing social actions.
 * @author Nik
 */
public class BlockControl extends AbstractEffect
{
	/**
	 * Creates a new {@code BlockControl} instance.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public BlockControl(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect's flags.<br>
	 * It returns the value associated with {@code EffectFlag.BLOCK_CONTROL}.
	 * @return The {@code long} bitmask representing the block control flag.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.BLOCK_CONTROL.getMask();
	}
	
	/**
	 * Checks if the effect can be applied to the target.<br>
	 * Raid bosses are immune, checked here so that the effect flag is not applied to them either.
	 * @param effector The {@code Creature} casting the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && !effected.isRaid();
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this block control effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BLOCK_CONTROL;
	}
}
