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
package ai.bosses.EtisVanEtina;

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
 * This class defines the artificial intelligence behavior for the boss {@code Etina}.<br>
 * It handles the logic for its unique combat patterns and actions as a {@code Script}.
 * @author NviX
 */
public class Etina extends Script
{
	// Npc
	private static final int ETINA1 = 29318;
	private static final int ETINA2 = 29319;
	
	// Skills
	private static final SkillHolder APOCALYPSE_FIRE = new SkillHolder(32001, 1);
	private static final SkillHolder DEMOLISH_GROUND = new SkillHolder(32002, 1);
	private static final SkillHolder WALL_OF_VOID = new SkillHolder(32003, 1);
	private static final SkillHolder FLASH_OF_DARKNESS = new SkillHolder(32007, 1);
	
	/**
	 * Private constructor for the {@code Etina} class.<br>
	 * This constructor initializes the attack and spell IDs.<br>
	 * It registers {@code ETINA1} and {@code ETINA2} to the script.
	 */
	private Etina()
	{
		addAttackId(ETINA1, ETINA2);
		addSpellFinishedId(ETINA1, ETINA2);
	}
	
	/**
	 * Handles specific events for the {@code Etina} boss.<br>
	 * It checks if the {@code event} is {@code "MANAGE_SKILLS"}.<br>
	 * If so, it calls the {@code manageSkills} method on the provided {@code Npc}.<br>
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
	 * This method updates the AI parameters based on the current health of the boss.<br>
	 * It also triggers skill management for specific boss IDs.
	 * @param npc The {@code Npc} being attacked.
	 * @param attacker The {@code Player} performing the attack.
	 * @param damage The amount of damage dealt in the attack.
	 * @param isSummon Indicates if the attack comes from a summon.
	 * @param skill The {@code Skill} used for the attack, or {@code null}.
	 */
	@Override
	public void onAttack(Npc npc, Player attacker, int damage, boolean isSummon, Skill skill)
	{
		final int npcId = npc.getId();
		if ((npcId == ETINA1) || (npcId == ETINA2))
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
	 * This method is called when a {@code Creature} deals damage.<br>
	 * It ensures the boss reacts correctly to the current combat state.
	 * @param attacker The {@code Creature} that performed the action.
	 * @param npc The {@code Npc} whose AI parameters are being updated.
	 * @param damage The amount of damage dealt during the attack.
	 */
	private void refreshAiParams(Creature attacker, Npc npc, int damage)
	{
		refreshAiParams(attacker, npc, damage, damage);
	}
	
	/**
	 * Updates the AI parameters for the {@code Npc} based on an attack.<br>
	 * This method recalculates aggro values and tracks the primary target.
	 * @param attacker The {@code Creature} that performed the action.
	 * @param npc The {@code Npc} whose AI is being updated.
	 * @param damage The amount of damage dealt by the attack.
	 * @param aggro The current aggro value of the {@code Npc}.
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
	 * It handles the logic that occurs immediately after the skill ends.
	 * @param npc The {@code Npc} that performed the action.
	 * @param player The {@code Player} who was targeted or involved.
	 * @param skill The {@code Skill} object representing the finished spell.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		startQuestTimer("MANAGE_SKILLS", 1000, npc, null);
	}
	
	/**
	 * Handles the logic for choosing and casting skills for an {@code Npc}.<br>
	 * It checks if the {@code Npc} is eligible to cast a skill based on combat status.<br>
	 * The method selects a target from quest variables and picks a random skill to use.<br>
	 * If all conditions are met, it sets the target and performs the cast.
	 * @param npc The {@code Npc} instance that will manage its skills.
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
			if (getRandom(100) < 10)
			{
				skillToCast = WALL_OF_VOID;
			}
			else if (getRandom(100) < 30)
			{
				skillToCast = DEMOLISH_GROUND;
			}
			else if (getRandom(100) < 50)
			{
				skillToCast = FLASH_OF_DARKNESS;
			}
			else
			{
				skillToCast = APOCALYPSE_FIRE;
			}
		}
		
		if ((skillToCast != null) && SkillCaster.checkUseConditions(npc, skillToCast.getSkill()))
		{
			npc.setTarget(player);
			npc.doCast(skillToCast.getSkill());
		}
	}
	
	/**
	 * This is the entry point for the {@code Etina} class.<br>
	 * It initializes a new instance of the {@code Etina} object.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new Etina();
	}
}
