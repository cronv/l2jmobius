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
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.homunculus.ExShowHomunculusBirthInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the effect of reducing a skill's waiting time.<br>
 * It modifies the {@code PlayerVariables} to decrease the cooldown period for specific skills.
 * @author CostyKiller
 */
public class DecreaseWaitingTime extends AbstractEffect
{
	private final long _time;
	
	/**
	 * This constructor initializes the effect with a specific time value.<br>
	 * It retrieves the {@code time} property from the provided {@code StatSet}.<br>
	 * If no value is found, it defaults to {@code 0}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public DecreaseWaitingTime(StatSet params)
	{
		_time = params.getLong("time", 0);
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
	 * Executes the instant effect for decreasing homunculus waiting time.<br>
	 * This method checks if the player has a valid creation in progress.<br>
	 * It validates the remaining time before updating the {@code PlayerVariables}.<br>
	 * If requirements are met, it reduces the wait time and sends a packet to the player.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The skill being used.
	 * @param item The item consumed to trigger the effect.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		final long currentTime = System.currentTimeMillis();
		long creationTime = player.getVariables().getLong(PlayerVariables.HOMUNCULUS_CREATION_TIME, 0);
		final long waitTime = 0; // 86400 = 24 Hours
		if (creationTime == 0)
		{
			player.getInventory().addItem(ItemProcessType.REFUND, item.getId(), 1, player, player);
			player.sendMessage("You don't have any Homunculus in progress.");
		}
		else if (((currentTime / 1000) - (creationTime / 1000)) >= waitTime)
		{
			player.getInventory().addItem(ItemProcessType.REFUND, item.getId(), 1, player, player);
			player.sendMessage("You cannot decrease the waiting time anymore.");
		}
		else if (((currentTime / 1000) - (creationTime / 1000)) < waitTime)
		{
			player.getVariables().set(PlayerVariables.HOMUNCULUS_CREATION_TIME, creationTime - (_time));
			player.sendPacket(new ExShowHomunculusBirthInfo(player));
		}
		else
		{
			player.sendMessage("You cannot use this item yet.");
		}
	}
}
