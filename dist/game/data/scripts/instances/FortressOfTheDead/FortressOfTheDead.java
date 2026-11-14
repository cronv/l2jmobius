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
package instances.FortressOfTheDead;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.script.QuestState;
import org.l2jmobius.gameserver.mechanics.script.newquestdata.QuestCondType;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.Movie;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.classchange.ExClassChangeSetAlarm;

import quests.Q10023_ProphesiedOne.Q10023_ProphesiedOne;
import quests.Q10123_ProphesiedOne.Q10123_ProphesiedOne;
import quests.Q10223_ProphesiedOne.Q10223_ProphesiedOne;
import quests.Q10323_ProphesiedOne.Q10323_ProphesiedOne;
import quests.Q10423_ProphesiedOne.Q10423_ProphesiedOne;
import quests.Q11023_ProphesiedOne.Q11023_ProphesiedOne;

/**
 * Defines the logic and behavior for the {@code FortressOfTheDead} instance zone.<br>
 * This class handles the script execution for players entering this specific area.
 * @author Gladicek, Mobius
 */
public class FortressOfTheDead extends InstanceScript
{
	// NPCs
	private static final int VAMPIRIC_SOLDIER = 19567;
	private static final int VON_HELLMAN = 19566;
	private static final int MYSTERIOUS_WIZARD = 31522;
	private static final int KAIN_VAN_HALTER = 33979;
	
	// Location
	private static final Location VON_HELLMAN_LOC = new Location(57963, -28676, 568, 49980);
	private static final Location MYSTERIOUS_WIZARD_LOC = new Location(57982, -28645, 568);
	private static final Location KAIN_VAN_HALTER_LOC = new Location(57963, -28676, 568, 49980);
	
	// Misc
	private static final int TEMPLATE_ID = 254;
	
	/**
	 * Initializes the {@code FortressOfTheDead} instance script.<br>
	 * Sets up the required NPCs and interaction rules for this zone.
	 */
	public FortressOfTheDead()
	{
		super(TEMPLATE_ID);
		addStartNpc(KAIN_VAN_HALTER);
		addFirstTalkId(KAIN_VAN_HALTER);
		addTalkId(KAIN_VAN_HALTER, MYSTERIOUS_WIZARD);
		addKillId(VAMPIRIC_SOLDIER, VON_HELLMAN);
	}
	
	/**
	 * Handles specific events triggered within the instance.<br>
	 * It processes actions like entering the zone, spawning NPCs, and completing quest steps.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} interacting with the event.
	 * @return The HTML text to display, or {@code null} if no text is required.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		if (event.equals("enterInstance"))
		{
			final QuestState questState = getQuestState(player);
			if ((questState != null) && !player.isInInstance())
			{
				enterInstance(player, npc, TEMPLATE_ID);
			}
		}
		else
		{
			final Instance world = npc.getInstanceWorld();
			if (isInInstance(world))
			{
				switch (event)
				{
					case "33979-01.html":
					case "33979-02.html":
					case "33979-03.html":
					case "33979-04.html":
					case "33979-05.html":
					case "33979-06.html":
					case "33979-07.html":
					case "33979-08.html":
					case "33979-09.html":
					case "33979-10.html":
					{
						htmltext = event;
						break;
					}
					case "exitInstance":
					{
						world.finishInstance(0);
						break;
					}
					case "vampire_dead":
					{
						addSpawn(VON_HELLMAN, VON_HELLMAN_LOC, false, 0, false, world.getId());
						break;
					}
					case "hellman_dead":
					{
						addSpawn(KAIN_VAN_HALTER, KAIN_VAN_HALTER_LOC, false, 0, false, world.getId());
						break;
					}
					case "spawnWizard":
					{
						if (npc.getInstanceWorld().getNpc(MYSTERIOUS_WIZARD) != null)
						{
							return null;
						}
						
						showOnScreenMsg(player, NpcStringId.TALK_TO_MYSTERIOUS_WIZARD, ExShowScreenMessage.TOP_CENTER, 5000);
						final Npc wizzard = addSpawn(MYSTERIOUS_WIZARD, MYSTERIOUS_WIZARD_LOC, true, 0, false, world.getId());
						wizzard.setSummoner(player);
						wizzard.setTitle(player.getAppearance().getVisibleName());
						wizzard.broadcastInfo();
						htmltext = "33979-11.html";
						break;
					}
					case "endCinematic":
					{
						final QuestState questState = getQuestState(player);
						if (questState != null)
						{
							questState.setCond(QuestCondType.DONE);
							ScriptManager.getInstance().getScript(questState.getQuestName()).notifyEvent("COMPLETE", null, player);
							player.sendPacket(ExClassChangeSetAlarm.STATIC_PACKET);
						}
						
						world.getNpc(KAIN_VAN_HALTER).deleteMe();
						world.getNpc(MYSTERIOUS_WIZARD).deleteMe();
						playMovie(player, Movie.ERT_QUEST_B);
						startQuestTimer("exitInstance", 25000, npc, player);
						break;
					}
				}
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This method is called when an {@code Npc} is killed by a {@code Player}.<br>
	 * It handles specific quest logic and rewards based on the NPC type.<br>
	 * The behavior changes depending on whether the kill was from a summoned entity.
	 * @param npc The {@code Npc} that was defeated.
	 * @param player The {@code Player} who dealt the killing blow.
	 * @param isSummon A boolean indicating if the kill occurred via a summon.
	 */
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			final int npcId = npc.getId();
			if (npcId == VAMPIRIC_SOLDIER)
			{
				if (world.getAliveNpcCount(VAMPIRIC_SOLDIER) == 0)
				{
					startQuestTimer("vampire_dead", 180, npc, player);
				}
			}
			else if (npcId == VON_HELLMAN)
			{
				npc.deleteMe();
				playMovie(player, Movie.ERT_QUEST_A);
				startQuestTimer("hellman_dead", 8000, npc, player);
			}
		}
	}
	
	/**
	 * Handles the dialogue for a player's first interaction with an {@code Npc}.<br>
	 * It checks if the {@code Player} has met specific quest conditions.<br>
	 * If condition 8 is met, it returns the corresponding HTML file.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return The name of the HTML dialogue file or {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	
	{
		return "33979.html";
	}
	
	/**
	 * Retrieves the current active quest state for a specific player.<br>
	 * This method checks multiple quest types in a specific order.<br>
	 * It returns the first started {@code QuestState} it finds.
	 * @param player The {@code Player} object to check.
	 * @return The {@code QuestState} of the active quest or {@code null}.
	 */
	private QuestState getQuestState(Player player)
	{
		QuestState questState = player.getQuestState(Q10023_ProphesiedOne.class.getSimpleName());
		if (questState != null)
		{
			return questState.isStarted() ? questState : null;
		}
		
		questState = player.getQuestState(Q10123_ProphesiedOne.class.getSimpleName());
		if (questState != null)
		{
			return questState.isStarted() ? questState : null;
		}
		
		questState = player.getQuestState(Q10223_ProphesiedOne.class.getSimpleName());
		if (questState != null)
		{
			return questState.isStarted() ? questState : null;
		}
		
		questState = player.getQuestState(Q10323_ProphesiedOne.class.getSimpleName());
		if (questState != null)
		{
			return questState.isStarted() ? questState : null;
		}
		
		questState = player.getQuestState(Q10423_ProphesiedOne.class.getSimpleName());
		if (questState != null)
		{
			return questState.isStarted() ? questState : null;
		}
		
		questState = player.getQuestState(Q11023_ProphesiedOne.class.getSimpleName());
		if (questState != null)
		{
			return questState.isStarted() ? questState : null;
		}
		
		return null;
	}
	
	/**
	 * This is the entry point for the {@code FortressOfTheDead} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments.
	 */
	public static void main(String[] args)
	{
		new FortressOfTheDead();
	}
}
