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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the application of magical damage based on the target's {@code mp} stat.<br>
 * This effect is used to calculate and apply damage for specific skills.
 * @author Adry_85
 */
public class MagicalDamageMp extends AbstractEffect
{
	private final double _power;
	private final boolean _critical;
	private final double _criticalLimit;
	
	/**
	 * Creates a new instance of {@code MagicalDamageMp}.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It extracts power, critical status, and critical limits.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public MagicalDamageMp(StatSet params)
	{
		_power = params.getDouble("power");
		_critical = params.getBoolean("critical");
		_criticalLimit = params.getDouble("criticalLimit");
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the magical damage effect can be successfully applied.<br>
	 * It checks for MP blocks and specific duelist fury conditions.<br>
	 * It also validates if the magic hit is successful based on formulas.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @return {@code true} if the effect succeeds, or {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isMpBlocked())
		{
			return false;
		}
		
		final boolean effectorIsPlayer = effector.isPlayer();
		final boolean effectedIsPlayer = effected.isPlayer();
		if (effectorIsPlayer && effectedIsPlayer && effected.isAffected(EffectFlag.DUELIST_FURY) && !effector.isAffected(EffectFlag.DUELIST_FURY))
		{
			return false;
		}
		
		if (!Formulas.calcMagicAffected(effector, effected, skill))
		{
			if (effectorIsPlayer)
			{
				effector.sendPacket(SystemMessageId.YOUR_ATTACK_HAS_FAILED);
			}
			
			if (effectedIsPlayer)
			{
				final SystemMessage sm = new SystemMessage(SystemMessageId.C1_RESISTED_C2_S_DRAIN);
				sm.addString(effected.getName());
				sm.addString(effector.getName());
				effected.sendPacket(sm);
			}
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return the {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_ATTACK;
	}
	
	/**
	 * Checks if this effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Applies the magical damage effect to a target immediately.<br>
	 * This method calculates and reduces the mana of the {@code effected} creature.<br>
	 * It also sends system messages to both players if applicable.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		final boolean sps = skill.useSpiritShot() && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = skill.useSpiritShot() && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final byte shld = Formulas.calcShldUse(effector, effected);
		final boolean mcrit = _critical && Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		final double damage = Formulas.calcManaDam(effector, effected, skill, _power, shld, sps, bss, mcrit, _criticalLimit);
		final double mp = Math.min(effected.getCurrentMp(), damage);
		
		if (damage > 0)
		{
			effected.stopEffectsOnDamage();
			effected.setCurrentMp(effected.getCurrentMp() - mp);
		}
		
		if (effected.isPlayer())
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S2_S_MP_HAS_BEEN_DRAINED_BY_C1);
			sm.addString(effector.getName());
			sm.addInt((int) mp);
			effected.sendPacket(sm);
		}
		
		if (effector.isPlayer())
		{
			final SystemMessage sm2 = new SystemMessage(SystemMessageId.YOUR_OPPONENT_S_MP_WAS_REDUCED_BY_S1);
			sm2.addInt((int) mp);
			effector.sendPacket(sm2);
		}
	}
}
