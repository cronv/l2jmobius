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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet handles the visual effect for a normal skill when applied to a dyed item.<br>
 * It informs the client to play the specific enchantment animation associated with the dye.
 * @author CostyKiller, Mobius
 */
public class DyeEffectEnchantNormalSkill extends ServerPacket
{
	private final int _category;
	private final int _slotId;
	private final boolean _success;
	private final int _slotLevel;
	private final int _skillId;
	private final int _skillLevel;
	private final int _challengeCount;
	
	/**
	 * Creates a new {@code DyeEffectEnchantNormalSkill} packet.<br>
	 * This packet handles the results of a normal skill enchantment.
	 * @param category The category of the item or effect.
	 * @param slotId The unique identifier for the item slot.
	 * @param success Whether the enchantment process was successful.
	 * @param slotLevel The level of the item slot.
	 * @param skillId The unique identifier for the skill used.
	 * @param skillLevel The level of the skill used.
	 * @param challengeCount The number of challenges completed.
	 */
	public DyeEffectEnchantNormalSkill(int category, int slotId, boolean success, int slotLevel, int skillId, int skillLevel, int challengeCount)
	{
		_category = category;
		_slotId = slotId;
		_success = success;
		_slotLevel = slotLevel;
		_skillId = skillId;
		_skillLevel = skillLevel;
		_challengeCount = challengeCount;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DYEEFFECT_ENCHANT_NORMALSKILL.writeId(this, buffer);
		buffer.writeInt(_success);
		buffer.writeInt(_category);
		buffer.writeInt(_slotId);
		buffer.writeInt(_slotLevel);
		buffer.writeInt(_skillId);
		buffer.writeInt(_skillLevel);
		buffer.writeInt(_challengeCount);
	}
}
