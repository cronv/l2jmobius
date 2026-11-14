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
package handlers.skill.effects;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.data.enums.ArmorMagicalDefenceType;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the application of a magical defense bonus to an actor's armor.<br>
 * This effect modifies the {@code Stat} associated with magical protection.
 * @author Norvox
 */
public class ArmorBonusMagicalDefence extends AbstractStatEffect
{
	private final List<Integer> _paperdollSlots = new ArrayList<>();
	private final ArmorMagicalDefenceType _defenceType;
	
	/**
	 * Creates a new instance of {@code ArmorBonusMagicalDefence}.<br>
	 * This constructor initializes the effect based on specific equipment slots.<br>
	 * It determines which inventory slots and defense types to apply.
	 * @param params The {@code StatSet} containing configuration data like the slot name.
	 */
	public ArmorBonusMagicalDefence(StatSet params)
	{
		super(params, Stat.ARMOR_BONUS_MAGICAL_DEFENCE);
		
		final String slotName = params.getString("slot", "");
		switch (slotName.toLowerCase())
		{
			case "ear":
			{
				_paperdollSlots.add(Inventory.PAPERDOLL_REAR);
				_paperdollSlots.add(Inventory.PAPERDOLL_LEAR);
				_defenceType = ArmorMagicalDefenceType.EARRING;
				break;
			}
			case "neck":
			{
				_paperdollSlots.add(Inventory.PAPERDOLL_NECK);
				_defenceType = ArmorMagicalDefenceType.NECKLACE;
				break;
			}
			case "finger":
			{
				_paperdollSlots.add(Inventory.PAPERDOLL_RFINGER);
				_paperdollSlots.add(Inventory.PAPERDOLL_LFINGER);
				_defenceType = ArmorMagicalDefenceType.RING;
				break;
			}
			case "agathion":
			{
				_paperdollSlots.add(Inventory.PAPERDOLL_AGATHION1);
				_paperdollSlots.add(Inventory.PAPERDOLL_AGATHION2);
				_paperdollSlots.add(Inventory.PAPERDOLL_AGATHION3);
				_paperdollSlots.add(Inventory.PAPERDOLL_AGATHION4);
				_paperdollSlots.add(Inventory.PAPERDOLL_AGATHION5);
				_defenceType = ArmorMagicalDefenceType.AGATHION;
				break;
			}
			case "talisman":
			{
				_paperdollSlots.add(Inventory.PAPERDOLL_DECO1);
				_paperdollSlots.add(Inventory.PAPERDOLL_DECO2);
				_paperdollSlots.add(Inventory.PAPERDOLL_DECO3);
				_paperdollSlots.add(Inventory.PAPERDOLL_DECO4);
				_paperdollSlots.add(Inventory.PAPERDOLL_DECO5);
				_paperdollSlots.add(Inventory.PAPERDOLL_DECO6);
				_defenceType = ArmorMagicalDefenceType.TALISMAN;
				break;
			}
			default:
			{
				throw new IllegalArgumentException("Unknown slot ArmorBonusMagicalDefence: " + slotName);
			}
		}
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method verifies if the {@code effected} creature is a {@code Player}.<br>
	 * It also checks if the player has any equipment equipped.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to apply the effect.
	 * @return {@code true} if the conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return effected.isPlayer() && (getEquippedCount(effected.asPlayer()) > 0);
	}
	
	/**
	 * Applies the magical defense bonus to a target creature.<br>
	 * This method updates the stats based on the equipped items of the player.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().addMagicalDefenceBonus(_defenceType, (int) _amount);
		effected.getStat().mergeAdd(_addStat, _amount * getEquippedCount(effected.asPlayer()));
	}
	
	/**
	 * Calculates how many items are currently equipped in specific slots.<br>
	 * It checks the {@code Player} inventory for non-null items.
	 * @param player The {@code Player} whose inventory will be checked.
	 * @return The total number of occupied paperdoll slots.
	 */
	private int getEquippedCount(Player player)
	{
		int count = 0;
		for (Integer paperdollSlot : _paperdollSlots)
		{
			if (player.getInventory().getPaperdollItem(paperdollSlot) != null)
			{
				count++;
			}
		}
		return count;
	}
}
