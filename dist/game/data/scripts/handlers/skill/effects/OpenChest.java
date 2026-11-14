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
package handlers.skill.effects;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Chest;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for opening a {@code Chest} object.<br>
 * This class manages the interaction between a {@code Creature} and a chest instance.
 * @author Adry_85, Mobius
 */
public class OpenChest extends AbstractEffect
{
	// @formatter:off
	// Unlock (27) success cap by skill level, starting at level 1.
	private static final int[] UNLOCK_CAPS =
	{
		98, 84, 99, 84, 88, 90, 89, 88, 86, 90, 87, 89, 89, 89, 89
	};

	// Key of treasure box (2229) factors a, b, c of (a * x^2 - b * x + c) by key level 1-7, x being the chest levels above the key range; level 8 always opens.
	private static final double[][] BOX_KEY_FACTORS =
	{
		{ 0.0002, 0.0264, 0.7695 },
		{ 0.0003, 0.0279, 0.7568 },
		{ 0.0003, 0.0269, 0.7334 },
		{ 0.0003, 0.0284, 0.8034 },
		{ 0.0005, 0.0356, 0.9065 },
		{ 0.0009, 0.0373, 0.8572 },
		{ 0.0043, 0.0671, 0.9593 }
	};
	// @formatter:on
	
	/**
	 * Creates a new instance of the {@code OpenChest} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public OpenChest(StatSet params)
	{
	}
	
	/**
	 * Checks if this effect happens immediately.<br>
	 * This method returns {@code true} for instant effects.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for opening a chest.<br>
	 * The success chance follows the retail treasure box AI for the skill used.<br>
	 * A successful attempt kills the chest for its special drop, a failed one destroys it.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} being acted upon, which should be a {@code Chest}.
	 * @param skill The {@code Skill} used to perform the action.
	 * @param item The {@code Item} associated with the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!(effected instanceof Chest))
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		final Chest chest = (Chest) effected;
		if (chest.isDead() || (player.getInstanceWorld() != chest.getInstanceWorld()))
		{
			return;
		}
		
		final int chestLevel = chest.getLevel();
		final int skillLevel = skill.getLevel();
		int successChance;
		switch (skill.getId())
		{
			case 27: // Unlock
			{
				final int cap = UNLOCK_CAPS[Math.min(skillLevel, UNLOCK_CAPS.length) - 1];
				successChance = Math.min(cap, cap - ((chestLevel - (skillLevel * 4) - 16) * 6));
				break;
			}
			case 2065: // Key of box
			{
				successChance = Math.min(60, (int) (60 - ((chestLevel - ((skillLevel - 1) * 10)) * 1.5)));
				break;
			}
			case 2229: // Key of treasure box
			{
				final int levelsAbove = chestLevel - (19 + ((skillLevel - 1) * 10));
				if ((skillLevel > BOX_KEY_FACTORS.length) || (levelsAbove <= 0))
				{
					successChance = 100;
				}
				else
				{
					final double[] factors = BOX_KEY_FACTORS[skillLevel - 1];
					successChance = (int) (((factors[0] * levelsAbove * levelsAbove) - (factors[1] * levelsAbove) + factors[2]) * 100);
				}
				break;
			}
			case 3155:
			{
				successChance = 90;
				break;
			}
			default:
			{
				// Other unlock skills.
				successChance = 100;
				break;
			}
		}
		
		if (Rnd.get(100) < successChance)
		{
			player.broadcastSocialAction(3);
			chest.setSpecialDrop();
			chest.setMustRewardExpSp(false);
			chest.reduceCurrentHp(chest.getMaxHp(), player, skill);
		}
		else
		{
			// A failed unlock destroys the box.
			player.broadcastSocialAction(13);
			chest.deleteMe();
		}
	}
}
