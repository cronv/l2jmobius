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

import java.util.Set;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Aggression effect making the effected attackable target the effector and call its clan for help.
 * @author Adry_85, Mobius
 */
public class GetAgro extends AbstractEffect
{
	/**
	 * Creates a {@code GetAgro} effect from the given parameters.
	 * @param params The {@code StatSet} of skill parameters.
	 */
	public GetAgro(StatSet params)
	{
	}
	
	/**
	 * Retrieves this effect type.
	 * @return {@code EffectType.AGGRESSION}.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.AGGRESSION;
	}
	
	/**
	 * Indicates whether this effect applies instantly.
	 * @return {@code true} always.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Makes the effected attackable target the effector and calls nearby clan members to assist.
	 * @param effector The {@code Creature} applying the effect.
	 * @param effected The {@code Creature} the effect is applied to.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((effected != null) && effected.isAttackable())
		{
			effected.getAI().setIntentionAttack(effector);
			
			// Monsters from the same clan should assist.
			final NpcTemplate template = effected.asAttackable().getTemplate();
			final Set<Integer> clans = template.getClans();
			if (clans != null)
			{
				World.forEachVisibleObjectInRange(effected, Attackable.class, template.getClanHelpRange(), nearby ->
				{
					if (!nearby.isMovementDisabled() && nearby.getTemplate().isClan(clans))
					{
						nearby.addDamageHate(effector, 1, 200);
						nearby.getAI().setIntentionAttack(effector);
						nearby.setRunning();
					}
				});
			}
		}
	}
}
