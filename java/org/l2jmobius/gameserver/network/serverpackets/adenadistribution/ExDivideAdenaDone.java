/*
 * This file is part of the L2J Mobius project.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.l2jmobius.gameserver.network.serverpackets.adenadistribution;

import org.l2jmobius.commons.network.buffer.WriteBuffer;
import org.l2jmobius.gameserver.network.GameClient;
import org.l2jmobius.gameserver.network.ServerPackets;
import org.l2jmobius.gameserver.network.serverpackets.ServerPacket;

/**
 * This packet is sent to the client to notify that the {@code ExDivideAdena} process has finished.<br>
 * It informs the user that their adena distribution is complete.
 * @author Sdw
 */
public class ExDivideAdenaDone extends ServerPacket
{
	private final boolean _isPartyLeader;
	private final boolean _isCCLeader;
	private final long _adenaCount;
	private final long _distributedAdenaCount;
	private final int _memberCount;
	private final String _distributorName;
	
	/**
	 * This constructor initializes the {@code ExDivideAdenaDone} packet.<br>
	 * It stores details about a completed adena distribution event.
	 * @param isPartyLeader Indicates if the player is the party leader.
	 * @param isCCLeader Indicates if the player is the Clan Castle leader.
	 * @param adenaCount The total amount of adena available for distribution.
	 * @param distributedAdenaCount The total amount of adena that was successfully distributed.
	 * @param memberCount The number of members who received adena.
	 * @param distributorName The name of the player who distributed the adena.
	 */
	public ExDivideAdenaDone(boolean isPartyLeader, boolean isCCLeader, long adenaCount, long distributedAdenaCount, int memberCount, String distributorName)
	{
		_isPartyLeader = isPartyLeader;
		_isCCLeader = isCCLeader;
		_adenaCount = adenaCount;
		_distributedAdenaCount = distributedAdenaCount;
		_memberCount = memberCount;
		_distributorName = distributorName;
	}
	
	@Override
	public void writeImpl(GameClient client, WriteBuffer buffer)
	{
		ServerPackets.EX_DIVIDE_ADENA_DONE.writeId(this, buffer);
		buffer.writeByte(_isPartyLeader);
		buffer.writeByte(_isCCLeader);
		buffer.writeInt(_memberCount);
		buffer.writeLong(_distributedAdenaCount);
		buffer.writeLong(_adenaCount);
		buffer.writeString(_distributorName);
	}
}
