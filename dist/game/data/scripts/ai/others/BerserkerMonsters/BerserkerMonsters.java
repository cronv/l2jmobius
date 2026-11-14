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
package ai.others.BerserkerMonsters;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Berserker monsters roar with Increase Rage Might when a monster of their clan dies nearby.<br>
 * Each roar raises the skill level by one, up to the roar count of the monster.
 * @author Mobius
 */
public class BerserkerMonsters extends Script
{
	// Skill
	private static final int RAGE_MIGHT = 4317;
	
	// Misc
	private static final int CLAN_HELP_RANGE = 300;
	
	// Monster id and how many times it roars.
	private static final Map<Integer, Integer> BERSERKERS = new HashMap<>();
	static
	{
		BERSERKERS.put(20086, 3); // Ant Guard
		BERSERKERS.put(20248, 2); // Turak Bugbear
		BERSERKERS.put(21269, 3); // Cave Ant
		BERSERKERS.put(21271, 3); // Cave Ant
	}
	
	private BerserkerMonsters()
	{
		// Any death inside the clans of the berserkers can make them roar.
		final Set<Integer> clans = new HashSet<>();
		for (int npcId : BERSERKERS.keySet())
		{
			final NpcTemplate template = NpcData.getInstance().getTemplate(npcId);
			final Set<Integer> berserkerClans = template == null ? null : template.getClans();
			if (berserkerClans != null)
			{
				clans.addAll(berserkerClans);
			}
		}
		
		for (NpcTemplate template : NpcData.getInstance().getTemplates(clanTemplate -> clanTemplate.isClan(clans)))
		{
			addKillId(template.getId());
		}
	}
	
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		for (Attackable berserker : World.getVisibleObjectsInRange(npc, Attackable.class, CLAN_HELP_RANGE))
		{
			final Integer maxRoarCount = BERSERKERS.get(berserker.getId());
			if ((maxRoarCount == null) || berserker.isDead() || !berserker.isInMyClan(npc))
			{
				continue;
			}
			
			// The script value counts the roars since spawn, the skill level follows it.
			final int roarCount = berserker.getScriptValue();
			if (roarCount >= maxRoarCount)
			{
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(RAGE_MIGHT, roarCount + 1);
			if ((skill != null) && (berserker.getCurrentMp() > skill.getMpConsume()) && (berserker.getCurrentHp() > skill.getHpConsume()) && !berserker.isSkillDisabled(skill))
			{
				berserker.setTarget(berserker);
				berserker.doCast(skill);
			}
			
			berserker.setScriptValue(roarCount + 1);
		}
	}
	
	public static void main(String[] args)
	{
		new BerserkerMonsters();
	}
}
