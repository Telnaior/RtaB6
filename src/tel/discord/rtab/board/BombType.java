package tel.discord.rtab.board;

import tel.discord.rtab.bombs.*;

public enum BombType implements WeightedSpace
{
	NORMAL		( 1, "BOMB") { public Bomb getBomb() { return new NormalBomb(); } };

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