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

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
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
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.l2jmobius.tools.map.UnrealCrypt;

/**
 * Draws verified Classic world-map labels from retail HuntingZone and MinimapRegion metadata.
 * <p>
 * HuntingZone supplies the localized display text and active map marker, while MinimapRegion_Classic supplies the retail world position, description offsets and font reference. The font reference is resolved through the exact profile's L2GameDataName.dat. No ZoneName fallback or unmarked HuntingZone location is inferred by the static preview.
 * </p>
 * <p>
 * MinimapRegion can contain conditional records that reuse a region ID. Their selector depends on runtime client state that is unavailable to the static preview, so the first record for each region ID is retained as the conservative base-map representation.
 * </p>
 * @author BazookaRpm
 */
final class ClassicHuntingZoneOverlay
{
	private static final Charset CLIENT_TEXT_CHARSET = Charset.forName("windows-1252");
	private static final int MAX_RECORDS = 10000;
	private static final int MAX_NAME_RECORDS = 500000;
	private static final int MAX_NAME_BYTES = 65536;
	private static final int MAX_MINIMAP_COLORS = 64;
	private static final int HUNTING_TRAILER_SIZE = 11;
	private static final int WORLD_REGION_SIZE = 32768;
	private static final int WORLD_REGION_X_OFFSET = 20;
	private static final int WORLD_REGION_Y_OFFSET = 18;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int MAP_FONT_SIZE_8 = 8;
	private static final int MAP_FONT_SIZE_9 = 9;
	private static final int MAP_FONT_SIZE_10 = 10;
	private static final int CASTLE_TYPE = 8;
	private static final Font MAP_FONT_8 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_8);
	private static final Font MAP_FONT_9 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_9);
	private static final Font MAP_FONT_10 = new Font(Font.SANS_SERIF, Font.BOLD, MAP_FONT_SIZE_10);
	private static final Color NORMAL_LABEL_COLOR = new Color(238, 238, 238);
	private static final Color CASTLE_LABEL_COLOR = new Color(235, 218, 103);
	private static final Color LABEL_OUTLINE_COLOR = new Color(18, 18, 18, 235);
	
	private ClassicHuntingZoneOverlay()
	{
	}
	
	/**
	 * Draws the verified Classic world-map label layer.
	 * @param systemDirectory the client system directory
	 * @param image the already-rendered Classic world map
	 * @param minimumX the minimum map-region X coordinate represented by the image
	 * @param minimumY the minimum map-region Y coordinate represented by the image
	 * @param profileVersion the verified Classic profile label
	 * @param expectedMinimapRegionSha256 the exact encrypted MinimapRegion_Classic DAT SHA-256
	 * @param expectedHuntingZoneSha256 the exact encrypted localized HuntingZone_Classic DAT SHA-256
	 * @param expectedGameDataNameSha256 the exact encrypted L2GameDataName DAT SHA-256
	 * @return annotation statistics, or {@code null} when required metadata does not match the verified profile
	 * @throws IOException if verified client metadata is malformed or cannot be read
	 */
	static AnnotationResult draw(Path systemDirectory, BufferedImage image, int minimumX, int minimumY, String profileVersion, String expectedMinimapRegionSha256, String expectedHuntingZoneSha256, String expectedGameDataNameSha256) throws IOException
	{
		final Path minimapRegionFile = findChildFile(systemDirectory, "MinimapRegion_Classic.dat");
		final Path huntingZoneFile = findLocalizedClassicData(systemDirectory, "HuntingZone_Classic", expectedHuntingZoneSha256);
		final Path gameDataNameFile = findChildFile(systemDirectory, "L2GameDataName.dat");
		if ((minimapRegionFile == null) || (huntingZoneFile == null) || (gameDataNameFile == null))
		{
			return null;
		}
		
		if (!sha256(minimapRegionFile).equals(expectedMinimapRegionSha256) || !sha256(gameDataNameFile).equals(expectedGameDataNameSha256))
		{
			return null;
		}
		
		final String[] gameDataNames = readGameDataNames(gameDataNameFile);
		final List<HuntingLabel> huntingLabels = readHuntingLabels(huntingZoneFile);
		final Map<Integer, MinimapRegionLayout> baseLayouts = firstLayoutByRegionId(readMinimapRegionLayouts(minimapRegionFile));
		final List<Label> labels = new ArrayList<>();
		for (HuntingLabel huntingLabel : huntingLabels)
		{
			if (huntingLabel.mapMarker() == 0)
			{
				continue;
			}
			
			final MinimapRegionLayout layout = baseLayouts.get(huntingLabel.mapMarker());
			if (layout == null)
			{
				throw new IOException("Classic " + profileVersion + " HuntingZone marker " + huntingLabel.mapMarker() + " has no matching MinimapRegion_Classic record.");
			}
			
			if (huntingLabel.text().isBlank())
			{
				throw new IOException("Classic " + profileVersion + " HuntingZone marker " + huntingLabel.mapMarker() + " has no display text.");
			}
			
			final int x = worldToMapPixel(layout.worldX(), minimumX, WORLD_REGION_X_OFFSET);
			final int y = worldToMapPixel(layout.worldY(), minimumY, WORLD_REGION_Y_OFFSET);
			final int fontSize = resolveFontSize(gameDataNames, layout.fontId(), profileVersion);
			labels.add(new Label(huntingLabel.text(), x, y, huntingLabel.huntingType(), fontSize, layout.descriptionOffsetX(), layout.descriptionOffsetY()));
		}
		
		final Graphics2D graphics = image.createGraphics();
		try
		{
			graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			for (Label label : labels)
			{
				final Font font = fontFor(label.fontSize());
				graphics.setFont(font);
				final FontMetrics metrics = graphics.getFontMetrics(font);
				final PlacedLabel placed = createPlacement(label, metrics, image.getWidth(), image.getHeight());
				drawOutlinedText(graphics, label.text(), placed.x(), placed.baselineY(), label.huntingType() == CASTLE_TYPE ? CASTLE_LABEL_COLOR : NORMAL_LABEL_COLOR);
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		return new AnnotationResult(labels.size());
	}
	
	private static int resolveFontSize(String[] gameDataNames, int fontId, String profileVersion) throws IOException
	{
		if ((fontId < 0) || (fontId >= gameDataNames.length))
		{
			throw new IOException("Classic " + profileVersion + " DescFontName index is outside L2GameDataName.dat: " + fontId + ".");
		}
		
		final String fontName = gameDataNames[fontId];
		if (fontName.equalsIgnoreCase("MapFontSize8"))
		{
			return MAP_FONT_SIZE_8;
		}
		
		if (fontName.equalsIgnoreCase("MapFontSize9"))
		{
			return MAP_FONT_SIZE_9;
		}
		
		if (fontName.equalsIgnoreCase("MapFontSize10"))
		{
			return MAP_FONT_SIZE_10;
		}
		
		throw new IOException("Classic " + profileVersion + " uses an unsupported map font reference: " + fontName + ".");
	}
	
	private static Font fontFor(int size)
	{
		if (size == MAP_FONT_SIZE_8)
		{
			return MAP_FONT_8;
		}
		
		if (size == MAP_FONT_SIZE_10)
		{
			return MAP_FONT_10;
		}
		
		return MAP_FONT_9;
	}
	
	private static Map<Integer, MinimapRegionLayout> firstLayoutByRegionId(List<MinimapRegionLayout> layouts)
	{
		final Map<Integer, MinimapRegionLayout> result = new LinkedHashMap<>();
		for (MinimapRegionLayout layout : layouts)
		{
			result.putIfAbsent(layout.regionId(), layout);
		}
		
		return result;
	}
	
	private static List<MinimapRegionLayout> readMinimapRegionLayouts(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile).order(ByteOrder.LITTLE_ENDIAN);
		try
		{
			requireRemaining(buffer, Integer.BYTES, dataFile);
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Classic MinimapRegion record count is invalid: " + count + ".");
			}
			
			final List<MinimapRegionLayout> layouts = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, 35, dataFile);
				final int regionId = Short.toUnsignedInt(buffer.getShort());
				buffer.getInt(); // IconTexNormal ASCF identifier.
				buffer.getInt(); // IconTexOver ASCF identifier.
				buffer.getInt(); // IconTexPushed ASCF identifier.
				final int worldX = buffer.getInt();
				final int worldY = buffer.getInt();
				buffer.getShort(); // Width.
				buffer.getShort(); // Height.
				final int descriptionOffsetX = buffer.getShort();
				final int descriptionOffsetY = buffer.getShort();
				final int fontId = buffer.getInt();
				final int colorCount = Byte.toUnsignedInt(buffer.get());
				if (colorCount > MAX_MINIMAP_COLORS)
				{
					throw new IOException("Classic MinimapRegion color count is invalid in record " + i + ": " + colorCount + ".");
				}
				
				requireRemaining(buffer, colorCount * Integer.BYTES, dataFile);
				buffer.position(buffer.position() + (colorCount * Integer.BYTES));
				layouts.add(new MinimapRegionLayout(regionId, worldX, worldY, descriptionOffsetX, descriptionOffsetY, fontId));
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Classic MinimapRegion data uses an unsupported record layout: " + dataFile);
			}
			
			return layouts;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Classic MinimapRegion data is truncated or malformed: " + dataFile, e);
		}
	}
	
	private static List<HuntingLabel> readHuntingLabels(Path dataFile) throws IOException
	{
		final ByteBuffer buffer = UnrealCrypt.decrypt(dataFile).order(ByteOrder.LITTLE_ENDIAN);
		try
		{
			requireRemaining(buffer, Integer.BYTES, dataFile);
			final int count = buffer.getInt();
			if ((count < 0) || (count > MAX_RECORDS))
			{
				throw new IOException("Classic HuntingZone record count is invalid: " + count + ".");
			}
			
			final List<HuntingLabel> labels = new ArrayList<>(count);
			for (int i = 0; i < count; i++)
			{
				requireRemaining(buffer, (Integer.BYTES * 4) + (Float.BYTES * 3), dataFile);
				buffer.getInt(); // ID.
				final int huntingType = buffer.getInt();
				buffer.getInt(); // MinimumLevel.
				buffer.getInt(); // MaximumLevel.
				buffer.getFloat(); // WorldX. MinimapRegion is the presentation authority.
				buffer.getFloat(); // WorldY.
				buffer.getFloat(); // WorldZ.
				readClientString(buffer, dataFile); // Description.
				requireRemaining(buffer, Integer.BYTES, dataFile);
				buffer.getInt(); // Localized-name metadata.
				final String text = readClientString(buffer, dataFile).trim();
				requireRemaining(buffer, HUNTING_TRAILER_SIZE, dataFile);
				final int mapMarker = Byte.toUnsignedInt(buffer.get());
				buffer.position(buffer.position() + HUNTING_TRAILER_SIZE - 1);
				labels.add(new HuntingLabel(text, huntingType, mapMarker));
			}
			
			final String tail = readClientString(buffer, dataFile);
			if (!tail.equals("SafePackage") || buffer.hasRemaining())
			{
				throw new IOException("Classic HuntingZone data uses an unsupported record layout: " + dataFile);
			}
			
			return labels;
		}
		catch (BufferUnderflowException | IndexOutOfBoundsException e)
		{
			throw new IOException("Classic HuntingZone data is truncated or malformed: " + dataFile, e);
		}
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
	
	private static PlacedLabel createPlacement(Label label, FontMetrics metrics, int width, int height)
	{
		final int textWidth = metrics.stringWidth(label.text());
		final int ascent = metrics.getAscent();
		final int descent = metrics.getDescent();
		int x = label.x() + label.descriptionOffsetX();
		int baselineY = label.y() + label.descriptionOffsetY() + ascent;
		if (x < 0)
		{
			x = 0;
		}
		else if ((x + textWidth) > width)
		{
			x = Math.max(0, width - textWidth);
		}
		
		if ((baselineY - ascent) < 0)
		{
			baselineY = ascent;
		}
		else if ((baselineY + descent) > height)
		{
			baselineY = height - descent;
		}
		
		return new PlacedLabel(x, baselineY);
	}
	
	private static void drawOutlinedText(Graphics2D graphics, String text, int x, int baselineY, Color color)
	{
		graphics.setColor(LABEL_OUTLINE_COLOR);
		for (int offsetY = -1; offsetY <= 1; offsetY++)
		{
			for (int offsetX = -1; offsetX <= 1; offsetX++)
			{
				if ((offsetX != 0) || (offsetY != 0))
				{
					graphics.drawString(text, x + offsetX, baselineY + offsetY);
				}
			}
		}
		
		graphics.setColor(color);
		graphics.drawString(text, x, baselineY);
	}
	
	private static int worldToMapPixel(int worldCoordinate, int minimumRegion, int regionOffset)
	{
		final double regionPosition = ((double) worldCoordinate / WORLD_REGION_SIZE) + regionOffset - minimumRegion;
		return (int) Math.round(regionPosition * REGION_PIXEL_SIZE);
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
			throw new IOException("Classic annotation string is too large: " + dataFile);
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
			
			throw new IOException("Classic annotation string length uses an invalid compact index: " + dataFile);
		}
		
		return negative ? -value : value;
	}
	
	private static void requireRemaining(ByteBuffer buffer, int required, Path dataFile) throws IOException
	{
		if ((required < 0) || (buffer.remaining() < required))
		{
			throw new IOException("Classic annotation data is truncated: " + dataFile);
		}
	}
	
	private static Path findLocalizedClassicData(Path systemDirectory, String prefix, String expectedSha256) throws IOException
	{
		if ((systemDirectory == null) || !Files.isDirectory(systemDirectory))
		{
			return null;
		}
		
		final String lowerPrefix = prefix.toLowerCase(Locale.ROOT);
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(systemDirectory))
		{
			for (Path child : stream)
			{
				if (!Files.isRegularFile(child))
				{
					continue;
				}
				
				final String name = child.getFileName().toString().toLowerCase(Locale.ROOT);
				if (name.startsWith(lowerPrefix + "-") && name.endsWith(".dat") && !name.startsWith(lowerPrefix + "aden-") && sha256(child).equals(expectedSha256))
				{
					return child;
				}
			}
		}
		
		return null;
	}
	
	private static Path findChildFile(Path directory, String fileName) throws IOException
	{
		if ((directory == null) || !Files.isDirectory(directory))
		{
			return null;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory))
		{
			for (Path child : stream)
			{
				if (Files.isRegularFile(child) && child.getFileName().toString().equalsIgnoreCase(fileName))
				{
					return child;
				}
			}
		}
		
		return null;
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
	
	static final class AnnotationResult
	{
		private final int _minimapLabels;
		
		AnnotationResult(int minimapLabels)
		{
			_minimapLabels = minimapLabels;
		}
		
		int minimapLabels()
		{
			return _minimapLabels;
		}
	}
	
	private record HuntingLabel(String text, int huntingType, int mapMarker)
	{
	}
	
	private record MinimapRegionLayout(int regionId, int worldX, int worldY, int descriptionOffsetX, int descriptionOffsetY, int fontId)
	{
	}
	
	private record Label(String text, int x, int y, int huntingType, int fontSize, int descriptionOffsetX, int descriptionOffsetY)
	{
	}
	
	private record PlacedLabel(int x, int baselineY)
	{
	}
}
