package tel.discord.rtab.board;

import tel.discord.rtab.events.*;

public enum EventType implements WeightedSpace
{
	BOOST_CHARGER		( 7) { public EventSpace getEvent() { return new BoostCharger(); } },
	DOUBLE_DEAL			( 7) { public EventSpace getEvent() { return new DoubleDeal(); } },
	STREAK_BONUS		( 7) { public EventSpace getEvent() { return new StreakBonus(); } },
	DRAW_TWO			( 6) { public EventSpace getEvent() { return new DrawCards(2); } },
	RTAB_MARKET			( 6) { public EventSpace getEvent() { return new Market(); } },
	QUAD_DAMAGE			( 6) { public EventSpace getEvent() { return new OneShotBooster(4); } },
	BOWSER				( 5) { public EventSpace getEvent() { return new Bowser(); } },
	PEEK_REPLENISH		( 5) { public EventSpace getEvent() { return new PeekReplenish(); } },
	SOMETHING_FOR_ALL	( 5) { public EventSpace getEvent() { return new SomethingForEveryone(); } },
	SPOILER_TAG			( 4) { public EventSpace getEvent() { return new HiddenCommandsForAll(); }
		@Override
		public int getWeight(int playerCount)
		{
			//This needs to be less common the bigger the game is
            return switch (playerCount) {
                case 16, 15, 14, 13, 12, 11 -> 1;
                case 10, 9, 8 -> 2;
                case 7, 6, 5 -> 3;
                default -> weight;
            };
		}
	},
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
	ONEBUCKBEHIND		( 4) { public EventSpace getEvent() { return new OneBuckBehind(); } },	//Author: JerryEris
	SPLIT_SHARE			( 3) { public EventSpace getEvent() { return new SplitAndShare(); } },
	BOOST_MAGNET		( 3) { public EventSpace getEvent() { return new BoostMagnet(); } },
	DRAW_FOUR			( 3) { public EventSpace getEvent() { return new DrawCards(4); }
	@Override
	public int getWeight(int playerCount)
	{
		//This space would be a little too painful in a small game.
		return switch (playerCount) {
			case 2 -> 0;
			case 3 -> 1;
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
	TIMES_TEN			( 0) { public EventSpace getEvent() { return new OneShotBooster(10); } },
	REVIVAL_CHANCE		( 0) { public EventSpace getEvent() { return new RevivalChance(); } },
	REVERSE				( 0) { public EventSpace getEvent() { return new Reverse(); } },
	CURSED_BOMB			( 0) { public EventSpace getEvent() { return new CursedBomb(); } },
	CASH_FOR_ALL		( 0) { public EventSpace getEvent() { return new CashForAll(); } },		//Author: JerryEris
	MINIGAMES_FOR_ALL	( 0) { public EventSpace getEvent() { return new MinigamesForAll(); }	//Author: StrangerCoug
		/*@Override
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
		}*/
	};

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
