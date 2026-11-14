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
package org.l2jmobius.gameserver.cache;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.l2jmobius.gameserver.config.GeneralConfig;
import org.l2jmobius.gameserver.config.ServerConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.enums.ChatType;
import org.l2jmobius.gameserver.network.serverpackets.CreatureSay;

/**
 * This class manages the caching of {@code .htm} files to improve performance.<br>
 * It stores file contents in memory to reduce frequent disk reads during gameplay.
 * @author Layane, Mobius
 */
public class HtmCache
{
	private static final Logger LOGGER = Logger.getLogger(HtmCache.class.getName());
	
	private static final Map<String, String> HTML_CACHE = GeneralConfig.HTM_CACHE ? new HashMap<>() : new ConcurrentHashMap<>();
	
	private int _loadedFiles;
	private long _bytesBuffLen;
	
	/**
	 * Initializes a new instance of the {@code HtmCache} class.<br>
	 * This constructor triggers an initial reload of all cached files.
	 */
	protected HtmCache()
	{
		reload();
	}
	
	/**
	 * Reloads the HTML cache from the default data pack root.<br>
	 * This method refreshes all loaded files in the {@code HtmCache}.<br>
	 * It uses the path defined in {@code ServerConfig.DATAPACK_ROOT}.
	 */
	public void reload()
	{
		reload(ServerConfig.DATAPACK_ROOT);
	}
	
	/**
	 * Reloads the HTML cache from a specific directory.<br>
	 * This method clears existing data if caching is disabled.<br>
	 * It parses all files within the provided folder to update the cache.
	 * @param file The {@code File} object representing the directory to load.
	 */
	public void reload(File file)
	{
		if (GeneralConfig.HTM_CACHE)
		{
			LOGGER.info("Html cache start...");
			parseDir(file);
			LOGGER.info("Cache[HTML]: " + String.format("%.3f", getMemoryUsage()) + " megabytes on " + _loadedFiles + " files loaded.");
		}
		else
		{
			HTML_CACHE.clear();
			_loadedFiles = 0;
			_bytesBuffLen = 0;
			LOGGER.info("Cache[HTML]: Running lazy cache.");
		}
	}
	
	/**
	 * Reloads the HTML cache from a specific directory.<br>
	 * This method parses all files within the provided {@code File}.<br>
	 * It updates the internal cache with new data from that location.
	 * @param file The {@code File} object representing the directory to reload.
	 */
	public void reloadPath(File file)
	{
		parseDir(file);
		LOGGER.info("Cache[HTML]: Reloaded specified path.");
	}
	
	/**
	 * Calculates the current memory usage of the cache.<br>
	 * The value is returned in megabytes.
	 * @return The amount of memory used as a {@code double}.
	 */
	public double getMemoryUsage()
	{
		return (float) _bytesBuffLen / 1048576;
	}
	
	/**
	 * Returns the total number of files currently loaded in the cache.<br>
	 * This count is updated whenever new files are added or removed.
	 * @return The number of loaded files as an {@code int}.
	 */
	public int getLoadedFiles()
	{
		return _loadedFiles;
	}
	
	/**
	 * Recursively scans a directory to load all HTML files.<br>
	 * It identifies subdirectories and calls itself to process them.<br>
	 * Files that are not directories are loaded using {@code loadFile}.
	 * @param dir The {@code File} object representing the directory to parse.
	 */
	private void parseDir(File dir)
	{
		final File[] files = dir.listFiles();
		if (files != null)
		{
			for (File file : files)
			{
				if (!file.isDirectory())
				{
					loadFile(file);
				}
				else
				{
					parseDir(file);
				}
			}
		}
	}
	
	/**
	 * Loads the content of an HTML file into the cache.<br>
	 * The method removes comments and whitespace from the file content.<br>
	 * It returns {@code null} if the file is invalid or not found.
	 * @param file The {@code File} object to be loaded.
	 * @return The processed content as a {@code String}, or {@code null} if loading fails.
	 */
	public String loadFile(File file)
	{
		if ((file == null) || !file.isFile())
		{
			return null;
		}
		
		final String lowerCaseName = file.getName().toLowerCase();
		if (!(lowerCaseName.endsWith(".htm") || lowerCaseName.endsWith(".html")))
		{
			return null;
		}
		
		String filePath = null;
		String content = null;
		try (FileInputStream fis = new FileInputStream(file);
			BufferedInputStream bis = new BufferedInputStream(fis))
		{
			final int bytes = bis.available();
			final byte[] raw = new byte[bytes];
			
			bis.read(raw);
			content = new String(raw, StandardCharsets.UTF_8);
			content = content.replaceAll("(?s)<!--.*?-->", ""); // Remove html comments.
			content = content.replaceAll("[\\t\\n]", ""); // Remove tabs and new lines.
			
			filePath = file.toURI().getPath().substring(ServerConfig.DATAPACK_ROOT.toURI().getPath().length());
			if (GeneralConfig.CHECK_HTML_ENCODING && !filePath.startsWith("data/lang") && !StandardCharsets.US_ASCII.newEncoder().canEncode(content))
			{
				LOGGER.warning("HTML encoding check: File " + filePath + " contains non ASCII content.");
			}
			
			final String oldContent = HTML_CACHE.put(filePath, content);
			if (oldContent == null)
			{
				_bytesBuffLen += bytes;
				_loadedFiles++;
			}
			else
			{
				_bytesBuffLen = (_bytesBuffLen - oldContent.length()) + bytes;
			}
		}
		catch (Exception e)
		{
			LOGGER.log(Level.WARNING, "Problem with htm file:", e);
		}
		
		return content;
	}
	
	/**
	 * Retrieves the HTML content for a specific path based on the player's prefix.<br>
	 * It checks both the localized and default paths if necessary.<br>
	 * If the player is a GM, it may send a debug message to the chat.
	 * @param player The {@code Player} object used to determine the HTML prefix.
	 * @param path The file path of the HTML content to retrieve.
	 * @return The HTML content as a {@code String}, or {@code null} if not found.
	 */
	public String getHtm(Player player, String path)
	{
		final String prefix = player != null ? player.getHtmlPrefix() : "";
		String newPath = prefix + path;
		String content = HTML_CACHE.get(newPath);
		if (!GeneralConfig.HTM_CACHE && (content == null))
		{
			content = loadFile(new File(ServerConfig.DATAPACK_ROOT, newPath));
			if (content == null)
			{
				content = loadFile(new File(ServerConfig.SCRIPT_ROOT, newPath));
			}
		}
		
		// In case localisation does not exist try the default path.
		if ((content == null) && !prefix.contentEquals(""))
		{
			content = HTML_CACHE.get(path);
			newPath = path;
		}
		
		if ((player != null) && player.isGM() && GeneralConfig.GM_DEBUG_HTML_PATHS)
		{
			player.sendPacket(new CreatureSay(null, ChatType.GENERAL, "HTML", newPath.substring(5)));
		}
		
		return content;
	}
	
	/**
	 * Checks if a specific file path exists in the cache.<br>
	 * This method looks up the {@code path} key in the internal map.
	 * @param path The file path to search for.
	 * @return {@code true} if the path is found, {@code false} otherwise.
	 */
	public boolean contains(String path)
	{
		return HTML_CACHE.containsKey(path);
	}
	
	/**
	 * Provides the global instance of the {@code HtmCache}.<br>
	 * This method follows the singleton pattern.<br>
	 * Use this to access the cache from anywhere in the application.
	 * @return The single instance of {@code HtmCache}.
	 */
	public static HtmCache getInstance()
	{
		return SingletonHolder.INSTANCE;
	}
	
	private static class SingletonHolder
	{
		protected static final HtmCache INSTANCE = new HtmCache();
	}
}
