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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.Collection;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends a list of skills that can be acquired by a specific character class.<br>
 * It is used to populate the skill selection interface for the client.
 * @author UnAfraid
 */
public class ExAcquirableSkillListByClass extends ServerPacket
{
	private final Collection<SkillLearn> _learnable;
	private final AcquireSkillType _type;
	
	/**
	 * Creates a new {@code ExAcquirableSkillListByClass} packet.<br>
	 * This packet contains skills that can be learned based on a specific class and acquisition type.
	 * @param learnable The collection of {@code SkillLearn} objects to include in the list.
	 * @param type The {@code AcquireSkillType} used to filter the skills.
	 */
	public ExAcquirableSkillListByClass(Collection<SkillLearn> learnable, AcquireSkillType type)
	{
		_learnable = learnable;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ACQUIRABLE_SKILL_LIST_BY_CLASS.writeId(this, buffer);
		buffer.writeShort(_type.getId());
		buffer.writeShort(_learnable.size());
		for (SkillLearn skill : _learnable)
		{
			buffer.writeInt(skill.getSkillId());
			final int skillLevel = skill.getSkillLevel();
			buffer.writeShort(skillLevel);
			buffer.writeShort(skillLevel);
			buffer.writeByte(skill.getGetLevel());
			buffer.writeLong(skill.getLevelUpSp());
			buffer.writeByte(skill.getRequiredItems().size());
			if (_type == AcquireSkillType.SUBPLEDGE)
			{
				buffer.writeShort(0);
			}
		}
	}
}
