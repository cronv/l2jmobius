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
package org.l2jmobius.gameserver.entity.actor.request;

import static java.lang.System.currentTimeMillis;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.l2jmobius.gameserver.config.custom.CaptchaConfig;
import org.l2jmobius.gameserver.data.BotReportTable;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.mechanics.captcha.Captcha;

/**
 * Handles the submission of a {@code Captcha} verification from a player.<br>
 * It validates the provided answer against the expected value to prevent automated bot activity.
 */
public class CaptchaRequest extends AbstractRequest
{
	private static final byte MAX_ATTEMPTS = 3;
	
	private Captcha _captcha;
	private byte _count = 0;
	private final Instant _timeout;
	
	/**
	 * Creates a new {@code CaptchaRequest} for a specific player.<br>
	 * This constructor initializes the captcha data and sets the expiration time.<br>
	 * The timeout is based on the configuration settings.
	 * @param activeChar The {@code Player} who is performing the action.
	 * @param captcha The {@code Captcha} object to be validated.
	 */
	public CaptchaRequest(Player activeChar, Captcha captcha)
	{
		super(activeChar);
		_captcha = captcha;
		final long currentTime = currentTimeMillis();
		setTimestamp(currentTime);
		scheduleTimeout(Duration.ofMinutes(CaptchaConfig.VALIDATION_TIME).toMillis());
		_timeout = Instant.ofEpochMilli(currentTime).plus(CaptchaConfig.VALIDATION_TIME, ChronoUnit.MINUTES);
	}
	
	/**
	 * Checks if the request is currently being used by a specific object.<br>
	 * This method always returns {@code false}.
	 * @param objectId The unique identifier of the object to check.
	 * @return {@code false} regardless of the input.
	 */
	@Override
	public boolean isUsing(int objectId)
	{
		return false;
	}
	
	/**
	 * Calculates the time left before the request expires.<br>
	 * This value is based on the {@code _timeout} timestamp.<br>
	 * It returns the difference in seconds between now and the deadline.
	 * @return The number of seconds remaining until timeout.
	 */
	public int getRemainingTime()
	{
		return (int) (_timeout.minusMillis(currentTimeMillis()).getEpochSecond());
	}
	
	/**
	 * Updates the current {@code Captcha} for this request.<br>
	 * This method replaces the old {@code Captcha} with a new one.
	 * @param captcha The new {@code Captcha} object to assign.
	 */
	public void refresh(Captcha captcha)
	{
		_captcha = captcha;
	}
	
	/**
	 * Updates the current request with a new {@code Captcha}.<br>
	 * This method increments the internal attempt counter.
	 * @param captcha The new {@code Captcha} object to assign.
	 */
	public void newRequest(Captcha captcha)
	{
		_count++;
		_captcha = captcha;
	}
	
	/**
	 * Checks if the maximum number of allowed attempts has been reached.<br>
	 * This is used to determine if a user can no longer try the captcha.
	 * @return {@code true} if the limit is reached, {@code false} otherwise.
	 */
	public boolean isLimitReached()
	{
		return _count >= (MAX_ATTEMPTS - 1);
	}
	
	/**
	 * Retrieves the {@code Captcha} object associated with this request.<br>
	 * This method returns the current captcha instance used for verification.
	 * @return The {@code Captcha} object.
	 */
	public Captcha getCaptcha()
	{
		return _captcha;
	}
	
	/**
	 * This method is called when the {@code CaptchaRequest} expires.<br>
	 * It triggers a punishment for the player who failed to solve the captcha.<br>
	 * The system records this action in the {@code BotReportTable}.
	 */
	@Override
	public void onTimeout()
	{
		BotReportTable.getInstance().punishBotDueUnsolvedCaptcha(getPlayer());
	}
	
	/**
	 * Returns the maximum number of allowed attempts.<br>
	 * This value is defined as a constant in this class.
	 * @return The total number of attempts permitted.
	 */
	public int maxAttemps()
	{
		return MAX_ATTEMPTS;
	}
	
	/**
	 * Calculates the number of attempts left for the current captcha.<br>
	 * It subtracts the current count from {@code MAX_ATTEMPTS}.<br>
	 * The result will never be less than {@code 0}.
	 * @return The number of remaining attempts.
	 */
	public int remainingAttemps()
	{
		return Math.max(MAX_ATTEMPTS - _count, 0);
	}
}
