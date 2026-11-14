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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.Recommendations;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExVoteSystemInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for applying a recommendation effect.<br>
 * It manages how recommendations are granted to players within the game.
 * @author NosBit, Mobius
 */
public class GiveRecommendation extends AbstractEffect
{
	private final int _amount;
	
	/**
	 * Creates a new instance of the {@code GiveRecommendation} effect.<br>
	 * It initializes the effect using values from a {@code StatSet}.<br>
	 * The {@code amount} value must be greater than 0.
	 * @param params The {@code StatSet} containing the configuration data for this effect.
	 */
	public GiveRecommendation(StatSet params)
	{
		_amount = params.getInt("amount", 0);
		if (_amount == 0)
		{
			throw new IllegalArgumentException("amount parameter is missing or set to 0.");
		}
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
	 * Applies the recommendation effect instantly to a target.<br>
	 * This method updates the target's recommendation count and sends notifications.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player target = (effected != null) && effected.isPlayer() ? effected.asPlayer() : null;
		if (target != null)
		{
			int recommendationsGiven = _amount;
			final Recommendations recommendations = target.getRecommendations();
			final int recomLeft = recommendations.getLeft();
			if ((recomLeft + _amount) >= 255)
			{
				recommendationsGiven = 255 - recomLeft;
			}
			
			if (recommendationsGiven > 0)
			{
				recommendations.setLeft(recomLeft + recommendationsGiven);
				
				final SystemMessage sm = new SystemMessage(SystemMessageId.RECOMMENDATIONS_OBTAINED_S1);
				sm.addInt(recommendationsGiven);
				target.sendPacket(sm);
				target.updateUserInfo();
				target.sendPacket(new ExVoteSystemInfo(target));
			}
			else
			{
				final Player player = (effector != null) && effector.isPlayer() ? effector.asPlayer() : null;
				if (player != null)
				{
					player.sendPacket(SystemMessageId.NOTHING_HAPPENED);
				}
			}
		}
	}
}
