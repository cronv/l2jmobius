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
package org.l2jmobius.tools.map;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * UE2 package reader: name/import/export tables, compact indices, tagged property lists, texture mips.
 * @author Mobius
 */
public class UnrealPackage
{
	public static int readIndex(ByteBuffer b)
	{
		int c = b.get() & 0xFF;
		final boolean neg = (c & 0x80) != 0;
		int out = c & 0x3F;
		if ((c & 0x40) != 0)
		{
			for (int shift = 6;; shift += 7)
			{
				c = b.get() & 0xFF;
				out |= (c & 0x7F) << shift;
				if ((c & 0x80) == 0)
				{
					break;
				}
			}
		}
		
		return neg ? -out : out;
	}
	
	public static String readString(ByteBuffer b)
	{
		final int len = readIndex(b);
		if (len >= 0) // ASCII, null-terminated
		{
			final byte[] s = new byte[len];
			b.get(s);
			return new String(s, 0, Math.max(0, len - 1), StandardCharsets.US_ASCII);
		}
		
		final byte[] s = new byte[-len * 2];
		b.get(s);
		return new String(s, 0, s.length - 2, StandardCharsets.UTF_16LE);
	}
	
	public final ByteBuffer buf;
	public final String[] names;
	public final Export[] exports;
	public final Import[] imports;
	
	public UnrealPackage(ByteBuffer b) throws IOException
	{
		buf = b;
		if (b.getInt(0) != 0x9E2A83C1)
		{
			throw new IOException("Bad Unreal package signature");
		}
		
		final int nameCount = b.getInt(12);
		final int nameOffset = b.getInt(16);
		final int exportCount = b.getInt(20);
		final int exportOffset = b.getInt(24);
		final int importCount = b.getInt(28);
		final int importOffset = b.getInt(32);
		
		names = new String[nameCount];
		b.position(nameOffset);
		for (int i = 0; i < nameCount; i++)
		{
			names[i] = readString(b);
			b.getInt(); // flags
		}
		
		imports = new Import[importCount];
		b.position(importOffset);
		for (int i = 0; i < importCount; i++)
		{
			final Import im = new Import();
			im.classPackage = readIndex(b);
			im.className = readIndex(b);
			im.packageRef = b.getInt();
			im.objectName = readIndex(b);
			imports[i] = im;
		}
		
		exports = new Export[exportCount];
		b.position(exportOffset);
		for (int i = 0; i < exportCount; i++)
		{
			final Export ex = new Export();
			ex.classRef = readIndex(b);
			ex.superRef = readIndex(b);
			ex.outerRef = b.getInt();
			ex.nameIdx = readIndex(b);
			ex.flags = b.getInt();
			ex.size = readIndex(b);
			ex.offset = ex.size > 0 ? readIndex(b) : 0;
			exports[i] = ex;
		}
	}
	
	public String objName(int ref)
	{
		if (ref > 0)
		{
			return names[exports[ref - 1].nameIdx];
		}
		
		if (ref < 0)
		{
			return names[imports[-ref - 1].objectName];
		}
		
		return "None";
	}
	
	/**
	 * Top-most package name of an imported object reference (e.g. T_22_22 for Texture'T_22_22.22_22').
	 * @param ref the object reference (negative for imports)
	 * @return the root package name, or null for non-imports
	 */
	public String rootPackage(int ref)
	{
		if (ref >= 0)
		{
			return null;
		}
		
		Import im = imports[-ref - 1];
		while (im.packageRef < 0)
		{
			im = imports[-im.packageRef - 1];
		}
		
		return names[im.objectName];
	}
	
	/**
	 * Full dotted path of an object reference, outermost package first (e.g. Door_Set_S.Human_Door_Set.Giran_Agit_door), the form the door XML stores as the name.
	 * @param ref the object reference (negative for imports, positive for exports)
	 * @return the dotted path, or the plain object name when it has no outer chain
	 */
	public String fullName(int ref)
	{
		if (ref >= 0)
		{
			return objName(ref);
		}
		
		Import im = imports[-ref - 1];
		String path = names[im.objectName];
		while (im.packageRef < 0)
		{
			im = imports[-im.packageRef - 1];
			path = names[im.objectName] + "." + path;
		}
		
		return path;
	}
	
	public static final int RF_HAS_STACK = 0x02000000;
	
	/**
	 * L2 serializes the Scale struct as a NESTED tagged property list ("Scale" Vector, "SheerRate" float, "SheerAxis" byte, "None" terminator) rather than a flat struct.<br>
	 * Parse the inner list and return the scale vector.
	 * @param p the package (for the name table)
	 * @param b the buffer positioned at the inner property list
	 * @return the scale vector {x, y, z}
	 */
	public static float[] readScale(UnrealPackage p, ByteBuffer b)
	{
		final float[] result =
		{
			1,
			1,
			1
		};
		while (true)
		{
			if (p.names[readIndex(b)].equals("None"))
			{
				return result;
			}
			
			final int info = b.get() & 0xFF;
			final int type = info & 0x0F;
			String structName = null;
			if (type == 10)
			{
				structName = p.names[readIndex(b)];
			}
			
			final int size = readPropSize((info >> 4) & 7, b);
			if (type == 3)
			{
				continue; // bool: value in the info byte
			}
			
			final int end = b.position() + size;
			if ((type == 10) && "Vector".equals(structName))
			{
				result[0] = b.getFloat();
				result[1] = b.getFloat();
				result[2] = b.getFloat();
			}
			
			b.position(end);
		}
	}
	
	/**
	 * Reads an object's property list. Leaves buffer positioned after the closing "None" (mip data follows for textures).
	 * @param p the package containing the object
	 * @param ex the export entry to read
	 * @return property name to decoded value
	 */
	public static Map<String, Object> readProps(UnrealPackage p, Export ex)
	{
		final ByteBuffer b = p.buf;
		b.position(ex.offset);
		if ((ex.flags & RF_HAS_STACK) != 0)
		{
			readIndex(b); // state node
			readIndex(b); // probe node
			b.getLong(); // probe mask
			b.getInt(); // latent action
			readIndex(b); // offset
		}
		
		final Map<String, Object> props = new LinkedHashMap<>();
		while (true)
		{
			// A tag whose declared size disagrees with the bytes actually written desyncs the walk, and the next "name index" is then garbage.
			// Real case: on the Superion-era EU client exactly one StaticMeshActor in 22_22 declares its ZoneRenderState array 3 bytes shorter than its records really are (694 actors carry that property; 676 declare it exactly, and only this one lands mid-record),
			// which read as name index -737 and killed the whole map. Give up on this object instead of the region: every caller already checks for the properties it needs, so a truncated list costs one actor rather than the build.
			final int nameIdx = readIndex(b);
			if ((nameIdx < 0) || (nameIdx >= p.names.length))
			{
				System.out.println("WARNING: property list desync in " + p.names[ex.nameIdx] + " (" + p.objName(ex.classRef) + "): bad name index " + nameIdx + " after " + props.size() + " properties - skipping the rest of this object.");
				break;
			}
			
			final String name = p.names[nameIdx];
			if (name.equals("None"))
			{
				break;
			}
			
			final int info = b.get() & 0xFF;
			final int type = info & 0x0F;
			final int sizeType = (info >> 4) & 7;
			final boolean array = (info & 0x80) != 0;
			
			String structName = null;
			if (type == 10) // StructProperty
			{
				structName = p.names[readIndex(b)];
			}
			
			final int size = readPropSize(sizeType, b);
			
			if (type == 3) // BoolProperty: value stored in array bit, size field present but no data
			{
				props.put(name, array);
				continue;
			}
			
			String key = name;
			if (array)
			{
				key = name + "[" + (b.get() & 0xFF) + "]"; // ponytail: 1-byte array index, TerrainInfo arrays never exceed 127
			}
			
			final int end = b.position() + size;
			final Object value;
			switch (type)
			{
				case 1: // Byte.
				{
					value = b.get() & 0xFF;
					break;
				}
				case 2: // Int.
				{
					value = b.getInt();
					break;
				}
				case 4: // Float.
				{
					value = b.getFloat();
					break;
				}
				case 5: // Object reference.
				{
					value = readIndex(b);
					break;
				}
				case 6: // Name.
				{
					value = p.names[readIndex(b)];
					break;
				}
				case 9: // Array: keep raw bytes (bitmaps etc.).
				{
					final byte[] raw = new byte[size];
					b.get(raw);
					value = raw;
					break;
				}
				case 10: // Struct: Vector, Scale and Rotator are decoded, the rest kept as a label.
				{
					if ("Vector".equals(structName))
					{
						value = new float[]
						{
							b.getFloat(),
							b.getFloat(),
							b.getFloat()
						};
					}
					else if ("Scale".equals(structName))
					{
						value = readScale(p, b);
					}
					else if ("Rotator".equals(structName))
					{
						value = new int[]
						{
							b.getInt(),
							b.getInt(),
							b.getInt()
						};
					}
					else
					{
						value = structName + "(" + size + " bytes)";
					}
					break;
				}
				default:
				{
					value = type + ":" + size + " bytes";
					break;
				}
			}
			
			props.put(key, value);
			b.position(end);
		}
		
		return props;
	}
	
	/**
	 * Decodes a property size field: codes 0-4 are fixed sizes, 5-7 read a byte, short or int from the buffer.
	 * @param sizeType the size code from the info byte
	 * @param b the buffer positioned at the optional size field
	 * @return the property data size in bytes
	 */
	private static int readPropSize(int sizeType, ByteBuffer b)
	{
		switch (sizeType)
		{
			case 0:
			{
				return 1;
			}
			case 1:
			{
				return 2;
			}
			case 2:
			{
				return 4;
			}
			case 3:
			{
				return 12;
			}
			case 4:
			{
				return 16;
			}
			case 5:
			{
				return b.get() & 0xFF;
			}
			case 6:
			{
				return b.getShort() & 0xFFFF;
			}
			default:
			{
				return b.getInt();
			}
		}
	}
	
	/**
	 * Reads a texture's top mip level as raw 16-bit samples, the form G16 terrain heightmaps are stored in.
	 * <p>
	 * The mip array sits at no fixed offset: a texture carries a preamble of unpredictable length after its property list - 4 bytes on the Interlude client, 257 on the Superion-era EU client - so guessing a version-specific skip breaks on the next client. This scans forward instead and makes the format prove itself at each offset against the mip header: mipCount (byte), posAfter (int32, the absolute offset just past the mip 0 data) and dataSize (compact index).
	 * <p>
	 * That dataSize field is the same thing hand-extraction guides have you hunt for: searching a dumped texture for the byte signature "40 80 10" and taking the heightmap to start immediately after it works because those three bytes ARE dataSize, decoding to 131072 = 256 * 256 * 2. Demanding that the size equal the caller's expectation AND that posAfter land exactly on the end of that data makes three independent things agree at once, so a false hit on arbitrary bytes is not a practical concern.
	 * @param b the package buffer, positioned at the end of the texture's property list
	 * @param expectedBytes the mip 0 byte count the caller expects - USize * VSize * 2 for G16
	 * @return the mip 0 samples, one unsigned 16-bit value per texel
	 * @throws IllegalStateException when no mip header carrying expectedBytes is found
	 */
	public static int[] readMip0(ByteBuffer b, int expectedBytes)
	{
		final int start = b.position();
		for (int skip = 0; (start + skip + 6) < b.limit(); skip++)
		{
			b.position(start + skip);
			final int mipCount = b.get() & 0xFF;
			if ((mipCount < 1) || (mipCount > 16))
			{
				continue;
			}
			
			final int posAfter = b.getInt();
			final int dataSize = readIndex(b);
			if ((dataSize != expectedBytes) || ((b.position() + dataSize) > b.limit()) || (posAfter != (b.position() + dataSize)))
			{
				continue;
			}
			
			final int[] h = new int[dataSize / 2];
			for (int i = 0; i < h.length; i++)
			{
				h[i] = b.getShort() & 0xFFFF;
			}
			
			return h;
		}
		
		throw new IllegalStateException("Could not locate mip data at offset " + start + " (expected " + expectedBytes + " bytes)");
	}
}
