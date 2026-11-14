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
package handlers.bypass.communityboard;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.communitybbs.BB.Forum;
import org.l2jmobius.gameserver.communitybbs.Manager.TopicBBSManager;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.handler.CommunityBoardHandler;
import org.l2jmobius.gameserver.handler.IWriteBoardHandler;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.HtmlUtil;

/**
 * Handles the logic for the clan community board.<br>
 * This class allows players to interact with and write messages on a specific board associated with a {@code Clan}.
 * @author Zoey76, Mobius
 */
public class ClanBoard implements IWriteBoardHandler
{
	private static final String[] COMMANDS =
	{
		"_bbsclan",
		"_bbsclan_list",
		"_bbsclan_clanhome"
	};
	
	/**
	 * Retrieves the list of supported commands.<br>
	 * This method returns all valid command strings for this board.
	 * @return an array of {@code String} containing the allowed commands.
	 */
	@Override
	public String[] getCommandList()
	{
		return COMMANDS;
	}
	
	/**
	 * Processes bypass commands related to the clan board.<br>
	 * It handles listing clans, viewing clan homes, and managing notices.
	 * @param command The string command sent by the player.
	 * @param player The {@code Player} object who executed the command.
	 * @return {@code true} if the command was processed successfully.
	 */
	@Override
	public boolean onCommand(String command, Player player)
	{
		if (command.equals("_bbsclan"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan", command);
			final Clan clan = player.getClan();
			if ((clan == null) || (clan.getLevel() < 2))
			{
				clanList(player, 1);
			}
			else
			{
				clanHome(player);
			}
		}
		else if (command.startsWith("_bbsclan_clanlist"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan List", command);
			if (command.equals("_bbsclan_clanlist"))
			{
				clanList(player, 1);
			}
			else if (command.startsWith("_bbsclan_clanlist;"))
			{
				try
				{
					clanList(player, Integer.parseInt(command.split(";")[1]));
				}
				catch (Exception e)
				{
					clanList(player, 1);
					LOG.warning(ClanBoard.class.getSimpleName() + ": " + player + " send invalid clan list bypass " + command + "!");
				}
			}
		}
		else if (command.startsWith("_bbsclan_clanhome"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Home", command);
			if (command.equals("_bbsclan_clanhome"))
			{
				clanHome(player);
			}
			else if (command.startsWith("_bbsclan_clanhome;"))
			{
				try
				{
					final String[] params = command.split(";");
					final int clanId = Integer.parseInt(params[1]);
					// Only members reach the boards of their own clan.
					if ((params.length > 2) && (player.getClanId() == clanId) && (params[2].equals("announce") || params[2].equals("cbb")))
					{
						showClanBoard(player, params[2]);
					}
					else
					{
						clanHome(player, clanId);
					}
				}
				catch (Exception e)
				{
					clanHome(player);
					LOG.warning(ClanBoard.class.getSimpleName() + ": " + player + " send invalid clan home bypass " + command + "!");
				}
			}
		}
		else if (command.startsWith("_bbsclan_clannotice_edit;"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Edit", command);
			clanNotice(player, player.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_enable"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Notice Enable", command);
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setNoticeEnabled(true);
			}
			
			clanNotice(player, player.getClanId());
		}
		else if (command.startsWith("_bbsclan_clannotice_disable"))
		{
			CommunityBoardHandler.getInstance().addBypass(player, "Clan Notice Disable", command);
			final Clan clan = player.getClan();
			if (clan != null)
			{
				clan.setNoticeEnabled(false);
			}
			
			clanNotice(player, player.getClanId());
		}
		else
		{
			CommunityBoardHandler.separateAndSend("<html><body><br><br><center>Command " + command + " need development.</center><br><br></body></html>", player);
		}
		
		return true;
	}
	
	/**
	 * Displays the clan notice interface to a specific player.<br>
	 * It checks if the clan level is sufficient to have communities.<br>
	 * If the player is the leader, it allows them to edit the notice.<br>
	 * Otherwise, it displays the current notice for viewing.
	 * @param player The {@code Player} who is viewing the notice.
	 * @param clanId The unique identifier of the clan.
	 */
	private void clanNotice(Player player, int clanId)
	{
		final Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			if (cl.getLevel() < 2)
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_COMMUNITIES_IN_MY_CLAN_CLAN_COMMUNITIES_ARE_ALLOWED_FOR_CLANS_WITH_SKILL_LEVELS_OF_2_AND_HIGHER);
				onCommand("_bbsclan_clanlist", player);
			}
			else
			{
				final StringBuilder html = new StringBuilder(2048);
				html.append("<html><body><br><br><table border=0 width=610><tr><td width=10></td><td width=600 align=left><a action=\"bypass _bbshome\">HOME</a> &gt; <a action=\"bypass _bbsclan_clanlist\"> CLAN COMMUNITY </a>  &gt; <a action=\"bypass _bbsclan_clanhome;");
				html.append(clanId);
				html.append("\"> &amp;$802; </a></td></tr></table>");
				if (player.isClanLeader())
				{
					html.append("<br><br><center><table width=610 border=0 cellspacing=0 cellpadding=0><tr><td fixwidth=610><font color=\"AAAAAA\">The Clan Notice function allows the clan leader to send messages through a pop-up window to clan members at login.</font> </td></tr><tr><td height=20></td></tr>");
					final Clan clan = player.getClan();
					if (clan.isNoticeEnabled())
					{
						html.append("<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;on&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_disable\">off</a>");
					}
					else
					{
						html.append("<tr><td fixwidth=610> Clan Notice Function:&nbsp;&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_enable\">on</a>&nbsp;&nbsp;&nbsp;/&nbsp;&nbsp;&nbsp;off");
					}
					
					html.append("</td></tr></table><img src=\"L2UI.Squaregray\" width=\"610\" height=\"1\"><br> <br><table width=610 border=0 cellspacing=2 cellpadding=0><tr><td>Edit Notice: </td></tr><tr><td height=5></td></tr><tr><td><MultiEdit var =\"Content\" width=610 height=100></td></tr></table><br><table width=610 border=0 cellspacing=0 cellpadding=0><tr><td height=5></td></tr><tr><td align=center FIXWIDTH=65><button value=\"&$140;\" action=\"Write Notice Set _ Content Content Content\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\" ></td><td align=center FIXWIDTH=45></td><td align=center FIXWIDTH=500></td></tr></table></center></body></html>");
					HtmlUtil.sendCBHtml(player, html.toString(), clan.getNotice());
				}
				else
				{
					html.append("<img src=\"L2UI.squareblank\" width=\"1\" height=\"10\"><center><table border=0 cellspacing=0 cellpadding=0><tr><td>You are not your clan's leader, and therefore cannot change the clan notice</td></tr></table>");
					final Clan clan = player.getClan();
					if (clan.isNoticeEnabled())
					{
						html.append("<table border=0 cellspacing=0 cellpadding=0><tr><td>The current clan notice:</td></tr><tr><td fixwidth=5></td><td FIXWIDTH=600 align=left>" + clan.getNotice() + "</td><td fixqqwidth=5></td></tr></table>");
					}
					html.append("</center></body></html>");
					CommunityBoardHandler.separateAndSend(html.toString(), player);
				}
			}
		}
	}
	
	/**
	 * Displays a paginated list of clans to the player.<br>
	 * This method generates an HTML table containing clan details based on the provided page index.<br>
	 * It handles navigation buttons and sends the final message via {@code CommunityBoardHandler}.
	 * @param player The {@code Player} who will receive the clan list.
	 * @param indexValue The page number to display.
	 */
	private void clanList(Player player, int indexValue)
	{
		int index = indexValue;
		if (index < 1)
		{
			index = 1;
		}
		
		// header
		final StringBuilder html = new StringBuilder(2048);
		html.append("<html><body><br><br><center><br1><br1><table border=0 cellspacing=0 cellpadding=0><tr><td FIXWIDTH=15>&nbsp;</td><td width=610 height=30 align=left><a action=\"bypass _bbshome\">HOME</a> &nbsp;&gt;<a action=\"bypass _bbsclan_clanlist\"> CLAN COMMUNITY </a></td></tr></table><table border=0 cellspacing=0 cellpadding=0 width=610 bgcolor=434343><tr><td height=10></td></tr><tr><td fixWIDTH=5></td><td fixWIDTH=600><a action=\"bypass _bbsclan_clanhome;");
		final Clan playerClan = player.getClan();
		html.append(playerClan != null ? playerClan.getId() : 0);
		html.append("\">[GO TO MY CLAN]</a>&nbsp;&nbsp;</td><td fixWIDTH=5></td></tr><tr><td height=10></td></tr></table><br><table border=0 cellspacing=0 cellpadding=2 bgcolor=5A5A5A width=610><tr><td FIXWIDTH=5></td><td FIXWIDTH=200 align=center>CLAN NAME</td><td FIXWIDTH=200 align=center>CLAN LEADER</td><td FIXWIDTH=100 align=center>CLAN LEVEL</td><td FIXWIDTH=100 align=center>CLAN MEMBERS</td><td FIXWIDTH=5></td></tr></table><img src=\"L2UI.Squareblank\" width=\"1\" height=\"5\">");
		int i = 0;
		for (Clan cl : ClanTable.getInstance().getClans())
		{
			if (i > ((index + 1) * 7))
			{
				break;
			}
			
			if (i++ >= ((index - 1) * 7))
			{
				html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><table border=0 cellspacing=0 cellpadding=0 width=610><tr> <td FIXWIDTH=5></td><td FIXWIDTH=200 align=center><a action=\"bypass _bbsclan_clanhome;");
				html.append(cl.getId());
				html.append("\">");
				html.append(cl.getName());
				html.append("</a></td><td FIXWIDTH=200 align=center>");
				html.append(cl.getLeaderName());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(cl.getLevel());
				html.append("</td><td FIXWIDTH=100 align=center>");
				html.append(cl.getMembersCount());
				html.append("</td><td FIXWIDTH=5></td></tr><tr><td height=5></td></tr></table><img src=\"L2UI.SquareBlank\" width=\"610\" height=\"3\"><img src=\"L2UI.SquareGray\" width=\"610\" height=\"1\">");
			}
		}
		
		html.append("<img src=\"L2UI.SquareBlank\" width=\"610\" height=\"2\"><table cellpadding=0 cellspacing=2 border=0><tr>");
		if (index == 1)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"_bbsclan_clanlist;");
			html.append(index - 1);
			html.append("\" back=\"l2ui_ch3.prev1_down\" fore=\"l2ui_ch3.prev1\" width=16 height=16 ></td>");
		}
		
		final int clanCount = ClanTable.getInstance().getClanCount();
		int nbp = clanCount / 8;
		if ((nbp * 8) != clanCount)
		{
			nbp++;
		}
		
		for (i = 1; i <= nbp; i++)
		{
			if (i == index)
			{
				html.append("<td> ");
				html.append(i);
				html.append(" </td>");
			}
			else
			{
				html.append("<td><a action=\"bypass _bbsclan_clanlist;");
				html.append(i);
				html.append("\"> ");
				html.append(i);
				html.append(" </a></td>");
			}
		}
		
		if (index == nbp)
		{
			html.append("<td><button action=\"\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		else
		{
			html.append("<td><button action=\"bypass _bbsclan_clanlist;");
			html.append(index + 1);
			html.append("\" back=\"l2ui_ch3.next1_down\" fore=\"l2ui_ch3.next1\" width=16 height=16 ></td>");
		}
		html.append("</tr></table><table border=0 cellspacing=0 cellpadding=0><tr><td width=610><img src=\"sek.cbui141\" width=\"610\" height=\"1\"></td></tr></table><table border=0><tr><td><combobox width=65 var=keyword list=\"Name;Ruler\"></td><td><edit var = \"Search\" width=130 height=11 length=\"16\"></td>" +
		// TODO: search (Write in BBS)
			"<td><button value=\"&$420;\" action=\"Write 5 -1 0 Search keyword keyword\" back=\"L2UI_CT1.Button_DF_Down\" width=65 height=20 fore=\"L2UI_CT1.Button_DF\"> </td> </tr></table><br><br></center></body></html>");
		CommunityBoardHandler.separateAndSend(html.toString(), player);
	}
	
	/**
	 * Teleports the {@code Player} to their clan home.<br>
	 * This method retrieves the current clan ID from the {@code Player}.<br>
	 * It then calls the overloaded {@code clanHome} method with that ID.
	 * @param player The {@code Player} who will be teleported.
	 */
	private void clanHome(Player player)
	{
		clanHome(player, player.getClan().getId());
	}
	
	/**
	 * Displays the home page for a specific clan.<br>
	 * It checks if the clan exists and meets the minimum level requirement.<br>
	 * If requirements are met, it sends an HTML layout with clan details to the player.
	 * @param player The {@code Player} who is viewing the board.
	 * @param clanId The unique identifier of the {@code Clan}.
	 */
	private void clanHome(Player player, int clanId)
	{
		final Clan cl = ClanTable.getInstance().getClan(clanId);
		if (cl != null)
		{
			final int clanLevel = cl.getLevel();
			if (clanLevel < 2)
			{
				player.sendPacket(SystemMessageId.THERE_ARE_NO_COMMUNITIES_IN_MY_CLAN_CLAN_COMMUNITIES_ARE_ALLOWED_FOR_CLANS_WITH_SKILL_LEVELS_OF_2_AND_HIGHER);
				onCommand("_bbsclan_clanlist", player);
			}
			else
			{
				final String html = Arrays.asList("<html><body><center><br><br><br1><br1><table border=0 cellspacing=0 cellpadding=0><tr><td FIXWIDTH=15>&nbsp;</td><td width=610 height=30 align=left><a action=\"bypass _bbshome\">HOME</a> &nbsp;&gt; <a action=\"bypass _bbsclan_clanlist\"> CLAN COMMUNITY </a> &nbsp;&gt; <a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), "\"> &amp;$802; </a></td></tr></table><table border=0 cellspacing=0 cellpadding=0 width=610 bgcolor=434343><tr><td height=10></td></tr><tr><td fixWIDTH=5></td><td fixwidth=600><a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";announce\">[CLAN ANNOUNCEMENT]</a> <a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";cbb\">[CLAN BULLETIN BOARD]</a><a action=\"bypass _bbsclan_clanhome;", String.valueOf(clanId), ";cmail\">[CLAN MAIL]</a>&nbsp;&nbsp;<a action=\"bypass _bbsclan_clannotice_edit;", String.valueOf(clanId), ";cnotice\">[CLAN NOTICE]</a>&nbsp;&nbsp;</td><td fixWIDTH=5></td></tr><tr><td height=10></td></tr></table><table border=0 cellspacing=0 cellpadding=0 width=610><tr><td height=10></td></tr><tr><td fixWIDTH=5></td><td fixwidth=290 valign=top></td><td fixWIDTH=5></td><td fixWIDTH=5 align=center valign=top><img src=\"l2ui.squaregray\" width=2  height=128></td><td fixWIDTH=5></td><td fixwidth=295><table border=0 cellspacing=0 cellpadding=0 width=295><tr><td fixWIDTH=100 align=left>CLAN NAME</td><td fixWIDTH=195 align=left>", cl.getName(), "</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left>CLAN LEVEL</td><td fixWIDTH=195 align=left height=16>", String.valueOf(clanLevel), "</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left>CLAN MEMBERS</td><td fixWIDTH=195 align=left height=16>", String.valueOf(cl.getMembersCount()), "</td></tr><tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left>CLAN LEADER</td><td fixWIDTH=195 align=left height=16>", cl.getLeaderName(), "</td></tr><tr><td height=7></td></tr>" +
				
				// ADMINISTRATOR ??
				/*
				 * html.append("<tr>"); html.append("<td fixWIDTH=100 align=left>ADMINISTRATOR</td>"); html.append("<td fixWIDTH=195 align=left height=16>"+cl.getLeaderName()+"</td>"); html.append("</tr>");
				 */
					"<tr><td height=7></td></tr><tr><td fixWIDTH=100 align=left>ALLIANCE</td><td fixWIDTH=195 align=left height=16>", (cl.getAllyName() != null) ? cl.getAllyName() : "", "</td></tr></table></td><td fixWIDTH=5></td></tr><tr><td height=10></td></tr></table>" +
				// TODO: the BB for clan :)
				// html.append("<table border=0 cellspacing=0 cellpadding=0 width=610 bgcolor=333333>");
						"<img src=\"L2UI.squareblank\" width=\"1\" height=\"5\"><img src=\"L2UI.squaregray\" width=\"610\" height=\"1\"><br></center><br> <br></body></html>").stream().collect(Collectors.joining());
				CommunityBoardHandler.separateAndSend(html, player);
			}
		}
	}
	
	/**
	 * Shows the announcement or free board of the {@code Player} clan.
	 * @param player The {@code Player} who opened the board.
	 * @param type {@code announce} for the announcement board, {@code cbb} for the free board.
	 */
	private void showClanBoard(Player player, String type)
	{
		final Clan clan = player.getClan();
		final Forum forum = (clan == null) ? null : clan.getBoard(type.equals("announce") ? Clan.ANNOUNCEMENT_BOARD : Clan.FREE_BOARD);
		if (forum == null)
		{
			clanHome(player);
			return;
		}
		
		TopicBBSManager.getInstance().parsecmd("_bbstopics;read;" + forum.getID(), player);
	}
	
	/**
	 * Processes the command to update a clan notice.<br>
	 * This method checks if the {@code Player} is a clan leader.<br>
	 * If valid, it updates the notice using the third argument.
	 * @param player The {@code Player} attempting to run the command.
	 * @param arg1 The first command argument.
	 * @param arg2 The second command argument.
	 * @param arg3 The content of the new clan notice.
	 * @param arg4 The fourth command argument.
	 * @param arg5 The fifth command argument.
	 * @return {@code true} if the command was processed successfully.
	 */
	@Override
	public boolean writeCommunityBoardCommand(Player player, String arg1, String arg2, String arg3, String arg4, String arg5)
	{
		// the only Write bypass that comes to this handler is "Write Notice Set _ Content Content Content";
		// arg1 = Set, arg2 = _
		final Clan clan = player.getClan();
		if ((clan != null) && player.isClanLeader())
		{
			clan.setNotice(arg3);
			player.sendPacket(SystemMessageId.YOUR_CLAN_NOTICE_HAS_BEEN_SAVED);
		}
		
		return true;
	}
}
