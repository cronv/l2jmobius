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
package org.l2jmobius.gameserver.network.serverpackets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.config.custom.MultilingualSupportConfig;
import org.l2jmobius.gameserver.entity.actor.Player;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.NpcStringId;
import org.l2jmobius.gameserver.network.NpcStringId.NSLocalisation;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.SystemMessageId;
import org.l2jmobius.gameserver.network.SystemMessageId.SMLocalisation;

/**
 * This packet is used to display a specific message on the player's screen.<br>
 * It handles the transmission of localized text and associated data to the client.
 * @author Kerberos, Enryu
 */
public class ExShowScreenMessage extends ServerPacket
{
	// Positions
	public static final byte TOP_LEFT = 1;
	public static final byte TOP_CENTER = 2;
	public static final byte TOP_CENTER_2 = 11;
	public static final byte TOP_RIGHT = 3;
	public static final byte MIDDLE_LEFT = 4;
	public static final byte MIDDLE_CENTER = 5;
	public static final byte MIDDLE_RIGHT = 6;
	public static final byte BOTTOM_CENTER = 7;
	public static final byte BOTTOM_RIGHT = 8;
	
	// Colors (client maps these in OnScreenMessage1Wnd - EV_ShowScreenMessage)
	public static final int COLOR_WHITE = 1;
	public static final int COLOR_GREY = 2;
	public static final int COLOR_BLUE = 3;
	public static final int COLOR_RED = 4;
	public static final int COLOR_YELLOW = 5;
	
	private final int _type;
	private final int _sysMessageId;
	private final int _unk1;
	private final int _textColor;
	private final int _unk2;
	private final boolean _fade;
	private final int _size;
	private final int _position;
	private final boolean _effect;
	private final String _text;
	private final int _time;
	private final int _npcString;
	private List<String> _parameters;
	
	/**
	 * Creates a new message to be displayed on the player screen.<br>
	 * This constructor sets the default position to {@code TOP_CENTER}.
	 * @param text The content of the message to display.
	 * @param time The duration in milliseconds that the message remains visible.
	 */
	public ExShowScreenMessage(String text, int time)
	{
		_type = 2;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = 0;
		_unk2 = 0;
		_fade = false;
		_position = TOP_CENTER;
		_text = text;
		_time = time;
		_size = 0;
		_effect = false;
		_npcString = -1;
	}
	
	/**
	 * Creates a new screen message with a specific position.<br>
	 * This constructor sets the default display properties for the message.
	 * @param text The content to be displayed on the screen.
	 * @param position The screen coordinate where the message will appear.
	 * @param time The duration in milliseconds that the message remains visible.
	 */
	public ExShowScreenMessage(String text, int position, int time)
	{
		_type = 2;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = 0;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = text;
		_time = time;
		_size = 0;
		_effect = false;
		_npcString = -1;
	}
	
	/**
	 * Creates a new screen message with specific visual properties.<br>
	 * This constructor allows for detailed control over the display appearance.
	 * @param text The content to be displayed on the screen.
	 * @param position The screen coordinate where the message will appear.
	 * @param time The duration in milliseconds that the message remains visible.
	 * @param textColor
	 */
	public ExShowScreenMessage(String text, int position, int time, int textColor)
	{
		_type = 2;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = textColor;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = text;
		_time = time;
		_size = 0;
		_effect = false;
		_npcString = -1;
	}
	
	/**
	 * Creates a new screen message with specific visual properties.<br>
	 * This constructor allows for detailed control over the display appearance.
	 * @param text The content to be displayed on the screen.
	 * @param position The screen coordinate where the message will appear.
	 * @param time The duration in milliseconds that the message remains visible.
	 * @param size The font size of the displayed text.
	 * @param fade Whether the message should use a fading animation.
	 * @param showEffect Whether special visual effects should be applied to the message.
	 */
	public ExShowScreenMessage(String text, int position, int time, int size, boolean fade, boolean showEffect)
	{
		_type = 1;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = 0;
		_unk2 = 0;
		_fade = fade;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
		_npcString = -1;
	}
	
	/**
	 * Creates a screen message using an {@code NpcStringId}.<br>
	 * This method sets the display position and duration.<br>
	 * It also handles any additional string parameters provided.
	 * @param text
	 * @param position The screen position where the message will appear.
	 * @param time The amount of time in milliseconds to show the message.
	 * @param size
	 * @param fade
	 * @param showEffect
	 * @param textColor
	 */
	public ExShowScreenMessage(String text, int position, int time, int size, boolean fade, boolean showEffect, int textColor)
	{
		_type = 1;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = textColor;
		_unk2 = 0;
		_fade = fade;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
		_npcString = -1;
	}
	
	/**
	 * Creates a screen message using an {@code NpcStringId}.<br>
	 * This method sets the display position and duration.<br>
	 * It also handles any additional string parameters provided.
	 * @param npcString The unique identifier for the NPC string to display.
	 * @param position The screen position where the message will appear.
	 * @param time The amount of time in milliseconds to show the message.
	 * @param params Variable arguments used to fill placeholders in the string.
	 */
	public ExShowScreenMessage(NpcStringId npcString, int position, int time, String... params)
	{
		_type = 2;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = 0;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0;
		_effect = false;
		_npcString = npcString.getId();
		if (params != null)
		{
			addStringParameter(params);
		}
	}
	
	/**
	 * Creates a new screen message using a {@code SystemMessageId}.<br>
	 * This method sets the message type to {@code 2}.<br>
	 * It handles the inclusion of optional string parameters.
	 * @param npcString
	 * @param position The screen position where the message will appear.
	 * @param time The duration in milliseconds that the message remains visible.
	 * @param textColor
	 * @param params Variable arguments to be inserted into the message text.
	 */
	public ExShowScreenMessage(NpcStringId npcString, int position, int time, int textColor, String... params)
	{
		_type = 2;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = textColor;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0;
		_effect = false;
		_npcString = npcString.getId();
		if (params != null)
		{
			addStringParameter(params);
		}
	}
	
	/**
	 * Creates a new screen message using a {@code SystemMessageId}.<br>
	 * This method sets the message type to {@code 2}.<br>
	 * It handles the inclusion of optional string parameters.
	 * @param systemMsg The unique identifier for the system message.
	 * @param position The screen position where the message will appear.
	 * @param time The duration in milliseconds that the message remains visible.
	 * @param params Variable arguments to be inserted into the message text.
	 */
	public ExShowScreenMessage(SystemMessageId systemMsg, int position, int time, String... params)
	{
		_type = 2;
		_sysMessageId = systemMsg.getId();
		_unk1 = 0;
		_textColor = 0;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0;
		_effect = false;
		_npcString = -1;
		if (params != null)
		{
			addStringParameter(params);
		}
	}
	
	/**
	 * Creates a screen message using an {@code NpcStringId}.<br>
	 * This method sets the display position and duration.<br>
	 * It also handles optional visual effects and extra parameters.
	 * @param systemMsg
	 * @param position The screen position where the message will appear.
	 * @param time The amount of time in milliseconds to show the message.
	 * @param textColor
	 * @param params Variable arguments used to fill placeholders in the NPC string.
	 */
	public ExShowScreenMessage(SystemMessageId systemMsg, int position, int time, int textColor, String... params)
	{
		_type = 2;
		_sysMessageId = systemMsg.getId();
		_unk1 = 0;
		_textColor = textColor;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0;
		_effect = false;
		_npcString = -1;
		if (params != null)
		{
			addStringParameter(params);
		}
	}
	
	/**
	 * Creates a screen message using an {@code NpcStringId}.<br>
	 * This method sets the display position and duration.<br>
	 * It also handles optional visual effects and extra parameters.
	 * @param npcString The unique identifier for the NPC string to display.
	 * @param position The screen position where the message will appear.
	 * @param time The amount of time in milliseconds to show the message.
	 * @param showEffect Whether a special effect should be shown with the message.
	 * @param params Variable arguments used to fill placeholders in the NPC string.
	 */
	public ExShowScreenMessage(NpcStringId npcString, int position, int time, boolean showEffect, String... params)
	{
		_type = 2;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = 0;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0;
		_effect = showEffect;
		_npcString = npcString.getId();
		if (params != null)
		{
			addStringParameter(params);
		}
	}
	
	/**
	 * Constructs a new {@code ExShowScreenMessage} with all specific configuration details.<br>
	 * This constructor allows for precise control over the message appearance and behavior.
	 * @param position The screen coordinate where the message will appear.
	 * @param showEffect Whether to trigger a visual effect when showing the message.
	 * @param time The duration in milliseconds that the message remains on screen.
	 * @param npcString The {@code NpcStringId} associated with the message.
	 * @param textColor
	 * @param params Additional parameters for formatting the message.
	 */
	public ExShowScreenMessage(NpcStringId npcString, int position, int time, boolean showEffect, int textColor, String... params)
	{
		_type = 2;
		_sysMessageId = -1;
		_unk1 = 0;
		_textColor = textColor;
		_unk2 = 0;
		_fade = false;
		_position = position;
		_text = null;
		_time = time;
		_size = 0;
		_effect = showEffect;
		_npcString = npcString.getId();
		if (params != null)
		{
			addStringParameter(params);
		}
	}
	
	/**
	 * Display a Text, System Message or a NPC String on the screen for the given parameters.
	 * @param type 0 - System Message, 1 - Text, 2 - NPC String
	 * @param messageId the System Message Id
	 * @param position the position on the screen
	 * @param unk1
	 * @param size the font size 0 - normal, 1 - small
	 * @param textColor the text color, see COLOR_* constants
	 * @param unk2
	 * @param showEffect upper effect (0 - disabled, 1 enabled) - _position must be 2 (center) otherwise no effect
	 * @param time the display time
	 * @param fade the fade effect (0 - disabled, 1 enabled)
	 * @param text the text to display
	 * @param npcString
	 * @param params the String parameters
	 */
	public ExShowScreenMessage(int type, int messageId, int position, int unk1, int size, int textColor, int unk2, boolean showEffect, int time, boolean fade, String text, NpcStringId npcString, String params)
	{
		_type = type;
		_sysMessageId = messageId;
		_unk1 = unk1;
		_textColor = textColor;
		_unk2 = unk2;
		_fade = fade;
		_position = position;
		_text = text;
		_time = time;
		_size = size;
		_effect = showEffect;
		_npcString = npcString.getId();
	}
	
	/**
	 * Adds multiple string parameters to the message.<br>
	 * These strings are used to fill placeholders in localized text.
	 * @param params a variable number of {@code String} values to add
	 */
	public void addStringParameter(String... params)
	{
		if (_parameters == null)
		{
			_parameters = new ArrayList<>();
		}
		
		for (String param : params)
		{
			_parameters.add(param);
		}
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_SHOW_SCREEN_MESSAGE.writeId(this, buffer);
		
		// Localisation related.
		if (MultilingualSupportConfig.MULTILANG_ENABLE)
		{
			final Player player = client.getPlayer();
			if (player != null)
			{
				final String lang = player.getLang();
				if ((lang != null) && !lang.equals("en"))
				{
					if (_sysMessageId > -1)
					{
						final SystemMessageId sm = SystemMessageId.getSystemMessageId(_sysMessageId);
						if (sm != null)
						{
							final SMLocalisation sml = sm.getLocalisation(lang);
							if (sml != null)
							{
								buffer.writeInt(_type);
								buffer.writeInt(-1);
								buffer.writeInt(_position);
								buffer.writeInt(_unk1);
								buffer.writeInt(_size);
								buffer.writeInt(_textColor);
								buffer.writeInt(_unk2);
								buffer.writeInt(_effect);
								buffer.writeInt(_time);
								buffer.writeInt(_fade);
								buffer.writeInt(-1);
								buffer.writeString(sml.getLocalisation(_parameters != null ? _parameters : Collections.emptyList()));
								return;
							}
						}
					}
					else if (_npcString > -1)
					{
						final NpcStringId ns = NpcStringId.getNpcStringId(_npcString);
						if (ns != null)
						{
							final NSLocalisation nsl = ns.getLocalisation(lang);
							if (nsl != null)
							{
								buffer.writeInt(_type);
								buffer.writeInt(-1);
								buffer.writeInt(_position);
								buffer.writeInt(_unk1);
								buffer.writeInt(_size);
								buffer.writeInt(_textColor);
								buffer.writeInt(_unk2);
								buffer.writeInt(_effect);
								buffer.writeInt(_time);
								buffer.writeInt(_fade);
								buffer.writeInt(-1);
								buffer.writeString(nsl.getLocalisation(_parameters != null ? _parameters : Collections.emptyList()));
								return;
							}
						}
					}
				}
			}
		}
		
		buffer.writeInt(_type);
		buffer.writeInt(_sysMessageId);
		buffer.writeInt(_position);
		buffer.writeInt(_unk1);
		buffer.writeInt(_size);
		buffer.writeInt(_textColor);
		buffer.writeInt(_unk2);
		buffer.writeInt(_effect);
		buffer.writeInt(_time);
		buffer.writeInt(_fade);
		buffer.writeInt(_npcString);
		if (_npcString == -1)
		{
			buffer.writeString(_text);
		}
		else if (_parameters != null)
		{
			for (String s : _parameters)
			{
				buffer.writeString(s);
			}
		}
	}
}
