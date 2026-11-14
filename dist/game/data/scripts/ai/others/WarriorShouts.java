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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.NpcSay;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Monsters with ShoutMsg parameters shout like the retail warrior AI: on spawn, now and then while idle or fighting, and when they die.
 * @author Mobius
 */
public class WarriorShouts extends Script
{
	private static final int[] NPCS =
	{
		20520, // Pirate Captain Uthanka
	};
	private static final String TIMER = "SHOUT";
	
	private WarriorShouts()
	{
		addSpawnId(NPCS);
		addKillId(NPCS);
	}
	
	@Override
	public void onSpawn(Npc npc)
	{
		shout(npc, "ShoutMsg1");
		final StatSet parameters = npc.getParameters();
		if ((parameters.getInt("ShoutMsg2", 0) > 0) || (parameters.getInt("ShoutMsg3", 0) > 0))
		{
			startQuestTimer(TIMER, 10000, npc, null, true);
		}
	}
	
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!event.equals(TIMER) || (npc == null))
		{
			return null;
		}
		
		if (npc.isDead() || !npc.isSpawned())
		{
			cancelQuestTimer(TIMER, npc, null);
			return null;
		}
		
		if (npc.isInCombat())
		{
			if (Rnd.get(100) < 10)
			{
				shout(npc, "ShoutMsg3");
			}
		}
		else if (Rnd.get(1000) < 17)
		{
			shout(npc, "ShoutMsg2");
		}
		
		return null;
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		cancelQuestTimer(TIMER, npc, null);
		if (Rnd.get(100) < 30)
		{
			shout(npc, "ShoutMsg4");
		}
	}
	
	/**
	 * Shouts the text of a ShoutMsg parameter to the players who see the {@code Npc}.
	 * @param npc The shouting {@code Npc}.
	 * @param parameter The ShoutMsg parameter name.
	 */
	private void shout(Npc npc, String parameter)
	{
		final int stringId = npc.getParameters().getInt(parameter, 0);
		if (stringId > 0)
		{
			World.broadcastToVisiblePlayers(npc, new NpcSay(npc, ChatType.NPC_SHOUT, NpcStringId.getNpcStringId(stringId)));
		}
	}
	
	public static void main(String[] args)
	{
		new WarriorShouts();
	}
}
