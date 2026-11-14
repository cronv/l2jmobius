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
package ai.others;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Monsters with the retail SelfExplosion parameter blow themselves up when attacked.<br>
 * Each hit has a 5% chance to try, which then succeeds by 10% minus a tenth of the HP percentage, so weaker monsters explode more often.
 * @author Mobius
 */
public class SelfExplosionMonsters extends Script
{
	private static final int[] NPCS =
	{
		21392, // Blazing Ifrit
	};
	
	private SelfExplosionMonsters()
	{
		addAttackId(NPCS);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		if (getRandom(100) >= 5)
		{
			return;
		}
		
		final int hpPercent = (int) ((npc.getCurrentHp() / npc.getMaxHp()) * 100);
		if ((10 - (hpPercent / 10)) <= getRandom(100))
		{
			return;
		}
		
		final SkillHolder explosion = npc.getParameters().getSkillHolder("SelfExplosion");
		if (explosion == null)
		{
			return;
		}
		
		final Skill skill = explosion.getSkill();
		if ((npc.getCurrentMp() > skill.getMpConsume()) && (npc.getCurrentHp() > skill.getHpConsume()) && !npc.isSkillDisabled(skill))
		{
			npc.setTarget(npc);
			npc.doCast(skill);
		}
	}
	
	public static void main(String[] args)
	{
		new SelfExplosionMonsters();
	}
}
