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
	
	public String addBomb(int location)
	{
		String whatItWas = truesightSpace(location);
		typeBoard.set(location, SpaceType.BOMB);
		return whatItWas;
	}
	
	public String defuseBomb(int location, int players)
	{
		String whatItWas = truesightSpace(location);
		if(typeBoard.get(location) == SpaceType.BOMB)
			typeBoard.set(location, generateSpaces(1, players, SpaceType.values()).get(0));
		return whatItWas;
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
	public Game getGame(int location)
	{
		return gameBoard.get(location);
	}
	public Event getEvent(int location)
	{
		return eventBoard.get(location);
	}
	public Bomb getBomb(int location)
	{
		return bombBoard.get(location);
	}
	
	public String truesightSpace(int location)
	{
		switch(typeBoard.get(location))
		{
		case CASH:
			if(cashBoard.get(location) == Cash.MYSTERY)
				return "Mystery Money";
			else
			{
				int cashAmount = cashBoard.get(location).getValue().getLeft();
				return (cashAmount<0?"-":"")+String.format("$%,d",Math.abs(cashBoard.get(location).getValue().getLeft()));
			}
		case BOOSTER:
			if(boostBoard.get(location) == Boost.MYSTERY)
				return "Mystery Boost";
			else
				return String.format("%+d%% Boost",boostBoard.get(location).getValue());
		case GAME:
			return gameBoard.get(location).toString();
		case EVENT:
			return eventBoard.get(location).getName();
		case GRAB_BAG:
			return "Grab Bag";
		case BLAMMO:
			return "BLAMMO";
		case BOMB:
			return bombBoard.get(location).getName();
		default: //This will never happen
			return "thing your aunt gave you which you don't know what it is";
		}
	}
}
