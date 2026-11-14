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
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;

/**
 * This class handles the {@code NpcHtmlMessage} server packet.<br>
 * It is used to send HTML-formatted messages to clients from NPCs.
 * @author Mobius
 */
public class NpcHtmlMessage extends AbstractHtmlPacket
{
	private final int _itemId;
	private final int _size;
	
	/**
	 * Creates a new instance of {@code NpcHtmlMessage}.<br>
	 * This constructor initializes the default state for an NPC HTML message.<br>
	 * The internal {@code _itemId} is set to {@code 0}.
	 */
	public NpcHtmlMessage()
	{
		_itemId = 0;
		_size = 0;
	}
	
	/**
	 * Creates a new {@code NpcHtmlMessage} for a specific NPC.<br>
	 * This constructor initializes the message with the provided {@code npcObjId}.<br>
	 * The default value for {@code _itemId} is set to {@code 0}.
	 * @param npcObjId The unique identifier of the NPC object.
	 */
	public NpcHtmlMessage(int npcObjId)
	{
		super(npcObjId);
		_itemId = 0;
		_size = 0;
	}
	
	/**
	 * Creates a new {@code NpcHtmlMessage} using the provided HTML string.<br>
	 * This constructor initializes the message with a default item ID of {@code 0}.
	 * @param html The HTML content to be displayed by the NPC.
	 */
	public NpcHtmlMessage(String html)
	{
		super(html);
		_itemId = 0;
		_size = 0;
	}
	
	/**
	 * Creates a new {@code NpcHtmlMessage} with a specific NPC ID and HTML content.<br>
	 * This packet is used to send HTML messages associated with an NPC.
	 * @param npcObjId The unique identifier of the NPC object.
	 * @param html The HTML string to be displayed.
	 */
	public NpcHtmlMessage(int npcObjId, String html)
	{
		super(npcObjId, html);
		_itemId = 0;
		_size = 0;
	}
	
	/**
	 * Creates a new {@code NpcHtmlMessage} using an NPC ID and an item ID.<br>
	 * This constructor initializes the packet with specific object identifiers.
	 * @param npcObjId The unique identifier of the NPC object.
	 * @param itemId The unique identifier of the item associated with this message.
	 */
	public NpcHtmlMessage(int npcObjId, int itemId)
	{
		super(npcObjId);
		if (itemId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_itemId = itemId;
		_size = 0;
	}
	
	/**
	 * Creates a new {@code NpcHtmlMessage} with an item ID.<br>
	 * This packet is used to display HTML content associated with a specific NPC and item.
	 * @param npcObjId The unique identifier of the NPC object.
	 * @param itemId The unique identifier of the item.
	 * @param html The HTML string to be displayed.
	 */
	public NpcHtmlMessage(int npcObjId, int itemId, String html)
	{
		super(npcObjId, html);
		if (itemId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_itemId = itemId;
		_size = 0;
	}
	
	/**
	 * Creates a new {@code NpcHtmlMessage} packet.<br>
	 * This message is used to display HTML content from an NPC.<br>
	 * It includes specific details about the item and the window size.
	 * @param npcObjId The unique identifier of the NPC object.
	 * @param itemId The unique identifier of the item associated with this message.
	 * @param html The HTML string content to be displayed.
	 * @param windowSize The size of the window where the HTML will appear.
	 */
	public NpcHtmlMessage(int npcObjId, int itemId, String html, int windowSize)
	{
		super(npcObjId, html);
		if (itemId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_itemId = itemId;
		_size = windowSize;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.NPC_HTML_MESSAGE.writeId(this, buffer);
		buffer.writeInt(getNpcObjId());
		buffer.writeString(getHtml());
		buffer.writeInt(_itemId);
		buffer.writeInt(0); // play sound - 0 = enabled, 1 = disabled
		buffer.writeByte(_size);
	}
	
	/**
	 * Retrieves the scope of the HTML action.<br>
	 * It determines if the message belongs to an NPC or an item.
	 * @return the {@code HtmlActionScope} associated with this packet.
	 */
	@Override
	public HtmlActionScope getScope()
	{
		return _itemId == 0 ? HtmlActionScope.NPC_HTML : HtmlActionScope.NPC_ITEM_HTML;
	}
}
