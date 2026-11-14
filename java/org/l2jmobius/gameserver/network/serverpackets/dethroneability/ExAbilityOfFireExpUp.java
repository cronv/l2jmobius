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

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;

/**
 * This packet handles the experience point increase for the {@code Ability Of Fire} skill.<br>
 * It informs the client that a player has gained experience from using this specific ability.
 * @author CostyKiller
 */
public class ExAbilityOfFireExpUp extends ServerPacket
{
	private final Player _player;
	private final int _type;
	private int _success = 0;
	
	/**
	 * This packet handles the experience point increase for a fire ability.<br>
	 * It identifies which player receives the bonus and the specific type of ability used.
	 * @param player The {@code Player} object who will receive the experience boost.
	 * @param type The integer representing the specific type of fire ability.
	 */
	public ExAbilityOfFireExpUp(Player player, int type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENHANCED_ABILITY_OF_FIRE_EXP_UP.writeId(this, buffer);
		buffer.writeByte(_type);
		if (_type == 0) // Fire Source
		{
			// _success = 0
		}
		else if (_type == 1) // Life Source
		{
			final PlayerVariables vars = _player.getVariables();
			if (!hasItems(ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REQUIRED_ITEMS) || !consumeItems(ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REQUIRED_ITEMS))
			{
				// Not enough items: report failure without applying any cost.
				buffer.writeByte(_success); // int Result
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_EXP, 0)); // int EXP
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, 500)); // int ExpUpCount
				buffer.writeInt(0); // array<_ItemInfo> rewards
				return;
			}
			
			_player.setCurrentHp(_player.getCurrentHp() - ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REQUIRED_HP_POINTS);
			_player.sendMessage("Your HP has decreased by " + NumberFormat.getInstance(Locale.of("en", "US")).format(ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REQUIRED_HP_POINTS) + ".");
			
			vars.set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, 500) - 1));
			vars.storeMe();
			
			if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADE_CHANCE)
			{
				vars.set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_EXP, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_EXP, 0) + ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_EXP_AMOUNT));
				vars.storeMe();
				_success = 1;
			}
			
			buffer.writeByte(_success); // int Result
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_EXP, 0)); // int EXP
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_UPGRADES, 500)); // int ExpUpCount
			if (_success == 1)
			{
				// Rewards Part
				if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REWARD_CHANCE)
				{
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REWARDS)
					{
						_player.addItem(ItemProcessType.REWARD, rewards.getId(), rewards.getCount(), _player, true);
					}
					
					buffer.writeInt(ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REWARDS.size()); // array<_ItemInfo> rewards
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REWARDS)
					{
						buffer.writeInt(rewards.getId());
						buffer.writeLong(rewards.getCount());
					}
				}
				else
				{
					buffer.writeInt(0); // array<_ItemInfo> rewards
				}
			}
			else
			{
				buffer.writeInt(0); // array<_ItemInfo> rewards
			}
		}
		else if (_type == 2) // Flame Spark
		{
			final PlayerVariables vars = _player.getVariables();
			if (!hasItems(ConquestConfig.CONQUEST_ABILITY_FLAME_SPARK_REQUIRED_ITEMS) || !consumeItems(ConquestConfig.CONQUEST_ABILITY_FLAME_SPARK_REQUIRED_ITEMS))
			{
				// Not enough items: report failure without applying any cost.
				buffer.writeByte(_success); // int Result
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_EXP, 0)); // int EXP
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, 60)); // int ExpUpCount
				buffer.writeInt(0); // array<_ItemInfo> rewards
				return;
			}
			
			vars.set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, 60) - 1));
			vars.storeMe();
			
			if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_FLAME_SPARK_UPGRADE_CHANCE)
			{
				vars.set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_EXP, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_EXP, 0) + ConquestConfig.CONQUEST_ABILITY_FLAME_SPARK_EXP_AMOUNT));
				vars.storeMe();
				_success = 1;
			}
			
			buffer.writeByte(_success); // int Result
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_EXP, 0)); // int EXP
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_UPGRADES, 60)); // int ExpUpCount
			if (_success == 1)
			{
				// Rewards Part
				if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_LIFE_SOURCE_REWARD_CHANCE)
				{
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_FLAME_SPARK_REWARDS)
					{
						_player.addItem(ItemProcessType.REWARD, rewards.getId(), rewards.getCount(), _player, true);
					}
					
					buffer.writeInt(ConquestConfig.CONQUEST_ABILITY_FLAME_SPARK_REWARDS.size()); // array<_ItemInfo> rewards
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_FLAME_SPARK_REWARDS)
					{
						buffer.writeInt(rewards.getId());
						buffer.writeLong(rewards.getCount());
					}
				}
				else
				{
					buffer.writeInt(0); // array<_ItemInfo> rewards
				}
			}
			else
			{
				buffer.writeInt(0); // array<_ItemInfo> rewards
			}
		}
		else if (_type == 3) // Fire Totem
		{
			final PlayerVariables vars = _player.getVariables();
			if (!hasItems(ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_REQUIRED_ITEMS) || !consumeItems(ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_REQUIRED_ITEMS))
			{
				// Not enough items: report failure without applying any cost.
				buffer.writeByte(_success); // int Result
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_EXP, 0)); // int EXP
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, 100)); // int ExpUpCount
				buffer.writeInt(0); // array<_ItemInfo> rewards
				return;
			}
			
			vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, 100) - 1));
			vars.storeMe();
			
			if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADE_CHANCE)
			{
				vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_EXP, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_EXP, 0) + ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_EXP_AMOUNT));
				vars.storeMe();
				_success = 1;
			}
			
			buffer.writeByte(_success); // int Result
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_EXP, 0)); // int EXP
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_UPGRADES, 100)); // int ExpUpCount
			if (_success == 1)
			{
				// Rewards Part
				if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_REWARD_CHANCE)
				{
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_REWARDS)
					{
						_player.addItem(ItemProcessType.REWARD, rewards.getId(), rewards.getCount(), _player, true);
					}
					
					buffer.writeInt(ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_REWARDS.size()); // array<_ItemInfo> rewards
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_FIRE_TOTEM_REWARDS)
					{
						buffer.writeInt(rewards.getId());
						buffer.writeLong(rewards.getCount());
					}
				}
				else
				{
					buffer.writeInt(0);
				}
			}
			else
			{
				buffer.writeInt(0); // array<_ItemInfo> rewards
			}
		}
		else if (_type == 4) // Battle Soul
		{
			final PlayerVariables vars = _player.getVariables();
			if (!hasItems(ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REQUIRED_ITEMS) || !consumeItems(ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REQUIRED_ITEMS))
			{
				// Not enough items: report failure without applying any cost.
				buffer.writeByte(_success); // int Result
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_EXP, 0)); // int EXP
				buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, 100)); // int ExpUpCount
				buffer.writeInt(0); // array<_ItemInfo> rewards
				return;
			}
			
			_player.setSp(_player.getSp() - ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REQUIRED_SP_POINTS);
			SystemMessage sm = new SystemMessage(SystemMessageId.YOUR_SP_HAS_DECREASED_BY_S1);
			sm.addLong(ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REQUIRED_SP_POINTS);
			_player.sendPacket(sm);
			_player.broadcastUserInfo();
			
			vars.set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, 100) - 1));
			vars.storeMe();
			
			if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADE_CHANCE)
			{
				vars.set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_EXP, (vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_EXP, 0) + ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_EXP_AMOUNT));
				vars.storeMe();
				_success = 1;
			}
			
			buffer.writeByte(_success); // int Result
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_EXP, 0)); // int EXP
			buffer.writeInt(vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_UPGRADES, 100)); // int ExpUpCount
			if (_success == 1)
			{
				// Rewards Part
				if (Rnd.get(100) < ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REWARD_CHANCE)
				{
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REWARDS)
					{
						_player.addItem(ItemProcessType.REWARD, rewards.getId(), rewards.getCount(), _player, true);
					}
					
					buffer.writeInt(ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REWARDS.size()); // array<_ItemInfo> rewards
					for (ItemHolder rewards : ConquestConfig.CONQUEST_ABILITY_BATTLE_SOUL_REWARDS)
					{
						buffer.writeInt(rewards.getId());
						buffer.writeLong(rewards.getCount());
					}
				}
				else
				{
					buffer.writeInt(0);
				}
			}
			else
			{
				buffer.writeInt(0); // array<_ItemInfo> rewards
			}
		}
	}
	
	/**
	 * Verifies the player owns every item in the given requirement list.
	 * @param requiredItems The list of {@code ItemHolder} requirements.
	 * @return {@code true} if the player has all required items, otherwise {@code false}.
	 */
	private boolean hasItems(List<ItemHolder> requiredItems)
	{
		final PlayerInventory inventory = _player.getInventory();
		for (ItemHolder item : requiredItems)
		{
			if (inventory.getInventoryItemCount(item.getId(), -1) < item.getCount())
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Consumes every item in the given requirement list.
	 * @param requiredItems The list of {@code ItemHolder} requirements.
	 * @return {@code true} only if all required items were destroyed, otherwise {@code false}.
	 */
	private boolean consumeItems(List<ItemHolder> requiredItems)
	{
		boolean consumed = true;
		for (ItemHolder item : requiredItems)
		{
			if (!_player.destroyItemByItemId(ItemProcessType.FEE, item.getId(), item.getCount(), _player, true))
			{
				consumed = false;
			}
		}
		
		return consumed;
	}
}
