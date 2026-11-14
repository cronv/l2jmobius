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

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.entity.actor.Creature;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;

/**
 * This packet is used to send special camera instructions to the client.<br>
 * It allows the server to control specific camera behaviors for a {@code Creature}.
 */
public class SpecialCamera extends ServerPacket
{
	private final int _id;
	private final int _force;
	private final int _angle1;
	private final int _angle2;
	private final int _time;
	private final int _duration;
	private final int _relYaw;
	private final int _relPitch;
	private final int _isWide;
	private final int _relAngle;
	private final int _unk;
	
	/**
	 * Creates a new {@code SpecialCamera} packet.<br>
	 * This constructor initializes the camera settings for a specific creature.<br>
	 * It uses a default value of {@code 0} for the unknown parameter.
	 * @param creature The {@code Creature} associated with this camera effect.
	 * @param force The force value applied to the camera.
	 * @param angle1 The first angle for the camera orientation.
	 * @param angle2 The second angle for the camera orientation.
	 * @param time The starting time for the effect.
	 * @param range The distance range of the camera.
	 * @param duration How long the camera effect lasts.
	 * @param relYaw The relative yaw rotation.
	 * @param relPitch The relative pitch rotation.
	 * @param isWide A flag indicating if the camera view is wide.
	 * @param relAngle The relative angle for the camera.
	 */
	public SpecialCamera(Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		this(creature, force, angle1, angle2, time, duration, range, relYaw, relPitch, isWide, relAngle, 0);
	}
	
	/**
	 * Creates a new {@code SpecialCamera} packet for a specific creature.<br>
	 * This constructor is used when a talker is involved in the camera effect.
	 * @param creature The {@code Creature} that will be the focus of the camera.
	 * @param talker The {@code Creature} who initiates the camera action.
	 * @param force The intensity or strength of the camera movement.
	 * @param angle1 The first target angle for the camera.
	 * @param angle2 The second target angle for the camera.
	 * @param time The starting time for the animation.
	 * @param duration The total length of the camera effect in milliseconds.
	 * @param relYaw The relative yaw rotation value.
	 * @param relPitch The relative pitch rotation value.
	 * @param isWide A boolean-style integer indicating if the camera view is wide.
	 * @param relAngle The relative angle for the camera positioning.
	 */
	public SpecialCamera(Creature creature, Creature talker, int force, int angle1, int angle2, int time, int duration, int relYaw, int relPitch, int isWide, int relAngle)
	{
		this(creature, force, angle1, angle2, time, duration, 0, relYaw, relPitch, isWide, relAngle, 0);
	}
	
	/**
	 * Constructs a new {@code SpecialCamera} packet.<br>
	 * This packet defines specific camera movements for a creature.
	 * @param creature The {@code Creature} associated with the camera effect.
	 * @param force The force value of the camera movement.
	 * @param angle1 The first angle for the camera rotation.
	 * @param angle2 The second angle for the camera rotation.
	 * @param time The starting time for the animation.
	 * @param range The distance range for the camera.
	 * @param duration The total length of the camera effect in milliseconds.
	 * @param relYaw The relative yaw value.
	 * @param relPitch The relative pitch value.
	 * @param isWide A boolean-like integer indicating if the view is wide.
	 * @param relAngle The relative angle for the movement.
	 * @param unk An unknown placeholder value.
	 */
	public SpecialCamera(Creature creature, int force, int angle1, int angle2, int time, int range, int duration, int relYaw, int relPitch, int isWide, int relAngle, int unk)
	{
		_id = creature.getObjectId();
		_force = force;
		_angle1 = angle1;
		_angle2 = angle2;
		_time = time;
		_duration = duration;
		_relYaw = relYaw;
		_relPitch = relPitch;
		_isWide = isWide;
		_relAngle = relAngle;
		_unk = unk;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.SPECIAL_CAMERA.writeId(this, buffer);
		buffer.writeInt(_id);
		buffer.writeInt(_force);
		buffer.writeInt(_angle1);
		buffer.writeInt(_angle2);
		buffer.writeInt(_time);
		buffer.writeInt(_duration);
		buffer.writeInt(_relYaw);
		buffer.writeInt(_relPitch);
		buffer.writeInt(_isWide);
		buffer.writeInt(_relAngle);
		buffer.writeInt(_unk);
	}
}
