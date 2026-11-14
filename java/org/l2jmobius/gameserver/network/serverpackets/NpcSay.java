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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.NpcStringId.NSLocalisation;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This packet is used to send a chat message from an {@code Npc} to the client.<br>
 * It handles the transmission of localized text or custom messages sent by non-player characters.
 * @author Kerberos
 */
public class NpcSay extends ServerPacket
{
	private final int _objectId;
	private final ChatType _textType;
	private final int _npcId;
	private String _text;
	private final int _npcString;
	private List<String> _parameters;
	
	/**
	 * Creates a new {@code NpcSay} packet for an NPC to speak.<br>
	 * This constructor sets the message type and the raw text content.<br>
	 * It also calculates the internal {@code npcId} by adding 1000000.
	 * @param objectId The unique identifier of the object.
	 * @param messageType The category of the chat message.
	 * @param npcId The base ID for the NPC.
	 * @param text The actual string content to be displayed.
	 */
	public NpcSay(int objectId, ChatType messageType, int npcId, String text)
	{
		_objectId = objectId;
		_textType = messageType;
		_npcId = 1000000 + npcId;
		_npcString = -1;
		_text = text;
	}
	
	/**
	 * Creates a new {@code NpcSay} packet for an NPC to speak.<br>
	 * This method sets the message type and the specific text content.<br>
	 * It automatically retrieves the object ID from the provided {@code Npc}.
	 * @param npc The {@code Npc} entity that is speaking.
	 * @param messageType The {@code ChatType} of the message.
	 * @param text The string content to be displayed in the chat.
	 */
	public NpcSay(Npc npc, ChatType messageType, String text)
	{
		_objectId = npc.getObjectId();
		_textType = messageType;
		_npcId = 1000000 + npc.getTemplate().getDisplayId();
		_npcString = -1;
		_text = text;
	}
	
	/**
	 * Creates a new {@code NpcSay} packet using specific identifiers.<br>
	 * This constructor sets the internal IDs for the NPC and its dialogue.
	 * @param objectId The unique identifier of the object.
	 * @param messageType The type of chat message to be sent.
	 * @param npcId The base ID of the NPC.
	 * @param npcString The {@code NpcStringId} containing the dialogue text.
	 */
	public NpcSay(int objectId, ChatType messageType, int npcId, NpcStringId npcString)
	{
		_objectId = objectId;
		_textType = messageType;
		_npcId = 1000000 + npcId;
		_npcString = npcString.getId();
	}
	
	/**
	 * Creates a new {@code NpcSay} packet for an NPC to speak.<br>
	 * This method initializes the packet with the correct object ID and internal IDs.
	 * @param npc The {@code Npc} instance that will be speaking.
	 * @param messageType The {@code ChatType} of the message.
	 * @param npcString The {@code NpcStringId} containing the text to display.
	 */
	public NpcSay(Npc npc, ChatType messageType, NpcStringId npcString)
	{
		_objectId = npc.getObjectId();
		_textType = messageType;
		_npcId = 1000000 + npc.getTemplate().getDisplayId();
		_npcString = npcString.getId();
	}
	
	/**
	 * Adds a single string parameter to the {@code NpcSay} packet.<br>
	 * This is useful for filling in dynamic values within an NPC message.<br>
	 * The method returns the current instance to allow for method chaining.
	 * @param text The string value to add to the parameters list.
	 * @return The current {@code NpcSay} object.
	 */
	public NpcSay addStringParameter(String text)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		
		_parameters.add(text);
		return this;
	}
	
	/**
	 * Adds multiple string parameters to the {@code NpcSay} packet.<br>
	 * These strings are used for dynamic text replacement.<br>
	 * This method ignores {@code null} or empty strings.
	 * @param params The array of strings to add to the packet.
	 * @return The current {@code NpcSay} instance for chaining.
	 */
	public NpcSay addStringParameters(String... params)
	{
		if ((params != null) && (params.length > 0))
		{
			if (_parameters == null)
			{
				_parameters = new ArrayList<>();
			}
			
			for (String item : params)
			{
				if ((item != null) && !item.isEmpty())
				{
					_parameters.add(item);
				}
			}
		}
		
		return this;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.NPC_SAY.writeId(this, buffer);
		buffer.writeInt(_objectId);
		buffer.writeInt(_textType.getClientId());
		buffer.writeInt(_npcId);
		
		// Localisation related.
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			final Player player = client.getPlayer();
			if (player != null)
			{
				final String lang = player.getLang();
				if ((lang != null) && !lang.equals("en"))
				{
					final NpcStringId ns = NpcStringId.getNpcStringId(_npcString);
					if (ns != null)
					{
						final NSLocalisation nsl = ns.getLocalisation(lang);
						if (nsl != null)
						{
							buffer.writeInt(-1);
							buffer.writeString(nsl.getLocalisation(_parameters != null ? _parameters : Collections.emptyList()));
							return;
						}
					}
				}
			}
		}
		
		buffer.writeInt(_npcString);
		if (_npcString == -1)
		{
			buffer.writeString(_text);
		}
		else if (_parameters != null)
		{
			for (String s : _parameters)
			{
				buffer.writeString(s);
			}
		}
	}
}
