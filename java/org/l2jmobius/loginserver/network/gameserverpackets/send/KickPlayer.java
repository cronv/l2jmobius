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
package org.l2jmobius.loginserver.network.gameserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

/**
 * This packet is used to disconnect a player from the game server.<br>
 * It sends a notification to the client to initiate the kick process.
 * @author -Wooden-
 */
public class KickPlayer extends SimpleWritablePacket
{
	/**
	 * Sends a packet to disconnect a specific player.<br>
	 * This method identifies the user by their account name.
	 * @param account The unique identifier for the player's account.
	 */
	public KickPlayer(String account)
	{
		writeByte(0x04);
		writeString(account);
	}
}
