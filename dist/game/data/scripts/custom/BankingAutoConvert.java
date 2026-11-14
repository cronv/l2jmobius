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
package custom;

import org.l2jmobius.gameserver.config.custom.BankingConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayerItemAdd;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;

/**
 * This class handles the automatic conversion of items when they are added to a player's bank.<br>
 * It listens for {@code OnPlayerItemAdd} events and processes items based on the {@code BankingConfig}.
 * @author CostyKiller
 */
public class BankingAutoConvert
{
	/**
	 * Initializes the {@code BankingAutoConvert} system.<br>
	 * This constructor checks if auto-conversion is enabled in {@code BankingConfig}.<br>
	 * It registers a listener to handle items added to player inventories.
	 */
	public BankingAutoConvert()
	{
		if (BankingConfig.BANKING_SYSTEM_AUTO_CONVERT_ENABLED)
		{
			Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_PLAYER_ITEM_ADD, (OnPlayerItemAdd event) -> onPlayerItemAdd(event), this));
		}
	}
	
	/**
	 * Handles the logic for automatically converting Adena into Gold Bars.<br>
	 * This method checks if the auto-convert feature is enabled in {@code BankingConfig}.<br>
	 * It verifies that the added item is Adena and that the player exceeds the required limit.<br>
	 * If conditions are met, it reduces the player's Adena and adds Gold Bars to their inventory.
	 * @param event The {@code OnPlayerItemAdd} event containing the item and player data.
	 */
	private void onPlayerItemAdd(OnPlayerItemAdd event)
	{
		if (!BankingConfig.BANKING_SYSTEM_AUTO_CONVERT_ENABLED)
		{
			return;
		}
		
		final Item item = event.getItem();
		if ((item == null) || (item.getId() != Inventory.ADENA_ID))
		{
			return;
		}
		
		final Player player = event.getPlayer();
		if ((player == null) || (player.getAdena() <= BankingConfig.BANKING_SYSTEM_AUTO_CONVERT_ADENA_LIMIT))
		{
			return;
		}
		
		final PlayerInventory inventory = player.getInventory();
		inventory.reduceAdena(ItemProcessType.COMPENSATE, BankingConfig.BANKING_SYSTEM_ADENA_COUNT, player, null);
		inventory.addItem(ItemProcessType.COMPENSATE, 3470, BankingConfig.BANKING_SYSTEM_GOLDBAR_COUNT, player, null);
		inventory.updateDatabase();
		
		player.sendMessage("Automatically converted " + BankingConfig.BANKING_SYSTEM_ADENA_COUNT + " Adena to " + BankingConfig.BANKING_SYSTEM_GOLDBAR_COUNT + " Gold " + (BankingConfig.BANKING_SYSTEM_GOLDBAR_COUNT > 1 ? "Bars." : "Bar."));
	}
	
	/**
	 * This is the main entry point for the {@code BankingAutoConvert} class.<br>
	 * It initializes a new instance of the converter to start the process.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new BankingAutoConvert();
	}
}
