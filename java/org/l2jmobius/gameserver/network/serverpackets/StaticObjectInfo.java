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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.StaticObject;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This class represents a packet sent to the client containing information about a {@code StaticObject}.<br>
 * It is used to synchronize the state and properties of static world objects with the game client.
 * @author KenM
 */
public class StaticObjectInfo extends ServerPacket
{
	private final int _staticObjectId;
	private final int _objectId;
	private final int _type;
	private final boolean _isTargetable;
	private final int _meshIndex;
	private final boolean _isClosed;
	private final boolean _isEnemy;
	private final int _maxHp;
	private final int _currentHp;
	private final boolean _showHp;
	private final int _damageGrade;
	
	/**
	 * Creates a new {@code StaticObjectInfo} packet from a {@code StaticObject}.<br>
	 * This constructor initializes the packet with default values.<br>
	 * It extracts the ID and mesh index from the provided object.
	 * @param staticObject The {@code StaticObject} to use for initialization.
	 */
	public StaticObjectInfo(StaticObject staticObject)
	{
		_staticObjectId = staticObject.getId();
		_objectId = staticObject.getObjectId();
		_type = 0;
		_isTargetable = true;
		_meshIndex = staticObject.getMeshIndex();
		_isClosed = false;
		_isEnemy = false;
		_maxHp = 0;
		_currentHp = 0;
		_showHp = false;
		_damageGrade = 0;
	}
	
	/**
	 * Creates a new {@code StaticObjectInfo} packet from a {@code Door}.<br>
	 * This constructor sets the object type to 1.<br>
	 * It determines if the object is targetable based on both the door and the provided boolean.
	 * @param door The {@code Door} instance used to populate the packet data.
	 * @param targetable A boolean value that can be used to override or combine with the door's targetable status.
	 */
	public StaticObjectInfo(Door door, boolean targetable)
	{
		_staticObjectId = door.getId();
		_objectId = door.getObjectId();
		_type = 1;
		_isTargetable = door.isTargetable() || targetable;
		_meshIndex = door.getMeshIndex();
		_isClosed = !door.isOpen();
		_isEnemy = door.isEnemy();
		_maxHp = (int) door.getMaxHp();
		_currentHp = (int) door.getCurrentHp();
		_showHp = door.isShowHp();
		_damageGrade = door.getDamage();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.STATIC_OBJECT_INFO.writeId(this, buffer);
		buffer.writeInt(_staticObjectId);
		buffer.writeInt(_objectId);
		buffer.writeInt(_type);
		buffer.writeInt(_isTargetable);
		buffer.writeInt(_meshIndex);
		buffer.writeInt(_isClosed);
		buffer.writeInt(_isEnemy);
		buffer.writeInt(_currentHp);
		buffer.writeInt(_maxHp);
		buffer.writeInt(_showHp);
		buffer.writeInt(_damageGrade);
	}
}
