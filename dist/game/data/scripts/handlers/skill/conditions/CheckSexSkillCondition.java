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
 * Checks if a skill can be used based on the gender of the character.<br>
 * This condition ensures that specific skills are restricted to either male or female characters.
 * @author UnAfraid
 */
public class CheckSexSkillCondition implements ISkillCondition
{
	private final boolean _isFemale;
	
	/**
	 * This constructor initializes the condition based on gender requirements.<br>
	 * It checks if a specific sex is required to use the skill.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public CheckSexSkillCondition(StatSet params)
	{
		_isFemale = params.getBoolean("isFemale");
	}
	
	/**
	 * Checks if the {@code caster} meets the gender requirements for a skill.<br>
	 * This method verifies if the {@code caster} is a player and matches the required sex.
	 * @param caster The entity attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the gender condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		return caster.isPlayer() && (caster.asPlayer().getAppearance().isFemale() == _isFemale);
	}
}
