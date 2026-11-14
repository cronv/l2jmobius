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
package org.l2jmobius.gameserver.mechanics.events.holders.actor.player;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.IBaseEvent;

/**
 * This class handles the event triggered when a {@code Player} successfully tames a beast.<br>
 * It serves as an event holder for processing logic related to this specific action.
 * @author CostyKiller
 */
public class OnPlayerTameBeast implements IBaseEvent
{
	private final Player _player;
	private final int _npcId;
	
	/**
	 * Creates a new event for when a {@code Player} tames a beast.<br>
	 * This constructor stores the involved actor and the target NPC identifier.
	 * @param player The {@code Player} who performed the action.
	 * @param npcId The unique ID of the beast being tamed.
	 */
	public OnPlayerTameBeast(Player player, int npcId)
	{
		_player = player;
		_npcId = npcId;
	}
	
	/**
	 * Retrieves the {@code Player} associated with this event.<br>
	 * This method returns the player who performed the action.
	 * @return the {@code Player} object.
	 */
	public Player getPlayer()
	{
		return _player;
	}
	
	/**
	 * Retrieves the unique identifier of the NPC. <br>
	 * This ID corresponds to the beast that was tamed by a {@code Player}.
	 * @return The {@code int} value representing the NPC ID.
	 */
	public int getNpcId()
	{
		return _npcId;
	}
	
	/**
	 * Retrieves the type of this event.<br>
	 * This method returns the specific {@code EventType} associated with a player taming a beast.
	 * @return The {@code EventType} for this action.
	 */
	@Override
	public EventType getType()
	{
		return EventType.ON_PLAYER_TAME_BEAST;
	}
}
