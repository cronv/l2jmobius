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
 * Builds verified Classic world-map previews from exact retail client resources.
 * <p>
 * Classic 1.0, 1.5 and 2.0 use artwork-only maps. <br>
 * Classic 2.5, 2.7, 2.9, 2.9.5, 3.0 and 3.5 reconstruct retail static WorldIcon markers and localized labels from the selected client's exact metadata.
 * </p>
 * @author BazookaRpm
 */
public final class ClassicClientMapPreviewProvider
{
	private static final int WORLD_MAP_COLUMNS = 2;
	private static final int WORLD_MAP_ROWS = 3;
	private static final int WORLD_MAP_TILE_SIZE = 1024;
	private static final int REGION_PIXEL_SIZE = 164;
	private static final int MINIMUM_X = 15;
	private static final int MAXIMUM_X = 26;
	private static final int MINIMUM_Y = 10;
	private static final int MAXIMUM_Y = 25;
	private static final int ACTIVE_WIDTH = ((MAXIMUM_X - MINIMUM_X) + 1) * REGION_PIXEL_SIZE;
	private static final int ACTIVE_HEIGHT = ((MAXIMUM_Y - MINIMUM_Y) + 1) * REGION_PIXEL_SIZE;
	private static final int DXT1_FORMAT = 3;
	private static final int[] CLASSIC_TILE_NUMBERS =
	{
		2,
		3,
		5,
		6,
		8,
		9
	};
	
	/**
	 * Loads a verified Classic retail world-map preview.
	 * @param clientRoot the client root directory
	 * @return the rendered preview layer, or {@code null} when the client does not match a verified Classic profile
	 * @throws IOException if a verified client resource is malformed or cannot be read
	 */
	public MapPreviewLayer load(Path clientRoot) throws IOException
	{
		final Path mapsDirectory = findChildDirectory(clientRoot, "maps");
		if (!hasClassicMapVariant(mapsDirectory))
		{
			return null;
		}
		
		final Path texturesDirectory = findChildDirectory(clientRoot, "systextures");
		final Path systemDirectory = findChildDirectory(clientRoot, "system");
		if ((texturesDirectory == null) || (systemDirectory == null))
		{
			return null;
		}
		
		final MapResource mapResource = selectMapResource(texturesDirectory, readLocalization(systemDirectory));
		if (mapResource == null)
		{
			return null;
		}
		
		final UnrealPackage pkg = mapResource.pkg();
		final int packageVersion = Short.toUnsignedInt(pkg.buf.getShort(4));
		final int packageLicense = Short.toUnsignedInt(pkg.buf.getShort(6));
		final WorldMapTile[] worldMapTiles = new WorldMapTile[WORLD_MAP_COLUMNS * WORLD_MAP_ROWS];
		final MessageDigest digest = newSha256Digest();
		for (int tileIndex = 0; tileIndex < worldMapTiles.length; tileIndex++)
		{
			final String textureName = mapResource.baseName() + CLASSIC_TILE_NUMBERS[tileIndex] + "_Classic";
			final Export texture = findTexture(pkg, mapResource.groupName(), textureName);
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
			worldMapTiles[tileIndex] = new WorldMapTile(dxt1);
			digest.update(dxt1);
		}
		
		final String worldMapSha256 = HexFormat.of().formatHex(digest.digest());
		final Profile profile = Profile.forClient(packageVersion, packageLicense, mapResource.packageFile(), worldMapSha256, systemDirectory);
		if (profile == null)
		{
			return null;
		}
		
		final boolean hasClassicMinimapRegion = findChildFile(systemDirectory, "MinimapRegion_Classic.dat") != null;
		if (profile.hasWorldMapAnnotations() != hasClassicMinimapRegion)
		{
			return null;
		}
		
		final BufferedImage image = new BufferedImage(ACTIVE_WIDTH, ACTIVE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
		final Graphics2D graphics = image.createGraphics();
		try
		{
			for (int tileIndex = 0; tileIndex < worldMapTiles.length; tileIndex++)
			{
				final BufferedImage tile = decodeDxt1(worldMapTiles[tileIndex].data(), WORLD_MAP_TILE_SIZE, WORLD_MAP_TILE_SIZE);
				final int column = tileIndex % WORLD_MAP_COLUMNS;
				final int row = tileIndex / WORLD_MAP_COLUMNS;
				graphics.drawImage(tile, column * WORLD_MAP_TILE_SIZE, row * WORLD_MAP_TILE_SIZE, null);
			}
		}
		finally
		{
			graphics.dispose();
		}
		
		String source = "Classic " + profile.version() + " Client Map: " + mapResource.sourceName();
		if (profile.hasWorldMapAnnotations())
		{
			final MetadataSignature metadata = profile.metadataSignature();
			final int worldIcons = ClassicWorldMapIconOverlay.draw(systemDirectory, mapResource.packageFile(), profile.version(), metadata.minimapRegionSha256(), metadata.huntingZoneSha256(), metadata.gameDataNameSha256(), image);
			final ClassicHuntingZoneOverlay.AnnotationResult annotations = ClassicHuntingZoneOverlay.draw(systemDirectory, image, MINIMUM_X, MINIMUM_Y, profile.version(), metadata.minimapRegionSha256(), metadata.huntingZoneSha256(), metadata.gameDataNameSha256());
			if (annotations == null)
			{
				return null;
			}
			
			source += " + Classic world-map annotations (" + worldIcons + " WorldIcon markers, " + annotations.minimapLabels() + " MinimapRegion labels)";
		}
		else
		{
			source += " [artwork-only]";
		}
		
		return new MapPreviewLayer(image, MINIMUM_X, MAXIMUM_X, MINIMUM_Y, MAXIMUM_Y, source);
	}
	
	private static Map<String, Map<String, String>> readLocalization(Path systemDirectory) throws IOException
	{
		final Path localizationFile = findChildFile(systemDirectory, "Localization.ini");
		return localizationFile != null ? readIni(localizationFile) : Map.of();
	}
	
	private static MapResource selectMapResource(Path texturesDirectory, Map<String, Map<String, String>> localization) throws IOException
	{
		final Path worldMapPackage = findChildFile(texturesDirectory, "L2WorldMap.utx");
		if (worldMapPackage != null)
		{
			final UnrealPackage pkg = new UnrealPackage(UnrealCrypt.decrypt(worldMapPackage));
			if (hasClassicTiles(pkg, "Minimap", "WorldMap"))
			{
				return new MapResource(worldMapPackage, pkg, "Minimap", "WorldMap", "L2WorldMap.Minimap.WorldMap [WorldMap*_Classic]");
			}
		}
		
		final String minimapResource = resolveMinimapResource(localization);
		if (minimapResource == null)
		{
			return null;
		}
		
		final String[] resourceParts = minimapResource.split("\\.");
		if (resourceParts.length != 3)
		{
			return null;
		}
		
		final Path localizedPackage = findChildFile(texturesDirectory, resourceParts[0] + ".utx");
		if (localizedPackage == null)
		{
			return null;
		}
		
		final UnrealPackage pkg = new UnrealPackage(UnrealCrypt.decrypt(localizedPackage));
		return hasClassicTiles(pkg, resourceParts[1], resourceParts[2]) ? new MapResource(localizedPackage, pkg, resourceParts[1], resourceParts[2], minimapResource + " [WorldMap*_Classic]") : null;
	}
	
	private static boolean hasClassicTiles(UnrealPackage pkg, String groupName, String textureBaseName)
	{
		for (int tileNumber : CLASSIC_TILE_NUMBERS)
		{
			if (findTexture(pkg, groupName, textureBaseName + tileNumber + "_Classic") == null)
			{
				return false;
			}
		}
		
		return true;
	}
	
	private static boolean hasClassicMapVariant(Path mapsDirectory) throws IOException
	{
		if ((mapsDirectory == null) || !Files.isDirectory(mapsDirectory))
		{
			return false;
		}
		
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(mapsDirectory, "*.unr"))
		{
			for (Path child : stream)
			{
				if (Files.isRegularFile(child) && child.getFileName().toString().matches("(?i)\\d+_\\d+_classic\\.unr"))
				{
					return true;
				}
			}
		}
		
		return false;
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
			throw new IllegalStateException("SHA-256 is unavailable.", e);
		}
	}
	
	private static String sha256(Path file) throws IOException
	{
		return HexFormat.of().formatHex(newSha256Digest().digest(Files.readAllBytes(file)));
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
		CLASSIC_1_0("1.0", 126, 0, "69fdfb9f18d73dcd277d646c27ea09abba70c5468a058074297df2f0e3b1f5ae", "1c37c9b92c8374f1459007347d9c568eb6bc4b0460dc7098767f18a9fa661f36", null),
		CLASSIC_1_5("1.5", 131, 40, "7a83e86d342963cbceedb27e4eebc4cce177359e0742fb5e425ceacdaef10e58", "ca828edeed5a1efcb3d3abc91e1f083e81d53ff9721207257a58e38272ee3a73", null),
		CLASSIC_2_0("2.0", 132, 40, "22e652c7a970ca1235998f4ee8511d058fadf0a5a335a6334cf7ff81f538071a", "8f575eabf129f40334c8349dd9ce72725e5260b2fadf973f5dad8349cd137c3b", null),
		CLASSIC_2_5("2.5 Zaken", 132, 40, "5d2410d915f28e7560972d4fa171f408f0657485035f183584a53c316d6eaeda", "2c35f8a9a3ef90f0db2cfe47ea0d30dc52144ba4fc7e17181596897b6de156b9", metadata("4f102d9ee13ba75d3260c5da2c6d076a587f501216ba8882c3537d74998ec1f0", "831969179aac79ac7758d646c1c2cbca0e02c24e6c5dad3d7c8bd39788426691", "7f113c3a247af77a32ffb3ea5088fd068260fe91d68e21ec258af6528785fa15")),
		CLASSIC_2_7("2.7 Antharas", 132, 40, "d4ace0bfe1e70a949a6c8c93915606f13c12d64a59a3dc8bde4bebe6c38a9f86", "0167302dd9bc6361acadba79179c70d796810eb6b2cfe406d5e5c93cbaa07d28", metadata("55f886e76133e6dc0066d416148ebfc17b48e317d8d11f00c04884f4bde48116", "c82a11e038f198ebb48be2ff86d0049aa137e7e85320845b9a753cc89f82da27", "ea7fa31ee4ce538916834c2a1a9625b19a4df9eaf27c336e191ae186c497a7da")),
		CLASSIC_2_9("2.9 Secret of Empire", 133, 40, "fdb643bb87a32dd04b769b379d4d91df76bd2aa5a5ddbe598ab7e5899c510ec0", "34fbaf187bfbe68138998f07f1859fd7f0948407e284d9cff4bb918a8a7db060", metadata("d827fbfe52746626cf6b8af46f33e397447f7b98a5c8d8f19c69941b5065a11f", "e7af4aa4b794a02c03767564a5a6f1353d1de81754ebde6c9230bb3fc0d19bfb", "1df7e871f34c237585a0a8f83c16bf6477ce2c66067b0352f81d5f37346c76f5")),
		CLASSIC_2_9_5("2.9.5 Saviors", 133, 40, "ea93a835960562c3249156a7974f6307f2b1eb07f6ccac61a308667009f97d04", "34fbaf187bfbe68138998f07f1859fd7f0948407e284d9cff4bb918a8a7db060", metadata("c58fdb2aeca1e24fa63bcd4df4fc8ff63293e892907c45e3b781dd18693c4085", "24a07a083077f0f89208396429c92e3343d7ea654ab01c85bbaff54b93e6597d", "d217acd56829e16d659414bb0f71cee3fa7613c2ac87d1e28554214b8879e4c7")),
		CLASSIC_3_0("3.0 The Kamael", 133, 40, "4bcbc185228ab0d29887959c84ec587e8ae54414cb131a9132734bde8f64f77b", "963cade235cfa1217cf8eab6c208541c8ee1751ec22bf5f256da94e210172440", metadata("313f7ad5f43623d15f78f504413a1a773f658f59aa5da493a3485d716faafdb2", "11908bb5934ae55d61858474ba5b23267402ea0507967d784336ce386089cde0", "6991591511999401700998b7235f744544a0bf1d2b1141c3ce5cafa25fbeb14a")),
		CLASSIC_3_5("3.5 Tales Untold", 133, 40, "e21c203210a6509352286c73af42a32930d885b9b34be0572d7ca3363684e8c2", "233ab41dbc7b5c6773cbb5d8a3ef036fdbb102449b033af4d1c483ce12a2c518", metadata("8a244e6307a275dfb82b3c1fde44f5175ec51ea2517bdad40fafbd9ffec687c5", "2d23c2be6729df55942f744ff8d8b8123e088798a1758c7e78e9e731d3fdeb7c", "4c4f691b9df0934d5acd2fae938d8ed658e90099f96d63d4e4c513692226ac81"));
		
		private final String _version;
		private final int _packageVersion;
		private final int _packageLicense;
		private final String _texturePackageSha256;
		private final String _worldMapSha256;
		private final MetadataSignature _metadataSignature;
		
		Profile(String version, int packageVersion, int packageLicense, String texturePackageSha256, String worldMapSha256, MetadataSignature metadataSignature)
		{
			_version = version;
			_packageVersion = packageVersion;
			_packageLicense = packageLicense;
			_texturePackageSha256 = texturePackageSha256;
			_worldMapSha256 = worldMapSha256;
			_metadataSignature = metadataSignature;
		}
		
		String version()
		{
			return _version;
		}
		
		boolean hasWorldMapAnnotations()
		{
			return _metadataSignature != null;
		}
		
		MetadataSignature metadataSignature()
		{
			return _metadataSignature;
		}
		
		static Profile forClient(int packageVersion, int packageLicense, Path texturePackage, String worldMapSha256, Path systemDirectory) throws IOException
		{
			final String texturePackageSha256 = sha256(texturePackage);
			Profile selected = null;
			for (Profile profile : values())
			{
				if ((profile._packageVersion != packageVersion) || (profile._packageLicense != packageLicense) || !profile._texturePackageSha256.equals(texturePackageSha256) || !profile._worldMapSha256.equals(worldMapSha256))
				{
					continue;
				}
				
				if ((profile._metadataSignature != null) && !profile._metadataSignature.matches(systemDirectory))
				{
					continue;
				}
				
				if (selected != null)
				{
					return null;
				}
				
				selected = profile;
			}
			
			return selected;
		}
		
		private static MetadataSignature metadata(String minimapRegionSha256, String huntingZoneSha256, String gameDataNameSha256)
		{
			return new MetadataSignature(minimapRegionSha256, huntingZoneSha256, gameDataNameSha256);
		}
	}
	
	private record WorldMapTile(byte[] data)
	{
	}
	
	private record MapResource(Path packageFile, UnrealPackage pkg, String groupName, String baseName, String sourceName)
	{
	}
	
	private record MetadataSignature(String minimapRegionSha256, String huntingZoneSha256, String gameDataNameSha256)
	{
		boolean matches(Path systemDirectory) throws IOException
		{
			final Path minimapRegionFile = findChildFile(systemDirectory, "MinimapRegion_Classic.dat");
			if ((minimapRegionFile == null) || !sha256(minimapRegionFile).equals(minimapRegionSha256))
			{
				return false;
			}
			
			final Path gameDataNameFile = findChildFile(systemDirectory, "L2GameDataName.dat");
			if ((gameDataNameFile == null) || !sha256(gameDataNameFile).equals(gameDataNameSha256))
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
					
					final String fileName = child.getFileName().toString().toLowerCase(Locale.ROOT);
					if (fileName.startsWith("huntingzone_classic-") && fileName.endsWith(".dat") && !fileName.startsWith("huntingzone_classicaden-") && sha256(child).equals(huntingZoneSha256))
					{
						return true;
					}
				}
			}
			
			return false;
		}
	}
}
