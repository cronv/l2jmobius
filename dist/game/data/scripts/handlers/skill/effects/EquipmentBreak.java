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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory.ItemSkillsListener;
import org.l2jmobius.gameserver.entity.itemcontainer.PlayerInventory;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.AbnormalType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.ExBreakEquipmentNoti;
import org.l2jmobius.gameserver.network.serverpackets.InventoryUpdate;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * @author Galagard
 */
public class EquipmentBreak extends AbstractEffect
{
	private final double _chance;
	private final boolean _armor;
	
	public EquipmentBreak(StatSet params)
	{
		_chance = params.getDouble("chance", 100.0);
		_armor = params.getBoolean("armor", false);
	}
	
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return (Rnd.get(100.0) < _chance);
	}
	
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!GeneralConfig.ALLOW_EQUIPMENT_BREAK)
		{
			return;
		}
		
		if ((effected == null) || !effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		
		if (_armor)
		{
			// Check if target is protected by the Pierce Defense abnormal.
			if (player.getEffectList().hasAbnormalType(AbnormalType.DEFENSE_PIERCING))
			{
				return;
			}
			
			// Collect equipped, non-damaged items from chest, head, legs, gloves, feet and left hand (shield/sigil).
			final List<Item> equippedArmor = new ArrayList<>();
			final int[] slots =
			{
				Inventory.PAPERDOLL_HEAD,
				Inventory.PAPERDOLL_CHEST,
				Inventory.PAPERDOLL_LEGS,
				Inventory.PAPERDOLL_GLOVES,
				Inventory.PAPERDOLL_FEET,
				Inventory.PAPERDOLL_LHAND
			};
			final PlayerInventory inventory = player.getInventory();
			for (int slot : slots)
			{
				final Item armorItem = inventory.getPaperdollItem(slot);
				if ((armorItem != null) && !armorItem.isDamaged() && !equippedArmor.contains(armorItem))
				{
					equippedArmor.add(armorItem);
				}
			}
			
			if (equippedArmor.isEmpty())
			{
				return;
			}
			
			// Select a random item.
			final Item armorToBreak = equippedArmor.get(Rnd.get(equippedArmor.size()));
			final int slot = armorToBreak.getLocationSlot();
			
			// Break the armor.
			ItemSkillsListener.getInstance().notifyUnequiped(slot, armorToBreak, inventory);
			
			armorToBreak.setDamaged(true);
			
			ItemSkillsListener.getInstance().notifyEquiped(slot, armorToBreak, inventory);
			
			player.getStat().recalculateStats(true);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(armorToBreak);
			player.sendInventoryUpdate(iu);
			player.broadcastUserInfo();
			
			player.sendPacket(new ExBreakEquipmentNoti(inventory.getDamagedArmorCount(), inventory.getDamagedWeaponsCount()));
			
			// Apply the protective buff Pierce Defense (skill 88468 level 1) to the target.
			final Skill pierceDefense = SkillData.getInstance().getSkill(88468, 1);
			if (pierceDefense != null)
			{
				pierceDefense.applyEffects(effector, player);
			}
			
			// Ancient Power: Master interaction, 50% chance to remove the target's protective barrier.
			if ((effector != null) && (effector.getSkillLevel(88460) >= 2))
			{
				if (Rnd.get(100.0) < 50.0)
				{
					player.getEffectList().stopEffects(AbnormalType.BARRIER);
				}
			}
		}
		else
		{
			// Weapon break.
			final Item weapon = player.getActiveWeaponInstance();
			if (weapon == null)
			{
				return;
			}
			
			final PlayerInventory inventory = player.getInventory();
			ItemSkillsListener.getInstance().notifyUnequiped(Inventory.PAPERDOLL_RHAND, weapon, inventory);
			
			weapon.setDamaged(true);
			
			ItemSkillsListener.getInstance().notifyEquiped(Inventory.PAPERDOLL_RHAND, weapon, inventory);
			
			player.getStat().recalculateStats(true);
			
			final InventoryUpdate iu = new InventoryUpdate();
			iu.addModifiedItem(weapon);
			player.sendInventoryUpdate(iu);
			player.broadcastUserInfo();
			
			player.sendPacket(new ExBreakEquipmentNoti(inventory.getDamagedArmorCount(), inventory.getDamagedWeaponsCount()));
		}
	}
}
