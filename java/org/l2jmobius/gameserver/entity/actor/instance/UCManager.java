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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.UndergroundColiseumConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.groups.Party;
import org.l2jmobius.gameserver.managers.games.UndergroundColiseumManager;
import org.l2jmobius.gameserver.mechanics.olympiad.OlympiadManager;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCArena;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCBestTeam;
import org.l2jmobius.gameserver.mechanics.undergroundColiseum.UCWaiting;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;

/**
 * Manages the logic and state for the Underground Coliseum instance.<br>
 * This class handles team organization, arena mechanics, and player progression within the {@code UCArena}.
 */
public class UCManager extends Folk
{
	private static final Logger LOGGER = Logger.getLogger(UCManager.class.getName());
	
	/**
	 * Creates a new instance of {@code UCManager}.<br>
	 * This constructor initializes the manager using a specific template.<br>
	 * It sets the instance type to {@code InstanceType.UCManagerInstance}.
	 * @param template The {@code NpcTemplate} used to create this manager.
	 */
	public UCManager(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.UCManagerInstance);
	}
	
	/**
	 * Processes bypass commands for the Underground Coliseum NPC.<br>
	 * It handles registration, cancellation, and team information requests.
	 * @param player The {@code Player} who sent the command.
	 * @param command The string command to execute.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		final NpcHtmlMessage html = new NpcHtmlMessage(getObjectId());
		final StringTokenizer token = new StringTokenizer(command, " ");
		final String actualCommand = token.nextToken();
		if (actualCommand.equalsIgnoreCase("register"))
		{
			try
			{
				if (!player.isInParty())
				{
					html.setFile(player, "data/html/undergroundColiseum/noTeam.htm");
					player.sendPacket(html);
					return;
				}
				
				if ((OlympiadManager.getInstance().isRegistered(player) || player.isInOlympiadMode() || player.isOnEvent() || player.isRegisteredOnEvent()))
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_BE_SIMULTANEOUSLY_REGISTERED_FOR_PVP_MATCHES_SUCH_AS_THE_OLYMPIAD_UNDERGROUND_COLISEUM_AERIAL_CLEFT_KRATEI_S_CUBE_AND_HANDY_S_BLOCK_CHECKERS);
					return;
				}
				
				if (player.isCursedWeaponEquipped())
				{
					player.sendPacket(SystemMessageId.YOU_CANNOT_REGISTER_WHILE_IN_POSSESSION_OF_A_CURSED_WEAPON);
					return;
				}
				
				if (!UndergroundColiseumManager.getInstance().isStarted())
				{
					html.setFile(player, "data/html/undergroundColiseum/notStarted.htm");
					player.sendPacket(html);
					return;
				}
				
				final Party party = player.getParty();
				if (!party.isLeader(player))
				{
					html.setFile(player, "data/html/undergroundColiseum/notPartyLeader.htm");
					player.sendPacket(html);
					return;
				}
				
				if (party.getUCState() instanceof UCWaiting)
				{
					html.setFile(player, "data/html/undergroundColiseum/alreadyRegistered.htm");
					player.sendPacket(html);
					return;
				}
				
				final int val = Integer.parseInt(token.nextToken());
				final UCArena arena = UndergroundColiseumManager.getInstance().getArena(val);
				if (arena == null)
				{
					player.sendMessage("This arena is temporarly unavailable.");
					return;
				}
				
				final Party team0Party = arena.getTeams()[0].getParty();
				final Party team1Party = arena.getTeams()[1].getParty();
				if ((team0Party != null) || (team1Party != null))
				{
					if ((team0Party == party) || (team1Party == party))
					{
						html.setFile(player, "data/html/undergroundColiseum/alreadyRegistered.htm");
						player.sendPacket(html);
						return;
					}
				}
				
				int realCount = 0;
				for (Player member : party.getMembers())
				{
					if (member == null)
					{
						continue;
					}
					
					if (member.getPlayerClass().level() < 2)
					{
						final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
						packet.setFile(member, "data/html/undergroundColiseum/wrongLevel.htm");
						packet.replace("%name%", member.getName());
						player.sendPacket(packet);
						return;
					}
					
					final int memberLevel = member.getLevel();
					if (!((memberLevel >= arena.getMinLevel()) && (memberLevel <= arena.getMaxLevel())))
					{
						final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
						packet.setFile(member, "data/html/undergroundColiseum/wrongLevel.htm");
						packet.replace("%name%", member.getName());
						player.sendPacket(packet);
						return;
					}
					
					realCount++;
				}
				
				if (realCount < UndergroundColiseumConfig.UC_PARTY_SIZE)
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
					packet.setFile(player, "data/html/undergroundColiseum/notEnoughMembers.htm");
					player.sendPacket(packet);
					return;
				}
				
				if (arena.getWaitingList().size() >= 5)
				{
					final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
					packet.setFile(player, "data/html/undergroundColiseum/arenaFull.htm");
					player.sendPacket(packet);
					return;
				}
				
				final UCWaiting waiting = new UCWaiting(party, arena);
				arena.getWaitingList().add(waiting);
				waiting.setParty(true);
				waiting.hasRegisterdNow();
				html.setFile(player, "data/html/undergroundColiseum/registered.htm");
				player.sendPacket(html);
				if ((arena.getWaitingList().size() >= 2) && !arena.isBattleNow())
				{
					arena.runTaskNow();
				}
			}
			catch (Exception e)
			{
				LOGGER.log(Level.WARNING, "onBypassFeedback: ", e);
			}
		}
		else if (actualCommand.equalsIgnoreCase("cancel"))
		{
			final Party party = player.getParty();
			if ((party == null) || !party.isLeader(player))
			{
				return;
			}
			
			if (party.getUCState() instanceof UCWaiting)
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
				final UCWaiting waiting = (UCWaiting) party.getUCState();
				
				waiting.setParty(false);
				waiting.clean();
				waiting.getBaseArena().getWaitingList().remove(waiting);
				packet.setFile(player, "data/html/undergroundColiseum/registrantionCanceled.htm");
				player.sendPacket(packet);
			}
		}
		else if (actualCommand.equalsIgnoreCase("bestTeam"))
		{
			final int val = Integer.parseInt(token.nextToken());
			final UCArena arena = UndergroundColiseumManager.getInstance().getArena(val);
			final UCBestTeam bestTeam = UndergroundColiseumManager.getInstance().getBestTeam(arena.getId());
			if (bestTeam != null)
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
				packet.setFile(player, "data/html/undergroundColiseum/bestTeam.htm");
				packet.replace("%name%", bestTeam.getLeaderName());
				packet.replace("%best%", String.valueOf(bestTeam.getWins()));
				player.sendPacket(packet);
			}
			else
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
				packet.setFile(player, "data/html/undergroundColiseum/view-most-wins.htm");
				player.sendPacket(packet);
			}
		}
		else if (actualCommand.equalsIgnoreCase("listTeams"))
		{
			final int val = Integer.parseInt(token.nextToken());
			final UCArena arena = UndergroundColiseumManager.getInstance().getArena(val);
			if (arena == null)
			{
				player.sendMessage("This arena is temporarly unavailable.");
				return;
			}
			
			final NpcHtmlMessage packet = new NpcHtmlMessage(getObjectId());
			packet.setFile(player, "data/html/undergroundColiseum/view-participating-teams.htm");
			
			final StringBuilder list = new StringBuilder();
			final int currentReg = arena.getWaitingList().size();
			for (int i = 1; i <= 5; i++)
			{
				if (i > currentReg)
				{
					list.append(i).append(". (Participating Team: Team)<br>");
				}
				else
				{
					final Party party = arena.getWaitingList().get(i - 1).getParty();
					if (party == null)
					{
						list.append(i).append(". (Participating Team: Team)<br>");
					}
					else
					{
						final StringBuilder teamList = new StringBuilder();
						for (Player m : party.getMembers())
						{
							if (m != null)
							{
								teamList.append(m.getName()).append(';');
							}
						}
						
						list.append(i).append(". (Participating Team: <font color=00ffff>").append(teamList).append("</font>)<br>");
					}
				}
			}
			
			packet.replace("%list%", list.toString());
			player.sendPacket(packet);
		}
		else
		{
			super.onBypassFeedback(player, command);
		}
	}
	
	/**
	 * Retrieves the file path for a specific NPC HTML template.<br>
	 * This method constructs a path based on the provided ID and value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value An additional value used to differentiate the HTML file.
	 * @param player The {@code Player} object associated with the request.
	 * @return A {@code String} representing the full path to the .htm file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom = "";
		
		if (value == 0)
		{
			pom = String.valueOf(npcId);
		}
		else
		{
			pom = npcId + "-" + value;
		}
		
		return "data/html/undergroundColiseum/" + pom + ".htm";
	}
}
