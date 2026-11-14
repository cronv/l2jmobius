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
package ai.others.Proclaimer;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcHtmlMessage;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;

/**
 * Handles the artificial intelligence for the {@code Proclaimer} NPC.<br>
 * This class manages its behaviors and interactions within the game world.
 * @author St3eT
 */
public class Proclaimer extends Script
{
	// NPCs
	private static final int[] PROCLAIMER =
	{
		36609, // Gludio
		36610, // Dion
		36611, // Giran
		36612, // Oren
		36613, // Aden
		36614, // Innadril
		36615, // Goddard
		36616, // Rune
		36617, // Schuttgart
	};
	
	// Skills
	private static final SkillHolder XP_BUFF = new SkillHolder(19036, 1); // Blessing of Light
	
	/**
	 * Private constructor for the {@code Proclaimer} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private Proclaimer()
	{
		addStartNpc(PROCLAIMER);
		addFirstTalkId(PROCLAIMER);
		addTalkId(PROCLAIMER);
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * It sends a whisper to players who are not on the dark side.<br>
	 * If a castle owner exists, it displays specific HTML information to the player.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return The name of the HTML file to display, or {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		String htmltext = null;
		if (!player.isOnDarkSide())
		{
			final int objectId = npc.getObjectId();
			player.sendPacket(new NpcSay(objectId, ChatType.WHISPER, npc.getId(), NpcStringId.WHEN_THE_WORLD_PLUNGES_INTO_CHAOS_WE_WILL_NEED_YOUR_HELP_WE_HOPE_YOU_JOIN_US_WHEN_THE_TIME_COMES));
			
			final Clan ownerClan = npc.getCastle().getOwner();
			if (ownerClan != null)
			{
				final NpcHtmlMessage packet = new NpcHtmlMessage(objectId);
				packet.setHtml(getHtm(player, "proclaimer.html"));
				packet.replace("%leaderName%", ownerClan.getLeaderName());
				packet.replace("%clanName%", ownerClan.getName());
				packet.replace("%castleName%", npc.getCastle().getName());
				player.sendPacket(packet);
			}
		}
		else
		{
			htmltext = "proclaimer-01.html";
		}
		
		return htmltext;
	}
	
	/**
	 * Handles specific events triggered by the {@code Proclaimer}.<br>
	 * It processes logic based on the provided {@code event} string.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return A string representing the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equals("giveBuff"))
		{
			if (!player.isOnDarkSide())
			{
				SkillCaster.triggerCast(npc, player, XP_BUFF.getSkill());
			}
			else
			{
				htmltext = "proclaimer-01.html";
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code Proclaimer} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Proclaimer();
	}
}
