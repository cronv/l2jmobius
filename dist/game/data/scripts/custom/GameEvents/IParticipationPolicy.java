package custom.GameEvents;

import org.l2jmobius.gameserver.entity.actor.Player;

@FunctionalInterface
public interface IParticipationPolicy
{
	/** @return null если участие разрешено, иначе текст отказа игроку. */
	String check(Player player, IGameEvent event);

	IParticipationPolicy DEFAULT = (player, event) ->
	{
		if ((player.getLevel() < event.getMinLevel()) || (player.getLevel() > event.getMaxLevel()))
		{
			return "Your level does not fit this event (" + event.getMinLevel() + "-" + event.getMaxLevel() + ").";
		}
		if (player.isDead())
		{
			return "You are dead.";
		}
		if (player.isInOlympiadMode())
		{
			return "You are in the Olympiad.";
		}
		if (player.isInDuel())
		{
			return "You are in a duel.";
		}
		if (player.isInCombat())
		{
			return "You are in combat.";
		}
		if (player.isInStoreMode())
		{
			return "Close your private store first.";
		}
		if (player.getInstanceId() != 0)
		{
			return "Leave the instance zone first.";
		}
		return null;
	};
}
