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
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.FlyType;
import org.l2jmobius.gameserver.network.serverpackets.FlyToLocation;
import org.l2jmobius.gameserver.network.serverpackets.ValidateLocation;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for a teleportation effect.<br>
 * It moves an entity to the location of a specific target.<br>
 * The {@code TeleportToTarget} class extends {@code AbstractEffect}.
 * @author Didldak, Adry_85
 */
public class TeleportToTarget extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code TeleportToTarget} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters for the teleport.
	 */
	public TeleportToTarget(StatSet params)
	{
	}
	
	/**
	 * Retrieves the type of effect for this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} associated with this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.TELEPORT_TO_TARGET;
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It verifies that the {@code effected} creature is not {@code null}.<br>
	 * It also checks if the {@code effector} can see the {@code effected} target.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && GeoEngine.getInstance().canSeeTarget(effected, effector);
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant teleport effect for a creature.<br>
	 * This method calculates a new location based on the target's position and heading.<br>
	 * It then moves the effector to that location and updates its state.
	 * @param effector The {@code Creature} that is performing the action.
	 * @param effected The {@code Creature} that is being targeted.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final int px = effected.getX();
		final int py = effected.getY();
		double ph = LocationUtil.convertHeadingToDegree(effected.getHeading());
		
		ph += 180;
		if (ph > 360)
		{
			ph -= 360;
		}
		
		ph = (Math.PI * ph) / 180;
		final int x = (int) (px + (25 * Math.cos(ph)));
		final int y = (int) (py + (25 * Math.sin(ph)));
		final int z = effected.getZ();
		
		final Location loc = GeoEngine.getInstance().getValidLocation(effector.getX(), effector.getY(), effector.getZ(), x, y, z, effector.getInstanceWorld());
		
		effector.getAI().setIntentionIdle();
		effector.broadcastPacket(new FlyToLocation(effector, loc.getX(), loc.getY(), loc.getZ(), FlyType.DUMMY));
		effector.abortAttack();
		effector.abortCast();
		effector.setXYZ(loc);
		effector.broadcastPacket(new ValidateLocation(effector));
		effected.revalidateZone(true);
	}
}
