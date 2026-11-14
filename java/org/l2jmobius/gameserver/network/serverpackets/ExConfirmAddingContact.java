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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet confirms the addition of a new contact to a player's list.<br>
 * It is sent from the server to the client to synchronize the updated contact information.
 * @author mrTJO, UnAfraid
 */
public class ExConfirmAddingContact extends ServerPacket
{
	private final String _charName;
	private final boolean _added;
	
	/**
	 * This packet confirms the result of an attempt to add a contact.<br>
	 * It sends the character name and whether the action was successful.
	 * @param charName The name of the character involved in the request.
	 * @param added A {@code true} value if the contact was successfully added, or {@code false} otherwise.
	 */
	public ExConfirmAddingContact(String charName, boolean added)
	{
		_charName = charName;
		_added = added;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_AGITAUCTION_CMD.writeId(this, buffer);
		buffer.writeString(_charName);
		buffer.writeInt(_added);
	}
}
