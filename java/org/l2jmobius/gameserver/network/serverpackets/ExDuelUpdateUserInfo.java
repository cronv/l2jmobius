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
 * This packet updates the user information for an active duel.<br>
 * It synchronizes specific player data between the server and the client during a duel session.
 * @author KenM
 */
public class ExDuelUpdateUserInfo extends ServerPacket
{
	private final Player _player;
	
	/**
	 * Updates the user information for a specific duel.<br>
	 * This packet sends data to the client about the {@code Player}.
	 * @param player The {@code Player} whose information needs to be updated.
	 */
	public ExDuelUpdateUserInfo(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DUEL_UPDATE_USER_INFO.writeId(this, buffer);
		buffer.writeString(_player.getName());
		buffer.writeInt(_player.getObjectId());
		buffer.writeInt(_player.getPlayerClass().getId());
		buffer.writeInt(_player.getLevel());
		buffer.writeInt((int) _player.getCurrentHp());
		buffer.writeInt((int) _player.getMaxHp());
		buffer.writeInt((int) _player.getCurrentMp());
		buffer.writeInt(_player.getMaxMp());
		buffer.writeInt((int) _player.getCurrentCp());
		buffer.writeInt(_player.getMaxCp());
	}
}
