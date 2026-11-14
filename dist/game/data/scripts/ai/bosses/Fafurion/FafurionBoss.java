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
package ai.bosses.Fafurion;

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
 * This class handles the artificial intelligence and behavior for the {@code Fafurion} boss.<br>
 * It manages combat logic, skill usage, and state transitions for this specific NPC.
 * @author NviX
 */
public class FafurionBoss extends Script
{
	// NPCs
	private static final int FAFURION_STAGE_1 = 29361;
	private static final int FAFURION_STAGE_2 = 29362;
	private static final int FAFURION_STAGE_3 = 29363;
	private static final int FAFURION_STAGE_4 = 29364;
	private static final int FAFURION_STAGE_5 = 29365;
	private static final int FAFURION_STAGE_6 = 29366;
	private static final int FAFURION_STAGE_7 = 29367;
	
	// Skills
	private static final SkillHolder FAFURION_NORMAL_ATTACK = new SkillHolder(32705, 1);
	private static final SkillHolder FAFURION_BITE = new SkillHolder(32706, 1);
	private static final SkillHolder FAFURION_WATER_BLAST = new SkillHolder(32708, 1);
	private static final SkillHolder FAFURION_WATER_SPIRAL = new SkillHolder(32709, 1);
	private static final SkillHolder FAFURION_HYDRO_BLAST = new SkillHolder(32711, 1);
	private static final SkillHolder FAFURION_HYDRO_SPIRAL = new SkillHolder(32712, 1);
	private static final SkillHolder FAFURION_TAIL_STRIKE_1 = new SkillHolder(32713, 1);
	private static final SkillHolder FAFURION_TAIL_STRIKE_2 = new SkillHolder(32714, 1);
	private static final SkillHolder FAFURION_WATER_WAVE = new SkillHolder(32715, 1);
	private static final SkillHolder FAFURION_BREATH = new SkillHolder(32716, 1);
	private static final SkillHolder FAFURION_FEAR = new SkillHolder(32717, 1);
	private static final SkillHolder FAFURION_TIDAL_WAVE = new SkillHolder(32723, 1);
	
	/**
	 * Private constructor for the {@code FafurionBoss} class.<br>
	 * It initializes the attack and spell IDs for all boss stages.
	 */
	private FafurionBoss()
	{
		addAttackId(FAFURION_STAGE_1, FAFURION_STAGE_2, FAFURION_STAGE_3, FAFURION_STAGE_4, FAFURION_STAGE_5, FAFURION_STAGE_6, FAFURION_STAGE_7);
		addSpellFinishedId(FAFURION_STAGE_1, FAFURION_STAGE_2, FAFURION_STAGE_3, FAFURION_STAGE_4, FAFURION_STAGE_5, FAFURION_STAGE_6, FAFURION_STAGE_7);
	}
	
	/**
	 * Handles specific events for the boss script.<br>
	 * It checks if the {@code event} is {@code "MANAGE_SKILLS"} and calls the corresponding method.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
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
	 * Handles the logic when an NPC is attacked by a player.<br>
	 * This method updates AI parameters based on current health and damage.<br>
	 * It also triggers skill management for specific boss stages.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of {@code int} damage dealt.
	 * @param isSummon Whether the attack comes from a summon.
	 * @param skill The {@code Skill} used for the attack, or {@code null}.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
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
	
	/**
	 * Updates the AI parameters for a boss based on an attack.<br>
	 * This method recalculates internal values using the attacker and damage amount.
	 * @param attacker The {@code Creature} performing the action.
	 * @param npc The {@code Npc} being targeted or acting.
	 * @param damage The amount of {@code int} damage dealt.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage)
	{
		refreshAiParams(attacker, npc, damage, damage);
	}
	
	/**
	 * Updates the AI parameters for an {@code Npc} based on combat actions.<br>
	 * This method recalculates aggro values and assigns the primary target.
	 * @param attacker The {@code Creature} performing the action.
	 * @param npc The {@code Npc} whose AI is being updated.
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
	 * This method is called when a spell cast by an {@code Npc} on a {@code Player} has finished.<br>
	 * It starts a timer to manage the boss skills.
	 * @param npc The {@code Npc} that performed the action.
	 * @param player The {@code Player} who was targeted by the spell.
	 * @param skill The {@code Skill} that was just completed.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		startQuestTimer("MANAGE_SKILLS", 1000, npc, null);
	}
	
	/**
	 * This method handles the logic for choosing and casting skills for an {@code Npc}.<br>
	 * It checks if the {@code Npc} is currently able to perform actions.<br>
	 * The method selects a skill based on the current health of the {@code Npc} and random chance.<br>
	 * If a valid skill is chosen, it sets the target and starts casting.
	 * @param npc The {@code Npc} instance whose skills are being managed.
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
		if ((player != null) && !player.isDead())
		{
			int chance = getRandom(100);
			final long maxHp = npc.getMaxHp();
			if (npc.getCurrentHp() > (maxHp * 0.8))
			{
				if (chance < 30)
				{
					skillToCast = FAFURION_WATER_BLAST;
				}
				else if (chance < 40)
				{
					skillToCast = FAFURION_FEAR;
				}
				else if (chance < 60)
				{
					skillToCast = FAFURION_BREATH;
				}
				else
				{
					skillToCast = FAFURION_NORMAL_ATTACK;
				}
			}
			else if (npc.getCurrentHp() > (maxHp * 0.6))
			{
				if (chance < 15)
				{
					skillToCast = FAFURION_WATER_BLAST;
				}
				else if (chance < 10)
				{
					skillToCast = FAFURION_FEAR;
				}
				else if (chance < 35)
				{
					skillToCast = FAFURION_BREATH;
				}
				else if (chance < 50)
				{
					skillToCast = FAFURION_TAIL_STRIKE_1;
				}
				else if (chance < 65)
				{
					skillToCast = FAFURION_TAIL_STRIKE_2;
				}
				else
				{
					skillToCast = FAFURION_NORMAL_ATTACK;
				}
			}
			else if (npc.getCurrentHp() > (maxHp * 0.3))
			{
				if (chance < 5)
				{
					skillToCast = FAFURION_WATER_BLAST;
				}
				else if (chance < 10)
				{
					skillToCast = FAFURION_FEAR;
				}
				else if (chance < 15)
				{
					skillToCast = FAFURION_BREATH;
				}
				else if (chance < 20)
				{
					skillToCast = FAFURION_TAIL_STRIKE_1;
				}
				else if (chance < 30)
				{
					skillToCast = FAFURION_TAIL_STRIKE_2;
				}
				else if (chance < 40)
				{
					skillToCast = FAFURION_BITE;
				}
				else if (chance < 50)
				{
					skillToCast = FAFURION_WATER_SPIRAL;
				}
				else if (chance < 60)
				{
					skillToCast = FAFURION_HYDRO_BLAST;
				}
				else if (chance < 70)
				{
					skillToCast = FAFURION_HYDRO_SPIRAL;
				}
				else if (chance < 80)
				{
					skillToCast = FAFURION_WATER_WAVE;
				}
				else if (chance < 90)
				{
					skillToCast = FAFURION_TIDAL_WAVE;
				}
				else
				{
					skillToCast = FAFURION_NORMAL_ATTACK;
				}
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	/**
	 * This is the entry point for the {@code FafurionBoss} class.<br>
	 * It initializes a new instance of the boss script.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new FafurionBoss();
	}
}
