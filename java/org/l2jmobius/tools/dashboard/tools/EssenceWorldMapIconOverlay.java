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
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.l2jmobius.tools.map.Export;
import org.l2jmobius.tools.map.UnrealCrypt;
import org.l2jmobius.tools.map.UnrealPackage;

/**
 * Draws verified Essence static WorldIcon markers using the exact client GameData name table. <br>
 * Numeric ASCF identifiers are resolved through the selected profile's L2GameDataName.dat and are never shared across Essence versions.
 * @author BazookaRpm
 */
final class EssenceWorldMapIconOverlay
{
	private static final Charset CLIENT_TEXT_CHARSET = Charset.forName("windows-1252");
	private static final int MAX_RECORDS = 10000;
	private static final int MAX_NAME_RECORDS = 500000;
	private static final int MAX_NAME_BYTES = 65536;
	private static final int MAX_MINIMAP_COLORS = 64;
	private static final int DXT3_FORMAT = 7;
	private static final int REGION_WORLD_SIZE = 32768;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int MINIMUM_X = 15;
	private static final int MINIMUM_Y = 10;
	private static final int WORLD_REGION_X_OFFSET = 20;
	private static final int WORLD_REGION_Y_OFFSET = 18;
	private static final int WORLD_MINIMUM_X = (MINIMUM_X - WORLD_REGION_X_OFFSET) * REGION_WORLD_SIZE;
	private static final int WORLD_MINIMUM_Y = (MINIMUM_Y - WORLD_REGION_Y_OFFSET) * REGION_WORLD_SIZE;
	private static final int SEVEN_SIGNS_TOP_TOWER_REGION_ID = 73;
	private static final int SEVEN_SIGNS_TOP_TOWER_DESC_OFFSET_X = 1369;
	private static final int SEVEN_SIGNS_TOP_TOWER_DESC_OFFSET_Y = 65;
	private static final int SEVEN_SIGNS_TOP_TOWER_PIXEL_X = 820;
	private static final int SEVEN_SIGNS_TOP_TOWER_PIXEL_Y = 0;
	private static final String SEVEN_SIGNS_TOP_TOWER_TEXTURE = "23_18_0";
	
	private EssenceWorldMapIconOverlay()
	{
	}
	
	/**
	 * Draws the verified static WorldIcon layer for one exact Essence profile.
	 * @param minimapRegionFile the verified MinimapRegion_ClassicAden DAT
	 * @param huntingZoneFile the verified localized HuntingZone_ClassicAden DAT
	 * @param gameDataNameFile the verified client L2GameDataName DAT
	 * @param worldMapPackage the verified L2WorldMap UTX
	 * @param profile the exact Essence profile
	 * @param image the already-rendered Essence world map
	 * @return the number of static WorldIcon markers drawn
	 * @throws IOException if the verified resources are malformed or inconsistent
	 */
	static int draw(Path minimapRegionFile, Path huntingZoneFile, Path gameDataNameFile, Path worldMapPackage, EssenceClientMapPreviewProvider.Profile profile, BufferedImage image) throws IOException
	{
		if ((minimapRegionFile == null) || (huntingZoneFile == null) || (gameDataNameFile == null) || (worldMapPackage == null) || (profile == null) || (image == null))
		{
			throw new IOException("Essence WorldIcon rendering requires the complete verified profile resource set.");
		}
		
		final String[] gameDataNames = readGameDataNames(gameDataNameFile);
		final Set<Integer> activeRegionIds = profile.strictActiveMarkers() ? EssenceHuntingZoneOverlay.readActiveMapMarkers(huntingZoneFile) : Set.of();
		final UnrealPackage worldMap = new UnrealPackage(UnrealCrypt.decrypt(worldMapPackage));
		final Map<String, BufferedImage> iconCache = new HashMap<>();
		final Set<Integer> usedRegionIds = new HashSet<>();
		final ByteBuffer buffer = UnrealCrypt.decrypt(minimapRegionFile).order(ByteOrder.LITTLE_ENDIAN);
		final Graphics2D graphics = image.createGraphics();
		int drawn = 0;
		try
		{
			requireRemaining(buffer, Integer.BYTES, minimapRegionFile);
			final int recordCount = buffer.getInt();
			if ((recordCount < 0) || (recordCount > MAX_RECORDS))
			{
				throw new IOException("Essence MinimapRegion record count is invalid: " + recordCount + ".");
			}
			
			for (int i = 0; i < recordCount; i++)
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
				final int descOffsetX = buffer.getShort();
				final int descOffsetY = buffer.getShort();
				buffer.getInt(); // DescFontName.
				final int colorCount = Byte.toUnsignedInt(buffer.get());
				if (colorCount > MAX_MINIMAP_COLORS)
				{
					throw new IOException("Essence MinimapRegion color count is invalid in record " + i + ": " + colorCount + ".");
				}
				
				requireRemaining(buffer, colorCount * Integer.BYTES, minimapRegionFile);
				buffer.position(buffer.position() + (colorCount * Integer.BYTES));
				
				final boolean sevenSignsTopTower = isSevenSignsTopTowerRecord(profile, regionId, worldX, worldY, width, height, descOffsetX, descOffsetY);
				if (!sevenSignsTopTower && !usedRegionIds.add(regionId))
				{
					continue;
				}
				
				if (profile.strictActiveMarkers() && !activeRegionIds.contains(regionId))
				{
					continue;
				}
				
				if ((iconTexNormal < 0) || (iconTexNormal >= gameDataNames.length))
				{
					throw new IOException("Essence " + profile.version() + " MinimapRegion ASCF index is outside L2GameDataName.dat: " + iconTexNormal + ".");
				}
				
				final String resourceName = gameDataNames[iconTexNormal];
				if (resourceName.equalsIgnoreCase("None"))
				{
					continue;
				}
				
				final WorldIconName worldIconName = parseWorldIconName(resourceName);
				if ((worldIconName == null) || worldIconName.textureName().equalsIgnoreCase("Font_Blank"))
				{
					continue;
				}
				
				if (sevenSignsTopTower && !worldIconName.textureName().equalsIgnoreCase(SEVEN_SIGNS_TOP_TOWER_TEXTURE))
				{
					throw new IOException("Essence 7.3 special top Tower of Insolence record resolved to an unexpected texture: " + resourceName + ".");
				}
				
				if (!worldIconName.packageName().equalsIgnoreCase("L2WorldMap"))
				{
					throw new IOException("Essence " + profile.version() + " static WorldIcon resolves outside L2WorldMap.utx: " + resourceName + ".");
				}
				
				BufferedImage icon = iconCache.get(worldIconName.textureName());
				if (icon == null)
				{
					icon = readWorldIcon(worldMap, worldIconName.textureName());
					if (icon == null)
					{
						throw new IOException("Essence " + profile.version() + " WorldIcon texture is missing or unsupported: " + resourceName + ".");
					}
					
					iconCache.put(worldIconName.textureName(), icon);
				}
				
				final int renderWidth = width > 0 ? width : icon.getWidth();
				final int renderHeight = height > 0 ? height : icon.getHeight();
				final int renderX = sevenSignsTopTower ? SEVEN_SIGNS_TOP_TOWER_PIXEL_X : projectX(worldX);
				final int renderY = sevenSignsTopTower ? SEVEN_SIGNS_TOP_TOWER_PIXEL_Y : projectY(worldY);
				graphics.drawImage(icon, renderX, renderY, renderWidth, renderHeight, null);
				drawn++;
			}
			
			final String tail = readClientString(buffer, minimapRegionFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Essence MinimapRegion data uses an unsupported record layout: " + minimapRegionFile);
			}
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Essence MinimapRegion data is truncated or malformed: " + minimapRegionFile, e);
		}
		finally
		{
			graphics.dispose();
		}
		
		return drawn;
	}
	
	private static boolean isSevenSignsTopTowerRecord(EssenceClientMapPreviewProvider.Profile profile, int regionId, int worldX, int worldY, int width, int height, int descOffsetX, int descOffsetY)
	{
		return profile == EssenceClientMapPreviewProvider.Profile.ESSENCE_7_3 && (regionId == SEVEN_SIGNS_TOP_TOWER_REGION_ID) && (worldX == WORLD_MINIMUM_X) && (worldY == WORLD_MINIMUM_Y) && (width == 0) && (height == 0) && (descOffsetX == SEVEN_SIGNS_TOP_TOWER_DESC_OFFSET_X) && (descOffsetY == SEVEN_SIGNS_TOP_TOWER_DESC_OFFSET_Y);
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
				throw new IOException("Essence L2GameDataName record count is invalid: " + count + ".");
			}
			
			final String[] names = new String[count];
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, Integer.BYTES, dataFile);
				final int byteCount = buffer.getInt();
				if ((byteCount < 0) || (byteCount > MAX_NAME_BYTES) || ((byteCount & 1) != 0))
				{
					throw new IOException("Essence L2GameDataName UTF-16 byte length is invalid in record " + i + ": " + byteCount + ".");
				}
				
				requireRemaining(buffer, byteCount, dataFile);
				final byte[] bytes = new byte[byteCount];
				buffer.get(bytes);
				names[i] = new String(bytes, StandardCharsets.UTF_16LE);
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Essence L2GameDataName.dat uses an unsupported layout: " + dataFile);
			}
			
			return names;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Essence L2GameDataName.dat is truncated or malformed: " + dataFile, e);
		}
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
			if (!pkg.objName(export.classRef).equalsIgnoreCase("Texture") || !pkg.names[export.nameIdx].equalsIgnoreCase(textureName) || (export.outerRef <= 0))
			{
				continue;
			}
			
			final Export outer = pkg.exports[export.outerRef - 1];
			if (pkg.names[outer.nameIdx].equalsIgnoreCase(groupName))
			{
				return export;
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
			final byte[] text = new byte[length];
			buffer.get(text);
			final int textLength = (text.length > 0) && (text[text.length - 1] == 0) ? text.length - 1 : text.length;
			return new String(text, 0, textLength, CLIENT_TEXT_CHARSET);
		}
		
		final long byteLengthValue = -(long) length * Character.BYTES;
		if (byteLengthValue > Integer.MAX_VALUE)
		{
			throw new IOException("Essence client string is too large: " + dataFile);
		}
		
		final int byteLength = (int) byteLengthValue;
		requireRemaining(buffer, byteLength, dataFile);
		final byte[] text = new byte[byteLength];
		buffer.get(text);
		final int textLength = (text.length >= 2) && (text[text.length - 2] == 0) && (text[text.length - 1] == 0) ? text.length - 2 : text.length;
		return new String(text, 0, textLength, StandardCharsets.UTF_16LE);
	}
	
	private static int readCompactIndex(ByteBuffer buffer, Path dataFile) throws IOException
	{
		requireRemaining(buffer, 1, dataFile);
		int current = Byte.toUnsignedInt(buffer.get());
		final boolean negative = (current & 0x80) != 0;
		int value = current & 0x3F;
		if ((current & 0x40) != 0)
		{
			for (int shift = 6; shift <= 27; shift += 7)
			{
				requireRemaining(buffer, 1, dataFile);
				current = Byte.toUnsignedInt(buffer.get());
				value |= (current & 0x7F) << shift;
				if ((current & 0x80) == 0)
				{
					return negative ? -value : value;
				}
			}
			
			throw new IOException("Essence client string length uses an invalid compact index: " + dataFile);
		}
		
		return negative ? -value : value;
	}
	
	private static void requireRemaining(ByteBuffer buffer, int required, Path dataFile) throws IOException
	{
		if ((required < 0) || (buffer.remaining() < required))
		{
			throw new IOException("Essence client data is truncated: " + dataFile);
		}
	}
	
	private record WorldIconName(String packageName, String textureName)
	{
	}
}
