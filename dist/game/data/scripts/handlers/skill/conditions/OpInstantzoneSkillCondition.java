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
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a skill can be cast instantly within an {@code Instance}.<br>
 * This condition ensures that specific skills are allowed to trigger immediately in instance zones.
 * @author UnAfraid
 */
public class OpInstantzoneSkillCondition implements ISkillCondition
{
	private final int _instanceId;
	
	/**
	 * Creates a new instance of {@code OpInstantzoneSkillCondition}.<br>
	 * This constructor initializes the condition using values from a {@code StatSet}.<br>
	 * It specifically extracts the {@code instanceId} to define the required zone.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public OpInstantzoneSkillCondition(StatSet params)
	{
		_instanceId = params.getInt("instanceId");
	}
	
	/**
	 * Checks if the skill can be used within a specific instance.<br>
	 * It verifies that the {@code caster} is currently inside an instance.<br>
	 * The instance ID must match the required {@code _instanceId}.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		final Instance instance = caster.getInstanceWorld();
		return (instance != null) && (instance.getTemplateId() == _instanceId);
	}
}
