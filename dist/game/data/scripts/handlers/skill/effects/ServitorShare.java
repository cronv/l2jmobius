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

import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.stat.CreatureStat;
import org.l2jmobius.gameserver.entity.actor.stat.PlayerStat;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code ServitorShare} skill effect.<br>
 * It manages how stats are shared between entities when this effect is active.
 * @author Mobius
 */
public class ServitorShare extends AbstractEffect
{
	private final Map<Stat, Float> _sharedStats = new EnumMap<>(Stat.class);
	
	/**
	 * Creates a new {@code ServitorShare} effect.<br>
	 * This constructor initializes the shared statistics from the provided parameters.<br>
	 * It converts percentage values into decimal multipliers for each {@code Stat}.
	 * @param params The {@code StatSet} containing the stats to be shared.
	 */
	public ServitorShare(StatSet params)
	{
		if (params.isEmpty())
		{
			return;
		}
		
		for (Entry<String, Object> param : params.getSet().entrySet())
		{
			_sharedStats.put(Stat.valueOf(param.getKey()), (Float.parseFloat((String) param.getValue())) / 100);
		}
	}
	
	/**
	 * Checks if the effect is allowed to perform a pump action.<br>
	 * This method currently always returns {@code true}.
	 * @return {@code true} if the pump can be delayed.
	 */
	@Override
	public boolean delayPump()
	{
		return true;
	}
	
	/**
	 * Checks if the {@code effected} creature is eligible to receive a stat boost.<br>
	 * This method returns {@code true} only if the target is a summon.
	 * @param effector The creature performing the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being used to apply the effect.
	 * @return {@code true} if the target is a summon, {@code false} otherwise.
	 */
	@Override
	public boolean canPump(Creature effector, Creature effected, Skill skill)
	{
		return effected.isSummon();
	}
	
	/**
	 * Applies shared statistics from a {@code Player} to the {@code effected} creature.<br>
	 * This method calculates values based on the multipliers stored in {@code _sharedStats}.<br>
	 * It ensures that only valid {@code Player} objects are processed.
	 * @param effected The {@code Creature} receiving the stat boost.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		final PlayerStat playerStat = player.getStat();
		final CreatureStat effectedStat = effected.getStat();
		for (Entry<Stat, Float> entry : _sharedStats.entrySet())
		{
			final Stat stat = entry.getKey();
			effectedStat.mergeAdd(stat, playerStat.getValue(stat) * entry.getValue());
		}
	}
	
	/**
	 * This method is called when the effect expires.<br>
	 * It updates the stats of the affected creature and its servitors.<br>
	 * If the target is a summon, it recalculates its own stats.<br>
	 * If the target is a player, it also refreshes all their servitors.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} receiving the effect.
	 * @param skill The {@code Skill} used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isSummon())
		{
			effected.getStat().recalculateStats(true);
			return;
		}
		
		final Player player = effected.asPlayer();
		if (player == null)
		{
			return;
		}
		
		player.getServitors().values().forEach(s -> s.getStat().recalculateStats(true));
	}
}
