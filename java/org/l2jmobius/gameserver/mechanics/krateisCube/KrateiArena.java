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
package org.l2jmobius.gameserver.mechanics.krateisCube;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.custom.PremiumSystemConfig;
import org.l2jmobius.gameserver.data.SpawnTable;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.KrateisMatchManager;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.games.KrateisCubeManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExPCCafePointInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCMyRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRecord;
import org.l2jmobius.gameserver.network.serverpackets.ExPVPMatchCCRetire;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic and environment for the {@code KrateisCube} arena.<br>
 * This class handles player interactions, match progression, and spatial rules within the cube.
 * @author LordWinter
 */
public class KrateiArena
{
	private final int _id;
	private final int _minLevel;
	private final int _maxLevel;
	private final int _minPlayers;
	private final int _maxPlayers;
	private KrateisMatchManager _manager = null;
	private final Map<Player, KrateiCubePlayer> _players = new ConcurrentHashMap<>();
	private final List<Skill> _buffs = new ArrayList<>();
	private final List<Door> _doorListA = new ArrayList<>();
	private final List<Door> _doorListB = new ArrayList<>();
	private final List<Location> _waitLoc = new ArrayList<>();
	private final List<Location> _battleLoc = new ArrayList<>();
	private final List<Location> _watcherLoc = new ArrayList<>();
	private final List<KrateisReward> _rewards = new ArrayList<>();
	private final List<Npc> _watchers = new ArrayList<>();
	
	private StatSet _params;
	
	private ScheduledFuture<?> _eventTask = null;
	private ScheduledFuture<?> _timeTask = null;
	private ScheduledFuture<?> _watcherTask = null;
	private ScheduledFuture<?> _doorTask = null;
	
	private boolean _doorsRotation = false;
	private boolean _watcherRotation = false;
	private boolean _isBattleNow = false;
	private boolean _isActiveNow = false;
	
	/**
	 * Creates a new instance of a {@code KrateiArena}.<br>
	 * This constructor initializes the arena with specific level and player requirements.<br>
	 * It also links the arena to its corresponding manager based on the provided ID.
	 * @param id The unique identifier for this arena.
	 * @param manager The manager associated with the arena.
	 * @param minLevel The minimum character level required to enter.
	 * @param maxLevel The maximum character level allowed in the arena.
	 * @param minPlayers The minimum number of players needed to start a match.
	 * @param maxPlayers The maximum number of players allowed in the arena.
	 */
	public KrateiArena(int id, int manager, int minLevel, int maxLevel, int minPlayers, int maxPlayers)
	{
		_id = id;
		_minLevel = minLevel;
		_maxLevel = maxLevel;
		_minPlayers = minPlayers;
		_maxPlayers = maxPlayers;
		_rewards.clear();
		
		final Spawn spawn = SpawnTable.getInstance().getAnySpawn(manager);
		if (spawn != null)
		{
			_manager = (KrateisMatchManager) spawn.getLastSpawn();
			_manager.setArenaId(getId());
		}
	}
	
	/**
	 * Retrieves the unique identifier for this arena.
	 * @return the {@code int} id of the arena.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Retrieves the minimum level required to enter this arena.
	 * @return the {@code int} value of the minimum level.
	 */
	public int getMinLevel()
	{
		return _minLevel;
	}
	
	/**
	 * Returns the maximum level allowed for this arena.<br>
	 * This value is used to restrict entry based on player level.
	 * @return The maximum level as an {@code int}.
	 */
	public int getMaxLevel()
	{
		return _maxLevel;
	}
	
	/**
	 * Returns the minimum number of players required for this arena.<br>
	 * This value is used to determine if a match can start.
	 * @return the minimum player count as an {@code int}.
	 */
	public int getMinPlayers()
	{
		return _minPlayers;
	}
	
	/**
	 * Retrieves the maximum number of players allowed in this arena.
	 * @return The maximum player count as an {@code int}.
	 */
	public int getMaxPlayers()
	{
		return _maxPlayers;
	}
	
	/**
	 * Retrieves the manager instance for this arena.<br>
	 * This provides access to the {@code KrateisMatchManager} responsible for handling match logic.
	 * @return The {@code KrateisMatchManager} associated with this arena.
	 */
	public KrateisMatchManager getManager()
	{
		return _manager;
	}
	
	/**
	 * Checks if a battle is currently in progress.
	 * @return {@code true} if the battle is active, {@code false} otherwise.
	 */
	public boolean isBattleNow()
	{
		return _isBattleNow;
	}
	
	/**
	 * Updates the status of whether a battle is currently active.<br>
	 * Sets the {@code _isBattleNow} field to the provided {@code boolean} value.
	 * @param value The new state for the current battle.
	 */
	public void setIsBattleNow(boolean value)
	{
		_isBattleNow = value;
	}
	
	/**
	 * Checks if the arena is currently active.
	 * @return {@code true} if the arena is active, {@code false} otherwise.
	 */
	public boolean isActiveNow()
	{
		return _isActiveNow;
	}
	
	/**
	 * Adds a new reward to the list of rewards.<br>
	 * This method updates the internal collection of {@code KrateisReward} objects.
	 * @param ucReward The {@code KrateisReward} object to be added.
	 */
	public void setReward(KrateisReward ucReward)
	{
		_rewards.add(ucReward);
	}
	
	/**
	 * Retrieves the list of rewards for this arena.
	 * @return a {@code List} of {@code KrateisReward} objects.
	 */
	public List<KrateisReward> getRewards()
	{
		return _rewards;
	}
	
	/**
	 * Adds a new {@code Skill} to the list of buffs.<br>
	 * This method updates the internal buff collection for this arena.
	 * @param skill The {@code Skill} object to be added.
	 */
	public void addBuff(Skill skill)
	{
		_buffs.add(skill);
	}
	
	/**
	 * Retrieves the list of all active buffs.<br>
	 * This method returns a {@code List} containing {@code Skill} objects.
	 * @return A {@code List} of {@code Skill} objects.
	 */
	public List<Skill> getBuffList()
	{
		return _buffs;
	}
	
	/**
	 * Adds a {@code Door} to the list of doors for side A.<br>
	 * This method registers a new door instance into the internal collection.
	 * @param door The {@code Door} object to be added.
	 */
	public void addDoorA(Door door)
	{
		_doorListA.add(door);
	}
	
	/**
	 * Retrieves the list of doors for area A.
	 * @return a {@code List} of {@code Door} objects.
	 */
	public List<Door> getDoorListA()
	{
		return _doorListA;
	}
	
	/**
	 * Adds a {@code Door} to the list of doors for side B.<br>
	 * This method registers a new door instance into the internal collection.
	 * @param door The {@code Door} object to be added.
	 */
	public void addDoorB(Door door)
	{
		_doorListB.add(door);
	}
	
	/**
	 * Retrieves the list of doors associated with area B.
	 * @return a {@code List} of {@code Door} objects.
	 */
	public List<Door> getDoorListB()
	{
		return _doorListB;
	}
	
	/**
	 * Retrieves the list of waiting locations.<br>
	 * This method returns all {@code Location} objects stored for this arena.
	 * @return a {@code List} of {@code Location} objects.
	 */
	public List<Location> getWaitLoc()
	{
		return _waitLoc;
	}
	
	/**
	 * Adds a new waiting location to the list.<br>
	 * This method stores a {@code Location} object in the internal wait locations collection.
	 * @param loc The {@code Location} to be added.
	 */
	public void addWaitLoc(Location loc)
	{
		_waitLoc.add(loc);
	}
	
	/**
	 * Retrieves the list of battle locations.<br>
	 * This method returns all {@code Location} objects associated with the battle area.
	 * @return a {@code List} of {@code Location} objects.
	 */
	public List<Location> getBattleLoc()
	{
		return _battleLoc;
	}
	
	/**
	 * Adds a new location to the battle area.<br>
	 * This method stores the provided {@code Location} in the internal list of battle locations.
	 * @param loc The {@code Location} to be added to the battle area.
	 */
	public void addBattleLoc(Location loc)
	{
		_battleLoc.add(loc);
	}
	
	/**
	 * Retrieves the list of watcher locations.<br>
	 * This method returns all {@code Location} objects associated with watchers.
	 * @return a {@code List} of {@code Location} objects.
	 */
	public List<Location> getWatcherLoc()
	{
		return _watcherLoc;
	}
	
	/**
	 * Adds a new watcher location to the list.<br>
	 * This method registers a {@code Location} for watchers.
	 * @param loc The {@code Location} to be added.
	 */
	public void addWatcherLoc(Location loc)
	{
		_watcherLoc.add(loc);
	}
	
	/**
	 * Sets the parameters for this arena.<br>
	 * This method updates the internal {@code _params} field with the provided data.
	 * @param params The {@code StatSet} containing the configuration values to apply.
	 */
	public void addParam(StatSet params)
	{
		_params = params;
	}
	
	/**
	 * Retrieves the current set of parameters for this arena.
	 * @return the {@code StatSet} containing the configuration data.
	 */
	public StatSet getParams()
	{
		return _params;
	}
	
	/**
	 * Registers a {@code Player} into the current arena.<br>
	 * This method checks if the player is already registered.<br>
	 * If not, it creates a new registration and updates the player state.
	 * @param player The {@code Player} to register.
	 * @return {@code true} if the player was successfully registered, or {@code false} if they were already registered.
	 */
	public boolean addRegisterPlayer(Player player)
	{
		if (!_players.containsKey(player))
		{
			final KrateiCubePlayer pl = _players.computeIfAbsent(player, _ -> new KrateiCubePlayer(player));
			pl.setIsRegister(true);
			player.setKrateiArena(this);
			player.setRegisteredOnEvent(true);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Removes a {@code Player} from the current arena.<br>
	 * This method clears the player's registration status and arena association.
	 * @param player The {@code Player} to be removed.
	 * @return {@code true} if the player was successfully removed, {@code false} otherwise.
	 */
	public boolean removePlayer(Player player)
	{
		if (_players.containsKey(player))
		{
			_players.remove(player);
			player.setKrateiArena(null);
			player.setRegisteredOnEvent(false);
			return true;
		}
		
		return false;
	}
	
	/**
	 * Checks if a specific {@code Player} is currently registered in the match.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player is registered, {@code false} otherwise.
	 */
	public boolean isRegisterPlayer(Player player)
	{
		return _players.containsKey(player);
	}
	
	/**
	 * Retrieves the current list of players in the match.
	 * @return A {@code Map} where the key is a {@code Player} and the value is their corresponding {@code KrateiCubePlayer} object.
	 */
	public Map<Player, KrateiCubePlayer> getPlayers()
	{
		return _players;
	}
	
	/**
	 * Starts the Krateis Cube event.<br>
	 * This method cancels any existing tasks and resets the door rotation status.<br>
	 * It spawns watchers at the defined locations and teleports all players to the arena.<br>
	 * Finally, it schedules the end time, time info updates, and watcher rotations.
	 */
	public void startEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(false);
		}
		
		if (_timeTask != null)
		{
			_timeTask.cancel(false);
		}
		
		if (_watcherTask != null)
		{
			_watcherTask.cancel(false);
		}
		
		if (_doorTask != null)
		{
			_doorTask.cancel(false);
		}
		
		_doorsRotation = true;
		changeDoorStatus(true);
		
		final List<Location> watcherLocs = getWatcherLoc();
		if (!watcherLocs.isEmpty())
		{
			for (Location loc : watcherLocs)
			{
				if (loc != null)
				{
					final Npc npc = Quest.addSpawn(18601, loc, false, 0);
					npc.disableCoreAI(true);
					npc.setImmobilized(true);
					_watchers.add(npc);
				}
			}
			
			_watcherRotation = true;
		}
		
		teleportAllPlayers();
		final long time = 1200000;
		_eventTask = ThreadPool.schedule(this::endEvent, time);
		_timeTask = ThreadPool.schedule(this::timeLeftInfo, time - 11000);
		_watcherTask = ThreadPool.schedule(this::changeWatchers, getParams().getInt("watcherRotationTime") * 1000);
		_isActiveNow = true;
	}
	
	/**
	 * Stops the current event and cleans up all active tasks.<br>
	 * Cancels scheduled tasks for watchers and doors.<br>
	 * Removes all watcher NPCs from the world.<br>
	 * Teleports participants to a safe location and clears their status.
	 */
	public void endEvent()
	{
		if (_eventTask != null)
		{
			_eventTask.cancel(true);
			_eventTask = null;
		}
		
		if (_watcherTask != null)
		{
			_watcherTask.cancel(true);
			_watcherTask = null;
		}
		
		if (_doorTask != null)
		{
			_doorTask.cancel(true);
			_doorTask = null;
		}
		
		_watcherRotation = false;
		changeDoorStatus(false);
		
		if (!_watchers.isEmpty())
		{
			for (Npc npc : _watchers)
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
			}
			
			_watchers.clear();
		}
		
		final Set<Player> participants = getPlayers().keySet();
		if ((participants != null) && !participants.isEmpty())
		{
			for (Player player : participants)
			{
				if (player != null)
				{
					removeAllEffects(player);
					player.teleToLocation(-70381, -70937, -1428, 0, true);
					player.setKrateiArena(null);
					player.setRegisteredOnEvent(false);
					player.sendPacket(ExPVPMatchCCRetire.STATIC);
					player.setCanRevive(true);
				}
			}
			
			checkRewardPlayers(getPlayers());
		}
		
		_players.clear();
		_isBattleNow = false;
		_isActiveNow = false;
		KrateisCubeManager.getInstance().setIsActivate(false);
	}
	
	/**
	 * Updates the current set of watcher NPCs.<br>
	 * This method cancels any existing tasks and removes old watchers from the world.<br>
	 * It spawns new watchers based on the configured locations.<br>
	 * Finally, it toggles the rotation state and schedules the next update.
	 */
	private void changeWatchers()
	{
		if (_watcherTask != null)
		{
			_watcherTask.cancel(true);
			_watcherTask = null;
		}
		
		if (!_watchers.isEmpty())
		{
			for (Npc npc : _watchers)
			{
				if (npc != null)
				{
					npc.deleteMe();
				}
			}
			
			_watchers.clear();
		}
		
		final List<Location> watcherLocs = getWatcherLoc();
		if (!watcherLocs.isEmpty())
		{
			for (Location loc : watcherLocs)
			{
				if (loc != null)
				{
					final Npc npc = Quest.addSpawn(_watcherRotation ? 18602 : 18601, loc, false, 0);
					npc.disableCoreAI(true);
					npc.setImmobilized(true);
					_watchers.add(npc);
				}
			}
		}
		
		if (_watcherRotation)
		{
			_watcherRotation = false;
		}
		else
		{
			_watcherRotation = true;
		}
		
		_watcherTask = ThreadPool.schedule(this::changeWatchers, getParams().getInt("watcherRotationTime") * 1000);
	}
	
	/**
	 * Retrieves a list of participants for the current match.<br>
	 * The results are sorted by points in descending order.<br>
	 * Returns {@code null} if no players are found.
	 * @return A {@code Map} where keys are {@code Player} objects and values are their scores.
	 */
	private Map<Player, Integer> getParticipants()
	{
		final Map<Player, Integer> participants = new HashMap<>();
		final Map<Player, KrateiCubePlayer> playersList = getPlayers();
		if (!playersList.isEmpty())
		{
			for (Player player : playersList.keySet())
			{
				if ((player != null) && !participants.containsKey(player))
				{
					participants.put(player, getPoints(player));
				}
			}
			
			if (participants.isEmpty())
			{
				return null;
			}
		}
		
		final Map<Player, Integer> sortedMap = participants.entrySet().stream().sorted(Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toMap(Entry::getKey, Entry::getValue, (e1, _) -> e1, LinkedHashMap::new));
		return sortedMap;
	}
	
	/**
	 * Moves all active players to a random battle location.<br>
	 * This method handles teleportation, effect application, and packet updates for participants.<br>
	 * It ensures that the {@code Player} objects are moved correctly during the match start.
	 */
	private void teleportAllPlayers()
	{
		final Map<Player, KrateiCubePlayer> players = getPlayers();
		if (!players.isEmpty())
		{
			final Map<Player, Integer> participants = getParticipants();
			if (participants == null)
			{
				return;
			}
			
			final ExPVPMatchCCRecord p = new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, participants);
			for (Player player : players.keySet())
			{
				if (player != null)
				{
					player.teleToLocation(getBattleLoc().get(Rnd.get(getBattleLoc().size())), true);
					addEffects(player);
					player.sendPacket(new ExPVPMatchCCMyRecord(getPoints(player)));
					player.sendPacket(p);
					player.setCanRevive(false);
				}
			}
		}
	}
	
	/**
	 * Moves a {@code Player} to a random battle location.<br>
	 * This method checks if the player is registered in the current match.<br>
	 * If the player is not provided, it moves all active players to the waiting room.
	 * @param player The {@code Player} to teleport.
	 */
	public void teleportToBattle(Player player)
	{
		if (player != null)
		{
			if (getPlayers().containsKey(player))
			{
				player.teleToLocation(getBattleLoc().get(Rnd.get(getBattleLoc().size())), true);
			}
		}
		else
		{
			final Map<Player, KrateiCubePlayer> players = getPlayers();
			if (!players.isEmpty())
			{
				for (Player pl : players.keySet())
				{
					if (pl != null)
					{
						teleportToWaitingRoom(pl, false);
					}
				}
			}
		}
	}
	
	/**
	 * Moves a {@code Player} to a random waiting room location.<br>
	 * This method checks if the player is currently in the match.<br>
	 * It optionally restores status effects based on the provided boolean.
	 * @param player The {@code Player} instance to teleport.
	 * @param restoreEffects Set to {@code true} to respawn effects, or {@code false} to keep current ones.
	 */
	public void teleportToWaitingRoom(Player player, boolean restoreEffects)
	{
		if (getPlayers().containsKey(player))
		{
			if (restoreEffects)
			{
				respawnEffects(player);
			}
			
			player.teleToLocation(getWaitLoc().get(Rnd.get(getWaitLoc().size())), true);
		}
	}
	
	/**
	 * Updates the status of the doors based on the provided state.<br>
	 * This method toggles rotation logic and schedules a task to update door positions.<br>
	 * It ensures that only one set of doors is active at a time if {@code active} is {@code true}.
	 * @param active The desired status for the primary door set.
	 */
	private void changeDoorStatus(boolean active)
	{
		if (_doorTask != null)
		{
			_doorTask.cancel(false);
		}
		
		if (_doorsRotation)
		{
			_doorsRotation = false;
		}
		else
		{
			_doorsRotation = true;
		}
		
		final List<Door> listA = getDoorListA();
		final List<Door> listB = getDoorListB();
		if (!listA.isEmpty() && !listB.isEmpty())
		{
			final List<Door> doorsA = (_doorsRotation) ? listA : listB;
			final List<Door> doorsB = (_doorsRotation) ? listB : listA;
			if (active)
			{
				changeDoorsStatus(doorsA, true);
				changeDoorsStatus(doorsB, false);
			}
			else
			{
				changeDoorsStatus(doorsA, false);
				changeDoorsStatus(doorsB, false);
			}
		}
		
		_doorTask = ThreadPool.schedule(() -> changeDoorStatus(active), (getParams().getInt("doorRotationTime") * 1000));
	}
	
	/**
	 * Updates the status of multiple doors.<br>
	 * This method iterates through a list and calls either {@code openMe()} or {@code closeMe()} based on the provided boolean.<br>
	 * It checks if each door is not {@code null} before attempting to change its state.
	 * @param doors The list of {@code Door} objects to update.
	 * @param isOpen The target state for the doors; {@code true} to open and {@code false} to close.
	 */
	private void changeDoorsStatus(List<Door> doors, boolean isOpen)
	{
		for (Door door : doors)
		{
			if (door != null)
			{
				if (isOpen)
				{
					door.openMe();
				}
				else
				{
					door.closeMe();
				}
			}
		}
	}
	
	/**
	 * Resets the health and mana of the {@code Player}.<br>
	 * Restores all resources for the player and their summoned NPCs.<br>
	 * Applies all active skills from the buff list to both the player and summons.<br>
	 * Updates the information broadcast for the player and their summons.
	 * @param player The {@code Player} who will receive the effects.
	 */
	public void addEffects(Player player)
	{
		player.setCurrentHp(player.getMaxHp());
		player.setCurrentCp(player.getMaxCp());
		player.setCurrentMp(player.getMaxMp());
		
		player.getSummonedNpcs().forEach(summon ->
		{
			summon.setCurrentHp(summon.getMaxHp());
			summon.setCurrentCp(summon.getMaxCp());
			summon.setCurrentMp(summon.getMaxMp());
		});
		
		if (!getBuffList().isEmpty())
		{
			for (Skill skill : getBuffList())
			{
				if (skill != null)
				{
					skill.applyEffects(player, player);
					player.getSummonedNpcs().forEach(summon -> skill.applyEffects(summon, summon));
				}
			}
		}
		
		player.broadcastUserInfo();
		
		player.getSummonedNpcs().forEach(summon -> summon.broadcastInfo());
	}
	
	/**
	 * Removes all active effects from the specified {@code Player}.<br>
	 * This method also clears effects for all summoned NPCs.<br>
	 * It updates and broadcasts the information for both the player and their summons.
	 * @param player The {@code Player} whose effects will be removed.
	 */
	private void removeAllEffects(Player player)
	{
		player.getEffectList().stopAllEffects(true);
		player.stopAllEffects();
		
		final Collection<Npc> summonedNpcs = player.getSummonedNpcs();
		summonedNpcs.forEach(summon -> summon.stopAllEffects());
		
		player.broadcastUserInfo();
		
		summonedNpcs.forEach(summon -> summon.broadcastInfo());
	}
	
	/**
	 * Removes all current effects from the player.<br>
	 * Revives the player if they are currently dead.<br>
	 * Updates and broadcasts the player's status to others.
	 * @param player The {@code Player} instance to update.
	 */
	private void respawnEffects(Player player)
	{
		removeAllEffects(player);
		
		if (player.isDead())
		{
			player.doRevive(100.);
		}
		
		player.broadcastStatusUpdate();
		player.broadcastUserInfo();
	}
	
	/**
	 * Calculates and distributes rewards to players based on their performance.<br>
	 * It sorts the participants by points and applies modifiers for experience, SP, and items.<br>
	 * The method also handles special rewards like PC Cafe Points, Clan Reputation, and Fame.
	 * @param players A map containing the {@code Player} objects and their corresponding {@code KrateiCubePlayer} data.
	 */
	private void checkRewardPlayers(Map<Player, KrateiCubePlayer> players)
	{
		final List<KrateiCubePlayer> playerList = new ArrayList<>();
		for (KrateiCubePlayer info : players.values())
		{
			if (info != null)
			{
				playerList.add(info);
			}
		}
		
		final Comparator<KrateiCubePlayer> statsComparator = new SortPointInfo();
		playerList.sort(statsComparator);
		
		ExPVPMatchCCRecord p = null;
		final Map<Player, Integer> participants = getParticipants();
		if (participants != null)
		{
			p = new ExPVPMatchCCRecord(ExPVPMatchCCRecord.INITIALIZE, participants);
		}
		
		double dif = 0.05;
		int pos = 0;
		for (KrateiCubePlayer pl : playerList)
		{
			if ((pl != null) && (pl.getPlayer() != null))
			{
				pos++;
				if (p != null)
				{
					pl.getPlayer().sendPacket(p);
				}
				
				final int points = pl.getPoints();
				final int minPoints = getParams().getInt("minPoints");
				if (points >= minPoints)
				{
					final int count = (int) (points * dif * (1.0 + ((players.size() / pos) * 0.04)));
					dif -= 0.0016;
					if (count > 0)
					{
						final int expModifier = getParams().getInt("expModifier");
						final int spModifier = getParams().getInt("spModifier");
						final int exp = count * expModifier;
						final int sp = count * spModifier;
						pl.getPlayer().addExpAndSp(exp, sp);
						if (!getRewards().isEmpty())
						{
							for (KrateisReward reward : getRewards())
							{
								if (reward != null)
								{
									long amount = reward.isAllowMidifier() ? count : reward.getAmount();
									final int rewardId = reward.getId();
									if (rewardId == -100)
									{
										final int pcCafePoints = pl.getPlayer().getPcCafePoints();
										if ((pcCafePoints + amount) > PremiumSystemConfig.PC_CAFE_MAX_POINTS)
										{
											amount = PremiumSystemConfig.PC_CAFE_MAX_POINTS - pcCafePoints;
										}
										
										final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_EARNED_PA_POINTS_X_S1);
										sm.addInt((int) amount);
										pl.getPlayer().sendPacket(sm);
										pl.getPlayer().setPcCafePoints((int) (pcCafePoints + amount));
										pl.getPlayer().sendPacket(new ExPCCafePointInfo(pl.getPlayer().getPcCafePoints(), (int) amount, 1));
									}
									else if (rewardId == -200)
									{
										if (pl.getPlayer().getClan() != null)
										{
											pl.getPlayer().getClan().addReputationScore((int) amount);
										}
									}
									else if (rewardId == -300)
									{
										pl.getPlayer().setFame((int) (pl.getPlayer().getFame() + amount));
										final SystemMessage sm = new SystemMessage(SystemMessageId.PERSONAL_REPUTATION_S1);
										sm.addInt((int) amount);
										pl.getPlayer().sendPacket(sm);
										pl.getPlayer().updateUserInfo();
									}
									else if (rewardId > 0)
									{
										pl.getPlayer().addItem(ItemProcessType.REWARD, rewardId, amount, pl.getPlayer(), true);
									}
								}
							}
						}
					}
				}
			}
		}
		
		playerList.clear();
	}
	
	private class SortPointInfo implements Comparator<KrateiCubePlayer>
	{
		@Override
		public int compare(KrateiCubePlayer o1, KrateiCubePlayer o2)
		{
			return Integer.compare(o2.getPoints(), o1.getPoints());
		}
	}
	
	/**
	 * Adds points to a {@code Player} based on the match parameters.<br>
	 * The amount depends on whether the source is a player or a mob.<br>
	 * It also sends an update packet to all participants.
	 * @param player The {@code Player} receiving the points.
	 * @param isPlayer Set to {@code true} if points are awarded for a player, and {@code false} for a mob.
	 */
	public void addPoints(Player player, boolean isPlayer)
	{
		final KrateiCubePlayer krateiPlayer = getPlayers().get(player);
		if (krateiPlayer != null)
		{
			final int krateiPoints = krateiPlayer.getPoints();
			krateiPlayer.addPoints(krateiPoints + (isPlayer ? getParams().getInt("pointPerPlayer") : getParams().getInt("pointPerMob")));
			krateiPlayer.getPlayer().sendPacket(new ExPVPMatchCCMyRecord(krateiPoints));
			
			final Map<Player, Integer> participants = getParticipants();
			if (participants != null)
			{
				final ExPVPMatchCCRecord p = new ExPVPMatchCCRecord(ExPVPMatchCCRecord.UPDATE, participants);
				if (!getPlayers().isEmpty())
				{
					for (Player pl : getPlayers().keySet())
					{
						if (pl != null)
						{
							pl.sendPacket(p);
						}
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves the total number of points for a specific player.<br>
	 * This method checks if the {@code Player} exists in the current match.<br>
	 * If the player is found, it returns their point count.<br>
	 * Otherwise, it returns 0.
	 * @param player The {@code Player} whose points need to be retrieved.
	 * @return The total number of points earned by the player.
	 */
	public int getPoints(Player player)
	{
		int kills = 0;
		final KrateiCubePlayer pl = getPlayers().get(player);
		if (pl != null)
		{
			kills = pl.getPoints();
		}
		
		return kills;
	}
	
	/**
	 * Cancels the existing time task if it is currently running.<br>
	 * Starts a new {@code MessageTask} to update players on the remaining time.<br>
	 * This method ensures that the countdown message is sent every 10 seconds.
	 */
	private void timeLeftInfo()
	{
		if (_timeTask != null)
		{
			_timeTask.cancel(true);
			_timeTask = null;
		}
		
		final Map<Player, KrateiCubePlayer> players = getPlayers();
		if (!players.isEmpty())
		{
			ThreadPool.schedule(new MessageTask(players, 10, 1), 0);
		}
	}
	
	/**
	 * Sends a message to all players currently in the arena.<br>
	 * This method uses {@code ThreadPool} to schedule a {@code MessageTask}.<br>
	 * It only executes if there is at least one player present.
	 */
	public void waitTimeInfo()
	{
		final Map<Player, KrateiCubePlayer> players = getPlayers();
		if (!players.isEmpty())
		{
			ThreadPool.schedule(new MessageTask(players, 10, 0), 0);
		}
	}
	
	public class MessageTask implements Runnable
	{
		private final Map<Player, KrateiCubePlayer> _players;
		private int _time;
		private final int _type;
		
		public MessageTask(Map<Player, KrateiCubePlayer> players, int time, int type)
		{
			_players = players;
			_time = time;
			_type = type;
		}
		
		@Override
		public void run()
		{
			if (((_players != null) && !_players.isEmpty()) && (_time > 0))
			{
				SystemMessage msg = null;
				switch (_type)
				{
					case 0:
					{
						msg = new SystemMessage(SystemMessageId.THE_MATCH_BEGINS_IN_S1_SEC).addInt(_time);
						break;
					}
					case 1:
					{
						msg = new SystemMessage(SystemMessageId.THE_GAME_ENDS_IN_S1_SEC_2).addInt(_time);
						break;
					}
				}
				
				for (Player player : _players.keySet())
				{
					if ((player != null) && (msg != null))
					{
						player.sendPacket(msg);
					}
				}
				
				_time--;
				if (_time > 0)
				{
					ThreadPool.schedule(new MessageTask(_players, _time, _type), 1000);
				}
			}
		}
	}
	
	/**
	 * Schedules a task to handle the player's respawn.<br>
	 * This method uses the respawn time defined in the current parameters.<br>
	 * The task is executed immediately using the {@code ThreadPool}.
	 * @param player The {@code Player} for whom the respawn task will be scheduled.
	 */
	public void setRespawnTask(Player player)
	{
		if (player != null)
		{
			ThreadPool.schedule(new RespawnTask(player, getParams().getInt("respawnTime")), 0);
		}
	}
	
	private class RespawnTask implements Runnable
	{
		private final Player _player;
		private int _time;
		
		public RespawnTask(Player player, int time)
		{
			_player = player;
			_time = time;
		}
		
		@Override
		public void run()
		{
			if ((_player != null) && (_time > 0))
			{
				_player.sendPacket(new SystemMessage(SystemMessageId.YOU_LL_BE_RESURRECTED_IN_THE_WAITING_ROOM_IN_S1_SEC).addInt(_time));
				_time--;
				if (_time > 0)
				{
					ThreadPool.schedule(new RespawnTask(_player, _time), 1000);
				}
				else
				{
					respawnEffects(_player);
					_player.teleToLocation(getWaitLoc().get(Rnd.get(getWaitLoc().size())), true);
				}
			}
		}
	}
}
