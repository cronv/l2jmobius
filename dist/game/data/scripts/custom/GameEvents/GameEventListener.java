package custom.GameEvents;

/** Observer. default-методы: реализуй только то, что нужно (stats, анонсы, discord-webhook...). */
public interface GameEventListener
{
	default void onStateChanged(IGameEvent event, EventState oldState, EventState newState)
	{
	}

	default void onParticipantsChanged(IGameEvent event)
	{
	}
}
