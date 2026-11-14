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
package ai.areas.IsleOfPrayer;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * This class handles the artificial intelligence for {@code EvasGiftBox}.<br>
 * It defines the behaviors and interactions for this specific NPC script.
 * @author St3eT
 */
public class EvasGiftBox extends Script
{
	// NPC
	private static final int BOX = 32342; // Eva's Gift Box
	
	// Skill
	private static final SkillHolder KISS_OF_EVA = new SkillHolder(1073, 1); // Kiss of Eva
	// Items
	private static final int CORAL = 9692; // Red Coral
	private static final int CRYSTAL = 9693; // Crystal Fragment
	
	/**
	 * Private constructor for the {@code EvasGiftBox} class.<br>
	 * It initializes the script by registering the NPC ID.<br>
	 * This prevents other classes from creating new instances.
	 */
	private EvasGiftBox()
	{
		addKillId(BOX);
	}
	
	/**
	 * This method is called when an {@code Npc} is killed.<br>
	 * It handles special loot drops based on the killer's status.<br>
	 * The logic checks if the {@code Player} has a specific abnormal type.
	 * @param npc The {@code Npc} that was defeated.
	 * @param killer The {@code Player} who dealt the killing blow.
	 * @param isSummon Indicates whether the kill was caused by a summon.
	 */
	@Override
	public void onKill(Npc npc, Player killer, boolean isSummon)
	{
		if (killer.hasAbnormalType(KISS_OF_EVA.getSkill().getAbnormalType())) // It was checking if abnormal level is > 0. All cases when has this abnormal type, level is > 0.
		{
			if (getRandomBoolean())
			{
				npc.dropItem(killer, CRYSTAL, 1);
			}
			
			if (getRandom(100) < 33)
			{
				npc.dropItem(killer, CORAL, 1);
			}
		}
	}
	
	/**
	 * This is the entry point for the {@code EvasGiftBox} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new EvasGiftBox();
	}
}
