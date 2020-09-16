package tel.discord.rtab.board;

import tel.discord.rtab.games.*;

public enum Game implements WeightedSpace
{
	MATH_TIME(1)		{ public MiniGame getGame() { return new MathTime(); } },		//Author: Atia
	CLOSE_SHAVE(1)		{ public MiniGame getGame() { return new CloseShave(); } },		//Author: JerryEris
	BOMB_ROULETTE(1)	{ public MiniGame getGame() { return new BombRoulette(); } },	//Author: StrangerCoug
	MONEY_CARDS(1)		{ public MiniGame getGame() { return new MoneyCards(); } },		//Author: StrangerCoug
	SHUT_THE_BOX(1)		{ public MiniGame getGame() { return new ShutTheBox(); } },		//Author: StrangerCoug
	TESTGAME(1)			{ public MiniGame getGame() { return new TestGame(); } };
	
	String fullName;
	String shortName;
	boolean bonus;
	int weight;
	Game(int valueWeight)
	{
		fullName = getGame().getName();
		shortName = getGame().getShortName();
		bonus = getGame().isBonus();
		weight = valueWeight; 
	}
	@Override
	public String toString()
	{
		return fullName;
	}
	public String getName()
	{
		return toString();
	}
	public String getShortName()
	{
		return shortName;
	}
	public boolean isBonus()
	{
		return bonus;
	}
	//Returns a new instance of the requested minigame
	public abstract MiniGame getGame();
	
	@Override
	public int getWeight(int playerCount)
	{
		//Minigame types don't care about playercount
		return weight;
	}
}
