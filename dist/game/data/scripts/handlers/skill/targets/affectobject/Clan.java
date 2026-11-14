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
package handlers.skill.targets.affectobject;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IAffectObjectHandler;
import org.l2jmobius.gameserver.mechanics.skill.targets.AffectObject;

/**
 * Handles skill effects that target a {@code Clan} object.<br>
 * This class processes interactions between skills and clan-specific entities.
 * @author Nik
 */
public class Clan implements IAffectObjectHandler
{
	/**
	 * Checks if the {@code target} is a valid object for the {@code creature}.<br>
	 * This method returns {@code true} if both entities are in the same clan.<br>
	 * It also returns {@code true} if the {@code creature} and {@code target} are the same entity.
	 * @param creature The entity performing the action.
	 * @param target The entity being targeted.
	 * @return {@code true} if the target is valid, otherwise {@code false}.
	 */
	@Override
	public boolean checkAffectedObject(Creature creature, Creature target)
	{
		if (creature == target)
		{
			return true;
		}
		
		final Player player = creature.asPlayer();
		if (player != null)
		{
			final org.l2jmobius.gameserver.entity.clan.Clan clan = player.getClan();
			if (clan != null)
			{
				return clan == target.getClan();
			}
		}
		else if (creature.isNpc() && target.isNpc())
		{
			return creature.asNpc().isInMyClan(target.asNpc());
		}
		
		return false;
	}
	
	/**
	 * Retrieves the type of affect object associated with this handler.<br>
	 * This method identifies the specific {@code AffectObject} category.
	 * @return The {@code AffectObject} enum value for this class.
	 */
	@Override
	public Enum<AffectObject> getAffectObjectType()
	{
		return AffectObject.CLAN;
	}
}
