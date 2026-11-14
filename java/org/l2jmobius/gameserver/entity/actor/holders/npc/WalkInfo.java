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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Npc;

/**
 * Runtime walking state of a single {@code Npc} moving along a {@code WalkRoute}.<br>
 * Holds the route cursor, traversal direction, wait deadline and pause flags.<br>
 * All scheduling is done by the {@code WalkingTaskManager} pulse task, this class holds state only.
 * @author Mobius
 */
public class WalkInfo
{
	private final Npc _npc;
	private final WalkRoute _route;
	
	private int _cursor;
	private int _step = 1;
	private volatile boolean _suspended;
	private volatile boolean _stoppedByAttack;
	private volatile long _waitUntil;
	
	private int _moveAttempts;
	private int _followerWaitTicks;
	private volatile boolean _waitingForFollowers;
	
	// Follower route state.
	private Npc _followTarget;
	private int _lastFollowX;
	private int _lastFollowY;
	
	// Followers spawned for this route leader.
	private List<Npc> _followers;
	
	/**
	 * Creates the walking state for an {@code Npc} on a specific route.
	 * @param npc The {@code Npc} performing the walk.
	 * @param route The {@code WalkRoute} to traverse.
	 */
	public WalkInfo(Npc npc, WalkRoute route)
	{
		_npc = npc;
		_route = route;
	}
	
	public Npc getNpc()
	{
		return _npc;
	}
	
	public WalkRoute getRoute()
	{
		return _route;
	}
	
	public boolean isSuspended()
	{
		return _suspended;
	}
	
	public void setSuspended(boolean value)
	{
		_suspended = value;
	}
	
	public boolean isStoppedByAttack()
	{
		return _stoppedByAttack;
	}
	
	public void setStoppedByAttack(boolean value)
	{
		_stoppedByAttack = value;
	}
	
	/**
	 * @return {@code true} if the NPC is waiting out a node delay.
	 */
	public boolean isWaiting()
	{
		return _waitUntil > 0;
	}
	
	/**
	 * @return The time in milliseconds when the node delay expires.
	 */
	public long getWaitUntil()
	{
		return _waitUntil;
	}
	
	/**
	 * Sets the node delay deadline. Use 0 to clear the waiting state.
	 * @param value The time in milliseconds when movement should resume.
	 */
	public void setWaitUntil(long value)
	{
		_waitUntil = value;
	}
	
	public int getCurrentNodeId()
	{
		return _cursor;
	}
	
	/**
	 * @return The current target {@code WalkNode}, clamped to the route bounds.
	 */
	public WalkNode getCurrentNode()
	{
		return _route.getNodeList().get(Math.min(Math.max(_cursor, 0), _route.getNodesCount() - 1));
	}
	
	/**
	 * @return The {@code WalkNode} the walker came from, clamped to the route bounds.
	 */
	public WalkNode getPreviousNode()
	{
		return _route.getNodeList().get(Math.min(Math.max(_cursor - _step, 0), _route.getNodesCount() - 1));
	}
	
	/**
	 * Moves the cursor to the next node of the route.<br>
	 * Handles random selection, direction bounce and cycle restart based on the route repeat type.
	 * @return {@code true} if the end of a cycle was reached, {@code false} otherwise.
	 */
	public synchronized boolean advance()
	{
		final int nodesCount = _route.getNodesCount();
		if (_route.getRepeatType() == WalkRoute.REPEAT_RANDOM)
		{
			if (nodesCount > 1)
			{
				int candidate = _cursor;
				while (candidate == _cursor)
				{
					candidate = Rnd.get(nodesCount);
				}
				_cursor = candidate;
			}
			return false;
		}
		
		_cursor += _step;
		if (_cursor >= nodesCount)
		{
			switch (_route.getRepeatType())
			{
				case WalkRoute.REPEAT_GO_BACK:
				{
					_step = -1;
					_cursor = nodesCount - 2;
					break;
				}
				case WalkRoute.REPEAT_GO_FIRST:
				{
					_step = 1;
					_cursor = 0;
					break;
				}
				case WalkRoute.REPEAT_TELE_FIRST:
				{
					_npc.teleToLocation(_npc.getSpawn().getLocation());
					_step = 1;
					_cursor = 0;
					break;
				}
			}
			return true;
		}
		
		if (_cursor < 0)
		{
			// Back style reached the first node again, bounce forward.
			_cursor = 1;
			_step = 1;
		}
		return false;
	}
	
	/**
	 * Counts a movement order towards the current node.
	 * @return The number of move orders issued for the current node so far.
	 */
	public int incrementMoveAttempts()
	{
		return ++_moveAttempts;
	}
	
	public void resetMoveAttempts()
	{
		_moveAttempts = 0;
	}
	
	/**
	 * Counts a pulse tick spent waiting for the followers to catch up.
	 * @return The number of ticks waited so far.
	 */
	public int incrementFollowerWaitTicks()
	{
		return ++_followerWaitTicks;
	}
	
	public void resetFollowerWaitTicks()
	{
		_followerWaitTicks = 0;
	}
	
	/**
	 * @return {@code true} if the route leader stands at a point waiting for its followers to adjust their position and distance.
	 */
	public boolean isWaitingForFollowers()
	{
		return _waitingForFollowers;
	}
	
	public void setWaitingForFollowers(boolean value)
	{
		_waitingForFollowers = value;
	}
	
	public Npc getFollowTarget()
	{
		return _followTarget;
	}
	
	public void setFollowTarget(Npc npc)
	{
		_followTarget = npc;
	}
	
	public int getLastFollowX()
	{
		return _lastFollowX;
	}
	
	public int getLastFollowY()
	{
		return _lastFollowY;
	}
	
	/**
	 * Stores the last destination a follow move order was issued for.
	 * @param x The X coordinate of the destination.
	 * @param y The Y coordinate of the destination.
	 */
	public void setLastFollowDestination(int x, int y)
	{
		_lastFollowX = x;
		_lastFollowY = y;
	}
	
	/**
	 * Registers a follower NPC spawned for this route leader.
	 * @param npc The spawned follower {@code Npc}.
	 */
	public void addFollower(Npc npc)
	{
		if (_followers == null)
		{
			_followers = new ArrayList<>(1);
		}
		_followers.add(npc);
	}
	
	/**
	 * @return The follower NPCs spawned for this route leader, or an empty list if none.
	 */
	public List<Npc> getFollowers()
	{
		return _followers == null ? Collections.emptyList() : _followers;
	}
	
	@Override
	public String toString()
	{
		return "WalkInfo [route=" + _route.getName() + ", cursor=" + _cursor + ", step=" + _step + ", suspended=" + _suspended + ", stoppedByAttack=" + _stoppedByAttack + ", waitUntil=" + _waitUntil + "]";
	}
}
