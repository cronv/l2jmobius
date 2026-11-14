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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.enums.ItemProcessType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayableExpChanged;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for granting an {@code Item} to a player based on experience changes.<br>
 * It listens for {@code OnPlayableExpChanged} events to trigger item rewards.
 * @author Mobius
 */
public class GiveItemByExp extends AbstractEffect
{
	private static final Map<Player, Long> PLAYER_VALUES = new ConcurrentHashMap<>();
	
	private final long _exp;
	private final int _itemId;
	
	/**
	 * Initializes a new effect that grants an item based on experience gained.<br>
	 * This constructor extracts the required values from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the {@code exp} and {@code itemId} values.
	 */
	public GiveItemByExp(StatSet params)
	{
		_exp = params.getLong("exp", 0);
		_itemId = params.getInt("itemId", 0);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It checks if the {@code effected} creature is a {@code Player}.<br>
	 * If it is, it adds an experience change listener to them.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with this effect.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isPlayer())
		{
			effected.addListener(new ConsumerEventListener(effected, EventType.ON_PLAYABLE_EXP_CHANGED, (OnPlayableExpChanged event) -> onExperienceReceived(event.getPlayable(), (event.getNewExp() - event.getOldExp())), this));
		}
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It cleans up data for the {@code effected} creature.<br>
	 * It removes specific listeners if the target is a {@code Player}.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			PLAYER_VALUES.remove(effected.asPlayer());
			effected.removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, listener -> listener.getOwner() == this);
		}
	}
	
	/**
	 * Handles the logic for awarding an item when a {@code Playable} gains experience.<br>
	 * It tracks accumulated experience for each player.<br>
	 * If the total reaches the required amount, the player receives one item.
	 * @param playable The entity receiving the experience.
	 * @param exp The amount of experience gained.
	 */
	private void onExperienceReceived(Playable playable, long exp)
	{
		if (exp < 1)
		{
			return;
		}
		
		final Player player = playable.asPlayer();
		final long sum = PLAYER_VALUES.getOrDefault(player, 0L) + exp;
		if (sum >= _exp)
		{
			PLAYER_VALUES.remove(player);
			player.addItem(ItemProcessType.REWARD, _itemId, 1, player, true);
		}
		else
		{
			PLAYER_VALUES.put(player, sum);
		}
	}
}
