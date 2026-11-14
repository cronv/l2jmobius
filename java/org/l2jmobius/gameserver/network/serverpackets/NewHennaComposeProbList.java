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
package org.l2jmobius.gameserver.network.serverpackets;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends a list of probabilities for composing {@code Henna} items to the client.<br>
 * It is used to update the UI with the success rates of various {@code CombinationHenna} recipes.
 * @author Mobius
 */
public class NewHennaComposeProbList extends ServerPacket
{
	/**
	 * Creates a new instance of the {@code NewHennaComposeProbList} packet.<br>
	 * This constructor initializes the object with default values.
	 */
	public NewHennaComposeProbList()
	{
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_NEW_HENNA_COMPOSE_PROB_LIST.writeId(this, buffer);
		
		// array<_HennaComposeProbInfo> vHennaComposeProbInfoList
		buffer.writeInt(0);
		
		// The HennaComposeProbInfo structure contains an integer HennaId and an array of ProbInfo objects named vProbInfoList.
		
		// The ProbInfo structure contains an item class ID and a probability.
	}
}
