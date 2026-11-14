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
package ai.others.AlchemistManager;

import java.util.List;

import org.l2jmobius.gameserver.data.xml.SkillTreeData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.enums.AcquireSkillType;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillLearn;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExAcquirableSkillListByClass;
import org.l2jmobius.gameserver.network.serverpackets.ExTutorialShowId;

/**
 * Manages the artificial intelligence behavior for {@code Alchemist} NPCs.<br>
 * This class handles specific logic and interactions related to alchemists within the game world.
 * @author Sdw
 */
public class AlchemistManager extends Script
{
	// NPCs
	private static final int[] ALCHEMISTS =
	{
		33978, // Zephyra
		33977, // Veruti
	};
	
	/**
	 * Private constructor for the {@code AlchemistManager} class.<br>
	 * This constructor initializes the NPC data and talk IDs.<br>
	 * It prevents other classes from creating new instances of this manager.
	 */
	private AlchemistManager()
	{
		addStartNpc(ALCHEMISTS);
		addTalkId(ALCHEMISTS);
		addFirstTalkId(ALCHEMISTS);
	}
	
	/**
	 * Handles specific events for the Alchemist NPC.<br>
	 * Processes requests like opening tutorials or learning skills.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the interaction.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The path to the HTML file to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "33978.html":
			case "33977.html":
			{
				htmltext = event;
				break;
			}
			case "open_tutorial":
			{
				player.sendPacket(new ExTutorialShowId(26));
				htmltext = npc.getId() + "-1.html";
				break;
			}
			case "learn_skill":
			{
				if (player.getRace() == Race.ERTHEIA)
				{
					final List<SkillLearn> alchemySkills = SkillTreeData.getInstance().getAvailableAlchemySkills(player);
					if (alchemySkills.isEmpty())
					{
						player.sendPacket(SystemMessageId.THERE_ARE_NO_OTHER_SKILLS_TO_LEARN);
					}
					else
					{
						player.sendPacket(new ExAcquirableSkillListByClass(alchemySkills, AcquireSkillType.ALCHEMY));
					}
				}
				else
				{
					htmltext = npc.getId() + "-2.html";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method determines which HTML file to display based on the NPC's ID.
	 * @param npc The {@code Npc} entity being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A string representing the path to the HTML file to be shown.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".html";
	}
	
	/**
	 * This is the entry point for the {@code AlchemistManager} class.<br>
	 * It initializes a new instance of the manager to start the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new AlchemistManager();
	}
}
