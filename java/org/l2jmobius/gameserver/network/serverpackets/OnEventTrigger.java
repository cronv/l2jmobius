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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to notify it that a specific event has been triggered.<br>
 * It allows the game client to execute corresponding visual or logic effects based on the received data.
 * @author Gnacik
 */
public class OnEventTrigger extends ServerPacket
{
	private final int _emitterId;
	private final boolean _enabled;
	
	/**
	 * Creates a new {@code OnEventTrigger} packet.<br>
	 * This packet defines an event trigger and its status.
	 * @param emitterId The unique identifier for the source of the event.
	 * @param enabled Whether the event is currently active.
	 */
	public OnEventTrigger(int emitterId, boolean enabled)
	{
		_emitterId = emitterId;
		_enabled = enabled;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EVENT_TRIGGER.writeId(this, buffer);
		buffer.writeInt(_emitterId);
		buffer.writeByte(_enabled);
	}
}
