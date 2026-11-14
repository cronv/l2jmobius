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
package instances.CallaxsAltar125;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This class handles the script logic for the {@code CallaxsAltar125} instance.<br>
 * It manages the sequence of events and interactions for players entering this specific zone.
 * @author Tanatos
 */
public class CallaxsAltar125 extends InstanceScript
{
	// NPC
	private static final int RUMIA = 31456; // Rumia - Priestess of Light
	private static final int SEAL_STONE = 31459; // Seal Stone of Sacrifice
	
	// Monsters
	private static final int FOLLOWER = 23931; // Follower of Sacrifice
	private static final int PRIEST = 23932; // Priest of Sacrifice
	private static final int APOSTLE = 23933; // Apostle of Sacrifice
	
	// Item
	private static final ItemHolder COMMON_PACK = new ItemHolder(83678, 1); // Callax's Common Supply Pack - 125
	private static final ItemHolder LARGE_PACK = new ItemHolder(83682, 1); // Callax's Large Supply Pack - 125
	
	// Misc
	private static final int TEMPLATE_ID = 358;
	private static final int COND_LEVEL = 110;
	private static final int FOLLOWERWAVES = 7;
	private static final int PRIESTWAVES = 3;
	private static final int PRIEST_CHANCE = 30;
	private static final int APOSTLE_CHANCE = 20;
	
	// Params
	private static final String CYCLE = "cycle";
	private static final String EVENT = "event";
	private static final String WAVE = "wave";
	private static final String ALIVE = "alive";
	
	/**
	 * Initializes the {@code CallaxsAltar125} instance script.<br>
	 * Sets up the required NPC IDs and monster spawns.<br>
	 * Registers the items and events for this specific instance.
	 */
	public CallaxsAltar125()
	{
		super(TEMPLATE_ID);
		addFirstTalkId(RUMIA, SEAL_STONE);
		addSpawnId(FOLLOWER, PRIEST, APOSTLE);
		addKillId(FOLLOWER, PRIEST, APOSTLE);
	}
	
	/**
	 * Handles various script events for the Callax's Altar instance.<br>
	 * It manages logic for entering the instance, spawning monsters, and completing objectives.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} performing the action.
	 * @return A string representing the HTML content to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "enterInstance":
			{
				// Cannot enter if player finished another Callax's Altar instance.
				final long currentTime = System.currentTimeMillis();
				if ((currentTime < InstanceManager.getInstance().getInstanceTime(player, 356)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 357)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 359)))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.C1_CANNOT_ENTER_YET).addString(player.getName()));
					htmltext = "condNoEnter.html";
					return htmltext;
				}
				
				if (player.getLevel() < COND_LEVEL)
				{
					htmltext = "condNoEnter.html";
					return htmltext;
				}
				
				enterInstance(player, npc, TEMPLATE_ID);
				
				final Instance world = player.getInstanceWorld();
				if (world != null)
				{
					world.getParameters().set(CYCLE, 0);
					world.getParameters().set(EVENT, "");
					world.getParameters().set(WAVE, 0);
					world.getParameters().set(ALIVE, 0);
					
					startQuestTimer("startInstance", 10000, null, player);
				}
				break;
			}
			case "startInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				showOnScreenMsg(world, NpcStringId.CALLAX_S_FOLLOWERS_ARE_CONDUCTING_A_RITUAL_THAT_WILL_TURN_THEM_INTO_MONSTERS_SELECT_A_SEAL_STONE_AND_STOP_THEM, ExShowScreenMessage.TOP_CENTER, 10000, true);
				world.spawnGroup("SEAL_STONES");
				break;
			}
			case "spawnSeals":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				showOnScreenMsg(world, NpcStringId.ANOTHER_SEAL_STONE_HAS_APPEARED_SELECT_A_SEAL_STONE_AND_STOP_THE_RITUAL, ExShowScreenMessage.TOP_CENTER, 10000, true);
				world.spawnGroup("SEAL_STONES");
				break;
			}
			case "stopTheRitual":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				final String current = world.getParameters().getString(EVENT, "");
				if (!current.isEmpty())
				{
					return null;
				}
				
				world.despawnGroup("SEAL_STONES");
				if (world.getStatus() == 0)
				{
					world.setStatus(1);
					world.setReenterTime();
				}
				
				final int random = getRandom(100);
				if (random < APOSTLE_CHANCE)
				{
					showOnScreenMsg(world, NpcStringId.CALLAX_S_FOLLOWERS_HAVE_TURNED_INTO_FEARSOME_MONSTERS, ExShowScreenMessage.TOP_CENTER, 10000, true);
					world.getParameters().set(EVENT, "apostle");
					startQuestTimer("spawnApostle", 500, null, player);
				}
				else if (random < (APOSTLE_CHANCE + PRIEST_CHANCE))
				{
					showOnScreenMsg(world, NpcStringId.CALLAX_S_FOLLOWERS_ARE_COMING_DEFEAT_THEM_AND_GET_READY_TO_FIGHT_CALLAX_S_PRIEST, ExShowScreenMessage.TOP_CENTER, 10000, true);
					world.getParameters().set(EVENT, "priest");
					world.getParameters().set(WAVE, 1);
					startQuestTimer("spawnFollowers", 500, null, player);
				}
				else
				{
					showOnScreenMsg(world, NpcStringId.CALLAX_S_FOLLOWERS_HAVE_TURNED_INTO_MONSTERS, ExShowScreenMessage.TOP_CENTER, 10000, true);
					world.getParameters().set(EVENT, "followers");
					world.getParameters().set(WAVE, 1);
					startQuestTimer("spawnFollowers", 500, null, player);
				}
				
				world.getParameters().set(ALIVE, 0);
				break;
			}
			case "spawnFollowers":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.spawnGroup("FOLLOWERS");
				break;
			}
			case "spawnPriest":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.spawnGroup("PRIEST");
				break;
			}
			case "spawnApostle":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.spawnGroup("APOSTLE");
				break;
			}
			case "spawnRumia":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				showOnScreenMsg(world, NpcStringId.ALL_RITUALS_HAVE_BEEN_INTERRUPTED_TALK_TO_PRIESTESS_OF_LIGHT_RUMIA, ExShowScreenMessage.TOP_CENTER, 10000, true);
				world.spawnGroup("RUMIA");
				world.finishInstance(5);
				break;
			}
			case "leaveInstance":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				world.finishInstance(0);
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the current world is an instance.<br>
	 * If it is an instance, it increments the {@code ALIVE} parameter in the world parameters.
	 * @param npc The {@code Npc} that has just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (!isInInstance(world))
		{
			return;
		}
		
		final int alive = world.getParameters().getInt(ALIVE, 0);
		world.getParameters().set(ALIVE, alive + 1);
	}
	
	/**
	 * Handles the logic triggered when an {@code Npc} is killed.<br>
	 * This method rewards the {@code Player} based on the specific monster type.<br>
	 * It also updates the remaining alive count and checks for wave completion.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the killed entity was a summoned unit.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			switch (npc.getId())
			{
				case PRIEST:
				{
					giveItems(killer, COMMON_PACK);
					break;
				}
				case APOSTLE:
				{
					giveItems(killer, LARGE_PACK);
					break;
				}
			}
			
			int alive = world.getParameters().getInt(ALIVE, 0);
			alive = Math.max(0, alive - 1);
			world.getParameters().set(ALIVE, alive);
			
			if (alive == 0)
			{
				handleWaveEnd(world, killer);
			}
		}
	}
	
	/**
	 * Processes the logic that occurs when a specific wave of enemies is defeated.<br>
	 * It checks the current event type and wave count to determine if more waves should spawn.<br>
	 * If all required waves are completed, it triggers the end of the instance event.
	 * @param world The {@code Instance} where the event is taking place.
	 * @param player The {@code Player} who triggered the wave completion.
	 */
	private void handleWaveEnd(Instance world, Player player)
	{
		final String e = world.getParameters().getString(EVENT, "");
		final int wave = world.getParameters().getInt(WAVE, 0);
		
		if (e.equals("followers"))
		{
			if (wave < FOLLOWERWAVES)
			{
				world.getParameters().set(WAVE, wave + 1);
				startQuestTimer("spawnFollowers", 500, null, player);
			}
			else
			{
				endEvent(world, player);
			}
			return;
		}
		
		if (e.equals("priest"))
		{
			if (wave < PRIESTWAVES)
			{
				world.getParameters().set(WAVE, wave + 1);
				startQuestTimer("spawnFollowers", 500, null, player);
				return;
			}
			
			if (wave == PRIESTWAVES)
			{
				world.getParameters().set(WAVE, PRIESTWAVES + 1);
				startQuestTimer("spawnPriest", 500, null, player);
				return;
			}
			
			endEvent(world, player);
			return;
		}
		
		if (e.equals("apostle"))
		{
			endEvent(world, player);
		}
	}
	
	/**
	 * Finalizes the current event and resets specific world parameters.<br>
	 * It increments the {@code CYCLE} count and starts a new quest timer.<br>
	 * The next task depends on whether the cycle is less than 3.
	 * @param world The {@code Instance} where the event occurs.
	 * @param player The {@code Player} involved in the event.
	 */
	private void endEvent(Instance world, Player player)
	{
		world.getParameters().set(EVENT, "");
		world.getParameters().set(WAVE, 0);
		world.getParameters().set(ALIVE, 0);
		
		int cycle = world.getParameters().getInt(CYCLE, 0);
		cycle++;
		world.getParameters().set(CYCLE, cycle);
		
		if (cycle < 3)
		{
			startQuestTimer("spawnSeals", 1000, null, player);
		}
		else
		{
			startQuestTimer("spawnRumia", 1000, null, player);
		}
	}
	
	/**
	 * Handles the initial interaction between a {@code Player} and an {@code Npc}.<br>
	 * This method determines which dialogue file to display.
	 * @param npc The {@code Npc} that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A string representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code CallaxsAltar125} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CallaxsAltar125();
	}
}
