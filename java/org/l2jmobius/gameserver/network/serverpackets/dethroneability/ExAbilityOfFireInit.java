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
package org.l2jmobius.gameserver.network.serverpackets.dethroneability;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet initializes the {@code AbilityOfFire} ability for a player.<br>
 * It handles the necessary data synchronization to enable the skill on the client side.
 * @author CostyKiller
 */
public class ExAbilityOfFireInit extends ServerPacket
{
	private final Player _player;
	private final int _type;
	private int _success = 0;
	
	/**
	 * Initializes a new {@code ExAbilityOfFireInit} packet.<br>
	 * This method sets the target {@code Player} and the specific ability {@code type}.
	 * @param player The {@code Player} who will receive this packet.
	 * @param type The integer ID representing the fire ability type.
	 */
	public ExAbilityOfFireInit(Player player, int type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENHANCED_ABILITY_OF_FIRE_INIT.writeId(this, buffer);
		buffer.writeByte(_type);
		
		switch (_type)
		{
			case 0: // Fire Source
			{
				// No reset button here.
				break;
			}
			case 1: // Life Source
			{
				if (checkItems(_player))
				{
					final PlayerVariables vars = _player.getVariables();
					if (consumeReset())
					{
						_success = 1;
					}
					
					buffer.writeByte(_success); // int Result
					if (_success == 1)
					{
						buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, 500)); // int ExpUpCount
						vars.set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, 500);
						vars.set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_RESET, 0);
						vars.storeMe();
					}
					else
					{
						buffer.writeInt(0);
					}
				}
				break;
			}
			case 2: // Flame Spark
			{
				if (checkItems(_player))
				{
					final PlayerVariables vars = _player.getVariables();
					if (consumeReset())
					{
						_success = 1;
					}
					
					buffer.writeByte(_success); // int Result
					if (_success == 1)
					{
						buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, 60)); // int ExpUpCount
						vars.set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, 60);
						vars.set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_RESET, 0);
						vars.storeMe();
					}
					else
					{
						buffer.writeInt(0);
					}
				}
				break;
			}
			case 3: // Fire Totem
			{
				if (checkItems(_player))
				{
					final PlayerVariables vars = _player.getVariables();
					if (consumeReset())
					{
						_success = 1;
					}
					
					buffer.writeByte(_success); // int Result
					if (_success == 1)
					{
						buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, 100)); // int ExpUpCount
						vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, 100);
						vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_RESET, 0);
						vars.storeMe();
					}
					else
					{
						buffer.writeInt(0);
					}
				}
				break;
			}
			case 4: // Battle Soul
			{
				if (checkItems(_player))
				{
					final PlayerVariables vars = _player.getVariables();
					if (consumeReset())
					{
						_success = 1;
					}
					
					buffer.writeByte(_success); // int Result
					if (_success == 1)
					{
						buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, 100)); // int ExpUpCount
						vars.set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, 100);
						vars.set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_RESET, 0);
						vars.storeMe();
					}
					else
					{
						buffer.writeInt(0);
					}
				}
				break;
			}
			default:
			{
				break;
			}
		}
	}
	
	/**
	 * Consumes every item required to reset a conquest ability.
	 * @return {@code true} only if all required items were destroyed, otherwise {@code false}.
	 */
	private boolean consumeReset()
	{
		boolean consumed = true;
		for (ItemHolder requiredItems : ConquestConfig.CONQUEST_ABILITY_UPGRADES_RESET_REQUIRED_ITEMS)
		{
			if (!_player.destroyItemByItemId(ItemProcessType.FEE, requiredItems.getId(), requiredItems.getCount(), _player, true))
			{
				consumed = false;
			}
		}
		
		return consumed;
	}
	
	/**
	 * Verifies the {@code Player} owns every item required to reset a conquest ability.<br>
	 * It checks the inventory against required items defined in {@code ConquestConfig}.
	 * @param player The {@code Player} whose inventory will be checked.
	 * @return {@code true} if the player has all required items, otherwise {@code false}.
	 */
	public boolean checkItems(Player player)
	{
		final PlayerInventory inventory = player.getInventory();
		for (ItemHolder requiredItems : ConquestConfig.CONQUEST_ABILITY_UPGRADES_RESET_REQUIRED_ITEMS)
		{
			if (inventory.getInventoryItemCount(requiredItems.getId(), -1) < requiredItems.getCount())
			{
				return false;
			}
		}
		
		return true;
	}
}
