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
package org.l2jmobius.gameserver.network.clientpackets;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.data.xml.PetData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * Handles a request from the client to change the name of a pet.<br>
 * This packet processes the new name provided by the player and updates it in the database.
 */
public class RequestChangePetName extends ClientPacket
{
	private static final Logger LOGGER = Logger.getLogger(RequestChangePetName.class.getName());
	
	private static final String CHECK_PET_NAME = "SELECT p.name FROM pets p JOIN items i ON p.item_obj_id = i.object_id WHERE p.name=? AND i.item_id=?";
	
	private String _name;
	
	@Override
	protected void readImpl()
	{
		_name = readString();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Summon pet = player.getPet();
		if (pet == null)
		{
			return;
		}
		
		if (!pet.isPet())
		{
			player.sendPacket(SystemMessageId.YOU_DO_NOT_HAVE_A_PET);
			return;
		}
		
		final String petName = pet.getName();
		if ((petName != null) && !petName.isEmpty())
		{
			player.sendPacket(SystemMessageId.YOU_CANNOT_SET_A_NAME_FOR_THE_PET);
			return;
		}
		
		boolean nameExists = false;
		final int petItemId = PetData.getInstance().getPetItemsByNpc(pet.getTemplate().getId());
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement ps = con.prepareStatement(CHECK_PET_NAME))
		{
			ps.setString(1, _name);
			ps.setInt(2, petItemId);
			
			try (ResultSet rs = ps.executeQuery())
			{
				nameExists = rs.next();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, getClass().getSimpleName() + ": Could not check existing pet name: " + e.getMessage(), e);
		}
		
		if (nameExists)
		{
			player.sendPacket(SystemMessageId.THE_NAME_IS_ALREADY_IN_USE_BY_ANOTHER_PET);
			return;
		}
		
		final int nameLength = _name.length();
		if ((nameLength < 3) || (nameLength > 16))
		{
			// player.sendPacket(SystemMessageId.YOUR_PET_S_NAME_CAN_BE_UP_TO_8_CHARACTERS_IN_LENGTH);
			player.sendMessage("Your pet's name can be up to 16 characters in length.");
		}
		
		// No spaces.
		if (_name.contains(" "))
		{
			player.sendPacket(SystemMessageId.THERE_IS_A_SPACE_IN_THE_NAME);
			return;
		}
		
		// Only allow alphanumeric names.
		if (!StringUtil.isAlphaNumeric(_name) || !ServerConfig.PET_NAME_TEMPLATE_PATTERN.matcher(_name).matches())
		{
			player.sendPacket(SystemMessageId.THE_PET_S_NAME_INCLUDES_PROHIBITED_CHARACTERS);
			return;
		}
		
		pet.setName(_name);
		pet.storeMe(); // Save to DB immediately.
		pet.updateAndBroadcastStatus(1);
	}
}
