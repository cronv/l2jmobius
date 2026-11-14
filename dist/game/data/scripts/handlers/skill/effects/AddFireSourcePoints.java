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

import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.util.StatSet;

/*
 * @author CostyKiller
 */
/**
 * This class handles the logic for adding fire source points to a target.<br>
 * It is triggered as an effect of a {@code Skill}.
 */
public class AddFireSourcePoints extends AbstractEffect
{
	/**
	 * Adds fire source points to a target.<br>
	 * This effect uses the provided {@code StatSet} for configuration.
	 * @param params The {@code StatSet} containing the necessary values.
	 */
	public AddFireSourcePoints(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect logic for a skill.<br>
	 * This method checks if the target is playable and attempts to grant fire source points based on a random chance.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayable())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		if (Quest.getRandom(100) < ConquestConfig.CONQUEST_ABILITY_FIRE_SOURCE_UPGRADE_CHANCE)
		{
			player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, 0) + ConquestConfig.CONQUEST_ABILITY_FIRE_SOURCE_EXP_AMOUNT);
			player.sendMessage("You have added Fire Source points.");
			Quest.showOnScreenMsg(player, "You have added Fire Source points.", 5000);
		}
		else
		{
			player.sendMessage("You have failed to add Fire Source points.");
			Quest.showOnScreenMsg(player, "You have failed to add Fire Source points.", 5000);
		}
	}
}
