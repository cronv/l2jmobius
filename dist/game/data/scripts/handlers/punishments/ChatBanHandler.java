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
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * This class handles the logic for {@code PunishmentType}.CHAT_BAN punishments.<br>
 * It manages restricting a player's ability to use the chat system.
 * @author UnAfraid
 */
public class ChatBanHandler implements IPunishmentHandler
{
	/**
	 * Starts the punishment process for a specific task.<br>
	 * This method identifies affected players based on the {@code PunishmentTask} data.<br>
	 * It applies the chat ban to characters, accounts, IP addresses, or hardware IDs.
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
					applyToPlayer(task, player);
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
						applyToPlayer(task, player);
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
						applyToPlayer(task, player);
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
						applyToPlayer(task, player);
					}
				}
				break;
			}
		}
	}
	
	/**
	 * This method is called when a punishment task finishes.<br>
	 * It removes the active chat ban from the affected player or account.<br>
	 * The logic checks if the target is a character, account, IP, or HWID.
	 * @param task The {@code PunishmentTask} that has completed.
	 */
	@Override
	public void onEnd(PunishmentTask task)
	{
		switch (task.getAffect())
		{
			case CHARACTER:
			{
				final int objectId = Integer.parseInt(String.valueOf(task.getKey()));
				final Player player = World.getPlayer(objectId);
				if (player != null)
				{
					removeFromPlayer(player);
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
						removeFromPlayer(player);
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
						removeFromPlayer(player);
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
						removeFromPlayer(player);
					}
				}
				break;
			}
		}
	}
	
	/**
	 * Applies a specific punishment to a {@code Player}.<br>
	 * It calculates the remaining time for the ban.<br>
	 * It sends a notification message and an update packet to the player.
	 * @param task The {@code PunishmentTask} containing the ban details.
	 * @param player The {@code Player} who will receive the punishment.
	 */
	private void applyToPlayer(PunishmentTask task, Player player)
	{
		final long delay = ((task.getExpirationTime() - System.currentTimeMillis()) / 1000) + 1;
		if (delay > 0)
		{
			final long hours = delay / 3600;
			final long minutes = (delay % 3600) / 60;
			String message = "You've been chat banned for ";
			if (hours > 0)
			{
				message += hours + " hour" + (hours > 1 ? "'s." : ".");
				if (minutes > 0)
				{
					message += " and ";
				}
			}
			
			if ((minutes > 0) || (hours == 0))
			{
				message += minutes + " minute" + (minutes > 1 ? "'s." : ".");
			}
			
			player.sendPacket(SystemMessageId.CHAT_DISABLED);
			player.sendMessage(message);
		}
		else
		{
			player.sendMessage("You've been chat banned forever.");
		}
		
		player.sendPacket(new EtcStatusUpdate(player));
	}
	
	/**
	 * Removes the chat ban from a specific player.<br>
	 * This method enables chat for the {@code Player}.<br>
	 * It sends the appropriate system messages to the client.
	 * @param player The {@code Player} instance to update.
	 */
	private void removeFromPlayer(Player player)
	{
		player.sendPacket(SystemMessageId.CHAT_ENABLED);
		player.sendPacket(new EtcStatusUpdate(player));
	}
	
	/**
	 * Retrieves the type of punishment associated with this handler.<br>
	 * This method returns a constant value from {@code PunishmentType}.
	 * @return The {@code PunishmentType} for this specific handler.
	 */
	@Override
	public PunishmentType getType()
	{
		return PunishmentType.CHAT_BAN;
	}
}
