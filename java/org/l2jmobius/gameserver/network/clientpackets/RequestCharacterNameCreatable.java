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
package org.l2jmobius.gameserver.network.clientpackets;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.sql.PlayerInfoTable;
import org.l2jmobius.gameserver.data.xml.FakePlayerData;
import org.l2jmobius.gameserver.network.serverpackets.ExIsCharNameCreatable;

/**
 * This packet is sent by the client to check if a specific character name is available for creation.<br>
 * It allows the game server to validate naming rules before a new character is registered.
 * @author UnAfraid
 */
public class RequestCharacterNameCreatable extends ClientPacket
{
	private String _name;
	
	public static final int CHARACTER_CREATE_FAILED = 1;
	public static final int NAME_ALREADY_EXISTS = 2;
	public static final int INVALID_LENGTH = 3;
	public static final int INVALID_NAME = 4;
	public static final int CANNOT_CREATE_SERVER = 5;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final int charId = PlayerInfoTable.getInstance().getIdByName(_name);
		int result;
		if (!StringUtil.isAlphaNumeric(_name) || !isValidName(_name))
		{
			result = INVALID_NAME;
		}
		else if (charId > 0)
		{
			result = NAME_ALREADY_EXISTS;
		}
		else if (FakePlayerData.getInstance().getProperName(_name) != null)
		{
			result = NAME_ALREADY_EXISTS;
		}
		else if (_name.length() > 16)
		{
			result = INVALID_LENGTH;
		}
		else
		{
			result = -1;
		}
		
		getClient().sendPacket(new ExIsCharNameCreatable(result));
	}
	
	/**
	 * Checks if a character name follows the allowed format.<br>
	 * It validates the input against the server configuration pattern.
	 * @param text The name string to validate.
	 * @return {@code true} if the name is valid, {@code false} otherwise.
	 */
	private boolean isValidName(String text)
	{
		return ServerConfig.CHARNAME_TEMPLATE_PATTERN.matcher(text).matches();
	}
}
