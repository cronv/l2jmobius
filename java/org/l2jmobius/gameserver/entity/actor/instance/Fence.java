/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.entity.actor.instance;

import org.l2jmobius.gameserver.data.xml.FenceData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.FenceState;
import org.l2jmobius.gameserver.managers.IdManager;
import org.l2jmobius.gameserver.network.serverpackets.DeleteObject;
import org.l2jmobius.gameserver.network.serverpackets.ExColosseumFenceInfo;

/**
 * Represents a fence object within the game world.<br>
 * This class handles the behavior and state of fences used in various instances.
 * @author HorridoJoho
 */
public class Fence extends WorldObject
{
	private final int _xMin;
	private final int _xMax;
	private final int _yMin;
	private final int _yMax;
	
	private final String _name;
	private final int _width;
	private final int _length;
	
	private FenceState _state;
	private int[] _heightFences;
	
	/**
	 * Creates a new {@code Fence} object in the game world.<br>
	 * This constructor initializes the position, dimensions, and state of the fence.<br>
	 * It also generates unique IDs for multiple height layers if required.
	 * @param x The center x-coordinate of the fence.
	 * @param y The center y-coordinate of the fence.
	 * @param name The display name of the fence.
	 * @param width The total width of the fence.
	 * @param length The total length of the fence.
	 * @param height The number of vertical layers for the fence.
	 * @param state The initial {@code FenceState} of the object.
	 */
	public Fence(int x, int y, String name, int width, int length, int height, FenceState state)
	{
		super(IdManager.getInstance().getNextId());
		
		_xMin = x - (width / 2);
		_xMax = x + (width / 2);
		_yMin = y - (length / 2);
		_yMax = y + (length / 2);
		_name = name;
		_width = width;
		_length = length;
		_state = state;
		if (height > 1)
		{
			_heightFences = new int[height - 1];
			for (int i = 0; i < _heightFences.length; i++)
			{
				_heightFences[i] = IdManager.getInstance().getNextId();
			}
		}
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Fence}.<br>
	 * This value corresponds to the object ID in the game world.
	 * @return The unique integer ID of the fence.
	 */
	@Override
	public int getId()
	{
		return getObjectId();
	}
	
	/**
	 * Retrieves the name of this {@code Fence}.<br>
	 * This value was provided during object creation.
	 * @return The {@code String} name of the fence.
	 */
	@Override
	public String getName()
	{
		return _name;
	}
	
	/**
	 * Checks if the {@code Fence} can be attacked by a specific creature.<br>
	 * This method currently always returns {@code false}.
	 * @param attacker The {@code Creature} attempting to perform an attack.
	 * @return {@code true} if the fence is attackable, otherwise {@code false}.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return false;
	}
	
	/**
	 * Sends the fence information to a specific player.<br>
	 * This method sends the main {@code Fence} data packet.<br>
	 * It also sends packets for all associated height fences if they exist.
	 * @param player The {@code Player} who will receive the information.
	 */
	@Override
	public void sendInfo(Player player)
	{
		player.sendPacket(new ExColosseumFenceInfo(this));
		if (_heightFences != null)
		{
			for (int objId : _heightFences)
			{
				player.sendPacket(new ExColosseumFenceInfo(objId, getX(), getY(), getZ(), _width, _length, _state));
			}
		}
	}
	
	/**
	 * This method handles the decay process for this fence.<br>
	 * It removes all associated height fences from visible players.<br>
	 * The method then calls the base {@code WorldObject} decay logic.
	 * @return {@code true} if the object was successfully decayed, otherwise {@code false}.
	 */
	@Override
	public boolean decayMe()
	{
		if (_heightFences != null)
		{
			final DeleteObject[] deleteObjects = new DeleteObject[_heightFences.length];
			for (int i = 0; i < _heightFences.length; i++)
			{
				deleteObjects[i] = new DeleteObject(_heightFences[i]);
			}
			
			World.forEachVisibleObject(this, Player.class, player ->
			{
				for (int i = 0; i < _heightFences.length; i++)
				{
					player.sendPacket(deleteObjects[i]);
				}
			});
		}
		
		return super.decayMe();
	}
	
	/**
	 * Removes this fence from the game world.<br>
	 * This method calls {@code decayMe()} before removal.<br>
	 * It returns {@code false} regardless of the outcome.
	 * @return {@code false} always.
	 */
	public boolean deleteMe()
	{
		decayMe();
		
		FenceData.getInstance().removeFence(this);
		return false;
	}
	
	/**
	 * Retrieves the current state of the fence.<br>
	 * This method returns the {@code FenceState} value stored in this object.
	 * @return The current {@code FenceState}.
	 */
	public FenceState getState()
	{
		return _state;
	}
	
	/**
	 * Updates the current state of the fence.<br>
	 * This method also broadcasts the new information to all players.
	 * @param type The new {@code FenceState} to apply.
	 */
	public void setState(FenceState type)
	{
		_state = type;
		broadcastInfo();
	}
	
	/**
	 * Retrieves the width of the {@code Fence}.<br>
	 * This value represents the horizontal size of the object.
	 * @return The width as an {@code int}.
	 */
	public int getWidth()
	{
		return _width;
	}
	
	/**
	 * Returns the length of the {@code Fence}.<br>
	 * This value represents the total distance along the long side.
	 * @return The length as an {@code int}.
	 */
	public int getLength()
	{
		return _length;
	}
	
	/**
	 * Retrieves the minimum X coordinate of the fence.<br>
	 * This value defines the left boundary of the object.
	 * @return The minimum X coordinate as an {@code int}.
	 */
	public int getXMin()
	{
		return _xMin;
	}
	
	/**
	 * Retrieves the minimum Y coordinate of the {@code Fence}.<br>
	 * This value represents the bottom boundary of the fence area.
	 * @return The minimum Y coordinate as an {@code int}.
	 */
	public int getYMin()
	{
		return _yMin;
	}
	
	/**
	 * Retrieves the maximum X coordinate of the fence.<br>
	 * This value defines the right boundary of the fence area.
	 * @return The maximum X coordinate as an {@code int}.
	 */
	public int getXMax()
	{
		return _xMax;
	}
	
	/**
	 * Retrieves the maximum Y coordinate of the {@code Fence}.<br>
	 * This value represents the upper boundary of the fence area.
	 * @return the maximum Y coordinate as an {@code int}.
	 */
	public int getYMax()
	{
		return _yMax;
	}
	
	/**
	 * Checks if the current object is a fence.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if this instance is a fence.
	 */
	@Override
	public boolean isFence()
	{
		return true;
	}
}
