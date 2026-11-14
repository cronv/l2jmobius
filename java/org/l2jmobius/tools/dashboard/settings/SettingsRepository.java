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
package org.l2jmobius.tools.dashboard.settings;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import org.l2jmobius.commons.util.ConfigReader;

/**
 * Loads the local Control Center configuration through {@link org.l2jmobius.commons.util.ConfigReader}, migrates legacy settings, and atomically saves Dashboard.ini. <br>
 * @author BazookaRpm
 */
public final class SettingsRepository
{
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final String[] ORDERED_KEYS =
	{
		ControlCenterSettings.MONITOR_HEALTH_CHECK_MS,
		ControlCenterSettings.MONITOR_TELEMETRY_MS,
		ControlCenterSettings.CONSOLE_MAX_LINES,
		ControlCenterSettings.TELEMETRY_HISTORY_SAMPLES,
		ControlCenterSettings.ALERT_CPU_PERCENT,
		ControlCenterSettings.ALERT_HEAP_PERCENT,
		ControlCenterSettings.ALERT_DISK_FREE_PERCENT,
		ControlCenterSettings.SERVICE_SHUTDOWN_TIMEOUT_SECONDS,
		ControlCenterSettings.SERVICE_AUTO_START_LOGIN,
		ControlCenterSettings.SERVICE_JAVA_PATH,
		ControlCenterSettings.MANAGEMENT_CONTROL_TOKEN,
		ControlCenterSettings.LOGIN_PORT,
		ControlCenterSettings.GAME_PORT,
		ControlCenterSettings.NAVIGATION_WIDTH,
		ControlCenterSettings.MAP_EXTRACTOR_CLIENT_PATH,
		ControlCenterSettings.MAP_EXTRACTOR_REFERENCE_GEODATA_PATH,
		ControlCenterSettings.MAP_EXTRACTOR_OUTPUT_PATH,
		ControlCenterSettings.MAP_EXTRACTOR_GENERATE_GEODATA,
		ControlCenterSettings.MAP_EXTRACTOR_GENERATE_DOORS,
		ControlCenterSettings.MAP_EXTRACTOR_GENERATE_WATER,
		ControlCenterSettings.MAP_EXTRACTOR_MAP_VARIANT,
		ControlCenterSettings.WINDOW_X,
		ControlCenterSettings.WINDOW_Y,
		ControlCenterSettings.WINDOW_WIDTH,
		ControlCenterSettings.WINDOW_HEIGHT,
		ControlCenterSettings.WINDOW_MAXIMIZED
	};
	
	private SettingsRepository()
	{
	}
	
	public static ControlCenterSettings open(Path distributionRoot) throws IOException
	{
		final Path root = distributionRoot.toAbsolutePath().normalize();
		final Path file = root.resolve("Dashboard.ini");
		final Properties values = defaults();
		if (Files.isRegularFile(file))
		{
			values.putAll(load(file));
		}
		
		boolean generatedToken = false;
		if (values.getProperty(ControlCenterSettings.MANAGEMENT_CONTROL_TOKEN, "").trim().isEmpty())
		{
			final byte[] tokenBytes = new byte[24];
			RANDOM.nextBytes(tokenBytes);
			values.setProperty(ControlCenterSettings.MANAGEMENT_CONTROL_TOKEN, HexFormat.of().formatHex(tokenBytes));
			generatedToken = true;
		}
		
		final ControlCenterSettings settings = new ControlCenterSettings(root, values);
		if (generatedToken)
		{
			save(settings);
		}
		
		return settings;
	}
	
	private static Properties load(Path file)
	{
		final ConfigReader config = new ConfigReader(file.toString());
		final Properties values = new Properties();
		for (String key : config.getStringPropertyNames())
		{
			final String value = config.getValue(key);
			if (value != null)
			{
				values.setProperty(key, value);
			}
		}
		
		return values;
	}
	
	public static void save(ControlCenterSettings settings) throws IOException
	{
		final Path file = settings.getSettingsFile();
		Files.createDirectories(file.getParent());
		final Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
		Files.writeString(temporary, buildDocument(settings.copyProperties()), StandardCharsets.UTF_8);
		try
		{
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
		}
		catch (AtomicMoveNotSupportedException e)
		{
			Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
		}
	}
	
	private static String buildDocument(Properties values)
	{
		final StringBuilder text = new StringBuilder(4096);
		appendHeader(text);
		
		appendSection(text, "MONITORING");
		appendSetting(text, values, ControlCenterSettings.MONITOR_HEALTH_CHECK_MS, "How often the Control Center checks Login Server and Game Server process/port health.", "Milliseconds. Valid range: 250-10000. Default: 1000.");
		appendSetting(text, values, ControlCenterSettings.MONITOR_TELEMETRY_MS, "How often JVM telemetry is collected while a visible page needs it.", "Milliseconds. Valid range: 500-60000. Default: 2000.");
		
		appendSection(text, "CONSOLE");
		appendSetting(text, values, ControlCenterSettings.CONSOLE_MAX_LINES, "Maximum console messages retained per managed service.", "Older messages are discarded from the central history. Valid range: 1000-100000. Default: 10000.");
		
		appendSection(text, "TELEMETRY HISTORY");
		appendSetting(text, values, ControlCenterSettings.TELEMETRY_HISTORY_SAMPLES, "Maximum telemetry samples retained for each history chart.", "At 2000 ms, 1800 samples represent about 60 minutes. Valid range: 60-20000. Default: 1800.");
		
		appendSection(text, "ALERTS");
		appendSetting(text, values, ControlCenterSettings.ALERT_CPU_PERCENT, "CPU usage percentage that triggers a warning.", "Valid range: 1-100. Default: 90.");
		appendSetting(text, values, ControlCenterSettings.ALERT_HEAP_PERCENT, "JVM heap usage percentage that triggers a warning.", "Valid range: 1-100. Default: 85.");
		appendSetting(text, values, ControlCenterSettings.ALERT_DISK_FREE_PERCENT, "Minimum free disk percentage before a warning is displayed.", "Valid range: 1-99. Default: 10.");
		
		appendSection(text, "SERVICE CONTROL");
		appendSetting(text, values, ControlCenterSettings.SERVICE_SHUTDOWN_TIMEOUT_SECONDS, "Maximum time to wait for a managed service to stop gracefully.", "Seconds. Valid range: 5-300. Default: 30.");
		appendSetting(text, values, ControlCenterSettings.SERVICE_AUTO_START_LOGIN, "Start Login Server automatically when Game Server requires it.", "Values: true / false. Default: true.");
		appendComment(text, "Optional full path to the Java executable used to launch managed services.");
		appendComment(text, "Leave empty to use JAVA_HOME or the Java runtime running the Control Center.");
		appendProperty(text, ControlCenterSettings.SERVICE_JAVA_PATH, values.getProperty(ControlCenterSettings.SERVICE_JAVA_PATH, ""));
		text.append('\n');
		
		appendSection(text, "LOCAL MANAGEMENT");
		appendSetting(text, values, ControlCenterSettings.MANAGEMENT_CONTROL_TOKEN, "Authentication token used by the local management MXBean.", "Generated automatically when empty. Keep this value private.");
		
		appendSection(text, "SERVICE PORTS");
		appendSetting(text, values, ControlCenterSettings.LOGIN_PORT, "Login Server client port used as the fallback when Server.ini cannot be read.", "Must match LoginserverPort. Default: 2106.");
		appendSetting(text, values, ControlCenterSettings.GAME_PORT, "Game Server client port used as the fallback when Server.ini cannot be read.", "Must match GameserverPort. Default: 7777.");
		
		appendSection(text, "INTERFACE");
		appendSetting(text, values, ControlCenterSettings.NAVIGATION_WIDTH, "Width of the left navigation panel in pixels.", "Valid range: 150-320. Default: 190.");
		
		appendSection(text, "MAP EXTRACTOR");
		appendSetting(text, values, ControlCenterSettings.MAP_EXTRACTOR_CLIENT_PATH, "Last selected Lineage II client root.", "Leave empty until a client is selected.");
		appendSetting(text, values, ControlCenterSettings.MAP_EXTRACTOR_REFERENCE_GEODATA_PATH, "Optional reference geodata directory used for comparison workflows.", "Leave empty when no reference dataset is used.");
		appendSetting(text, values, ControlCenterSettings.MAP_EXTRACTOR_OUTPUT_PATH, "Output directory for generated Map Extractor data.", "Leave empty to use game/data/geodata in this distribution.");
		appendSetting(text, values, ControlCenterSettings.MAP_EXTRACTOR_GENERATE_GEODATA, "Generate .l2j geodata files.", "Values: true / false. Default: true.");
		appendSetting(text, values, ControlCenterSettings.MAP_EXTRACTOR_GENERATE_DOORS, "Generate door XML when supported by the selected client data.", "Values: true / false. Default: false.");
		appendSetting(text, values, ControlCenterSettings.MAP_EXTRACTOR_GENERATE_WATER, "Generate water-zone XML when supported by the selected client data.", "Values: true / false. Default: false.");
		appendSetting(text, values, ControlCenterSettings.MAP_EXTRACTOR_MAP_VARIANT, "Preferred detected map variant suffix.", "Leave empty for the standard map set. The available variants come from the selected client.");
		
		appendSection(text, "WINDOW");
		appendSetting(text, values, ControlCenterSettings.WINDOW_X, "Saved horizontal window position.", "Use -1 for automatic placement.");
		appendSetting(text, values, ControlCenterSettings.WINDOW_Y, "Saved vertical window position.", "Use -1 for automatic placement.");
		appendSetting(text, values, ControlCenterSettings.WINDOW_WIDTH, "Saved Control Center width in pixels.", "Valid range: 980-5000. Default: 1440.");
		appendSetting(text, values, ControlCenterSettings.WINDOW_HEIGHT, "Saved Control Center height in pixels.", "Valid range: 680-4000. Default: 860.");
		appendSetting(text, values, ControlCenterSettings.WINDOW_MAXIMIZED, "Restore the Control Center maximized on the next launch.", "Values: true / false. Default: false.");
		
		final Set<String> additionalKeys = new TreeSet<>(values.stringPropertyNames());
		for (String key : ORDERED_KEYS)
		{
			additionalKeys.remove(key);
		}
		
		if (!additionalKeys.isEmpty())
		{
			appendSection(text, "ADDITIONAL SETTINGS");
			appendComment(text, "Unrecognized settings are preserved to avoid data loss during upgrades.");
			for (String key : additionalKeys)
			{
				appendProperty(text, key, values.getProperty(key, ""));
			}
		}
		
		return text.toString();
	}
	
	private static void appendHeader(StringBuilder text)
	{
		text.append("# Local Control Center configuration loaded with ConfigReader.\n");
		text.append("# Setting names are intentionally flat and descriptive.\n");
		text.append("# Service ports must match the corresponding Server.ini files.\n");
		text.append("# =============================================================================\n\n");
	}
	
	private static void appendSection(StringBuilder text, String name)
	{
		text.append("# -----------------------------------------------------------------------------\n");
		text.append("# ").append(name).append('\n');
		text.append("# -----------------------------------------------------------------------------\n\n");
	}
	
	private static void appendSetting(StringBuilder text, Properties values, String key, String... comments)
	{
		for (String comment : comments)
		{
			appendComment(text, comment);
		}
		
		appendProperty(text, key, values.getProperty(key, ""));
		text.append('\n');
	}
	
	private static void appendComment(StringBuilder text, String comment)
	{
		text.append("# ").append(comment).append('\n');
	}
	
	private static void appendProperty(StringBuilder text, String key, String value)
	{
		text.append(key).append(" = ").append(escapeValue(value)).append('\n');
	}
	
	private static String escapeValue(String value)
	{
		final StringBuilder escaped = new StringBuilder(value.length() + 16);
		for (int index = 0; index < value.length(); index++)
		{
			final char character = value.charAt(index);
			switch (character)
			{
				case '\\':
				{
					escaped.append("\\\\");
					break;
				}
				case '\t':
				{
					escaped.append("\\t");
					break;
				}
				case '\n':
				{
					escaped.append("\\n");
					break;
				}
				case '\r':
				{
					escaped.append("\\r");
					break;
				}
				case '\f':
				{
					escaped.append("\\f");
					break;
				}
				case '=':
				case ':':
				case '#':
				case '!':
				{
					escaped.append('\\').append(character);
					break;
				}
				case ' ':
				{
					if (index == 0)
					{
						escaped.append('\\');
					}
					escaped.append(character);
					break;
				}
				default:
				{
					escaped.append(character);
					break;
				}
			}
		}
		
		return escaped.toString();
	}
	
	private static Properties defaults()
	{
		final Properties values = new Properties();
		values.setProperty(ControlCenterSettings.MONITOR_HEALTH_CHECK_MS, "1000");
		values.setProperty(ControlCenterSettings.MONITOR_TELEMETRY_MS, "2000");
		values.setProperty(ControlCenterSettings.CONSOLE_MAX_LINES, "10000");
		values.setProperty(ControlCenterSettings.TELEMETRY_HISTORY_SAMPLES, "1800");
		values.setProperty(ControlCenterSettings.ALERT_CPU_PERCENT, "90");
		values.setProperty(ControlCenterSettings.ALERT_HEAP_PERCENT, "85");
		values.setProperty(ControlCenterSettings.ALERT_DISK_FREE_PERCENT, "10");
		values.setProperty(ControlCenterSettings.SERVICE_SHUTDOWN_TIMEOUT_SECONDS, "30");
		values.setProperty(ControlCenterSettings.SERVICE_AUTO_START_LOGIN, "true");
		values.setProperty(ControlCenterSettings.SERVICE_JAVA_PATH, "");
		values.setProperty(ControlCenterSettings.MANAGEMENT_CONTROL_TOKEN, "");
		values.setProperty(ControlCenterSettings.LOGIN_PORT, "2106");
		values.setProperty(ControlCenterSettings.GAME_PORT, "7777");
		values.setProperty(ControlCenterSettings.NAVIGATION_WIDTH, "190");
		values.setProperty(ControlCenterSettings.MAP_EXTRACTOR_CLIENT_PATH, "");
		values.setProperty(ControlCenterSettings.MAP_EXTRACTOR_REFERENCE_GEODATA_PATH, "");
		values.setProperty(ControlCenterSettings.MAP_EXTRACTOR_OUTPUT_PATH, "");
		values.setProperty(ControlCenterSettings.MAP_EXTRACTOR_GENERATE_GEODATA, "true");
		values.setProperty(ControlCenterSettings.MAP_EXTRACTOR_GENERATE_DOORS, "false");
		values.setProperty(ControlCenterSettings.MAP_EXTRACTOR_GENERATE_WATER, "false");
		values.setProperty(ControlCenterSettings.MAP_EXTRACTOR_MAP_VARIANT, "");
		values.setProperty(ControlCenterSettings.WINDOW_X, "-1");
		values.setProperty(ControlCenterSettings.WINDOW_Y, "-1");
		values.setProperty(ControlCenterSettings.WINDOW_WIDTH, "1440");
		values.setProperty(ControlCenterSettings.WINDOW_HEIGHT, "860");
		values.setProperty(ControlCenterSettings.WINDOW_MAXIMIZED, "false");
		return values;
	}
}
