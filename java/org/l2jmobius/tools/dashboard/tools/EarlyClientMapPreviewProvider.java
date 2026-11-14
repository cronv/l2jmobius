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
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import org.l2jmobius.tools.map.Export;
import org.l2jmobius.tools.map.UnrealCrypt;
import org.l2jmobius.tools.map.UnrealPackage;

/**
 * Builds the supported C1, C4, and Interlude client maps as Dashboard preview layers. <br>
 * C1 composes the native Aden and Elmore textures, while C4 and Interlude share the localized six-tile world-map layout.
 * @author BazookaRpm
 */
public final class EarlyClientMapPreviewProvider
{
	private static final int DXT1_FORMAT = 3;
	private static final int REGION_PIXEL_SIZE = 164;
	
	private static final int C1_TEXTURE_SIZE = 2048;
	private static final int C1_ADEN_NATIVE_WIDTH = 1642;
	private static final int C1_ADEN_NATIVE_HEIGHT = 1476;
	private static final int C1_ELMORE_NATIVE_WIDTH = 1312;
	private static final int C1_ELMORE_NATIVE_HEIGHT = 984;
	private static final int C1_MINIMUM_X = 16;
	private static final int C1_MAXIMUM_X = 25;
	private static final int C1_MINIMUM_Y = 10;
	private static final int C1_MAXIMUM_Y = 25;
	private static final int C1_COMPOSITE_WIDTH = C1_ADEN_NATIVE_WIDTH;
	private static final int C1_COMPOSITE_HEIGHT = ((C1_MAXIMUM_Y - C1_MINIMUM_Y) + 1) * REGION_PIXEL_SIZE;
	private static final int C1_ADEN_MINIMUM_X = 16;
	private static final int C1_ADEN_MINIMUM_Y = 17;
	private static final int C1_ELMORE_MINIMUM_X = 18;
	private static final int C1_ELMORE_MINIMUM_Y = 10;
	private static final MapResource C1_FALLBACK_ADEN = new MapResource("L2Font", "AdenMap-k");
	private static final MapResource C1_FALLBACK_ELMORE = new MapResource("L2Font", "ElmoreMap-k");
	
	private static final int SIX_TILE_COLUMNS = 2;
	private static final int SIX_TILE_ROWS = 3;
	private static final int SIX_TILE_TEXTURE_SIZE = 1024;
	private static final int SIX_TILE_MINIMUM_X = 16;
	private static final int SIX_TILE_MAXIMUM_X = 26;
	private static final int SIX_TILE_MINIMUM_Y = 10;
	private static final int SIX_TILE_MAXIMUM_Y = 25;
	private static final int SIX_TILE_ACTIVE_WIDTH = ((SIX_TILE_MAXIMUM_X - SIX_TILE_MINIMUM_X) + 1) * REGION_PIXEL_SIZE;
	private static final int SIX_TILE_ACTIVE_HEIGHT = ((SIX_TILE_MAXIMUM_Y - SIX_TILE_MINIMUM_Y) + 1) * REGION_PIXEL_SIZE;
	
	/**
	 * Loads the first matching early-client map using the same detection order previously used by the Dashboard.
	 * @param clientRoot client installation root
	 * @return the matching client-map layer, or {@code null} when the client is not C1, C4, or Interlude
	 * @throws IOException if client resources cannot be read
	 */
	public MapPreviewLayer load(Path clientRoot) throws IOException
	{
		for (Profile profile : Profile.values())
		{
			final MapPreviewLayer result;
			if (profile.layout() == Layout.SIX_TILE)
			{
				result = loadSixTile(clientRoot, profile);
			}
			else if (profile.layout() == Layout.ADEN_ELMORE)
			{
				result = loadC1(clientRoot, profile);
			}
			else
			{
				throw new IllegalStateException("Unsupported early client-map layout: " + profile.layout());
			}
			
			if (result != null)
			{
				return result;
			}
		}
		
		return null;
	}
	
	private static MapPreviewLayer loadC1(Path clientRoot, Profile profile) throws IOException
	{
		final Path texturesDirectory = findChildDirectory(clientRoot, "systextures");
		if (texturesDirectory == null)
		{
			return null;
		}
		
		ResourcePair resources = null;
		final Path fontInfo = findChildFile(findChildDirectory(clientRoot, "system"), "FontInfo.ini");
		if (fontInfo != null)
		{
			resources = resolveC1ConfiguredResources(fontInfo);
		}
		
		MapImages maps = null;
		if (resources != null)
		{
			maps = loadC1MapImages(texturesDirectory, resources);
		}
		
		if (maps == null)
		{
			resources = new ResourcePair(C1_FALLBACK_ADEN, C1_FALLBACK_ELMORE, "native fallback");
			maps = loadC1MapImages(texturesDirectory, resources);
		}
		
		if ((maps == null) || (resources == null))
		{
			return null;
		}
		
		final BufferedImage image = new BufferedImage(C1_COMPOSITE_WIDTH, C1_COMPOSITE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
			
			final int columns = (C1_MAXIMUM_X - C1_MINIMUM_X) + 1;
			final int elmoreX = (int) Math.round(((C1_ELMORE_MINIMUM_X - C1_MINIMUM_X) * C1_COMPOSITE_WIDTH) / (double) columns);
			final int elmoreY = (C1_ELMORE_MINIMUM_Y - C1_MINIMUM_Y) * REGION_PIXEL_SIZE;
			graphics.drawImage(maps.elmore(), elmoreX, elmoreY, C1_COMPOSITE_WIDTH, elmoreY + C1_ELMORE_NATIVE_HEIGHT, 0, 0, C1_ELMORE_NATIVE_WIDTH, C1_ELMORE_NATIVE_HEIGHT, null);
			
			final int adenX = (C1_ADEN_MINIMUM_X - C1_MINIMUM_X) * REGION_PIXEL_SIZE;
			final int adenY = (C1_ADEN_MINIMUM_Y - C1_MINIMUM_Y) * REGION_PIXEL_SIZE;
			graphics.drawImage(maps.aden(), adenX, adenY, adenX + C1_ADEN_NATIVE_WIDTH, adenY + C1_ADEN_NATIVE_HEIGHT, 0, 0, C1_ADEN_NATIVE_WIDTH, C1_ADEN_NATIVE_HEIGHT, null);
		}
		finally
		{
			graphics.dispose();
		}
		
		return new MapPreviewLayer(image, C1_MINIMUM_X, C1_MAXIMUM_X, C1_MINIMUM_Y, C1_MAXIMUM_Y, profile.displayName() + " Client Map: " + resources.description());
	}
	
	private static ResourcePair resolveC1ConfiguredResources(Path fontInfo) throws IOException
	{
		final Map<String, Map<String, String>> ini = readC1Ini(fontInfo);
		final Map<String, String> languageSet = ini.get("languageset");
		if (languageSet == null)
		{
			return null;
		}
		
		final String languageValue = languageSet.get("language");
		if (languageValue == null)
		{
			return null;
		}
		
		final String languageName;
		try
		{
			switch (Integer.parseInt(languageValue.trim()))
			{
				case 0:
				{
					languageName = "Korean";
					break;
				}
				case 1:
				{
					languageName = "English";
					break;
				}
				case 2:
				{
					languageName = "Japanese";
					break;
				}
				case 3:
				{
					languageName = "Chinese(Taiwan)";
					break;
				}
				case 4:
				{
					languageName = "China";
					break;
				}
				default:
				{
					return null;
				}
			}
		}
		catch (NumberFormatException e)
		{
			return null;
		}
		
		final Map<String, String> language = ini.get(languageName.toLowerCase());
		if (language == null)
		{
			return null;
		}
		
		final String adenValue = language.get((languageName + "_AdenMinimap").toLowerCase());
		final String elmoreValue = language.get((languageName + "_ElmoreMinimap").toLowerCase());
		final MapResource aden = parseC1Resource(adenValue);
		final MapResource elmore = parseC1Resource(elmoreValue);
		if ((aden == null) || (elmore == null))
		{
			return null;
		}
		
		return new ResourcePair(aden, elmore, adenValue + " + " + elmoreValue);
	}
	
	private static MapResource parseC1Resource(String value)
	{
		if (value == null)
		{
			return null;
		}
		
		final String[] parts = value.trim().split("\\.");
		if (parts.length < 2)
		{
			return null;
		}
		
		return new MapResource(parts[0], parts[parts.length - 1]);
	}
	
	private static MapImages loadC1MapImages(Path texturesDirectory, ResourcePair resources) throws IOException
	{
		final BufferedImage aden = loadC1Texture(texturesDirectory, resources.aden());
		if (aden == null)
		{
			return null;
		}
		
		final BufferedImage elmore = loadC1Texture(texturesDirectory, resources.elmore());
		if (elmore == null)
		{
			return null;
		}
		
		return new MapImages(aden, elmore);
	}
	
	private static BufferedImage loadC1Texture(Path texturesDirectory, MapResource resource) throws IOException
	{
		final Path texturePackage = findChildFile(texturesDirectory, resource.packageName() + ".utx");
		if (texturePackage == null)
		{
			return null;
		}
		
		final UnrealPackage pkg;
		try
		{
			pkg = new UnrealPackage(UnrealCrypt.decrypt(texturePackage));
		}
		catch (IOException e)
		{
			return null;
		}
		
		final Export texture = findTexture(pkg, null, resource.textureName());
		if (texture == null)
		{
			return null;
		}
		
		final Map<String, Object> properties = UnrealPackage.readProps(pkg, texture);
		final int width = intProperty(properties, "USize");
		final int height = intProperty(properties, "VSize");
		final int format = intProperty(properties, "Format");
		if ((width != C1_TEXTURE_SIZE) || (height != C1_TEXTURE_SIZE) || (format != DXT1_FORMAT))
		{
			return null;
		}
		
		return decodeDxt1(readTopMip(pkg, texture, (width * height) / 2, true), width, height);
	}
	
	private static MapPreviewLayer loadSixTile(Path clientRoot, Profile profile) throws IOException
	{
		final Path localizationFile = findChildFile(findChildDirectory(clientRoot, "system"), "localization.ini");
		if (localizationFile == null)
		{
			return null;
		}
		
		final Map<String, Map<String, String>> localization = readLocalizedIni(localizationFile);
		final String minimapResource = resolveMinimapResource(localization);
		if (minimapResource == null)
		{
			return null;
		}
		
		final String[] resourceParts = minimapResource.split("\\.");
		if ((resourceParts.length != 3) || !resourceParts[2].equalsIgnoreCase(profile.worldMapName()))
		{
			return null;
		}
		
		final Path texturesDirectory = findChildDirectory(clientRoot, "systextures");
		final Path texturePackage = findChildFile(texturesDirectory, resourceParts[0] + ".utx");
		if (texturePackage == null)
		{
			return null;
		}
		
		final UnrealPackage pkg = new UnrealPackage(UnrealCrypt.decrypt(texturePackage));
		final BufferedImage image = new BufferedImage(SIX_TILE_ACTIVE_WIDTH, SIX_TILE_ACTIVE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			for (int tileIndex = 0; tileIndex < (SIX_TILE_COLUMNS * SIX_TILE_ROWS); tileIndex++)
			{
				final String textureName = resourceParts[2] + (tileIndex + 1);
				final Export texture = findTexture(pkg, resourceParts[1], textureName);
				if (texture == null)
				{
					return null;
				}
				
				final Map<String, Object> properties = UnrealPackage.readProps(pkg, texture);
				final int width = intProperty(properties, "USize");
				final int height = intProperty(properties, "VSize");
				final int format = intProperty(properties, "Format");
				if ((width != SIX_TILE_TEXTURE_SIZE) || (height != SIX_TILE_TEXTURE_SIZE) || (format != DXT1_FORMAT))
				{
					return null;
				}
				
				final byte[] dxt1 = readTopMip(pkg, texture, (width * height) / 2, false);
				final BufferedImage tile = decodeDxt1(dxt1, width, height);
				final int column = tileIndex % SIX_TILE_COLUMNS;
				final int row = tileIndex / SIX_TILE_COLUMNS;
				graphics.drawImage(tile, column * SIX_TILE_TEXTURE_SIZE, row * SIX_TILE_TEXTURE_SIZE, null);
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		return new MapPreviewLayer(image, SIX_TILE_MINIMUM_X, SIX_TILE_MAXIMUM_X, SIX_TILE_MINIMUM_Y, SIX_TILE_MAXIMUM_Y, profile.displayName() + " Client Map: " + minimapResource);
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
	
	private static Map<String, Map<String, String>> readC1Ini(Path file) throws IOException
	{
		final byte[] raw = Files.readAllBytes(file);
		final byte[] bytes;
		if (startsWithLineageHeader(raw))
		{
			try
			{
				final ByteBuffer decrypted = UnrealCrypt.decrypt(file);
				bytes = new byte[decrypted.remaining()];
				decrypted.get(bytes);
			}
			catch (IOException e)
			{
				return new LinkedHashMap<>();
			}
		}
		else
		{
			bytes = raw;
		}
		
		return parseIni(decodeIniText(bytes));
	}
	
	private static Map<String, Map<String, String>> readLocalizedIni(Path file) throws IOException
	{
		final ByteBuffer decrypted = UnrealCrypt.decrypt(file);
		final ByteBuffer copy = decrypted.duplicate();
		final byte[] bytes = new byte[copy.remaining()];
		copy.get(bytes);
		return parseIni(new String(bytes, StandardCharsets.ISO_8859_1));
	}
	
	private static Map<String, Map<String, String>> parseIni(String text)
	{
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
	
	private static boolean startsWithLineageHeader(byte[] bytes)
	{
		return (bytes.length >= 28) && (bytes[0] == 'L') && (bytes[1] == 0) && (bytes[2] == 'i') && (bytes[3] == 0);
	}
	
	private static String decodeIniText(byte[] bytes)
	{
		if ((bytes.length >= 2) && ((bytes[0] & 0xFF) == 0xFF) && ((bytes[1] & 0xFF) == 0xFE))
		{
			return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
		}
		
		int zeroCount = 0;
		final int sampleSize = Math.min(bytes.length, 128);
		for (int index = 1; index < sampleSize; index += 2)
		{
			if (bytes[index] == 0)
			{
				zeroCount++;
			}
		}
		
		if ((sampleSize >= 8) && (zeroCount >= (sampleSize / 6)))
		{
			return new String(bytes, StandardCharsets.UTF_16LE);
		}
		
		return new String(bytes, StandardCharsets.ISO_8859_1);
	}
	
	private static Export findTexture(UnrealPackage pkg, String groupName, String textureName)
	{
		for (Export export : pkg.exports)
		{
			if (!pkg.objName(export.classRef).equalsIgnoreCase("Texture") || !pkg.names[export.nameIdx].equalsIgnoreCase(textureName))
			{
				continue;
			}
			
			if (groupName == null)
			{
				return export;
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
	
	private static byte[] readTopMip(UnrealPackage pkg, Export texture, int expectedBytes, boolean rereadProperties) throws IOException
	{
		if (rereadProperties)
		{
			UnrealPackage.readProps(pkg, texture);
		}
		
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
	
	private enum Layout
	{
		SIX_TILE,
		ADEN_ELMORE
	}
	
	private enum Profile
	{
		INTERLUDE("Interlude", Layout.SIX_TILE, "int_worldmap"),
		C1("C1", Layout.ADEN_ELMORE, null),
		C4("C4", Layout.SIX_TILE, "ch4_worldmap");
		
		private final String _displayName;
		private final Layout _layout;
		private final String _worldMapName;
		
		Profile(String displayName, Layout layout, String worldMapName)
		{
			_displayName = displayName;
			_layout = layout;
			_worldMapName = worldMapName;
		}
		
		String displayName()
		{
			return _displayName;
		}
		
		Layout layout()
		{
			return _layout;
		}
		
		String worldMapName()
		{
			return _worldMapName;
		}
	}
	
	private record MapResource(String packageName, String textureName)
	{
	}
	
	private record ResourcePair(MapResource aden, MapResource elmore, String description)
	{
	}
	
	private record MapImages(BufferedImage aden, BufferedImage elmore)
	{
	}
}
