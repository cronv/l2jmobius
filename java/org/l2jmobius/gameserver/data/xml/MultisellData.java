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
package org.l2jmobius.gameserver.data.xml;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import org.l2jmobius.commons.util.IXmlReader;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.holders.MultisellEntryHolder;
import org.l2jmobius.gameserver.data.holders.MultisellListHolder;
import org.l2jmobius.gameserver.data.holders.PreparedMultisellListHolder;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enchant.EnchantItemGroup;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.enums.SpecialItemType;
import org.l2jmobius.gameserver.entity.item.holders.ItemChanceHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.network.serverpackets.MultiSellList;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the loading and parsing of multisell data from XML files.<br>
 * It provides the configuration for items that can be sold in bulk by NPCs.<br>
 * The parsed data is used to populate {@code MultisellListHolder} objects.
 */
public class MultisellData implements IXmlReader
{
	private static final Logger LOGGER = Logger.getLogger(MultisellData.class.getName());
	
	private static final Pattern XML_FILE_PATTERN = Pattern.compile("\\d+\\.xml");
	
	public static final int PAGE_SIZE = 40;
	
	private final Map<Integer, MultisellListHolder> _multisells = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new instance of {@code MultisellData}.<br>
	 * This constructor calls the {@code load()} method to populate data.
	 */
	protected MultisellData()
	{
		load();
	}
	
	/**
	 * Loads all multisell data from the configuration files.<br>
	 * This method clears existing data and parses the default directory.<br>
	 * It also loads custom data if {@code GeneralConfig.CUSTOM_MULTISELL_LOAD} is true.
	 */
	@Override
	public void load()
	{
		_multisells.clear();
		parseDatapackDirectory("data/multisell", false);
		parseDatapackDirectory("data/multisell/items", false);
		if (GeneralConfig.CUSTOM_MULTISELL_LOAD)
		{
			parseDatapackDirectory("data/multisell/custom", false);
		}
		
		LOGGER.info(getClass().getSimpleName() + ": Loaded " + _multisells.size() + " multisell lists.");
	}
	
	/**
	 * Parses a {@code Document} from an XML file to populate multisell data.<br>
	 * It extracts items, ingredients, and production chances for each list.<br>
	 * The method also handles NPC restrictions and price validation.
	 * @param document The {@code Document} object containing the parsed XML content.
	 * @param file The {@code File} object representing the source of the data.
	 */
	@Override
	public void parseDocument(Document document, File file)
	{
		final EnchantItemGroup magicWeaponGroup = EnchantItemGroupsData.getInstance().getItemGroup("MAGE_WEAPON_GROUP");
		final int magicWeaponGroupMax = magicWeaponGroup != null ? magicWeaponGroup.getMaximumEnchant() : -2;
		final EnchantItemGroup weapongroup = EnchantItemGroupsData.getInstance().getItemGroup("FIGHTER_WEAPON_GROUP");
		final int weaponGroupMax = weapongroup != null ? weapongroup.getMaximumEnchant() : -2;
		final EnchantItemGroup fullArmorGroup = EnchantItemGroupsData.getInstance().getItemGroup("FULL_ARMOR_GROUP");
		final int fullArmorGroupMax = fullArmorGroup != null ? fullArmorGroup.getMaximumEnchant() : -2;
		final EnchantItemGroup armorGroup = EnchantItemGroupsData.getInstance().getItemGroup("ARMOR_GROUP");
		final int armorGroupMax = armorGroup != null ? armorGroup.getMaximumEnchant() : -2;
		
		try
		{
			forEach(document, "list", listNode ->
			{
				final StatSet set = new StatSet(parseAttributes(listNode));
				final String fileName = file.getName();
				final int listId = Integer.parseInt(fileName.substring(0, fileName.length() - 4));
				final List<MultisellEntryHolder> entries = new ArrayList<>(listNode.getChildNodes().getLength());
				final AtomicInteger entryCounter = new AtomicInteger();
				
				forEach(listNode, itemNode ->
				{
					if ("item".equalsIgnoreCase(itemNode.getNodeName()))
					{
						long totalPrice = 0;
						int lastIngredientId = 0;
						long lastIngredientCount = 0;
						entryCounter.incrementAndGet();
						
						final List<ItemChanceHolder> ingredients = new ArrayList<>(1);
						final List<ItemChanceHolder> products = new ArrayList<>(1);
						final MultisellEntryHolder entry = new MultisellEntryHolder(ingredients, products);
						for (Node d = itemNode.getFirstChild(); d != null; d = d.getNextSibling())
						{
							final String dName = d.getNodeName();
							if ("ingredient".equalsIgnoreCase(dName))
							{
								final NamedNodeMap dAttrs = d.getAttributes();
								final int id = parseInteger(dAttrs, "id");
								final long count = parseLong(dAttrs, "count");
								final byte enchantmentLevel = parseByte(dAttrs, "enchantmentLevel", (byte) 0);
								final Boolean maintainIngredient = parseBoolean(dAttrs, "maintainIngredient", false);
								final ItemChanceHolder ingredient = new ItemChanceHolder(id, 0, count, enchantmentLevel, maintainIngredient);
								if (itemExists(ingredient))
								{
									ingredients.add(ingredient);
									
									lastIngredientId = id;
									lastIngredientCount = count;
								}
								else
								{
									LOGGER.warning("Invalid ingredient id or count for itemId: " + ingredient.getId() + ", count: " + ingredient.getCount() + " in list: " + listId);
									continue;
								}
							}
							else if ("production".equalsIgnoreCase(dName))
							{
								final NamedNodeMap dAttrs = d.getAttributes();
								final int id = parseInteger(dAttrs, "id");
								final long count = parseLong(dAttrs, "count");
								final double chance = parseDouble(dAttrs, "chance", Double.NaN);
								byte enchantmentLevel = parseByte(dAttrs, "enchantmentLevel", (byte) 0);
								if (enchantmentLevel > 0)
								{
									final ItemTemplate item = ItemData.getInstance().getTemplate(id);
									if (item != null)
									{
										if (item.isWeapon())
										{
											enchantmentLevel = (byte) Math.min(enchantmentLevel, item.isMagicWeapon() ? magicWeaponGroupMax > -2 ? magicWeaponGroupMax : enchantmentLevel : weaponGroupMax > -2 ? weaponGroupMax : enchantmentLevel);
										}
										else if (item.isArmor())
										{
											enchantmentLevel = (byte) Math.min(enchantmentLevel, item.getBodyPart() == BodyPart.FULL_ARMOR ? fullArmorGroupMax > -2 ? fullArmorGroupMax : enchantmentLevel : armorGroupMax > -2 ? armorGroupMax : enchantmentLevel);
										}
									}
								}
								
								final ItemChanceHolder product = new ItemChanceHolder(id, chance, count, enchantmentLevel);
								if (itemExists(product))
								{
									// Check chance only of items that have set chance. Items without chance (NaN) are used for displaying purposes.
									if ((!Double.isNaN(chance) && (chance < 0)) || (chance > 100))
									{
										LOGGER.warning("Invalid chance for itemId: " + product.getId() + ", count: " + product.getCount() + ", chance: " + chance + " in list: " + listId);
										continue;
									}
									
									products.add(product);
									
									final ItemTemplate item = ItemData.getInstance().getTemplate(id);
									if (item != null)
									{
										if (chance > 0)
										{
											totalPrice += ((item.getReferencePrice() / 2) * count) * (chance / 100);
										}
										else
										{
											totalPrice += ((item.getReferencePrice() / 2) * count);
										}
									}
								}
								else
								{
									LOGGER.warning("Invalid product id or count for itemId: " + product.getId() + ", count: " + product.getCount() + " in list: " + listId);
									continue;
								}
							}
						}
						
						final double totalChance = products.stream().filter(i -> !Double.isNaN(i.getChance())).mapToDouble(ItemChanceHolder::getChance).sum();
						if (totalChance > 100)
						{
							LOGGER.warning("Products' total chance of " + totalChance + "% exceeds 100% for list: " + listId + " at entry " + entries.size() + 1 + ".");
						}
						
						// Check if buy price is lower than sell price.
						// Only applies when there is only one ingredient and it is adena.
						if (GeneralConfig.CORRECT_PRICES && (ingredients.size() == 1) && (lastIngredientId == 57) && (lastIngredientCount < totalPrice))
						{
							LOGGER.warning("Buy price " + lastIngredientCount + " is less than sell price " + totalPrice + " at entry " + entryCounter.intValue() + " of multisell " + listId + ".");
							
							// Adjust price.
							final ItemChanceHolder ingredient = new ItemChanceHolder(57, 0, totalPrice, (byte) 0, ingredients.get(0).isMaintainIngredient());
							ingredients.clear();
							ingredients.add(ingredient);
						}
						
						entries.add(entry);
					}
					else if ("npcs".equalsIgnoreCase(itemNode.getNodeName()))
					{
						// Initialize NPCs with the size of child nodes.
						final Set<Integer> allowNpc = new HashSet<>(itemNode.getChildNodes().getLength());
						forEach(itemNode, n -> "npc".equalsIgnoreCase(n.getNodeName()), n -> allowNpc.add(Integer.parseInt(n.getTextContent())));
						
						// Add npcs to stats set.
						set.set("allowNpc", allowNpc);
					}
				});
				
				set.set("listId", listId);
				set.set("entries", entries);
				
				if (_multisells.put(listId, new MultisellListHolder(set)) != null)
				{
					LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Duplicate multisell with id: " + listId);
				}
			});
		}
		catch (Exception e)
		{
			LOGGER.log(Level.SEVERE, getClass().getSimpleName() + ": Error in file " + file, e);
		}
	}
	
	/**
	 * Checks if the provided {@code File} is a valid XML file.<br>
	 * It verifies that the file exists and matches the required naming pattern.
	 * @param file The {@code File} object to validate.
	 * @return {@code true} if the file is valid, otherwise {@code false}.
	 */
	@Override
	public boolean isValidXmlFile(File file)
	{
		return (file != null) && file.isFile() && XML_FILE_PATTERN.matcher(file.getName().toLowerCase()).matches();
	}
	
	/**
	 * Processes and sends a multisell list to a specific player.<br>
	 * It validates the NPC permissions and calculates multipliers before sending packets.<br>
	 * The method handles pagination for large lists of items.
	 * @param listId The unique identifier for the multisell list template.
	 * @param player The {@code Player} receiving the multisell data.
	 * @param npc The {@code Npc} associated with this transaction, if any.
	 * @param inventoryOnly If {@code true}, only items in the player's inventory are considered.
	 * @param ingredientMultiplierValue The multiplier for ingredients; uses template default if {@code Double.NaN}.
	 * @param productMultiplierValue The multiplier for products; uses template default if {@code Double.NaN}.
	 * @param type The packet type used to send the multisell list.
	 */
	public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly, double ingredientMultiplierValue, double productMultiplierValue, int type)
	{
		final MultisellListHolder template = _multisells.get(listId);
		if (template == null)
		{
			LOGGER.warning("Can't find list id: " + listId + " requested by player: " + player.getName() + ", npcId: " + (npc != null ? npc.getId() : 0));
			return;
		}
		
		if (!template.isNpcAllowed(-1) && ((npc == null) || !template.isNpcAllowed(npc.getId())))
		{
			if (player.isGM())
			{
				player.sendMessage("Multisell " + listId + " is restricted. Under current conditions cannot be used. Only GMs are allowed to use it.");
			}
			else
			{
				LOGGER.warning(getClass().getSimpleName() + ": " + player + " attempted to open multisell " + listId + " from npc " + npc + " which is not allowed!");
				return;
			}
		}
		
		// Check if ingredient/product multipliers are set, if not, set them to the template value.
		final double ingredientMultiplier = (Double.isNaN(ingredientMultiplierValue) ? template.getIngredientMultiplier() : ingredientMultiplierValue);
		final double productMultiplier = (Double.isNaN(productMultiplierValue) ? template.getProductMultiplier() : productMultiplierValue);
		final PreparedMultisellListHolder list = new PreparedMultisellListHolder(template, inventoryOnly, player.getInventory(), npc, ingredientMultiplier, productMultiplier);
		int index = 0;
		do
		{
			// Send list at least once even if size = 0.
			player.sendPacket(new MultiSellList(player, list, index, type));
			index += PAGE_SIZE;
		}
		while (index < list.getEntries().size());
		
		player.setMultiSell(list);
	}
	
	/**
	 * This method separates and sends a multisell list to a specific {@code Player}.<br>
	 * It uses the default multiplier values for the operation.
	 * @param listId The unique identifier of the multisell list.
	 * @param player The {@code Player} receiving the data.
	 * @param npc The {@code Npc} associated with the multisell list.
	 * @param inventoryOnly If {@code true}, only items in the inventory are considered.
	 */
	public void separateAndSend(int listId, Player player, Npc npc, boolean inventoryOnly)
	{
		separateAndSend(listId, player, npc, inventoryOnly, Double.NaN, Double.NaN, 0);
	}
	
	/**
	 * Checks if an item exists based on its holder data.<br>
	 * It verifies the item type and the current count.
	 * @param holder The {@code ItemHolder} to check.
	 * @return {@code true} if the item is valid and exists, otherwise {@code false}.
	 */
	private boolean itemExists(ItemHolder holder)
	{
		final int holderId = holder.getId();
		final SpecialItemType specialItem = SpecialItemType.getByClientId(holderId);
		if (specialItem != null)
		{
			return true;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(holderId);
		return (template != null) && (template.isStackable() ? (holder.getCount() >= 1) : (holder.getCount() == 1));
	}
	
	/**
	 * Retrieves a {@code MultisellListHolder} based on the provided ID.<br>
	 * This method looks up the data in the internal collection.
	 * @param id The unique identifier for the multisell list.
	 * @return The {@code MultisellListHolder} associated with the given ID, or {@code null} if not found.
	 */
	public MultisellListHolder getMultisell(int id)
	{
		return _multisells.get(id);
	}
	
	/**
	 * Retrieves the singleton instance of {@code MultisellData}.<br>
	 * This method provides global access to the loaded multisell data.
	 * @return The single instance of {@code MultisellData}.
	 */
	public static MultisellData getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final MultisellData INSTANCE = new MultisellData();
	}
}
