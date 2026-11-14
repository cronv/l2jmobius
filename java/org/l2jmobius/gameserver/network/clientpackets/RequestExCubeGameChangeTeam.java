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

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.managers.HandysBlockCheckerManager;
import org.l2jmobius.gameserver.network.PacketLogger;

/**
 * Handles a request from the client to change teams within an ExCube game.<br>
 * It processes the {@code Arena} and {@code Team} identifiers provided in the packet.
 * @author mrTJO
 */
public class RequestExCubeGameChangeTeam extends ClientPacket
{
	private int _arena;
	private int _team;
	
	@Override
	protected void readImpl()
	{
		// client sends -1,0,1,2 for arena parameter
		_arena = readInt() + 1;
		_team = readInt();
	}
	
	@Override
	protected void runImpl()
	{
		// do not remove players after start
		if (HandysBlockCheckerManager.getInstance().arenaIsBeingUsed(_arena))
		{
			return;
		}
		
		final Player player = getPlayer();
		
		switch (_team)
		{
			case 0:
			case 1:
			{
				// Change Player Team
				HandysBlockCheckerManager.getInstance().changePlayerToTeam(player, _arena);
				break;
			}
			case -1:
			{
				// Remove Player (me)
			}
			{
				final int team = HandysBlockCheckerManager.getInstance().getHolder(_arena).getPlayerTeam(player);
				
				// The client sends this packet twice if they click exit, but does not send it on restart.
				if (team > -1)
				{
					HandysBlockCheckerManager.getInstance().removePlayer(player, _arena, team);
				}
				break;
			}
			default:
			{
				PacketLogger.warning("Wrong Cube Game Team ID: " + _team);
				break;
			}
		}
	}
}
