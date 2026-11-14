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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.holders.TimedHuntingZoneHolder;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.zone.type.RespawnZone;
import org.l2jmobius.gameserver.interfaces.ILocational;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Fort;

/**
 * Manages map regions, spawn points, and teleportation locations across the game world.<br>
 * This class handles player respawning logic based on location, karma status, clan ownership, and race restrictions.
 * @author Mobius
 */
public class MapRegionData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MapRegionData.class.getName());
	
	// Constants.
	private static final String DEFAULT_RESPAWN = "talking_island_town";
	private static final String FALLBACK_FLYING_RESPAWN = "union_base_of_kserth";
	private static final String DEFAULT_TOWN = "Aden Castle Town";
	private static final int MAP_REGION_X_OFFSET = 20; // (9 + 11) center tile offset.
	private static final int MAP_REGION_Y_OFFSET = 18; // (10 + 8) center tile offset.
	private static final int COORDINATE_SHIFT = 15; // Bit shift for region calculation.
	private static final int GRID_SIZE = 32; // Spatial grid dimensions for fast lookups.
	
	/**
	 * Compact region data holder to improve cache locality and reduce memory overhead.<br>
	 * All region properties stored together in a single object for efficient access.<br>
	 * Package-private to keep implementation details hidden.
	 */
	private static class RegionData
	{
		final String town;
		final int locId;
		final int bbs;
		List<int[]> maps;
		List<Location> spawnLocs;
		List<Location> otherSpawnLocs;
		List<Location> chaoticSpawnLocs;
		List<Location> banishSpawnLocs;
		Map<Race, String> bannedRaces;
		
		RegionData(String town, int locId, int bbs)
		{
			this.town = town;
			this.locId = locId;
			this.bbs = bbs;
		}
	}
	
	// Primary region storage - single HashMap for all regions.
	private final Map<String, RegionData> _regions = new HashMap<>();
	
	// Spatial index for O(1) coordinate-to-region lookups.
	private final RegionData[][] _spatialGrid = new RegionData[GRID_SIZE][GRID_SIZE];
	
	// Cached frequently accessed regions to avoid HashMap lookups.
	private RegionData _defaultRespawnRegion;
	private RegionData _fallbackFlyingRegion;
	
	/**
	 * Initializes a new instance of the {@code MapRegionData} class.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected MapRegionData()
	{
		load();
	}
	
	/**
	 * Loads all map region data from the configuration files.<br>
	 * This method clears existing data and rebuilds the spatial index.<br>
	 * It also initializes default respawn regions for use by the server.
	 */
	@Override
	public void load()
	{
		_regions.clear();
		
		// Clear spatial grid.
		for (int i = 0; i < GRID_SIZE; i++)
		{
			for (int j = 0; j < GRID_SIZE; j++)
			{
				_spatialGrid[i][j] = null;
			}
		}
		
		parseDatapackDirectory("data/mapregion", false);
		
		// Build spatial index after loading all regions.
		buildSpatialIndex();
		
		// Cache frequently accessed regions.
		_defaultRespawnRegion = _regions.get(DEFAULT_RESPAWN);
		_fallbackFlyingRegion = _regions.get(FALLBACK_FLYING_RESPAWN);
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _regions.size() + " map regions.");
	}
	
	/**
	 * Initializes the spatial grid for fast location lookups.<br>
	 * Iterates through all regions to map their coordinates into a 2D array.<br>
	 * This allows the system to quickly find region data based on x and y positions.
	 */
	private void buildSpatialIndex()
	{
		for (RegionData region : _regions.values())
		{
			if (region.maps != null)
			{
				for (int[] map : region.maps)
				{
					final int x = map[0];
					final int y = map[1];
					if ((x >= 0) && (x < GRID_SIZE) && (y >= 0) && (y < GRID_SIZE))
					{
						_spatialGrid[x][y] = region;
					}
				}
			}
		}
	}
	
	/**
	 * Parses the provided {@code Document} to populate region data.<br>
	 * This method reads XML nodes to identify regions and their properties.<br>
	 * It extracts information such as spawn locations, maps, and banned races.
	 * @param document The {@code Document} object containing the XML content.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		String name;
		String town;
		int locId;
		int bbs;
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("region".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						name = attrs.getNamedItem("name").getNodeValue();
						town = attrs.getNamedItem("town").getNodeValue();
						locId = parseInteger(attrs, "locId");
						bbs = parseInteger(attrs, "bbs");
						
						final RegionData region = new RegionData(town, locId, bbs);
						
						for (Node c = d.getFirstChild(); c != null; c = c.getNextSibling())
						{
							attrs = c.getAttributes();
							if ("respawnPoint".equalsIgnoreCase(c.getNodeName()))
							{
								final int spawnX = parseInteger(attrs, "X");
								final int spawnY = parseInteger(attrs, "Y");
								final int spawnZ = parseInteger(attrs, "Z");
								final boolean other = parseBoolean(attrs, "isOther", false);
								final boolean chaotic = parseBoolean(attrs, "isChaotic", false);
								final boolean banish = parseBoolean(attrs, "isBanish", false);
								if (other)
								{
									if (region.otherSpawnLocs == null)
									{
										region.otherSpawnLocs = new ArrayList<>();
									}
									
									region.otherSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
								else if (chaotic)
								{
									if (region.chaoticSpawnLocs == null)
									{
										region.chaoticSpawnLocs = new ArrayList<>();
									}
									
									region.chaoticSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
								else if (banish)
								{
									if (region.banishSpawnLocs == null)
									{
										region.banishSpawnLocs = new ArrayList<>();
									}
									
									region.banishSpawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
								else
								{
									if (region.spawnLocs == null)
									{
										region.spawnLocs = new ArrayList<>();
									}
									
									region.spawnLocs.add(new Location(spawnX, spawnY, spawnZ));
								}
							}
							else if ("map".equalsIgnoreCase(c.getNodeName()))
							{
								if (region.maps == null)
								{
									region.maps = new ArrayList<>();
								}
								
								region.maps.add(new int[]
								{
									parseInteger(attrs, "X"),
									parseInteger(attrs, "Y")
								});
							}
							else if ("banned".equalsIgnoreCase(c.getNodeName()))
							{
								if (region.bannedRaces == null)
								{
									region.bannedRaces = new EnumMap<>(Race.class);
								}
								
								region.bannedRaces.put(Race.valueOf(attrs.getNamedItem("race").getNodeValue()), attrs.getNamedItem("point").getNodeValue());
							}
						}
						
						_regions.put(name, region);
					}
				}
			}
		}
	}
	
	/**
	 * Retrieves a spawn location for the given {@code RegionData}.<br>
	 * It returns a random location if random respawns are enabled.<br>
	 * Otherwise, it returns the first available location in the list.
	 * @param region The {@code RegionData} containing the spawn locations.
	 * @return The selected {@code Location}, or {@code null} if no locations exist.
	 */
	private Location getSpawnLoc(RegionData region)
	{
		if (region.spawnLocs == null)
		{
			return null;
		}
		
		if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
		{
			return region.spawnLocs.get(Rnd.get(region.spawnLocs.size()));
		}
		
		return region.spawnLocs.get(0);
	}
	
	/**
	 * Retrieves a chaotic spawn location for the given region.<br>
	 * It checks if any chaotic locations are defined in {@code RegionData}.<br>
	 * If enabled, it returns a random location from the list.<br>
	 * Otherwise, it returns the first available chaotic location or falls back to the default spawn.
	 * @param region The {@code RegionData} containing the spawn information.
	 * @return The determined {@code Location} for spawning.
	 */
	private Location getChaoticSpawnLoc(RegionData region)
	{
		if (region.chaoticSpawnLocs != null)
		{
			if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
			{
				return region.chaoticSpawnLocs.get(Rnd.get(region.chaoticSpawnLocs.size()));
			}
			
			return region.chaoticSpawnLocs.get(0);
		}
		
		return getSpawnLoc(region);
	}
	
	/**
	 * Retrieves the {@code RegionData} for a specific set of coordinates.<br>
	 * This method uses a spatial grid to find the region at the given location.<br>
	 * It returns {@code null} if the coordinates are outside the valid map boundaries.
	 * @param locX The X coordinate of the location.
	 * @param locY The Y coordinate of the location.
	 * @return The {@code RegionData} object for the specified coordinates, or {@code null}.
	 */
	public RegionData getMapRegion(int locX, int locY)
	{
		final int regionX = getMapRegionX(locX);
		final int regionY = getMapRegionY(locY);
		
		// O(1) lookup via spatial grid.
		if ((regionX >= 0) && (regionX < GRID_SIZE) && (regionY >= 0) && (regionY < GRID_SIZE))
		{
			return _spatialGrid[regionX][regionY];
		}
		
		return null;
	}
	
	/**
	 * Retrieves the unique location ID for a specific set of coordinates.<br>
	 * This method finds the {@code RegionData} at the given position.<br>
	 * If no region is found, it returns {@code 0}.
	 * @param locX The X coordinate of the location.
	 * @param locY The Y coordinate of the location.
	 * @return The unique location ID or {@code 0} if not found.
	 */
	public int getMapRegionLocId(int locX, int locY)
	{
		final RegionData region = getMapRegion(locX, locY);
		return region != null ? region.locId : 0;
	}
	
	/**
	 * Retrieves the {@code RegionData} for a specific world object.<br>
	 * This method uses the X and Y coordinates of the provided {@code WorldObject}.
	 * @param obj The {@code WorldObject} to check for its region.
	 * @return The {@code RegionData} associated with the object's location.
	 */
	public RegionData getMapRegion(WorldObject obj)
	{
		return getMapRegion(obj.getX(), obj.getY());
	}
	
	/**
	 * Retrieves the region location ID for a specific world object.<br>
	 * This method uses the X and Y coordinates of the provided {@code WorldObject}.
	 * @param obj The {@code WorldObject} to check for its location.
	 * @return The integer ID of the map region.
	 */
	public int getMapRegionLocId(WorldObject obj)
	{
		return getMapRegionLocId(obj.getX(), obj.getY());
	}
	
	/**
	 * Calculates the map region identifier for a given X coordinate.<br>
	 * This method uses bitwise shifting and an offset to determine the region.
	 * @param posX The X coordinate of the location.
	 * @return The calculated map region ID for the provided X position.
	 */
	public int getMapRegionX(int posX)
	{
		return (posX >> COORDINATE_SHIFT) + MAP_REGION_X_OFFSET;
	}
	
	/**
	 * Calculates the map region identifier for a given Y coordinate.<br>
	 * This method uses bitwise shifting and an offset to determine the correct region.
	 * @param posY The raw Y coordinate of the location.
	 * @return The calculated map region ID for the specified Y position.
	 */
	public int getMapRegionY(int posY)
	{
		return (posY >> COORDINATE_SHIFT) + MAP_REGION_Y_OFFSET;
	}
	
	/**
	 * Finds the name of the town closest to a specific creature.<br>
	 * It checks the current map region for a town association.<br>
	 * If no region is found, it returns a default value.
	 * @param creature The {@code Creature} used to determine the location.
	 * @return The name of the nearest town as a {@code String}.
	 */
	public String getClosestTownName(Creature creature)
	{
		final RegionData region = getMapRegion(creature);
		return region == null ? DEFAULT_TOWN : region.town;
	}
	
	/**
	 * Determines the destination location for a creature based on the teleport type.<br>
	 * This method handles specific logic for players such as clan halls, castles, and fortresses.<br>
	 * It also accounts for karma status, siege events, and faction systems.<br>
	 * If no specific location is found, it defaults to the nearest town respawn.
	 * @param creature The {@code Creature} attempting to teleport.
	 * @param teleportWhere The {@code TeleportWhereType} specifying the destination category.
	 * @return The {@code Location} of the destination or {@code null} if unavailable.
	 */
	public Location getTeleToLocation(Creature creature, TeleportWhereType teleportWhere)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			if (player.getUCState() != Player.UC_STATE_NONE)
			{
				return null;
			}
			
			Castle castle = null;
			Fort fort = null;
			ClanHall clanhall = null;
			final Clan clan = player.getClan();
			
			// Flying players in Gracia cannot use teleports to Aden continent.
			if ((clan != null) && !player.isFlyingMounted() && !player.isFlying())
			{
				// Clan hall teleportation.
				switch (teleportWhere)
				{
					case CLANHALL:
					{
						clanhall = ClanHallData.getInstance().getClanHallByClan(clan);
						if ((clanhall != null) && !player.isFlyingMounted())
						{
							return clanhall.getOwnerLocation();
						}
						break;
					}
					case CASTLE:
					{
						// Check if player is on castle ground and clan is defender.
						castle = CastleManager.getInstance().getCastleByOwner(clan);
						if (castle == null)
						{
							castle = CastleManager.getInstance().getCastle(player);
							if (castle != null)
							{
								final org.l2jmobius.gameserver.mechanics.siege.Siege castleSiege = castle.getSiege();
								if (!(castleSiege.isInProgress() && (castleSiege.getDefenderClan(clan) != null)))
								{
									castle = null;
								}
							}
						}
						
						if ((castle != null) && (castle.getResidenceId() > 0))
						{
							final org.l2jmobius.gameserver.entity.zone.type.CastleZone castleZone = castle.getResidenceZone();
							if (player.getReputation() < 0)
							{
								return castleZone.getChaoticSpawnLoc();
							}
							
							return castleZone.getSpawnLoc();
						}
						break;
					}
					case FORTRESS:
					{
						// Check if player is on fortress ground and clan is defender.
						fort = FortManager.getInstance().getFortByOwner(clan);
						if (fort == null)
						{
							fort = FortManager.getInstance().getFort(player);
							if (fort != null)
							{
								final org.l2jmobius.gameserver.mechanics.siege.FortSiege fortSiege = fort.getSiege();
								if (!(fortSiege.isInProgress() && (fort.getOwnerClan() == clan)))
								{
									fort = null;
								}
							}
						}
						
						if ((fort != null) && (fort.getResidenceId() > 0))
						{
							final org.l2jmobius.gameserver.entity.zone.type.FortZone fortZone = fort.getResidenceZone();
							if (player.getReputation() < 0)
							{
								return fortZone.getChaoticSpawnLoc();
							}
							
							return fortZone.getSpawnLoc();
						}
						break;
					}
					case SIEGEFLAG:
					{
						castle = CastleManager.getInstance().getCastle(player);
						fort = FortManager.getInstance().getFort(player);
						if (castle != null)
						{
							final org.l2jmobius.gameserver.mechanics.siege.Siege castleSiege = castle.getSiege();
							if (castleSiege.isInProgress())
							{
								final Set<Npc> flags = castleSiege.getFlag(clan);
								if ((flags != null) && !flags.isEmpty())
								{
									return flags.stream().findAny().get().getLocation();
								}
							}
						}
						else if (fort != null)
						{
							final org.l2jmobius.gameserver.mechanics.siege.FortSiege fortSiege = fort.getSiege();
							if (fortSiege.isInProgress())
							{
								final Set<Npc> flags = fortSiege.getFlag(clan);
								if ((flags != null) && !flags.isEmpty())
								{
									return flags.stream().findAny().get().getLocation();
								}
							}
						}
						break;
					}
				}
			}
			
			// Timed hunting zone exit.
			final TimedHuntingZoneHolder timedHuntingZone = player.getTimedHuntingZone();
			if (timedHuntingZone != null)
			{
				final Location exitLocation = timedHuntingZone.getExitLocation();
				if (exitLocation != null)
				{
					return exitLocation;
				}
			}
			
			// Karma player respawn outside city.
			if (player.getReputation() < 0)
			{
				return getNearestKarmaRespawn(player);
			}
			
			// Respawn far from castle during siege if clan is participating.
			castle = CastleManager.getInstance().getCastle(player);
			if ((castle != null) && castle.getSiege().isInProgress() && (castle.getSiege().checkIsDefender(clan) || castle.getSiege().checkIsAttacker(clan)))
			{
				return castle.getResidenceZone().getOtherSpawnLoc();
			}
			
			// Instance exit location.
			final Instance inst = player.getInstanceWorld();
			if (inst != null)
			{
				final Location loc = inst.getExitLocation(player);
				if (loc != null)
				{
					return loc;
				}
			}
			
			// Faction system base respawn.
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_RESPAWN_AT_BASE)
			{
				if (player.isGood())
				{
					return FactionSystemConfig.FACTION_GOOD_BASE_LOCATION;
				}
				
				if (player.isEvil())
				{
					return FactionSystemConfig.FACTION_EVIL_BASE_LOCATION;
				}
			}
		}
		
		return getNearestTownRespawn(creature);
	}
	
	/**
	 * Finds the closest karma respawn location for a specific player.<br>
	 * This method checks for active respawn zones and handles race restrictions.<br>
	 * It falls back to default regions if no specific zone is found.
	 * @param player The {@code Player} object to check for respawn logic.
	 * @return The {@code Location} of the nearest karma respawn point, or {@code null} if none exist.
	 */
	public Location getNearestKarmaRespawn(Player player)
	{
		try
		{
			final RespawnZone zone = ZoneManager.getInstance().getZone(player, RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegionChaoticSpawnLoc(player, zone.getRespawnPoint(player));
			}
			
			final RegionData region = getMapRegion(player);
			if (region != null)
			{
				// Check for banned races and redirect to alternate region.
				final Map<Race, String> bannedRaces = region.bannedRaces;
				if (bannedRaces != null)
				{
					final Race playerRace = player.getRace();
					if (bannedRaces.containsKey(playerRace))
					{
						final String alternateRegionName = bannedRaces.get(playerRace);
						final RegionData alternateRegion = _regions.get(alternateRegionName);
						if (alternateRegion != null)
						{
							return getChaoticSpawnLoc(alternateRegion);
						}
					}
				}
				
				return getChaoticSpawnLoc(region);
			}
		}
		catch (Exception e)
		{
			// Fall through to default handling.
		}
		
		// Use cached region to avoid HashMap lookup.
		if (player.isFlyingMounted() && (_fallbackFlyingRegion != null))
		{
			return getChaoticSpawnLoc(_fallbackFlyingRegion);
		}
		
		// Use cached default region.
		if (_defaultRespawnRegion != null)
		{
			return getChaoticSpawnLoc(_defaultRespawnRegion);
		}
		
		return null;
	}
	
	/**
	 * Finds the nearest town respawn location for a given creature.<br>
	 * It checks for specific respawn zones and region rules first.<br>
	 * If no specific rule applies, it returns the default respawn location.
	 * @param creature The {@code Creature} to calculate the respawn for.
	 * @return The {@code Location} of the nearest town respawn or {@code null}.
	 */
	public Location getNearestTownRespawn(Creature creature)
	{
		try
		{
			final RespawnZone zone = ZoneManager.getInstance().getZone(creature, RespawnZone.class);
			if (zone != null)
			{
				return getRestartRegionSpawnLoc(creature, zone.getRespawnPoint(creature.asPlayer()));
			}
			
			final RegionData region = getMapRegion(creature);
			if (region != null)
			{
				// Check for banned races and redirect to alternate region.
				if (creature.isPlayer())
				{
					final Player player = creature.asPlayer();
					final Map<Race, String> bannedRaces = region.bannedRaces;
					if (bannedRaces != null)
					{
						final Race playerRace = player.getRace();
						if (bannedRaces.containsKey(playerRace))
						{
							final String alternateRegionName = bannedRaces.get(playerRace);
							final RegionData alternateRegion = _regions.get(alternateRegionName);
							if (alternateRegion != null)
							{
								return getSpawnLoc(alternateRegion);
							}
						}
					}
				}
				
				return getSpawnLoc(region);
			}
		}
		catch (Exception e)
		{
			// Fall through to default respawn.
		}
		
		// Use cached default region.
		if (_defaultRespawnRegion != null)
		{
			return getSpawnLoc(_defaultRespawnRegion);
		}
		
		return null;
	}
	
	/**
	 * Retrieves the spawn location for a creature in a specific region.<br>
	 * It checks if the player's race is banned in the requested point.<br>
	 * If the race is banned, it finds an alternative spawn point.<br>
	 * Returns the default respawn location if no valid region is found.
	 * @param creature The {@code Creature} requesting a spawn location.
	 * @param point The name of the region point to check.
	 * @return The {@code Location} where the creature should spawn.
	 */
	public Location getRestartRegionSpawnLoc(Creature creature, String point)
	{
		try
		{
			final Player player = creature.asPlayer();
			RegionData region = _regions.get(point);
			if (region == null)
			{
				region = _defaultRespawnRegion;
			}
			
			final Map<Race, String> bannedRaces = region.bannedRaces;
			if (bannedRaces != null)
			{
				final Race playerRace = player.getRace();
				if (bannedRaces.containsKey(playerRace))
				{
					return getRestartRegionSpawnLoc(player, bannedRaces.get(playerRace));
				}
			}
			
			return getSpawnLoc(region);
		}
		catch (Exception e)
		{
			if (_defaultRespawnRegion != null)
			{
				return getSpawnLoc(_defaultRespawnRegion);
			}
			
			return null;
		}
	}
	
	/**
	 * Retrieves the chaotic spawn location for a creature based on a specific region point.<br>
	 * This method checks if the player's race is banned in the specified region.<br>
	 * If the race is banned, it returns a fallback location for that race.<br>
	 * Otherwise, it returns the standard chaotic spawn location for the region.
	 * @param creature The {@code Creature} requesting the spawn location.
	 * @param point The unique identifier for the region.
	 * @return The {@code Location} of the chaotic spawn or {@code null} if no region is found.
	 */
	public Location getRestartRegionChaoticSpawnLoc(Creature creature, String point)
	{
		try
		{
			RegionData region = _regions.get(point);
			if (region == null)
			{
				region = _defaultRespawnRegion;
			}
			
			final Player player = creature.asPlayer();
			final Map<Race, String> bannedRaces = region.bannedRaces;
			if (bannedRaces != null)
			{
				final Race playerRace = player.getRace();
				if (bannedRaces.containsKey(playerRace))
				{
					return getRestartRegionChaoticSpawnLoc(player, bannedRaces.get(playerRace));
				}
			}
			
			return getChaoticSpawnLoc(region);
		}
		catch (Exception e)
		{
			if (_defaultRespawnRegion != null)
			{
				return getChaoticSpawnLoc(_defaultRespawnRegion);
			}
			
			return null;
		}
	}
	
	/**
	 * Retrieves the location ID for a restart region based on a creature and a specific point.<br>
	 * This method checks if the player's race is banned in the specified region.<br>
	 * If it is banned, it recursively finds the correct location ID for that race.<br>
	 * It returns 0 or the default respawn ID if no valid region is found.
	 * @param creature The {@code Creature} entity to check.
	 * @param point The name of the restart region point.
	 * @return The integer ID of the location.
	 */
	public int getRestartRegionLocId(Creature creature, String point)
	{
		try
		{
			RegionData region = _regions.get(point);
			if (region == null)
			{
				return _defaultRespawnRegion != null ? _defaultRespawnRegion.locId : 0;
			}
			
			final Player player = creature.asPlayer();
			final Map<Race, String> bannedRaces = region.bannedRaces;
			if (bannedRaces != null)
			{
				final Race playerRace = player.getRace();
				if (bannedRaces.containsKey(playerRace))
				{
					return getRestartRegionLocId(player, bannedRaces.get(playerRace));
				}
			}
			
			return region.locId;
		}
		catch (Exception e)
		{
			return _defaultRespawnRegion != null ? _defaultRespawnRegion.locId : 0;
		}
	}
	
	/**
	 * Retrieves the unique identifier for a restart region based on a specific point.<br>
	 * This method is specifically used when the entity is a {@code Player}.
	 * @param player The {@code Player} object requesting the location ID.
	 * @param point The name of the specific point within the restart region.
	 * @return The integer ID of the restart region location.
	 */
	public int getRestartRegionLocId(Player player, String point)
	{
		return getRestartRegionLocId((Creature) player, point);
	}
	
	/**
	 * Retrieves a spawn location based on the provided region name.<br>
	 * It returns a random location if random respawns are enabled in {@code PlayerConfig}.<br>
	 * Otherwise, it returns the first available location for that region.
	 * @param regionName The unique name of the region to look up.
	 * @return A {@code Location} object or {@code null} if the region or its locations are not found.
	 */
	public Location getSpawnLocByRegionName(String regionName)
	{
		final RegionData region = _regions.get(regionName);
		if (region == null)
		{
			return null;
		}
		
		if (region.spawnLocs == null)
		{
			return null;
		}
		
		if (PlayerConfig.RANDOM_RESPAWN_IN_TOWN_ENABLED)
		{
			return region.spawnLocs.get(Rnd.get(region.spawnLocs.size()));
		}
		
		return region.spawnLocs.get(0);
	}
	
	/**
	 * Retrieves the name of the town associated with a specific map region.<br>
	 * This method looks up the {@code RegionData} using the provided name.<br>
	 * If the region is not found, it returns {@code null}.
	 * @param regionName The unique name of the map region to search for.
	 * @return The name of the town as a {@code String}, or {@code null} if no region exists.
	 */
	public String getMapRegionTown(String regionName)
	{
		final RegionData region = _regions.get(regionName);
		return region != null ? region.town : null;
	}
	
	/**
	 * Retrieves the number of BBs for a specific location.<br>
	 * It checks the map region at the given coordinates.<br>
	 * If no region is found, it returns the value from the default respawn region or 0.
	 * @param loc The {@code ILocational} object containing the coordinates.
	 * @return The number of BBs as an {@code int}.
	 */
	public int getBBs(ILocational loc)
	{
		final RegionData region = getMapRegion(loc.getX(), loc.getY());
		if (region != null)
		{
			return region.bbs;
		}
		
		// Use cached default region.
		return _defaultRespawnRegion != null ? _defaultRespawnRegion.bbs : 0;
	}
	
	/**
	 * Retrieves the singleton instance of {@code MapRegionData}.<br>
	 * This method provides global access to the map region data manager.
	 * @return The singleton instance of {@code MapRegionData}.
	 */
	public static MapRegionData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MapRegionData INSTANCE = new MapRegionData();
	}
}
