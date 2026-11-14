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
import org.l2jmobius.gameserver.managers.PetitionManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles messages sent within the petition chat channel.<br>
 * It manages how players interact and communicate in this specific context.
 * @author durgus
 */
public class ChatPetition implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.PETITION_PLAYER,
		ChatType.PETITION_GM,
	};
	
	/**
	 * Handles chat messages sent within a petition.<br>
	 * It checks if the {@code activeChar} is allowed to speak.<br>
	 * It validates if the player is currently in a consultation.<br>
	 * Finally, it broadcasts the message via {@code PetitionManager}.
	 * @param type The category of the chat message.
	 * @param activeChar The {@code Player} who sent the message.
	 * @param target The recipient of the message.
	 * @param text The content of the message.
	 */
	@Override
	public void onChat(ChatType type, Player activeChar, String target, String text)
	{
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
		
		if (!PetitionManager.getInstance().isPlayerInConsultation(activeChar))
		{
			activeChar.sendPacket(SystemMessageId.YOU_ARE_NOT_IN_THE_GLOBAL_SUPPORT_CHAT);
			return;
		}
		
		PetitionManager.getInstance().sendActivePetitionMessage(activeChar, text);
	}
	
	/**
	 * Retrieves the list of supported chat types.<br>
	 * This method returns all {@code ChatType} values associated with this handler.
	 * @return An array of {@code ChatType} objects.
	 */
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
