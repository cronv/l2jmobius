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
 * This class handles messages sent within a party chat channel.<br>
 * It manages the logic for broadcasting party-specific communications to members.
 * @author durgus
 */
public class ChatParty implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.PARTY,
	};
	
	/**
	 * Handles chat messages sent by a {@code Player}.<br>
	 * It checks if the player is in a party and not banned before broadcasting.
	 * @param type The {@code ChatType} of the message.
	 * @param activeChar The {@code Player} who is sending the message.
	 * @param target The name of the recipient or target.
	 * @param text The content of the chat message.
	 */
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text)
	{
		if (!activeChar.isInParty())
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_A_PARTY);
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
		
		activeChar.getParty().broadcastCreatureSay(new CreatureSay(activeChar, type, activeChar.getName(), text), activeChar);
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
