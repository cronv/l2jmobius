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

import java.util.ArrayList;
import java.util.List;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillUse;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.holders.SkillHolder;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for skills that allow a summon to cast on behalf of its owner.<br>
 * It ensures that the skill's effects are correctly attributed to the original caster.<br>
 * Use this effect when a {@code Summon} needs to replicate an action performed by its master.
 * @author Mobius
 */
public class AssistOwnerCast extends AbstractEffect
{
	private final int _summonId;
	private final int _castSkillId;
	private final List<SkillHolder> _skills = new ArrayList<>();
	private final boolean _onFinishCast;
	
	/**
	 * Initializes a new {@code AssistOwnerCast} effect using the provided parameters.<br>
	 * This constructor extracts summon IDs, skill IDs, and other configuration data from the {@code StatSet}.<br>
	 * It also parses and populates the list of skills to be cast.
	 * @param params The {@code StatSet} containing all necessary configuration values for this effect.
	 */
	public AssistOwnerCast(StatSet params)
	{
		_summonId = params.getInt("summonId"); // Npc id
		_castSkillId = params.getInt("castSkillId");
		_onFinishCast = params.getBoolean("onFinishCast", false);
		
		final String skillIds = params.getString("skillId", "");
		final String skillLevels = params.getString("skillLevel", "");
		
		final String[] skillIdArray = skillIds.split(";");
		final String[] skillLevelArray = skillLevels.split(";");
		
		final int skillIdArrayLength = skillIdArray.length;
		if (skillIdArrayLength != skillLevelArray.length)
		{
			throw new IllegalArgumentException("Mismatch between number of skillIds and skillLevels!");
		}
		
		for (int i = 0; i < skillIdArrayLength; i++)
		{
			final int skillId = Integer.parseInt(skillIdArray[i].trim());
			final int skillLevel = Integer.parseInt(skillLevelArray[i].trim());
			_skills.add(new SkillHolder(skillId, skillLevel));
		}
	}
	
	/**
	 * This method initializes the effect when it starts.<br>
	 * It registers a listener to the {@code effected} creature based on the {@code _onFinishCast} flag.<br>
	 * The listener will trigger the skill logic for the summon.
	 * @param effector The creature that triggers the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_skills.isEmpty() || (_castSkillId == 0))
		{
			return;
		}
		
		if (_onFinishCast)
		{
			effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onSkillUseEvent(event), this));
		}
		else
		{
			effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_USE, (OnCreatureSkillUse event) -> onSkillUseEvent(event), this));
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It removes specific event listeners from the {@code effected} creature.<br>
	 * The removal depends on whether the effect was for finishing a cast or using a skill.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature that received the effect.
	 * @param skill The skill associated with this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (_onFinishCast)
		{
			effected.removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == this);
		}
		else
		{
			effected.removeListenerIf(EventType.ON_CREATURE_SKILL_USE, listener -> listener.getOwner() == this);
		}
	}
	
	/**
	 * Handles the logic for summons to perform actions based on a caster's skill.<br>
	 * It identifies active summons and triggers their skills on a specific target.
	 * @param caster The {@code Creature} who initiated the action.
	 * @param target The {@code WorldObject} that the summon will act upon.
	 */
	private void processSummonActions(Creature caster, WorldObject target)
	{
		if (caster.getServitors() != null)
		{
			for (Summon servitor : caster.getServitors().values())
			{
				if ((servitor != null) && (_summonId == servitor.getId()) && !servitor.isDisabled())
				{
					servitor.setTarget(target);
					castSkills(servitor, target);
				}
			}
		}
		
		if (caster.getSummonedNpcCount() != 0)
		{
			caster.getSummonedNpcs().forEach(summon ->
			{
				if ((_summonId == summon.getId()) && !summon.isDisabled())
				{
					summon.setTarget(target);
					castSkills(summon, target);
				}
			});
		}
	}
	
	/**
	 * This method handles the execution of skills for a {@code Creature} summon.<br>
	 * It iterates through all assigned skills and schedules them to be cast at {@code target}.<br>
	 * Each skill is delayed sequentially to prevent simultaneous casting.
	 * @param summon The {@code Creature} instance that will perform the skill casts.
	 * @param target The {@code WorldObject} that is being targeted by the skills.
	 */
	private void castSkills(Creature summon, WorldObject target)
	{
		for (int i = 0; i < _skills.size(); i++)
		{
			final Skill skill = _skills.get(i).getSkill();
			if (skill != null)
			{
				ThreadPool.schedule(() ->
				{
					if (skill.hasNegativeEffect())
					{
						summon.getAI().setIntentionAttack(target);
					}
					
					summon.doCast(skill);
				}, (1250 * i) + 50); // Delay between skills.
			}
		}
	}
	
	/**
	 * Processes the actions for a summon when a skill is used.<br>
	 * This method handles specific logic related to the {@code caster} and the {@code target}.
	 * @param caster The {@code Creature} who initiated the action.
	 * @param target The {@code WorldObject} being targeted by the action.
	 */
	private void handleSkillUseEvent(Creature caster, WorldObject target)
	{
		processSummonActions(caster, target);
	}
	
	/**
	 * Handles the logic when a creature finishes casting a skill.<br>
	 * This method checks if the caster is a player with active summons.<br>
	 * It also verifies that the skill ID matches and the target is valid.<br>
	 * If all conditions are met, it triggers {@code handleSkillUseEvent}.
	 * @param event The {@code OnCreatureSkillFinishCast} event containing caster and target data.
	 */
	private void onSkillUseEvent(OnCreatureSkillFinishCast event)
	{
		final Creature caster = event.getCaster();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || (_castSkillId != event.getSkill().getId()) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = event.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		handleSkillUseEvent(caster, target);
	}
	
	/**
	 * Handles the logic when a creature uses a skill.<br>
	 * This method checks if the caster is a player with active summons.<br>
	 * It also verifies that the skill ID matches and the target is valid.<br>
	 * If all conditions are met, it triggers {@code handleSkillUseEvent}.
	 * @param event The {@code OnCreatureSkillUse} event containing the skill details.
	 */
	private void onSkillUseEvent(OnCreatureSkillUse event)
	{
		final Creature caster = event.getCaster();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || (_castSkillId != event.getSkill().getId()) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = caster.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		handleSkillUseEvent(caster, target);
	}
}
