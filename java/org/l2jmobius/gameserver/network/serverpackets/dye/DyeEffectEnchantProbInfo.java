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
package org.l2jmobius.gameserver.network.serverpackets.dye;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet provides information regarding the probability of applying an enchantment effect to a dye.<br>
 * It is sent to the client to display relevant success rates for dyeing items.
 * @author CostyKiller
 */
public class DyeEffectEnchantProbInfo extends ServerPacket
{
	private final int _category;
	private final int _slotId;
	
	/**
	 * Creates a new {@code DyeEffectEnchantProbInfo} object.<br>
	 * This constructor initializes the dye effect data for a specific item.
	 * @param category The category of the dye effect.
	 * @param slotId The unique identifier for the item slot.
	 */
	public DyeEffectEnchantProbInfo(int category, int slotId)
	{
		_category = category;
		_slotId = slotId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DYEEFFECT_ENCHANT_PROB_INFO.writeId(this, buffer);
		buffer.writeInt(_category);
		buffer.writeInt(_slotId);
		buffer.writeInt((int) RatesConfig.DYE_ENCHANT_NORMAL_SKILL_CHANCE);
		buffer.writeInt((int) RatesConfig.DYE_ENCHANT_HIDDEN_SKILL_CHANCE);
	}
}
