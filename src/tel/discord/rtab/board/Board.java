package tel.discord.rtab.board;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import tel.discord.rtab.RtaBMath;

public class Board
{
	ArrayList<SpaceType> typeBoard;
	ArrayList<Cash> cashBoard;
	ArrayList<Boost> boostBoard;
	ArrayList<BombType> bombBoard;
	ArrayList<Game> gameBoard;
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
		bombBoard.addAll(generateSpaces(size, players, BombType.values()));
		gameBoard.addAll(generateSpaces(size, players, Game.values()));
		eventBoard.addAll(generateSpaces(size, players, EventType.values()));
	}
	
	public void rerollSpace(int space, int players)
	{
		typeBoard.set(space, generateSpaces(1, players, SpaceType.values()).get(0));
		cashBoard.set(space, generateSpaces(1, players, Cash.values()).get(0));
		boostBoard.set(space, generateSpaces(1, players, Boost.values()).get(0));
		bombBoard.set(space, generateSpaces(1, players, BombType.values()).get(0));
		gameBoard.set(space, generateSpaces(1, players, Game.values()).get(0));
		eventBoard.set(space, generateSpaces(1, players, EventType.values()).get(0));
	}
	
	public static <T extends WeightedSpace> T generateSpace(T[] values)
	{
		return generateSpaces(1, 4, values).get(0);
	}
	
	public static <T extends WeightedSpace> T generateSpace(int players, T[] values)
	{
		return generateSpaces(1, players, values).get(0);
	}
	
	public static <T extends WeightedSpace> List<T> generateSpaces(int spaces, int players, T[] values)
	{
		//Set up our return variable
		ArrayList<T> board = new ArrayList<>(spaces);
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
			random = RtaBMath.random() * totalWeight;
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
		switch (getType(location)) {
			case BOMB, GB_BOMB -> {
				// do nothing; just return
			}
			case GRAB_BAG -> typeBoard.set(location, SpaceType.GB_BOMB);
			default -> typeBoard.set(location, SpaceType.BOMB);
		}
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
        Collections.fill(eventBoard, curse);
	}
	public void forceExplosiveBomb(int location)
	{
		if(bombBoard.get(location) == BombType.DUD)
			bombBoard.set(location, BombType.NORMAL);
	}
	public void bankruptCurse()
	{
        Collections.fill(bombBoard, BombType.BANKRUPT);
	}
	public void lockdownBombs()
	{
		Collections.fill(bombBoard, BombType.LOCKDOWN);
	}
	public void cursedBomb(int location)
	{
		typeBoard.set(location, SpaceType.EVENT);
		eventBoard.set(location, EventType.CURSED_BOMB);
	}
	public void makeSeasonal(int location)
	{
		typeBoard.set(location, SpaceType.EVENT);
		eventBoard.set(location, EventType.REVIVAL_CHANCE);
	}
	
	public String truesightSpace(int location, int baseNumerator, int baseDenominator)
	{
		switch (typeBoard.get(location)) {
			case CASH -> {
				if (cashBoard.get(location) == Cash.MYSTERY)
					return "Mystery Money";
				else if (cashBoard.get(location) == Cash.PRIZE)
					return "Prize";
				else {
					int cashAmount = RtaBMath.applyBaseMultiplier(cashBoard.get(location).getValue().getLeft(), baseNumerator, baseDenominator);
					return (cashAmount < 0 ? "-" : "") + String.format("$%,d", Math.abs(cashBoard.get(location).getValue().getLeft()));
				}
			}
			case BOOSTER -> {
				if (boostBoard.get(location) == Boost.MYSTERY)
					return "Mystery Boost";
				else
					return String.format("%+d%% Boost", boostBoard.get(location).getValue());
			}
			case GAME -> {
				return gameBoard.get(location).getName();
			}
			case EVENT -> {
				return eventBoard.get(location).getName();
			}
			case GRAB_BAG -> {
				return "Grab Bag";
			}
			case BLAMMO -> {
				return "BLAMMO";
			}
			case BOMB -> {
				return bombBoard.get(location).getName();
			}
			case GB_BOMB -> {
				return "GRAB BAG BOMB";
			}
			default -> { //This will never happen
				return "thing your aunt gave you which you don't know what it is";
			}
		}
	}
}
