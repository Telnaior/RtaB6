package tel.discord.rtab.board;

public enum Game implements WeightedSpace
{
	TESTGAME(0, "Test Game", "test");
	
	
	String fullName;
	String shortName;
	int weight;
	Game(int valueWeight, String gameName, String miniName)
	{
		fullName = gameName;
		shortName = miniName;
		weight = valueWeight; 
	}
	@Override
	public String toString()
	{
		return fullName;
	}
	public String getShortName()
	{
		return shortName;
	}
	//Returns a new instance of the requested minigame
	//public abstract MiniGame getGame();
	
	@Override
	public int getWeight(int playerCount)
	{
		//Minigame types don't care about playercount
		return weight;
	}
}
