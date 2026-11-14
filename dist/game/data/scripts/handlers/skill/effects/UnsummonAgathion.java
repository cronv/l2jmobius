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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerUnsummonAgathion;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoCubic;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code UnsummonAgathion} skill effect.<br>
 * This class manages the removal of an Agathion when it is unsummoned.
 * @author Zoey76, Galagard
 */
public class UnsummonAgathion extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code UnsummonAgathion} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the necessary parameters for the effect.
	 */
	public UnsummonAgathion(StatSet params)
	{
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method returns {@code true} for this specific effect.
	 * @return {@code true} if the effect is instant, otherwise {@code false}.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of unsummoning an Agathion.<br>
	 * This method removes the active Agathion from a {@code Player}.<br>
	 * It updates the player's state and notifies all nearby clients.
	 * @param effector The {@code Creature} who performs the action.
	 * @param effected The {@code Creature} who is affected by the action.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effector.asPlayer();
		if (player != null)
		{
			final int index = player.getLastAgathionSlot();
			final int agathionId = player.getAgathionId(index);
			if (agathionId > 0)
			{
				player.setAgathionId(index, 0);
				player.sendPacket(new ExUserInfoCubic(player));
				player.broadcastCharInfo();
				
				if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_UNSUMMON_AGATHION))
				{
					EventDispatcher.getInstance().notifyEventAsync(new OnPlayerUnsummonAgathion(player, agathionId));
				}
			}
		}
	}
}
