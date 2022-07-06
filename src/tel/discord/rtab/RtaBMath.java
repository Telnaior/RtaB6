package tel.discord.rtab;

import java.util.ArrayList;

public final class RtaBMath
{
	//Private constructor to prevent instantiation
	private RtaBMath() 
	{
	    throw new java.lang.UnsupportedOperationException("This is a utility class and cannot be instantiated.");
	}
	
	public static int calculateEntryFee(int money, int lives)
	{
		int entryFee = Math.max(money/500,20000);
		entryFee *= 5 - lives;
		return entryFee;
	}
	
	public static int applyBaseMultiplier(int amount, int baseNumerator, int baseDenominator)
	{
		long midStep = amount * (long)baseNumerator;
		long endStep = midStep / baseDenominator;
		if(endStep > 1_000_000_000)
			endStep = 1_000_000_000;
		if(endStep < -1_000_000_000)
			endStep = -1_000_000_000;
		return (int)endStep;
	}
	
	public static int getEnhanceCap(int lives)
	{
		//25 = 1, 75 = 2, 150 = 3, 250 = 4, ..., round down
		int weeks = lives/25;
		int count = 0;
		while(weeks > count)
		{
			count ++;
			weeks -= count;
		}
		return count;
	}
	
	public static ArrayList<Integer> getAdjacentSpaces(int centre, int players)
	{
		ArrayList<Integer> adjacentSpaces = new ArrayList<Integer>(8);
		//Start by figuring out the board size
		int size = (players+1) * 5;
		int columns = Math.max(5, players+1);
		//Up-Left
		if(centre >= columns && centre % columns != 0)
			adjacentSpaces.add((centre-1) - columns);
		//Up
		if(centre >= columns)
			adjacentSpaces.add(centre - columns);
		//Up-Right
		if(centre >= columns && centre % columns != (columns-1))
			adjacentSpaces.add((centre+1) - columns);
		//Left
		if(centre % columns != 0)
			adjacentSpaces.add(centre-1);
		//Right
		if(centre % columns != (columns-1))
			adjacentSpaces.add(centre+1);
		//Down-Left
		if(centre < (size - columns) && centre % columns != 0)
			adjacentSpaces.add((centre-1) + columns);
		//Down
		if(centre < (size - columns))
			adjacentSpaces.add(centre + columns);
		//Down-Right
		if(centre < (size - columns) && centre % columns != (columns-1))
			adjacentSpaces.add((centre+1) + columns);
		return adjacentSpaces;
	}
}
