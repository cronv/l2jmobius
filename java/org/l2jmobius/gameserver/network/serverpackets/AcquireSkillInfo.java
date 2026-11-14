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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends information about a skill that a player has acquired.<br>
 * It is used to notify the client when a new {@code CommonSkill} is learned.
 * @author Mobius
 */
public class AcquireSkillInfo extends ServerPacket
{
	private final Player _player;
	private final AcquireSkillType _type;
	private final int _id;
	private final int _level;
	private final long _spCost;
	private final List<Req> _reqs;
	
	/**
	 * Private class containing learning skill requisites.
	 */
	private static class Req
	{
		public int itemId;
		public long count;
		public int type;
		public int unk;
		
		public Req(int pType, int pItemId, long itemCount, int pUnk)
		{
			itemId = pItemId;
			type = pType;
			count = itemCount;
			unk = pUnk;
		}
	}
	
	/**
	 * Creates a new {@code AcquireSkillInfo} packet for a specific player.<br>
	 * This constructor initializes the skill data and required items.
	 * @param player The {@code Player} who is learning the skill.
	 * @param skillType The {@code AcquireSkillType} used to learn the skill.
	 * @param skillLearn The {@code SkillLearn} object containing skill details.
	 */
	public AcquireSkillInfo(Player player, AcquireSkillType skillType, SkillLearn skillLearn)
	{
		_player = player;
		_id = skillLearn.getSkillId();
		_level = skillLearn.getSkillLevel();
		_spCost = skillLearn.getLevelUpSp();
		_type = skillType;
		_reqs = new ArrayList<>();
		if ((skillType != AcquireSkillType.PLEDGE) || PlayerConfig.LIFE_CRYSTAL_NEEDED)
		{
			for (List<ItemHolder> item : skillLearn.getRequiredItems())
			{
				if (!PlayerConfig.DIVINE_SP_BOOK_NEEDED && (_id == CommonSkill.DIVINE_INSPIRATION.getId()))
				{
					continue;
				}
				
				_reqs.add(new Req(99, item.get(0).getId(), item.get(0).getCount(), 50));
			}
		}
	}
	
	/**
	 * Creates a new {@code AcquireSkillInfo} packet for a specific skill.<br>
	 * This constructor initializes the required items and skill costs.
	 * @param player The {@code Player} who is learning the skill.
	 * @param skillType The {@code AcquireSkillType} of the skill.
	 * @param skillLearn The {@code SkillLearn} object containing skill details.
	 * @param sp The amount of skill points required to learn the skill.
	 */
	public AcquireSkillInfo(Player player, AcquireSkillType skillType, SkillLearn skillLearn, int sp)
	{
		_player = player;
		_id = skillLearn.getSkillId();
		_level = skillLearn.getSkillLevel();
		_spCost = sp;
		_type = skillType;
		_reqs = new ArrayList<>();
		for (List<ItemHolder> item : skillLearn.getRequiredItems())
		{
			_reqs.add(new Req(99, item.get(0).getId(), item.get(0).getCount(), 50));
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.ACQUIRE_SKILL_INFO.writeId(this, buffer);
		buffer.writeInt(_player.getReplacementSkill(_id));
		buffer.writeInt(_level);
		buffer.writeLong(_spCost);
		buffer.writeInt(_type.getId());
		buffer.writeInt(_reqs.size());
		for (Req temp : _reqs)
		{
			buffer.writeInt(temp.type);
			buffer.writeInt(temp.itemId);
			buffer.writeLong(temp.count);
			buffer.writeInt(temp.unk);
		}
	}
}
