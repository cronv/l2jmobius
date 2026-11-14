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
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonAgathion;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoCubic;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code SummonAgathion} skill effect.<br>
 * This class manages the spawning and behavior of the Agathion entity.
 * @author Zoey76, Galagard
 */
public class SummonAgathion extends AbstractEffect
{
	private final int _npcId;
	
	/**
	 * Creates a new instance of the {@code SummonAgathion} effect.<br>
	 * This constructor initializes the required NPC data from the provided stats.<br>
	 * It throws an exception if the {@code params} object is empty.
	 * @param params The {@code StatSet} containing the necessary parameters like {@code npcId}.
	 */
	public SummonAgathion(StatSet params)
	{
		if (params.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": must have parameters.");
		}
		
		_npcId = params.getInt("npcId", 0);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for summoning an Agathion.<br>
	 * This method updates the player's Agathion ID and sends the necessary network packets.<br>
	 * It also triggers the {@code ON_PLAYER_SUMMON_AGATHION} event if a listener exists.
	 * @param effector The creature who performed the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final int index = item != null ? item.getLocationSlot() - Inventory.PAPERDOLL_AGATHION1 : 0;
		player.setAgathionId(index, _npcId);
		player.sendPacket(new ExUserInfoCubic(player));
		player.broadcastCharInfo();
		
		if (EventDispatcher.getInstance().hasListener(EventType.ON_PLAYER_SUMMON_AGATHION))
		{
			EventDispatcher.getInstance().notifyEventAsync(new OnPlayerSummonAgathion(player, _npcId));
		}
	}
}
