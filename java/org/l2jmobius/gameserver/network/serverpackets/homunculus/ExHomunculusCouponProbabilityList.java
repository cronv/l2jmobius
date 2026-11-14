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
import org.l2jmobius.gameserver.mechanics.homunculus.HomunculusCreationTemplate;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends a list of probabilities for the {@code Homunculus} coupon system to the client.<br>
 * It allows the game client to display the correct chances associated with specific homunculus types.
 * @author Index
 */
public class ExHomunculusCouponProbabilityList extends ServerPacket
{
	private final Player _player;
	private final int _couponId;
	
	/**
	 * Creates a new packet to send homunculus coupon probabilities.<br>
	 * This constructor initializes the data for a specific {@code Player}.<br>
	 * It also sets the required {@code couponId}.
	 * @param player The {@code Player} who will receive this packet.
	 * @param couponId The unique identifier for the coupon.
	 */
	public ExHomunculusCouponProbabilityList(Player player, int couponId)
	{
		_player = player;
		_couponId = couponId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		if (_player == null)
		{
			return;
		}
		
		final HomunculusCreationTemplate creationTemplate = HomunculusCreationData.getInstance().getTemplateByItemId(_couponId);
		if (creationTemplate == null)
		{
			return;
		}
		
		ServerPackets.EX_HOMUNCULUS_COUPON_PROB_LIST.writeId(this, buffer);
		buffer.writeInt(_couponId);
		buffer.writeInt(creationTemplate.getCreationChance().size());
		for (int type = 0; type < 3; type++)
		{
			for (Double[] homunculusChance : creationTemplate.getCreationChance())
			{
				if (HomunculusData.getInstance().getTemplate(homunculusChance[0].intValue()).getType() == type)
				{
					buffer.writeInt(homunculusChance[0].intValue());
					buffer.writeInt((int) (homunculusChance[1] * 1000000));
				}
			}
		}
	}
}
