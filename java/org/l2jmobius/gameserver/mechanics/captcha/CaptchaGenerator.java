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
package org.l2jmobius.gameserver.mechanics.captcha;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.l2jmobius.commons.util.Rnd;
import org.l2jmobius.gameserver.util.DXT1ImageCompressor;

/**
 * This class is responsible for generating visual captcha images.<br>
 * It creates unique challenges to verify that a user is human.
 * @author JoeAlisson
 */
public class CaptchaGenerator
{
	private static final Map<Integer, Captcha> CAPTCHAS = new ConcurrentHashMap<>();
	private static final DXT1ImageCompressor COMPRESSOR = new DXT1ImageCompressor();
	
	/**
	 * Private constructor for the {@code CaptchaGenerator} class.<br>
	 * This prevents other classes from creating new instances of this generator.<br>
	 * Use {@code getInstance} to get the singleton instance.
	 */
	private CaptchaGenerator()
	{
	}
	
	/**
	 * Generates and returns a new {@code Captcha} object.<br>
	 * This method retrieves a unique ID from the internal cache.<br>
	 * It ensures that each {@code Captcha} is generated only once.
	 * @return A new {@code Captcha} instance.
	 */
	public Captcha next()
	{
		final int id = Rnd.get(CAPTCHAS.size() + 5);
		return CAPTCHAS.computeIfAbsent(id, this::generateCaptcha);
	}
	
	/**
	 * Generates the next {@code Captcha} object.<br>
	 * It ensures the new ID is different from the provided {@code previousId}.<br>
	 * The method retrieves an existing captcha or creates a new one if it does not exist.
	 * @param previousId The ID of the last generated captcha to avoid duplicates.
	 * @return The next available {@code Captcha} object.
	 */
	public Captcha next(int previousId)
	{
		int id = Rnd.get(CAPTCHAS.size() + 5);
		if (id == previousId)
		{
			id++;
		}
		
		return CAPTCHAS.computeIfAbsent(id, this::generateCaptcha);
	}
	
	/**
	 * Generates a random numeric code for the captcha.<br>
	 * The value is selected from the range {@code 111111} to {@code 999999}.
	 * @return The generated integer code.
	 */
	private int generateCaptchaCode()
	{
		return Rnd.get(111111, 999999);
	}
	
	/**
	 * Creates a new {@code Captcha} object for a specific ID.<br>
	 * This method generates an image with a random code and noise.<br>
	 * It uses the internal {@code COMPRESSOR} to process the final image.
	 * @param id The unique identifier for the captcha.
	 * @return A new {@code Captcha} instance containing the generated data.
	 */
	private Captcha generateCaptcha(int id)
	{
		final int height = 32;
		final int width = 128;
		
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR_PRE);
		Graphics2D graphics = createGraphics(height, width, image);
		graphics.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 22));
		
		final int code = generateCaptchaCode();
		
		writeCode(code, graphics);
		addNoise(graphics);
		graphics.dispose();
		return new Captcha(id, code, COMPRESSOR.compress(image));
	}
	
	/**
	 * Creates a {@code Graphics2D} object for drawing on an image.<br>
	 * This method initializes the canvas with a black background.
	 * @param height The height of the image in pixels.
	 * @param width The width of the image in pixels.
	 * @param image The {@code BufferedImage} to draw upon.
	 * @return A configured {@code Graphics2D} object.
	 */
	private Graphics2D createGraphics(int height, int width, BufferedImage image)
	{
		final Graphics2D graphics = image.createGraphics();
		graphics.setColor(Color.BLACK);
		graphics.fillRect(0, 0, width, height);
		return graphics;
	}
	
	/**
	 * This method draws the captcha code onto a {@code Graphics2D} object.<br>
	 * It converts the integer {@code code} into a string of characters.<br>
	 * Each character is drawn at a random vertical position with spacing.
	 * @param code The numeric value to be displayed as text.
	 * @param graphics The {@code Graphics2D} context used for drawing.
	 */
	private void writeCode(int code, Graphics2D graphics)
	{
		final String text = String.valueOf(code);
		final FontMetrics metrics = graphics.getFontMetrics();
		
		final int textStart = 10;
		for (int i = 0; i < text.length(); i++)
		{
			final char character = text.charAt(i);
			final int charWidth = metrics.charWidth(character) + 5;
			graphics.setColor(getColor());
			graphics.drawString(String.valueOf(character), textStart + (i * charWidth), Rnd.get(24, 32));
		}
	}
	
	/**
	 * Adds random visual noise to the captcha image.<br>
	 * This method draws small white circles and lines.<br>
	 * It helps prevent automated systems from reading the text easily.
	 * @param graphics The {@code Graphics2D} object used for drawing.
	 */
	private void addNoise(Graphics2D graphics)
	{
		graphics.setColor(Color.WHITE);
		for (int i = 0; i < 20; i++)
		{
			graphics.fillOval(Rnd.get(10, 122), Rnd.get(6, 20), 4, 4);
		}
		
		for (int i = 0; i < 6; i++)
		{
			graphics.drawLine(Rnd.get(30, 90), Rnd.get(6, 28), Rnd.get(80, 120), Rnd.get(10, 26));
		}
	}
	
	/**
	 * Selects a random color for the captcha text.<br>
	 * It picks from a set of predefined {@code Color} values.
	 * @return the randomly selected {@code Color} object.
	 */
	private Color getColor()
	{
		switch (Rnd.get(5))
		{
			case 1:
			{
				return Color.WHITE;
			}
			case 2:
			{
				return Color.RED;
			}
			case 3:
			{
				return Color.YELLOW;
			}
			case 4:
			{
				return Color.CYAN;
			}
			default:
			{
				return Color.GREEN;
			}
		}
	}
	
	/**
	 * Gets the global instance of the {@code CaptchaGenerator}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access captcha generation features throughout the application.
	 * @return The single shared instance of {@code CaptchaGenerator}.
	 */
	public static CaptchaGenerator getInstance()
	{
		return Singleton.INSTANCE;
	}
	
	private static class Singleton
	{
		private static final CaptchaGenerator INSTANCE = new CaptchaGenerator();
	}
}
