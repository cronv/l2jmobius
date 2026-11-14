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
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class implements the {@code ManaHealByLevel} effect.<br>
 * It calculates and applies mana restoration based on the character's level.
 * @author UnAfraid
 */
public class ManaHealByLevel extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new {@code ManaHealByLevel} effect.<br>
	 * This constructor initializes the healing power from the provided stats.<br>
	 * It will throw an exception if the {@code amount} key is used instead of {@code power}.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public ManaHealByLevel(StatSet params)
	{
		_power = params.getDouble("power", 0);
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the specific type of effect for this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} associated with this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.MANAHEAL_BY_LEVEL;
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
	 * Applies the mana heal effect instantly to a target.<br>
	 * This method calculates the amount based on level differences and updates the target's mana.
	 * @param effector The creature casting the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead() || effected.isDoor() || effected.isMpBlocked())
		{
			return;
		}
		
		if ((effected != effector) && effected.isAffected(EffectFlag.FACEOFF))
		{
			return;
		}
		
		double amount = _power;
		
		// Recharged mp influenced by difference between target level and skill level. If target is within 5 levels or lower then skill level there's no penalty.
		amount = effected.getStat().getValue(Stat.MANA_CHARGE, amount);
		final int effectedLevel = effected.getLevel();
		final int magicLevel = skill.getMagicLevel();
		if (effectedLevel > magicLevel)
		{
			final int levelDiff = effectedLevel - magicLevel;
			
			// If target is too high compared to skill level, the amount of recharged mp gradually decreases.
			switch (levelDiff)
			{
				case 6:
				{
					amount *= 0.9; // only 90% effective.
					break;
				}
				case 7:
				{
					amount *= 0.8; // 80%.
					break;
				}
				case 8:
				{
					amount *= 0.7; // 70%.
					break;
				}
				case 9:
				{
					amount *= 0.6; // 60%.
					break;
				}
				case 10:
				{
					amount *= 0.5; // 50%.
					break;
				}
				case 11:
				{
					amount *= 0.4; // 40%.
					break;
				}
				case 12:
				{
					amount *= 0.3; // 30%.
					break;
				}
				case 13:
				{
					amount *= 0.2; // 20%.
					break;
				}
				case 14:
				{
					amount *= 0.1; // 10%.
					break;
				}
				default:
				{
					if (levelDiff >= 15)
					{
						amount = 0; // 0mp recharged.
					}
					break;
				}
			}
		}
		
		// Prevents overheal and negative amount.
		final double currentMp = effected.getCurrentMp();
		amount = Math.max(Math.min(amount, effected.getMaxRecoverableMp() - currentMp), 0);
		if (amount != 0)
		{
			final double newMp = amount + currentMp;
			effected.setCurrentMp(newMp, false);
			effected.broadcastStatusUpdate(effector);
		}
		
		final int objectId = effector.getObjectId();
		final int effectedObjectId = effected.getObjectId();
		final SystemMessage sm = new SystemMessage(objectId != effectedObjectId ? SystemMessageId.YOU_HAVE_RECOVERED_S2_MP_WITH_C1_S_HELP : SystemMessageId.S1_MP_HAS_BEEN_RESTORED);
		if (objectId != effectedObjectId)
		{
			sm.addString(effector.getName());
		}
		
		sm.addInt((int) amount);
		effected.sendPacket(sm);
	}
}
