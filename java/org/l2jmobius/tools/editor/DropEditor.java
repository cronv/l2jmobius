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
package org.l2jmobius.tools.editor;

import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

import org.l2jmobius.commons.ui.DarkTheme;
import org.l2jmobius.commons.ui.LightTheme;
import org.l2jmobius.commons.ui.SplashScreen;
import org.l2jmobius.commons.ui.WindowSize;
import org.l2jmobius.commons.util.ConfigReader;
import org.l2jmobius.tools.editor.drop.ui.DropEditorFrame;

/**
 * Offline editor for L2JMobius NPC drop lists stored in the game datapack.<br>
 * The tool loads NPC and item XML from the current chronicle and writes only the selected NPC drop block.
 * @author BazookaRpm
 */
public final class DropEditor
{
	private static final Path START_DIR = Paths.get(System.getProperty("user.dir"));
	
	private DropEditor()
	{
	}
	
	private static Path resolveGamePath()
	{
		final Path sourceGamePath = START_DIR.resolve("dist" + File.separator + "game");
		if (Files.isDirectory(sourceGamePath))
		{
			return sourceGamePath;
		}
		
		return START_DIR;
	}
	
	private static Path resolveImagesPath()
	{
		final Path sourceImagesPath = START_DIR.resolve("dist" + File.separator + "images");
		if (Files.isDirectory(sourceImagesPath))
		{
			return sourceImagesPath;
		}
		
		return START_DIR.resolve(".." + File.separator + "images").normalize();
	}
	
	private static void applyIcons(DropEditorFrame frame, Path imagesPath)
	{
		final List<Image> icons = new ArrayList<>();
		icons.add(new ImageIcon(imagesPath.resolve("l2jmobius_16x16.png").toString()).getImage());
		icons.add(new ImageIcon(imagesPath.resolve("l2jmobius_32x32.png").toString()).getImage());
		icons.add(new ImageIcon(imagesPath.resolve("l2jmobius_64x64.png").toString()).getImage());
		icons.add(new ImageIcon(imagesPath.resolve("l2jmobius_128x128.png").toString()).getImage());
		frame.setIconImages(icons);
	}
	
	/**
	 * Starts the Drop Editor using the same interface configuration and images as the other L2JMobius tools.
	 * @param args Command-line arguments are currently ignored.
	 */
	public static void main(String[] args)
	{
		final Path gamePath = resolveGamePath();
		final ConfigReader interfaceConfig = new ConfigReader(gamePath.resolve("config" + File.separator + "Interface.ini").toString());
		if (!interfaceConfig.getBoolean("EnableGUI", true) || GraphicsEnvironment.isHeadless())
		{
			System.out.println("Drop Editor requires the L2JMobius GUI. Enable EnableGUI in config/Interface.ini and run it in a graphical environment.");
			return;
		}
		
		// Disable hardware acceleration to match the other L2JMobius desktop tools.
		System.setProperty("sun.java2d.opengl", "false");
		System.setProperty("sun.java2d.d3d", "false");
		System.setProperty("sun.java2d.noddraw", "true");
		
		if (interfaceConfig.getBoolean("DarkTheme", true))
		{
			DarkTheme.activate();
		}
		else
		{
			LightTheme.activate();
		}
		
		final Path imagesPath = resolveImagesPath();
		SwingUtilities.invokeLater(() ->
		{
			final DropEditorFrame frame = new DropEditorFrame(gamePath);
			applyIcons(frame, imagesPath);
			WindowSize.applyDefault(frame);
			frame.setLocationRelativeTo(null);
			new SplashScreen(imagesPath.resolve("splash.png").toString(), 3000, frame);
		});
	}
}
