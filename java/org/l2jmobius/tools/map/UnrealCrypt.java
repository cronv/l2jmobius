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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.zip.InflaterInputStream;

/**
 * Client file encryption layer: Lineage2Ver111 (XOR), Ver121 (filename-keyed XOR) and Ver413 (RSA + zlib).
 * @author Mobius
 */
public class UnrealCrypt
{
	private static final int RSA_BLOCK_SIZE = 128;
	private static final int RSA_BLOCK_DATA_SIZE = 124;
	private static final int BUFFER_SIZE = 8192;
	private static final BigInteger VER413_MODULUS = new BigInteger("97df398472ddf737ef0a0cd17e8d172f0fef1661a38a8ae1d6e829bc1c6e4c3cfc19292dda9ef90175e46e7394a18850b6417d03be6eea274d3ed1dde5b5d7bde72cc0a0b71d03608655633881793a02c9a67d9ef2b45eb7c08d4be329083ce450e68f7867b6749314d40511d09bc5744551baa86a89dc38123dc1668fd72d83", 16);
	private static final BigInteger VER413_EXPONENT = new BigInteger("35", 16);
	private static final BigInteger L2ENCDEC_MODULUS = new BigInteger("75b4d6de5c016544068a1acf125869f43d2e09fc55b8b1e289556daf9b8757635593446288b3653da1ce91c87bb1a5c18f16323495c55d7d72c0890a83f69bfd1fd9434eb1c02f3e4679edfa43309319070129c267c85604d87bb65bae205de3707af1d2108881abb567c3b3d069ae67c3a4c6a3aa93d26413d4c66094ae2039", 16);
	private static final BigInteger L2ENCDEC_EXPONENT = new BigInteger("1d", 16);
	
	public static ByteBuffer decrypt(Path file) throws IOException
	{
		final byte[] raw = Files.readAllBytes(file);
		if ((raw.length > 4) && ((raw[0] & 0xFF) == 0xC1) && ((raw[1] & 0xFF) == 0x83))
		{
			return ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN); // already decrypted
		}
		
		if (raw.length < 28)
		{
			throw new IOException("Encrypted client file is too short: " + file);
		}
		
		final String header = new String(raw, 0, 28, StandardCharsets.UTF_16LE);
		if (!header.startsWith("Lineage2Ver"))
		{
			throw new IOException("Not an L2 encrypted file: " + file);
		}
		
		final String ver = header.substring(11, 14);
		if (ver.equals("413"))
		{
			return decryptVer413(raw, file);
		}
		
		final int key;
		if (ver.equals("111"))
		{
			key = 0xAC;
		}
		else if (ver.equals("121"))
		{
			int sum = 0;
			
			// The sum feeds the decryption key, so the case fold must not follow the machine locale.
			for (char c : file.getFileName().toString().toLowerCase(Locale.ENGLISH).toCharArray())
			{
				sum += c;
			}
			
			key = sum & 0xFF;
		}
		else
		{
			throw new IOException("Unsupported L2 version " + ver + ": " + file);
		}
		
		final byte[] out = new byte[raw.length - 28];
		for (int i = 0; i < out.length; i++)
		{
			out[i] = (byte) (raw[i + 28] ^ key);
		}
		
		return ByteBuffer.wrap(out).order(ByteOrder.LITTLE_ENDIAN);
	}
	
	private static ByteBuffer decryptVer413(byte[] raw, Path file) throws IOException
	{
		IOException firstFailure;
		try
		{
			return decryptVer413(raw, VER413_MODULUS, VER413_EXPONENT);
		}
		catch (IOException e)
		{
			firstFailure = e;
		}
		
		try
		{
			return decryptVer413(raw, L2ENCDEC_MODULUS, L2ENCDEC_EXPONENT);
		}
		catch (IOException e)
		{
			e.addSuppressed(firstFailure);
			throw new IOException("Could not decrypt Lineage2Ver413 resource: " + file, e);
		}
	}
	
	private static ByteBuffer decryptVer413(byte[] raw, BigInteger modulus, BigInteger exponent) throws IOException
	{
		final ByteArrayOutputStream rsaPayload = new ByteArrayOutputStream();
		int blockCount = 0;
		
		// One buffer serves every block, the BigInteger below copies what it needs.
		final byte[] encrypted = new byte[RSA_BLOCK_SIZE];
		for (int offset = 28; (offset + RSA_BLOCK_SIZE) <= raw.length; offset += RSA_BLOCK_SIZE)
		{
			System.arraycopy(raw, offset, encrypted, 0, encrypted.length);
			final byte[] decrypted = normalizeRsaBlock(new BigInteger(1, encrypted).modPow(exponent, modulus).toByteArray());
			final int size = decrypted[3] & 0xFF;
			if (size > RSA_BLOCK_DATA_SIZE)
			{
				throw new IOException("Invalid Lineage2Ver413 RSA block data size " + size + ".");
			}
			
			final int start = RSA_BLOCK_SIZE - size - ((RSA_BLOCK_DATA_SIZE - size) % 4);
			if ((start < 4) || ((start + size) > decrypted.length))
			{
				throw new IOException("Invalid Lineage2Ver413 RSA block layout.");
			}
			
			rsaPayload.write(decrypted, start, size);
			blockCount++;
		}
		
		if (blockCount == 0)
		{
			throw new IOException("Lineage2Ver413 resource does not contain a complete RSA block.");
		}
		
		final byte[] compressed = rsaPayload.toByteArray();
		if (compressed.length < Integer.BYTES)
		{
			throw new IOException("Lineage2Ver413 payload does not contain the decompressed size.");
		}
		
		final int expectedSize = (compressed[0] & 0xFF) | ((compressed[1] & 0xFF) << 8) | ((compressed[2] & 0xFF) << 16) | ((compressed[3] & 0xFF) << 24);
		if (expectedSize < 0)
		{
			throw new IOException("Invalid Lineage2Ver413 decompressed size " + expectedSize + ".");
		}
		
		final ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(expectedSize, 1024 * 1024));
		try (InflaterInputStream inflater = new InflaterInputStream(new ByteArrayInputStream(compressed, Integer.BYTES, compressed.length - Integer.BYTES)))
		{
			final byte[] buffer = new byte[BUFFER_SIZE];
			int total = 0;
			int read;
			while ((read = inflater.read(buffer)) != -1)
			{
				total += read;
				if (total > expectedSize)
				{
					throw new IOException("Lineage2Ver413 payload exceeds its declared decompressed size.");
				}
				
				output.write(buffer, 0, read);
			}
		}
		
		final byte[] result = output.toByteArray();
		if (result.length != expectedSize)
		{
			throw new IOException("Lineage2Ver413 payload size mismatch: expected " + expectedSize + ", got " + result.length + ".");
		}
		
		return ByteBuffer.wrap(result).order(ByteOrder.LITTLE_ENDIAN);
	}
	
	private static byte[] normalizeRsaBlock(byte[] value) throws IOException
	{
		if (value.length > (RSA_BLOCK_SIZE + 1))
		{
			throw new IOException("Invalid Lineage2Ver413 RSA block length " + value.length + ".");
		}
		
		final byte[] block = new byte[RSA_BLOCK_SIZE];
		if (value.length > RSA_BLOCK_SIZE)
		{
			if ((value.length != (RSA_BLOCK_SIZE + 1)) || (value[0] != 0))
			{
				throw new IOException("Invalid Lineage2Ver413 RSA block sign extension.");
			}
			
			System.arraycopy(value, 1, block, 0, block.length);
		}
		else
		{
			System.arraycopy(value, 0, block, block.length - value.length, value.length);
		}
		
		return block;
	}
}
