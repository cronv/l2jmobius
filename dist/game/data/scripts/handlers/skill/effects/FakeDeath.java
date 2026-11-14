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

import java.util.List;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.World;
import org.l2jmobius.gameserver.entity.actor.Attackable;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectFlag;
import org.l2jmobius.gameserver.mechanics.skill.EffectScope;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.ChangeWaitType;
import org.l2jmobius.gameserver.network.serverpackets.Revive;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the {@code FakeDeath} effect.<br>
 * It simulates a death state for an entity without actually killing it.
 * @author mkizub, Mobius
 */
public class FakeDeath extends AbstractEffect
{
	private final double _power;
	
	/**
	 * Creates a new instance of the {@code FakeDeath} effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the duration in ticks and the power value.
	 * @param params The {@code StatSet} containing configuration data for the effect.
	 */
	public FakeDeath(StatSet params)
	{
		_power = params.getDouble("power", 0);
		setTicks(params.getInt("ticks"));
		
		if (params.contains("amount"))
		{
			throw new IllegalArgumentException(getClass().getSimpleName() + " should use power instead of amount.");
		}
	}
	
	/**
	 * Retrieves the bitmask for this effect.<br>
	 * It returns the value associated with {@code EffectFlag.FAKE_DEATH}.
	 * @return The bitmask of the effect flags as a {@code long}.
	 */
	@Override
	public long getEffectFlags()
	{
		return EffectFlag.FAKE_DEATH.getMask();
	}
	
	/**
	 * Handles the logic that occurs during each tick of the effect.<br>
	 * It reduces the mana of the {@code effected} creature based on the effect power.<br>
	 * The method checks if the target is alive and has enough mana for toggle skills.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill being executed.
	 * @param item The item used to cast the skill, if any.
	 * @return {@code true} if the skill is a toggle and continues, {@code false} otherwise.
	 */
	@Override
	public boolean onActionTime(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isDead())
		{
			return false;
		}
		
		final double manaDam = _power * getTicksMultiplier();
		if ((manaDam > effected.getCurrentMp()) && skill.isToggle())
		{
			effected.sendPacket(SystemMessageId.YOUR_SKILL_WAS_DEACTIVATED_DUE_TO_LACK_OF_MP);
			return false;
		}
		
		effected.reduceCurrentMp(manaDam);
		
		return skill.isToggle();
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It handles the logic for reviving a player from a fake death state.<br>
	 * It updates the player status and sends necessary network packets.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} who received the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		if (effected.isPlayer())
		{
			effected.asPlayer().setRecentFakeDeath(true);
		}
		
		effected.broadcastPacket(new ChangeWaitType(effected, ChangeWaitType.WT_STOP_FAKEDEATH));
		effected.broadcastPacket(new Revive(effected));
	}
	
	/**
	 * This method is called when the effect begins.<br>
	 * It triggers the fake death state on the target creature.
	 * @param effector The creature who cast the skill.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 * @param item The {@code Item} involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.startFakeDeath();
		
		// Retail p_avoid_agro (the SilentMove chance of the skill): each monster hating the player is shaken off with it, the others see through it.
		if (effected.isPlayer())
		{
			final Player player = effected.asPlayer();
			player.getFakeDeathWatchers().clear();
			int avoidAggro = 100;
			final List<AbstractEffect> effects = skill.getEffects(EffectScope.GENERAL);
			if (effects != null)
			{
				for (AbstractEffect effect : effects)
				{
					if (effect instanceof SilentMove)
					{
						avoidAggro = ((SilentMove) effect).getChance();
					}
				}
			}
			
			if (avoidAggro < 100)
			{
				final int chance = avoidAggro;
				World.forEachVisibleObject(player, Attackable.class, npc ->
				{
					if (npc.getAggroList().containsKey(player) && (Rnd.get(100) >= chance))
					{
						player.getFakeDeathWatchers().add(npc.getObjectId());
					}
				});
			}
		}
	}
}
