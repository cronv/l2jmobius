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

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.l2jmobius.gameserver.entity.WorldObject;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Summon;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureAttack;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This effect allows a {@code Summon} to perform an attack on behalf of its owner.<br>
 * It triggers when the owner performs an attack action.<br>
 * The {@code Summon} will then execute an attack towards the same target.
 * @author Liamxroy
 */
public class AssistOwnerAttack extends AbstractEffect
{
	private final Set<Integer> _summonIds;
	
	/**
	 * Creates a new {@code AssistOwnerAttack} effect.<br>
	 * This constructor initializes the effect using provided parameters.<br>
	 * It parses the summon IDs from the {@code StatSet}.
	 * @param params The {@code StatSet} containing configuration data.
	 */
	public AssistOwnerAttack(StatSet params)
	{
		String summonIdString = params.getString("summonId");
		_summonIds = Arrays.stream(summonIdString.split(";")).map(Integer::parseInt).collect(Collectors.toSet());
	}
	
	/**
	 * This method is called when the effect starts.<br>
	 * It registers a listener to handle attack events for the target creature.
	 * @param effector The creature that triggered the effect.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to apply this effect.
	 * @param item The {@code Item} associated with the skill, if any.
	 */
	@Override
	public void onStart(Creature effector, Creature effected, Skill skill, Item item)
	{
		effected.addListener(new ConsumerEventListener(effected, EventType.ON_CREATURE_ATTACK, (OnCreatureAttack event) -> onAttackEvent(event), this));
	}
	
	/**
	 * This method is called when the effect ends.<br>
	 * It handles any cleanup logic required after the skill effect expires.
	 * @param effector The {@code Creature} that cast the skill.
	 * @param effected The {@code Creature} that was affected by the skill.
	 * @param skill The {@code Skill} used to trigger this effect.
	 */
	@Override
	public void onExit(Creature effector, Creature effected, Skill skill)
	{
	}
	
	/**
	 * Processes the attack logic for a specific creature and target.<br>
	 * This method is triggered when an attack event occurs.<br>
	 * It delegates the action to {@code processSummonActions}.
	 * @param caster The {@code Creature} performing the attack.
	 * @param target The {@code WorldObject} being attacked.
	 */
	private void handleAttackEvent(Creature caster, WorldObject target)
	{
		processSummonActions(caster, target);
	}
	
	/**
	 * This method handles the behavior of summons when a caster attacks.<br>
	 * It identifies valid summons and commands them to attack the target.<br>
	 * Summons are updated based on their specific type and status.
	 * @param caster The {@code Creature} who initiated the action.
	 * @param target The {@code WorldObject} being targeted by the caster.
	 */
	private void processSummonActions(Creature caster, WorldObject target)
	{
		if (caster.getServitors() != null)
		{
			for (Summon servitor : caster.getServitors().values())
			{
				if ((servitor != null) && _summonIds.contains(servitor.getId()) && !servitor.isDisabled())
				{
					servitor.setTarget(target);
					servitor.doAttack(target);
				}
			}
		}
		
		if (caster.getSummonedNpcCount() != 0)
		{
			caster.getSummonedNpcs().forEach(summon ->
			{
				if (_summonIds.contains(summon.getId()) && !summon.isDisabled())
				{
					summon.setTarget(target);
					summon.moveToLocation(target.getX(), target.getY(), target.getZ(), summon.getPhysicalAttackRange());
					summon.doAutoAttack(target.asCreature());
				}
			});
		}
	}
	
	/**
	 * Handles the logic when a creature performs an attack.<br>
	 * This method checks if the attacker is a player with active summons.<br>
	 * It also verifies that the target is a valid creature before processing.
	 * @param event The {@code OnCreatureAttack} event containing attacker and target data.
	 */
	private void onAttackEvent(OnCreatureAttack event)
	{
		final Creature caster = event.getAttacker();
		if (((caster.getSummonedNpcCount() == 0) && (caster.getServitors() == null)) || !caster.isPlayer())
		{
			return;
		}
		
		final WorldObject target = event.getTarget();
		if ((target == null) || !target.isCreature())
		{
			return;
		}
		
		handleAttackEvent(caster, target);
	}
}
