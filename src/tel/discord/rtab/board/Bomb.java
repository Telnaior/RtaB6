package tel.discord.rtab.board;

public enum Bomb implements WeightedSpace
{
	NORMAL		(14),
	BANKRUPT	( 2),
	LOOTHOLD	( 2),
	CHAIN		( 2),
	DETONATION	( 2),
	REVERSE		( 2),
	DUD			( 1)
	{
		@Override
		public int getWeight(int playerCount)
		{
			//No duds allowed in 2p!
			return (playerCount == 2) ? 0 : weight;
		}
	};

	int weight;
	Bomb(int valueWeight)
	{
		weight = valueWeight;
	}
	@Override
	public int getWeight(int playerCount)
	{
		//Duds override this to check 2p, everything else doesn't care
		return weight;
	}
}