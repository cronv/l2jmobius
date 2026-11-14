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

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code BlockActions} effect.<br>
 * It defines how actions are restricted when this effect is active on a creature.
 * @author mkizub
 */
public class BlockActions extends AbstractEffect
{
	private final Set<Integer> _allowedSkills = new HashSet<>();
	
	/**
	 * Creates a new {@code BlockActions} effect.<br>
	 * This constructor initializes the allowed skills from the provided parameters.<br>
	 * It parses a semicolon-separated list of skill IDs from the {@code params} object.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public BlockActions(StatSet params)
	{
		for (String skill : params.getString("allowedSkills", "").split(";"))
		{
			if (!skill.isEmpty())
			{
				_allowedSkills.add(Integer.parseInt(skill));
			}
		}
	}
	
	/**
	 * Retrieves the bitmask for the effect flags.<br>
	 * It determines if actions are blocked completely or conditionally.<br>
	 * This depends on whether the {@code _allowedSkills} set is empty.
	 * @return The bitmask value from {@code EffectFlag}.
	 */
	@Override
	public long getEffectFlags()
	{
		return _allowedSkills.isEmpty() ? EffectFlag.BLOCK_ACTIONS.getMask() : EffectFlag.CONDITIONAL_BLOCK_ACTIONS.getMask();
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from the {@code EffectType} enum.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.BLOCK_ACTIONS;
	}
	
	/**
	 * Checks if the effect can be applied to the target.<br>
	 * Raid bosses are immune, checked here so that the effect flag is not applied to them either.
	 * @param effector The {@code Creature} casting the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		return (effected != null) && !effected.isRaid();
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It applies restrictions to the {@code effected} creature.<br>
	 * The logic checks if the target is valid before proceeding.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		for (Integer skillId : _allowedSkills)
		{
			effected.addBlockActionsAllowedSkill(skillId);
		}
		
		effected.startParalyze();
		
		// Cancel running skill casters.
		effected.abortAllSkillCasters();
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes allowed skills from the {@code effected} creature.<br>
	 * It also updates the AI behavior of the {@code effected} creature based on its state.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		for (Integer skillId : _allowedSkills)
		{
			effected.removeBlockActionsAllowedSkill(skillId);
		}
		
		if (effected.isPlayable())
		{
			if (effected.isSummon())
			{
				if ((effector != null) && !effector.isDead())
				{
					if (effector.isPlayable() && (effected.asPlayer().getPvpFlag() == 0))
					{
						effected.disableCoreAI(false);
					}
					else
					{
						effected.asSummon().doAutoAttack(effector);
					}
				}
				else
				{
					effected.disableCoreAI(false);
				}
			}
			else
			{
				effected.getAI().setIntentionActive();
			}
		}
		else
		{
			effected.getAI().notifyActionThink();
		}
	}
}
