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
		switch(setting.toLowerCase())
		{
		case "true":
		case "false":
		case "yes":
		case "no":
		case "t":
		case "f":
		case "y":
		case "n":
			return true;
		default:
			return false;
		}
	}
	
	public static boolean parseSetting(String setting, boolean defaultValue)
	{
		switch(setting.toLowerCase())
		{
		case "true":
		case "yes":
		case "t":
		case "y":
			return true;
		case "false":
		case "no":
		case "f":
		case "n":
			return false;
		default:
			return defaultValue;
		}
	}
}
