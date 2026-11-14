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

import java.nio.file.Path;
import java.util.Locale;
import java.util.Properties;

import org.l2jmobius.tools.dashboard.runtime.ServiceRole;

/**
 * Provides typed access to the persisted Control Center configuration. <br>
 * Setting names intentionally use flat PascalCase keys so Dashboard.ini remains easy to read and edit manually.
 * @author BazookaRpm
 */
public final class ControlCenterSettings
{
	public static final String MONITOR_HEALTH_CHECK_MS = "MonitorHealthCheckMs";
	public static final String MONITOR_TELEMETRY_MS = "MonitorTelemetryMs";
	public static final String CONSOLE_MAX_LINES = "ConsoleMaxLines";
	public static final String TELEMETRY_HISTORY_SAMPLES = "TelemetryHistorySamples";
	public static final String ALERT_CPU_PERCENT = "AlertCpuPercent";
	public static final String ALERT_HEAP_PERCENT = "AlertHeapPercent";
	public static final String ALERT_DISK_FREE_PERCENT = "AlertDiskFreePercent";
	public static final String SERVICE_SHUTDOWN_TIMEOUT_SECONDS = "ServiceShutdownTimeoutSeconds";
	public static final String SERVICE_AUTO_START_LOGIN = "ServiceAutoStartLogin";
	public static final String SERVICE_JAVA_PATH = "ServiceJavaPath";
	public static final String MANAGEMENT_CONTROL_TOKEN = "ManagementControlToken";
	public static final String LOGIN_PORT = "LoginPort";
	public static final String GAME_PORT = "GamePort";
	public static final String NAVIGATION_WIDTH = "NavigationWidth";
	public static final String MAP_EXTRACTOR_CLIENT_PATH = "MapExtractorClientPath";
	public static final String MAP_EXTRACTOR_REFERENCE_GEODATA_PATH = "MapExtractorReferenceGeodataPath";
	public static final String MAP_EXTRACTOR_OUTPUT_PATH = "MapExtractorOutputPath";
	public static final String MAP_EXTRACTOR_GENERATE_GEODATA = "MapExtractorGenerateGeodata";
	public static final String MAP_EXTRACTOR_GENERATE_DOORS = "MapExtractorGenerateDoors";
	public static final String MAP_EXTRACTOR_GENERATE_WATER = "MapExtractorGenerateWater";
	public static final String MAP_EXTRACTOR_MAP_VARIANT = "MapExtractorMapVariant";
	public static final String WINDOW_X = "WindowX";
	public static final String WINDOW_Y = "WindowY";
	public static final String WINDOW_WIDTH = "WindowWidth";
	public static final String WINDOW_HEIGHT = "WindowHeight";
	public static final String WINDOW_MAXIMIZED = "WindowMaximized";
	
	private final Path _distributionRoot;
	private final Properties _values;
	
	ControlCenterSettings(Path distributionRoot, Properties values)
	{
		_distributionRoot = distributionRoot.toAbsolutePath().normalize();
		_values = values;
	}
	
	public Path getDistributionRoot()
	{
		return _distributionRoot;
	}
	
	public Path getSettingsFile()
	{
		return _distributionRoot.resolve("Dashboard.ini");
	}
	
	public int getHealthCheckIntervalMillis()
	{
		return intValue(MONITOR_HEALTH_CHECK_MS, 1000, 250, 10000);
	}
	
	public int getTelemetryIntervalMillis()
	{
		return intValue(MONITOR_TELEMETRY_MS, 2000, 500, 60000);
	}
	
	public int getConsoleLineLimit()
	{
		return intValue(CONSOLE_MAX_LINES, 10000, 1000, 100000);
	}
	
	public int getHistoryCapacity()
	{
		return intValue(TELEMETRY_HISTORY_SAMPLES, 1800, 60, 20000);
	}
	
	public int getCpuWarningPercent()
	{
		return intValue(ALERT_CPU_PERCENT, 90, 1, 100);
	}
	
	public int getHeapWarningPercent()
	{
		return intValue(ALERT_HEAP_PERCENT, 85, 1, 100);
	}
	
	public int getDiskFreeWarningPercent()
	{
		return intValue(ALERT_DISK_FREE_PERCENT, 10, 1, 99);
	}
	
	public int getShutdownTimeoutSeconds()
	{
		return intValue(SERVICE_SHUTDOWN_TIMEOUT_SECONDS, 30, 5, 300);
	}
	
	public boolean isLoginAutoStartEnabled()
	{
		return Boolean.parseBoolean(_values.getProperty(SERVICE_AUTO_START_LOGIN, "true"));
	}
	
	public String getJavaExecutableOverride()
	{
		return _values.getProperty(SERVICE_JAVA_PATH, "").trim();
	}
	
	public String getControlToken()
	{
		return _values.getProperty(MANAGEMENT_CONTROL_TOKEN, "");
	}
	
	public int getNavigationWidth()
	{
		return intValue(NAVIGATION_WIDTH, 190, 150, 320);
	}
	
	public int getWindowX()
	{
		return intValue(WINDOW_X, -1, -1, 10000);
	}
	
	public int getWindowY()
	{
		return intValue(WINDOW_Y, -1, -1, 10000);
	}
	
	public int getWindowWidth()
	{
		return intValue(WINDOW_WIDTH, 1440, 980, 5000);
	}
	
	public int getWindowHeight()
	{
		return intValue(WINDOW_HEIGHT, 860, 680, 4000);
	}
	
	public boolean isWindowMaximized()
	{
		return Boolean.parseBoolean(_values.getProperty(WINDOW_MAXIMIZED, "false"));
	}
	
	public String getMapExtractorClientPath()
	{
		return _values.getProperty(MAP_EXTRACTOR_CLIENT_PATH, "").trim();
	}
	
	public String getMapExtractorReferenceGeodataPath()
	{
		return _values.getProperty(MAP_EXTRACTOR_REFERENCE_GEODATA_PATH, "").trim();
	}
	
	public String getMapExtractorOutputPath()
	{
		final String configured = _values.getProperty(MAP_EXTRACTOR_OUTPUT_PATH, "").trim();
		return configured.isEmpty() ? _distributionRoot.resolve("game/data/geodata").toString() : configured;
	}
	
	public boolean isMapExtractorGeodataEnabled()
	{
		return Boolean.parseBoolean(_values.getProperty(MAP_EXTRACTOR_GENERATE_GEODATA, "true"));
	}
	
	public boolean isMapExtractorDoorsEnabled()
	{
		return Boolean.parseBoolean(_values.getProperty(MAP_EXTRACTOR_GENERATE_DOORS, "false"));
	}
	
	public boolean isMapExtractorWaterEnabled()
	{
		return Boolean.parseBoolean(_values.getProperty(MAP_EXTRACTOR_GENERATE_WATER, "false"));
	}
	
	/**
	 * Returns the preferred map variant suffix.
	 * @return normalized map variant suffix, or an empty string for the standard map set
	 */
	public String getMapExtractorMapVariant()
	{
		String normalized = _values.getProperty(MAP_EXTRACTOR_MAP_VARIANT, "").trim();
		while (normalized.startsWith("_"))
		{
			normalized = normalized.substring(1);
		}
		
		return normalized.toLowerCase(Locale.ROOT);
	}
	
	public Path getServiceDirectory(ServiceRole role)
	{
		return _distributionRoot.resolve(role.workingFolder());
	}
	
	public Path getServiceJar(ServiceRole role)
	{
		return _distributionRoot.resolve("libs").resolve(role.artifactName());
	}
	
	public Path getJavaOptionsFile(ServiceRole role)
	{
		return getServiceDirectory(role).resolve("java.cfg");
	}
	
	public Path getServerConfig(ServiceRole role)
	{
		return getServiceDirectory(role).resolve("config/Server.ini");
	}
	
	public Path getDatabaseConfig(ServiceRole role)
	{
		return getServiceDirectory(role).resolve("config/Database.ini");
	}
	
	public Path getProcessRecord(ServiceRole role)
	{
		return _distributionRoot.resolve("runtime").resolve(role.workingFolder() + ".pid");
	}
	
	public Path getDashboardLogFile()
	{
		return _distributionRoot.resolve("log/dashboard.log");
	}
	
	public Path getServerLogDirectory(ServiceRole role)
	{
		return getServiceDirectory(role).resolve("log");
	}
	
	public Path getDatabaseAuditLog()
	{
		return _distributionRoot.resolve("log/database-audit.log");
	}
	
	public Path getBackupDirectory()
	{
		return _distributionRoot.resolve("backup");
	}
	
	public int getServicePort(ServiceRole role)
	{
		return intValue(role.servicePortSetting(), role.servicePortDefault(), 1, 65535);
	}
	
	public synchronized void set(String key, Object value)
	{
		_values.setProperty(key, String.valueOf(value));
	}
	
	public synchronized Properties copyProperties()
	{
		final Properties copy = new Properties();
		copy.putAll(_values);
		return copy;
	}
	
	private int intValue(String key, int fallback, int minimum, int maximum)
	{
		final String raw = _values.getProperty(key);
		if (raw == null)
		{
			return fallback;
		}
		
		try
		{
			final int parsed = Integer.parseInt(raw.trim());
			return Math.max(minimum, Math.min(maximum, parsed));
		}
		catch (NumberFormatException e)
		{
			return fallback;
		}
	}
}
