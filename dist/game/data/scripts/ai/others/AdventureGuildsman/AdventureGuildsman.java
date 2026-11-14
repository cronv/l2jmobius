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
package ai.others.AdventureGuildsman;

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.data.enums.CategoryType;
import org.l2jmobius.gameserver.data.xml.MultisellData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.ExShowQuestInfo;

/**
 * This class defines the artificial intelligence for the {@code AdventureGuildsman} NPC.<br>
 * It handles the behavior and logic for this specific entity within the game world.
 * @author ChaosPaladin
 */
public class AdventureGuildsman extends Script
{
	// NPC
	private static final int ADVENTURE_GUILDSMAN = 33946;
	
	// Items
	private static final int PCCAFE_LOTTERY_TICKET_30DAYS = 15358;
	private static final int PCCAFE_1ST_LOTTERY_TICKET_30DAYS = 15359;
	private static final int PCCAFE_2ND_LOTTERY_TICKET_30DAYS = 15360;
	private static final int PCCAFE_3RD_LOTTERY_TICKET_30DAYS = 15361;
	private static final int PCCAFE_4TH_LOTTERY_TICKET_30DAYS = 15362;
	private static final int PCCAFE_5TH_LOTTERY_TICKET_30DAYS = 15363;
	private static final int VOUCHER_LEV_85 = 17739;
	private static final int VOUCHER_LEV_90 = 17740;
	private static final int VOUCHER_LEV_95 = 17741;
	private static final int VOUCHER_LEV_97 = 17742;
	private static final int SEAL_LEV_85 = 17743;
	private static final int SEAL_LEV_90 = 17744;
	private static final int SEAL_LEV_95 = 17745;
	private static final int SEAL_LEV_97 = 17746;
	
	// Skills
	private static final SkillHolder KNIGHT = new SkillHolder(62074, 1); // Fantasia Harmony - Adventurer
	private static final SkillHolder WARRIOR = new SkillHolder(62074, 1); // Fantasia Harmony - Adventurer
	private static final SkillHolder WIZARD = new SkillHolder(62074, 1); // Fantasia Harmony - Adventurer
	private static final SkillHolder MELODY = new SkillHolder(62072, 1); // Fever Melody - Adventurer
	private static final SkillHolder SONATA = new SkillHolder(62073, 1); // Drumbeat of Serenity - Adventurer
	
	// Misc
	// @formatter:off
	private static final Map<CategoryType, Integer> R_CLASS_TALISMAN = new EnumMap<>(CategoryType.class);
	static
	{
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_SIGEL_GROUP, 735);
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_TIR_GROUP, 736);
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_OTHEL_GROUP, 737);
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_YR_GROUP, 738);
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_WYNN_GROUP, 739);
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_IS_GROUP, 740);
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_FEOH_GROUP, 741);
		R_CLASS_TALISMAN.put(CategoryType.SIXTH_EOLH_GROUP, 742);
		R_CLASS_TALISMAN.put(CategoryType.ERTHEIA_FIGHTER_GROUP, 736);
		R_CLASS_TALISMAN.put(CategoryType.ERTHEIA_WIZARD_GROUP, 741);
	}
	private static final Map<CategoryType, Integer> R90_CLASS_TALISMAN = new EnumMap<>(CategoryType.class);
	static
	{
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_SIGEL_GROUP, 743);
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_TIR_GROUP, 744);
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_OTHEL_GROUP, 745);
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_YR_GROUP, 746);
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_WYNN_GROUP, 747);
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_IS_GROUP, 748);
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_FEOH_GROUP, 749);
		R90_CLASS_TALISMAN.put(CategoryType.SIXTH_EOLH_GROUP, 750);
		R90_CLASS_TALISMAN.put(CategoryType.ERTHEIA_FIGHTER_GROUP, 744);
		R90_CLASS_TALISMAN.put(CategoryType.ERTHEIA_WIZARD_GROUP, 749);
	}
	private static final Map<CategoryType, Integer> R95_CLASS_TALISMAN = new EnumMap<>(CategoryType.class);
	static
	{
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_SIGEL_GROUP, 751);
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_TIR_GROUP, 752);
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_OTHEL_GROUP, 753);
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_YR_GROUP, 754);
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_WYNN_GROUP, 755);
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_IS_GROUP, 756);
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_FEOH_GROUP, 757);
		R95_CLASS_TALISMAN.put(CategoryType.SIXTH_EOLH_GROUP, 758);
		R95_CLASS_TALISMAN.put(CategoryType.ERTHEIA_FIGHTER_GROUP, 752);
		R95_CLASS_TALISMAN.put(CategoryType.ERTHEIA_WIZARD_GROUP, 757);
	}
	private static final Map<CategoryType, Integer> R99_CLASS_TALISMAN = new EnumMap<>(CategoryType.class);
	static
	{
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_SIGEL_GROUP, 759);
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_TIR_GROUP, 760);
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_OTHEL_GROUP, 761);
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_YR_GROUP, 762);
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_WYNN_GROUP, 763);
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_IS_GROUP, 764);
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_FEOH_GROUP, 765);
		R99_CLASS_TALISMAN.put(CategoryType.SIXTH_EOLH_GROUP, 766);
		R99_CLASS_TALISMAN.put(CategoryType.ERTHEIA_FIGHTER_GROUP, 760);
		R99_CLASS_TALISMAN.put(CategoryType.ERTHEIA_WIZARD_GROUP, 765);
	}
	// @formatter:on
	private static final String USED_PC_LOTTERY_TICKET = "USED_PC_LOTTERY_TICKET";
	
	/**
	 * Private constructor for the {@code AdventureGuildsman} class.<br>
	 * This constructor initializes the NPC IDs and talk IDs.<br>
	 * It registers the required data into the script system.
	 */
	private AdventureGuildsman()
	{
		addStartNpc(ADVENTURE_GUILDSMAN);
		addTalkId(ADVENTURE_GUILDSMAN);
		addFirstTalkId(ADVENTURE_GUILDSMAN);
	}
	
	/**
	 * Handles specific events for the {@code AdventureGuildsman} script.<br>
	 * It returns HTML content based on the event name or sends a quest info packet.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} instance involved in the event.
	 * @param player The {@code Player} instance interacting with the NPC.
	 * @return The HTML text to display, or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "pccafe_list":
			{
				htmltext = "pccafe_list001.htm";
				break;
			}
			case "quest_list":
			{
				player.sendPacket(ExShowQuestInfo.STATIC_PACKET);
				break;
			}
			case "buff_list":
			{
				htmltext = "pccafe_buff_1001.htm";
				break;
			}
			case "item_list":
			{
				htmltext = "pccafe_item001.htm";
				break;
			}
			case "pccafe_help_inzone001.htm":
			case "pccafe_help_lottery001.htm":
			case "pccafe_help_lottery002.htm":
			case "adventurer_agent_town_voucher_change.htm":
			case "life_crystal_merge001.htm":
			case "life_crystal_merge002.htm":
			case "voucher_trader1001.htm":
			case "voucher_trader2001.htm":
			case "voucher_trader3001.htm":
			case "voucher_trader4001.htm":
			case "voucher_trader1004.htm":
			case "voucher_trader2004.htm":
			case "voucher_trader3004.htm":
			case "voucher_trader4004.htm":
			case "voucher_trader1005.htm":
			case "voucher_trader2005.htm":
			case "voucher_trader3005.htm":
			case "voucher_trader4005.htm":
			case "voucher_trader1006.htm":
			case "voucher_trader2006.htm":
			case "voucher_trader3006.htm":
			case "voucher_trader4006.htm":
			{
				htmltext = event;
				break;
			}
			case "index":
			{
				htmltext = player.getLevel() < 40 ? "adventurer_agent_town_77001.htm" : "adventurer_agent_town_77001e.htm";
				break;
			}
			case "buff_setlist":
			{
				htmltext = "pccafe_newbuff_001.htm";
				break;
			}
			case "buff_group":
			{
				htmltext = player.getPcCafePoints() >= 120 ? applyBuffsGroup(npc, player) : "pccafe_buff_1001.htm";
				break;
			}
			case "knight":
			{
				htmltext = player.getPcCafePoints() >= 200 ? applyBuffs(npc, player, KNIGHT.getSkill()) : "pccafe_buff_1001.htm";
				break;
			}
			case "warrior":
			{
				htmltext = player.getPcCafePoints() >= 200 ? applyBuffs(npc, player, WARRIOR.getSkill()) : "pccafe_buff_1001.htm";
				break;
			}
			case "wizard":
			{
				htmltext = player.getPcCafePoints() >= 200 ? applyBuffs(npc, player, WIZARD.getSkill()) : "pccafe_buff_1001.htm";
				break;
			}
			case "give_lottery_ticket":
			{
				if (!player.getVariables().getBoolean(USED_PC_LOTTERY_TICKET, false))
				{
					player.getVariables().set(USED_PC_LOTTERY_TICKET, true);
					giveItems(player, PCCAFE_LOTTERY_TICKET_30DAYS, 1);
				}
				else
				{
					htmltext = "pccafe_help_lottery_notoneday.htm";
				}
				break;
			}
			case "trade_10":
			{
				htmltext = tradeItem(player, PCCAFE_5TH_LOTTERY_TICKET_30DAYS, 10);
				break;
			}
			case "trade_100":
			{
				htmltext = tradeItem(player, PCCAFE_4TH_LOTTERY_TICKET_30DAYS, 100);
				break;
			}
			case "trade_200":
			{
				htmltext = tradeItem(player, PCCAFE_3RD_LOTTERY_TICKET_30DAYS, 200);
				break;
			}
			case "trade_1000":
			{
				htmltext = tradeItem(player, PCCAFE_2ND_LOTTERY_TICKET_30DAYS, 1000);
				break;
			}
			case "trade_10000":
			{
				htmltext = tradeItem(player, PCCAFE_1ST_LOTTERY_TICKET_30DAYS, 10000);
				break;
			}
			case "trade_seal85":
			{
				if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					if (hasQuestItems(player, VOUCHER_LEV_85))
					{
						takeItems(player, VOUCHER_LEV_85, 1);
						giveItems(player, SEAL_LEV_85, 20);
						addExpAndSp(player, 60000000, 0);
					}
					else
					{
						htmltext = "voucher_trader1003b.htm";
					}
				}
				else
				{
					htmltext = "voucher_trader1007.htm";
				}
				break;
			}
			case "trade_seal90":
			{
				if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					if (hasQuestItems(player, VOUCHER_LEV_90))
					{
						takeItems(player, VOUCHER_LEV_90, 1);
						giveItems(player, SEAL_LEV_90, 20);
						addExpAndSp(player, 66000000, 0);
					}
					else
					{
						htmltext = "voucher_trader2003b.htm";
					}
				}
				else
				{
					htmltext = "voucher_trader1007.htm";
				}
				break;
			}
			case "trade_seal95":
			{
				if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					if (hasQuestItems(player, VOUCHER_LEV_95))
					{
						takeItems(player, VOUCHER_LEV_95, 1);
						giveItems(player, SEAL_LEV_95, 20);
						addExpAndSp(player, 68000000, 0);
					}
					else
					{
						htmltext = "voucher_trader3003b.htm";
					}
				}
				else
				{
					htmltext = "voucher_trader1007.htm";
				}
				break;
			}
			case "trade_seal97":
			{
				if (player.isInCategory(CategoryType.SIXTH_CLASS_GROUP))
				{
					if (hasQuestItems(player, VOUCHER_LEV_97))
					{
						takeItems(player, VOUCHER_LEV_97, 1);
						giveItems(player, SEAL_LEV_97, 20);
						addExpAndSp(player, 76000000, 0);
					}
					else
					{
						htmltext = "voucher_trader3003b.htm";
					}
				}
				else
				{
					htmltext = "voucher_trader1007.htm";
				}
				break;
			}
			case "give_talismanR_by_class":
			{
				int multisellId = -1;
				for (Entry<CategoryType, Integer> entry : R_CLASS_TALISMAN.entrySet())
				{
					if (player.isInCategory(entry.getKey()))
					{
						multisellId = entry.getValue().intValue();
						break;
					}
				}
				
				if (multisellId > 0)
				{
					MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
				}
				break;
			}
			case "give_talismanR90_by_class":
			{
				int multisellId = -1;
				for (Entry<CategoryType, Integer> entry : R90_CLASS_TALISMAN.entrySet())
				{
					if (player.isInCategory(entry.getKey()))
					{
						multisellId = entry.getValue().intValue();
						break;
					}
				}
				
				if (multisellId > 0)
				{
					MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
				}
				break;
			}
			case "give_talismanR95_by_class":
			{
				int multisellId = -1;
				for (Entry<CategoryType, Integer> entry : R95_CLASS_TALISMAN.entrySet())
				{
					if (player.isInCategory(entry.getKey()))
					{
						multisellId = entry.getValue().intValue();
						break;
					}
				}
				
				if (multisellId > 0)
				{
					MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
				}
				break;
			}
			
			case "give_talismanR99_by_class":
			{
				int multisellId = -1;
				for (Entry<CategoryType, Integer> entry : R99_CLASS_TALISMAN.entrySet())
				{
					if (player.isInCategory(entry.getKey()))
					{
						multisellId = entry.getValue().intValue();
						break;
					}
				}
				
				if (multisellId > 0)
				{
					MultisellData.getInstance().separateAndSend(multisellId, player, npc, false);
				}
				break;
			}
		}
		
		if (event.startsWith("melody"))
		{
			final int pcCafePoints = player.getPcCafePoints();
			if (pcCafePoints >= 20)
			{
				npc.setTarget(player);
				npc.doCast(MELODY.getSkill());
				player.setPcCafePoints(pcCafePoints - 20);
				htmltext = "pccafe_buff_1001.htm";
			}
			else
			{
				htmltext = "pccafe_notpoint001.htm";
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Applies a series of buffs to the {@code Player}.<br>
	 * It triggers multiple skills from predefined groups.<br>
	 * It also applies the specific {@code Skill} provided as an argument.<br>
	 * Finally, it deducts 200 points from the player's PC Cafe points.
	 * @param npc The {@code Npc} performing the action.
	 * @param player The {@code Player} receiving the buffs.
	 * @param skill The specific {@code Skill} to be applied last.
	 * @return Always returns {@code null}.
	 */
	private String applyBuffs(Npc npc, Player player, Skill skill)
	{
		SkillCaster.triggerCast(npc, player, MELODY.getSkill());
		SkillCaster.triggerCast(npc, player, SONATA.getSkill());
		SkillCaster.triggerCast(npc, player, skill);
		player.setPcCafePoints(player.getPcCafePoints() - 200);
		return null;
	}
	
	/**
	 * Applies a group of skills to the {@code Player}.<br>
	 * This method triggers all skills defined in the {@code GROUP_MELODY} collection.<br>
	 * It also deducts 120 points from the player's {@code PcCafePoints}.
	 * @param npc The {@code Npc} performing the action.
	 * @param player The {@code Player} receiving the buffs.
	 * @return Always returns {@code null}.
	 */
	private String applyBuffsGroup(Npc npc, Player player)
	{
		SkillCaster.triggerCast(npc, player, MELODY.getSkill());
		player.setPcCafePoints(player.getPcCafePoints() - 120);
		return null;
	}
	
	/**
	 * Processes a trade between the player and the NPC.<br>
	 * It checks if the player has enough points to continue.<br>
	 * If successful, it removes an item and adds points to the player's balance.
	 * @param player The {@code Player} performing the trade.
	 * @param itemId The unique ID of the item to be traded.
	 * @param points The amount of points to add to the player.
	 * @return A string representing the HTML file to display to the player.
	 */
	private String tradeItem(Player player, int itemId, int points)
	{
		final int pcCafePoints = player.getPcCafePoints();
		if (pcCafePoints >= 200000)
		{
			return "pccafe_help_lottery_fail2.htm";
		}
		
		if (takeItems(player, itemId, 1))
		{
			player.setPcCafePoints(pcCafePoints + points);
			return "pccafe_help_lottery003.htm";
		}
		
		return "pccafe_help_lottery_fail.htm";
	}
	
	/**
	 * Handles the first interaction between a player and an NPC.<br>
	 * It determines which dialogue file to display based on the NPC ID.
	 * @param npc The {@code Npc} entity that is being spoken to.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return player.getLevel() < 40 ? "adventurer_agent_town_77001.htm" : "adventurer_agent_town_77001e.htm";
	}
	
	/**
	 * This is the entry point for the {@code AdventureGuildsman} class.<br>
	 * It initializes a new instance of the script.
	 * @param args Command line arguments passed to the program.
	 */
	public static void main(String[] args)
	{
		new AdventureGuildsman();
	}
}
