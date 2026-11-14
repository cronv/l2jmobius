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
import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the distribution of rewards for players within a central area during a conquest event.<br>
 * This effect identifies eligible targets and grants items based on {@code ConquestConfig} settings.
 * @author CostyKiller
 */
public class ConquestFireCentralAreaReward extends AbstractEffect
{
	private static final ZoneType FIRE_SOURCE_CENTRAL_AREA_ZONE = ZoneManager.getInstance().getZoneByName("central_area_fire_source");
	
	/**
	 * Checks if the effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Creates a reward effect for the central area fire source.<br>
	 * This constructor initializes the reward using specific statistics.
	 * @param params The {@code StatSet} containing the reward values.
	 */
	public ConquestFireCentralAreaReward(StatSet params)
	{
	}
	
	/**
	 * Executes the instant effect for a specific skill action.<br>
	 * This method checks if the {@code effected} creature is eligible to receive rewards.<br>
	 * It grants Fire Source points to players in the correct zone based on a random chance.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayable())
		{
			return;
		}
		
		if (effected.isPlayer() && FIRE_SOURCE_CENTRAL_AREA_ZONE.isInsideZone(effected))
		{
			final Player player = effected.asPlayer();
			if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_FIRE_SOURCE_UPGRADE_CHANCE)
			{
				player.getVariables().set(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, player.getVariables().getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, 0) + ConquestConfig.CONQUEST_ABILITY_FIRE_SOURCE_EXP_AMOUNT);
				player.sendPacket(SystemMessageId.YOU_HAVE_RECEIVED_FIRE_SOURCE_POINTS);
				Quest.showOnScreenMsg(player, "You have received Fire Source points.", 5000);
			}
		}
	}
}
