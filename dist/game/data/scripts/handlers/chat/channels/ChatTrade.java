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
import org.l2jmobius.gameserver.config.custom.FactionSystemConfig;
import org.l2jmobius.gameserver.data.xml.MapRegionData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.ChatBroadcastType;
import org.l2jmobius.gameserver.entity.actor.holders.player.BlockList;
import org.l2jmobius.gameserver.handler.IChatHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * Handles messages sent within the trade chat channel.<br>
 * This class manages how trade-related communications are processed and broadcast to players.
 * @author durgus
 */
public class ChatTrade implements IChatHandler
{
	private static final ChatType[] CHAT_TYPES =
	{
		ChatType.TRADE,
	};
	
	/**
	 * Processes chat messages sent through the trade channel.<br>
	 * This method checks for bans, jail status, and level requirements before broadcasting.<br>
	 * It handles different broadcast types based on server configuration.
	 * @param type The {@code ChatType} of the message.
	 * @param activeChar The {@code Player} who is sending the message.
	 * @param target The name of the recipient, if applicable.
	 * @param text The content of the chat message.
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
		
		if (activeChar.getLevel() < 20)
		{
			activeChar.sendPacket(new SystemMessage(SystemMessageId.CHARACTERS_OF_LV_S1_OR_LOWER_CANNOT_USE_THE_TRADE_CHAT).addInt(20));
			return;
		}
		
		final CreatureSay cs = new CreatureSay(activeChar, type, activeChar.getName(), text);
		switch (GeneralConfig.DEFAULT_TRADE_CHAT)
		{
			case ChatBroadcastType.ON:
			case ChatBroadcastType.GM:
			{
				if ((GeneralConfig.DEFAULT_TRADE_CHAT == ChatBroadcastType.ON) || ((GeneralConfig.DEFAULT_TRADE_CHAT == ChatBroadcastType.GM) && activeChar.isGM()))
				{
					final int region = MapRegionData.getInstance().getMapRegionLocId(activeChar);
					for (Player player : World.getPlayers())
					{
						if ((region == MapRegionData.getInstance().getMapRegionLocId(player)) && !BlockList.isBlocked(player, activeChar) && (player.getInstanceId() == activeChar.getInstanceId()))
						{
							if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
							{
								if (FactionSystemConfig.FACTION_SPECIFIC_CHAT)
								{
									if ((activeChar.isGood() && player.isGood()) || (activeChar.isEvil() && player.isEvil()))
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
					}
				}
				break;
			}
			case ChatBroadcastType.GLOBAL:
			{
				if (!activeChar.isGM() && !activeChar.getClient().getFloodProtectors().canUseGlobalChat())
				{
					activeChar.sendMessage("Do not spam trade channel.");
					return;
				}
				
				for (Player player : World.getPlayers())
				{
					if (!BlockList.isBlocked(player, activeChar))
					{
						if (FactionSystemConfig.FACTION_SYSTEM_ENABLED)
						{
							if (FactionSystemConfig.FACTION_SPECIFIC_CHAT)
							{
								if ((activeChar.isGood() && player.isGood()) || (activeChar.isEvil() && player.isEvil()))
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
				}
				break;
			}
		}
	}
	
	/**
	 * Retrieves the list of chat types handled by this class.<br>
	 * This method returns an array of {@code ChatType}.
	 * @return An array containing all supported {@code ChatType} values.
	 */
	@Override
	public ChatType[] getChatTypeList()
	{
		return CHAT_TYPES;
	}
}
