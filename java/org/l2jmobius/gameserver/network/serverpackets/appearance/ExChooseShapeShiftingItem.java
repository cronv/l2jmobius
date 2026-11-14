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
package org.l2jmobius.gameserver.network.serverpackets.appearance;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceStone;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceTargetType;
import org.l2jmobius.gameserver.entity.item.appearance.AppearanceType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to handle the selection of a shape-shifting item.<br>
 * It allows the player to choose which specific appearance they want to apply from an available list.
 * @author UnAfraid
 */
public class ExChooseShapeShiftingItem extends ServerPacket
{
	private final AppearanceType _type;
	private final AppearanceTargetType _targetType;
	private final int _itemId;
	
	/**
	 * Creates a new {@code ExChooseShapeShiftingItem} packet.<br>
	 * This constructor extracts data from an {@code AppearanceStone}.<br>
	 * It sets the item type, target type, and ID based on the stone's properties.
	 * @param stone The {@code AppearanceStone} used to populate the packet data.
	 */
	public ExChooseShapeShiftingItem(AppearanceStone stone)
	{
		_type = stone.getType();
		_targetType = stone.getTargetTypes().size() > 1 ? AppearanceTargetType.ALL : stone.getTargetTypes().stream().findFirst().get();
		_itemId = stone.getId();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHOOSE_SHAPE_SHIFTING_ITEM.writeId(this, buffer);
		buffer.writeInt(_targetType != null ? _targetType.ordinal() : 0);
		buffer.writeInt(_type != null ? _type.ordinal() : 0);
		buffer.writeInt(_itemId);
	}
}
