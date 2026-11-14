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
package handlers.skill.conditions;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Position;
import org.l2jmobius.gameserver.geoengine.GeoEngine;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.LocationUtil;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the specific conditions for an {@code OpBlink} skill are met.<br>
 * This condition ensures that the skill can be cast based on defined game rules.
 * @author Sdw
 */
public class OpBlinkSkillCondition implements ISkillCondition
{
	private final int _angle;
	private final int _range;
	
	/**
	 * Creates a new {@code OpBlinkSkillCondition} using the provided parameters.<br>
	 * This constructor initializes the required angle and range values.<br>
	 * It extracts data from the {@code StatSet} to configure the skill behavior.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public OpBlinkSkillCondition(StatSet params)
	{
		final Position position = params.getEnum("direction", Position.class);
		switch (position)
		{
			case BACK:
			{
				_angle = 0;
				break;
			}
			case FRONT:
			{
				_angle = 180;
				break;
			}
			default:
			{
				_angle = -1;
				break;
			}
		}
		
		_range = params.getInt("range") + 15; // TODO: Check if 15 can be replaced with effected collision radius.
	}
	
	/**
	 * Checks if the {@code caster} can use a specific {@code skill} on a {@code target}.<br>
	 * This method verifies if there is a clear path to the calculated destination.<br>
	 * It uses the {@code _angle} and {@code _range} properties of this condition.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} that is the target of the skill.
	 * @return {@code true} if the destination is reachable, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final double angle = LocationUtil.convertHeadingToDegree(caster.getHeading());
		final double radian = Math.toRadians(angle);
		final double course = Math.toRadians(_angle);
		final int x1 = (int) (Math.cos(Math.PI + radian + course) * _range);
		final int y1 = (int) (Math.sin(Math.PI + radian + course) * _range);
		final int z = caster.getZ();
		return GeoEngine.getInstance().canMoveToTarget(caster.getX(), caster.getY(), z, caster.getX() + x1, caster.getY() + y1, z, caster.getInstanceWorld());
	}
}
