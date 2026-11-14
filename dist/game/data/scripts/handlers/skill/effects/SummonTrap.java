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
import org.l2jmobius.gameserver.entity.actor.holders.player.ObserverMode;
import org.l2jmobius.gameserver.entity.actor.instance.Trap;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code SummonTrap} skill effect.<br>
 * It manages the creation and behavior of a {@code Trap} entity in the game world.
 * @author Zoey76
 */
public class SummonTrap extends AbstractEffect
{
	private final int _despawnTime;
	private final int _npcId;
	
	/**
	 * Creates a new {@code SummonTrap} effect.<br>
	 * This constructor initializes the trap using values from a {@code StatSet}.<br>
	 * It sets the despawn time and the NPC ID for the trap.
	 * @param params The {@code StatSet} containing configuration data.
	 */
	public SummonTrap(StatSet params)
	{
		_despawnTime = params.getInt("despawnTime", 0);
		_npcId = params.getInt("npcId", 0);
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
	 * Executes the instant effect of a summon trap.<br>
	 * This method handles spawning the trap for a valid player.<br>
	 * It ensures that any previous trap is removed before creating a new one.
	 * @param effector The creature who performs the action.
	 * @param effected The creature who receives the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (_npcId <= 0)
		{
			LOGGER.warning(SummonTrap.class.getSimpleName() + ": Invalid NPC ID:" + _npcId + " in skill ID: " + skill.getId());
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
		
		// Unsummon previous trap
		final Trap currentTrap = player.getTrap();
		if (currentTrap != null)
		{
			currentTrap.unSummon();
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			LOGGER.warning(SummonTrap.class.getSimpleName() + ": Spawn of the non-existing Trap ID: " + _npcId + " in skill ID:" + skill.getId());
			return;
		}
		
		final Trap trap = new Trap(npcTemplate, player, _despawnTime);
		trap.fullRestore();
		trap.setInvul(true);
		trap.setHeading(player.getHeading());
		trap.spawnMe(player.getX(), player.getY(), player.getZ());
		player.addSummonedNpc(trap); // player.setTrap(trap);
	}
}
