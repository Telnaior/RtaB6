package tel.discord.rtab.board;

import tel.discord.rtab.events.*;

public enum EventType implements WeightedSpace
{
	BOOST_CHARGER		( 7,"Boost Charger") { public EventSpace getEvent() { return new BoostCharger(); } },
	DOUBLE_DEAL			( 7,"Double Deal") { public EventSpace getEvent() { return new DoubleDeal(); } },
	STREAK_BONUS		( 7,"Streak Bonus") { public EventSpace getEvent() { return new StreakBonus(); } },
	ONEBUCKBEHIND		( 1,"One Buck Behind the Leader") { public EventSpace getEvent() { return new OneBuckBehind(); } },
	MINIGAMES_FOR_ALL	( 1,"Minigames For All") { public EventSpace getEvent() { return new MinigamesForAll(); } },
	PEEK_REPLENISH		( 6,"Extra Peek") { public EventSpace getEvent() { return new PeekReplenish(); } },
	REVERSE				( 6,"Reverse") { public EventSpace getEvent() { return new Reverse(); } },
	CASH_FOR_ALL		( 6,"Cash For All") { public EventSpace getEvent() { return new CashForAll(); } },
	DRAW_TWO			( 5,"Draw Two") { public EventSpace getEvent() { return new DrawCards(2); } },
	BOWSER				( 5,"Bowser Event") { public EventSpace getEvent() { return new Bowser(); } },
	DRAW_FOUR			( 4,"Draw Four") { public EventSpace getEvent() { return new DrawCards(4); }
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
	},/*
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
	MINEFIELD		( 3,"Minefield"),
	SPLIT_SHARE		( 3,"Split & Share"),
	LOCKDOWN		( 2,"Triple Deal Lockdown"),
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
