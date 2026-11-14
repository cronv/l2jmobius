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
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Formulas;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code Unsummon} effect.<br>
 * It manages the removal of a {@code Summon} entity from the game world.
 * @author Adry_85
 */
public class Unsummon extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new {@code Unsummon} effect.<br>
	 * This constructor initializes the effect using the provided statistics.<br>
	 * It extracts the success chance from the {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public Unsummon(StatSet params)
	{
		_chance = params.getInt("chance", -1);
	}
	
	/**
	 * Determines if the unsummon effect succeeds.<br>
	 * It calculates success based on a chance percentage and various bonuses.<br>
	 * The result depends on the {@code effector}, {@code effected}, and the {@code skill} used.
	 * @param effector The creature performing the action.
	 * @param effected The creature being affected by the action.
	 * @param skill The skill being executed.
	 * @return {@code true} if the effect succeeds, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		if (_chance < 0)
		{
			return true;
		}
		
		final int magicLevel = skill.getMagicLevel();
		if ((magicLevel <= 0) || ((effected.getLevel() - 9) <= magicLevel))
		{
			final double chance = _chance * Formulas.calcAttributeBonus(effector, effected, skill) * Formulas.calcGeneralTraitBonus(effector, effected, skill.getTraitType(), false);
			if ((chance >= 100) || (chance > (Rnd.nextDouble() * 100)))
			{
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * This method returns {@code true} only if the {@code effected} creature is a summon.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @return {@code true} if the target is a summon, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return effected.isSummon();
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for an unsummon action.<br>
	 * This method handles the logic when a servitor is removed from the game world.<br>
	 * It stops all current actions and effects on the target creature.
	 * @param effector The {@code Creature} performing the action.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used to trigger this effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isServitor())
		{
			final Summon servitor = effected.asSummon();
			final Player summonOwner = servitor.getOwner();
			
			servitor.abortAttack();
			servitor.abortCast();
			servitor.stopAllEffects();
			
			servitor.unSummon(summonOwner);
			summonOwner.sendPacket(SystemMessageId.YOUR_SERVITOR_HAS_VANISHED_YOU_LL_NEED_TO_SUMMON_A_NEW_ONE);
		}
	}
}
