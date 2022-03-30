package tel.discord.rtab.board;

public enum SpaceType implements WeightedSpace
{
	//Total weight 200: 1pt = 0.5% chance
	NUMBER	(1, false),
	BOMB	( 0, true); //Never generated, but tends to end up on the board anyway
	
	int weight;
	boolean bomb;
	SpaceType(int spaceWeight, boolean isBomb)
	{
		weight = spaceWeight;
		bomb = isBomb;
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
}
