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
package org.l2jmobius.gameserver.entity.zone.type;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

import org.l2jmobius.commons.threads.ThreadPool;
import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.ConquestConfig;
import org.l2jmobius.gameserver.data.xml.SkillData;
import org.l2jmobius.gameserver.entity.Location;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.appearance.PlayerAppearance;
import org.l2jmobius.gameserver.entity.actor.enums.creature.InstanceType;
import org.l2jmobius.gameserver.entity.actor.enums.player.TeleportWhereType;
import org.l2jmobius.gameserver.entity.zone.ZoneId;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.GlobalVariablesManager;
import org.l2jmobius.gameserver.mechanics.skill.Skill;
import org.l2jmobius.gameserver.mechanics.skill.enums.SkillFinishType;
import org.l2jmobius.gameserver.mechanics.variables.PlayerVariables;
import org.l2jmobius.gameserver.network.serverpackets.EtcStatusUpdate;

/**
 * Represents a zone that can be captured and contested by different factions.<br>
 * This class handles the logic for territory control and conquest mechanics.
 * @author Liamxroy, Mobius
 */
public class ConquestZone extends ZoneType
{
	private int _chance;
	private int _initialDelay;
	private int _reuse;
	private boolean _bypassConditions;
	private boolean _isShowDangerIcon;
	private boolean _removeEffectsOnExit;
	private Map<Integer, Integer> _skills;
	/** Skills resolved from the {@code _skills} map, rebuilt at tick time after a mutation set it to {@code null}. */
	private volatile Skill[] _skillCache;
	private volatile Future<?> _task;
	
	/**
	 * Creates a new {@code ConquestZone} instance.<br>
	 * This constructor initializes the zone with default values.<br>
	 * It sets the target type to {@code InstanceType.Playable}.
	 * @param id The unique identifier for this zone.
	 */
	public ConquestZone(int id)
	{
		super(id);
		_chance = 100;
		_initialDelay = 0;
		_reuse = 30000;
		setTargetType(InstanceType.Playable); // default only playable
		_bypassConditions = false;
		_isShowDangerIcon = false;
		_removeEffectsOnExit = true;
	}
	
	/**
	 * Sets a specific configuration parameter for the {@code ConquestZone}.<br>
	 * This method updates internal variables based on the provided name.<br>
	 * If the name is not recognized, it calls the superclass method.
	 * @param name The name of the property to set.
	 * @param value The string value associated with the property.
	 */
	@Override
	public void setParameter(String name, String value)
	{
		switch (name)
		{
			case "chance":
			{
				_chance = Integer.parseInt(value);
				break;
			}
			case "initialDelay":
			{
				_initialDelay = Integer.parseInt(value);
				break;
			}
			case "reuse":
			{
				_reuse = Integer.parseInt(value);
				break;
			}
			case "bypassSkillConditions":
			{
				_bypassConditions = Boolean.parseBoolean(value);
				break;
			}
			case "maxDynamicSkillCount":
			{
				_skills = new ConcurrentHashMap<>(Integer.parseInt(value));
				break;
			}
			case "showDangerIcon":
			{
				_isShowDangerIcon = Boolean.parseBoolean(value);
				break;
			}
			case "skillIdLvl":
			{
				final String[] propertySplit = value.split(";");
				if (_skills == null)
				{
					_skills = new ConcurrentHashMap<>(propertySplit.length);
				}
				
				for (String skill : propertySplit)
				{
					final String[] skillSplit = skill.split("-");
					if (skillSplit.length != 2)
					{
						LOGGER.warning(getClass().getSimpleName() + ": invalid config property -> skillIdLvl \"" + skill + "\"");
						continue;
					}
					
					try
					{
						_skills.put(Integer.parseInt(skillSplit[0]), Integer.parseInt(skillSplit[1]));
					}
					catch (NumberFormatException nfe)
					{
						if (!skill.isEmpty())
						{
							LOGGER.warning(getClass().getSimpleName() + ": invalid config property -> skillIdLvl \"" + skill + "\"");
						}
					}
				}
				
				_skillCache = null;
				break;
			}
			case "removeEffectsOnExit":
			{
				_removeEffectsOnExit = Boolean.parseBoolean(value);
				break;
			}
			default:
			{
				super.setParameter(name, value);
				break;
			}
		}
	}
	
	/**
	 * Handles logic when a {@code Creature} enters the conquest zone.<br>
	 * It updates player appearance and checks for required conquest names.<br>
	 * If conditions are not met, it teleports the player to a town.
	 * @param creature The {@code Creature} entering the zone.
	 */
	@Override
	protected void onEnter(Creature creature)
	{
		if (_skills != null)
		{
			Future<?> task = _task;
			if (task == null)
			{
				synchronized (this)
				{
					task = _task;
					if (task == null)
					{
						_task = task = ThreadPool.scheduleAtFixedRate(new ApplySkill(), _initialDelay, _reuse);
					}
				}
			}
		}
		
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			player.setInsideZone(ZoneId.CONQUEST, true);
			
			// Prevent db reading conquest name error.
			final String conquestName = player.getConquestName();
			if (conquestName == null)
			{
				player.teleToLocation(TeleportWhereType.TOWN);
				onExit(player);
				player.sendMessage("You must choose a conquest name before going there!");
				return;
			}
			
			// Change player information.
			final PlayerAppearance app = player.getAppearance();
			app.setVisibleName(conquestName);
			app.setVisibleTitle("");
			app.setVisibleClanData(0, 0, 0, 0, 0);
			
			if (ConquestConfig.CONQUEST_PVP_ZONE)
			{
				player.setInsideZone(ZoneId.PVP, true);
			}
			
			if (_isShowDangerIcon)
			{
				player.setInsideZone(ZoneId.DANGER_AREA, true);
				player.sendPacket(new EtcStatusUpdate(player));
			}
			
			// Teleport player to Town if still in zone and not conquest period.
			if (!GlobalVariablesManager.getInstance().hasVariable("CONQUEST_AVAILABLE") || (GlobalVariablesManager.getInstance().hasVariable("CONQUEST_AVAILABLE") && !GlobalVariablesManager.getInstance().getBoolean("CONQUEST_AVAILABLE", false)))
			{
				if (!player.isGM())
				{
					final PlayerVariables vars = player.getVariables();
					Location location = new Location(147458, 26903, -2206); // Aden center if no location stored.
					if (vars.contains(PlayerVariables.CONQUEST_ORIGIN))
					{
						final int[] loc = vars.getIntArray(PlayerVariables.CONQUEST_ORIGIN, ";");
						if ((loc != null) && (loc.length == 3))
						{
							location = new Location(loc[0], loc[1], loc[2]);
						}
						
						player.teleToLocation(location);
						vars.remove(PlayerVariables.CONQUEST_ORIGIN);
					}
					else
					{
						player.teleToLocation(location);
					}
					
					onExit(player);
				}
			}
		}
	}
	
	/**
	 * Handles the logic when a {@code Creature} leaves this zone.<br>
	 * It restores player appearance and status flags if the creature is a {@code Player}.<br>
	 * It also removes specific skill effects based on the zone configuration.<br>
	 * Finally, it cancels any active tasks if no characters remain in the zone.
	 * @param creature The {@code Creature} that is exiting the zone.
	 */
	@Override
	protected void onExit(Creature creature)
	{
		if (creature.isPlayer())
		{
			final Player player = creature.asPlayer();
			player.setInsideZone(ZoneId.CONQUEST, false);
			
			// Restore player information.
			final PlayerAppearance app = player.getAppearance();
			app.setVisibleName(null);
			app.setVisibleTitle(null);
			app.setVisibleClanData(-1, -1, -1, -1, -1);
			
			if (ConquestConfig.CONQUEST_PVP_ZONE)
			{
				player.setInsideZone(ZoneId.PVP, false);
			}
			
			if (_isShowDangerIcon)
			{
				player.setInsideZone(ZoneId.DANGER_AREA, false);
				if (!player.isInsideZone(ZoneId.DANGER_AREA))
				{
					player.sendPacket(new EtcStatusUpdate(player));
				}
			}
			
			if (_removeEffectsOnExit && (_skills != null))
			{
				Skill[] skills = _skillCache;
				if (skills == null)
				{
					skills = rebuildSkillCache();
				}
				
				for (int i = 0; i < skills.length; i++)
				{
					final int skillId = skills[i].getId();
					if (player.isAffectedBySkill(skillId))
					{
						player.stopSkillEffects(SkillFinishType.REMOVED, skillId);
					}
				}
			}
		}
		
		if (getCharactersInside().isEmpty())
		{
			stopTask();
		}
	}
	
	/**
	 * Handles the logic when a {@code Player} logs out from inside this zone.<br>
	 * It resets the player's zone status and appearance data.<br>
	 * It also removes specific skill effects if configured.<br>
	 * Finally, it cancels any active tasks if no players remain in the zone.
	 * @param player The {@code Player} who is logging out.
	 */
	@Override
	public void onPlayerLogoutInside(Player player)
	{
		if (player != null)
		{
			player.setInsideZone(ZoneId.CONQUEST, false);
			
			// Restore player information.
			final PlayerAppearance appearance = player.getAppearance();
			appearance.setVisibleName(null);
			appearance.setVisibleTitle(null);
			appearance.setVisibleClanData(-1, -1, -1, -1, -1);
			
			if (ConquestConfig.CONQUEST_PVP_ZONE)
			{
				player.setInsideZone(ZoneId.PVP, false);
			}
			
			if (_isShowDangerIcon)
			{
				player.setInsideZone(ZoneId.DANGER_AREA, false);
				if (!player.isInsideZone(ZoneId.DANGER_AREA))
				{
					player.sendPacket(new EtcStatusUpdate(player));
				}
			}
			
			if (_removeEffectsOnExit && (_skills != null))
			{
				Skill[] skills = _skillCache;
				if (skills == null)
				{
					skills = rebuildSkillCache();
				}
				
				for (int i = 0; i < skills.length; i++)
				{
					final int skillId = skills[i].getId();
					if (player.isAffectedBySkill(skillId))
					{
						player.stopSkillEffects(SkillFinishType.REMOVED, skillId);
					}
				}
			}
		}
		
		if (getCharactersInside().isEmpty())
		{
			stopTask();
		}
	}
	
	/**
	 * Retrieves the chance value for this zone.<br>
	 * This value determines how often an effect occurs.
	 * @return The current {@code int} chance value.
	 */
	public int getChance()
	{
		return _chance;
	}
	
	/**
	 * Adds a specific skill to the {@code ConquestZone}.<br>
	 * If the provided level is less than {@code 1}, the skill is removed.<br>
	 * This method ensures the internal skills map is initialized before adding data.
	 * @param skillId The unique identifier for the skill.
	 * @param skillLevel The level of the skill to add.
	 */
	public void addSkill(int skillId, int skillLevel)
	{
		if (skillLevel < 1) // remove skill
		{
			removeSkill(skillId);
			return;
		}
		
		if (_skills == null)
		{
			synchronized (this)
			{
				if (_skills == null)
				{
					_skills = new ConcurrentHashMap<>(3);
				}
			}
		}
		
		_skills.put(skillId, skillLevel);
		_skillCache = null;
	}
	
	/**
	 * Removes a specific skill from the {@code ConquestZone}.<br>
	 * This method checks if the internal skills map is not {@code null} before removing.
	 * @param skillId The unique identifier of the skill to remove.
	 */
	public void removeSkill(int skillId)
	{
		if (_skills != null)
		{
			_skills.remove(skillId);
			_skillCache = null;
		}
	}
	
	/**
	 * Removes all skills from the {@code ConquestZone}.<br>
	 * This method clears the internal {@code _skills} map.<br>
	 * It checks if the map is not {@code null} before clearing.
	 */
	public void clearSkills()
	{
		if (_skills != null)
		{
			_skills.clear();
			_skillCache = null;
		}
	}
	
	/**
	 * Retrieves the skill level for a specific skill ID.<br>
	 * It checks if the skill exists in the current zone.<br>
	 * If the skill is not found, it returns {@code 0}.
	 * @param skillId The unique identifier of the skill.
	 * @return The integer level of the skill or {@code 0} if it does not exist.
	 */
	public int getSkillLevel(int skillId)
	{
		if (_skills == null)
		{
			return 0;
		}
		
		final Integer level = _skills.get(skillId);
		return level != null ? level.intValue() : 0;
	}
	
	/**
	 * Rebuilds the resolved skill array from the skill level map.<br>
	 * Runs at tick time when a mutation has invalidated the cache.
	 * @return The resolved skills, or an empty array when none resolve.
	 */
	private Skill[] rebuildSkillCache()
	{
		final List<Skill> resolved = new ArrayList<>(_skills.size());
		for (Integer id : _skills.keySet())
		{
			final Integer level = _skills.get(id);
			if (level == null)
			{
				continue;
			}
			
			final Skill skill = SkillData.getInstance().getSkill(id.intValue(), level.intValue());
			if (skill != null)
			{
				resolved.add(skill);
			}
		}
		
		final Skill[] cache = resolved.toArray(new Skill[resolved.size()]);
		_skillCache = cache;
		return cache;
	}
	
	/**
	 * Stops the skill application task when it is running.
	 */
	private void stopTask()
	{
		final Future<?> task = _task;
		if (task != null)
		{
			task.cancel(false);
			_task = null;
		}
	}
	
	private class ApplySkill implements Runnable
	{
		protected ApplySkill()
		{
			if (_skills == null)
			{
				throw new IllegalStateException("No skills defined.");
			}
		}
		
		@Override
		public void run()
		{
			final Collection<Creature> charactersInside = getCharactersInside();
			if (charactersInside.isEmpty())
			{
				stopTask();
				return;
			}
			
			if (!isEnabled())
			{
				return;
			}
			
			Skill[] skills = _skillCache;
			if (skills == null)
			{
				skills = rebuildSkillCache();
			}
			
			if (skills.length == 0)
			{
				return;
			}
			
			for (Creature character : charactersInside)
			{
				if ((character != null) && character.isPlayer() && !character.isDead() && (Rnd.get(100) < _chance))
				{
					for (int i = 0; i < skills.length; i++)
					{
						final Skill skill = skills[i];
						if (_bypassConditions || skill.checkCondition(character, character, false))
						{
							if (character.getAffectedSkillLevel(skill.getId()) < skill.getLevel())
							{
								skill.activateSkill(character, character);
							}
						}
					}
				}
			}
		}
	}
}
