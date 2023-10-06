package tel.discord.rtab;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import java.util.random.RandomGenerator;

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
	
	public static int applyBankPercentBaseMultiplier(int amount, int baseNumerator, int baseDenominator)
	{
		double baseMultiplier = baseNumerator/(double)(baseDenominator);
		double effectiveMultiplier;
		//Squash large base multipliers down
		if(baseMultiplier < 1)
			effectiveMultiplier = baseMultiplier;
		else if(baseMultiplier >= 10)
			effectiveMultiplier = 2.5;
		else if(baseMultiplier >= 5)
			effectiveMultiplier = 2.0;
		else if(baseMultiplier >= 2.5)
			effectiveMultiplier = 1.5;
		else
			effectiveMultiplier = 1.0;
		//and then multiply
		return (int)(amount * effectiveMultiplier);
	}
	
	public static int getEnhanceCap(int lives, int livesPerEnhance)
	{
		//25 = 1, 75 = 2, 150 = 3, 250 = 4, ..., round down (channel settings can make it more or less than 25)
		int weeks = lives/livesPerEnhance;
		int count = 0;
		while(weeks > count)
		{
			count ++;
			weeks -= count;
		}
		return count;
	}
	
	public static List<Integer> getAdjacentSpaces(int centre, int players)
	{
		ArrayList<Integer> adjacentSpaces = new ArrayList<>(8);
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

	static RandomGenerator r;
	public static synchronized double random() //lol thread safety
	{
		if(r == null)
			r = RandomGenerator.of("L128X1024MixRandom");
		return r.nextDouble();
	}
}
