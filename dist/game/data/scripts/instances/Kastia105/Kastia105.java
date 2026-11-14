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
package instances.Kastia105;

import java.util.List;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.managers.InstanceManager;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;

/**
 * This class defines the script logic for the {@code Kastia105} instance.<br>
 * It handles the sequence of events and interactions occurring within this specific zone.
 * @author Mobius
 */
public class Kastia105 extends InstanceScript
{
	// NPC
	private static final int KARINIA = 34541;
	private static final int RESEARCHER = 34566;
	private static final int BOSS = 24542;
	
	// Monsters
	private static final int[] MONSTERS =
	{
		24539, // Kastia's Keeper
		24540, // Kastia's Overseer
		24541, // Kastia's Warder
	};
	
	// Item
	private static final ItemHolder KASTIAS_PACK = new ItemHolder(81148, 1);
	
	// Skills
	private static final SkillHolder BOSS_BERSERKER = new SkillHolder(32520, 1);
	
	// Misc
	private static final int TEMPLATE_ID = 299;
	
	/**
	 * Initializes the {@code Kastia105} instance script.<br>
	 * Sets up the required NPCs and monster IDs for this zone.
	 */
	public Kastia105()
	{
		super(TEMPLATE_ID);
		addStartNpc(KARINIA);
		addTalkId(KARINIA);
		addSpawnId(BOSS);
		addCreatureSeeId(MONSTERS);
		addCreatureSeeId(BOSS);
		addKillId(BOSS);
	}
	
	/**
	 * Handles specific script events for the Kastia instance.<br>
	 * It manages instance entry requirements, wave progression, and NPC movement logic.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} interacting with the script.
	 * @return The result of the super method call or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "enterInstance":
			{
				// Cannot enter if player finished another Kastia instance.
				final long currentTime = System.currentTimeMillis();
				if ((currentTime < InstanceManager.getInstance().getInstanceTime(player, 298)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 300)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 305)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 306)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 317)) //
					|| (currentTime < InstanceManager.getInstance().getInstanceTime(player, 327)))
				{
					player.sendPacket(new SystemMessage(SystemMessageId.YOU_CANNOT_ENTER_AS_C1_IS_IN_ANOTHER_INSTANCE_ZONE).addString(player.getName()));
					return null;
				}
				
				enterInstance(player, npc, TEMPLATE_ID);
				if (player.getInstanceWorld() != null)
				{
					startQuestTimer("check_status", 10000, null, player);
				}
				
				return null;
			}
			case "check_status":
			{
				final Instance world = player.getInstanceWorld();
				if (!isInInstance(world))
				{
					return null;
				}
				
				switch (world.getStatus())
				{
					case 0:
					{
						world.setStatus(1);
						showOnScreenMsg(world, NpcStringId.LV_1_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
						moveMonsters(world.spawnGroup("wave_1"));
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 1:
					{
						if (world.getAliveNpcCount(MONSTERS) == 0)
						{
							world.setStatus(2);
							showOnScreenMsg(world, NpcStringId.LV_2_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
							moveMonsters(world.spawnGroup("wave_2"));
						}
						
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 2:
					{
						if (world.getAliveNpcCount(MONSTERS) == 0)
						{
							world.setStatus(3);
							showOnScreenMsg(world, NpcStringId.LV_3_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
							moveMonsters(world.spawnGroup("wave_3"));
						}
						
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 3:
					{
						if (world.getAliveNpcCount(MONSTERS) == 0)
						{
							world.setStatus(4);
							showOnScreenMsg(world, NpcStringId.LV_4_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
							moveMonsters(world.spawnGroup("wave_4"));
						}
						
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 4:
					{
						if (world.getAliveNpcCount(MONSTERS) == 0)
						{
							world.setStatus(5);
							showOnScreenMsg(world, NpcStringId.LV_5_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
							moveMonsters(world.spawnGroup("wave_5"));
						}
						
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 5:
					{
						if (world.getAliveNpcCount(MONSTERS) == 0)
						{
							world.setStatus(6);
							showOnScreenMsg(world, NpcStringId.LV_6_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
							moveMonsters(world.spawnGroup("wave_6"));
						}
						
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
					case 6:
					{
						if (world.getAliveNpcCount(MONSTERS) == 0)
						{
							world.setStatus(7);
							showOnScreenMsg(world, NpcStringId.LV_7_2, ExShowScreenMessage.TOP_CENTER, 10000, true);
							moveMonsters(world.spawnGroup("wave_7"));
							break;
						}
						
						startQuestTimer("check_status", 10000, null, player);
						break;
					}
				}
				
				return null;
			}
			case "move_to_spawn":
			{
				final Instance world = npc.getInstanceWorld();
				final Location loc = world.getTemplateParameters().getLocation("spawnpoint");
				final Location moveTo = new Location(loc.getX() + getRandom(-100, 100), loc.getY() + getRandom(-100, 100), loc.getZ());
				npc.setRunning();
				addMoveToDesire(npc, moveTo, 4);
				startQuestTimer("start_moving", 5000, npc, null);
				break;
			}
			case "start_moving":
			{
				final Instance world = npc.getInstanceWorld();
				final String selectedRoute = "routetospawn";
				WalkingTaskManager.getInstance().startMoving(npc, world.getTemplateParameters().getString(selectedRoute));
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Updates the movement state for a list of monsters.<br>
	 * This method disables random walking and starts a timer to move them to their spawn points.<br>
	 * It also prevents attackable monsters from returning to their original spawn points.
	 * @param monsterList The list of {@code Npc} objects to be moved.
	 */
	private void moveMonsters(List<Npc> monsterList)
	{
		int delay = 500;
		for (Npc monster : monsterList)
		{
			final Instance world = monster.getInstanceWorld();
			if (monster.isAttackable() && (world != null))
			{
				monster.setRandomWalking(false);
				startQuestTimer("move_to_spawn", delay, monster, null);
				monster.asAttackable().setCanReturnToSpawnPoint(false);
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It checks if the NPC belongs to a world instance.<br>
	 * If it does, the NPC casts the {@code BOSS_BERSERKER} skill.
	 * @param npc The {@code Npc} that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		final Instance world = npc.getInstanceWorld();
		if (world != null)
		{
			npc.doCast(BOSS_BERSERKER.getSkill());
		}
	}
	
	/**
	 * This method is called when an {@code Npc} sees a {@code Creature}.<br>
	 * It checks if the {@code Creature} is a player and within a specific distance.<br>
	 * If conditions are met, it makes the {@code Npc} stop moving and target the player.
	 * @param npc The {@code Npc} that spotted the creature.
	 * @param player The {@code Creature} that was seen by the {@code Npc}.
	 */
	@Override
	public void onCreatureSee(Npc npc, Creature player)
	{
		final Instance world = player.getInstanceWorld();
		if ((world != null) && (player.isPlayer()))
		{
			final double distance = npc.calculateDistance2D(player);
			if (distance < 900)
			{
				WalkingTaskManager.getInstance().cancelMoving(npc);
				npc.asMonster().addDamageHate(player, 0, 1000);
				npc.getAI().setIntentionActive();
				addAttackDesire(npc, player);
			}
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is killed.<br>
	 * This method checks if the kill occurred inside an instance.<br>
	 * It rewards the {@code Player} and completes the instance.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who performed the kill.
	 * @param isSummon Indicates whether the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance world = npc.getInstanceWorld();
		if (isInInstance(world))
		{
			world.getAliveNpcs(MONSTERS).forEach(monster -> monster.deleteMe());
			showOnScreenMsg(world, NpcStringId.YOU_HAVE_SUCCESSFULLY_COMPLETED_THE_KASTIA_S_LABYRINTH_YOU_WILL_BE_TRANSPORTED_TO_THE_SURFACE_SHORTLY_ALSO_YOU_CAN_LEAVE_THIS_PLACE_WITH_THE_HELP_OF_KASTIA_S_RESEARCHER, ExShowScreenMessage.TOP_CENTER, 10000, true);
			addSpawn(RESEARCHER, killer.getLocation(), true, 0, false, world.getId());
			giveItems(killer, KASTIAS_PACK);
			world.finishInstance(3);
		}
	}
	
	/**
	 * This is the entry point for the {@code Kastia105} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Kastia105();
	}
}
