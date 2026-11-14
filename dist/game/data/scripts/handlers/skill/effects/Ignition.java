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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the {@code Ignition} status effect applied to a target.<br>
 * This class manages the periodic damage and visual behavior associated with being ignited.
 * @author Mobius
 */
public class Ignition extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Ignition} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public Ignition(StatSet params)
	{
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the specific flag associated with {@code Ignition}.
	 * @return The {@code long} value representing the effect flags.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.IGNITION.getMask();
	}
	
	/**
	 * Handles the logic when this effect begins.<br>
	 * It schedules a packet to be sent to the {@code effector}.<br>
	 * This happens after a short delay of {@code 100} milliseconds.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		ThreadPool.schedule(() ->
		{
			effector.sendPacket(new ExUserInfoEquipSlot(effector.asPlayer()));
			effector.broadcastInfo();
		}, 100);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It schedules a task to update the {@code effector}.<br>
	 * The task sends an equipment slot packet and broadcasts information after 100 milliseconds.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		ThreadPool.schedule(() ->
		{
			effector.sendPacket(new ExUserInfoEquipSlot(effector.asPlayer()));
			effector.broadcastInfo();
		}, 100);
	}
}
