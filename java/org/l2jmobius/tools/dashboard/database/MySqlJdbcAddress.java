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

/**
 * Represents the editable authority and schema portion of a MySQL JDBC connection string.
 * @param host MySQL server host
 * @param port MySQL server port
 * @param database database schema name
 * @param query JDBC query-string options without the leading question mark
 * @author BazookaRpm
 */
public record MySqlJdbcAddress(String host, int port, String database, String query)
{
	private static final String PREFIX = "jdbc:mysql://";
	private static final int PREFIX_LENGTH = PREFIX.length();
	
	public static MySqlJdbcAddress parse(String jdbcUrl)
	{
		if ((jdbcUrl == null) || !jdbcUrl.regionMatches(true, 0, PREFIX, 0, PREFIX_LENGTH))
		{
			throw new IllegalArgumentException("Expected a MySQL JDBC URL.");
		}
		
		final String body = jdbcUrl.substring(PREFIX_LENGTH);
		final int queryStart = body.indexOf('?');
		final String authorityAndSchema = (queryStart >= 0) ? body.substring(0, queryStart) : body;
		final String options = (queryStart >= 0) ? body.substring(queryStart + 1) : "";
		final int slash = authorityAndSchema.indexOf('/');
		if (slash <= 0)
		{
			throw new IllegalArgumentException("JDBC URL does not contain a database schema.");
		}
		
		final String authority = authorityAndSchema.substring(0, slash);
		final String schema = authorityAndSchema.substring(slash + 1);
		final int colon = authority.lastIndexOf(':');
		final String parsedHost = (colon > 0) ? authority.substring(0, colon) : authority;
		final int parsedPort = (colon > 0) ? Integer.parseInt(authority.substring(colon + 1)) : 3306;
		if (parsedHost.isBlank() || schema.isBlank())
		{
			throw new IllegalArgumentException("JDBC host and schema must not be empty.");
		}
		
		return new MySqlJdbcAddress(parsedHost, parsedPort, schema, options);
	}
	
	public String rebuild(String targetHost, int targetPort, String targetDatabase)
	{
		final StringBuilder result = new StringBuilder(PREFIX_LENGTH + targetHost.length() + targetDatabase.length() + query.length() + 16);
		result.append(PREFIX).append(targetHost).append(':').append(targetPort).append('/').append(targetDatabase);
		if (!query.isEmpty())
		{
			result.append('?').append(query);
		}
		
		return result.toString();
	}
}
