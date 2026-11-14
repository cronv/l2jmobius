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
package org.l2jmobius.gameserver.network.serverpackets.teleports;

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet sends a list of favorite teleport locations to the client.<br>
 * It allows the player to see their saved coordinates in the user interface.
 * @author Mobius
 */
public class ExTeleportFavoritesList extends ServerPacket
{
	private final List<Integer> _teleports;
	private final boolean _enable;
	
	/**
	 * Creates a new {@code ExTeleportFavoritesList} packet for a specific player.<br>
	 * This constructor retrieves the favorite teleports from the player's variables.<br>
	 * It also sets whether the teleport feature is enabled.
	 * @param player The {@code Player} object whose favorites will be retrieved.
	 * @param enable A boolean indicating if the teleport feature should be active.
	 */
	public ExTeleportFavoritesList(Player player, boolean enable)
	{
		_teleports = player.getVariables().getIntegerList(PlayerVariables.FAVORITE_TELEPORTS);
		_enable = enable;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_TELEPORT_FAVORITES_LIST.writeId(this, buffer);
		buffer.writeByte(_enable);
		buffer.writeInt(_teleports.size());
		for (int id : _teleports)
		{
			buffer.writeInt(id);
		}
	}
}
