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
package org.l2jmobius.gameserver.network.serverpackets.captcha;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.mechanics.captcha.Captcha;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to deliver a captcha image.<br>
 * It is used as part of the anti-bot system to verify human interaction.<br>
 * The {@code ReceiveBotCaptchaImage} class handles the transmission of the required visual data.
 */
public class ReceiveBotCaptchaImage extends ServerPacket
{
	private final Captcha _captcha;
	private final int _time;
	
	/**
	 * Sends a captcha image to the client.<br>
	 * This packet includes the specific {@code Captcha} data.<br>
	 * It also sets a timeout value for the user.
	 * @param captcha The {@code Captcha} object containing the image and details.
	 * @param time The amount of time allowed to complete the captcha.
	 */
	public ReceiveBotCaptchaImage(Captcha captcha, int time)
	{
		_captcha = captcha;
		_time = time;
	}
	
	@Override
	protected void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_CAPTCHA_IMAGE.writeId(this, buffer);
		buffer.writeLong(_captcha.getId());
		buffer.writeByte(2); // unk
		buffer.writeInt(_time);
		buffer.writeBytes(_captcha.getData());
	}
}
