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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.OlympiadConfig;
import org.l2jmobius.gameserver.data.xml.ClassListData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.entity.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneRespawn;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadGameTask;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.olympiad.ExOlympiadMatchEnd;

/**
 * Represents a specific zone configuration for an olympiad stadium.<br>
 * This class handles the unique behaviors and settings required for olympiad events.
 * @author durgus, DS
 */
public class OlympiadStadiumZone extends ZoneRespawn
{
	private final List<Door> _doors = new ArrayList<>(2);
	private final List<Spawn> _buffers = new ArrayList<>(2);
	private final List<Location> _spectatorLocations = new ArrayList<>(1);
	
	/**
	 * Creates a new {@code OlympiadStadiumZone} instance.<br>
	 * This constructor initializes the zone with a specific ID.<br>
	 * It also loads or creates the default settings for this zone.
	 * @param id The unique identifier for the stadium zone.
	 */
	public OlympiadStadiumZone(int id)
	{
		super(id);
		AbstractZoneSettings settings = ZoneManager.getSettings(getName());
		if (settings == null)
		{
			settings = new Settings();
		}
		
		setSettings(settings);
	}
	
	public class Settings extends AbstractZoneSettings
	{
		private OlympiadGameTask _task = null;
		
		protected Settings()
		{
		}
		
		public OlympiadGameTask getOlympiadTask()
		{
			return _task;
		}
		
		protected void setTask(OlympiadGameTask task)
		{
			_task = task;
		}
		
		@Override
		public void clear()
		{
			_task = null;
		}
	}
	
	/**
	 * Retrieves the configuration settings for this zone.<br>
	 * This method returns a {@code Settings} object.
	 * @return the {@code Settings} of the current zone.
	 */
	@Override
	public Settings getSettings()
	{
		return (Settings) super.getSettings();
	}
	
	/**
	 * Parses a location from the configuration file.<br>
	 * It handles specific types like {@code spectatorSpawn}.<br>
	 * If the type is not recognized, it uses the parent class logic.
	 * @param x The X coordinate of the location.
	 * @param y The Y coordinate of the location.
	 * @param z The Z coordinate of the location.
	 * @param type The type of location to parse.
	 */
	@Override
	public void parseLoc(int x, int y, int z, String type)
	{
		if ((type != null) && type.equals("spectatorSpawn"))
		{
			_spectatorLocations.add(new Location(x, y, z));
		}
		else
		{
			super.parseLoc(x, y, z, type);
		}
	}
	
	/**
	 * Adds a new task to the olympiad stadium settings.<br>
	 * This method links an {@code OlympiadGameTask} to the current zone.
	 * @param task The {@code OlympiadGameTask} to be registered.
	 */
	public void registerTask(OlympiadGameTask task)
	{
		getSettings().setTask(task);
	}
	
	/**
	 * Handles logic when a {@code Creature} enters the zone.<br>
	 * It sets the PVP status and checks for olympiad requirements.<br>
	 * Players who do not meet the entry criteria will be kicked.<br>
	 * Pets are automatically unsummoned if they are present.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, true);
		
		if (creature.isPlayer())
		{
			if (OlympiadConfig.OLYMPIAD_HIDE_NAMES)
			{
				final Player player = creature.asPlayer();
				final PlayerAppearance appearance = player.getAppearance();
				appearance.setVisibleName(ClassListData.getInstance().getClass(player.getPlayerClass()).getClassName());
				appearance.setVisibleTitle("");
				appearance.setVisibleClanData(0, 0, 0, 0, 0);
			}
			
			final OlympiadGameTask olympiadTask = getSettings().getOlympiadTask();
			if ((olympiadTask != null) && olympiadTask.isBattleStarted())
			{
				creature.sendPacket(SystemMessageId.YOU_HAVE_ENTERED_A_COMBAT_ZONE);
				olympiadTask.getGame().sendOlympiadInfo(creature);
			}
		}
		
		if (creature.isPlayable())
		{
			final Player player = creature.asPlayer();
			if (player != null)
			{
				// Only participants, observers and GMs allowed.
				if (!player.isGM() && !player.isInOlympiadMode() && !player.getObserverMode().isActive())
				{
					ThreadPool.execute(new KickPlayer(player));
				}
				else
				{
					// Check for pet.
					final Summon pet = player.getPet();
					if (pet != null)
					{
						pet.unSummon(player);
					}
				}
			}
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} leaves the zone.<br>
	 * It updates the {@code ZoneId} status for the creature.<br>
	 * It sends specific packets if a player leaves during an active olympiad task.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		creature.setInsideZone(ZoneId.PVP, false);
		
		if (creature.isPlayer())
		{
			if (OlympiadConfig.OLYMPIAD_HIDE_NAMES)
			{
				final PlayerAppearance appearance = creature.asPlayer().getAppearance();
				appearance.setVisibleName(null);
				appearance.setVisibleTitle(null);
				appearance.setVisibleClanData(-1, -1, -1, -1, -1);
			}
			
			final Settings settings = getSettings();
			if ((settings.getOlympiadTask() != null) && settings.getOlympiadTask().isBattleStarted())
			{
				creature.sendPacket(SystemMessageId.YOU_HAVE_LEFT_A_COMBAT_ZONE);
				creature.sendPacket(ExOlympiadMatchEnd.STATIC_PACKET);
			}
		}
	}
	
	private static class KickPlayer implements Runnable
	{
		private Player _player;
		
		protected KickPlayer(Player player)
		{
			_player = player;
		}
		
		@Override
		public void run()
		{
			if (_player != null)
			{
				_player.getServitors().values().forEach(s -> s.unSummon(_player));
				_player.teleToLocation(TeleportWhereType.TOWN, null);
				_player = null;
			}
		}
	}
	
	/**
	 * Retrieves the list of doors for this stadium.<br>
	 * This method returns all {@code Door} objects associated with the zone.
	 * @return a {@code List} containing all {@code Door} instances.
	 */
	public List<Door> getDoors()
	{
		return _doors;
	}
	
	/**
	 * Retrieves the list of buffer spawns for this zone.<br>
	 * These locations are used to manage player positioning.
	 * @return a {@code List} of {@code Spawn} objects.
	 */
	public List<Spawn> getBuffers()
	{
		return _buffers;
	}
	
	/**
	 * Retrieves the list of locations where spectators can spawn.<br>
	 * This method returns all predefined spectator points in the stadium.
	 * @return a {@code List} of {@code Location} objects representing spectator spawns.
	 */
	public List<Location> getSpectatorSpawns()
	{
		return _spectatorLocations;
	}
}
