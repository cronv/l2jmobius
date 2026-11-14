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
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.entity.item.type.CrystalType;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExMagicAttackInfo;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code HpCpHeal} effect.<br>
 * It restores health and maximum health to a target creature.
 * @author Sdw, Mobius
 */
public class HpCpHeal extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code HpCpHeal} effect.<br>
	 * This constructor initializes the healing power from the provided stats.<br>
	 * It ensures that the {@code power} key is used instead of {@code amount}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public HpCpHeal(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL;
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies an instant healing and CP recovery effect to a target.<br>
	 * This method calculates the heal amount based on skill power, magic attack, and item bonuses.<br>
	 * It handles critical heals, potion bonuses, and prevents overhealing.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the healing effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} involved in the action, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor() || effected.isHpBlocked())
		{
			return;
		}
		
		if ((effected != effector) && effected.isAffected(EffectFlag.FACEOFF))
		{
			return;
		}
		
		double amount = _power;
		double staticShotBonus = 0;
		double mAtkMul = 1;
		final boolean sps = skill.isMagic() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.isMagic() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final double shotsBonus = effector.getStat().getValue(Stat.SHOTS_BONUS);
		
		if (((sps || bss) && (effector.isPlayer() && effector.asPlayer().isMageClass())) || effector.isSummon())
		{
			staticShotBonus = skill.getMpConsume(); // static bonus for spiritshots
			mAtkMul = bss ? 4 * shotsBonus : 2 * shotsBonus;
			staticShotBonus *= bss ? 2.4 : 1.0;
		}
		else if ((sps || bss) && effector.isNpc())
		{
			staticShotBonus = 2.4 * skill.getMpConsume(); // always blessed spiritshots
			mAtkMul = 4 * shotsBonus;
		}
		else
		{
			// No static bonus or dynamic bonus.
			final Item weaponInst = effector.getActiveWeaponInstance();
			if (weaponInst != null)
			{
				mAtkMul = weaponInst.getTemplate().getCrystalTypePlus() == CrystalType.L ? 4 : weaponInst.getTemplate().getCrystalTypePlus() == CrystalType.R ? 4 : weaponInst.getTemplate().getCrystalTypePlus() == CrystalType.S ? 2 : 1;
			}
			
			// shot dynamic bonus
			mAtkMul = bss ? mAtkMul * 4 : mAtkMul + 1;
		}
		
		if (!skill.isStatic())
		{
			amount += staticShotBonus + Math.sqrt(mAtkMul * effector.getMAtk());
			amount *= effected.getStat().getValue(Stat.HEAL_EFFECT, 1);
			amount += effected.getStat().getValue(Stat.HEAL_EFFECT_ADD, 0);
			amount *= (item == null) && effector.isPlayable() ? ClassBalanceConfig.PLAYER_HEALING_SKILL_MULTIPLIERS[effector.asPlayer().getPlayerClass().getId()] : 1f;
			
			// Heal critic, since CT2.3 Gracia Final.
			if (skill.isMagic() && (Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill) || effector.isAffected(EffectFlag.HPCPHEAL_CRITICAL)))
			{
				amount *= 3;
				effector.sendPacket(SystemMessageId.M_CRITICAL);
				final int objectId = effector.getObjectId();
				final int effectedObjectId = effected.getObjectId();
				effector.sendPacket(new ExMagicAttackInfo(objectId, effectedObjectId, ExMagicAttackInfo.CRITICAL_HEAL));
				if (effected.isPlayer() && (effected != effector))
				{
					effected.sendPacket(new ExMagicAttackInfo(objectId, effectedObjectId, ExMagicAttackInfo.CRITICAL_HEAL));
				}
			}
		}
		
		// Additional potion HP.
		double additionalHp = 0;
		
		// Additional potion CP.
		double additionalCp = 0;
		
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			additionalHp = effected.getStat().getValue(Stat.ADDITIONAL_POTION_HP, 0);
			additionalCp = effected.getStat().getValue(Stat.ADDITIONAL_POTION_CP, 0);
		}
		
		// Prevents overheal and negative amount.
		final long maxRecoverableHp = effected.getMaxRecoverableHp();
		final double currentHp = effected.getCurrentHp();
		final double healAmount = Math.max(Math.min(amount, maxRecoverableHp - currentHp), 0);
		if (healAmount != 0)
		{
			final double newHp = healAmount + currentHp;
			if ((newHp + additionalHp) > maxRecoverableHp)
			{
				additionalHp = Math.max(maxRecoverableHp - newHp, 0);
			}
			
			effected.setCurrentHp(newHp + additionalHp, false);
			
			if (effector.isPlayer() && (effector != effected))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S2_HP_HAS_BEEN_RECOVERED_BY_C1);
				sm.addString(effector.getName());
				sm.addInt((int) (healAmount + additionalHp));
				effected.sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_VE_RECOVERED_S1_HP);
				sm.addInt((int) (healAmount + additionalHp));
				effected.sendPacket(sm);
			}
		}
		
		// CP recovery.
		if (effected.isPlayer())
		{
			final int maxRecoverableCp = effected.getMaxRecoverableCp();
			final double currentCp = effected.getCurrentCp();
			amount = Math.max(Math.min(amount - healAmount, maxRecoverableCp - currentCp), 0);
			if (amount != 0)
			{
				final double newCp = amount + currentCp;
				if ((newCp + additionalCp) > maxRecoverableCp)
				{
					additionalCp = Math.max(maxRecoverableCp - newCp, 0);
				}
				
				effected.setCurrentCp(newCp + additionalCp, false);
				
				if (effector != effected)
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOVERED_S2_CP_WITH_C1_S_HELP);
					sm.addString(effector.getName());
					sm.addInt((int) (amount + additionalCp));
					effected.sendPacket(sm);
				}
				else
				{
					final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_HAVE_RECOVERED_S1_CP);
					sm.addInt((int) (amount + additionalCp));
					effected.sendPacket(sm);
				}
			}
		}
		
		effected.broadcastStatusUpdate(effector);
	}
}
