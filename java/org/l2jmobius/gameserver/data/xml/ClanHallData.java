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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.enums.ClanHallGrade;
import org.l2jmobius.gameserver.entity.clan.enums.ClanHallType;
import org.l2jmobius.gameserver.entity.residences.ClanHall;
import org.l2jmobius.gameserver.entity.residences.ClanHallTeleportHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of {@code ClanHall} data from XML files.<br>
 * It provides the configuration necessary to initialize clan hall properties in the game world.
 * @author St3eT, BazookaRpm
 */
public class ClanHallData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(ClanHallData.class.getName());
	private static final Comparator<ClanHall> RESIDENCE_ID_COMPARATOR = Comparator.comparingInt(ClanHall::getResidenceId);
	
	private final Map<Integer, ClanHall> _clanHalls = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of the {@code ClanHallData} class.<br>
	 * This constructor automatically calls the {@code load()} method to populate data.
	 */
	protected ClanHallData()
	{
		load();
	}
	
	/**
	 * Loads all clan hall data from the configuration files.<br>
	 * This method reads the datapack directory and populates the internal map.<br>
	 * It logs a message confirming how many halls were successfully loaded.
	 */
	@Override
	public void load()
	{
		_clanHalls.clear();
		parseDatapackDirectory("data/residences/clanHalls", true);
		LOGGER.info(getClass().getSimpleName() + ": Successfully loaded " + _clanHalls.size() + " clan halls.");
	}
	
	/**
	 * Parses the provided {@code Document} to load clan hall data.<br>
	 * This method reads XML nodes and populates the internal collection of {@code ClanHall} objects.<br>
	 * It extracts attributes such as IDs, grades, types, and teleport locations from the file structure.
	 * @param document The {@code Document} object containing the XML data to be parsed.
	 * @param file The {@code File} associated with the loaded XML content.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		final List<Door> doors = new ArrayList<>();
		final List<Integer> npcs = new ArrayList<>();
		final List<ClanHallTeleportHolder> teleports = new ArrayList<>();
		final StatSet params = new StatSet();
		for (Node listNode = document.getFirstChild(); listNode != null; listNode = listNode.getNextSibling())
		{
			if ("list".equals(listNode.getNodeName()))
			{
				for (Node clanHallNode = listNode.getFirstChild(); clanHallNode != null; clanHallNode = clanHallNode.getNextSibling())
				{
					if ("clanHall".equals(clanHallNode.getNodeName()))
					{
						final NamedNodeMap chAttrs = clanHallNode.getAttributes();
						params.set("id", parseInteger(chAttrs, "id"));
						params.set("name", parseString(chAttrs, "name", "None"));
						params.set("grade", parseEnum(chAttrs, ClanHallGrade.class, "grade", ClanHallGrade.GRADE_NONE));
						params.set("type", parseEnum(chAttrs, ClanHallType.class, "type", ClanHallType.OTHER));
						for (Node tpNode = clanHallNode.getFirstChild(); tpNode != null; tpNode = tpNode.getNextSibling())
						{
							switch (tpNode.getNodeName())
							{
								case "auction":
								{
									final NamedNodeMap at = tpNode.getAttributes();
									params.set("minBid", parseInteger(at, "minBid"));
									params.set("lease", parseInteger(at, "lease"));
									params.set("deposit", parseInteger(at, "deposit"));
									break;
								}
								case "npcs":
								{
									for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
									{
										if ("npc".equals(npcNode.getNodeName()))
										{
											final NamedNodeMap np = npcNode.getAttributes();
											final int npcId = parseInteger(np, "id");
											npcs.add(npcId);
										}
									}
									
									params.set("npcList", npcs);
									break;
								}
								case "doorlist":
								{
									for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
									{
										if ("door".equals(npcNode.getNodeName()))
										{
											final NamedNodeMap np = npcNode.getAttributes();
											final int doorId = parseInteger(np, "id");
											final Door door = DoorData.getInstance().getDoor(doorId);
											if (door != null)
											{
												doors.add(door);
											}
										}
									}
									
									params.set("doorList", doors);
									break;
								}
								case "teleportList":
								{
									for (Node npcNode = tpNode.getFirstChild(); npcNode != null; npcNode = npcNode.getNextSibling())
									{
										if ("teleport".equals(npcNode.getNodeName()))
										{
											final NamedNodeMap np = npcNode.getAttributes();
											final int npcStringId = parseInteger(np, "npcStringId");
											final int x = parseInteger(np, "x");
											final int y = parseInteger(np, "y");
											final int z = parseInteger(np, "z");
											final int minFunctionLevel = parseInteger(np, "minFunctionLevel");
											final int cost = parseInteger(np, "cost");
											teleports.add(new ClanHallTeleportHolder(npcStringId, x, y, z, minFunctionLevel, cost));
										}
									}
									
									params.set("teleportList", teleports);
									break;
								}
								case "ownerRestartPoint":
								{
									final NamedNodeMap ol = tpNode.getAttributes();
									params.set("owner_loc", new Location(parseInteger(ol, "x"), parseInteger(ol, "y"), parseInteger(ol, "z")));
									break;
								}
								case "banishPoint":
								{
									final NamedNodeMap bl = tpNode.getAttributes();
									params.set("banish_loc", new Location(parseInteger(bl, "x"), parseInteger(bl, "y"), parseInteger(bl, "z")));
									break;
								}
							}
						}
					}
				}
			}
		}
		
		_clanHalls.put(params.getInt("id"), new ClanHall(params));
	}
	
	/**
	 * Retrieves a {@code ClanHall} object based on its unique identifier.<br>
	 * This method searches the internal map for the provided ID.
	 * @param clanHallId The unique integer ID of the hall to find.
	 * @return The corresponding {@code ClanHall} object, or {@code null} if not found.
	 */
	public ClanHall getClanHallById(int clanHallId)
	{
		return _clanHalls.get(clanHallId);
	}
	
	/**
	 * Retrieves all loaded clan halls.<br>
	 * This method returns the collection of {@code ClanHall} objects stored in the data map.
	 * @return A {@code Collection} containing all {@code ClanHall} instances.
	 */
	public Collection<ClanHall> getClanHalls()
	{
		return _clanHalls.values();
	}
	
	/**
	 * Finds a {@code ClanHall} based on a specific NPC ID.<br>
	 * It searches through all loaded halls to find a match.
	 * @param npcId The unique identifier of the NPC to search for.
	 * @return The matching {@code ClanHall} object, or {@code null} if no match is found.
	 */
	public ClanHall getClanHallByNpcId(int npcId)
	{
		for (ClanHall ch : _clanHalls.values())
		{
			if (ch.getNpcs().contains(npcId))
			{
				return ch;
			}
		}
		
		return null;
	}
	
	/**
	 * Finds the {@code ClanHall} belonging to a specific {@code Clan}.<br>
	 * It searches through all loaded halls to find the one owned by the provided clan.
	 * @param clan The {@code Clan} object to search for.
	 * @return The matching {@code ClanHall} or {@code null} if no hall is found.
	 */
	public ClanHall getClanHallByClan(Clan clan)
	{
		for (ClanHall ch : _clanHalls.values())
		{
			if (ch.getOwner() == clan)
			{
				return ch;
			}
		}
		
		return null;
	}
	
	/**
	 * Finds a {@code ClanHall} associated with a specific door.<br>
	 * It searches through all loaded halls to see if they contain the door.
	 * @param doorId The unique identifier of the door to search for.
	 * @return The matching {@code ClanHall} object, or {@code null} if no hall is found.
	 */
	public ClanHall getClanHallByDoorId(int doorId)
	{
		final Door door = DoorData.getInstance().getDoor(doorId);
		for (ClanHall ch : _clanHalls.values())
		{
			if (ch.getDoors().contains(door))
			{
				return ch;
			}
		}
		
		return null;
	}
	
	/**
	 * Retrieves a list of available auction halls.<br>
	 * It filters for {@code ClanHall} objects that have no owner and are of type {@code ClanHallType.AUCTIONABLE}.<br>
	 * The resulting list is sorted by residence ID.
	 * @return A {@code List} of {@code ClanHall} objects that can be auctioned.
	 */
	public List<ClanHall> getFreeAuctionableHall()
	{
		final List<ClanHall> freeAuctionableHalls = new ArrayList<>();
		for (ClanHall ch : _clanHalls.values())
		{
			if ((ch.getType() == ClanHallType.AUCTIONABLE) && (ch.getOwner() == null))
			{
				freeAuctionableHalls.add(ch);
			}
		}
		
		freeAuctionableHalls.sort(RESIDENCE_ID_COMPARATOR);
		return freeAuctionableHalls;
	}
	
	/**
	 * Retrieves the singleton instance of {@code ClanHallData}.<br>
	 * This provides a global access point to the clan hall data.
	 * @return The single instance of {@code ClanHallData}.
	 */
	public static ClanHallData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final ClanHallData INSTANCE = new ClanHallData();
	}
}
