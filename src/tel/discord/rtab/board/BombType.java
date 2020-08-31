package tel.discord.rtab.board;

import tel.discord.rtab.bombs.*;

public enum BombType implements WeightedSpace
{
	NORMAL		(14, "BOMB") { public Bomb getBomb() { return new NormalBomb(); } },
	/* TODO - remove comment block when other bombs ready
	BANKRUPT	( 2, "BANKRUPT BOMB"),
	LOOTHOLD	( 2, "BOOST/GAME HOLD BOMB"),
	CHAIN		( 2, "CLUSTER BOMB"),
	DETONATION	( 2, "COLLATERAL DAMAGE BOMB"),
	REVERSE		( 2, "REVERSE BOMB"),
	*/
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