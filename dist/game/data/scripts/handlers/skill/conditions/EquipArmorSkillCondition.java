/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.conditions;

import java.util.List;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.ItemTemplate;
import org.l2jmobius.gameserver.entity.item.enums.BodyPart;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.ArmorType;
import org.l2jmobius.gameserver.entity.itemcontainer.Inventory;
import org.l2jmobius.gameserver.mechanics.skill.ISkillCondition;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Checks if a character is wearing specific armor to satisfy a skill requirement.<br>
 * This condition validates the presence of an {@code Item} on a required {@code BodyPart}.<br>
 * It ensures that the equipped gear matches the criteria defined for the {@code Skill}.
 * @author Sdw
 */
public class EquipArmorSkillCondition implements ISkillCondition
{
	private int _armorTypesMask = 0;
	
	/**
	 * Creates a new condition based on required armor types.<br>
	 * It reads the allowed {@code ArmorType} values from the provided set.<br>
	 * This is used to check if a player is wearing specific gear.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public EquipArmorSkillCondition(StatSet params)
	{
		final List<ArmorType> armorTypes = params.getEnumList("armorType", ArmorType.class);
		if (armorTypes != null)
		{
			for (ArmorType armorType : armorTypes)
			{
				_armorTypesMask |= armorType.mask();
			}
		}
	}
	
	/**
	 * Checks if the caster meets the armor requirements to use a skill.<br>
	 * The caster must be a player and have specific chest or leg armor equipped.
	 * @param caster The {@code Creature} attempting to use the skill.
	 * @param skill The {@code Skill} being cast.
	 * @param target The {@code WorldObject} targeted by the skill.
	 * @return {@code true} if all armor conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canUse(Creature caster, Skill skill, WorldObject target)
	{
		if ((caster == null) || !caster.isPlayer())
		{
			return false;
		}
		
		// Get the itemMask of the weared chest (if exists).
		final Inventory inv = caster.getInventory();
		final Item chest = inv.getPaperdollItem(Inventory.PAPERDOLL_CHEST);
		if (chest == null)
		{
			return false;
		}
		
		// If chest armor is different from the condition one return false.
		final ItemTemplate chestTemplate = chest.getTemplate();
		final int chestMask = chestTemplate.getItemMask();
		if ((_armorTypesMask & chestMask) == 0)
		{
			return false;
		}
		
		// So from here, chest armor matches conditions.
		
		// Return true if chest armor is a Full Armor.
		final BodyPart chestBodyPart = chestTemplate.getBodyPart();
		if (chestBodyPart == BodyPart.FULL_ARMOR)
		{
			return true;
		}
		
		// Check legs armor.
		final Item legs = inv.getPaperdollItem(Inventory.PAPERDOLL_LEGS);
		if (legs == null)
		{
			return false;
		}
		
		// Return true if legs armor matches too.
		final int legMask = legs.getTemplate().getItemMask();
		return (_armorTypesMask & legMask) != 0;
	}
}
