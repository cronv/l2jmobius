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
package instances.FortressDungeon;

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
import org.l2jmobius.gameserver.mechanics.siege.Fort;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * This class defines the {@code FortressDungeon} instance.<br>
 * It handles the logic for the quest {@code Awl Under Foot (511)}.<br>
 * It manages the dungeon environment and specific script events.
 * @author malyelfik
 */
public class FortressDungeon extends InstanceScript
{
	// NPCs
	private static final Map<Integer, Integer> NPCS = new HashMap<>();
	static
	{
		NPCS.put(35666, 22); // Shanty
		NPCS.put(35698, 23); // Southern
		NPCS.put(35735, 24); // Hive
		NPCS.put(35767, 25); // Valley
		NPCS.put(35804, 26); // Ivory
		NPCS.put(35835, 27); // Narsell
		NPCS.put(35867, 28); // Bayou
		NPCS.put(35904, 29); // White Sands
		NPCS.put(35936, 30); // Borderland
		NPCS.put(35974, 31); // Swamp
		NPCS.put(36011, 32); // Archaic
		NPCS.put(36043, 33); // Floran
		NPCS.put(36081, 34); // Cloud Mountain
		NPCS.put(36118, 35); // Tanor
		NPCS.put(36149, 36); // Dragonspine
		NPCS.put(36181, 37); // Antharas
		NPCS.put(36219, 38); // Western
		NPCS.put(36257, 39); // Hunter
		NPCS.put(36294, 40); // Aaru
		NPCS.put(36326, 41); // Demon
		NPCS.put(36364, 42); // Monastic
	}
	
	// Monsters
	private static final int[] RAIDS1 =
	{
		25572,
		25575,
		25578
	};
	private static final int[] RAIDS2 =
	{
		25579,
		25582,
		25585,
		25588
	};
	private static final int[] RAIDS3 =
	{
		25589,
		25592,
		25593
	};
	
	// Item
	private static final int MARK = 9797;
	
	// Locations
	private static final Location SPAWN_LOC = new Location(53319, 245814, -6576);
	
	// Misc
	private static final int MARK_COUNT = 2520;
	private static final long REENTER = 24 * 3600000; // 24 hours
	private static final Map<Integer, Long> REENETER_HOLDER = new ConcurrentHashMap<>();
	
	/**
	 * Initializes the {@code FortressDungeon} instance script.<br>
	 * This constructor sets up the required NPCs, talk IDs, and kill IDs.<br>
	 * It also registers the instance creation and destruction events.
	 */
	public FortressDungeon()
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
	 * If valid, it triggers the instance entry for the player.
	 * @param npc The {@code Npc} that was interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return Always returns {@code null}.
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
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles rewards and instance progression based on the kill.<br>
	 * If the NPC belongs to a specific raid, it distributes items to quest members.<br>
	 * Otherwise, it increments the status and spawns a new raid.
	 * @param npc The {@code Npc} that was killed.
	 * @param player The {@code Player} who performed the kill.
	 * @param isSummon Indicates if the NPC was a summon.
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
				// final QuestState qs = member.getQuestState(Q00511_AwlUnderFoot.class.getSimpleName());
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
	 * It updates the re-entry timer for the specific template.<br>
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
	 * Checks if the group meets all requirements to enter the instance.<br>
	 * It verifies the fort status and re-entry cooldowns.<br>
	 * If any condition fails, it shows an error message to the leader.
	 * @param group The list of players attempting to enter.
	 * @param npc The NPC being interacted with.
	 * @param template The instance template data.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	protected boolean validateConditions(List<Player> group, Npc npc, InstanceTemplate template)
	{
		final Player groupLeader = group.get(0);
		final Fort fort = npc.getFort();
		if (fort == null)
		{
			showHtmlFile(groupLeader, "noProperPledge.html");
			return false;
		}
		else if (fort.getFortState() == 0)
		{
			showHtmlFile(groupLeader, "noContractYet.html");
			return false;
		}
		else if (fort.getFortState() == 2)
		{
			showHtmlFile(groupLeader, "noCastleContract.html");
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
	 * It stops any active spawn tasks associated with the instance.<br>
	 * The {@code spawnTask} parameter is cleared to {@code null}.
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
	 * Schedules a task to spawn a raid after a delay.<br>
	 * The method determines the raid type based on the {@code Instance} status.<br>
	 * It sets the spawn task as a parameter in the {@code Instance}.
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
	 * This is the entry point for the {@code FortressDungeon} class.<br>
	 * It initializes a new instance of the dungeon script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new FortressDungeon();
	}
}
