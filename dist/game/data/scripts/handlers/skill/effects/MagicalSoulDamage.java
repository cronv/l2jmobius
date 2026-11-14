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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ShotType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the application of magical soul damage effects.<br>
 * It defines how {@code MagicalSoulDamage} interacts with targets in the game world.
 * @author Adry_85
 */
public class MagicalSoulDamage extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new instance of the {@code MagicalSoulDamage} effect.<br>
	 * It initializes the damage power from the provided statistics.<br>
	 * This constructor will throw an exception if the {@code amount} key is used instead of {@code power}.
	 * @param params The {@code StatSet} containing the required attributes for this effect.
	 */
	public MagicalSoulDamage(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Determines if the effect successfully hits the target.<br>
	 * This method checks if the {@code effector} fails to evade the {@code skill}.<br>
	 * It returns {@code true} if the hit is successful and {@code false} otherwise.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect hits, {@code false} if it is evaded.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !Formulas.calcSkillEvasion(effector, effected, skill);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MAGICAL_ATTACK;
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
	 * Executes the instant effect of a magical soul damage skill.<br>
	 * This method handles charge consumption and calculates final damage.<br>
	 * It also manages fake death states for player targets.
	 * @param effector The {@code Creature} casting the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effector.isAlikeDead())
		{
			return;
		}
		
		if (effected.isPlayer() && effected.asPlayer().isFakeDeath() && PlayerConfig.FAKE_DEATH_DAMAGE_STAND)
		{
			effected.stopFakeDeath(true);
		}
		
		final Player caster = effector.asPlayer();
		final int chargedSouls = Math.min(skill.getMaxSoulConsumeCount(), caster.getCharges());
		if (!caster.decreaseCharges(chargedSouls))
		{
			final SystemMessage sm = new SystemMessage(SystemMessageId.S1_CANNOT_BE_USED_THE_REQUIREMENTS_ARE_NOT_MET);
			sm.addSkillName(skill);
			effector.sendPacket(sm);
			return;
		}
		
		final boolean spiritShotUsed = skill.useSpiritShot();
		final boolean sps = spiritShotUsed && effector.isChargedShot(ShotType.SPIRITSHOTS);
		final boolean bss = spiritShotUsed && effector.isChargedShot(ShotType.BLESSED_SPIRITSHOTS);
		final boolean mcrit = Formulas.calcCrit(skill.getMagicCriticalRate(), effector, effected, skill);
		final double mAtk = effector.getMAtk() * (1 + (chargedSouls * 0.022));
		final double damage = Formulas.calcMagicDam(effector, effected, skill, mAtk, _power, effected.getMDef(), sps, bss, mcrit);
		
		effector.doAttack(damage, effected, skill, false, false, mcrit, false);
	}
}
