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
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to reset a specific list of items or entities.<br>
 * It handles the synchronization of data when a collection needs to be cleared or refreshed.
 * @author Sdw
 */
public class ExResponseResetList extends ServerPacket
{
	private final Player _player;
	
	/**
	 * This packet handles the reset list response.<br>
	 * It stores the {@code Player} who triggered the action.
	 * @param player The {@code Player} instance associated with this request.
	 */
	public ExResponseResetList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_RESPONSE_RESET_LIST.writeId(this, buffer);
		
		buffer.writeLong(_player.getAdena());
		buffer.writeLong(_player.getBeautyTickets());
		
		final PlayerAppearance appearance = _player.getAppearance();
		buffer.writeInt(appearance.getHairStyle());
		buffer.writeInt(appearance.getHairColor());
		buffer.writeInt(appearance.getFace());
	}
}
