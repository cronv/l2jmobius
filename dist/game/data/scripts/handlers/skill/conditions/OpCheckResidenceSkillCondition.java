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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a player is currently inside a {@code ClanHall} residence.<br>
 * This condition ensures that the skill can only be used within specific residential areas.
 * @author Sdw
 */
public class OpCheckResidenceSkillCondition implements ISkillCondition
{
	private final Set<Integer> _residenceIds = new HashSet<>();
	private final boolean _isWithin;
	
	/**
	 * Creates a new condition to check if a player is inside a specific residence.<br>
	 * It initializes the required residence IDs and the proximity status from the provided data.
	 * @param params The {@code StatSet} containing configuration values like "residenceIds" and "isWithin".
	 */
	public OpCheckResidenceSkillCondition(StatSet params)
	{
		_residenceIds.addAll(params.getList("residenceIds", Integer.class));
		_isWithin = params.getBoolean("isWithin");
	}
	
	/**
	 * Checks if the caster meets the residence requirements for a skill.<br>
	 * This method verifies the player's clan hall status.<br>
	 * It returns {@code true} if the condition is met and {@code false} otherwise.
	 * @param caster The creature attempting to use the skill.
	 * @param skill The skill being cast.
	 * @param target The object being targeted by the skill.
	 * @return {@code true} if the skill can be used, {@code false} otherwise.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if (caster.isPlayer())
		{
			final Clan clan = caster.asPlayer().getClan();
			if (clan != null)
			{
				final ClanHall clanHall = ClanHallData.getInstance().getClanHallByClan(clan);
				if (clanHall != null)
				{
					return _isWithin ? _residenceIds.contains(clanHall.getResidenceId()) : !_residenceIds.contains(clanHall.getResidenceId());
				}
			}
		}
		
		return false;
	}
}
