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
package ai.areas.CrumaTower;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.variables.NpcVariables;

/**
 * Porta and Perum summons a distant attacker to its location.<br>
 * An attacker further than 300 is summoned with 50% chance; one further than 100 with 50% chance when it is the most hated, 10% otherwise.
 * @author Zoey76, Mobius
 */
public class SummonPc extends Script
{
	// NPCs
	private static final int PORTA = 20213;
	private static final int PERUM = 20221;
	
	// Skill
	private static final SkillHolder SUMMON_PC = new SkillHolder(4161, 1);
	
	private SummonPc()
	{
		addAttackId(PORTA, PERUM);
		addSpellFinishedId(PORTA, PERUM);
	}
	
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon)
	{
		final NpcVariables variables = npc.getVariables();
		if (variables.getBoolean("attacked", false))
		{
			return;
		}
		
		final Skill summonPc = SUMMON_PC.getSkill();
		if ((summonPc.getMpConsume() >= npc.getCurrentMp()) || (summonPc.getHpConsume() >= npc.getCurrentHp()) || npc.isSkillDisabled(summonPc))
		{
			return;
		}
		
		final int chance = getRandom(100);
		final double distance = npc.calculateDistance3D(attacker);
		final boolean summon;
		if (distance > 300)
		{
			summon = chance < 50;
		}
		else if (distance > 100)
		{
			final Creature mostHated = npc.asAttackable().getMostHated();
			summon = (mostHated != null) && (((mostHated == attacker) && (chance < 50)) || (chance < 10));
		}
		else
		{
			summon = false;
		}
		
		if (summon)
		{
			npc.setTarget(attacker);
			npc.doCast(summonPc);
			variables.set("attacked", true);
		}
	}
	
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		final NpcVariables variables = npc.getVariables();
		if ((skill.getId() == SUMMON_PC.getSkillId()) && !npc.isDead() && variables.getBoolean("attacked", false))
		{
			player.teleToLocation(npc);
			variables.set("attacked", false);
		}
	}
	
	public static void main(String[] args)
	{
		new SummonPc();
	}
}
