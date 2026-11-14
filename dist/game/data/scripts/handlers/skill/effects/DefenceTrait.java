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

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.TraitType;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code DefenceTrait} effect.<br>
 * It manages how defensive traits are applied to a {@code Creature}.
 * @author NosBit
 */
public class DefenceTrait extends AbstractEffect
{
	private final Map<TraitType, Float> _defenceTraits = new EnumMap<>(TraitType.class);
	
	/**
	 * Creates a new {@code DefenceTrait} effect.<br>
	 * This constructor initializes the defense traits from a provided set.<br>
	 * It ensures that the input parameters are not empty.
	 * @param params The {@code StatSet} containing the trait types and their values.
	 */
	public DefenceTrait(StatSet params)
	{
		if (params.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": must have parameters.");
		}
		
		for (Entry<String, Object> param : params.getSet().entrySet())
		{
			_defenceTraits.put(TraitType.valueOf(param.getKey()), Float.parseFloat((String) param.getValue()) / 100);
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies defense traits to the target creature.<br>
	 * It checks if a trait value is less than {@code 1.0f}.<br>
	 * If it is lower, it merges as a defense trait.<br>
	 * Otherwise, it marks the trait as invulnerable.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (Entry<TraitType, Float> trait : _defenceTraits.entrySet())
		{
			final float traitValue = trait.getValue().floatValue();
			if (traitValue < 1.0f)
			{
				effected.getStat().mergeDefenceTrait(trait.getKey(), traitValue);
			}
			else
			{
				effected.getStat().mergeInvulnerableTrait(trait.getKey());
			}
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes all defense traits from the {@code effected} creature.<br>
	 * It also removes invulnerability traits if they were applied.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		for (Entry<TraitType, Float> trait : _defenceTraits.entrySet())
		{
			final float traitValue = trait.getValue().floatValue();
			if (traitValue < 1.0f)
			{
				effected.getStat().removeDefenceTrait(trait.getKey(), traitValue);
			}
			else
			{
				effected.getStat().removeInvulnerableTrait(trait.getKey());
			}
		}
	}
}
