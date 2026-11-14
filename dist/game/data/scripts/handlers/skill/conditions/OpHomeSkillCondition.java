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

import org.l2jmobius.gameserver.data.xml.ClanHallData;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.residences.ResidenceType;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if the player is currently inside their home residence.<br>
 * This condition ensures that a {@code Skill} can only be used while the character is at their designated home.
 * @author Sdw
 */
public class OpHomeSkillCondition implements ISkillCondition
{
	private final ResidenceType _type;
	
	/**
	 * Creates a new {@code OpHomeSkillCondition} instance.<br>
	 * This constructor initializes the condition using provided parameters.<br>
	 * It extracts the residence type from the input set.
	 * @param params The {@code StatSet} containing the required configuration data.
	 */
	public OpHomeSkillCondition(StatSet params)
	{
		_type = params.getEnum("type", ResidenceType.class);
	}
	
	/**
	 * Checks if the caster meets the requirements to use a specific skill.<br>
	 * This method verifies ownership of a residence based on the configured type.<br>
	 * It returns {@code true} only if the player belongs to a clan with the required property.
	 * @param caster The {@code Creature} attempting to cast the skill.
	 * @param skill The {@code Skill} being used.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if (caster.isPlayer())
		{
			final Clan clan = caster.asPlayer().getClan();
			if (clan != null)
			{
				switch (_type)
				{
					case CASTLE:
					{
						return CastleManager.getInstance().getCastleByOwner(clan) != null;
					}
					case FORTRESS:
					{
						return FortManager.getInstance().getFortByOwner(clan) != null;
					}
					case CLANHALL:
					{
						return ClanHallData.getInstance().getClanHallByClan(clan) != null;
					}
				}
			}
		}
		
		return false;
	}
}
