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
package ai.others.EquipmentSetPack;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;

/**
 * Handles the class pack checkers summoned by the Equipment Set Pack tickets.<br>
 * The checker reads the class of the character that summoned it, hands over the pack of that class and leaves.
 * @author Mobius
 */
public class EquipmentSetPack extends Script
{
	// NPCs
	private static final int CHECKER_10_DAY = 32702;
	private static final int CHECKER_30_DAY = 32703;
	private static final int CHECKER_90_DAY = 13284;
	
	// Packs, in the order of CLASS_CATEGORIES, with the fallback pack last.
	private static final int[] PACKS_10_DAY =
	{
		15194,
		15195,
		15196,
		15197,
		15198,
		15199,
		15200,
		15210
	};
	
	private static final int[] PACKS_30_DAY =
	{
		15201,
		15202,
		15203,
		15204,
		15205,
		15206,
		15207,
		15211
	};
	
	private static final int[] PACKS_90_DAY =
	{
		16968,
		16969,
		16970,
		16971,
		16972,
		16973,
		16974,
		16853
	};
	
	private static final CategoryType[] CLASS_CATEGORIES =
	{
		CategoryType.RECOM_KNIGHT_GROUP,
		CategoryType.RECOM_MAGIC_GROUP,
		CategoryType.RECOM_WARRIOR_GROUP,
		CategoryType.RECOM_ROGUE_GROUP,
		CategoryType.RECOM_KAMAEL_GROUP,
		CategoryType.RECOM_ORCF_GROUP,
		CategoryType.RECOM_ORCM_GROUP
	};
	
	// Misc
	private static final int GIVE_DELAY = 100;
	private static final int DESPAWN_DELAY = 5000;
	
	private EquipmentSetPack()
	{
		addSpawnId(CHECKER_10_DAY, CHECKER_30_DAY, CHECKER_90_DAY);
	}
	
	/**
	 * Hands the pack of the summoner's class over, then removes the checker.
	 * @param event The timer name.
	 * @param npc The checker.
	 * @param player Unused.
	 * @return Always {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (npc == null)
		{
			return null;
		}
		
		switch (event)
		{
			case "give":
			{
				final Creature summoner = npc.getSummoner();
				if ((summoner == null) || !summoner.isPlayer())
				{
					break;
				}
				
				final Player owner = summoner.asPlayer();
				final int[] packs = getPacks(npc.getId());
				int packIndex = CLASS_CATEGORIES.length; // The fallback pack.
				for (int i = 0; i < CLASS_CATEGORIES.length; i++)
				{
					if (owner.isInCategory(CLASS_CATEGORIES[i]))
					{
						packIndex = i;
						break;
					}
				}
				
				giveItems(owner, packs[packIndex], 1);
				break;
			}
			case "despawn":
			{
				npc.deleteMe();
				break;
			}
		}
		
		return null;
	}
	
	/**
	 * Starts the pack and despawn timers of a summoned checker.<br>
	 * SummonNpc sets the summoner only after the spawn event, so the pack is handed over by a short timer.
	 * @param npc The checker that was just summoned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		startQuestTimer("give", GIVE_DELAY, npc, null);
		startQuestTimer("despawn", DESPAWN_DELAY, npc, null);
	}
	
	/**
	 * Returns the pack table of a checker.
	 * @param npcId The checker id.
	 * @return The pack ids of that ticket.
	 */
	private static int[] getPacks(int npcId)
	{
		switch (npcId)
		{
			case CHECKER_30_DAY:
			{
				return PACKS_30_DAY;
			}
			case CHECKER_90_DAY:
			{
				return PACKS_90_DAY;
			}
			default:
			{
				return PACKS_10_DAY;
			}
		}
	}
	
	public static void main(String[] args)
	{
		new EquipmentSetPack();
	}
}
