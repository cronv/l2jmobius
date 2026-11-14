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
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to synchronize the current state of a {@code Castle} with the client.<br>
 * It provides information regarding the active {@code CastleSide} and other relevant siege data.
 * @author UnAfraid
 */
public class ExCastleState extends ServerPacket
{
	private final int _castleId;
	private final CastleSide _castleSide;
	
	/**
	 * Creates a new {@code ExCastleState} packet.<br>
	 * This constructor extracts the ID and side from a {@code Castle} object.
	 * @param castle The {@code Castle} instance to use for data.
	 */
	public ExCastleState(Castle castle)
	{
		_castleId = castle.getResidenceId();
		_castleSide = castle.getSide();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CASTLE_STATE.writeId(this, buffer);
		buffer.writeInt(_castleId);
		buffer.writeInt(_castleSide.ordinal());
	}
}
