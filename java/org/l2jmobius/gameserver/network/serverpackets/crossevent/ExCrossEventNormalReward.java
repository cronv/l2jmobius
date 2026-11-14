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
package org.l2jmobius.gameserver.network.serverpackets.crossevent;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to handle normal rewards for cross-server events.<br>
 * It contains the necessary data to display and process these specific rewards.
 * @author Smoto
 */
public class ExCrossEventNormalReward extends ServerPacket
{
	private final int _vertical;
	private final int _horizontal;
	private final int _rewardAmount;
	
	/**
	 * Creates a new {@code ExCrossEventNormalReward} packet.<br>
	 * This packet handles the rewards for a normal cross event.<br>
	 * It stores the vertical and horizontal positions along with the amount.
	 * @param vertical The vertical coordinate of the reward.
	 * @param horizontal The horizontal coordinate of the reward.
	 * @param rewardAmount The total amount of the reward.
	 */
	public ExCrossEventNormalReward(int vertical, int horizontal, int rewardAmount)
	{
		_vertical = vertical;
		_horizontal = horizontal;
		_rewardAmount = rewardAmount;
	}
	
	@Override
	protected void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CROSS_EVENT_NORMAL_REWARD.writeId(this, buffer);
		
		buffer.writeByte(1); // on / off ??
		buffer.writeInt(_vertical); // Select 0-3 vertical.
		buffer.writeInt(_horizontal); // Select 0 - 3 horizontal.
		buffer.writeInt(_rewardAmount); // Reward amount (need to be more then 1 to show received reward.
	}
}
