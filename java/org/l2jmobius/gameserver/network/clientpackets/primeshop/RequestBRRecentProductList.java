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
package org.l2jmobius.gameserver.network.clientpackets.primeshop;

import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;

/**
 * This packet is sent by the client to request a list of recently viewed products from the Prime Shop.<br>
 * It allows the game server to identify which items the player has interacted with recently.
 * @author Gnacik, UnAfraid
 */
public class RequestBRRecentProductList extends ClientPacket
{
	@Override
	protected void readImpl()
	{
	}
	
	@Override
	protected void runImpl()
	{
	}
}
