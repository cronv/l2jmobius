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
package org.l2jmobius.gameserver.network.serverpackets.enchant.single;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet contains a list of probabilities for items that can be changed during an enchantment.<br>
 * It is used to inform the client about potential outcomes when interacting with enchanted targets.
 * @author Serenitty
 */
public class ExChangedEnchantTargetItemProbList extends ServerPacket
{
	private final List<EnchantProbInfo> _probList;
	
	/**
	 * Creates a new {@code ExChangedEnchantTargetItemProbList} packet.<br>
	 * This constructor initializes the list of enchantment probabilities.
	 * @param probList The list of {@code EnchantProbInfo} objects to include in the packet.
	 */
	public ExChangedEnchantTargetItemProbList(List<EnchantProbInfo> probList)
	{
		_probList = probList;
	}
	
	/**
	 * Creates a new packet containing a single enchantment probability.<br>
	 * This constructor wraps the provided {@code EnchantProbInfo} into a list.
	 * @param probInfo The {@code EnchantProbInfo} to be included in the list.
	 */
	public ExChangedEnchantTargetItemProbList(EnchantProbInfo probInfo)
	{
		_probList = new ArrayList<>();
		_probList.add(probInfo);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CHANGED_ENCHANT_TARGET_ITEM_PROB_LIST.writeId(this, buffer);
		
		buffer.writeInt(_probList.size()); // vProbList;
		for (EnchantProbInfo info : _probList)
		{
			buffer.writeInt(info.itemObjId); // nItemServerId;
			buffer.writeInt(info.totalSuccessProb);// nTotalSuccessProbPermyriad;
			buffer.writeInt(info.baseProb);// nBaseProbPermyriad;
			buffer.writeInt(info.supportProb);// nSupportProbPermyriad;
			buffer.writeInt(info.itemSkillProb);// nItemSkillProbPermyriad;
		}
	}
	
	public static class EnchantProbInfo
	{
		int itemObjId;
		int totalSuccessProb;
		int baseProb;
		int supportProb;
		int itemSkillProb;
		
		public EnchantProbInfo(int itemObjId, int totalSuccessProb, int baseProb, int supportProb, int itemSkillProb)
		{
			this.itemObjId = itemObjId;
			this.totalSuccessProb = Math.min(10000, totalSuccessProb);
			this.baseProb = baseProb;
			this.supportProb = supportProb;
			this.itemSkillProb = itemSkillProb;
		}
	}
}
