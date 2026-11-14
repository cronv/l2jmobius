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
package ai.areas.Giran;

import java.time.MonthDay;

import org.l2jmobius.gameserver.config.FeatureConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.zone.ZoneType;
import org.l2jmobius.gameserver.managers.ZoneManager;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.ListenerRegisterType;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterEvent;
import org.l2jmobius.gameserver.mechanics.events.annotations.RegisterType;
import org.l2jmobius.gameserver.mechanics.events.holders.OnDayNightChange;
import org.l2jmobius.gameserver.mechanics.script.Script;
import org.l2jmobius.gameserver.network.serverpackets.ExChangeClientEffectInfo;
import org.l2jmobius.gameserver.taskmanagers.GameTimeTaskManager;

/**
 * Applies a client-side visual effect to players located within the Giran zone.<br>
 * It determines the active effect based on a priority system involving calendar overrides, seasons, and manual day/night configurations.
 * @author Mobius, CostyKiller
 */
public class GiranZoneEffect extends Script
{
	private static final ZoneType GIRAN_ZONE = ZoneManager.getInstance().getZoneById(11020);
	private static final ZoneType GIRAN_SQUARE_WATER_ZONE = ZoneManager.getInstance().getZoneByName("Giran Square Water Zone");
	
	private static boolean _isNight = GameTimeTaskManager.getInstance().isNight();
	
	/**
	 * Private constructor for the {@code GiranZoneEffect} class.<br>
	 * It initializes the script by registering the zone ID and updating water status.
	 */
	private GiranZoneEffect()
	{
		addEnterZoneId(GIRAN_ZONE.getId());
		changeWaterZoneStatus();
	}
	
	// -------------------------------------------------------------------------
	// Zone / event hooks
	// -------------------------------------------------------------------------
	
	/**
	 * Handles the logic when a {@code Creature} enters a specific {@code ZoneType}.<br>
	 * This method triggers the visual effect for the creature.
	 * @param creature The {@code Creature} that entered the zone.
	 * @param zone The {@code ZoneType} of the area entered.
	 */
	@Override
	public void onEnterZone(Creature creature, ZoneType zone)
	{
		sendEffect(creature);
	}
	
	/**
	 * Handles the logic when the game time transitions between day and night.<br>
	 * This method updates the internal state of the zone.<br>
	 * It sends the correct visual effects to all characters inside the {@code GIRAN_ZONE}.<br>
	 * It also updates the status of the water zones.
	 * @param event The {@code OnDayNightChange} event containing the new time data.
	 */
	@RegisterEvent(EventType.ON_DAY_NIGHT_CHANGE)
	@RegisterType(ListenerRegisterType.GLOBAL)
	public void onDayNightChange(OnDayNightChange event)
	{
		_isNight = event.isNight();
		for (Creature creature : GIRAN_ZONE.getCharactersInside())
		{
			sendEffect(creature);
		}
		changeWaterZoneStatus();
	}
	
	// -------------------------------------------------------------------------
	// Effect resolution
	// -------------------------------------------------------------------------
	
	/**
	 * Determines the correct visual effect based on current configuration.<br>
	 * It checks for calendar overrides, then seasons, and finally manual settings.<br>
	 * The result depends on whether it is currently day or night.
	 * @return the resolved {@code ExChangeClientEffectInfo} object.
	 */
	private ExChangeClientEffectInfo resolveEffect()
	{
		final MonthDay today = MonthDay.now();
		
		// 1 - Custom calendar override.
		if (FeatureConfig.GIRAN_CALENDAR_EFFECT_ENABLED)
		{
			for (FeatureConfig.GiranCalendarEntry entry : FeatureConfig.GIRAN_CALENDAR_ENTRIES)
			{
				if (isInRange(today, entry.getFrom(), entry.getTo()))
				{
					return _isNight ? entry.getNightEffect() : entry.getDayEffect();
				}
			}
		}
		
		// 2 - Predefined season.
		if (FeatureConfig.GIRAN_SEASON_EFFECT_ENABLED)
		{
			final FeatureConfig.GiranSeason season = getSeason(today);
			return _isNight ? season.getNightEffect() : season.getDayEffect();
		}
		
		// 3 - Manual day/night config.
		return _isNight ? FeatureConfig.GIRAN_NIGHT_EFFECT : FeatureConfig.GIRAN_DAY_EFFECT;
	}
	
	/**
	 * Determines the current season based on the provided date.<br>
	 * It checks the month value of {@code today}.<br>
	 * The result is returned as a {@code FeatureConfig.GiranSeason} enum.
	 * @param today The {@code MonthDay} object representing the current date.
	 * @return The corresponding {@code FeatureConfig.GiranSeason} for the month.
	 */
	private FeatureConfig.GiranSeason getSeason(MonthDay today)
	{
		final int month = today.getMonthValue();
		if ((month >= 3) && (month <= 5))
		{
			return FeatureConfig.GiranSeason.SPRING;
		}
		if ((month >= 6) && (month <= 8))
		{
			return FeatureConfig.GiranSeason.SUMMER;
		}
		if ((month >= 9) && (month <= 11))
		{
			return FeatureConfig.GiranSeason.AUTUMN;
		}
		return FeatureConfig.GiranSeason.WINTER;
	}
	
	/**
	 * Checks if a specific date falls within a given range.<br>
	 * This method handles both continuous and wrapping year ranges.
	 * @param date The {@code MonthDay} to check.
	 * @param from The start of the range.
	 * @param to The end of the range.
	 * @return {@code true} if the date is within the range, {@code false} otherwise.
	 */
	private boolean isInRange(MonthDay date, MonthDay from, MonthDay to)
	{
		if (!from.isAfter(to))
		{
			// Normal range: e.g. Mar 01 - May 31.
			return !date.isBefore(from) && !date.isAfter(to);
		}
		
		// Wrapping range: e.g. Dec 20 - Jan 05.
		return !date.isBefore(from) || !date.isAfter(to);
	}
	
	// -------------------------------------------------------------------------
	// Packet / zone helpers
	// -------------------------------------------------------------------------
	
	/**
	 * Sends the visual effect packet to a {@code Creature}.<br>
	 * This method checks if the {@code creature} is a player.<br>
	 * If it is a player, it sends the result of {@code resolveEffect}.
	 * @param creature The {@code Creature} to receive the effect.
	 */
	private void sendEffect(Creature creature)
	{
		if (creature.isPlayer())
		{
			creature.sendPacket(resolveEffect());
		}
	}
	
	/**
	 * Updates the status of the Giran square water zone.<br>
	 * This method checks if the resolved effect is {@code ExChangeClientEffectInfo.GIRAN_WATER}.<br>
	 * It then enables or disables the {@code GIRAN_SQUARE_WATER_ZONE} based on that result.
	 */
	private void changeWaterZoneStatus()
	{
		GIRAN_SQUARE_WATER_ZONE.setEnabled(resolveEffect() == ExChangeClientEffectInfo.GIRAN_WATER);
	}
	
	/**
	 * This is the entry point for the {@code GiranZoneEffect} script.<br>
	 * It initializes a new instance of the class to start the logic.
	 * @param args Command line arguments passed to the application.
	 */
	public static void main(String[] args)
	{
		new GiranZoneEffect();
	}
}