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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.TemplateChanceHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles transformation effects that modify the character's body.<br>
 * This effect disables the ability to perform attacks or use skills while active.
 * @author Nik, Mobius
 */
public class ChangeBody extends AbstractEffect
{
	private final Set<TemplateChanceHolder> _transformations = new HashSet<>();
	
	/**
	 * Creates a new {@code ChangeBody} effect.<br>
	 * This constructor initializes the transformation list from the provided parameters.
	 * @param params The {@code StatSet} containing the template data.
	 */
	public ChangeBody(StatSet params)
	{
		for (StatSet item : params.getList("templates", StatSet.class))
		{
			_transformations.add(new TemplateChanceHolder(item.getInt(".templateId"), item.getInt(".minChance"), item.getInt(".maxChance")));
		}
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code false} if the target is a door.<br>
	 * Otherwise, it returns {@code true}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect can start, {@code false} otherwise.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return !effected.isDoor();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It determines if a transformation should occur based on random chance.<br>
	 * If successful, it applies the transformation to the {@code effected} creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		final int chance = Rnd.get(100);
		for (TemplateChanceHolder holder : _transformations)
		{
			if (holder.calcChance(chance))
			{
				effected.transform(holder.getTemplateId(), false);
				return;
			}
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It stops the transformation on the affected creature.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} who was affected by the skill.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.stopTransformation(false);
	}
}
