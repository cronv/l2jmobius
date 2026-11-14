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
package ai.bosses.Lilith;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.mechanics.variables.NpcVariables;
import org.l2jmobius.gameserver.util.MathUtil;

/**
 * This class handles the artificial intelligence and behavior for the {@code Lilith} boss.<br>
 * It manages complex combat patterns, skill usage, and state transitions for this specific NPC.
 * @author NviX
 * @URL https://web.archive.org/web/20170314173542/http://boards.lineage2.com/showpost.php?p=3386784&postcount=6 <br>
 * @video https://www.youtube.com/watch?v=H3MuIwUjjD4
 */
public class LilithBoss extends Script
{
	// Npc
	private static final int LILITH = 29336;
	
	// Skills
	private static final SkillHolder MAGIC_BOLT = new SkillHolder(32536, 1);
	private static final SkillHolder MAGIC_BLAST = new SkillHolder(32537, 1);
	private static final SkillHolder UNHOLY_VENGEANCE = new SkillHolder(32538, 1);
	private static final SkillHolder UNHOLY_DIMENSION = new SkillHolder(32539, 1);
	private static final SkillHolder UNHOLY_SHIELD = new SkillHolder(32540, 1);
	
	// Others
	private boolean _hp75 = false;
	private boolean _hp50 = false;
	private boolean _hp25 = false;
	
	/**
	 * Initializes the {@code LilithBoss} script.<br>
	 * Registers the required attack and spell finished IDs.
	 */
	private LilithBoss()
	{
		addAttackId(LILITH);
		addSpellFinishedId(LILITH);
	}
	
	/**
	 * Handles specific events for the boss logic.<br>
	 * It checks if the {@code event} is {@code "MANAGE_SKILLS"} and calls the corresponding method.<br>
	 * Finally, it returns the result of the superclass method.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the superclass {@code onEvent} call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "MANAGE_SKILLS":
			{
				if (npc != null)
				{
					manageSkills(npc);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} is attacked by a {@code Player}.<br>
	 * This method updates AI parameters based on current health and damage.<br>
	 * It also triggers skill management for the boss.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attack comes from a summon.
	 * @param skill The {@code Skill} used for the attack, or {@code null}.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == LILITH)
		{
			if (skill == null)
			{
				refreshAiParams(attacker, npc, (damage * 1000));
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.25))
			{
				refreshAiParams(attacker, npc, ((damage / 3) * 100));
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.5))
			{
				refreshAiParams(attacker, npc, (damage * 20));
			}
			else if (npc.getCurrentHp() < (npc.getMaxHp() * 0.75))
			{
				refreshAiParams(attacker, npc, (damage * 10));
			}
			else
			{
				refreshAiParams(attacker, npc, ((damage / 3) * 20));
			}
			
			manageSkills(npc);
		}
	}
	
	/**
	 * Updates the AI parameters for a boss based on an attack.<br>
	 * This method recalculates internal states using the attacker and damage values.
	 * @param attacker The {@code Creature} that performed the action.
	 * @param npc The {@code Npc} instance being updated.
	 * @param damage The amount of {@code int} damage dealt.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage)
	{
		refreshAiParams(attacker, npc, damage, damage);
	}
	
	/**
	 * Updates the AI parameters for the boss based on combat actions.<br>
	 * This method recalculates aggro values and tracks the primary target.
	 * @param attacker The {@code Creature} that performed the action.
	 * @param npc The {@code Npc} instance of the boss.
	 * @param damage The amount of damage dealt in the current action.
	 * @param aggro The current base aggro value for the target.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage, int aggro)
	{
		final int newAggroVal = damage + getRandom(3000);
		final int aggroVal = aggro + 1000;
		final NpcVariables vars = npc.getVariables();
		for (int i = 0; i < 3; i++)
		{
			if (attacker == vars.getObject("c_quest" + i, Creature.class))
			{
				if (vars.getInt("i_quest" + i) < aggroVal)
				{
					vars.set("i_quest" + i, newAggroVal);
				}
				return;
			}
		}
		
		final int index = MathUtil.getIndexOfMinValue(vars.getInt("i_quest0"), vars.getInt("i_quest1"), vars.getInt("i_quest2"));
		vars.set("i_quest" + index, newAggroVal);
		vars.set("c_quest" + index, attacker);
	}
	
	/**
	 * This method is called when a spell cast by an {@code Npc} has finished.<br>
	 * It triggers the logic to manage skills for the boss.
	 * @param npc The {@code Npc} that cast the skill.
	 * @param player The {@code Player} who was targeted or involved in the action.
	 * @param skill The {@code Skill} object representing the finished spell.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		startQuestTimer("MANAGE_SKILLS", 1000, npc, null);
	}
	
	/**
	 * Handles the logic for selecting and casting skills for the NPC.<br>
	 * It checks health thresholds to trigger defensive abilities.<br>
	 * It also selects offensive skills based on random chances.
	 * @param npc The {@code Npc} instance to manage.
	 */
	private void manageSkills(Npc npc)
	{
		if (npc.isCastingNow(SkillCaster::isAnyNormalType) || npc.isCoreAIDisabled() || !npc.isInCombat())
		{
			return;
		}
		
		final NpcVariables vars = npc.getVariables();
		for (int i = 0; i < 3; i++)
		{
			final Creature attacker = vars.getObject("c_quest" + i, Creature.class);
			if ((attacker == null) || ((npc.calculateDistance3D(attacker) > 9000) || attacker.isDead()))
			{
				vars.set("i_quest" + i, 0);
			}
		}
		
		final int index = MathUtil.getIndexOfMaxValue(vars.getInt("i_quest0"), vars.getInt("i_quest1"), vars.getInt("i_quest2"));
		final Creature player = vars.getObject("c_quest" + index, Creature.class);
		final int i2 = vars.getInt("i_quest" + index);
		if ((i2 > 0) && (getRandom(100) < 70))
		{
			vars.set("i_quest" + index, 500);
		}
		
		SkillHolder skillToCast = null;
		int chance = getRandom(100);
		if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.75)) && !_hp75)
		{
			_hp75 = true;
			npc.abortCast();
			npc.abortAttack();
			npc.setTarget(npc);
			npc.doCast(UNHOLY_SHIELD.getSkill());
			return;
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.50)) && !_hp50)
		{
			_hp50 = true;
			npc.abortCast();
			npc.abortAttack();
			npc.setTarget(npc);
			npc.doCast(UNHOLY_SHIELD.getSkill());
			return;
		}
		else if ((npc.getCurrentHp() <= (npc.getMaxHp() * 0.25)) && !_hp25)
		{
			_hp25 = true;
			npc.abortCast();
			npc.abortAttack();
			npc.setTarget(npc);
			npc.doCast(UNHOLY_SHIELD.getSkill());
			return;
		}
		
		if ((player != null) && !player.isDead())
		{
			if (chance < 15)
			{
				skillToCast = UNHOLY_DIMENSION;
			}
			else if (chance < 30)
			{
				skillToCast = UNHOLY_VENGEANCE;
			}
			else if (chance < 50)
			{
				skillToCast = MAGIC_BLAST;
			}
			else
			{
				skillToCast = MAGIC_BOLT;
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	/**
	 * This is the entry point for the {@code LilithBoss} class.<br>
	 * It initializes a new instance of the boss.
	 * @param args The command line arguments.
	 */
	public static void main(String[] args)
	{
		new LilithBoss();
	}
}
