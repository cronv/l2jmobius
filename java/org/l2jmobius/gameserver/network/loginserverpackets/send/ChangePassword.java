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
package org.l2jmobius.gameserver.network.loginserverpackets.send;

import org.l2jmobius.commons.network.packet.SimpleWritablePacket;

/**
 * Sends a request to the login server to change an account password.<br>
 * This packet handles the transition from {@code oldPass} to {@code newPass}.<br>
 * It uses opcode {@code 0x0B} and requires the account and character identifiers.
 * @author BazookaRpm
 */
public class ChangePassword extends SimpleWritablePacket
{
	// Opcode.
	private static final int OPCODE = 0x0B;
	
	// Data.
	private final String _accountName;
	private final String _characterName;
	private final String _oldPass;
	private final String _newPass;
	
	/**
	 * Creates a new {@code ChangePassword} packet.<br>
	 * This method handles the password update request for a specific account.<br>
	 * It ensures that all input strings are not {@code null}.
	 * @param accountName The name of the user account.
	 * @param characterName The name of the character.
	 * @param oldPass The current password.
	 * @param newPass The new password to be set.
	 */
	public ChangePassword(String accountName, String characterName, String oldPass, String newPass)
	{
		_accountName = (accountName != null) ? accountName : "";
		_characterName = (characterName != null) ? characterName : "";
		_oldPass = (oldPass != null) ? oldPass : "";
		_newPass = (newPass != null) ? newPass : "";
	}
	
	/**
	 * Writes the packet data to the output stream.<br>
	 * This method serializes the opcode and all required fields.
	 */
	@Override
	public void write()
	{
		writeByte(OPCODE);
		writeString(_accountName);
		writeString(_characterName);
		writeString(_oldPass);
		writeString(_newPass);
	}
}
