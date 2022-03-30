package tel.discord.rtab.board;

import java.util.ArrayList;

public class Board
{
	ArrayList<SpaceType> typeBoard;	
	ArrayList<Cash> cashBoard;
	ArrayList<Boost> boostBoard;
	ArrayList<BombType> bombBoard;
	ArrayList<EventType> eventBoard;
	
	/**
	 * This constructor creates an empty board.
	 * It must be filled using <code>generateBoard</code> before it can be used.
	 */
	public Board()
	{
		typeBoard = new ArrayList<>();
		cashBoard = new ArrayList<>();
		boostBoard = new ArrayList<>();
		bombBoard = new ArrayList<>();
		eventBoard = new ArrayList<>();
	}
	
	/**
	 * This constructor creates the component boards and fills them with spaces using the class's other methods.
	 * <p>
	 * The board is immediately ready for use.
	 * 
	 * @param size The size of the generated board (which can be extended later using <code>generateBoard</code>)
	 * @param players The number of players the board is for, which affects the space weighting
	 */
	public Board(int size, int players)
	{
		typeBoard = new ArrayList<>(size);
		cashBoard = new ArrayList<>(size);
		boostBoard = new ArrayList<>(size);
		bombBoard = new ArrayList<>(size);
		eventBoard = new ArrayList<>(size);
		generateBoard(size, players);
	}
	
	public void generateBoard(int size, int players)
	{
		//Create each board in turn
		typeBoard.addAll(generateSpaces(size, players, SpaceType.values()));
		cashBoard.addAll(generateSpaces(size, players, Cash.values()));
		boostBoard.addAll(generateSpaces(size, players, Boost.values()));
		bombBoard.addAll(generateSpaces(size, players, BombType.values()));
		eventBoard.addAll(generateSpaces(size, players, EventType.values()));
	}
	
	public void rerollSpace(int space, int players)
	{
		typeBoard.set(space, generateSpaces(1, players, SpaceType.values()).get(0));
		cashBoard.set(space, generateSpaces(1, players, Cash.values()).get(0));
		boostBoard.set(space, generateSpaces(1, players, Boost.values()).get(0));
		bombBoard.set(space, generateSpaces(1, players, BombType.values()).get(0));
		eventBoard.set(space, generateSpaces(1, players, EventType.values()).get(0));
	}
	
	static public <T extends WeightedSpace> T generateSpace(T[] values)
	{
		return generateSpaces(1, 4, values).get(0);
	}
	
	static public <T extends WeightedSpace> T generateSpace(int players, T[] values)
	{
		return generateSpaces(1, players, values).get(0);
	}
	
	static public <T extends WeightedSpace> ArrayList<T> generateSpaces(int spaces, int players, T[] values)
	{
		//Set up our return variable
		ArrayList<T> board = new ArrayList<T>(spaces);
		//Declare possible values and weights
		//Autogenerate cumulative weights
		int[] cumulativeWeights = new int[values.length];
		int totalWeight = 0;
		for(int i=0; i<values.length; i++)
		{
			totalWeight += values[i].getWeight(players);
			cumulativeWeights[i] = totalWeight;
		}
		double random;
		for(int i=0; i<spaces; i++)
		{
			//Get random spot in weight table
			random = Math.random() * totalWeight;
			//Find where we actually landed
			int search=0;
			while(cumulativeWeights[search] < random)
				search++;
			//Find the corresponding value and add it to our spaces
			board.add(values[search]);
		}
		return board;
	}
	
	public void addBomb(int location)
	{
		typeBoard.set(location, SpaceType.BOMB);
	}
	
	public void changeType(int location, SpaceType newType)
	{
		typeBoard.set(location, newType);
	}
	public SpaceType getType(int location)
	{
		return typeBoard.get(location);
	}
	public Cash getCash(int location)
	{
		return cashBoard.get(location);
	}
	public Boost getBoost(int location)
	{
		return boostBoard.get(location);
	}
	public EventType getEvent(int location)
	{
		return eventBoard.get(location);
	}
	public BombType getBomb(int location)
	{
		return bombBoard.get(location);
	}
	public void eventCurse(EventType curse)
	{
		for(int i=0; i<eventBoard.size(); i++)
			eventBoard.set(i, curse);
	}
}
