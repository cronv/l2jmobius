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
import java.util.concurrent.atomic.AtomicBoolean;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenersContainer;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureHpChange;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Provides a base for skill effects that trigger based on changes to a creature's HP.<br>
 * This class handles the logic for monitoring {@code OnCreatureHpChange} events.<br>
 * It allows for dynamic responses when health values are modified.
 * @author Mobius
 */
abstract class AbstractConditionalHpEffect extends AbstractStatEffect
{
	private final int _hpPercent;
	private final Map<Creature, AtomicBoolean> _updates = new ConcurrentHashMap<>();
	
	/**
	 * Initializes a new conditional health effect.<br>
	 * This constructor sets up the base stats and the required health percentage.
	 * @param params The {@code StatSet} containing configuration values like {@code hpPercent}.
	 * @param stat The specific {@code Stat} that this effect will modify.
	 */
	protected AbstractConditionalHpEffect(StatSet params, Stat stat)
	{
		super(params, stat);
		_hpPercent = params.getInt("hpPercent", 0);
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It registers necessary listeners for health changes.<br>
	 * It checks if the skill is valid before proceeding.
	 * @param effector The creature that triggers the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to apply the effect.
	 * @param item The item associated with the effect, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		// Augmentation option
		if (skill == null)
		{
			return;
		}
		
		// Register listeners
		if ((_hpPercent > 0) && !_updates.containsKey(effected))
		{
			_updates.put(effected, new AtomicBoolean(canPump(effector, effected, skill)));
			final ListenersContainer container = effected;
			container.addListener(new ConsumerEventListener(container, EventType.ON_CREATURE_HP_CHANGE, (OnCreatureHpChange event) -> onHpChange(event), this));
		}
	}
	
	/**
	 * This method is called when a skill effect ends.<br>
	 * It cleans up listeners and internal data for the {@code effected} creature.<br>
	 * The method returns early if the {@code skill} parameter is {@code null}.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} that was affected by the skill.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		// Augmentation option
		if (skill == null)
		{
			return;
		}
		
		effected.removeListenerIf(listener -> listener.getOwner() == this);
		_updates.remove(effected);
	}
	
	/**
	 * Checks if the health condition for this effect is met.<br>
	 * It returns {@code true} if the current health percentage of the target is low enough.<br>
	 * The check also passes if the required percentage is zero or less.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to apply the effect.
	 * @return {@code true} if the condition is met, {@code false} otherwise.
	 */
	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return (_hpPercent <= 0) || (effected.getCurrentHpPercent() <= _hpPercent);
	}
	
	/**
	 * Handles the logic when a creature's health changes.<br>
	 * This method checks if stats need to be recalculated based on current conditions.<br>
	 * It uses a thread pool to perform the recalculation asynchronously.
	 * @param event The {@code OnCreatureHpChange} event containing the affected creature.
	 */
	private void onHpChange(OnCreatureHpChange event)
	{
		final Creature creature = event.getCreature();
		final AtomicBoolean update = _updates.get(creature);
		if (update == null)
		{
			return;
		}
		
		if (canPump(null, creature, null))
		{
			if (update.get())
			{
				update.set(false);
				ThreadPool.execute(() -> creature.getStat().recalculateStats(true));
			}
		}
		else if (!update.get())
		{
			update.set(true);
			ThreadPool.execute(() -> creature.getStat().recalculateStats(true));
		}
	}
}
