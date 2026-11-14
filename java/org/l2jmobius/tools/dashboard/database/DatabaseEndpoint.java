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

import java.nio.file.Path;

/**
 * Holds one database configuration loaded from a Interlude Database.ini file.
 * @param sourceFile source Database.ini file
 * @param driver configured JDBC driver class
 * @param jdbcUrl configured JDBC connection URL
 * @param user database user name
 * @param password database password
 * @param mysqlBinDirectory optional MySQL binary directory
 * @author BazookaRpm
 */
public record DatabaseEndpoint(Path sourceFile, String driver, String jdbcUrl, String user, String password, String mysqlBinDirectory)
{
	public MySqlJdbcAddress address()
	{
		return MySqlJdbcAddress.parse(jdbcUrl);
	}
	
	public String maskedDescription()
	{
		final MySqlJdbcAddress parsed = address();
		return parsed.host() + ":" + parsed.port() + "/" + parsed.database() + " as " + user;
	}
}
