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
package org.l2jmobius.gameserver.network.clientpackets.storereview;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.gameserver.data.holders.CollectionDataHolder;
import org.l2jmobius.gameserver.data.xml.AugmentationData;
import org.l2jmobius.gameserver.data.xml.CollectionData;
import org.l2jmobius.gameserver.data.xml.EnsoulData;
import org.l2jmobius.gameserver.data.xml.HennaData;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.player.PrivateStoreType;
import org.l2jmobius.gameserver.entity.item.EtcItem;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.holders.ItemEnchantHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemInfo;
import org.l2jmobius.gameserver.entity.item.type.ActionType;
import org.l2jmobius.gameserver.entity.item.type.EtcItemType;
import org.l2jmobius.gameserver.handler.IItemHandler;
import org.l2jmobius.gameserver.handler.ItemHandler;
import org.l2jmobius.gameserver.managers.PrivateStoreHistoryManager;
import org.l2jmobius.gameserver.managers.PrivateStoreHistoryManager.ItemHistoryTransaction;
import org.l2jmobius.gameserver.network.clientpackets.ClientPacket;
import org.l2jmobius.gameserver.network.holders.TradeItem;
import org.l2jmobius.gameserver.network.serverpackets.storereview.ExPrivateStoreSearchHistory;
import org.l2jmobius.gameserver.network.serverpackets.storereview.ExPrivateStoreSearchItem;

/**
 * Handles the client request to search for items within a private store.<br>
 * This packet allows players to filter and view specific items in a {@code PrivateStore}.<br>
 * It processes the search criteria provided by the client to return matching results.
 * @author Mobius
 */
public class ExRequestPrivateStoreSearchList extends ClientPacket
{
	public static final int MAX_ITEM_PER_PAGE = 120;
	
	private static final Set<EtcItemType> STANDARD_ENCHANT_SCROLL_TYPES = EnumSet.of(EtcItemType.ENCHT_WP, EtcItemType.ENCHT_AM, EtcItemType.ENCHT_WP_DOWN, EtcItemType.ENCHT_AM_DOWN);
	private static final Set<EtcItemType> BLESSED_ENCHANT_SCROLL_TYPES = EnumSet.of(EtcItemType.BLESS_ENCHT_WP, EtcItemType.BLESS_ENCHT_AM, EtcItemType.BLESS_ENCHT_AM_DOWN);
	private static final Set<EtcItemType> GIANT_ENCHANT_SCROLL_TYPES = EnumSet.of(EtcItemType.GIANT_ENCHT_WP, EtcItemType.GIANT_ENCHT_AM, EtcItemType.MULTI_ENCHT_WP, EtcItemType.MULTI_ENCHT_AM);
	private static final Set<EtcItemType> INC_ENCHANT_PROP_TYPES = EnumSet.of(EtcItemType.ENCHT_ATTR_INC_PROP_ENCHT_WP, EtcItemType.ENCHT_ATTR_INC_PROP_ENCHT_AM, EtcItemType.BLESS_INC_PROP_ENCHT_WP, EtcItemType.BLESS_INC_PROP_ENCHT_AM, EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_WP, EtcItemType.BLESSED_ENCHT_ATTR_INC_PROP_ENCHT_AM, EtcItemType.GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP, EtcItemType.GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM, EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_WP, EtcItemType.BLESSED_GIANT_ENCHT_ATTR_INC_PROP_ENCHT_AM, EtcItemType.MULTI_INC_PROB_ENCHT_WP, EtcItemType.MULTI_INC_PROB_ENCHT_AM);
	
	private String _searchWord;
	private StoreType _storeType;
	private StoreItemType _itemType;
	private StoreSubItemType _itemSubtype;
	private int _searchCollection;
	
	@Override
	protected void readImpl()
	{
		_searchWord = readSizedString();
		_storeType = StoreType.findById(readByte());
		_itemType = StoreItemType.findById(readByte());
		_itemSubtype = StoreSubItemType.findById(readByte());
		_searchCollection = readByte();
	}
	
	@Override
	protected void runImpl()
	{
		final Player player = getPlayer();
		if (player == null)
		{
			return;
		}
		
		final Collection<Player> stores = World.getSellingOrBuyingPlayers();
		final List<ShopItem> items = new ArrayList<>();
		final List<Integer> itemIds = new ArrayList<>();
		stores.forEach(vendor ->
		{
			for (TradeItem item : vendor.getPrivateStoreType() == PrivateStoreType.BUY ? vendor.getBuyList().getItems() : vendor.getSellList().getItems())
			{
				// Filter by storeType.
				if ((_storeType == StoreType.ALL) || ((_storeType == StoreType.SELL) && ((vendor.getPrivateStoreType() == PrivateStoreType.SELL) || (vendor.getPrivateStoreType() == PrivateStoreType.PACKAGE_SELL))) || ((_storeType == StoreType.BUY) && (vendor.getPrivateStoreType() == PrivateStoreType.BUY)))
				{
					if (isItemVisibleForShop(item))
					{
						// Filter by Word if supplied.
						if (_searchWord.isEmpty() || item.getItem().getName().toLowerCase().contains(_searchWord.toLowerCase()))
						{
							items.add(new ShopItem(item, vendor, vendor.getPrivateStoreType()));
							itemIds.add(item.getItem().getId());
						}
					}
				}
			}
		});
		
		int nSize = items.size();
		int maxPage = Math.max(1, (nSize / (MAX_ITEM_PER_PAGE * 1F)) > (nSize / MAX_ITEM_PER_PAGE) ? (nSize / MAX_ITEM_PER_PAGE) + 1 : nSize / MAX_ITEM_PER_PAGE);
		for (int page = 1; page <= maxPage; page++)
		{
			final int nsize = page == maxPage ? ((nSize % MAX_ITEM_PER_PAGE) > 0) || (nSize == 0) ? nSize % MAX_ITEM_PER_PAGE : MAX_ITEM_PER_PAGE : MAX_ITEM_PER_PAGE;
			player.sendPacket(new ExPrivateStoreSearchItem(page, maxPage, nsize, items));
		}
		
		final List<ItemHistoryTransaction> history = new ArrayList<>();
		final List<ItemHistoryTransaction> historyTemp = new ArrayList<>();
		PrivateStoreHistoryManager.getInstance().getHistory().forEach(transaction ->
		{
			if (itemIds.contains(transaction.getItemId()))
			{
				history.add(transaction);
			}
		});
		
		int page = 1;
		final int historySize = history.size();
		maxPage = Math.max(1, (historySize / (MAX_ITEM_PER_PAGE * 1F)) > (historySize / MAX_ITEM_PER_PAGE) ? (historySize / MAX_ITEM_PER_PAGE) + 1 : historySize / MAX_ITEM_PER_PAGE);
		
		for (int index = 0; index < historySize; index++)
		{
			historyTemp.add(history.get(index));
			
			if ((index == (historySize - 1)) || (index == (MAX_ITEM_PER_PAGE - 1)) || ((index > 0) && ((index % (MAX_ITEM_PER_PAGE - 1)) == 0)))
			{
				player.sendPacket(new ExPrivateStoreSearchHistory(page, maxPage, historyTemp));
				page++;
				historyTemp.clear();
			}
		}
		
		if (page == 1)
		{
			player.sendPacket(new ExPrivateStoreSearchHistory(1, 1, historyTemp));
		}
	}
	
	/**
	 * Checks if an item should be displayed in the shop based on current filters.<br>
	 * It validates the item against types like equipment, enhancement items, and groceries.<br>
	 * The method also handles specific collection search logic.
	 * @param item The {@code TradeItem} to check for visibility.
	 * @return {@code true} if the item matches the shop filters, {@code false} otherwise.
	 */
	private boolean isItemVisibleForShop(TradeItem item)
	{
		/**
		 * Equipement
		 */
		if ((_itemType == StoreItemType.EQUIPMENT) && (_itemSubtype == StoreSubItemType.ALL) && (_searchCollection == 0))
		{
			// Equipment - All
			return item.getItem().isEquipable();
		}
		
		if ((_itemType == StoreItemType.EQUIPMENT) && (_itemSubtype == StoreSubItemType.WEAPON) && (_searchCollection == 0))
		{
			// Equipment - Weapon
			return item.getItem().isEquipable() && item.getItem().isWeapon();
		}
		
		if ((_itemType == StoreItemType.EQUIPMENT) && (_itemSubtype == StoreSubItemType.ARMOR) && (_searchCollection == 0))
		{
			// Equipment - Armor
			return item.getItem().isEquipable() && isEquipmentArmor(item.getItem());
		}
		
		if ((_itemType == StoreItemType.EQUIPMENT) && (_itemSubtype == StoreSubItemType.ACCESSORY) && (_searchCollection == 0))
		{
			// Equipment - Accessory
			return item.getItem().isEquipable() && isAccessory(item.getItem());
		}
		
		if ((_itemType == StoreItemType.EQUIPMENT) && (_itemSubtype == StoreSubItemType.EQUIPMENT_MISC) && (_searchCollection == 0))
		{
			// Equipment - Misc
			return item.getItem().isEquipable() && !item.getItem().isWeapon() && !isEquipmentArmor(item.getItem()) && !isAccessory(item.getItem());
		}
		
		/**
		 * Artifact
		 */
		if ((_itemType == StoreItemType.ARTIFACT) && (_itemSubtype == StoreSubItemType.ALL) && (_searchCollection == 0))
		{
			// Artifact - All
			return isArtifact(item.getItem());
		}
		
		if ((_itemType == StoreItemType.ARTIFACT) && (_itemSubtype == StoreSubItemType.ARTIFACT_ATTACK) && (_searchCollection == 0))
		{
			// Artifact - Attack
			return isArtifact(item.getItem()) && (item.getItem().getArtifactSlot() == 1);
		}
		
		if ((_itemType == StoreItemType.ARTIFACT) && (_itemSubtype == StoreSubItemType.ARTIFACT_DEFENSE) && (_searchCollection == 0))
		{
			// Artifact - Defense
			return isArtifact(item.getItem()) && (item.getItem().getArtifactSlot() == 2);
		}
		
		if ((_itemType == StoreItemType.ARTIFACT) && (_itemSubtype == StoreSubItemType.ARTIFACT_SUPPORT) && (_searchCollection == 0))
		{
			// Artifact - Support
			return isArtifact(item.getItem()) && (item.getItem().getArtifactSlot() == 3);
		}
		
		if ((_itemType == StoreItemType.ARTIFACT) && (_itemSubtype == StoreSubItemType.ARTIFACT_BALANCE) && (_searchCollection == 0))
		{
			// Artifact - Balance
			return isArtifact(item.getItem()) && (item.getItem().getArtifactSlot() == 4);
		}
		
		/**
		 * Scrolls
		 */
		if ((_itemType == StoreItemType.ENHANCEMENT_OR_EXPING) && (_itemSubtype == StoreSubItemType.ALL) && (_searchCollection == 0))
		{
			// Scrolls - All
			return isEnchantScroll(item.getItem()) && !isEnchantSupport(item.getItem());
		}
		
		if ((_itemType == StoreItemType.ENHANCEMENT_OR_EXPING) && (_itemSubtype == StoreSubItemType.ENCHANT_SCROLL) && (_searchCollection == 0))
		{
			// Scrolls - Standard
			return isStandardEnchantScroll(item.getItem());
		}
		
		if ((_itemType == StoreItemType.ENHANCEMENT_OR_EXPING) && (_itemSubtype == StoreSubItemType.BLESS_ENCHANT_SCROLL) && (_searchCollection == 0))
		{
			// Scrolls - Blessed scrolls
			return isBlessedEnchantScroll(item.getItem());
		}
		
		if ((_itemType == StoreItemType.ENHANCEMENT_OR_EXPING) && (_itemSubtype == StoreSubItemType.MULTI_ENCHANT_SCROLL) && (_searchCollection == 0))
		{
			// Scrolls - Giant's Scrolls
			return isGiantEnchantScroll(item.getItem());
		}
		
		if ((_itemType == StoreItemType.ENHANCEMENT_OR_EXPING) && (_itemSubtype == StoreSubItemType.ANCIENT_ENCHANT_SCROLL) && (_searchCollection == 0))
		{
			// Scrolls - Other scrolls
			return isEnchantScroll(item.getItem()) && !isStandardEnchantScroll(item.getItem()) && !isBlessedEnchantScroll(item.getItem()) && !isGiantEnchantScroll(item.getItem()) && !isEnchantSupport(item.getItem());
		}
		
		/**
		 * Supplies
		 */
		if ((_itemType == StoreItemType.SUPPLIES) && (_itemSubtype == StoreSubItemType.ALL) && (_searchCollection == 0))
		{
			// Supplies - All
			return isSpiritshot(item.getItem()) || isSoulshot(item.getItem()) || isBuffSupply(item.getItem());
		}
		
		if ((_itemType == StoreItemType.SUPPLIES) && (_itemSubtype == StoreSubItemType.SPIRITSHOT) && (_searchCollection == 0))
		{
			// Supplies - Spiritshots
			return isSpiritshot(item.getItem());
		}
		
		if ((_itemType == StoreItemType.SUPPLIES) && (_itemSubtype == StoreSubItemType.SOULSHOT) && (_searchCollection == 0))
		{
			// Supplies - Soulshots
			return isSoulshot(item.getItem());
		}
		
		if ((_itemType == StoreItemType.SUPPLIES) && (_itemSubtype == StoreSubItemType.BUFF) && (_searchCollection == 0))
		{
			// Supplies - Buffs
			return isBuffSupply(item.getItem());
		}
		
		/**
		 * Misc.
		 */
		if ((_itemType == StoreItemType.GROCERY_OR_COLLECTION_MISC) && (_itemSubtype == StoreSubItemType.ALL) && (_searchCollection == 0))
		{
			// Misc. - All
			return isLifeStone(item.getItem()) || isDye(item.getItem()) || isEnchantSupport(item.getItem()) || isMiscItem(item.getItem());
		}
		
		if ((_itemType == StoreItemType.GROCERY_OR_COLLECTION_MISC) && (_itemSubtype == StoreSubItemType.LIFE_STONE) && (_searchCollection == 0))
		{
			// Misc. - Augmenting
			return isLifeStone(item.getItem());
		}
		
		if ((_itemType == StoreItemType.GROCERY_OR_COLLECTION_MISC) && (_itemSubtype == StoreSubItemType.DYES) && (_searchCollection == 0))
		{
			// Misc. - Dyes
			return isDye(item.getItem());
		}
		
		if ((_itemType == StoreItemType.GROCERY_OR_COLLECTION_MISC) && (_itemSubtype == StoreSubItemType.INC_ENCHANT_PROP) && (_searchCollection == 0))
		{
			// Misc. - Boosts
			return isEnchantSupport(item.getItem());
		}
		
		if ((_itemType == StoreItemType.GROCERY_OR_COLLECTION_MISC) && (_itemSubtype == StoreSubItemType.ETC_SUBTYPE) && (_searchCollection == 0))
		{
			// Misc. - Misc.
			return isMiscItem(item.getItem());
		}
		
		/**
		 * Collections
		 */
		if ((_itemType == StoreItemType.ALL) && (_searchCollection == 1))
		{
			// Collections - All
			return isCollection(item.getItem());
		}
		
		if ((_itemType == StoreItemType.EQUIPMENT) && (_searchCollection == 1))
		{
			// Collections - Equipement
			return isCollectionEquipement(item.getItem());
		}
		
		if ((_itemType == StoreItemType.ENHANCEMENT_OR_EXPING) && (_searchCollection == 1))
		{
			// Collections - Enchanted Item
			return isCollectionEnchanted(item.getItem());
		}
		
		if ((_itemType == StoreItemType.GROCERY_OR_COLLECTION_MISC) && (_searchCollection == 1))
		{
			// Collections - Misc
			return isCollectionMisc(item.getItem());
		}
		
		// Only the All tab shows everything. An unmapped tab shows nothing rather than every item.
		return (_itemType == StoreItemType.ALL) && (_searchCollection == 0);
	}
	
	/**
	 * Checks if the given item is an artifact.<br>
	 * Both artifact cards and artifact books are included.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is an artifact, {@code false} otherwise.
	 */
	private boolean isArtifact(ItemTemplate item)
	{
		final BodyPart bodyPart = item.getBodyPart();
		return (bodyPart == BodyPart.ARTIFACT) || (bodyPart == BodyPart.ARTIFACT_BOOK);
	}
	
	/**
	 * Checks if the given item is a soulshot.<br>
	 * Both player and servitor soulshots are included.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a soulshot, {@code false} otherwise.
	 */
	private boolean isSoulshot(ItemTemplate item)
	{
		final ActionType action = item.getDefaultAction();
		return (action == ActionType.SOULSHOT) || (action == ActionType.SUMMON_SOULSHOT);
	}
	
	/**
	 * Checks if the given item is a spiritshot.<br>
	 * Both player and servitor spiritshots are included.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a spiritshot, {@code false} otherwise.
	 */
	private boolean isSpiritshot(ItemTemplate item)
	{
		final ActionType action = item.getDefaultAction();
		return (action == ActionType.SPIRITSHOT) || (action == ActionType.SUMMON_SPIRITSHOT);
	}
	
	/**
	 * Checks if the given item is a consumable that grants a buff.<br>
	 * Potions and scrolls other than enchant scrolls are included.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a buff consumable, {@code false} otherwise.
	 */
	private boolean isBuffSupply(ItemTemplate item)
	{
		return !item.isEquipable() && (item.isPotion() || (item.isScroll() && !isEnchantScroll(item)));
	}
	
	/**
	 * Checks if the given item is an {@code EtcItem} of one of the supplied enchant types.
	 * @param item The {@code ItemTemplate} to check.
	 * @param types The {@code EtcItemType} values that satisfy the check.
	 * @return {@code true} if the item type is in the given set, {@code false} otherwise.
	 */
	private boolean isEnchantScrollType(ItemTemplate item, Set<EtcItemType> types)
	{
		return (item instanceof EtcItem) && types.contains(((EtcItem) item).getItemType());
	}
	
	/**
	 * Checks if the given item is a standard enchant scroll.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a standard enchant scroll, {@code false} otherwise.
	 */
	private boolean isStandardEnchantScroll(ItemTemplate item)
	{
		return isEnchantScrollType(item, STANDARD_ENCHANT_SCROLL_TYPES);
	}
	
	/**
	 * Checks if the given item is a blessed enchant scroll.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a blessed enchant scroll, {@code false} otherwise.
	 */
	private boolean isBlessedEnchantScroll(ItemTemplate item)
	{
		return isEnchantScrollType(item, BLESSED_ENCHANT_SCROLL_TYPES);
	}
	
	/**
	 * Checks if the given item is a giant or multi enchant scroll.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a giant or multi enchant scroll, {@code false} otherwise.
	 */
	private boolean isGiantEnchantScroll(ItemTemplate item)
	{
		return isEnchantScrollType(item, GIANT_ENCHANT_SCROLL_TYPES);
	}
	
	/**
	 * Checks if the given item increases enchant probability.<br>
	 * These are the support items the client groups under its Boosts sub-tab.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item increases enchant probability, {@code false} otherwise.
	 */
	private boolean isEnchantSupport(ItemTemplate item)
	{
		return isEnchantScrollType(item, INC_ENCHANT_PROP_TYPES);
	}
	
	/**
	 * Checks if the given item belongs to the client Misc. tab.<br>
	 * It is the leftover category for items no other tab claims.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item belongs to the Misc. tab, {@code false} otherwise.
	 */
	private boolean isMiscItem(ItemTemplate item)
	{
		return isGroceryMisc(item) && !isSoulshot(item) && !isSpiritshot(item) && !isEnchantSupport(item);
	}
	
	/**
	 * Checks if the given item is a piece of armor.<br>
	 * It verifies that the {@code ItemTemplate} is classified as armor.<br>
	 * It also checks if it belongs to specific body parts like head, chest, or legs.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a valid piece of armor, {@code false} otherwise.
	 */
	private boolean isEquipmentArmor(ItemTemplate item)
	{
		if (!item.isArmor())
		{
			return false;
		}
		
		final BodyPart bodyPart = item.getBodyPart();
		return (bodyPart == BodyPart.CHEST) //
			|| (bodyPart == BodyPart.FULL_ARMOR) //
			|| (bodyPart == BodyPart.HEAD) //
			|| (bodyPart == BodyPart.LEGS) //
			|| (bodyPart == BodyPart.FEET) //
			|| (bodyPart == BodyPart.GLOVES);
	}
	
	/**
	 * Checks if the given item is an accessory.<br>
	 * It verifies that the item is armor and matches specific body parts.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is an accessory, {@code false} otherwise.
	 */
	private boolean isAccessory(ItemTemplate item)
	{
		if (!item.isArmor())
		{
			return false;
		}
		
		final BodyPart bodyPart = item.getBodyPart();
		return ((bodyPart == BodyPart.L_BRACELET) //
			|| (bodyPart == BodyPart.R_BRACELET) //
			|| (bodyPart == BodyPart.BROOCH) //
			|| (bodyPart == BodyPart.R_FINGER) //
			|| (bodyPart == BodyPart.L_FINGER) //
			|| (bodyPart == BodyPart.LR_FINGER) //
			|| (bodyPart == BodyPart.NECK) //
			|| (bodyPart == BodyPart.R_EAR) //
			|| (bodyPart == BodyPart.L_EAR) //
			|| (bodyPart == BodyPart.LR_EAR));
	}
	
	/**
	 * Checks if the given {@code ItemTemplate} is an enchantment scroll.<br>
	 * It verifies if the item is an {@code EtcItem}.<br>
	 * It also checks if its handler class name matches {@code EnchantScrolls}.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is an enchantment scroll, {@code false} otherwise.
	 */
	private boolean isEnchantScroll(ItemTemplate item)
	{
		if (!(item instanceof EtcItem))
		{
			return false;
		}
		
		final IItemHandler ih = ItemHandler.getInstance().getHandler((EtcItem) item);
		return (ih != null) && ih.getClass().getSimpleName().equals("EnchantScrolls");
	}
	
	/**
	 * Checks if the given item is a crystal.<br>
	 * This method verifies if the item ID exists in the {@code EnsoulData} collection.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a crystal, {@code false} otherwise.
	 */
	private boolean isCrystal(ItemTemplate item)
	{
		return EnsoulData.getInstance().getStone(item.getId()) != null;
	}
	
	/**
	 * Checks if the given item is a Life Stone.<br>
	 * This method verifies the item against the {@code AugmentationData} collection.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a Life Stone, {@code false} otherwise.
	 */
	private boolean isLifeStone(ItemTemplate item)
	{
		return AugmentationData.getInstance().hasAugmentation(item.getId());
	}
	
	/**
	 * Checks if the given item is a dye.<br>
	 * This method verifies if the item exists in the {@code HennaData} collection.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a dye, {@code false} otherwise.
	 */
	private boolean isDye(ItemTemplate item)
	{
		return HennaData.getInstance().getHenna(item.getId()) != null;
	}
	
	/**
	 * Checks if the given item is a spellbook.<br>
	 * This method looks for the prefix "Spellbook: " in the item name.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if it is a spellbook, {@code false} otherwise.
	 */
	private boolean isSpellBook(ItemTemplate item)
	{
		return item.getName().contains("Spellbook: ");
	}
	
	/**
	 * Checks if the given {@code ItemTemplate} is a miscellaneous enhancement item.<br>
	 * This method verifies if the item ID falls within the specific range of 91031 to 91038.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a miscellaneous enhancement, {@code false} otherwise.
	 */
	private boolean isEnhancementMisc(ItemTemplate item)
	{
		return (item.getId() >= 91031) && (item.getId() <= 91038);
	}
	
	/**
	 * Checks if the given item is an enhancement type.<br>
	 * This includes scrolls, crystals, life stones, dyes, spell books, and miscellaneous enhancements.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is an enhancement item, {@code false} otherwise.
	 */
	private boolean isEnhancementItem(ItemTemplate item)
	{
		return isEnchantScroll(item) || isCrystal(item) || isLifeStone(item) || isDye(item) || isSpellBook(item) || isEnhancementMisc(item);
	}
	
	/**
	 * Checks if the given item is a ticket.<br>
	 * This method verifies specific IDs for ticket items.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a ticket, {@code false} otherwise.
	 */
	private boolean isTicket(ItemTemplate item)
	{
		return (item.getId() == 90045) || (item.getId() == 91462) || (item.getId() == 91463) || (item.getId() == 91972) || (item.getId() == 93903);
	}
	
	/**
	 * Checks if the given {@code ItemTemplate} is a pack or a craftable item.<br>
	 * It verifies specific IDs and checks if the item belongs to the {@code ExtractableItems} handler.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a pack or craftable, {@code false} otherwise.
	 */
	private boolean isPackOrCraft(ItemTemplate item)
	{
		final int itemId = item.getId();
		if ((itemId == 92477) || (itemId == 91462) || (itemId == 92478) || (itemId == 92479) || (itemId == 92480) || (itemId == 92481) || (itemId == 49756) || (itemId == 93906) || (itemId == 93907) || (itemId == 93908) || (itemId == 93909) || (itemId == 93910) || (itemId == 91076))
		{
			return true;
		}
		
		if (!(item instanceof EtcItem))
		{
			return false;
		}
		
		final IItemHandler ih = ItemHandler.getInstance().getHandler((EtcItem) item);
		return (ih != null) && ih.getClass().getSimpleName().equals("ExtractableItems");
	}
	
	/**
	 * Checks if an item belongs to the miscellaneous grocery category.<br>
	 * This acts as a fallback for items that do not fit into any other specific categories.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a miscellaneous grocery, {@code false} otherwise.
	 */
	private boolean isGroceryMisc(ItemTemplate item)
	{
		// Kinda fallback trash category to ensure no skipping any items.
		return !item.isEquipable() && !isEnhancementItem(item) && !isCollection(item) && !item.isPotion() && !item.isScroll() && !isTicket(item) && !isPackOrCraft(item);
	}
	
	/**
	 * Checks if the given {@code ItemTemplate} belongs to a collection.<br>
	 * It iterates through all registered collections in {@code CollectionData}.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is part of any collection, otherwise {@code false}.
	 */
	private boolean isCollection(ItemTemplate item)
	{
		for (CollectionDataHolder collectionHolder : CollectionData.getInstance().getCollections())
		{
			for (ItemEnchantHolder itemData : collectionHolder.getItems())
			{
				if (itemData.getId() == item.getId())
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if an item belongs to a collection and can be equipped.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is both a collection item and equippable, {@code false} otherwise.
	 */
	private boolean isCollectionEquipement(ItemTemplate item)
	{
		return isCollection(item) && item.isEquipable();
	}
	
	/**
	 * Checks if the given item belongs to a collection and is a spellbook.<br>
	 * This method returns {@code true} only if both conditions are met.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a collection spellbook, {@code false} otherwise.
	 */
	private boolean isCollectionEnchanted(ItemTemplate item)
	{
		return isCollection(item) && item.getName().contains("Spellbook: ");
	}
	
	/**
	 * Checks if the given {@code ItemTemplate} belongs to a specific collection of miscellaneous items.<br>
	 * This method verifies if the item ID falls within the range of {@code 93906} to {@code 93910}.
	 * @param item The {@code ItemTemplate} to check.
	 * @return {@code true} if the item is a collection miscellaneous item, {@code false} otherwise.
	 */
	private boolean isCollectionMisc(ItemTemplate item)
	{
		return (item.getId() >= 93906) && (item.getId() <= 93910);
	}
	
	private enum StoreType
	{
		SELL((byte) 0x00),
		BUY((byte) 0x01),
		ALL((byte) 0x03);
		
		private final byte _storeType;
		
		StoreType(byte storeType)
		{
			_storeType = storeType;
		}
		
		private static final Map<Integer, StoreType> BY_ID = new HashMap<>(values().length);
		static
		{
			for (StoreType storeType : values())
			{
				BY_ID.put((int) storeType.getValue(), storeType);
			}
		}
		
		public static StoreType findById(int id)
		{
			return BY_ID.get(id);
		}
		
		public byte getValue()
		{
			return _storeType;
		}
	}
	
	private enum StoreItemType
	{
		ALL((byte) 0xFF),
		EQUIPMENT((byte) 0x00),
		ARTIFACT((byte) 0x01),
		ENHANCEMENT_OR_EXPING((byte) 0x02),
		SUPPLIES((byte) 0x03),
		GROCERY_OR_COLLECTION_MISC((byte) 0x04);
		
		private final byte _storeItemType;
		
		StoreItemType(byte storeItemType)
		{
			_storeItemType = storeItemType;
		}
		
		private static final Map<Integer, StoreItemType> BY_ID = new HashMap<>(values().length);
		static
		{
			for (StoreItemType storeItemType : values())
			{
				BY_ID.put((int) storeItemType.getValue(), storeItemType);
			}
		}
		
		public static StoreItemType findById(int id)
		{
			return BY_ID.get(id);
		}
		
		public byte getValue()
		{
			return _storeItemType;
		}
	}
	
	private enum StoreSubItemType
	{
		ALL((byte) 0xFF),
		WEAPON((byte) 0x00),
		ARMOR((byte) 0x01),
		ACCESSORY((byte) 0x02),
		EQUIPMENT_MISC((byte) 0x03),
		ENCHANT_SCROLL((byte) 0x08),
		BLESS_ENCHANT_SCROLL((byte) 0x09),
		MULTI_ENCHANT_SCROLL((byte) 0x0A),
		ANCIENT_ENCHANT_SCROLL((byte) 0x0B),
		SPIRITSHOT((byte) 0x0C),
		SOULSHOT((byte) 0x0D),
		BUFF((byte) 0x0E),
		ARTIFACT_ATTACK((byte) 0x04),
		ARTIFACT_DEFENSE((byte) 0x05),
		ARTIFACT_SUPPORT((byte) 0x06),
		ARTIFACT_BALANCE((byte) 0x07),
		INC_ENCHANT_PROP((byte) 0x17),
		ETC_SUBTYPE((byte) 0x1A),
		LIFE_STONE((byte) 0x0F),
		DYES((byte) 0x10),
		CRYSTAL((byte) 0x11),
		SPELLBOOK((byte) 0x12),
		ENHANCEMENT_MISC((byte) 0x13),
		POTION_SCROLL((byte) 0x14),
		TICKET((byte) 0x15),
		PACK_CRAFT((byte) 0x16),
		GROCERY_MISC((byte) 0x18);
		
		private final byte _storeSubItemType;
		
		StoreSubItemType(byte storeSubItemType)
		{
			_storeSubItemType = storeSubItemType;
		}
		
		private static final Map<Integer, StoreSubItemType> BY_ID = new HashMap<>(values().length);
		static
		{
			for (StoreSubItemType storeSubItemType : values())
			{
				BY_ID.put((int) storeSubItemType.getValue(), storeSubItemType);
			}
		}
		
		public static StoreSubItemType findById(int id)
		{
			return BY_ID.get(id);
		}
		
		public byte getValue()
		{
			return _storeSubItemType;
		}
	}
	
	public static class ShopItem
	{
		private final TradeItem _item;
		private final Player _owner;
		private final PrivateStoreType _storeType;
		
		public ShopItem(TradeItem item, Player owner, PrivateStoreType storeType)
		{
			_item = item;
			_owner = owner;
			_storeType = storeType;
		}
		
		public long getCount()
		{
			return _item.getCount();
		}
		
		public ItemInfo getItemInfo()
		{
			return new ItemInfo(_item);
		}
		
		public Player getOwner()
		{
			return _owner;
		}
		
		public PrivateStoreType getStoreType()
		{
			return _storeType;
		}
		
		public long getPrice()
		{
			return _item.getPrice();
		}
	}
}
