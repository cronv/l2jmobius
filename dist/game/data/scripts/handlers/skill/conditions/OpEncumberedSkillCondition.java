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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a {@code Creature} is currently encumbered.<br>
 * This condition determines if the character's weight exceeds their carrying capacity.
 * @author UnAfraid
 */
public class OpEncumberedSkillCondition implements ISkillCondition
{
	private final int _slotsPercent;
	private final int _weightPercent;
	
	/**
	 * Creates a new {@code OpEncumberedSkillCondition} using the provided stats.<br>
	 * This constructor initializes the required slot and weight percentages from a {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public OpEncumberedSkillCondition(StatSet params)
	{
		_slotsPercent = params.getInt("slotsPercent");
		_weightPercent = params.getInt("weightPercent");
	}
	
	/**
	 * Checks if the caster meets the requirements to use a specific skill.<br>
	 * This method verifies if the player is sufficiently encumbered by weight and inventory size.<br>
	 * It returns {@code true} only if both conditions are met.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the requirements are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if (!caster.isPlayer())
		{
			return false;
		}
		
		final Player player = caster.asPlayer();
		final int currentSlotsPercent = calcPercent(player.getInventoryLimit(), player.getInventory().getNonQuestSize());
		final int currentWeightPercent = calcPercent(player.getMaxLoad(), player.getCurrentLoad());
		return (currentSlotsPercent >= _slotsPercent) && (currentWeightPercent >= _weightPercent);
	}
	
	/**
	 * Calculates the remaining percentage of a value.<br>
	 * It subtracts the used percentage from {@code 100}.
	 * @param max The maximum possible value.
	 * @param current The current value being measured.
	 * @return The calculated percentage as an {@code int}.
	 */
	private int calcPercent(int max, int current)
	{
		return 100 - ((current * 100) / max);
	}
}
