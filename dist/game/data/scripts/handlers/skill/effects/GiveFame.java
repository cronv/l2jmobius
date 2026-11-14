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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for granting fame to a {@code Creature}.<br>
 * It is triggered as an effect of a {@code Skill} execution.
 * @author Mobius
 */
public class GiveFame extends AbstractEffect
{
	private final int _fame;
	
	/**
	 * Creates a new {@code GiveFame} effect.<br>
	 * This constructor initializes the fame value from the provided stats.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public GiveFame(StatSet params)
	{
		_fame = params.getInt("fame", 0);
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
	 * Applies the effect immediately to a target.<br>
	 * This method checks if both creatures are players and the target is alive.<br>
	 * It then increases the fame of the targeted player.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		player.setFame(player.getFame() + _fame);
	}
}
