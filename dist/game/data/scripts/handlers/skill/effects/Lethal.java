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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code Lethal} effect implementation.<br>
 * It defines how lethal mechanics are applied to entities within the game.
 * @author Adry_85
 */
public class Lethal extends AbstractEffect
{
	private final double _fullLethal;
	private final double _halfLethal;
	
	/**
	 * Creates a new {@code Lethal} effect instance.<br>
	 * This constructor initializes the lethal values from the provided parameters.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public Lethal(StatSet params)
	{
		_fullLethal = params.getDouble("fullLethal", 0);
		_halfLethal = params.getDouble("halfLethal", 0);
	}
	
	/**
	 * Checks if the effect is applied instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this lethal effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.LETHAL_ATTACK;
	}
	
	/**
	 * Applies a lethal strike effect to a target.<br>
	 * This method checks for various conditions such as level requirements and resistances.<br>
	 * It determines if the target receives a full kill or a half-kill based on calculated chances.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final boolean isEffectorPlayer = effector.isPlayer();
		if (isEffectorPlayer && !effector.getAccessLevel().canGiveDamage())
		{
			return;
		}
		
		if (skill.getMagicLevel() < (effected.getLevel() - 6))
		{
			return;
		}
		
		if (!effected.isLethalable() || effected.isHpBlocked())
		{
			return;
		}
		
		final boolean isEffectedPlayer = effected.isPlayer();
		if (isEffectorPlayer && isEffectedPlayer && effected.isAffected(EffectFlag.DUELIST_FURY) && !effector.isAffected(EffectFlag.DUELIST_FURY))
		{
			return;
		}
		
		final double chanceMultiplier = Formulas.calcAttributeBonus(effector, effected, skill) * Formulas.calcGeneralTraitBonus(effector, effected, skill.getTraitType(), false);
		
		// Calculate instant kill resistance first.
		if (Rnd.get(100) < effected.getStat().getValue(Stat.INSTANT_KILL_RESIST, 0))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.C1_HAS_EVADED_C2_S_ATTACK);
			sm.addString(effected.getName());
			sm.addString(effector.getName());
			effected.sendPacket(sm);
			final SystemMessage sm2 = new SystemMessage(SystemMessageId.C1_S_ATTACK_WENT_ASTRAY);
			sm2.addString(effector.getName());
			effector.sendPacket(sm2);
		}
		// Lethal Strike
		else if (Rnd.get(100) < (_fullLethal * chanceMultiplier))
		{
			// for Players CP and HP is set to 1.
			if (isEffectedPlayer)
			{
				effected.setCurrentCp(1);
				effected.setCurrentHp(1);
				effected.sendPacket(SystemMessageId.LETHAL_STRIKE);
			}
			// For Monsters HP is set to 1.
			else if (effected.isMonster() || effected.isSummon())
			{
				effected.setCurrentHp(1);
			}
			
			effector.sendPacket(SystemMessageId.HIT_WITH_LETHAL_STRIKE);
		}
		// Half-Kill
		else if (Rnd.get(100) < (_halfLethal * chanceMultiplier))
		{
			// for Players CP is set to 1.
			if (isEffectedPlayer)
			{
				effected.setCurrentCp(1);
				effected.sendPacket(SystemMessageId.HALF_KILL);
				effected.sendPacket(SystemMessageId.YOUR_CP_WAS_DRAINED_BECAUSE_YOU_WERE_HIT_WITH_A_HALF_KILL_SKILL);
			}
			// For Monsters HP is set to 50%.
			else if (effected.isMonster() || effected.isSummon())
			{
				effected.setCurrentHp(effected.getCurrentHp() * 0.5);
			}
			
			effector.sendPacket(SystemMessageId.HALF_KILL);
		}
		
		// No matter if lethal succeeded or not, it's reflected.
		Formulas.calcCounterAttack(effector, effected, skill, false);
	}
}
