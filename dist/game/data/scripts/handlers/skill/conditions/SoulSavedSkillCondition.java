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
 * Checks if the {@code SoulSaved} condition is met for a skill.<br>
 * This condition verifies that the required soul has been successfully saved.
 * @author UnAfraid
 */
public class SoulSavedSkillCondition implements ISkillCondition
{
	private final int _amount;
	
	/**
	 * Creates a new {@code SoulSavedSkillCondition} using the provided statistics.<br>
	 * This constructor extracts the required amount from the {@code StatSet}.
	 * @param params The {@code StatSet} containing the skill configuration.
	 */
	public SoulSavedSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount");
	}
	
	/**
	 * Checks if the {@code caster} meets the requirements to use a specific {@code skill}.<br>
	 * The {@code caster} must be a player.<br>
	 * The player must have enough charged souls to meet the required amount.
	 * @param caster The entity attempting to cast the skill.
	 * @param skill The skill being attempted.
	 * @param target The object targeted by the skill.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && (caster.asPlayer().getChargedSouls() >= _amount);
	}
}
