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
package ai.areas.BeastFarm;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the improved artificial intelligence for baby pets.<br>
 * It manages specific behaviors and interactions for these entities within the game world.
 * @author St3eT
 */
public class ImprovedBabyPets extends Script
{
	// NPCs
	private static final int[] BABY_PETS =
	{
		16034, // Improved Baby Buffalo
		16035, // Improved Baby Kookaburra
		16036, // Improved Baby Cougar
	};
	
	// Skills
	private static final int PET_CONTROL = 5771;
	
	/**
	 * Initializes the {@code ImprovedBabyPets} script.<br>
	 * Registers the required NPC IDs for baby pets.
	 */
	private ImprovedBabyPets()
	{
		addSummonSpawnId(BABY_PETS);
	}
	
	/**
	 * Handles specific events for the baby pet system.<br>
	 * This method manages healing and buffing logic based on the {@code event} type.<br>
	 * It checks the status of the {@code Player} and their associated summon.
	 * @param event The name of the event to process.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (player != null)
		{
			final Summon summon = player.getPet();
			if (summon == null)
			{
				cancelQuestTimer("HEAL", null, player);
				cancelQuestTimer("BUFF", null, player);
			}
			else if (event.equals("HEAL") && player.isInCombat() && !summon.isHungry())
			{
				final double hpPer = (player.getCurrentHp() / player.getMaxHp()) * 100;
				final double mpPer = (player.getCurrentMp() / player.getMaxMp()) * 100;
				final int healType = summon.getTemplate().getParameters().getInt("heal_type", 0);
				final int skillLv = (summon.getLevel() / 5) - 11;
				if (healType == 1)
				{
					final int stepLv = Math.clamp(skillLv, 0, 3);
					if ((hpPer >= 30) && (hpPer < 70))
					{
						castHeal(summon, stepLv, 1);
					}
					else if (hpPer < 30)
					{
						castHeal(summon, stepLv, 2);
					}
				}
				else if (healType == 0)
				{
					if (hpPer < 30)
					{
						castHeal(summon, Math.clamp(skillLv, 0, 3), 2);
					}
					else if (mpPer < 60)
					{
						castHeal(summon, Math.clamp(skillLv, 0, 5), 1);
					}
				}
			}
			else if (event.equals("BUFF") && !summon.isAffectedBySkill(PET_CONTROL) && !summon.isHungry())
			{
				final int buffStep = Math.clamp(((summon.getLevel() / 5) - 11), 0, 3);
				for (int i = 1; i <= (2 * (1 + buffStep)); i++)
				{
					if (castBuff(summon, buffStep, i))
					{
						break;
					}
				}
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * This method is called when a new {@code Summon} is spawned.<br>
	 * It starts timers to manage healing and buffing for the owner.
	 * @param summon The {@code Summon} entity that was just created.
	 */
	@Override
	public void onSummonSpawn(Summon summon)
	{
		final Player owner = summon.getOwner();
		startQuestTimer("HEAL", 5000, null, owner, true);
		startQuestTimer("BUFF", 10000, null, owner, true);
	}
	
	/**
	 * This method is called when a player logs out of the game.<br>
	 * It stops any active quest timers for the logging out player.<br>
	 * Specifically, it cancels the {@code HEAL} and {@code BUFF} timers.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event.getPlayer();
		cancelQuestTimer("HEAL", null, player);
		cancelQuestTimer("BUFF", null, player);
	}
	
	/**
	 * Checks if a {@code Summon} can cast a specific buff based on its template parameters.<br>
	 * It validates the owner status, skill conditions, and target requirements.
	 * @param summon The {@code Summon} entity attempting to cast the buff.
	 * @param stepNumber The current step index for the AI sequence.
	 * @param buffNumber The specific buffer index within that step.
	 * @return {@code true} if the buff was successfully initiated, {@code false} otherwise.
	 */
	private boolean castBuff(Summon summon, int stepNumber, int buffNumber)
	{
		final Player owner = summon.getOwner();
		final StatSet parameters = summon.getTemplate().getParameters();
		final SkillHolder skill = parameters.getObject("step" + stepNumber + "_buff0" + buffNumber, SkillHolder.class);
		if ((skill != null) && (owner != null))
		{
			final boolean previousFollowStatus = summon.getFollowStatus();
			final SkillHolder mergedSkill = parameters.getObject("step" + stepNumber + "_merged_buff0" + buffNumber, SkillHolder.class);
			final int targetType = parameters.getInt("step" + stepNumber + "_buff_target0" + buffNumber, 0);
			if (!owner.hasAbnormalType(skill.getSkill().getAbnormalType()) && SkillCaster.checkUseConditions(summon, skill.getSkill()) && !owner.isDead())
			{
				if ((mergedSkill != null) && owner.hasAbnormalType(mergedSkill.getSkill().getAbnormalType()))
				{
					return false;
				}
				
				if (!previousFollowStatus && !summon.isInsideRadius3D(owner, skill.getSkill().getCastRange()))
				{
					return false;
				}
				
				if ((targetType >= 0) && (targetType <= 2))
				{
					summon.getAI().setIntentionCast(skill.getSkill(), (targetType == 1) ? summon : owner);
					summon.sendPacket(new SystemMessage(SystemMessageId.YOUR_PET_USES_S1).addSkillName(skill.getSkill()));
					if (previousFollowStatus != summon.getFollowStatus())
					{
						summon.setFollowStatus(previousFollowStatus);
					}
					
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Executes a healing skill for a specific pet.<br>
	 * This method checks if the conditions are met to cast a heal based on the provided step and number.<br>
	 * It determines the correct target and updates the pet's follow status if necessary.
	 * @param summon The {@code Summon} entity that will perform the action.
	 * @param stepNumber The specific AI step index to check for healing logic.
	 * @param healNumber The specific healing skill identifier within that step.
	 */
	private void castHeal(Summon summon, int stepNumber, int healNumber)
	{
		final boolean previousFollowStatus = summon.getFollowStatus();
		final Player owner = summon.getOwner();
		final StatSet parameters = summon.getTemplate().getParameters();
		final SkillHolder skill = parameters.getObject("step" + stepNumber + "_heal0" + healNumber, SkillHolder.class);
		final int targetType = parameters.getInt("step" + stepNumber + "_heal_target0" + healNumber, 0);
		if ((skill != null) && (owner != null) && SkillCaster.checkUseConditions(summon, skill.getSkill()) && !owner.isDead())
		{
			if (!previousFollowStatus && !summon.isInsideRadius3D(owner, skill.getSkill().getCastRange()))
			{
				return;
			}
			
			if (!owner.hasAbnormalType(skill.getSkill().getAbnormalType()) && (targetType >= 0) && (targetType <= 2))
			{
				summon.getAI().setIntentionCast(skill.getSkill(), (targetType == 1) ? summon : owner);
				summon.sendPacket(new SystemMessage(SystemMessageId.YOUR_PET_USES_S1).addSkillName(skill.getSkill()));
				if (previousFollowStatus != summon.getFollowStatus())
				{
					summon.setFollowStatus(previousFollowStatus);
				}
			}
		}
	}
	
	/**
	 * This is the main entry point for the {@code ImprovedBabyPets} class.<br>
	 * It initializes a new instance of the script to start the logic.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new ImprovedBabyPets();
	}
}
