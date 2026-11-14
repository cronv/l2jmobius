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

import org.l2jmobius.gameserver.config.custom.ClassBalanceConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for energy attack effects.<br>
 * It calculates and applies damage based on the {@code EnergyAttack} mechanic.
 * @author NosBit, Mobius
 */
public class EnergyDamage extends AbstractEffect
{
	private final double _power;
	private final int _chargeConsume;
	private final int _criticalChance;
	private final boolean _ignoreShieldDefence;
	private final boolean _overHit;
	private final double _pDefMod;
	
	/**
	 * Creates a new {@code EnergyDamage} effect.<br>
	 * This constructor initializes the damage properties from a {@code StatSet}.<br>
	 * It validates that the configuration uses {@code power} instead of {@code amount}.
	 * @param params The {@code StatSet} containing all required effect parameters.
	 */
	public EnergyDamage(StatSet params)
	{
		_power = params.getDouble("power", 0);
		_criticalChance = params.getInt("criticalChance", 10);
		_ignoreShieldDefence = params.getBoolean("ignoreShieldDefence", false);
		_overHit = params.getBoolean("overHit", false);
		_chargeConsume = params.getInt("chargeConsume", 0);
		_pDefMod = params.getDouble("pDefMod", 1.0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the effect successfully hits the target.<br>
	 * This method checks if the {@code effector} succeeds in hitting the {@code effected} creature.<br>
	 * It returns {@code true} if the attack is not evaded.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used for the calculation.
	 * @return {@code true} if the hit is successful, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		// TODO: Verify this on retail
		return !Formulas.calcSkillEvasion(effector, effected, skill);
	}
	
	/**
	 * Retrieves the category of this effect.<br>
	 * This method returns the {@code EffectType} associated with the damage.
	 * @return The {@code EffectType} for this specific effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.PHYSICAL_ATTACK;
	}
	
	/**
	 * Checks if this effect is triggered instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant damage effect for an energy attack.<br>
	 * This method handles charge consumption and calculates final damage based on various modifiers.<br>
	 * It applies the resulting damage to the target creature.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used for the attack.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effector.isPlayer())
		{
			return;
		}
		
		final Player attacker = effector.asPlayer();
		final int charge = Math.min(_chargeConsume, attacker.getCharges());
		if (!attacker.decreaseCharges(charge))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addSkillName(skill);
			attacker.sendPacket(sm);
			return;
		}
		
		if (_overHit && effected.isAttackable())
		{
			effected.asAttackable().overhitEnabled(true);
		}
		
		final double defenceIgnoreRemoval = effected.getStat().getValue(Stat.DEFENCE_IGNORE_REMOVAL, 1);
		final double defenceIgnoreRemovalAdd = effected.getStat().getValue(Stat.DEFENCE_IGNORE_REMOVAL_ADD, 0);
		final double pDefMod = Math.min(1, (defenceIgnoreRemoval - 1) + (_pDefMod));
		final int pDef = effected.getPDef();
		double ignoredPDef = pDef - (pDef * pDefMod);
		if (ignoredPDef > 0)
		{
			ignoredPDef = Math.max(0, ignoredPDef - defenceIgnoreRemovalAdd);
		}
		
		double defence = effected.getPDef() - ignoredPDef;
		
		final double shieldDefenceIgnoreRemoval = effected.getStat().getValue(Stat.SHIELD_DEFENCE_IGNORE_REMOVAL, 1);
		final double shieldDefenceIgnoreRemovalAdd = effected.getStat().getValue(Stat.SHIELD_DEFENCE_IGNORE_REMOVAL_ADD, 0);
		if (!_ignoreShieldDefence || (shieldDefenceIgnoreRemoval > 1) || (shieldDefenceIgnoreRemovalAdd > 0))
		{
			final byte shield = Formulas.calcShldUse(attacker, effected);
			switch (shield)
			{
				case Formulas.SHIELD_DEFENSE_SUCCEED:
				{
					final int shieldDef = effected.getShldDef();
					if (_ignoreShieldDefence)
					{
						final double shieldDefMod = Math.max(0, shieldDefenceIgnoreRemoval - 1);
						double ignoredShieldDef = shieldDef - (shieldDef * shieldDefMod);
						if (ignoredShieldDef > 0)
						{
							ignoredShieldDef = Math.max(0, ignoredShieldDef - shieldDefenceIgnoreRemovalAdd);
						}
						
						defence += shieldDef - ignoredShieldDef;
					}
					else
					{
						defence += effected.getShldDef();
					}
					break;
				}
				case Formulas.SHIELD_DEFENSE_PERFECT_BLOCK:
				{
					defence = -1;
					break;
				}
			}
		}
		
		double damage = 1;
		final boolean critical = Formulas.calcCrit(_criticalChance, attacker, effected, skill);
		
		if (defence != -1)
		{
			if (effected.isPlayable())
			{
				final int effectedPlayerClassId = effected.asPlayer().getPlayerClass().getId();
				defence *= attacker.isPlayable() ? ClassBalanceConfig.PVP_ENERGY_SKILL_DEFENCE_MULTIPLIERS[effectedPlayerClassId] : ClassBalanceConfig.PVE_ENERGY_SKILL_DEFENCE_MULTIPLIERS[effectedPlayerClassId];
			}
			
			// Trait, elements
			final double weaponTraitMod = Formulas.calcWeaponTraitBonus(attacker, effected);
			final double generalTraitMod = Formulas.calcGeneralTraitBonus(attacker, effected, skill.getTraitType(), true);
			final double weaknessMod = Formulas.calcWeaknessBonus(attacker, effected, skill.getTraitType());
			final double attributeMod = Formulas.calcAttributeBonus(attacker, effected, skill);
			final double pvpPveMod = Formulas.calculatePvpPveBonus(attacker, effected, skill, true);
			
			// Skill specific mods.
			final double energyChargesBoost = 1 + (charge * 0.1); // 10% bonus damage for each charge used.
			final double chargingSkillPowerMod = effector.getStat().getValue(Stat.CHARGING_SKILL_POWER, 1);
			final double critMod = critical ? Formulas.calcCritDamage(attacker, effected, skill) : 1;
			double ssmod = 1;
			if (skill.useSoulShot())
			{
				if (attacker.isChargedShot(ShotType.SOULSHOTS))
				{
					ssmod = Math.max(1, (2 * effector.getStat().getValue(Stat.SHOTS_BONUS)) - (effected.getStat().getValue(Stat.SOULSHOT_RESISTANCE, 0) / 100)); // 2.04 for dual weapon?
				}
				else if (attacker.isChargedShot(ShotType.BLESSED_SOULSHOTS))
				{
					ssmod = Math.max(1, (4 * effector.getStat().getValue(Stat.SHOTS_BONUS)) - (effected.getStat().getValue(Stat.SOULSHOT_RESISTANCE, 0) / 100));
				}
			}
			
			// Calculate the attack damage by multiplying 77 by the sum of adjusted attack and power, scaled by charge consumption and defense, then multiplied by skill power and added to skill power addition.
			// ```````````````````^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^```^^^^^^^^^^^^^^^^^^^^^^^^^^^^^```^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
			final double baseMod = (77 * ((attacker.getPAtk() * attacker.getLevelMod()) + _power + effector.getStat().getValue(Stat.SKILL_POWER_ADD, 0))) / defence;
			damage = baseMod * ssmod * critMod * weaponTraitMod * generalTraitMod * weaknessMod * attributeMod * energyChargesBoost * chargingSkillPowerMod * pvpPveMod;
		}
		
		double balanceMod = 1;
		if (attacker.isPlayable())
		{
			final int playerClassId = attacker.asPlayer().getPlayerClass().getId();
			balanceMod = effected.isPlayable() ? ClassBalanceConfig.PVP_ENERGY_SKILL_DAMAGE_MULTIPLIERS[playerClassId] : ClassBalanceConfig.PVE_ENERGY_SKILL_DAMAGE_MULTIPLIERS[playerClassId];
		}
		
		damage = Math.max(0, damage * effector.getStat().getValue(Stat.PHYSICAL_SKILL_POWER, 1)) * balanceMod;
		effector.doAttack(damage, effected, skill, false, false, critical, false);
	}
}
