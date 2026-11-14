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
package handlers.punishments;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.time.TimeUtil;
import org.l2jmobius.gameserver.LoginServerThread;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.tasks.player.TeleportTask;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.type.JailZone;
import org.l2jmobius.gameserver.handler.IPunishmentHandler;
import org.l2jmobius.gameserver.managers.PunishmentManager;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentAffect;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentTask;
import org.l2jmobius.gameserver.mechanics.punishment.PunishmentType;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class manages the logic for the jail punishment system.<br>
 * It handles placing players into a {@code JailZone} and managing their restricted status.
 * @author UnAfraid, Skache
 */
public class JailHandler implements IPunishmentHandler
{
	/**
	 * Initializes a new instance of the {@code JailHandler}.<br>
	 * This constructor registers the necessary global listeners for handling jail punishments.
	 */
	public JailHandler()
	{
		// Register global listener.
		Containers.Global().addListener(new ConsumerEventListener(Containers.Global(), EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
	}
	
	/**
	 * Handles jail punishment logic when a player logs in.<br>
	 * Checks if the player is jailed and moves them to the correct zone if needed.<br>
	 * Sends a message to the player regarding their remaining jail time.<br>
	 * Removes the jail status if a non-GM player is found inside the jail zone.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event.getPlayer();
		
		if (player.isJailed() && !player.isInsideZone(ZoneId.JAIL))
		{
			applyToPlayer(null, player);
		}
		else if (player.isJailed())
		{
			// Notify about remaining jail time.
			PunishmentTask task = PunishmentManager.getInstance().getPunishment(player.getObjectId(), PunishmentAffect.CHARACTER, PunishmentType.JAIL);
			if (task != null)
			{
				long timeRemaining = task.getExpirationTime() - System.currentTimeMillis();
				if (timeRemaining > 0)
				{
					timeRemaining = TimeUtil.roundUpToSecond(timeRemaining);
					
					String message = "Jail time remaining: " + TimeUtil.formatDuration(timeRemaining) + ".";
					player.sendMessage(message);
				}
				else
				{
					player.sendMessage("You are jailed forever.");
				}
			}
		}
		else if (!player.isJailed() && player.isInsideZone(ZoneId.JAIL) && !player.isGM())
		{
			removeFromPlayer(player);
		}
	}
	
	/**
	 * Starts the punishment process for a specific task.<br>
	 * This method identifies affected players based on the {@code PunishmentTask} type.<br>
	 * It applies the punishment to characters, accounts, IP addresses, or hardware IDs.
	 * @param task The {@code PunishmentTask} containing the details of the punishment to apply.
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
	 * It removes the jail effect from the affected player or account.<br>
	 * The logic depends on whether the affect type is {@code CHARACTER}, {@code ACCOUNT}, {@code IP}, or {@code HWID}.
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
	 * Executes the jail punishment logic for a specific player.<br>
	 * This method handles teleportation, olympiad status updates, and UI messages.
	 * @param task The {@code PunishmentTask} containing details about the punishment.
	 * @param player The {@code Player} who is receiving the punishment.
	 */
	private void applyToPlayer(PunishmentTask task, Player player)
	{
		player.setInstance(null);
		
		if (OlympiadManager.getInstance().isRegisteredInComp(player))
		{
			OlympiadManager.getInstance().removeDisconnectedCompetitor(player);
		}
		
		ThreadPool.schedule(new TeleportTask(player, JailZone.getLocationIn()), 2000);
		
		// Show jail reason HTML message.
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		String content = HtmCache.getInstance().getHtm(player, "data/html/jail_in.htm");
		if (content != null)
		{
			content = content.replace("%reason%", task != null ? task.getReason() : "");
			content = content.replace("%punishedBy%", task != null ? task.getPunishedBy() : "");
			msg.setHtml(content);
		}
		else
		{
			msg.setHtml("<html><body>You have been put in jail by an admin.</body></html>");
		}
		
		player.sendPacket(msg);
		
		// Inform the player about jail duration.
		if (task != null)
		{
			long jailDuration = task.getExpirationTime() - System.currentTimeMillis();
			if (jailDuration > 0)
			{
				jailDuration = TimeUtil.roundUpToSecond(jailDuration);
				
				String message = "You've been jailed for " + TimeUtil.formatDuration(jailDuration) + "!";
				player.sendMessage(message);
			}
			else
			{
				player.sendMessage("You've been jailed forever.");
			}
		}
	}
	
	/**
	 * Removes the punishment from a specific player.<br>
	 * This method schedules a teleport to move the player out of the jail zone.<br>
	 * It also sends an HTML message to notify the player that they are free.
	 * @param player The {@code Player} object to be released from jail.
	 */
	private void removeFromPlayer(Player player)
	{
		ThreadPool.schedule(new TeleportTask(player, JailZone.getLocationOut()), 2000);
		
		// Open a Html message to inform the player.
		final NpcHtmlMessage msg = new NpcHtmlMessage();
		final String content = HtmCache.getInstance().getHtm(player, "data/html/jail_out.htm");
		if (content != null)
		{
			msg.setHtml(content);
		}
		else
		{
			msg.setHtml("<html><body>You are free for now, respect server rules!</body></html>");
		}
		
		player.sendPacket(msg);
	}
	
	/**
	 * Retrieves the punishment type associated with this handler.<br>
	 * This method returns the {@code JAIL} constant from the {@code PunishmentType} enum.
	 * @return The {@code PunishmentType} of this handler.
	 */
	@Override
	public PunishmentType getType()
	{
		return PunishmentType.JAIL;
	}
}
