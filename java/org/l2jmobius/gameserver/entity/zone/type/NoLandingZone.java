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
package org.l2jmobius.gameserver.entity.zone.type;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.MountType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Represents a zone where players and creatures are prohibited from landing.<br>
 * This type prevents entities from touching the ground within its boundaries.
 * @author durgus
 */
public class NoLandingZone extends ZoneType
{
	private int dismountDelay = 5;
	
	/**
	 * Creates a new {@code NoLandingZone} instance.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public NoLandingZone(int id)
	{
		super(id);
	}
	
	/**
	 * Sets a specific configuration parameter for this zone.<br>
	 * This method updates the {@code dismountDelay} if the name matches.<br>
	 * Otherwise, it passes the values to the parent class.
	 * @param name The name of the parameter to update.
	 * @param value The new value to assign to the parameter.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		if (name.equals("dismountDelay"))
		{
			dismountDelay = Integer.parseInt(value);
		}
		else
		{
			super.setParameter(name, value);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} enters the zone.<br>
	 * It checks if the creature is a {@code Player}.<br>
	 * It handles dismount restrictions and sends messages to players based on their mount type.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.NO_LANDING, true);
			
			final Player player = creature.asPlayer();
			
			if (player.isGM())
			{
				player.sendMessage("You have entered a no-landing zone. Dismount restrictions are ignored for GMs.");
				return;
			}
			
			if (player.getMountType() == MountType.WYVERN)
			{
				player.sendPacket(SystemMessageId.THIS_AREA_CANNOT_BE_ENTERED_WHILE_MOUNTED_ATOP_OF_A_WYVERN_YOU_WILL_BE_DISMOUNTED_FROM_YOUR_WYVERN_IF_YOU_DO_NOT_LEAVE);
				player.enteredNoLanding(dismountDelay);
			}
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves the zone.<br>
	 * It updates the internal state for players who exit.<br>
	 * It specifically handles logic for players riding a {@code MountType.WYVERN}.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.NO_LANDING, false);
			
			final Player player = creature.asPlayer();
			if (player.getMountType() == MountType.WYVERN)
			{
				player.exitedNoLanding();
			}
		}
	}
}
