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

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.SystemMessageId;

/**
 * This class handles the artificial intelligence for the {@code Ballista} entity.<br>
 * It defines the behaviors and actions performed by this specific NPC script.
 * @author St3eT
 */
public class Ballista extends Script
{
	// NPCs
	private static final int[] BALLISTA =
	{
		35685, // Shanty Fortress
		35723, // Southern Fortress
		35754, // Hive Fortress
		35792, // Valley Fortress
		35823, // Ivory Fortress
		35854, // Narsell Fortress
		35892, // Bayou Fortress
		35923, // White Sands Fortress
		35961, // Borderland Fortress
		35999, // Swamp Fortress
		36030, // Archaic Fortress
		36068, // Floran Fortress
		36106, // Cloud Mountain)
		36137, // Tanor Fortress
		36168, // Dragonspine Fortress
		36206, // Antharas's Fortress
		36244, // Western Fortress
		36282, // Hunter's Fortress
		36313, // Aaru Fortress
		36351, // Demon Fortress
		36389, // Monastic Fortress
	};
	
	// Skill
	private static final SkillHolder BOMB = new SkillHolder(2342, 1); // Ballista Bomb
	// Misc
	private static final int MIN_CLAN_LV = 5;
	
	/**
	 * Private constructor for the {@code Ballista} class.<br>
	 * This constructor initializes the script by registering necessary IDs.<br>
	 * It adds the {@code BALLISTA} array to both skill and spawn registries.
	 */
	private Ballista()
	{
		addSkillSeeId(BALLISTA);
		addSpawnId(BALLISTA);
	}
	
	/**
	 * Handles the logic when an {@code Npc} sees a specific skill being used.<br>
	 * This method checks if the caster uses the Ballista Bomb skill on the NPC.<br>
	 * If successful, it rewards clan reputation and kills the NPC.
	 * @param npc The {@code Npc} that is seeing the skill.
	 * @param caster The {@code Player} who cast the skill.
	 * @param skill The {@code Skill} object being used.
	 * @param targets A collection of {@code WorldObject} objects targeted by the skill.
	 * @param isSummon A boolean indicating if the skill is a summon.
	 */
	@Override
	public void onSkillSee(Npc npc, Player caster, Skill skill, Collection<WorldObject> targets, boolean isSummon)
	{
		if ((skill != null) && (caster.getTarget() == npc) && (getRandom(100) < 40) && (skill == BOMB.getSkill()))
		{
			if (npc.getFort().getSiege().isInProgress() && (caster.getClan() != null) && (caster.getClan().getLevel() >= MIN_CLAN_LV))
			{
				caster.getClan().addReputationScore(FeatureConfig.BALLISTA_POINTS);
				caster.sendPacket(SystemMessageId.THE_BALLISTA_HAS_BEEN_SUCCESSFULLY_DESTROYED_THE_CLAN_REPUTATION_WILL_BE_INCREASED);
			}
			
			npc.doDie(caster);
		}
	}
	
	/**
	 * This method is called when a new {@code Npc} spawns.<br>
	 * It disables the default core AI for the entity.<br>
	 * It sets the entity to be undying.
	 * @param npc The {@code Npc} instance that was just spawned.
	 */
	@Override
	public void onSpawn(Npc npc)
	{
		npc.disableCoreAI(true);
		npc.setUndying(true);
	}
	
	/**
	 * This is the entry point for the {@code Ballista} class.<br>
	 * It initializes a new instance of the {@code Ballista} script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new Ballista();
	}
}
