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
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.tools.map.Export;
import org.l2jmobius.tools.map.UnrealCrypt;
import org.l2jmobius.tools.map.UnrealPackage;

/**
 * Draws the verified retail static WorldIcon layer used by supported Live 4.0 and newer profiles.
 * <p>
 * Numeric ASCF identifiers from MinimapRegion.dat are resolved through the selected client's L2GameDataName.dat instead of a cross-version numeric lookup table. WorldLocX and WorldLocY remain the retail icon origin, while Width and Height are used as the retail presentation size. The provider removes the retail Gracia/Aden separator before this overlay is drawn, so world coordinates use the Dashboard's continuous region grid from X=11 through X=26.
 * </p>
 * @author BazookaRpm
 */
final class LiveWorldMapIconOverlay
{
	private static final Charset CLIENT_TEXT_CHARSET = Charset.forName("windows-1252");
	private static final int MAX_RECORDS = 10000;
	private static final int MAX_NAME_RECORDS = 500000;
	private static final int MAX_NAME_BYTES = 65536;
	private static final int MAX_MINIMAP_COLORS = 64;
	private static final int REGION_WORLD_SIZE = 32768;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int WORLD_REGION_X_OFFSET = 20;
	private static final int WORLD_REGION_Y_OFFSET = 18;
	private static final int MINIMUM_X = 11;
	private static final int MINIMUM_Y = 10;
	private static final int WORLD_MINIMUM_X = (MINIMUM_X - WORLD_REGION_X_OFFSET) * REGION_WORLD_SIZE;
	private static final int WORLD_MINIMUM_Y = (MINIMUM_Y - WORLD_REGION_Y_OFFSET) * REGION_WORLD_SIZE;
	private static final int DXT3_FORMAT = 7;
	private static final String LIVE_4_0_HUNTING_ZONE_SHA256 = "c0cf356357e34c8ad11ab05588cf3a6c58550a10b5c861acb923833090b78089";
	private static final String LIVE_4_0_GAME_DATA_NAME_SHA256 = "bea89129c85f8e55cb71d3872404b105c2f09c1f2e7472f9748b16aeda5e2d93";
	private static final String LIVE_5_0_HUNTING_ZONE_SHA256 = "be60032d93f6864dfb39f2a3ae7d4d98d7e00a1a3d2e602b22d71e123985daa9";
	private static final String LIVE_5_0_GAME_DATA_NAME_SHA256 = "ea7fa31ee4ce538916834c2a1a9625b19a4df9eaf27c336e191ae186c497a7da";
	private static final String LIVE_6_0_HUNTING_ZONE_SHA256 = "eb4ae305a719ef9081b80967053fb112c5fa2edfc556b9e9d4f7e43c03c9824c";
	private static final String LIVE_6_0_GAME_DATA_NAME_SHA256 = "1df7e871f34c237585a0a8f83c16bf6477ce2c66067b0352f81d5f37346c76f5";
	
	private LiveWorldMapIconOverlay()
	{
	}
	
	/**
	 * Draws static Live WorldIcons using the selected client's exact GameData name table.
	 * @param dataDirectory the directory containing the selected Live metadata set
	 * @param worldIconPackage the already-selected retail package containing the WorldIcon group
	 * @param profileVersion the exact Live profile version label
	 * @param expectedMinimapRegionSha256 the exact encrypted MinimapRegion.dat SHA-256 for the profile
	 * @param expectedHuntingZoneSha256 the exact encrypted localized HuntingZone DAT SHA-256 for the profile
	 * @param expectedGameDataNameSha256 the exact encrypted L2GameDataName.dat SHA-256 for the profile
	 * @param image the already-rendered Live world map
	 * @return the number of static WorldIcon markers drawn
	 * @throws IOException if verified client metadata or textures are missing, malformed or inconsistent
	 */
	static int draw(Path dataDirectory, Path worldIconPackage, String profileVersion, String expectedMinimapRegionSha256, String expectedHuntingZoneSha256, String expectedGameDataNameSha256, BufferedImage image) throws IOException
	{
		if ((dataDirectory == null) || (worldIconPackage == null) || (profileVersion == null) || (expectedMinimapRegionSha256 == null) || (expectedHuntingZoneSha256 == null) || (expectedGameDataNameSha256 == null) || (image == null))
		{
			return 0;
		}
		
		final Path minimapRegionFile = findChildFile(dataDirectory, "MinimapRegion.dat");
		if ((minimapRegionFile == null) || !sha256(minimapRegionFile).equals(expectedMinimapRegionSha256))
		{
			return 0;
		}
		
		final Path huntingZoneFile = findLocalizedLiveData(dataDirectory, "HuntingZone", expectedHuntingZoneSha256);
		if (huntingZoneFile == null)
		{
			return 0;
		}
		
		final Path gameDataNameFile = findChildFile(dataDirectory, "L2GameDataName.dat");
		if (gameDataNameFile == null)
		{
			throw new IOException("Live " + profileVersion + " requires L2GameDataName.dat in the selected metadata directory for exact WorldIcon resolution.");
		}
		
		if (!sha256(gameDataNameFile).equals(expectedGameDataNameSha256))
		{
			throw new IOException("Live " + profileVersion + " L2GameDataName.dat does not match the verified client profile.");
		}
		
		final String[] gameDataNames = readGameDataNames(gameDataNameFile);
		final Set<Integer> activeRegionIds = readActiveMapMarkers(huntingZoneFile);
		final UnrealPackage worldMap = new UnrealPackage(UnrealCrypt.decrypt(worldIconPackage));
		final String selectedPackageName = packageName(worldIconPackage);
		final Map<String, BufferedImage> iconCache = new HashMap<>();
		final Set<Integer> seenRegionIds = new HashSet<>();
		final Graphics2D graphics = image.createGraphics();
		int drawn = 0;
		try
		{
			final ByteBuffer buffer = UnrealCrypt.decrypt(minimapRegionFile).order(ByteOrder.LITTLE_ENDIAN);
			try
			{
				requireRemaining(buffer, Integer.BYTES, minimapRegionFile);
				final int count = buffer.getInt();
				if ((count < 0) || (count > MAX_RECORDS))
				{
					throw new IOException("Live MinimapRegion record count is invalid: " + count + ".");
				}
				
				for (int i = 0; i < count; i++)
				{
					requireRemaining(buffer, 35, minimapRegionFile);
					final int regionId = Short.toUnsignedInt(buffer.getShort());
					final int iconTexNormal = buffer.getInt();
					buffer.getInt(); // IconTexOver.
					buffer.getInt(); // IconTexPushed.
					final int worldX = buffer.getInt();
					final int worldY = buffer.getInt();
					final int width = Short.toUnsignedInt(buffer.getShort());
					final int height = Short.toUnsignedInt(buffer.getShort());
					buffer.getShort(); // DescOffsetX.
					buffer.getShort(); // DescOffsetY.
					buffer.getInt(); // DescFontName.
					final int colorCount = Byte.toUnsignedInt(buffer.get());
					if (colorCount > MAX_MINIMAP_COLORS)
					{
						throw new IOException("Live MinimapRegion color count is invalid in record " + i + ": " + colorCount + ".");
					}
					
					requireRemaining(buffer, colorCount * Integer.BYTES, minimapRegionFile);
					buffer.position(buffer.position() + (colorCount * Integer.BYTES));
					
					if (!seenRegionIds.add(regionId) || !activeRegionIds.contains(regionId) || (width == 0) || (height == 0))
					{
						continue;
					}
					
					if ((iconTexNormal < 0) || (iconTexNormal >= gameDataNames.length))
					{
						throw new IOException("Live " + profileVersion + " MinimapRegion ASCF index is outside L2GameDataName.dat: " + iconTexNormal + ".");
					}
					
					final String resourceName = gameDataNames[iconTexNormal];
					if (resourceName.equalsIgnoreCase("None"))
					{
						continue;
					}
					
					final WorldIconName worldIconName = parseWorldIconName(resourceName);
					if (worldIconName == null)
					{
						throw new IOException("Live " + profileVersion + " active MinimapRegion ASCF " + iconTexNormal + " does not resolve to a WorldIcon resource: " + resourceName + ".");
					}
					
					if (!worldIconName.packageName().equalsIgnoreCase(selectedPackageName))
					{
						throw new IOException("Live " + profileVersion + " WorldIcon package mismatch for ASCF " + iconTexNormal + ": " + resourceName + " vs " + selectedPackageName + ".utx.");
					}
					
					if (worldIconName.textureName().equalsIgnoreCase("Font_Blank"))
					{
						continue;
					}
					
					BufferedImage icon = iconCache.get(worldIconName.textureName());
					if (icon == null)
					{
						icon = readWorldIcon(worldMap, worldIconName.textureName());
						if (icon == null)
						{
							throw new IOException("Live " + profileVersion + " WorldIcon texture is missing or unsupported: " + resourceName + ".");
						}
						
						iconCache.put(worldIconName.textureName(), icon);
					}
					
					graphics.drawImage(icon, projectX(worldX), projectY(worldY), width, height, null);
					drawn++;
				}
				
				final String tail = readClientString(buffer, minimapRegionFile);
				if (!tail.equals("SafePackage") || buffer.hasRemaining())
				{
					throw new IOException("Live MinimapRegion data uses an unsupported record layout: " + minimapRegionFile);
				}
			}
			catch (BufferUnderflowException | IndexOutOfBoundsException e)
			{
				throw new IOException("Live MinimapRegion data is truncated or malformed: " + minimapRegionFile, e);
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		return drawn;
	}
	
	/**
	 * Draws a verified Live WorldIcon layer using the legacy profile-version hash mapping.
	 * @param systemDirectory the client system directory
	 * @param worldIconPackage the already-selected retail package containing the WorldIcon group
	 * @param profileVersion the exact Live profile version label
	 * @param expectedMinimapRegionSha256 the exact encrypted MinimapRegion.dat SHA-256 for the profile
	 * @param expectedHuntingZoneSha256 the exact encrypted localized HuntingZone DAT SHA-256 for the profile
	 * @param image the already-rendered Live world map
	 * @return the number of static WorldIcon markers drawn
	 * @throws IOException if verified client metadata or textures are missing, malformed or inconsistent
	 */
	static int draw(Path systemDirectory, Path worldIconPackage, String profileVersion, String expectedMinimapRegionSha256, String expectedHuntingZoneSha256, BufferedImage image) throws IOException
	{
		return draw(systemDirectory, worldIconPackage, profileVersion, expectedMinimapRegionSha256, expectedHuntingZoneSha256, gameDataNameSha256(profileVersion), image);
	}
	
	/**
	 * Compatibility overload for the earlier Live icon overlay call shape.
	 * @param systemDirectory the client system directory
	 * @param texturesDirectory the client textures directory
	 * @param worldIconPackage the already-selected retail package containing WorldIcon textures
	 * @param profileVersion the exact Live profile version label
	 * @param expectedMinimapRegionSha256 the exact encrypted MinimapRegion.dat SHA-256 for the profile
	 * @param image the already-rendered Live world map
	 * @return the number of static WorldIcon markers drawn
	 * @throws IOException if verified client metadata or textures are missing, malformed or inconsistent
	 */
	static int draw(Path systemDirectory, Path texturesDirectory, Path worldIconPackage, String profileVersion, String expectedMinimapRegionSha256, BufferedImage image) throws IOException
	{
		return draw(systemDirectory, worldIconPackage, profileVersion, expectedMinimapRegionSha256, huntingZoneSha256(profileVersion), image);
	}
	
	private static String huntingZoneSha256(String profileVersion) throws IOException
	{
		if (profileVersion.equals("4.0 Grand Crusade"))
		{
			return LIVE_4_0_HUNTING_ZONE_SHA256;
		}
		
		if (profileVersion.equals("5.0 Salvation"))
		{
			return LIVE_5_0_HUNTING_ZONE_SHA256;
		}
		
		if (profileVersion.equals("6.0 Fafurion"))
		{
			return LIVE_6_0_HUNTING_ZONE_SHA256;
		}
		
		throw new IOException("Unsupported Live WorldIcon profile: " + profileVersion + ".");
	}
	
	private static String gameDataNameSha256(String profileVersion) throws IOException
	{
		if (profileVersion.equals("4.0 Grand Crusade"))
		{
			return LIVE_4_0_GAME_DATA_NAME_SHA256;
		}
		
		if (profileVersion.equals("5.0 Salvation"))
		{
			return LIVE_5_0_GAME_DATA_NAME_SHA256;
		}
		
		if (profileVersion.equals("6.0 Fafurion"))
		{
			return LIVE_6_0_GAME_DATA_NAME_SHA256;
		}
		
		throw new IOException("Unsupported Live WorldIcon profile: " + profileVersion + ".");
	}
	
	private static String[] readGameDataNames(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile).order(ByteOrder.LITTLE_ENDIAN);
		try
		{
			requireRemaining(buffer, Integer.BYTES, dataFile);
			final int count = buffer.getInt();
			if ((count <= 0) || (count > MAX_NAME_RECORDS))
			{
				throw new IOException("L2GameDataName record count is invalid: " + count + ".");
			}
			
			final String[] names = new String[count];
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, Integer.BYTES, dataFile);
				final int byteCount = buffer.getInt();
				if ((byteCount < 0) || (byteCount > MAX_NAME_BYTES) || ((byteCount & 1) != 0))
				{
					throw new IOException("L2GameDataName UTF-16 byte length is invalid in record " + i + ": " + byteCount + ".");
				}
				
				requireRemaining(buffer, byteCount, dataFile);
				final byte[] bytes = new byte[byteCount];
				buffer.get(bytes);
				names[i] = new String(bytes, StandardCharsets.UTF_16LE);
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("L2GameDataName.dat uses an unsupported layout: " + dataFile);
			}
			
			return names;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("L2GameDataName.dat is truncated or malformed: " + dataFile, e);
		}
	}
	
	private static Set<Integer> readActiveMapMarkers(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile).order(ByteOrder.LITTLE_ENDIAN);
		final Set<Integer> result = new HashSet<>();
		try
		{
			requireRemaining(buffer, Integer.BYTES, dataFile);
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Live HuntingZone record count is invalid: " + count + ".");
			}
			
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, (Integer.BYTES * 4) + (Float.BYTES * 3), dataFile);
				buffer.getInt(); // ID.
				buffer.getInt(); // HuntingType.
				buffer.getInt(); // MinimumLevel.
				buffer.getInt(); // MaximumLevel.
				buffer.getFloat(); // WorldX.
				buffer.getFloat(); // WorldY.
				buffer.getFloat(); // WorldZ.
				readClientString(buffer, dataFile); // Description.
				requireRemaining(buffer, Integer.BYTES, dataFile);
				buffer.getInt(); // Localized-name metadata.
				readClientString(buffer, dataFile); // Display name.
				requireRemaining(buffer, 7, dataFile);
				final int mapMarker = Byte.toUnsignedInt(buffer.get());
				buffer.get(); // Reserved.
				buffer.getInt(); // Runtime metadata.
				final int extraCount = Byte.toUnsignedInt(buffer.get());
				requireRemaining(buffer, (extraCount * Short.BYTES) + Integer.BYTES, dataFile);
				buffer.position(buffer.position() + (extraCount * Short.BYTES));
				buffer.getInt(); // Runtime metadata.
				if (mapMarker != 0)
				{
					result.add(mapMarker);
				}
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Live HuntingZone data uses an unsupported record layout: " + dataFile);
			}
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Live HuntingZone data is truncated or malformed: " + dataFile, e);
		}
		
		return result;
	}
	
	private static WorldIconName parseWorldIconName(String resourceName)
	{
		final String marker = ".WorldIcon.";
		final int markerIndex = resourceName.toLowerCase().indexOf(marker.toLowerCase());
		if ((markerIndex <= 0) || ((markerIndex + marker.length()) >= resourceName.length()))
		{
			return null;
		}
		
		return new WorldIconName(resourceName.substring(0, markerIndex), resourceName.substring(markerIndex + marker.length()));
	}
	
	private static BufferedImage readWorldIcon(UnrealPackage pkg, String iconName) throws IOException
	{
		final Export texture = findTexture(pkg, "WorldIcon", iconName);
		if (texture == null)
		{
			return null;
		}
		
		final Map<String, Object> properties = UnrealPackage.readProps(pkg, texture);
		final int width = intProperty(properties, "USize");
		final int height = intProperty(properties, "VSize");
		final int format = intProperty(properties, "Format");
		if ((width <= 0) || (height <= 0) || (format != DXT3_FORMAT))
		{
			return null;
		}
		
		final int expectedBytes = ((width + 3) / 4) * ((height + 3) / 4) * 16;
		return decodeDxt3(readTopMip(pkg, texture, expectedBytes), width, height);
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
			final int mipCount = Byte.toUnsignedInt(buffer.get());
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
		
		throw new IOException("Could not locate DXT3 mip data for " + pkg.names[texture.nameIdx] + ".");
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
				final int[] colors = buildDxtColorPalette(color0, color1);
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
	
	private static int[] buildDxtColorPalette(int color0, int color1)
	{
		final int[] first = decodeRgb565(color0);
		final int[] second = decodeRgb565(color1);
		return new int[]
		{
			argb(first[0], first[1], first[2]),
			argb(second[0], second[1], second[2]),
			argb(((2 * first[0]) + second[0]) / 3, ((2 * first[1]) + second[1]) / 3, ((2 * first[2]) + second[2]) / 3),
			argb((first[0] + (2 * second[0])) / 3, (first[1] + (2 * second[1])) / 3, (first[2] + (2 * second[2])) / 3)
		};
	}
	
	private static int[] decodeRgb565(int color)
	{
		return new int[]
		{
			((color >>> 11) & 0x1F) * 255 / 31,
			((color >>> 5) & 0x3F) * 255 / 63,
			(color & 0x1F) * 255 / 31
		};
	}
	
	private static int argb(int red, int green, int blue)
	{
		return 0xFF000000 | (red << 16) | (green << 8) | blue;
	}
	
	private static int projectX(int worldX)
	{
		return (int) Math.round(((worldX - (double) WORLD_MINIMUM_X) / REGION_WORLD_SIZE) * REGION_PIXEL_SIZE);
	}
	
	private static int projectY(int worldY)
	{
		return (int) Math.round(((worldY - (double) WORLD_MINIMUM_Y) / REGION_WORLD_SIZE) * REGION_PIXEL_SIZE);
	}
	
	private static String readClientString(ByteBuffer buffer, Path dataFile) throws IOException
	{
		final int length = readCompactIndex(buffer, dataFile);
		if (length == 0)
		{
			return "";
		}
		
		if (length > 0)
		{
			requireRemaining(buffer, length, dataFile);
			final byte[] bytes = new byte[length];
			buffer.get(bytes);
			final int textLength = (bytes[length - 1] == 0) ? length - 1 : length;
			return new String(bytes, 0, textLength, CLIENT_TEXT_CHARSET);
		}
		
		final int characterCount = -length;
		if (characterCount > (Integer.MAX_VALUE / Character.BYTES))
		{
			throw new IOException("Live client string length is invalid in " + dataFile + ".");
		}
		
		final int byteCount = characterCount * Character.BYTES;
		requireRemaining(buffer, byteCount, dataFile);
		final byte[] bytes = new byte[byteCount];
		buffer.get(bytes);
		int textLength = byteCount;
		if ((byteCount >= Character.BYTES) && (bytes[byteCount - 1] == 0) && (bytes[byteCount - 2] == 0))
		{
			textLength -= Character.BYTES;
		}
		
		return new String(bytes, 0, textLength, StandardCharsets.UTF_16LE);
	}
	
	private static int readCompactIndex(ByteBuffer buffer, Path dataFile) throws IOException
	{
		requireRemaining(buffer, 1, dataFile);
		int current = Byte.toUnsignedInt(buffer.get());
		final boolean negative = (current & 0x80) != 0;
		int value = current & 0x3F;
		if ((current & 0x40) != 0)
		{
			int shift = 6;
			while (true)
			{
				if (shift > 27)
				{
					throw new IOException("Live compact index is too large in " + dataFile + ".");
				}
				
				requireRemaining(buffer, 1, dataFile);
				current = Byte.toUnsignedInt(buffer.get());
				value |= (current & 0x7F) << shift;
				if ((current & 0x80) == 0)
				{
					break;
				}
				
				shift += 7;
			}
		}
		
		return negative ? -value : value;
	}
	
	private static void requireRemaining(ByteBuffer buffer, int bytes, Path dataFile) throws IOException
	{
		if ((bytes < 0) || (buffer.remaining() < bytes))
		{
			throw new IOException("Live client data is truncated: " + dataFile);
		}
	}
	
	private static Path findLocalizedLiveData(Path systemDirectory, String baseName, String expectedSha256) throws IOException
	{
		if ((systemDirectory == null) || !Files.isDirectory(systemDirectory))
		{
			return null;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(systemDirectory))
		{
			for (Path child : stream)
			{
				if (!Files.isRegularFile(child))
				{
					continue;
				}
				
				final String fileName = child.getFileName().toString();
				final String lowerName = fileName.toLowerCase();
				if (!lowerName.startsWith(baseName.toLowerCase() + "-") || !lowerName.endsWith(".dat") || lowerName.contains("_classic"))
				{
					continue;
				}
				
				if (sha256(child).equals(expectedSha256))
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
	
	private static String packageName(Path packageFile)
	{
		final String fileName = packageFile.getFileName().toString();
		final int extensionIndex = fileName.lastIndexOf('.');
		return extensionIndex > 0 ? fileName.substring(0, extensionIndex) : fileName;
	}
	
	private static String sha256(Path file) throws IOException
	{
		try
		{
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
		}
		catch (NoSuchAlgorithmException e)
		{
			throw new IllegalStateException("SHA-256 is unavailable.", e);
		}
	}
	
	private record WorldIconName(String packageName, String textureName)
	{
	}
}
