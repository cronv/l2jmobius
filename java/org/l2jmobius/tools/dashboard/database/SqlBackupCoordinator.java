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
package org.l2jmobius.tools.dashboard.database;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Creates and restores MySQL backups with preventive restore copies, SHA-256 auditing and no password arguments.
 * @author BazookaRpm
 */
public final class SqlBackupCoordinator
{
	private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS").withZone(ZoneId.systemDefault());
	private static final long COMMAND_TIMEOUT_MINUTES = 30;
	
	private final Path _backupDirectory;
	private final Path _auditLog;
	
	public SqlBackupCoordinator(Path backupDirectory, Path auditLog)
	{
		_backupDirectory = backupDirectory;
		_auditLog = auditLog;
	}
	
	public Path backupDirectory()
	{
		return _backupDirectory;
	}
	
	public Path create(DatabaseEndpoint endpoint) throws Exception
	{
		Files.createDirectories(_backupDirectory);
		final MySqlJdbcAddress address = endpoint.address();
		final Path output = _backupDirectory.resolve(address.database() + "_" + FILE_TIME.format(Instant.now()) + ".sql");
		final List<String> command = baseCommand(requireTool(endpoint, "mysqldump"), endpoint, address);
		command.add("--single-transaction");
		command.add("--quick");
		command.add("--no-tablespaces");
		command.add("--triggers");
		command.add("--routines");
		command.add("--events");
		command.add("--result-file=" + output.toAbsolutePath().normalize());
		command.add(address.database());
		try
		{
			run(command, endpoint.password(), null);
			if (!Files.isRegularFile(output) || (Files.size(output) == 0))
			{
				throw new IOException("mysqldump completed without creating a valid backup.");
			}
			
			audit("BACKUP_OK", endpoint, output, sha256(output), null, "Backup created.");
			return output;
		}
		catch (Exception e)
		{
			Files.deleteIfExists(output);
			audit("BACKUP_FAILED", endpoint, output, "", null, sanitize(e.getMessage()));
			throw e;
		}
	}
	
	public void restore(DatabaseEndpoint endpoint, Path backup) throws Exception
	{
		if ((backup == null) || !Files.isRegularFile(backup))
		{
			throw new IOException("Selected backup file does not exist.");
		}
		
		final String sourceHash = sha256(backup);
		final Path safetyBackup = create(endpoint);
		audit("RESTORE_PREPARED", endpoint, backup, sourceHash, safetyBackup, "Preventive backup created before restore.");
		final Path mysqlTool = requireTool(endpoint, "mysql");
		final MySqlJdbcAddress address = endpoint.address();
		final List<String> command = baseCommand(mysqlTool, endpoint, address);
		command.add(address.database());
		try
		{
			run(command, endpoint.password(), backup);
			audit("RESTORE_OK", endpoint, backup, sourceHash, safetyBackup, "Restore completed.");
		}
		catch (Exception e)
		{
			audit("RESTORE_FAILED", endpoint, backup, sourceHash, safetyBackup, sanitize(e.getMessage()));
			throw e;
		}
	}
	
	public Path locateDumpTool(DatabaseEndpoint endpoint)
	{
		return MySqlBinaryResolver.resolve(endpoint, "mysqldump");
	}
	
	private static Path requireTool(DatabaseEndpoint endpoint, String tool) throws IOException
	{
		final Path resolved = MySqlBinaryResolver.resolve(endpoint, tool);
		if (resolved == null)
		{
			throw new IOException(tool + " was not found. Configure MySqlBinLocation in Database.ini or add the tool to PATH.");
		}
		
		return resolved;
	}
	
	private static List<String> baseCommand(Path tool, DatabaseEndpoint endpoint, MySqlJdbcAddress address)
	{
		final List<String> command = new ArrayList<>();
		command.add(tool.toString());
		command.add("--host=" + address.host());
		command.add("--port=" + address.port());
		command.add("--user=" + endpoint.user());
		command.add("--protocol=TCP");
		return command;
	}
	
	private void run(List<String> command, String password, Path input) throws Exception
	{
		final Path commandLog = Files.createTempFile(_backupDirectory, ".mysql-dashboard-", ".log");
		try
		{
			final ProcessBuilder builder = new ProcessBuilder(command);
			builder.environment().put("MYSQL_PWD", password == null ? "" : password);
			builder.redirectErrorStream(true);
			builder.redirectOutput(commandLog.toFile());
			if (input != null)
			{
				builder.redirectInput(input.toFile());
			}
			
			final Process process = builder.start();
			if (!process.waitFor(COMMAND_TIMEOUT_MINUTES, TimeUnit.MINUTES))
			{
				process.destroyForcibly();
				throw new IOException("MySQL command timed out.");
			}
			
			if (process.exitValue() != 0)
			{
				final String message = Files.readString(commandLog, StandardCharsets.UTF_8).trim();
				throw new IOException(message.isEmpty() ? "MySQL command failed with exit code " + process.exitValue() + "." : sanitize(message));
			}
		}
		finally
		{
			Files.deleteIfExists(commandLog);
		}
	}
	
	private static String sha256(Path file) throws Exception
	{
		final MessageDigest digest = MessageDigest.getInstance("SHA-256");
		try (DigestInputStream input = new DigestInputStream(Files.newInputStream(file), digest))
		{
			while (input.read(new byte[16384]) != -1)
			{
				// Reading the stream updates the digest through DigestInputStream.
			}
		}
		
		return HexFormat.of().formatHex(digest.digest());
	}
	
	private synchronized void audit(String action, DatabaseEndpoint endpoint, Path artifact, String hash, Path safetyBackup, String result)
	{
		try
		{
			// A path with no parent would throw a NullPointerException past the catch below.
			final Path folder = _auditLog.getParent();
			if (folder != null)
			{
				Files.createDirectories(folder);
			}
			
			try (BufferedWriter writer = Files.newBufferedWriter(_auditLog, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND))
			{
				writer.write(Instant.now() + " | " + action + " | " + endpoint.maskedDescription() + " | source=" + artifact.toAbsolutePath().normalize() + " | sha256=" + hash + " | safetyBackup=" + (safetyBackup == null ? "N/A" : safetyBackup.toAbsolutePath().normalize()) + " | result=" + sanitize(result));
				writer.newLine();
			}
		}
		catch (IOException e)
		{
			// Audit logging failure must not overwrite the authoritative database operation result.
		}
	}
	
	private static String sanitize(String message)
	{
		if (message == null)
		{
			return "";
		}
		
		return message.replace('\r', ' ').replace('\n', ' ').trim();
	}
}
