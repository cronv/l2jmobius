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
import java.util.Locale;
import java.util.Map;

import org.l2jmobius.tools.map.Export;
import org.l2jmobius.tools.map.UnrealCrypt;
import org.l2jmobius.tools.map.UnrealPackage;

/**
 * Builds verified Live-client world maps from the retail nine-tile resource. <br>
 * Legacy profiles use the verified composite SHA-256 of the nine top-level world-map mip payloads. Live 7.0 and newer profiles additionally require the exact selected UTX SHA-256 because multiple retail clients can share the same nine-tile composite. When retail chronicles also share the exact UTX, their verified localized metadata signatures are used to select a single profile and ambiguous matches fail closed.
 * @author BazookaRpm
 */
public final class LiveClientMapPreviewProvider
{
	private static final int WORLD_MAP_COLUMNS = 3;
	private static final int WORLD_MAP_ROWS = 3;
	private static final int WORLD_MAP_TILE_SIZE = 1024;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int LEGACY_SOURCE_ACTIVE_X = 368;
	private static final int LEGACY_SOURCE_ACTIVE_Y = 0;
	private static final int MINIMUM_X = 11;
	private static final int ADEN_MINIMUM_X = 15;
	private static final int MAXIMUM_X = 26;
	private static final int MINIMUM_Y = 10;
	private static final int MAXIMUM_Y = 25;
	private static final int ACTIVE_WIDTH = ((MAXIMUM_X - MINIMUM_X) + 1) * REGION_PIXEL_SIZE;
	private static final int ACTIVE_HEIGHT = ((MAXIMUM_Y - MINIMUM_Y) + 1) * REGION_PIXEL_SIZE;
	private static final int ANNOTATED_GRACIA_ATLAS_WIDTH = 819;
	private static final int ANNOTATED_GRACIA_SOURCE_X = REGION_PIXEL_SIZE;
	private static final int ANNOTATED_GRACIA_ACTIVE_WIDTH = (ADEN_MINIMUM_X - MINIMUM_X) * REGION_PIXEL_SIZE;
	private static final int ANNOTATED_ADEN_LAST_TILE_WIDTH = ACTIVE_WIDTH - ANNOTATED_GRACIA_ACTIVE_WIDTH - WORLD_MAP_TILE_SIZE;
	private static final int DXT1_FORMAT = 3;
	private static final int DXT3_FORMAT = 7;
	
	/**
	 * Loads a verified Live world-map profile from the selected client.
	 * @param clientRoot the Lineage II client root
	 * @return the rendered Live client map, or {@code null} when the client does not match a verified profile
	 * @throws IOException if a verified candidate resource cannot be read
	 */
	public MapPreviewLayer load(Path clientRoot) throws IOException
	{
		final Path systemDirectory = findChildDirectory(clientRoot, "system");
		final Path localizationFile = findChildFile(systemDirectory, "localization.ini");
		if (localizationFile == null)
		{
			return null;
		}
		
		final Map<String, Map<String, String>> localization = readIni(localizationFile);
		final String minimapResource = resolveMinimapResource(localization);
		final MapResource mapResource = selectMapResource(findChildDirectory(clientRoot, "systextures"), minimapResource);
		if (mapResource == null)
		{
			return null;
		}
		
		final UnrealPackage pkg = mapResource.pkg();
		final int packageVersion = pkg.buf.getShort(4) & 0xFFFF;
		final int packageLicense = pkg.buf.getShort(6) & 0xFFFF;
		final WorldMapTile[] worldMapTiles = new WorldMapTile[WORLD_MAP_COLUMNS * WORLD_MAP_ROWS];
		final MessageDigest digest = newSha256Digest();
		for (int tileIndex = 0; tileIndex < worldMapTiles.length; tileIndex++)
		{
			final String textureName = mapResource.baseName() + (tileIndex + 1);
			final Export texture = findTexture(pkg, mapResource.groupName(), textureName);
			if (texture == null)
			{
				return null;
			}
			
			final Map<String, Object> properties = UnrealPackage.readProps(pkg, texture);
			final int width = intProperty(properties, "USize");
			final int height = intProperty(properties, "VSize");
			final int format = intProperty(properties, "Format");
			if ((width != WORLD_MAP_TILE_SIZE) || (height != WORLD_MAP_TILE_SIZE) || ((format != DXT1_FORMAT) && (format != DXT3_FORMAT)))
			{
				return null;
			}
			
			final byte[] data = readTopMip(pkg, texture, compressedSize(width, height, format));
			worldMapTiles[tileIndex] = new WorldMapTile(format, data);
			digest.update(data);
		}
		
		final String worldMapSha256 = HexFormat.of().formatHex(digest.digest());
		final String languageCode = resolveLanguageCode(localization);
		final Profile profile = Profile.forClient(packageVersion, packageLicense, mapResource.packageFile(), worldMapSha256, systemDirectory, languageCode);
		if (profile == null)
		{
			return null;
		}
		
		final Path liveDataDirectory = profile.hasWorldMapAnnotations() ? resolveLiveDataDirectory(systemDirectory, languageCode) : systemDirectory;
		if ((liveDataDirectory == null) || (findChildFile(liveDataDirectory, "ExceptionMinimapData.dat") == null))
		{
			return null;
		}
		
		final boolean hasLiveMinimapRegion = findChildFile(liveDataDirectory, "MinimapRegion.dat") != null;
		if (profile.hasWorldMapAnnotations() != hasLiveMinimapRegion)
		{
			return null;
		}
		
		final MetadataSignature metadata = profile.metadataSignature(languageCode);
		if (profile.hasWorldMapAnnotations() && (metadata == null))
		{
			return null;
		}
		
		final BufferedImage image = new BufferedImage(ACTIVE_WIDTH, ACTIVE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			for (int tileIndex = 0; tileIndex < worldMapTiles.length; tileIndex++)
			{
				final WorldMapTile worldMapTile = worldMapTiles[tileIndex];
				final BufferedImage tile = decodeWorldMapTile(worldMapTile.data(), WORLD_MAP_TILE_SIZE, WORLD_MAP_TILE_SIZE, worldMapTile.format());
				final int column = tileIndex % WORLD_MAP_COLUMNS;
				final int row = tileIndex / WORLD_MAP_COLUMNS;
				final int destinationY = row * WORLD_MAP_TILE_SIZE;
				if (!profile.hasWorldMapAnnotations())
				{
					graphics.drawImage(tile, (column * WORLD_MAP_TILE_SIZE) - LEGACY_SOURCE_ACTIVE_X, destinationY - LEGACY_SOURCE_ACTIVE_Y, null);
					continue;
				}
				
				if (column == 0)
				{
					graphics.drawImage(tile, 0, destinationY, ANNOTATED_GRACIA_ACTIVE_WIDTH, destinationY + WORLD_MAP_TILE_SIZE, ANNOTATED_GRACIA_SOURCE_X, 0, ANNOTATED_GRACIA_ATLAS_WIDTH, WORLD_MAP_TILE_SIZE, null);
				}
				else if (column == 1)
				{
					graphics.drawImage(tile, ANNOTATED_GRACIA_ACTIVE_WIDTH, destinationY, ANNOTATED_GRACIA_ACTIVE_WIDTH + WORLD_MAP_TILE_SIZE, destinationY + WORLD_MAP_TILE_SIZE, 0, 0, WORLD_MAP_TILE_SIZE, WORLD_MAP_TILE_SIZE, null);
				}
				else
				{
					graphics.drawImage(tile, ANNOTATED_GRACIA_ACTIVE_WIDTH + WORLD_MAP_TILE_SIZE, destinationY, ACTIVE_WIDTH, destinationY + WORLD_MAP_TILE_SIZE, 0, 0, ANNOTATED_ADEN_LAST_TILE_WIDTH, WORLD_MAP_TILE_SIZE, null);
				}
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		int worldIcons = 0;
		if (profile.hasWorldMapAnnotations())
		{
			worldIcons = LiveWorldMapIconOverlay.draw(liveDataDirectory, mapResource.packageFile(), profile.version(), metadata.minimapRegionSha256(), metadata.huntingZoneSha256(), metadata.gameDataNameSha256(), image);
		}
		
		String source = "Live " + profile.version() + " Client Map: " + mapResource.sourceName();
		if (profile.hasWorldMapAnnotations())
		{
			final LiveHuntingZoneOverlay.AnnotationResult annotations = LiveHuntingZoneOverlay.draw(liveDataDirectory, image, MINIMUM_X, MINIMUM_Y, metadata.minimapRegionSha256(), metadata.huntingZoneSha256());
			if (annotations == null)
			{
				return null;
			}
			
			source += " + Live world-map annotations (" + worldIcons + " WorldIcon markers, " + annotations.minimapLabels() + " MinimapRegion labels)";
		}
		
		return new MapPreviewLayer(image, MINIMUM_X, MAXIMUM_X, MINIMUM_Y, MAXIMUM_Y, source);
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
		
		String languageName = languageSet.get(languageId.toLowerCase(Locale.ROOT));
		if (languageName == null)
		{
			languageName = languageId;
		}
		
		final Map<String, String> language = ini.get(languageName.toLowerCase(Locale.ROOT));
		if (language == null)
		{
			return null;
		}
		
		return language.get((languageName + "_Minimap").toLowerCase(Locale.ROOT));
	}
	
	private static String resolveLanguageCode(Map<String, Map<String, String>> ini)
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
		
		String languageName = languageSet.get(languageId.toLowerCase(Locale.ROOT));
		if (languageName == null)
		{
			languageName = languageId;
		}
		
		final Map<String, String> language = ini.get(languageName.toLowerCase(Locale.ROOT));
		if (language == null)
		{
			return null;
		}
		
		return language.get((languageName + "_Code").toLowerCase(Locale.ROOT));
	}
	
	private static Path resolveLiveDataDirectory(Path systemDirectory, String languageCode) throws IOException
	{
		if (findChildFile(systemDirectory, "MinimapRegion.dat") != null)
		{
			return systemDirectory;
		}
		
		if ((languageCode == null) || languageCode.isBlank())
		{
			return null;
		}
		
		final Path localizedDirectory = findChildDirectory(systemDirectory, languageCode);
		return (localizedDirectory != null) && (findChildFile(localizedDirectory, "MinimapRegion.dat") != null) ? localizedDirectory : null;
	}
	
	private static MapResource selectMapResource(Path texturesDirectory, String minimapResource) throws IOException
	{
		if ((texturesDirectory == null) || !Files.isDirectory(texturesDirectory))
		{
			return null;
		}
		
		if (minimapResource != null)
		{
			final String[] resourceParts = minimapResource.split("\\.");
			if ((resourceParts.length == 3) && resourceParts[2].equalsIgnoreCase("worldmap"))
			{
				final Path localizedPackage = findChildFile(texturesDirectory, resourceParts[0] + ".utx");
				if (localizedPackage != null)
				{
					final UnrealPackage pkg = new UnrealPackage(UnrealCrypt.decrypt(localizedPackage));
					if (findTexture(pkg, resourceParts[1], resourceParts[2] + "1") != null)
					{
						return new MapResource(localizedPackage, pkg, resourceParts[1], resourceParts[2], minimapResource);
					}
				}
			}
		}
		
		final Path worldMapPackage = findChildFile(texturesDirectory, "L2WorldMap.utx");
		if (worldMapPackage == null)
		{
			return null;
		}
		
		final UnrealPackage pkg = new UnrealPackage(UnrealCrypt.decrypt(worldMapPackage));
		if (findTexture(pkg, "Minimap", "worldmap1") == null)
		{
			return null;
		}
		
		final String sourceName = minimapResource != null ? minimapResource : "L2WorldMap.Minimap.worldmap";
		return new MapResource(worldMapPackage, pkg, "Minimap", "worldmap", sourceName);
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
				final String section = trimmed.substring(1, trimmed.length() - 1).trim().toLowerCase(Locale.ROOT);
				current = sections.get(section);
				if (current == null)
				{
					current = new LinkedHashMap<>();
					sections.put(section, current);
				}
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
			
			final String key = trimmed.substring(0, separator).trim().toLowerCase(Locale.ROOT);
			final String value = trimmed.substring(separator + 1).trim();
			current.put(key, value);
		}
		
		return sections;
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
	
	private static int compressedSize(int width, int height, int format)
	{
		final int blocksWide = (width + 3) / 4;
		final int blocksHigh = (height + 3) / 4;
		return blocksWide * blocksHigh * ((format == DXT1_FORMAT) ? 8 : 16);
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
		
		throw new IOException("Could not locate world-map mip data for " + pkg.names[texture.nameIdx] + ".");
	}
	
	private static BufferedImage decodeWorldMapTile(byte[] data, int width, int height, int format) throws IOException
	{
		if (format == DXT1_FORMAT)
		{
			return decodeDxt1(data, width, height);
		}
		
		if (format == DXT3_FORMAT)
		{
			return decodeDxt3(data, width, height);
		}
		
		throw new IOException("Unsupported Live world-map texture format " + format + ".");
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
	
	private static BufferedImage decodeDxt3(byte[] data, int width, int height) throws IOException
	{
		final int blocksWide = (width + 3) / 4;
		final int blocksHigh = (height + 3) / 4;
		final int expectedBytes = blocksWide * blocksHigh * 16;
		if (data.length != expectedBytes)
		{
			throw new IOException("Unexpected DXT3 payload size " + data.length + " for " + width + "x" + height + ".");
		}
		
		final BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int offset = 0;
		for (int blockY = 0; blockY < blocksHigh; blockY++)
		{
			for (int blockX = 0; blockX < blocksWide; blockX++)
			{
				long alphaBits = 0;
				for (int i = 0; i < 8; i++)
				{
					alphaBits |= (data[offset + i] & 0xFFL) << (8 * i);
				}
				
				final int color0 = (data[offset + 8] & 0xFF) | ((data[offset + 9] & 0xFF) << 8);
				final int color1 = (data[offset + 10] & 0xFF) | ((data[offset + 11] & 0xFF) << 8);
				final int[] colors = buildDxt3Palette(color0, color1);
				final int indices = (data[offset + 12] & 0xFF) | ((data[offset + 13] & 0xFF) << 8) | ((data[offset + 14] & 0xFF) << 16) | ((data[offset + 15] & 0xFF) << 24);
				offset += 16;
				
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
						
						final int pixelIndex = (pixelY * 4) + pixelX;
						final int alpha = (int) ((alphaBits >>> (4 * pixelIndex)) & 0x0F) * 17;
						final int paletteIndex = (indices >>> (2 * pixelIndex)) & 0x03;
						image.setRGB(x, y, (colors[paletteIndex] & 0x00FFFFFF) | (alpha << 24));
					}
				}
			}
		}
		
		return image;
	}
	
	private static int[] buildDxt3Palette(int color0, int color1)
	{
		final int[] first = decodeRgb565(color0);
		final int[] second = decodeRgb565(color1);
		return new int[]
		{
			argb(255, first[0], first[1], first[2]),
			argb(255, second[0], second[1], second[2]),
			argb(255, ((2 * first[0]) + second[0]) / 3, ((2 * first[1]) + second[1]) / 3, ((2 * first[2]) + second[2]) / 3),
			argb(255, (first[0] + (2 * second[0])) / 3, (first[1] + (2 * second[1])) / 3, (first[2] + (2 * second[2])) / 3)
		};
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
			throw new IllegalStateException("SHA-256 is unavailable.", e);
		}
	}
	
	private static String sha256(Path file) throws IOException
	{
		final MessageDigest digest = newSha256Digest();
		final byte[] buffer = new byte[8192];
		try (InputStream input = Files.newInputStream(file))
		{
			int length;
			while ((length = input.read(buffer)) != -1)
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
		LIVE_1_0_ERTHEIA("1.0 Ertheia", 130, 40, null, "160891fcfdd6066cc55aa97069923e6989ae2597c24b60360824bfea246adf23"),
		LIVE_2_5_UNDERGROUND("2.5 Underground", 131, 40, null, "9ae12a98e44e7dd1fccc5e4ebf4a71b009172b56526661d58af50069589b5f19"),
		LIVE_3_0_HELIOS("3.0 Helios", 132, 40, null, "51ccbc577f2e5b75dbbf547acd15f761b974615c17584d6d7d61199a3be383cc"),
		LIVE_4_0_GRAND_CRUSADE("4.0 Grand Crusade", 132, 40, null, "6c7616d0efa35ff60a3e8dd0534ce1259c9005b644a9c4516f604207a37223c9", metadata("3873f2bb18ddf3cb35136c6b0f3756c04081496e4be5e35267c7c6316022983f", "c0cf356357e34c8ad11ab05588cf3a6c58550a10b5c861acb923833090b78089", "bea89129c85f8e55cb71d3872404b105c2f09c1f2e7472f9748b16aeda5e2d93")),
		LIVE_5_0_SALVATION("5.0 Salvation", 132, 40, null, "2b7924bccdb68adbb6c917be2bc74f2490e987b0136c5ecd0010516f4141c710", metadata("374e17d76407818c70bf6ff988c95b11bb42a5696cb5d7956a952f78a0eed6bc", "be60032d93f6864dfb39f2a3ae7d4d98d7e00a1a3d2e602b22d71e123985daa9", "ea7fa31ee4ce538916834c2a1a9625b19a4df9eaf27c336e191ae186c497a7da")),
		LIVE_6_0_FAFURION("6.0 Fafurion", 133, 40, null, "49c52258bbbdfa3fec55dd273a0c652a01a854ded3a77232849bd327ca0ae808", metadata("0ea85d114c9945f9dde73d9b008bed20ccca712546d7442362b5efa39abd649a", "eb4ae305a719ef9081b80967053fb112c5fa2edfc556b9e9d4f7e43c03c9824c", "1df7e871f34c237585a0a8f83c16bf6477ce2c66067b0352f81d5f37346c76f5")),
		LIVE_7_0("7.0", 133, 40, "4bcbc185228ab0d29887959c84ec587e8ae54414cb131a9132734bde8f64f77b", "ba0ebd566e37070ef614741231f1d267aec3ac86215d15639e2e34d5a2c784c2", metadata("7ea65981b78812ffcb20b86c18b87860a71b84d0f966c05b59aaacaa9a2717da", "1256562a1482aa724fbc64f9267b14c1b7cbce09dc9f7ae0806e00d810c05cac", "6991591511999401700998b7235f744544a0bf1d2b1141c3ce5cafa25fbeb14a")),
		LIVE_8_2("8.2", 133, 40, "e21c203210a6509352286c73af42a32930d885b9b34be0572d7ca3363684e8c2", "ba0ebd566e37070ef614741231f1d267aec3ac86215d15639e2e34d5a2c784c2", metadata("a398846daaeac59a59030bdbb127e9abfa4310c62466bd5cf359c81497baf5ba", "c11d3ee78a7a6339a1445396782a3b562da2655a78a7c65f6670cdc0cc9aa81b", "e710798d00ad334311e1d9b55f9f2f1dc4a74686fb4043cac993e00bc8b710cb")),
		LIVE_9_2("9.2", 133, 40, "3513e5c67769ce30033276072a6fe98b056b1721c11d7e7fb5f54a5c99bdfd43", "ba0ebd566e37070ef614741231f1d267aec3ac86215d15639e2e34d5a2c784c2", metadata("064f8491cdaf86556038e7a673895cea09251f46ac256ccd77348ea819538a9b", "21251381b53b28dd8c47509fdbacceb67a8bbee195a6b6341716bd42a0601de4", "51ffb6680984b47290c5c5b314ab734dd484d2a8e7d1c40c3bcc69f611f0ab12")),
		LIVE_10_3("10.3", 133, 40, "effdabb608ad3bc5ac9ebf974d4611f4e8dc29502f2bce4fe1daf9f27e53c1c7", "c4ad1d9a5f3b68212e6f9dd46043ed06383061b2c3322049f8b39ab3d3f6a1ab", metadata("eu", "a0666c96b0ef9ea56da8d9c87b950264e1592a4bc8d678f06552ed4fbafd2a6b", "cbc6b73871a89ca4e4c8c0cd8409d8fe3ec27f9b9f0805671a14f5f22deda6c3", "62838402e159ec30ecff04ddd7f3e7e987005a8b0f4667aa7f6cad56aae87c43"), metadata("es", "de805476207660c54cc29104e359bebc79c0881cc7e553ea18fdf709b91c5903", "cbc6b73871a89ca4e4c8c0cd8409d8fe3ec27f9b9f0805671a14f5f22deda6c3", "cf88bfd5f5b6b0f296d09df216ae2dfe171fc71577a11bb2475fa877a09f890b"), metadata("pl", "de805476207660c54cc29104e359bebc79c0881cc7e553ea18fdf709b91c5903", "cbc6b73871a89ca4e4c8c0cd8409d8fe3ec27f9b9f0805671a14f5f22deda6c3", "eb381299c7d0258392978b379cfba04cee2bbf844734b84be8b85bedfa0c4375"), metadata("ru", "64e7b224a0dce152b508447cfa05786e085fac2f7596f6328302ca5eaaf43e48", "251076cbd1389b9f2583de6f59e57a4e626a79db10ec6974d2f86a42deba64f4", "50316d06a369073cf7a896b315023dbfb87fbfa983fbb7298a2d5cd7d89f139c")),
		LIVE_11_3("11.3", 133, 40, "849553021a76dd31dd6ffbd7d98193eed77cddf8ddbaeed58067057de3b42be4", "93f4f2a82018e997c004d43f75c6de3a119d66ceb8ed7135ea6a1060298fd33e", metadata("eu", "bdca79ede10aecff4cb3f00cbb93ef1610d52425b54a2bdf5f7105fbd1c9efe7", "f9d48e0ac35208ce9e2695b665161815047fdcad4b731b30bdb3dbc6f2ce381c", "a0ebb3aa88fb78eaa6830c35759200f70349d81cb7c54559981faa3426846a8a"), metadata("es", "626e976e3fa9c7850667a66b6d3f4813252c75dce5e6c00eb319bb8a50172c19", "c337d9ea0ee58e59474f0cbaf000104b736a1ae3224b5449c0854acdb1531be2", "a03dcbc7c2a208c2de91a12b74e24aa1f200e8431190b32ed5d0538e24e0f19b"), metadata("pl", "626e976e3fa9c7850667a66b6d3f4813252c75dce5e6c00eb319bb8a50172c19", "c337d9ea0ee58e59474f0cbaf000104b736a1ae3224b5449c0854acdb1531be2", "eefeb80be7fd2eb74149ea70a94d4ad9a306c5a76677a498ab6c713dd7e05a9f"), metadata("ru", "64e7b224a0dce152b508447cfa05786e085fac2f7596f6328302ca5eaaf43e48", "251076cbd1389b9f2583de6f59e57a4e626a79db10ec6974d2f86a42deba64f4", "50316d06a369073cf7a896b315023dbfb87fbfa983fbb7298a2d5cd7d89f139c")),
		LIVE_12_3("12.3", 133, 40, "87302334102e8cd7a384c90022666cf2e40d6988c5d0899abab09a5f29493ed1", "93f4f2a82018e997c004d43f75c6de3a119d66ceb8ed7135ea6a1060298fd33e", metadata("eu", "54578d07825031fcc6515a6add1de970453ed30d955246972bf282b5dbc7a60c", "2c9c6cf7c0d940f2f771844f5c55c414cb91db630ac1de498866388e2cf058ed", "5ecfdb9616d1fe56c6a7c824497b629b903ee15bd5c0ecfe73574927c470b812"), metadata("es", "3c02f5054db9f64b36ad39ac9406ebf28fa6c98e23f401bc408b9b6671d87619", "7274c3c558170df049534636457519bf65573d38c9de7c11269cbf63a15e75dd", "5f7ccb035eba5494764959ef8e2dab03268daad1488e4ec632d1e7338647b650"), metadata("pl", "3c02f5054db9f64b36ad39ac9406ebf28fa6c98e23f401bc408b9b6671d87619", "7274c3c558170df049534636457519bf65573d38c9de7c11269cbf63a15e75dd", "1ff2d5df37d2bcfa995ccef0a9fee829d32c0f2a8b90c5003086c4a6913a123a")),
		LIVE_13_1("13.1", 133, 40, "9c5f5f6c5b927d993d45e4e842d14185c8dee029cf1bd991e9700011ff7ca488", "d99ddbf4ba78d85ae23d1f0116d86635cfcd8ad07fa8c2a5d324deda5201b59e", metadata("eu", "2ab81400fc2fe3fc2508b8dafb584bc285442bbd7e3ec10bee08b57005657544", "0014f39bc9cb11c4d4fae21f2ce962d2f88ba9f41da0e146098975f162f3d800", "92a538ee1b0f8badab19073d1226bcc5627d1800e0523ee2e7fc28bcebd8e9cd"), metadata("es", "ae8c88ab3c9847cd6b26ebde1503fe08f965feb6ffbcb1866354183aa7947530", "192fe59e5b2e744250bfc03122b6aabbbade00517752d9f454bc736814985d95", "45ca6a555ddb3d2d7c411fd7e627915ed8b7f9428192820fbad6a512de664885"), metadata("pl", "ae8c88ab3c9847cd6b26ebde1503fe08f965feb6ffbcb1866354183aa7947530", "192fe59e5b2e744250bfc03122b6aabbbade00517752d9f454bc736814985d95", "bdd62d570e2389e3c0b2a78920e6d6a6c012ac9888cf1def74c5a489bb89343c")),
		LIVE_13_2("13.2", 133, 40, "9c5f5f6c5b927d993d45e4e842d14185c8dee029cf1bd991e9700011ff7ca488", "d99ddbf4ba78d85ae23d1f0116d86635cfcd8ad07fa8c2a5d324deda5201b59e", metadata("eu", "e28593e35ac457495bdf7778a839599d8561f1bfbf4a684cf0d0e2fd228d5114", "87f2b347fd677bcb6ca858d2a316c0cac47ba9ffa62bb93de360d374040ac7f2", "957cad3e2a47c7db372dfd10a4a5e1f6e7be5364676da578da8f991fb055ea15"), metadata("es", "b416bbaff101ffc520d8581f5e0fbe64381b5b4ee9bd9e53dde9faf37d31cfa2", "87f2b347fd677bcb6ca858d2a316c0cac47ba9ffa62bb93de360d374040ac7f2", "75d399754b344b88dc3fd2cbf403cb7231e3d0f53935807d882189c92073a7bd"), metadata("pl", "b416bbaff101ffc520d8581f5e0fbe64381b5b4ee9bd9e53dde9faf37d31cfa2", "87f2b347fd677bcb6ca858d2a316c0cac47ba9ffa62bb93de360d374040ac7f2", "3e7717208f3f3aa752b38a68d7d3cfe9165bfdc49e455abea9eb08caf36c9c25")),
		LIVE_14_1_SAMURAI_CROW("14.1 SamuraiCrow", 133, 40, "9fc1e776d11e01fd39fd7f907ddfae3457822cb3dacc99f9b87eb2fe74f5006b", "fe2897d6d00e00930a4eb809cb216b5109b6a2028ee5741ee0be650f98312def", metadata("eu", "6c888a1e1273bc058edfe45fa0fa0b9ad05f2785a07baff4b201317eb5b18a71", "02a701c245922c55d4ad82468dcfc5c9f53afd3fad707e1554844e8671c10dfa", "58abbbb0bd2b9aa0be1c0594ebef1ca29c92bba21ededfaabfbe4fb9ddc68e5f"), metadata("es", "ee16a140d68f954911c3918f116bc627ded194684c689bcb58f7a93d073c36b8", "e42af75bee23b30b767b0901211800ddff7fae82b2abbfa7d1ea90ff43b7367e", "87be32118180c4e3fc3f813301f8019e4b16d11eb120382bc7d019a38ceb376e"), metadata("pl", "ee16a140d68f954911c3918f116bc627ded194684c689bcb58f7a93d073c36b8", "e42af75bee23b30b767b0901211800ddff7fae82b2abbfa7d1ea90ff43b7367e", "180e0a53c420a3ae0631e55390c8c6dd3c552171962dbca992c19400a75d0a44")),
		LIVE_14_2_SAKURA_ISLE("14.2 SakuraIsle", 133, 40, "9fc1e776d11e01fd39fd7f907ddfae3457822cb3dacc99f9b87eb2fe74f5006b", "fe2897d6d00e00930a4eb809cb216b5109b6a2028ee5741ee0be650f98312def", metadata("eu", "03f4587129f579446a9950d9808aa7d1d7efd5cf5e110d3415f460825b32564b", "286b4eb4727cda796005306e44ebc80be4db969563b75ebd8c719f2ad5fde166", "83518649d6f19db0e2562486689f234cf3b178bda2fce4694bacb6f046cc0cce"), metadata("es", "3ab87603f589c499bb80b7fa38ef5d4e3f3abbbf14e5eda9b6acb52829375ff5", "7c7e9a26729a34511523ce8b2fe88b1d788767e87a5ebc0491bd118814813532", "f51fd7407748486c0f618a2876475316e109cb9b5ed4c34a0258fa4f4021c019"), metadata("pl", "3ab87603f589c499bb80b7fa38ef5d4e3f3abbbf14e5eda9b6acb52829375ff5", "7c7e9a26729a34511523ce8b2fe88b1d788767e87a5ebc0491bd118814813532", "165a4334304cd80c1503ef33128c69da28f3a6bcf5512e37a8426dab7c1a50fe")),
		LIVE_14_3_THRONE("14.3 Throne", 133, 40, "4cd535ad142a491feddfbcbd6f4e6213a23e40eeab7e1e5918de9b32b4a1104e", "6a83222b7ddd3f026381d5e2cf0b8126617a8eba40b32d50fa634000e014b889", metadata("eu", "9c46998df762f66fedd87b38f2f01f8c4b283390e54d6ece41e87f6e2a49a588", "c1e7d61af98fc7a993517e97689f6c0b7c4218140c435adcce1e17a5391b6625", "e0862db8799b46c5adf20a5776fdf63bfd7dbe15749407478c0ee2e6ff8fb782"), metadata("es", "e845abe15be4109192fcf996fc7dae7a949186b5b1e2e0b11cd2204f9d70ea2f", "61893192fb6d7d04d617920232561264073193dbe22dce2215678098f30ae241", "564934fd2911217394fdd965beb3a6ce013a1355ac37b7c793ed938650a25851"), metadata("pl", "01902f1d5b92fab9c7f41732358af0bdd04cd76af5a49b0b5f5f41c9143b7fc1", "70641e2a4d1205746ff2136d469fcdfbc5153312e5a2f29833db5748e6754b36", "04fda6db60f8671fb38fc6b6849adde169cdf716138b13f1096bb8c445d8456e"));
		
		private final String _version;
		private final int _packageVersion;
		private final int _packageLicense;
		private final String _texturePackageSha256;
		private final String _worldMapSha256;
		private final MetadataSignature[] _metadataSignatures;
		
		Profile(String version, int packageVersion, int packageLicense, String texturePackageSha256, String worldMapSha256, MetadataSignature... metadataSignatures)
		{
			_version = version;
			_packageVersion = packageVersion;
			_packageLicense = packageLicense;
			_texturePackageSha256 = texturePackageSha256;
			_worldMapSha256 = worldMapSha256;
			_metadataSignatures = metadataSignatures;
		}
		
		String version()
		{
			return _version;
		}
		
		boolean hasWorldMapAnnotations()
		{
			return _metadataSignatures.length > 0;
		}
		
		MetadataSignature metadataSignature(String languageCode)
		{
			if (_metadataSignatures.length == 0)
			{
				return null;
			}
			
			if ((_metadataSignatures.length == 1) && (_metadataSignatures[0].languageCode() == null))
			{
				return _metadataSignatures[0];
			}
			
			if (languageCode == null)
			{
				return null;
			}
			
			for (MetadataSignature metadata : _metadataSignatures)
			{
				if ((metadata.languageCode() != null) && metadata.languageCode().equalsIgnoreCase(languageCode))
				{
					return metadata;
				}
			}
			
			return null;
		}
		
		static Profile forClient(int packageVersion, int packageLicense, Path texturePackage, String worldMapSha256, Path systemDirectory, String languageCode) throws IOException
		{
			final Profile[] candidates = new Profile[values().length];
			int candidateCount = 0;
			String texturePackageSha256 = null;
			for (Profile profile : values())
			{
				if ((profile._packageVersion != packageVersion) || (profile._packageLicense != packageLicense) || !profile._worldMapSha256.equals(worldMapSha256))
				{
					continue;
				}
				
				if (profile._texturePackageSha256 != null)
				{
					if (texturePackageSha256 == null)
					{
						texturePackageSha256 = sha256(texturePackage);
					}
					
					if (!profile._texturePackageSha256.equals(texturePackageSha256))
					{
						continue;
					}
				}
				
				candidates[candidateCount++] = profile;
			}
			
			if (candidateCount == 0)
			{
				return null;
			}
			
			if (candidateCount == 1)
			{
				return candidates[0];
			}
			
			final Path liveDataDirectory = resolveLiveDataDirectory(systemDirectory, languageCode);
			if (liveDataDirectory == null)
			{
				return null;
			}
			
			Profile selected = null;
			for (int i = 0; i < candidateCount; i++)
			{
				final Profile candidate = candidates[i];
				final MetadataSignature metadata = candidate.metadataSignature(languageCode);
				if ((metadata == null) || !metadata.matches(liveDataDirectory))
				{
					continue;
				}
				
				if (selected != null)
				{
					return null;
				}
				
				selected = candidate;
			}
			
			return selected;
		}
		
		private static MetadataSignature metadata(String minimapRegionSha256, String huntingZoneSha256, String gameDataNameSha256)
		{
			return new MetadataSignature(null, minimapRegionSha256, huntingZoneSha256, gameDataNameSha256);
		}
		
		private static MetadataSignature metadata(String languageCode, String minimapRegionSha256, String huntingZoneSha256, String gameDataNameSha256)
		{
			return new MetadataSignature(languageCode, minimapRegionSha256, huntingZoneSha256, gameDataNameSha256);
		}
	}
	
	private record WorldMapTile(int format, byte[] data)
	{
	}
	
	private record MapResource(Path packageFile, UnrealPackage pkg, String groupName, String baseName, String sourceName)
	{
	}
	
	private record MetadataSignature(String languageCode, String minimapRegionSha256, String huntingZoneSha256, String gameDataNameSha256)
	{
		boolean matches(Path dataDirectory) throws IOException
		{
			final Path minimapRegionFile = findChildFile(dataDirectory, "MinimapRegion.dat");
			if ((minimapRegionFile == null) || !sha256(minimapRegionFile).equals(minimapRegionSha256))
			{
				return false;
			}
			
			final Path gameDataNameFile = findChildFile(dataDirectory, "L2GameDataName.dat");
			if ((gameDataNameFile == null) || !sha256(gameDataNameFile).equals(gameDataNameSha256))
			{
				return false;
			}
			
			try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDirectory))
			{
				for (Path child : stream)
				{
					if (!Files.isRegularFile(child))
					{
						continue;
					}
					
					final String fileName = child.getFileName().toString().toLowerCase(Locale.ROOT);
					if (fileName.startsWith("huntingzone-") && fileName.endsWith(".dat") && !fileName.contains("_classic") && sha256(child).equals(huntingZoneSha256))
					{
						return true;
					}
				}
			}
			
			return false;
		}
	}
}
