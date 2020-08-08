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
	
	public Board(int size, int players)
	{
		typeBoard = new ArrayList<>(size+1);
		cashBoard = new ArrayList<>(size+1);
		boostBoard = new ArrayList<>(size+1);
		bombBoard = new ArrayList<>(size+1);
		gameBoard = new ArrayList<>(size+1);
		eventBoard = new ArrayList<>(size+1);
		addSpaces(size, players);
	}
	
	void addSpaces(int size, int players)
	{
		//Boost each board
		addToBoard(size, players, typeBoard,SpaceType.values());
		addToBoard(size, players, cashBoard,Cash.values());
		addToBoard(size, players, boostBoard,Boost.values());
		addToBoard(size, players, bombBoard,Bomb.values());
		addToBoard(size, players, gameBoard,Game.values());
		addToBoard(size, players, eventBoard,Event.values());
	}
	private <T extends WeightedSpace> void addToBoard(int spaces, int players, ArrayList<T> board, T[] values)
	{
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
			//And set the value to that
			board.add(values[search]);
		}
		return;
	}
}
