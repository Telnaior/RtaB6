package tel.discord.rtab.board;

import tel.discord.rtab.events.*;

public enum EventType implements WeightedSpace
{
	BOOST_CHARGER		( 7) { public EventSpace getEvent() { return new BoostCharger(); } },
	DOUBLE_DEAL			( 7) { public EventSpace getEvent() { return new DoubleDeal(); } },
	STREAK_BONUS		( 7) { public EventSpace getEvent() { return new StreakBonus(); } },
	ONEBUCKBEHIND		( 1) { public EventSpace getEvent() { return new OneBuckBehind(); } },		//Author: JerryEris
	MINIGAMES_FOR_ALL	( 1) { public EventSpace getEvent() { return new MinigamesForAll(); } },	//Author: StrangerCoug
	PEEK_REPLENISH		( 6) { public EventSpace getEvent() { return new PeekReplenish(); } },
	REVERSE				( 6) { public EventSpace getEvent() { return new Reverse(); } },
	CASH_FOR_ALL		( 6) { public EventSpace getEvent() { return new CashForAll(); } },			//Author: JerryEris
	DRAW_TWO			( 5) { public EventSpace getEvent() { return new DrawCards(2); } },
	BOWSER				( 5) { public EventSpace getEvent() { return new Bowser(); } },
	DRAW_FOUR			( 4) { public EventSpace getEvent() { return new DrawCards(4); }
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
	SPOILER_TAG		( 5) { public EventSpace getEvent() { return new HiddenCommandsForAll(); }
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
	BOOST_MAGNET	( 4) { public EventSpace getEvent() { return new BoostMagnet(); } },
	JOKER			( 4) { public EventSpace getEvent() { return new Joker(); }
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
	MINEFIELD		( 3) { public EventSpace getEvent() { return new Minefield(); } },
	SPLIT_SHARE		( 3) { public EventSpace getEvent() { return new SplitAndShare(); } },
	LOCKDOWN		( 2) { public EventSpace getEvent() { return new Lockdown(); } },
	END_ROUND		( 2) { public EventSpace getEvent() { return new FinalCountdown(); } },
	SUPER_JOKER		( 1) { public EventSpace getEvent() { return new SuperJoker(); }
		@Override
		public int getWeight(int playerCount)
		{
			//Super Jokers don't belong in small games
			return (playerCount < 4) ? 0 : weight;
		}
	},
	STARMAN			( 1) { public EventSpace getEvent() { return new Starman(); } },
	JACKPOT			( 1) { public EventSpace getEvent() { return new Jackpot(); } };

	int weight;
	EventType(int valueWeight)
	{
		weight = valueWeight;
	}
	@Override
	public int getWeight(int playerCount)
	{
		//This gets overriden by a few events that don't belong in small or large games
		return weight;
	}
	public String getName()
	{
		return getEvent().getName();
	}
	public abstract EventSpace getEvent();
}
