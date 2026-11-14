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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.StatusUpdateType;

/**
 * This packet is used to synchronize the status of world objects with the client.<br>
 * It informs the player about changes such as animations, states, or visual updates for {@code WorldObject} instances.
 * @author Mobius
 */
public class StatusUpdate extends ServerPacket
{
	private final int _objectId;
	private int _casterObjectId = 0;
	private final boolean _isPlayable;
	private boolean _isVisible = false;
	private final Map<StatusUpdateType, Number> _updates = new LinkedHashMap<>();
	
	/**
	 * Creates a new {@code StatusUpdate} packet for a specific world object.<br>
	 * This constructor initializes the packet with the object's ID and playability status.
	 * @param object The {@code WorldObject} to be represented in this update.
	 */
	public StatusUpdate(WorldObject object)
	{
		_objectId = object.getObjectId();
		_isPlayable = object.isPlayable();
	}
	
	/**
	 * Adds a new status update to the packet.<br>
	 * This method stores the value for the specified {@code StatusUpdateType}.<br>
	 * It also updates the visibility of the packet if it is playable.
	 * @param type The type of status being updated.
	 * @param level The numerical value associated with this status update.
	 */
	public void addUpdate(StatusUpdateType type, long level)
	{
		_updates.put(type, level);
		if (_isPlayable)
		{
			switch (type)
			{
				case CUR_HP:
				case CUR_MP:
				case CUR_CP:
				{
					_isVisible = true;
					break;
				}
			}
		}
	}
	
	/**
	 * Adds a caster to the {@code StatusUpdate} packet.<br>
	 * This method sets the internal ID of the caster from the provided {@code WorldObject}.
	 * @param object The {@code WorldObject} that will be used as the caster.
	 */
	public void addCaster(WorldObject object)
	{
		_casterObjectId = object.getObjectId();
	}
	
	/**
	 * Checks if there are any updates in the packet.<br>
	 * Returns {@code true} if the update map is not empty.<br>
	 * Returns {@code false} if no updates have been added.
	 * @return a boolean indicating if updates exist.
	 */
	public boolean hasUpdates()
	{
		return !_updates.isEmpty();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.STATUS_UPDATE.writeId(this, buffer);
		buffer.writeInt(_objectId); // casterId
		buffer.writeInt(_isVisible ? _casterObjectId : 0);
		buffer.writeByte(_isVisible);
		
		buffer.writeByte(_updates.size());
		for (Entry<StatusUpdateType, Number> entry : _updates.entrySet())
		{
			final StatusUpdateType type = entry.getKey();
			buffer.writeByte(type.getClientId());
			
			switch (type)
			{
				case CUR_HP:
				case MAX_HP:
				case ATK_SPD:
				case CAST_SPD:
				{
					buffer.writeLong(entry.getValue().longValue());
					break;
				}
				default:
				{
					buffer.writeInt(entry.getValue().intValue());
					break;
				}
			}
		}
	}
	
	/**
	 * Checks if the packet can be dropped by the server.<br>
	 * This method currently always returns {@code true}.
	 * @param client The {@code GameClient} instance to check.
	 * @return {@code true} if the packet can be dropped, otherwise {@code false}.
	 */
	@Override
	public boolean canBeDropped(GameClient client)
	{
		return true;
	}
}
