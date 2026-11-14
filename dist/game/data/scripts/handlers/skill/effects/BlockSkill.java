/*
 * Copyright (c) 2013 L2jMobius
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
 * IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package handlers.skill.effects;

import java.util.HashSet;
import java.util.Set;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.listeners.FunctionEventListener;
import org.l2jmobius.gameserver.mechanics.events.returns.TerminateReturn;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the blocking of specific skills.<br>
 * It allows for restrictions based on a {@code Skill} ID or its magic type.<br>
 * Use this effect to prevent certain actions from being performed by actors.
 * @author Nik, Mobius
 */
public class BlockSkill extends AbstractEffect
{
	private final Set<Integer> _magicTypes = new HashSet<>();
	private final Set<Integer> _skillIds = new HashSet<>();
	
	/**
	 * Creates a new {@code BlockSkill} instance.<br>
	 * This constructor initializes the blocked magic types and skill IDs.<br>
	 * It reads these values from the provided {@code StatSet}.<br>
	 * An exception is thrown if both sets are empty.
	 * @param params The {@code StatSet} containing configuration data.
	 */
	public BlockSkill(StatSet params)
	{
		if (params.contains("magicTypes"))
		{
			for (int id : params.getIntArray("magicTypes", ";"))
			{
				_magicTypes.add(id);
			}
		}
		
		if (params.contains("skillIds"))
		{
			for (int id : params.getIntArray("skillIds", ";"))
			{
				_skillIds.add(id);
			}
		}
		
		if (_magicTypes.isEmpty() && _skillIds.isEmpty())
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " magicTypes and skillIds are empty.");
		}
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It attaches a listener to the {@code effected} creature.<br>
	 * The listener monitors skill usage events.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill associated with this effect.
	 * @param item The item used to trigger the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new FunctionEventListener(effected, EventType.ON_CREATURE_SKILL_USE, (OnCreatureSkillUse event) -> onSkillUseEvent(event), this));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes the skill restriction from the {@code effected} creature.<br>
	 * The listener associated with this effect is removed during this process.
	 * @param effector The creature that applied the effect.
	 * @param effected The creature that was affected by the effect.
	 * @param skill The skill used to trigger or manage the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_SKILL_USE, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Checks if a skill should be blocked based on its magic type or ID.<br>
	 * This method is triggered when a creature uses a skill.<br>
	 * It returns a termination signal if the skill matches any restricted criteria.
	 * @param event The {@code OnCreatureSkillUse} event containing the skill details.
	 * @return A {@code TerminateReturn} object with all values set to {@code true} if blocked, otherwise {@code null}.
	 */
	private TerminateReturn onSkillUseEvent(OnCreatureSkillUse event)
	{
		if (_magicTypes.contains(event.getSkill().getMagicType()) || _skillIds.contains(event.getSkill().getId()))
		{
			return new TerminateReturn(true, true, true);
		}
		
		return null;
	}
}
