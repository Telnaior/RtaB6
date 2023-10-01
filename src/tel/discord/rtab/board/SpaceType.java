package tel.discord.rtab.board;

public enum SpaceType implements WeightedSpace
{
	//Total weight 200: 1pt = 0.5% chance
	CASH	(116, false, true, false, false, false)
	{
		@Override
		public int getWeight(int playerCount)
		{
			//Boost cash frequency in large games
			//And even more in extra large games
			if(playerCount >= 8)
				return weight + 10;
			else if(playerCount >= 6)
				return weight + 5;
			//Normal occurence in moderately-sized games
			else return weight;
		}
	},
	BOOSTER	(26, false, false, true, false, false),
	GAME	(26, false, false, false, true, false)
	{
		@Override
		public int getWeight(int playerCount)
		{
			//Boost or reduce minigame frequency depending on playercount
			//More minigames in small games
			if(playerCount < 4)
				return weight + 5;
			//Very few minigames in extra large games
			else if(playerCount >= 8)
				return weight - 10;
			//Few minigames in large games
			else if(playerCount >= 6)
				return weight - 5;
			//Normal occurence in moderately-sized games
			else return weight;
		}
	},
	EVENT	(26, false, false, false, false, true)
	{
		@Override
		public int getWeight(int playerCount)
		{
			//Fewer events in small games
			if(playerCount < 4)
				return weight - 5;
			else return weight;
		}
	},
	GRAB_BAG( 5, false, true, true, true, true),
	BLAMMO  ( 1, false, false, false, false, false),
	GB_BOMB ( 0, true, true, true, true, false),
	BOMB	( 0, true, false, false, false, false); //Never generated, but tends to end up on the board anyway
	
	final int weight;
	final boolean bomb;
	final boolean cash;
	final boolean boost;
	final boolean game;
	final boolean event;
	SpaceType(int spaceWeight, boolean isBomb, boolean isCash, boolean isBoost, boolean isGame, boolean isEvent)
	{
		weight = spaceWeight;
		bomb = isBomb;
		cash = isCash;
		boost = isBoost;
		game = isGame;
		event = isEvent;
	}
	@Override
	public int getWeight(int playerCount)
	{
		//Hoo boy. Lots of things will override this.
		return weight;
	}
	public boolean isBomb()
	{
		return bomb;
	}
	public boolean isCash()
	{
		return cash;
	}
	public boolean isBoost()
	{
		return boost;
	}
	public boolean isGame()
	{
		return game;
	}
	public boolean isEvent()
	{
		return event;
	}
}
