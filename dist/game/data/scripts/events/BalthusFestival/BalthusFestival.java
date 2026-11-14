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
package events.BalthusFestival;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.w3c.dom.Document;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemSkillHolder;
import org.l2jmobius.gameserver.managers.events.BalthusEventManager;
import org.l2jmobius.gameserver.managers.events.BalthusEventManager.BalthusEventHolder;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.Id;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.holders.item.OnItemUse;
import org.l2jmobius.gameserver.mechanics.script.LongTimeEvent;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.network.serverpackets.balthusevent.ExBalthusEvent;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic and lifecycle for the {@code BalthusFestival} event.<br>
 * This class handles event data loading from XML and processes various game mechanics during the festival.
 * @author Index
 */
public class BalthusFestival extends LongTimeEvent implements IXmlReader
{
	// NPC
	private static final int FESTIVAL_FAIRY = 34330;
	
	// Item
	private static final ItemHolder GOOD_LUCK_BAG = new ItemHolder(60011, 1); // Festival Fairy's Good Luck Bag
	// Skills
	private static final List<SkillHolder> SKILLS = new ArrayList<>(8);
	static
	{
		SKILLS.add(new SkillHolder(29441, 1)); // Fairy_Coupon_1_Hour
		SKILLS.add(new SkillHolder(39171, 1)); // Fairy_Coupon_2_Hour
		SKILLS.add(new SkillHolder(39171, 2)); // Fairy_Coupon_3_Hour
		SKILLS.add(new SkillHolder(39171, 3)); // Fairy_Coupon_6_Hour
		SKILLS.add(new SkillHolder(39171, 4)); // Fairy_Coupon_8_Hour
		SKILLS.add(new SkillHolder(39171, 5)); // Fairy_Coupon_Unlimited_Hour
		SKILLS.add(new SkillHolder(27859, 1)); // Balthus_Coupon_2_Hour
		SKILLS.add(new SkillHolder(48853, 1)); // Balthus_Coupon_2_Hour
	}
	
	// Misc
	private static final String BALTHUS_BAG_VAR = "BALTHUS_BAG";
	
	/**
	 * Private constructor for the {@code BalthusFestival} class.<br>
	 * It initializes the festival NPCs and talk IDs.<br>
	 * It also triggers the initial loading process if the event is active.
	 */
	private BalthusFestival()
	{
		addStartNpc(FESTIVAL_FAIRY);
		addFirstTalkId(FESTIVAL_FAIRY);
		addTalkId(FESTIVAL_FAIRY);
		addVariableToClear(BALTHUS_BAG_VAR);
		if (isEventPeriod())
		{
			load();
			BalthusEventManager.getInstance().init();
		}
	}
	
	/**
	 * Loads the festival reward data from the configuration file.<br>
	 * This method initializes the necessary rewards for the {@code BalthusFestival}.
	 */
	@Override
	public synchronized void load()
	{
		parseDatapackFile("data/scripts/events/BalthusFestival/rewards.xml");
	}
	
	/**
	 * Parses the provided {@code Document} to configure event settings.<br>
	 * This method reads rewards and global configurations from an XML file.<br>
	 * It populates the {@code BalthusEventManager} with data from the {@code list} nodes.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} source of the configuration document.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		final AtomicInteger i = new AtomicInteger();
		forEach(document, "list", listNode ->
		{
			final StatSet set = new StatSet(parseAttributes(listNode));
			if (BalthusEventManager.getInstance().getMinLevel() == 0)
			{
				BalthusEventManager.getInstance().setEasyMode(set.getBoolean("easyMode", false));
				BalthusEventManager.getInstance().setMinLevel(set.getInt("minLevel", 85));
				BalthusEventManager.getInstance().setConsolation(new ItemHolder(set.getInt("id", 49783), set.getInt("count", 100)));
				BalthusEventManager.getInstance().setMailSubject(set.getString("mailSubject", "Balthus Knight Lottery"));
				BalthusEventManager.getInstance().setMailContent(set.getString("mailContent", "You win reward in Balthus Event!"));
			}
			
			forEach(listNode, "reward", reward ->
			{
				final AtomicInteger j = new AtomicInteger();
				final Map<Integer, Map<ItemChanceHolder, Double>> temanaRewardList = new HashMap<>();
				final Map<Integer, Integer> rewardTime = new HashMap<>();
				final StatSet rewardSet = new StatSet(parseAttributes(reward));
				final int fromValue = rewardSet.getInt("from");
				rewardTime.put(fromValue, rewardSet.getInt("to", fromValue));
				forEach(reward, "items", itemNode ->
				{
					forEach(itemNode, "item", item ->
					{
						final Map<ItemChanceHolder, Double> tempChanceRewardList = new HashMap<>();
						j.getAndIncrement();
						final StatSet itemSet = new StatSet(parseAttributes(item));
						ItemChanceHolder itemChanceHolder = new ItemChanceHolder(itemSet.getInt("id", 57), itemSet.getDouble("chance", 100), itemSet.getInt("count", 100));
						tempChanceRewardList.put(itemChanceHolder, itemSet.getDouble("lotteryChance", 0.0));
						temanaRewardList.put(j.intValue(), tempChanceRewardList);
					});
					i.getAndIncrement();
					BalthusEventManager.getInstance().addTemplate(i.intValue(), new BalthusEventHolder(rewardTime, temanaRewardList));
				});
			});
		});
	}
	
	/**
	 * Handles specific logic for Balthus Festival events.<br>
	 * Checks if the event is active before processing any actions.<br>
	 * Manages rewards, item exchanges, and player notifications based on the event name.
	 * @param event The unique identifier or name of the event being triggered.
	 * @param npc The {@code Npc} instance associated with the event.
	 * @param player The {@code Player} instance interacting with the event.
	 * @return A string representing the HTML content to display, or {@code null} if no action is taken.
	 */
	@Override
	public String onEvent(String event, Npc npc, Player player)
	{
		if (!isEventPeriod())
		{
			return "Balthus Event disabled.";
		}
		
		if (event.equals("balthusEventBuff" + player.getObjectId()))
		{
			BalthusEventManager.getInstance().removePlayer(player);
			player.sendPacket(new ExBalthusEvent(player));
		}
		
		if (event.equals("get_event_bag"))
		{
			final int minLevel = BalthusEventManager.getInstance().getMinLevel();
			if (player.getLevel() < minLevel)
			{
				return getHtm(player, "34330-7.htm").replace("%require_level%", String.valueOf(minLevel));
			}
			else if (player.getVariables().getBoolean(BALTHUS_BAG_VAR, false))
			{
				return "34330-3.htm";
			}
			else
			{
				player.getVariables().set(BALTHUS_BAG_VAR, true);
				player.addItem(ItemProcessType.REWARD, GOOD_LUCK_BAG.getId(), GOOD_LUCK_BAG.getCount(), null, true);
				return "34330-2.htm";
			}
		}
		else if (event.equals("34330.htm"))
		{
			return event;
		}
		else if (event.equals("34330-1.htm"))
		{
			return event;
		}
		else if (event.equals("34330-4.htm"))
		{
			return event;
		}
		else if (event.startsWith("exchange_coupon_"))
		{
			switch (event.substring(16))
			{
				case ("1"):
				{
					if (!player.destroyItemByItemId(ItemProcessType.FEE, 81726, 1, null, true))
					{
						return "34330-5.htm";
					}
					
					player.addItem(ItemProcessType.REWARD, 81711, 1, null, true);
					return "34330-6.htm";
				}
				case ("2"):
				{
					if (!player.destroyItemByItemId(ItemProcessType.FEE, 81726, 1, null, true))
					{
						return "34330-5.htm";
					}
					
					player.addItem(ItemProcessType.REWARD, 81710, 1, null, true);
					return "34330-6.htm";
				}
				case ("3"):
				{
					if (!player.destroyItemByItemId(ItemProcessType.FEE, 81726, 1, null, true))
					{
						return "34330-5.htm";
					}
					
					player.addItem(ItemProcessType.REWARD, 81709, 1, null, true);
					return "34330-6.htm";
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Determines the dialogue file to display when a player talks to an {@code Npc} for the first time.<br>
	 * This method constructs a filename based on the unique ID of the {@code Npc}.
	 * @param npc The {@code Npc} entity that is being interacted with.
	 * @param player The {@code Player} who initiated the conversation.
	 * @return A {@code String} representing the path to the HTML dialogue file.
	 */
	@Override
	public String onFirstTalk(Npc npc, Player player)
	{
		return npc.getId() + ".htm";
	}
	
	/**
	 * Handles the logic when a player uses an item.<br>
	 * This method checks if the event is active and processes specific skills.<br>
	 * It starts quest timers and sends packets to the player if requirements are met.
	 * @param event The {@code OnItemUse} event containing the player and item data.
	 */
	@RegisterEvent(EventType.ON_ITEM_USE)
	@RegisterType(ListenerRegisterType.ITEM)
	@Id(28609) // Balthus Knights' High-grade Mark
	@Id(47385) // Lucky Gift Coupon (2-hour)
	@Id(48223) // Lucky Gift Coupon (3-hour)
	@Id(48224) // Lucky Gift Coupon (6-hour)
	@Id(48853) // Lucky Gift Coupon (8-hour)
	@Id(60009) // Festival Fairy's Lucky Ticket
	public void onItemUse(OnItemUse event)
	{
		if (!isEventPeriod())
		{
			return;
		}
		
		final List<ItemSkillHolder> skills = event.getItem().getTemplate().getAllSkills();
		if (skills != null)
		{
			final Player player = event.getPlayer();
			for (SkillHolder skill : skills)
			{
				if (SKILLS.contains(skill))
				{
					startQuestTimer("balthusEventBuff" + player.getObjectId(), skill.getSkill().getAbnormalTime() * 1000, null, player);
					BalthusEventManager.getInstance().addPlayer(player);
					player.sendPacket(new ExBalthusEvent(player));
					break;
				}
			}
		}
	}
	
	/**
	 * Handles logic when a player logs into the game.<br>
	 * This method checks if the event is active and updates player buffs.<br>
	 * It also registers the player in the {@code BalthusEventManager}.
	 * @param event The {@code OnPlayerLogin} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGIN)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogin(OnPlayerLogin event)
	{
		if (!isEventPeriod())
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		for (SkillHolder skill : SKILLS)
		{
			final BuffInfo buff = player.getEffectList().getBuffInfoBySkillId(skill.getSkillId());
			if (buff != null)
			{
				final int objectId = player.getObjectId();
				cancelQuestTimer("balthusEventBuff" + objectId, null, player);
				startQuestTimer("balthusEventBuff" + objectId, buff.getTime() * 1000, null, player);
				BalthusEventManager.getInstance().addPlayer(player);
			}
		}
		
		player.sendPacket(new ExBalthusEvent(player));
	}
	
	/**
	 * This method handles the logic when a player logs out.<br>
	 * It checks if the event is currently active.<br>
	 * It removes the player from the {@code BalthusEventManager}.<br>
	 * It also cancels any active quest timers for that player.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	@RegisterEvent(EventType.ON_PLAYER_LOGOUT)
	@RegisterType(ListenerRegisterType.GLOBAL_PLAYERS)
	public void onPlayerLogout(OnPlayerLogout event)
	{
		if (!isEventPeriod())
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		cancelQuestTimer("balthusEventBuff" + player.getObjectId(), null, player);
		BalthusEventManager.getInstance().removePlayer(player);
	}
	
	/**
	 * This is the entry point for the {@code BalthusFestival} class.<br>
	 * It initializes a new instance of the festival manager.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new BalthusFestival();
	}
}
