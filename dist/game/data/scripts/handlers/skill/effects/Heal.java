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
 * This class handles the logic for a healing effect.<br>
 * It applies health restoration to a {@code Creature} when triggered by a skill.<br>
 * It serves as an implementation of {@code AbstractEffect}.
 * @author UnAfraid
 */
public class Heal extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code Heal} effect.<br>
	 * This constructor initializes the healing power from the provided parameters.<br>
	 * It ensures that the {@code power} value is used instead of an {@code amount} key.
	 * @param params The {@code StatSet} containing the configuration values for this effect.
	 */
	public Heal(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this heal effect.
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
	 * Applies the healing effect instantly to a target.<br>
	 * This method calculates the heal amount based on stats, items, and skills.<br>
	 * It handles special cases like spiritshots, critical heals, and overheal prevention.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the healing effect.
	 * @param skill The {@code Skill} being used to trigger the heal.
	 * @param item The {@code Item} used in the action, if any.
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
		if ((item != null) && (item.isPotion() || item.isElixir()))
		{
			amount += effected.getStat().getValue(Stat.ADDITIONAL_POTION_HP, 0);
		}
		
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
			if (skill.isMagic() && Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill))
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
		
		// Prevents overheal
		final double currentHp = effected.getCurrentHp();
		amount = Math.min(amount, Math.max(0, effected.getMaxRecoverableHp() - currentHp));
		if (amount != 0)
		{
			final double newHp = amount + currentHp;
			effected.setCurrentHp(newHp, false);
			effected.broadcastStatusUpdate(effector);
		}
		
		if (effected.isPlayer())
		{
			if (skill.getId() == 4051)
			{
				effected.sendPacket(SystemMessageId.RECOVERS_HP);
			}
			else if (effector.isPlayer() && (effector != effected))
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.S2_HP_HAS_BEEN_RECOVERED_BY_C1);
				sm.addString(effector.getName());
				sm.addInt((int) amount);
				effected.sendPacket(sm);
			}
			else
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.YOU_VE_RECOVERED_S1_HP);
				sm.addInt((int) amount);
				effected.sendPacket(sm);
			}
		}
	}
}
