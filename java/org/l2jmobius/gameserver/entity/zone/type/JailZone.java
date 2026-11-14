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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.tasks.player.TeleportTask;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Represents a specific type of zone used to confine players.<br>
 * This {@code JailZone} handles the logic for restricted areas where movement is limited.
 * @author durgus
 */
public class JailZone extends ZoneType
{
	private static final Location JAIL_IN_LOC = new Location(-114356, -249645, -2984);
	private static final Location JAIL_OUT_LOC = new Location(17836, 170178, -3507);
	
	/**
	 * Creates a new instance of a {@code JailZone}.<br>
	 * This constructor initializes the zone with a specific identifier.
	 * @param id The unique identification number for this zone.
	 */
	public JailZone(int id)
	{
		super(id);
	}
	
	/**
	 * This method is called when a {@code Creature} enters the jail zone.<br>
	 * It updates various zone flags for players based on the current configuration.<br>
	 * These flags can affect PvP status, summoning, and trading.
	 * @param creature The {@code Creature} that has entered the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.setInsideZone(ZoneId.JAIL, true);
			creature.setInsideZone(ZoneId.NO_SUMMON_FRIEND, true);
			if (GeneralConfig.JAIL_IS_PVP)
			{
				creature.setInsideZone(ZoneId.PVP, true);
				creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
			}
			
			if (GeneralConfig.JAIL_DISABLE_TRANSACTION)
			{
				creature.setInsideZone(ZoneId.NO_STORE, true);
			}
		}
	}
	
	/**
	 * Handles the logic when a {@code Creature} leaves the jail zone.<br>
	 * It updates various zone flags for players who exit.<br>
	 * If a player is still jailed, they are teleported back to the jail location after 2000 milliseconds.
	 * @param creature The {@code Creature} that is leaving the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			player.setInsideZone(ZoneId.JAIL, false);
			player.setInsideZone(ZoneId.NO_SUMMON_FRIEND, false);
			
			if (GeneralConfig.JAIL_IS_PVP)
			{
				creature.setInsideZone(ZoneId.PVP, false);
				creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
			}
			
			if (player.isJailed())
			{
				// When a player wants to exit jail even if he is still jailed, teleport him back to jail.
				ThreadPool.schedule(new TeleportTask(player, JAIL_IN_LOC), 2000);
				creature.sendMessage("You cannot cheat your way out of here. You must wait until your jail time is over.");
			}
			
			if (GeneralConfig.JAIL_DISABLE_TRANSACTION)
			{
				creature.setInsideZone(ZoneId.NO_STORE, false);
			}
		}
	}
	
	/**
	 * Retrieves the entry location for the jail zone.<br>
	 * This method returns a constant {@code Location} object.
	 * @return The {@code Location} where players enter the jail.
	 */
	public static Location getLocationIn()
	{
		return JAIL_IN_LOC;
	}
	
	/**
	 * Retrieves the exit location for the jail zone.<br>
	 * This method returns a constant {@code Location} object.
	 * @return The {@code Location} where players are released from the jail.
	 */
	public static Location getLocationOut()
	{
		return JAIL_OUT_LOC;
	}
}
