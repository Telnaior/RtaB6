package tel.discord.rtab.board;

import java.util.ArrayList;

public class Board
{
	ArrayList<SpaceType> typeBoard;
	ArrayList<Cash> cashBoard;
	ArrayList<Boost> boostBoard;
	ArrayList<Bomb> bombBoard;
	ArrayList<Game> gameBoard;
	ArrayList<Event> eventBoard;
	
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
		gameBoard = new ArrayList<>();
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
		gameBoard = new ArrayList<>(size);
		eventBoard = new ArrayList<>(size);
		generateBoard(size, players);
	}
	
	public void generateBoard(int size, int players)
	{
		//Create each board in turn
		typeBoard.addAll(generateSpaces(size, players, SpaceType.values()));
		cashBoard.addAll(generateSpaces(size, players, Cash.values()));
		boostBoard.addAll(generateSpaces(size, players, Boost.values()));
		bombBoard.addAll(generateSpaces(size, players, Bomb.values()));
		gameBoard.addAll(generateSpaces(size, players, Game.values()));
		eventBoard.addAll(generateSpaces(size, players, Event.values()));
	}
	
	public <T extends WeightedSpace> ArrayList<T> generateSpaces(int spaces, int players, T[] values)
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
}
