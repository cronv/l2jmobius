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
package ai.others;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles the artificial intelligence for the Warrior Fishing Block.<br>
 * It defines the behavior of this specific NPC script.
 * @author Zoey76
 */
public class WarriorFishingBlock extends Script
{
	// Monsters
	private static final int[] MONSTERS =
	{
		18319, // Caught Frog
		18320, // Caught Undine
		18321, // Caught Rakul
		18322, // Caught Sea Giant
		18323, // Caught Sea Horse Soldier
		18324, // Caught Homunculus
		18325, // Caught Flava
		18326, // Caught Gigantic Eye
	};
	
	// NPC Strings
	private static final NpcStringId[] NPC_STRINGS_ON_SPAWN =
	{
		NpcStringId.IS_THERE_FOOD_LIKE_S1_HERE,
		NpcStringId.S1_HOW_LUCKY_I_AM,
		NpcStringId.PRAY_THAT_YOU_CAUGHT_A_WRONG_FISH_S1
	};
	private static final NpcStringId[] NPC_STRINGS_ON_ATTACK =
	{
		NpcStringId.DO_YOU_KNOW_WHAT_A_FROG_TASTES_LIKE,
		NpcStringId.I_WILL_SHOW_YOU_THE_POWER_OF_A_FROG,
		NpcStringId.I_WILL_SWALLOW_AT_A_MOUTHFUL
	};
	private static final NpcStringId[] NPC_STRINGS_ON_KILL =
	{
		NpcStringId.UGH_NO_CHANCE_HOW_COULD_THIS_ELDER_PASS_AWAY_LIKE_THIS,
		NpcStringId.CROAK_CROAK_A_FROG_IS_DYING,
		NpcStringId.A_FROG_TASTES_BAD_YUCK
	};
	
	// Misc
	private static final int CHANCE_TO_SHOUT_ON_ATTACK = 33;
	private static final int DESPAWN_TIME = 50; // 50 seconds to despawn
	
	/**
	 * Initializes the {@code WarriorFishingBlock} script.<br>
	 * This constructor registers all required monster IDs for attack, kill, and spawn events.
	 */
	public WarriorFishingBlock()
	{
		addAttackId(MONSTERS);
		addKillId(MONSTERS);
		addSpawnId(MONSTERS);
	}
	
	/**
	 * Handles specific events for the warrior fishing block.<br>
	 * It processes logic for spawning and despawning behaviors.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "SPAWN":
			{
				final WorldObject obj = npc.getTarget();
				if ((obj == null) || !obj.isPlayer())
				{
					npc.decayMe();
				}
				else
				{
					final Player target = obj.asPlayer();
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_SPAWN), target.getName());
					npc.asAttackable().addDamageHate(target, 0, 2000);
					npc.getAI().notifyActionAttacked(target);
					npc.addAttackerToAttackByList(target);
					
					startQuestTimer("DESPAWN", DESPAWN_TIME * 1000, npc, target);
				}
				break;
			}
			case "DESPAWN":
			{
				npc.decayMe();
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * It may trigger a random dialogue message based on a specific chance.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (getRandom(100) < CHANCE_TO_SHOUT_ON_ATTACK)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_ATTACK));
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} is killed.<br>
	 * It broadcasts a random message and cancels the despawn timer.
	 * @param npc The {@code Npc} that was killed.
	 * @param killer The {@code Player} who killed the {@code Npc}.
	 * @param isSummon Whether the kill was performed by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(NPC_STRINGS_ON_KILL));
		cancelQuestTimer("DESPAWN", npc, killer);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It cancels existing quest timers for the NPC.<br>
	 * It starts a new {@code SPAWN} quest timer for 2000 milliseconds.
	 * @param npc The {@code Npc} instance that just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		cancelQuestTimer("SPAWN", npc, null);
		cancelQuestTimer("DESPAWN", npc, null);
		startQuestTimer("SPAWN", 2000, npc, null);
	}
	
	/**
	 * This is the entry point for the {@code WarriorFishingBlock} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new WarriorFishingBlock();
	}
}
