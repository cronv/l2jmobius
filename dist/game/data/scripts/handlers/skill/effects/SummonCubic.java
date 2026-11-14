/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package handlers.skill.effects;

import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.data.xml.CubicData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.holders.player.CubicList;
import org.l2jmobius.gameserver.entity.actor.holders.player.ObserverMode;
import org.l2jmobius.gameserver.entity.actor.templates.CubicTemplate;
import org.l2jmobius.gameserver.entity.cubic.Cubic;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.serverpackets.ExUserInfoCubic;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for the {@code SummonCubic} skill effect.<br>
 * This class manages the spawning and behavior of a {@code Cubic} entity.
 * @author Zoey76
 */
public class SummonCubic extends AbstractEffect
{
	private static final Logger LOGGER = Logger.getLogger(SummonCubic.class.getName());
	
	private final int _cubicId;
	private final int _cubicLvl;
	
	/**
	 * Creates a new instance of the {@code SummonCubic} effect.<br>
	 * This constructor initializes the cubic ID and level from the provided data.
	 * @param params The {@code StatSet} containing the required configuration values.
	 */
	public SummonCubic(StatSet params)
	{
		_cubicId = params.getInt("cubicId", -1);
		_cubicLvl = params.getInt("cubicLvl", 0);
	}
	
	/**
	 * Retrieves the unique identifier for the cubic.<br>
	 * This value is used to identify which specific cubic was summoned.
	 * @return The {@code int} ID of the cubic.
	 */
	public int getCubicId()
	{
		return _cubicId;
	}
	
	/**
	 * Checks if the effect occurs instantly.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect of summoning a cubic.<br>
	 * This method validates the target and handles cubic replacement or removal.<br>
	 * It creates a new {@code Cubic} for the player if requirements are met.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature who received the effect.
	 * @param skill The {@code Skill} being used.
	 * @param item The {@code Item} associated with the action.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_cubicId < 0)
		{
			LOGGER.warning(SummonCubic.class.getSimpleName() + ": Invalid Cubic ID:" + _cubicId + " in skill ID: " + skill.getId());
			return;
		}
		
		if (!effected.isPlayer() || effected.isAlikeDead())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		final ObserverMode observerMode = player.getObserverMode();
		if (observerMode.isActive())
		{
			return;
		}
		
		if (observerMode.isActive() || player.isMounted())
		{
			return;
		}
		
		// If cubic is already present, it's replaced.
		final CubicList cubicList = player.getCubicList();
		final Cubic cubic = cubicList.getById(_cubicId);
		if (cubic != null)
		{
			if (cubic.getTemplate().getLevel() > _cubicLvl)
			{
				// What do we do in such case?
				return;
			}
			
			cubic.deactivate();
		}
		else
		{
			// If maximum amount is reached, random cubic is removed.
			// Players with no mastery can have only one cubic.
			final double allowedCubicCount = player.getStat().getValue(Stat.MAX_CUBIC, 1);
			
			// Extra cubics are removed, one by one, randomly.
			final int currentCubicCount = cubicList.size();
			if (currentCubicCount >= allowedCubicCount)
			{
				cubicList.getCubics().values().stream().skip((int) (currentCubicCount * Rnd.nextDouble())).findAny().get().deactivate();
			}
		}
		
		final CubicTemplate template = CubicData.getInstance().getCubicTemplate(_cubicId, _cubicLvl);
		if (template == null)
		{
			LOGGER.warning("Attempting to summon cubic without existing template id: " + _cubicId + " level: " + _cubicLvl);
			return;
		}
		
		// Adding a new cubic.
		cubicList.add(new Cubic(player, effector.asPlayer(), template));
		player.sendPacket(new ExUserInfoCubic(player));
		player.broadcastCharInfo();
	}
}
