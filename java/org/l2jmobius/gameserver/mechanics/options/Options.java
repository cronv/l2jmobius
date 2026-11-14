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
package org.l2jmobius.gameserver.mechanics.options;

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;

/**
 * This class manages the configuration options for game mechanics.<br>
 * It provides a centralized way to handle various settings and parameters used by the server.
 * @author UnAfraid
 */
public class Options
{
	private final int _id;
	private List<AbstractEffect> _effects = null;
	private List<Skill> _activeSkill = null;
	private List<Skill> _passiveSkill = null;
	private List<OptionSkillHolder> _activationSkills = null;
	
	/**
	 * Creates a new instance of {@code Options}.<br>
	 * This constructor initializes the object with a unique identifier.
	 * @param id The unique identification number for this option.
	 */
	public Options(int id)
	{
		_id = id;
	}
	
	/**
	 * Retrieves the unique identifier for this {@code Options} instance.<br>
	 * This value is assigned when the object is created.
	 * @return The unique integer ID of the option.
	 */
	public int getId()
	{
		return _id;
	}
	
	/**
	 * Adds a new effect to the list of effects.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param effect The {@code AbstractEffect} to be added.
	 */
	public void addEffect(AbstractEffect effect)
	{
		if (_effects == null)
		{
			_effects = new ArrayList<>();
		}
		
		_effects.add(effect);
	}
	
	/**
	 * Retrieves the list of effects associated with this {@code Options} instance.
	 * @return a {@code List} containing all {@code AbstractEffect} objects.
	 */
	public List<AbstractEffect> getEffects()
	{
		return _effects;
	}
	
	/**
	 * Checks if this option contains any effects.<br>
	 * It returns {@code true} if the internal effect list is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there are effects, {@code false} otherwise.
	 */
	public boolean hasEffects()
	{
		return _effects != null;
	}
	
	/**
	 * Checks if there are any active skills associated with this {@code Options} instance.<br>
	 * It returns {@code true} if the internal skill list is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if active skills exist, {@code false} otherwise.
	 */
	public boolean hasActiveSkills()
	{
		return _activeSkill != null;
	}
	
	/**
	 * Retrieves the list of active skills associated with this {@code Options} instance.<br>
	 * This method returns all {@code Skill} objects currently marked as active.
	 * @return A {@code List} containing all active {@code Skill} objects.
	 */
	public List<Skill> getActiveSkills()
	{
		return _activeSkill;
	}
	
	/**
	 * Adds a new active skill to the list of skills.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param holder The {@code Skill} object to be added.
	 */
	public void addActiveSkill(Skill holder)
	{
		if (_activeSkill == null)
		{
			_activeSkill = new ArrayList<>();
		}
		
		_activeSkill.add(holder);
	}
	
	/**
	 * Checks if this option contains any passive skills.<br>
	 * It returns {@code true} if the internal list is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there are passive skills, {@code false} otherwise.
	 */
	public boolean hasPassiveSkills()
	{
		return _passiveSkill != null;
	}
	
	/**
	 * Retrieves the list of passive skills associated with this {@code Options} object.
	 * @return a {@code List} containing all {@code Skill} objects for passive effects.
	 */
	public List<Skill> getPassiveSkills()
	{
		return _passiveSkill;
	}
	
	/**
	 * Adds a passive skill to the list of skills for this option.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param holder The {@code Skill} object to be added.
	 */
	public void addPassiveSkill(Skill holder)
	{
		if (_passiveSkill == null)
		{
			_passiveSkill = new ArrayList<>();
		}
		
		_passiveSkill.add(holder);
	}
	
	/**
	 * Checks if this option contains any activation skills.<br>
	 * It returns {@code true} if the list of activation skills is not {@code null}.<br>
	 * Otherwise, it returns {@code false}.
	 * @return {@code true} if there are activation skills, {@code false} otherwise.
	 */
	public boolean hasActivationSkills()
	{
		return _activationSkills != null;
	}
	
	/**
	 * Checks if the {@code Options} object contains any activation skills of a specific type.<br>
	 * It iterates through all available activation skills to find a match.
	 * @param type The {@code OptionSkillType} to search for.
	 * @return {@code true} if at least one matching skill is found, otherwise {@code false}.
	 */
	public boolean hasActivationSkills(OptionSkillType type)
	{
		if (_activationSkills != null)
		{
			for (OptionSkillHolder holder : _activationSkills)
			{
				if (holder.getSkillType() == type)
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Retrieves the list of skills associated with this option.<br>
	 * This method returns all {@code OptionSkillHolder} objects currently stored in the internal list.
	 * @return a {@code List} containing all {@code OptionSkillHolder} instances.
	 */
	public List<OptionSkillHolder> getActivationSkills()
	{
		return _activationSkills;
	}
	
	/**
	 * Retrieves a list of activation skills for a specific type.<br>
	 * It filters the internal collection based on the provided {@code OptionSkillType}.<br>
	 * Returns an empty list if no matching skills are found.
	 * @param type The {@code OptionSkillType} to filter by.
	 * @return A {@code List} of {@code OptionSkillHolder} objects that match the given type.
	 */
	public List<OptionSkillHolder> getActivationSkills(OptionSkillType type)
	{
		final List<OptionSkillHolder> temp = new ArrayList<>();
		if (_activationSkills != null)
		{
			for (OptionSkillHolder holder : _activationSkills)
			{
				if (holder.getSkillType() == type)
				{
					temp.add(holder);
				}
			}
		}
		
		return temp;
	}
	
	/**
	 * Adds a new skill to the list of activation skills.<br>
	 * This method initializes the internal list if it is currently {@code null}.
	 * @param holder The {@code OptionSkillHolder} object to be added.
	 */
	public void addActivationSkill(OptionSkillHolder holder)
	{
		if (_activationSkills == null)
		{
			_activationSkills = new ArrayList<>();
		}
		
		_activationSkills.add(holder);
	}
	
	/**
	 * Applies all associated effects and skills to the specified player.<br>
	 * This method processes active, passive, and activation skills.<br>
	 * It also handles continuous and instant effects for the target.<br>
	 * Finally, it recalculates the player stats and updates their skill list.
	 * @param player The {@code Player} object to receive the options.
	 */
	public void apply(Player player)
	{
		if (hasEffects())
		{
			final BuffInfo info = new BuffInfo(player, player, null, true, null, this);
			for (AbstractEffect effect : _effects)
			{
				if (effect.isInstant())
				{
					if (effect.calcSuccess(info.getEffector(), info.getEffected(), info.getSkill()))
					{
						effect.instant(info.getEffector(), info.getEffected(), info.getSkill(), info.getItem());
					}
				}
				else
				{
					effect.continuousInstant(info.getEffector(), info.getEffected(), info.getSkill(), info.getItem());
					effect.pump(player, info.getSkill());
					if (effect.canStart(info.getEffector(), info.getEffected(), info.getSkill()))
					{
						info.addEffect(effect);
					}
				}
			}
			
			if (!info.getEffects().isEmpty())
			{
				player.getEffectList().add(info);
			}
		}
		
		if (hasActiveSkills())
		{
			for (Skill skill : _activeSkill)
			{
				addSkill(player, skill);
			}
		}
		
		if (hasPassiveSkills())
		{
			for (Skill skill : _passiveSkill)
			{
				addSkill(player, skill);
			}
		}
		
		if (hasActivationSkills())
		{
			for (OptionSkillHolder holder : _activationSkills)
			{
				player.addTriggerSkill(holder);
			}
		}
		
		player.getStat().recalculateStats(true);
		player.sendSkillList();
	}
	
	/**
	 * Removes all effects and skills associated with this {@code Options} instance from a player.<br>
	 * This method updates the player's stats and sends the updated skill list to the client.
	 * @param player The {@code Player} from whom to remove the options.
	 */
	public void remove(Player player)
	{
		if (hasEffects())
		{
			for (BuffInfo info : player.getEffectList().getOptions())
			{
				if (info.getOption() == this)
				{
					player.getEffectList().remove(info, SkillFinishType.NORMAL, true, true);
				}
			}
		}
		
		if (hasActiveSkills())
		{
			for (Skill skill : _activeSkill)
			{
				player.removeSkill(skill, false, false);
			}
		}
		
		if (hasPassiveSkills())
		{
			for (Skill skill : _passiveSkill)
			{
				player.removeSkill(skill, false, true);
			}
		}
		
		if (hasActivationSkills())
		{
			for (OptionSkillHolder holder : _activationSkills)
			{
				player.removeTriggerSkill(holder);
			}
		}
		
		player.getStat().recalculateStats(true);
		player.sendSkillList();
	}
	
	/**
	 * Adds a {@code Skill} to a {@code Player}.<br>
	 * This method also handles cooldown logic for active skills.<br>
	 * It updates the player's timestamp and sends a {@code SkillCoolTime} packet if needed.
	 * @param player The {@code Player} receiving the skill.
	 * @param skill The {@code Skill} to be added.
	 */
	private void addSkill(Player player, Skill skill)
	{
		boolean updateTimeStamp = false;
		player.addSkill(skill, false);
		if (skill.isActive())
		{
			final long remainingTime = player.getSkillRemainingReuseTime(skill.getReuseHashCode());
			if (remainingTime > 0)
			{
				player.addTimeStamp(skill, remainingTime);
				player.disableSkill(skill, remainingTime);
			}
			
			updateTimeStamp = true;
		}
		
		if (updateTimeStamp)
		{
			player.sendPacket(new SkillCoolTime(player));
		}
	}
}
