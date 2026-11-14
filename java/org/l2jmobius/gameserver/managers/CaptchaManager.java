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
package org.l2jmobius.gameserver.managers;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.logging.Logger;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.custom.CaptchaConfig;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.entity.actor.instance.Monster;
import org.l2jmobius.gameserver.entity.actor.request.CaptchaRequest;
import org.l2jmobius.gameserver.mechanics.captcha.Captcha;
import org.l2jmobius.gameserver.mechanics.captcha.CaptchaGenerator;
import org.l2jmobius.gameserver.mechanics.events.Containers;
import org.l2jmobius.gameserver.mechanics.events.EventType;
import org.l2jmobius.gameserver.mechanics.events.holders.actor.creature.OnCreatureKilled;
import org.l2jmobius.gameserver.mechanics.events.listeners.ConsumerEventListener;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.serverpackets.captcha.ReceiveBotCaptchaImage;

/**
 * Manages the generation and validation of {@code Captcha} challenges for players.<br>
 * It handles requests to display images and verifies user responses to prevent automated bot activity.
 * @author Mobius
 */
public class CaptchaManager
{
	private static final Logger LOGGER = Logger.getLogger(CaptchaManager.class.getName());
	
	private static final Map<Integer, Integer> MONSTER_COUNTER = new ConcurrentHashMap<>();
	private static final Map<Integer, Long> LAST_KILL_TIME = new ConcurrentHashMap<>();
	
	private final Consumer<OnCreatureKilled> _onCreatureKilled = event -> updateCounter(event.getAttacker(), event.getTarget());
	
	/**
	 * Initializes the {@code CaptchaManager} if captchas are enabled.<br>
	 * This constructor loads images and registers necessary event listeners.
	 */
	protected CaptchaManager()
	{
		if (CaptchaConfig.ENABLE_CAPTCHA)
		{
			Containers.Players().addListener(new ConsumerEventListener(Containers.Players(), EventType.ON_CREATURE_KILLED, _onCreatureKilled, this));
			LOGGER.info(getClass().getSimpleName() + ": Enabled.");
		}
	}
	
	/**
	 * Updates the kill counter for a player after defeating a monster.<br>
	 * This method checks if the player should be prompted with a captcha based on their kill count.<br>
	 * It ignores kills made during auto-play or by players already in validation.
	 * @param player The {@code Creature} instance of the player who performed the action.
	 * @param monster The {@code Creature} instance of the monster that was killed.
	 */
	public void updateCounter(Creature player, Creature monster)
	{
		if (!CaptchaConfig.ENABLE_CAPTCHA)
		{
			return;
		}
		
		if (!(player instanceof Player) || !(monster instanceof Monster))
		{
			return;
		}
		
		// Check if auto-play is enabled and player is auto-playing.
		final Player killer = player.asPlayer();
		if (GeneralConfig.ENABLE_AUTO_PLAY && killer.isAutoPlaying())
		{
			return; // Don't count kills when auto-play is enabled.
		}
		
		final int objectId = killer.getObjectId();
		if (CaptchaConfig.KILL_COUNTER_RESET)
		{
			final long currentTime = System.currentTimeMillis();
			final long previousKillTime = LAST_KILL_TIME.getOrDefault(objectId, currentTime);
			if ((currentTime - previousKillTime) > CaptchaConfig.KILL_COUNTER_RESET_TIME)
			{
				MONSTER_COUNTER.put(objectId, 0);
			}
			
			LAST_KILL_TIME.put(objectId, currentTime);
		}
		
		int count = 1;
		if (MONSTER_COUNTER.get(objectId) != null)
		{
			count = MONSTER_COUNTER.get(objectId) + 1;
		}
		
		final int next = Rnd.get(CaptchaConfig.KILL_COUNTER_RANDOMIZATION);
		if ((CaptchaConfig.KILL_COUNTER + next) < count)
		{
			MONSTER_COUNTER.remove(objectId);
			
			final Captcha captcha = CaptchaGenerator.getInstance().next();
			if (!killer.hasRequest(CaptchaRequest.class))
			{
				final CaptchaRequest request = new CaptchaRequest(killer, captcha);
				killer.addRequest(request);
				killer.sendPacket(new ReceiveBotCaptchaImage(captcha, request.getRemainingTime()));
				killer.sendPacket(SystemMessageId.PLEASE_ENTER_THE_AUTHENTICATION_CODE_IN_TIME_TO_CONTINUE_PLAYING);
			}
		}
		else
		{
			MONSTER_COUNTER.put(objectId, count);
		}
	}
	
	/**
	 * Retrieves the singleton instance of {@code CaptchaManager}.<br>
	 * Use this method to access the global captcha manager.
	 * @return The active {@code CaptchaManager} instance.
	 */
	public static CaptchaManager getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final CaptchaManager INSTANCE = new CaptchaManager();
	}
}
