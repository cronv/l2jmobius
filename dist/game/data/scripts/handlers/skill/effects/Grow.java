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
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code Grow} effect implementation.<br>
 * It manages the logic for increasing the size of a target entity.
 */
public class Grow extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Grow} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the growth values.
	 */
	public Grow(StatSet params)
	{
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It updates the physical size of an {@code Npc}.<br>
	 * The new dimensions are taken from the growth template.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply this effect.
	 * @param item The item used to apply this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isNpc())
		{
			final Npc npc = effected.asNpc();
			npc.setCollisionHeight(npc.getTemplate().getCollisionHeightGrown());
			npc.setCollisionRadius(npc.getTemplate().getCollisionRadiusGrown());
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It resets the collision properties of an {@code Npc}.<br>
	 * The values are restored to their original template defaults.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isNpc())
		{
			final Npc npc = effected.asNpc();
			npc.setCollisionHeight(npc.getTemplate().getCollisionHeight());
			npc.setCollisionRadius(npc.getTemplate().getFCollisionRadius());
		}
	}
}
