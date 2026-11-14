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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.data.holders.NpcRoutesHolder;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.enums.npc.FollowPosition;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkNode;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkRoute;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Loads the walking route definitions from the {@code data/routes} folder.<br>
 * Holds the routes and the NPC spawn point to route assignments used by the {@code WalkingTaskManager}.
 * @author Mobius
 */
public class RouteData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(RouteData.class.getName());
	
	private final Map<String, WalkRoute> _routes = new ConcurrentHashMap<>();
	private final Map<Integer, NpcRoutesHolder> _routesToAttach = new ConcurrentHashMap<>();
	private final Set<Integer> _targetedNpcIds = ConcurrentHashMap.newKeySet();
	
	protected RouteData()
	{
		load();
	}
	
	@Override
	public void load()
	{
		_routes.clear();
		_routesToAttach.clear();
		_targetedNpcIds.clear();
		parseDatapackDirectory("data/routes", false);
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _routes.size() + " walking routes.");
	}
	
	/**
	 * Additionally loads one or more route XML files into the already loaded routes.<br>
	 * Useful for scripts that ship their own route files outside the {@code data/routes} directory.
	 * @param paths the datapack relative paths of the route XML files to load
	 */
	public void loadRoutes(String... paths)
	{
		for (String path : paths)
		{
			parseDatapackFile(path);
		}
	}
	
	@Override
	public void parseDocument(Document document, File file)
	{
		for (Node d = document.getFirstChild().getFirstChild(); d != null; d = d.getNextSibling())
		{
			if (!d.getNodeName().equals("route"))
			{
				continue;
			}
			
			final NamedNodeMap dAttrs = d.getAttributes();
			final String routeName = parseString(dAttrs, "name");
			final boolean repeat = parseBoolean(dAttrs, "repeat");
			final String repeatStyle = dAttrs.getNamedItem("repeatStyle").getNodeValue().toLowerCase();
			final byte repeatType;
			switch (repeatStyle)
			{
				case "back":
				{
					repeatType = WalkRoute.REPEAT_GO_BACK;
					break;
				}
				case "cycle":
				{
					repeatType = WalkRoute.REPEAT_GO_FIRST;
					break;
				}
				case "conveyor":
				{
					repeatType = WalkRoute.REPEAT_TELE_FIRST;
					break;
				}
				case "random":
				{
					repeatType = WalkRoute.REPEAT_RANDOM;
					break;
				}
				default:
				{
					repeatType = WalkRoute.NO_REPEAT;
					break;
				}
			}
			
			int leaderNpcId = 0;
			int rHandId = 0;
			int lHandId = 0;
			final List<FollowerHolder> followers = new ArrayList<>(1);
			final List<WalkNode> list = new ArrayList<>();
			for (Node r = d.getFirstChild(); r != null; r = r.getNextSibling())
			{
				final String rNodeName = r.getNodeName();
				if (rNodeName.equals("point"))
				{
					final NamedNodeMap attrs = r.getAttributes();
					final int x = parseInteger(attrs, "X");
					final int y = parseInteger(attrs, "Y");
					final int z = parseInteger(attrs, "Z");
					final int delay = parseInteger(attrs, "delay");
					final boolean run = parseBoolean(attrs, "run");
					NpcStringId npcString = null;
					String chatString = null;
					Node node = attrs.getNamedItem("string");
					if (node != null)
					{
						chatString = node.getNodeValue();
					}
					else
					{
						node = attrs.getNamedItem("npcStringId");
						if (node != null)
						{
							final String npcStringIdValue = node.getNodeValue();
							npcString = NpcStringId.getNpcStringId(Integer.parseInt(npcStringIdValue));
							if (npcString == null)
							{
								LOGGER.warning(getClass().getSimpleName() + ": Unknown npcStringId '" + npcStringIdValue + "' for route '" + routeName + "'");
								continue;
							}
						}
					}
					
					list.add(new WalkNode(x, y, z, delay, run, npcString, chatString, parseInteger(attrs, "talkerNpcId", 0)));
				}
				else if (rNodeName.equals("follower"))
				{
					final NamedNodeMap attrs = r.getAttributes();
					followers.add(new FollowerHolder(parseInteger(attrs, "id"), parseEnum(attrs, FollowPosition.class, "position", FollowPosition.BACK), parseInteger(attrs, "distance", 40), parseInteger(attrs, "rhand", 0), parseInteger(attrs, "lhand", 0)));
				}
				else if (rNodeName.equals("target"))
				{
					final NamedNodeMap attrs = r.getAttributes();
					try
					{
						final int npcId = Integer.parseInt(attrs.getNamedItem("id").getNodeValue());
						final int x = Integer.parseInt(attrs.getNamedItem("spawnX").getNodeValue());
						final int y = Integer.parseInt(attrs.getNamedItem("spawnY").getNodeValue());
						final int z = Integer.parseInt(attrs.getNamedItem("spawnZ").getNodeValue());
						if (NpcData.getInstance().getTemplate(npcId) != null)
						{
							NpcRoutesHolder holder = _routesToAttach.get(npcId);
							if (holder == null)
							{
								holder = new NpcRoutesHolder();
								_routesToAttach.put(npcId, holder);
							}
							holder.addRoute(routeName, new Location(x, y, z));
							_targetedNpcIds.add(npcId);
							if (leaderNpcId == 0)
							{
								leaderNpcId = npcId;
								rHandId = parseInteger(attrs, "rhand", 0);
								lHandId = parseInteger(attrs, "lhand", 0);
							}
						}
					}
					catch (Exception e)
					{
						LOGGER.warning(getClass().getSimpleName() + ": Error in target definition for route '" + routeName + "'");
					}
				}
			}
			
			if (list.isEmpty())
			{
				LOGGER.warning(getClass().getSimpleName() + ": Route '" + routeName + "' has no points.");
				continue;
			}
			
			final WalkRoute route = new WalkRoute(routeName, list, repeat, repeatType, rHandId, lHandId);
			for (FollowerHolder follower : followers)
			{
				final int followerNpcId = follower.getNpcId();
				if (leaderNpcId == 0)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Route '" + routeName + "' defines follower " + followerNpcId + " but has no target NPC.");
					continue;
				}
				
				if (NpcData.getInstance().getTemplate(followerNpcId) == null)
				{
					LOGGER.warning(getClass().getSimpleName() + ": Route '" + routeName + "' follower NPC " + followerNpcId + " does not exist.");
					continue;
				}
				
				route.addFollower(new WalkRoute(routeName + "_follower_" + followerNpcId, followerNpcId, leaderNpcId, follower.getPosition(), follower.getDistance(), follower.getRHandId(), follower.getLHandId()));
				_targetedNpcIds.add(followerNpcId);
			}
			
			_routes.put(routeName, route);
		}
	}
	
	/**
	 * Retrieves a walking route by its unique name.
	 * @param route The unique name of the route to find.
	 * @return The {@code WalkRoute} associated with the given name, or {@code null} if not found.
	 */
	public WalkRoute getRoute(String route)
	{
		return _routes.get(route);
	}
	
	/**
	 * Retrieves all loaded walking routes.
	 * @return a {@code Collection} of every {@code WalkRoute}.
	 */
	public Collection<WalkRoute> getAllRoutes()
	{
		return _routes.values();
	}
	
	/**
	 * Retrieves the route assigned to the spawn point of an {@code Npc}.
	 * @param npc The {@code Npc} to check.
	 * @return The route name as a {@code String}, or an empty string if none is assigned.
	 */
	public String getRouteName(Npc npc)
	{
		final NpcRoutesHolder holder = _routesToAttach.get(npc.getId());
		return holder != null ? holder.getRouteName(npc) : "";
	}
	
	/**
	 * Checks if the given NPC id has at least one route assigned in RouteData.xml.
	 * @param npcId The NPC id to check.
	 * @return {@code true} if the NPC id is targeted by a route, {@code false} otherwise.
	 */
	public boolean isTargeted(int npcId)
	{
		return _targetedNpcIds.contains(npcId);
	}
	
	/**
	 * Temporary holder for a follower definition parsed before the route target is known.
	 */
	private static class FollowerHolder
	{
		private final int _npcId;
		private final FollowPosition _position;
		private final int _distance;
		private final int _rHandId;
		private final int _lHandId;
		
		protected FollowerHolder(int npcId, FollowPosition position, int distance, int rHandId, int lHandId)
		{
			_npcId = npcId;
			_position = position;
			_distance = distance;
			_rHandId = rHandId;
			_lHandId = lHandId;
		}
		
		public int getNpcId()
		{
			return _npcId;
		}
		
		public FollowPosition getPosition()
		{
			return _position;
		}
		
		public int getDistance()
		{
			return _distance;
		}
		
		public int getRHandId()
		{
			return _rHandId;
		}
		
		public int getLHandId()
		{
			return _lHandId;
		}
	}
	
	public static RouteData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final RouteData INSTANCE = new RouteData();
	}
}
