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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet informs the client that a character's nickname has been changed.<br>
 * It is used to update the displayed name of a {@code Creature} in the game world.
 * @author devScarlet
 */
public class NicknameChanged extends ServerPacket
{
	private final String _title;
	private final int _objectId;
	
	/**
	 * Creates a new {@code NicknameChanged} packet.<br>
	 * This packet notifies clients that a creature has changed its title.<br>
	 * It extracts the object ID and title from the provided {@code Creature}.
	 * @param creature The {@code Creature} whose nickname change needs to be sent.
	 */
	public NicknameChanged(Creature creature)
	{
		_objectId = creature.getObjectId();
		_title = creature.getTitle();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.NICKNAME_CHANGED.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeString(_title);
	}
}
