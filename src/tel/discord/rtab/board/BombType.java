package tel.discord.rtab.board;

import tel.discord.rtab.bombs.*;

public enum BombType implements WeightedSpace
{
	NORMAL		(33, "BOMB") { public Bomb getBomb() { return new NormalBomb(); } },
	BANKRUPT	( 3, "BANKRUPT BOMB") { public Bomb getBomb() { return new BankruptBomb(); } },
	LOOTHOLD	( 3, "BOOST/GAME HOLD BOMB") { public Bomb getBomb() { return new LootHoldBomb(); } },
	CLUSTER		( 3, "CLUSTER BOMB") { public Bomb getBomb() { return new ClusterBomb(); } },
	REVERSE		( 3, "REVERSE BOMB") { public Bomb getBomb() { return new ReverseBomb(); } },
	COLLATERAL	( 3, "COLLATERAL DAMAGE BOMB") { public Bomb getBomb() { return new CollateralBomb(); } },
	DUD			( 2, "BOMB") //As if we'd let you see that it was fake
	{
		public Bomb getBomb() { return new DudBomb(); }
		@Override
		public int getWeight(int playerCount)
		{
			//No duds allowed in 2p!
			return (playerCount == 2) ? 0 : weight;
		}
	};

	int weight;
	String name;
	BombType(int weight, String name)
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
	public abstract Bomb getBomb();
}