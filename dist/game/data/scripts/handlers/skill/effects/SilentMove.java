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

import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code SilentMove} effect.<br>
 * It allows a character to move without making any noise or being detected.<br>
 * It extends {@code AbstractEffect} to manage its behavior within the game engine.
 */
public class SilentMove extends AbstractEffect
{
	private final int _chance;
	
	/**
	 * Creates a new instance of the {@code SilentMove} effect.<br>
	 * This constructor initializes the effect using the provided statistics.
	 * @param params The {@code StatSet} containing the required parameters.
	 */
	public SilentMove(StatSet params)
	{
		_chance = params.getInt("chance", 100); // Retail p_avoid_agro.
	}
	
	/**
	 * Returns the chance to avoid aggro, the retail p_avoid_agro value (Fake Death uses it for its shake-off roll).
	 * @return The chance in percent, {@code 100} by default.
	 */
	public int getChance()
	{
		return _chance;
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.SILENT_MOVE}.
	 * @return the {@code long} mask representing the silent move flag.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.SILENT_MOVE.getMask();
	}
}
