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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet provides information regarding the system points associated with a virtual item.<br>
 * It is used to synchronize point data between the server and the client for {@code ExVirtualItem} objects.
 * @author CostyKiller
 */
public class ExVirtualItemSystemPointInfo extends ServerPacket
{
	private final Player _player;
	private final int _illusoryPointsAcquired;
	private final int _illusoryPointsUsed;
	private final int _illusoryPointsDiff;
	private final int _illusoryPointsBonus;
	private final int _illusoryPointsLimit;
	
	/**
	 * Creates a new {@code ExVirtualItemSystemPointInfo} packet.<br>
	 * This constructor initializes the point difference and fetches current player statistics.
	 * @param player The {@code Player} object associated with this packet.
	 * @param illusoryPointsDiff The change in illusory points to be sent to the client.
	 */
	public ExVirtualItemSystemPointInfo(Player player, int illusoryPointsDiff)
	{
		_player = player;
		_illusoryPointsDiff = illusoryPointsDiff;
		
		final PlayerVariables variables = _player.getVariables();
		_illusoryPointsAcquired = variables.getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0);
		_illusoryPointsUsed = variables.getInt(PlayerVariables.ILLUSORY_POINTS_USED, 0);
		_illusoryPointsBonus = variables.getInt(PlayerVariables.ILLUSORY_POINTS_BONUS, 0);
		_illusoryPointsLimit = IllusoryEquipmentConfig.getPointsLimit(player.getLevel());
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_VIRTUALITEM_SYSTEM_POINT_INFO.writeId(this, buffer);
		buffer.writeInt(_illusoryPointsDiff); // var int nDiffPoint;
		buffer.writeInt(_illusoryPointsAcquired); // var int nTotalGetVISPoint; Total Illusory Points acquired
		buffer.writeInt(_illusoryPointsUsed); // var int nTotalUsedVISPoint; Total Illusory Points used
		buffer.writeInt(_illusoryPointsBonus); // var int nVISMaxPoint; the bonus the window shows beside the cap.
		buffer.writeInt(_illusoryPointsLimit); // var int nTotalVISMaxPoint; the cap of the character level.
	}
}
