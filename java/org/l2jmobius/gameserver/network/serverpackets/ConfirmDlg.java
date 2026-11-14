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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage.SMParam;

/**
 * This packet is used to send a confirmation dialog to the client.<br>
 * It allows the player to confirm or cancel an action before it is executed.
 * @author Mobius
 */
public class ConfirmDlg extends ServerPacket
{
	private int _time;
	private int _requesterId;
	private final SystemMessage _systemMessage;
	
	/**
	 * Creates a new {@code ConfirmDlg} instance.<br>
	 * This constructor initializes the internal system message using the provided ID.
	 * @param smId The unique identifier for the system message.
	 */
	public ConfirmDlg(SystemMessageId smId)
	{
		_systemMessage = new SystemMessage(smId);
	}
	
	/**
	 * Creates a new {@code ConfirmDlg} instance using a system message ID.<br>
	 * This constructor initializes the internal {@code SystemMessage} based on the provided integer.
	 * @param id The unique identifier for the system message.
	 */
	public ConfirmDlg(int id)
	{
		_systemMessage = new SystemMessage(id);
	}
	
	/**
	 * Creates a new {@code ConfirmDlg} with a custom message.<br>
	 * This constructor sets the system message to {@code SystemMessageId.S1_3}.<br>
	 * The provided text is added as a parameter to that message.
	 * @param text The text to display in the confirmation dialog.
	 */
	public ConfirmDlg(String text)
	{
		_systemMessage = new SystemMessage(SystemMessageId.S1_3);
		_systemMessage.addString(text);
	}
	
	/**
	 * Sets the duration for the dialog.<br>
	 * This method updates the internal {@code _time} field.<br>
	 * It returns the current instance to allow for method chaining.
	 * @param time The amount of time to set.
	 * @return The current {@code ConfirmDlg} object.
	 */
	public ConfirmDlg addTime(int time)
	{
		_time = time;
		return this;
	}
	
	/**
	 * Sets the unique identifier for the requester.<br>
	 * This value is stored in the {@code _requesterId} field.
	 * @param id The unique ID of the user making the request.
	 * @return The current {@code ConfirmDlg} instance.
	 */
	public ConfirmDlg addRequesterId(int id)
	{
		_requesterId = id;
		return this;
	}
	
	/**
	 * Retrieves the {@code SystemMessage} associated with this dialog.<br>
	 * This method returns the internal message object used for display.
	 * @return the {@code SystemMessage} instance.
	 */
	public SystemMessage getSystemMessage()
	{
		return _systemMessage;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.CONFIRM_DLG.writeId(this, buffer);
		final SMParam[] params = _systemMessage.getParams();
		buffer.writeInt(_systemMessage.getId());
		buffer.writeInt(params.length);
		for (SMParam param : params)
		{
			final int paramType = param.getType();
			buffer.writeInt(paramType);
			switch (paramType)
			{
				case SystemMessage.TYPE_ELEMENT_NAME:
				case SystemMessage.TYPE_BYTE:
				case SystemMessage.TYPE_FACTION_NAME:
				{
					buffer.writeByte(param.getIntValue());
					break;
				}
				case SystemMessage.TYPE_CASTLE_NAME:
				case SystemMessage.TYPE_SYSTEM_STRING:
				case SystemMessage.TYPE_INSTANCE_NAME:
				case SystemMessage.TYPE_CLASS_ID:
				{
					buffer.writeShort(param.getIntValue());
					break;
				}
				case SystemMessage.TYPE_ITEM_NAME:
				case SystemMessage.TYPE_INT_NUMBER:
				case SystemMessage.TYPE_NPC_NAME:
				case SystemMessage.TYPE_DOOR_NAME:
				{
					buffer.writeInt(param.getIntValue());
					break;
				}
				case SystemMessage.TYPE_LONG_NUMBER:
				{
					buffer.writeLong(param.getLongValue());
					break;
				}
				case SystemMessage.TYPE_TEXT:
				case SystemMessage.TYPE_PLAYER_NAME:
				{
					buffer.writeString(param.getStringValue());
					break;
				}
				case SystemMessage.TYPE_SKILL_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // skill id
					buffer.writeShort(array[1]); // skill level
					buffer.writeShort(array[2]); // skill sub level
					break;
				}
				case SystemMessage.TYPE_POPUP_ID:
				case SystemMessage.TYPE_ZONE_NAME:
				{
					final int[] array = param.getIntArrayValue();
					buffer.writeInt(array[0]); // x
					buffer.writeInt(array[1]); // y
					buffer.writeInt(array[2]); // z
					break;
				}
			}
		}
		
		buffer.writeInt(_time);
		buffer.writeInt(_requesterId);
	}
}
