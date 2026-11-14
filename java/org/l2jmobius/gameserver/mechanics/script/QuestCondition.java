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
package org.l2jmobius.gameserver.mechanics.script;

import java.util.function.Predicate;

import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Player;

/**
 * Binds a player predicate to an HTML quest dialog response.
 * @author Galagard
 */
public class QuestCondition
{
	private final Predicate<Player> _condition;
	private final String _html;
	
	/**
	 * Binds a player-state predicate to an HTML response.
	 * @param condition The {@code Predicate} evaluated against a {@code Player}.
	 * @param html The HTML shown when the condition is met.
	 */
	public QuestCondition(Predicate<Player> condition, String html)
	{
		_condition = condition;
		_html = html;
	}
	
	/**
	 * Evaluates the bound condition against the given player.
	 * @param player The {@code Player} to test.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	public boolean test(Player player)
	{
		return _condition.test(player);
	}
	
	/**
	 * Retrieves the HTML associated with this condition.
	 * @param npc The {@code Npc} that triggered the condition.
	 * @return The HTML as a {@code String}.
	 */
	public String getHtml(Npc npc)
	{
		return _html;
	}
}
