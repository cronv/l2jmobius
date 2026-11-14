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
 * This packet is sent to the client when an exceptional notice has arrived.<br>
 * It handles the delivery of specific server-side notifications to the player.
 * @author Migi
 */
public class ExNoticePostArrived extends ServerPacket
{
	private static final ExNoticePostArrived STATIC_PACKET_TRUE = new ExNoticePostArrived(true);
	private static final ExNoticePostArrived STATIC_PACKET_FALSE = new ExNoticePostArrived(false);
	
	/**
	 * Creates an {@code ExNoticePostArrived} instance based on a boolean value.<br>
	 * This method returns a pre-defined static packet object.
	 * @param result The boolean value to determine which packet to return.
	 * @return An {@code ExNoticePostArrived} object corresponding to the provided {@code result}.
	 */
	public static ExNoticePostArrived valueOf(boolean result)
	{
		return result ? STATIC_PACKET_TRUE : STATIC_PACKET_FALSE;
	}
	
	private final boolean _showAnim;
	
	/**
	 * Creates a new {@code ExNoticePostArrived} packet.<br>
	 * This method sets whether an animation should be displayed to the client.
	 * @param showAnimation The boolean value determining if the animation is shown.
	 */
	public ExNoticePostArrived(boolean showAnimation)
	{
		_showAnim = showAnimation;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_NOTICE_POST_ARRIVED.writeId(this, buffer);
		buffer.writeInt(_showAnim);
	}
}
