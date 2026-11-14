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
package ai.areas.MonasteryOfSilence;

import java.util.Collection;

import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.enums.ChatType;

/**
 * Handles the artificial intelligence for the Monastery of Silence area.<br>
 * This {@code MonasteryOfSilence} class manages specific behaviors and scripts for NPCs within this zone.
 * @author Kerberos, nonom
 */
public class MonasteryOfSilence extends Script
{
	// NPCs
	private static final int CAPTAIN = 18910; // Solina Knight Captain
	private static final int KNIGHT = 18909; // Solina Knights
	private static final int SCARECROW = 18912; // Scarecrow
	private static final int GUIDE = 22789; // Guide Solina
	private static final int SEEKER = 22790; // Seeker Solina
	private static final int SAVIOR = 22791; // Savior Solina
	private static final int ASCETIC = 22793; // Ascetic Solina
	private static final int[] DIVINITY_CLAN =
	{
		22794, // Divinity Judge
		22795, // Divinity Manager
	};
	
	// Skills
	private static final SkillHolder ORDEAL_STRIKE = new SkillHolder(6303, 1); // Trial of the Coup
	private static final SkillHolder LEADER_STRIKE = new SkillHolder(6304, 1); // Shock
	private static final SkillHolder SAVER_STRIKE = new SkillHolder(6305, 1); // Sacred Gnosis
	private static final SkillHolder SAVER_BLEED = new SkillHolder(6306, 1); // Solina Strike
	private static final SkillHolder LEARNING_MAGIC = new SkillHolder(6308, 1); // Opus of the Wave
	private static final SkillHolder STUDENT_CANCEL = new SkillHolder(6310, 1); // Loss of Quest
	private static final SkillHolder WARRIOR_THRUSTING = new SkillHolder(6311, 1); // Solina Thrust
	private static final SkillHolder KNIGHT_BLESS = new SkillHolder(6313, 1); // Solina Bless
	
	// Misc
	private static final NpcStringId[] DIVINITY_MSG =
	{
		NpcStringId.S1_WHY_WOULD_YOU_CHOOSE_THE_PATH_OF_DARKNESS,
		NpcStringId.S1_HOW_DARE_YOU_DEFY_THE_WILL_OF_EINHASAD
	};
	
	// Removed with Etina's Fate.
	// private static final NpcStringId[] SOLINA_KNIGHTS_MSG =
	// {
	// NpcStringId.PUNISH_ALL_THOSE_WHO_TREAD_FOOTSTEPS_IN_THIS_PLACE,
	// NpcStringId.WE_ARE_THE_SWORD_OF_TRUTH_THE_SWORD_OF_SOLINA,
	// NpcStringId.WE_RAISE_OUR_BLADES_FOR_THE_GLORY_OF_SOLINA
	// };
	
	/**
	 * Private constructor for the {@code MonasteryOfSilence} class.<br>
	 * This prevents other classes from creating new instances of this script.
	 */
	private MonasteryOfSilence()
	{
		addSkillSeeId(DIVINITY_CLAN);
		addAttackId(KNIGHT, CAPTAIN, GUIDE, SEEKER, ASCETIC);
		addNpcHateId(GUIDE, SEEKER, SAVIOR, ASCETIC);
		addAggroRangeEnterId(GUIDE, SEEKER, SAVIOR, ASCETIC);
		addSpawnId(SCARECROW);
	}
	
	/**
	 * Handles specific events for the Monastery of Silence script.<br>
	 * Processes logic for training, casting skills, and timers.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class method call.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		switch (event)
		{
			case "TRAINING":
			{
				World.forEachVisibleObjectInRange(npc, Npc.class, 400, character ->
				{
					if ((getRandom(100) < 30) && !character.isDead() && !character.isInCombat())
					{
						final int characterId = character.getId();
						if ((characterId == CAPTAIN) && (getRandom(100) < 10) && npc.isScriptValue(0))
						{
							// character.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(SOLINA_KNIGHTS_MSG));
							character.setScriptValue(1);
							startQuestTimer("TIMER", 10000, character, null);
						}
						else if (characterId == KNIGHT)
						{
							character.setRunning();
							character.asAttackable().addDamageHate(npc, 0, 100);
							character.getAI().setIntentionAttack(npc);
						}
					}
				});
				break;
			}
			case "DO_CAST":
			{
				if ((npc != null) && (player != null) && (getRandom(100) < 3))
				{
					if (SkillCaster.checkUseConditions(npc, STUDENT_CANCEL.getSkill()))
					{
						npc.setTarget(player);
						npc.doCast(STUDENT_CANCEL.getSkill());
					}
					
					npc.setScriptValue(0);
				}
				break;
			}
			case "TIMER":
			{
				if (npc != null)
				{
					npc.setScriptValue(0);
				}
				break;
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles special behaviors when an {@code Npc} attacks a {@code Player}.<br>
	 * This method triggers specific skills and actions based on the NPC type.
	 * @param npc The {@code Npc} performing the attack.
	 * @param player The {@code Player} being attacked.
	 * @param damage The amount of damage dealt during the attack.
	 * @param isSummon Indicates if the attacker is a summon.
	 */
	@Override
	public void onAttack(Npc npc, Player player, int damage, boolean isSummon)
	{
		final Attackable mob = npc.asAttackable();
		
		switch (npc.getId())
		{
			case KNIGHT:
			{
				if ((getRandom(100) < 10) && (mob.getMostHated() == player) && SkillCaster.checkUseConditions(mob, WARRIOR_THRUSTING.getSkill()))
				{
					npc.setTarget(player);
					npc.doCast(WARRIOR_THRUSTING.getSkill());
				}
				break;
			}
			case CAPTAIN:
			{
				if ((getRandom(100) < 20) && (npc.getCurrentHp() < (npc.getMaxHp() * 0.5)) && npc.isScriptValue(0))
				{
					if (SkillCaster.checkUseConditions(npc, KNIGHT_BLESS.getSkill()))
					{
						npc.setTarget(npc);
						npc.doCast(KNIGHT_BLESS.getSkill());
					}
					
					npc.setScriptValue(1);
					npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.FOR_THE_GLORY_OF_SOLINA);
					addAttackPlayerDesire(addSpawn(KNIGHT, npc), player);
				}
				break;
			}
			case GUIDE:
			{
				if ((getRandom(100) < 3) && (mob.getMostHated() == player) && SkillCaster.checkUseConditions(npc, ORDEAL_STRIKE.getSkill()))
				{
					npc.setTarget(player);
					npc.doCast(ORDEAL_STRIKE.getSkill());
				}
				break;
			}
			case SEEKER:
			{
				if ((getRandom(100) < 33) && (mob.getMostHated() == player) && SkillCaster.checkUseConditions(npc, SAVER_STRIKE.getSkill()))
				{
					npc.setTarget(npc);
					npc.doCast(SAVER_STRIKE.getSkill());
				}
				break;
			}
			case ASCETIC:
			{
				if ((mob.getMostHated() == player) && npc.isScriptValue(0))
				{
					npc.setScriptValue(1);
					startQuestTimer("DO_CAST", 20000, npc, player);
				}
				break;
			}
		}
	}
	
	/**
	 * Determines if an {@code Attackable} mob should target a {@code Player}.<br>
	 * This check returns {@code true} only if the player is currently holding a weapon.
	 * @param mob The {@code Attackable} entity that might become hostile.
	 * @param player The {@code Player} being checked for hostility.
	 * @param isSummon Indicates if the action involves a summon.
	 * @return {@code true} if the player has an active weapon, otherwise {@code false}.
	 */
	@Override
	public boolean onNpcHate(Attackable mob, Player player, boolean isSummon)
	{
		return player.getActiveWeaponInstance() != null;
	}
	
	/**
	 * Handles logic when a {@code Player} enters the aggro range of an {@code Npc}.<br>
	 * This method checks if the player is using a weapon and triggers specific skills.<br>
	 * It also handles dialogue for unauthorized weapons and updates attack desires.
	 * @param npc The {@code Npc} that has detected the player.
	 * @param player The {@code Player} who entered the aggro range.
	 * @param isSummon Indicates if the action involves a summon.
	 */
	@Override
	public void onAggroRangeEnter(Npc npc, Player player, boolean isSummon)
	{
		if (player.getActiveWeaponInstance() != null)
		{
			SkillHolder skill = null;
			switch (npc.getId())
			{
				case GUIDE:
				{
					if (getRandom(100) < 3)
					{
						skill = LEADER_STRIKE;
					}
					break;
				}
				case SEEKER:
				{
					skill = SAVER_BLEED;
					break;
				}
				case SAVIOR:
				{
					skill = LEARNING_MAGIC;
					break;
				}
				case ASCETIC:
				{
					if (getRandom(100) < 3)
					{
						skill = STUDENT_CANCEL;
					}
					
					if (npc.isScriptValue(0))
					{
						npc.setScriptValue(1);
						startQuestTimer("DO_CAST", 20000, npc, player);
					}
					break;
				}
			}
			
			if ((skill != null) && SkillCaster.checkUseConditions(npc, skill.getSkill()))
			{
				npc.setTarget(player);
				npc.doCast(skill.getSkill());
			}
			
			if (!npc.isInCombat())
			{
				npc.broadcastSay(ChatType.NPC_GENERAL, NpcStringId.YOU_CANNOT_CARRY_A_WEAPON_WITHOUT_AUTHORIZATION);
			}
			
			addAttackPlayerDesire(npc, player);
		}
	}
	
	/**
	 * Handles the logic when an {@code Npc} detects a specific skill being used.<br>
	 * This method checks if the skill causes aggression and affects the {@code Npc}.<br>
	 * If triggered, the {@code Npc} will speak to the {@code Player} and increase its desire to attack them.
	 * @param npc The {@code Npc} that detects the skill.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} being used by the player.
	 * @param targets A collection of {@code WorldObject} instances hit by the skill.
	 * @param isSummon Whether the skill was cast on a summon.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		if (skill.hasEffectType(EffectType.AGGRESSION) && !targets.isEmpty())
		{
			for (WorldObject obj : targets)
			{
				if (obj.equals(npc))
				{
					npc.broadcastSay(ChatType.NPC_GENERAL, getRandomEntry(DIVINITY_MSG), caster.getName());
					addAttackPlayerDesire(npc, caster);
					break;
				}
			}
		}
	}
	
	/**
	 * This method is called when an {@code Npc} spawns.<br>
	 * It sets the NPC to be invincible and disables its core AI.<br>
	 * It also manages the quest timers for the entity.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.setInvul(true);
		npc.disableCoreAI(true);
		cancelQuestTimer("TRAINING", npc, null);
		startQuestTimer("TRAINING", 30000, npc, null, true);
	}
	
	/**
	 * This is the entry point for the {@code MonasteryOfSilence} class.<br>
	 * It initializes a new instance of the script to start the AI logic.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new MonasteryOfSilence();
	}
}
