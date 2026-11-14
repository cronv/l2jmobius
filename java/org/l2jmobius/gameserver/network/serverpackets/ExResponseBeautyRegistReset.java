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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to confirm a beauty registration reset.<br>
 * It informs the player that their previous beauty data has been cleared.
 * @author Sdw
 */
public class ExResponseBeautyRegistReset extends ServerPacket
{
	public static final int FAILURE = 0;
	public static final int SUCCESS = 1;
	public static final int CHANGE = 0;
	public static final int RESTORE = 1;
	
	private final Player _player;
	private final int _type;
	private final int _result;
	
	/**
	 * This method creates a new {@code ExResponseBeautyRegistReset} packet.<br>
	 * It handles the response for resetting beauty registration.
	 * @param player The {@code Player} who triggered the action.
	 * @param type The category of the beauty registration.
	 * @param result The outcome of the operation, such as {@code ExResponseBeautyRegistReset.SUCCESS}.
	 */
	public ExResponseBeautyRegistReset(Player player, int type, int result)
	{
		_player = player;
		_type = type;
		_result = result;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RESPONSE_BEAUTY_REGIST_RESET.writeId(this, buffer);
		buffer.writeLong(_player.getAdena());
		buffer.writeLong(_player.getBeautyTickets());
		buffer.writeInt(_type);
		buffer.writeInt(_result);
		buffer.writeInt(_player.getVisualHair());
		buffer.writeInt(_player.getVisualFace());
		buffer.writeInt(_player.getVisualHairColor());
	}
}
