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
package handlers.bypass.npc;

import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.IBypassHandler;
import org.l2jmobius.gameserver.mechanics.siege.CastleTaxType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Handles the bypass for displaying territory status information to players.<br>
 * This class manages how {@code Npc} entities communicate territory data via {@code NpcHtmlMessage}.
 */
public class TerritoryStatus implements IBypassHandler
{
	private static final String[] COMMANDS =
	{
		"TerritoryStatus"
	};
	
	/**
	 * Handles the command to display territory status information.<br>
	 * This method checks if the target is an {@code Npc}.<br>
	 * It sends a custom HTML message to the {@code Player} based on the castle data.
	 * @param command The name of the command being executed.
	 * @param player The {@code Player} who sent the command.
	 * @param target The {@code Creature} being targeted by the player.
	 * @return {@code true} if the command was handled successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player player, Creature target)
	{
		if (!target.isNpc())
		{
			return false;
		}
		
		final Npc npc = target.asNpc();
		final int objectId = npc.getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		{
			if (npc.getCastle().getOwnerId() > 0)
			{
				html.setFile(player, "data/html/territorystatus.htm");
				final Clan clan = ClanTable.getInstance().getClan(npc.getCastle().getOwnerId());
				html.replace("%clanname%", clan.getName());
				html.replace("%clanleadername%", clan.getLeaderName());
			}
			else
			{
				html.setFile(player, "data/html/territorynoclan.htm");
			}
		}
		
		html.replace("%castlename%", npc.getCastle().getName());
		html.replace("%taxpercent%", Integer.toString(npc.getCastle().getTaxPercent(CastleTaxType.BUY)));
		html.replace("%objectId%", String.valueOf(objectId));
		{
			if (npc.getCastle().getResidenceId() > 6)
			{
				html.replace("%territory%", "The Kingdom of Elmore");
			}
			else
			{
				html.replace("%territory%", "The Kingdom of Aden");
			}
		}
		
		player.sendPacket(html);
		return true;
	}
	
	/**
	 * Retrieves the list of valid commands for this handler.<br>
	 * This method returns all strings that can be used to trigger the bypass.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
}
