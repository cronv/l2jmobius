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
package ai.areas.GardenOfGenesis.Apherus;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;

/**
 * This class handles the logic for the {@code Apherus} Raid Boss.<br>
 * It manages the boss behavior and interactions within its specific area.
 * @author Gigi
 */
public class Apherus extends Script
{
	private static final int APHERUS = 25775;
	private static final int APHERUS_SUBORDINATE = 25865;
	private static final int[] APHERUS_DOOR_NPCS =
	{
		33133,
		33134,
		33135,
		33136
	};
	private static int[] APHERUS_DOORS =
	{
		26210041,
		26210042,
		26210043,
		26210044
	};
	private static final int[] APHERUS_DOOR_GUARD =
	{
		25776,
		25777,
		25778
	};
	private static final SkillHolder GARDEN_APHERUS_RECOVERY = new SkillHolder(14088, 1);
	private static final SkillHolder APHERUS_INVICIBILITY = new SkillHolder(14201, 1);
	private static final int APERUS_KEY = 17373;
	private static final int APHERUS_ZONE_ID = 60060;
	private static boolean _doorIsOpen = false;
	
	/**
	 * Initializes the {@code Apherus} script with required IDs.<br>
	 * This constructor registers attack, kill, spawn, and zone identifiers.<br>
	 * It also sets up the starting NPCs and talk IDs for the area.
	 */
	public Apherus()
	{
		addAttackId(APHERUS);
		addKillId(APHERUS);
		addSpawnId(APHERUS);
		addTalkId(APHERUS_DOOR_NPCS);
		addStartNpc(APHERUS_DOOR_NPCS);
		addFirstTalkId(APHERUS_DOOR_NPCS);
		addEnterZoneId(APHERUS_ZONE_ID);
		addExitZoneId(APHERUS_ZONE_ID);
	}
	
	/**
	 * Handles specific events triggered by the {@code Apherus} script.<br>
	 * If the {@code event} is {@code "buff"}, it removes invincibility from nearby {@code Npc} objects.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} that triggered the event.
	 * @param player The {@code Player} involved in the interaction.
	 * @return The original {@code event} string or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("buff"))
		{
			World.forEachVisibleObjectInRange(npc, Npc.class, 3000, apherus ->
			{
				if ((apherus.getId() == APHERUS))
				{
					apherus.stopSkillEffects(APHERUS_INVICIBILITY.getSkill());
				}
			});
			
			return null;
		}
		
		return event;
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method may trigger the spawning of subordinate minions.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if ((getRandom(120) < 2) && !npc.isDead())
		{
			final Npc minions = addSpawn(APHERUS_SUBORDINATE, npc.getX() + getRandom(-30, 30), npc.getY() + getRandom(-30, 30), npc.getZ(), npc.getHeading(), true, 300000, false);
			addAttackPlayerDesire(minions, attacker);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It removes nearby subordinates from the world.
	 * @param npc The {@code Npc} that was just killed.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		World.forEachVisibleObjectInRange(npc, Npc.class, 1500, minion ->
		{
			if (minion.getId() == APHERUS_SUBORDINATE)
			{
				minion.deleteMe();
			}
		});
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It applies the invincibility skill to the NPC.<br>
	 * It sets the door status to closed and closes all doors in the area.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		APHERUS_INVICIBILITY.getSkill().applyEffects(npc, npc);
		_doorIsOpen = false;
		for (int door : APHERUS_DOORS)
		{
			closeDoor(door, npc.getInstanceId());
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} enters a specific {@code ZoneType}.<br>
	 * It stops recovery effects for raid bosses.<br>
	 * It teleports non-GM playable characters to town if the door is closed.
	 * @param creature The {@code Creature} entering the zone.
	 * @param zone The {@code ZoneType} being entered.
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		if (creature.isRaid())
		{
			creature.stopSkillEffects(GARDEN_APHERUS_RECOVERY.getSkill());
		}
		else if (creature.isPlayable() && !_doorIsOpen && !creature.isGM())
		{
			creature.teleToLocation(TeleportWhereType.TOWN);
		}
	}
	
	/**
	 * This method is called when a {@code Creature} leaves a specific {@code ZoneType}.<br>
	 * It checks if the creature is in raid mode.<br>
	 * If it is in raid, it applies recovery effects to the creature.
	 * @param creature The {@code Creature} that is leaving the zone.
	 * @param zone The {@code ZoneType} that was just exited.
	 */
	@Override
	public void onExitZone(Creature creature, ZoneType zone)
	{
		if (creature.isRaid())
		{
			GARDEN_APHERUS_RECOVERY.getSkill().applyEffects(creature, creature);
		}
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method checks if the door is open and attempts to use a key item.<br>
	 * It may trigger a quest timer, open doors, or spawn protector monsters.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A string representing the HTML file to display, or {@code null}.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		if (!_doorIsOpen)
		{
			if (!player.destroyItemByItemId(ItemProcessType.FEE, APERUS_KEY, 1, player, true))
			{
				return "apherusDoor-no.html";
			}
			
			if (getRandom(100) > 60)
			{
				startQuestTimer("buff", 500, npc, player);
				for (int door : APHERUS_DOORS)
				{
					openDoor(door, npc.getInstanceId());
				}
				
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.APHERUS_GARDEN_IS_OPEN, ExShowScreenMessage.TOP_CENTER, 3000, true));
			}
			else
			{
				for (int i = 0; i < 4; i++)
				{
					final Monster protector = addSpawn(getRandomEntry(APHERUS_DOOR_GUARD), player.getX() + getRandom(10, 30), player.getY() + getRandom(10, 30), player.getZ(), 0, false, 600000, false).asMonster();
					protector.setRunning();
					protector.setTarget(player);
					addAttackPlayerDesire(protector, player);
				}
				
				showOnScreenMsg(player, NpcStringId.S1_THE_KEY_DOES_NOT_MATCH_SO_WE_RE_IN_TROUBLE, ExShowScreenMessage.TOP_CENTER, 6000, true, player.getName());
			}
		}
		else
		{
			return "apherusDoor-no.html";
		}
		
		return null;
	}
	
	/**
	 * This is the entry point for the {@code Apherus} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Apherus();
	}
}
