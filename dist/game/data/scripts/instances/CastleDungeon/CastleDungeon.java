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
package instances.CastleDungeon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.instancezone.InstanceTemplate;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.script.QuestSound;
import org.l2jmobius.gameserver.mechanics.siege.Castle;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * This class defines the {@code CastleDungeon} instance.<br>
 * It is used for the quest {@code BladeUnderFoot (512)}.<br>
 * It handles the logic and scripts specific to this dungeon area.
 * @author Mobius
 */
public class CastleDungeon extends InstanceScript
{
	// NPCs
	private static final Map<Integer, Integer> NPCS = new HashMap<>();
	static
	{
		NPCS.put(36403, 13); // Gludio
		NPCS.put(36404, 14); // Dion
		NPCS.put(36405, 15); // Giran
		NPCS.put(36406, 16); // Oren
		NPCS.put(36407, 17); // Aden
		NPCS.put(36408, 18); // Innadril
		NPCS.put(36409, 19); // Goddard
		NPCS.put(36410, 20); // Rune
		NPCS.put(36411, 21); // Schuttgart
	}
	
	// Monsters
	private static final int[] RAIDS1 =
	{
		25546,
		25549,
		25552
	};
	private static final int[] RAIDS2 =
	{
		25553,
		25554,
		25557,
		25560
	};
	private static final int[] RAIDS3 =
	{
		25563,
		25566,
		25569
	};
	
	// Item
	private static final int MARK = 9798;
	
	// Locations
	private static final Location SPAWN_LOC = new Location(12230, -49139, -3013);
	
	// Misc
	private static final int MARK_COUNT = 2520;
	private static final long REENTER = 24 * 3600000; // 24 hours
	private static final Map<Integer, Long> REENETER_HOLDER = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code CastleDungeon} instance script.<br>
	 * This constructor registers all required NPCs and monster IDs.<br>
	 * It also sets up the necessary event listeners for instance creation and destruction.
	 */
	public CastleDungeon()
	{
		super(NPCS.values().stream().mapToInt(Integer::valueOf).toArray());
		
		// NPCs
		final Set<Integer> npcKeys = NPCS.keySet();
		addStartNpc(npcKeys);
		addTalkId(npcKeys);
		
		// Monsters
		addKillId(RAIDS1);
		addKillId(RAIDS2);
		addKillId(RAIDS3);
		
		// Instance
		final Collection<Integer> npcValues = NPCS.values();
		addInstanceCreatedId(npcValues);
		addInstanceDestroyId(npcValues);
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc}.<br>
	 * It checks if the {@code Npc} is registered in the instance map.<br>
	 * If valid, it triggers the entry into the specific instance.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return Returns {@code null} after processing the interaction.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		final int npcId = npc.getId();
		final Integer template = NPCS.get(npcId);
		if (template != null)
		{
			enterInstance(player, npc, template);
		}
		
		return null;
	}
	
	/**
	 * Handles the logic triggered when an {@code Npc} is killed.<br>
	 * This method checks if the defeated monster belongs to a specific raid group.<br>
	 * If it does, it rewards eligible players and finishes the instance.<br>
	 * Otherwise, it increments the status and spawns a new raid.
	 * @param npc The {@code Npc} that was killed.
	 * @param player The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the monster was a summoned entity.
	 */
	@Override
	public void onKill(Npc npc, Player player, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			if (ArrayUtil.contains(RAIDS3, npc.getId()))
			{
				// Get players with active quest
				final List<Player> members = new ArrayList<>();
				
				// for (Player member : world.getPlayers())
				// {
				// final QuestState qs = member.getQuestState(Q00512_BladeUnderFoot.class.getSimpleName());
				// if ((qs != null) && qs.isCond(1))
				// {
				// members.add(member);
				// }
				// }
				
				// Distribute marks between them
				if (!members.isEmpty())
				{
					final long itemCount = MARK_COUNT / members.size();
					for (Player member : members)
					{
						giveItems(member, MARK, itemCount);
						playSound(member, QuestSound.ITEMSOUND_QUEST_ITEMGET);
					}
				}
				
				world.finishInstance();
			}
			else
			{
				world.incStatus();
				spawnRaid(world);
			}
		}
	}
	
	/**
	 * This method is called when a new instance is created.<br>
	 * It updates the re-entry time for the instance template.<br>
	 * It also schedules the spawning of the first raid.
	 * @param instance The {@code Instance} that was just created.
	 * @param player The {@code Player} who triggered the creation.
	 */
	@Override
	public void onInstanceCreated(Instance instance, Player player)
	{
		// Put re-enter for instance
		REENETER_HOLDER.put(instance.getTemplateId(), System.currentTimeMillis() + REENTER);
		
		// Schedule spawn of first raid
		spawnRaid(instance);
	}
	
	/**
	 * Checks if the players meet the requirements to enter the instance.<br>
	 * Verifies that the {@code Npc} belongs to a valid castle.<br>
	 * Ensures the group is not currently restricted by a re-entry cooldown.
	 * @param group The list of {@code Player} objects attempting to enter.
	 * @param npc The {@code Npc} being interacted with.
	 * @param template The {@code InstanceTemplate} for the target instance.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	protected boolean validateConditions(List<Player> group, Npc npc, InstanceTemplate template)
	{
		final Player groupLeader = group.get(0);
		final Castle castle = npc.getCastle();
		if (castle == null)
		{
			showHtmlFile(groupLeader, "noProperPledge.html");
			return false;
		}
		else if (REENETER_HOLDER.containsKey(template.getId()))
		{
			final int templateId = template.getId();
			final long time = REENETER_HOLDER.get(templateId);
			if (time > System.currentTimeMillis())
			{
				showHtmlFile(groupLeader, "enterRestricted.html");
				return false;
			}
			
			REENETER_HOLDER.remove(templateId);
		}
		
		return true;
	}
	
	/**
	 * This method is called when the instance is destroyed.<br>
	 * It cancels any active spawn tasks associated with the {@code Instance}.<br>
	 * It also clears the {@code spawnTask} parameter from the {@code Instance}.
	 * @param instance The {@code Instance} being destroyed.
	 */
	@Override
	public void onInstanceDestroy(Instance instance)
	{
		// Stop running spawn task
		final ScheduledFuture<?> task = instance.getParameters().getObject("spawnTask", ScheduledFuture.class);
		if ((task != null) && !task.isDone())
		{
			task.cancel(true);
		}
		
		instance.setParameter("spawnTask", null);
	}
	
	/**
	 * Schedules the spawning of a raid boss.<br>
	 * The task runs after a delay of 120000 milliseconds.<br>
	 * It selects an NPC ID based on the current status of the {@code Instance}.
	 * @param instance The {@code Instance} where the raid will be spawned.
	 */
	private void spawnRaid(Instance instance)
	{
		final ScheduledFuture<?> spawnTask = ThreadPool.schedule(() ->
		{
			// Get template id of raid.
			final int npcId;
			switch (instance.getStatus())
			{
				case 0:
				{
					npcId = getRandomEntry(RAIDS1);
					break;
				}
				case 1:
				{
					npcId = getRandomEntry(RAIDS2);
					break;
				}
				default:
				{
					npcId = getRandomEntry(RAIDS3);
					break;
				}
			}
			
			// Spawn raid.
			addSpawn(npcId, SPAWN_LOC, false, 0, false, instance.getId());
			
			// Unset spawn task reference.
			instance.setParameter("spawnTask", null);
		}, 2 * 60 * 1000); // 2 minutes
		
		// Save timer to instance world.
		instance.setParameter("spawnTask", spawnTask);
	}
	
	/**
	 * This is the entry point for the {@code CastleDungeon} class.<br>
	 * It initializes a new instance of the dungeon script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CastleDungeon();
	}
}
