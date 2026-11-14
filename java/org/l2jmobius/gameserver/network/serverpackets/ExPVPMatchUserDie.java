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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCArena;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client when a user dies during an {@code UCArena} match.<br>
 * It informs the game client to update the UI and state for that specific player.
 */
public class ExPVPMatchUserDie extends ServerPacket
{
	private final int _blueKills;
	private final int _redKills;
	
	/**
	 * Creates a new {@code ExPVPMatchUserDie} packet.<br>
	 * This method extracts the kill counts from the provided {@code UCArena}.<br>
	 * It assigns the first team's kills to red and the second team's kills to blue.
	 * @param arena The {@code UCArena} instance containing the match data.
	 */
	public ExPVPMatchUserDie(UCArena arena)
	{
		_redKills = arena.getTeams()[0].getKillCount();
		_blueKills = arena.getTeams()[1].getKillCount();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PVPMATCH_USER_DIE.writeId(this, buffer);
		buffer.writeInt(_redKills);
		buffer.writeInt(_blueKills);
	}
}
