package tel.discord.rtab.board;

public enum Bomb implements WeightedSpace
{
	NORMAL		(14, "BOMB"),
	BANKRUPT	( 2, "BANKRUPT BOMB"),
	LOOTHOLD	( 2, "BOOST/GAME HOLD BOMB"),
	CHAIN		( 2, "CLUSTER BOMB"),
	DETONATION	( 2, "COLLATERAL DAMAGE BOMB"),
	REVERSE		( 2, "REVERSE BOMB"),
	DUD			( 1, "BOMB") //As if we'd tell you
	{
		@Override
		public int getWeight(int playerCount)
		{
			//No duds allowed in 2p!
			return (playerCount == 2) ? 0 : weight;
		}
	};

	int weight;
	String name;
	Bomb(int weight, String name)
	{
		this.weight = weight;
		this.name = name;
	}
	public int getWeight(int playerCount)
	{
		return weight;
	}
	public String getName()
	{
		return name;
	}
}