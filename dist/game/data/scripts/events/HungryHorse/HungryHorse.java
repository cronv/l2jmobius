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
package events.HungryHorse;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Handles the logic for the {@code HungryHorse} event.<br>
 * This class manages the behavior and mechanics of the horse during the event period.
 * @URL http://www.lineage2.com/en/news/events/hungry-horse-event-09192017.php
 * @author Mobius
 */
public class HungryHorse extends LongTimeEvent
{
	// NPC
	private static final int GALUP = 34010;
	
	// Items
	private static final int CARROT = 40363;
	private static final int POUCH = 40365;
	
	// Skills
	private static final SkillHolder[] GROUP_BUFFS =
	{
		new SkillHolder(62072, 1), // Fever Melody - Adventurer
		new SkillHolder(62073, 1), // Drumbeat of Serenity - Adventurer
	};
	private static final SkillHolder FANTASIA = new SkillHolder(62074, 1); // Fantasia Harmony - Adventurer
	private static final SkillHolder XP_BUFF = new SkillHolder(19036, 1); // Blessing of Light
	
	/**
	 * Private constructor for the {@code HungryHorse} class.<br>
	 * This constructor initializes the event by registering the required NPC IDs.<br>
	 * It sets up the starting NPC and the necessary talk IDs.
	 */
	private HungryHorse()
	{
		addStartNpc(GALUP);
		addFirstTalkId(GALUP);
		addTalkId(GALUP);
	}
	
	/**
	 * Handles the logic for specific event interactions.<br>
	 * It processes rewards and skills based on the provided {@code event} type.
	 * @param event The identifier for the current interaction.
	 * @param npc The {@code Npc} entity involved in the event.
	 * @param player The {@code Player} interacting with the NPC.
	 * @return The name of the HTML file to display to the player.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		String htmltext = null;
		switch (event)
		{
			case "34010-01.htm":
			case "34010-02.htm":
			{
				htmltext = event;
				break;
			}
			case "knight":
			{
				if (getQuestItemsCount(player, CARROT) >= 7)
				{
					takeItems(player, CARROT, 7);
					for (SkillHolder holder : GROUP_BUFFS)
					{
						SkillCaster.triggerCast(npc, player, holder.getSkill());
					}
					
					SkillCaster.triggerCast(npc, player, FANTASIA.getSkill()); // TODO: Merge events.
					SkillCaster.triggerCast(npc, player, XP_BUFF.getSkill());
				}
				else
				{
					htmltext = "34010-03.htm";
				}
				break;
			}
			case "warrior":
			{
				if (getQuestItemsCount(player, CARROT) >= 7)
				{
					takeItems(player, CARROT, 7);
					for (SkillHolder holder : GROUP_BUFFS)
					{
						SkillCaster.triggerCast(npc, player, holder.getSkill());
					}
					
					SkillCaster.triggerCast(npc, player, FANTASIA.getSkill()); // TODO: Merge events.
					SkillCaster.triggerCast(npc, player, XP_BUFF.getSkill());
				}
				else
				{
					htmltext = "34010-03.htm";
				}
				break;
			}
			case "wizard":
			{
				if (getQuestItemsCount(player, CARROT) >= 7)
				{
					takeItems(player, CARROT, 7);
					for (SkillHolder holder : GROUP_BUFFS)
					{
						SkillCaster.triggerCast(npc, player, holder.getSkill());
					}
					
					SkillCaster.triggerCast(npc, player, FANTASIA.getSkill()); // TODO: Merge events.
					SkillCaster.triggerCast(npc, player, XP_BUFF.getSkill());
				}
				else
				{
					htmltext = "34010-03.htm";
				}
				break;
			}
			case "giveCarrots40":
			{
				if (getQuestItemsCount(player, CARROT) >= 40)
				{
					takeItems(player, CARROT, 40);
					giveItems(player, POUCH, 1);
					htmltext = "34010-04.htm";
				}
				else
				{
					htmltext = "34010-03.htm";
				}
				break;
			}
			case "giveCarrots4000":
			{
				if (getQuestItemsCount(player, CARROT) >= 4000)
				{
					takeItems(player, CARROT, 4000);
					giveItems(player, POUCH, 100);
					htmltext = "34010-04.htm";
				}
				else
				{
					htmltext = "34010-03.htm";
				}
				break;
			}
			case "giveCarrots40000":
			{
				if (getQuestItemsCount(player, CARROT) >= 40000)
				{
					takeItems(player, CARROT, 40000);
					giveItems(player, POUCH, 1000);
					htmltext = "34010-04.htm";
				}
				else
				{
					htmltext = "34010-03.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the logic for when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method determines which HTML file to display based on the NPC ID.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} entity who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML file to be displayed.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + "-01.htm";
	}
	
	/**
	 * This is the entry point for the {@code HungryHorse} class.<br>
	 * It initializes a new instance of the event.
	 * @param args The command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new HungryHorse();
	}
}
