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
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for effects that modify a creature's vital statistics.<br>
 * It provides an implementation of {@code AbstractEffect} to adjust various stats.
 * @author malyelfik
 */
public class ModifyVital extends AbstractEffect
{
	// Modify types
	private enum ModifyType
	{
		DIFF,
		SET,
		PER;
	}
	
	// Effect parameters
	private final ModifyType _type;
	private final int _hp;
	private final int _mp;
	private final int _cp;
	
	/**
	 * Creates a new {@code ModifyVital} effect.<br>
	 * This constructor initializes the vital statistics based on the provided parameters.<br>
	 * It sets the modification type and values for HP, MP, and CP.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public ModifyVital(StatSet params)
	{
		_type = params.getEnum("type", ModifyType.class);
		if (_type != ModifyType.SET)
		{
			_hp = params.getInt("hp", 0);
			_mp = params.getInt("mp", 0);
			_cp = params.getInt("cp", 0);
		}
		else
		{
			_hp = params.getInt("hp", -1);
			_mp = params.getInt("mp", -1);
			_cp = params.getInt("cp", -1);
		}
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
	 * Checks if the effect can be successfully applied to a target.<br>
	 * This method returns {@code false} if the target is a raid boss or a raid minion.<br>
	 * Otherwise, it returns {@code true}.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @return {@code true} if the effect is successful, {@code false} otherwise.
	 */
	@Override
	public boolean calcSuccess(Creature effector, Creature effected, Skill skill)
	{
		return !effected.isRaid() && !effected.isRaidMinion();
	}
	
	/**
	 * Applies the vital modification effect immediately to a target.<br>
	 * This method updates the HP, MP, and CP of the {@code effected} creature based on the defined type.<br>
	 * It checks if the target is alive before applying any changes.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The skill being used to trigger the effect.
	 * @param item The item involved in the interaction, if any.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return;
		}
		
		if (effector.isPlayer() && effected.isPlayer() && effected.isAffected(EffectFlag.DUELIST_FURY) && !effector.isAffected(EffectFlag.DUELIST_FURY))
		{
			return;
		}
		
		switch (_type)
		{
			case DIFF:
			{
				effected.setCurrentCp(effected.getCurrentCp() + _cp);
				effected.setCurrentHp(effected.getCurrentHp() + _hp);
				effected.setCurrentMp(effected.getCurrentMp() + _mp);
				break;
			}
			case SET:
			{
				if (_cp >= 0)
				{
					effected.setCurrentCp(_cp);
				}
				
				if (_hp >= 0)
				{
					effected.setCurrentHp(_hp);
				}
				
				if (_mp >= 0)
				{
					effected.setCurrentMp(_mp);
				}
				break;
			}
			case PER:
			{
				effected.setCurrentCp(effected.getCurrentCp() + (effected.getMaxCp() * (_cp / 100)));
				effected.setCurrentHp(effected.getCurrentHp() + (effected.getMaxHp() * (_hp / 100)));
				effected.setCurrentMp(effected.getCurrentMp() + (effected.getMaxMp() * (_mp / 100)));
				break;
			}
		}
	}
}
