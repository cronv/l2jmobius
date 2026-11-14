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
import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.config.custom.FakePlayersConfig;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.managers.FakePlayerChatManager;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles the logic for private whisper messages between players.<br>
 * This class processes incoming {@code ChatType} requests to ensure only the intended recipient receives the message.
 * @author durgus, Mobius
 */
public class ChatWhisper implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.WHISPER
	};
	
	/**
	 * Handles the logic for sending a whisper message between players.<br>
	 * This method checks for bans, jail status, and faction restrictions before delivering the text.<br>
	 * It also manages interactions with fake players and handles silence modes.
	 * @param type The {@code ChatType} of the message being sent.
	 * @param activeChar The {@code Player} who is initiating the chat.
	 * @param target The name of the player intended to receive the message.
	 * @param text The content of the message to be sent.
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
		
		// Return if no target is set.
		if (target == null)
		{
			return;
		}
		
		if (FakePlayersConfig.FAKE_PLAYERS_ENABLED && (FakePlayerData.getInstance().getProperName(target) != null))
		{
			if (FakePlayerData.getInstance().isTalkable(target))
			{
				if (FakePlayersConfig.FAKE_PLAYER_CHAT)
				{
					final String name = FakePlayerData.getInstance().getProperName(target);
					activeChar.sendPacket(new CreatureSay(activeChar, null, "->" + name, type, text));
					FakePlayerChatManager.getInstance().manageChat(activeChar, name, text);
				}
				else
				{
					activeChar.sendPacket(SystemMessageId.THAT_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
				}
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
			}
			return;
		}
		
		// Nobody can whisper while blocking everything.
		if (activeChar.getMessageRefusal())
		{
			activeChar.sendPacket(SystemMessageId.WHILE_BLOCKING_EVERYTHING_WHISPERING_IS_NOT_POSSIBLE);
			return;
		}
		
		final Player receiver = World.getPlayer(target);
		if ((receiver != null) && !receiver.isSilenceMode(activeChar.getObjectId()))
		{
			if (GeneralConfig.JAIL_DISABLE_CHAT && receiver.isJailed() && !activeChar.isGM())
			{
				activeChar.sendMessage("Player is in jail.");
				return;
			}
			
			if (receiver.isChatBanned())
			{
				activeChar.sendPacket(SystemMessageId.THAT_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
				return;
			}
			
			if ((receiver.getClient() == null) || receiver.getClient().isDetached())
			{
				activeChar.sendMessage("Player is in offline mode.");
				return;
			}
			
			if (FactionSystemConfig.FACTION_SYSTEM_ENABLED && FactionSystemConfig.FACTION_SPECIFIC_CHAT && ((activeChar.isGood() && receiver.isEvil()) || (activeChar.isEvil() && receiver.isGood())))
			{
				activeChar.sendMessage("Player belongs to the opposing faction.");
				return;
			}
			
			if ((activeChar.getLevel() < GeneralConfig.MINIMUM_CHAT_LEVEL) && !activeChar.getWhisperers().contains(receiver.getObjectId()) && !activeChar.isGM())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTERS_LV_S1_OR_LOWER_CAN_RESPOND_TO_A_WHISPER_BUT_CANNOT_INITIATE_IT).addInt(GeneralConfig.MINIMUM_CHAT_LEVEL));
				return;
			}
			
			if (!BlockList.isBlocked(receiver, activeChar))
			{
				// Allow receiver to send PMs to this char, which is in silence mode.
				if (PlayerConfig.SILENCE_MODE_EXCLUDE && activeChar.isSilenceMode())
				{
					activeChar.addSilenceModeExcluded(receiver.getObjectId());
				}
				
				receiver.getWhisperers().add(activeChar.getObjectId());
				receiver.sendPacket(new CreatureSay(activeChar, receiver, activeChar.getName(), type, text));
				activeChar.sendPacket(new CreatureSay(activeChar, receiver, "->" + receiver.getName(), type, text));
			}
			else
			{
				activeChar.sendPacket(SystemMessageId.THAT_PERSON_IS_IN_MESSAGE_REFUSAL_MODE);
			}
		}
		else
		{
			activeChar.sendPacket(SystemMessageId.THAT_PLAYER_IS_NOT_ONLINE);
		}
	}
	
	/**
	 * Retrieves the list of supported chat types for this handler.<br>
	 * This method returns an array of {@code ChatType}.
	 * @return An array containing all valid {@code ChatType} values.
	 */
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
