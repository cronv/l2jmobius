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
package org.l2jmobius.gameserver.network.serverpackets.homunculus;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.data.xml.HomunculusCreationData;
import org.l2jmobius.gameserver.data.xml.HomunculusData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends a list of probabilities for creating homunculi to the client.<br>
 * It contains data used by {@code HomunculusCreationData} to determine success rates.
 * @author Index
 */
public class ExHomunculusCreateProbabilityList extends ServerPacket
{
	private final Player _player;
	
	/**
	 * This constructor initializes a new {@code ExHomunculusCreateProbabilityList} packet.<br>
	 * It stores the {@code Player} who will receive the data.
	 * @param player The {@code Player} object associated with this packet.
	 */
	public ExHomunculusCreateProbabilityList(Player player)
	{
		_player = player;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		if (_player == null)
		{
			return;
		}
		
		ServerPackets.EX_HOMUNCULUS_CREATE_PROB_LIST.writeId(this, buffer);
		buffer.writeInt(HomunculusCreationData.getInstance().getDefaultTemplate().getCreationChance().size());
		for (int type = 0; type < 3; type++)
		{
			for (Double[] homunculusChance : HomunculusCreationData.getInstance().getDefaultTemplate().getCreationChance())
			{
				final int chanceId = homunculusChance[0].intValue();
				if (HomunculusData.getInstance().getTemplate(chanceId).getType() == type)
				{
					buffer.writeInt(chanceId);
					buffer.writeInt((int) (homunculusChance[1] * 1000000));
				}
			}
		}
	}
}
