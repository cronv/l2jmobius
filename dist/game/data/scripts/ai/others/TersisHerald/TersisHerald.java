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
package ai.others.TersisHerald;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.script.timers.TimerExecutor;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the artificial intelligence behavior for the {@code TersisHerald} NPC.<br>
 * It handles the logic and actions performed by this specific entity in the game world.
 * @author St3eT
 */
public class TersisHerald extends Script
{
	// NPCs
	private static final int HERALD = 4326; // Tersi's Herald
	private static final int ANTHARAS = 29068;
	private static final int VALAKAS = 29415; // Valakas Lv.130
	private static final int LINDVIOR = 29240;
	
	// Skills
	private static final SkillHolder DRAGON_BUFF = new SkillHolder(23312, 1); // Fall of the Dragon
	
	// Location
	private static final Location[] SPAWNS =
	{
		new Location(-13865, 122081, -2984, 32768), // Gludio
		new Location(16200, 142823, -2696, 17736), // Dion
		new Location(83273, 148396, -3400, 0), // Giran
		new Location(82272, 53278, -1488, 16384), // Oren
		new Location(147134, 25925, -2008, 48679), // Aden
		new Location(111620, 219189, -3536, 49152), // Heine
		new Location(148166, -55833, -2776, 53663), // Goddard
		new Location(44153, -48520, -792, 32768), // Rune
		new Location(86971, -142772, -1336, 14324), // Schuttgart
	};
	
	// Misc
	private static final int DESPAWN_TIME = 4 * 60 * 60 * 1000; // 4h
	private static final NpcStringId[] SPAM_MSGS =
	{
		NpcStringId.SHOW_RESPECT_TO_THE_HEROES_WHO_DEFEATED_THE_EVIL_DRAGON_AND_PROTECTED_THIS_ADEN_WORLD,
		NpcStringId.SHOUT_TO_CELEBRATE_THE_VICTORY_OF_THE_HEROES,
		NpcStringId.PRAISE_THE_ACHIEVEMENT_OF_THE_HEROES_AND_RECEIVE_TERSI_S_BLESSING,
	};
	private static final List<Npc> SPAWNED_NPCS = new ArrayList<>();
	
	/**
	 * Private constructor for the {@code TersisHerald} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private TersisHerald()
	{
		addStartNpc(HERALD);
		addFirstTalkId(HERALD);
		addTalkId(HERALD);
		addKillId(ANTHARAS, VALAKAS, LINDVIOR);
	}
	
	/**
	 * Handles specific events for the {@code TersisHerald} script.<br>
	 * This method processes logic such as giving buffs to players.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return A string representing the HTML file to display or the result of the super method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("giveBuff"))
		{
			if (player.isAffectedBySkill(DRAGON_BUFF.getSkillId()))
			{
				return npc.getId() + "-01.html";
			}
			
			npc.setTarget(player);
			npc.doCast(DRAGON_BUFF.getSkill());
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles specific events triggered by the internal timer.<br>
	 * This method manages NPC despawning and periodic text broadcasts.
	 * @param event The name of the event to process.
	 * @param params A set of statistics used as parameters for the event.
	 * @param npc The {@code Npc} instance associated with the timer.
	 * @param player The {@code Player} instance associated with the timer.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		if (event.equals("DESPAWN_NPCS"))
		{
			cancelQuestTimer("TEXT_SPAM", null, null);
			SPAWNED_NPCS.forEach(Npc::deleteMe);
			SPAWNED_NPCS.clear();
		}
		else if (event.equals("TEXT_SPAM"))
		{
			SPAWNED_NPCS.forEach(n -> n.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(SPAM_MSGS)));
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles specific logic for defeating major bosses like Antharas or Valakas.<br>
	 * It also manages the spawning of new heralds and sets up relevant timers.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who killed the entity.
	 * @param isSummon A boolean indicating if the kill was from a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final NpcStringId npcStringId;
		switch (npc.getId())
		{
			case ANTHARAS:
			{
				npcStringId = NpcStringId.THE_EVIL_EARTH_DRAGON_ANTHARAS_HAS_BEEN_DEFEATED_BY_BRAVE_HEROES;
				break;
			}
			case VALAKAS:
			{
				npcStringId = NpcStringId.THE_EVIL_FIRE_DRAGON_VALAKAS_HAS_BEEN_DEFEATED;
				break;
			}
			case LINDVIOR:
			{
				npcStringId = NpcStringId.HONORABLE_WARRIORS_HAVE_DRIVEN_OFF_LINDVIOR_THE_EVIL_WIND_DRAGON;
				break;
			}
			default:
			{
				return;
			}
		}
		
		World.getPlayers().forEach(p -> showOnScreenMsg(p, npcStringId, 2, 10000, true));
		if (!SPAWNED_NPCS.isEmpty())
		{
			final TimerExecutor<String> timers = getTimers();
			timers.cancelTimers("DESPAWN_NPCS");
			timers.addTimer("DESPAWN_NPCS", DESPAWN_TIME, null, null);
		}
		else
		{
			for (Location loc : SPAWNS)
			{
				SPAWNED_NPCS.add(addSpawn(HERALD, loc));
			}
			
			final TimerExecutor<String> timers = getTimers();
			timers.addTimer("DESPAWN_NPCS", DESPAWN_TIME, null, null);
			timers.addTimer("TEXT_SPAM", 300000, null, null);
		}
	}
	
	/**
	 * This method handles the first interaction between a {@code Player} and an {@code Npc}.<br>
	 * It determines which dialogue file to display based on the NPC's ID.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the main entry point for the {@code TersisHerald} class.<br>
	 * It initializes a new instance of the script to start the AI logic.
	 * @param args Command line arguments passed during startup.
	 */
	public static void main(String[] args)
	{
		new TersisHerald();
	}
}
