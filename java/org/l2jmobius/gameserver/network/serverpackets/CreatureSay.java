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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.managers.CursedWeaponsManager;
import org.l2jmobius.gameserver.managers.MentorManager;
import org.l2jmobius.gameserver.managers.RankManager;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This packet is sent to clients to notify them that a {@code Creature} has spoken.<br>
 * It contains the message content and the specific {@code ChatType} used for the communication.
 * @author Mobius, Notorion
 */
public class CreatureSay extends ServerPacket
{
	private final Creature _sender;
	private final ChatType _chatType;
	private String _senderName = null;
	private String _text = null;
	private int _charId = 0;
	private int _messageId = -1;
	private int _mask;
	private List<String> _parameters;
	
	/**
	 * Creates a new {@code CreatureSay} packet for communication between two players.<br>
	 * This constructor calculates the visibility mask based on social relationships.<br>
	 * It checks for friends, clans, mentors, and allies.
	 * @param sender The {@code Player} who is sending the message.
	 * @param receiver The {@code Player} who will receive the message.
	 * @param name The display name of the sender.
	 * @param chatType The type of chat being sent.
	 * @param text The actual content of the message.
	 */
	public CreatureSay(Player sender, Player receiver, String name, ChatType chatType, String text)
	{
		_sender = sender;
		_chatType = chatType;
		_text = text;
		
		if (sender.isCursedWeaponEquipped() && (chatType != ChatType.WHISPER))
		{
			_senderName = CursedWeaponsManager.getInstance().getCursedWeapon(sender.getCursedWeaponEquippedId()).getName();
		}
		else
		{
			_senderName = name;
		}
		
		if (receiver != null)
		{
			final int objectId = sender.getObjectId();
			if (receiver.getFriends().contains(objectId))
			{
				_mask |= 0x01;
			}
			
			final int clanId = receiver.getClanId();
			if ((clanId > 0) && (clanId == sender.getClanId()))
			{
				_mask |= 0x02;
			}
			
			if ((MentorManager.getInstance().getMentee(receiver.getObjectId(), objectId) != null) || (MentorManager.getInstance().getMentee(objectId, receiver.getObjectId()) != null))
			{
				_mask |= 0x04;
			}
			
			final int receiverAllyId = receiver.getAllyId();
			if ((receiverAllyId > 0) && (receiverAllyId == sender.getAllyId()))
			{
				_mask |= 0x08;
			}
		}
		
		// Does not shows level.
		if (sender.isGM())
		{
			_mask |= 0x10;
		}
	}
	
	/**
	 * Creates a new {@code CreatureSay} packet.<br>
	 * This packet is used to send a chat message from a creature.<br>
	 * It stores the sender, the type of chat, and the message content.
	 * @param sender The {@code Creature} that is sending the message.
	 * @param chatType The {@code ChatType} of the message.
	 * @param senderName The name to display for the sender.
	 * @param text The actual text content of the message.
	 */
	public CreatureSay(Creature sender, ChatType chatType, String senderName, String text)
	{
		_sender = sender;
		_chatType = chatType;
		_text = text;
		if ((sender != null) && sender.isPlayer() && sender.asPlayer().isCursedWeaponEquipped() && (chatType != ChatType.WHISPER))
		{
			_senderName = CursedWeaponsManager.getInstance().getCursedWeapon(sender.asPlayer().getCursedWeaponEquippedId()).getName();
		}
		else
		{
			_senderName = senderName;
		}
	}
	
	/**
	 * Creates a new {@code CreatureSay} packet for an NPC.<br>
	 * This constructor uses a specific {@code NpcStringId} to determine the message content.<br>
	 * It automatically sets the sender name if the {@code sender} is not {@code null}.
	 * @param sender The {@code Creature} sending the message.
	 * @param chatType The type of chat being sent.
	 * @param npcStringId The unique identifier for the NPC's text string.
	 */
	public CreatureSay(Creature sender, ChatType chatType, NpcStringId npcStringId)
	{
		_sender = sender;
		_chatType = chatType;
		_messageId = npcStringId.getId();
		if (sender != null)
		{
			_senderName = sender.getName();
		}
	}
	
	/**
	 * Creates a {@code CreatureSay} packet for a system message.<br>
	 * This constructor is used when the sender is not a specific creature.<br>
	 * It uses a character ID and a system message ID to determine the content.
	 * @param chatType The type of chat to be displayed.
	 * @param charId The unique identifier for the character.
	 * @param systemMessageId The ID of the system message to send.
	 */
	public CreatureSay(ChatType chatType, int charId, SystemMessageId systemMessageId)
	{
		_sender = null;
		_chatType = chatType;
		_charId = charId;
		_messageId = systemMessageId.getId();
	}
	
	/**
	 * Adds a new string to the list of parameters.<br>
	 * This method initializes the {@code _parameters} list if it is currently {@code null}.<br>
	 * The provided {@code text} is appended to the end of the collection.
	 * @param text The string value to add to the parameter list.
	 */
	public void addStringParameter(String text)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		
		_parameters.add(text);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SAY2.writeId(this, buffer);
		buffer.writeInt(_sender == null ? 0 : _sender.getObjectId());
		
		buffer.writeInt(_chatType.getClientId());
		if (_senderName != null)
		{
			buffer.writeString(_senderName);
		}
		else
		{
			buffer.writeInt(_charId);
		}
		
		buffer.writeInt(_messageId); // High Five NPCString ID
		if (_text != null)
		{
			buffer.writeString(_text);
			if ((_sender != null) && (_sender.isPlayer() || _sender.isFakePlayer()) && (_chatType == ChatType.WHISPER))
			{
				buffer.writeByte(_mask);
				if ((_mask & 0x10) == 0)
				{
					buffer.writeByte(_sender.getLevel());
				}
			}
		}
		else if (_parameters != null)
		{
			for (String s : _parameters)
			{
				buffer.writeString(s);
			}
		}
		
		// Rank
		if ((_sender != null) && _sender.isPlayer())
		{
			final Clan clan = _sender.getClan();
			if ((clan != null) && ((_chatType == ChatType.CLAN) || (_chatType == ChatType.ALLIANCE)))
			{
				buffer.writeByte(0); // unknown clan byte
			}
			
			final Player player = _sender.asPlayer();
			final int rank = RankManager.getInstance().getPlayerGlobalRank(player);
			if ((rank == 0) || (rank > 100))
			{
				buffer.writeByte(0);
			}
			else if (rank <= 10)
			{
				buffer.writeByte(1);
			}
			else if (rank <= 50)
			{
				buffer.writeByte(2);
			}
			else if (rank <= 100)
			{
				buffer.writeByte(3);
			}
			
			if (clan != null)
			{
				buffer.writeByte(clan.getCastleId());
			}
			else
			{
				buffer.writeByte(0);
			}
			
			// Share location?
			buffer.writeInt(0);
			
			// Scholar hat in front of name.
			buffer.writeByte(player.isMentee());
		}
		else
		{
			buffer.writeByte(0);
		}
	}
	
	@Override
	public void runImpl(Player player)
	{
		if (player != null)
		{
			player.broadcastSnoop(_chatType, _senderName, _text, this);
		}
	}
}