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
package org.l2jmobius.gameserver.entity.instancezone.conditions;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.entity.residences.ResidenceType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if an entity possesses a valid residence.<br>
 * This condition is used to verify ownership of a {@code ResidenceType} within an instance zone.
 * @author malyelfik
 */
public class ConditionHasResidence extends Condition
{
	/**
	 * Creates a new {@code ConditionHasResidence} object.<br>
	 * This condition checks if an actor has access to a residence.<br>
	 * It uses the provided template and parameters for validation.
	 * @param template The {@code InstanceTemplate} associated with this condition.
	 * @param parameters The {@code StatSet} containing specific values for the check.
	 * @param onlyLeader Set to {@code true} if only the clan leader can pass the check.
	 * @param showMessageAndHtml Set to {@code true} to display a message and HTML when the condition fails.
	 */
	public ConditionHasResidence(InstanceTemplate template, StatSet parameters, boolean onlyLeader, boolean showMessageAndHtml)
	{
		super(template, parameters, onlyLeader, showMessageAndHtml);
	}
	
	/**
	 * Checks if the {@code Player} belongs to a specific residence.<br>
	 * This method verifies the clan's ownership of a castle, fortress, or clan hall.
	 * @param player The {@code Player} being checked.
	 * @param npc The {@code Npc} associated with the condition.
	 * @return {@code true} if the player owns the required residence, otherwise {@code false}.
	 */
	@Override
	protected boolean test(Player player, Npc npc)
	{
		final Clan clan = player.getClan();
		if (clan == null)
		{
			return false;
		}
		
		boolean test = false;
		final StatSet params = getParameters();
		final int id = params.getInt("id");
		final ResidenceType type = params.getEnum("type", ResidenceType.class);
		switch (type)
		{
			case CASTLE:
			{
				test = clan.getCastleId() == id;
				break;
			}
			case FORTRESS:
			{
				test = clan.getFortId() == id;
				break;
			}
			case CLANHALL:
			{
				test = clan.getHideoutId() == id;
				break;
			}
		}
		
		return test;
	}
}
