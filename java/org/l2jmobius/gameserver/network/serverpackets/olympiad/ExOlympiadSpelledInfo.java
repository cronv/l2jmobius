/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets.olympiad;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends information about spells cast during an Olympiad event to the client.<br>
 * It contains a list of {@code BuffInfo} objects associated with the active skills.
 * @author godson
 */
public class ExOlympiadSpelledInfo extends ServerPacket
{
	private final int _playerId;
	private final List<BuffInfo> _effects = new ArrayList<>();
	private final List<Skill> _effects2 = new ArrayList<>();
	
	/**
	 * Creates a new {@code ExOlympiadSpelledInfo} packet for a specific player.<br>
	 * This constructor initializes the packet with the unique ID of the provided {@code Player}.
	 * @param player The {@code Player} object used to identify the target.
	 */
	public ExOlympiadSpelledInfo(Player player)
	{
		_playerId = player.getObjectId();
	}
	
	/**
	 * Adds a new {@code BuffInfo} object to the list of effects.<br>
	 * This method updates the internal collection used for olympiad spells.
	 * @param info The {@code BuffInfo} to be added.
	 */
	public void addSkill(BuffInfo info)
	{
		_effects.add(info);
	}
	
	/**
	 * Adds a {@code Skill} to the list of effects.<br>
	 * This method updates the internal collection used for olympiad data.
	 * @param skill The {@code Skill} object to be added.
	 */
	public void addSkill(Skill skill)
	{
		_effects2.add(skill);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_OLYMPIAD_SPELLED_INFO.writeId(this, buffer);
		buffer.writeInt(_playerId);
		buffer.writeInt(_effects.size() + _effects2.size());
		for (BuffInfo info : _effects)
		{
			if ((info != null) && info.isInUse())
			{
				buffer.writeInt(info.getSkill().getDisplayId());
				buffer.writeShort(info.getSkill().getDisplayLevel());
				buffer.writeShort(info.getSkill().getSubLevel()); // Sub level
				buffer.writeInt(info.getSkill().getAbnormalType().getClientId());
				writeOptionalInt(info.getSkill().isAura() ? -1 : info.getTime(), buffer);
			}
		}
		
		for (Skill skill : _effects2)
		{
			if (skill != null)
			{
				buffer.writeInt(skill.getDisplayId());
				buffer.writeShort(skill.getDisplayLevel());
				buffer.writeShort(skill.getSubLevel()); // Sub level
				buffer.writeInt(skill.getAbnormalType().getClientId());
				buffer.writeShort(-1);
			}
		}
	}
}
