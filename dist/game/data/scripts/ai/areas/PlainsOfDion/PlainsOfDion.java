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
package ai.areas.PlainsOfDion;

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Delu Lizardmen of the Plains of Dion prefer one-on-one fights, like the retail IsVs monsters.<br>
 * They challenge a player within one level of their own on sight, and call the monsters around them for help when another player interrupts their duel.
 * @author Gladicek, Mobius
 */
public class PlainsOfDion extends Script
{
	private static final int[] DELU_LIZARDMEN =
	{
		21105, // Delu Lizardman Special Agent
		21107, // Delu Lizardman Commander
	};
	private static final String TIMER = "DUEL_OVER";
	private static final NpcStringId[] CHALLENGE_MSG =
	{
		NpcStringId.S1_COME_ON_I_LL_TAKE_YOU_ON,
		NpcStringId.S1_I_LL_DEAL_WITH_YOU_MYSELF,
		NpcStringId.S1_THIS_IS_PERSONAL,
		NpcStringId.S1_LEAVE_US_ALONE_THIS_IS_BETWEEN_US,
		NpcStringId.S1_KILLING_YOU_WILL_BE_A_PLEASURE
	};
	private static final NpcStringId[] MONSTERS_MSG =
	{
		NpcStringId.S1_HOW_DARE_YOU_INTERRUPT_OUR_FIGHT_HEY_GUYS_HELP,
		NpcStringId.S1_HEY_WE_RE_HAVING_A_DUEL_HERE,
		NpcStringId.THE_DUEL_IS_OVER_ATTACK,
		NpcStringId.KILL_THE_COWARD,
		NpcStringId.HOW_DARE_YOU_INTERRUPT_A_SACRED_DUEL_YOU_MUST_BE_TAUGHT_A_LESSON
	};
	private static final NpcStringId[] MONSTERS_ASSIST_MSG =
	{
		NpcStringId.DIE_YOU_COWARD,
		NpcStringId.WHAT_ARE_YOU_LOOKING_AT_2,
		NpcStringId.KILL_THE_COWARD_2
	};
	
	private PlainsOfDion()
	{
		addCreatureSeeId(DELU_LIZARDMEN);
		addAttackId(DELU_LIZARDMEN);
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals(TIMER) && (npc != null))
		{
			npc.setScriptValue(0);
		}
		
		return null;
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (!creature.isPlayer() || npc.isDead() || (Math.abs(creature.getLevel() - npc.getLevel()) >= 2))
		{
			return;
		}
		
		final Intention intention = npc.getAI().getIntention();
		if ((intention == Intention.ATTACK) || (intention == Intention.CAST))
		{
			return;
		}
		
		npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(CHALLENGE_MSG), creature.getName());
		addAttackDesire(npc, creature);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		// Only another player hitting a lizardman busy with its opponent interrupts the duel.
		final WorldObject opponent = npc.getTarget();
		if (isSummon || (opponent == null) || (opponent == attacker) || !npc.isScriptValue(0))
		{
			return;
		}
		
		final int i = getRandom(5);
		if (i < 2)
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_MSG[i], attacker.getName());
		}
		else
		{
			npc.broadcastSay(ChatType.NPC_GENERAL, MONSTERS_MSG[i]);
		}
		
		npc.setScriptValue(attacker.getObjectId());
		startQuestTimer(TIMER, 20000, npc, null);
		World.forEachVisibleObjectInRange(npc, Monster.class, 600, monster ->
		{
			if (!monster.isDead() && !monster.isRaid() && (monster.getAI().getIntention() != Intention.ATTACK))
			{
				monster.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(MONSTERS_ASSIST_MSG));
				addAttackDesire(monster, attacker);
			}
		});
	}
	
	public static void main(String[] args)
	{
		new PlainsOfDion();
	}
}
