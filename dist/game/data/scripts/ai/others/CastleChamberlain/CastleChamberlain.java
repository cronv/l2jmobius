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
package ai.others.CastleChamberlain;

import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.sql.ClanTable;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Door;
import org.l2jmobius.gameserver.entity.actor.instance.Merchant;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.managers.CastleManorManager;
import org.l2jmobius.gameserver.managers.FortManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcManorBypass;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.mechanics.siege.Castle.CastleFunction;
import org.l2jmobius.gameserver.mechanics.siege.CastleSide;
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowCropInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExShowCropSetting;
import org.l2jmobius.gameserver.network.serverpackets.ExShowManorDefaultInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExShowSeedInfo;
import org.l2jmobius.gameserver.network.serverpackets.ExShowSeedSetting;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.util.FormatUtil;

/**
 * Handles the artificial intelligence for the {@code CastleChamberlain} NPC.<br>
 * This class defines the behaviors and logic for this specific entity within the game world.
 * @author malyelfik, Mobius
 */
public class CastleChamberlain extends Script
{
	// NPCs
	// @formatter:off
	private static final int[] NPC =
	{
		// Chamberlain of Light / Chamberlain of Darkness
		35100, 36653, // Gludio
		35142, 36654, // Dion
		35184, 36655, // Giran
		35226, 36656, // Oren
		35274, 36657, // Aden
		35316, 36658, // Innadril
		35363, 36659, // Goddard
		35509, 36660, // Rune
		35555, 36661, // Schuttgart
	};
	// @formatter:on
	
	// Item
	private static final int CROWN = 6841;
	private static final int LORD_CLOAK_OF_LIGHT = 34996;
	private static final int LORD_CLOAK_OF_DARK = 34997;
	
	// Fortress
	private static final Map<Integer, List<Integer>> FORTRESS = new HashMap<>();
	static
	{
		FORTRESS.put(1, Arrays.asList(101, 102, 112, 113)); // Gludio Castle
		FORTRESS.put(2, Arrays.asList(103, 112, 114, 115)); // Dion Castle
		FORTRESS.put(3, Arrays.asList(104, 114, 116, 118, 119)); // Giran Castle
		FORTRESS.put(4, Arrays.asList(105, 113, 115, 116, 117)); // Oren Castle
		FORTRESS.put(5, Arrays.asList(106, 107, 117, 118)); // Aden Castle
		FORTRESS.put(6, Arrays.asList(108, 119)); // Innadril Castle
		FORTRESS.put(7, Arrays.asList(109, 117, 120)); // Goddard Castle
		FORTRESS.put(8, Arrays.asList(110, 120, 121)); // Rune Castle
		FORTRESS.put(9, Arrays.asList(111, 121)); // Schuttgart Castle
	}
	
	// Buffs
	private static final SkillHolder[] BUFFS =
	{
		new SkillHolder(4342, 2), // Wind Walk Lv.2
		new SkillHolder(4343, 3), // Decrease Weight Lv.3
		new SkillHolder(4344, 3), // Shield Lv.3
		new SkillHolder(4346, 4), // Mental Shield Lv.4
		new SkillHolder(4345, 3), // Might Lv.3
		new SkillHolder(4347, 2), // Bless the Body Lv.2
		new SkillHolder(4349, 1), // Magic Barrier Lv.1
		new SkillHolder(4350, 1), // Resist Shock Lv.1
		new SkillHolder(4348, 2), // Bless the Soul Lv.2
		new SkillHolder(4351, 2), // Concentration Lv.2
		new SkillHolder(4352, 1), // Berserker Spirit Lv.1
		new SkillHolder(4353, 2), // Bless Shield Lv.2
		new SkillHolder(4358, 1), // Guidance Lv.1
		new SkillHolder(4354, 1), // Vampiric Rage Lv.1
		new SkillHolder(4347, 6), // Bless the Body Lv.6
		new SkillHolder(4349, 2), // Magic Barrier Lv.2
		new SkillHolder(4350, 4), // Resist Shock Lv.4
		new SkillHolder(4348, 6), // Bless the Soul Lv.6
		new SkillHolder(4351, 6), // Concentration Lv.6
		new SkillHolder(4352, 2), // Berserker Spirit Lv.2
		new SkillHolder(4353, 6), // Bless Shield Lv.6
		new SkillHolder(4358, 3), // Guidance Lv.3
		new SkillHolder(4354, 4), // Vampiric Rage Lv.4
		new SkillHolder(4355, 1), // Acumen Lv.1
		new SkillHolder(4356, 1), // Empower Lv.1
		new SkillHolder(4357, 1), // Haste Lv.1
		new SkillHolder(4359, 1), // Focus Lv.1
		new SkillHolder(4360, 1), // Death Whisper Lv.1
	};
	
	/**
	 * Private constructor for the {@code CastleChamberlain} class.<br>
	 * This constructor initializes the NPC data and talk IDs.
	 */
	private CastleChamberlain()
	{
		addStartNpc(NPC);
		addTalkId(NPC);
		addFirstTalkId(NPC);
	}
	
	/**
	 * Creates an {@code NpcHtmlMessage} for a specific NPC.<br>
	 * This method retrieves the HTML content based on the provided file name.
	 * @param player The {@code Player} who will receive the message.
	 * @param npc The {@code Npc} that is sending the message.
	 * @param htmlFile The name of the HTML file to load.
	 * @return The generated {@code NpcHtmlMessage} packet.
	 */
	private NpcHtmlMessage getHtmlPacket(Player player, Npc npc, String htmlFile)
	{
		final NpcHtmlMessage packet = new NpcHtmlMessage(npc.getObjectId());
		packet.setHtml(getHtm(player, htmlFile));
		return packet;
	}
	
	/**
	 * Generates and sends an HTML confirmation message for castle functions.<br>
	 * It checks if the {@code Player} is the owner and has management permissions.<br>
	 * The specific HTML content depends on the function type and level.
	 * @param player The {@code Player} interacting with the NPC.
	 * @param npc The {@code Npc} being interacted with.
	 * @param castle The {@code Castle} object associated with the action.
	 * @param func The unique identifier for the castle function.
	 * @param level The current level of the function.
	 * @return Returns {@code null} if the packet is sent successfully, otherwise returns an HTML file path.
	 */
	private String funcConfirmHtml(Player player, Npc npc, Castle castle, int func, int level)
	{
		if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
		{
			final NpcHtmlMessage html;
			final String fstring = (func == Castle.FUNC_TELEPORT) ? "9" : "10";
			if (level == 0)
			{
				html = getHtmlPacket(player, npc, "castleresetdeco.html");
				html.replace("%AgitDecoSubmit%", Integer.toString(func));
			}
			else if ((castle.getCastleFunction(func) != null) && (castle.getCastleFunction(func).getLvl() == level))
			{
				html = getHtmlPacket(player, npc, "castledecoalreadyset.html");
				html.replace("%AgitDecoEffect%", "<fstring p1=\"" + level + "\">" + fstring + "</fstring>");
			}
			else
			{
				html = getHtmlPacket(player, npc, "castledeco-0" + func + ".html");
				html.replace("%AgitDecoCost%", "<fstring p1=\"" + getFunctionFee(func, level) + "\" p2=\"" + (getFunctionRatio(func) / 86400000) + "\">6</fstring>");
				html.replace("%AgitDecoEffect%", "<fstring p1=\"" + level + "\">" + fstring + "</fstring>");
				html.replace("%AgitDecoSubmit%", func + " " + level);
			}
			
			player.sendPacket(html);
			return null;
		}
		
		return "chamberlain-21.html";
	}
	
	/**
	 * Replaces specific placeholders in an {@code NpcHtmlMessage} based on a castle function.<br>
	 * It updates values like depth, cost, expiration, and reset status.<br>
	 * If the function is null, it uses default placeholder replacements.
	 * @param castle The {@code Castle} object containing the siege data.
	 * @param html The {@code NpcHtmlMessage} to be modified.
	 * @param func The unique identifier for the {@code Castle.CastleFunction}.
	 * @param str The string prefix used for the placeholders in the HTML content.
	 */
	private void funcReplace(Castle castle, NpcHtmlMessage html, int func, String str)
	{
		final CastleFunction function = castle.getCastleFunction(func);
		if (function == null)
		{
			html.replace("%" + str + "Depth%", "<fstring>4</fstring>");
			html.replace("%" + str + "Cost%", "");
			html.replace("%" + str + "Expire%", "<fstring>4</fstring>");
			html.replace("%" + str + "Reset%", "");
		}
		else
		{
			final String fstring = ((func == Castle.FUNC_SUPPORT) || (func == Castle.FUNC_TELEPORT)) ? "9" : "10";
			final Calendar calendar = Calendar.getInstance();
			calendar.setTimeInMillis(function.getEndTime());
			html.replace("%" + str + "Depth%", "<fstring p1=\"" + function.getLvl() + "\">" + fstring + "</fstring>");
			html.replace("%" + str + "Cost%", "<fstring p1=\"" + function.getLease() + "\" p2=\"" + (function.getRate() / 86400000) + "\">6</fstring>");
			html.replace("%" + str + "Expire%", "<fstring p1=\"" + calendar.get(Calendar.DATE) + "\" p2=\"" + (calendar.get(Calendar.MONTH) + 1) + "\" p3=\"" + calendar.get(Calendar.YEAR) + "\">5</fstring>");
			html.replace("%" + str + "Reset%", "[<a action=\"bypass -h Script CastleChamberlain " + str + " 0\">Deactivate</a>]");
		}
	}
	
	/**
	 * Calculates the cost for a specific castle function based on the level.<br>
	 * It checks the {@code func} type and returns the corresponding fee from {@code FeatureConfig}.
	 * @param func The unique identifier for the castle function.
	 * @param level The level used to determine which fee tier to apply.
	 * @return The calculated cost as an {@code int}.
	 */
	private int getFunctionFee(int func, int level)
	{
		int fee = 0;
		switch (func)
		{
			case Castle.FUNC_RESTORE_EXP:
			{
				fee = (level == 45) ? FeatureConfig.CS_EXPREG1_FEE : FeatureConfig.CS_EXPREG2_FEE;
				break;
			}
			case Castle.FUNC_RESTORE_HP:
			{
				fee = (level == 300) ? FeatureConfig.CS_HPREG1_FEE : FeatureConfig.CS_HPREG2_FEE;
				break;
			}
			case Castle.FUNC_RESTORE_MP:
			{
				fee = (level == 40) ? FeatureConfig.CS_MPREG1_FEE : FeatureConfig.CS_MPREG2_FEE;
				break;
			}
			case Castle.FUNC_SUPPORT:
			{
				fee = (level == 5) ? FeatureConfig.CS_SUPPORT1_FEE : FeatureConfig.CS_SUPPORT2_FEE;
				break;
			}
			case Castle.FUNC_TELEPORT:
			{
				fee = (level == 1) ? FeatureConfig.CS_TELE1_FEE : FeatureConfig.CS_TELE2_FEE;
				break;
			}
		}
		
		return fee;
	}
	
	/**
	 * Retrieves the fee ratio for a specific castle function.<br>
	 * This method maps the {@code func} ID to its corresponding value from {@code FeatureConfig}.
	 * @param func The unique identifier for the castle function.
	 * @return The long value representing the fee ratio.
	 */
	private long getFunctionRatio(int func)
	{
		long ratio = 0;
		switch (func)
		{
			case Castle.FUNC_RESTORE_EXP:
			{
				ratio = FeatureConfig.CS_EXPREG_FEE_RATIO;
				break;
			}
			case Castle.FUNC_RESTORE_HP:
			{
				ratio = FeatureConfig.CS_HPREG_FEE_RATIO;
				break;
			}
			case Castle.FUNC_RESTORE_MP:
			{
				ratio = FeatureConfig.CS_MPREG_FEE_RATIO;
				break;
			}
			case Castle.FUNC_SUPPORT:
			{
				ratio = FeatureConfig.CS_SUPPORT_FEE_RATIO;
				break;
			}
			case Castle.FUNC_TELEPORT:
			{
				ratio = FeatureConfig.CS_TELE_FEE_RATIO;
				break;
			}
		}
		
		return ratio;
	}
	
	/**
	 * Calculates the cost to upgrade a specific structure.<br>
	 * This method retrieves prices from {@code FeatureConfig} based on the door type and level.
	 * @param type The category of the structure, such as outer door, inner door, or wall.
	 * @param level The target level for the upgrade.
	 * @return The price required for the upgrade as an {@code int}.
	 */
	private int getDoorUpgradePrice(int type, int level)
	{
		int price = 0;
		switch (type)
		{
			case 1: // Outer Door.
			{
				switch (level)
				{
					case 2:
					{
						price = FeatureConfig.OUTER_DOOR_UPGRADE_PRICE2;
						break;
					}
					case 3:
					{
						price = FeatureConfig.OUTER_DOOR_UPGRADE_PRICE3;
						break;
					}
					case 5:
					{
						price = FeatureConfig.OUTER_DOOR_UPGRADE_PRICE5;
						break;
					}
				}
				break;
			}
			case 2: // Inner Door.
			{
				switch (level)
				{
					case 2:
					{
						price = FeatureConfig.INNER_DOOR_UPGRADE_PRICE2;
						break;
					}
					case 3:
					{
						price = FeatureConfig.INNER_DOOR_UPGRADE_PRICE3;
						break;
					}
					case 5:
					{
						price = FeatureConfig.INNER_DOOR_UPGRADE_PRICE5;
						break;
					}
				}
				break;
			}
			case 3: // Wall.
			{
				switch (level)
				{
					case 2:
					{
						price = FeatureConfig.WALL_UPGRADE_PRICE2;
						break;
					}
					case 3:
					{
						price = FeatureConfig.WALL_UPGRADE_PRICE3;
						break;
					}
					case 5:
					{
						price = FeatureConfig.WALL_UPGRADE_PRICE5;
						break;
					}
				}
				break;
			}
		}
		
		return price;
	}
	
	/**
	 * Calculates the cost to upgrade a trap based on its level.<br>
	 * This method retrieves values from {@code FeatureConfig}.
	 * @param level The current level of the trap.
	 * @return The price required for the upgrade as an {@code int}.
	 */
	private int getTrapUpgradePrice(int level)
	{
		int price = 0;
		switch (level)
		{
			case 1:
			{
				price = FeatureConfig.TRAP_UPGRADE_PRICE1;
				break;
			}
			case 2:
			{
				price = FeatureConfig.TRAP_UPGRADE_PRICE2;
				break;
			}
			case 3:
			{
				price = FeatureConfig.TRAP_UPGRADE_PRICE3;
				break;
			}
			case 4:
			{
				price = FeatureConfig.TRAP_UPGRADE_PRICE4;
				break;
			}
		}
		
		return price;
	}
	
	/**
	 * Checks if any fortress associated with a specific castle is currently in contract status.<br>
	 * It iterates through the fortresses belonging to the given {@code castleId}.<br>
	 * Returns {@code true} if at least one fortress has a state of {@code 2}.
	 * @param castleId The unique identifier for the castle.
	 * @return {@code true} if a fortress is in contract status, otherwise {@code false}.
	 */
	private boolean isDomainFortressInContractStatus(int castleId)
	{
		final int numFort = ((castleId == 1) || (castleId == 5)) ? 2 : 1;
		final List<Integer> fortList = FORTRESS.get(castleId);
		for (int i = 0; i < numFort; i++)
		{
			final Fort fortress = FortManager.getInstance().getFortById(fortList.get(i));
			if (fortress.getFortState() == 2)
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the {@code Player} has ownership rights over an {@code Npc}.<br>
	 * This is true if the player is a GM or belongs to the clan that owns the castle.
	 * @param player The {@code Player} to check.
	 * @param npc The {@code Npc} associated with the castle.
	 * @return {@code true} if the player is an owner, {@code false} otherwise.
	 */
	private boolean isOwner(Player player, Npc npc)
	{
		return player.isGM() || ((player.getClan() != null) && (player.getClanId() == npc.getCastle().getOwnerId()));
	}
	
	/**
	 * Handles various interactions and events for the Castle Chamberlain NPC.<br>
	 * It processes commands like upgrading doors, managing traps, and viewing fort statuses.<br>
	 * The method checks player permissions and clan ownership before executing actions.
	 * @param event The specific action or HTML file requested by the player.
	 * @param npc The {@code Npc} instance interacting with the player.
	 * @param player The {@code Player} who triggered the event.
	 * @return A string representing the HTML response or a default error message.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		final Castle castle = npc.getCastle();
		final StringTokenizer st = new StringTokenizer(event, " ");
		String htmltext = null;
		final boolean isMyLord = player.isClanLeader() && (player.getClan().getCastleId() == (npc.getCastle() != null ? npc.getCastle().getResidenceId() : -1));
		
		switch (st.nextToken())
		{
			case "chamberlain-01.html":
			case "manor-help-01.html":
			case "manor-help-02.html":
			case "manor-help-03.html":
			case "manor-help-04.html":
			{
				htmltext = event;
				break;
			}
			case "fort_status":
			{
				if (isMyLord)
				{
					final StringBuilder sb = new StringBuilder();
					final List<Integer> fort = FORTRESS.get(castle.getResidenceId());
					for (int id : fort)
					{
						final Fort fortress = FortManager.getInstance().getFortById(id);
						if (fortress == null)
						{
							continue;
						}
						
						final int fortId = fortress.getResidenceId();
						final String fortType = (fortId < 112) ? "1300133" : "1300134";
						final String fortStatus;
						switch (fortress.getFortState())
						{
							case 1:
							{
								fortStatus = "1300122";
								break;
							}
							case 2:
							{
								fortStatus = "1300124";
								break;
							}
							default:
							{
								fortStatus = "1300123";
								break;
							}
						}
						
						sb.append("<fstring>1300" + fortId + "</fstring>");
						sb.append(" (<fstring>" + fortType + "</fstring>)");
						sb.append(" : <font color=\"00FFFF\"><fstring>" + fortStatus + "</fstring></font><br>");
					}
					
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-28.html");
					html.replace("%list%", sb.toString());
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "siege_functions":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					if (castle.getSiege().isInProgress())
					{
						htmltext = "chamberlain-08.html";
					}
					else if (!isDomainFortressInContractStatus(castle.getResidenceId()))
					{
						htmltext = "chamberlain-27.html";
					}
					else
					{
						htmltext = "chamberlain-12.html";
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "manage_doors":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					if (st.hasMoreTokens())
					{
						final StringBuilder sb = new StringBuilder();
						final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-13.html");
						html.replace("%type%", st.nextToken());
						while (st.hasMoreTokens())
						{
							sb.append(" " + st.nextToken());
						}
						
						html.replace("%doors%", sb.toString());
						player.sendPacket(html);
					}
					else
					{
						htmltext = npc.getCastle().getName() + "-du.html";
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "upgrade_doors":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					final int type = Integer.parseInt(st.nextToken());
					final int level = Integer.parseInt(st.nextToken());
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-14.html");
					html.replace("%gate_price%", Integer.toString(getDoorUpgradePrice(type, level)));
					html.replace("%event%", event.substring("upgrade_doors".length() + 1));
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "upgrade_doors_confirm":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					if (castle.getSiege().isInProgress())
					{
						htmltext = "chamberlain-08.html";
					}
					else
					{
						final int type = Integer.parseInt(st.nextToken());
						final int level = Integer.parseInt(st.nextToken());
						final int price = getDoorUpgradePrice(type, level);
						final int[] doors = new int[2];
						for (int i = 0; i <= st.countTokens(); i++)
						{
							doors[i] = Integer.parseInt(st.nextToken());
						}
						
						final Door door = castle.getDoor(doors[0]);
						if (door != null)
						{
							final int currentLevel = door.getStat().getUpgradeHpRatio();
							if (currentLevel >= level)
							{
								final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-15.html");
								html.replace("%doorlevel%", Integer.toString(currentLevel));
								player.sendPacket(html);
							}
							else if (player.getAdena() >= price)
							{
								takeItems(player, Inventory.ADENA_ID, price);
								for (int doorId : doors)
								{
									castle.setDoorUpgrade(doorId, level, true);
								}
								
								htmltext = "chamberlain-16.html";
							}
							else
							{
								htmltext = "chamberlain-09.html";
							}
						}
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "manage_trap":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					if (st.hasMoreTokens())
					{
						final NpcHtmlMessage html;
						if (castle.getName().equalsIgnoreCase("aden"))
						{
							html = getHtmlPacket(player, npc, "chamberlain-17a.html");
						}
						else
						{
							html = getHtmlPacket(player, npc, "chamberlain-17.html");
						}
						
						html.replace("%trapIndex%", st.nextToken());
						player.sendPacket(html);
					}
					else
					{
						htmltext = npc.getCastle().getName() + "-tu.html";
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "upgrade_trap":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					final String trapIndex = st.nextToken();
					final int level = Integer.parseInt(st.nextToken());
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-18.html");
					html.replace("%trapIndex%", trapIndex);
					html.replace("%level%", Integer.toString(level));
					html.replace("%dmgzone_price%", Integer.toString(getTrapUpgradePrice(level)));
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "upgrade_trap_confirm":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					if (castle.getSiege().isInProgress())
					{
						htmltext = "chamberlain-08.html";
					}
					else
					{
						final int trapIndex = Integer.parseInt(st.nextToken());
						final int level = Integer.parseInt(st.nextToken());
						final int price = getTrapUpgradePrice(level);
						final int currentLevel = castle.getTrapUpgradeLevel(trapIndex);
						if (currentLevel >= level)
						{
							final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-19.html");
							html.replace("%dmglevel%", Integer.toString(currentLevel));
							player.sendPacket(html);
						}
						else if (player.getAdena() >= price)
						{
							takeItems(player, Inventory.ADENA_ID, price);
							castle.setTrapUpgrade(trapIndex, level, true);
							htmltext = "chamberlain-20.html";
						}
						else
						{
							htmltext = "chamberlain-09.html";
						}
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "receive_report":
			{
				if (isMyLord)
				{
					if (castle.getSiege().isInProgress())
					{
						htmltext = "chamberlain-07.html";
					}
					else
					{
						final Clan clan = ClanTable.getInstance().getClan(castle.getOwnerId());
						final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-02.html");
						html.replace("%clanleadername%", clan.getLeaderName());
						html.replace("%clanname%", clan.getName());
						html.replace("%castlename%", String.valueOf(1001000 + castle.getResidenceId()));
						player.sendPacket(html);
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "manage_vault":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_VAULT))
				{
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "castlemanagevault.html");
					html.replace("%tax_income%", FormatUtil.formatAdena(castle.getTreasury()));
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "manage_vault_deposit":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_VAULT))
				{
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "castlemanagevault_deposit.html");
					html.replace("%tax_income%", FormatUtil.formatAdena(castle.getTreasury()));
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "manage_vault_withdraw":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_VAULT))
				{
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "castlemanagevault_withdraw.html");
					html.replace("%tax_income%", FormatUtil.formatAdena(castle.getTreasury()));
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "deposit":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_VAULT))
				{
					final long amount = (st.hasMoreTokens()) ? Long.parseLong(st.nextToken()) : 0;
					if ((amount > 0) && (amount < Inventory.MAX_ADENA))
					{
						if (player.getAdena() >= amount)
						{
							takeItems(player, Inventory.ADENA_ID, amount);
							castle.addToTreasuryNoTax(amount);
						}
						else
						{
							player.sendPacket(SystemMessageId.NOT_ENOUGH_ADENA);
						}
					}
					
					htmltext = "chamberlain-01.html";
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "withdraw":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_VAULT))
				{
					final long amount = (st.hasMoreTokens()) ? Long.parseLong(st.nextToken()) : 0;
					if ((amount > 0) && (amount <= castle.getTreasury()))
					{
						castle.addToTreasuryNoTax((-1) * amount);
						giveAdena(player, amount, false);
						htmltext = "chamberlain-01.html";
					}
					else
					{
						final NpcHtmlMessage html = getHtmlPacket(player, npc, "castlenotenoughbalance.html");
						html.replace("%tax_income%", FormatUtil.formatAdena(castle.getTreasury()));
						html.replace("%withdraw_amount%", FormatUtil.formatAdena(amount));
						player.sendPacket(html);
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "manage_functions":
			{
				if (!isOwner(player, npc))
				{
					htmltext = "chamberlain-21.html";
				}
				else if (castle.getSiege().isInProgress())
				{
					htmltext = "chamberlain-08.html";
				}
				else
				{
					htmltext = "chamberlain-23.html";
				}
				break;
			}
			case "banish_foreigner_show":
			{
				if (!isOwner(player, npc) || !player.hasAccess(ClanAccess.CASTLE_BANISH))
				{
					htmltext = "chamberlain-21.html";
				}
				else if (castle.getSiege().isInProgress())
				{
					htmltext = "chamberlain-08.html";
				}
				else
				{
					htmltext = "chamberlain-10.html";
				}
				break;
			}
			case "banish_foreigner":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_BANISH))
				{
					if (castle.getSiege().isInProgress())
					{
						htmltext = "chamberlain-08.html";
					}
					else
					{
						castle.banishForeigners();
						htmltext = "chamberlain-11.html";
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "doors":
			{
				if (!isOwner(player, npc) || !player.hasAccess(ClanAccess.CASTLE_OPEN_DOOR))
				{
					htmltext = "chamberlain-21.html";
				}
				else if (castle.getSiege().isInProgress())
				{
					htmltext = "chamberlain-08.html";
				}
				else
				{
					htmltext = npc.getCastle().getName() + "-d.html";
				}
				break;
			}
			case "operate_door":
			{
				if (!isOwner(player, npc) || !player.hasAccess(ClanAccess.CASTLE_OPEN_DOOR))
				{
					htmltext = "chamberlain-21.html";
				}
				else if (castle.getSiege().isInProgress())
				{
					htmltext = "chamberlain-08.html";
				}
				else
				{
					final boolean open = (Integer.parseInt(st.nextToken()) == 1);
					while (st.hasMoreTokens())
					{
						castle.openCloseDoor(player, Integer.parseInt(st.nextToken()), open);
					}
					
					htmltext = (open ? "chamberlain-05.html" : "chamberlain-06.html");
				}
				break;
			}
			case "additional_functions":
			{
				htmltext = (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS)) ? "castletdecomanage.html" : "chamberlain-21.html";
				break;
			}
			case "recovery":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "castledeco-AR01.html");
					funcReplace(castle, html, Castle.FUNC_RESTORE_HP, "HP");
					funcReplace(castle, html, Castle.FUNC_RESTORE_MP, "MP");
					funcReplace(castle, html, Castle.FUNC_RESTORE_EXP, "XP");
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "other":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "castledeco-AE01.html");
					funcReplace(castle, html, Castle.FUNC_TELEPORT, "TP");
					funcReplace(castle, html, Castle.FUNC_SUPPORT, "BF");
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "HP":
			{
				final int level = Integer.parseInt(st.nextToken());
				htmltext = funcConfirmHtml(player, npc, castle, Castle.FUNC_RESTORE_HP, level);
				break;
			}
			case "MP":
			{
				final int level = Integer.parseInt(st.nextToken());
				htmltext = funcConfirmHtml(player, npc, castle, Castle.FUNC_RESTORE_MP, level);
				break;
			}
			case "XP":
			{
				final int level = Integer.parseInt(st.nextToken());
				htmltext = funcConfirmHtml(player, npc, castle, Castle.FUNC_RESTORE_EXP, level);
				break;
			}
			case "TP":
			{
				final int level = Integer.parseInt(st.nextToken());
				htmltext = funcConfirmHtml(player, npc, castle, Castle.FUNC_TELEPORT, level);
				break;
			}
			case "BF":
			{
				final int level = Integer.parseInt(st.nextToken());
				htmltext = funcConfirmHtml(player, npc, castle, Castle.FUNC_SUPPORT, level);
				break;
			}
			case "set_func":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANAGE_FUNCTIONS))
				{
					final int func = Integer.parseInt(st.nextToken());
					final int level = Integer.parseInt(st.nextToken());
					if (level == 0)
					{
						castle.updateFunctions(player, func, level, 0, 0, false);
					}
					else if (!castle.updateFunctions(player, func, level, getFunctionFee(func, level), getFunctionRatio(func), castle.getCastleFunction(func) == null))
					{
						htmltext = "chamberlain-09.html";
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "functions":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_FUNCTIONS))
				{
					final CastleFunction hp = castle.getCastleFunction(Castle.FUNC_RESTORE_HP);
					final CastleFunction mp = castle.getCastleFunction(Castle.FUNC_RESTORE_MP);
					final CastleFunction xp = castle.getCastleFunction(Castle.FUNC_RESTORE_EXP);
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "castledecofunction.html");
					html.replace("%HPDepth%", (hp == null) ? "0" : Integer.toString(hp.getLvl()));
					html.replace("%MPDepth%", (mp == null) ? "0" : Integer.toString(mp.getLvl()));
					html.replace("%XPDepth%", (xp == null) ? "0" : Integer.toString(xp.getLvl()));
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "teleport":
			{
				if (!isOwner(player, npc) || !player.hasAccess(ClanAccess.CASTLE_FUNCTIONS))
				{
					htmltext = "chamberlain-21.html";
				}
				else if (castle.getCastleFunction(Castle.FUNC_TELEPORT) == null)
				{
					htmltext = "castlefuncdisabled.html";
				}
				else
				{
					final String listName = "tel" + castle.getCastleFunction(Castle.FUNC_TELEPORT).getLvl();
					final TeleportHolder holder = TeleporterData.getInstance().getHolder(npc.getId(), listName);
					if (holder != null)
					{
						holder.showTeleportList(player, npc, "Script CastleChamberlain goto");
					}
				}
				break;
			}
			case "goto": // goto listId locId
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_FUNCTIONS) && (st.countTokens() >= 2))
				{
					final CastleFunction func = castle.getCastleFunction(Castle.FUNC_TELEPORT);
					if (func == null)
					{
						return "castlefuncdisabled.html";
					}
					
					final String listId = st.nextToken();
					final int funcLvl = (listId.length() >= 4) ? StringUtil.parseInt(listId.substring(3), -1) : -1;
					if (func.getLvl() == funcLvl)
					{
						final TeleportHolder holder = TeleporterData.getInstance().getHolder(npc.getId(), listId);
						if (holder != null)
						{
							holder.doTeleport(player, npc, StringUtil.parseNextInt(st, -1));
						}
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "buffer":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_FUNCTIONS))
				{
					if (castle.getCastleFunction(Castle.FUNC_SUPPORT) == null)
					{
						htmltext = "castlefuncdisabled.html";
					}
					else
					{
						final NpcHtmlMessage html = getHtmlPacket(player, npc, "castlebuff-0" + castle.getCastleFunction(Castle.FUNC_SUPPORT).getLvl() + ".html");
						html.replace("%MPLeft%", Integer.toString((int) npc.getCurrentMp()));
						player.sendPacket(html);
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "cast_buff":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_FUNCTIONS))
				{
					if (castle.getCastleFunction(Castle.FUNC_SUPPORT) == null)
					{
						htmltext = "castlefuncdisabled.html";
					}
					else
					{
						final int index = Integer.parseInt(st.nextToken());
						if (BUFFS.length > index)
						{
							final NpcHtmlMessage html;
							final SkillHolder holder = BUFFS[index];
							if (holder.getSkill().getMpConsume() < npc.getCurrentMp())
							{
								npc.setTarget(player);
								npc.doCast(holder.getSkill());
								html = getHtmlPacket(player, npc, "castleafterbuff.html");
							}
							else
							{
								html = getHtmlPacket(player, npc, "castlenotenoughmp.html");
							}
							
							html.replace("%MPLeft%", Integer.toString((int) npc.getCurrentMp()));
							player.sendPacket(html);
						}
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "list_siege_clans":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_SIEGE))
				{
					castle.getSiege().listRegisterClan(player);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "manor":
			{
				if (GeneralConfig.ALLOW_MANOR)
				{
					htmltext = (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_MANOR)) ? "manor.html" : "chamberlain-21.html";
				}
				else
				{
					player.sendMessage("Manor system is deactivated.");
				}
				break;
			}
			case "products":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_FUNCTIONS))
				{
					final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-22.html");
					html.replace("%npcId%", Integer.toString(npc.getId()));
					player.sendPacket(html);
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "buy":
			{
				if (isOwner(player, npc) && player.hasAccess(ClanAccess.CASTLE_FUNCTIONS))
				{
					((Merchant) npc).showBuyWindow(player, Integer.parseInt(st.nextToken()));
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
			case "give_cloak":
			{
				if (castle.getSiege().isInProgress())
				{
					htmltext = "chamberlain-08.html";
					break;
				}
				else if (isMyLord)
				{
					final int cloakId = npc.getCastle().getSide() == CastleSide.DARK ? LORD_CLOAK_OF_DARK : LORD_CLOAK_OF_LIGHT;
					if (hasQuestItems(player, cloakId))
					{
						htmltext = "chamberlain-03.html";
						break;
					}
					
					giveItems(player, cloakId, 1);
				}
				else
				{
					htmltext = "chamberlain-29.html";
				}
				break;
			}
			case "give_crown":
			{
				if (castle.getSiege().isInProgress())
				{
					htmltext = "chamberlain-08.html";
				}
				else if (isMyLord)
				{
					if (hasQuestItems(player, CROWN))
					{
						htmltext = "chamberlain-24.html";
					}
					else
					{
						final NpcHtmlMessage html = getHtmlPacket(player, npc, "chamberlain-25.html");
						html.replace("%owner_name%", player.getName());
						html.replace("%feud_name%", String.valueOf(1001000 + castle.getResidenceId()));
						player.sendPacket(html);
						giveItems(player, CROWN, 1);
					}
				}
				else
				{
					htmltext = "chamberlain-21.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines which HTML file to display when a player talks to the NPC for the first time.<br>
	 * It checks if the {@code Player} is the owner of the {@code Npc}.<br>
	 * Returns different files based on ownership status.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the name of the HTML file to display.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return (isOwner(player, npc)) ? "chamberlain-01.html" : "chamberlain-04.html";
	}
	
	// @formatter:off
	/**
	 * Handles the bypass events for the Manor system.<br>
	 * This method checks if the player is the owner of the NPC.<br>
	 * It processes different requests such as seed and crop information.<br>
	 * It also handles manor settings while checking for maintenance status.
	 * @param event The {@code OnNpcManorBypass} event containing request details.
	 */
	@RegisterEvent(EventType.ON_NPC_MANOR_BYPASS)
	@RegisterType(ListenerRegisterType.NPC)
	@Id({35100, 35142, 35184, 35226, 35274,	35316, 35363, 35509, 35555, 36653, 36654, 36655, 36656, 36657, 36658, 36659, 36660, 36661})
	// @formatter:on
	public void onNpcManorBypass(OnNpcManorBypass event)
	{
		final Player player = event.getPlayer();
		final Npc npc = event.getTarget();
		if (isOwner(player, npc))
		{
			final CastleManorManager manor = CastleManorManager.getInstance();
			if (manor.isUnderMaintenance())
			{
				player.sendPacket(SystemMessageId.THE_MANOR_SYSTEM_IS_CURRENTLY_UNDER_MAINTENANCE);
				return;
			}
			
			final int castleId = (event.getManorId() == -1) ? npc.getCastle().getResidenceId() : event.getManorId();
			switch (event.getRequest())
			{
				case 3: // Seed info
				{
					player.sendPacket(new ExShowSeedInfo(castleId, event.isNextPeriod(), true));
					break;
				}
				case 4: // Crop info
				{
					player.sendPacket(new ExShowCropInfo(castleId, event.isNextPeriod(), true));
					break;
				}
				case 5: // Basic info
				{
					player.sendPacket(new ExShowManorDefaultInfo(true));
					break;
				}
				case 7: // Seed settings
				{
					if (manor.isManorApproved())
					{
						player.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_00_AM_AND_8_00_PM);
						return;
					}
					
					player.sendPacket(new ExShowSeedSetting(castleId));
					break;
				}
				case 8: // Crop settings
				{
					if (manor.isManorApproved())
					{
						player.sendPacket(SystemMessageId.A_MANOR_CANNOT_BE_SET_UP_BETWEEN_6_00_AM_AND_8_00_PM);
						return;
					}
					
					player.sendPacket(new ExShowCropSetting(castleId));
					break;
				}
				default:
				{
					LOGGER.warning(getClass().getSimpleName() + ": " + player + " send unknown request id " + event.getRequest() + "!");
					break;
				}
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code CastleChamberlain} class.<br>
	 * It initializes a new instance of the class to start the program.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CastleChamberlain();
	}
}
