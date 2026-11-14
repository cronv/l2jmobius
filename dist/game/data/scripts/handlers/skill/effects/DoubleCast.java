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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code DoubleCast} effect logic.<br>
 * It manages mechanics related to casting skills twice or simultaneously.
 * @author Nik
 */
public class DoubleCast extends AbstractEffect
{
	private static final SkillHolder[] TOGGLE_SKILLS = new SkillHolder[]
	{
		new SkillHolder(11007, 1),
		new SkillHolder(11009, 1),
		new SkillHolder(11008, 1),
		new SkillHolder(11010, 1)
	};
	
	private final Map<Integer, List<SkillHolder>> _addedToggles;
	
	/**
	 * Creates a new instance of the {@code DoubleCast} effect.<br>
	 * This constructor initializes the internal state using the provided statistics.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public DoubleCast(StatSet params)
	{
		_addedToggles = new HashMap<>();
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.DOUBLE_CAST}.
	 * @return The bitmask of the effect flags as a {@code long}.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.DOUBLE_CAST.getMask();
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It checks if the {@code effected} creature is a player.<br>
	 * If it is, it applies specific toggle skills to that player.<br>
	 * Finally, it calls the super class implementation.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill being used.
	 * @param item The item used to cast the skill.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer())
		{
			for (SkillHolder holder : TOGGLE_SKILLS)
			{
				final Skill s = holder.getSkill();
				if ((s != null) && !effected.isAffectedBySkill(holder))
				{
					_addedToggles.computeIfAbsent(effected.getObjectId(), v -> new ArrayList<>()).add(holder);
					s.applyEffects(effected, effected);
				}
			}
		}
		
		super.onStart(effector, effected, skill, item);
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes specific skill effects from a player.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} who received the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			_addedToggles.computeIfPresent(effected.getObjectId(), (k, v) ->
			{
				v.forEach(h -> effected.stopSkillEffects(h.getSkill()));
				return null;
			});
		}
	}
}
