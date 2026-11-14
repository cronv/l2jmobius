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
package custom.FakePlayers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.xml.RouteData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.enums.npc.FollowPosition;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkNode;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkRoute;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;

/**
 * Spawns a random fake player at the first point of every custom fake player route (name starting with {@code FPC_}) and starts it walking that route.<br>
 * If the route name contains "Strider" the fake player rides a strider.
 * @author Mobius
 */
public class GludioFakePlayers extends Script
{
	// Only routes with this name prefix get a fake player, so datapack routes are left untouched.
	private static final String ROUTE_PREFIX = "FPC_";
	private static final String STRIDER_KEYWORD = "Strider";
	private static final String FOLLOWER_KEYWORD = "Follower";
	private static final int FOLLOW_DISTANCE = 40;
	// Fake players respawn this many seconds after death, like a player returning to the game.
	private static final int RESPAWN_DELAY = 300;
	// Remembers the route of every spawned walking fake player, so it resumes walking after a respawn.
	private static final Map<Integer, String> ROUTE_BY_NPC = new HashMap<>();
	
	// Fixed locations where a standing fake player is spawned. More can be added here.
	private static final Location[] SPAWN_LOCATIONS =
	{
		new Location(-14480, 123219, -3151, 10294),
		new Location(-14451, 123236, -3151, 36899),
		new Location(-14172, 121720, -3015, 14461),
		new Location(-14179, 121811, -3015, 49467),
		new Location(-14234, 121755, -3015, 59844),
		new Location(-14983, 122086, -3102, 33380),
		new Location(-15036, 122133, -3109, 36285)
	};
	
	// Sitting Dwarf shop fake players: spawn location to private store sell message.
	private static final Map<Location, String> SHOP_LOCATIONS = new HashMap<>();
	static
	{
		SHOP_LOCATIONS.put(new Location(-14146, 124098, -3145, 31855), "WTS spoil mats, cheap!");
		SHOP_LOCATIONS.put(new Location(-14140, 124024, -3145, 31613), "Selling all crystals, fast trade");
		SHOP_LOCATIONS.put(new Location(-14759, 123163, -3144, 945), "WTS soulshots, best price");
	}
	
	private GludioFakePlayers()
	{
		if (!FakePlayersConfig.FAKE_PLAYERS_ENABLED)
		{
			return;
		}
		
		RouteData.getInstance().loadRoutes("data/scripts/custom/FakePlayers/GludioRoutes.xml");
		
		for (WalkRoute route : RouteData.getInstance().getAllRoutes())
		{
			final String routeName = route.getName();
			if (!routeName.startsWith(ROUTE_PREFIX))
			{
				continue;
			}
			
			final List<WalkNode> nodes = route.getNodeList();
			if (nodes.isEmpty())
			{
				continue;
			}
			
			final WalkNode first = nodes.get(0);
			final int npcId = RandomFakePlayerGenerator.generate(routeName.contains(STRIDER_KEYWORD));
			
			// A follower fake player that walks to the right of the leader.
			if (routeName.contains(FOLLOWER_KEYWORD))
			{
				route.addFollower(new WalkRoute(routeName + "_follower", RandomFakePlayerGenerator.generate(), npcId, FollowPosition.RIGHT, FOLLOW_DISTANCE, 0, 0));
			}
			
			// Remember the route and register the id so onSpawn starts the walk, both on the first spawn and after every respawn.
			ROUTE_BY_NPC.put(npcId, routeName);
			addSpawnId(npcId);
			enableRespawn(addSpawn(npcId, first.getX(), first.getY(), first.getZ(), 0, false, 0));
		}
		
		// Standing fake players at fixed locations.
		for (Location location : SPAWN_LOCATIONS)
		{
			enableRespawn(addSpawn(RandomFakePlayerGenerator.generate(), location, false, 0));
		}
		
		// Dwarf shop fake players sitting in a private sell store.
		for (Entry<Location, String> shop : SHOP_LOCATIONS.entrySet())
		{
			enableRespawn(addSpawn(RandomFakePlayerGenerator.generateShop(shop.getValue()), shop.getKey(), false, 0));
		}
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		// Route fake players (including those that just respawned) resume walking their assigned route.
		final String routeName = ROUTE_BY_NPC.get(npc.getId());
		if (routeName != null)
		{
			WalkingTaskManager.getInstance().startMoving(npc, routeName);
		}
	}
	
	/**
	 * Makes a spawned fake player respawn {@link #RESPAWN_DELAY} seconds after it dies.
	 * @param npc the freshly spawned fake player, or {@code null} if the spawn failed
	 */
	private static void enableRespawn(Npc npc)
	{
		if (npc == null)
		{
			return;
		}
		
		final Spawn spawn = npc.getSpawn();
		if (spawn != null)
		{
			spawn.setRespawnDelay(RESPAWN_DELAY);
			spawn.startRespawn();
		}
	}
	
	public static void main(String[] args)
	{
		new GludioFakePlayers();
	}
}
