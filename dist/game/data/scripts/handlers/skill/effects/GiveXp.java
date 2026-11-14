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
 * This class handles the logic for granting experience points to a target.<br>
 * It is an implementation of {@code AbstractEffect} used within skill mechanics.
 * @author Mobius
 */
public class GiveXp extends AbstractEffect
{
	private final long _xp;
	private final int _level;
	private final double _percentage;
	
	/**
	 * Creates a new instance of the {@code GiveXp} effect.<br>
	 * This method initializes the experience points, level, and percentage values.<br>
	 * It extracts these values from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public GiveXp(StatSet params)
	{
		_xp = params.getLong("xp", 0);
		_level = params.getInt("level", 0);
		_percentage = params.getDouble("percentage", 0);
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
	 * Applies the experience point gain to a target creature.<br>
	 * This method checks if both creatures are players and not dead.<br>
	 * It calculates the amount based on the player's level.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the experience.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer() || !effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Player player = effector.asPlayer();
		final double amount;
		if (player.getLevel() < _level)
		{
			amount = (_xp / 100.0) * _percentage;
		}
		else
		{
			amount = _xp;
		}
		
		player.addExpAndSp(amount, 0);
	}
}
