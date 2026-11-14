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

import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client when a player levels up the {@code Ability of Fire} skill.<br>
 * It handles the synchronization of the new skill level for the {@code Player}.
 * @author CostyKiller
 */
public class ExAbilityOfFireLevelUp extends ServerPacket
{
	private static final int PRIMORDIAL_FIRE_SOURCE_SKILL_ID = 34495;
	private static final int FIRE_SOURCE_SKILL_ID = 34498;
	private static final int LIFE_SOURCE_SKILL_ID = 34499;
	private static final int BATTLE_SOUL_SKILL_ID = 34500;
	private static final int FIRE_TOTEM_SKILL_ID = 34501;
	private static final int FLAME_SPARK_SKILL_ID = 34502;
	
	private final Player _player;
	private final int _type;
	private int _success = 0;
	private int _nextAbilityLevel = 0;
	
	/**
	 * This constructor initializes a new {@code ExAbilityOfFireLevelUp} packet.<br>
	 * It sets the target {@code Player} and the specific ability {@code type}.
	 * @param player The {@code Player} who is leveling up their fire ability.
	 * @param type The integer ID representing the type of fire ability.
	 */
	public ExAbilityOfFireLevelUp(Player player, int type)
	{
		_player = player;
		_type = type;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_ENHANCED_ABILITY_OF_FIRE_LEVEL_UP.writeId(this, buffer);
		
		buffer.writeByte(_type);
		switch (_type)
		{
			case 0: // Fire Source.
			{
				final PlayerVariables vars = _player.getVariables();
				_nextAbilityLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, 0) + 1;
				if (checkItems(_player) && consumeItems())
				{
					_success = 1;
					buffer.writeByte(_success); // int Result.
					buffer.writeInt(_nextAbilityLevel); // int Level.
					vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_EXP, 0);
					vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, _nextAbilityLevel);
					
					// Add ability skill.
					_player.addSkill(SkillData.getInstance().getSkill(FIRE_SOURCE_SKILL_ID, _nextAbilityLevel), true);
					
					// Check ability set levels to add set skill.
					final int abilitySetLevels = checkAbilitySetLevels();
					if (abilitySetLevels != 0)
					{
						_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, abilitySetLevels), true);
					}
				}
				break;
			}
			case 1: // Life Source.
			{
				final PlayerVariables vars = _player.getVariables();
				_nextAbilityLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, 0) + 1;
				if (checkItems(_player) && consumeItems())
				{
					_success = 1;
					buffer.writeByte(_success); // int Result.
					buffer.writeInt(_nextAbilityLevel); // int Level.
					vars.set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_EXP, 0);
					vars.set(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, _nextAbilityLevel);
					
					// Add ability skill.
					_player.addSkill(SkillData.getInstance().getSkill(LIFE_SOURCE_SKILL_ID, _nextAbilityLevel), true);
					
					// Check ability set levels to add set skill.
					final int abilitySetLevels = checkAbilitySetLevels();
					if (abilitySetLevels != 0)
					{
						_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, abilitySetLevels), true);
					}
				}
				break;
			}
			case 2: // Flame Spark.
			{
				final PlayerVariables vars = _player.getVariables();
				_nextAbilityLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, 0) + 1;
				if (checkItems(_player) && consumeItems())
				{
					_success = 1;
					buffer.writeByte(_success); // int Result.
					buffer.writeInt(_nextAbilityLevel); // int Level.
					vars.set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_EXP, 0);
					vars.set(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, _nextAbilityLevel);
					
					// Add ability skill.
					_player.addSkill(SkillData.getInstance().getSkill(FLAME_SPARK_SKILL_ID, _nextAbilityLevel), true);
					
					// Check ability set levels to add set skill.
					final int abilitySetLevels = checkAbilitySetLevels();
					if (abilitySetLevels != 0)
					{
						_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, abilitySetLevels), true);
					}
				}
				break;
			}
			case 3: // Fire Totem.
			{
				final PlayerVariables vars = _player.getVariables();
				_nextAbilityLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, 0) + 1;
				if (checkItems(_player) && consumeItems())
				{
					_success = 1;
					buffer.writeByte(_success); // int Result.
					buffer.writeInt(_nextAbilityLevel); // int Level.
					vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_EXP, 0);
					vars.set(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, _nextAbilityLevel);
					
					// Add ability skill.
					_player.addSkill(SkillData.getInstance().getSkill(FIRE_TOTEM_SKILL_ID, _nextAbilityLevel), true);
					
					// Check ability set levels to add set skill.
					final int abilitySetLevels = checkAbilitySetLevels();
					if (abilitySetLevels != 0)
					{
						_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, abilitySetLevels), true);
					}
				}
				break;
			}
			case 4: // Battle Soul.
			{
				final PlayerVariables vars = _player.getVariables();
				_nextAbilityLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, 0) + 1;
				if (checkItems(_player) && consumeItems())
				{
					_success = 1;
					buffer.writeByte(_success); // int Result.
					buffer.writeInt(_nextAbilityLevel); // int Level.
					vars.set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_EXP, 0);
					vars.set(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, _nextAbilityLevel);
					
					// Add ability skill.
					_player.addSkill(SkillData.getInstance().getSkill(BATTLE_SOUL_SKILL_ID, _nextAbilityLevel), true);
					
					// Check ability set levels to add set skill.
					final int abilitySetLevels = checkAbilitySetLevels();
					if (abilitySetLevels != 0)
					{
						_player.addSkill(SkillData.getInstance().getSkill(PRIMORDIAL_FIRE_SOURCE_SKILL_ID, abilitySetLevels), true);
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
	 * Consumes every item required for the next ability level.
	 * @return {@code true} only if all required items were destroyed, otherwise {@code false}.
	 */
	private boolean consumeItems()
	{
		boolean consumed = true;
		for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
		{
			if (!_player.destroyItemByItemId(ItemProcessType.FEE, requiredItems.getId(), requiredItems.getCount(), _player, true))
			{
				consumed = false;
			}
		}
		
		return consumed;
	}
	
	/**
	 * Verifies the player owns every item required to level up.<br>
	 * It checks all requirements from {@code getNextAbilityLevelRequirements}.
	 * @param player The {@code Player} to check.
	 * @return {@code true} if the player has all required items, otherwise {@code false}.
	 */
	public boolean checkItems(Player player)
	{
		final PlayerInventory inventory = player.getInventory();
		for (ItemHolder requiredItems : getNextAbilityLevelRequirements())
		{
			if (inventory.getInventoryItemCount(requiredItems.getId(), -1) < requiredItems.getCount())
			{
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * Retrieves the list of items needed for the next ability level.<br>
	 * This method checks the current {@code _nextAbilityLevel} value.<br>
	 * It returns a list of {@code ItemHolder} objects based on the configuration.
	 * @return A {@code List} of {@code ItemHolder} requirements.
	 */
	public List<ItemHolder> getNextAbilityLevelRequirements()
	{
		final List<ItemHolder> requiredItems;
		switch (_nextAbilityLevel)
		{
			case 1:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_1_REQUIRED_ITEMS;
				break;
			}
			case 2:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_2_REQUIRED_ITEMS;
				break;
			}
			case 3:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_3_REQUIRED_ITEMS;
				break;
			}
			case 4:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_4_REQUIRED_ITEMS;
				break;
			}
			case 5:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_5_REQUIRED_ITEMS;
				break;
			}
			case 6:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_6_REQUIRED_ITEMS;
				break;
			}
			case 7:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_7_REQUIRED_ITEMS;
				break;
			}
			case 8:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_8_REQUIRED_ITEMS;
				break;
			}
			case 9:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_9_REQUIRED_ITEMS;
				break;
			}
			case 10:
			{
				requiredItems = ConquestConfig.CONQUEST_ABILITY_UPGRADE_LEVEL_10_REQUIRED_ITEMS;
				break;
			}
			default:
			{
				requiredItems = Collections.emptyList();
				break;
			}
		}
		
		return requiredItems;
	}
	
	/**
	 * Checks the current level of the ability set based on player variables.<br>
	 * It evaluates multiple skill levels to determine a set rank.
	 * @return The calculated set level as an {@code int}.
	 */
	public int checkAbilitySetLevels()
	{
		int setSkillLevel = 0;
		final PlayerVariables vars = _player.getVariables();
		final int fireSourceLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_SOURCE_LEVEL, 0);
		final int lifeSourceLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_LIFE_SOURCE_LEVEL, 0);
		final int flameSparkLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_FLAME_SPARK_LEVEL, 0);
		final int fireTotemLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_FIRE_TOTEM_LEVEL, 0);
		final int battleSoulLevel = vars.getInt(PlayerVariables.CONQUEST_ABILITY_BATTLE_SOUL_LEVEL, 0);
		
		if ((fireSourceLevel == 10) && //
			(lifeSourceLevel == 10) && //
			(flameSparkLevel == 10) && //
			(fireTotemLevel == 10) && //
			(battleSoulLevel == 10))
		{
			setSkillLevel = 3;
		}
		else if ((fireSourceLevel >= 6) && //
			(lifeSourceLevel >= 6) && //
			(flameSparkLevel >= 6) && //
			(fireTotemLevel >= 6) && //
			(battleSoulLevel >= 6))
		{
			setSkillLevel = 2;
		}
		else if ((fireSourceLevel >= 3) && //
			(lifeSourceLevel >= 3) && //
			(flameSparkLevel >= 3) && //
			(fireTotemLevel >= 3) && //
			(battleSoulLevel >= 3))
		{
			setSkillLevel = 1;
		}
		
		return setSkillLevel;
	}
}
