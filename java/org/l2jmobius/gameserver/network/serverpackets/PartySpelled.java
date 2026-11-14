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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to clients to notify them when a party member casts a spell.<br>
 * It contains the details of the {@code Skill} used and the affected {@code Creature}.
 * @author Mobius
 */
public class PartySpelled extends ServerPacket
{
	private final List<BuffInfo> _effects = new ArrayList<>();
	private final List<Skill> _effects2 = new ArrayList<>();
	private final Creature _creature;
	
	/**
	 * Creates a new {@code PartySpelled} packet for a specific creature.<br>
	 * This constructor initializes the packet with the provided {@code Creature} object.
	 * @param creature The {@code Creature} that cast the spell.
	 */
	public PartySpelled(Creature creature)
	{
		_creature = creature;
	}
	
	/**
	 * Adds a new buff effect to the list of effects.<br>
	 * This method updates the internal collection of {@code BuffInfo}.
	 * @param info The {@code BuffInfo} object to be added.
	 */
	public void addSkill(BuffInfo info)
	{
		_effects.add(info);
	}
	
	/**
	 * Adds a new {@code Skill} to the list of effects.<br>
	 * This method updates the internal collection used for party spells.
	 * @param skill The {@code Skill} object to be added.
	 */
	public void addSkill(Skill skill)
	{
		_effects2.add(skill);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.PARTY_SPELLED_INFO.writeId(this, buffer);
		buffer.writeInt(_creature.isServitor() ? 2 : _creature.isPet() ? 1 : 0);
		buffer.writeInt(_creature.getObjectId());
		buffer.writeInt(_effects.size() + _effects2.size());
		for (BuffInfo info : _effects)
		{
			if ((info != null) && info.isInUse())
			{
				buffer.writeInt(info.getSkill().getDisplayId());
				buffer.writeShort(info.getSkill().getDisplayLevel());
				buffer.writeShort(0); // Sub level
				buffer.writeInt(info.getSkill().getAbnormalType().getClientId());
				writeOptionalInt(info.getTime(), buffer);
			}
		}
		
		for (Skill skill : _effects2)
		{
			if (skill != null)
			{
				buffer.writeInt(skill.getDisplayId());
				buffer.writeShort(skill.getDisplayLevel());
				buffer.writeShort(0); // Sub level
				buffer.writeInt(skill.getAbnormalType().getClientId());
				buffer.writeShort(-1);
			}
		}
	}
}
