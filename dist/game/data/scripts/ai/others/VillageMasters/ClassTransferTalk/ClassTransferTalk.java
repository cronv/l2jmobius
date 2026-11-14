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
package ai.others.VillageMasters.ClassTransferTalk;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Handles the dialogue scripts for NPC interactions related to class transfers.<br>
 * This script uses a custom implementation to avoid legacy issues found in retail data.<br>
 * It manages the flow of information between {@code Player} entities and village masters.
 * @author Gladicek
 */
public class ClassTransferTalk extends Script
{
	// NPCs are talking in the Administrative Office of the Island Village.
	private static final int FRANCO = 32153;
	private static final int RIVIAN = 32147;
	private static final int DEVON = 32160;
	private static final int TOOK = 32150;
	private static final int KAKAI = 30565;
	private static final int MOKA = 32157;
	private static final int VALFAR = 32146;
	private static final int DRIKUS = 30505;
	private static final int RAYMOND = 30289;
	// @formatter:off
	private static final int[] FIRST_CLASS_TOWNS =
	{
		// Town of Gludio
		30288, 30297, 30504, 30503, 32196,
		// Town of Dion
		30070, 30066, 30462, 30508, 30595, 30594, 32199,
		// Gludin Village
		30290, 30499, 30498, 30500, 32193, 30373, 30037,
		// Town of Schuttgard  
		32095, 32098, 32094, 32092, 32093, 32097, 32202
	};
	private static final int[] SECOND_CLASS_TOWNS =
	{
		// Town of Goddard
		31279, 31755, 31276, 32226, 32225, 31272, 31269, 31288, 31285,
		// Town of Aden
		30865, 30862, 32221, 32222, 30854, 30857, 30849, 30847, 30845,
		// Town of Giran
		30512, 30474, 32213, 32214, 30513, 30511, 30109, 30120, 30115,
		// Town of Rune
		31317, 31314, 31324, 31321, 31326, 31336, 31334, 31328, 31331,
		// Town of Oren
		30681, 30677, 30676, 30187, 30191, 30195, 32205, 32206,
		// Heine
		30905, 30900, 30894, 30897, 32210, 32209, 30913, 30910,
		// Hunters Village
		30687, 30685, 32218, 32217, 30694, 30689, 30699, 30704,
		// Ivory Tower
		30174, 30175, 30176
	};
	// @formatter:on
	
	/**
	 * Private constructor for the {@code ClassTransferTalk} class.<br>
	 * This prevents direct instantiation of this script.
	 */
	private ClassTransferTalk()
	{
		addStartNpc(FRANCO, RIVIAN, DEVON, TOOK, KAKAI, MOKA, VALFAR);
		addStartNpc(FIRST_CLASS_TOWNS);
		addStartNpc(SECOND_CLASS_TOWNS);
		addTalkId(FRANCO, RIVIAN, DEVON, TOOK, KAKAI, MOKA, VALFAR);
		addTalkId(FIRST_CLASS_TOWNS);
		addTalkId(SECOND_CLASS_TOWNS);
	}
	
	/**
	 * Handles specific events for the class transfer dialogue.<br>
	 * It checks if the provided {@code event} matches a known HTML file path.<br>
	 * If it matches, the method returns that path as a string.
	 * @param event The name of the event or HTML file to check.
	 * @param npc The {@code Npc} entity involved in the interaction.
	 * @param player The {@code Player} entity interacting with the NPC.
	 * @return The matching HTML file path if found, otherwise {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "30289-01.html":
			case "30505-01.html":
			case "30565-02.html":
			case "30565-03.html":
			case "30565-04.html":
			case "30565-05.html":
			case "30565-06.html":
			case "30565-07.html":
			case "30565-08.html":
			case "30565-09.html":
			case "32147-02.html":
			case "32147-03.html":
			case "32147-04.html":
			case "32147-05.html":
			case "32147-06.html":
			case "32147-07.html":
			case "32147-08.html":
			case "32147-09.html":
			case "32147-10.html":
			case "32147-11.html":
			case "32150-02.html":
			case "32150-03.html":
			case "32150-04.html":
			case "32150-05.html":
			case "32150-06.html":
			case "32150-07.html":
			case "32150-08.html":
			case "32150-09.html":
			case "32157-01.html":
			case "32157-02.html":
			case "32157-03.html":
			case "32157-04.html":
			case "32160-02.html":
			case "32160-03.html":
			case "32160-04.html":
			case "32160-05.html":
			case "32160-06.html":
			case "32160-07.html":
			case "32160-08.html":
			case "32160-09.html":
			{
				htmltext = event;
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Determines the correct HTML dialogue file to display based on the NPC and player.<br>
	 * It checks if the {@code npc} belongs to specific town lists or unique IDs.<br>
	 * The logic also considers the {@code player} race and class type.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who is initiating the talk.
	 * @return A {@code String} representing the name of the HTML file to be loaded.
	 */
	@Override
	public String onTalk(Npc npc, Player player)
	{
		String htmltext = null;
		
		// Custom... retail still use old class transfer way for quest
		final int npcId = npc.getId();
		if (ArrayUtil.contains(FIRST_CLASS_TOWNS, npcId))
		{
			htmltext = "first_class_transfer.html";
		}
		else if (ArrayUtil.contains(SECOND_CLASS_TOWNS, npcId))
		{
			htmltext = "second_class_transfer.html";
		}
		else
		{
			switch (npcId)
			{
				case FRANCO:
				{
					if (player.getRace() == Race.HUMAN)
					{
						if (player.isMageClass())
						{
							htmltext = "32153-02.html";
						}
						else
						{
							// Custom (missing html for fighters?!)
							htmltext = "first_class_transfer.html";
						}
						break;
					}
					
					htmltext = "32153-01.html";
					break;
				}
				case RIVIAN:
				{
					if (player.getRace() == Race.ELF)
					{
						if (player.isMageClass())
						{
							htmltext = "32147-07.html";
						}
						else
						{
							htmltext = "32147-02.html";
						}
						break;
					}
					
					htmltext = "32147-01.html";
					break;
				}
				case DEVON:
				{
					if (player.getRace() == Race.DARK_ELF)
					{
						if (player.isMageClass())
						{
							htmltext = "32160-02.html";
						}
						else
						{
							htmltext = "32160-07.html";
						}
						break;
					}
					
					htmltext = "32160-01.html";
					break;
				}
				case TOOK:
				{
					if (player.getRace() == Race.ORC)
					{
						if (player.isMageClass())
						{
							htmltext = "32150-07.html";
						}
						else
						{
							htmltext = "32150-02.html";
						}
						break;
					}
					
					htmltext = "32150-01.html";
					break;
				}
				case KAKAI:
				{
					if (player.getRace() == Race.ORC)
					{
						if (player.isMageClass())
						{
							htmltext = "30565-07.html";
						}
						else
						{
							htmltext = "30565-02.html";
						}
						break;
					}
					
					htmltext = "30565-01.html";
					break;
				}
				case MOKA:
				{
					// Custom because on retail you can access this with every race...
					if (player.getRace() == Race.DWARF)
					{
						htmltext = "32157-01.html";
					}
					else
					{
						htmltext = "32157-05.html";
					}
					break;
				}
				case VALFAR:
				{
					// Retail like
					htmltext = "first_class_transfer.html";
					break;
				}
				case DRIKUS:
				{
					if (player.getRace() == Race.ORC)
					{
						if (player.isMageClass())
						{
							htmltext = "30505-07.html";
						}
						else
						{
							htmltext = "30505-02.html";
						}
						break;
					}
					
					htmltext = "30505-01.html";
					break;
				}
				case RAYMOND:
				{
					htmltext = "30289-01.html";
					break;
				}
			}
		}
		
		return htmltext;
	}
	
	/**
	 * This is the entry point for the {@code ClassTransferTalk} class.<br>
	 * It initializes a new instance of the script.
	 * @param args The command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new ClassTransferTalk();
	}
}
