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
package org.l2jmobius.gameserver.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.config.IllusoryEquipmentConfig;
import org.l2jmobius.gameserver.data.holders.VirtualItemHolder;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.data.xml.VirtualItemData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoEquipSlot;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.network.serverpackets.virtualItem.ExVirtualItemSystemBaseInfo;
import org.l2jmobius.gameserver.util.ArrayUtil;

/**
 * Keeps track of the illusory equipment a player carries.<br>
 * Equipped items are stored by the inventory itself, while the skills are buffs that do not survive a<br>
 * relog on their own, so the chosen skill entries are written to a player variable and applied at login.
 * @author CostyKiller
 */
public class IllusoryEquipmentManager
{
	private static final Logger LOGGER = Logger.getLogger(IllusoryEquipmentManager.class.getName());
	
	/** Items of the system, deleted along with the season the way their own description warns. */
	private static final int[] SEASON_ITEMS =
	{
		60793, // Illusory Equipment Points
		60893, // Illusory Equipment Points - 10
		60894, // Illusory Equipment Points Coupon
		60907, // Kit: Illusory Equipment Assist
		60909, // Illusory Equipment Points - 30
		61375, // Illusory Equipment Points - 100
	};
	
	/**
	 * Arms the sweep that clears everyone still online when the season runs out.<br>
	 * A player who is away at that moment is cleared by {@link #restore(Player)} at the next login instead.
	 */
	protected IllusoryEquipmentManager()
	{
		final int remaining = VirtualItemData.getInstance().getEventRemainingTime();
		if (remaining > 0)
		{
			ThreadPool.schedule(this::endSeason, remaining * 1000L);
			LOGGER.info("IllusoryEquipment: The season ends in " + (remaining / 86400) + " days.");
		}
	}
	
	/**
	 * Collects the virtual items a player currently carries.<br>
	 * Every applied entry is remembered in a player variable, the equipped items only carry the gear itself.
	 * @param player The {@code Player} to read.
	 * @param slotIndexes Filled with the window slot of every collected virtual item, mapped by category.
	 * @return A {@code List} with the virtual items the player currently carries.
	 */
	public List<VirtualItemHolder> getSelection(Player player, Map<Integer, Integer> slotIndexes)
	{
		final List<VirtualItemHolder> selection = new ArrayList<>();
		final VirtualItemData virtualItemData = VirtualItemData.getInstance();
		final StringTokenizer tokenizer = new StringTokenizer(player.getVariables().getString(PlayerVariables.ILLUSORY_SLOTS, ""), ";");
		while (tokenizer.hasMoreTokens())
		{
			final int[] entry = parseEntry(tokenizer.nextToken());
			if (entry == null)
			{
				continue;
			}
			
			final VirtualItemHolder holder = virtualItemData.getVirtualItem(entry[0], entry[1]);
			if (holder == null)
			{
				continue;
			}
			
			selection.add(holder);
			slotIndexes.put(Integer.valueOf(entry[0]), Integer.valueOf(entry[2]));
		}
		
		return selection;
	}
	
	/**
	 * Sums up the points a selection costs.
	 * @param selection The virtual items to add up.
	 * @return The amount of points the given selection costs.
	 */
	public int getUsedPoints(List<VirtualItemHolder> selection)
	{
		final VirtualItemData virtualItemData = VirtualItemData.getInstance();
		int usedPoints = 0;
		for (VirtualItemHolder holder : selection)
		{
			usedPoints += virtualItemData.getTotalCost(holder);
		}
		
		return usedPoints;
	}
	
	/**
	 * Remembers an equipped virtual item, so the window knows which slot holds which entry.
	 * @param player The {@code Player} wearing the item.
	 * @param holder The virtual item that was equipped.
	 * @param paperdollSlot The paperdoll slot the item landed in.
	 */
	public void rememberItem(Player player, VirtualItemHolder holder, int paperdollSlot)
	{
		writeEntry(player, holder.getIndexMain(), holder.getIndexSub(), getWindowSlot(paperdollSlot));
	}
	
	/**
	 * Retrieves the points a player has left to spend, in the window or in the store of the system.
	 * @param player The {@code Player} to read.
	 * @return The amount of points the player may still spend.
	 */
	public int getSpendablePoints(Player player)
	{
		final PlayerVariables variables = player.getVariables();
		return variables.getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0) - variables.getInt(PlayerVariables.ILLUSORY_POINTS_USED, 0);
	}
	
	/**
	 * Takes points away from a player for good, the way the store of the system charges them.
	 * @param player The {@code Player} paying.
	 * @param amount The amount of points to take.
	 */
	public void takePoints(Player player, int amount)
	{
		final PlayerVariables variables = player.getVariables();
		variables.set(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, Math.max(0, variables.getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0) - amount));
		player.sendPacket(new ExVirtualItemSystemBaseInfo(player));
	}
	
	/**
	 * Forgets a single category of a player.
	 * @param player The {@code Player} to write.
	 * @param indexMain The category to forget.
	 */
	public void forget(Player player, int indexMain)
	{
		writeEntry(player, indexMain, 0, 0);
	}
	
	/**
	 * Destroys a virtual item that left the paperdoll and forgets the entry it belonged to.<br>
	 * A virtual item never belongs in the inventory, so unequipping it takes it away for good.
	 * @param player The {@code Player} that wore the item.
	 * @param item The virtual {@code Item} that was unequipped.
	 */
	public void onUnequipped(Player player, Item item)
	{
		if (VirtualItemData.getInstance().getEventRemainingTime() == 0)
		{
			// The season sweep takes the item away itself, this would only chase an item that is already gone.
			return;
		}
		
		final int itemId = item.getId();
		final int enchant = item.getEnchantLevel();
		for (VirtualItemHolder holder : getSelection(player, new HashMap<>()))
		{
			if ((holder.getItemId() == itemId) && (holder.getEnchant() == enchant) && (holder.getSlot() != 0))
			{
				forget(player, holder.getIndexMain());
				break;
			}
		}
		
		// Whoever took the item off still has to answer the client, so the item goes the ordinary way once that is done.
		ThreadPool.schedule(() ->
		{
			player.getInventory().destroyItem(ItemProcessType.DESTROY, item, item.getCount(), player, null);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addRemovedItem(item);
			player.sendInventoryUpdate(iu);
			player.sendPacket(new ExUserInfoEquipSlot(player));
			player.broadcastUserInfo();
		}, 100);
		player.getVariables().set(PlayerVariables.ILLUSORY_POINTS_USED, getUsedPoints(getSelection(player, new HashMap<>())));
		player.sendPacket(new ExVirtualItemSystemBaseInfo(player));
		if (IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED)
		{
			LOGGER.info("IllusoryEquipment: Destroyed the unequipped virtual item " + itemId + " +" + enchant + " of " + player.getName() + ".");
		}
	}
	
	/**
	 * Grants a skill of the system as a buff and remembers it, replacing the one held by its category.
	 * @param player The {@code Player} receiving the buff.
	 * @param holder The virtual item granting the skill.
	 * @param slot The buff slot the window picked.
	 */
	public void applySkill(Player player, VirtualItemHolder holder, int slot)
	{
		final int skillId = holder.getItemId();
		final int skillLevel = holder.getEnchant();
		final Skill skill = SkillData.getInstance().getSkill(skillId, skillLevel);
		if (skill == null)
		{
			LOGGER.warning("IllusoryEquipment: The skill " + skillId + " level " + skillLevel + " does not exist.");
			return;
		}
		
		skill.applyEffects(player, player);
		writeEntry(player, holder.getIndexMain(), holder.getIndexSub(), slot);
	}
	
	/**
	 * Takes a skill of the system back from a player and forgets it.
	 * @param player The {@code Player} losing the buff.
	 * @param holder The virtual item granting the skill.
	 */
	public void removeSkill(Player player, VirtualItemHolder holder)
	{
		final Skill skill = SkillData.getInstance().getSkill(holder.getItemId(), holder.getEnchant());
		if (skill != null)
		{
			player.getEffectList().stopSkillEffects(SkillFinishType.REMOVED, skill);
		}
		
		writeEntry(player, holder.getIndexMain(), 0, 0);
	}
	
	/**
	 * Forgets every skill of the system a player picked, without touching the buffs themselves.
	 * @param player The {@code Player} to clear.
	 */
	public void clearSkills(Player player)
	{
		player.getVariables().remove(PlayerVariables.ILLUSORY_SLOTS);
	}
	
	/**
	 * Clears the illusory equipment of every player online, called once the season runs out.
	 */
	public void endSeason()
	{
		int cleared = 0;
		for (Player player : World.getPlayers())
		{
			if (endSeason(player))
			{
				cleared++;
			}
		}
		
		LOGGER.info("IllusoryEquipment: The season is over, " + cleared + " players online were cleared.");
	}
	
	/**
	 * Wipes everything of the system off a single player, the way the season end deletes it for good.<br>
	 * The worn items, the loose ones, the point items and every point collected or spent all go.
	 * @param player The {@code Player} to clear.
	 * @return {@code true} when the player had anything to clear, {@code false} otherwise.
	 */
	public boolean endSeason(Player player)
	{
		final PlayerVariables variables = player.getVariables();
		final List<VirtualItemHolder> selection = getSelection(player, new HashMap<>());
		for (VirtualItemHolder holder : selection)
		{
			if (holder.getSlot() == 0)
			{
				removeSkill(player, holder);
			}
		}
		
		final PlayerInventory inventory = player.getInventory();
		final InventoryUpdate iu = new InventoryUpdate();
		for (Item item : new ArrayList<>(inventory.getItems()))
		{
			if (!item.isVirtual() && !ArrayUtil.contains(SEASON_ITEMS, item.getId()))
			{
				continue;
			}
			
			inventory.destroyItem(ItemProcessType.DESTROY, item, item.getCount(), player, null);
			iu.addRemovedItem(item);
		}
		
		// The limit the coupons bought may be left standing, so a player keeps the higher cap into the next season.
		final boolean keepBonus = IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_KEEP_BONUS_POINTS;
		final boolean hadPoints = (variables.getInt(PlayerVariables.ILLUSORY_POINTS_ACQUIRED, 0) > 0) || (variables.getInt(PlayerVariables.ILLUSORY_POINTS_USED, 0) > 0) || (!keepBonus && (variables.getInt(PlayerVariables.ILLUSORY_POINTS_BONUS, 0) > 0));
		variables.remove(PlayerVariables.ILLUSORY_SLOTS);
		variables.remove(PlayerVariables.ILLUSORY_POINTS_ACQUIRED);
		variables.remove(PlayerVariables.ILLUSORY_POINTS_USED);
		if (!keepBonus)
		{
			variables.remove(PlayerVariables.ILLUSORY_POINTS_BONUS);
		}
		if (!hadPoints && selection.isEmpty() && iu.getItems().isEmpty())
		{
			return false;
		}
		
		if (!iu.getItems().isEmpty())
		{
			player.sendInventoryUpdate(iu);
			player.sendPacket(new ExUserInfoEquipSlot(player));
			player.broadcastUserInfo();
		}
		
		player.sendPacket(SystemMessageId.YOUR_ILLUSORY_EQUIPMENT_POINTS_ARE_RESET_SPEND_THESE_ANEW_IN_THE_ILLUSORY_EQUIPMENT_MENU);
		player.sendPacket(new ExVirtualItemSystemBaseInfo(player));
		return true;
	}
	
	/**
	 * Puts the illusory equipment of a player back in place, called once the player enters the world.<br>
	 * The skills are buffs that no relog survives, while a worn item that missed its last save comes back loose in the inventory.
	 * @param player The {@code Player} to restore.
	 */
	public void restore(Player player)
	{
		if (VirtualItemData.getInstance().getEventRemainingTime() == 0)
		{
			endSeason(player);
			return;
		}
		
		final List<VirtualItemHolder> selection = getSelection(player, new HashMap<>());
		final SkillData skillData = SkillData.getInstance();
		final String playerName = player.getName();
		for (VirtualItemHolder holder : selection)
		{
			if (holder.getSlot() != 0)
			{
				// Entries with a body part are worn items, the sweep below takes care of those.
				continue;
			}
			
			final int skillId = holder.getItemId();
			final int skillLevel = holder.getEnchant();
			final Skill skill = skillData.getSkill(skillId, skillLevel);
			if (skill == null)
			{
				continue;
			}
			
			skill.applyEffects(player, player);
			if (IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED)
			{
				LOGGER.info("IllusoryEquipment: Restored skill " + skillId + " level " + skillLevel + " of " + playerName + ".");
			}
		}
		
		// A virtual item is only ever worn, one resting in the inventory would be drawn by the window in place of the worn one.
		final PlayerInventory inventory = player.getInventory();
		final InventoryUpdate iu = new InventoryUpdate();
		for (Item item : new ArrayList<>(inventory.getItems()))
		{
			if (!item.isVirtual() || item.isEquipped())
			{
				continue;
			}
			
			final boolean remembered = isRemembered(selection, item);
			if (remembered)
			{
				inventory.equipItem(item);
				iu.addModifiedItem(item);
			}
			else
			{
				inventory.destroyItem(ItemProcessType.NONE, item, item.getCount(), player, null);
				iu.addRemovedItem(item);
			}
			
			if (IllusoryEquipmentConfig.ILLUSORY_EQUIPMENT_EVENT_DEBUG_ENABLED)
			{
				LOGGER.info("IllusoryEquipment: The loose virtual item " + item.getId() + " +" + item.getEnchantLevel() + " of " + playerName + " was " + (remembered ? "put back on." : "dropped."));
			}
		}
		
		if (!iu.getItems().isEmpty())
		{
			player.sendInventoryUpdate(iu);
			player.sendPacket(new ExUserInfoEquipSlot(player));
			player.broadcastUserInfo();
		}
	}
	
	/**
	 * Tells whether a virtual item is one the player picked and paid for.
	 * @param selection The virtual items the player currently carries.
	 * @param item The virtual {@code Item} to look up.
	 * @return {@code true} when the selection holds the given item, {@code false} otherwise.
	 */
	private boolean isRemembered(List<VirtualItemHolder> selection, Item item)
	{
		final int itemId = item.getId();
		final int enchant = item.getEnchantLevel();
		for (VirtualItemHolder holder : selection)
		{
			if ((holder.getSlot() != 0) && (holder.getItemId() == itemId) && (holder.getEnchant() == enchant))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Converts a paperdoll slot into the slot number the window uses.<br>
	 * Body parts a character wears only once are slot 0, the repeated ones count from one.
	 * @param paperdollSlot The paperdoll slot the item sits in.
	 * @return The window slot of the given paperdoll slot.
	 */
	public int getWindowSlot(int paperdollSlot)
	{
		if ((paperdollSlot == Inventory.PAPERDOLL_REAR) || (paperdollSlot == Inventory.PAPERDOLL_RFINGER))
		{
			return 1;
		}
		
		if ((paperdollSlot == Inventory.PAPERDOLL_LEAR) || (paperdollSlot == Inventory.PAPERDOLL_LFINGER))
		{
			return 2;
		}
		
		if ((paperdollSlot >= Inventory.PAPERDOLL_AGATHION1) && (paperdollSlot <= Inventory.PAPERDOLL_AGATHION5))
		{
			return (paperdollSlot - Inventory.PAPERDOLL_AGATHION1) + 1;
		}
		
		if ((paperdollSlot >= Inventory.PAPERDOLL_DECO1) && (paperdollSlot <= Inventory.PAPERDOLL_DECO6))
		{
			return (paperdollSlot - Inventory.PAPERDOLL_DECO1) + 1;
		}
		
		if ((paperdollSlot >= Inventory.PAPERDOLL_BROOCH_JEWEL1) && (paperdollSlot <= Inventory.PAPERDOLL_BROOCH_JEWEL6))
		{
			return (paperdollSlot - Inventory.PAPERDOLL_BROOCH_JEWEL1) + 1;
		}
		
		return 0;
	}
	
	/**
	 * Reads one stored skill entry.
	 * @param entry The stored text, category, sub index and window slot separated by commas.
	 * @return The three numbers of the entry, or {@code null} when the text is unreadable.
	 */
	private int[] parseEntry(String entry)
	{
		final StringTokenizer numbers = new StringTokenizer(entry, ",");
		if (numbers.countTokens() != 3)
		{
			return null;
		}
		
		try
		{
			final int indexMain = Integer.parseInt(numbers.nextToken().trim());
			final int indexSub = Integer.parseInt(numbers.nextToken().trim());
			final int slot = Integer.parseInt(numbers.nextToken().trim());
			return new int[]
			{
				indexMain,
				indexSub,
				slot
			};
		}
		catch (NumberFormatException e)
		{
			LOGGER.warning("IllusoryEquipment: Could not read the stored skill entry \"" + entry + "\".");
			return null;
		}
	}
	
	/**
	 * Writes one skill entry into the player variable, replacing whatever its category held.<br>
	 * A sub index of zero drops the category instead.
	 * @param player The {@code Player} to write.
	 * @param indexMain The category of the virtual item.
	 * @param indexSub The index of the virtual item inside its category.
	 * @param slot The buff slot the window picked.
	 */
	private void writeEntry(Player player, int indexMain, int indexSub, int slot)
	{
		final PlayerVariables variables = player.getVariables();
		final StringTokenizer tokenizer = new StringTokenizer(variables.getString(PlayerVariables.ILLUSORY_SLOTS, ""), ";");
		final StringBuilder sb = new StringBuilder();
		while (tokenizer.hasMoreTokens())
		{
			final String token = tokenizer.nextToken();
			final int[] entry = parseEntry(token);
			if ((entry != null) && (entry[0] != indexMain))
			{
				sb.append(token).append(";");
			}
		}
		
		if (indexSub > 0)
		{
			sb.append(indexMain).append(",").append(indexSub).append(",").append(slot).append(";");
		}
		
		variables.set(PlayerVariables.ILLUSORY_SLOTS, sb.toString());
	}
	
	/**
	 * Retrieves the single instance of {@code IllusoryEquipmentManager}.
	 * @return The active instance of {@code IllusoryEquipmentManager}.
	 */
	public static IllusoryEquipmentManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final IllusoryEquipmentManager INSTANCE = new IllusoryEquipmentManager();
	}
}
