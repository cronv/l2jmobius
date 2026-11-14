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
 * This packet is sent to the client to initiate an Ex Duel request.<br>
 * It informs the player that a duel challenge has been received.
 * @author KenM
 */
public class ExDuelAskStart extends ServerPacket
{
	private final String _requestorName;
	private final int _partyDuel;
	
	/**
	 * This constructor initializes a new {@code ExDuelAskStart} packet.<br>
	 * It sets the name of the player requesting the duel.<br>
	 * It also stores the specific party duel ID.
	 * @param requestor The name of the player who started the request.
	 * @param partyDuel The unique identifier for the party duel.
	 */
	public ExDuelAskStart(String requestor, int partyDuel)
	{
		_requestorName = requestor;
		_partyDuel = partyDuel;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DUEL_ASK_START.writeId(this, buffer);
		buffer.writeString(_requestorName);
		buffer.writeInt(_partyDuel);
	}
}
