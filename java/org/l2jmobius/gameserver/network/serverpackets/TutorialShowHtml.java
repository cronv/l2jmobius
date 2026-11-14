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
 * This class handles the {@code TutorialShowHtml} server packet.<br>
 * It is used to display HTML content as part of a tutorial for the client.
 * @author Mobius
 */
public class TutorialShowHtml extends AbstractHtmlPacket
{
	// TODO: Enum
	public static final int NORMAL_WINDOW = 1;
	public static final int LARGE_WINDOW = 2;
	
	private final int _type;
	
	/**
	 * Creates a new {@code TutorialShowHtml} packet with default settings.<br>
	 * This constructor sets the window type to {@code NORMAL_WINDOW}.
	 * @param html The HTML content to be displayed.
	 */
	public TutorialShowHtml(String html)
	{
		super(html);
		_type = NORMAL_WINDOW;
	}
	
	/**
	 * Creates a new {@code TutorialShowHtml} packet.<br>
	 * This method sets the NPC ID, HTML content, and window type.
	 * @param npcObjId The unique identifier of the NPC object.
	 * @param html The HTML string to be displayed in the tutorial window.
	 * @param type The window size type for the display.
	 */
	public TutorialShowHtml(int npcObjId, String html, int type)
	{
		super(npcObjId, html);
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.TUTORIAL_SHOW_HTML.writeId(this, buffer);
		buffer.writeInt(_type);
		buffer.writeString(getHtml());
	}
	
	/**
	 * Retrieves the scope associated with this HTML action.<br>
	 * This method returns the specific {@code HtmlActionScope} for tutorial content.
	 * @return the {@code HtmlActionScope} value.
	 */
	@Override
	public HtmlActionScope getScope()
	{
		return HtmlActionScope.TUTORIAL_HTML;
	}
}
