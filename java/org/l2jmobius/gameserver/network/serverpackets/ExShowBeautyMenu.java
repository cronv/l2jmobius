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
 * This packet is sent to the client to display the beauty menu.<br>
 * It allows players to view and interact with cosmetic options.
 * @author Sdw
 */
public class ExShowBeautyMenu extends ServerPacket
{
	// TODO: Enum
	public static final int MODIFY_APPEARANCE = 0;
	public static final int RESTORE_APPEARANCE = 1;
	
	private final Player _player;
	private final int _type;
	
	/**
	 * This constructor initializes a new {@code ExShowBeautyMenu} packet.<br>
	 * It sets the target {@code Player} and the menu {@code type}.
	 * @param player The {@code Player} who will receive this packet.
	 * @param type The specific beauty menu type to display.
	 */
	public ExShowBeautyMenu(Player player, int type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_BEAUTY_MENU.writeId(this, buffer);
		buffer.writeInt(_type);
		buffer.writeInt(_player.getVisualHair());
		buffer.writeInt(_player.getVisualHairColor());
		buffer.writeInt(_player.getVisualFace());
	}
}
