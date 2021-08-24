package tel.discord.rtab.board;

public enum SpaceType implements WeightedSpace
{
	//Total weight 200: 1pt = 0.5% chance
	CASH	(116)
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
	BOOSTER	(26),
	GAME	(26)
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
	EVENT	(26)
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
	GRAB_BAG( 5),
	BLAMMO  ( 1),
	GB_BOMB ( 0),
	BOMB	( 0); //Never generated, but tends to end up on the board anyway
	
	int weight;
	SpaceType(int spaceWeight)
	{
		weight = spaceWeight;
	}
	@Override
	public int getWeight(int playerCount)
	{
		//Hoo boy. Lots of things will override this.
		return weight;
	}
}
