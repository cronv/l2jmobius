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
package handlers.chat.commands.admin;

import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * This class handles administrative commands related to the castle siege system.<br>
 * It allows administrators to interact with {@code Castle} mechanics and manage siege-related data.
 * @author St3eT
 */
public class AdminCastle implements IAdminCommandHandler
{
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_castlemanage",
	};
	
	/**
	 * Processes admin commands related to castle management.<br>
	 * It handles actions like starting sieges, changing owners, and switching sides.
	 * @param command The full command string entered by the player.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return {@code true} if the command was processed successfully, otherwise {@code false}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		if (actualCommand.equals("admin_castlemanage"))
		{
			if (st.hasMoreTokens())
			{
				final String param = st.nextToken();
				final Castle castle;
				if (StringUtil.isNumeric(param))
				{
					castle = CastleManager.getInstance().getCastleById(Integer.parseInt(param));
				}
				else
				{
					castle = CastleManager.getInstance().getCastle(param);
				}
				
				if (castle == null)
				{
					activeChar.sendSysMessage("Invalid parameters! Usage: //castlemanage <castleId[1-9] / castleName>");
					return false;
				}
				
				final int residenceId = castle.getResidenceId();
				if (!st.hasMoreTokens())
				{
					showCastleMenu(activeChar, residenceId);
				}
				else
				{
					final String action = st.nextToken();
					final Player target = checkTarget(activeChar) ? activeChar.getTarget().asPlayer() : null;
					switch (action)
					{
						case "showRegWindow":
						{
							castle.getSiege().listRegisterClan(activeChar);
							break;
						}
						case "addAttacker":
						{
							if (checkTarget(activeChar))
							{
								castle.getSiege().registerAttacker(target, true);
							}
							else
							{
								activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
							}
							break;
						}
						case "removeAttacker":
						{
							if (checkTarget(activeChar))
							{
								castle.getSiege().removeSiegeClan(activeChar);
							}
							else
							{
								activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
							}
							break;
						}
						case "addDeffender":
						{
							if (checkTarget(activeChar))
							{
								castle.getSiege().registerDefender(target, true);
							}
							else
							{
								activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
							}
							break;
						}
						case "removeDeffender":
						{
							if (checkTarget(activeChar))
							{
								castle.getSiege().removeSiegeClan(target);
							}
							else
							{
								activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
							}
							break;
						}
						case "startSiege":
						{
							if (!castle.getSiege().getAttackerClans().isEmpty())
							{
								castle.getSiege().startSiege();
							}
							else
							{
								activeChar.sendSysMessage("There is currently not registered any clan for castle siege!");
							}
							break;
						}
						case "stopSiege":
						{
							if (castle.getSiege().isInProgress())
							{
								castle.getSiege().endSiege();
							}
							else
							{
								activeChar.sendSysMessage("Castle siege is not currently in progress!");
							}
							
							showCastleMenu(activeChar, residenceId);
							break;
						}
						case "setOwner":
						{
							if ((target == null) || !checkTarget(activeChar))
							{
								activeChar.sendPacket(SystemMessageId.INVALID_TARGET);
							}
							else if (target.getClan().getCastleId() > 0)
							{
								activeChar.sendSysMessage("This clan already have castle!");
							}
							else if (castle.getOwner() != null)
							{
								activeChar.sendSysMessage("This castle is already taken by another clan!");
							}
							else if (!st.hasMoreTokens())
							{
								activeChar.sendSysMessage("Invalid parameters!!");
							}
							else
							{
								final CastleSide side = Enum.valueOf(CastleSide.class, st.nextToken().toUpperCase());
								if (side != null)
								{
									castle.setSide(side);
									castle.setOwner(target.getClan());
								}
							}
							
							showCastleMenu(activeChar, residenceId);
							break;
						}
						case "takeCastle":
						{
							final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
							if (clan != null)
							{
								castle.removeOwner(clan);
							}
							else
							{
								activeChar.sendSysMessage("Error during removing castle!");
							}
							
							showCastleMenu(activeChar, residenceId);
							break;
						}
						case "switchSide":
						{
							final CastleSide currentSide = castle.getSide();
							if (currentSide == CastleSide.DARK)
							{
								castle.setSide(CastleSide.LIGHT);
							}
							else if (currentSide == CastleSide.LIGHT)
							{
								castle.setSide(CastleSide.DARK);
							}
							else
							{
								activeChar.sendSysMessage("You can't switch sides when is castle neutral!");
							}
							
							showCastleMenu(activeChar, residenceId);
							break;
						}
					}
				}
			}
			else
			{
				final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
				html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/castlemanage.htm"));
				activeChar.sendPacket(html);
			}
		}
		
		return true;
	}
	
	/**
	 * Displays the management menu for a specific castle to an admin.<br>
	 * It retrieves the castle data and sends an {@code NpcHtmlMessage} to the player.<br>
	 * The message is populated with details like the name, owner, and side of the castle.
	 * @param player The {@code Player} who will receive the menu packet.
	 * @param castleId The unique identifier for the castle to display.
	 */
	private void showCastleMenu(Player player, int castleId)
	{
		final Castle castle = CastleManager.getInstance().getCastleById(castleId);
		if (castle != null)
		{
			final Clan ownerClan = castle.getOwner();
			final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
			html.setHtml(HtmCache.getInstance().getHtm(player, "data/html/admin/castlemanage_selected.htm"));
			html.replace("%castleId%", castle.getResidenceId());
			html.replace("%castleName%", castle.getName());
			html.replace("%ownerName%", ownerClan != null ? ownerClan.getLeaderName() : "NPC");
			html.replace("%ownerClan%", ownerClan != null ? ownerClan.getName() : "NPC");
			html.replace("%castleSide%", StringUtil.capitalizeFirst(castle.getSide().toString().toLowerCase()));
			player.sendPacket(html);
		}
	}
	
	/**
	 * Checks if the {@code Player} has a valid target.<br>
	 * The target must be another {@code Player}.<br>
	 * The target must also belong to a {@code Clan}.
	 * @param player The {@code Player} who is performing the action.
	 * @return {@code true} if the target meets all requirements, otherwise {@code false}.
	 */
	private boolean checkTarget(Player player)
	{
		final WorldObject target = player.getTarget();
		if (target != null)
		{
			return (target.isPlayer() && (target.asPlayer().getClan() != null));
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all registered command strings for this handler.
	 * @return an array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
