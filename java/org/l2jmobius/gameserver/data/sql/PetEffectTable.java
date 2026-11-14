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

import org.l2jmobius.gameserver.data.holders.PetEffect;

/**
 * This class caches active skill effects for pets during the period between despawn and resummon.<br>
 * It ensures that effects are restored upon the next summon without losing their remaining duration.<br>
 * Effects are stored in a {@code ConcurrentHashMap} keyed by the pet's control item object id.
 * @author Nyaran, Mobius
 */
public class PetEffectTable
{
	// Pet Effects.
	// Key: pet control item object id, Value: cached effects.
	private final Map<Integer, Collection<PetEffect>> _petEffects = new ConcurrentHashMap<>();
	
	/**
	 * Retrieves the internal map of all cached pet effects.<br>
	 * The keys represent the unique object IDs for control items.
	 * @return a {@code Map} containing every cached {@code PetEffect} collection.
	 */
	public Map<Integer, Collection<PetEffect>> getPetEffects()
	{
		return _petEffects;
	}
	
	/**
	 * Retrieves the collection of {@code PetEffect} objects for a specific pet.<br>
	 * This method looks up effects based on the provided control object ID.<br>
	 * It returns {@code null} if no effects are found for that ID.
	 * @param controlObjectId The unique identifier of the pet's control item.
	 * @return A {@code Collection} of {@code PetEffect} objects, or {@code null}.
	 */
	public Collection<PetEffect> getPetEffectsFor(int controlObjectId)
	{
		return _petEffects.get(controlObjectId);
	}
	
	/**
	 * Checks if a specific pet has any active effects.<br>
	 * This method looks up the collection of effects for the given ID.<br>
	 * It returns {@code true} if there are one or more effects present.
	 * @param controlObjectId The unique identifier for the pet's control item.
	 * @return {@code true} if the pet has active effects, {@code false} otherwise.
	 */
	public boolean hasPetEffects(int controlObjectId)
	{
		final Collection<PetEffect> effects = _petEffects.get(controlObjectId);
		return (effects != null) && !effects.isEmpty();
	}
	
	/**
	 * Adds a new {@code PetEffect} to a specific pet.<br>
	 * This method links the effect to the provided {@code controlObjectId}.<br>
	 * It ensures that effects are saved for later restoration.
	 * @param controlObjectId The unique ID of the pet's control item.
	 * @param effect The {@code PetEffect} instance to be added.
	 */
	public void addPetEffect(int controlObjectId, PetEffect effect)
	{
		_petEffects.computeIfAbsent(controlObjectId, _ -> ConcurrentHashMap.newKeySet()).add(effect);
	}
	
	/**
	 * Removes all cached pet effects for a specific control object.<br>
	 * This method clears the collection of {@code PetEffect} objects associated with the provided ID.<br>
	 * If no effects exist for the given ID, the method does nothing.
	 * @param controlObjectId The unique identifier of the pet's control item to clear.
	 */
	public void clearPetEffects(int controlObjectId)
	{
		final Collection<PetEffect> effects = _petEffects.get(controlObjectId);
		if (effects != null)
		{
			effects.clear();
		}
	}
	
	/**
	 * Removes all cached effects for a specific pet.<br>
	 * This clears the data associated with the provided {@code controlObjectId}.
	 * @param controlObjectId The unique ID of the pet control item to clear.
	 */
	public void removePetEffects(int controlObjectId)
	{
		_petEffects.remove(controlObjectId);
	}
	
	/**
	 * Provides access to the singleton instance of {@code PetEffectTable}.<br>
	 * Use this method to get the global table for managing pet effects.
	 * @return the single instance of {@code PetEffectTable}.
	 */
	public static PetEffectTable getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final PetEffectTable INSTANCE = new PetEffectTable();
	}
}
