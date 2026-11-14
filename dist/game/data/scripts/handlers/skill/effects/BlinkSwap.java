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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This {@code BlinkSwap} effect swaps the locations of the caster and the target.<br>
 * The logic is handled entirely based on the client description.<br>
 * Geodata checks are assumed to be completed during the skill cast.
 * @author Nik
 */
public class BlinkSwap extends AbstractEffect
{
	/**
	 * Creates a new instance of {@code BlinkSwap}.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public BlinkSwap(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect can be triggered between two creatures.<br>
	 * It verifies that the {@code effected} creature is not {@code null}.<br>
	 * It also checks if the {@code effector} can see the {@code effected} target.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger this effect.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && GeoEngine.getInstance().canSeeTarget(effected, effector);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant swap of positions between two creatures.<br>
	 * This method teleports both entities to each other's locations.<br>
	 * It also resets their current actions and updates their coordinates.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used for this swap.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Location effectedLoc = new Location(effected);
		final Location effectorLoc = new Location(effector);
		
		effector.getAI().setIntentionIdle();
		effector.broadcastPacket(new FlyToLocation(effector, effectedLoc, FlyType.DUMMY));
		effector.abortAttack();
		effector.abortCast();
		effector.setXYZ(effectedLoc);
		effector.broadcastPacket(new ValidateLocation(effector));
		effector.revalidateZone(true);
		
		effected.getAI().setIntentionIdle();
		effected.broadcastPacket(new FlyToLocation(effected, effectorLoc, FlyType.DUMMY));
		effected.abortAttack();
		effected.abortCast();
		effected.setXYZ(effectorLoc);
		effected.broadcastPacket(new ValidateLocation(effected));
		effected.revalidateZone(true);
	}
}
