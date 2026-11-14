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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.script.NpcLogListHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to send a list of NPC logs to the client.<br>
 * It contains data related to {@code NpcLogListHolder} for quest-related NPCs.
 * @author UnAfraid
 */
public class ExQuestNpcLogList extends ServerPacket
{
	private final int _questId;
	private final List<NpcLogListHolder> _npcLogList = new ArrayList<>();
	
	/**
	 * Creates a new {@code ExQuestNpcLogList} for a specific quest.<br>
	 * This constructor initializes the internal quest identifier.
	 * @param questId The unique ID of the quest.
	 */
	public ExQuestNpcLogList(int questId)
	{
		_questId = questId;
	}
	
	/**
	 * Adds a new NPC entry to the quest log list.<br>
	 * This method creates a new {@code NpcLogListHolder} and stores it in the internal list.
	 * @param npcId The unique identifier for the NPC.
	 * @param count The number of times this NPC appears in the log.
	 */
	public void addNpc(int npcId, int count)
	{
		_npcLogList.add(new NpcLogListHolder(npcId, false, count));
	}
	
	/**
	 * Adds a specific NPC string to the log list.<br>
	 * This method creates a new {@code NpcLogListHolder} entry.
	 * @param npcStringId The unique identifier for the NPC string.
	 * @param count The number of times this NPC appears in the log.
	 */
	public void addNpcString(NpcStringId npcStringId, int count)
	{
		_npcLogList.add(new NpcLogListHolder(npcStringId.getId(), true, count));
	}
	
	/**
	 * Adds a new entry to the internal list of NPC logs.<br>
	 * This method updates the {@code _npcLogList} collection.
	 * @param holder The {@code NpcLogListHolder} object to be added.
	 */
	public void add(NpcLogListHolder holder)
	{
		_npcLogList.add(holder);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_QUEST_NPC_LOG_LIST.writeId(this, buffer);
		buffer.writeInt(_questId);
		buffer.writeByte(_npcLogList.size());
		for (NpcLogListHolder holder : _npcLogList)
		{
			final boolean isNpcString = holder.isNpcString();
			buffer.writeInt(isNpcString ? holder.getId() : holder.getId() + 1000000);
			buffer.writeByte(isNpcString);
			buffer.writeInt(holder.getCount());
		}
	}
}
