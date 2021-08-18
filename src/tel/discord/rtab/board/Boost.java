package tel.discord.rtab.board;

public enum Boost implements WeightedSpace
{
	//Negative
	N50	(- 50,0),
	N45	(- 45,0),
	N40	(- 40,0),
	N35	(- 35,0),
	N30	(- 30,0),
	N25	(- 25,0),
	N20	(- 20,0),
	N15	(- 15,0),
	N10	(- 10,0),
	//Small
	P1	(   1,0),
	P10	(  10,1),
	P15	(  15,0),
	P20	(  20,2),
	P25	(  25,0),
	P30	(  30,3),
	P35	(  35,0),
	P40	(  40,4),
	P45	(  45,0),
	P50	(  50,5),
	//Big
	P60	(  60,4),
	P70	(  70,4),
	P80	(  80,4),
	P90	(  90,4),
	P100( 100,4),
	P125( 125,3),
	P150( 150,3),
	P200( 200,3),
	P300( 300,2),
	P500( 500,1),
	//Other
	MYSTERY(0,3)
	{
		@Override
		public int getValue()
		{
			return (int)Math.pow((Math.random()*7)+1,3);
		}
	};
	
	int value;
	int weight;
	Boost(int boostValue, int valueWeight)
	{
		value = boostValue;
		weight = valueWeight;
	}
	@Override
	public int getWeight(int playerCount)
	{
		//Booster values don't care about playercount
		return weight;
	}
	public int getValue()
	{
		return value;
	}
}
