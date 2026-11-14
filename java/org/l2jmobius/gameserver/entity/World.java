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
package org.l2jmobius.gameserver.entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.l2jmobius.commons.config.InterfaceConfig;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.CreatureAI;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.instance.Pet;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * Represents the global game world environment.<br>
 * It manages the lifecycle and interactions of {@code Creature}, {@code Player}, and other actors.<br>
 * This class serves as the primary container for spatial data and world-wide state.
 */
public class World
{
	private static final Logger LOGGER = Logger.getLogger(World.class.getName());
	
	public static volatile int MAX_CONNECTED_COUNT = 0;
	public static volatile int OFFLINE_TRADE_COUNT = 0;
	
	/** Gracia border Flying objects not allowed to the east of it. */
	public static final int GRACIA_MAX_X = -166168;
	public static final int GRACIA_MAX_Z = 6105;
	public static final int GRACIA_MIN_Z = -895;
	
	/** Bit shift, defines number of regions note, shifting by 15 will result in regions corresponding to map tiles shifting by 11 divides one tile to 16x16 regions. */
	public static final int SHIFT_BY = 11;
	
	public static final int TILE_SIZE = 32768;
	
	/** Map dimensions. */
	public static final int TILE_X_MIN = 11;
	public static final int TILE_Y_MIN = 10;
	public static final int TILE_X_MAX = 28;
	public static final int TILE_Y_MAX = 26;
	public static final int TILE_ZERO_COORD_X = 20;
	public static final int TILE_ZERO_COORD_Y = 18;
	public static final int WORLD_X_MIN = (TILE_X_MIN - TILE_ZERO_COORD_X) * TILE_SIZE;
	public static final int WORLD_Y_MIN = (TILE_Y_MIN - TILE_ZERO_COORD_Y) * TILE_SIZE;
	
	public static final int WORLD_X_MAX = ((TILE_X_MAX - TILE_ZERO_COORD_X) + 1) * TILE_SIZE;
	public static final int WORLD_Y_MAX = ((TILE_Y_MAX - TILE_ZERO_COORD_Y) + 1) * TILE_SIZE;
	
	/** Z coordinate limits */
	public static final int WORLD_Z_MIN = -16000;
	public static final int WORLD_Z_MAX = 16000;
	public static final int Z_REGION_SIZE = 2000;
	
	/** Calculated offset used so top left region is 0,0 */
	public static final int OFFSET_X = Math.abs(WORLD_X_MIN >> SHIFT_BY);
	public static final int OFFSET_Y = Math.abs(WORLD_Y_MIN >> SHIFT_BY);
	
	/** Number of regions. */
	public static final int REGIONS_X = (WORLD_X_MAX >> SHIFT_BY) + OFFSET_X;
	public static final int REGIONS_Y = (WORLD_Y_MAX >> SHIFT_BY) + OFFSET_Y;
	public static final int REGIONS_Z = (Math.abs(WORLD_Z_MIN) + Math.abs(WORLD_Z_MAX)) / Z_REGION_SIZE;
	
	/** Map containing all the players in game. */
	private static final Map<Integer, Player> _allPlayers = new ConcurrentHashMap<>();
	/** Map containing all the Good players in game. */
	private static final Map<Integer, Player> _allGoodPlayers = new ConcurrentHashMap<>();
	/** Map containing all the Evil players in game. */
	private static final Map<Integer, Player> _allEvilPlayers = new ConcurrentHashMap<>();
	/** Map containing all the players in Store Buy or Sell mode. */
	private static final Map<Integer, Player> _allStoreModeBuySellPlayers = new ConcurrentHashMap<>();
	/** Map containing all visible objects. */
	private static final Map<Integer, WorldObject> _allObjects = new ConcurrentHashMap<>();
	/** Map with the pets instances and their owner ID. */
	private static final Map<Integer, Pet> _petsInstance = new ConcurrentHashMap<>();
	
	private static final AtomicInteger _partyNumber = new AtomicInteger();
	private static final AtomicInteger _memberInPartyNumber = new AtomicInteger();
	
	private static final Set<Player> _pkPlayers = ConcurrentHashMap.newKeySet(30);
	private static final AtomicInteger _lastPkTime = new AtomicInteger((int) System.currentTimeMillis() / 1000);
	
	/** Lazily populated region grid, flattened as (((x * (REGIONS_Y + 1)) + y) * REGIONS_Z) + z. */
	private static final AtomicReferenceArray<WorldRegion> _worldRegions = new AtomicReferenceArray<>((REGIONS_X + 1) * (REGIONS_Y + 1) * REGIONS_Z);
	
	private static long _nextPrivateStoreUpdate = 0;
	
	/**
	 * Private constructor for the {@code World} class.<br>
	 * This prevents direct instantiation of the world instance.
	 */
	private World()
	{
	}
	
	/**
	 * Initializes the world region grid.<br>
	 * {@code WorldRegion} instances are not created here.<br>
	 * They are created on demand, the first time a coordinate resolves to them.
	 */
	public static void init()
	{
		// When GUI is enabled World is called early. So we need to skip this log.
		if (!InterfaceConfig.ENABLE_GUI)
		{
			LOGGER.info(World.class.getSimpleName() + ": (" + REGIONS_X + " by " + REGIONS_Y + ") World Region Grid set up.");
		}
	}
	
	/**
	 * Adds a {@code WorldObject} to the world.<br>
	 * This method registers the object in the global collection.<br>
	 * If the object is a {@code Player}, it handles faction registration and checks for duplicates.
	 * @param object The {@code WorldObject} to add to the world.
	 */
	public static void addObject(WorldObject object)
	{
		final Integer objectId = object.getObjectId();
		_allObjects.putIfAbsent(objectId, object);
		
		// if (_allObjects.putIfAbsent(object.getObjectId(), object) != null)
		// {
		// LOGGER.warning(World.class.getSimpleName() + ": Object " + object + " already exists in the world. Stack Trace: " + CommonUtil.getTraceString(Thread.currentThread().getStackTrace()));
		// }
		
		if (object.isPlayer())
		{
			final Player newPlayer = object.asPlayer();
			if (newPlayer.isTeleporting()) // TODO: Drop when we stop removing player from the world while teleporting.
			{
				return;
			}
			
			final Player existingPlayer = _allPlayers.putIfAbsent(objectId, newPlayer);
			if (existingPlayer != null)
			{
				Disconnection.of(existingPlayer).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
				Disconnection.of(newPlayer).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
				LOGGER.warning(World.class.getSimpleName() + ": Duplicate character!? Disconnected both characters (" + newPlayer.getName() + ")");
			}
			else if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
			{
				addFactionPlayerToWorld(newPlayer);
			}
		}
	}
	
	/**
	 * Removes a specific object from the world.<br>
	 * This method updates internal collections for objects, players, and factions.<br>
	 * It skips removal if the {@code Player} is currently teleporting.
	 * @param object The {@code WorldObject} to be removed.
	 */
	public static void removeObject(WorldObject object)
	{
		final Integer objectId = object.getObjectId();
		_allObjects.remove(objectId);
		if (object.isPlayer())
		{
			final Player player = object.asPlayer();
			if (player.isTeleporting()) // TODO: Drop when we stop removing player from the world while teleporting.
			{
				return;
			}
			
			_allPlayers.remove(objectId);
			
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
			{
				if (player.isGood())
				{
					_allGoodPlayers.remove(objectId);
				}
				else if (player.isEvil())
				{
					_allEvilPlayers.remove(objectId);
				}
			}
		}
	}
	
	/**
	 * Retrieves a {@code WorldObject} from the world using its unique ID.<br>
	 * This method searches the internal object map for the specified identifier.
	 * @param objectId The unique identification number of the object to find.
	 * @return The {@code WorldObject} associated with the ID, or {@code null} if not found.
	 */
	public static WorldObject findObject(int objectId)
	{
		return _allObjects.get(objectId);
	}
	
	/**
	 * Retrieves all objects currently visible in the world.<br>
	 * This method returns a collection of {@code WorldObject} instances.
	 * @return A {@code Collection} containing all visible {@code WorldObject}s.
	 */
	public static Collection<WorldObject> getVisibleObjects()
	{
		return _allObjects.values();
	}
	
	/**
	 * Returns the total number of visible objects.<br>
	 * This count is based on the current size of the internal object collection.
	 * @return The total count of visible {@code WorldObject} instances.
	 */
	public static int getVisibleObjectsCount()
	{
		return _allObjects.size();
	}
	
	/**
	 * Retrieves all players currently in the world.
	 * @return a {@code Collection} of {@code Player} objects.
	 */
	public static Collection<Player> getPlayers()
	{
		return _allPlayers.values();
	}
	
	/**
	 * Retrieves all players who are classified as good.<br>
	 * This method returns a collection of {@code Player} objects.
	 * @return A {@code Collection} containing all good {@code Player} instances.
	 */
	public static Collection<Player> getAllGoodPlayers()
	{
		return _allGoodPlayers.values();
	}
	
	/**
	 * Retrieves all players who are currently classified as evil.<br>
	 * This method returns a collection of {@code Player} objects.
	 * @return A {@code Collection} containing all evil {@code Player} instances.
	 */
	public static Collection<Player> getAllEvilPlayers()
	{
		return _allEvilPlayers.values();
	}
	
	/**
	 * Retrieves a {@code Player} object based on their name.<br>
	 * This method looks up the unique ID associated with the provided {@code name}.<br>
	 * It then returns the corresponding {@code Player} instance.
	 * @param name The name of the player to find.
	 * @return The {@code Player} matching the given name, or {@code null} if not found.
	 */
	public static Player getPlayer(String name)
	{
		return getPlayer(PlayerInfoTable.getInstance().getIdByName(name));
	}
	
	/**
	 * Retrieves a {@code Player} based on their unique object ID.<br>
	 * This method looks up the player in the internal collection.
	 * @param objectId The unique identifier of the player to find.
	 * @return The {@code Player} associated with the given {@code objectId}, or {@code null} if not found.
	 */
	public static Player getPlayer(int objectId)
	{
		return _allPlayers.get(objectId);
	}
	
	/**
	 * Retrieves the {@code Pet} associated with a specific owner.<br>
	 * This method looks up the pet using the provided unique identifier.
	 * @param ownerId The unique ID of the player who owns the pet.
	 * @return The {@code Pet} object belonging to the owner, or {@code null} if none exists.
	 */
	public static Pet getPet(int ownerId)
	{
		return _petsInstance.get(ownerId);
	}
	
	/**
	 * Retrieves a list of players currently in store sell or buy mode.<br>
	 * This method updates the internal cache based on {@code GeneralConfig.STORE_REVIEW_CACHE_TIME}.<br>
	 * It filters all active players to find those who have enabled trading modes.
	 * @return A {@code Collection} of {@code Player} objects currently in store mode.
	 */
	public static synchronized Collection<Player> getSellingOrBuyingPlayers()
	{
		final long currentTime = System.currentTimeMillis();
		if (currentTime > _nextPrivateStoreUpdate)
		{
			_nextPrivateStoreUpdate = currentTime + GeneralConfig.STORE_REVIEW_CACHE_TIME;
			_allStoreModeBuySellPlayers.clear();
			for (Player player : _allPlayers.values())
			{
				if (player.isInStoreSellOrBuyMode())
				{
					_allStoreModeBuySellPlayers.put(player.getObjectId(), player);
				}
			}
		}
		
		return _allStoreModeBuySellPlayers.values();
	}
	
	/**
	 * Registers a {@code Pet} to a specific owner.<br>
	 * This method stores the pet in the internal registry using the provided {@code ownerId}.
	 * @param ownerId The unique identifier of the player who owns the pet.
	 * @param pet The {@code Pet} object to be added.
	 * @return The previous {@code Pet} associated with this {@code ownerId}, or {@code null} if there was no mapping.
	 */
	public static Pet addPet(int ownerId, Pet pet)
	{
		return _petsInstance.put(ownerId, pet);
	}
	
	/**
	 * Removes the pet associated with a specific owner.<br>
	 * This method updates the internal storage by removing the entry for the given ID.
	 * @param ownerId The unique identifier of the player who owns the pet.
	 */
	public static void removePet(int ownerId)
	{
		_petsInstance.remove(ownerId);
	}
	
	/**
	 * Retrieves an {@code Npc} object based on its unique identifier.<br>
	 * This method searches through all visible objects in the world.<br>
	 * It returns the matching {@code Npc} if found.
	 * @param npcId The unique ID of the NPC to find.
	 * @return The corresponding {@code Npc} object, or {@code null} if no match is found.
	 */
	public static Npc getNpc(int npcId)
	{
		for (WorldObject wo : getVisibleObjects())
		{
			if (wo.isNpc() && (wo.getId() == npcId))
			{
				return wo.asNpc();
			}
		}
		
		return null;
	}
	
	/**
	 * Adds a {@code WorldObject} to the visible objects of a new region.<br>
	 * This method updates visibility for players and creatures in the area.<br>
	 * It ensures that relevant information is sent to nearby entities.
	 * @param object The {@code WorldObject} to be added.
	 * @param newRegion The {@code WorldRegion} where the object is being placed.
	 */
	public static void addVisibleObject(WorldObject object, WorldRegion newRegion)
	{
		if (!newRegion.isActive())
		{
			return;
		}
		
		forEachVisibleObject(object, WorldObject.class, wo ->
		{
			if (object.isPlayer() && wo.isVisibleFor(object.asPlayer()))
			{
				wo.sendInfo(object.asPlayer());
				if (wo.isCreature())
				{
					final CreatureAI ai = wo.asCreature().getAI();
					if (ai != null)
					{
						ai.describeStateToPlayer(object.asPlayer());
						if (wo.isMonster() && (ai.getIntention() == Intention.IDLE))
						{
							ai.setIntentionActive();
						}
					}
				}
			}
			
			if (wo.isPlayer() && object.isVisibleFor(wo.asPlayer()))
			{
				object.sendInfo(wo.asPlayer());
				if (object.isCreature())
				{
					final CreatureAI ai = object.asCreature().getAI();
					if (ai != null)
					{
						ai.describeStateToPlayer(wo.asPlayer());
						if (object.isMonster() && (ai.getIntention() == Intention.IDLE))
						{
							ai.setIntentionActive();
						}
					}
				}
			}
		});
	}
	
	/**
	 * Registers a {@code Player} into the world's faction tracking system.<br>
	 * This method checks if the {@code Player} is good or evil.<br>
	 * It then adds them to the appropriate internal collection based on their status.
	 * @param player The {@code Player} object to be added to the world factions.
	 */
	public static void addFactionPlayerToWorld(Player player)
	{
		if (player.isGood())
		{
			_allGoodPlayers.put(player.getObjectId(), player);
		}
		else if (player.isEvil())
		{
			_allEvilPlayers.put(player.getObjectId(), player);
		}
	}
	
	/**
	 * Removes an object from the visible list of a specific region.<br>
	 * This method also updates surrounding regions to clear visibility and targets.<br>
	 * It ensures that players receive packets to remove the hidden objects from their view.
	 * @param object The {@code WorldObject} to be removed from visibility.
	 * @param oldRegion The {@code WorldRegion} where the object is currently located.
	 */
	public static void removeVisibleObject(WorldObject object, WorldRegion oldRegion)
	{
		if ((object == null) || (oldRegion == null))
		{
			return;
		}
		
		oldRegion.removeVisibleObject(object);
		
		// Go through all surrounding WorldRegion Creatures.
		final WorldRegion[] surroundingRegions = oldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if (wo == object)
				{
					continue;
				}
				
				if (object.isCreature())
				{
					final Creature objectCreature = object.asCreature();
					final CreatureAI ai = objectCreature.getAI();
					if (ai != null)
					{
						ai.notifyActionForgetObject(wo);
					}
					
					if (objectCreature.getTarget() == wo)
					{
						objectCreature.setTarget(null);
					}
					
					if (object.isPlayer())
					{
						object.sendPacket(new DeleteObject(wo));
					}
				}
				
				if (wo.isCreature())
				{
					final Creature woCreature = wo.asCreature();
					final CreatureAI ai = woCreature.getAI();
					if (ai != null)
					{
						ai.notifyActionForgetObject(object);
					}
					
					if (woCreature.getTarget() == object)
					{
						woCreature.setTarget(null);
					}
					
					if (wo.isPlayer())
					{
						wo.sendPacket(new DeleteObject(object));
					}
				}
			}
		}
	}
	
	/**
	 * Moves an object to a new region.<br>
	 * This method updates the visibility of objects between regions.<br>
	 * It handles AI notifications and target clearing for creatures.<br>
	 * It also manages network packets for players to hide or show objects.
	 * @param object The {@code WorldObject} to move.
	 * @param newRegion The {@code WorldRegion} to move the object into.
	 */
	public static void switchRegion(WorldObject object, WorldRegion newRegion)
	{
		final WorldRegion oldRegion = object.getWorldRegion();
		if ((oldRegion == null) || (oldRegion == newRegion))
		{
			return;
		}
		
		final WorldRegion[] oldSurroundingRegions = oldRegion.getSurroundingRegions();
		for (int i = 0; i < oldSurroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = oldSurroundingRegions[i];
			if (newRegion.isSurroundingRegion(worldRegion))
			{
				continue;
			}
			
			final Collection<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if (wo == object)
				{
					continue;
				}
				
				if (object.isCreature())
				{
					final Creature objectCreature = object.asCreature();
					final CreatureAI ai = objectCreature.getAI();
					if (ai != null)
					{
						ai.notifyActionForgetObject(wo);
					}
					
					if (objectCreature.getTarget() == wo)
					{
						objectCreature.setTarget(null);
					}
					
					if (object.isPlayer())
					{
						object.sendPacket(new DeleteObject(wo));
					}
				}
				
				if (wo.isCreature())
				{
					final Creature woCreature = wo.asCreature();
					final CreatureAI ai = woCreature.getAI();
					if (ai != null)
					{
						ai.notifyActionForgetObject(object);
					}
					
					if (woCreature.getTarget() == object)
					{
						woCreature.setTarget(null);
					}
					
					if (wo.isPlayer())
					{
						wo.sendPacket(new DeleteObject(object));
					}
				}
			}
		}
		
		final WorldRegion[] newSurroundingRegions = newRegion.getSurroundingRegions();
		for (int i = 0; i < newSurroundingRegions.length; i++)
		{
			final WorldRegion worldRegion = newSurroundingRegions[i];
			if (oldRegion.isSurroundingRegion(worldRegion))
			{
				continue;
			}
			
			final Collection<WorldObject> visibleObjects = worldRegion.getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				if (object.isPlayer() && wo.isVisibleFor(object.asPlayer()))
				{
					wo.sendInfo(object.asPlayer());
					if (wo.isCreature())
					{
						final CreatureAI ai = wo.asCreature().getAI();
						if (ai != null)
						{
							ai.describeStateToPlayer(object.asPlayer());
							if (wo.isMonster() && (ai.getIntention() == Intention.IDLE))
							{
								ai.setIntentionActive();
							}
						}
					}
				}
				
				if (wo.isPlayer() && object.isVisibleFor(wo.asPlayer()))
				{
					object.sendInfo(wo.asPlayer());
					if (object.isCreature())
					{
						final CreatureAI ai = object.asCreature().getAI();
						if (ai != null)
						{
							ai.describeStateToPlayer(wo.asPlayer());
							if (object.isMonster() && (ai.getIntention() == Intention.IDLE))
							{
								ai.setIntentionActive();
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Collects all visible objects of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @return A {@code List} of matching objects; empty if none are found.
	 */
	public static <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz)
	{
		final List<T> result = new ArrayList<>();
		if (object == null)
		{
			return result;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return result;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				result.add(clazz.cast(wo));
			}
		}
		
		return result;
	}
	
	/**
	 * Collects all visible objects of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @return A {@code List} of matching objects; empty if none are found.
	 */
	public static <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range)
	{
		final List<T> result = new ArrayList<>();
		if (object == null)
		{
			return result;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return result;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				result.add(clazz.cast(wo));
			}
		}
		
		return result;
	}
	
	/**
	 * Collects all visible objects of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param predicate The condition each candidate must satisfy.
	 * @return A {@code List} of matching objects; empty if none are found.
	 */
	public static <T extends WorldObject> List<T> getVisibleObjects(WorldObject object, Class<T> clazz, Predicate<T> predicate)
	{
		final List<T> result = new ArrayList<>();
		if (object == null)
		{
			return result;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return result;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					result.add(cast);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Collects all visible objects of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param predicate The condition each candidate must satisfy.
	 * @return A {@code List} of matching objects; empty if none are found.
	 */
	public static <T extends WorldObject> List<T> getVisibleObjectsInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate)
	{
		final List<T> result = new ArrayList<>();
		if (object == null)
		{
			return result;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return result;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					result.add(cast);
				}
			}
		}
		
		return result;
	}
	
	/**
	 * Performs an action on every visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param c The operation to perform on each matching object.
	 */
	public static <T extends WorldObject> void forEachVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> c)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				c.accept(clazz.cast(wo));
			}
		}
	}
	
	/**
	 * Performs an action on every visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param c The operation to perform on each matching object.
	 */
	public static <T extends WorldObject> void forEachVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> c)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				c.accept(clazz.cast(wo));
			}
		}
	}
	
	/**
	 * Finds the first visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @return The first matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getFirstVisibleObject(WorldObject object, Class<T> clazz)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				return clazz.cast(wo);
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the first visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @return The first matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getFirstVisibleObjectInRange(WorldObject object, Class<T> clazz, int range)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				return clazz.cast(wo);
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the first visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param predicate The condition each candidate must satisfy.
	 * @return The first matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getFirstVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					return cast;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the first visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param predicate The condition each candidate must satisfy.
	 * @return The first matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getFirstVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					return cast;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Performs an action on the first visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forFirstVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				action.accept(clazz.cast(wo));
				return;
			}
		}
	}
	
	/**
	 * Performs an action on the first visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forFirstVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				action.accept(clazz.cast(wo));
				return;
			}
		}
	}
	
	/**
	 * Performs an action on the first visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param predicate The condition each candidate must satisfy.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forFirstVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					action.accept(cast);
					return;
				}
			}
		}
	}
	
	/**
	 * Performs an action on the first visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param predicate The condition each candidate must satisfy.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forFirstVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					action.accept(cast);
					return;
				}
			}
		}
	}
	
	/**
	 * Finds the closest visible object of the given type to {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @return The nearest matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getNearestVisibleObject(WorldObject object, Class<T> clazz)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final double distance = wo.calculateDistance3D(object);
				if (distance < bestDistance)
				{
					bestDistance = distance;
					nearest = clazz.cast(wo);
				}
			}
		}
		
		return nearest;
	}
	
	/**
	 * Finds the closest visible object of the given type to {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @return The nearest matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getNearestVisibleObjectInRange(WorldObject object, Class<T> clazz, int range)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final double distance = wo.calculateDistance3D(object);
				if ((distance <= range) && (distance < bestDistance))
				{
					bestDistance = distance;
					nearest = clazz.cast(wo);
				}
			}
		}
		
		return nearest;
	}
	
	/**
	 * Finds the closest visible object of the given type to {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param predicate The condition each candidate must satisfy.
	 * @return The nearest matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getNearestVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					final double distance = wo.calculateDistance3D(object);
					if (distance < bestDistance)
					{
						bestDistance = distance;
						nearest = cast;
					}
				}
			}
		}
		
		return nearest;
	}
	
	/**
	 * Finds the closest visible object of the given type to {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param predicate The condition each candidate must satisfy.
	 * @return The nearest matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getNearestVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final double distance = wo.calculateDistance3D(object);
				if (distance <= range)
				{
					final T cast = clazz.cast(wo);
					if (predicate.test(cast) && (distance < bestDistance))
					{
						bestDistance = distance;
						nearest = cast;
					}
				}
			}
		}
		
		return nearest;
	}
	
	/**
	 * Performs an action on the closest visible object of the given type to {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forNearestVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final double distance = wo.calculateDistance3D(object);
				if (distance < bestDistance)
				{
					bestDistance = distance;
					nearest = clazz.cast(wo);
				}
			}
		}
		
		if (nearest != null)
		{
			action.accept(nearest);
		}
	}
	
	/**
	 * Performs an action on the closest visible object of the given type to {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forNearestVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final double distance = wo.calculateDistance3D(object);
				if ((distance <= range) && (distance < bestDistance))
				{
					bestDistance = distance;
					nearest = clazz.cast(wo);
				}
			}
		}
		
		if (nearest != null)
		{
			action.accept(nearest);
		}
	}
	
	/**
	 * Performs an action on the closest visible object of the given type to {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param predicate The condition each candidate must satisfy.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forNearestVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					final double distance = wo.calculateDistance3D(object);
					if (distance < bestDistance)
					{
						bestDistance = distance;
						nearest = cast;
					}
				}
			}
		}
		
		if (nearest != null)
		{
			action.accept(nearest);
		}
	}
	
	/**
	 * Performs an action on the closest visible object of the given type to {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param predicate The condition each candidate must satisfy.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forNearestVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T nearest = null;
		double bestDistance = Double.MAX_VALUE;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final double distance = wo.calculateDistance3D(object);
				if (distance <= range)
				{
					final T cast = clazz.cast(wo);
					if (predicate.test(cast) && (distance < bestDistance))
					{
						bestDistance = distance;
						nearest = cast;
					}
				}
			}
		}
		
		if (nearest != null)
		{
			action.accept(nearest);
		}
	}
	
	/**
	 * Selects a random visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @return A randomly selected matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getRandomVisibleObject(WorldObject object, Class<T> clazz)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T selected = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				count++;
				if (Rnd.get(count) == 0)
				{
					selected = clazz.cast(wo);
				}
			}
		}
		
		return (selected != null) ? selected : getFirstVisibleObject(object, clazz);
	}
	
	/**
	 * Selects a random visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @return A randomly selected matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getRandomVisibleObjectInRange(WorldObject object, Class<T> clazz, int range)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T selected = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				count++;
				if (Rnd.get(count) == 0)
				{
					selected = clazz.cast(wo);
				}
			}
		}
		
		return (selected != null) ? selected : getFirstVisibleObjectInRange(object, clazz, range);
	}
	
	/**
	 * Selects a random visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param predicate The condition each candidate must satisfy.
	 * @return A randomly selected matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getRandomVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T selected = null;
		T firstValid = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					if (firstValid == null)
					{
						firstValid = cast;
					}
					
					count++;
					if (Rnd.get(count) == 0)
					{
						selected = cast;
					}
				}
			}
		}
		
		return (selected != null) ? selected : firstValid;
	}
	
	/**
	 * Selects a random visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param predicate The condition each candidate must satisfy.
	 * @return A randomly selected matching object, or {@code null} if none are found.
	 */
	public static <T extends WorldObject> T getRandomVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate)
	{
		if (object == null)
		{
			return null;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return null;
		}
		
		T selected = null;
		T firstValid = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					if (firstValid == null)
					{
						firstValid = cast;
					}
					
					count++;
					if (Rnd.get(count) == 0)
					{
						selected = cast;
					}
				}
			}
		}
		
		return (selected != null) ? selected : firstValid;
	}
	
	/**
	 * Performs an action on a random visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forRandomVisibleObject(WorldObject object, Class<T> clazz, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T selected = null;
		T firstValid = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (firstValid == null)
				{
					firstValid = cast;
				}
				
				count++;
				if (Rnd.get(count) == 0)
				{
					selected = cast;
				}
			}
		}
		
		final T target = (selected != null) ? selected : firstValid;
		if (target != null)
		{
			action.accept(target);
		}
	}
	
	/**
	 * Performs an action on a random visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forRandomVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T selected = null;
		T firstValid = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (firstValid == null)
				{
					firstValid = cast;
				}
				
				count++;
				if (Rnd.get(count) == 0)
				{
					selected = cast;
				}
			}
		}
		
		final T target = (selected != null) ? selected : firstValid;
		if (target != null)
		{
			action.accept(target);
		}
	}
	
	/**
	 * Performs an action on a random visible object of the given type around {@code object}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param predicate The condition each candidate must satisfy.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forRandomVisibleObject(WorldObject object, Class<T> clazz, Predicate<T> predicate, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T selected = null;
		T firstValid = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					if (firstValid == null)
					{
						firstValid = cast;
					}
					
					count++;
					if (Rnd.get(count) == 0)
					{
						selected = cast;
					}
				}
			}
		}
		
		final T target = (selected != null) ? selected : firstValid;
		if (target != null)
		{
			action.accept(target);
		}
	}
	
	/**
	 * Performs an action on a random visible object of the given type around {@code object} within {@code range}.
	 * @param <T> The type of {@code WorldObject} to search for.
	 * @param object The reference {@code WorldObject} that defines the search origin.
	 * @param clazz The class of {@code WorldObject} to match.
	 * @param range The maximum distance from {@code object}.
	 * @param predicate The condition each candidate must satisfy.
	 * @param action The operation to perform on the matching object.
	 */
	public static <T extends WorldObject> void forRandomVisibleObjectInRange(WorldObject object, Class<T> clazz, int range, Predicate<T> predicate, Consumer<T> action)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		T selected = null;
		T firstValid = null;
		int count = 0;
		final WorldRegion[] surroundingRegions = worldRegion.getSurroundingRegions();
		for (int i = 0; i < surroundingRegions.length; i++)
		{
			final Collection<WorldObject> visibleObjects = surroundingRegions[i].getVisibleObjects();
			if (visibleObjects.isEmpty())
			{
				continue;
			}
			
			for (WorldObject wo : visibleObjects)
			{
				if ((wo == object) || !clazz.isInstance(wo) || (wo.getInstanceWorld() != object.getInstanceWorld()) || (wo.calculateDistance3D(object) > range))
				{
					continue;
				}
				
				final T cast = clazz.cast(wo);
				if (predicate.test(cast))
				{
					if (firstValid == null)
					{
						firstValid = cast;
					}
					
					count++;
					if (Rnd.get(count) == 0)
					{
						selected = cast;
					}
				}
			}
		}
		
		final T target = (selected != null) ? selected : firstValid;
		if (target != null)
		{
			action.accept(target);
		}
	}
	
	/**
	 * Sends a packet to all players who can see {@code object}.
	 * @param object The {@code WorldObject} used to determine the broadcast area.
	 * @param packet The {@code ServerPacket} to be sent to the players.
	 */
	public static void broadcastToVisiblePlayers(WorldObject object, ServerPacket packet)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		packet.sendInBroadcast();
		
		final WorldRegion[] regions = worldRegion.getSurroundingRegions();
		if (regions != null)
		{
			for (int i = 0; i < regions.length; i++)
			{
				final Collection<WorldObject> objects = regions[i].getVisibleObjects();
				if (objects.isEmpty())
				{
					continue;
				}
				
				for (WorldObject nearby : objects)
				{
					if ((nearby == object) || !nearby.isPlayer() || (nearby.getInstanceWorld() != object.getInstanceWorld()))
					{
						continue;
					}
					
					final Player nearbyPlayer = nearby.asPlayer();
					if (!object.isVisibleFor(nearbyPlayer))
					{
						continue;
					}
					
					nearbyPlayer.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Sends a packet to all players who can see {@code object} within {@code range}.
	 * @param object The source {@code WorldObject} used to determine the broadcast location.
	 * @param packet The {@code ServerPacket} to be sent to the players.
	 * @param range The maximum distance for the broadcast.
	 */
	public static void broadcastToVisiblePlayersInRange(WorldObject object, ServerPacket packet, int range)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		packet.sendInBroadcast();
		
		final WorldRegion[] regions = worldRegion.getSurroundingRegions();
		if (regions != null)
		{
			for (int i = 0; i < regions.length; i++)
			{
				final Collection<WorldObject> objects = regions[i].getVisibleObjects();
				if (objects.isEmpty())
				{
					continue;
				}
				
				for (WorldObject nearby : objects)
				{
					if ((nearby == object) || !nearby.isPlayer() || (nearby.getInstanceWorld() != object.getInstanceWorld()) || (nearby.calculateDistance3D(object) > range))
					{
						continue;
					}
					
					final Player nearbyPlayer = nearby.asPlayer();
					if (!object.isVisibleFor(nearbyPlayer))
					{
						continue;
					}
					
					nearbyPlayer.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Sends a packet to {@code object} and all players who can see it.
	 * @param object The {@code WorldObject} associated with the action.
	 * @param packet The {@code ServerPacket} to be broadcasted.
	 */
	public static void broadcastToSelfAndVisiblePlayers(WorldObject object, ServerPacket packet)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		packet.sendInBroadcast();
		
		final WorldRegion[] regions = worldRegion.getSurroundingRegions();
		if (regions != null)
		{
			for (int i = 0; i < regions.length; i++)
			{
				final Collection<WorldObject> objects = regions[i].getVisibleObjects();
				if (objects.isEmpty())
				{
					continue;
				}
				
				for (WorldObject nearby : objects)
				{
					if (!nearby.isPlayer() || (nearby.getInstanceWorld() != object.getInstanceWorld()))
					{
						continue;
					}
					
					final Player nearbyPlayer = nearby.asPlayer();
					if ((nearby != object) && !object.isVisibleFor(nearbyPlayer))
					{
						continue;
					}
					
					nearbyPlayer.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Sends a packet to {@code object} and all players who can see it within {@code range}.
	 * @param object The {@code WorldObject} used as the center point for the broadcast.
	 * @param packet The {@code ServerPacket} to be sent to the players.
	 * @param range The maximum distance to check for visible players.
	 */
	public static void broadcastToSelfAndVisiblePlayersInRange(WorldObject object, ServerPacket packet, int range)
	{
		if (object == null)
		{
			return;
		}
		
		final WorldRegion worldRegion = getRegion(object);
		if (worldRegion == null)
		{
			return;
		}
		
		packet.sendInBroadcast();
		
		final WorldRegion[] regions = worldRegion.getSurroundingRegions();
		if (regions != null)
		{
			for (int i = 0; i < regions.length; i++)
			{
				final Collection<WorldObject> objects = regions[i].getVisibleObjects();
				if (objects.isEmpty())
				{
					continue;
				}
				
				for (WorldObject nearby : objects)
				{
					if (!nearby.isPlayer() || (nearby.getInstanceWorld() != object.getInstanceWorld()) || (nearby.calculateDistance3D(object) > range))
					{
						continue;
					}
					
					final Player nearbyPlayer = nearby.asPlayer();
					if ((nearby != object) && !object.isVisibleFor(nearbyPlayer))
					{
						continue;
					}
					
					nearbyPlayer.sendPacket(packet);
				}
			}
		}
	}
	
	/**
	 * Sends a {@code ServerPacket} to every online player.<br>
	 * This method ensures that all active players receive the specified packet.<br>
	 * It iterates through the internal collection of all players.
	 * @param packet The {@code ServerPacket} to be sent to all online players.
	 */
	public static void broadcastToAllOnlinePlayers(ServerPacket packet)
	{
		packet.sendInBroadcast();
		
		for (Player player : _allPlayers.values())
		{
			if (player.isOnline())
			{
				player.sendPacket(packet);
			}
		}
	}
	
	/**
	 * Sends a message to every player currently online.<br>
	 * This method handles the distribution of text across all active connections.
	 * @param text The content of the message to be sent.
	 */
	public static void broadcastToAllOnlinePlayers(String text)
	{
		broadcastToAllOnlinePlayers(text, false);
	}
	
	/**
	 * Sends a message to every player currently online.<br>
	 * The message appears as an announcement or a critical alert.
	 * @param text The content of the message to be sent.
	 * @param isCritical Set to {@code true} for a critical announcement, or {@code false} for a standard one.
	 */
	public static void broadcastToAllOnlinePlayers(String text, boolean isCritical)
	{
		broadcastToAllOnlinePlayers(new CreatureSay(null, isCritical ? ChatType.CRITICAL_ANNOUNCE : ChatType.ANNOUNCEMENT, "", text));
	}
	
	/**
	 * Sends a message to every online player currently on the screen.<br>
	 * The message is displayed as an {@code ExShowScreenMessage}.<br>
	 * It remains visible for 10000 milliseconds.
	 * @param text The content of the message to display.
	 */
	public static void broadcastToAllOnlinePlayersOnScreen(String text)
	{
		broadcastToAllOnlinePlayers(new ExShowScreenMessage(text, 10000));
	}
	
	/**
	 * Retrieves the world's region grid.<br>
	 * The returned {@code AtomicReferenceArray} stores all {@code WorldRegion} instances using the world's flattened region index.<br>
	 * Regions are created lazily and may contain {@code null} entries until first accessed.
	 * @return The {@code AtomicReferenceArray} containing all world regions.
	 */
	public static AtomicReferenceArray<WorldRegion> getWorldRegions()
	{
		return _worldRegions;
	}
	
	/**
	 * Finds the {@code WorldRegion} associated with a specific {@code WorldObject}.<br>
	 * It calculates the coordinates based on the object's position.<br>
	 * If the object is outside of valid bounds, it is disposed and returns {@code null}.
	 * @param object The {@code WorldObject} to check.
	 * @return The corresponding {@code WorldRegion}, or {@code null} if not found.
	 */
	public static WorldRegion getRegion(WorldObject object)
	{
		final int regionX = (object.getX() >> SHIFT_BY) + OFFSET_X;
		final int regionY = (object.getY() >> SHIFT_BY) + OFFSET_Y;
		final int regionZ = calculateRegionZ(object.getZ());
		if ((regionX < 0) || (regionX > REGIONS_X) || (regionY < 0) || (regionY > REGIONS_Y) || (regionZ < 0) || (regionZ >= REGIONS_Z)) // Precaution. Moved at invalid region?
		{
			disposeOutOfBoundsObject(object);
			return null;
		}
		
		final int index = ((((regionX * (REGIONS_Y + 1)) + regionY) * REGIONS_Z) + regionZ);
		WorldRegion region = _worldRegions.get(index);
		if (region == null)
		{
			final WorldRegion newRegion = new WorldRegion(regionX, regionY, regionZ);
			final WorldRegion existingRegion = _worldRegions.compareAndExchange(index, null, newRegion);
			region = existingRegion == null ? newRegion : existingRegion;
		}
		
		region.getSurroundingRegions(); // Link the surrounding regions once, so callers always receive a fully linked region.
		return region;
	}
	
	/**
	 * Retrieves the {@code WorldRegion} at the specified coordinates.<br>
	 * This method calculates the region based on the provided {@code x}, {@code y}, and {@code z} values.<br>
	 * It returns {@code null} if the coordinates are outside the valid world boundaries.
	 * @param x The horizontal coordinate.
	 * @param y The vertical coordinate.
	 * @param z The depth coordinate.
	 * @return The {@code WorldRegion} at the location, or {@code null} if not found.
	 */
	public static WorldRegion getRegion(int x, int y, int z)
	{
		final int regionX = (x >> SHIFT_BY) + OFFSET_X;
		final int regionY = (y >> SHIFT_BY) + OFFSET_Y;
		final int regionZ = calculateRegionZ(z);
		if ((regionX < 0) || (regionX > REGIONS_X) || (regionY < 0) || (regionY > REGIONS_Y) || (regionZ < 0) || (regionZ >= REGIONS_Z))
		{
			LOGGER.warning(World.class.getSimpleName() + ": Incorrect world region X: " + regionX + " Y: " + regionY + " Z: " + regionZ);
			return null;
		}
		
		final int index = ((((regionX * (REGIONS_Y + 1)) + regionY) * REGIONS_Z) + regionZ);
		WorldRegion region = _worldRegions.get(index);
		if (region == null)
		{
			final WorldRegion newRegion = new WorldRegion(regionX, regionY, regionZ);
			final WorldRegion existingRegion = _worldRegions.compareAndExchange(index, null, newRegion);
			region = existingRegion == null ? newRegion : existingRegion;
		}
		
		region.getSurroundingRegions(); // Link the surrounding regions once, so callers always receive a fully linked region.
		return region;
	}
	
	/**
	 * Calculates the vertical region index based on a given height.<br>
	 * It clamps values outside the allowed range to the minimum or maximum regions.
	 * @param z The current height value of an object.
	 * @return The calculated zero-based region index.
	 */
	private static int calculateRegionZ(int z)
	{
		// Handle values outside the normal range.
		if (z < WORLD_Z_MIN)
		{
			return 0; // Bottom region.
		}
		if (z > WORLD_Z_MAX)
		{
			return REGIONS_Z - 1; // Top region.
		}
		
		// Calculate the region Z index (0-based).
		return (z - WORLD_Z_MIN) / Z_REGION_SIZE;
	}
	
	/**
	 * Removes an object that has moved outside of the valid game boundaries.<br>
	 * This method handles specific cleanup for players, summons, and NPCs.<br>
	 * It ensures the object is removed from the world tracking system.
	 * @param object The {@code WorldObject} to be disposed.
	 */
	public static synchronized void disposeOutOfBoundsObject(WorldObject object)
	{
		if (object.isPlayer())
		{
			object.asCreature().stopMove(object.asPlayer().getLastServerPosition());
		}
		else if (object.isSummon())
		{
			final Summon summon = object.asSummon();
			summon.unSummon(summon.getOwner());
		}
		else if (_allObjects.remove(object.getObjectId()) != null)
		{
			if (object.isNpc())
			{
				final Npc npc = object.asNpc();
				LOGGER.warning("Deleting npc " + object.getName() + " NPCID[" + npc.getId() + "] from invalid location X:" + object.getX() + " Y:" + object.getY() + " Z:" + object.getZ());
				npc.deleteMe();
				
				final Spawn spawn = npc.getSpawn();
				if (spawn != null)
				{
					LOGGER.warning("Spawn location X:" + spawn.getX() + " Y:" + spawn.getY() + " Z:" + spawn.getZ() + " Heading:" + spawn.getHeading());
				}
			}
			else if (object.isCreature())
			{
				LOGGER.warning("Deleting object " + object.getName() + " OID[" + object.getObjectId() + "] from invalid location X:" + object.getX() + " Y:" + object.getY() + " Z:" + object.getZ());
				object.asCreature().deleteMe();
			}
			
			final WorldRegion worldRegion = object.getWorldRegion();
			if (worldRegion != null)
			{
				worldRegion.removeVisibleObject(object);
			}
		}
	}
	
	/**
	 * Increments the party counter.
	 */
	public static void incrementParty()
	{
		_partyNumber.incrementAndGet();
	}
	
	/**
	 * Decrements the party counter.
	 */
	public static void decrementParty()
	{
		_partyNumber.decrementAndGet();
	}
	
	/**
	 * Increments the party member counter.
	 */
	public static void incrementPartyMember()
	{
		_memberInPartyNumber.incrementAndGet();
	}
	
	/**
	 * Decrements the party member counter.
	 */
	public static void decrementPartyMember()
	{
		_memberInPartyNumber.decrementAndGet();
	}
	
	/**
	 * Retrieves the current number of parties.<br>
	 * This method returns the value stored in the {@code _partyNumber} atomic integer.
	 * @return The total count of parties as an {@code int}.
	 */
	public static int getPartyCount()
	{
		return _partyNumber.get();
	}
	
	/**
	 * Retrieves the current number of members in a party.<br>
	 * This method returns the value stored in the {@code _memberInPartyNumber} counter.
	 * @return The total count of party members as an {@code int}.
	 */
	public static int getPartyMemberCount()
	{
		return _memberInPartyNumber.get();
	}
	
	/**
	 * Adds a {@code Player} to the list of top players.<br>
	 * This method manages the size of the player list by removing the player with the fewest kills if the limit is exceeded.<br>
	 * It updates the last update time to the current system time in seconds.
	 * @param player The {@code Player} to be added to the collection.
	 */
	public static synchronized void addPkPlayer(Player player)
	{
		if (_pkPlayers.size() > 29)
		{
			Player lowestPk = null;
			int lowestPkCount = Integer.MAX_VALUE;
			for (Player pk : _pkPlayers)
			{
				final int pkKills = pk.getPkKills();
				if (pkKills < lowestPkCount)
				{
					lowestPk = pk;
					lowestPkCount = pkKills;
				}
			}
			
			if ((lowestPk != null) && (lowestPkCount < player.getPkKills()))
			{
				_pkPlayers.remove(lowestPk);
			}
			else
			{
				return;
			}
		}
		
		_pkPlayers.add(player);
		_lastPkTime.set((int) System.currentTimeMillis() / 1000);
	}
	
	/**
	 * Removes a specific {@code Player} from the PK players list.<br>
	 * This method also updates the last PK time to the current timestamp.
	 * @param player The {@code Player} object to remove.
	 */
	public static void removePkPlayer(Player player)
	{
		_pkPlayers.remove(player);
		_lastPkTime.set((int) System.currentTimeMillis() / 1000);
	}
	
	/**
	 * Retrieves the set of players who are currently on the PK penalty list.<br>
	 * This method returns an empty {@code Set} if the PK penalty feature is disabled.
	 * @return a {@code Set} of {@code Player} objects on the penalty list.
	 */
	public static Set<Player> getPkPlayers()
	{
		if (!FeatureConfig.PK_PENALTY_LIST)
		{
			return Collections.emptySet();
		}
		
		return _pkPlayers;
	}
	
	/**
	 * Retrieves the timestamp of the last Player Kill (PK).<br>
	 * This value is stored in an {@code AtomicInteger}.
	 * @return The time of the last PK event as an {@code int}.
	 */
	public static int getLastPkTime()
	{
		return _lastPkTime.get();
	}
	
	/**
	 * Retrieves the singleton instance of the {@code World} class.<br>
	 * This method provides global access to the game world data.
	 * @return The single {@code World} instance.
	 */
	public static World getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final World INSTANCE = new World();
	}
}
