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
 * Checks if the {@code Skill} requires a specific amount of energy to be saved.<br>
 * This condition validates whether the player has met the necessary energy requirements for execution.
 * @author Sdw
 */
public class EnergySavedSkillCondition implements ISkillCondition
{
	private final int _amount;
	
	/**
	 * Creates a new condition for skills that require saved energy.<br>
	 * It extracts the required amount from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the "amount" value.
	 */
	public EnergySavedSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount");
	}
	
	/**
	 * Checks if the {@code caster} has enough charges to use the {@code skill}.<br>
	 * It compares the current charges against the required amount.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the caster has enough charges, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.asPlayer().getCharges() >= _amount;
	}
}
