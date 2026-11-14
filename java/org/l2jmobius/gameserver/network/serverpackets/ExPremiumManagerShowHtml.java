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
 * This packet is used to display HTML content related to the premium manager.<br>
 * It sends specific web-based information to the client for viewing.
 * @author JoeAlisson
 */
public class ExPremiumManagerShowHtml extends AbstractHtmlPacket
{
	/**
	 * Displays a specific HTML content to the player.<br>
	 * This method initializes the packet with the provided {@code html} string.
	 * @param html The HTML content to be displayed.
	 */
	public ExPremiumManagerShowHtml(String html)
	{
		super(html);
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_PREMIUM_MANAGER_SHOW_HTML.writeId(this, buffer);
		buffer.writeInt(getNpcObjId());
		buffer.writeString(getHtml());
		buffer.writeInt(-1);
		buffer.writeInt(0);
	}
	
	/**
	 * Retrieves the scope of the HTML action.<br>
	 * This identifies which area of the UI is affected by this packet.
	 * @return the {@code HtmlActionScope} for this action.
	 */
	@Override
	public HtmlActionScope getScope()
	{
		return HtmlActionScope.PREMIUM_HTML;
	}
}
