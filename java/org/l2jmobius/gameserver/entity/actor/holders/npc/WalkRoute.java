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
package org.l2jmobius.gameserver.entity.actor.holders.npc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.enums.npc.FollowPosition;

/**
 * Immutable definition of a walking route loaded from RouteData.xml.<br>
 * A point route holds the list of points to traverse, a repeat policy and optional follower definitions.<br>
 * A follower definition is itself a {@code WalkRoute} that keeps a spawned NPC near the route leader at a given angle and distance.
 * @author Mobius
 */
public class WalkRoute
{
	// Repeat style:
	// Use -1 for no repeat, 0 to go back, and 1 to go to the first point in a circle style.
	// 2 - teleport to first point (conveyor style).
	// 3 - random walking between points.
	public static final byte NO_REPEAT = -1;
	public static final byte REPEAT_GO_BACK = 0;
	public static final byte REPEAT_GO_FIRST = 1;
	public static final byte REPEAT_TELE_FIRST = 2;
	public static final byte REPEAT_RANDOM = 3;
	
	private final String _name;
	private final List<WalkNode> _nodeList;
	private final boolean _repeatWalk;
	private final byte _repeatType;
	private final List<WalkRoute> _followers = new ArrayList<>(1);
	
	// Hand items displayed by the NPC walking this route (0 for none).
	private final int _rHandId;
	private final int _lHandId;
	
	// Follower route data (0 when this is a point route).
	private final int _followerNpcId;
	private final int _leaderNpcId;
	private final FollowPosition _followPosition;
	private final int _followDistance;
	
	/**
	 * Creates a point route.
	 * @param name The unique name for this route.
	 * @param nodeList The ordered list of {@code WalkNode} points, must not be empty.
	 * @param repeat Whether the NPC should repeat the walk after the last point.
	 * @param repeatType The repeat style, one of the REPEAT_* constants.
	 * @param rHandId The right hand item id displayed by the walker, or 0 for none.
	 * @param lHandId The left hand item id displayed by the walker, or 0 for none.
	 */
	public WalkRoute(String name, List<WalkNode> nodeList, boolean repeat, byte repeatType, int rHandId, int lHandId)
	{
		if (nodeList.isEmpty())
		{
			throw new IllegalArgumentException("WalkRoute node list is empty: " + name);
		}
		
		_name = name;
		_nodeList = Collections.unmodifiableList(nodeList);
		_repeatType = repeatType;
		_repeatWalk = repeat && (repeatType >= REPEAT_GO_BACK) && (repeatType <= REPEAT_TELE_FIRST);
		_rHandId = rHandId;
		_lHandId = lHandId;
		_followerNpcId = 0;
		_leaderNpcId = 0;
		_followPosition = null;
		_followDistance = 0;
	}
	
	/**
	 * Creates a follower route.<br>
	 * The follower NPC always moves to the point its leader walks to, shifted to the given position at the given distance.
	 * @param name The unique name for this route.
	 * @param followerNpcId The NPC id spawned as follower.
	 * @param leaderNpcId The NPC id of the leader to follow.
	 * @param followPosition The {@code FollowPosition} relative to the leader movement direction.
	 * @param followDistance The distance in units kept from the leader destination.
	 * @param rHandId The right hand item id displayed by the follower, or 0 for none.
	 * @param lHandId The left hand item id displayed by the follower, or 0 for none.
	 */
	public WalkRoute(String name, int followerNpcId, int leaderNpcId, FollowPosition followPosition, int followDistance, int rHandId, int lHandId)
	{
		_name = name;
		_nodeList = Collections.emptyList();
		_repeatWalk = false;
		_repeatType = NO_REPEAT;
		_rHandId = rHandId;
		_lHandId = lHandId;
		_followerNpcId = followerNpcId;
		_leaderNpcId = leaderNpcId;
		_followPosition = followPosition;
		_followDistance = followDistance;
	}
	
	public String getName()
	{
		return _name;
	}
	
	public List<WalkNode> getNodeList()
	{
		return _nodeList;
	}
	
	public int getNodesCount()
	{
		return _nodeList.size();
	}
	
	public boolean repeatWalk()
	{
		return _repeatWalk;
	}
	
	public byte getRepeatType()
	{
		return _repeatType;
	}
	
	/**
	 * Adds a follower definition to this point route.
	 * @param followerRoute The follower {@code WalkRoute} spawned and driven alongside this route.
	 */
	public void addFollower(WalkRoute followerRoute)
	{
		_followers.add(followerRoute);
	}
	
	/**
	 * @return The follower routes spawned alongside this route, empty when none are defined.
	 */
	public List<WalkRoute> getFollowers()
	{
		return _followers;
	}
	
	public boolean isFollowRoute()
	{
		return _followerNpcId > 0;
	}
	
	/**
	 * @return The NPC id spawned as follower for this follower route.
	 */
	public int getFollowerNpcId()
	{
		return _followerNpcId;
	}
	
	/**
	 * @return The NPC id of the leader this follower route follows.
	 */
	public int getLeaderNpcId()
	{
		return _leaderNpcId;
	}
	
	public FollowPosition getFollowPosition()
	{
		return _followPosition;
	}
	
	public int getFollowDistance()
	{
		return _followDistance;
	}
	
	/**
	 * @return The right hand item id displayed by the walker, or 0 for none.
	 */
	public int getRHandId()
	{
		return _rHandId;
	}
	
	/**
	 * @return The left hand item id displayed by the walker, or 0 for none.
	 */
	public int getLHandId()
	{
		return _lHandId;
	}
}
