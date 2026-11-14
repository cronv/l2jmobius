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
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * Handles chat messages specifically for the Party Room Commander channel.<br>
 * This class processes commands and interactions within this specific chat context.
 * @author durgus, Mobius
 */
public class ChatPartyRoomCommander implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.PARTYROOM_COMMANDER,
	};
	
	/**
	 * Handles chat messages sent in the party room commander channel.<br>
	 * This method checks if the {@code activeChar} is the leader of a command channel.<br>
	 * It validates permissions and then broadcasts the message to other members.
	 * @param type The {@code ChatType} of the message being sent.
	 * @param activeChar The {@code Player} who is sending the message.
	 * @param target The target of the message, which may be null.
	 * @param text The actual content of the chat message.
	 */
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text)
	{
		if (activeChar.isInParty() && activeChar.getParty().isInCommandChannel() && activeChar.getParty().getCommandChannel().getLeader().equals(activeChar))
		{
			// The command channel chat only works once the channel is activated by five parties.
			if (activeChar.getParty().getCommandChannel().getParties().size() < 5)
			{
				activeChar.sendPacket(SystemMessageId.THE_COMMAND_CHANNEL_IS_ACTIVATED_ONLY_WHEN_THERE_ARE_AT_LEAST_5_PARTIES_PARTICIPATING);
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
			
			activeChar.getParty().getCommandChannel().broadcastCreatureSay(new CreatureSay(activeChar, type, activeChar.getName(), text), activeChar);
		}
	}
	
	/**
	 * Retrieves the list of supported chat types.<br>
	 * This method returns all {@code ChatType} values handled by this class.
	 * @return an array of {@code ChatType} objects.
	 */
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
