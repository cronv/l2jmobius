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

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.network.NpcStringId;

/**
 * Immutable single point of a walking route.<br>
 * Extends {@code Location} with the wait delay at the point, the movement mode and optional chat said on arrival.
 * @author Mobius
 */
public class WalkNode extends Location
{
	private final int _delay;
	private final boolean _run;
	private final NpcStringId _npcString;
	private final String _chatText;
	private final int _talkerNpcId;
	
	/**
	 * Creates a new {@code WalkNode}.
	 * @param x The X coordinate of the node.
	 * @param y The Y coordinate of the node.
	 * @param z The Z coordinate of the node.
	 * @param delay The wait time in seconds at this node.
	 * @param run Set to {@code true} to run to this node, {@code false} to walk.
	 * @param npcString The {@code NpcStringId} said on arrival, or {@code null}.
	 * @param chatText The custom text said on arrival, or {@code null}.
	 * @param talkerNpcId The NPC id of the follower saying the line, or 0 for the route walker itself.
	 */
	public WalkNode(int x, int y, int z, int delay, boolean run, NpcStringId npcString, String chatText, int talkerNpcId)
	{
		super(x, y, z);
		_delay = delay;
		_run = run;
		_npcString = npcString;
		_chatText = chatText == null ? "" : chatText;
		_talkerNpcId = talkerNpcId;
	}
	
	/**
	 * @return The wait time in seconds at this node.
	 */
	public int getDelay()
	{
		return _delay;
	}
	
	/**
	 * @return {@code true} if the NPC should run to this node, {@code false} to walk.
	 */
	public boolean runToLocation()
	{
		return _run;
	}
	
	/**
	 * @return The {@code NpcStringId} said on arrival, or {@code null} if none.
	 */
	public NpcStringId getNpcString()
	{
		return _npcString;
	}
	
	/**
	 * @return The custom chat text said on arrival, or an empty string if none.
	 */
	public String getChatText()
	{
		return _chatText;
	}
	
	/**
	 * @return The NPC id of the follower saying the line, or 0 for the route walker itself.
	 */
	public int getTalkerNpcId()
	{
		return _talkerNpcId;
	}
}
