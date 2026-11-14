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
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldRegion;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.templates.DoorTemplate;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class loads and holds information about doors from XML files.<br>
 * It serves as a data container for door properties used by the game server.
 * @author JIV, GodKratos, UnAfraid, Mobius
 */
public class DoorData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(DoorData.class.getName());
	
	private final Map<String, Set<Integer>> _groups = new HashMap<>();
	private final Map<Integer, Door> _doors = new HashMap<>();
	private final Map<Integer, StatSet> _templates = new HashMap<>();
	
	/**
	 * Initializes a new instance of the {@code DoorData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected DoorData()
	{
		load();
	}
	
	/**
	 * Loads door data from the configuration file.<br>
	 * This method clears existing data before parsing the XML file.<br>
	 * It populates the internal maps with new information.
	 */
	@Override
	public void load()
	{
		_doors.clear();
		_groups.clear();
		parseDatapackFile("data/DoorData.xml");
	}
	
	/**
	 * Parses the provided {@code Document} to load door data.<br>
	 * It iterates through all door nodes and spawns them into the system.<br>
	 * Logs the total number of loaded doors upon completion.
	 * @param document The {@code Document} object containing the XML data.
	 * @param file The {@code File} associated with the parsed document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		forEach(document, "list", listNode -> forEach(listNode, "door", doorNode ->
		{
			final StatSet set = parseDoor(doorNode);
			final DoorTemplate template = new DoorTemplate(set);
			final Door door = spawnDoor(template, null);
			final Integer doorId = door.getId();
			_templates.put(doorId, set);
			_doors.put(doorId, door);
		}));
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _doors.size() + " doors.");
	}
	
	/**
	 * Parses a door node from the XML structure into a {@code StatSet}.<br>
	 * It extracts attributes and nested nodes to define door properties.<br>
	 * The method also ensures a default health value is set.
	 * @param doorNode The {@code Node} representing the door data in the XML.
	 * @return A populated {@code StatSet} containing the parsed door information.
	 */
	public StatSet parseDoor(Node doorNode)
	{
		final StatSet params = new StatSet(parseAttributes(doorNode));
		params.set("baseHpMax", 1); // Avoid doors without HP value created dead due to default value 0 in CreatureTemplate.
		
		forEach(doorNode, IXmlReader::isNode, innerDoorNode ->
		{
			final NamedNodeMap attrs = innerDoorNode.getAttributes();
			if (innerDoorNode.getNodeName().equals("nodes"))
			{
				params.set("nodeZ", parseInteger(attrs, "nodeZ"));
				
				final AtomicInteger count = new AtomicInteger();
				forEach(innerDoorNode, IXmlReader::isNode, nodes ->
				{
					final NamedNodeMap nodeAttrs = nodes.getAttributes();
					if ("node".equals(nodes.getNodeName()))
					{
						params.set("nodeX_" + count.get(), parseInteger(nodeAttrs, "x"));
						params.set("nodeY_" + count.getAndIncrement(), parseInteger(nodeAttrs, "y"));
					}
				});
			}
			else if (attrs != null)
			{
				for (int i = 0; i < attrs.getLength(); i++)
				{
					final Node att = attrs.item(i);
					params.set(att.getNodeName(), att.getNodeValue());
				}
			}
		});
		
		// Insert collision data if all necessary nodes are present.
		if (params.contains("nodeX_0") && params.contains("nodeY_0") && params.contains("nodeX_1") && params.contains("nodeY_1"))
		{
			final int height = params.getInt("height", 150); // Default collision height when not provided.
			final int nodeX = params.getInt("nodeX_0");
			final int nodeY = params.getInt("nodeY_0");
			final int posX = params.getInt("nodeX_1");
			final int posY = params.getInt("nodeY_1");
			int collisionRadius = Math.min(Math.abs(nodeX - posX), Math.abs(nodeY - posY)); // Minimum radius 20.
			if (collisionRadius < 20)
			{
				collisionRadius = 20;
			}
			params.set("collision_radius", collisionRadius);
			params.set("collision_height", height);
		}
		return params;
	}
	
	/**
	 * Creates and spawns a new {@code Door} object.<br>
	 * This method initializes the door with its maximum health.<br>
	 * It sets the instance if one is provided.<br>
	 * The door is spawned at the coordinates defined in the {@code DoorTemplate}.
	 * @param template The {@code DoorTemplate} containing the door data and coordinates.
	 * @param instance The {@code Instance} where the door will be placed, or {@code null}.
	 * @return The newly created {@code Door} object.
	 */
	public Door spawnDoor(DoorTemplate template, Instance instance)
	{
		final Door door = new Door(template);
		door.setCurrentHp(door.getMaxHp());
		
		// Set instance world if provided.
		if (instance != null)
		{
			door.setInstance(instance);
		}
		
		// Spawn the door in the world at specified coordinates.
		door.spawnMe(template.getX(), template.getY(), template.getZ());
		
		// Register door's group if available.
		if (template.getGroupName() != null)
		{
			_groups.computeIfAbsent(door.getGroupName(), _ -> new HashSet<>()).add(door.getId());
		}
		
		return door;
	}
	
	/**
	 * Retrieves the {@code StatSet} for a specific door.<br>
	 * This method looks up the template using the provided ID.
	 * @param doorId The unique identifier of the door.
	 * @return The {@code StatSet} associated with the door, or {@code null} if not found.
	 */
	public StatSet getDoorTemplate(int doorId)
	{
		return _templates.get(doorId);
	}
	
	/**
	 * Retrieves a {@code Door} object based on its unique ID.<br>
	 * This method looks up the door in the internal data map.
	 * @param doorId The unique identifier for the door to find.
	 * @return The {@code Door} associated with the provided ID, or {@code null} if not found.
	 */
	public Door getDoor(int doorId)
	{
		return _doors.get(doorId);
	}
	
	/**
	 * Retrieves a set of door IDs belonging to a specific group.<br>
	 * This method looks up the group name in the internal data map.<br>
	 * If the group does not exist, it returns an empty {@code Set}.
	 * @param groupName The name of the group to search for.
	 * @return A {@code Set} of door IDs associated with the provided group name.
	 */
	public Set<Integer> getDoorsByGroup(String groupName)
	{
		return _groups.getOrDefault(groupName, Collections.emptySet());
	}
	
	/**
	 * Retrieves all loaded doors.<br>
	 * This method returns the values from the internal door map.
	 * @return a {@code Collection} of {@code Door} objects.
	 */
	public Collection<Door> getDoors()
	{
		return _doors.values();
	}
	
	/**
	 * Checks if there are any doors between two locations.<br>
	 * This method uses the coordinates from {@code Location} objects to perform the check.<br>
	 * It validates the path within a specific {@code Instance}.
	 * @param start The starting {@code Location}.
	 * @param end The ending {@code Location}.
	 * @param instance The {@code Instance} where the check occurs.
	 * @return {@code true} if doors are found between the locations, {@code false} otherwise.
	 */
	public boolean checkIfDoorsBetween(Location start, Location end, Instance instance)
	{
		return checkIfDoorsBetween(start.getX(), start.getY(), start.getZ(), end.getX(), end.getY(), end.getZ(), instance);
	}
	
	/**
	 * Checks if there are any doors between two sets of coordinates.<br>
	 * This method uses the default face check logic.
	 * @param x The starting X coordinate.
	 * @param y The starting Y coordinate.
	 * @param z The starting Z coordinate.
	 * @param tx The target X coordinate.
	 * @param ty The target Y coordinate.
	 * @param tz The target Z coordinate.
	 * @param instance The {@code Instance} where the check occurs.
	 * @return {@code true} if a door is found between the points, {@code false} otherwise.
	 */
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance)
	{
		return checkIfDoorsBetween(x, y, z, tx, ty, tz, instance, false);
	}
	
	/**
	 * Checks if there are any active doors between two sets of coordinates.<br>
	 * This method calculates line intersections to detect physical obstructions.
	 * @param x The starting X coordinate.
	 * @param y The starting Y coordinate.
	 * @param z The starting Z coordinate.
	 * @param tx The target X coordinate.
	 * @param ty The target Y coordinate.
	 * @param tz The target Z coordinate.
	 * @param instance The {@code Instance} where the check should occur.
	 * @param doubleFaceCheck If {@code true}, it ignores doors that are only touched by one face.
	 * @return {@code true} if a door is blocking the path, {@code false} otherwise.
	 */
	public boolean checkIfDoorsBetween(int x, int y, int z, int tx, int ty, int tz, Instance instance, boolean doubleFaceCheck)
	{
		final Collection<Door> doors;
		if (instance == null)
		{
			final WorldRegion region = World.getRegion(x, y, z);
			if (region != null)
			{
				doors = region.getDoors();
			}
			else
			{
				doors = null;
			}
		}
		else
		{
			doors = instance.getDoors();
		}
		
		if ((doors == null) || doors.isEmpty())
		{
			return false;
		}
		
		for (Door doorInst : doors)
		{
			// Check dead and open.
			if ((instance != doorInst.getInstanceWorld()) || doorInst.isDead() || doorInst.isOpen() || !doorInst.checkCollision() || (doorInst.getX(0) == 0))
			{
				continue;
			}
			
			boolean intersectFace = false;
			for (int i = 0; i < 4; i++)
			{
				final int j = (i + 1) < 4 ? i + 1 : 0;
				
				// Lower part of the multiplier fraction, if it is 0 we avoid an error and also know that the lines are parallel.
				final int denominator = ((ty - y) * (doorInst.getX(i) - doorInst.getX(j))) - ((tx - x) * (doorInst.getY(i) - doorInst.getY(j)));
				if (denominator == 0)
				{
					continue;
				}
				
				// Multipliers to the equations of the lines. If they are lower than 0 or bigger than 1, we know that segments don't intersect.
				final float multiplier1 = (float) (((doorInst.getX(j) - doorInst.getX(i)) * (y - doorInst.getY(i))) - ((doorInst.getY(j) - doorInst.getY(i)) * (x - doorInst.getX(i)))) / denominator;
				final float multiplier2 = (float) (((tx - x) * (y - doorInst.getY(i))) - ((ty - y) * (x - doorInst.getX(i)))) / denominator;
				if ((multiplier1 >= 0) && (multiplier1 <= 1) && (multiplier2 >= 0) && (multiplier2 <= 1))
				{
					final int intersectZ = Math.round(z + (multiplier1 * (tz - z)));
					
					// Now checking if the resulting point is between door's min and max z.
					if ((intersectZ > doorInst.getZMin()) && (intersectZ < doorInst.getZMax()))
					{
						if (!doubleFaceCheck || intersectFace)
						{
							return true;
						}
						
						intersectFace = true;
					}
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Provides access to the singleton instance of {@code DoorData}.<br>
	 * Use this method to get the global data manager for doors.
	 * @return The singleton instance of {@code DoorData}.
	 */
	public static DoorData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final DoorData INSTANCE = new DoorData();
	}
}
