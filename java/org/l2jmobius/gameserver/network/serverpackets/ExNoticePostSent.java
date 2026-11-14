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
 * This packet is sent to the client to confirm that an extra notice has been successfully posted.<br>
 * It informs the {@code GameClient} that the request was processed by the server.
 * @author Migi
 */
public class ExNoticePostSent extends ServerPacket
{
	private static final ExNoticePostSent STATIC_PACKET_TRUE = new ExNoticePostSent(true);
	private static final ExNoticePostSent STATIC_PACKET_FALSE = new ExNoticePostSent(false);
	
	/**
	 * Creates an {@code ExNoticePostSent} packet based on a boolean value.<br>
	 * This method returns a pre-existing static instance to save memory.
	 * @param result The boolean value determining which packet type to return.
	 * @return An {@code ExNoticePostSent} object corresponding to the provided {@code result}.
	 */
	public static ExNoticePostSent valueOf(boolean result)
	{
		return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
	}
	
	private final boolean _showAnim;
	
	/**
	 * Creates a new {@code ExNoticePostSent} packet.<br>
	 * This constructor sets the animation visibility flag.
	 * @param showAnimation The value to determine if the animation should be shown.
	 */
	public ExNoticePostSent(boolean showAnimation)
	{
		_showAnim = showAnimation;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_REPLY_WRITE_POST.writeId(this, buffer);
		buffer.writeInt(_showAnim);
	}
}
