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
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureDamageReceived;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.network.serverpackets.SkillCoolTime;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect allows a skill to be reused based on the amount of damage dealt.<br>
 * It monitors {@code OnCreatureDamageReceived} events to trigger specific logic when damage thresholds are met.
 * @author NasSeKa
 */
public class ReuseSkillIdByDamage extends AbstractEffect
{
	private final int _minAttackerLevel;
	private final int _maxAttackerLevel;
	private final int _minDamage;
	private final int _chance;
	private final int _hpPercent;
	private final int _skillId;
	private final int _amount;
	private final InstanceType _attackerType;
	
	/**
	 * Initializes a new effect that triggers a skill based on damage received.<br>
	 * This constructor reads all required configuration values from the provided {@code StatSet}.
	 * @param params The {@code StatSet} containing the configuration for this effect.
	 */
	public ReuseSkillIdByDamage(StatSet params)
	{
		_minAttackerLevel = params.getInt("minAttackerLevel", 1);
		_maxAttackerLevel = params.getInt("maxAttackerLevel", Integer.MAX_VALUE);
		_minDamage = params.getInt("minDamage", 1);
		_chance = params.getInt("chance", 100);
		_hpPercent = params.getInt("hpPercent", 100);
		_skillId = params.getInt("skillId", 0);
		_amount = params.getInt("amount", 0);
		_attackerType = params.getEnum("attackerType", InstanceType.class, InstanceType.Creature);
	}
	
	/**
	 * Handles the logic when a creature receives damage.<br>
	 * This method checks various conditions such as attacker level, damage amount, and chance.<br>
	 * If all conditions are met, it may reduce or reset the cooldown of a specific skill for the target player.
	 * @param event The {@code OnCreatureDamageReceived} event containing details about the damage dealt.
	 */
	private void onDamageReceivedEvent(OnCreatureDamageReceived event)
	{
		if (event.isDamageOverTime() || (_chance == 0))
		{
			return;
		}
		
		final Creature attacker = event.getAttacker();
		final Creature target = event.getTarget();
		if (attacker == target)
		{
			return;
		}
		
		final int attackerLevel = attacker.getLevel();
		if ((attackerLevel < _minAttackerLevel) || (attackerLevel > _maxAttackerLevel))
		{
			return;
		}
		
		if (event.getDamage() < _minDamage)
		{
			return;
		}
		
		if ((_chance < 100) && (Rnd.get(100) > _chance))
		{
			return;
		}
		
		if ((_hpPercent < 100) && (attacker.getCurrentHpPercent() > _hpPercent))
		{
			return;
		}
		
		if (!attacker.getInstanceType().isType(_attackerType))
		{
			return;
		}
		
		final Player player = target.asPlayer();
		if (player != null)
		{
			final Skill s = player.getKnownSkill(_skillId);
			if (s != null)
			{
				if (_amount > 0)
				{
					final long reuse = player.getSkillRemainingReuseTime(s.getReuseHashCode());
					if (reuse > 0)
					{
						player.removeTimeStamp(s);
						player.addTimeStamp(s, Math.max(0, reuse - _amount));
						player.sendPacket(new SkillCoolTime(player));
					}
				}
				else
				{
					player.removeTimeStamp(s);
					player.enableSkill(s);
					player.sendPacket(new SkillCoolTime(player));
				}
			}
		}
	}
	
	/**
	 * This method is called when the effect finishes.<br>
	 * It removes the damage listener from the {@code effected} creature.<br>
	 * This ensures that the effect no longer triggers on subsequent hits.
	 * @param effector The creature that cast the skill.
	 * @param effected The creature that was affected by the skill.
	 * @param skill The skill used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
		effected.removeListenerIf(EventType.ON_CREATURE_DAMAGE_RECEIVED, listener -> listener.getOwner() == this);
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It attaches a damage listener to the {@code effected} creature.<br>
	 * The listener triggers the {@code onDamageReceivedEvent} logic.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The skill used to trigger the effect.
	 * @param item The item involved in the action, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_DAMAGE_RECEIVED, (OnCreatureDamageReceived event) -> onDamageReceivedEvent(event), this));
	}
}
