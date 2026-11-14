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

import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.SiegeFlag;
import org.l2jmobius.gameserver.entity.clan.Clan;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for creating a headquarter.<br>
 * This class manages the specific effects triggered when a player successfully creates a new base.
 * @author Adry_85
 */
public class HeadquarterCreate extends AbstractEffect
{
	private static final int HQ_NPC_ID = 35062;
	private final boolean _isAdvanced;
	
	/**
	 * Creates a new instance of the {@code HeadquarterCreate} effect.<br>
	 * This constructor initializes the effect using provided parameters.
	 * @param params The {@code StatSet} containing configuration values.
	 */
	public HeadquarterCreate(StatSet params)
	{
		_isAdvanced = params.getBoolean("isAdvanced", false);
	}
	
	/**
	 * Checks if the effect happens immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant effect for creating a headquarter.<br>
	 * This method checks if the {@code effector} is the leader of a clan.<br>
	 * It spawns a new siege flag based on the provided parameters.
	 * @param effector The {@code Creature} who initiates the action.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill usage.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		final Player player = effector.asPlayer();
		final Clan clan = player.getClan();
		if ((clan == null) || (clan.getLeaderId() != player.getObjectId()))
		{
			return;
		}
		
		final SiegeFlag flag = new SiegeFlag(player, NpcData.getInstance().getTemplate(HQ_NPC_ID), _isAdvanced);
		flag.setTitle(clan.getName());
		flag.setCurrentHpMp(flag.getMaxHp(), flag.getMaxMp());
		flag.setHeading(player.getHeading());
		flag.spawnMe(player.getX(), player.getY(), player.getZ() + 50);
	}
}
