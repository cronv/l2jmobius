/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package org.l2jmobius.gameserver.managers.games;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiArena;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiCubePlayer;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateiMsgType;
import org.l2jmobius.gameserver.mechanics.krateisCube.KrateisReward;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic and lifecycle of the Krateis Cube game mode.<br>
 * This class handles the initialization, rewards, and general mechanics for players participating in the cube event.
 * @author LordWinter
 */
public class KrateisCubeManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(KrateisCubeManager.class.getName());
	
	private final Map<Integer, KrateiArena> _arenas = new HashMap<>(3);
	private Npc _manager = null;
	private long _nextMatchTime;
	private boolean _registerActive = false;
	private boolean _isHalfAnHour = false;
	private EventState _state = EventState.REGISTRATION;
	
	private ScheduledFuture<?> _periodTask = null;
	private ScheduledFuture<?> _eventTask = null;
	private ScheduledFuture<?> _msgTask = null;
	
	private enum EventState
	{
		REGISTRATION,
		PREPARING,
		STARTED
	}
	
	/**
	 * Constructs a new instance of the {@code KrateisCubeManager}.<br>
	 * This constructor automatically calls the {@code load()} method.
	 */
	public KrateisCubeManager()
	{
		load();
	}
	
	/**
	 * Loads the configuration data for the Krateis Cube event.<br>
	 * This method parses the XML file and initializes the arena templates.<br>
	 * It also checks for the required spawn location to set up the event timing.
	 */
	@Override
	public void load()
	{
		parseDatapackFile("data/KrateisCube.xml");
		
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + _arenas.size() + " arena templates.");
		
		final Spawn spawn = SpawnTable.getInstance().getAnySpawn(32503);
		if (spawn != null)
		{
			_manager = spawn.getLastSpawn();
			recalcEventTime();
		}
		else
		{
			LOGGER.info(simpleName + ": Event can't be started because npc not found!");
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate the arena data.<br>
	 * It reads configuration values from the XML file structure.<br>
	 * The method extracts locations, doors, buffs, and rewards for each arena.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap map;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("arena".equalsIgnoreCase(d.getNodeName()))
					{
						map = d.getAttributes();
						final int id = Integer.parseInt(map.getNamedItem("id").getNodeValue());
						final int manager = Integer.parseInt(map.getNamedItem("manager").getNodeValue());
						final int minLevel = Integer.parseInt(map.getNamedItem("minLevel").getNodeValue());
						final int maxLevel = Integer.parseInt(map.getNamedItem("maxLevel").getNodeValue());
						final int minPlayers = Integer.parseInt(map.getNamedItem("minPlayers").getNodeValue());
						final int maxPlayers = Integer.parseInt(map.getNamedItem("maxPlayers").getNodeValue());
						
						final KrateiArena arena = new KrateiArena(id, manager, minLevel, maxLevel, minPlayers, maxPlayers);
						final StatSet params = new StatSet();
						
						for (Node kc = d.getFirstChild(); kc != null; kc = kc.getNextSibling())
						{
							if ("doorA".equalsIgnoreCase(kc.getNodeName()))
							{
								map = kc.getAttributes();
								final String doorList = map.getNamedItem("list") != null ? map.getNamedItem("list").getNodeValue() : "";
								if (!doorList.isEmpty())
								{
									final String[] doorSplint = doorList.split(",");
									for (String doorId : doorSplint)
									{
										final Door door = DoorData.getInstance().getDoor(Integer.parseInt(doorId));
										if (door != null)
										{
											arena.addDoorA(door);
										}
									}
								}
							}
							else if ("doorB".equalsIgnoreCase(kc.getNodeName()))
							{
								map = kc.getAttributes();
								final String doorList = map.getNamedItem("list") != null ? map.getNamedItem("list").getNodeValue() : "";
								if (!doorList.isEmpty())
								{
									final String[] doorSplint = doorList.split(",");
									for (String doorId : doorSplint)
									{
										final Door door = DoorData.getInstance().getDoor(Integer.parseInt(doorId));
										if (door != null)
										{
											arena.addDoorB(door);
										}
									}
								}
							}
							else if ("waitLocations".equalsIgnoreCase(kc.getNodeName()))
							{
								for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("point".equalsIgnoreCase(c.getNodeName()))
									{
										map = c.getAttributes();
										final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
										final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
										final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
										arena.addWaitLoc(new Location(x, y, z));
									}
								}
							}
							else if ("battleLocations".equalsIgnoreCase(kc.getNodeName()))
							{
								for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("point".equalsIgnoreCase(c.getNodeName()))
									{
										map = c.getAttributes();
										final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
										final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
										final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
										arena.addBattleLoc(new Location(x, y, z));
									}
								}
							}
							else if ("watcherLocations".equalsIgnoreCase(kc.getNodeName()))
							{
								for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("point".equalsIgnoreCase(c.getNodeName()))
									{
										map = c.getAttributes();
										final int x = Integer.parseInt(map.getNamedItem("x").getNodeValue());
										final int y = Integer.parseInt(map.getNamedItem("y").getNodeValue());
										final int z = Integer.parseInt(map.getNamedItem("z").getNodeValue());
										arena.addWatcherLoc(new Location(x, y, z));
									}
								}
							}
							else if ("buffs".equalsIgnoreCase(kc.getNodeName()))
							{
								for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("skill".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attr = c.getAttributes();
										final int skillId = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
										final int level = Integer.parseInt(attr.getNamedItem("level").getNodeValue());
										final Skill skill = SkillData.getInstance().getSkill(skillId, level);
										if (skill != null)
										{
											arena.addBuff(skill);
										}
									}
								}
							}
							else if ("rewards".equalsIgnoreCase(kc.getNodeName()))
							{
								for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("item".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attr = c.getAttributes();
										final int itemId = Integer.parseInt(attr.getNamedItem("id").getNodeValue());
										final long amount = Long.parseLong(attr.getNamedItem("amount").getNodeValue());
										final boolean useModifier = attr.getNamedItem("useModifers") != null ? Boolean.parseBoolean(attr.getNamedItem("useModifers").getNodeValue()) : false;
										arena.setReward(new KrateisReward(itemId, amount, useModifier));
									}
								}
							}
							else if ("add_parameters".equalsIgnoreCase(kc.getNodeName()))
							{
								for (Node c = kc.getFirstChild(); c != null; c = c.getNextSibling())
								{
									if ("set".equalsIgnoreCase(c.getNodeName()))
									{
										final NamedNodeMap attr = c.getAttributes();
										params.set(attr.getNamedItem("name").getNodeValue(), attr.getNamedItem("value").getNodeValue());
									}
								}
								
								arena.addParam(params);
							}
						}
						
						_arenas.put(arena.getId(), arena);
					}
				}
			}
		}
	}
	
	/**
	 * Prepares the Krateis Cube event by checking arena requirements.<br>
	 * It validates registered players and their positions in the world.<br>
	 * If enough valid players are found, it schedules the start of the battle.<br>
	 * Otherwise, it recalculates the remaining time for the event.
	 */
	public void prepareEvent()
	{
		if (_registerActive)
		{
			_registerActive = false;
		}
		
		if (_periodTask != null)
		{
			_periodTask.cancel(false);
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		if (_msgTask != null)
		{
			_msgTask.cancel(false);
		}
		
		boolean isActive = false;
		for (KrateiArena arena : _arenas.values())
		{
			if ((arena != null) && (arena.getPlayers().size() >= arena.getMinPlayers()))
			{
				int count = 0;
				final List<Player> invalidPlayers = new ArrayList<>();
				for (KrateiCubePlayer pl : arena.getPlayers().values())
				{
					if ((pl != null) && pl.isRegister())
					{
						if (pl.getPlayer().isInsideRadius3D(_manager, PlayerConfig.ALT_PARTY_RANGE))
						{
							pl.setIsInside(true);
							pl.setIsRegister(false);
							count++;
						}
						else
						{
							invalidPlayers.add(pl.getPlayer());
						}
					}
				}
				
				if (!invalidPlayers.isEmpty())
				{
					for (Player pl : invalidPlayers)
					{
						arena.removePlayer(pl);
					}
				}
				
				invalidPlayers.clear();
				
				if (count > 1)
				{
					arena.setIsBattleNow(true);
					arena.teleportToBattle(null);
					arena.waitTimeInfo();
					isActive = true;
				}
				else
				{
					// Match will not start, revert players to their registered state.
					for (KrateiCubePlayer pl : arena.getPlayers().values())
					{
						if ((pl != null) && pl.isInside())
						{
							pl.setIsInside(false);
							pl.setIsRegister(true);
						}
					}
				}
			}
		}
		
		if (isActive)
		{
			_eventTask = ThreadPool.schedule(this::startEvent, 11000);
			getManagerMessage(KrateiMsgType.STARTED);
		}
		else
		{
			recalcEventTime();
		}
	}
	
	/**
	 * Starts the active event for all arenas.<br>
	 * This method cancels any existing task and initiates the event state.<br>
	 * It also schedules a recurring task to recalculate the event time.
	 */
	public void startEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		for (KrateiArena arena : _arenas.values())
		{
			if ((arena != null) && arena.isBattleNow())
			{
				arena.startEvent();
				_state = EventState.STARTED;
			}
		}
		
		_periodTask = ThreadPool.schedule(this::recalcEventTime, 180000);
	}
	
	/**
	 * Stops all currently active events.<br>
	 * This method iterates through all {@code KrateiArena} objects and ends any ongoing battles.<br>
	 * It also recalculates the event time if registration is not active.
	 */
	public void abortEvent()
	{
		for (KrateiArena arena : _arenas.values())
		{
			if ((arena != null) && arena.isBattleNow())
			{
				arena.endEvent();
			}
		}
		
		if (!_registerActive)
		{
			recalcEventTime();
		}
	}
	
	/**
	 * Checks if the registration period is currently active.<br>
	 * Returns {@code true} if players can register for the event.<br>
	 * Returns {@code false} otherwise.
	 * @return The current registration status as a boolean.
	 */
	public boolean isRegisterTime()
	{
		return _registerActive;
	}
	
	/**
	 * Checks if the event is currently active.<br>
	 * Returns {@code true} if the state is {@code STARTED}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return The current activity status of the event.
	 */
	public boolean isActive()
	{
		return _state == EventState.STARTED;
	}
	
	/**
	 * Checks if the event is currently in the preparation state.
	 * @return {@code true} if the state is {@code PREPARING}, otherwise {@code false}.
	 */
	public boolean isPreparing()
	{
		return _state == EventState.PREPARING;
	}
	
	/**
	 * Sets the activation state of the event.<br>
	 * If {@code false}, the state is set to {@code REGISTRATION}.
	 * @param val The new activation status.
	 */
	public void setIsActivate(boolean val)
	{
		if (!val)
		{
			_state = EventState.REGISTRATION;
		}
	}
	
	/**
	 * Recalculates the timing for the next event.<br>
	 * This method updates the {@code _nextMatchTime} based on the current system clock.<br>
	 * It cancels existing tasks and schedules new ones for registration closing and notifications.<br>
	 * The internal state is set to {@code EventState.REGISTRATION} if it was not already started.
	 */
	public void recalcEventTime()
	{
		if (!_registerActive)
		{
			_registerActive = true;
		}
		
		if (_periodTask != null)
		{
			_periodTask.cancel(false);
		}
		
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		final Calendar calendar = Calendar.getInstance();
		final int minute = calendar.get(Calendar.MINUTE);
		if (minute >= 57)
		{
			calendar.add(Calendar.HOUR, 1);
			calendar.set(Calendar.MINUTE, 27);
		}
		else if ((minute >= 0) && (minute <= 26))
		{
			calendar.set(Calendar.MINUTE, 27);
			calendar.set(Calendar.SECOND, 0);
			_isHalfAnHour = true;
		}
		else
		{
			calendar.set(Calendar.MINUTE, 57);
			calendar.set(Calendar.SECOND, 0);
			_isHalfAnHour = false;
		}
		
		final long currentTime = System.currentTimeMillis();
		if (calendar.getTimeInMillis() < currentTime)
		{
			calendar.add(Calendar.DAY_OF_YEAR, 1);
		}
		
		_nextMatchTime = calendar.getTimeInMillis();
		final long lastTime = _nextMatchTime - currentTime;
		_eventTask = ThreadPool.schedule(this::closeRegistration, lastTime);
		getManagerMessage(KrateiMsgType.INITIALIZED);
		
		final int time = (int) ((lastTime / 1000) / 60);
		if (time >= 5)
		{
			_msgTask = ThreadPool.schedule(() -> getManagerMessage(KrateiMsgType.REGISTRATION_5), lastTime - 300000);
		}
		else if ((time >= 3) && (time < 5))
		{
			_msgTask = ThreadPool.schedule(() -> getManagerMessage(KrateiMsgType.REGISTRATION_3), lastTime - 180000);
		}
		else if ((time >= 1) && (time < 3))
		{
			_msgTask = ThreadPool.schedule(() -> getManagerMessage(KrateiMsgType.REGISTRATION_1), lastTime - 60000);
		}
		
		if (_state != EventState.STARTED)
		{
			_state = EventState.REGISTRATION;
		}
		
		// LOGGER.info(getClass().getSimpleName() + ": Next match " + TimeUtil.getDateTimeString(calendar.getTimeInMillis()));
	}
	
	/**
	 * Closes the current registration period.<br>
	 * This method cancels any active periodic tasks.<br>
	 * It sets {@code _registerActive} to {@code false}.<br>
	 * The manager state is changed back to {@code EventState.PREPARING}.
	 */
	public void closeRegistration()
	{
		if (_periodTask != null)
		{
			_periodTask.cancel(false);
		}
		
		_registerActive = false;
		getManagerMessage(KrateiMsgType.PREPATING);
		_state = EventState.PREPARING;
		_periodTask = ThreadPool.schedule(this::prepareEvent, 180000);
	}
	
	/**
	 * Broadcasts a specific message to all players based on the current event state.<br>
	 * It handles different messages for registration countdowns and match start alerts.<br>
	 * This method also schedules future messages using {@code ThreadPool}.
	 * @param state The current {@code KrateiMsgType} used to determine which message to send.
	 */
	private void getManagerMessage(KrateiMsgType state)
	{
		final long lastTime = _nextMatchTime - System.currentTimeMillis();
		switch (state)
		{
			case INITIALIZED:
			{
				final CreatureSay msg = new CreatureSay(_manager, ChatType.NPC_SHOUT, NpcStringId.YOU_HAVE_S1_MIN_TO_REGISTER_FOR_THE_MATCH);
				msg.addStringParameter(String.valueOf(!_isHalfAnHour ? 57 : 27));
				_manager.broadcastPacket(msg);
				break;
			}
			case REGISTRATION_5:
			{
				_manager.broadcastPacket(new CreatureSay(_manager, ChatType.NPC_SHOUT, NpcStringId.FIVE_MIN_REMAINING_TO_REGISTER_FOR_KRATEI_S_CUBE_MATCH));
				if (lastTime > 180000)
				{
					_msgTask = ThreadPool.schedule(() -> getManagerMessage(KrateiMsgType.REGISTRATION_3), lastTime - 180000);
				}
				else if (lastTime > 60000)
				{
					_msgTask = ThreadPool.schedule(() -> getManagerMessage(KrateiMsgType.REGISTRATION_1), lastTime - 60000);
				}
				break;
			}
			case REGISTRATION_3:
			{
				_manager.broadcastPacket(new CreatureSay(_manager, ChatType.NPC_SHOUT, NpcStringId.THREE_MIN_REMAINING_TO_REGISTER_FOR_KRATEI_S_CUBE_MATCH));
				if (lastTime > 60000)
				{
					_msgTask = ThreadPool.schedule(() -> getManagerMessage(KrateiMsgType.REGISTRATION_1), lastTime - 60000);
				}
				break;
			}
			case REGISTRATION_1:
			{
				_manager.broadcastPacket(new CreatureSay(_manager, ChatType.NPC_SHOUT, NpcStringId.ONE_MIN_REMAINING_TO_REGISTER_FOR_KRATEI_S_CUBE_MATCH));
				break;
			}
			case PREPATING:
			{
				final CreatureSay msg3 = new CreatureSay(_manager, ChatType.NPC_SHOUT, NpcStringId.THE_MATCH_STARTS_IN_S1_MIN);
				msg3.addStringParameter(String.valueOf(3));
				_manager.broadcastPacket(msg3);
				break;
			}
			case STARTED:
			{
				_manager.broadcastPacket(new CreatureSay(_manager, ChatType.NPC_SHOUT, NpcStringId.THE_MATCH_WILL_BEGIN_SHORTLY));
				break;
			}
		}
	}
	
	/**
	 * Returns the timestamp for the next scheduled match.<br>
	 * This value is used to determine when the next event begins.
	 * @return The time of the next match as a {@code long}.
	 */
	public long getNextMatchTime()
	{
		return _nextMatchTime;
	}
	
	/**
	 * Retrieves a specific arena based on its unique identifier.<br>
	 * This method looks up the arena in the internal map.
	 * @param id The unique integer ID of the arena to retrieve.
	 * @return The {@code KrateiArena} object associated with the provided ID, or {@code null} if not found.
	 */
	public KrateiArena getArenaId(int id)
	{
		return _arenas.get(id);
	}
	
	/**
	 * Retrieves all active arenas.<br>
	 * The map uses the arena ID as the key.
	 * @return A {@code Map} containing all {@code KrateiArena} objects.
	 */
	public Map<Integer, KrateiArena> getArenas()
	{
		return _arenas;
	}
	
	/**
	 * Retrieves the singleton instance of the {@code KrateisCubeManager}.<br>
	 * Use this method to access the global manager for Krateis Cube events.
	 * @return The active {@code KrateisCubeManager} instance.
	 */
	public static KrateisCubeManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final KrateisCubeManager INSTANCE = new KrateisCubeManager();
	}
}
