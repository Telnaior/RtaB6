package tel.discord.rtab.board;

import tel.discord.rtab.events.*;

public enum EventType implements WeightedSpace
{
	BOOST_CHARGER	( 7,"Boost Charger") { public EventSpace getEvent() { return new BoostCharger(); } },
	DOUBLE_DEAL		( 7,"Double Deal") { public EventSpace getEvent() { return new DoubleDeal(); } },
	STREAK_BONUS	( 7,"Streak Bonus") { public EventSpace getEvent() { return new StreakBonus(); } },
	CASHFOR_ALL	( 1,"Cash For All") { public EventSpace getEvent() { return new CashForAll(); } },
	MINIGAMESFORALL	( 1,"Minigames For All") { public EventSpace getEvent() { return new MinigamesForAll(); } }/*,
	DRAW_TWO		( 6,"Draw Two"),
	PEEK_REPLENISH	( 6,"Extra Peek"),
	REVERSE_ORDER	( 6,"Reverse"),
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
	JACKPOT			( 1,"Jackpot")*/;

	int weight;
	String name;
	EventType(int valueWeight, String eventName)
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
	public abstract EventSpace getEvent();
}
