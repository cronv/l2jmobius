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

package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

/**
 * This class handles the packet to change a player's access level on the login server.<br>
 * It uses opcode {@code 0x04} and contains an {@code int} for the access level and a {@code String} for the player name.
 * @author BazookaRpm
 */
public class ChangeAccessLevel extends SimpleWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x04;
	
	// Data.
	private final String _player;
	private final int _access;
	
	/**
	 * Updates the permission level for a specific player.<br>
	 * This method sets the new access value for the provided name.
	 * @param player The name of the player to update.
	 * @param access The new integer value for the access level.
	 */
	public ChangeAccessLevel(String player, int access)
	{
		_player = (player != null) ? player : "";
		_access = access;
	}
	
	/**
	 * Writes the packet data to the network stream.<br>
	 * This method serializes the {@code OPCODE}, {@code _access}, and {@code _player} fields.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeInt(_access);
		writeString(_player);
	}
}
