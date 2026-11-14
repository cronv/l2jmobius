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
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the target's current energy is equal to their maximum energy.<br>
 * This condition is used to validate skill requirements based on {@code energy} stats.
 * @author UnAfraid
 */
public class OpEnergyMaxSkillCondition implements ISkillCondition
{
	private final int _amount;
	
	/**
	 * Creates a new condition that checks for maximum energy.<br>
	 * It uses the provided {@code StatSet} to determine the required amount.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public OpEnergyMaxSkillCondition(StatSet params)
	{
		_amount = params.getInt("amount");
	}
	
	/**
	 * Checks if the caster can use a specific skill based on their current charges.<br>
	 * This method returns {@code false} if the caster has reached the maximum capacity.<br>
	 * It also sends a system message to the player when the limit is hit.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} being targeted by the skill.
	 * @return {@code true} if the skill can be used, or {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if (caster.asPlayer().getCharges() >= _amount)
		{
			caster.sendPacket(SystemMessageId.YOUR_FORCE_HAS_REACHED_MAXIMUM_CAPACITY);
			return false;
		}
		
		return true;
	}
}
