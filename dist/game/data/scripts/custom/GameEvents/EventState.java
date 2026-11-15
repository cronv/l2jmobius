package custom.GameEvents;

public enum EventState
{
	INACTIVE("Closed"),
	REGISTERING("Registration"),
	RUNNING("Running"),
	FINISHED("Finished");

	private final String _title;

	EventState(String title)
	{
		_title = title;
	}

	public String getTitle()
	{
		return _title;
	}
}
