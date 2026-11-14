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
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;

/**
 * This class handles the {@code NpcQuestHtmlMessage} server packet.<br>
 * It is used to send HTML-formatted quest messages from an NPC to the client.
 * @author HorridoJoho
 */
public class NpcQuestHtmlMessage extends AbstractHtmlPacket
{
	private final int _questId;
	
	/**
	 * Creates a new {@code NpcQuestHtmlMessage} packet.<br>
	 * This packet is used to display HTML content for a specific quest.
	 * @param npcObjId The unique identifier of the NPC object.
	 * @param questId The unique identifier of the quest.
	 */
	public NpcQuestHtmlMessage(int npcObjId, int questId)
	{
		super(npcObjId);
		_questId = questId;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_NPC_QUEST_HTML_MESSAGE.writeId(this, buffer);
		buffer.writeInt(getNpcObjId());
		buffer.writeString(getHtml());
		buffer.writeInt(_questId);
	}
	
	/**
	 * Retrieves the scope for this HTML message.<br>
	 * This identifies how the message should be displayed to the player.
	 * @return the {@code HtmlActionScope} value associated with this packet.
	 */
	@Override
	public HtmlActionScope getScope()
	{
		return HtmlActionScope.NPC_QUEST_HTML;
	}
}
