package tel.discord.rtab.commands.channel;

public final class BooleanSetting
{
	//Private constructor to prevent instantiation
	private BooleanSetting() 
	{
	    throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated.");
	}
	
	public static boolean checkValidSetting(String setting)
	{
		return switch (setting.toLowerCase()) {
			case "true", "false", "yes", "no", "on", "off", "t", "f", "y", "n" -> true;
			default -> false;
		};
	}
	
	public static boolean parseSetting(String setting, boolean defaultValue)
	{
		return switch (setting.toLowerCase()) {
			case "true", "yes", "on", "t", "y" -> true;
			case "false", "no", "off", "f", "n" -> false;
			default -> defaultValue;
		};
	}
}
