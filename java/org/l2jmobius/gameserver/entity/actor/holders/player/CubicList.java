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
package org.l2jmobius.gameserver.entity.actor.holders.player;

import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.cubic.Cubic;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoCubic;

/**
 * Manages the cubics of a {@code Player}.<br>
 * It handles summoning, lookup and stopping the active cubics.
 * @author Mobius
 */
public class CubicList
{
	private final Player _player;
	private final Map<Integer, Cubic> _entries = new ConcurrentSkipListMap<>();
	
	/**
	 * Creates a new {@code CubicList} for a specific player.
	 * @param player The {@code Player} object associated with this instance.
	 */
	public CubicList(Player player)
	{
		_player = player;
	}
	
	/**
	 * Adds a {@code Cubic} object to the internal collection.<br>
	 * This method maps the {@code Cubic} to its template ID.<br>
	 * It returns the value associated with the key or {@code null}.
	 * @param cubic The {@code Cubic} object to add.
	 * @return The {@code Cubic} that was mapped, or {@code null}.
	 */
	public Cubic add(Cubic cubic)
	{
		return _entries.put(cubic.getTemplate().getId(), cubic);
	}
	
	/**
	 * Retrieves the collection of cubic items.
	 * @return a {@code Map} where the key is an {@code Integer} and the value is a {@code Cubic}.
	 */
	public Map<Integer, Cubic> getCubics()
	{
		return _entries;
	}
	
	/**
	 * Retrieves a {@code Cubic} object based on its unique identifier.<br>
	 * This method looks up the cubic in the internal collection.
	 * @param cubicId The unique ID of the cubic to find.
	 * @return The {@code Cubic} object associated with the provided ID, or {@code null} if not found.
	 */
	public Cubic getById(int cubicId)
	{
		return _entries.get(cubicId);
	}
	
	/**
	 * Stops all active cubics for the player.<br>
	 * This method deactivates every cubic in the {@code _entries} collection.<br>
	 * It then clears the internal map of cubics.
	 */
	public void stopAll()
	{
		if (!_entries.isEmpty())
		{
			_entries.values().forEach(Cubic::deactivate);
			_entries.clear();
		}
	}
	
	/**
	 * Stops all cubic effects that were given by other players.<br>
	 * This method removes these cubics from the internal collection.<br>
	 * It also sends a packet to update the user information if any changes occur.
	 */
	public void stopByOthers()
	{
		if (!_entries.isEmpty())
		{
			boolean broadcast = false;
			for (Cubic cubic : _entries.values())
			{
				if (cubic.isGivenByOther())
				{
					cubic.deactivate();
					_entries.remove(cubic.getTemplate().getId());
					broadcast = true;
				}
			}
			
			if (broadcast)
			{
				_player.sendPacket(new ExUserInfoCubic(_player));
				_player.broadcastUserInfo();
			}
		}
	}
	
	/**
	 * Checks whether the player has no active cubic.
	 * @return {@code true} if there is no active cubic, {@code false} otherwise.
	 */
	public boolean isEmpty()
	{
		return _entries.isEmpty();
	}
	
	/**
	 * Retrieves how many cubics are active.
	 * @return The number of active cubics.
	 */
	public int size()
	{
		return _entries.size();
	}
	
	/**
	 * Removes every cubic from the list without deactivating them.
	 */
	public void clear()
	{
		_entries.clear();
	}
	
	/**
	 * Removes a cubic from the list.
	 * @param cubicId The identifier of the cubic to remove.
	 */
	public void remove(int cubicId)
	{
		_entries.remove(cubicId);
	}
}
