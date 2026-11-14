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

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.PacketLogger;
import org.l2jmobius.gameserver.network.enums.HtmlActionScope;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * This class serves as a base for all packets that handle HTML content.<br>
 * It provides common functionality for sending and processing {@code HtmlUtil} data to clients.
 * @author HorridoJoho
 */
public abstract class AbstractHtmlPacket extends ServerPacket
{
	public static final char VAR_PARAM_START_CHAR = '$';
	
	private final int _npcObjId;
	private String _html = null;
	private boolean _disabledValidation = false;
	
	/**
	 * Default constructor for the {@code AbstractHtmlPacket} class.<br>
	 * It initializes the {@code _npcObjId} field to {@code 0}.
	 */
	protected AbstractHtmlPacket()
	{
		_npcObjId = 0;
	}
	
	/**
	 * Creates a new instance of an {@code AbstractHtmlPacket} for a specific NPC.<br>
	 * This constructor initializes the packet with a valid object ID.<br>
	 * It throws an {@code IllegalArgumentException} if the provided ID is less than 0.
	 * @param npcObjId The unique identifier for the NPC associated with this packet.
	 */
	protected AbstractHtmlPacket(int npcObjId)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
	}
	
	/**
	 * Creates a new {@code AbstractHtmlPacket} with the provided HTML content.<br>
	 * This constructor initializes the internal HTML string.<br>
	 * The {@code _npcObjId} is set to {@code 0} by default.
	 * @param html The raw HTML string to be used in the packet.
	 */
	protected AbstractHtmlPacket(String html)
	{
		_npcObjId = 0;
		setHtml(html);
	}
	
	/**
	 * Creates a new instance of an {@code AbstractHtmlPacket}.<br>
	 * This constructor initializes the packet with a specific NPC ID and HTML content.<br>
	 * It ensures that the provided {@code npcObjId} is not negative.
	 * @param npcObjId The unique identifier for the NPC object.
	 * @param html The raw HTML string to be sent to the client.
	 */
	protected AbstractHtmlPacket(int npcObjId, String html)
	{
		if (npcObjId < 0)
		{
			throw new IllegalArgumentException();
		}
		
		_npcObjId = npcObjId;
		setHtml(html);
	}
	
	/**
	 * This method turns off the validation check for this packet.<br>
	 * It sets the internal {@code _disabledValidation} flag to {@code true}.<br>
	 * Use this when you want to bypass standard security or format checks.
	 */
	public void disableValidation()
	{
		_disabledValidation = true;
	}
	
	/**
	 * Sets the HTML content for this packet.<br>
	 * This method automatically wraps the string in basic tags if they are missing.<br>
	 * It also truncates the input to 17200 characters to prevent client crashes.
	 * @param html The HTML string to be used by the packet.
	 */
	public void setHtml(String html)
	{
		if (html.length() > 17200)
		{
			PacketLogger.warning(getClass().getSimpleName() + ": Html is too long! this will crash the client!");
			_html = html.substring(0, 17200);
		}
		else
		{
			if (!html.contains("<html") && !html.startsWith("..\\L2"))
			{
				_html = "<html><body>" + html + "</body></html>";
			}
			else
			{
				_html = html;
			}
		}
	}
	
	/**
	 * Loads an HTML file for a specific player.<br>
	 * It retrieves the content from the {@code HtmCache}.<br>
	 * If the file is missing, it sets a default error message.
	 * @param player The {@code Player} who will receive the HTML content.
	 * @param path The file path to the HTML template.
	 * @return {@code true} if the file was loaded successfully, or {@code false} otherwise.
	 */
	public boolean setFile(Player player, String path)
	{
		final String content = HtmCache.getInstance().getHtm(player, path);
		if (content == null)
		{
			setHtml("<html><body>My Text is missing:<br>" + path + "</body></html>");
			PacketLogger.warning(getClass().getSimpleName() + ": Missing html page " + path);
			return false;
		}
		
		setHtml(content);
		return true;
	}
	
	/**
	 * Replaces all occurrences of a specific pattern within the HTML content.<br>
	 * The {@code value} provided will have its dollar signs escaped to prevent variable processing.
	 * @param pattern The string sequence to search for in the HTML.
	 * @param value The replacement string to insert into the HTML.
	 */
	public void replace(String pattern, String value)
	{
		_html = _html.replaceAll(pattern, value.replaceAll("\\$", "\\\\\\$"));
	}
	
	/**
	 * Replaces all occurrences of a pattern in the HTML content.<br>
	 * The replacement value is converted to a {@code String}.
	 * @param pattern The text sequence to search for.
	 * @param value The new text sequence to insert.
	 */
	public void replace(String pattern, CharSequence value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	/**
	 * Replaces all occurrences of a pattern in the HTML content.<br>
	 * The replacement value is converted to a {@code String}.
	 * @param pattern The text sequence to search for.
	 * @param value The boolean value to insert as a replacement.
	 */
	public void replace(String pattern, boolean value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	/**
	 * Replaces all occurrences of a pattern in the HTML content.<br>
	 * The provided {@code int} value is converted to a {@code String}.<br>
	 * This method updates the internal HTML string.
	 * @param pattern The text sequence to search for.
	 * @param value The integer value to insert into the HTML.
	 */
	public void replace(String pattern, int value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	/**
	 * Replaces all occurrences of a pattern in the HTML content.<br>
	 * The replacement value is converted to a {@code String}.
	 * @param pattern The text sequence to search for.
	 * @param value The numeric value to insert into the HTML.
	 */
	public void replace(String pattern, long value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	/**
	 * Replaces all occurrences of a pattern in the HTML content.<br>
	 * The {@code value} is converted to a {@code String} before replacement.
	 * @param pattern The text sequence to search for.
	 * @param value The numeric value to insert into the HTML.
	 */
	public void replace(String pattern, double value)
	{
		replace(pattern, String.valueOf(value));
	}
	
	@Override
	public void runImpl(Player player)
	{
		if (player != null)
		{
			player.clearHtmlActions(getScope());
		}
		
		if (_disabledValidation)
		{
			return;
		}
		
		if (player != null)
		{
			HtmlUtil.buildHtmlActionCache(player, getScope(), _npcObjId, _html);
		}
	}
	
	/**
	 * Retrieves the unique identifier for the NPC object.<br>
	 * This ID is used to identify which NPC the HTML content belongs to.
	 * @return The {@code int} value of the NPC object ID.
	 */
	public int getNpcObjId()
	{
		return _npcObjId;
	}
	
	/**
	 * Retrieves the HTML content of this packet.<br>
	 * This method returns the internal {@code _html} string.
	 * @return The HTML content as a {@code String}.
	 */
	public String getHtml()
	{
		return _html;
	}
	
	public abstract HtmlActionScope getScope();
}
