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
package org.l2jmobius.gameserver.communitybbs.Manager;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.serverpackets.ShowBoard;

/**
 * Provides the base functionality for managing community bulletin boards.<br>
 * This abstract class serves as a foundation for different types of board systems.<br>
 * It handles core logic shared across all {@code BBS} implementations.
 */
public abstract class BaseBBSManager
{
	public abstract void parsecmd(String command, Player player);
	
	public abstract void parsewrite(String ar1, String ar2, String ar3, String ar4, String ar5, Player player);
	
	/**
	 * Sends a specific board packet to a player.<br>
	 * This method checks if the {@code html} string is shorter than {@code 8192} characters.<br>
	 * If valid, it sends the {@code ShowBoard} packet with ID {@code 1001}.
	 * @param html The HTML content to be displayed.
	 * @param acha The {@code Player} who will receive the packet.
	 */
	protected void send1001(String html, Player acha)
	{
		if (html.length() < 8192)
		{
			acha.sendPacket(new ShowBoard(html, "1001"));
		}
	}
	
	/**
	 * Sends a specific board message to the player.<br>
	 * This method calls {@code send1002} with default values.
	 * @param acha The {@code Player} who will receive the message.
	 */
	protected void send1002(Player acha)
	{
		send1002(acha, " ", " ", "0");
	}
	
	/**
	 * Sends a {@code ShowBoard} packet to the specified player.<br>
	 * This method constructs a list of arguments containing board details.<br>
	 * It uses the provided strings for the content, subject, and date.
	 * @param player The {@code Player} who will receive the packet.
	 * @param string The main text content of the board.
	 * @param string2 The subject of the board post.
	 * @param string3 The date associated with the board post.
	 */
	protected void send1002(Player player, String string, String string2, String string3)
	{
		final List<String> arg = new ArrayList<>(20);
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add("0");
		arg.add(player.getName());
		arg.add(Integer.toString(player.getObjectId()));
		arg.add(player.getAccountName());
		arg.add("9");
		arg.add(string2); // subject?
		arg.add(string2); // subject?
		arg.add(string); // text
		arg.add(string3); // date?
		arg.add(string3); // date?
		arg.add("0");
		arg.add("0");
		player.sendPacket(new ShowBoard(arg));
	}
}
