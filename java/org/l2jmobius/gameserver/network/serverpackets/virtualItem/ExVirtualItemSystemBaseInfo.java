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
package org.l2jmobius.gameserver.network.serverpackets.virtualItem;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.IllusoryEquipmentConfig;
import org.l2jmobius.gameserver.data.xml.VirtualItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This class handles the transmission of base information for virtual items to the client.<br>
 * It provides essential data required by {@code ExVirtualItemSystem} to initialize item properties.
 * @author CostyKiller
 */
public class ExVirtualItemSystemBaseInfo extends ServerPacket
{
	private final Player _player;
	private final int _illusoryPointsAcquired;
	private final int _illusoryPointsUsed;
	private final int _illusoryPointsBonus;
	private final int _illusoryPointsLimit;
	private final int _virtualItemEventEnd;
	
	/**
	 * Creates a new {@code ExVirtualItemSystemBaseInfo} packet for a specific player.<br>
	 * This constructor initializes the points acquired and used by the {@code Player}.<br>
	 * It also calculates the remaining time for the virtual item event.
	 * @param player The {@code Player} object associated with this packet.
	 */
	public ExVirtualItemSystemBaseInfo(Player player)
	{
		_player = player;
		_illusoryPointsAcquired = _player.getVariables().getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0);
		_illusoryPointsUsed = _player.getVariables().getInt(PlayerVariables.ILLUSORY_POINTS_USED, 0);
		_illusoryPointsBonus = _player.getVariables().getInt(PlayerVariables.ILLUSORY_POINTS_BONUS, 0);
		_illusoryPointsLimit = IllusoryEquipmentConfig.getPointsLimit(_player.getLevel());
		_virtualItemEventEnd = VirtualItemData.getInstance().getEventRemainingTime();
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_VIRTUALITEM_SYSTEM_BASE_INFO.writeId(this, buffer);
		buffer.writeInt(_virtualItemEventEnd);
		buffer.writeInt(_illusoryPointsAcquired); // Total Illusory Points acquired
		buffer.writeInt(_illusoryPointsUsed); // Total Illusory Points used
		buffer.writeInt(_illusoryPointsBonus); // var int nVISMaxPoint; the bonus the window shows beside the cap.
		buffer.writeInt(_illusoryPointsLimit); // var int nTotalVISMaxPoint; the cap of the character level.
		buffer.writeByte(0); // 563 - bResetAllEquipment
	}
}
