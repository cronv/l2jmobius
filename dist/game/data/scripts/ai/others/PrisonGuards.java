/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ai.others;

import java.util.Collection;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.interfaces.IPositionable;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * This class handles the artificial intelligence for {@code PrisonGuards}.<br>
 * It defines the behaviors and logic for these NPCs within the game world.
 * @author St3eT
 */
public class PrisonGuards extends Script
{
	// NPCs
	private static final int GUARD_HEAD = 18367; // Prison Guard
	private static final int GUARD = 18368; // Prison Guard
	
	// Item
	private static final int STAMP = 10013; // Race Stamp
	
	// Skills
	private static final int TIMER = 5239; // Event Timer
	private static final SkillHolder STONE = new SkillHolder(4578, 1); // Petrification
	private static final SkillHolder SILENCE = new SkillHolder(4098, 9); // Silence
	
	/**
	 * Private constructor for the {@code PrisonGuards} class.<br>
	 * It initializes the required IDs for attacks, spawns, and skills.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private PrisonGuards()
	{
		addAttackId(GUARD_HEAD, GUARD);
		addSpawnId(GUARD_HEAD, GUARD);
		addNpcHateId(GUARD);
		addSkillSeeId(GUARD);
		addSpellFinishedId(GUARD_HEAD, GUARD);
	}
	
	/**
	 * Handles specific events for the prison guard AI.<br>
	 * This method processes logic like clearing status or checking home locations.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("CLEAR_STATUS"))
		{
			npc.setScriptValue(0);
		}
		else if (event.equals("CHECK_HOME"))
		{
			final IPositionable spawnLocation = npc.getSpawn().getLocation();
			if ((npc.calculateDistance2D(spawnLocation) > 10) && !npc.isInCombat() && !npc.isDead())
			{
				npc.teleToLocation(spawnLocation);
			}
			
			startQuestTimer("CHECK_HOME", 30000, npc, null);
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when an {@code Npc} attacks a {@code Player}.<br>
	 * This method checks for specific conditions to trigger quest items or skills.
	 * @param npc The {@code Npc} performing the attack.
	 * @param player The {@code Player} being attacked.
	 * @param damage The amount of damage dealt.
	 * @param isSummon Whether the attack was performed by a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		if (npc.getId() == GUARD_HEAD)
		{
			if (player.isAffectedBySkill(TIMER))
			{
				if ((getRandom(100) < 10) && (npc.calculateDistance3D(player) < 100) && (getQuestItemsCount(player, STAMP) <= 3) && npc.isScriptValue(0))
				{
					giveItems(player, STAMP, 1);
					npc.setScriptValue(1);
					startQuestTimer("CLEAR_STATUS", 600000, npc, null);
				}
			}
			else
			{
				npc.setTarget(player);
				npc.doCast(STONE.getSkill());
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.IT_S_NOT_EASY_TO_OBTAIN);
			}
		}
		else if (!player.isAffectedBySkill(TIMER) && (npc.calculateDistance2D(npc.getSpawn().getLocation()) < 2000))
		{
			npc.setTarget(player);
			npc.doCast(STONE.getSkill());
			npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_RE_OUT_OF_YOUR_MIND_COMING_HERE);
		}
	}
	
	/**
	 * This method is called when an {@code Npc} sees a skill being used.<br>
	 * It checks if the {@code caster} is affected by the {@code TIMER} skill.<br>
	 * If not, the {@code npc} targets the {@code caster} and casts the {@code SILENCE} skill.
	 * @param npc The {@code Npc} that sees the skill.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} object being used.
	 * @param targets A collection of {@code WorldObject} entities hit by the skill.
	 * @param isSummon Indicates if the skill was cast by a summon.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		if (!caster.isAffectedBySkill(TIMER))
		{
			npc.setTarget(caster);
			npc.doCast(SILENCE.getSkill());
		}
	}
	
	/**
	 * This method is called when a spell finishes casting.<br>
	 * It checks if the skill used was {@code SILENCE} or {@code STONE}.<br>
	 * If so, it clears the aggro list for the {@code Npc} and sets its target to itself.
	 * @param npc The {@code Npc} that cast the spell.
	 * @param player The {@code Player} who was targeted by the spell.
	 * @param skill The {@code Skill} object representing the finished spell.
	 */
	@Override
	public void onSpellFinished(Npc npc, Player player, Skill skill)
	{
		if ((skill == SILENCE.getSkill()) || (skill == STONE.getSkill()))
		{
			npc.asAttackable().clearAggroList();
			npc.setTarget(npc);
		}
	}
	
	/**
	 * Determines if an {@code Npc} should target a specific {@code Player}.<br>
	 * This check is used to manage aggression behavior.
	 * @param mob The {@code Attackable} entity that might initiate the attack.
	 * @param player The {@code Player} being targeted by the {@code mob}.
	 * @param isSummon Indicates if the action is performed by a summon.
	 * @return {@code true} if the {@code player} is affected by the {@code TIMER} skill, otherwise {@code false}.
	 */
	@Override
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return player.isAffectedBySkill(TIMER);
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It sets specific behaviors based on the NPC ID.<br>
	 * The head guard becomes immobilized and invincible.<br>
	 * Other guards start walking randomly and reset their quest timers.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		if (npc.getId() == GUARD_HEAD)
		{
			npc.setImmobilized(true);
			npc.setInvul(true);
		}
		else
		{
			npc.setRandomWalking(true);
			cancelQuestTimer("CHECK_HOME", npc, null);
			startQuestTimer("CHECK_HOME", 30000, npc, null);
		}
	}
	
	/**
	 * This is the entry point for the {@code PrisonGuards} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new PrisonGuards();
	}
}
