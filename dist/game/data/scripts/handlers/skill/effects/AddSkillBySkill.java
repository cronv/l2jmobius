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

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.transform.Transform;
import org.l2jmobius.gameserver.entity.actor.transform.TransformType;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect grants a new skill to a {@code Creature} based on another skill.<br>
 * It is used to trigger the acquisition of skills through specific game mechanics.
 * @author Mobius
 */
public class AddSkillBySkill extends AbstractEffect
{
	private final int _existingSkillId;
	private final int _existingSkillLevel;
	private final SkillHolder _addedSkill;
	
	/**
	 * Creates a new effect that adds a skill to an actor.<br>
	 * This constructor initializes the required skill IDs and levels from the provided data.
	 * @param params The {@code StatSet} containing the configuration values.
	 */
	public AddSkillBySkill(StatSet params)
	{
		_existingSkillId = params.getInt("existingSkillId");
		_existingSkillLevel = params.getInt("existingSkillLevel");
		_addedSkill = new SkillHolder(params.getInt("addedSkillId"), params.getInt("addedSkillLevel"));
	}
	
	/**
	 * Checks if the target can receive a skill level increase.<br>
	 * The target must be a {@code Player}.<br>
	 * The target must not be in a transformed state.<br>
	 * The target's current skill level must match the required level.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being evaluated.
	 * @return {@code true} if all conditions are met, otherwise {@code false}.
	 */
	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		final Transform transform = effected.getTransformation();
		return effected.isPlayer() && ((transform == null) || (transform.getType() == TransformType.MODE_CHANGE)) && (effected.getSkillLevel(_existingSkillId) == _existingSkillLevel);
	}
	
	/**
	 * Applies a skill to the target creature.<br>
	 * This method grants the {@code Skill} from the added skill holder to the player.<br>
	 * It also schedules a task to update the player's stats and broadcast their information.
	 * @param effected The {@code Creature} receiving the new skill.
	 * @param skill The {@code Skill} being processed by this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		player.addSkill(_addedSkill.getSkill(), false);
		
		ThreadPool.schedule(() ->
		{
			player.sendSkillList();
			player.getStat().recalculateStats(false);
			player.broadcastUserInfo();
		}, 100);
	}
	
	/**
	 * This method is called when the effect expires.<br>
	 * It removes the added skill from the {@code effected} player.<br>
	 * It also schedules a task to update the player's stats and information.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The skill that triggered this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		player.removeSkill(_addedSkill.getSkill(), false);
		
		ThreadPool.schedule(() ->
		{
			player.sendSkillList();
			player.getStat().recalculateStats(false);
			player.broadcastUserInfo();
		}, 100);
	}
}
