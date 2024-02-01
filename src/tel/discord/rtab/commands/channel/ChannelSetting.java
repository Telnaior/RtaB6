package tel.discord.rtab.commands.channel;

public enum ChannelSetting
{
	RESULT_CHANNEL("ResultChannel",2,"null")
	{
		//This one will accept longs, not just ints, and will also accept null
		//If they put in a number that doesn't actually match a channel things won't break, so we don't have to check that here
		@Override
		boolean isValidSetting(String newString)
		{
			if(newString.equalsIgnoreCase("null"))
				return true;
			try
			{
				Long.parseLong(newString);
				return true;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	BASE_MULTIPLIER("BaseMultiplier",3,"1/1")
	{
		//Base multiplier will accept fractions
		@Override
		boolean isValidSetting(String newString)
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
	BOT_COUNT("BotCount",4,"80")
	{
		//Bot count will accept any integer 0-80 (higher counts can be set manually if a larger botlist is supplied)
		@Override
		boolean isValidSetting(String newString)
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
	},
	DEMO_TIMER("DemoTime",5,"60")
	{
		//Demo timer will accept any non-negative integer
		@Override
		boolean isValidSetting(String newString)
		{
			try
			{
				int demoTimer = Integer.parseInt(newString);
				return demoTimer >= 0;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	MIN_PLAYERS("MinPlayers",6,"2")
	{
		//Min players will accept any integer 2-16
		@Override
		boolean isValidSetting(String newString)
		{
			try
			{
				int minPlayers = Integer.parseInt(newString);
				return minPlayers >= 2 && minPlayers <= 16;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	MAX_PLAYERS("MaxPlayers",7,"16")
	{
		//Max players will accept any integer 2-16
		@Override
		boolean isValidSetting(String newString)
		{
			try
			{
				int maxPlayers = Integer.parseInt(newString);
				return maxPlayers >= 2 && maxPlayers <= 16;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	MAX_LIVES("MaxLives",8,"5")
	{
		//Max lives will accept any positive integer
		@Override
		boolean isValidSetting(String newString)
		{
			try
			{
				int maxLives = Integer.parseInt(newString);
				return maxLives > 0;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	LIFE_PENALTY("LifePenalty",9,"3")
	{
		//0-4 here
		@Override
		boolean isValidSetting(String newString)
		{
			try
			{
				int lifePenalty = Integer.parseInt(newString);
				return lifePenalty >= 0 && lifePenalty <= 4;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	VERBOSE_BOT_GAMES("VerboseBotMinigames",10,"false")
	{
		//It's a boolean setting
		@Override
		boolean isValidSetting(String newString)
		{
			return BooleanSetting.checkValidSetting(newString);
		}
	},
	DO_BONUS_GAMES("DoBonusGames",11,"true")
	{
		//It's a boolean setting
		@Override
		boolean isValidSetting(String newString)
		{
			return BooleanSetting.checkValidSetting(newString);
		}
	},
	CHANNEL_COUNTS_TO_PLAYER_LEVEL("CountsToPlayerLevel",12,"false")
	{
		//It's a boolean setting
		@Override
		boolean isValidSetting(String newString)
		{
			return BooleanSetting.checkValidSetting(newString);
		}
	},
	NEWBIE_PROTECTION("NewbieProtection",13,"10")
	{
		//0 to anything
		@Override
		boolean isValidSetting(String newString)
		{
			try
			{
				int newbieProtection = Integer.parseInt(newString);
				return newbieProtection >= 0;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	LIVES_PER_ENHANCE("EnhancementLivesNeeded",14,"25")
	{
		//1 to anything
		@Override
		boolean isValidSetting(String newString)
		{
			try
			{
				int livesPerEnhance = Integer.parseInt(newString);
				return livesPerEnhance >= 1;
			}
			catch(NumberFormatException e1)
			{
				return false;
			}
		}
	},
	TURBO_TIMERS("TurboTimers",15,"false")
	{
		//It's a boolean setting
		@Override
		boolean isValidSetting(String newString)
		{
			return BooleanSetting.checkValidSetting(newString);
		}
	};
	
	final String settingName;
	final int recordLocation;
	final String defaultValue;
	
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
	boolean isValidSetting(String newSetting)
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
