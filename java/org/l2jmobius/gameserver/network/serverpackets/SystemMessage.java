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

import java.util.Arrays;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.SystemMessageId.SMLocalisation;

/**
 * Represents a system message sent from the server to the client.<br>
 * This packet is used to display general information, warnings, or notifications to players.
 * @author Mobius
 */
public class SystemMessage extends ServerPacket
{
	private static final SMParam[] EMPTY_PARAM_ARRAY = new SMParam[0];
	
	public class SMParam
	{
		private final byte _type;
		private final Object _value;
		
		public SMParam(byte type, Object value)
		{
			_type = type;
			_value = value;
		}
		
		public byte getType()
		{
			return _type;
		}
		
		public Object getValue()
		{
			return _value;
		}
		
		public String getStringValue()
		{
			return (String) _value;
		}
		
		public int getIntValue()
		{
			return ((Integer) _value).intValue();
		}
		
		public long getLongValue()
		{
			return ((Long) _value).longValue();
		}
		
		public int[] getIntArrayValue()
		{
			return (int[]) _value;
		}
	}
	
	public static final byte TYPE_FACTION_NAME = 24; // c(short), faction id.
	// ID 22 D is shared with IDs 1-3 and 17; ID 21 H, ID 20 C, ID 19 C, and ID 18 Q read the same as 6.
	// id 17 shared with 1-3,17,22
	public static final byte TYPE_BYTE = 20;
	public static final byte TYPE_POPUP_ID = 16;
	public static final byte TYPE_CLASS_ID = 15;
	
	// id 14 dSSSSS
	public static final byte TYPE_SYSTEM_STRING = 13;
	public static final byte TYPE_PLAYER_NAME = 12;
	public static final byte TYPE_DOOR_NAME = 11;
	public static final byte TYPE_INSTANCE_NAME = 10;
	public static final byte TYPE_ELEMENT_NAME = 9;
	
	// id 8 - ddd
	public static final byte TYPE_ZONE_NAME = 7;
	public static final byte TYPE_LONG_NUMBER = 6;
	public static final byte TYPE_CASTLE_NAME = 5;
	public static final byte TYPE_SKILL_NAME = 4;
	public static final byte TYPE_ITEM_NAME = 3;
	public static final byte TYPE_NPC_NAME = 2;
	public static final byte TYPE_INT_NUMBER = 1;
	public static final byte TYPE_TEXT = 0;
	
	private SMParam[] _params;
	private final SystemMessageId _smId;
	private int _paramIndex;
	
	/**
	 * Creates a new {@code SystemMessage} using a specific ID.<br>
	 * This constructor retrieves the corresponding {@code SystemMessageId}.<br>
	 * It also initializes the required parameters for the message.
	 * @param id The unique identifier for the system message.
	 */
	public SystemMessage(int id)
	{
		_smId = SystemMessageId.getSystemMessageId(id);
		final int paramCount = _smId.getParamCount();
		_params = paramCount > 0 ? new SMParam[paramCount] : EMPTY_PARAM_ARRAY;
	}
	
	/**
	 * Creates a new {@code SystemMessage} using a specific identifier.<br>
	 * This constructor initializes the message with its required parameters.
	 * @param smId The unique {@code SystemMessageId} for this message.
	 */
	public SystemMessage(SystemMessageId smId)
	{
		if (smId == null)
		{
			throw new NullPointerException("SystemMessageId cannot be null!");
		}
		
		_smId = smId;
		final int paramCount = smId.getParamCount();
		_params = paramCount > 0 ? new SMParam[paramCount] : EMPTY_PARAM_ARRAY;
	}
	
	/**
	 * Creates a new {@code SystemMessage} using a raw text string.<br>
	 * This constructor sets the message ID to {@code S1_2}.<br>
	 * The provided text is added as the first parameter.
	 * @param text The text content to be displayed in the system message.
	 */
	public SystemMessage(String text)
	{
		if (text == null)
		{
			throw new NullPointerException();
		}
		
		_smId = SystemMessageId.getSystemMessageId(SystemMessageId.S1_2.getId());
		_params = new SMParam[1];
		addString(text);
	}
	
	/**
	 * Retrieves the unique identifier for this system message.<br>
	 * This value corresponds to the ID defined in {@code SystemMessageId}.
	 * @return The integer ID of the system message.
	 */
	public int getId()
	{
		return _smId.getId();
	}
	
	/**
	 * Retrieves the unique identifier for this system message.<br>
	 * This ID is used to determine which localization string to display.
	 * @return The {@code SystemMessageId} associated with this message.
	 */
	public SystemMessageId getSystemMessageId()
	{
		return _smId;
	}
	
	/**
	 * Adds a new {@code SMParam} to the internal parameter list.<br>
	 * This method expands the array if necessary and updates the parameter count.
	 * @param param The {@code SMParam} object to append.
	 */
	private void append(SMParam param)
	{
		if (_paramIndex >= _params.length)
		{
			_params = Arrays.copyOf(_params, _paramIndex + 1);
			_smId.setParamCount(_paramIndex + 1);
			
			// Mobius: With additional on-screen damage param (popup), length is increased.
			if (param.getType() != TYPE_POPUP_ID)
			{
				PacketLogger.info("Wrong parameter count '" + (_paramIndex + 1) + "' for SystemMessageId: " + _smId);
			}
		}
		
		_params[_paramIndex++] = param;
	}
	
	/**
	 * Adds a plain text string to the system message.<br>
	 * This method appends the provided {@code text} as a new parameter.
	 * @param text The string content to be added.
	 * @return The current {@code SystemMessage} instance for chaining.
	 */
	public SystemMessage addString(String text)
	{
		append(new SMParam(TYPE_TEXT, text));
		return this;
	}
	
	/**
	 * Adds a castle identifier to the system message.<br>
	 * This method appends a value of type {@code TYPE_CASTLE_NAME} to the message.
	 * @param number The unique identifier for the castle.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addCastleId(int number)
	{
		append(new SMParam(TYPE_CASTLE_NAME, number));
		return this;
	}
	
	/**
	 * Adds an integer value to the system message.<br>
	 * This method appends a new {@code TYPE_INT_NUMBER} parameter to the current message.
	 * @param number The integer value to add.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addInt(int number)
	{
		append(new SMParam(TYPE_INT_NUMBER, number));
		return this;
	}
	
	/**
	 * Adds a {@code long} value to the system message.<br>
	 * This method appends the provided {@code number} as a parameter.
	 * @param number The {@code long} value to add.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addLong(long number)
	{
		append(new SMParam(TYPE_LONG_NUMBER, number));
		return this;
	}
	
	/**
	 * Adds the visible name of a {@code Player} to the system message.<br>
	 * This method retrieves the name from the player's appearance and appends it.
	 * @param pc The {@code Player} object whose name will be added.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addPcName(Player pc)
	{
		append(new SMParam(TYPE_PLAYER_NAME, pc.getAppearance().getVisibleName()));
		return this;
	}
	
	/**
	 * Adds a door name to the system message.<br>
	 * This method uses the provided {@code doorId} to fetch and include the correct name.
	 * @param doorId The unique identifier of the door.
	 * @return The current {@code SystemMessage} instance for chaining.
	 */
	public SystemMessage addDoorName(int doorId)
	{
		append(new SMParam(TYPE_DOOR_NAME, doorId));
		return this;
	}
	
	/**
	 * Adds the name of an {@code Npc} to the system message.<br>
	 * This method retrieves the template from the provided {@code Npc} object.<br>
	 * It then appends the corresponding name to the message.
	 * @param npc The {@code Npc} entity whose name should be added.
	 * @return The updated {@code SystemMessage} object.
	 */
	public SystemMessage addNpcName(Npc npc)
	{
		return addNpcName(npc.getTemplate());
	}
	
	/**
	 * Adds the name of a {@code Summon} to the system message.<br>
	 * This method retrieves the name based on the {@code Summon} ID.
	 * @param npc The {@code Summon} object containing the name information.
	 * @return The updated {@code SystemMessage} object.
	 */
	public SystemMessage addNpcName(Summon npc)
	{
		return addNpcName(npc.getId());
	}
	
	/**
	 * Adds the name of an NPC based on a provided {@code NpcTemplate}.<br>
	 * If the template uses a server-side name, that name is added.<br>
	 * Otherwise, it adds the name associated with the template ID.
	 * @param template The {@code NpcTemplate} to retrieve the name from.
	 * @return The updated {@code SystemMessage} object.
	 */
	public SystemMessage addNpcName(NpcTemplate template)
	{
		if (template.isUsingServerSideName())
		{
			return addString(template.getName());
		}
		
		return addNpcName(template.getId());
	}
	
	/**
	 * Adds an NPC name to the system message.<br>
	 * This method uses the provided {@code id} to resolve the correct name.
	 * @param id The unique identifier of the NPC.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addNpcName(int id)
	{
		append(new SMParam(TYPE_NPC_NAME, 1000000 + id));
		return this;
	}
	
	/**
	 * Adds the name of an {@code Item} to the system message.<br>
	 * This method retrieves the item ID and uses it to format the display name.
	 * @param item The {@code Item} object to include in the message.
	 * @return The updated {@code SystemMessage} object.
	 */
	public SystemMessage addItemName(Item item)
	{
		return addItemName(item.getId());
	}
	
	/**
	 * Adds the name of an item to the system message.<br>
	 * This method retrieves the name from the provided {@code ItemTemplate}.
	 * @param item The {@code ItemTemplate} containing the item data.
	 * @return The updated {@code SystemMessage} object.
	 */
	public SystemMessage addItemName(ItemTemplate item)
	{
		return addItemName(item.getId());
	}
	
	/**
	 * Adds the name of an item to the system message.<br>
	 * It retrieves the item template using the provided {@code id}.<br>
	 * If the display ID does not match the input, it adds the raw name as a string.<br>
	 * Otherwise, it appends the item name parameter.
	 * @param id The unique identifier of the item to add.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addItemName(int id)
	{
		final ItemTemplate item = ItemData.getInstance().getTemplate(id);
		if (item.getDisplayId() != id)
		{
			return addString(item.getName());
		}
		
		append(new SMParam(TYPE_ITEM_NAME, id));
		return this;
	}
	
	/**
	 * Adds the name of a zone to the system message.<br>
	 * This method uses the provided coordinates to identify the location.
	 * @param x The x coordinate of the zone.
	 * @param y The y coordinate of the zone.
	 * @param z The z coordinate of the zone.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addZoneName(int x, int y, int z)
	{
		append(new SMParam(TYPE_ZONE_NAME, new int[]
		{
			x,
			y,
			z
		}));
		return this;
	}
	
	/**
	 * Adds the name of a {@code Skill} to the system message.<br>
	 * It checks if the display ID matches the internal ID.<br>
	 * If they differ, it adds the skill name as a string.<br>
	 * Otherwise, it adds the skill with its level and sub-level details.
	 * @param skill The {@code Skill} object to include in the message.
	 * @return The updated {@code SystemMessage} instance.
	 */
	public SystemMessage addSkillName(Skill skill)
	{
		final int skillId = skill.getId();
		if (skillId != skill.getDisplayId())
		{
			return addString(skill.getName());
		}
		
		return addSkillName(skillId, skill.getLevel(), skill.getSubLevel());
	}
	
	/**
	 * Adds a skill name to the {@code SystemMessage}.<br>
	 * This method uses the default level of 1 and sub-level of 0.
	 * @param id The unique identifier for the skill.
	 * @return The updated {@code SystemMessage} object.
	 */
	public SystemMessage addSkillName(int id)
	{
		return addSkillName(id, 1, 0);
	}
	
	/**
	 * Adds a skill name to the {@code SystemMessage}.<br>
	 * This method uses the provided ID, level, and sub-level to identify the skill.
	 * @param id The unique identifier of the skill.
	 * @param lvl The level of the skill.
	 * @param subLevel The sub-level of the skill.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addSkillName(int id, int lvl, int subLevel)
	{
		append(new SMParam(TYPE_SKILL_NAME, new int[]
		{
			id,
			lvl,
			subLevel
		}));
		return this;
	}
	
	/**
	 * Adds an element name to the system message.<br>
	 * This method appends a parameter of type {@code TYPE_ELEMENT_NAME}.
	 * @param type The unique identifier for the element name.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addAttribute(int type)
	{
		append(new SMParam(TYPE_ELEMENT_NAME, type));
		return this;
	}
	
	/**
	 * Adds a system string parameter to the message.<br>
	 * This method appends a value of type {@code TYPE_SYSTEM_STRING}.
	 * @param type The integer value to be added as a system string.
	 * @return The current {@code SystemMessage} instance for chaining.
	 */
	public SystemMessage addSystemString(int type)
	{
		append(new SMParam(TYPE_SYSTEM_STRING, type));
		return this;
	}
	
	/**
	 * Adds a class identifier to the system message.<br>
	 * This method appends a new parameter of type {@code TYPE_CLASS_ID}.
	 * @param type The integer value representing the class ID.
	 * @return The current {@code SystemMessage} instance for chaining.
	 */
	public SystemMessage addClassId(int type)
	{
		append(new SMParam(TYPE_CLASS_ID, type));
		return this;
	}
	
	/**
	 * Adds a faction name to the system message.<br>
	 * This method appends the provided {@code factionId} as a parameter.
	 * @param factionId The unique identifier of the faction.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addFactionName(int factionId)
	{
		append(new SMParam(TYPE_FACTION_NAME, factionId));
		return this;
	}
	
	/**
	 * Adds a popup message to the system message.<br>
	 * This method includes information about the target, attacker, and damage amount.
	 * @param target The ID of the entity receiving the action.
	 * @param attacker The ID of the entity performing the action.
	 * @param damage The amount of damage dealt.
	 * @return The current {@code SystemMessage} instance.
	 */
	public SystemMessage addPopup(int target, int attacker, int damage)
	{
		append(new SMParam(TYPE_POPUP_ID, new int[]
		{
			target,
			attacker,
			damage
		}));
		return this;
	}
	
	/**
	 * Adds a byte value to the system message.<br>
	 * This method appends the provided {@code int} as a {@code TYPE_BYTE} parameter.
	 * @param time The integer value to be converted and added.
	 * @return The current {@code SystemMessage} instance for chaining.
	 */
	public SystemMessage addByte(int time)
	{
		append(new SMParam(TYPE_BYTE, time));
		return this;
	}
	
	/**
	 * Adds an instance name to the system message.<br>
	 * This method appends a parameter of type {@code TYPE_INSTANCE_NAME}.
	 * @param type The unique identifier for the instance.
	 * @return The current {@code SystemMessage} object.
	 */
	public SystemMessage addInstanceName(int type)
	{
		append(new SMParam(TYPE_INSTANCE_NAME, type));
		return this;
	}
	
	/**
	 * Retrieves the list of parameters associated with this system message.
	 * @return an array of {@code SMParam} objects.
	 */
	public SMParam[] getParams()
	{
		return _params;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SYSTEM_MESSAGE.writeId(this, buffer);
		
		// Localisation related.
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			final Player player = client.getPlayer();
			if (player != null)
			{
				final String lang = player.getLang();
				if ((lang != null) && !lang.equals("en"))
				{
					final SMLocalisation sml = _smId.getLocalisation(lang);
					if (sml != null)
					{
						final Object[] params = new Object[_paramIndex];
						for (int i = 0; i < _paramIndex; i++)
						{
							params[i] = _params[i].getValue();
						}
						
						buffer.writeShort(SystemMessageId.S1_2.getId());
						buffer.writeByte(1);
						buffer.writeByte(TYPE_TEXT);
						buffer.writeString(sml.getLocalisation(params));
						return;
					}
				}
			}
		}
		
		buffer.writeShort(getId());
		buffer.writeByte(_params.length);
		for (SMParam param : _params)
		{
			if (param == null)
			{
				PacketLogger.warning("Found null parameter for SystemMessageId " + _smId);
				continue;
			}
			
			final int type = param.getType();
			buffer.writeByte(type);
			switch (type)
			{
				case TYPE_ELEMENT_NAME:
				case TYPE_BYTE:
				case TYPE_FACTION_NAME:
				{
					buffer.writeByte(param.getIntValue());
					break;
				}
				case TYPE_CASTLE_NAME:
				case TYPE_SYSTEM_STRING:
				case TYPE_INSTANCE_NAME:
				case TYPE_CLASS_ID:
				{
					buffer.writeShort(param.getIntValue());
					break;
				}
				case TYPE_ITEM_NAME:
				case TYPE_INT_NUMBER:
				case TYPE_NPC_NAME:
				case TYPE_DOOR_NAME:
				{
					buffer.writeInt(param.getIntValue());
					break;
				}
				case TYPE_LONG_NUMBER:
				{
					buffer.writeLong(param.getLongValue());
					break;
				}
				case TYPE_TEXT:
				case TYPE_PLAYER_NAME:
				{
					buffer.writeString(param.getStringValue());
					break;
				}
				case TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // skill id
					buffer.writeShort(array[1]); // skill level
					buffer.writeShort(array[2]); // skill sub level
					break;
				}
				case TYPE_POPUP_ID:
				case TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // x
					buffer.writeInt(array[1]); // y
					buffer.writeInt(array[2]); // z
					break;
				}
			}
		}
	}
}
