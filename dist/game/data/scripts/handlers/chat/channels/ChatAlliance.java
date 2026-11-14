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
package handlers.chat.channels;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * Handles chat messages sent through the alliance channel.<br>
 * This class manages communication between players who belong to the same alliance.<br>
 * It ensures that messages are correctly processed and distributed to eligible members.
 */
public class ChatAlliance implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.ALLIANCE,
	};
	
	/**
	 * Handles chat messages sent within an alliance.<br>
	 * It checks if the player is in a valid alliance and not banned from chatting.<br>
	 * It also verifies if the player is currently jailed or restricted by configuration.<br>
	 * If all checks pass, it broadcasts the message to all online members of the alliance.
	 * @param type The {@code ChatType} of the message being sent.
	 * @param activeChar The {@code Player} who is sending the message.
	 * @param target The name of the chat target.
	 * @param text The content of the chat message.
	 */
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text)
	{
		final Clan clan = activeChar.getClan();
		if ((clan == null) || (clan.getAllyId() == 0))
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_AN_ALLIANCE);
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
		
		clan.broadcastToOnlineAllyMembers(new CreatureSay(activeChar, type, activeChar.getName(), text));
	}
	
	/**
	 * Retrieves the list of chat types supported by this handler.<br>
	 * This method returns an array of {@code ChatType}.
	 * @return An array containing the allowed {@code ChatType} values.
	 */
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
