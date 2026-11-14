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
import org.l2jmobius.gameserver.entity.groups.PartyDistributionType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to request permission to join a party.<br>
 * It allows the server to handle group membership requests from players.
 */
public class AskJoinParty extends ServerPacket
{
	private final String _requestorName;
	private final PartyDistributionType _partyDistributionType;
	
	/**
	 * Creates a new {@code AskJoinParty} packet.<br>
	 * This packet is used to handle requests for joining a party.
	 * @param requestorName The name of the player requesting to join.
	 * @param partyDistributionType The type of distribution requested by the player.
	 */
	public AskJoinParty(String requestorName, PartyDistributionType partyDistributionType)
	{
		_requestorName = requestorName;
		_partyDistributionType = partyDistributionType;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.ASK_JOIN_PARTY.writeId(this, buffer);
		buffer.writeString(_requestorName);
		buffer.writeInt(_partyDistributionType.getId());
	}
}
