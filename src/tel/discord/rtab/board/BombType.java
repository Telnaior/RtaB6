package tel.discord.rtab.board;

import tel.discord.rtab.bombs.*;

public enum BombType implements WeightedSpace
{
	NORMAL		(14, "BOMB") { public Bomb getBomb() { return new NormalBomb(); } },
	BANKRUPT	( 2, "BANKRUPT BOMB") { public Bomb getBomb() { return new BankruptBomb(); } },
	LOOTHOLD	( 2, "BOOST/GAME HOLD BOMB") { public Bomb getBomb() { return new LootHoldBomb(); } },
	CLUSTER		( 2, "CLUSTER BOMB") { public Bomb getBomb() { return new ClusterBomb(); } },
	REVERSE		( 2, "REVERSE BOMB") { public Bomb getBomb() { return new ReverseBomb(); } },
	COLLATERAL	( 2, "COLLATERAL DAMAGE BOMB") { public Bomb getBomb() { return new CollateralBomb(); } },
	DUD			( 1, "BOMB") //As if we'd let you see that it was fake
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