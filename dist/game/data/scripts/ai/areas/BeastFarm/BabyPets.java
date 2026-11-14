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

/**
 * This class handles the artificial intelligence for baby pets.<br>
 * It defines behaviors and interactions for these entities within the game world.
 * @author St3eT
 */
public class BabyPets extends Script
{
	// NPCs
	private static final int[] BABY_PETS =
	{
		12780, // Baby Buffalo
		12781, // Baby Kookaburra
		12782, // Baby Cougar
	};
	
	// Skills
	private static final int HEAL_1 = 4717; // Heal Trick
	private static final int HEAL_2 = 4718; // Greater Heal Trick
	
	/**
	 * Private constructor for the {@code BabyPets} class.<br>
	 * This constructor initializes the script and registers the required summon IDs.
	 */
	private BabyPets()
	{
		addSummonSpawnId(BABY_PETS);
	}
	
	/**
	 * Handles specific events for the baby pets script.<br>
	 * This method checks if a {@code HEAL} event occurs and attempts to heal the player's pet.<br>
	 * It uses random chances to cast different healing skills on the {@code Summon}.
	 * @param event The name of the event being triggered.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance involved in the event.
	 * @return The result of the super class {@code onEvent} method.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("HEAL") && (player != null))
		{
			final Summon summon = player.getPet();
			if (summon != null)
			{
				if (getRandom(100) <= 25)
				{
					castHeal(summon, new SkillHolder(HEAL_1, getHealLv(summon)), 80);
				}
				
				if (getRandom(100) <= 75)
				{
					castHeal(summon, new SkillHolder(HEAL_2, getHealLv(summon)), 15);
				}
			}
			else
			{
				cancelQuestTimer("HEAL", null, player);
			}
		}
		
		return super.onEvent(event, npc, player);
	}
	
	/**
	 * Handles the logic when a player logs out of the game.<br>
	 * This method cancels any active quest timers for the logging out player.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		cancelQuestTimer("HEAL", null, event.getPlayer());
	}
	
	/**
	 * This method is called when a new {@code Summon} is spawned.<br>
	 * It starts a quest timer for the owner of the summon.<br>
	 * The timer is used to trigger healing logic.
	 * @param summon The {@code Summon} entity that was just created.
	 */
	@Override
	public void onSummonSpawn(Summon summon)
	{
		startQuestTimer("HEAL", 5000, null, summon.getOwner(), true);
	}
	
	/**
	 * Calculates the healing level for a specific {@code Summon}.<br>
	 * The level is based on the {@code summon.getLevel()} value.<br>
	 * It ensures the result stays between {@code 1} and {@code 12}.
	 * @param summon The {@code Summon} object to check.
	 * @return The calculated healing level as an {@code int}.
	 */
	private int getHealLv(Summon summon)
	{
		final int summonLv = summon.getLevel();
		return Math.clamp(summonLv < 70 ? (summonLv / 10) : (7 + ((summonLv - 70) / 5)), 1, 12);
	}
	
	/**
	 * Casts a healing skill on the owner of a {@code Summon}.<br>
	 * This method checks if the owner's health is below a certain threshold.<br>
	 * It also ensures the pet is not hungry and can use the skill.
	 * @param summon The {@code Summon} entity that will cast the skill.
	 * @param skill The {@code SkillHolder} containing the healing skill information.
	 * @param maxHpPer The health percentage threshold required to trigger the heal.
	 */
	private void castHeal(Summon summon, SkillHolder skill, int maxHpPer)
	{
		final boolean previousFollowStatus = summon.getFollowStatus();
		final Player owner = summon.getOwner();
		if (!owner.isDead() && (((owner.getCurrentHp() / owner.getMaxHp()) * 100) < maxHpPer) && !summon.isHungry() && SkillCaster.checkUseConditions(summon, skill.getSkill()))
		{
			summon.getAI().setIntentionCast(skill.getSkill(), owner);
			summon.sendPacket(new SystemMessage(SystemMessageId.YOUR_PET_USES_S1).addSkillName(skill.getSkill()));
			if (previousFollowStatus != summon.getFollowStatus())
			{
				summon.setFollowStatus(previousFollowStatus);
			}
		}
	}
	
	/**
	 * This is the main entry point for the {@code BabyPets} script.<br>
	 * It initializes a new instance of the {@code BabyPets} class.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new BabyPets();
	}
}
