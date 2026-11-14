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
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Prevents a skill from being used while the character is in a transformed state.<br>
 * This condition checks if the {@code Creature} is currently using a transformation skill.
 * @author Sdw
 */
public class CannotUseInTransformSkillCondition implements ISkillCondition
{
	private final int _transformId;
	
	/**
	 * Creates a new condition to restrict skills based on transformation.<br>
	 * It uses the provided {@code StatSet} to determine the required ID.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public CannotUseInTransformSkillCondition(StatSet params)
	{
		_transformId = params.getInt("transformId", -1);
	}
	
	/**
	 * Checks if the {@code caster} is allowed to use a specific {@code skill}.<br>
	 * This method validates transformation requirements for the action.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the conditions are met, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return (_transformId > 0) ? caster.getTransformationId() != _transformId : !caster.isTransformed();
	}
}
