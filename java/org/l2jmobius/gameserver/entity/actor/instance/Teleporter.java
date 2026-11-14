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
package org.l2jmobius.gameserver.entity.actor.instance;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.data.xml.TeleporterData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportType;
import org.l2jmobius.gameserver.entity.actor.holders.npc.TeleporterQuestRecommendationHolder;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.teleporter.TeleportHolder;
import org.l2jmobius.gameserver.managers.CastleManager;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.skill.CommonSkill;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.teleports.ExShowTeleportUi;

/**
 * Represents a {@code Teleporter} NPC that allows players to travel between different locations.<br>
 * This class handles the logic for teleportation requests and interacts with {@code TeleporterData}.
 * @author NightMarez
 */
public class Teleporter extends Merchant
{
	private static final Logger LOGGER = Logger.getLogger(Teleporter.class.getName());
	
	private static final CommonSkill[] FORBIDDEN_TRANSFORM =
	{
		CommonSkill.FROG_TRANSFORM,
		CommonSkill.CHILD_TRANSFORM,
		CommonSkill.NATIVE_TRANSFORM
	};
	
	private static final Map<Integer, List<TeleporterQuestRecommendationHolder>> QUEST_RECOMENDATIONS = new HashMap<>();
	
	// static
	// {
	// @formatter:off
		// QUEST_RECOMENDATIONS.put(30006, new ArrayList<>()); // Gatekeeper Milia
		// QUEST_RECOMENDATIONS.get(30006).add(new TeleporterQuestRecommendationHolder(30006, "Q00561_BasicMissionHarnakUndergroundRuins", new int[]{3}, "30006-Q561"));
		// QUEST_RECOMENDATIONS.put(30134, new ArrayList<>()); // Dark Elf Village Teleport Device
		// QUEST_RECOMENDATIONS.get(30134).add(new TeleporterQuestRecommendationHolder(30134, "Q00562_BasicMissionAltarOfEvil", new int[]{4}, "30134-Q562"));
		// QUEST_RECOMENDATIONS.put(30256, new ArrayList<>()); // Gatekeeper Bella
		// QUEST_RECOMENDATIONS.get(30256).add(new TeleporterQuestRecommendationHolder(30256, "Q00562_BasicMissionAltarOfEvil", new int[]{3}, "30256-Q562"));
		// QUEST_RECOMENDATIONS.put(30848, new ArrayList<>()); // Gatekeeper Elisa
		// QUEST_RECOMENDATIONS.get(30848).add(new TeleporterQuestRecommendationHolder(30848, "Q00561_BasicMissionHarnakUndergroundRuins", new int[]{2,4}, "30848-Q561-Q562"));
		// QUEST_RECOMENDATIONS.get(30848).add(new TeleporterQuestRecommendationHolder(30848, "Q00562_BasicMissionAltarOfEvil", new int[]{2,4}, "30848-Q561-Q562"));
		// @formatter:on
	// }
	
	/**
	 * Creates a new {@code Teleporter} instance using the provided template.<br>
	 * This constructor initializes the entity as a teleporter type.
	 * @param template The {@code NpcTemplate} used to create this teleporter.
	 */
	public Teleporter(NpcTemplate template)
	{
		super(template);
		setInstanceType(InstanceType.Teleporter);
	}
	
	/**
	 * Checks if the creature can be attacked automatically.<br>
	 * This method returns {@code true} if the attacker is a monster or meets the parent class criteria.
	 * @param attacker The {@code Creature} attempting to perform the attack.
	 * @return {@code true} if the attack is allowed, {@code false} otherwise.
	 */
	@Override
	public boolean isAutoAttackable(Creature attacker)
	{
		return attacker.isMonster() || super.isAutoAttackable(attacker);
	}
	
	/**
	 * Handles bypass commands sent by a {@code Player}.<br>
	 * It checks for forbidden transformations before processing the request.<br>
	 * The method supports specific actions like showing teleport lists or chatting.
	 * @param player The {@code Player} who sent the command.
	 * @param command The string command to be processed.
	 */
	@Override
	public void onBypassFeedback(Player player, String command)
	{
		// Check if transformed.
		for (CommonSkill skill : FORBIDDEN_TRANSFORM)
		{
			if (player.isAffectedBySkill(skill.getId()))
			{
				sendHtmlMessage(player, "data/html/teleporter/epictransformed.htm");
				return;
			}
		}
		
		// Process bypass.
		final StringTokenizer st = new StringTokenizer(command, " ");
		switch (st.nextToken())
		{
			case "showNoblesSelect":
			{
				sendHtmlMessage(player, "data/html/teleporter/" + (player.getNobleLevel() > 0 ? "nobles_select" : "not_nobles") + ".htm");
				break;
			}
			case "showTeleports":
			{
				final String listName = (st.hasMoreTokens()) ? st.nextToken() : TeleportType.NORMAL.name();
				final int id = getId();
				final TeleportHolder holder = TeleporterData.getInstance().getHolder(id, listName);
				if (holder == null)
				{
					LOGGER.warning(player + " requested show teleports for list with name " + listName + " at NPC " + id + "!");
					return;
				}
				
				holder.showTeleportList(player, this);
				break;
			}
			case "showTeleportsHunting":
			{
				final String listName = (st.hasMoreTokens()) ? st.nextToken() : TeleportType.HUNTING.name();
				final int id = getId();
				final TeleportHolder holder = TeleporterData.getInstance().getHolder(id, listName);
				if (holder == null)
				{
					LOGGER.warning(player + " requested show teleports for hunting list with name " + listName + " at NPC " + id + "!");
					return;
				}
				
				holder.showTeleportList(player, this);
				break;
			}
			case "showTeleportList":
			{
				player.sendPacket(ExShowTeleportUi.STATIC_PACKET);
				break;
			}
			case "teleport":
			{
				// Check for required count of params.
				if (st.countTokens() != 2)
				{
					LOGGER.warning(player + " send unhandled teleport command: " + command);
					return;
				}
				
				final String listName = st.nextToken();
				final int id = getId();
				final TeleportHolder holder = TeleporterData.getInstance().getHolder(id, listName);
				if (holder == null)
				{
					LOGGER.warning(player + " requested unknown teleport list: " + listName + " for npc: " + id + "!");
					return;
				}
				
				holder.doTeleport(player, this, parseNextInt(st, -1));
				break;
			}
			case "chat":
			{
				int val = 0;
				try
				{
					val = Integer.parseInt(command.substring(5));
				}
				catch (IndexOutOfBoundsException | NumberFormatException ignored)
				{
				}
				
				showChatWindow(player, val);
				break;
			}
			default:
			{
				super.onBypassFeedback(player, command);
				break;
			}
		}
	}
	
	/**
	 * Retrieves the next integer from a {@code StringTokenizer}.<br>
	 * It returns the {@code defaultVal} if no numeric token is found.
	 * @param st The {@code StringTokenizer} to read from.
	 * @param defaultVal The value to return if parsing fails.
	 * @return The parsed integer or the provided default value.
	 */
	private int parseNextInt(StringTokenizer st, int defaultVal)
	{
		if (st.hasMoreTokens())
		{
			final String token = st.nextToken();
			if (StringUtil.isNumeric(token))
			{
				return Integer.parseInt(token);
			}
		}
		
		return defaultVal;
	}
	
	/**
	 * Retrieves the correct HTML file path for a teleporter.<br>
	 * It determines the path based on the NPC ID and a specific value.<br>
	 * If the value is 0, it checks for active quest recommendations for the player.<br>
	 * Otherwise, it constructs a path using the NPC ID and the provided value.
	 * @param npcId The unique identifier of the NPC.
	 * @param value A numeric value used to construct the HTML filename.
	 * @param player The {@code Player} instance used to check quest progress.
	 * @return The full file path to the teleporter's HTML file.
	 */
	@Override
	public String getHtmlPath(int npcId, int value, Player player)
	{
		String pom;
		if (value == 0)
		{
			pom = String.valueOf(npcId);
			if ((player != null) && QUEST_RECOMENDATIONS.containsKey(npcId))
			{
				CHECK: for (TeleporterQuestRecommendationHolder rec : QUEST_RECOMENDATIONS.get(npcId))
				{
					final QuestState qs = player.getQuestState(rec.getQuestName());
					if ((qs != null) && qs.isStarted())
					{
						for (int cond : rec.getConditions())
						{
							if ((cond == -1) || qs.isCond(cond))
							{
								pom = rec.getHtml();
								break CHECK;
							}
						}
					}
				}
			}
		}
		else
		{
			pom = (npcId + "-" + value);
		}
		
		return "data/html/teleporter/" + pom + ".htm";
	}
	
	/**
	 * Displays a chat window to the player based on the teleporter's current status.<br>
	 * It checks if the teleporter is in a castle and determines the correct HTML file to show.<br>
	 * The logic handles cases for clan ownership and active sieges.
	 * @param player The {@code Player} who will view the chat window.
	 */
	@Override
	public void showChatWindow(Player player)
	{
		// Teleporter isn't on castle ground.
		if (CastleManager.getInstance().getCastle(this) == null)
		{
			super.showChatWindow(player);
			return;
		}
		
		// Teleporter is on castle ground.
		String filename = "data/html/teleporter/castleteleporter-no.htm";
		if ((player.getClan() != null) && (getCastle().getOwnerId() == player.getClanId())) // Clan owns castle
		{
			filename = getHtmlPath(getId(), 0, player); // Owner message window
		}
		else if (getCastle().getSiege().isInProgress()) // Teleporter is busy due siege.
		{
			filename = "data/html/teleporter/castleteleporter-busy.htm"; // Busy because of siege
		}
		
		sendHtmlMessage(player, filename);
	}
	
	/**
	 * Sends an HTML message to a specific player.<br>
	 * This method creates an {@code NpcHtmlMessage} using the current NPC object ID.<br>
	 * It replaces placeholders with the correct names and files before sending it to the {@code Player}.
	 * @param player The {@code Player} who will receive the message.
	 * @param filename The name of the HTML file to display.
	 */
	private void sendHtmlMessage(Player player, String filename)
	{
		final int objectId = getObjectId();
		final NpcHtmlMessage html = new NpcHtmlMessage(objectId);
		html.setFile(player, filename);
		html.replace("%objectId%", String.valueOf(objectId));
		html.replace("%npcname%", getName());
		player.sendPacket(html);
	}
}
