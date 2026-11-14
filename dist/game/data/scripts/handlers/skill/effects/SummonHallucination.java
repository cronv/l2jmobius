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
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Doppelganger;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * Handles the logic for summoning a hallucination effect.<br>
 * This class manages the creation of temporary entities based on specific skill triggers.
 * @author Sdw, Enryu
 */
public class SummonHallucination extends AbstractEffect
{
	private final int _despawnDelay;
	private final int _npcId;
	private final int _npcCount;
	
	/**
	 * Creates a new hallucination effect.<br>
	 * This constructor initializes the effect using values from a {@code StatSet}.<br>
	 * It sets the NPC ID, count, and despawn delay.
	 * @param params The {@code StatSet} containing the configuration data.
	 */
	public SummonHallucination(StatSet params)
	{
		_despawnDelay = params.getInt("despawnDelay", 20000);
		_npcId = params.getInt("npcId", 0);
		_npcCount = params.getInt("npcCount", 1);
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value from {@code EffectType}.
	 * @return The {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON_NPC;
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
	 * Executes the instant effect of summoning hallucinations.<br>
	 * This method spawns multiple {@code Doppelganger} entities around a target.<br>
	 * It validates that the target is alive and the player is not mounted.
	 * @param effector The {@code Creature} who casts the skill.
	 * @param effected The {@code Creature} who receives the effect.
	 * @param skill The {@code Skill} being used to trigger the effect.
	 * @param item The {@code Item} associated with the skill execution.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (effected.isAlikeDead())
		{
			return;
		}
		
		if ((_npcId <= 0) || (_npcCount <= 0))
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Invalid NPC ID or count skill ID: " + skill.getId());
			return;
		}
		
		final Player player = effector.asPlayer();
		if (player.isMounted())
		{
			return;
		}
		
		final NpcTemplate npcTemplate = NpcData.getInstance().getTemplate(_npcId);
		if (npcTemplate == null)
		{
			LOGGER.warning(SummonNpc.class.getSimpleName() + ": Spawn of the nonexisting NPC ID: " + _npcId + ", skill ID:" + skill.getId());
			return;
		}
		
		int x = effected.getX();
		int y = effected.getY();
		final int z = effected.getZ();
		
		for (int i = 0; i < _npcCount; i++)
		{
			x += (Rnd.nextBoolean() ? Rnd.get(0, 20) : Rnd.get(-20, 0));
			y += (Rnd.nextBoolean() ? Rnd.get(0, 20) : Rnd.get(-20, 0));
			
			final Doppelganger clone = new Doppelganger(npcTemplate, player);
			clone.fullRestore();
			clone.setSummoner(player);
			player.addSummonedNpc(clone);
			clone.spawnMe(x, y, z);
			clone.scheduleDespawn(_despawnDelay);
			clone.startAttackTask(effected);
		}
	}
}
