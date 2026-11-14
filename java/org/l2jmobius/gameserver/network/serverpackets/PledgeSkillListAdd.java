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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is sent to the client to add a new skill to the pledge list.<br>
 * It updates the user's available skills for their pledges.
 * @author -Wooden-
 */
public class PledgeSkillListAdd extends ServerPacket
{
	private final int _id;
	private final int _level;
	
	/**
	 * This constructor creates a new {@code PledgeSkillListAdd} packet.<br>
	 * It initializes the skill ID and its corresponding level.
	 * @param id The unique identifier for the pledge skill.
	 * @param level The current level of the pledge skill.
	 */
	public PledgeSkillListAdd(int id, int level)
	{
		_id = id;
		_level = level;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PLEDGE_SKILL_ADD.writeId(this, buffer);
		buffer.writeInt(_id);
		buffer.writeInt(_level);
	}
}
