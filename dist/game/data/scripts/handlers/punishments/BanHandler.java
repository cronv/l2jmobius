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
package handlers.punishments;

import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.handler.IPunishmentHandler;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.network.Disconnection;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.LeaveWorld;

/**
 * This class manages the logic for applying ban punishments to players.<br>
 * It ensures that banned accounts are restricted from accessing the game world.
 * @author UnAfraid
 */
public class BanHandler implements IPunishmentHandler
{
	/**
	 * Starts the punishment process for a specific task.<br>
	 * This method identifies affected players based on the {@code PunishmentTask} data.<br>
	 * It applies the ban to characters, accounts, IP addresses, or hardware IDs.
	 * @param task The {@code PunishmentTask} containing the details of the punishment.
	 */
	@Override
	public void onStart(PunishmentTask task)
	{
		switch (task.getAffect())
		{
			case CHARACTER:
			{
				final int objectId = Integer.parseInt(String.valueOf(task.getKey()));
				final Player player = World.getPlayer(objectId);
				if (player != null)
				{
					applyToPlayer(player);
				}
				break;
			}
			case ACCOUNT:
			{
				final String account = String.valueOf(task.getKey());
				final GameClient client = LoginServerThread.getInstance().getClient(account);
				if (client != null)
				{
					final Player player = client.getPlayer();
					if (player != null)
					{
						applyToPlayer(player);
					}
					else
					{
						Disconnection.of(client).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
					}
				}
				break;
			}
			case IP:
			{
				final String ip = String.valueOf(task.getKey());
				for (Player player : World.getPlayers())
				{
					if (player.getIPAddress().equals(ip))
					{
						applyToPlayer(player);
					}
				}
				break;
			}
			case HWID:
			{
				final String hwid = String.valueOf(task.getKey());
				for (Player player : World.getPlayers())
				{
					final GameClient client = player.getClient();
					if ((client != null) && client.getHardwareInfo().getMacAddress().equals(hwid))
					{
						applyToPlayer(player);
					}
				}
				break;
			}
		}
	}
	
	/**
	 * This method is called when a {@code PunishmentTask} finishes.<br>
	 * It currently performs no actions.
	 * @param task The {@code PunishmentTask} that has ended.
	 */
	@Override
	public void onEnd(PunishmentTask task)
	{
		// Should not do anything.
	}
	
	/**
	 * This method applies the ban to a specific {@code Player}.<br>
	 * It disconnects the player from the game world.<br>
	 * The disconnection uses the {@code LeaveWorld.STATIC_PACKET}.
	 * @param player The {@code Player} object to be disconnected.
	 */
	private void applyToPlayer(Player player)
	{
		Disconnection.of(player).storeAndDeleteWith(LeaveWorld.STATIC_PACKET);
	}
	
	/**
	 * Retrieves the type of punishment associated with this handler.<br>
	 * This method returns a constant value from {@code PunishmentType}.
	 * @return The {@code PunishmentType} for this handler.
	 */
	@Override
	public PunishmentType getType()
	{
		return PunishmentType.BAN;
	}
}
