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
package org.l2jmobius.gameserver.network.serverpackets.dye;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.RatesConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.variables.AccountVariables;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet informs the client to reset the dye effect enchantment on an item.<br>
 * It is used when a player successfully performs an action that clears existing dyes.
 * @author CostyKiller
 */
public class DyeEffectEnchantReset extends ServerPacket
{
	private final Player _player;
	private boolean _success = false;
	private final int _category;
	private final int _slotId;
	private int _skillId;
	private final int _skillLevel;
	private int _hiddenSkillId;
	private final int _hiddenSkillLevel;
	private final int _hiddenSealBuffSkillId;
	private final int _hiddenSealBuffSkillLevel;
	
	/**
	 * Initializes a new dye effect enchant reset packet.<br>
	 * This constructor sets the player, category, and slot information.<br>
	 * It also calculates the required skill levels based on the provided {@code slotId}.
	 * @param player The {@code Player} who is performing the action.
	 * @param category The category of the dye effect.
	 * @param slotId The specific slot identifier for the enchantment.
	 */
	public DyeEffectEnchantReset(Player player, int category, int slotId)
	{
		_player = player;
		_category = category;
		_slotId = slotId;
		switch (slotId)
		{
			case 1:
			{
				_skillId = 37073; // Seal - Giant's Power
				_hiddenSkillId = 37066; // Seal Heritage - Giant's Power
				break;
			}
			case 2:
			{
				_skillId = 37074; // Seal - Giant's Wisdom
				_hiddenSkillId = 37067; // Seal Heritage - Giant's Wisdom
				break;
			}
			case 3:
			{
				_skillId = 37075; // Seal - Giant's Might
				_hiddenSkillId = 37068; // Seal Heritage - Giant's Might
				break;
			}
			default:
			{
				break;
			}
		}
		
		final AccountVariables accountVars = _player.getAccountVariables();
		_skillLevel = accountVars.getInt(AccountVariables.DYE_LEVEL_FOR_SLOT_ + slotId, 0);
		_hiddenSkillLevel = accountVars.getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + slotId, 0);
		
		// Check hidden skills levels here.
		final int hiddenPowerSkillLevel = accountVars.getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 1, 0);
		final int hiddenWisdomSkillLevel = accountVars.getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 2, 0);
		final int hiddenMightSkillLevel = accountVars.getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + 3, 0);
		_hiddenSealBuffSkillId = 37072; // Giant Seal Buff
		_hiddenSealBuffSkillLevel = hiddenPowerSkillLevel + hiddenWisdomSkillLevel + hiddenMightSkillLevel;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DYEEFFECT_ENCHANT_RESET.writeId(this, buffer);
		final Item dyeResetFeeItem = _player.getInventory().getItemByItemId(RatesConfig.DYE_ENCHANT_RESET_FEE_ITEM_ID);
		if ((dyeResetFeeItem == null) || (dyeResetFeeItem.getCount() < RatesConfig.DYE_ENCHANT_RESET_FEE_ITEM_COUNT))
		{
			_player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS_FOR_RESET);
			return;
		}
		
		// Take the reset fee before applying the reset.
		if (!_player.destroyItemByItemId(ItemProcessType.FEE, RatesConfig.DYE_ENCHANT_RESET_FEE_ITEM_ID, RatesConfig.DYE_ENCHANT_RESET_FEE_ITEM_COUNT, _player, true))
		{
			_player.sendPacket(SystemMessageId.NOT_ENOUGH_ITEMS_FOR_RESET);
			return;
		}
		
		_success = true;
		
		final AccountVariables accountVars = _player.getAccountVariables();
		
		// Remove skills here?
		_player.removeSkill(SkillData.getInstance().getSkill(_skillId, _skillLevel), true);
		if (_hiddenSkillLevel != 0)
		{
			_player.removeSkill(SkillData.getInstance().getSkill(_hiddenSkillId, _hiddenSkillLevel), true);
			accountVars.set(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + _slotId, 0);
		}
		
		// Update hidden seal buff skill level based on the slot that is reset
		if (_hiddenSealBuffSkillLevel != 0)
		{
			_player.addSkill(SkillData.getInstance().getSkill(_hiddenSealBuffSkillId, _hiddenSealBuffSkillLevel - accountVars.getInt(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + _slotId, 0)), true);
		}
		
		_player.sendSkillList();
		
		accountVars.set(AccountVariables.DYE_CHALLENGE_COUNT_FOR_SLOT_ + _slotId, 30);
		accountVars.set(AccountVariables.DYE_LEVEL_FOR_SLOT_ + _slotId, 0);
		accountVars.set(AccountVariables.DYE_HIDDEN_SKILL_LEVEL_FOR_SLOT_ + _slotId, 0);
		buffer.writeInt(_success);
		buffer.writeInt(_category);
		buffer.writeInt(_slotId);
	}
}
