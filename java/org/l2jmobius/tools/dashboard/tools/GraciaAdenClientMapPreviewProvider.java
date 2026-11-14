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
package org.l2jmobius.tools.dashboard.tools;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;

import org.l2jmobius.tools.map.Export;
import org.l2jmobius.tools.map.UnrealCrypt;
import org.l2jmobius.tools.map.UnrealPackage;

/**
 * Builds verified Epilogue, High Five, and Goddess of Destruction world maps from their shared Gracia/Aden atlas layout. <br>
 * The retail atlas stores a 205-pixel separator between the native Gracia and Aden canvases. The separator is removed before the map is projected onto the Dashboard region grid.
 * @author BazookaRpm
 */
public final class GraciaAdenClientMapPreviewProvider
{
	private static final int WORLD_MAP_COLUMNS = 3;
	private static final int WORLD_MAP_ROWS = 3;
	private static final int WORLD_MAP_TILE_SIZE = 1024;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int MINIMUM_X = 11;
	private static final int ADEN_MINIMUM_X = 15;
	private static final int MAXIMUM_X = 26;
	private static final int MINIMUM_Y = 10;
	private static final int MAXIMUM_Y = 25;
	private static final int ACTIVE_WIDTH = ((MAXIMUM_X - MINIMUM_X) + 1) * REGION_PIXEL_SIZE;
	private static final int ACTIVE_HEIGHT = ((MAXIMUM_Y - MINIMUM_Y) + 1) * REGION_PIXEL_SIZE;
	private static final int GRACIA_ATLAS_WIDTH = 819;
	private static final int GRACIA_SOURCE_X = REGION_PIXEL_SIZE;
	private static final int GRACIA_ACTIVE_WIDTH = (ADEN_MINIMUM_X - MINIMUM_X) * REGION_PIXEL_SIZE;
	private static final int ADEN_LAST_TILE_WIDTH = ACTIVE_WIDTH - GRACIA_ACTIVE_WIDTH - WORLD_MAP_TILE_SIZE;
	private static final int DXT1_FORMAT = 3;
	
	/**
	 * Loads a verified client world map that uses the audited Gracia/Aden atlas layout.
	 * @param clientRoot the Lineage II client root
	 * @return the rendered client map, or {@code null} when the client does not match a verified profile
	 * @throws IOException if a verified candidate resource cannot be read
	 */
	public MapPreviewLayer load(Path clientRoot) throws IOException
	{
		final Path systemDirectory = findChildDirectory(clientRoot, "system");
		final Path exceptionMinimapData = findChildFile(systemDirectory, "ExceptionMinimapData.dat");
		final String exceptionMinimapDataSha256 = exceptionMinimapData != null ? sha256(exceptionMinimapData) : null;
		final Profile candidate = Profile.forExceptionMinimapDataSha256(exceptionMinimapDataSha256);
		if (candidate == null)
		{
			return null;
		}
		
		final Path localizationFile = findChildFile(systemDirectory, "localization.ini");
		if (localizationFile == null)
		{
			return null;
		}
		
		final Map<String, Map<String, String>> localization = readIni(localizationFile);
		final String minimapResource = resolveMinimapResource(localization);
		if (minimapResource == null)
		{
			return null;
		}
		
		final String[] resourceParts = minimapResource.split("\\.");
		if ((resourceParts.length != 3) || !resourceParts[2].equalsIgnoreCase("worldmap"))
		{
			return null;
		}
		
		final Path texturePackage = findChildFile(findChildDirectory(clientRoot, "systextures"), resourceParts[0] + ".utx");
		if (texturePackage == null)
		{
			return null;
		}
		
		final String texturePackageSha256 = sha256(texturePackage);
		final UnrealPackage pkg = new UnrealPackage(UnrealCrypt.decrypt(texturePackage));
		final int packageVersion = pkg.buf.getShort(4) & 0xFFFF;
		final int packageLicense = pkg.buf.getShort(6) & 0xFFFF;
		final byte[][] worldMapTiles = new byte[WORLD_MAP_COLUMNS * WORLD_MAP_ROWS][];
		final MessageDigest digest = newSha256Digest();
		for (int tileIndex = 0; tileIndex < worldMapTiles.length; tileIndex++)
		{
			final Export texture = findTexture(pkg, resourceParts[1], resourceParts[2] + (tileIndex + 1));
			if (texture == null)
			{
				return null;
			}
			
			final Map<String, Object> properties = UnrealPackage.readProps(pkg, texture);
			final int width = intProperty(properties, "USize");
			final int height = intProperty(properties, "VSize");
			final int format = intProperty(properties, "Format");
			if ((width != WORLD_MAP_TILE_SIZE) || (height != WORLD_MAP_TILE_SIZE) || (format != DXT1_FORMAT))
			{
				return null;
			}
			
			final byte[] dxt1 = readTopMip(pkg, texture, (width * height) / 2);
			worldMapTiles[tileIndex] = dxt1;
			digest.update(dxt1);
		}
		
		final String worldMapSha256 = HexFormat.of().formatHex(digest.digest());
		if (hasMinimapRegionMetadata(systemDirectory) || !candidate.matches(packageVersion, packageLicense, texturePackageSha256, worldMapSha256))
		{
			return null;
		}
		
		final Profile profile = candidate;
		
		final BufferedImage image = new BufferedImage(ACTIVE_WIDTH, ACTIVE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			for (int tileIndex = 0; tileIndex < worldMapTiles.length; tileIndex++)
			{
				final BufferedImage tile = decodeDxt1(worldMapTiles[tileIndex], WORLD_MAP_TILE_SIZE, WORLD_MAP_TILE_SIZE);
				final int column = tileIndex % WORLD_MAP_COLUMNS;
				final int row = tileIndex / WORLD_MAP_COLUMNS;
				final int destinationY = row * WORLD_MAP_TILE_SIZE;
				switch (column)
				{
					case 0:
					{
						graphics.drawImage(tile, 0, destinationY, GRACIA_ACTIVE_WIDTH, destinationY + WORLD_MAP_TILE_SIZE, GRACIA_SOURCE_X, 0, GRACIA_ATLAS_WIDTH, WORLD_MAP_TILE_SIZE, null);
						break;
					}
					case 1:
					{
						graphics.drawImage(tile, GRACIA_ACTIVE_WIDTH, destinationY, GRACIA_ACTIVE_WIDTH + WORLD_MAP_TILE_SIZE, destinationY + WORLD_MAP_TILE_SIZE, 0, 0, WORLD_MAP_TILE_SIZE, WORLD_MAP_TILE_SIZE, null);
						break;
					}
					case 2:
					{
						graphics.drawImage(tile, GRACIA_ACTIVE_WIDTH + WORLD_MAP_TILE_SIZE, destinationY, ACTIVE_WIDTH, destinationY + WORLD_MAP_TILE_SIZE, 0, 0, ADEN_LAST_TILE_WIDTH, WORLD_MAP_TILE_SIZE, null);
						break;
					}
					default:
					{
						throw new IllegalStateException("Unexpected world-map column " + column + ".");
					}
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		return new MapPreviewLayer(image, MINIMUM_X, MAXIMUM_X, MINIMUM_Y, MAXIMUM_Y, profile.displayName + " Client Map: " + minimapResource);
	}
	
	private static String resolveMinimapResource(Map<String, Map<String, String>> ini)
	{
		final Map<String, String> languageSet = ini.get("languageset");
		if (languageSet == null)
		{
			return null;
		}
		
		final String languageId = languageSet.get("language");
		if (languageId == null)
		{
			return null;
		}
		
		String languageName = languageSet.get(languageId.toLowerCase());
		if (languageName == null)
		{
			languageName = languageId;
		}
		
		final Map<String, String> language = ini.get(languageName.toLowerCase());
		if (language == null)
		{
			return null;
		}
		
		return language.get((languageName + "_Minimap").toLowerCase());
	}
	
	private static Map<String, Map<String, String>> readIni(Path file) throws IOException
	{
		final ByteBuffer decrypted = UnrealCrypt.decrypt(file);
		final ByteBuffer copy = decrypted.duplicate();
		final byte[] bytes = new byte[copy.remaining()];
		copy.get(bytes);
		final String text = new String(bytes, StandardCharsets.ISO_8859_1);
		final Map<String, Map<String, String>> sections = new LinkedHashMap<>();
		Map<String, String> current = null;
		for (String line : text.split("\\r?\\n"))
		{
			final String trimmed = line.trim();
			if (trimmed.isEmpty() || trimmed.startsWith(";") || trimmed.startsWith("#"))
			{
				continue;
			}
			
			if (trimmed.startsWith("[") && trimmed.endsWith("]"))
			{
				final String section = trimmed.substring(1, trimmed.length() - 1).trim().toLowerCase();
				current = sections.computeIfAbsent(section, key -> new LinkedHashMap<>());
				continue;
			}
			
			if (current == null)
			{
				continue;
			}
			
			final int separator = trimmed.indexOf('=');
			if (separator <= 0)
			{
				continue;
			}
			
			final String key = trimmed.substring(0, separator).trim().toLowerCase();
			final String value = trimmed.substring(separator + 1).trim();
			current.put(key, value);
		}
		
		return sections;
	}
	
	private static boolean hasMinimapRegionMetadata(Path systemDirectory) throws IOException
	{
		if ((systemDirectory == null) || !Files.isDirectory(systemDirectory))
		{
			return false;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(systemDirectory))
		{
			for (Path child : stream)
			{
				if (!Files.isRegularFile(child))
				{
					continue;
				}
				
				final String name = child.getFileName().toString();
				if (name.regionMatches(true, 0, "MinimapRegion", 0, "MinimapRegion".length()) && (name.length() >= 4) && name.regionMatches(true, name.length() - 4, ".dat", 0, 4))
				{
					return true;
				}
			}
		}
		
		return false;
	}
	
	private static Export findTexture(UnrealPackage pkg, String groupName, String textureName)
	{
		for (Export export : pkg.exports)
		{
			if (!pkg.objName(export.classRef).equalsIgnoreCase("Texture") || !pkg.names[export.nameIdx].equalsIgnoreCase(textureName))
			{
				continue;
			}
			
			if (export.outerRef > 0)
			{
				final Export outer = pkg.exports[export.outerRef - 1];
				if (pkg.names[outer.nameIdx].equalsIgnoreCase(groupName))
				{
					return export;
				}
			}
		}
		
		return null;
	}
	
	private static int intProperty(Map<String, Object> properties, String name)
	{
		final Object value = properties.get(name);
		return value instanceof Number ? ((Number) value).intValue() : -1;
	}
	
	private static byte[] readTopMip(UnrealPackage pkg, Export texture, int expectedBytes) throws IOException
	{
		final int start = pkg.buf.position();
		final int end = texture.offset + texture.size;
		final ByteBuffer buffer = pkg.buf.duplicate().order(ByteOrder.LITTLE_ENDIAN);
		for (int skip = 0; (start + skip + 6) < end; skip++)
		{
			buffer.position(start + skip);
			final int mipCount = buffer.get() & 0xFF;
			if ((mipCount < 1) || (mipCount > 16))
			{
				continue;
			}
			
			final int positionAfter = buffer.getInt();
			final int dataSize = UnrealPackage.readIndex(buffer);
			final int dataStart = buffer.position();
			if ((dataSize != expectedBytes) || ((dataStart + dataSize) > end) || (positionAfter != (dataStart + dataSize)))
			{
				continue;
			}
			
			final byte[] data = new byte[dataSize];
			buffer.get(data);
			return data;
		}
		
		throw new IOException("Could not locate DXT1 mip data for " + pkg.names[texture.nameIdx] + ".");
	}
	
	private static BufferedImage decodeDxt1(byte[] data, int width, int height) throws IOException
	{
		final int blocksWide = (width + 3) / 4;
		final int blocksHigh = (height + 3) / 4;
		final int expectedBytes = blocksWide * blocksHigh * 8;
		if (data.length != expectedBytes)
		{
			throw new IOException("Unexpected DXT1 payload size " + data.length + " for " + width + "x" + height + ".");
		}
		
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int offset = 0;
		for (int blockY = 0; blockY < blocksHigh; blockY++)
		{
			for (int blockX = 0; blockX < blocksWide; blockX++)
			{
				final int color0 = (data[offset] & 0xFF) | ((data[offset + 1] & 0xFF) << 8);
				final int color1 = (data[offset + 2] & 0xFF) | ((data[offset + 3] & 0xFF) << 8);
				final int[] colors = buildDxt1Palette(color0, color1);
				final int indices = (data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8) | ((data[offset + 6] & 0xFF) << 16) | ((data[offset + 7] & 0xFF) << 24);
				offset += 8;
				for (int pixelY = 0; pixelY < 4; pixelY++)
				{
					final int y = (blockY * 4) + pixelY;
					if (y >= height)
					{
						continue;
					}
					
					for (int pixelX = 0; pixelX < 4; pixelX++)
					{
						final int x = (blockX * 4) + pixelX;
						if (x >= width)
						{
							continue;
						}
						
						final int paletteIndex = (indices >>> (2 * ((pixelY * 4) + pixelX))) & 0x03;
						image.setRGB(x, y, colors[paletteIndex]);
					}
				}
			}
		}
		
		return image;
	}
	
	private static int[] buildDxt1Palette(int color0, int color1)
	{
		final int[] first = decodeRgb565(color0);
		final int[] second = decodeRgb565(color1);
		final int[] palette = new int[4];
		palette[0] = argb(255, first[0], first[1], first[2]);
		palette[1] = argb(255, second[0], second[1], second[2]);
		if (color0 > color1)
		{
			palette[2] = argb(255, ((2 * first[0]) + second[0]) / 3, ((2 * first[1]) + second[1]) / 3, ((2 * first[2]) + second[2]) / 3);
			palette[3] = argb(255, (first[0] + (2 * second[0])) / 3, (first[1] + (2 * second[1])) / 3, (first[2] + (2 * second[2])) / 3);
		}
		else
		{
			palette[2] = argb(255, (first[0] + second[0]) / 2, (first[1] + second[1]) / 2, (first[2] + second[2]) / 2);
			palette[3] = 0;
		}
		
		return palette;
	}
	
	private static int[] decodeRgb565(int color)
	{
		final int red5 = (color >>> 11) & 0x1F;
		final int green6 = (color >>> 5) & 0x3F;
		final int blue5 = color & 0x1F;
		return new int[]
		{
			(red5 << 3) | (red5 >>> 2),
			(green6 << 2) | (green6 >>> 4),
			(blue5 << 3) | (blue5 >>> 2)
		};
	}
	
	private static int argb(int alpha, int red, int green, int blue)
	{
		return (alpha << 24) | (red << 16) | (green << 8) | blue;
	}
	
	private static MessageDigest newSha256Digest()
	{
		try
		{
			return MessageDigest.getInstance("SHA-256");
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is not available.", e);
		}
	}
	
	private static String sha256(Path file) throws IOException
	{
		final MessageDigest digest = newSha256Digest();
		final byte[] buffer = new byte[8192];
		try (InputStream input = Files.newInputStream(file))
		{
			int length;
			while ((length = input.read(buffer)) >= 0)
			{
				if (length > 0)
				{
					digest.update(buffer, 0, length);
				}
			}
		}
		
		return HexFormat.of().formatHex(digest.digest());
	}
	
	private static Path findChildDirectory(Path parent, String name) throws IOException
	{
		if ((parent == null) || !Files.isDirectory(parent))
		{
			return null;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent))
		{
			for (Path child : stream)
			{
				if (Files.isDirectory(child) && child.getFileName().toString().equalsIgnoreCase(name))
				{
					return child;
				}
			}
		}
		
		return null;
	}
	
	private static Path findChildFile(Path parent, String name) throws IOException
	{
		if ((parent == null) || !Files.isDirectory(parent))
		{
			return null;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(parent))
		{
			for (Path child : stream)
			{
				if (Files.isRegularFile(child) && child.getFileName().toString().equalsIgnoreCase(name))
				{
					return child;
				}
			}
		}
		
		return null;
	}
	
	private enum Profile
	{
		EPILOGUE("Epilogue", 123, 37, "ea4b09b0598e0119cc53d302c6f21c278ab32ba13952eb842bda210f686f0729", "80abcc3e71c3f5f6f3e64f97fa36f78e4a002564965b597b7545764491fdac7a", null),
		HIGH_FIVE("High Five", 123, 37, "6da0a66bb0f5693612ec46726dd609457fe1799ddc0da981ddedc44e19e81945", "cb9c5a3f7ec2378c66432cbda1f37a3e8375ca0afd17d7c3990694ff18722f18", "1ed30acdf6a2333895589e2fb73edcd70b5f5c7d1e8b2d76d644101f38f1432a"),
		GD_1_0_AWAKENING("GD 1.0 Awakening", 123, 37, "c7c410c75c4b5b6877ecafd9c4d6dd3875d9754f61758a6b30e4bbeb05d67274", "912f84aa6d2846e3fe5587c853618b9197a2179a30f8b0d42cda238267960cf3", "154c8e99372cbd50cd831c03cccbcf1a87fa7f5a2e61f2848401ad5d74924e6e"),
		GD_3_0_LINDVIOR("GD 3.0 Lindvior", 123, 37, "1f5abfa89601624165d784baf109ce5f671ba7c30bb0e0ea3052d8d59b1664a6", "608c800e9cde03a2ecc86454b8d5339d0689e8ee94703d62c4f8589de142504c", "91094ddf37688bb0cbb9b7ceeaa6ecca3cd993c53f6d9d1c0af695c4a1538910");
		
		private final String displayName;
		private final int packageVersion;
		private final int packageLicense;
		private final String texturePackageSha256;
		private final String worldMapSha256;
		private final String exceptionMinimapDataSha256;
		
		Profile(String displayName, int packageVersion, int packageLicense, String texturePackageSha256, String worldMapSha256, String exceptionMinimapDataSha256)
		{
			this.displayName = displayName;
			this.packageVersion = packageVersion;
			this.packageLicense = packageLicense;
			this.texturePackageSha256 = texturePackageSha256;
			this.worldMapSha256 = worldMapSha256;
			this.exceptionMinimapDataSha256 = exceptionMinimapDataSha256;
		}
		
		private boolean matches(int version, int license, String packageSha256, String mapSha256)
		{
			return (version == packageVersion) && (license == packageLicense) && texturePackageSha256.equals(packageSha256) && worldMapSha256.equals(mapSha256);
		}
		
		private static Profile forExceptionMinimapDataSha256(String sha256)
		{
			for (Profile profile : values())
			{
				if ((profile.exceptionMinimapDataSha256 == null) ? (sha256 == null) : profile.exceptionMinimapDataSha256.equals(sha256))
				{
					return profile;
				}
			}
			
			return null;
		}
	}
}
