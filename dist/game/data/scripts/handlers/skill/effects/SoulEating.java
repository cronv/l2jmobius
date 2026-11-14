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

import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Npc;
import org.l2jmobius.gameserver.entity.actor.Playable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.player.OnPlayableExpChanged;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.stats.Stat;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ExSpawnEmitter;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for the {@code SoulEating} skill effect.<br>
 * It manages how experience is transferred between entities when this effect is active.
 * @author UnAfraid
 */
public class SoulEating extends AbstractEffect
{
	private final int _expNeeded;
	private final Double _maxSouls;
	
	/**
	 * Creates a new instance of the {@code SoulEating} effect.<br>
	 * It initializes the required experience and maximum souls from the provided parameters.
	 * @param params The {@code StatSet} containing configuration values like "expNeeded" and "maxSouls".
	 */
	public SoulEating(StatSet params)
	{
		_expNeeded = params.getInt("expNeeded");
		_maxSouls = params.getDouble("maxSouls");
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It sets up listeners for experience changes if the target is a player.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} associated with the skill, if any.
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
	 * It removes the experience change listener from the {@code effected} creature if it is a {@code Player}.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that received the effect.
	 * @param skill The skill used to apply the effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			effected.removeListenerIf(EventType.ON_PLAYABLE_EXP_CHANGED, listener -> listener.getOwner() == this);
		}
	}
	
	/**
	 * Increases the maximum souls of a creature.<br>
	 * This method updates the {@code Stat.MAX_SOULS} for the target.
	 * @param effected The {@code Creature} to receive the effect.
	 * @param skill The {@code Skill} used to trigger this action.
	 */
	@Override
	public void pump(Creature effected, Skill skill)
	{
		effected.getStat().mergeAdd(Stat.MAX_SOULS, _maxSouls);
	}
	
	/**
	 * This method handles the logic when a {@code Playable} entity gains experience.<br>
	 * It checks if the player has enough experience to absorb a soul.<br>
	 * If successful, it increases the player's souls and triggers an effect on their target.
	 * @param playable The {@code Playable} entity that received the experience.
	 * @param exp The amount of experience gained by the entity.
	 */
	private void onExperienceReceived(Playable playable, long exp)
	{
		// TODO: Verify logic.
		if (playable.isPlayer() && (exp >= _expNeeded))
		{
			final Player player = playable.asPlayer();
			final int maxSouls = (int) player.getStat().getValue(Stat.MAX_SOULS, 0);
			if (player.getChargedSouls() >= maxSouls)
			{
				playable.sendPacket(SystemMessageId.YOU_CANNOT_ABSORB_MORE_SOULS);
				return;
			}
			
			player.increaseSouls(1);
			
			if ((player.getTarget() != null) && player.getTarget().isNpc())
			{
				final Npc npc = playable.getTarget().asNpc();
				player.broadcastPacket(new ExSpawnEmitter(player, npc));
			}
		}
	}
}
