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
package org.l2jmobius.gameserver.mechanics.html.styles;

import org.l2jmobius.gameserver.mechanics.html.IHtmlStyle;

/**
 * This class defines the CSS styles for buttons used in HTML elements.<br>
 * It implements {@code IHtmlStyle} to provide consistent styling across the game interface.
 * @author UnAfraid
 */
public class ButtonsStyle implements IHtmlStyle
{
	private static final String DEFAULT_PAGE_LINK_FORMAT = "<td><button action=\"%s\" value=\"%s\" width=\"%d\" height=\"%d\" back=\"%s\" fore=\"%s\"></td>";
	private static final String DEFAULT_PAGE_TEXT_FORMAT = "<td>%s</td>";
	private static final String DEFAULT_PAGER_SEPARATOR = "<td align=center> | </td>";
	
	public static final ButtonsStyle INSTANCE = new ButtonsStyle(40, 15, "L2UI_CT1.Button_DF_Down", "L2UI_CT1.Button_DF");
	
	private final int _width;
	private final int _height;
	private final String _back;
	private final String _fore;
	
	/**
	 * Creates a new {@code ButtonsStyle} object.<br>
	 * This constructor sets the dimensions and textures for buttons.
	 * @param width The width of the button in pixels.
	 * @param height The height of the button in pixels.
	 * @param back The texture name for the background.
	 * @param fore The texture name for the foreground.
	 */
	public ButtonsStyle(int width, int height, String back, String fore)
	{
		_width = width;
		_height = height;
		_back = back;
		_fore = fore;
	}
	
	/**
	 * This method determines how a button or text is displayed.<br>
	 * It checks the {@code isEnabled} status to decide between two formats.<br>
	 * If {@code true}, it returns a simple text format.<br>
	 * If {@code false}, it returns a button link format.
	 * @param bypass The action value for the button link.
	 * @param name The display name of the element.
	 * @param isEnabled The status determining which style to use.
	 * @return A formatted HTML string based on the provided parameters.
	 */
	@Override
	public String applyBypass(String bypass, String name, boolean isEnabled)
	{
		if (isEnabled)
		{
			return String.format(DEFAULT_PAGE_TEXT_FORMAT, name);
		}
		
		return String.format(DEFAULT_PAGE_LINK_FORMAT, bypass, name, _width, _height, _back, _fore);
	}
	
	/**
	 * Returns the default separator for pagination buttons.<br>
	 * This method provides a visual divider between page links.
	 * @return The {@code String} representing the separator.
	 */
	@Override
	public String applySeparator()
	{
		return DEFAULT_PAGER_SEPARATOR;
	}
}
