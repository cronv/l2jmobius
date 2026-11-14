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
package ai.others;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * This script handles the behavior of monsters that polymorph when they are attacked.<br>
 * It manages the logic for triggering transformations during combat encounters.
 * @author Slyce, Mobius
 */
public class PolymorphingOnAttack extends Script
{
	private static final Map<Integer, List<Integer>> MOBSPAWNS = new HashMap<>();
	static
	{
		// Next npc, HP percent above and below which it polymorphs (both excluded), chance and message group.
		MOBSPAWNS.put(21265, Arrays.asList(21271, 50, 70, 30, 0)); // Cave Ant Larva -> Cave Ant
		MOBSPAWNS.put(21266, Arrays.asList(21269, -1, 101, 100, -1)); // Cave Ant Larva -> Cave Ant (always polymorphs)
		MOBSPAWNS.put(21267, Arrays.asList(21270, -1, 101, 100, -1)); // Cave Ant Larva -> Cave Ant Soldier (always polymorphs)
		MOBSPAWNS.put(21271, Arrays.asList(21272, 30, 50, 20, 1)); // Cave Ant -> Cave Ant Soldier
		MOBSPAWNS.put(21272, Arrays.asList(21273, 5, 30, 10, 2)); // Cave Ant Soldier -> Cave Noble Ant
	}
	
	protected static final NpcStringId[][] MOBTEXTS =
	{
		new NpcStringId[]
		{
			NpcStringId.ENOUGH_FOOLING_AROUND_GET_READY_TO_DIE,
			NpcStringId.YOU_IDIOT_I_VE_JUST_BEEN_TOYING_WITH_YOU,
			NpcStringId.NOW_THE_FUN_STARTS
		},
		new NpcStringId[]
		{
			NpcStringId.I_MUST_ADMIT_NO_ONE_MAKES_MY_BLOOD_BOIL_QUITE_LIKE_YOU_DO,
			NpcStringId.NOW_THE_BATTLE_BEGINS,
			NpcStringId.WITNESS_MY_TRUE_POWER
		},
		new NpcStringId[]
		{
			NpcStringId.PREPARE_TO_DIE,
			NpcStringId.I_LL_DOUBLE_MY_STRENGTH,
			NpcStringId.IT_S_STRONGER_THAN_EXPECTED_2
		}
	};
	
	/**
	 * Initializes the attack IDs for polymorphing.<br>
	 * This method populates the internal list of IDs from {@code MOBSPAWNS}.
	 */
	private PolymorphingOnAttack()
	{
		addAttackId(MOBSPAWNS.keySet());
	}
	
	/**
	 * Handles the logic for an {@code Npc} polymorphing when it is attacked.<br>
	 * This method checks if the {@code Npc} should transform based on its health and a random chance.<br>
	 * If the conditions are met, the current {@code Npc} is deleted and replaced by a new one.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} who performed the attack.
	 * @param damage The amount of damage dealt to the {@code Npc}.
	 * @param isSummon A boolean indicating if the attack came from a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (npc.isSpawned() && !npc.isDead())
		{
			final List<Integer> tmp = MOBSPAWNS.get(npc.getId());
			final int hpPercent = (int) ((npc.getCurrentHp() * 100) / npc.getMaxHp());
			if ((tmp != null) && (hpPercent > tmp.get(1)) && (hpPercent < tmp.get(2)) && (getRandom(100) < tmp.get(3)))
			{
				if (tmp.get(4) >= 0)
				{
					npc.broadcastPacket(new CreatureSay(npc, ChatType.NPC_GENERAL, getRandomEntry(MOBTEXTS[tmp.get(4)])));
				}
				
				npc.deleteMe();
				final Attackable newNpc = addSpawn(tmp.get(0), npc.getX(), npc.getY(), npc.getZ() + 20, npc.getHeading(), false, 0, true).asAttackable();
				final Creature originalAttacker = isSummon ? attacker.getServitors().values().stream().findFirst().orElse(attacker.getPet()) : attacker;
				newNpc.setRunning();
				newNpc.addDamageHate(originalAttacker, 0, 500);
				newNpc.getAI().setIntentionAttack(originalAttacker);
			}
		}
	}
	
	/**
	 * This is the main entry point for the {@code PolymorphingOnAttack} class.<br>
	 * It initializes a new instance of the script to start the logic.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new PolymorphingOnAttack();
	}
}
