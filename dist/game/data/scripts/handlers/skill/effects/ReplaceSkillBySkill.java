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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.actor.transform.TransformType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.replacement.SkillReplacementList;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect replaces a skill with another specific skill.<br>
 * The skill the player knows is left alone, and only what it resolves to changes, for as long as this effect lasts.<br>
 * Several effects may name the same source, in which case the highest priority wins and the rest wait behind it.
 * @author fruit
 */
public class ReplaceSkillBySkill extends AbstractEffect
{
	private final int _existingSkillId;
	private final int _replacementSkillId;
	private final int _replacementSkillLevel;
	private final int _replacementSkillPriority;
	
	/**
	 * Creates a new instance of the {@code ReplaceSkillBySkill} effect.<br>
	 * It initializes the skills to be swapped using values from the provided parameters.
	 * @param params The {@code StatSet} containing the IDs, levels and priority for both skills.
	 */
	public ReplaceSkillBySkill(StatSet params)
	{
		_existingSkillId = params.getInt("existingSkillId");
		_replacementSkillId = params.getInt("replacementSkillId");
		_replacementSkillLevel = params.getInt("replacementSkillLevel", -1);
		_replacementSkillPriority = params.getInt("replacementSkillPriority", SkillReplacementList.DEFAULT_PRIORITY);
	}
	
	/**
	 * Checks if the effect can be applied to a target.<br>
	 * It returns {@code true} only if the target is a {@code Player}.<br>
	 * The target must not be in a transformed state, unless it is a mode change, a visual transform.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the action.
	 * @param skill The skill being used.
	 * @return {@code true} if the effect can start, otherwise {@code false}.
	 */
	@Override
	public boolean canStart(Creature effector, Creature effected, Skill skill)
	{
		final Transform transform = effected.getTransformation();
		return effected.isPlayer() && ((transform == null) || (transform.getType() == TransformType.MODE_CHANGE) || transform.isVisualOnly());
	}
	
	/**
	 * Checks if the skill can be pumped based on specific conditions.<br>
	 * The skill must not be {@code null}.<br>
	 * The skill must be a passive type.<br>
	 * The target creature must be a player and either untransformed, in a mode change, in a visual transform.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being evaluated.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		final Transform transform = effected.getTransformation();
		return (skill != null) && skill.isPassive() && effected.isPlayer() && ((transform == null) || (transform.getType() == TransformType.MODE_CHANGE) || transform.isVisualOnly());
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It registers the replacement rule for the duration of the effect.
	 * @param effector The creature that initiates the action.
	 * @param effected The creature that receives the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} involved in the action.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		registerRule(effected);
	}
	
	/**
	 * Registers the replacement rule for a passive skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to trigger the effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		registerRule(effected);
	}
	
	/**
	 * Removes the rule this effect registered, exposing whichever rule of the same source comes next.
	 * @param effector The creature that cast the effect.
	 * @param effected The creature who received the effect.
	 * @param skill The skill associated with this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final SkillReplacementList manager = effected.asPlayer().getSkillReplacementListIfPresent();
		if (manager != null)
		{
			manager.unregisterReplacement(_existingSkillId, _replacementSkillId, _replacementSkillLevel, _replacementSkillPriority);
		}
	}
	
	/**
	 * Registers this effect's rule on the player that received it.<br>
	 * One effect instance is shared by every player using the skill, so nothing about the rule is kept here.
	 * @param effected The creature receiving the effect.
	 */
	private void registerRule(Creature effected)
	{
		effected.asPlayer().getSkillReplacementList().registerReplacement(_existingSkillId, _replacementSkillId, _replacementSkillLevel, _replacementSkillPriority);
	}
}
