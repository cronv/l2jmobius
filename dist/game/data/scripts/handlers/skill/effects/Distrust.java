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

import org.l2jmobius.gameserver.ai.AttackableAI;
import org.l2jmobius.gameserver.ai.DistrustAI;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.instance.Chest;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code Distrust} skill effect.<br>
 * This class manages how entities are affected when they enter a state of distrust.<br>
 * It interacts with {@code AttackableAI} to modify behavior during the effect duration.
 * @author Naker
 */
public class Distrust extends AbstractEffect
{
	/**
	 * Creates a new instance of the {@code Distrust} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the parameters for this effect.
	 */
	public Distrust(StatSet params)
	{
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It checks if the target is a valid monster to receive the effect.<br>
	 * If valid, it finds a nearby monster and changes the target's AI.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply this effect.
	 * @param item The item used to apply this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || !effected.isMonster() || (effected instanceof Chest) || effected.isRaid() || effected.isRaidMinion())
		{
			effector.sendPacket(SystemMessageId.INVALID_TARGET);
			return;
		}
		
		final Monster targetMonster = effected.asMonster();
		final Monster newTarget = World.getRandomVisibleObjectInRange(targetMonster, Monster.class, 1100, m -> (m != targetMonster) && !m.isDead() && m.isAttackable() && !(m instanceof Chest) && !m.isRaid() && !m.isRaidMinion());
		if (newTarget == null)
		{
			return;
		}
		
		targetMonster.setAI(new DistrustAI(effected.asAttackable(), newTarget));
		effector.sendPacket(SystemMessageId.YOU_HAVE_SUCCEEDED_IN_CONFUSING_THE_ENEMY);
	}
	
	/**
	 * Handles the logic when a {@code Distrust} effect ends.<br>
	 * This method resets the behavior of the affected monster.<br>
	 * It restores the standard AI and clears current targets.
	 * @param effector The creature that applied the effect.
	 * @param effected The creature that was affected by the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Attackable monster = effected.asAttackable();
		if ((monster == null) || monster.isDead())
		{
			return;
		}
		
		monster.setAI(new AttackableAI(monster));
		monster.setTarget(null);
		monster.setWalking();
		monster.getAI().setIntentionActive();
	}
}
