package custom.GameEvents;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Supplier;

import custom.GameEvents.events.TvTAdapter;

public enum GameEventType
{
	TEAM_VS_TEAM("Team vs Team", TvTAdapter::new),
	;

	private static final Map<String, GameEventType> BY_NAME = new HashMap<>();
	static
	{
		for (GameEventType type : values())
		{
			BY_NAME.put(type.name().toLowerCase(Locale.ROOT), type);
		}
	}

	private final String _displayName;
	private final Supplier<? extends IGameEvent> _factory;

	GameEventType(String displayName, Supplier<? extends IGameEvent> factory)
	{
		_displayName = displayName;
		_factory = factory;
	}

	public String getDisplayName()
	{
		return _displayName;
	}

	public IGameEvent create()
	{
		return _factory.get();
	}

	public static GameEventType fromName(String name)
	{
		return (name == null) ? null : BY_NAME.get(name.toLowerCase(Locale.ROOT));
	}
}
