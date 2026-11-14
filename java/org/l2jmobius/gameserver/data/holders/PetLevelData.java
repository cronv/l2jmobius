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
package org.l2jmobius.gameserver.data.holders;

import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class defines the statistics associated with each individual pet level.<br>
 * It serves as a data holder for scaling pet attributes based on their progression.
 * @author JIV, Mobius
 */
public class PetLevelData
{
	private final int _ownerExpTaken;
	private final int _petFeedBattle;
	private final int _petFeedNormal;
	private final int _petFeedBattleOnRide;
	private final int _petFeedNormalOnRide;
	private final float _petMAtk;
	private final long _petMaxExp;
	private final int _petMaxFeed;
	private final float _petMaxHP;
	private final float _petMaxMP;
	private final float _petMDef;
	private final float _petPAtk;
	private final float _petPDef;
	private final float _petRegenHP;
	private final float _petRegenMP;
	private final short _petSoulShot;
	private final short _petSpiritShot;
	private final double _walkSpeedOnRide;
	private final double _runSpeedOnRide;
	private final double _slowSwimSpeedOnRide;
	private final double _fastSwimSpeedOnRide;
	private final double _slowFlySpeedOnRide;
	private final double _fastFlySpeedOnRide;
	private final double _pAtkOnRide;
	private final double _mAtkOnRide;
	private final double _attackSpeedOnRide;
	
	/**
	 * Creates a new {@code PetLevelData} instance using the provided stats.<br>
	 * This constructor maps values from a {@code StatSet} to internal pet properties.
	 * @param set The {@code StatSet} containing all required pet statistics.
	 */
	public PetLevelData(StatSet set)
	{
		_ownerExpTaken = set.getInt("get_exp_type");
		_petMaxExp = (long) set.getDouble("exp");
		_petMaxHP = set.getFloat("org_hp");
		_petMaxMP = set.getFloat("org_mp");
		_petPAtk = set.getFloat("org_pattack");
		_petPDef = set.getFloat("org_pdefend");
		_petMAtk = set.getFloat("org_mattack");
		_petMDef = set.getFloat("org_mdefend");
		_petMaxFeed = set.getInt("max_meal");
		_petFeedBattle = set.getInt("consume_meal_in_battle");
		_petFeedNormal = set.getInt("consume_meal_in_normal");
		_petFeedBattleOnRide = set.getInt("consume_meal_in_battle_on_ride", _petFeedBattle);
		_petFeedNormalOnRide = set.getInt("consume_meal_in_normal_on_ride", _petFeedNormal);
		_petRegenHP = set.getFloat("org_hp_regen");
		_petRegenMP = set.getFloat("org_mp_regen");
		_petSoulShot = set.getShort("soulshot_count");
		_petSpiritShot = set.getShort("spiritshot_count");
		_walkSpeedOnRide = set.getDouble("walkSpeedOnRide", 0);
		_runSpeedOnRide = set.getDouble("runSpeedOnRide", 0);
		_slowSwimSpeedOnRide = set.getDouble("slowSwimSpeedOnRide", 0);
		_fastSwimSpeedOnRide = set.getDouble("fastSwimSpeedOnRide", 0);
		_slowFlySpeedOnRide = set.getDouble("slowFlySpeedOnRide", 0);
		_fastFlySpeedOnRide = set.getDouble("fastFlySpeedOnRide", 0);
		_pAtkOnRide = set.getDouble("pattack_on_ride", 0);
		_mAtkOnRide = set.getDouble("mattack_on_ride", 0);
		_attackSpeedOnRide = set.getDouble("attack_speed_on_ride", 0);
	}
	
	/**
	 * Returns the amount of experience gained by the owner.<br>
	 * This value is specific to the current pet level data.
	 * @return the owner experience taken as an {@code int}.
	 */
	public int getOwnerExpTaken()
	{
		return _ownerExpTaken;
	}
	
	/**
	 * Retrieves the amount of feed required for a battle. <br>
	 * This value is specific to the pet's level data.
	 * @return The {@code int} value representing the battle feed requirement.
	 */
	public int getPetFeedBattle()
	{
		return _petFeedBattle;
	}
	
	/**
	 * Retrieves the normal feed amount for a pet.<br>
	 * This value is used to determine how much food is consumed during regular feeding.
	 * @return The integer value of the normal pet feed.
	 */
	public int getPetFeedNormal()
	{
		return _petFeedNormal;
	}
	
	/**
	 * Retrieves the feed consumed in battle while the pet is ridden.
	 * @return The battle feed rate on ride, the plain battle rate when the data has none.
	 */
	public int getPetFeedBattleOnRide()
	{
		return _petFeedBattleOnRide;
	}
	
	/**
	 * Retrieves the feed consumed out of battle while the pet is ridden.
	 * @return The normal feed rate on ride, the plain normal rate when the data has none.
	 */
	public int getPetFeedNormalOnRide()
	{
		return _petFeedNormalOnRide;
	}
	
	/**
	 * Retrieves the P. Atk. the mount attacks with while ridden.
	 * @return the ridden P. Atk., or 0 if the mount has none.
	 */
	public double getPAtkOnRide()
	{
		return _pAtkOnRide;
	}
	
	/**
	 * Retrieves the M. Atk. the mount attacks with while ridden.
	 * @return the ridden M. Atk., or 0 if the mount has none.
	 */
	public double getMAtkOnRide()
	{
		return _mAtkOnRide;
	}
	
	/**
	 * Retrieves the attack speed of the mount while ridden.
	 * @return the ridden attack speed, or 0 if the mount has none.
	 */
	public double getAttackSpeedOnRide()
	{
		return _attackSpeedOnRide;
	}
	
	/**
	 * Retrieves the Magic Attack value for the pet.<br>
	 * This value is specific to the current {@code PetLevelData}.
	 * @return The magic attack value as a {@code float}.
	 */
	public float getPetMAtk()
	{
		return _petMAtk;
	}
	
	/**
	 * Retrieves the maximum experience points for a pet.<br>
	 * This value is specific to the current {@code PetLevelData}.
	 * @return The maximum experience as a {@code long}.
	 */
	public long getPetMaxExp()
	{
		return _petMaxExp;
	}
	
	/**
	 * Retrieves the maximum feed amount for a pet.<br>
	 * This value is specific to the current {@code PetLevelData}.
	 * @return The maximum number of feeds allowed.
	 */
	public int getPetMaxFeed()
	{
		return _petMaxFeed;
	}
	
	/**
	 * Retrieves the maximum health points for a pet.<br>
	 * This value is specific to the current {@code PetLevelData}.
	 * @return The maximum HP of the pet as a {@code float}.
	 */
	public float getPetMaxHP()
	{
		return _petMaxHP;
	}
	
	/**
	 * Retrieves the maximum mana points for a pet.<br>
	 * This value is specific to the current {@code PetLevelData}.
	 * @return The maximum mana of the pet as a {@code float}.
	 */
	public float getPetMaxMP()
	{
		return _petMaxMP;
	}
	
	/**
	 * Retrieves the magic defense value for the pet.<br>
	 * This value determines how much magic damage the pet can resist.
	 * @return The {@code float} value of the pet's magic defense.
	 */
	public float getPetMDef()
	{
		return _petMDef;
	}
	
	/**
	 * Retrieves the physical attack power of the pet.<br>
	 * This value is specific to the current level data.
	 * @return The {@code float} value of the pet's physical attack.
	 */
	public float getPetPAtk()
	{
		return _petPAtk;
	}
	
	/**
	 * Retrieves the physical defense value for the pet.<br>
	 * This value determines how much physical damage the pet can withstand.
	 * @return The {@code float} value of the pet's physical defense.
	 */
	public float getPetPDef()
	{
		return _petPDef;
	}
	
	/**
	 * Retrieves the health regeneration rate for a pet.<br>
	 * This value is specific to the current {@code PetLevelData}.
	 * @return The health regeneration amount as a {@code float}.
	 */
	public float getPetRegenHP()
	{
		return _petRegenHP;
	}
	
	/**
	 * Retrieves the mana regeneration rate for a pet.<br>
	 * This value is specific to the current level data.
	 * @return the {@code float} value of the pet's mana regeneration.
	 */
	public float getPetRegenMP()
	{
		return _petRegenMP;
	}
	
	/**
	 * Retrieves the soul shot value for the pet.<br>
	 * This value is specific to the current level data.
	 * @return the {@code short} value of the pet soul shot.
	 */
	public short getPetSoulShot()
	{
		return _petSoulShot;
	}
	
	/**
	 * Retrieves the spirit shot value for the pet.<br>
	 * This value is specific to the current {@code PetLevelData}.
	 * @return The {@code short} value of the pet's spirit shot.
	 */
	public short getPetSpiritShot()
	{
		return _petSpiritShot;
	}
	
	/**
	 * Retrieves the movement speed for a specific type of travel.<br>
	 * This method checks the provided {@code Stat} to determine which speed value to return.
	 * @param stat The {@code Stat} type representing the movement category.
	 * @return The corresponding speed value as a {@code double}.
	 */
	public double getSpeedOnRide(Stat stat)
	{
		switch (stat)
		{
			case WALK_SPEED:
			{
				return _walkSpeedOnRide;
			}
			case RUN_SPEED:
			{
				return _runSpeedOnRide;
			}
			case SWIM_WALK_SPEED:
			{
				return _slowSwimSpeedOnRide;
			}
			case SWIM_RUN_SPEED:
			{
				return _fastSwimSpeedOnRide;
			}
			case FLY_RUN_SPEED:
			{
				return _slowFlySpeedOnRide;
			}
			case FLY_WALK_SPEED:
			{
				return _fastFlySpeedOnRide;
			}
			default:
			{
				return 0;
			}
		}
	}
}
