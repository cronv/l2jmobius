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

import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.managers.FortSiegeManager;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.siege.FortSiege;
import org.l2jmobius.gameserver.mechanics.siege.FortSpawnHolder;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet sends fortress map information to the client.<br>
 * It provides details about active {@code FortSiege} instances and their associated spawns.
 * @author KenM
 */
public class ExShowFortressMapInfo extends ServerPacket
{
	private final Fort _fortress;
	private final FortSiege _siege;
	private final List<FortSpawnHolder> _commanders;
	
	/**
	 * Creates a new {@code ExShowFortressMapInfo} packet.<br>
	 * This method initializes the data for a specific fortress siege.<br>
	 * It retrieves the current siege and commander spawn list based on the residence ID.
	 * @param fortress The {@code Fort} object containing the siege information.
	 */
	public ExShowFortressMapInfo(Fort fortress)
	{
		_fortress = fortress;
		_siege = fortress.getSiege();
		_commanders = FortSiegeManager.getInstance().getCommanderSpawnList(fortress.getResidenceId());
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_FORTRESS_MAP_INFO.writeId(this, buffer);
		buffer.writeInt(_fortress.getResidenceId());
		buffer.writeInt(_siege.isInProgress()); // fortress siege status
		buffer.writeInt(_fortress.getFortSize()); // barracks count
		if ((_commanders != null) && !_commanders.isEmpty() && _siege.isInProgress())
		{
			switch (_commanders.size())
			{
				case 3:
				{
					for (FortSpawnHolder spawn : _commanders)
					{
						if (isSpawned(spawn.getId()))
						{
							buffer.writeInt(0);
						}
						else
						{
							buffer.writeInt(1);
						}
					}
					break;
				}
				case 4: // TODO: change 4 to 5 once control room supported
				{
					int count = 0;
					for (FortSpawnHolder spawn : _commanders)
					{
						count++;
						if (count == 4)
						{
							buffer.writeInt(1); // TODO: control room emulated
						}
						
						if (isSpawned(spawn.getId()))
						{
							buffer.writeInt(0);
						}
						else
						{
							buffer.writeInt(1);
						}
					}
					break;
				}
			}
		}
		else
		{
			for (int i = 0; i < _fortress.getFortSize(); i++)
			{
				buffer.writeInt(0);
			}
		}
	}
	
	/**
	 * Checks if a specific NPC is currently spawned as a commander.<br>
	 * It iterates through the list of commanders in the current siege.
	 * @param npcId The unique identifier of the NPC to check.
	 * @return {@code true} if the NPC is found, otherwise {@code false}.
	 */
	private boolean isSpawned(int npcId)
	{
		boolean ret = false;
		for (Spawn spawn : _siege.getCommanders())
		{
			if (spawn.getId() == npcId)
			{
				ret = true;
				break;
			}
		}
		
		return ret;
	}
}
