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
package handlers.chat.commands.admin;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.StringTokenizer;

import org.l2jmobius.gameserver.cache.HtmCache;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.zone.type.NoRestartZone;
import org.l2jmobius.gameserver.entity.zone.type.NoSummonFriendZone;
import org.l2jmobius.gameserver.entity.zone.type.ScriptZone;
import org.l2jmobius.gameserver.handler.IAdminCommandHandler;
import org.l2jmobius.gameserver.managers.GrandBossManager;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.StatSet;

import ai.bosses.Baium.Baium;
import ai.bosses.Fafurion.Fafurion;
import ai.bosses.Trasken.Trasken;

/**
 * Handles the admin command for spawning or managing Grand Bosses.<br>
 * This class allows administrators to interact with {@code GrandBossManager} to manipulate boss entities in the game world.
 * @author St3eT, Mobius
 */
public class AdminGrandBoss implements IAdminCommandHandler
{
	private static final DateTimeFormatter RESPAWN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
	
	private static final int ANAKIM = 29348; // Anakim
	private static final ScriptZone ANAKIM_ZONE = ZoneManager.getInstance().getZoneById(12003, ScriptZone.class); // Anakim Zone
	
	private static final int BAIUM = 29020; // Baium
	private static final NoRestartZone BAIUM_ZONE = ZoneManager.getInstance().getZoneById(70051, NoRestartZone.class); // Baium Nest
	
	private static final int BELETH = 29118; // Beleth
	private static final ScriptZone BELETH_ZONE = ZoneManager.getInstance().getZoneById(60022, ScriptZone.class); // Beleth Zone
	
	private static final int CORE = 29006; // Core
	private static final ScriptZone CORE_ZONE = ZoneManager.getInstance().getZoneById(12007, ScriptZone.class); // Core Zone
	
	private static final int FAFURION = 19740; // Fafurion
	private static final NoRestartZone FAFURION_ZONE = ZoneManager.getInstance().getZoneById(85002, NoRestartZone.class); // Fafurion Nest
	
	private static final int HELIOS = 29305; // Helios
	private static final NoSummonFriendZone HELIOS_ZONE = ZoneManager.getInstance().getZoneById(210109, NoSummonFriendZone.class); // Helios Zone
	
	private static final int KELBIM = 26124; // Kelbim
	private static final ScriptZone KELBIM_ZONE = ZoneManager.getInstance().getZoneById(60023, ScriptZone.class); // Kelbim Zone
	
	private static final int LILITH = 29336; // Lilith
	private static final ScriptZone LILITH_ZONE = ZoneManager.getInstance().getZoneById(12005, ScriptZone.class); // Lilith Zone
	
	private static final int LINDVIOR = 29240; // Lindvior
	private static final NoSummonFriendZone LINDVIOR_ZONE = ZoneManager.getInstance().getZoneById(12107, NoSummonFriendZone.class); // Lindvior Zone
	
	private static final int ORFEN = 29325; // Orfen
	private static final ScriptZone ORFEN_ZONE = ZoneManager.getInstance().getZoneById(12013, ScriptZone.class); // Orfen Zone
	
	private static final int QUEENANT = 29381; // Queen Ant
	private static final ScriptZone QUEENANT_ZONE = ZoneManager.getInstance().getZoneById(12012, ScriptZone.class); // Queen Ant Nest Zone
	
	private static final int TRASKEN = 29197; // Trasken
	private static final NoSummonFriendZone TRASKEN_ZONE = ZoneManager.getInstance().getZoneById(12108, NoSummonFriendZone.class); // Trasken Nest
	
	private static final int VALAKAS = 29415; // Valakas
	private static final ScriptZone VALAKAS_ZONE = ZoneManager.getInstance().getZoneById(12010, ScriptZone.class); // Valakas Nest
	
	private static final String[] ADMIN_COMMANDS =
	{
		"admin_grandboss",
		"admin_grandboss_skip",
		"admin_grandboss_respawn",
		"admin_grandboss_minions",
		"admin_grandboss_abort",
	};
	
	/**
	 * Processes administrative commands for managing grand bosses.<br>
	 * It handles actions like skipping waits, respawning, and aborting fights.
	 * @param command The raw command string entered by the player.
	 * @param activeChar The {@code Player} who executed the command.
	 * @return Always returns {@code true}.
	 */
	@Override
	public boolean onCommand(String command, Player activeChar)
	{
		final StringTokenizer st = new StringTokenizer(command, " ");
		final String actualCommand = st.nextToken();
		switch (actualCommand.toLowerCase())
		{
			case "admin_grandboss":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					manageHtml(activeChar, grandBossId);
				}
				else
				{
					final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
					html.setHtml(HtmCache.getInstance().getHtm(activeChar, "data/html/admin/grandboss/grandboss.htm"));
					activeChar.sendPacket(html);
				}
				break;
			}
			case "admin_grandboss_skip":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case FAFURION:
						{
							fafurionAi().notifyEvent("SKIP_WAITING", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
							break;
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_skip Id");
				}
				break;
			}
			case "admin_grandboss_respawn":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case BAIUM:
						{
							baiumAi().notifyEvent("RESPAWN_BAIUM", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case FAFURION:
						{
							fafurionAi().notifyEvent("RESPAWN_FAFURION", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case TRASKEN:
						{
							traskenAi().notifyEvent("RESPAWN_TRASKEN", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
							break;
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_respawn Id");
				}
				break;
			}
			case "admin_grandboss_minions":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case BAIUM:
						{
							baiumAi().notifyEvent("DESPAWN_MINIONS", null, activeChar);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
							break;
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_minions Id");
				}
				break;
			}
			case "admin_grandboss_abort":
			{
				if (st.hasMoreTokens())
				{
					final int grandBossId = Integer.parseInt(st.nextToken());
					switch (grandBossId)
					{
						case BAIUM:
						{
							baiumAi().notifyEvent("ABORT_FIGHT", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case FAFURION:
						{
							fafurionAi().notifyEvent("ABORT_FIGHT", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						case TRASKEN:
						{
							traskenAi().notifyEvent("ABORT_FIGHT", null, activeChar);
							manageHtml(activeChar, grandBossId);
							break;
						}
						default:
						{
							activeChar.sendSysMessage("Wrong ID!");
							break;
						}
					}
				}
				else
				{
					activeChar.sendSysMessage("Usage: //grandboss_abort Id");
				}
			}
				break;
		}
		
		return true;
	}
	
	/**
	 * This method handles the display of information for a specific grand boss.<br>
	 * It retrieves the boss status, respawn time, and zone player count.<br>
	 * The gathered data is then sent to the {@code Player} as an HTML message.<br>
	 * If the provided ID is invalid, it sends a system message to the user.
	 * @param activeChar The {@code Player} who will receive the information packet.
	 * @param grandBossId The unique identifier for the boss being queried.
	 */
	private void manageHtml(Player activeChar, int grandBossId)
	{
		if (Arrays.asList(ANAKIM, BAIUM, BELETH, CORE, FAFURION, HELIOS, KELBIM, LILITH, LINDVIOR, ORFEN, QUEENANT, TRASKEN, VALAKAS).contains(grandBossId))
		{
			final int bossStatus = GrandBossManager.getInstance().getStatus(grandBossId);
			ScriptZone anakimZone = null;
			NoRestartZone baiumZone = null;
			ScriptZone belethZone = null;
			ScriptZone coreZone = null;
			NoRestartZone fafurionZone = null;
			NoSummonFriendZone heliosZone = null;
			ScriptZone kelbimZone = null;
			ScriptZone lilithZone = null;
			NoSummonFriendZone lindviorZone = null;
			ScriptZone orfenZone = null;
			ScriptZone queenantZone = null;
			NoSummonFriendZone traskenZone = null;
			ScriptZone valakasZone = null;
			String textColor = null;
			String text = null;
			String htmlPath = null;
			int deadStatus = 0;
			
			switch (grandBossId)
			{
				case ANAKIM:
				{
					anakimZone = ANAKIM_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_anakim.htm";
					break;
				}
				case BAIUM:
				{
					baiumZone = BAIUM_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_baium.htm";
					break;
				}
				case BELETH:
				{
					belethZone = BELETH_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_beleth.htm";
					break;
				}
				case CORE:
				{
					coreZone = CORE_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_core.htm";
					break;
				}
				case FAFURION:
				{
					fafurionZone = FAFURION_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_fafurion.htm";
					break;
				}
				case HELIOS:
				{
					heliosZone = HELIOS_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_helios.htm";
					break;
				}
				case KELBIM:
				{
					kelbimZone = KELBIM_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_kelbim.htm";
					break;
				}
				case LILITH:
				{
					lilithZone = LILITH_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_lilith.htm";
					break;
				}
				case LINDVIOR:
				{
					lindviorZone = LINDVIOR_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_lindvior.htm";
					break;
				}
				case ORFEN:
				{
					orfenZone = ORFEN_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_orfen.htm";
					break;
				}
				case QUEENANT:
				{
					queenantZone = QUEENANT_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_queenant.htm";
					break;
				}
				case TRASKEN:
				{
					traskenZone = TRASKEN_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_trasken.htm";
					break;
				}
				case VALAKAS:
				{
					valakasZone = VALAKAS_ZONE;
					htmlPath = "data/html/admin/grandboss/grandboss_valakas.htm";
					break;
				}
			}
			
			if (Arrays.asList(BAIUM, BELETH, CORE, FAFURION, HELIOS, KELBIM, LINDVIOR, TRASKEN, VALAKAS).contains(grandBossId))
			{
				deadStatus = 3;
				switch (bossStatus)
				{
					case 0:
					{
						textColor = "00FF00"; // Green.
						text = "Alive";
						break;
					}
					case 1:
					{
						textColor = "FFFF00"; // Yellow.
						text = "Waiting";
						break;
					}
					case 2:
					{
						textColor = "FF9900"; // Orange.
						text = "In Fight";
						break;
					}
					case 3:
					{
						textColor = "FF0000"; // Red.
						text = "Dead";
						break;
					}
					default:
					{
						textColor = "FFFFFF"; // White.
						text = "Unk " + bossStatus;
						break;
					}
				}
			}
			else if (Arrays.asList(ANAKIM, LILITH).contains(grandBossId))
			{
				deadStatus = 2;
				switch (bossStatus)
				{
					case 0:
					{
						textColor = "00FF00"; // Green.
						text = "Alive";
						break;
					}
					case 1:
					{
						textColor = "FF9900"; // Orange.
						text = "In Fight";
						break;
					}
					case 2:
					{
						textColor = "FF0000"; // Red.
						text = "Dead";
						break;
					}
					default:
					{
						textColor = "FFFFFF"; // White.
						text = "Unk " + bossStatus;
						break;
					}
				}
			}
			else
			{
				deadStatus = 1;
				switch (bossStatus)
				{
					case 0:
					{
						textColor = "00FF00"; // Green.
						text = "Alive";
						break;
					}
					case 1:
					{
						textColor = "FF0000"; // Red.
						text = "Dead";
						break;
					}
					default:
					{
						textColor = "FFFFFF"; // White.
						text = "Unk " + bossStatus;
						break;
					}
				}
			}
			
			// @formatter:off
			final StatSet info = GrandBossManager.getInstance().getStatSet(grandBossId);
			final String bossRespawn = RESPAWN_FORMAT.format(Instant.ofEpochMilli(info.getLong("respawn_time")).atZone(ZoneId.systemDefault()));
			final NpcHtmlMessage html = new NpcHtmlMessage(0, 1);
			html.setHtml(HtmCache.getInstance().getHtm(activeChar, htmlPath));
			html.replace("%bossStatus%", text);
			html.replace("%bossColor%", textColor);
			html.replace("%respawnTime%", bossStatus == deadStatus ? bossRespawn : "Already respawned!");
			html.replace("%playersInside%", (anakimZone != null) ? String.valueOf(anakimZone.getPlayersInside().size()) :
				 							(baiumZone != null) ? String.valueOf(baiumZone.getPlayersInside().size()) :
				 							(belethZone != null) ? String.valueOf(belethZone.getPlayersInside().size()) :
				 							(coreZone != null) ? String.valueOf(coreZone.getPlayersInside().size()) :
				 							(fafurionZone != null) ? String.valueOf(fafurionZone.getPlayersInside().size()) :
				 							(heliosZone != null) ? String.valueOf(heliosZone.getPlayersInside().size()) :
				 							(kelbimZone != null) ? String.valueOf(kelbimZone.getPlayersInside().size()) :
				 							(lilithZone != null) ? String.valueOf(lilithZone.getPlayersInside().size()) :
				 							(lindviorZone != null) ? String.valueOf(lindviorZone.getPlayersInside().size()) :
				 							(orfenZone != null) ? String.valueOf(orfenZone.getPlayersInside().size()) :
				 							(queenantZone != null) ? String.valueOf(queenantZone.getPlayersInside().size()) :
				 							(traskenZone != null) ? String.valueOf(traskenZone.getPlayersInside().size()) :
				 							(valakasZone != null) ? String.valueOf(valakasZone.getPlayersInside().size()) :
											"Zone not found!");
			activeChar.sendPacket(html);
			// @formatter:on
		}
		else
		{
			activeChar.sendSysMessage("Wrong ID!");
		}
	}
	
	/**
	 * Retrieves the quest script for the {@code Baium} boss.<br>
	 * This method fetches the script from the {@code ScriptManager}.
	 * @return The {@code Quest} object associated with {@code Baium}.
	 */
	private Quest baiumAi()
	{
		return ScriptManager.getInstance().getScript(Baium.class.getSimpleName());
	}
	
	/**
	 * Retrieves the quest script for the {@code Fafurion} boss.<br>
	 * This method fetches the logic from the {@code ScriptManager}.
	 * @return the {@code Quest} object associated with {@code Fafurion}.
	 */
	private Quest fafurionAi()
	{
		return ScriptManager.getInstance().getScript(Fafurion.class.getSimpleName());
	}
	
	/**
	 * Retrieves the quest script for the {@code Trasken} boss.<br>
	 * This method fetches the script from the {@code ScriptManager}.
	 * @return The {@code Quest} object associated with {@code Trasken}.
	 */
	private Quest traskenAi()
	{
		return ScriptManager.getInstance().getScript(Trasken.class.getSimpleName());
	}
	
	/**
	 * Retrieves the list of available admin commands.<br>
	 * This method returns all registered command strings for this handler.
	 * @return An array of {@code String} containing the command names.
	 */
	@Override
	public String[] getCommandList()
	{
		return ADMIN_COMMANDS;
	}
}
