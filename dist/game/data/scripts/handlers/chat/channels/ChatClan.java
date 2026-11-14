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
package handlers.chat.channels;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * This class handles the logic for clan-specific chat messages.<br>
 * It processes incoming {@code ChatType} requests to ensure only members of a specific clan can communicate within that channel.
 * @author durgus
 */
public class ChatClan implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.CLAN,
	};
	
	/**
	 * Handles chat messages sent within a clan.<br>
	 * It checks if the {@code Player} is in a clan and not banned from chatting.<br>
	 * If all conditions are met, it broadcasts the message to all online members of the clan.
	 * @param type The {@code ChatType} of the message being sent.
	 * @param activeChar The {@code Player} who is sending the message.
	 * @param target The name or ID of the chat target.
	 * @param text The actual content of the message.
	 */
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text)
	{
		if (activeChar.getClan() == null)
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_CLAN);
			return;
		}
		
		if (activeChar.isChatBanned() && GeneralConfig.BAN_CHAT_CHANNELS.contains(type))
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_INCREASE_EVEN_FURTHER_CHATTING_BAN_TIME_REMAINING_S1_SECONDS);
			return;
		}
		
		if (GeneralConfig.JAIL_DISABLE_CHAT && activeChar.isJailed() && !activeChar.isGM())
		{
			activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED);
			return;
		}
		
		activeChar.getClan().broadcastCSToOnlineMembers(new CreatureSay(activeChar, type, activeChar.getName(), text), activeChar);
	}
	
	/**
	 * Retrieves the list of chat types supported by this handler.<br>
	 * This method returns a static array of {@code ChatType}.
	 * @return an array of {@code ChatType} objects.
	 */
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
