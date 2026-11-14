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

import org.l2jmobius.gameserver.config.PlayerConfig;
import org.l2jmobius.gameserver.data.xml.ExperienceData;
import org.l2jmobius.gameserver.data.xml.NpcData;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.enums.creature.Race;
import org.l2jmobius.gameserver.entity.actor.instance.Servitor;
import org.l2jmobius.gameserver.entity.actor.templates.NpcTemplate;
import org.l2jmobius.gameserver.entity.item.holders.ItemHolder;
import org.l2jmobius.gameserver.entity.item.instance.Item;
import org.l2jmobius.gameserver.mechanics.effects.AbstractEffect;
import org.l2jmobius.gameserver.mechanics.effects.EffectType;
import org.l2jmobius.gameserver.mechanics.skill.BuffInfo;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.util.StatSet;

/**
 * This class handles the logic for summon effects in the game.<br>
 * It manages the creation and behavior of summoned entities like servitors.<br>
 * It extends {@code AbstractEffect} to provide specific summoning functionality.
 * @author UnAfraid
 */
public class Summon extends AbstractEffect
{
	private final int _npcId;
	private final float _expMultiplier;
	private final ItemHolder _consumeItem;
	private final int _lifeTime;
	private final int _consumeItemInterval;
	
	/**
	 * Creates a new {@code Summon} effect.<br>
	 * This constructor initializes the summon properties from a {@code StatSet}.<br>
	 * It validates that the provided parameters are not empty.
	 * @param params The {@code StatSet} containing all required configuration values.
	 */
	public Summon(StatSet params)
	{
		if (params.isEmpty())
		{
			throw new IllegalArgumentException("Summon effect without parameters!");
		}
		
		_npcId = params.getInt("npcId");
		_expMultiplier = params.getFloat("expMultiplier", 1);
		_consumeItem = new ItemHolder(params.getInt("consumeItemId", 0), params.getInt("consumeItemCount", 1));
		_consumeItemInterval = params.getInt("consumeItemInterval", 0);
		_lifeTime = params.getInt("lifeTime", 3600) > 0 ? params.getInt("lifeTime", 3600) * 1000 : -1;
	}
	
	/**
	 * Retrieves the type of effect associated with this instance.<br>
	 * This method returns a constant value representing a summon.
	 * @return the {@code EffectType} for this effect.
	 */
	@Override
	public EffectType getEffectType()
	{
		return EffectType.SUMMON;
	}
	
	/**
	 * Checks if the effect occurs immediately.<br>
	 * This method always returns {@code true}.
	 * @return {@code true} if the effect is instant.
	 */
	@Override
	public boolean isInstant()
	{
		return true;
	}
	
	/**
	 * Executes the instant summon logic for a player.<br>
	 * This method creates a new {@code Servitor} based on the configured NPC data.<br>
	 * It also handles clearing existing servitors and applying shared buffs from the master.
	 * @param effector The creature who performs the action.
	 * @param effected The creature receiving the effect.
	 * @param skill The {@code Skill} used to perform the summon.
	 * @param item The {@code Item} consumed during the process.
	 */
	@Override
	public void instant(Creature effector, Creature effected, Skill skill, Item item)
	{
		if (!effected.isPlayer())
		{
			return;
		}
		
		final Player player = effected.asPlayer();
		if (player.hasServitors())
		{
			player.getServitors().values().forEach(s -> s.unSummon(player));
		}
		
		final NpcTemplate template = NpcData.getInstance().getTemplate(_npcId);
		final Servitor summon = new Servitor(template, player);
		final boolean siegeWeapon = template.getRace() == Race.SIEGE_WEAPON;
		final double timeMultiplier = siegeWeapon ? 1 : PlayerConfig.SUMMON_TIME_MULTIPLIER;
		final int lifeTime = _lifeTime > 0 ? (int) (_lifeTime * timeMultiplier) : _lifeTime;
		final int consumeItemInterval = (int) ((_consumeItemInterval > 0 ? _consumeItemInterval : (siegeWeapon ? 60 : 240)) * 1000 * timeMultiplier);
		
		summon.setName(template.getName());
		summon.setTitle(effected.getName());
		summon.setReferenceSkill(skill.getId());
		summon.setExpMultiplier(_expMultiplier);
		summon.setLifeTime(lifeTime);
		summon.setItemConsume(_consumeItem);
		summon.setItemConsumeInterval(consumeItemInterval);
		
		final int maxPetLevel = ExperienceData.getInstance().getMaxPetLevel();
		if (summon.getLevel() >= maxPetLevel)
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(maxPetLevel - 1));
		}
		else
		{
			summon.getStat().setExp(ExperienceData.getInstance().getExpForLevel(summon.getLevel() % maxPetLevel));
		}
		
		// Summons must have their master buffs upon spawn.
		for (BuffInfo effect : player.getEffectList().getEffects())
		{
			final Skill sk = effect.getSkill();
			if (!sk.hasNegativeEffect() && !sk.isTransformation() && sk.isSharedWithSummon())
			{
				sk.applyEffects(player, summon, false, effect.getTime());
			}
		}
		
		summon.fullRestore();
		summon.setHeading(player.getHeading());
		player.addServitor(summon);
		summon.setShowSummonAnimation(true);
		summon.spawnMe();
		summon.setRunning();
	}
}
