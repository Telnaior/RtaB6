package tel.discord.rtab.board;

import java.security.SecureRandom;

public enum Boost implements WeightedSpace
{
	//Negative
	N50	(- 50,2),
	N45	(- 45,2),
	N40	(- 40,2),
	N35	(- 35,2),
	N30	(- 30,2),
	N25	(- 25,3),
	N20	(- 20,3),
	N15	(- 15,3),
	N10	(- 10,3),
	//Small
	P1	(   1,1),
	P10	(  10,7),
	P15	(  15,7),
	P20	(  20,7),
	P25	(  25,7),
	P30	(  30,7),
	P35	(  35,7),
	P40	(  40,7),
	P45	(  45,7),
	P50	(  50,7),
	//Big
	P60	(  60,4),
	P70	(  70,4),
	P80	(  80,4),
	P90	(  90,4),
	P100( 100,4),
	P125( 125,2),
	P150( 150,2),
	P200( 200,2),
	P300( 300,1),
	P500( 500,1),
	//Other
	MYSTERY(0,3)
	{
		@Override
		public int getValue()
		{
			SecureRandom r = new SecureRandom();
			if(r.nextDouble() < 0.1)
				return -1*(int)Math.pow(r.nextDouble(7)+1,2);
			else
				return (int)Math.pow(r.nextDouble(7)+1,3);
		}
	};
	
	final int value;
	final int weight;
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
