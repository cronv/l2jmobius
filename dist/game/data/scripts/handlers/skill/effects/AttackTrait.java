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
 * This class handles the logic for {@code AttackTrait} effects.<br>
 * It manages how specific traits modify attack-related statistics on a creature.
 * @author NosBit
 */
public class AttackTrait extends AbstractEffect
{
	private final Map<TraitType, Float> _attackTraits = new EnumMap<>(TraitType.class);
	
	/**
	 * Creates a new {@code AttackTrait} effect.<br>
	 * This constructor initializes the attack traits from a provided set of parameters.<br>
	 * It throws an exception if the {@code params} object is empty.
	 * @param params The {@code StatSet} containing the trait types and their values.
	 */
	public AttackTrait(StatSet params)
	{
		if (params.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + ": this effect must have parameters!");
		}
		
		for (Entry<String, Object> param : params.getSet().entrySet())
		{
			_attackTraits.put(TraitType.valueOf(param.getKey()), Float.parseFloat((String) param.getValue()) / 100);
		}
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies attack traits to the target creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} used to trigger this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (Entry<TraitType, Float> trait : _attackTraits.entrySet())
		{
			effected.getStat().mergeAttackTrait(trait.getKey(), trait.getValue().floatValue());
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes all attack traits from the target creature.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		for (Entry<TraitType, Float> trait : _attackTraits.entrySet())
		{
			effected.getStat().removeAttackTrait(trait.getKey(), trait.getValue().floatValue());
		}
	}
}
