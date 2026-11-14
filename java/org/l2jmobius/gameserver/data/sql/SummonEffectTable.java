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
package org.l2jmobius.gameserver.data.sql;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.data.holders.SummonEffect;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * This class caches active skill effects for servitors during the period between despawn and resummon.<br>
 * It ensures that effects are restored on the next summon without losing their remaining duration.<br>
 * The data is stored in a thread-safe manner to support multi-threaded summon lifecycle events.
 * @author Nyaran, Mobius
 */
public class SummonEffectTable
{
	// Servitor Effects.
	// Key: ownerObjectId, Value: per-class map.
	// Key: classIndex, Value: per-skill map.
	// Key: referenceSkillId, Value: cached effects.
	private final Map<Integer, Map<Integer, Map<Integer, Collection<SummonEffect>>>> _servitorEffects = new ConcurrentHashMap<>();
	
	/**
	 * Retrieves the full map of cached servitor effects.<br>
	 * The data is organized by owner ID, class index, and skill ID.<br>
	 * This provides access to all stored {@code SummonEffect} collections.
	 * @return a nested map containing all cached servitor effects.
	 */
	public Map<Integer, Map<Integer, Map<Integer, Collection<SummonEffect>>>> getServitorEffectsOwner()
	{
		return _servitorEffects;
	}
	
	/**
	 * Retrieves the cached effects for a specific player's servitors.<br>
	 * This method returns a map of class indices to their respective effect collections.<br>
	 * It returns {@code null} if the player has no cached effects.
	 * @param owner The {@code Player} whose servitor effects are being requested.
	 * @return A map where keys are class indices and values are collections of {@code SummonEffect}.
	 */
	public Map<Integer, Collection<SummonEffect>> getServitorEffects(Player owner)
	{
		final Map<Integer, Map<Integer, Collection<SummonEffect>>> servitorMap = _servitorEffects.get(owner.getObjectId());
		if (servitorMap == null)
		{
			return null;
		}
		
		return servitorMap.get(owner.getClassIndex());
	}
	
	/**
	 * Retrieves the cached {@code SummonEffect} objects for a specific skill.<br>
	 * This method looks up effects based on the owner, class, and skill ID.<br>
	 * It returns {@code null} if no matching data is found.
	 * @param ownerObjectId The unique identifier of the owner.
	 * @param classIndex The index representing the specific class.
	 * @param referenceSkill The ID of the skill to check.
	 * @return A {@code Collection} of {@code SummonEffect} objects or {@code null}.
	 */
	public Collection<SummonEffect> getServitorEffectsForSkill(int ownerObjectId, int classIndex, int referenceSkill)
	{
		final Map<Integer, Map<Integer, Collection<SummonEffect>>> classMap = _servitorEffects.get(ownerObjectId);
		if (classMap == null)
		{
			return null;
		}
		
		final Map<Integer, Collection<SummonEffect>> skillMap = classMap.get(classIndex);
		if (skillMap == null)
		{
			return null;
		}
		
		return skillMap.get(referenceSkill);
	}
	
	/**
	 * Checks if there are any active servitor effects for a specific skill.<br>
	 * This is used to determine if cached effects exist for the given owner and class.
	 * @param ownerObjectId The unique ID of the owner.
	 * @param classIndex The index of the servitor class.
	 * @param referenceSkill The ID of the skill being checked.
	 * @return {@code true} if effects exist, otherwise {@code false}.
	 */
	public boolean hasServitorEffectsForSkill(int ownerObjectId, int classIndex, int referenceSkill)
	{
		final Collection<SummonEffect> effects = getServitorEffectsForSkill(ownerObjectId, classIndex, referenceSkill);
		return (effects != null) && !effects.isEmpty();
	}
	
	/**
	 * Adds a {@code SummonEffect} to the cache for a specific servitor.<br>
	 * This ensures effects persist between despawn and resummon events.<br>
	 * The effect is stored based on the owner, class, and skill identifiers.
	 * @param ownerObjectId The unique ID of the player who owns the servitor.
	 * @param classIndex The index representing the specific servitor class.
	 * @param referenceSkill The ID of the skill associated with the effect.
	 * @param effect The {@code SummonEffect} object to be added to the cache.
	 */
	public void addServitorEffect(int ownerObjectId, int classIndex, int referenceSkill, SummonEffect effect)
	{
		_servitorEffects.computeIfAbsent(ownerObjectId, _ -> new ConcurrentHashMap<>()).computeIfAbsent(classIndex, _ -> new ConcurrentHashMap<>()).computeIfAbsent(referenceSkill, _ -> ConcurrentHashMap.newKeySet()).add(effect);
	}
	
	/**
	 * Removes all cached effects for a specific skill of a servitor.<br>
	 * This method clears the collection associated with the provided identifiers.<br>
	 * If any of the maps are null, no action is taken.
	 * @param ownerObjectId The unique identifier of the owner.
	 * @param classIndex The index representing the servitor class.
	 * @param referenceSkill The ID of the skill to clear effects for.
	 */
	public void clearServitorEffectsForSkill(int ownerObjectId, int classIndex, int referenceSkill)
	{
		final Map<Integer, Map<Integer, Collection<SummonEffect>>> classMap = _servitorEffects.get(ownerObjectId);
		if (classMap == null)
		{
			return;
		}
		
		final Map<Integer, Collection<SummonEffect>> skillMap = classMap.get(classIndex);
		if (skillMap == null)
		{
			return;
		}
		
		final Collection<SummonEffect> effects = skillMap.get(referenceSkill);
		if (effects != null)
		{
			effects.clear();
		}
	}
	
	/**
	 * Removes all cached servitor effects for a specific owner.<br>
	 * This clears the data associated with the provided {@code ownerObjectId}.<br>
	 * Use this when an owner is permanently removed or no longer needs cached effects.
	 * @param ownerObjectId The unique ID of the owner whose effects should be deleted.
	 */
	public void removeServitorEffects(int ownerObjectId)
	{
		_servitorEffects.remove(ownerObjectId);
	}
	
	/**
	 * Gets the singleton instance of the {@code SummonEffectTable}.<br>
	 * Use this method to access the global table for managing summon effects.
	 * @return the single instance of {@code SummonEffectTable}.
	 */
	public static SummonEffectTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final SummonEffectTable INSTANCE = new SummonEffectTable();
	}
}
