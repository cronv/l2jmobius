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
package org.l2jmobius.gameserver.entity.item;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.commons.util.StringUtil;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.item.enums.ItemSkillType;
import org.l2jmobius.gameserver.entity.item.type.WeaponType;
import org.l2jmobius.gameserver.mechanics.events.EventDispatcher;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.npc.OnNpcSkillSee;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class is dedicated to the management of weapons.<br>
 * It handles all logic and properties specific to weapon items in the game world.
 */
public class Weapon extends ItemTemplate
{
	private WeaponType _type;
	private boolean _isMagicWeapon;
	private boolean _isDragonWeapon;
	private boolean _isCursedWeapon;
	private int _soulShotCount;
	private int _spiritShotCount;
	private int _mpConsume;
	private int _baseAttackRange;
	private int _baseAttackRadius;
	private int _baseAttackAngle;
	private int _changeWeaponId;
	
	private int _reducedSoulshot;
	private int _reducedSoulshotChance;
	
	private int _reducedMpConsume;
	private int _reducedMpConsumeChance;
	
	private boolean _isForceEquip;
	private boolean _isAttackWeapon;
	private boolean _useWeaponSkillsOnly;
	
	/**
	 * Creates a new {@code Weapon} instance using the provided statistics.<br>
	 * This constructor initializes the weapon with its base properties.
	 * @param set The {@code StatSet} containing the initial attributes for this weapon.
	 */
	public Weapon(StatSet set)
	{
		super(set);
	}
	
	/**
	 * Updates the weapon properties using a {@code StatSet}.<br>
	 * This method initializes all internal fields from the provided data.<br>
	 * It handles values for types, magic status, and attack ranges.
	 * @param set The {@code StatSet} containing the configuration data.
	 */
	@Override
	public void set(StatSet set)
	{
		super.set(set);
		_type = WeaponType.valueOf(set.getString("weapon_type", "none").toUpperCase());
		_type1 = ItemTemplate.TYPE1_WEAPON_RING_EARRING_NECKLACE;
		_type2 = ItemTemplate.TYPE2_WEAPON;
		_isMagicWeapon = set.getBoolean("is_magic_weapon", false);
		_isDragonWeapon = set.getBoolean("is_dragon_weapon", false);
		_isCursedWeapon = set.getBoolean("is_cursed_weapon", false);
		_soulShotCount = set.getInt("soulshots", 0);
		_spiritShotCount = set.getInt("spiritshots", 0);
		_mpConsume = set.getInt("mp_consume", 0);
		_baseAttackRange = set.getInt("attack_range", 40);
		final String[] damageRange = set.getString("damage_range", "").split(";"); // 0?;0?;fan sector;base attack angle
		if ((damageRange.length > 1) && StringUtil.isNumeric(damageRange[2]) && StringUtil.isNumeric(damageRange[3]))
		{
			_baseAttackRadius = Integer.parseInt(damageRange[2]);
			_baseAttackAngle = Integer.parseInt(damageRange[3]);
		}
		else if (_type == WeaponType.POLE)
		{
			// Polearm templates that omit damage_range must still cleave nearby targets; default to the standard polearm fan instead of 40/0 (which only hits the main target).
			_baseAttackRadius = 66;
			_baseAttackAngle = 120;
		}
		else
		{
			_baseAttackRadius = 40;
			_baseAttackAngle = 0;
		}
		
		final String[] reducedSoulshots = set.getString("reduced_soulshot", "").split(",");
		_reducedSoulshotChance = (reducedSoulshots.length == 2) ? Integer.parseInt(reducedSoulshots[0]) : 0;
		_reducedSoulshot = (reducedSoulshots.length == 2) ? Integer.parseInt(reducedSoulshots[1]) : 0;
		
		final String[] reducedMpConsume = set.getString("reduced_mp_consume", "").split(",");
		_reducedMpConsumeChance = (reducedMpConsume.length == 2) ? Integer.parseInt(reducedMpConsume[0]) : 0;
		_reducedMpConsume = (reducedMpConsume.length == 2) ? Integer.parseInt(reducedMpConsume[1]) : 0;
		_changeWeaponId = set.getInt("change_weaponId", 0);
		_isForceEquip = set.getBoolean("isForceEquip", false);
		_isAttackWeapon = set.getBoolean("isAttackWeapon", true);
		_useWeaponSkillsOnly = set.getBoolean("useWeaponSkillsOnly", false);
		
		// Check if ranged weapon reuse delay is missing.
		if ((_reuseDelay == 0) && _type.isRanged())
		{
			_reuseDelay = 1500;
		}
	}
	
	/**
	 * Retrieves the category of the weapon.<br>
	 * This method returns the {@code WeaponType} associated with this item.
	 * @return the {@code WeaponType} of the item.
	 */
	@Override
	public WeaponType getItemType()
	{
		return _type;
	}
	
	/**
	 * Retrieves the bitmask associated with the weapon type.<br>
	 * This value is used to identify specific categories of weapons.
	 * @return the integer mask for the current {@code WeaponType}.
	 */
	@Override
	public int getItemMask()
	{
		return _type.mask();
	}
	
	/**
	 * Checks if the item is a weapon.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the item is a weapon.
	 */
	@Override
	public boolean isWeapon()
	{
		return true;
	}
	
	/**
	 * Checks if the weapon is classified as a magic weapon.
	 * @return {@code true} if it is a magic weapon, {@code false} otherwise.
	 */
	@Override
	public boolean isMagicWeapon()
	{
		return _isMagicWeapon;
	}
	
	/**
	 * Checks if the weapon is classified as a dragon weapon.
	 * @return {@code true} if it is a dragon weapon, {@code false} otherwise.
	 */
	public boolean isDragonWeapon()
	{
		return _isDragonWeapon;
	}
	
	/**
	 * Checks if the weapon is cursed.<br>
	 * Returns {@code true} if the item has a curse status.<br>
	 * Returns {@code false} otherwise.
	 * @return The curse status of the weapon.
	 */
	public boolean isCursedWeapon()
	{
		return _isCursedWeapon;
	}
	
	/**
	 * Retrieves the total number of soul shots available for this weapon.<br>
	 * This value is used to determine how many times a soul shot can be fired.
	 * @return The current {@code int} count of soul shots.
	 */
	public int getSoulShotCount()
	{
		return _soulShotCount;
	}
	
	/**
	 * Retrieves the total number of spirit shots available for this weapon.<br>
	 * This value is used to determine how many times a spirit shot can be fired.
	 * @return The current {@code spiritShotCount}.
	 */
	public int getSpiritShotCount()
	{
		return _spiritShotCount;
	}
	
	/**
	 * Retrieves the reduced soul shot value.<br>
	 * This value is used to determine the modified count for soul shots.
	 * @return the {@code int} value of the reduced soul shot.
	 */
	public int getReducedSoulShot()
	{
		return _reducedSoulshot;
	}
	
	/**
	 * Retrieves the chance for a reduced soul shot to occur.<br>
	 * This value is used by the {@code Weapon} class to determine skill behavior.
	 * @return The integer percentage of the reduced soul shot chance.
	 */
	public int getReducedSoulShotChance()
	{
		return _reducedSoulshotChance;
	}
	
	/**
	 * Retrieves the mana cost of the weapon.<br>
	 * This value represents the amount of {@code mp} consumed when using the item.
	 * @return The current {@code mp} consumption value.
	 */
	public int getMpConsume()
	{
		return _mpConsume;
	}
	
	/**
	 * Retrieves the base attack range for this weapon.<br>
	 * This value represents the default distance a creature can strike with this item.
	 * @return The base attack range as an {@code int}.
	 */
	public int getBaseAttackRange()
	{
		return _baseAttackRange;
	}
	
	/**
	 * Retrieves the base attack radius for this weapon.<br>
	 * This value determines the circular area of effect for attacks.
	 * @return the base attack radius as an {@code int}.
	 */
	public int getBaseAttackRadius()
	{
		return _baseAttackRadius;
	}
	
	/**
	 * Retrieves the base attack angle for this weapon.<br>
	 * This value determines the default cone of attack.
	 * @return the base attack angle as an {@code int}.
	 */
	public int getBaseAttackAngle()
	{
		return _baseAttackAngle;
	}
	
	/**
	 * Retrieves the reduced MP consumption value for this weapon.<br>
	 * This value is used to determine how much mana is saved during use.
	 * @return the {@code int} value of the reduced MP consumption.
	 */
	public int getReducedMpConsume()
	{
		return _reducedMpConsume;
	}
	
	/**
	 * Gets the chance for reduced MP consumption.<br>
	 * This value determines how often a weapon reduces its mana cost.
	 * @return The chance as an integer.
	 */
	public int getReducedMpConsumeChance()
	{
		return _reducedMpConsumeChance;
	}
	
	/**
	 * Retrieves the unique identifier for the weapon change.<br>
	 * This value is used to identify specific weapon swap actions.
	 * @return the {@code int} ID of the weapon change.
	 */
	public int getChangeWeaponId()
	{
		return _changeWeaponId;
	}
	
	/**
	 * Checks if the weapon is set to be equipped automatically.<br>
	 * Returns {@code true} if force equip is enabled.<br>
	 * Returns {@code false} otherwise.
	 * @return The status of the force equip property.
	 */
	public boolean isForceEquip()
	{
		return _isForceEquip;
	}
	
	/**
	 * Checks if the weapon is categorized as an attack weapon.<br>
	 * This method returns {@code true} if it can be used for attacking.
	 * @return {@code true} if this is an attack weapon, {@code false} otherwise.
	 */
	public boolean isAttackWeapon()
	{
		return _isAttackWeapon;
	}
	
	/**
	 * Checks if the weapon is restricted to using only its own skills.<br>
	 * This determines whether other skills can be used while this weapon is active.
	 * @return {@code true} if only weapon skills are allowed, {@code false} otherwise.
	 */
	public boolean useWeaponSkillsOnly()
	{
		return _useWeaponSkillsOnly;
	}
	
	/**
	 * This method applies skills based on specific conditions.<br>
	 * It checks the chance and requirements for each skill of a given {@code ItemSkillType}.<br>
	 * If all conditions are met, the skill is activated on the target.<br>
	 * Special logic handles magic skills including effects and toggle states.
	 * @param caster The creature performing the action.
	 * @param target The creature receiving the effect.
	 * @param trigger The skill that initiated the check.
	 * @param type The category of item skill to process.
	 */
	public void applyConditionalSkills(Creature caster, Creature target, Skill trigger, ItemSkillType type)
	{
		forEachSkill(type, holder ->
		{
			if (Rnd.get(100) >= holder.getChance())
			{
				return;
			}
			
			final Skill skill = holder.getSkill();
			if (type == ItemSkillType.ON_MAGIC_SKILL)
			{
				// Trigger only if both are good or bad magic.
				if (trigger.hasNegativeEffect() != skill.hasNegativeEffect())
				{
					return;
				}
				
				// No Trigger if not Magic Skill or is toggle.
				if (trigger.isMagic() != skill.isMagic())
				{
					return;
				}
				
				// No Trigger if skill is toggle.
				if (trigger.isToggle())
				{
					return;
				}
				
				if (skill.hasNegativeEffect() && (Formulas.calcShldUse(caster, target) == Formulas.SHIELD_DEFENSE_PERFECT_BLOCK))
				{
					return;
				}
			}
			
			// Skill condition not met.
			if (!skill.checkCondition(caster, target, true))
			{
				return;
			}
			
			skill.activateSkill(caster, target);
			
			// TODO: Verify if this applies ONLY to ON_MAGIC_SKILL!
			if (type == ItemSkillType.ON_MAGIC_SKILL)
			{
				// Notify quests of a skill use.
				final boolean isPlayer = caster.isPlayer();
				if (isPlayer)
				{
					World.forEachVisibleObjectInRange(caster, Npc.class, 1000, npc ->
					{
						if (EventDispatcher.getInstance().hasListener(EventType.ON_NPC_SKILL_SEE, npc))
						{
							EventDispatcher.getInstance().notifyEventAsync(new OnNpcSkillSee(npc, caster.asPlayer(), skill, false, target), npc);
						}
					});
				}
				
				if (isPlayer)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.S1_HAS_BEEN_ACTIVATED);
					sm.addSkillName(skill);
					caster.sendPacket(sm);
				}
			}
		});
	}
}
