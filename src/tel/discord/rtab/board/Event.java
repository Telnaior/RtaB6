package tel.discord.rtab.board;

public enum Event implements WeightedSpace
{
	STREAKPSMALL	( 7,"Streak Bonus"),
	BOOST_CHARGER	( 7,"Boost Charger"),
	DOUBLE_DEAL		( 7,"Double Deal"),
	DRAW_TWO		( 6,"Draw Two"),
	PEEK_REPLENISH	( 6,"Extra Peek"),
	REVERSE_ORDER	( 6,"Reverse"),
	STREAKPLARGE	( 5,"Streak Bonus"),
	BOWSER			( 5,"Bowser Event"),
	SPOILER_TAG		( 5,"Hidden Commands for All")
	{
		@Override
		public int getWeight(int playerCount)
		{
			//This needs to be less common the bigger the game is
			switch(playerCount)
			{
			case 16:
			case 15:
			case 14:
				return 1;
			case 13:
			case 12:
			case 11:
				return 2;
			case 10:
			case 9:
			case 8:
				return 3;
			case 7:
			case 6:
			case 5:
				return 4;
			default:
				return weight;
			}
		}
	},
	BOOST_MAGNET	( 4,"Boost Magnet"),
	JOKER			( 4,"Joker")
	{
		@Override
		public int getWeight(int playerCount)
		{
			//Jokers don't belong in 2p, and have reduced frequency in 3p
			switch(playerCount)
			{
			case 2:
				return 0;
			case 3:
				return 1;
			default:
				return weight;
			}
		}
	},
	DRAW_FOUR		( 4,"Draw Four")
	{
		@Override
		public int getWeight(int playerCount)
		{
			//This space would be a little too painful in a small game.
			switch(playerCount)
			{
			case 2:
				return 1;
			case 3:
				return 2;
			default:
				return weight;
			}
		}
	},
	MINEFIELD		( 3,"Minefield"),
	BRIBE			( 3,"Ejector Seat"),
	SPLIT_SHARE		( 3,"Split & Share"),
	LOCKDOWN		( 2,"Triple Deal Lockdown"),
	BLAMMO_FRENZY	( 2,"Blammo Frenzy"),
	END_ROUND		( 2,"Final Countdown"),
	SUPER_JOKER		( 1,"Midas Touch")
	{
		@Override
		public int getWeight(int playerCount)
		{
			//Super Jokers don't belong in small games
			return (playerCount < 4) ? 0 : weight;
		}
	},
	STARMAN			( 1,"Starman"),
	JACKPOT			( 1,"Jackpot"),
	//Events Rotated Out
	SKIP_TURN		( 0,"Skip Turn"), //This still appears in 2p from reverse autoconverting lol
	BOOST_DRAIN		( 0,"Boost Drain"),
	//Bowser Events
	COINS_FOR_BOWSER( 0,"Cash for Bowser"),
	BOWSER_POTLUCK	( 0,"Bowser's Cash Potluck"),
	RUNAWAY			( 0,"Billion-Dollar Present"),
	BOWSER_JACKPOT	( 0,"Bowser's Jackpot"),
	COMMUNISM		( 0,"Bowser Revolution");

	int weight;
	String name;
	Event(int valueWeight, String eventName)
	{
		weight = valueWeight;
		name = eventName;
	}
	@Override
	public int getWeight(int playerCount)
	{
		//This gets overriden by a few events that don't belong in small or large games
		return weight;
	}
	public String getName()
	{
		return name;
	}
}
