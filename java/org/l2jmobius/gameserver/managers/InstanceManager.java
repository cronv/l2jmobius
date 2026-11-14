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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.holders.InstanceReenterTimeHolder;
import org.l2jmobius.gameserver.data.holders.StringStringHolder;
import org.l2jmobius.gameserver.data.xml.DoorData;
import org.l2jmobius.gameserver.data.xml.SpawnData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.npc.MinionHolder;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceReenterType;
import org.l2jmobius.gameserver.entity.instancezone.InstanceRemoveBuffType;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTeleportType;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.entity.instancezone.conditions.Condition;
import org.l2jmobius.gameserver.entity.spawns.SpawnTemplate;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the lifecycle and data of game instances.<br>
 * This class handles the loading, creation, and tracking of {@code Instance} objects.
 * @author evill33t, GodKratos, malyelfik, Mobius
 */
public class InstanceManager implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(InstanceManager.class.getName());
	
	// Database query
	private static final String DELETE_INSTANCE_TIME = "DELETE FROM character_instance_time WHERE charId=? AND instanceId=?";
	
	// Client instance names
	private final Map<Integer, String> _instanceNames = new HashMap<>();
	private final List<StringStringHolder> _instanceTemplateNames = new ArrayList<>(); // Id, Name.
	
	// Instance templates holder
	private final Map<Integer, InstanceTemplate> _instanceTemplates = new ConcurrentHashMap<>();
	
	// Created instance worlds
	private int _currentInstanceId = 0;
	private final Map<Integer, Instance> _instanceWorlds = new ConcurrentHashMap<>();
	
	// Player reenter times
	private final Map<Integer, Map<Integer, Long>> _playerTimes = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code InstanceManager}.<br>
	 * This constructor calls the {@code load()} method to prepare data.
	 */
	protected InstanceManager()
	{
		load();
	}
	
	// --------------------------------------------------------------------
	// Instance data loader
	// --------------------------------------------------------------------
	/**
	 * Loads all necessary data for instances from the configuration files.<br>
	 * This method clears existing data and populates instance names, templates, and player reenter times.
	 */
	@Override
	public void load()
	{
		// Load instance names.
		_instanceNames.clear();
		parseDatapackFile("data/InstanceNames.xml");
		final String simpleName = getClass().getSimpleName();
		LOGGER.info(simpleName + ": Loaded " + _instanceNames.size() + " instance names.");
		
		// Load instance templates.
		_instanceTemplates.clear();
		parseDatapackDirectory("data/instances", true);
		LOGGER.info(simpleName + ": Loaded " + _instanceTemplates.size() + " instance templates.");
		
		// Load player's reenter data.
		_playerTimes.clear();
		restoreInstanceTimes();
		LOGGER.info(simpleName + ": Loaded instance reenter times for " + _playerTimes.size() + " players.");
	}
	
	/**
	 * Parses the XML content of a document to load instance data.<br>
	 * This method iterates through all nodes in the {@code Document}.<br>
	 * It identifies and processes specific tags like "list" and "instance".
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the loaded document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, IXmlReader::isNode, listNode ->
		{
			switch (listNode.getNodeName())
			{
				case "list":
				{
					parseInstanceName(listNode);
					break;
				}
				case "instance":
				{
					parseInstanceTemplate(listNode, file);
					break;
				}
			}
		});
	}
	
	/**
	 * Parses the instance names from an XML node.<br>
	 * It extracts the id and name attributes to populate internal maps.
	 * @param n The {@code Node} containing the instance data.
	 */
	private void parseInstanceName(Node n)
	{
		forEach(n, "instance", instanceNode ->
		{
			final NamedNodeMap attrs = instanceNode.getAttributes();
			final int id = parseInteger(attrs, "id");
			final String name = parseString(attrs, "name");
			_instanceNames.put(id, name);
			_instanceTemplateNames.add(new StringStringHolder(String.valueOf(id), name));
		});
	}
	
	/**
	 * Parses an instance template from an XML node.<br>
	 * This method extracts data such as rates, locations, and spawns to create a new {@code InstanceTemplate}.<br>
	 * It also handles special configurations like doors, buffs, and reenter times.
	 * @param instanceNode The {@code Node} containing the instance template data.
	 * @param file The {@code File} source of the XML document.
	 */
	private void parseInstanceTemplate(Node instanceNode, File file)
	{
		// Parse "instance" node.
		final int id = parseInteger(instanceNode.getAttributes(), "id");
		if (_instanceTemplates.containsKey(id))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Instance template with ID " + id + " already exists");
			return;
		}
		
		final InstanceTemplate template = new InstanceTemplate(new StatSet(parseAttributes(instanceNode)));
		
		// Update name if wasn't provided.
		if (template.getName() == null)
		{
			template.setName(_instanceNames.get(id));
		}
		
		// Parse "instance" node children.
		forEach(instanceNode, IXmlReader::isNode, innerNode ->
		{
			switch (innerNode.getNodeName())
			{
				case "time":
				{
					final NamedNodeMap attrs = innerNode.getAttributes();
					template.setDuration(parseInteger(attrs, "duration", -1));
					template.setEmptyDestroyTime(parseInteger(attrs, "empty", -1));
					template.setEjectTime(parseInteger(attrs, "eject", -1));
					break;
				}
				case "misc":
				{
					final NamedNodeMap attrs = innerNode.getAttributes();
					template.allowPlayerSummon(parseBoolean(attrs, "allowPlayerSummon", false));
					template.setPvP(parseBoolean(attrs, "isPvP", false));
					break;
				}
				case "rates":
				{
					final NamedNodeMap attrs = innerNode.getAttributes();
					template.setExpRate(parseFloat(attrs, "exp", RatesConfig.RATE_INSTANCE_XP));
					template.setSPRate(parseFloat(attrs, "sp", RatesConfig.RATE_INSTANCE_SP));
					template.setExpPartyRate(parseFloat(attrs, "partyExp", RatesConfig.RATE_INSTANCE_PARTY_XP));
					template.setSPPartyRate(parseFloat(attrs, "partySp", RatesConfig.RATE_INSTANCE_PARTY_SP));
					break;
				}
				case "locations":
				{
					forEach(innerNode, IXmlReader::isNode, locationsNode ->
					{
						switch (locationsNode.getNodeName())
						{
							case "enter":
							{
								final InstanceTeleportType type = parseEnum(locationsNode.getAttributes(), InstanceTeleportType.class, "type");
								final List<Location> locations = new ArrayList<>();
								forEach(locationsNode, "location", locationNode -> locations.add(parseLocation(locationNode)));
								template.setEnterLocation(type, locations);
								break;
							}
							case "exit":
							{
								final InstanceTeleportType type = parseEnum(locationsNode.getAttributes(), InstanceTeleportType.class, "type");
								if (type.equals(InstanceTeleportType.ORIGIN))
								{
									template.setExitLocation(type, null);
								}
								else if (type.equals(InstanceTeleportType.TOWN))
								{
									template.setExitLocation(type, null);
								}
								else
								{
									final List<Location> locations = new ArrayList<>();
									forEach(locationsNode, "location", locationNode -> locations.add(parseLocation(locationNode)));
									if (locations.isEmpty())
									{
										LOGGER.warning(getClass().getSimpleName() + ": Missing exit location data for instance " + template.getName() + " (" + template.getId() + ")!");
									}
									else
									{
										template.setExitLocation(type, locations);
									}
								}
								break;
							}
						}
					});
					break;
				}
				case "spawnlist":
				{
					final List<SpawnTemplate> spawns = new ArrayList<>();
					SpawnData.getInstance().parseSpawn(innerNode, file, spawns);
					template.addSpawns(spawns);
					break;
				}
				case "doorlist":
				{
					for (Node doorNode = innerNode.getFirstChild(); doorNode != null; doorNode = doorNode.getNextSibling())
					{
						if (doorNode.getNodeName().equals("door"))
						{
							final StatSet parsedSet = DoorData.getInstance().parseDoor(doorNode);
							final StatSet mergedSet = new StatSet();
							final int doorId = parsedSet.getInt("id");
							final StatSet templateSet = DoorData.getInstance().getDoorTemplate(doorId);
							if (templateSet != null)
							{
								mergedSet.merge(templateSet);
							}
							else
							{
								LOGGER.warning(getClass().getSimpleName() + ": Cannot find template for door: " + doorId + ", instance: " + template.getName() + " (" + template.getId() + ")");
							}
							
							mergedSet.merge(parsedSet);
							
							try
							{
								template.addDoor(doorId, new DoorTemplate(mergedSet));
							}
							catch (Exception e)
							{
								LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Cannot initialize template for door: " + doorId + ", instance: " + template.getName() + " (" + template.getId() + ")", e);
							}
						}
					}
					break;
				}
				case "removeBuffs":
				{
					final InstanceRemoveBuffType removeBuffType = parseEnum(innerNode.getAttributes(), InstanceRemoveBuffType.class, "type");
					final List<Integer> exceptionBuffList = new ArrayList<>();
					for (Node e = innerNode.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("skill"))
						{
							exceptionBuffList.add(parseInteger(e.getAttributes(), "id"));
						}
					}
					
					template.setRemoveBuff(removeBuffType, exceptionBuffList);
					break;
				}
				case "reenter":
				{
					final InstanceReenterType type = parseEnum(innerNode.getAttributes(), InstanceReenterType.class, "apply", InstanceReenterType.NONE);
					final List<InstanceReenterTimeHolder> data = new ArrayList<>();
					for (Node e = innerNode.getFirstChild(); e != null; e = e.getNextSibling())
					{
						if (e.getNodeName().equals("reset"))
						{
							final NamedNodeMap attrs = e.getAttributes();
							final int time = parseInteger(attrs, "time", -1);
							if (time > 0)
							{
								data.add(new InstanceReenterTimeHolder(time));
							}
							else
							{
								final DayOfWeek day = parseEnum(attrs, DayOfWeek.class, "day");
								final int hour = parseInteger(attrs, "hour", -1);
								final int minute = parseInteger(attrs, "minute", -1);
								data.add(new InstanceReenterTimeHolder(day, hour, minute));
							}
						}
					}
					
					template.setReenterData(type, data);
					break;
				}
				case "parameters":
				{
					final Map<String, Object> parameters = new HashMap<>();
					for (Node parameterNode = innerNode.getFirstChild(); parameterNode != null; parameterNode = parameterNode.getNextSibling())
					{
						NamedNodeMap attributes = parameterNode.getAttributes();
						switch (parameterNode.getNodeName().toLowerCase())
						{
							case "param":
							{
								parameters.put(parseString(attributes, "name"), parseString(attributes, "value"));
								break;
							}
							case "skill":
							{
								parameters.put(parseString(attributes, "name"), new SkillHolder(parseInteger(attributes, "id"), parseInteger(attributes, "level")));
								break;
							}
							case "location":
							{
								parameters.put(parseString(attributes, "name"), new Location(parseInteger(attributes, "x"), parseInteger(attributes, "y"), parseInteger(attributes, "z"), parseInteger(attributes, "heading", 0)));
								break;
							}
							case "minions":
							{
								final List<MinionHolder> minions = new ArrayList<>(1);
								for (Node minionNode = parameterNode.getFirstChild(); minionNode != null; minionNode = minionNode.getNextSibling())
								{
									if (minionNode.getNodeName().equalsIgnoreCase("npc"))
									{
										attributes = minionNode.getAttributes();
										minions.add(new MinionHolder(parseInteger(attributes, "id"), parseInteger(attributes, "count"), parseInteger(attributes, "max", 0), parseInteger(attributes, "respawnTime"), parseInteger(attributes, "weightPoint", 0)));
									}
								}
								
								if (!minions.isEmpty())
								{
									parameters.put(parseString(parameterNode.getAttributes(), "name"), minions);
								}
								break;
							}
						}
					}
					
					template.setParameters(parameters);
					break;
				}
				case "conditions":
				{
					final List<Condition> conditions = new ArrayList<>();
					for (Node conditionNode = innerNode.getFirstChild(); conditionNode != null; conditionNode = conditionNode.getNextSibling())
					{
						if (conditionNode.getNodeName().equals("condition"))
						{
							final NamedNodeMap attrs = conditionNode.getAttributes();
							final String type = parseString(attrs, "type");
							final boolean onlyLeader = parseBoolean(attrs, "onlyLeader", false);
							final boolean showMessageAndHtml = parseBoolean(attrs, "showMessageAndHtml", false);
							
							// Load parameters
							StatSet params = null;
							for (Node f = conditionNode.getFirstChild(); f != null; f = f.getNextSibling())
							{
								if (f.getNodeName().equals("param"))
								{
									if (params == null)
									{
										params = new StatSet();
									}
									
									params.set(parseString(f.getAttributes(), "name"), parseString(f.getAttributes(), "value"));
								}
							}
							
							// If none parameters found then set empty StatSet.
							if (params == null)
							{
								params = StatSet.EMPTY_STATSET;
							}
							
							// Now when everything is loaded register condition to template.
							try
							{
								final Class<?> clazz = Class.forName("org.l2jmobius.gameserver.entity.instancezone.conditions.Condition" + type);
								final Constructor<?> constructor = clazz.getConstructor(InstanceTemplate.class, StatSet.class, boolean.class, boolean.class);
								conditions.add((Condition) constructor.newInstance(template, params, onlyLeader, showMessageAndHtml));
							}
							catch (Exception ex)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Unknown condition type " + type + " for instance " + template.getName() + " (" + id + ")!");
							}
						}
					}
					
					template.setConditions(conditions);
					break;
				}
			}
		});
		
		// Save template
		_instanceTemplates.put(id, template);
	}
	
	/**
	 * Converts an XML {@code Node} into a {@code Location} object.<br>
	 * It extracts the x, y, z, and heading attributes from the node.
	 * @param node The XML {@code Node} containing location data.
	 * @return A new {@code Location} instance based on the parsed values.
	 */
	private Location parseLocation(Node node)
	{
		final NamedNodeMap attributes = node.getAttributes();
		final int x = parseInteger(attributes, "x");
		final int y = parseInteger(attributes, "y");
		final int z = parseInteger(attributes, "z");
		final int heading = parseInteger(attributes, "heading", 0);
		return new Location(x, y, z, heading);
	}
	
	/**
	 * Creates a new {@code Instance} object.<br>
	 * This method generates an instance with a unique ID and default settings.
	 * @return A new {@code Instance} object.
	 */
	public Instance createInstance()
	{
		return new Instance(getNewInstanceId(), new InstanceTemplate(StatSet.EMPTY_STATSET), null);
	}
	
	/**
	 * Creates a new {@code Instance} based on the provided template and player.<br>
	 * This method generates a unique ID for the new instance.<br>
	 * It returns {@code null} if the {@code template} is {@code null}.
	 * @param template The {@code InstanceTemplate} to use for creating the instance.
	 * @param player The {@code Player} who will enter the instance.
	 * @return A new {@code Instance} object, or {@code null} if the template is missing.
	 */
	public Instance createInstance(InstanceTemplate template, Player player)
	{
		return (template != null) ? new Instance(getNewInstanceId(), template, player) : null;
	}
	
	/**
	 * Creates a new {@code Instance} based on a template ID.<br>
	 * This method checks if the provided {@code id} exists in the templates.<br>
	 * It returns a new instance associated with the specified {@code Player}.
	 * @param id The unique identifier for the instance template.
	 * @param player The {@code Player} who will enter the instance.
	 * @return A new {@code Instance} object, or {@code null} if the template is missing.
	 */
	public Instance createInstance(int id, Player player)
	{
		if (!_instanceTemplates.containsKey(id))
		{
			LOGGER.warning(getClass().getSimpleName() + ": Missing template for instance with id " + id + "!");
			return null;
		}
		
		return new Instance(getNewInstanceId(), _instanceTemplates.get(id), player);
	}
	
	/**
	 * Retrieves a specific instance based on its unique identifier.
	 * @param instanceId The unique ID of the instance to find.
	 * @return The {@code Instance} associated with the provided ID, or {@code null} if not found.
	 */
	public Instance getInstance(int instanceId)
	{
		return _instanceWorlds.get(instanceId);
	}
	
	/**
	 * Retrieves all currently active instances.
	 * @return A {@code Collection} of {@code Instance} objects.
	 */
	public Collection<Instance> getInstances()
	{
		return _instanceWorlds.values();
	}
	
	/**
	 * Retrieves the {@code Instance} associated with a specific {@code Player}.<br>
	 * It checks if the player is currently inside or allowed to enter based on the {@code isInside} flag.
	 * @param player The {@code Player} to check for an instance.
	 * @param isInside A boolean indicating if we are looking for the instance the player is currently in.
	 * @return The matching {@code Instance} or {@code null} if no match is found.
	 */
	public Instance getPlayerInstance(Player player, boolean isInside)
	{
		for (Instance instance : _instanceWorlds.values())
		{
			if (isInside)
			{
				if (instance.containsPlayer(player))
				{
					return instance;
				}
			}
			else
			{
				if (instance.isAllowed(player))
				{
					return instance;
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Generates a unique identifier for a new instance.<br>
	 * This method ensures the returned {@code int} is not already in use.<br>
	 * It increments the internal counter until a free ID is found.
	 * @return The next available unique instance ID.
	 */
	private synchronized int getNewInstanceId()
	{
		do
		{
			if (_currentInstanceId == Integer.MAX_VALUE)
			{
				_currentInstanceId = 0;
			}
			
			_currentInstanceId++;
		}
		while (_instanceWorlds.containsKey(_currentInstanceId));
		return _currentInstanceId;
	}
	
	/**
	 * Adds a new {@code Instance} to the internal registry.<br>
	 * This method ensures that the instance is stored using its unique ID.
	 * @param instance The {@code Instance} object to register.
	 */
	public void register(Instance instance)
	{
		_instanceWorlds.putIfAbsent(instance.getId(), instance);
	}
	
	/**
	 * Removes an instance from the internal tracking system.<br>
	 * This method deletes the entry associated with the provided {@code instanceId}.<br>
	 * It checks if the ID exists before attempting to remove it.
	 * @param instanceId The unique identifier of the instance to remove.
	 */
	public void unregister(int instanceId)
	{
		if (_instanceWorlds.containsKey(instanceId))
		{
			_instanceWorlds.remove(instanceId);
		}
	}
	
	/**
	 * Retrieves the name of an instance based on its template ID.<br>
	 * This method looks up the name in the internal map.
	 * @param templateId The unique identifier for the instance template.
	 * @return The name of the instance as a {@code String}.
	 */
	public String getInstanceName(int templateId)
	{
		return _instanceNames.get(templateId);
	}
	
	/**
	 * Retrieves the collection of names for instance templates.<br>
	 * This method returns all registered template names stored in the manager.
	 * @return a {@code Collection} of {@code StringStringHolder} objects containing the template names.
	 */
	public Collection<StringStringHolder> getInstanceTemplateNames()
	{
		return _instanceTemplateNames;
	}
	
	/**
	 * Loads the instance reenter penalties from the database.<br>
	 * This method checks if a player's penalty time is still active.<br>
	 * If valid, it calls {@code setReenterPenalty} to apply the restriction.
	 */
	private void restoreInstanceTimes()
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement ps = con.createStatement();
			ResultSet rs = ps.executeQuery("SELECT * FROM character_instance_time ORDER BY charId"))
		{
			while (rs.next())
			{
				// Check if instance penalty passed.
				final long time = rs.getLong("time");
				if (time > System.currentTimeMillis())
				{
					// Load params
					final int charId = rs.getInt("charId");
					final int instanceId = rs.getInt("instanceId");
					
					// Set penalty
					setReenterPenalty(charId, instanceId, time);
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Cannot restore players instance reenter data: ", e);
		}
	}
	
	/**
	 * Retrieves all active instance re-entry penalties for a specific player.<br>
	 * This method also removes expired penalties from the database and the internal cache.
	 * @param player The {@code Player} whose instance times are being retrieved.
	 * @return A {@code Map<Integer, Long>} containing the instance IDs and their corresponding expiration timestamps.
	 */
	public Map<Integer, Long> getAllInstanceTimes(Player player)
	{
		// When player don't have any instance penalty.
		final int objectId = player.getObjectId();
		final Map<Integer, Long> instanceTimes = _playerTimes.get(objectId);
		if ((instanceTimes == null) || instanceTimes.isEmpty())
		{
			return Collections.emptyMap();
		}
		
		// Find passed penalty
		final List<Integer> invalidPenalty = new ArrayList<>(instanceTimes.size());
		for (Entry<Integer, Long> entry : instanceTimes.entrySet())
		{
			if (entry.getValue() <= System.currentTimeMillis())
			{
				invalidPenalty.add(entry.getKey());
			}
		}
		
		// Remove them
		if (!invalidPenalty.isEmpty())
		{
			try (Connection con = DatabaseFactory.getConnection();
				PreparedStatement ps = con.prepareStatement(DELETE_INSTANCE_TIME))
			{
				for (Integer id : invalidPenalty)
				{
					ps.setInt(1, objectId);
					ps.setInt(2, id);
					ps.addBatch();
				}
				
				ps.executeBatch();
				invalidPenalty.forEach(instanceTimes::remove);
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Cannot delete instance character reenter data: ", e);
			}
		}
		
		return instanceTimes;
	}
	
	/**
	 * Sets the re-entry penalty for a specific object and ID.<br>
	 * This method updates the cooldown time in the internal map.
	 * @param objectId The unique identifier for the object.
	 * @param id The specific identifier for the instance or entry.
	 * @param time The amount of time to set as the penalty.
	 */
	public void setReenterPenalty(int objectId, int id, long time)
	{
		_playerTimes.computeIfAbsent(objectId, _ -> new ConcurrentHashMap<>()).put(id, time);
	}
	
	/**
	 * Retrieves the remaining reenter penalty time for a specific instance.<br>
	 * It checks if the player has an active penalty for the given {@code id}.<br>
	 * If the penalty has expired or does not exist, it returns -1.
	 * @param player The {@code Player} object who is checking the time.
	 * @param id The unique identifier of the instance.
	 * @return The remaining time in milliseconds, or -1 if no active penalty exists.
	 */
	public long getInstanceTime(Player player, int id)
	{
		// Check if exists reenter data for player.
		final Map<Integer, Long> playerData = _playerTimes.get(player.getObjectId());
		if ((playerData == null) || !playerData.containsKey(id))
		{
			return -1;
		}
		
		// If reenter time is higher then current, delete it.
		final long time = playerData.get(id);
		if (time <= System.currentTimeMillis())
		{
			deleteInstanceTime(player, id);
			return -1;
		}
		
		return time;
	}
	
	/**
	 * Removes the reenter penalty time for a specific instance.<br>
	 * This method updates both the database and the internal cache.
	 * @param player The {@code Player} who owns the instance data.
	 * @param id The unique identifier of the instance to clear.
	 */
	public void deleteInstanceTime(Player player, int id)
	{
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(DELETE_INSTANCE_TIME))
		{
			final Integer playerObjectId = player.getObjectId();
			ps.setInt(1, playerObjectId);
			ps.setInt(2, id);
			ps.execute();
			final Map<Integer, Long> playerInstances = _playerTimes.get(playerObjectId);
			if (playerInstances != null)
			{
				playerInstances.remove(id);
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not delete character instance reenter data: ", e);
		}
	}
	
	/**
	 * Retrieves a specific instance template from the internal cache.
	 * @param id The unique identifier of the template to find.
	 * @return The {@code InstanceTemplate} associated with the given {@code id}.
	 */
	public InstanceTemplate getInstanceTemplate(int id)
	{
		return _instanceTemplates.get(id);
	}
	
	/**
	 * Retrieves all loaded instance templates.
	 * @return a {@code Collection} of {@code InstanceTemplate} objects.
	 */
	public Collection<InstanceTemplate> getInstanceTemplates()
	{
		return _instanceTemplates.values();
	}
	
	/**
	 * Counts the number of active instances for a specific template.<br>
	 * It iterates through all registered worlds to find matches.
	 * @param templateId The unique identifier for the instance template.
	 * @return The total count of instances matching the provided {@code templateId}.
	 */
	public long getWorldCount(int templateId)
	{
		long count = 0;
		for (Instance i : _instanceWorlds.values())
		{
			if (i.getTemplateId() == templateId)
			{
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Retrieves the singleton instance of {@code InstanceManager}.<br>
	 * This method provides a global access point to the manager.
	 * @return The single {@code InstanceManager} instance.
	 */
	public static InstanceManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final InstanceManager INSTANCE = new InstanceManager();
	}
}
