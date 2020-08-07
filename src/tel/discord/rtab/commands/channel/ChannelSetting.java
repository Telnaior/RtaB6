package tel.discord.rtab.commands.channel;

public enum ChannelSetting
{
	
	BASE_MULTIPLIER("BaseMultiplier",2,"1/1")
	{
		//Base multiplier will accept fractions
		@Override
		public boolean isValidSetting(String newString)
		{
			String[] settingHalves = newString.split("/");
			//If there are too many parts to the fraction, it's bad
			if(settingHalves.length > 2)
				return false;
			//Check each part individually, and make sure they're both positive
			try
			{
				int numerator = 0;
				int denominator = 0;
				numerator = Integer.parseInt(settingHalves[0]);
				if(settingHalves.length == 2)
				{
					denominator = Integer.parseInt(settingHalves[1]);
					return numerator > 0 && denominator > 0;
				}
				return numerator > 0;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	BOTCOUNT("BotCount",3,"80")
	{
		//Bot count will accept any number 0-80 (higher counts can be set manually if a larger botlist is supplied)
		@Override
		public boolean isValidSetting(String newString)
		{
			try
			{
				int botCount = Integer.parseInt(newString);
				return botCount >= 0 && botCount <= 80;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	};
	
	String settingName;
	int recordLocation;
	String defaultValue;
	
	ChannelSetting(String settingName, int recordLocation, String defaultValue)
	{
		this.settingName = settingName;
		this.recordLocation = recordLocation;
		this.defaultValue = defaultValue;
	}
	
	public String getName()
	{
		return settingName;
	}
	public int getLocation()
	{
		return recordLocation;
	}
	public String getDefault()
	{
		return defaultValue;
	}
	//Most settings will accept any number
	public boolean isValidSetting(String newSetting)
	{
		try
		{
			//If this doesn't throw an exception we're good
			Integer.parseInt(newSetting);
			return true;
		}
		catch(NumberFormatException e1)
		{
			return false;
		}
	}
}
