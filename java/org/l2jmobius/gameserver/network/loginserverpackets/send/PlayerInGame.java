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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

/**
 * Announces the players currently connected on this game server to the login server.<br>
 * This packet supports sending either a single player or a list of players.<br>
 * It uses opcode {@code 0x02} and relies on {@code SimpleWritablePacket.write*} methods for its payload.
 * @author BazookaRpm
 */
public class PlayerInGame extends SimpleWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x02;
	
	// Data.
	private final List<String> _players;
	
	/**
	 * Creates a new {@code PlayerInGame} packet for a single player.<br>
	 * This constructor handles the case where only one name is provided.<br>
	 * If the input is {@code null}, it defaults to an empty string.
	 * @param player The name of the player to include in the packet.
	 */
	public PlayerInGame(String player)
	{
		this(Arrays.asList((player != null) ? player : ""));
	}
	
	/**
	 * Creates a new {@code PlayerInGame} packet with a list of player names.<br>
	 * This method handles the initialization of the {@code _players} field.<br>
	 * If the provided list is {@code null}, it defaults to an empty list.
	 * @param players A {@code List} of player names to include in the packet.
	 */
	public PlayerInGame(List<String> players)
	{
		_players = (players != null) ? players : Collections.emptyList();
	}
	
	/**
	 * Writes the packet data to the output stream.<br>
	 * It includes the {@code OPCODE}, the size of the player list, and each player name.<br>
	 * If a player name is {@code null}, it writes an empty string instead.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeShort(_players.size());
		for (String p : _players)
		{
			writeString((p != null) ? p : "");
		}
	}
}
