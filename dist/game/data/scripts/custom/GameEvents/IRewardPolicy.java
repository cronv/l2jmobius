package custom.GameEvents;

import org.l2jmobius.gameserver.entity.actor.Player;

@FunctionalInterface
public interface IRewardPolicy
{
	void reward(Player player, IGameEvent event, int place);

	/** Null Object: ивент без наград, без null-проверок. */
	IRewardPolicy NONE = (player, event, place) ->
	{
	};
}
