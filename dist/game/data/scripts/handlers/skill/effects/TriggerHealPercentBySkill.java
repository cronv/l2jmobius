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

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureSkillFinishCast;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.SystemMessage;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for a skill effect that heals a creature based on a percentage.<br>
 * It triggers when a specific {@code Skill} is completed by an actor.
 * @author NasSeKa
 */
public class TriggerHealPercentBySkill extends AbstractEffect
{
	private final int _castSkillId;
	private final int _chance;
	private final int _power;
	
	/**
	 * Creates a new instance of the heal percent effect.<br>
	 * It initializes the effect using values from a {@code StatSet}.<br>
	 * The constructor sets the skill ID, chance, and power levels.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public TriggerHealPercentBySkill(StatSet params)
	{
		_castSkillId = params.getInt("castSkillId");
		_chance = params.getInt("chance", 100);
		_power = params.getInt("power", 0);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It checks if the required conditions are met to register a listener.<br>
	 * If valid, it adds an event listener to the {@code effected} creature.
	 * @param effector The creature that triggers the effect.
	 * @param effected The creature that receives the effect.
	 * @param skill The skill being used.
	 * @param item The item involved in the action.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		if ((_chance == 0) || (_castSkillId == 0))
		{
			return;
		}
		
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_SKILL_FINISH_CAST, (OnCreatureSkillFinishCast event) -> onSkillUseEvent(event), this));
	}
	
	/**
	 * This method is called when the effect finishes.<br>
	 * It removes the specific skill finish cast listener from the {@code effected} creature.<br>
	 * This ensures that the effect does not trigger multiple times for the same action.
	 * @param effector The {@code Creature} who performed the action.
	 * @param effected The {@code Creature} who received the effect.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_SKILL_FINISH_CAST, listener -> listener.getOwner() == this);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.HEAL;
	}
	
	/**
	 * Handles the logic when a creature finishes casting a skill.<br>
	 * This method checks if the casted skill matches the required ID.<br>
	 * It then calculates and applies a healing amount to the target player based on the defined power and chance.
	 * @param event The {@code OnCreatureSkillFinishCast} event containing the skill and target information.
	 */
	private void onSkillUseEvent(OnCreatureSkillFinishCast event)
	{
		if (_castSkillId != event.getSkill().getId())
		{
			return;
		}
		
		final WorldObject target = event.getTarget();
		if (target == null)
		{
			return;
		}
		
		final Player player = target.asPlayer();
		if ((player == null) || player.isDead() || player.isHpBlocked())
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		double amount = 0;
		final double power = _power;
		final boolean full = (power == 100.0);
		
		final long maxHp = player.getMaxHp();
		amount = full ? maxHp : (maxHp * power) / 100.0;
		
		// Prevents overheal.
		amount = Math.min(amount, Math.max(0, player.getMaxRecoverableHp() - player.getCurrentHp()));
		if (amount >= 0)
		{
			if (amount != 0)
			{
				player.setCurrentHp(amount + player.getCurrentHp(), false);
				player.broadcastStatusUpdate(player);
			}
			
			SystemMessage sm;
			sm = new SystemMessage(SystemMessageId.YOU_VE_RECOVERED_S1_HP);
			sm.addInt((int) amount);
			player.sendPacket(sm);
		}
	}
}
