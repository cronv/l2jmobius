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
package ai.areas.Rune.Roiental;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.clan.ClanAccess;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class defines the artificial intelligence behavior for NPCs in the {@code Roiental} area.<br>
 * It handles specific logic and interactions for entities within this region.
 * @author CostyKiller
 */
public class Roiental extends Script
{
	// NPCs
	private static final int ROIENTAL = 34571;
	
	// Misc
	private static final int TOH_GB_TEMPLATE_ID = 307; // Throne of Heroes - Goldberg
	private static final int TOH_MR_TEMPLATE_ID = 308; // Throne of Heroes - Mary Reed
	private static final int TOH_TA_TEMPLATE_ID = 309; // Throne of Heroes - Tauti
	private static final int TOH_FE_TEMPLATE_ID = 364; // Throne of Heroes - Ferion
	private static final int TOH_TO_TEMPLATE_ID = 365; // Throne of Heroes - Toros
	private static final int MIN_LVL = 100;
	private static final int CLAN_MIN_LVL_GB = 7;
	private static final int CLAN_MIN_LVL_MR = 10;
	private static final int CLAN_MIN_LVL_TA = 11;
	private static final int CLAN_MIN_LVL_FE = 12;
	private static final int CLAN_MIN_LVL_TO = 13;
	
	/**
	 * Private constructor for the {@code Roiental} class.<br>
	 * This constructor initializes the NPC IDs and talk events.<br>
	 * It prevents other classes from creating new instances of this script.
	 */
	private Roiental()
	{
		addStartNpc(ROIENTAL);
		addFirstTalkId(ROIENTAL);
		addTalkId(ROIENTAL);
	}
	
	/**
	 * Handles specific events for the Roiental area.<br>
	 * It checks player requirements like level, clan status, and permissions.<br>
	 * Returns the appropriate HTML file path based on the event type.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A {@code String} containing the path to the HTML file or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		final Clan clan = player.getClan();
		if (event.equals("enterGoldberg"))
		{
			if (player.getLevel() < MIN_LVL)
			{
				htmltext = "Roiental-NoLevel.html";
			}
			else if (clan == null)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_3);
			}
			else if ((clan.getLevel() < CLAN_MIN_LVL_GB))
			{
				htmltext = "Roiental-03a.html";
			}
			else if (clan.getVariables().hasVariable("TOH_GOLDBERG_DONE"))
			{
				htmltext = "Roiental-AlreadyDone.html";
			}
			else if (!player.hasAccess(ClanAccess.THRONE_OF_HEROES))
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			}
			else
			{
				htmltext = "Roiental-01a.html";
			}
		}
		
		if (event.equals("enterMaryReed"))
		{
			if (player.getLevel() < MIN_LVL)
			{
				htmltext = "Roiental-NoLevel.html";
			}
			else if (clan == null)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_3);
			}
			else if ((clan.getLevel() < CLAN_MIN_LVL_MR))
			{
				htmltext = "Roiental-03b.html";
			}
			else if (clan.getVariables().hasVariable("TOH_MARYREED_DONE"))
			{
				htmltext = "Roiental-AlreadyDone.html";
			}
			else if (!player.hasAccess(ClanAccess.THRONE_OF_HEROES))
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			}
			else
			{
				htmltext = "Roiental-01b.html";
			}
		}
		
		if (event.equals("enterTauti"))
		{
			if (player.getLevel() < MIN_LVL)
			{
				htmltext = "Roiental-NoLevel.html";
			}
			else if (clan == null)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_3);
			}
			else if ((clan.getLevel() < CLAN_MIN_LVL_TA))
			{
				htmltext = "Roiental-03c.html";
			}
			else if (clan.getVariables().hasVariable("TOH_TAUTI_DONE"))
			{
				htmltext = "Roiental-AlreadyDone.html";
			}
			else if (!player.hasAccess(ClanAccess.THRONE_OF_HEROES))
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			}
			else
			{
				htmltext = "Roiental-01c.html";
			}
		}
		
		if (event.equals("enterFerion"))
		{
			if (player.getLevel() < MIN_LVL)
			{
				htmltext = "Roiental-NoLevel.html";
			}
			else if (clan == null)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_3);
			}
			else if ((clan.getLevel() < CLAN_MIN_LVL_FE))
			{
				htmltext = "Roiental-03d.html";
			}
			else if (clan.getVariables().hasVariable("TOH_FERION_DONE"))
			{
				htmltext = "Roiental-AlreadyDone.html";
			}
			else if (!player.hasAccess(ClanAccess.THRONE_OF_HEROES))
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			}
			else
			{
				htmltext = "Roiental-01d.html";
			}
		}
		
		if (event.equals("enterToros"))
		{
			if (player.getLevel() < MIN_LVL)
			{
				htmltext = "Roiental-NoLevel.html";
			}
			else if (clan == null)
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_A_CLAN_MEMBER_3);
			}
			else if ((clan.getLevel() < CLAN_MIN_LVL_TO))
			{
				htmltext = "Roiental-03e.html";
			}
			else if (clan.getVariables().hasVariable("TOH_TOROS_DONE"))
			{
				htmltext = "Roiental-AlreadyDone.html";
			}
			else if (!player.hasAccess(ClanAccess.THRONE_OF_HEROES))
			{
				player.sendPacket(SystemMessageId.YOU_ARE_NOT_AUTHORIZED_TO_DO_THAT);
			}
			else
			{
				htmltext = "Roiental-01e.html";
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct HTML text to display when a player talks to an {@code Npc} for the first time.<br>
	 * This method checks the current instance template ID of the {@code Player}.<br>
	 * It returns different files based on whether the player is in Goldberg, Mary Reed, or Tauti instances.
	 * @param npc The {@code Npc} entity that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} containing the name of the HTML file to be displayed.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		final Instance instance = InstanceManager.getInstance().getPlayerInstance(player, false);
		if ((instance != null) && ((instance.getTemplateId() == TOH_GB_TEMPLATE_ID)))
		{
			htmltext = "Roiental-02a.html";
		}
		else if ((instance != null) && ((instance.getTemplateId() == TOH_MR_TEMPLATE_ID)))
		{
			htmltext = "Roiental-02b.html";
		}
		else if ((instance != null) && ((instance.getTemplateId() == TOH_TA_TEMPLATE_ID)))
		{
			htmltext = "Roiental-02c.html";
		}
		else if ((instance != null) && ((instance.getTemplateId() == TOH_FE_TEMPLATE_ID)))
		{
			htmltext = "Roiental-02d.html";
		}
		else if ((instance != null) && ((instance.getTemplateId() == TOH_TO_TEMPLATE_ID)))
		{
			htmltext = "Roiental-02e.html";
		}
		else
		{
			htmltext = "Roiental-01.html";
		}
		
		return htmltext;
	}
	
	/**
	 * This is the main entry point for the {@code Roiental} class.<br>
	 * It initializes a new instance of the {@code Roiental} script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Roiental();
	}
}
