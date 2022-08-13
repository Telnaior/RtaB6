package tel.discord.rtab.board;

import tel.discord.rtab.events.*;

public enum EventType implements WeightedSpace
{
	BOOST_CHARGER		( 7) { public EventSpace getEvent() { return new BoostCharger(); } },
	DOUBLE_DEAL			( 7) { public EventSpace getEvent() { return new DoubleDeal(); } },
	STREAK_BONUS		( 7) { public EventSpace getEvent() { return new StreakBonus(); } },
	REVERSE				( 6) { public EventSpace getEvent() { return new Reverse(); } },
	DRAW_TWO			( 6) { public EventSpace getEvent() { return new DrawCards(2); } },
	RTAB_MARKET			( 6) { public EventSpace getEvent() { return new Market(); } },
	MINIGAMES_FOR_ALL	( 5) { public EventSpace getEvent() { return new MinigamesForAll(); } },	//Author: StrangerCoug
	BOWSER				( 5) { public EventSpace getEvent() { return new Bowser(); } },
	SPOILER_TAG			( 5) { public EventSpace getEvent() { return new HiddenCommandsForAll(); }
		@Override
		public int getWeight(int playerCount)
		{
			//This needs to be less common the bigger the game is
			return switch (playerCount) {
				case 16, 15, 14 -> 1;
				case 13, 12, 11 -> 2;
				case 10, 9, 8 -> 3;
				case 7, 6, 5 -> 4;
				default -> weight;
			};
		}
	},
	PEEK_REPLENISH		( 4) { public EventSpace getEvent() { return new PeekReplenish(); } },
	JOKER				( 4) { public EventSpace getEvent() { return new Joker(); }
		@Override
		public int getWeight(int playerCount)
		{
			//Jokers don't belong in 2p, and have reduced frequency in 3p
			return switch (playerCount) {
				case 2 -> 0;
				case 3 -> 1;
				default -> weight;
			};
		}
	},
	SPLIT_SHARE			( 4) { public EventSpace getEvent() { return new SplitAndShare(); } },
	ONEBUCKBEHIND		( 3) { public EventSpace getEvent() { return new OneBuckBehind(); } },	//Author: JerryEris
	BOOST_MAGNET		( 3) { public EventSpace getEvent() { return new BoostMagnet(); } },
	DRAW_FOUR			( 3) { public EventSpace getEvent() { return new DrawCards(4); }
	@Override
	public int getWeight(int playerCount)
	{
		//This space would be a little too painful in a small game.
		return switch (playerCount) {
			case 2 -> 1;
			case 3 -> 2;
			default -> weight;
		};
	}
},
	MINEFIELD			( 2) { public EventSpace getEvent() { return new Minefield(); } },
	LOCKDOWN			( 2) { public EventSpace getEvent() { return new Lockdown(); } },
	END_ROUND			( 2) { public EventSpace getEvent() { return new FinalCountdown(); } },
	SUPER_JOKER			( 1) { public EventSpace getEvent() { return new SuperJoker(); }
		@Override
		public int getWeight(int playerCount)
		{
			//Super Jokers don't belong in small games
			return (playerCount < 4) ? 0 : weight;
		}
	},
	STARMAN				( 1) { public EventSpace getEvent() { return new Starman(); } },
	JACKPOT				( 1) { public EventSpace getEvent() { return new Jackpot(); } },
	
	//Seasonal events / otherwise rotated out
	LUCKY_SPACE			( 0) { public EventSpace getEvent() { return new LuckySpace(); } },
	TIMES_TEN			( 0) { public EventSpace getEvent() { return new TimesTen(); } },
	CASH_FOR_ALL		( 0) { public EventSpace getEvent() { return new CashForAll(); } };		//Author: JerryEris

	final int weight;
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
