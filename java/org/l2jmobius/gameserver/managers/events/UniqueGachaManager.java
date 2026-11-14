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
package org.l2jmobius.gameserver.managers.events;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.commons.database.DatabaseFactory;
import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.ItemData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.enums.UniqueGachaRank;
import org.l2jmobius.gameserver.entity.item.holders.GachaItemHolder;
import org.l2jmobius.gameserver.entity.item.holders.GachaItemTimeStampHolder;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.GachaWarehouse;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.managers.ScriptManager;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogin;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerLogout;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.script.Quest;
import org.l2jmobius.gameserver.network.serverpackets.gacha.UniqueGachaSidebarInfo;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Manages the logic and data for the unique gacha system.<br>
 * This class handles item rewards, player progress, and database interactions related to gacha events.
 */
public class UniqueGachaManager
{
	private static final Logger LOGGER = Logger.getLogger(UniqueGachaManager.class.getName());
	
	/**
	 * @implNote 0.000001 MAXIMAL OF MINIMUM VALUE
	 */
	public static final int MINIMUM_CHANCE = 1_000_000;
	public static final int MINIMUM_CHANCE_AFTER_DOT = 6;
	public static final String GACHA_PLAYER_VARIABLE = "GACHA_ROLL_COUNT";
	public static final String GACHA_LOCK_PLAYER_VARIABLE = "UniqueGachaRoll";
	private static final int MAX_HISTORY_ITEMS = 100;
	
	private static final List<GachaItemTimeStampHolder> EMPTY_LINKED_LIST = new ArrayList<>();
	
	private final Set<GachaItemHolder> _visibleItems = new HashSet<>();
	private final Map<UniqueGachaRank, Set<GachaItemHolder>> _rewardItems = new HashMap<>();
	private final Map<UniqueGachaRank, Integer> _rewardChance = new HashMap<>();
	private final Map<Integer, Long> _gameCosts = new HashMap<>();
	
	private final Map<Player, GachaWarehouse> _temporaryWarehouse = new HashMap<>();
	private final Map<Integer, List<GachaItemTimeStampHolder>> _gachaHistory = new HashMap<>();
	
	private boolean _isActive;
	private long _activeUntilPeriod;
	private int _guaranteeRoll;
	private boolean _showProbability;
	private int _currencyItemId;
	private int _totalRewardCount = 0;
	private int _totalChanceSumma = 0;
	
	/**
	 * Initializes the {@code UniqueGachaManager}.<br>
	 * This constructor restores gacha history and sets up periodic database tasks.<br>
	 * It also registers login and logout event listeners for players.
	 */
	public UniqueGachaManager()
	{
		restoreGachaHistory(_gachaHistory);
		ThreadPool.scheduleAtFixedRate(() -> storeGachaHistory(_gachaHistory), TimeUnit.MINUTES.toMillis(5), TimeUnit.MINUTES.toMillis(5));
		final ListenersContainer Global = Containers.Global();
		Global.addListener(new ConsumerEventListener(Global, EventType.ON_PLAYER_LOGIN, (OnPlayerLogin event) -> onPlayerLogin(event), this));
		Global.addListener(new ConsumerEventListener(Global, EventType.ON_PLAYER_LOGOUT, (OnPlayerLogout event) -> onPlayerLogout(event), this));
	}
	
	/**
	 * Reloads the gacha data from the database.<br>
	 * This method clears all current rewards and costs.<br>
	 * It then notifies the {@code UniqueGacha} script to refresh its data.
	 */
	public void reload()
	{
		final Quest quest = ScriptManager.getInstance().getScript("UniqueGacha");
		if (quest == null)
		{
			return;
		}
		
		_totalRewardCount = 0;
		_totalChanceSumma = 0;
		_visibleItems.clear();
		_rewardItems.clear();
		_rewardChance.clear();
		_gameCosts.clear();
		quest.notifyEvent("RELOAD", null, null);
	}
	
	/**
	 * Updates the internal configuration of the manager.<br>
	 * This method reads values from a {@code StatSet} object.<br>
	 * It sets properties like activity status and currency IDs.
	 * @param params The {@code StatSet} containing the new configuration values.
	 */
	public void setParameters(StatSet params)
	{
		_isActive = params.getBoolean("isActive", false);
		_activeUntilPeriod = params.getLong("activeUntilPeriod", 0L);
		_guaranteeRoll = params.getInt("guaranteeRoll", 200);
		_showProbability = params.getBoolean("showProbability", false);
		_currencyItemId = params.getInt("currencyItemId", 57);
	}
	
	/**
	 * Adds a new reward to a specific gacha rank.<br>
	 * This method registers an item with its associated properties into the rewards list.
	 * @param rank The {@code UniqueGachaRank} this reward belongs to.
	 * @param itemId The unique identifier for the item.
	 * @param itemCount The quantity of the item to be awarded.
	 * @param itemChance The probability weight for obtaining this specific item.
	 * @param enchantLevel The enchantment level applied to the item.
	 */
	public void addReward(UniqueGachaRank rank, int itemId, long itemCount, int itemChance, int enchantLevel)
	{
		_totalRewardCount += 1;
		_totalChanceSumma += itemChance;
		_rewardItems.putIfAbsent(rank, new HashSet<>());
		_rewardItems.get(rank).add(new GachaItemHolder(itemId, itemCount, itemChance, enchantLevel, rank));
	}
	
	/**
	 * Adds a new game cost to the internal collection.<br>
	 * This method maps a specific roll count to an item count.
	 * @param rollCount The number of rolls associated with the cost.
	 * @param itemCount The amount of items required for this cost.
	 */
	public void addGameCost(int rollCount, long itemCount)
	{
		_gameCosts.put(rollCount, itemCount);
	}
	
	/**
	 * Updates the total probability for each {@code UniqueGachaRank}.<br>
	 * This method sums up all item chances within a rank.<br>
	 * It then calls {@code recalculateVisibleItems()} to refresh the display.
	 */
	public void recalculateChances()
	{
		for (Entry<UniqueGachaRank, Set<GachaItemHolder>> entry : _rewardItems.entrySet())
		{
			int totalChance = 0;
			for (GachaItemHolder item : entry.getValue())
			{
				totalChance += item.getItemChance();
			}
			
			_rewardChance.put(entry.getKey(), totalChance);
		}
		
		recalculateVisibleItems();
	}
	
	/**
	 * Updates the list of items displayed to the user.<br>
	 * This method sorts the {@code UniqueGachaRank.RANK_UR} rewards by their chance.<br>
	 * It populates the {@code _visibleItems} collection with up to 5 items.<br>
	 * The internal reward list is cleared after processing.
	 */
	private void recalculateVisibleItems()
	{
		final List<GachaItemHolder> rewards = new ArrayList<>(_rewardItems.getOrDefault(UniqueGachaRank.RANK_UR, Set.of()));
		rewards.sort(Comparator.comparingInt(GachaItemHolder::getItemChance));
		if (rewards.isEmpty())
		{
			return;
		}
		
		_visibleItems.clear();
		_visibleItems.addAll(rewards.subList(0, Math.min(5, (rewards.size()))));
		rewards.clear();
	}
	
	/**
	 * Handles the logic when a player logs into the server.<br>
	 * It initializes the {@code GachaWarehouse} for active players.<br>
	 * It also sends the correct sidebar information packet to the player.
	 * @param event The login event containing the {@code Player} data.
	 */
	private void onPlayerLogin(OnPlayerLogin event)
	{
		final Player player = event == null ? null : event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		if (isActive())
		{
			GachaWarehouse warehouse = _temporaryWarehouse.get(player);
			if (warehouse == null)
			{
				warehouse = new GachaWarehouse(player);
				warehouse.restore();
				_temporaryWarehouse.put(player, warehouse);
			}
		}
		
		player.sendPacket(isActive() ? UniqueGachaSidebarInfo.GACHA_ON : UniqueGachaSidebarInfo.GACHA_OFF);
	}
	
	/**
	 * Handles the logic when a player logs out of the game.<br>
	 * It removes the temporary warehouse associated with the {@code Player}.
	 * @param event The {@code OnPlayerLogout} event containing the player data.
	 */
	private void onPlayerLogout(OnPlayerLogout event)
	{
		final Player player = event == null ? null : event.getPlayer();
		if (player == null)
		{
			return;
		}
		
		final GachaWarehouse warehouse = _temporaryWarehouse.get(player);
		if (warehouse != null)
		{
			warehouse.deleteMe();
			_temporaryWarehouse.remove(player);
		}
	}
	
	/**
	 * Retrieves the set of items currently visible to players.<br>
	 * This includes all rewards that are displayed in the gacha interface.
	 * @return A {@code Set} of {@code GachaItemHolder} objects representing the visible items.
	 */
	public Set<GachaItemHolder> getVisibleItems()
	{
		return _visibleItems;
	}
	
	/**
	 * Retrieves the full list of reward items.<br>
	 * The map organizes {@code GachaItemHolder} objects by their {@code UniqueGachaRank}.
	 * @return A map where each key is a {@code UniqueGachaRank} and each value is a set of rewards.
	 */
	public Map<UniqueGachaRank, Set<GachaItemHolder>> getRewardItems()
	{
		return _rewardItems;
	}
	
	/**
	 * Retrieves the current costs for the game.<br>
	 * The map contains roll counts as keys and item counts as values.
	 * @return a {@code Map<Integer, Long>} containing the game costs.
	 */
	public Map<Integer, Long> getGameCosts()
	{
		return _gameCosts;
	}
	
	/**
	 * Checks if the manager is currently active.
	 * @return {@code true} if the manager is active, {@code false} otherwise.
	 */
	public boolean isActive()
	{
		return _isActive;
	}
	
	/**
	 * Retrieves the timestamp when the current event expires.<br>
	 * This value is used to determine if the gacha is still active.
	 * @return The expiration period as a {@code long}.
	 */
	public long getActiveUntilPeriod()
	{
		return _activeUntilPeriod;
	}
	
	/**
	 * Retrieves the number of rolls required to guarantee a reward.<br>
	 * This value is used by the {@code UniqueGachaManager} to track progress.
	 * @return The total number of rolls for a guaranteed outcome.
	 */
	public int getGuaranteeRoll()
	{
		return _guaranteeRoll;
	}
	
	/**
	 * Checks if the probability display is currently enabled.<br>
	 * This determines whether chance percentages are visible to players.
	 * @return {@code true} if probabilities are shown, {@code false} otherwise.
	 */
	public boolean isShowProbability()
	{
		return _showProbability;
	}
	
	/**
	 * Retrieves the unique identifier for the currency used in this gacha.<br>
	 * This ID corresponds to an entry in the item database.
	 * @return The {@code int} ID of the currency item.
	 */
	public int getCurrencyItemId()
	{
		return _currencyItemId;
	}
	
	/**
	 * Retrieves the total amount of currency held by a {@code Player}.<br>
	 * It checks the inventory for the item ID defined by {@code getCurrencyItemId()}.<br>
	 * The result is capped at {@code Long.MAX_VALUE}.
	 * @param player The {@code Player} whose inventory will be checked.
	 * @return The total count of currency items found in the inventory.
	 */
	public long getCurrencyCount(Player player)
	{
		final long count = player.getInventory().getInventoryItemCount(getCurrencyItemId(), -1);
		return Math.min(Long.MAX_VALUE, count);
	}
	
	/**
	 * Removes a specific amount of currency from the {@code Player}.<br>
	 * This method checks if the inventory is valid and contains the required item.<br>
	 * It then destroys the specified quantity using the {@code FEE} process type.
	 * @param player The {@code Player} who will have their currency removed.
	 * @param count The amount of currency to remove from the inventory.
	 */
	private void removeCurrency(Player player, long count)
	{
		final PlayerInventory inv = player.getInventory();
		if ((inv == null) || !inv.canManipulateWithItemId(_currencyItemId))
		{
			return;
		}
		
		final Item item = inv.getItemByItemId(_currencyItemId);
		if (item == null)
		{
			return;
		}
		
		player.destroyItemByItemId(ItemProcessType.FEE, item.getId(), count, player, true);
	}
	
	/**
	 * Calculates the number of rolls remaining until a guaranteed reward is given.<br>
	 * It subtracts the current progress from the total required rolls.
	 * @param player The {@code Player} whose progress is being checked.
	 * @return The number of steps left to reach the guarantee.
	 */
	public int getStepsToGuaranteedReward(Player player)
	{
		return _guaranteeRoll - player.getVariables().getInt(GACHA_PLAYER_VARIABLE, 0);
	}
	
	/**
	 * Attempts to perform a gacha roll for the specified player.<br>
	 * This method checks requirements and deducts currency before generating rewards.<br>
	 * It handles progress tracking for guaranteed rewards and updates history.
	 * @param player The {@code Player} who is performing the roll.
	 * @param rollCount The number of times to roll in this attempt.
	 * @return An {@code Entry} containing a list of {@code GachaItemHolder} rewards and a {@code Boolean} indicating if any rare items were won.
	 */
	public Entry<List<GachaItemHolder>, Boolean> tryToRoll(Player player, int rollCount)
	{
		if (!checkRequirements(player, rollCount))
		{
			return returnEmptyList();
		}
		
		boolean rare = false;
		final List<GachaItemHolder> rewards = new ArrayList<>();
		
		try
		{
			player.getVariables().set(GACHA_LOCK_PLAYER_VARIABLE, true);
			removeCurrency(player, _gameCosts.get(rollCount));
			int playerRollProgress = player.getVariables().getInt(GACHA_PLAYER_VARIABLE, 0);
			for (int roll = 0; roll < rollCount; roll++)
			{
				playerRollProgress += 1;
				final boolean isGuaranteed = playerRollProgress >= _guaranteeRoll;
				playerRollProgress = isGuaranteed ? 0 : playerRollProgress;
				final UniqueGachaRank rank = randomRank(isGuaranteed);
				final GachaItemHolder item = getRandomReward(rank);
				final UniqueGachaRank itemRank = item.getRank();
				rare = rare || (itemRank.equals(UniqueGachaRank.RANK_SR) || itemRank.equals(UniqueGachaRank.RANK_UR));
				addItemToTemporaryWarehouse(player, item);
				rewards.add(item);
				addToHistory(player, item, roll);
			}
			
			player.getVariables().set(GACHA_PLAYER_VARIABLE, playerRollProgress);
		}
		finally
		{
			player.getVariables().remove(GACHA_LOCK_PLAYER_VARIABLE);
		}
		
		return new AbstractMap.SimpleEntry<>(rewards, rare);
	}
	
	/**
	 * Verifies if the {@code Player} meets all conditions to perform a gacha roll.<br>
	 * This checks for locks, valid costs, inventory access, and warehouse space.
	 * @param player The {@code Player} attempting to perform the action.
	 * @param rollCount The number of rolls requested by the player.
	 * @return {@code true} if all requirements are met; {@code false} otherwise.
	 */
	private boolean checkRequirements(Player player, int rollCount)
	{
		if (player.getVariables().getBoolean(GACHA_LOCK_PLAYER_VARIABLE, false))
		{
			return false;
		}
		
		final long currencyCount = _gameCosts.getOrDefault(rollCount, -1L);
		if (currencyCount == -1L)
		{
			return false;
		}
		
		final PlayerInventory inv = player.getInventory();
		if (inv == null)
		{
			return false;
		}
		
		if (!inv.canManipulateWithItemId(_currencyItemId))
		{
			return false;
		}
		
		if (getCurrencyCount(player) < currencyCount)
		{
			return false;
		}
		
		final GachaWarehouse warehouse = _temporaryWarehouse.get(player);
		if (warehouse == null)
		{
			return false;
		}
		
		if (warehouse.getSize() >= GachaWarehouse.MAXIMUM_TEMPORARY_WAREHOUSE_COUNT)
		{
			return false;
		}
		
		return true;
	}
	
	/**
	 * Records a gacha roll result into the player's history.<br>
	 * This method creates a new {@code GachaItemTimeStampHolder}.<br>
	 * It stores the item details and a generated timestamp.<br>
	 * The data is saved in the internal history map for the specific {@code Player}.
	 * @param player The {@code Player} who performed the roll.
	 * @param item The {@code GachaItemHolder} received from the roll.
	 * @param roll The roll number used to generate the timestamp.
	 */
	private void addToHistory(Player player, GachaItemHolder item, int roll)
	{
		final String count = String.valueOf(roll);
		final String timeStamp = String.valueOf(System.currentTimeMillis() / 1000L) + "0".repeat(3 - count.length()) + (count);
		_gachaHistory.computeIfAbsent(player.getObjectId(), _ -> new ArrayList<>()).addLast(new GachaItemTimeStampHolder(item.getId(), item.getCount(), item.getEnchantLevel(), item.getRank(), Long.parseLong(timeStamp), false));
	}
	
	/**
	 * Adds a specific reward to the temporary warehouse of a {@code Player}.<br>
	 * This method checks if the warehouse exists and if the item template is valid.<br>
	 * It handles both stackable and non-stackable items during the addition process.
	 * @param player The {@code Player} who owns the warehouse.
	 * @param reward The {@code GachaItemHolder} containing the reward details.
	 * @return {@code true} if all items were successfully added, otherwise {@code false}.
	 */
	private boolean addItemToTemporaryWarehouse(Player player, GachaItemHolder reward)
	{
		final GachaWarehouse warehouse = _temporaryWarehouse.get(player);
		if (warehouse == null)
		{
			return false;
		}
		
		final ItemTemplate template = ItemData.getInstance().getTemplate(reward.getId());
		if (template == null)
		{
			return false;
		}
		
		boolean isSuccess = false;
		if (!template.isStackable())
		{
			final long rewardCount = reward.getCount();
			for (long index = 0; index < rewardCount; index++)
			{
				final Item item = warehouse.addItem(ItemProcessType.REWARD, reward.getId(), rewardCount, player, null);
				if (item == null)
				{
					isSuccess = false;
					break;
				}
				
				isSuccess = true;
			}
		}
		else
		{
			isSuccess = warehouse.addItem(ItemProcessType.REWARD, reward.getId(), reward.getCount(), player, null) != null;
		}
		
		return isSuccess;
	}
	
	/**
	 * Transfers specific items from the temporary warehouse to a {@code Player}.<br>
	 * This method verifies if all requested items are available in sufficient quantities.<br>
	 * If any item is missing or insufficient, the operation fails and returns {@code false}.<br>
	 * Upon success, it moves the items to the player inventory and sends an update.
	 * @param player The {@code Player} receiving the items.
	 * @param requestedItems A list of {@code ItemHolder} objects defining what to move.
	 * @return {@code true} if all items were successfully transferred, otherwise {@code false}.
	 */
	public boolean receiveItemsFromTemporaryWarehouse(Player player, List<ItemHolder> requestedItems)
	{
		final GachaWarehouse warehouse = _temporaryWarehouse.get(player);
		final PlayerInventory inventory = player.getInventory();
		if ((warehouse == null) || (inventory == null))
		{
			return false;
		}
		
		// Check items.
		for (ItemHolder requestedItem : requestedItems)
		{
			final ItemTemplate template = ItemData.getInstance().getTemplate(requestedItem.getId());
			final List<Item> item = template == null ? null : template.isStackable() ? List.of(warehouse.getItemByItemId(requestedItem.getId())) : new ArrayList<>(warehouse.getAllItemsByItemId(requestedItem.getId()));
			if ((item == null) || item.isEmpty())
			{
				return false;
			}
			
			if ((template != null) && template.isStackable() ? item.get(0).getCount() < requestedItem.getCount() : item.size() < requestedItem.getCount())
			{
				return false;
			}
		}
		
		for (ItemHolder requestedItem : requestedItems)
		{
			final ItemTemplate template = ItemData.getInstance().getTemplate(requestedItem.getId());
			final List<Item> item = template.isStackable() ? List.of(warehouse.getItemByItemId(requestedItem.getId())) : new ArrayList<>(warehouse.getAllItemsByItemId(requestedItem.getId()));
			for (Item wareHouseItem : item)
			{
				warehouse.transferItem(ItemProcessType.TRANSFER, wareHouseItem.getObjectId(), (template.isStackable() ? requestedItem.getCount() : 1), inventory, player, null);
			}
		}
		
		player.sendItemList();
		
		return true;
	}
	
	/**
	 * Retrieves the items from a player's temporary warehouse.<br>
	 * Returns an empty collection if no warehouse exists for the player.
	 * @param player The {@code Player} whose warehouse is being accessed.
	 * @return A {@code Collection} of {@code Item} objects found in the warehouse.
	 */
	public Collection<Item> getTemporaryWarehouse(Player player)
	{
		final GachaWarehouse warehouse = _temporaryWarehouse.get(player);
		if (warehouse == null)
		{
			return Collections.emptyList();
		}
		
		return warehouse.getItems();
	}
	
	/**
	 * Selects a random gacha rank based on configured probabilities.<br>
	 * It determines if the result is an {@code UR}, {@code SR}, or {@code R} rank.<br>
	 * The logic accounts for special guarantee rules when provided.
	 * @param isGuaranteed Determines if the roll should favor higher ranks.
	 * @return The selected {@code UniqueGachaRank}.
	 */
	private UniqueGachaRank randomRank(boolean isGuaranteed)
	{
		final int rollRank = Rnd.get(0, _totalChanceSumma);
		final UniqueGachaRank rank;
		int sumChance = 0;
		if ((isGuaranteed && (Rnd.get(0, 100) <= 10)) || (rollRank <= (sumChance += _rewardChance.getOrDefault(UniqueGachaRank.RANK_UR, 0))))
		{
			rank = UniqueGachaRank.RANK_UR;
		}
		else if (isGuaranteed || (rollRank <= (sumChance + _rewardChance.getOrDefault(UniqueGachaRank.RANK_SR, 0))))
		{
			rank = UniqueGachaRank.RANK_SR;
		}
		else
		{
			rank = UniqueGachaRank.RANK_R;
		}
		
		return rank;
	}
	
	// Old method using Collections.shuffle to get a random reward.
	// {
	// final List<GachaItemHolder> rollRewards = new ArrayList<>(_rewardItems.getOrDefault(rank, Set.of()));
	// Collections.shuffle(rollRewards);
	// int rollChance = 0;
	// final int rollReward = Rnd.get(0, _rewardChance.getOrDefault(rank, MINIMUM_CHANCE));
	// GachaItemHolder rewardItem = null;
	// for (GachaItemHolder item : rollRewards)
	// {
	// rewardItem = item;
	// rollChance += item.getItemChance();
	// if (rollChance >= rollReward)
	// {
	// break;
	// }
	// }
	// return rewardItem;
	// }
	
	/**
	 * Selects a random reward based on the provided rank.<br>
	 * It calculates the total weight of all items for that rank.<br>
	 * A random item is chosen using weighted probabilities.
	 * @param rank The {@code UniqueGachaRank} to determine which rewards to pick from.
	 * @return The selected {@code GachaItemHolder} or {@code null} if no rewards exist.
	 */
	private GachaItemHolder getRandomReward(UniqueGachaRank rank)
	{
		final Set<GachaItemHolder> rollRewards = _rewardItems.getOrDefault(rank, Collections.emptySet());
		int totalItems = rollRewards.size();
		if (totalItems == 0)
		{
			return null; // or handle the case where no rewards are available for the given rank.
		}
		
		int rollChance = 0;
		GachaItemHolder rewardItem = null;
		
		// Calculate total chances.
		for (GachaItemHolder item : rollRewards)
		{
			rollChance += item.getItemChance();
		}
		
		// Generate random index.
		int randomIndex = Rnd.get(0, rollChance);
		int cumulativeChance = 0;
		
		// Find the reward item corresponding to the random index.
		for (GachaItemHolder item : rollRewards)
		{
			cumulativeChance += item.getItemChance();
			if (randomIndex < cumulativeChance)
			{
				rewardItem = item;
				break;
			}
		}
		
		return rewardItem;
	}
	
	/**
	 * Retrieves the total number of rewards available.<br>
	 * This count includes all items across all ranks.
	 * @return The total count of reward items as an {@code int}.
	 */
	public int getTotalRewardCount()
	{
		return _totalRewardCount;
	}
	
	/**
	 * Retrieves the history of gacha items obtained by a specific player.<br>
	 * This method returns an empty list if no history exists for the {@code Player}.
	 * @param player The {@code Player} whose history is being requested.
	 * @return A {@code List} of {@code GachaItemTimeStampHolder} objects representing the player's gacha history.
	 */
	public List<GachaItemTimeStampHolder> getGachaCharacterHistory(Player player)
	{
		final List<GachaItemTimeStampHolder> history = _gachaHistory.getOrDefault(player.getObjectId(), EMPTY_LINKED_LIST);
		final int size = history.size();
		if (size <= MAX_HISTORY_ITEMS)
		{
			return history;
		}
		
		// The client shows up to 100 entries, send the newest ones.
		return new ArrayList<>(history.subList(size - MAX_HISTORY_ITEMS, size));
	}
	
	/**
	 * Loads the gacha history from the database into a map.<br>
	 * It keeps the newest 100 records of each character from the character_gacha_history table.<br>
	 * The data is organized by character ID.
	 * @param history A map where the key is the character ID and the value is a list of {@code GachaItemTimeStampHolder} objects.
	 */
	private static void restoreGachaHistory(Map<Integer, List<GachaItemTimeStampHolder>> history)
	{
		try (Connection con = DatabaseFactory.getConnection();
			Statement st = con.createStatement();
			ResultSet rset = st.executeQuery("SELECT * FROM character_gacha_history ORDER BY receive_time ASC"))
		{
			while (rset.next())
			{
				final int characterId = rset.getInt("char_id");
				final int itemId = rset.getInt("item_id");
				final long itemCount = rset.getLong("item_count");
				final int enchantLevel = rset.getInt("item_enchant");
				final int itemRank = rset.getInt("item_rank");
				final long receiveTime = rset.getLong("receive_time");
				final List<GachaItemTimeStampHolder> characterHistory = history.computeIfAbsent(characterId, _ -> new ArrayList<>());
				characterHistory.addLast(new GachaItemTimeStampHolder(itemId, itemCount, enchantLevel, UniqueGachaRank.getRankByClientId(itemRank), receiveTime, true));
				if (characterHistory.size() > MAX_HISTORY_ITEMS)
				{
					characterHistory.removeFirst();
				}
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, UniqueGachaManager.class.getSimpleName() + ": Could not load character_gacha_history table: ", e);
		}
	}
	
	/**
	 * Saves the gacha history to the database.<br>
	 * This method iterates through the provided map and inserts new records into the {@code character_gacha_history} table.<br>
	 * It only saves items that have not been stored yet.
	 * @param history A map where the key is the character ID and the value is a list of {@code GachaItemTimeStampHolder} objects.
	 */
	private static void storeGachaHistory(Map<Integer, List<GachaItemTimeStampHolder>> history)
	{
		final Map<Integer, List<GachaItemTimeStampHolder>> map = new HashMap<>(history);
		try (Connection con = DatabaseFactory.getConnection();
			PreparedStatement statement = con.prepareStatement("INSERT INTO `character_gacha_history`(`char_id`, `item_id`, `item_count`, `item_enchant`, `item_rank`, `receive_time`) VALUES(?, ?, ?, ?, ?, ?)"))
		{
			boolean containsUpdate = false;
			for (Entry<Integer, List<GachaItemTimeStampHolder>> entry : map.entrySet())
			{
				final int charId = entry.getKey();
				final List<GachaItemTimeStampHolder> list = new ArrayList<>(entry.getValue());
				for (GachaItemTimeStampHolder item : list)
				{
					if (item.getStoredStatus())
					{
						continue;
					}
					
					statement.setInt(1, charId);
					statement.setInt(2, item.getId());
					statement.setLong(3, item.getCount());
					statement.setInt(4, item.getEnchantLevel());
					statement.setInt(5, item.getRank().getClientId());
					statement.setLong(6, item.getTimeStamp());
					statement.addBatch();
					containsUpdate = true;
				}
			}
			
			if (containsUpdate)
			{
				statement.executeBatch();
			}
		}
		catch (SQLException e)
		{
			LOGGER.log(Level.WARNING, UniqueGachaManager.class.getSimpleName() + ": Could not store character_gacha_history table: ", e);
		}
	}
	
	/**
	 * Provides a default empty result for gacha rolls.<br>
	 * This method returns an {@code Entry} containing an empty {@code List} and {@code false}.
	 * @return An {@code Entry} with an empty list and the boolean value {@code false}.
	 */
	private static Entry<List<GachaItemHolder>, Boolean> returnEmptyList()
	{
		return new AbstractMap.SimpleEntry<>(Collections.emptyList(), false);
	}
	
	/**
	 * Retrieves the singleton instance of the {@code UniqueGachaManager}.<br>
	 * Use this method to access the global manager for unique gacha events.
	 * @return The active {@code UniqueGachaManager} instance.
	 */
	public static UniqueGachaManager getInstance()
	{
		return UniqueGachaManager.SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final UniqueGachaManager INSTANCE = new UniqueGachaManager();
	}
}
