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
package org.l2jmobius.gameserver.managers;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalInt;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.AbstractZoneSettings;
import org.l2jmobius.gameserver.entity.zone.ZoneForm;
import org.l2jmobius.gameserver.entity.zone.ZoneRegion;
import org.l2jmobius.gameserver.entity.zone.ZoneRespawn;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.entity.zone.form.ZoneCuboid;
import org.l2jmobius.gameserver.entity.zone.form.ZoneCylinder;
import org.l2jmobius.gameserver.entity.zone.form.ZoneNPoly;
import org.l2jmobius.gameserver.entity.zone.type.ArenaZone;
import org.l2jmobius.gameserver.entity.zone.type.CastleZone;
import org.l2jmobius.gameserver.entity.zone.type.ClanHallZone;
import org.l2jmobius.gameserver.entity.zone.type.CleftZone;
import org.l2jmobius.gameserver.entity.zone.type.ConditionZone;
import org.l2jmobius.gameserver.entity.zone.type.ConquestZone;
import org.l2jmobius.gameserver.entity.zone.type.DamageZone;
import org.l2jmobius.gameserver.entity.zone.type.DerbyTrackZone;
import org.l2jmobius.gameserver.entity.zone.type.EffectZone;
import org.l2jmobius.gameserver.entity.zone.type.FishingZone;
import org.l2jmobius.gameserver.entity.zone.type.FlagPvPZone;
import org.l2jmobius.gameserver.entity.zone.type.FortZone;
import org.l2jmobius.gameserver.entity.zone.type.HqZone;
import org.l2jmobius.gameserver.entity.zone.type.JailZone;
import org.l2jmobius.gameserver.entity.zone.type.LandingZone;
import org.l2jmobius.gameserver.entity.zone.type.MotherTreeZone;
import org.l2jmobius.gameserver.entity.zone.type.NoLandingZone;
import org.l2jmobius.gameserver.entity.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.entity.zone.type.NoStoreZone;
import org.l2jmobius.gameserver.entity.zone.type.NoSummonFriendZone;
import org.l2jmobius.gameserver.entity.zone.type.OlympiadStadiumZone;
import org.l2jmobius.gameserver.entity.zone.type.PeaceZone;
import org.l2jmobius.gameserver.entity.zone.type.ResidenceHallTeleportZone;
import org.l2jmobius.gameserver.entity.zone.type.ResidenceTeleportZone;
import org.l2jmobius.gameserver.entity.zone.type.ResidenceZone;
import org.l2jmobius.gameserver.entity.zone.type.RespawnZone;
import org.l2jmobius.gameserver.entity.zone.type.SayuneZone;
import org.l2jmobius.gameserver.entity.zone.type.ScriptZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegableHallZone;
import org.l2jmobius.gameserver.entity.zone.type.SiegeZone;
import org.l2jmobius.gameserver.entity.zone.type.SpawnTerritory;
import org.l2jmobius.gameserver.entity.zone.type.SwampZone;
import org.l2jmobius.gameserver.entity.zone.type.TaxZone;
import org.l2jmobius.gameserver.entity.zone.type.TeleportZone;
import org.l2jmobius.gameserver.entity.zone.type.TimedHuntingZone;
import org.l2jmobius.gameserver.entity.zone.type.UndyingZone;
import org.l2jmobius.gameserver.entity.zone.type.WaterZone;
import org.l2jmobius.gameserver.interfaces.ILocational;

/**
 * Loads zone definitions from XML and manages all active game zones.
 * @author durgus, Mobius
 */
public class ZoneManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ZoneManager.class.getName());
	
	private static final Map<String, AbstractZoneSettings> SETTINGS = new HashMap<>();
	
	private static final int SHIFT_BY = 15;
	private static final int OFFSET_X = Math.abs(World.WORLD_X_MIN >> SHIFT_BY);
	private static final int OFFSET_Y = Math.abs(World.WORLD_Y_MIN >> SHIFT_BY);
	
	private final Map<Class<? extends ZoneType>, ConcurrentHashMap<Integer, ? extends ZoneType>> _classZones = new ConcurrentHashMap<>();
	private final Map<String, SpawnTerritory> _spawnTerritories = new ConcurrentHashMap<>();
	private final AtomicInteger _lastDynamicId = new AtomicInteger(300000);
	private List<Item> _debugItems;
	
	private final ZoneRegion[][] _zoneRegions = new ZoneRegion[(World.WORLD_X_MAX >> SHIFT_BY) + OFFSET_X + 1][(World.WORLD_Y_MAX >> SHIFT_BY) + OFFSET_Y + 1];
	
	/**
	 * Builds the grid of {@code ZoneRegion} objects and triggers the initial load of all zones.
	 */
	protected ZoneManager()
	{
		for (int x = 0; x < _zoneRegions.length; x++)
		{
			for (int y = 0; y < _zoneRegions[x].length; y++)
			{
				_zoneRegions[x][y] = new ZoneRegion(x, y);
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + " " + _zoneRegions.length + " by " + _zoneRegions[0].length + " Zone Region Grid set up.");
		load();
	}
	
	/**
	 * Clears existing zones, registers the known zone-class maps, and parses all zone data from the datapack.
	 */
	@Override
	public void load()
	{
		_classZones.clear();
		_classZones.put(ArenaZone.class, new ConcurrentHashMap<>());
		_classZones.put(CastleZone.class, new ConcurrentHashMap<>());
		_classZones.put(ClanHallZone.class, new ConcurrentHashMap<>());
		_classZones.put(CleftZone.class, new ConcurrentHashMap<>());
		_classZones.put(ConditionZone.class, new ConcurrentHashMap<>());
		_classZones.put(ConquestZone.class, new ConcurrentHashMap<>());
		_classZones.put(DamageZone.class, new ConcurrentHashMap<>());
		_classZones.put(DerbyTrackZone.class, new ConcurrentHashMap<>());
		_classZones.put(EffectZone.class, new ConcurrentHashMap<>());
		_classZones.put(FishingZone.class, new ConcurrentHashMap<>());
		_classZones.put(FlagPvPZone.class, new ConcurrentHashMap<>());
		_classZones.put(FortZone.class, new ConcurrentHashMap<>());
		_classZones.put(HqZone.class, new ConcurrentHashMap<>());
		_classZones.put(JailZone.class, new ConcurrentHashMap<>());
		_classZones.put(LandingZone.class, new ConcurrentHashMap<>());
		_classZones.put(MotherTreeZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoLandingZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoRestartZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoStoreZone.class, new ConcurrentHashMap<>());
		_classZones.put(NoSummonFriendZone.class, new ConcurrentHashMap<>());
		_classZones.put(OlympiadStadiumZone.class, new ConcurrentHashMap<>());
		_classZones.put(PeaceZone.class, new ConcurrentHashMap<>());
		_classZones.put(ResidenceHallTeleportZone.class, new ConcurrentHashMap<>());
		_classZones.put(ResidenceTeleportZone.class, new ConcurrentHashMap<>());
		_classZones.put(ResidenceZone.class, new ConcurrentHashMap<>());
		_classZones.put(RespawnZone.class, new ConcurrentHashMap<>());
		_classZones.put(SayuneZone.class, new ConcurrentHashMap<>());
		_classZones.put(ScriptZone.class, new ConcurrentHashMap<>());
		_classZones.put(SiegableHallZone.class, new ConcurrentHashMap<>());
		_classZones.put(SiegeZone.class, new ConcurrentHashMap<>());
		_classZones.put(SwampZone.class, new ConcurrentHashMap<>());
		_classZones.put(TaxZone.class, new ConcurrentHashMap<>());
		_classZones.put(TeleportZone.class, new ConcurrentHashMap<>());
		_classZones.put(TimedHuntingZone.class, new ConcurrentHashMap<>());
		_classZones.put(UndyingZone.class, new ConcurrentHashMap<>());
		_classZones.put(WaterZone.class, new ConcurrentHashMap<>());
		_spawnTerritories.clear();
		
		parseDatapackDirectory("data/zones", false);
		
		final String simpleName = getClass().getSimpleName();
		final int size = getSize();
		LOGGER.info(simpleName + ": Loaded " + _classZones.size() + " zone classes and " + size + " zones.");
		
		final OptionalInt maxId = _classZones.values().stream().flatMap(map -> map.keySet().stream()).mapToInt(Integer.class::cast).filter(value -> value < 300000).max();
		LOGGER.info(simpleName + ": Last static id " + maxId.getAsInt() + ".");
	}
	
	/**
	 * Parses zone definitions from an XML document, building each zone shape and registering it into intersecting world regions.
	 * @param document The {@code Document} containing the zone XML.
	 * @param file The source {@code File}.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		NamedNodeMap attrs;
		Node attribute;
		String zoneName;
		int[][] coords;
		int zoneId;
		int minZ;
		int maxZ;
		String zoneType;
		String zoneShape;
		final List<int[]> rs = new ArrayList<>();
		for (Node n = document.getFirstChild(); n != null; n = n.getNextSibling())
		{
			if ("list".equalsIgnoreCase(n.getNodeName()))
			{
				attrs = n.getAttributes();
				attribute = attrs.getNamedItem("enabled");
				if ((attribute != null) && !Boolean.parseBoolean(attribute.getNodeValue()))
				{
					continue;
				}
				
				for (Node d = n.getFirstChild(); d != null; d = d.getNextSibling())
				{
					if ("zone".equalsIgnoreCase(d.getNodeName()))
					{
						attrs = d.getAttributes();
						attribute = attrs.getNamedItem("type");
						if (attribute != null)
						{
							zoneType = attribute.getNodeValue();
						}
						else
						{
							LOGGER.warning("ZoneData: Missing type for zone in file: " + file.getName());
							continue;
						}
						
						attribute = attrs.getNamedItem("id");
						if (attribute != null)
						{
							zoneId = Integer.parseInt(attribute.getNodeValue());
						}
						else
						{
							zoneId = zoneType.equalsIgnoreCase("NpcSpawnTerritory") ? 0 : _lastDynamicId.incrementAndGet();
						}
						
						attribute = attrs.getNamedItem("name");
						if (attribute != null)
						{
							zoneName = attribute.getNodeValue();
						}
						else
						{
							zoneName = null;
						}
						
						// Check zone name for NpcSpawnTerritory. Must exist and to be unique.
						if (zoneType.equalsIgnoreCase("NpcSpawnTerritory"))
						{
							if (zoneName == null)
							{
								LOGGER.warning("ZoneData: Missing name for NpcSpawnTerritory in file: " + file.getName() + ", skipping zone");
								continue;
							}
							else if (_spawnTerritories.containsKey(zoneName))
							{
								LOGGER.warning("ZoneData: Name " + zoneName + " already used for another zone, check file: " + file.getName() + ". Skipping zone");
								continue;
							}
						}
						
						minZ = parseInteger(attrs, "minZ");
						maxZ = parseInteger(attrs, "maxZ");
						zoneType = parseString(attrs, "type");
						zoneShape = parseString(attrs, "shape");
						
						// Get the zone shape from xml.
						ZoneForm zoneForm = null;
						try
						{
							for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
							{
								if ("node".equalsIgnoreCase(cd.getNodeName()))
								{
									attrs = cd.getAttributes();
									final int[] point = new int[2];
									point[0] = parseInteger(attrs, "X");
									point[1] = parseInteger(attrs, "Y");
									rs.add(point);
								}
							}
							
							coords = rs.toArray(new int[0][2]);
							rs.clear();
							
							if ((coords == null) || (coords.length == 0))
							{
								LOGGER.warning(getClass().getSimpleName() + ": ZoneData: missing data for zone: " + zoneId + " XML file: " + file.getName());
								continue;
							}
							
							// Create this zone. Parsing for cuboids is a bit different than for other polygons cuboids need exactly 2 points to be defined.
							// Other polygons need at least 3 (one per vertex).
							if (zoneShape.equalsIgnoreCase("Cuboid"))
							{
								if (coords.length == 2)
								{
									zoneForm = new ZoneCuboid(coords[0][0], coords[1][0], coords[0][1], coords[1][1], minZ, maxZ);
								}
								else
								{
									LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Missing cuboid vertex data for zone: " + zoneId + " in file: " + file.getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("NPoly"))
							{
								// nPoly needs to have at least 3 vertices.
								if (coords.length > 2)
								{
									final int[] aX = new int[coords.length];
									final int[] aY = new int[coords.length];
									for (int i = 0; i < coords.length; i++)
									{
										aX[i] = coords[i][0];
										aY[i] = coords[i][1];
									}
									
									zoneForm = new ZoneNPoly(aX, aY, minZ, maxZ);
								}
								else
								{
									LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Bad data for zone: " + zoneId + " in file: " + file.getName());
									continue;
								}
							}
							else if (zoneShape.equalsIgnoreCase("Cylinder"))
							{
								// A Cylinder zone requires a center point.
								// at x,y and a radius.
								attrs = d.getAttributes();
								final int zoneRad = Integer.parseInt(attrs.getNamedItem("rad").getNodeValue());
								if ((coords.length == 1) && (zoneRad > 0))
								{
									zoneForm = new ZoneCylinder(coords[0][0], coords[0][1], minZ, maxZ, zoneRad);
								}
								else
								{
									LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Bad data for zone: " + zoneId + " in file: " + file.getName());
									continue;
								}
							}
							else
							{
								LOGGER.warning(getClass().getSimpleName() + ": ZoneData: Unknown shape: \"" + zoneShape + "\"  for zone: " + zoneId + " in file: " + file.getName());
								continue;
							}
						}
						catch (Exception e)
						{
							LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": ZoneData: Failed to load zone " + zoneId + " coordinates: " + e.getMessage(), e);
						}
						
						// No further parameters needed, if NpcSpawnTerritory is loading.
						if (zoneType.equalsIgnoreCase("NpcSpawnTerritory"))
						{
							_spawnTerritories.put(zoneName, new SpawnTerritory(zoneName, zoneForm));
							continue;
						}
						
						// Create the zone.
						Class<?> newZone = null;
						Constructor<?> zoneConstructor = null;
						ZoneType temp;
						try
						{
							newZone = Class.forName("org.l2jmobius.gameserver.entity.zone.type." + zoneType);
							zoneConstructor = newZone.getConstructor(int.class);
							temp = (ZoneType) zoneConstructor.newInstance(zoneId);
							temp.setZone(zoneForm);
						}
						catch (Exception e)
						{
							LOGGER.warning(getClass().getSimpleName() + ": ZoneData: No such zone type: " + zoneType + " in file: " + file.getName());
							continue;
						}
						
						// Check for additional parameters.
						for (Node cd = d.getFirstChild(); cd != null; cd = cd.getNextSibling())
						{
							if ("stat".equalsIgnoreCase(cd.getNodeName()))
							{
								attrs = cd.getAttributes();
								final String name = attrs.getNamedItem("name").getNodeValue();
								final String val = attrs.getNamedItem("val").getNodeValue();
								temp.setParameter(name, val);
							}
							else if ("spawn".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof ZoneRespawn))
							{
								attrs = cd.getAttributes();
								final int spawnX = Integer.parseInt(attrs.getNamedItem("X").getNodeValue());
								final int spawnY = Integer.parseInt(attrs.getNamedItem("Y").getNodeValue());
								final int spawnZ = Integer.parseInt(attrs.getNamedItem("Z").getNodeValue());
								final Node val = attrs.getNamedItem("type");
								((ZoneRespawn) temp).parseLoc(spawnX, spawnY, spawnZ, val == null ? null : val.getNodeValue());
							}
							else if ("race".equalsIgnoreCase(cd.getNodeName()) && (temp instanceof RespawnZone))
							{
								attrs = cd.getAttributes();
								final String race = attrs.getNamedItem("name").getNodeValue();
								final String point = attrs.getNamedItem("point").getNodeValue();
								((RespawnZone) temp).addRaceRespawnPoint(race, point);
							}
						}
						
						if (checkId(zoneId))
						{
							LOGGER.config(getClass().getSimpleName() + ": Caution: Zone (" + zoneId + ") from file: " + file.getName() + " overrides previous definition.");
						}
						
						if ((zoneName != null) && !zoneName.isEmpty())
						{
							temp.setName(zoneName);
						}
						
						addZone(zoneId, temp);
						
						// Register the zone into any world region it intersects with.
						for (int x = 0; x < _zoneRegions.length; x++)
						{
							for (int y = 0; y < _zoneRegions[x].length; y++)
							{
								final int ax = (x - OFFSET_X) << SHIFT_BY;
								final int bx = ((x + 1) - OFFSET_X) << SHIFT_BY;
								final int ay = (y - OFFSET_Y) << SHIFT_BY;
								final int by = ((y + 1) - OFFSET_Y) << SHIFT_BY;
								if (temp.getZone().intersectsRectangle(ax, bx, ay, by))
								{
									_zoneRegions[x][y].getZones().put(temp.getId(), temp);
								}
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Unloads and reloads all zones, revalidates every creature, and clears the cached {@code SETTINGS}.
	 */
	public void reload()
	{
		// Unload zones.
		unload();
		
		// Load the zones.
		load();
		
		// Re-validate all characters in zones.
		for (WorldObject obj : World.getVisibleObjects())
		{
			if (obj.isCreature())
			{
				obj.asCreature().revalidateZone(true);
			}
		}
		
		SETTINGS.clear();
	}
	
	/**
	 * Backs up active zone settings into {@code SETTINGS} and clears the zone lists of every {@code ZoneRegion}.
	 */
	public void unload()
	{
		// Get the world regions.
		int count = 0;
		
		// Backup old zone settings.
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			for (ZoneType zone : map.values())
			{
				final AbstractZoneSettings settings = zone.getSettings();
				if (settings != null)
				{
					SETTINGS.put(zone.getName(), settings);
				}
			}
		}
		
		// Clear zones.
		for (ZoneRegion[] zoneRegions : _zoneRegions)
		{
			for (ZoneRegion zoneRegion : zoneRegions)
			{
				zoneRegion.getZones().clear();
				count++;
			}
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Removed zones in " + count + " regions.");
	}
	
	/**
	 * Retrieves the total number of zones across all class maps.
	 * @return The total zone count as an {@code int}.
	 */
	public int getSize()
	{
		int i = 0;
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			i += map.size();
		}
		
		return i;
	}
	
	/**
	 * Checks whether a zone with the given id exists in any class map.
	 * @param id The zone id to check.
	 * @return {@code true} if found, {@code false} otherwise.
	 */
	private boolean checkId(int id)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			if (map.containsKey(id))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Registers a zone into the class map keyed by its runtime type.
	 * @param <T> The zone subtype.
	 * @param id The zone id.
	 * @param zone The {@code ZoneType} to add.
	 */
	@SuppressWarnings("unchecked")
	private <T extends ZoneType> void addZone(Integer id, T zone)
	{
		ConcurrentHashMap<Integer, T> map = (ConcurrentHashMap<Integer, T>) _classZones.get(zone.getClass());
		if (map == null)
		{
			_classZones.put(zone.getClass(), new ConcurrentHashMap<>());
			map = (ConcurrentHashMap<Integer, T>) _classZones.get(zone.getClass());
		}
		
		map.put(id, zone);
	}
	
	/**
	 * Retrieves all zones of the given type.
	 * @param <T> The zone subtype.
	 * @param zoneType The {@code Class} of the zone type.
	 * @return A {@code Collection} of all zones of that type.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> Collection<T> getAllZones(Class<T> zoneType)
	{
		return (Collection<T>) _classZones.get(zoneType).values();
	}
	
	/**
	 * Retrieves the first zone matching the given id across all class maps.
	 * @param id The zone id.
	 * @return The matching {@code ZoneType}, or {@code null} if none.
	 */
	public ZoneType getZoneById(int id)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			final ZoneType zone = map.get(id);
			if (zone != null)
			{
				return zone;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the first zone matching the given name across all class maps.
	 * @param name The zone name.
	 * @return The matching {@code ZoneType}, or {@code null} if none.
	 */
	public ZoneType getZoneByName(String name)
	{
		for (Map<Integer, ? extends ZoneType> map : _classZones.values())
		{
			for (ZoneType zone : map.values())
			{
				final String zName = zone.getName();
				if ((zName != null) && zName.equals(name))
				{
					return zone;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the zone of the given type with the given id.
	 * @param <T> The zone subtype.
	 * @param id The zone id.
	 * @param zoneType The {@code Class} of the zone type.
	 * @return The matching zone, or {@code null} if none.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZoneById(int id, Class<T> zoneType)
	{
		return (T) _classZones.get(zoneType).get(id);
	}
	
	/**
	 * Retrieves the zone of the given type with the given name.
	 * @param <T> The zone subtype.
	 * @param name The zone name.
	 * @param zoneType The {@code Class} of the zone type.
	 * @return The matching zone, or {@code null} if none.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZoneByName(String name, Class<T> zoneType)
	{
		final Map<Integer, ? extends ZoneType> zones = _classZones.get(zoneType);
		if (zones != null)
		{
			for (ZoneType zone : zones.values())
			{
				final String zoneName = zone.getName();
				if ((zoneName != null) && zoneName.equals(name))
				{
					return (T) zone;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves all zones at the coordinates of the given location.
	 * @param locational The location to check.
	 * @return A {@code List} of zones at the location.
	 */
	public List<ZoneType> getZones(ILocational locational)
	{
		return getZones(locational.getX(), locational.getY(), locational.getZ());
	}
	
	/**
	 * Retrieves the zone of the given type at the given location.
	 * @param <T> The zone subtype.
	 * @param locational The location to check.
	 * @param type The {@code Class} of the zone type.
	 * @return The matching zone, or {@code null} if the location is {@code null} or none matches.
	 */
	public <T extends ZoneType> T getZone(ILocational locational, Class<T> type)
	{
		if (locational == null)
		{
			return null;
		}
		
		return getZone(locational.getX(), locational.getY(), locational.getZ(), type);
	}
	
	/**
	 * Retrieves all zones containing the given point.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @return A {@code List} of zones at the coordinates.
	 */
	public List<ZoneType> getZones(int x, int y)
	{
		final ZoneRegion region = getRegion(x, y);
		if (region == null)
		{
			return Collections.emptyList();
		}
		
		final List<ZoneType> result = new ArrayList<>();
		for (ZoneType zone : region.getZones().values())
		{
			if (zone.isInsideZone(x, y))
			{
				result.add(zone);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves all zones containing the given point.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param z The Z coordinate.
	 * @return A {@code List} of zones at the coordinates.
	 */
	public List<ZoneType> getZones(int x, int y, int z)
	{
		final ZoneRegion region = getRegion(x, y);
		if (region == null)
		{
			return Collections.emptyList();
		}
		
		final List<ZoneType> result = new ArrayList<>();
		for (ZoneType zone : region.getZones().values())
		{
			if (zone.isInsideZone(x, y, z))
			{
				result.add(zone);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the first zone of the given type containing the given point.
	 * @param <T> The zone subtype.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @param z The Z coordinate.
	 * @param type The {@code Class} of the zone type.
	 * @return The matching zone, or {@code null} if none.
	 */
	@SuppressWarnings("unchecked")
	public <T extends ZoneType> T getZone(int x, int y, int z, Class<T> type)
	{
		final ZoneRegion region = getRegion(x, y);
		if (region == null)
		{
			return null;
		}
		
		for (ZoneType zone : region.getZones().values())
		{
			if (zone.isInsideZone(x, y, z) && type.isInstance(zone))
			{
				return (T) zone;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the spawn territory with the given name.
	 * @param name The territory name.
	 * @return The matching {@code SpawnTerritory}, or {@code null} if none.
	 */
	public SpawnTerritory getSpawnTerritory(String name)
	{
		return _spawnTerritories.get(name);
	}
	
	/**
	 * Retrieves all spawn territories containing the given object.
	 * @param object The {@code WorldObject} to check.
	 * @return A {@code List} of containing {@code SpawnTerritory} objects.
	 */
	public List<SpawnTerritory> getSpawnTerritories(WorldObject object)
	{
		final List<SpawnTerritory> result = new ArrayList<>();
		for (SpawnTerritory territory : _spawnTerritories.values())
		{
			if (territory.isInsideZone(object.getX(), object.getY(), object.getZ()))
			{
				result.add(territory);
			}
		}
		
		return result;
	}
	
	/**
	 * Retrieves the {@code OlympiadStadiumZone} containing the given creature.
	 * @param creature The {@code Creature} to locate.
	 * @return The containing {@code OlympiadStadiumZone}, or {@code null} if none or the creature is {@code null}.
	 */
	public OlympiadStadiumZone getOlympiadStadium(Creature creature)
	{
		if (creature == null)
		{
			return null;
		}
		
		for (ZoneType zone : getInstance().getZones(creature.getX(), creature.getY(), creature.getZ()))
		{
			if ((zone instanceof OlympiadStadiumZone) && zone.isCharacterInZone(creature))
			{
				return (OlympiadStadiumZone) zone;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves the debug item list, initializing it on first access.
	 * @return The {@code List} of debug {@code Item} objects.
	 */
	public List<Item> getDebugItems()
	{
		if (_debugItems == null)
		{
			_debugItems = new ArrayList<>();
		}
		
		return _debugItems;
	}
	
	/**
	 * Decays every debug item and clears the debug item list.
	 */
	public synchronized void clearDebugItems()
	{
		if (_debugItems != null)
		{
			for (Item item : _debugItems)
			{
				if (item != null)
				{
					item.decayMe();
				}
			}
			
			_debugItems.clear();
		}
	}
	
	/**
	 * Retrieves the {@code ZoneRegion} for the given coordinates.
	 * @param x The X coordinate.
	 * @param y The Y coordinate.
	 * @return The {@code ZoneRegion}, or {@code null} if outside the map bounds.
	 */
	public ZoneRegion getRegion(int x, int y)
	{
		try
		{
			return _zoneRegions[(x >> SHIFT_BY) + OFFSET_X][(y >> SHIFT_BY) + OFFSET_Y];
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			// LOGGER.warning(getClass().getSimpleName() + ": Incorrect zone region X: " + ((x >> SHIFT_BY) + OFFSET_X) + " Y: " + ((y >> SHIFT_BY) + OFFSET_Y) + " for coordinates x: " + x + " y: " + y);
			return null;
		}
	}
	
	/**
	 * Retrieves the {@code ZoneRegion} at the coordinates of the given location.
	 * @param point The {@code ILocational} to check.
	 * @return The {@code ZoneRegion} at those coordinates.
	 */
	public ZoneRegion getRegion(ILocational point)
	{
		return getRegion(point.getX(), point.getY());
	}
	
	/**
	 * Retrieves the cached settings for the named zone.
	 * @param name The zone name.
	 * @return The {@code AbstractZoneSettings}, or {@code null} if none.
	 */
	public static AbstractZoneSettings getSettings(String name)
	{
		return SETTINGS.get(name);
	}
	
	/**
	 * Retrieves the singleton {@code ZoneManager} instance.
	 * @return The single {@code ZoneManager} instance.
	 */
	public static ZoneManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ZoneManager INSTANCE = new ZoneManager();
	}
}
