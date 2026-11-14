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
package ai.bosses;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.serverpackets.ExShowScreenMessage;
import org.l2jmobius.gameserver.scripting.annotations.Disabled;

/**
 * This class handles the "Limit Barrier" mechanic for specific Raid and Epic bosses.<br>
 * It triggers a defensive barrier when boss HP reaches {@code 90%}, {@code 60%}, or {@code 30%}.<br>
 * Players must land {@code 600} hits within {@code 15} seconds to destroy the barrier, otherwise the boss restores full HP.
 * @author RobikBobik
 */
@Disabled // Behaviour does not go well with low population servers.
public class LimitBarrier extends Script
{
	// NPCs
	private static final int[] RAID_BOSSES =
	{
		29325, // Orfen
		29001, // Queen Ant
		29006, // Core
		26131, // Isabella
		26137, // Mimir
		26277, // Ashen Shadow Expeditionary Force Leaders Kantu
		26278, // Ashen Shadow Expeditionary Force Leaders Kai
		26279, // Ashen Shadow Expeditionary Force Leaders Heine
		26280, // Ashen Shadow Expeditionary Force Leaders Xenon
		26312, // Lithra
		29374, // Cyrax
		26431, // Avenger Alusion
		26432, // Avenger Graff
		26433, // Demon Venoma
		26434, // Fiend Sarboth
		26435, // Watcher Tristan
		26436, // Watcher Setheth
		26437, // Berserker Zetahl
		26438, // Berserker Tabris
		26439, // Ferocious Valac
		26440, // Arrogant Lebruum
		26441, // Witch Moira
		26442, // Mad Cullan
		26443, // Akrikhin Charon
		26444, // Scorpion King Votan
	};
	
	// Skill
	private static final SkillHolder LIMIT_BARRIER = new SkillHolder(32203, 1);
	
	// Misc
	private static final int HIT_COUNT = 600;
	private static final Map<Npc, Integer> RAIDBOSS_HITS = new ConcurrentHashMap<>();
	
	/**
	 * Private constructor for the {@code LimitBarrier} class.<br>
	 * This constructor initializes the required IDs for attack and kill events.
	 */
	private LimitBarrier()
	{
		addAttackId(RAID_BOSSES);
		addKillId(RAID_BOSSES);
	}
	
	/**
	 * Handles specific events for the Limit Barrier mechanic.<br>
	 * This method processes logic when a barrier is destroyed or fails.<br>
	 * It updates the {@code Npc} health and sends messages to the {@code Player}.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} who triggered the action.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "RESTORE_FULL_HP":
			{
				final int hits = RAIDBOSS_HITS.getOrDefault(npc, 0);
				if (hits < HIT_COUNT)
				{
					if (player != null)
					{
						npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_HAVE_FAILED_TO_DESTROY_THE_LIMIT_BARRIER_THE_RAID_BOSS_HAS_FULLY_RECOVERED_ITS_HEALTH, 2, 5000, true));
					}
					
					npc.setCurrentHp(npc.getStat().getMaxHp(), true);
					npc.stopSkillEffects(SkillFinishType.REMOVED, LIMIT_BARRIER.getSkillId());
					RAIDBOSS_HITS.put(npc, 0);
				}
				else if (hits > HIT_COUNT)
				{
					if (player != null)
					{
						npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.YOU_HAVE_DESTROYED_THE_LIMIT_BARRIER, 2, 5000, true));
					}
					
					npc.stopSkillEffects(SkillFinishType.REMOVED, LIMIT_BARRIER.getSkillId());
					RAIDBOSS_HITS.put(npc, 0);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method tracks hits against the limit barrier and triggers it at specific HP thresholds.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt by the attack.
	 * @param isSummon Indicates if the attack comes from a summon.
	 * @param skill The {@code Skill} used to perform the attack.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
		{
			final int hits = RAIDBOSS_HITS.getOrDefault(npc, 0);
			RAIDBOSS_HITS.put(npc, hits + 1);
		}
		
		if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.9)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.87)))
		{
			if (!npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
			{
				npc.setTarget(npc);
				npc.abortAttack();
				npc.abortCast();
				npc.doCast(LIMIT_BARRIER.getSkill());
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_RAID_BOSS_USES_THE_LIMIT_BARRIER_FOCUS_YOUR_ATTACKS_TO_DESTROY_THE_BARRIER_IN_15_SEC, 2, 5000, true));
				startQuestTimer("RESTORE_FULL_HP", 15000, npc, attacker);
			}
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.6)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.58)))
		{
			if (!npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
			{
				npc.setTarget(npc);
				npc.abortAttack();
				npc.abortCast();
				npc.doCast(LIMIT_BARRIER.getSkill());
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_RAID_BOSS_USES_THE_LIMIT_BARRIER_FOCUS_YOUR_ATTACKS_TO_DESTROY_THE_BARRIER_IN_15_SEC, 2, 5000, true));
				startQuestTimer("RESTORE_FULL_HP", 15000, npc, attacker);
			}
		}
		else if ((npc.getCurrentHp() < (npc.getMaxHp() * 0.3)) && (npc.getCurrentHp() > (npc.getMaxHp() * 0.28)))
		{
			if (!npc.isAffectedBySkill(LIMIT_BARRIER.getSkillId()))
			{
				npc.setTarget(npc);
				npc.abortAttack();
				npc.abortCast();
				npc.doCast(LIMIT_BARRIER.getSkill());
				npc.broadcastPacket(new ExShowScreenMessage(NpcStringId.THE_RAID_BOSS_USES_THE_LIMIT_BARRIER_FOCUS_YOUR_ATTACKS_TO_DESTROY_THE_BARRIER_IN_15_SEC, 2, 5000, true));
				startQuestTimer("RESTORE_FULL_HP", 15000, npc, attacker);
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles the cleanup of hit counters for the specific boss.
	 * @param npc The {@code Npc} that was just defeated.
	 * @param killer The {@code Player} who dealt the final blow.
	 * @param isSummon Indicates if the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		RAIDBOSS_HITS.remove(npc);
	}
	
	/**
	 * This method serves as the entry point for the {@code LimitBarrier} class.<br>
	 * It initializes a new instance of the {@code LimitBarrier} object.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new LimitBarrier();
	}
}
