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
package org.l2jmobius.gameserver.taskmanagers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.data.xml.RouteData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkInfo;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkNode;
import org.l2jmobius.gameserver.entity.actor.holders.npc.WalkRoute;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveNodeArrived;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcMoveRouteFinished;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Moves NPCs along the routes defined in {@code data/RouteData.xml}.<br>
 * A single pulse task drives all registered walkers, there are no per NPC scheduled tasks.<br>
 * Node arrivals are event driven through {@code CreatureAI}, the pulse only handles node delays, combat resume, follow targets, minions and cleanup.
 * @author Mobius
 */
public class WalkingTaskManager
{
	private static final Logger LOGGER = Logger.getLogger(WalkingTaskManager.class.getName());
	
	private static final int PULSE_INTERVAL = 1000;
	private static final int START_RADIUS = 3000;
	private static final int ARRIVE_RADIUS = 10;
	private static final int MAX_MOVE_ATTEMPTS = 5;
	private static final int FOLLOW_STAND_RANGE = 50;
	private static final int FOLLOWER_WAIT_RANGE = 150;
	private static final int FOLLOWER_PAUSE_RANGE = 300;
	private static final int MAX_FOLLOWER_WAIT = 30;
	private static final int MINION_FOLLOW_RANGE = 200;
	
	private final Map<Integer, WalkInfo> _activeRoutes = new ConcurrentHashMap<>();
	
	protected WalkingTaskManager()
	{
		ThreadPool.scheduleAtFixedRate(this::pulse, PULSE_INTERVAL, PULSE_INTERVAL);
	}
	
	/**
	 * Checks if the given {@code Npc} is currently performing a walk route and has not been stopped or suspended.
	 * @param npc The {@code Npc} to check.
	 * @return {@code true} if the NPC is actively walking, {@code false} otherwise.
	 */
	public boolean isOnWalk(Npc npc)
	{
		final Monster monster = npc.isMonster() ? npc.asMonster().getLeader() == null ? npc.asMonster() : npc.asMonster().getLeader() : null;
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return false;
		}
		
		final WalkInfo walk = _activeRoutes.get(monster != null ? monster.getObjectId() : npc.getObjectId());
		return !walk.isStoppedByAttack() && !walk.isSuspended();
	}
	
	/**
	 * Checks if a specific {@code Npc} id has a route assigned in RouteData.xml.
	 * @param npc The {@code Npc} to check.
	 * @return {@code true} if the NPC is targeted by a route, otherwise {@code false}.
	 */
	public boolean isTargeted(Npc npc)
	{
		return RouteData.getInstance().isTargeted(npc.getId());
	}
	
	/**
	 * Checks if the {@code Npc} is currently registered as an active walker.
	 * @param npc The {@code Npc} to check.
	 * @return {@code true} if the {@code Npc} is registered, {@code false} otherwise.
	 */
	private boolean isRegistered(Npc npc)
	{
		return _activeRoutes.containsKey(npc.getObjectId());
	}
	
	/**
	 * Retrieves the name of the route currently assigned to an {@code Npc}.
	 * @param npc The {@code Npc} to check for an active route.
	 * @return The name of the route as a {@code String}, or an empty string if none exists.
	 */
	public String getRouteName(Npc npc)
	{
		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		return walk != null ? walk.getRoute().getName() : "";
	}
	
	/**
	 * Starts or resumes a walking route for an {@code Npc}.<br>
	 * Registers the NPC as an active walker and issues the first movement when its AI is free, otherwise the pulse task picks it up.<br>
	 * Calling this with the route the NPC is already on resumes a stopped or suspended walk.
	 * @param npc The {@code Npc} entity that will perform the movement.
	 * @param routeName The unique name of the route to be followed.
	 */
	public void startMoving(Npc npc, String routeName)
	{
		if ((npc == null) || npc.isDead())
		{
			return;
		}
		
		final WalkRoute route = RouteData.getInstance().getRoute(routeName);
		if (route == null)
		{
			return;
		}
		
		final Integer npcObjectId = npc.getObjectId();
		final WalkInfo activeWalk = _activeRoutes.get(npcObjectId);
		if (activeWalk != null)
		{
			if (activeWalk.getRoute().getName().equals(routeName))
			{
				// Resume the current route.
				activeWalk.setSuspended(false);
				activeWalk.setStoppedByAttack(false);
				if (!route.isFollowRoute() && !activeWalk.isWaiting())
				{
					final Intention intention = npc.getAI().getIntention();
					if ((intention == Intention.ACTIVE) || (intention == Intention.IDLE))
					{
						moveToCurrentNode(activeWalk);
					}
				}
				return;
			}
			
			// Switching to another route restarts the walk.
			cancelMoving(npc);
		}
		
		final WalkInfo walk = new WalkInfo(npc, route);
		if (!route.isFollowRoute())
		{
			WalkNode node = walk.getCurrentNode();
			
			// Adjust next waypoint, if NPC spawns at first waypoint.
			if ((npc.getX() == node.getX()) && (npc.getY() == node.getY()))
			{
				walk.advance();
				node = walk.getCurrentNode();
			}
			
			if (!npc.isInsideRadius3D(node, START_RADIUS))
			{
				LOGGER.warning(getClass().getSimpleName() + ": Route '" + routeName + "': NPC (id=" + npc.getId() + ", x=" + npc.getX() + ", y=" + npc.getY() + ", z=" + npc.getZ() + ") is too far from starting point (node x=" + node.getX() + ", y=" + node.getY() + ", z=" + node.getZ() + ", range=" + npc.calculateDistance3D(node) + "). Teleporting to proper location.");
				npc.teleToLocation(node);
			}
		}
		
		npc.setWalker();
		applyHandItems(npc, route);
		_activeRoutes.put(npcObjectId, walk);
		spawnFollowers(npc, walk);
		
		if (!route.isFollowRoute())
		{
			final Intention intention = npc.getAI().getIntention();
			if ((intention == Intention.ACTIVE) || (intention == Intention.IDLE))
			{
				moveToCurrentNode(walk);
			}
		}
	}
	
	/**
	 * Stops the current movement for a specific {@code Npc} and removes it from the active walkers.
	 * @param npc The {@code Npc} whose movement should be cancelled.
	 */
	public void cancelMoving(Npc npc)
	{
		final WalkInfo walk = _activeRoutes.remove(npc.getObjectId());
		if (walk == null)
		{
			return;
		}
		
		// Despawn any followers spawned for this walker.
		for (Npc follower : walk.getFollowers())
		{
			_activeRoutes.remove(follower.getObjectId());
			follower.deleteMe();
		}
	}
	
	/**
	 * Resumes the movement of a specific {@code Npc} after a suspension.
	 * @param npc The {@code Npc} to resume moving.
	 */
	public void resumeMoving(Npc npc)
	{
		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		if (walk != null)
		{
			walk.setSuspended(false);
			walk.setStoppedByAttack(false);
			startMoving(npc, walk.getRoute().getName());
		}
	}
	
	/**
	 * Stops the movement of a specific {@code Npc} without removing it from the active walkers.
	 * @param npc The {@code Npc} to stop moving.
	 * @param suspend Set to {@code true} if the movement is paused until {@code resumeMoving} is called.
	 * @param stoppedByAttack Set to {@code true} if an attack caused the stop, movement resumes when the AI is free again.
	 */
	public void stopMoving(Npc npc, boolean suspend, boolean stoppedByAttack)
	{
		final Monster monster = npc.isMonster() ? npc.asMonster().getLeader() == null ? npc.asMonster() : npc.asMonster().getLeader() : null;
		if (((monster != null) && !isRegistered(monster)) || !isRegistered(npc))
		{
			return;
		}
		
		final WalkInfo walk = _activeRoutes.get(monster != null ? monster.getObjectId() : npc.getObjectId());
		walk.setSuspended(suspend);
		walk.setStoppedByAttack(stoppedByAttack);
		
		if (monster != null)
		{
			monster.stopMove(null);
			monster.getAI().setIntentionActive();
		}
		else
		{
			npc.stopMove(null);
			npc.getAI().setIntentionActive();
		}
	}
	
	/**
	 * This method is called by {@code CreatureAI} when an {@code Npc} finishes a movement.<br>
	 * It broadcasts node chat, advances the route cursor and either moves on directly or stores the node delay deadline for the pulse task.
	 * @param npc The {@code Npc} that has arrived at a node.
	 */
	public void onArrived(Npc npc)
	{
		final WalkInfo walk = _activeRoutes.get(npc.getObjectId());
		if ((walk == null) || walk.getRoute().isFollowRoute())
		{
			return;
		}
		
		// Notify quest.
		if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_NODE_ARRIVED, npc))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnNpcMoveNodeArrived(npc), npc);
		}
		
		synchronized (walk)
		{
			if (walk.isSuspended() || walk.isWaiting())
			{
				return;
			}
			
			final WalkNode node = walk.getCurrentNode();
			if (!npc.isInsideRadius2D(node, ARRIVE_RADIUS))
			{
				return;
			}
			
			walk.resetMoveAttempts();
			broadcastNodeChat(npc, walk, node);
			npc.broadcastInfo();
			continueWalk(npc, walk, node);
		}
	}
	
	/**
	 * Advances the route cursor of an arrived walker and either moves on directly or stores the node delay deadline for the pulse task.
	 * @param npc The {@code Npc} that finished the current node.
	 * @param walk The {@code WalkInfo} of the walker.
	 * @param node The arrived {@code WalkNode} providing the wait delay.
	 */
	private void continueWalk(Npc npc, WalkInfo walk, WalkNode node)
	{
		if (walk.advance()) // End of the route cycle reached.
		{
			if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_ROUTE_FINISHED, npc))
			{
				EventDispatcher.getInstance().notifyEventAsync(new OnNpcMoveRouteFinished(npc), npc);
			}
			
			if (!walk.getRoute().repeatWalk())
			{
				cancelMoving(npc);
				return;
			}
		}
		
		// Wait for the followers to adjust their position and distance for the next point before moving on.
		if (!walk.getFollowers().isEmpty())
		{
			walk.setWaitingForFollowers(true);
			return;
		}
		
		final int delay = node.getDelay();
		if (delay > 0)
		{
			walk.setWaitUntil(System.currentTimeMillis() + (delay * 1000L));
		}
		else
		{
			moveToCurrentNode(walk);
		}
	}
	
	/**
	 * Checks if all living followers of a walker are close enough to it.
	 * @param walk The {@code WalkInfo} of the route leader.
	 * @param range The maximum distance a follower may be away.
	 * @return {@code true} if there are no followers left to wait for, {@code false} otherwise.
	 */
	private boolean areFollowersNear(WalkInfo walk, int range)
	{
		for (Npc follower : walk.getFollowers())
		{
			// Do not wait forever for missing followers.
			if (follower.isDead() || !follower.isSpawned())
			{
				continue;
			}
			
			if (follower.calculateDistance2D(walk.getNpc()) > range)
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Handles the logic when an {@code Npc} dies, cancelling any active movement.
	 * @param npc The {@code Npc} instance that has died.
	 */
	public void onDeath(Npc npc)
	{
		cancelMoving(npc);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns, starting its assigned route if one exists.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	public void onSpawn(Npc npc)
	{
		final String routeName = RouteData.getInstance().getRouteName(npc);
		if (!routeName.isEmpty())
		{
			startMoving(npc, routeName);
		}
	}
	
	/**
	 * Single tick for all active walkers.<br>
	 * Handles node delay expiry, resume after combat, follow target movement and cleanup of despawned walkers.
	 */
	private void pulse()
	{
		if (_activeRoutes.isEmpty())
		{
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		for (WalkInfo walk : _activeRoutes.values())
		{
			try
			{
				tick(walk, currentTime);
			}
			catch (Exception e)
			{
				LOGGER.warning(getClass().getSimpleName() + ": Error ticking " + walk + ": " + e.getMessage());
			}
		}
	}
	
	private void tick(WalkInfo walk, long currentTime)
	{
		final Npc npc = walk.getNpc();
		if (!npc.isSpawned() || npc.isDead())
		{
			// Walker left the world without cancelMoving, clean up.
			cancelMoving(npc);
			return;
		}
		
		if (walk.isSuspended())
		{
			return;
		}
		
		if (walk.getRoute().isFollowRoute())
		{
			tickFollow(walk);
			return;
		}
		
		// Respawn dead or deleted followers.
		maintainFollowers(npc, walk);
		
		// Keep the minions of a walking monster with it.
		if (npc.isMonster())
		{
			moveMinions(npc.asMonster());
		}
		
		synchronized (walk)
		{
			// Stand at the point until the followers adjusted their position and distance, but never wait forever for a stuck follower.
			if (walk.isWaitingForFollowers())
			{
				if (areFollowersInFormation(walk) || (walk.incrementFollowerWaitTicks() > MAX_FOLLOWER_WAIT))
				{
					walk.resetFollowerWaitTicks();
					walk.setWaitingForFollowers(false);
					
					final int delay = walk.getPreviousNode().getDelay();
					if (delay > 0)
					{
						walk.setWaitUntil(System.currentTimeMillis() + (delay * 1000L));
					}
					else
					{
						moveToCurrentNode(walk);
					}
				}
				return;
			}
			
			if (walk.isWaiting())
			{
				if (currentTime >= walk.getWaitUntil())
				{
					walk.setWaitUntil(0);
					moveToCurrentNode(walk);
				}
				return;
			}
			
			// Resume movement when the AI is free again (after combat, a failed move or a busy spawn).
			final Intention intention = npc.getAI().getIntention();
			if ((intention == Intention.ACTIVE) || (intention == Intention.IDLE))
			{
				// Hold the resume while the followers are still catching up, but never wait forever.
				if (!areFollowersNear(walk, FOLLOWER_WAIT_RANGE) && (walk.incrementFollowerWaitTicks() <= MAX_FOLLOWER_WAIT))
				{
					return;
				}
				
				walk.resetFollowerWaitTicks();
				walk.setStoppedByAttack(false);
				
				// A node that repeatedly fails to be reached exactly is skipped to prevent jittering in place.
				if (walk.incrementMoveAttempts() > MAX_MOVE_ATTEMPTS)
				{
					walk.resetMoveAttempts();
					if (walk.advance()) // End of the route cycle reached.
					{
						if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_MOVE_ROUTE_FINISHED, npc))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnNpcMoveRouteFinished(npc), npc);
						}
						
						if (!walk.getRoute().repeatWalk())
						{
							cancelMoving(npc);
							return;
						}
					}
				}
				
				moveToCurrentNode(walk);
			}
			else if ((intention == Intention.MOVE_TO) && !areFollowersNear(walk, FOLLOWER_PAUSE_RANGE))
			{
				// Pause mid-leg until the followers catch up.
				npc.stopMove(null);
				npc.getAI().setIntentionActive();
			}
		}
	}
	
	/**
	 * Moves a follower to the point its leader currently walks to, shifted by the configured offset.<br>
	 * A new move order is only issued when the leader destination changes or the follower was interrupted.
	 * @param walk The {@code WalkInfo} of the follower.
	 */
	private void tickFollow(WalkInfo walk)
	{
		final Npc npc = walk.getNpc();
		final WalkRoute route = walk.getRoute();
		
		Npc leader = walk.getFollowTarget();
		if ((leader == null) || leader.isDead() || !leader.isSpawned())
		{
			walk.setFollowTarget(null);
			World.forEachVisibleObject(npc, Npc.class, nearby ->
			{
				if ((nearby.getId() == route.getLeaderNpcId()) && !nearby.isDead() && (walk.getFollowTarget() == null))
				{
					walk.setFollowTarget(nearby);
				}
			});
			
			leader = walk.getFollowTarget();
			if (leader == null)
			{
				return;
			}
		}
		
		final WalkInfo leaderWalk = _activeRoutes.get(leader.getObjectId());
		if ((leaderWalk == null) || leaderWalk.getRoute().isFollowRoute())
		{
			return;
		}
		
		// Go to the position the leader walks to, shifted to the configured follow position.
		final WalkNode node = leaderWalk.getCurrentNode();
		final int nodeX = node.getX();
		final int nodeY = node.getY();
		final int followDistance = route.getFollowDistance();
		final double legAngle = Math.atan2(nodeY - leader.getY(), nodeX - leader.getX());
		final double positionAngle = legAngle + Math.toRadians(route.getFollowPosition().getAngle());
		
		// While the leader waits at a point, take position next to it for the next direction.
		if (leaderWalk.isWaitingForFollowers())
		{
			final int turnX = leader.getX() + (int) (Math.cos(positionAngle) * followDistance);
			final int turnY = leader.getY() + (int) (Math.sin(positionAngle) * followDistance);
			if (npc.calculateDistance2D(turnX, turnY, npc.getZ()) > FOLLOW_STAND_RANGE)
			{
				walk.setLastFollowDestination(turnX, turnY);
				npc.getAI().moveTo(new Location(turnX, turnY, leader.getZ()));
			}
			else if (npc.isMoving())
			{
				npc.stopMove(null);
			}
			return;
		}
		
		final int x = nodeX + (int) (Math.cos(positionAngle) * followDistance);
		final int y = nodeY + (int) (Math.sin(positionAngle) * followDistance);
		final int z = node.getZ();
		
		// Never move farther along the path than the leader, hold position until it comes closer.
		if ((npc.calculateDistance2D(node) + followDistance) < leader.calculateDistance2D(node))
		{
			if (npc.isMoving())
			{
				npc.stopMove(null);
			}
			return;
		}
		
		// Run to catch up when left behind more than 1.5 times the follow distance, walk again once caught up.
		final double leaderDistance = npc.calculateDistance2D(leader);
		if (leaderDistance > (followDistance * 1.5))
		{
			if (!npc.isRunning())
			{
				npc.setRunning();
			}
		}
		else if (node.runToLocation())
		{
			if (!npc.isRunning())
			{
				npc.setRunning();
			}
		}
		else if (npc.isRunning())
		{
			npc.setWalking();
		}
		
		// Only issue a new move order when the leader destination changed, the follower was interrupted or it stands too far behind.
		if ((nodeX == walk.getLastFollowX()) && (nodeY == walk.getLastFollowY()) && (npc.isMoving() || ((npc.calculateDistance2D(x, y, z) < FOLLOW_STAND_RANGE) && (leaderDistance < (followDistance * 2)))))
		{
			return;
		}
		
		walk.setLastFollowDestination(nodeX, nodeY);
		
		npc.getAI().moveTo(new Location(x, y, z));
	}
	
	/**
	 * Moves the minions of a walking monster after it when they stand too far behind.<br>
	 * A minion follows its leader through its own AI only inside an active region, so a route crossing empty regions would leave the minions standing where the region went to sleep.
	 * @param leader The walking {@code Monster}.
	 */
	private void moveMinions(Monster leader)
	{
		if (!leader.hasMinions())
		{
			return;
		}
		
		for (Monster minion : leader.getMinionList().getSpawnedMinions())
		{
			if (minion.isDead() || minion.isMoving() || minion.isInCombat() || minion.isMovementDisabled() || (minion.calculateDistance2D(leader) <= MINION_FOLLOW_RANGE))
			{
				continue;
			}
			
			if (leader.isRunning())
			{
				minion.setRunning();
			}
			else
			{
				minion.setWalking();
			}
			
			// A random spot next to the leader, so the minions do not pile up on one point.
			final double angle = Rnd.nextDouble() * 2 * Math.PI;
			final int distance = Rnd.get(FOLLOW_STAND_RANGE, MINION_FOLLOW_RANGE / 2);
			minion.getAI().moveTo(new Location(leader.getX() + (int) (Math.cos(angle) * distance), leader.getY() + (int) (Math.sin(angle) * distance), leader.getZ()));
		}
	}
	
	/**
	 * Spawns the follower NPCs defined for a route next to the route leader and registers them as active walkers.
	 * @param leader The {@code Npc} walking the route.
	 * @param leaderWalk The {@code WalkInfo} of the leader.
	 */
	private void spawnFollowers(Npc leader, WalkInfo leaderWalk)
	{
		for (WalkRoute followerRoute : leaderWalk.getRoute().getFollowers())
		{
			spawnFollower(leader, leaderWalk, followerRoute);
		}
	}
	
	/**
	 * Spawns a single follower NPC next to the route leader and registers it as an active walker.
	 * @param leader The {@code Npc} walking the route.
	 * @param leaderWalk The {@code WalkInfo} of the leader.
	 * @param followerRoute The follower {@code WalkRoute} definition to spawn.
	 */
	private void spawnFollower(Npc leader, WalkInfo leaderWalk, WalkRoute followerRoute)
	{
		try
		{
			final Spawn spawn = new Spawn(followerRoute.getFollowerNpcId());
			spawn.setXYZ(leader.getX() + 10, leader.getY() + 10, leader.getZ());
			spawn.setHeading(leader.getHeading());
			spawn.setAmount(1);
			spawn.stopRespawn();
			
			final Npc follower = spawn.doSpawn(false);
			if (follower == null)
			{
				return;
			}
			
			follower.setWalker();
			applyHandItems(follower, followerRoute);
			
			final WalkInfo followerWalk = new WalkInfo(follower, followerRoute);
			followerWalk.setFollowTarget(leader);
			_activeRoutes.put(follower.getObjectId(), followerWalk);
			leaderWalk.addFollower(follower);
		}
		catch (Exception e)
		{
			LOGGER.warning(getClass().getSimpleName() + ": Failed to spawn follower " + followerRoute.getFollowerNpcId() + " for route '" + leaderWalk.getRoute().getName() + "': " + e.getMessage());
		}
	}
	
	/**
	 * Replaces dead or deleted followers of a route leader with fresh spawns.
	 * @param leader The {@code Npc} walking the route.
	 * @param walk The {@code WalkInfo} of the leader.
	 */
	private void maintainFollowers(Npc leader, WalkInfo walk)
	{
		final List<WalkRoute> followerRoutes = walk.getRoute().getFollowers();
		if (followerRoutes.isEmpty())
		{
			return;
		}
		
		final List<Npc> followers = walk.getFollowers();
		if (followers.isEmpty())
		{
			// All initial spawns failed, try again.
			spawnFollowers(leader, walk);
			return;
		}
		
		for (WalkRoute followerRoute : followerRoutes)
		{
			Npc existing = null;
			for (Npc follower : followers)
			{
				if (follower.getId() == followerRoute.getFollowerNpcId())
				{
					existing = follower;
					break;
				}
			}
			
			if (existing == null)
			{
				// Initial spawn failed, try again.
				spawnFollower(leader, walk, followerRoute);
			}
			else if (existing.isDead() || !existing.isSpawned())
			{
				// Replace the dead or deleted follower.
				_activeRoutes.remove(existing.getObjectId());
				followers.remove(existing);
				spawnFollower(leader, walk, followerRoute);
			}
		}
	}
	
	/**
	 * Displays the route configured hand items on a walker.
	 * @param npc The walker {@code Npc}.
	 * @param route The {@code WalkRoute} providing the hand item ids.
	 */
	private void applyHandItems(Npc npc, WalkRoute route)
	{
		final int rHandId = route.getRHandId();
		if (rHandId > 0)
		{
			npc.setRHandId(rHandId);
		}
		
		final int lHandId = route.getLHandId();
		if (lHandId > 0)
		{
			npc.setLHandId(lHandId);
		}
	}
	
	/**
	 * Checks if all living followers stand at their position and distance for the current leader destination.
	 * @param walk The {@code WalkInfo} of the route leader.
	 * @return {@code true} if every follower is placed, {@code false} otherwise.
	 */
	private boolean areFollowersInFormation(WalkInfo walk)
	{
		final Npc leader = walk.getNpc();
		final WalkNode node = walk.getCurrentNode();
		final double legAngle = Math.atan2(node.getY() - leader.getY(), node.getX() - leader.getX());
		for (Npc follower : walk.getFollowers())
		{
			// Do not wait forever for missing followers.
			if (follower.isDead() || !follower.isSpawned())
			{
				continue;
			}
			
			final WalkInfo followerWalk = _activeRoutes.get(follower.getObjectId());
			if (followerWalk == null)
			{
				continue;
			}
			
			final WalkRoute followerRoute = followerWalk.getRoute();
			final int followDistance = followerRoute.getFollowDistance();
			final double positionAngle = legAngle + Math.toRadians(followerRoute.getFollowPosition().getAngle());
			final int x = leader.getX() + (int) (Math.cos(positionAngle) * followDistance);
			final int y = leader.getY() + (int) (Math.sin(positionAngle) * followDistance);
			if (follower.calculateDistance2D(x, y, follower.getZ()) > FOLLOW_STAND_RANGE)
			{
				return false;
			}
		}
		
		return true;
	}
	
	private void moveToCurrentNode(WalkInfo walk)
	{
		final Npc npc = walk.getNpc();
		final WalkNode node = walk.getCurrentNode();
		if (node.runToLocation())
		{
			npc.setRunning();
		}
		else
		{
			npc.setWalking();
		}
		
		npc.getAI().setIntentionMoveTo(node);
	}
	
	private void broadcastNodeChat(Npc npc, WalkInfo walk, WalkNode node)
	{
		final NpcStringId npcString = node.getNpcString();
		final String chatText = node.getChatText();
		if ((npcString == null) && chatText.isEmpty())
		{
			return;
		}
		
		// Without a talkerNpcId the route walker itself speaks, otherwise the follower with that NPC id.
		Npc speaker = npc;
		final int talkerNpcId = node.getTalkerNpcId();
		if (talkerNpcId > 0)
		{
			speaker = null;
			for (Npc follower : walk.getFollowers())
			{
				if ((follower.getId() == talkerNpcId) && !follower.isDead() && follower.isSpawned())
				{
					speaker = follower;
					break;
				}
			}
			
			if (speaker == null)
			{
				return;
			}
		}
		
		if (npcString != null)
		{
			speaker.broadcastSay(ChatType.NPC_GENERAL, npcString);
		}
		else
		{
			speaker.broadcastSay(ChatType.NPC_GENERAL, chatText);
		}
	}
	
	/**
	 * Retrieves the singleton instance of the {@code WalkingTaskManager}.
	 * @return The active {@code WalkingTaskManager} instance.
	 */
	public static WalkingTaskManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final WalkingTaskManager INSTANCE = new WalkingTaskManager();
	}
}
