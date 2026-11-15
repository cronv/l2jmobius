package custom.GameEvents;

import org.l2jmobius.gameserver.entity.actor.Player;

/** Публичный контракт. Доска и сторонние системы зависят только от него. */
public interface IGameEvent
{
	GameEventType getType();

	String getDisplayName();

	EventState getState();

	/** true, пока ивент "действующий": открыта регистрация или он идёт. */
	boolean isActive();

	int getParticipantCount();

	int getMinParticipants();

	int getMaxParticipants();

	int getMinLevel();

	int getMaxLevel();

	boolean isRegistered(int objectId);

	boolean register(Player player);

	boolean unregister(Player player);
}
