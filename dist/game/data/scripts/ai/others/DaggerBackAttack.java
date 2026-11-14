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

import org.l2jmobius.gameserver.ai.Intention;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Monsters with the DaggerBackAttack parameter notice a player who shows them their back up close.<br>
 * On sight of such a player within 100 range, a monster that is not already fighting calls it out with a 50% chance and attacks.
 * @author Mobius
 */
public class DaggerBackAttack extends Script
{
	private static final int[] NPCS =
	{
		20084, // Ant Scout
		20551, // Road Scavenger
	};
	
	private DaggerBackAttack()
	{
		addCreatureSeeId(NPCS);
	}
	
	@Override
	public void onCreatureSee(Npc npc, Creature creature)
	{
		if (!creature.isPlayer() || npc.isDead())
		{
			return;
		}
		
		final Intention intention = npc.getAI().getIntention();
		if ((intention == Intention.ATTACK) || (intention == Intention.CAST) || (getRandom(100) >= 50) || !npc.isInsideRadius2D(creature, 100) || !npc.isBehind(creature))
		{
			return;
		}
		
		final Player player = creature.asPlayer();
		switch (getRandom(4))
		{
			case 0:
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, player.getName() + "! Watch your back!!!");
				break;
			}
			case 1:
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, player.getName() + "! Watch your back!");
				break;
			}
			case 2:
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, "Your rear is practically unguarded!");
				break;
			}
			default:
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, "How dare you turn your back on me!");
				break;
			}
		}
		
		npc.asAttackable().addDamageHate(player, 0, 200);
		npc.getAI().setIntentionAttack(player);
	}
	
	public static void main(String[] args)
	{
		new DaggerBackAttack();
	}
}
