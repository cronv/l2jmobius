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
package instances.CrystalCaverns;

import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.instancezone.Instance;
import org.l2jmobius.gameserver.entity.spawns.Spawn;
import org.l2jmobius.gameserver.mechanics.script.InstanceScript;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExSendUIEvent;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.taskmanagers.WalkingTaskManager;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Defines the logic and behavior for the Coral Garden instance zone within the Crystal Caverns.<br>
 * This class handles specific script events and environmental configurations for this area.
 * @author St3eT
 */
public class CrystalCavernsCoralGarden extends InstanceScript
{
	// NPCs
	private static final int CAVERNS_ENTRACE = 33522;
	private static final int MICHAELA_NORMAL = 25799;
	private static final int MICHAELA_WISE = 26116;
	private static final int MICHAELA_WEALTHY = 26115;
	private static final int MICHAELA_ARMED = 26114;
	private static final int GOLEM_1 = 19013; // Crystalline Golem
	private static final int GOLEM_2 = 19014; // Crystalline Golem
	
	// Location
	private static final Location BOSS_LOC = new Location(144307, 220032, -11824);
	
	// Misc
	private static final int TEMPLATE_ID = 165;
	private static final int BOSS_DOOR_ID = 24240026;
	private static final int PLAYER_MAX_DISTANCE = 250;
	
	/**
	 * Initializes the {@code CrystalCavernsCoralGarden} instance script.<br>
	 * This constructor sets up all required NPCs and event triggers.<br>
	 * It registers start, talk, kill, and attack IDs for the zone.
	 */
	public CrystalCavernsCoralGarden()
	{
		super(TEMPLATE_ID);
		addStartNpc(CAVERNS_ENTRACE);
		addTalkId(CAVERNS_ENTRACE);
		addFirstTalkId(GOLEM_1, GOLEM_2);
		addKillId(MICHAELA_NORMAL, MICHAELA_WISE, MICHAELA_WEALTHY, MICHAELA_ARMED);
		addAttackId(MICHAELA_NORMAL, MICHAELA_WISE, MICHAELA_WEALTHY, MICHAELA_ARMED);
		addRouteFinishedId(GOLEM_1, GOLEM_2);
		addInstanceEnterId(TEMPLATE_ID);
		addInstanceLeaveId(TEMPLATE_ID);
	}
	
	/**
	 * Handles specific timed events for the instance.<br>
	 * This method processes logic for success, loop, and fail timers.
	 * @param event The name of the timer event to process.
	 * @param params A {@code StatSet} containing variables related to the NPC or player.
	 * @param npc The {@code Npc} instance associated with the event.
	 * @param player The {@code Player} involved in the interaction.
	 */
	@Override
	public void onTimerEvent(String event, StatSet params, Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			final StatSet npcVars = npc.getVariables();
			
			switch (event)
			{
				case "SUCCESS_TIMER":
				{
					showOnScreenMsg(instance, NpcStringId.GOLEM_LOCATION_SUCCESSFUL_ENTRY_ACCESSED, ExShowScreenMessage.MIDDLE_CENTER, 5000);
					break;
				}
				case "LOOP_TIMER":
				{
					final Player plr = npcVars.getObject("PLAYER_OBJECT", Player.class);
					if ((plr != null) && (npc.calculateDistance3D(plr) > PLAYER_MAX_DISTANCE) && npcVars.getBoolean("NPC_FOLLOWING", true))
					{
						WalkingTaskManager.getInstance().cancelMoving(npc);
						addMoveToDesire(npc, new Location(npc.getX() + getRandom(-100, 100), npc.getY() + getRandom(-150, 150), npc.getZ()), 23);
						npc.setRunning();
						npcVars.set("NPC_FOLLOWING", false);
						getTimers().cancelTimer("LOOP_TIMER", npc, null);
						getTimers().addTimer("FAIL_TIMER", 5000, npc, null);
					}
					break;
				}
				case "FAIL_TIMER":
				{
					final Spawn spawn = npc.getSpawn();
					final int spawnX = spawn.getX();
					final int spawnY = spawn.getY();
					if (!npcVars.getBoolean("NPC_FOLLOWING", true))
					{
						WalkingTaskManager.getInstance().cancelMoving(npc);
						npc.setWalking();
						npc.teleToLocation(spawnX, spawnY, spawn.getZ());
						npc.setScriptValue(0);
						npc.setNameString(null);
						npc.setTitleString(null);
						npc.setTitle(null);
						npc.broadcastInfo();
					}
					
					final int x = npc.getX();
					final int y = npc.getY();
					npcVars.set("CAN_CALL_MONSTERS", ((spawnX - ((x * spawnX) - x)) + (spawnY - (y * spawnY) - y)) > (200 * 200));
					break;
				}
			}
		}
	}
	
	/**
	 * Handles specific events triggered within the instance.<br>
	 * This method checks for the {@code enterInstance} event to trigger logic.<br>
	 * It then calls the parent class implementation.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} involved in the event.
	 * @param player The {@code Player} involved in the event.
	 * @return The result of the superclass method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("enterInstance"))
		{
			enterInstance(player, npc, TEMPLATE_ID);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when a {@code Player} enters an {@code Instance}.<br>
	 * It sends the elapsed and remaining time to the player.
	 * @param player The {@code Player} who entered the instance.
	 * @param instance The {@code Instance} being entered.
	 */
	@Override
	public void onInstanceEnter(Player player, Instance instance)
	{
		final int startTime = (int) (instance.getElapsedTime() / 1000);
		final int endTime = (int) (instance.getRemainingTime() / 1000);
		player.sendPacket(new ExSendUIEvent(player, false, true, startTime, endTime, NpcStringId.ELAPSED_TIME));
	}
	
	/**
	 * This method is called when a {@code Player} leaves an {@code Instance}.<br>
	 * It sends a UI event to the player to reset the elapsed time.
	 * @param player The {@code Player} who is leaving the instance.
	 * @param instance The {@code Instance} that was just exited.
	 */
	@Override
	public void onInstanceLeave(Player player, Instance instance)
	{
		player.sendPacket(new ExSendUIEvent(player, true, true, 0, 0, NpcStringId.ELAPSED_TIME));
	}
	
	/**
	 * Handles the logic when a player talks to an {@code Npc} for the first time.<br>
	 * This method updates the {@code Npc} state and starts its movement.<br>
	 * It also sets specific titles and variables based on the {@code Player}.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} who initiated the talk.
	 * @return This method returns {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance) && npc.isScriptValue(0))
		{
			npc.setScriptValue(1);
			npc.getVariables().set("PLAYER_OBJECT", player);
			npc.setNameString(NpcStringId.TRAITOR_CRYSTALLINE_GOLEM);
			npc.setTitleString(NpcStringId.S1_A_TREASON);
			npc.setTitle(player.getName());
			npc.broadcastInfo();
			WalkingTaskManager.getInstance().startMoving(npc, npc.getId() == GOLEM_1 ? "gd_golem_1" : "gd_golem_2");
			getTimers().addRepeatingTimer("LOOP_TIMER", 500, npc, null);
		}
		
		return null;
	}
	
	/**
	 * This method is called when an {@code Npc} finishes its movement route.<br>
	 * It stops the NPC and displays a message to the player.<br>
	 * If no golems are present, it opens the boss door and spawns a random boss.
	 * @param npc The {@code Npc} that finished moving.
	 */
	@Override
	public void onRouteFinished(Npc npc)
	{
		final Instance instance = npc.getInstanceWorld();
		if (instance != null)
		{
			WalkingTaskManager.getInstance().cancelMoving(npc);
			showOnScreenMsg(instance, NpcStringId.GOLEM_ENTERED_THE_REQUIRED_ZONE, ExShowScreenMessage.MIDDLE_CENTER, 5000);
			npc.deleteMe();
			
			if (instance.getAliveNpcCount(GOLEM_1, GOLEM_2) == 0)
			{
				instance.openCloseDoor(BOSS_DOOR_ID, true);
				
				final int random = getRandom(100);
				int bossId = -1;
				if (random < 55)
				{
					bossId = MICHAELA_NORMAL;
				}
				else if (random < 80)
				{
					bossId = MICHAELA_WISE;
				}
				else if (random < 95)
				{
					bossId = MICHAELA_WEALTHY;
				}
				else
				{
					bossId = MICHAELA_ARMED;
				}
				
				final Npc boss = addSpawn(bossId, BOSS_LOC, false, 0, false, instance.getId());
				getTimers().addTimer("SUCCESS_TIMER", 5000, boss, null);
			}
		}
	}
	
	/**
	 * Handles logic when an {@code Npc} is killed.<br>
	 * This method checks if the killed NPC is a specific quest character.<br>
	 * If it is, the current instance will be finished.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who killed the NPC.
	 * @param isSummon Whether the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			switch (npc.getId())
			{
				case MICHAELA_NORMAL:
				case MICHAELA_WISE:
				case MICHAELA_WEALTHY:
				case MICHAELA_ARMED:
				{
					instance.finishInstance();
					break;
				}
			}
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method checks if specific NPCs are hit to trigger door events.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final Instance instance = npc.getInstanceWorld();
		if (isInInstance(instance))
		{
			switch (npc.getId())
			{
				case MICHAELA_NORMAL:
				case MICHAELA_WISE:
				case MICHAELA_WEALTHY:
				case MICHAELA_ARMED:
				{
					if (npc.isScriptValue(0))
					{
						npc.setScriptValue(1);
						instance.openCloseDoor(BOSS_DOOR_ID, false);
					}
					break;
				}
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code CrystalCavernsCoralGarden} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new CrystalCavernsCoralGarden();
	}
}
