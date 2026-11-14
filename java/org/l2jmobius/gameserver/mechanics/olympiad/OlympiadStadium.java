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
package org.l2jmobius.gameserver.mechanics.olympiad;

import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.ObserverMode;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.OlympiadStadiumZone;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchEnd;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadUserInfo;

/**
 * Manages the logic and state of an {@code OlympiadStadium} instance.<br>
 * This class handles the initialization, progression, and completion of olympiad matches.
 * @author JIV
 */
public class OlympiadStadium
{
	private static final Logger LOGGER = Logger.getLogger(OlympiadStadium.class.getName());
	
	private final OlympiadStadiumZone _zone;
	private final int _stadiumId;
	private final Instance _instance;
	private final List<Spawn> _buffers;
	private OlympiadGameTask _task = null;
	
	/**
	 * Creates a new {@code OlympiadStadium} instance.<br>
	 * This constructor initializes the zone and creates a new {@code Instance}.<br>
	 * It also prepares the buffer spawns for the stadium.
	 * @param olyzone The {@code OlympiadStadiumZone} used to create the stadium.
	 * @param stadiumId The unique identifier for this specific stadium.
	 */
	protected OlympiadStadium(OlympiadStadiumZone olyzone, int stadiumId)
	{
		_zone = olyzone;
		_stadiumId = stadiumId;
		_instance = InstanceManager.getInstance().createInstance(olyzone.getInstanceTemplateId(), null);
		_buffers = _instance.getNpcs().stream().map(Npc::getSpawn).toList();
		_buffers.stream().map(Spawn::getLastSpawn).forEach(Npc::deleteMe);
	}
	
	/**
	 * Retrieves the {@code OlympiadStadiumZone} associated with this stadium.<br>
	 * This method returns the internal zone object used for olympiad mechanics.
	 * @return The {@code OlympiadStadiumZone} instance.
	 */
	public OlympiadStadiumZone getZone()
	{
		return _zone;
	}
	
	/**
	 * This method assigns a specific task to the stadium.<br>
	 * It updates the internal {@code _task} field with the provided object.
	 * @param task The {@code OlympiadGameTask} to be registered.
	 */
	public void registerTask(OlympiadGameTask task)
	{
		_task = task;
	}
	
	/**
	 * Retrieves the current game task for this stadium.<br>
	 * This method returns the {@code OlympiadGameTask} object.<br>
	 * It may return {@code null} if no task is currently active.
	 * @return The current {@code OlympiadGameTask} or {@code null}.
	 */
	public OlympiadGameTask getTask()
	{
		return _task;
	}
	
	/**
	 * Retrieves the {@code Instance} associated with this stadium.<br>
	 * This method returns the current active instance.
	 * @return The {@code Instance} object.
	 */
	public Instance getInstance()
	{
		return _instance;
	}
	
	/**
	 * Opens all doors within the current {@code Instance}.<br>
	 * This method iterates through every {@code Door} object.<br>
	 * It calls the {@code openMe} method on each door.
	 */
	public void openDoors()
	{
		_instance.getDoors().forEach(Door::openMe);
	}
	
	/**
	 * Closes all doors within the current {@code Instance}.<br>
	 * This method iterates through every {@code Door} object.<br>
	 * It calls the {@code closeMe} method on each door.
	 */
	public void closeDoors()
	{
		_instance.getDoors().forEach(Door::closeMe);
	}
	
	/**
	 * Spawns all buffer objects in the stadium.<br>
	 * This method iterates through the {@code _buffers} list.<br>
	 * It calls {@code doSpawn(false)} on each {@code Spawn} object.
	 */
	public void spawnBuffers()
	{
		_buffers.forEach(spawn -> spawn.doSpawn(false));
	}
	
	/**
	 * Removes all active buffer entities from the stadium.<br>
	 * This method iterates through the {@code _buffers} list.<br>
	 * It calls {@code deleteMe()} on each non-null {@code Npc}.
	 */
	public void deleteBuffers()
	{
		_buffers.stream().map(Spawn::getLastSpawn).filter(Objects::nonNull).forEach(Npc::deleteMe);
	}
	
	/**
	 * Sends the current olympiad status to relevant players.<br>
	 * This method creates an {@code ExOlympiadUserInfo} packet for the given {@code Player}.<br>
	 * It sends this packet to all observers and players on the opposing side.
	 * @param player The {@code Player} whose information should be broadcast.
	 */
	public void broadcastStatusUpdate(Player player)
	{
		final ExOlympiadUserInfo packet = new ExOlympiadUserInfo(player);
		for (Player target : _instance.getPlayers())
		{
			if (target.getObserverMode().isActive() || (target.getOlympiadSide() != player.getOlympiadSide()))
			{
				target.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Sends a network packet to all players in the current instance.<br>
	 * This method uses the {@code Instance} object to distribute the data.
	 * @param packet The {@code ServerPacket} to be sent to everyone.
	 */
	public void broadcastPacket(ServerPacket packet)
	{
		_instance.broadcastPacket(packet);
	}
	
	/**
	 * Sends a specific packet to all players in observer mode.<br>
	 * This method iterates through all players in the current {@code Instance}.<br>
	 * It checks if each player is currently viewing the event as an observer.
	 * @param packet The {@code ServerPacket} to be sent to the observers.
	 */
	public void broadcastPacketToObservers(ServerPacket packet)
	{
		for (Player target : _instance.getPlayers())
		{
			if (target.getObserverMode().isActive())
			{
				target.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Updates the PvP status for all players currently inside the instance.<br>
	 * This method checks if an {@code OlympiadGameTask} exists before proceeding.<br>
	 * It sets whether characters are in a {@code ZoneId.PVP} area and updates their start status.
	 * @param value The boolean state to set for PvP and olympiad start flags.
	 */
	public void makeZonePvPForCharsInside(boolean value)
	{
		if (_task == null)
		{
			return;
		}
		
		if (value)
		{
			for (Player player : _instance.getPlayers())
			{
				player.setInsideZone(ZoneId.PVP, true);
				player.setOlympiadStart(true);
				_task.getGame().sendOlympiadInfo(player);
			}
		}
		else
		{
			for (Player player : _instance.getPlayers())
			{
				player.setInsideZone(ZoneId.PVP, false);
				player.setOlympiadStart(false);
				_task.getGame().sendOlympiadInfo(player);
			}
		}
	}
	
	/**
	 * Updates the zone status for all players currently inside the instance.<br>
	 * This method checks if a battle has started via {@code OlympiadGameTask}.<br>
	 * It sends the appropriate system message and updates the {@code ZoneId.PVP} status.
	 */
	public void updateZoneStatusForCharactersInside()
	{
		if (_task == null)
		{
			return;
		}
		
		final boolean battleStarted = _task.isBattleStarted();
		final boolean battleEnded = _task.isBattleFinished();
		// final SystemMessage sm;
		// if (battleStarted)
		// {
		// sm = new SystemMessage(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
		// }
		// else
		// {
		// sm = new SystemMessage(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
		// }
		
		for (Player player : _instance.getPlayers())
		{
			if (player.getObserverMode().isActive())
			{
				return;
			}
			
			if (battleStarted)
			{
				player.setInsideZone(ZoneId.PVP, true);
				// player.sendPacket(sm);
			}
			else
			{
				player.setInsideZone(ZoneId.PVP, false);
				// player.sendPacket(sm);
				// player.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
			}
			
			if (battleEnded)
			{
				player.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
			}
		}
	}
	
	/**
	 * Updates the location and information for all players in observer mode.<br>
	 * This method checks if an {@code OlympiadGameTask} is active.<br>
	 * It moves observers to random spectator spawns within the zone.<br>
	 * Finally, it sends the current olympiad info to each observer.
	 */
	public void updateZoneInfoForObservers()
	{
		if (_task == null)
		{
			return;
		}
		
		for (Player player : _instance.getPlayers())
		{
			final ObserverMode observerMode = player.getObserverMode();
			if (!observerMode.isActive())
			{
				return;
			}
			
			final List<Location> spectatorSpawns = getZone().getSpectatorSpawns();
			if (spectatorSpawns.isEmpty())
			{
				LOGGER.warning(getClass().getSimpleName() + ": Zone: " + getZone() + " doesn't have specatator spawns defined!");
				return;
			}
			
			final Location loc = spectatorSpawns.get(Rnd.get(spectatorSpawns.size()));
			observerMode.enterOlympiad(loc, _stadiumId);
			
			_task.getGame().sendOlympiadInfo(player);
		}
	}
}
