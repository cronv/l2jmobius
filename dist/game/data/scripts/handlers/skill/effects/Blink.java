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
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the {@code Blink} warp effect where an actor disappears and reappears at a nearby location.<br>
 * It uses {@code flyRadius} for distance and {@code flyCourse} for direction, with collision detection enabled if geodata is active.
 * @author DrHouse
 */
public class Blink extends AbstractEffect
{
	private final int _flyCourse;
	private final int _flyRadius;
	
	private final FlyType _flyType;
	private final int _flySpeed;
	private final int _flyDelay;
	private final int _animationSpeed;
	
	/**
	 * Creates a new {@code Blink} instance using the provided parameters.<br>
	 * This constructor initializes all movement and animation properties from a {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration values like angle, range, and speed.
	 */
	public Blink(StatSet params)
	{
		_flyCourse = params.getInt("angle", 0);
		_flyRadius = params.getInt("range", 0);
		_flyType = params.getEnum("flyType", FlyType.class, FlyType.DUMMY);
		_flySpeed = params.getInt("speed", 0);
		_flyDelay = params.getInt("delay", 0);
		_animationSpeed = params.getInt("animationSpeed", 0);
	}
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Checks if the {@code Blink} effect can be started.<br>
	 * It returns {@code false} if the {@code effected} creature is currently blocked from escaping.<br>
	 * Otherwise, it returns {@code true}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect can start, {@code false} otherwise.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		// While affected by escape blocking effect you cannot use Blink or Scroll of Escape.
		return !effected.cannotEscape();
	}
	
	/**
	 * Executes the instant blink effect for a specific creature.<br>
	 * This method calculates the destination based on heading and radius.<br>
	 * It then moves the target to the valid location and updates its state.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final double angle = LocationUtil.convertHeadingToDegree(effected.getHeading());
		final double radian = Math.toRadians(angle);
		final double course = Math.toRadians(_flyCourse);
		final int x1 = (int) (Math.cos(Math.PI + radian + course) * _flyRadius);
		final int y1 = (int) (Math.sin(Math.PI + radian + course) * _flyRadius);
		
		final int effectedX = effected.getX();
		final int effectedY = effected.getY();
		final int z = effected.getZ();
		
		final int x = effectedX + x1;
		final int y = effectedY + y1;
		
		final Location destination = GeoEngine.getInstance().getValidLocation(effectedX, effectedY, z, x, y, z, effected.getInstanceWorld());
		
		effected.getAI().setIntentionIdle();
		effected.broadcastPacket(new FlyToLocation(effected, destination, _flyType, _flySpeed, _flyDelay, _animationSpeed));
		effected.setXYZ(destination);
		effected.broadcastPacket(new ValidateLocation(effected));
		effected.revalidateZone(true);
	}
}
