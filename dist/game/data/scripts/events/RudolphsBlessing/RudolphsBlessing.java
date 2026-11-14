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
package events.RudolphsBlessing;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerSummonAgathion;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerUnsummonAgathion;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.SkillCaster;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;

/**
 * Handles the logic for the {@code RudolphsBlessing} event.<br>
 * This class manages specific actions triggered during the duration of this long-time event.
 * @author ivantotov, Edoo
 */
public class RudolphsBlessing extends LongTimeEvent
{
	private static final int SANTA_CLAUS = 13285;
	
	private static final int AGATHION_SEAL_BRACELET_RUDOLPH = 21709;
	private static final int AGATHION_SEAL_BRACELET_RUDOLPH_NPC = 1598;
	private static final int ICE_CANDY_PIECE = 21915;
	private static final int SANTA_CLAUS_TREASURE_BOX = 21873;
	
	private static final int RUDOLPH_PRICE = 2019; // Calendar.getInstance().get(Calendar.YEAR) can be used but need to replace htmls.
	
	private static final SkillHolder RUDOLPH_TRICK = new SkillHolder(23181, 1);
	private static final ItemHolder ITEM_REQUIREMENT = new ItemHolder(21872, 2);
	
	/**
	 * Initializes the {@code RudolphsBlessing} event.<br>
	 * This constructor sets up the required NPCs and global listeners.<br>
	 * It registers handlers for summoning and unsummoning Agathions.
	 */
	public RudolphsBlessing()
	{
		addStartNpc(SANTA_CLAUS);
		addFirstTalkId(SANTA_CLAUS);
		addTalkId(SANTA_CLAUS);
		final ListenersContainer global = Containers.Global();
		global.addListener(new ConsumerEventListener(global, EventType.ON_PLAYER_SUMMON_AGATHION, (OnPlayerSummonAgathion event) -> onPlayerSummonAgathion(event), this));
		global.addListener(new ConsumerEventListener(global, EventType.ON_PLAYER_UNSUMMON_AGATHION, (OnPlayerUnsummonAgathion event) -> onPlayerUnsummonAgathion(event), this));
	}
	
	/**
	 * Handles specific event logic for the Rudolph's Blessing quest.<br>
	 * It processes item requirements, skill triggers, and reward distribution.
	 * @param event The name of the event to trigger.
	 * @param npc The {@code Npc} involved in the interaction.
	 * @param player The {@code Player} interacting with the event.
	 * @return The HTML text file to display or {@code null}.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (event.equals("rudolph_eat") && (player != null) && player.isOnline())
		{
			if (takeItems(player, ITEM_REQUIREMENT.getId(), ITEM_REQUIREMENT.getCount()))
			{
				SkillCaster.triggerCast(player, player, RUDOLPH_TRICK.getSkill());
				final Skill rudolphsBlessing = SkillData.getInstance().getSkill(23297, 1);
				rudolphsBlessing.applyEffects(player, player);
			}
			
			startQuestTimer("rudolph_eat", 10 * 60 * 1000, null, player);
		}
		
		if (player == null)
		{
			return null;
		}
		
		String htmltext = event;
		switch (event)
		{
			case "rudolph":
			{
				if (ownsAtLeastOneItem(player, AGATHION_SEAL_BRACELET_RUDOLPH))
				{
					htmltext = "13285-05.htm";
				}
				else if (player.getAdena() < RUDOLPH_PRICE)
				{
					htmltext = "13285-06.htm";
				}
				else
				{
					takeItems(player, Inventory.ADENA_ID, RUDOLPH_PRICE);
					giveItems(player, AGATHION_SEAL_BRACELET_RUDOLPH, 1);
					htmltext = "13285-04.htm";
				}
				break;
			}
			case "event_info":
			{
				htmltext = "13285-02.htm";
				break;
			}
			case "reward":
			{
				if (getQuestItemsCount(player, ICE_CANDY_PIECE) >= 50)
				{
					takeItems(player, ICE_CANDY_PIECE, 50);
					giveItems(player, SANTA_CLAUS_TREASURE_BOX, 1);
				}
				else
				{
					htmltext = "13285-03.htm";
				}
				break;
			}
		}
		
		return htmltext;
	}
	
	/**
	 * Handles the logic when a player summons an Agathion.<br>
	 * This method checks if the summoned Agathion is the correct NPC.<br>
	 * It starts a quest timer for the player if the conditions are met.
	 * @param event The {@code OnPlayerSummonAgathion} event containing summon details.
	 */
	private void onPlayerSummonAgathion(OnPlayerSummonAgathion event)
	{
		if (event.getAgathionId() != AGATHION_SEAL_BRACELET_RUDOLPH_NPC)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		startQuestTimer("rudolph_eat", 10 * 60 * 1000, null, player);
	}
	
	/**
	 * Handles the logic when a player unsummons an Agathion.<br>
	 * This method checks if the specific Rudolph NPC is involved.<br>
	 * It cancels the quest timer for the player if conditions are met.
	 * @param event The {@code OnPlayerUnsummonAgathion} event data.
	 */
	private void onPlayerUnsummonAgathion(OnPlayerUnsummonAgathion event)
	{
		if (event.getAgathionId() != AGATHION_SEAL_BRACELET_RUDOLPH_NPC)
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		cancelQuestTimer("rudolph_eat", null, player);
	}
	
	/**
	 * Handles the logic when a {@code Player} talks to an {@code Npc} for the first time.<br>
	 * This method returns the name of the HTML file to display.
	 * @param npc The {@code Npc} being interacted with.
	 * @param player The {@code Player} initiating the conversation.
	 * @return A {@code String} representing the path to the dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return "13285.htm";
	}
	
	/**
	 * This is the entry point for the application.<br>
	 * It creates a new instance of {@code RudolphsBlessing}.
	 * @param args Command line arguments.
	 */
	public static void main(String[] args)
	{
		new RudolphsBlessing();
	}
}
