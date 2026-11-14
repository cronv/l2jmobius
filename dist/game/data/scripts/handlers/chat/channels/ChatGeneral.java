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

import java.util.StringTokenizer;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.handler.IVoicedCommandHandler;
import org.l2jmobius.gameserver.handler.VoicedCommandHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles the logic for the general chat channel.<br>
 * It processes messages sent by players to the global chat area.
 * @author durgus, Mobius
 */
public class ChatGeneral implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.GENERAL,
	};
	
	/**
	 * Processes a chat message sent by a player.<br>
	 * This method handles command parsing, level requirements, and chat bans.<br>
	 * It also manages faction-based visibility for the outgoing message.
	 * @param type The {@code ChatType} of the channel being used.
	 * @param activeChar The {@code Player} who is sending the message.
	 * @param paramsValue A string containing additional parameters for commands.
	 * @param text The raw text content of the chat message.
	 */
	@Override
	public void onChat(ChatType type, Player activeChar, String paramsValue, String text)
	{
		boolean vcdUsed = false;
		if (text.startsWith("."))
		{
			final StringTokenizer st = new StringTokenizer(text);
			final IVoicedCommandHandler vch;
			String command = "";
			String params = paramsValue;
			if (st.countTokens() > 1)
			{
				command = st.nextToken().substring(1);
				params = text.substring(command.length() + 2);
			}
			else
			{
				command = text.substring(1);
			}
			
			vch = VoicedCommandHandler.getInstance().getHandler(command);
			if (vch != null)
			{
				vch.onCommand(command, activeChar, params);
				vcdUsed = true;
			}
			else
			{
				vcdUsed = false;
			}
		}
		
		if (!vcdUsed)
		{
			if (activeChar.isChatBanned() && GeneralConfig.BAN_CHAT_CHANNELS.contains(type))
			{
				activeChar.sendPacket(SystemMessageId.CHATTING_IS_CURRENTLY_PROHIBITED_IF_YOU_TRY_TO_CHAT_BEFORE_THE_PROHIBITION_IS_REMOVED_THE_PROHIBITION_TIME_WILL_INCREASE_EVEN_FURTHER_CHATTING_BAN_TIME_REMAINING_S1_SECONDS);
				return;
			}
			
			if ((activeChar.getLevel() < GeneralConfig.MINIMUM_CHAT_LEVEL) && !activeChar.isGM())
			{
				activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTERS_OF_LV_S1_OR_LOWER_CANNOT_USE_THE_GENERAL_CHAT).addInt(GeneralConfig.MINIMUM_CHAT_LEVEL));
				return;
			}
			
			final String visibleName = activeChar.getAppearance().getVisibleName();
			final CreatureSay cs = new CreatureSay(activeChar, type, visibleName, text);
			final CreatureSay csRandom = new CreatureSay(activeChar, type, visibleName, ChatRandomizer.randomize(text));
			World.forEachVisibleObjectInRange(activeChar, Player.class, 1250, player ->
			{
				if ((player != null) && !BlockList.isBlocked(player, activeChar) && activeChar.isVisibleFor(player))
				{
					if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
					{
						if (FactionSystemConfig.FACTION_SPECIFIC_CHAT)
						{
							if ((activeChar.isGood() && player.isEvil()) || (activeChar.isEvil() && player.isGood()))
							{
								player.sendPacket(csRandom);
							}
							else
							{
								player.sendPacket(cs);
							}
						}
						else
						{
							player.sendPacket(cs);
						}
					}
					else
					{
						player.sendPacket(cs);
					}
				}
			});
			
			activeChar.sendPacket(cs);
		}
	}
	
	/**
	 * Retrieves the list of available chat types.<br>
	 * This method returns all {@code ChatType} categories supported by this handler.
	 * @return An array of {@code ChatType} objects.
	 */
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
