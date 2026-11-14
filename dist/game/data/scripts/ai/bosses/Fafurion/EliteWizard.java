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
 * This class defines the artificial intelligence behavior for the {@code EliteWizard} boss.<br>
 * It handles specific combat logic and skill usage patterns for this entity.
 * @author NviX
 */
public class EliteWizard extends Script
{
	// Npc
	private static final int ELITE_WIZARD = 29373;
	
	// Skills
	private static final SkillHolder WATER_ARROW = new SkillHolder(32740, 1);
	private static final SkillHolder ARROW_FLUSH = new SkillHolder(32741, 1);
	
	/**
	 * Private constructor for the {@code EliteWizard} class.<br>
	 * This constructor initializes the required attack and spell IDs.<br>
	 * It registers {@code ELITE_WIZARD} to the script's event handlers.
	 */
	private EliteWizard()
	{
		addAttackId(ELITE_WIZARD);
		addSpellFinishedId(ELITE_WIZARD);
	}
	
	/**
	 * Handles specific events for the {@code EliteWizard} script.<br>
	 * It checks if the {@code event} is {@code "MANAGE_SKILLS"}.<br>
	 * If so, it calls the {@code manageSkills} method on the {@code npc}.<br>
	 * Finally, it returns the result of the superclass method.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the superclass {@code onEvent} method.
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
	 * This method updates the AI parameters based on current health and damage.<br>
	 * It also triggers skill management for the {@code EliteWizard} entity.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt in the attack.
	 * @param isSummon Indicates if the attack comes from a summon.
	 * @param skill The {@code Skill} used for the attack, or {@code null}.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		if (npc.getId() == ELITE_WIZARD)
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
	 * Updates the AI parameters for an {@code Npc}.<br>
	 * This method adjusts behavior based on the current attacker and damage dealt.
	 * @param attacker The {@code Creature} performing the action.
	 * @param npc The {@code Npc} whose AI is being updated.
	 * @param damage The amount of damage dealt in the last hit.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage)
	{
		refreshAiParams(attacker, npc, damage, damage);
	}
	
	/**
	 * Updates the AI parameters for an {@code Npc} based on combat data.<br>
	 * This method recalculates aggro values and tracks the primary target.
	 * @param attacker The {@code Creature} that dealt damage.
	 * @param npc The {@code Npc} whose AI is being updated.
	 * @param damage The amount of damage dealt by the attacker.
	 * @param aggro The current aggro value of the target.
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
	 * This method is called when a spell cast by an {@code Npc} finishes.<br>
	 * It handles the logic for what happens after the skill ends.<br>
	 * It triggers a timer to manage the next set of skills.
	 * @param npc The {@code Npc} that cast the spell.
	 * @param player The {@code Player} who was targeted by the spell.
	 * @param skill The {@code Skill} that was just completed.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		startQuestTimer("MANAGE_SKILLS", 1000, npc, null);
	}
	
	/**
	 * Handles the logic for choosing and casting skills for an {@code Npc}.<br>
	 * It checks if the {@code Npc} is eligible to cast a skill.<br>
	 * The method updates quest variables based on nearby targets.<br>
	 * It selects between {@code ARROW_FLUSH} and {@code WATER_ARROW} to use.
	 * @param npc The {@code Npc} instance that will perform the actions.
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
			if (getRandom(100) < 30)
			{
				skillToCast = ARROW_FLUSH;
			}
			else
			{
				skillToCast = WATER_ARROW;
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	/**
	 * This is the entry point for the {@code EliteWizard} class.<br>
	 * It initializes a new instance of the {@code EliteWizard}.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new EliteWizard();
	}
}
