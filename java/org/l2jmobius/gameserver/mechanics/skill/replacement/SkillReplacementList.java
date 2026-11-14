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
package org.l2jmobius.gameserver.mechanics.skill.replacement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.skill.Skill;

/**
 * Holds the active skill replacements of one player.<br>
 * A replacement is a directed rule from a source skill to a target skill, and the rules of one source compete by priority.<br>
 * Nothing here changes the skills the owner knows, so a resolved result is never stored on the owner and disappears with its rule.
 * @author fruit
 */
public class SkillReplacementList
{
	/** The precedence a replacement takes when its caller does not name one. */
	public static final int DEFAULT_PRIORITY = 10;
	
	private final Player _owner;
	
	/** Rules of one source skill, keyed by their activation order. */
	private final Map<Integer, Map<Long, SkillReplacementHolder>> _rules = new ConcurrentHashMap<>();
	private final Map<Integer, ResolvedSkill> _resolvedSkills = new ConcurrentHashMap<>();
	
	/** Identifies a rule and breaks ties between equal priorities. */
	private final AtomicLong _activationSequence = new AtomicLong();
	
	/**
	 * Creates a manager for the specified owner.
	 * @param owner The player whose replacements are held here.
	 */
	public SkillReplacementList(Player owner)
	{
		_owner = owner;
	}
	
	/**
	 * Registers a replacement rule.
	 * @param existingSkillId The source skill ID.
	 * @param replacementSkillId The target skill ID.
	 * @param replacementSkillLevel The fixed target level, or {@code -1} to inherit the resolved level.
	 * @param priority The precedence among rules that share the same source.
	 * @return The registered rule, used later to remove exactly this one, or {@code null} when either ID is invalid.
	 */
	public SkillReplacementHolder registerReplacement(int existingSkillId, int replacementSkillId, int replacementSkillLevel, int priority)
	{
		if ((existingSkillId <= 0) || (replacementSkillId <= 0))
		{
			return null;
		}
		
		// One effect instance serves every playable, so a rule is identified by its values rather than by who registered it.
		final SkillReplacementHolder existingRule = findRule(existingSkillId, replacementSkillId, replacementSkillLevel, priority);
		if (existingRule != null)
		{
			return existingRule;
		}
		
		final long activationOrder = _activationSequence.incrementAndGet();
		final SkillReplacementHolder rule = new SkillReplacementHolder(existingSkillId, replacementSkillId, replacementSkillLevel, priority, activationOrder);
		_rules.computeIfAbsent(existingSkillId, key -> new ConcurrentHashMap<>()).put(activationOrder, rule);
		_resolvedSkills.clear();
		return rule;
	}
	
	/**
	 * Removes exactly the given rule, exposing whichever rule of the same source comes next.
	 * @param rule The rule that {@code registerReplacement} returned.
	 */
	public void unregisterReplacement(SkillReplacementHolder rule)
	{
		if (rule == null)
		{
			return;
		}
		
		final int existingSkillId = rule.getExistingSkillId();
		final Map<Long, SkillReplacementHolder> sourceRules = _rules.get(existingSkillId);
		if ((sourceRules == null) || (sourceRules.remove(rule.getActivationOrder()) == null))
		{
			return;
		}
		
		if (sourceRules.isEmpty())
		{
			_rules.remove(existingSkillId, sourceRules);
		}
		
		_resolvedSkills.clear();
	}
	
	/**
	 * Removes the rule carrying these values, which is how an effect shared by every playable removes its own.
	 * @param existingSkillId The source skill ID.
	 * @param replacementSkillId The target skill ID.
	 * @param replacementSkillLevel The fixed target level, or {@code -1} when the resolved level is inherited.
	 * @param priority The precedence among rules that share the same source.
	 */
	public void unregisterReplacement(int existingSkillId, int replacementSkillId, int replacementSkillLevel, int priority)
	{
		unregisterReplacement(findRule(existingSkillId, replacementSkillId, replacementSkillLevel, priority));
	}
	
	/**
	 * Finds the rule carrying these values.
	 * @param existingSkillId The source skill ID.
	 * @param replacementSkillId The target skill ID.
	 * @param replacementSkillLevel The fixed target level, or {@code -1} when the resolved level is inherited.
	 * @param priority The precedence among rules that share the same source.
	 * @return The matching rule, or {@code null} when the source has none like it.
	 */
	private SkillReplacementHolder findRule(int existingSkillId, int replacementSkillId, int replacementSkillLevel, int priority)
	{
		final Map<Long, SkillReplacementHolder> sourceRules = _rules.get(existingSkillId);
		if (sourceRules == null)
		{
			return null;
		}
		
		for (SkillReplacementHolder rule : sourceRules.values())
		{
			if ((rule.getReplacementSkillId() == replacementSkillId) && (rule.getReplacementSkillLevel() == replacementSkillLevel) && (rule.getPriority() == priority))
			{
				return rule;
			}
		}
		
		return null;
	}
	
	/**
	 * Removes every rule of one source skill.<br>
	 * This serves the callers that never held a rule, where an effect removes the single rule it registered.
	 * @param existingSkillId The source skill ID to clear.
	 */
	public void unregisterReplacements(int existingSkillId)
	{
		if (_rules.remove(existingSkillId) != null)
		{
			_resolvedSkills.clear();
		}
	}
	
	/**
	 * Follows the active rules from one ID to its final ID without touching the owner's skills.
	 * @param skillId The ID to resolve.
	 * @return The final ID, or the given one when no rule applies or the chain loops.
	 */
	public int resolveSkillId(int skillId)
	{
		if (skillId <= 0)
		{
			return skillId;
		}
		
		int currentSkillId = skillId;
		
		// A chain visits each source at most once, so needing more steps than there are sources means it loops.
		for (int step = _rules.size(); step > 0; step--)
		{
			final SkillReplacementHolder rule = getWinningRule(currentSkillId);
			if (rule == null)
			{
				return currentSkillId;
			}
			
			final int replacementSkillId = rule.getReplacementSkillId();
			if (replacementSkillId == currentSkillId)
			{
				return currentSkillId;
			}
			
			currentSkillId = replacementSkillId;
		}
		
		return skillId;
	}
	
	/**
	 * Walks the rules backwards, from a target ID to the source that reaches it.
	 * @param skillId The ID to trace back.
	 * @return The source ID at the head of the chain, or the given one when nothing points at it or the chain loops.
	 */
	public int resolveOriginalSkillId(int skillId)
	{
		if (skillId <= 0)
		{
			return skillId;
		}
		
		int currentSkillId = skillId;
		for (int step = _rules.size(); step > 0; step--)
		{
			final int previousSkillId = getWinningSource(currentSkillId);
			if (previousSkillId == currentSkillId)
			{
				return currentSkillId;
			}
			
			currentSkillId = previousSkillId;
		}
		
		return skillId;
	}
	
	/**
	 * Follows the active rules from one skill to the skill it finally becomes.<br>
	 * The skills along the way need not be known by the owner.
	 * @param skill The skill to resolve.
	 * @return The final skill, or the given one when no rule applies.
	 */
	public Skill resolveSkill(Skill skill)
	{
		if (skill == null)
		{
			return null;
		}
		
		final int skillId = skill.getId();
		if (!hasReplacement(skillId))
		{
			return skill;
		}
		
		final ResolvedSkill cachedSkill = _resolvedSkills.get(skillId);
		if ((cachedSkill != null) && cachedSkill.matches(skill))
		{
			return cachedSkill.skill();
		}
		
		int currentSkillId = skillId;
		int currentLevel = skill.getLevel();
		int currentSubLevel = skill.getSubLevel();
		Skill currentSkill = skill;
		for (int step = _rules.size(); step > 0; step--)
		{
			final SkillReplacementHolder rule = getWinningRule(currentSkillId);
			if (rule == null)
			{
				break;
			}
			
			final int replacementSkillId = rule.getReplacementSkillId();
			if (replacementSkillId == currentSkillId)
			{
				break;
			}
			
			final int replacementSkillLevel = rule.getReplacementSkillLevel();
			final Skill nextSkill = SkillData.getInstance().getSkill(replacementSkillId, replacementSkillLevel > 0 ? replacementSkillLevel : currentLevel, currentSubLevel);
			if (nextSkill == null)
			{
				return currentSkill;
			}
			
			currentSkill = nextSkill;
			currentSkillId = nextSkill.getId();
			currentLevel = nextSkill.getLevel();
			currentSubLevel = nextSkill.getSubLevel();
		}
		
		_resolvedSkills.put(skillId, new ResolvedSkill(skill.getLevel(), skill.getSubLevel(), currentSkill));
		return currentSkill;
	}
	
	/**
	 * Finds the one known skill that resolves to the given final ID.<br>
	 * A visual only transform sends final IDs back to the server, and this turns one of them into the skill the owner actually knows.
	 * @param resolvedSkillId The final ID the client sent.
	 * @return The known skill behind it, or {@code null} when none or more than one matches.
	 */
	public Skill findKnownSkillByResolvedId(int resolvedSkillId)
	{
		Skill result = null;
		for (Skill knownSkill : _owner.getAllSkills())
		{
			final Skill resolvedSkill = resolveSkill(knownSkill);
			if ((resolvedSkill == null) || (resolvedSkill.getId() != resolvedSkillId))
			{
				continue;
			}
			
			if ((result != null) && (result.getId() != knownSkill.getId()))
			{
				return null;
			}
			
			result = knownSkill;
		}
		
		return result;
	}
	
	/**
	 * @param skillId The source skill ID.
	 * @return {@code true} when at least one rule starts from that skill.
	 */
	public boolean hasReplacement(int skillId)
	{
		final Map<Long, SkillReplacementHolder> sourceRules = _rules.get(skillId);
		return (sourceRules != null) && !sourceRules.isEmpty();
	}
	
	/**
	 * @return The source skills that currently have a rule.
	 */
	public Collection<Integer> getReplacedSkills()
	{
		return _rules.keySet();
	}
	
	/**
	 * @return A snapshot of every active rule.
	 */
	public Collection<SkillReplacementHolder> getRules()
	{
		final List<SkillReplacementHolder> result = new ArrayList<>();
		for (Map<Long, SkillReplacementHolder> sourceRules : _rules.values())
		{
			result.addAll(sourceRules.values());
		}
		
		return result;
	}
	
	/**
	 * Picks the rule that wins for one source, the highest priority and, among equals, the one registered first.
	 * @param skillId The source skill ID.
	 * @return The winning rule, or {@code null} when the source has none.
	 */
	private SkillReplacementHolder getWinningRule(int skillId)
	{
		final Map<Long, SkillReplacementHolder> sourceRules = _rules.get(skillId);
		if (sourceRules == null)
		{
			return null;
		}
		
		SkillReplacementHolder result = null;
		int resultPriority = 0;
		for (SkillReplacementHolder rule : sourceRules.values())
		{
			final int priority = rule.getPriority();
			if ((result == null) || (priority > resultPriority) || ((priority == resultPriority) && (rule.getActivationOrder() < result.getActivationOrder())))
			{
				result = rule;
				resultPriority = priority;
			}
		}
		
		return result;
	}
	
	/**
	 * Finds the source whose winning rule points at the given ID.
	 * @param skillId The target skill ID.
	 * @return The source ID, or the given one when no winning rule reaches it.
	 */
	private int getWinningSource(int skillId)
	{
		for (Integer sourceSkillId : _rules.keySet())
		{
			final SkillReplacementHolder rule = getWinningRule(sourceSkillId.intValue());
			if ((rule != null) && (rule.getReplacementSkillId() == skillId))
			{
				return rule.getExistingSkillId();
			}
		}
		
		return skillId;
	}
	
	/**
	 * A resolved skill, valid only while its source keeps the level it was resolved for.
	 * @param level The level the source skill had when this was resolved.
	 * @param subLevel The sub level the source skill had when this was resolved.
	 * @param skill The skill the source resolved to.
	 */
	private record ResolvedSkill(int level, int subLevel, Skill skill)
	{
		/**
		 * @param sourceSkill The skill to compare against.
		 * @return {@code true} when the source still carries the level this was resolved for.
		 */
		boolean matches(Skill sourceSkill)
		{
			return (level == sourceSkill.getLevel()) && (subLevel == sourceSkill.getSubLevel());
		}
	}
}
